/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrotdata.memcarrot.util.HashUtil;

public class AuthorizationHandler {
  
  private static final Logger log = LogManager.getLogger(AuthorizationHandler.class);


  ConcurrentHashMap<SocketChannel, SocketChannel> pendingConns = new ConcurrentHashMap<>();

  ConcurrentHashMap<String, String> authHosts = new ConcurrentHashMap<String, String>();

  AuthorizationHandler() {
  }

  public void addPendingConnection(SocketChannel channel) {
    this.pendingConns.put(channel,  channel);
  }
  
  /**
   * @param channel
   * @return
   */
  public boolean authorizeConnection(SocketChannel channel) {
    
    if (!pendingConns.containsKey(channel)) {
      return false;// 
    }
    String remoteIp;
    String username;
    String password;
    
    try {
      // Step 1: Check if connection is local
      InetSocketAddress remoteAddr = (InetSocketAddress) channel.getRemoteAddress();
      boolean localAddress = remoteAddr.getAddress().isLoopbackAddress();
      log.debug("remote={} local={}", remoteAddr, localAddress); 
      if (localAddress) {
        // Step 2: Read PROXY header and extract remote IP
        String proxyLine = readLine(channel);
        log.debug("proxyLine={}", proxyLine);
        
        if (proxyLine == null || !proxyLine.startsWith("PROXY ")) {
          sendErrorAndClose(channel, "CLIENT_ERROR Authorization required\r\n");
          return true;
        }
        String[] proxyTokens = proxyLine.split(" ");
        if (proxyTokens.length < 6) {
          sendErrorAndClose(channel, "CLIENT_ERROR Authorization required\r\n");
          return true;
        }
        remoteIp = proxyTokens[2];
      } else {
        remoteIp = remoteAddr.getHostString();
      }
      
      if (authHosts.containsKey(remoteIp)) {
        // Authorized host
        return false;
      }
      // Step 3: Read Memcached SET command
      String commandLine = readLine(channel);
      if (commandLine == null || !commandLine.startsWith("set ")) {
        sendErrorAndClose(channel, "CLIENT_ERROR Authorization required\r\n");
        return true;
      }
      
      log.debug("cmd={}", commandLine);
      String[] cmdTokens = commandLine.split(" ");
      if (cmdTokens.length < 5) {
        sendErrorAndClose(channel, "CLIENT_ERROR Authorization required\r\n");
        return true;
      }

      username = cmdTokens[1];
      boolean noreply = cmdTokens.length == 6 && cmdTokens[5].equals("noreply");
      password = readLine(channel);
      if (password == null) {
        sendErrorAndClose(channel, "CLIENT_ERROR Authorization required\r\n");
        return true;
      }
      
      String hash = HashUtil.hashString(password);
      String loginName = MemcarrotConf.getConf().getUserLoginName();
      String hashedPassword = MemcarrotConf.getConf().getUserPasswordSHA256();
      
      if (loginName == null || hashedPassword == null) {
        sendErrorAndClose(channel, "SERVER_ERROR Authorization not configured\r\n");
        return true;
      }
      
      // (Optional) Here you'd typically verify username/password...
      if (username.equals(loginName) && hash.equals(hashedPassword)) {
        // Step 5: Send success response
        authHosts.put(remoteIp,  remoteIp);
        if (!noreply) {
          writeString(channel, "STORED\r\n");
        }
      } else {
        sendErrorAndClose(channel, "CLIENT_ERROR Authorization failed\r\n");
      }

      // For demonstration purposes, print extracted data
      log.debug("Authorized connection from IP: {}", remoteIp);
      log.debug("Username: {}", username);
      log.debug("Password: {}", password);

    } catch (IOException e) {
      sendErrorAndClose(channel, "CLIENT_ERROR Authorization required\r\n");
    } finally {
      pendingConns.remove(channel);
    }
    return true;
  }

  // Helper method to read a line from a non-blocking channel
  private String readLine(SocketChannel channel) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(1);
    StringBuilder line = new StringBuilder();
    boolean gotCR = false;

    long timeout = System.currentTimeMillis() + 500; // 500ms timeout

    while (System.currentTimeMillis() < timeout) {
      int bytesRead = channel.read(buffer);
      if (bytesRead > 0) {
        buffer.flip();
        char ch = (char) buffer.get();
        buffer.clear();

        if (ch == '\r') {
          gotCR = true;
        } else if (gotCR && ch == '\n') {
          return line.toString();
        } else {
          line.append(ch);
        }
      } else if (bytesRead == -1) {
        /*DEBUG*/ log.info("End-Of-Stream");
        return null; // End-of-stream
      } else {
        sleepBriefly();
      }
    }
    throw new IOException("Timeout while reading line");
  }

  // Helper to write a string fully
  private void writeString(SocketChannel channel, String data) throws IOException {
    ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }

  // Helper method to send error message and close connection
  private void sendErrorAndClose(SocketChannel channel, String errorMsg) {
    try {
      writeString(channel, errorMsg);
      channel.close();
    } catch (IOException ignored) {
    }
  }

  // Helper to sleep briefly for non-blocking reads
  private void sleepBriefly() {
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
    }
  }
}
