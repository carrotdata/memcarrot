/*
 Copyright (C) 2021-present Onecache, Inc.

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/** Main service launcher */
public class OnecacheMain {

  private static final Logger log = LogManager.getLogger(OnecacheMain.class);
  
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      usage();
    }
    if (args[1].equals("stop")) {
      stopServer(args[0]);
    } else if (args[1].equals("start")) {
      startServer(args[0]);
    } else {
      usage();
    }
  }

  private static void stopServer(String configFile) throws IOException {
    System.out.println("Stopping  Server");
    log("Stopping Onecache server ...");
    OnecacheConf conf = OnecacheConf.getConf(configFile);
    String node = conf.getNode();
    SimpleClient client = new SimpleClient(node);
    client.shutdown(true);
    // shutdown
    log("Shutdown finished.");
  }


  private static void startServer(String configFile) throws IOException {
    
    log("Starting Onecache server ...");
    OnecacheConf conf = OnecacheConf.getConf(configFile);
    String node = conf.getNode();
    String[] parts = node.split(":");
    String host = parts[0].trim();
    int port = Integer.parseInt(parts[1].trim());
    OnecacheServer server = new OnecacheServer(host, port);
    server.runNodeServer();;
    // shutdown
    log.info("[" + Thread.currentThread().getName() + "] " +"Shutdown finished.");
  }

  private static void usage() {
    log("Usage: java com.onecache.server.OnecacheMain config_file_path [start|stop]");
    System.exit(-1);
  }

  static void log(String str) {
    log.info("[{}] {}", Thread.currentThread().getName(), str);
  }
}
