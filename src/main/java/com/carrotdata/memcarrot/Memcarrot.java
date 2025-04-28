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
      System.setProperty(MEMCARROT_VERSION, version);
    } else if (System.getProperty(MEMCARROT_VERSION) == null) {
      log.warn("Cannot find version information in manifest file or system property");
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
      log.error("Get manifest attribute: ", e);
    }
    return null;
  }

  private static void usage() {
    log.info("Usage: ./bin/memcarrot.sh [config_file_path] [start|stop]");
    System.exit(-1);
  }

}
