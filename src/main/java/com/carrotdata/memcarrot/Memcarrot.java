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
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.carrotdata.memcarrot.MemcarrotConf.MEMCARROT_VERSION;

/** Main service launcher */
public class Memcarrot {

  private static final Logger log = LogManager.getLogger(Memcarrot.class);
  private static String version;
  private static String buildTime;

  public static void main(String[] args) throws IOException {
    version = getManifestAttribute("Implementation-Version");
    buildTime = getManifestAttribute("Build-Time");

    if (version != null) {
      System.setProperty(MEMCARROT_VERSION, "memcarrot@" + version);
    } else if (System.getProperty(MEMCARROT_VERSION) == null) {
      log.fatal("Cannot find version information in manifest file or system property");
      System.exit(-255);
    }

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
    log.info("Stop Memcarrot server. Version: {}, BuildTime: {}", version, buildTime);
    MemcarrotConf conf = MemcarrotConf.getConf(configFile);
    String node = conf.getNode();
    SimpleClient client = new SimpleClient(node);
    client.shutdown();
  }

  private static void startServer(String configFile) throws IOException {
    log.info("Start Memcarrot server. Version: {}, BuildTime: {}", version, buildTime);
    MemcarrotConf conf = MemcarrotConf.getConf(configFile);
    long start = System.currentTimeMillis();
    MemcarrotServer server = new MemcarrotServer(conf);
    server.start();
    long end = System.currentTimeMillis();
    log.info("Memcarrot started on {}:{} in {}ms", server.getHost(), server.getPort(), end - start);
  }

  private static String getManifestAttribute(String attributeName) {
    try (InputStream manifestStream = Memcarrot.class.getClassLoader()
        .getResourceAsStream("META-INF/MANIFEST.MF")) {
      if (manifestStream != null) {
        Manifest manifest = new Manifest(manifestStream);
        Attributes attributes = manifest.getMainAttributes();
        return attributes.getValue(attributeName);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static void usage() {
    log.info("Usage: ./bin/memcarrot.sh [config_file_path] [start|stop]");
    System.exit(-1);
  }

}
