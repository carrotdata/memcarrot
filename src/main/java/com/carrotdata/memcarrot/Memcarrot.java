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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Main service launcher */
public class Memcarrot {

  private static final Logger log = LogManager.getLogger(Memcarrot.class);

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
    MemcarrotConf conf = MemcarrotConf.getConf(configFile);
    String node = conf.getNode();
    SimpleClient client = new SimpleClient(node);
    client.shutdown();
  }

  private static void startServer(String configFile) throws IOException {
    log.info("Starting Memcarrot server");
    log.info("version=" + System.getProperty("MEMCARROT_VERSION"));
    log.info("build=" + configFile);

    //    TOOO conf/memcarrot-prod.yaml is hardcoded for demo purposes only
    MemcarrotConfYaml confYaml = MemcarrotConfYaml.loadConfigProfile("conf/memcarrot-prod.yaml");
    log.info("Configuration:\n{}", confYaml.toYaml());

    MemcarrotConf conf = MemcarrotConf.getConf(configFile);
    log.trace("Configuration:\n{}", conf);
    long start = System.currentTimeMillis();
    log.info("Starting Memcarrot server");
    MemcarrotServer server = new MemcarrotServer(conf);
    log.trace("Starting server");
    server.start();
    log.trace("Server started");
    long end = System.currentTimeMillis();
    log.info("Memcarrot started on {}:{} in {}ms", server.getHost(), server.getPort(), end - start);
  }

  private static void usage() {
    log.info("Usage: ./bin/memcarrot.sh [config_file_path] [start|stop]");
    System.exit(-1);
  }

}
