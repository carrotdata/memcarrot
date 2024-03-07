/*
 Copyright (C) 2021-present Carrot, Inc.

 <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 Server Side Public License, version 1, as published by MongoDB, Inc.

 <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 Server Side Public License for more details.

 <p>You should have received a copy of the Server Side Public License along with this program. If
 not, see <http://www.mongodb.com/licensing/server-side-public-license>.
*/
package com.onecache.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.onecache.core.support.Memcached;
import com.onecache.server.util.Utils;

/** Carrot node server (single thread) */
public class OnecacheServer {
  private static final Logger log = LogManager.getLogger(OnecacheServer.class);

  static int bufferSize = 256 * 1024;
  /*
   * Input buffer
   */
  static ThreadLocal<ByteBuffer> inBuf =
      new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
          return ByteBuffer.allocateDirect(bufferSize);
        }
      };

  /*
   * Output buffer
   */
  static ThreadLocal<ByteBuffer> outBuf =
      new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
          return ByteBuffer.allocateDirect(bufferSize);
        }
      };

  private String host;
  private int port;
  private Memcached memcached;
  /** Executor service (request handlers) */
  static RequestHandlers service;
  /**
   *
   * @param host
   * @param port
   */
  public OnecacheServer(String host, int port) {
    this.port = port;
    this.host = host;
  }


  public void runNodeServer() throws IOException {
    // Create memcached support instance
    memcached = new Memcached();
    // Start request handlers
    startRequestHandlers();
    
    final Selector selector = Selector.open(); // selector is open here
    log.debug("Selector started");

    // ServerSocketChannel: selectable channel for stream-oriented listening sockets
    ServerSocketChannel serverSocket = ServerSocketChannel.open();
    log.debug("Server socket opened");

    InetSocketAddress serverAddr = new InetSocketAddress(host, port);

    // Binds the channel's socket to a local address and configures the socket to listen for
    // connections
    serverSocket.bind(serverAddr);
    // Adjusts this channel's blocking mode.
    serverSocket.configureBlocking(false);
    int ops = serverSocket.validOps();
    serverSocket.register(selector, ops, null);
    log.debug("[{}] Server started on port: {}]", Thread.currentThread().getName(), port);

    Consumer<SelectionKey> action =
        key -> {
          try {
            if (!key.isValid()) return;
            if (key.isValid() && key.isAcceptable()) {
              SocketChannel client = serverSocket.accept();
              // Adjusts this channel's blocking mode to false
              client.configureBlocking(false);
              client.setOption(StandardSocketOptions.TCP_NODELAY, true);
              client.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
              client.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
              // Operation-set bit for read operations
              client.register(selector, SelectionKey.OP_READ);
              log.debug("[{}] Connection Accepted: {}]", Thread.currentThread().getName(), client.getLocalAddress());
            } else if (key.isValid() && key.isReadable()) {
              // Check if it is in use
              RequestHandlers.Attachment att = (RequestHandlers.Attachment) key.attachment();
              if (att != null && att.inUse()) return;
              // process request
              //processRequest(key);
              service.submit(key);
            }
          } catch (IOException e) {
            log.error("StackTrace: ", e);
            log.error("Shutting down server ...");
            memcached.dispose();
            memcached = null;
            log.error("Bye-bye folks. See you soon :)");
          }
        };
    // Infinite loop..
    // Keep server running
    while (true) {
      // Selects a set of keys whose corresponding channels are ready for I/O operations
      selector.select(action);
    }
  }

  long totalReqTime = 0;
  int ricCount = 0;
  int iter = 0;

  /**
   * Process incoming request
   *
   * @param key selection key for a socket channel
   */
  private void processRequest(SelectionKey key) {
    long startTime = System.nanoTime();
    if (key.attachment() == null) {
      key.attach(new RequestHandlers.Attachment());
    }

    SocketChannel channel = (SocketChannel) key.channel();
    // Read request first
    ByteBuffer in = inBuf.get();
    ByteBuffer out = outBuf.get();
    in.clear();
    out.clear();

    try {
      long startCounter = System.nanoTime();
      long max_wait_ns = 100000000; // 100ms
      long startClock = 0;

      while (true) {
        iter++;
        int num = channel.read(in);

        if (num < 0) {
          // End-Of-Stream - socket was closed, cancel the key
          key.cancel();
          break;
        } else if (num == 0) {
          if (System.nanoTime() - startCounter > max_wait_ns) {
            break;
          }
          continue;
        }
        // Try to parse
        int oldPos = in.position();
        if (startClock == 0) startClock = System.nanoTime();
        if (!requestIsComplete(in)) {
          // restore position
          in.position(oldPos);
          in.limit(in.capacity());
          continue;
        }

        ricCount++;

        in.position(oldPos);
        // Process request
        boolean shutdown = CommandProcessor.process(memcached, in, out);

        // TODO: this is poor man terminator - FIXME
        if (shutdown) {
          shutdownNode();
        }
        // send response back
        out.flip();
        while (out.hasRemaining()) {
          channel.write(out);
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
    }
    totalReqTime += System.nanoTime() - startTime;
  }

  private void shutdownNode() {
    System.exit(0);
  }

  /**
   * Release key - mark it not in use
   *
   * @param key
   */
  void release(SelectionKey key) {
    RequestHandlers.Attachment att = (RequestHandlers.Attachment) key.attachment();
    att.setInUse(false);
  }

  /**
   * Checks if request is complete
   *
   * @param in input data buffer
   * @return true - complete, false - otherwise
   */
  private boolean requestIsComplete(ByteBuffer in) {
    return Utils.requestIsComplete(in);
  }

  private void startRequestHandlers() throws IOException {
    OnecacheConf conf = OnecacheConf.getConf();
    int numThreads = conf.getThreadPoolSize();
    service = RequestHandlers.create(memcached, numThreads);
    service.start();
  }
}
