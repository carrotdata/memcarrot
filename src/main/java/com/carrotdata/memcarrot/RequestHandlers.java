/*
 * Copyright (C) 2024-present Carrot Data, Inc. 
 * <p>This program is free software: you can redistribute it
 * and/or modify it under the terms of the Server Side Public License, version 1, as published by
 * MongoDB, Inc.
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the Server Side Public License for more details. 
 * <p>You should have received a copy of the Server Side Public License along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package com.carrotdata.memcarrot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;

public class RequestHandlers {

  private static final Logger log = LogManager.getLogger(RequestHandlers.class);

  static long epochStartNanos = System.nanoTime();

  static class Attachment {
    private long accessTime;
    private boolean inUse = false;

    Attachment() {
      accessTime = System.nanoTime() - epochStartNanos;
      setInUse(true);
    }

    boolean inUse() {
      return inUse;
    }

    void setInUse(boolean b) {
      this.inUse = b;
    }

    long lastAccess() {
      return accessTime;
    }

    void access() {
      accessTime = System.nanoTime() - epochStartNanos;
    }
  }

  /*
   * Request handlers
   */
  WorkThread[] workers;

  volatile boolean shutdown;

  private RequestHandlers(Memcached store, int numThreads, int bufferSize) {
    workers = new WorkThread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      workers[i] = new WorkThread(store, bufferSize);
    }
  }

  public static RequestHandlers create(Memcached store, int numThreads, int bufferSize) {
    return new RequestHandlers(store, numThreads, bufferSize);
  }

  public void start() {
    Arrays.stream(workers).forEach(Thread::start);
    log.debug("Started request handlers: count={}", workers.length);
  }

  /**
   * Submit next socket channel for processing
   * @param key selection key for socket channel
   */
  public void submit(SelectionKey key) {
    if (this.shutdown) {
      return;
    }
    while (true) {
      for (int i = 0; i < workers.length; i++) {
        if (workers[i].isBusy()) continue;
        workers[i].nextKey(key);
        return;
      }
    }
  }

  /** Shutdown service */
  public void shutdown() {
    this.shutdown = true;
    Arrays.stream(workers).forEach(Thread::interrupt);
    log.debug("Stopped request handlers: count={}", workers.length);
  }
}

class WorkThread extends Thread {

  private static final Logger log = LogManager.getLogger(WorkThread.class);

  /*
   * Busy loop max iteration
   */
  private static final long BUSY_LOOP_MAX = 10000;

  /*
   * Input buffer
   */
  ByteBuffer inBuf;

  /**
   * Address of input buffer
   */
  long in_ptr;

  /*
   * Output buffer
   */
  ByteBuffer outBuf;

  /**
   * Output buffer address
   */
  long out_ptr;

  /*
   * Data store
   */
  private final Memcached store;

  /** Next selection key atomic reference */
  private final AtomicReference<SelectionKey> nextKey = new AtomicReference<>();

  /*
   * Busy flag
   */
  private volatile boolean busy = false;

  private int bufferSize;

  private static AtomicInteger counter = new AtomicInteger();

  /**
   * Default constructor
   * @param store data store
   */
  WorkThread(Memcached store, int bufferSize) {
    super("mc-pool-thread-" + counter.getAndIncrement());
    this.store = store;
    this.bufferSize = bufferSize;
    setDaemon(true);
  }

  private ByteBuffer getInputBuffer() {
    if (inBuf == null) {
      inBuf = ByteBuffer.allocateDirect(bufferSize);
      in_ptr = UnsafeAccess.address(inBuf);
    }
    return inBuf;
  }

  private ByteBuffer getOutputBuffer() {
    if (outBuf == null) {
      outBuf = ByteBuffer.allocateDirect(bufferSize);
      out_ptr = UnsafeAccess.address(outBuf);
    }
    return outBuf;
  }

  /**
   * Is thread busy working?
   * @return busy
   */
  boolean isBusy() {
    return busy;
  }

  /**
   * Submits next selection key for processing
   * @param key selection key
   */
  void nextKey(SelectionKey key) {
    key.attach(new RequestHandlers.Attachment());
    busy = true;
    while (!nextKey.compareAndSet(null, key)) {
      Thread.onSpinWait();
    }
    LockSupport.unpark(this);
  }

  /**
   * Release key - mark it not in use
   * @param key
   */
  void release(SelectionKey key) {
    RequestHandlers.Attachment att = (RequestHandlers.Attachment) key.attachment();
    att.setInUse(false);
  }

  /** Busy loop with expo-linear back off */
  private SelectionKey waitForKey() {
    long counter = 0;
    long timeout = 50000;
    SelectionKey key = null;
    // wait for next task
    while ((key = nextKey.get()) == null) {
      if (Thread.interrupted()) {
        return null;
      }
      // Exponential (actually, linear :)) back off
      if (counter < BUSY_LOOP_MAX) {
        counter++;
        Thread.onSpinWait();
      } else {
        LockSupport.parkNanos(timeout);
      }
    }
    return key;
  }

  /*
   * Main loop
   */
  public void run() {

    // infinite loop
    while (true) {
      SelectionKey key = null;
      // Double busy set to false
      // busy = false;
      // Can it override
      key = waitForKey();
      if (key == null) {
        log.debug("Thread {} got interrupt signal, exiting", Thread.currentThread().getName());
        return;
      }
      // We are busy now
      //busy = true;
      SocketChannel channel = (SocketChannel) key.channel();

      // Read request first
      ByteBuffer in = getInputBuffer();
      ByteBuffer out = getOutputBuffer();
      in.clear();
      out.clear();

      try {
        long startCounter = 0; 
        long max_wait_ns = 500_000_000; // 500ms - FIXME - make it configurable
        int inputSize  = 0;

        outer: while (true) {
          int num = channel.read(in);
          if (num < 0) {
            // End-Of-Stream - socket was closed, cancel the key
            key.cancel();
            channel.close();
            break;
          } else if (num == 0) {
            if (startCounter == 0) {
              startCounter = System.nanoTime();
            }
            if (System.nanoTime() - startCounter > max_wait_ns) {
              // FIXME: Request timeout
              // timeout
              key.cancel();
              channel.close();
              break;
            }
            Thread.onSpinWait();
            continue;
          }
          startCounter = 0;
          int consumed = 0;
          inputSize += num;;
          
          while (consumed < inputSize) {
            // Try to parse
            // Process request using buffer's addresses
            int responseLength = CommandProcessor.process(store, in_ptr + consumed,
              inputSize - consumed, out_ptr, bufferSize);
            if (responseLength < 0) {
              // command is incomplete
              // check if we consumed something, then compact input buffer
              if (consumed > 0) {
                // compact input buffer
                UnsafeAccess.copy(in_ptr + consumed, in_ptr, inputSize - consumed);
                inputSize -= consumed;
                in.position(inputSize);
              }
              continue outer;
            }
            if (responseLength > 0) {
              out.limit(responseLength);
              out.position(0);
              // send response back
              while (out.hasRemaining()) {
                // FIXME: Can we stuck here?
                channel.write(out);
              }
            }
            consumed += CommandProcessor.getLastExecutedCommand().inputConsumed();
          }
          break;
        }
      } catch (IOException e) {
        String msg = e.getMessage();
        if (!msg.equals("Connection reset by peer")) {
          // TODO
          log.error("StackTrace: ", e);
        }
        key.cancel();
      } finally {
        // Release selection key - ready for the next request
        release(key);
        nextKey.set(null);
        // set busy flag to false
        busy = false;
      }
    }
  }
}
