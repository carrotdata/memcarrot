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
import java.util.Properties;

import com.carrotdata.cache.util.CacheConfig;

/** Class which keeps all the configuration parameters for Memcarrot server */
public class MemcarrotConf {

  public static final String MEMCARROT_VERSION = "MEMCARROT_VERSION";
  public static final String CONF_SERVER_PORT = "server.port";
  public static final String CONF_SERVER_ADDRESS = "server.address";
  public static final String CONF_THREAD_POOL_SIZE = "workers.pool.size";
  public static final String CONF_KV_SIZE_MAX = "kv.size.max";
  public static final String CONF_SND_RCV_BUFFER_SIZE = "tcp.buffer.size";
  public static final String CONF_USER_NAME = "user.name";
  public static final String CONF_USER_PASSWORD_SHA256 = "user.password.sha256";


  public static final int DEFAULT_SERVER_PORT = 11211;
  public static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
  public static final int DEFAULT_THREAD_POOL_SIZE =
      Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

  public static final int DEFAULT_KV_SIZE_MAX = 4 * 1024 * 1024;
  public static final int DEFAULT_SNDRCV_BUFFER_SIZE = 32 * 1024;

  private static MemcarrotConf conf;
  private CacheConfig cacheConfig;

  public static MemcarrotConf getConf() throws IOException {
    return getConf(null);
  }

  public static MemcarrotConf getConf(String file) throws IOException {
    if (conf != null) {
      return conf;
    }
    synchronized (MemcarrotConf.class) {
      CacheConfig config = file != null ? CacheConfig.getInstance(file) : CacheConfig.getInstance();
      conf = new MemcarrotConf(config);
      return conf;
    }
  }

  /** For testing */
  public MemcarrotConf(CacheConfig conf) {
    this.cacheConfig = conf;
  }

  /** Default constructor */
  public MemcarrotConf() {
    this.cacheConfig = CacheConfig.getInstance();
  }

  /**
   * Get cache configuration
   * @return cache configuration
   */
  public CacheConfig getCacheConfig() {
    return this.cacheConfig;
  }

  /**
   * Sets cache configuration
   * @param conf
   */
  public void setCacheConfig(CacheConfig conf) {
    this.cacheConfig = conf;
  }

  /**
   * Get server's port
   * @return port
   */
  public int getServerPort() {
    String sport = System.getenv(CONF_SERVER_PORT);
    if (sport == null) {
      Properties props = this.cacheConfig.getProperties();
      sport =
        (String) props.getOrDefault(CONF_SERVER_PORT, Integer.toString(DEFAULT_SERVER_PORT));
    }
    return Integer.parseInt(sport);
  }

  /**
   * Sets server port
   * @param port
   */
  public void setServerPort(int port) {
    Properties props = this.cacheConfig.getProperties();
    props.setProperty(CONF_SERVER_PORT, Integer.toString(port));
  }

  /**
   * Get server address
   * @return address
   */
  public String getServerAddress() {
    String saddress = System.getenv(CONF_SERVER_ADDRESS);
    if (saddress == null) {
      Properties props = this.cacheConfig.getProperties();
      saddress = (String) props.getOrDefault(CONF_SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS);
    }
    return saddress;
  }

  /**
   * Sets TCP send/receive buffer sizes
   * @param size
   */
  public void setSndRcvBufferSize(int size) {
    Properties props = this.cacheConfig.getProperties();
    props.setProperty(CONF_SND_RCV_BUFFER_SIZE, Integer.toString(size));
  }

  /**
   * Get TCP send-receive buffer size
   * @return size
   */
  public int getSndRcvBufferSize() {
    String ssize = System.getenv(CONF_SND_RCV_BUFFER_SIZE);
    if (ssize == null) {
      Properties props = this.cacheConfig.getProperties();
      ssize = (String) props.getOrDefault(CONF_SND_RCV_BUFFER_SIZE,
        Integer.toString(DEFAULT_SNDRCV_BUFFER_SIZE));
    }
    return Integer.parseInt(ssize);
  }
  /**
   * Set server address
   * @param address
   */
  public void setServerAddress(String address) {
    Properties props = this.cacheConfig.getProperties();
    props.setProperty(CONF_SERVER_ADDRESS, address);
  }

  /**
   * Thread pool size
   * @return pool size
   */
  public int getThreadPoolSize() {
    String spool = System.getenv(CONF_THREAD_POOL_SIZE);
    if (spool == null) {
      Properties props = this.cacheConfig.getProperties();
      spool = (String) props.getOrDefault(CONF_THREAD_POOL_SIZE,
      Integer.toString(DEFAULT_THREAD_POOL_SIZE));
    }
    return Integer.parseInt(spool);
  }

  /**
   * Sets thread pool size
   * @param pool size
   */
  public void setThreadPoolSize(int size) {
    Properties props = this.cacheConfig.getProperties();
    props.setProperty(CONF_THREAD_POOL_SIZE, Integer.toString(size));
  }

  /**
   * Key-Value maximum size
   * @return max size
   */
  public int getKeyValueMaxSize() {
    String ssize = System.getenv(CONF_KV_SIZE_MAX);
    if (ssize == null) {
      Properties props = this.cacheConfig.getProperties();
      ssize =
        (String) props.getOrDefault(CONF_KV_SIZE_MAX, Integer.toString(DEFAULT_KV_SIZE_MAX));
    }
    return Integer.parseInt(ssize);
  }

  /**
   * Key-Value maximum size
   * @param max size
   */
  public void setKeyValueMaxSize(int size) {
    Properties props = this.cacheConfig.getProperties();
    props.setProperty(CONF_KV_SIZE_MAX, Integer.toString(size));
  }

  /**
   * Get server node (address:port)
   * @return address:port
   */
  public String getNode() {
    return getServerAddress() + ":" + getServerPort();
  }
  
  public String getUserLoginName() {
    String name = System.getenv(CONF_USER_NAME);
    if (name == null) {
      Properties props = this.cacheConfig.getProperties();
      name = (String) props.getOrDefault(CONF_USER_NAME, "");
    }
    return name;
  }
  
  public String getUserPasswordSHA256() {
    String pass = System.getenv(CONF_USER_PASSWORD_SHA256);
    if (pass == null) {
      Properties props = this.cacheConfig.getProperties();
      pass = (String) props.getOrDefault(CONF_USER_PASSWORD_SHA256, "");
    }
    return pass;
  }
  
}
