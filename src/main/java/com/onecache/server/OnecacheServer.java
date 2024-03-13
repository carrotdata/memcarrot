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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.onecache.core.support.Memcached;

/** Carrot node server (single thread) */
public class OnecacheServer {
  private static final Logger log = LogManager.getLogger(OnecacheServer.class);

  /** 
   * Executor service (request handlers)
   **/
  static RequestHandlers service;

  /**
   * Host name
   */
  private String host;
  /**
   * Host port
   */
  private int port;
  /**
   * Memcached support
   */
  private Memcached memcached;
  
  /**
   * Buffer size
   */
  private int bufferSize;
  /**
   * Constructor
   * @param host
   * @param port
   * @throws IOException 
   */
  public OnecacheServer(String host, int port) throws IOException {
    this.port = port;
    this.host = host;
    this.bufferSize = OnecacheConf.getConf().getIOBufferSize();
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

  private void startRequestHandlers() throws IOException {
    OnecacheConf conf = OnecacheConf.getConf();
    int numThreads = conf.getThreadPoolSize();
    service = RequestHandlers.create(memcached, numThreads, bufferSize);
    service.start();
  }
}
