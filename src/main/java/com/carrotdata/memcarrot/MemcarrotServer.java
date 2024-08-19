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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;


import com.carrotdata.cache.Cache;
import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.CacheConfig;

/** Memcarrot node server */
public class MemcarrotServer {
  private static final Logger log = LogManager.getLogger(MemcarrotServer.class);
  /**
   * Executor service (request handlers)
   **/
  RequestHandlers service;

  /**
   * Host name
   */
  String host;
  /**
   * Host port
   */
  int port;
  /**
   * Memcached support
   */
  Memcached memcached;

  /**
   * Buffer size
   */
  int bufferSize;

  /**
   * TCP Send/Receive buffer size
   */
  int tcpSndRcvBufferSize;

  /**
   * Selector
   */
  Selector selector;

  /**
   * Server socket
   */
  ServerSocketChannel serverSocket;

  /**
   * Server runner thread
   */
  Thread serverRunner;
  
  /**
   * Server started and ready to accept connections
   */
  volatile boolean started;
  /**
   * Server failed to start
   */
  volatile boolean failed;

  /**
   * Constructor
   * @param host
   * @param port
   * @throws IOException
   */
  public MemcarrotServer(String host, int port) throws IOException {
    this.port = port;
    this.host = host;
    this.bufferSize = MemcarrotConf.getConf().getKeyValueMaxSize();
    this.tcpSndRcvBufferSize = MemcarrotConf.getConf().getSndRcvBufferSize();

  }

  public MemcarrotServer() throws IOException {
    MemcarrotConf config = MemcarrotConf.getConf();
    this.port = config.getServerPort();
    this.host = config.getServerAddress();
    this.bufferSize = config.getKeyValueMaxSize();
    this.tcpSndRcvBufferSize = MemcarrotConf.getConf().getSndRcvBufferSize();

  }

  public MemcarrotServer(MemcarrotConf config) throws IOException {
    this.port = config.getServerPort();
    this.host = config.getServerAddress();
    this.bufferSize = config.getKeyValueMaxSize();
    this.tcpSndRcvBufferSize = config.getSndRcvBufferSize();
  }

  /**
   * Used for testing only
   * @param m memcached support
   */
  void setMemachedSupport(Memcached m) {
    this.memcached = m;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void start() {
    if (serverRunner != null) {
      return;
    }
    serverRunner = new Thread(() -> {
      IOException ee = null;
      try {
        run();
      } catch (IOException e) {
        log.error(e);
        ee = e;
      } catch (ClosedSelectorException ex) {
        // We closed selector on shutdown - its OK

      }
      onShutdown(ee);
    });
    serverRunner.setName("memcarrot-main");
    serverRunner.start();
    while (!this.started) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {

      }
      if (this.failed) {
        // Silent exit, we have already message and stack trace in the log file
        System.exit(-1);
      }
    }
  }

  public void stop() {
    service.shutdown();
    try {
      // this should interrupt main I/O loop thread
      selector.close();
      serverSocket.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }
  }

  // Sub-classes can override
  protected void onShutdown(IOException e) {
    String msgStart = null;
    if (e == null) {
      msgStart = "Server received shutdown request. ";
    } else {
      msgStart = "Server error. ";
    }
    Cache c = memcached.getCache();
    CacheConfig config = c.getCacheConfig();
    if (!config.isSaveOnShutdown(c.getName())) {
      memcached.dispose();
      // If not save on shutdown - dispose
      String msg = msgStart + "Disposed internal cache";
      if (e == null) {
        log.info(msg);
      } else {
        log.error(msg);
      }
    }
    String msg = msgStart + "Exited.";
    if (e == null) {
      log.info(msg);
    } else {
      log.error(msg);
    }
  }

  private void run() throws IOException {

    try {
      // Create memcached support instance if not null
      // It is not null in tests
      if (memcached == null) {
        memcached = new Memcached();
      }
      // Start request handlers
      startRequestHandlers();

      selector = Selector.open(); // selector is open here
      log.debug("Selector started");

      // ServerSocketChannel: selectable channel for stream-oriented listening sockets
      serverSocket = ServerSocketChannel.open();
      log.debug("Server socket opened");

      InetSocketAddress serverAddr = new InetSocketAddress(host, port);

      // Binds the channel's socket to a local address and configures the socket to listen for
      // connections
      serverSocket.bind(serverAddr, Integer.MAX_VALUE);
      serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      // Adjusts this channel's blocking mode.
      serverSocket.configureBlocking(false);
      int ops = serverSocket.validOps();
      serverSocket.register(selector, ops, null);

      log.info("Memcarrot Server started on: {}. Ready to accept new connections.", serverAddr);

      this.started = true;

      Consumer<SelectionKey> action = key -> {
        try {
          if (!key.isValid()) {
            return;
          }
          if (key.isValid() && key.isAcceptable()) {
            accept(key);
          } else if (key.isValid() && key.isReadable()) {
            read(key);
          }
        } catch (IOException e) {
          log.error(e.getMessage());
          try {
            key.cancel();
            key.channel().close();
          } catch (IOException ee) {
            // FIXME: is this correct?
            log.error("Error during select: ", e);
          }
        } catch (CancelledKeyException eee) {
          // swallow
        }
      };
      // Infinite loop..
      // Keep server running
      while (true) {
        // Selects a set of keys whose corresponding channels are ready for I/O operations
        selector.select(action);
      }
    } catch (Throwable t) {
      if (!(t instanceof ClosedSelectorException)) {
        log.fatal("FATAL", t);
        this.failed = true;
      }
    }
  }

  private void accept(SelectionKey key) throws IOException {
    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    SocketChannel client = serverSocketChannel.accept();
    client.configureBlocking(false);
    client.setOption(StandardSocketOptions.TCP_NODELAY, true);
    client.setOption(StandardSocketOptions.SO_SNDBUF, this.tcpSndRcvBufferSize);
    client.setOption(StandardSocketOptions.SO_RCVBUF, this.tcpSndRcvBufferSize);
    client.register(selector, SelectionKey.OP_READ);
    log.debug("Connection Accepted: remote={}]", client.getRemoteAddress());
  }

  private void read(SelectionKey key) {
    try {
      RequestHandlers.Attachment att = (RequestHandlers.Attachment) key.attachment();
      if (att != null && att.inUse()) return;
      service.submit(key);
    } catch (CancelledKeyException e) {
      log.warn("CancelledKeyException: ", e);
    }
  }

  private void startRequestHandlers() throws IOException {
    MemcarrotConf conf = MemcarrotConf.getConf();
    int numThreads = conf.getThreadPoolSize();
    service = RequestHandlers.create(memcached, numThreads, bufferSize);
    service.start();
  }

}
