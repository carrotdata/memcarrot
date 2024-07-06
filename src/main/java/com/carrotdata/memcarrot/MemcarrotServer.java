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
import java.util.Iterator;
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
   * Constructor
   * @param host
   * @param port
   * @throws IOException
   */
  public MemcarrotServer(String host, int port) throws IOException {
    this.port = port;
    this.host = host;
    this.bufferSize = MemcarrotConf.getConf().getIOBufferSize();
  }

  public MemcarrotServer() throws IOException {
    MemcarrotConf config = MemcarrotConf.getConf();
    this.port = config.getServerPort();
    this.host = config.getServerAddress();
    this.bufferSize = config.getIOBufferSize();
  }

  public MemcarrotServer(MemcarrotConf config) throws IOException {
    this.port = config.getServerPort();
    this.host = config.getServerAddress();
    this.bufferSize = config.getIOBufferSize();
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
        Thread.sleep(1);
      } catch (InterruptedException e) {

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

  // private void run() throws IOException {
  // // Create memcached support instance if not null
  // // It is not null in tests
  // if (memcached == null) {
  // memcached = new Memcached();
  // }
  // // Start request handlers
  // startRequestHandlers();
  //
  // selector = Selector.open(); // selector is open here
  // log.debug("Selector started");
  //
  // // ServerSocketChannel: selectable channel for stream-oriented listening sockets
  // serverSocket = ServerSocketChannel.open();
  // log.debug("Server socket opened");
  //
  // InetSocketAddress serverAddr = new InetSocketAddress(host, port);
  //
  // // Binds the channel's socket to a local address and configures the socket to listen for
  // // connections
  // serverSocket.bind(serverAddr);
  // // Adjusts this channel's blocking mode.
  // serverSocket.configureBlocking(false);
  // int ops = serverSocket.validOps();
  // serverSocket.register(selector, ops, null);
  //
  // log.info("Memcarrot Server started on: {}. Ready to accept new connections.", serverAddr);
  //
  // this.started = true;
  //
  // Consumer<SelectionKey> action = key -> {
  // try {
  // if (!key.isValid()) return;
  // if (key.isValid() && key.isAcceptable()) {
  // SocketChannel client = serverSocket.accept();
  // // Adjusts this channel's blocking mode to false
  // client.configureBlocking(false);
  // client.setOption(StandardSocketOptions.TCP_NODELAY, true);
  // client.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
  // client.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
  // //client.setOption(StandardSocketOptions.SO_REUSEADDR, true);
  // // Operation-set bit for read operations
  // client.register(selector, SelectionKey.OP_READ);
  // log.debug("[{}] Connection Accepted: remote={}]", Thread.currentThread().getName(),
  // client.getRemoteAddress());
  // } else if (key.isValid() && key.isReadable()) {
  // // Check if it is in use
  // RequestHandlers.Attachment att = (RequestHandlers.Attachment) key.attachment();
  // if (att != null && att.inUse()) return;
  // // process request
  // service.submit(key);
  // }
  // } catch (IOException e) {
  // log.error(e.getMessage());
  // try {
  // key.channel().close();
  // } catch (IOException ee) {
  // //FIXME: is this correct?
  // }
  // //key.cancel();
  // } catch (CancelledKeyException eee) {
  // // swallow
  // }
  // };
  // // Infinite loop..
  // // Keep server running
  // while (true) {
  // // Selects a set of keys whose corresponding channels are ready for I/O operations
  // selector.select(action);
  // }
  // }

  private void run() throws IOException {
    if (memcached == null) {
      memcached = new Memcached();
    }
    startRequestHandlers();

    selector = Selector.open();
    log.debug("Selector started");

    serverSocket = ServerSocketChannel.open();
    log.debug("Server socket opened");

    InetSocketAddress serverAddr = new InetSocketAddress(host, port);

    serverSocket.bind(serverAddr);
    serverSocket.configureBlocking(false);
    serverSocket.register(selector, SelectionKey.OP_ACCEPT);

    log.info("Memcarrot Server started on: {}. Ready to accept new connections.", serverAddr);

    this.started = true;

    while (true) {
      try {
        // Wait for an event
        selector.select();

        // Iterate over the set of selected keys
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
          SelectionKey key = keys.next();
          keys.remove();

          if (!key.isValid()) continue;

          if (key.isAcceptable()) {
            accept(key);
          } else if (key.isReadable()) {
            read(key);
          }
        }
      } catch (IOException e) {
        log.error("Error during select: ", e);
      } catch (CancelledKeyException e) {
        log.warn("CancelledKeyException: ", e);
      }
    }
  }

  private void accept(SelectionKey key) throws IOException {
    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    SocketChannel client = serverSocketChannel.accept();
    client.configureBlocking(false);
    client.setOption(StandardSocketOptions.TCP_NODELAY, true);
    client.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
    client.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
    client.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    client.register(selector, SelectionKey.OP_READ);
    log.debug("[{}] Connection Accepted: remote={}]", Thread.currentThread().getName(),
      client.getRemoteAddress());
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
