/*
 * Copyright (C) 2023-present Onecache, Inc. <p>This program is free software: you can redistribute
 * it and/or modify it under the terms of the Server Side Public License, version 1, as published by
 * MongoDB, Inc. <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the Server Side Public License for more details. <p>You should have received a copy
 * of the Server Side Public License along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package com.carrotdata.memcarrot;

import java.io.IOException;
import java.util.Properties;

import com.carrotdata.cache.util.CacheConfig;

/** Class which keeps all the configuration parameters for Memcarrot server */
public class MemcarrotConf {

  public static final String CONF_SERVER_PORT = "memcarrot.server-port";
  public static final String CONF_SERVER_ADDRESS = "memcarrot.server-address";
  public static final String CONF_THREAD_POOL_SIZE = "memcarrot.workers-pool-size";
  public static final String CONF_IO_BUFFER_SIZE = "memcarrot.io-buffer-size";

  public static final int DEFAULT_SERVER_PORT = 11211;
  public static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
  public static final int DEFAULT_THREAD_POOL_SIZE =
      Math.max(1, Runtime.getRuntime().availableProcessors() / 4);

  public static final int DEFAULT_IO_BUFFER_SIZE = 1024 * 1204;

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
    Properties props = this.cacheConfig.getProperties();
    String sport =
        (String) props.getOrDefault(CONF_SERVER_PORT, Integer.toString(DEFAULT_SERVER_PORT));
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
    Properties props = this.cacheConfig.getProperties();
    String address = (String) props.getOrDefault(CONF_SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS);
    return address;
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
    Properties props = this.cacheConfig.getProperties();
    String sport = (String) props.getOrDefault(CONF_THREAD_POOL_SIZE,
      Integer.toString(DEFAULT_THREAD_POOL_SIZE));
    return Integer.parseInt(sport);
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
   * I/O buffer size
   * @return buffer size
   */
  public int getIOBufferSize() {
    Properties props = this.cacheConfig.getProperties();
    String ssize =
        (String) props.getOrDefault(CONF_IO_BUFFER_SIZE, Integer.toString(DEFAULT_IO_BUFFER_SIZE));
    return Integer.parseInt(ssize);
  }

  /**
   * I/O buffer size
   * @param buffer size
   */
  public void setIOBufferSize(int size) {
    Properties props = this.cacheConfig.getProperties();
    props.setProperty(CONF_IO_BUFFER_SIZE, Integer.toString(size));
  }

  /**
   * Get server node (address:port)
   * @return address:port
   */
  public String getNode() {
    return getServerAddress() + ":" + getServerPort();
  }
}
