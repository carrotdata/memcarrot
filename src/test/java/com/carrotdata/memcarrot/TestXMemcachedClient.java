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

import static com.carrotdata.memcarrot.MemcarrotConf.MEMCARROT_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carrotdata.cache.Cache;
import com.carrotdata.cache.support.Memcached;
import com.carrotdata.memcarrot.util.TestUtils;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

public class TestXMemcachedClient {
  private static Logger logger = LogManager.getLogger(TestXMemcachedClient.class);

  MemcarrotServer server;
  XMemcachedClient client;
  String version = "1.0";
  int n = 50_000;
  boolean localRun = true;

  
  @Before
  public void setUp() throws IOException {
    
    System.setProperty(MemcarrotConf.CONF_KV_SIZE_MAX, "262144");
    String host = null;
    int port = 0;
    
    host = System.getProperty("host");
    String ps = System.getProperty("port");
    if (ps != null) {
      port = Integer.parseInt(ps);
    }
    if (host == null) {
      Cache c = TestUtils.createCache(800_000_000, 4_000_000, true, true);
      Memcached m = new Memcached(c);
      server = new MemcarrotServer();
      server.setMemachedSupport(m);
      server.start();
      host = server.getHost();
      port = server.getPort();
      System.setProperty(MEMCARROT_VERSION, version);
    } else {
      localRun = false;
    }
    logger.info("kv-max-size={}", MemcarrotConf.getConf().getKeyValueMaxSize());
    client = new XMemcachedClient(host, port);

  }

  @After
  public void tearDown() throws IOException {
    client.shutdown();
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testSet()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);

    Object res = client.get(key);
    assertTrue(res instanceof String);
    assertEquals(value, (String) res);

    value += 1;
    client.setWithNoReply(key, expire, value);
    // Now get
    res = client.get(key);

    assertTrue(res instanceof String);
    assertEquals(value, (String) res);

  }

  @Test
  public void testVersion()
      throws TimeoutException, InterruptedException, MemcachedException {
    
    if (!localRun) return;
    Map<InetSocketAddress, String> result = client.getVersions();
    assertTrue(result.size() == 1);
    String v = result.values().iterator().next();
    assertEquals(v, version);

  }
  
  @Test
  public void testStats()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    
    if (!localRun) return;
    Map<InetSocketAddress, Map<String,String>> result = client.getStats();
    assertTrue(result.size() == 1);
    Map<String, String> stats = result.values().iterator().next();
    for (Map.Entry<String, String> entry: stats.entrySet()) {
      logger.info(entry.getKey() + " " + entry.getValue());
    }

  }
  
  @Test 
  public void testGet()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);

    Object res = client.get(key);
    assertTrue(res instanceof String);
    assertEquals(value, (String) res);
    // Get non-existent key
    res = client.get(key + 1);
    assertNull(res);

  }

  @Test
  public void testGets()
      throws TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);

    GetsResponse<?> res = client.gets(key);
    assertTrue(res.getValue() instanceof String);
    assertEquals(value, (String) res.getValue());
    long cas1 = res.getCas();
    assertTrue(cas1 != 0);

    value += 1;
    client.setWithNoReply(key, expire, value);
    // Now get
    res = client.gets(key);

    assertTrue(res.getValue() instanceof String);
    assertEquals(value, (String) res.getValue());
    long cas2 = res.getCas();
    assertTrue(cas2 != 0);
    assertTrue(cas1 != cas2);
  }

  @Test
  public void testAdd()
      throws IOException, InterruptedException, MemcachedException, TimeoutException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean res = client.add(key, expire, value);
    assertTrue(res);

    res = client.add(key, expire, value);
    assertFalse(res);

    Object v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value, (String) v);

    value = value + 1;
    key = key + 1;
    client.addWithNoReply(key, expire, value);

    v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value, (String) v);
  }

  @Test
  public void testAppend()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean res = client.append(key, value);
    assertFalse(res);

    res = client.add(key, expire, value);
    assertTrue(res);

    Object v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value, (String) v);

    client.appendWithNoReply(key, value);

    v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value + value, (String) (v));
  }

  @Test
  public void testPrepend()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);

    int expire = 100;
    boolean res = client.prepend(key, value);
    assertFalse(res);

    res = client.add(key, expire, value);
    assertTrue(res);

    client.prependWithNoReply(key, value1);

    Object v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value1 + value, (String) (v));
  }

  @Test
  public void testReplace()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);

    int expire = 100;
    boolean res = client.replace(key, expire, value);
    assertFalse(res);

    res = client.add(key, expire, value);
    assertTrue(res);

    Object v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value, (String) v);

    client.replaceWithNoReply(key, expire, value1);

    v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value1, (String) v);
  }

  @Test
  public void testCAS()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;

    boolean code;
    try {
      code = client.cas(key, expire, value, 0);
    } catch (MemcachedException e) {

    }
    boolean res = client.add(key, expire, value);
    assertTrue(res);

    // Now get
    GetsResponse<?> result = client.gets(key);

    long cas = result.getCas();

    code = client.cas(key, expire, value + value, cas + 1);
    assertFalse(code);

    code = client.cas(key, expire, value + value, cas);
    assertTrue(code);

    Object v = client.get(key);
    assertTrue(v instanceof String);
    assertEquals(value + value, (String) v);

  }

  @Test
  public void testDelete()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);

    Object res = client.get(key);
    assertTrue(res instanceof String);
    assertEquals(value, (String) res);

    result = client.delete(key);
    assertTrue(result);
    res = client.get(key);
    assertNull(res);
  }

  @Test
  public void testTouch()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);

    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);

    Object res = client.get(key);
    assertTrue(res instanceof String);
    assertEquals(value, (String) res);

    result = client.touch(key, 2);
    assertTrue(result);

    res = client.get(key);
    assertTrue(res instanceof String);
    assertEquals(value, (String) res);

    Thread.sleep(2000);
    res = client.get(key);
    assertNull(res);
  }

  @Test
  public void testIncr()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key1 = TestUtils.randomString(20);
    String key2 = TestUtils.randomString(20);
    String num = "100";
    String some = "some";

    int expire = 100;
    boolean result = client.set(key1, expire, num);
    assertTrue(result);
    result = client.set(key2, expire, some);
    assertTrue(result);

    Object res = client.get(key1);
    assertTrue(res instanceof String);
    assertEquals(num, (String) res);

    res = client.get(key2);
    assertTrue(res instanceof String);
    assertEquals(some, (String) res);

    long value = client.incr(key1, 200);
    assertEquals(300L, value);

    // negative
    try {
      value = client.incr(key1, -10);
      fail();
    } catch (MemcachedException e) {

    }
    // non - numeric value
    try {
      value = client.incr(key2, 10);
      fail();
    } catch (MemcachedException e) {

    }

    // non - existent key
    try {
      value = client.incr(key2 + 1, 10);
      fail();
    } catch (MemcachedException e) {

    }
  }

  @Test
  public void testDecr()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key1 = TestUtils.randomString(20);
    String key2 = TestUtils.randomString(20);
    String num = "100";
    String some = "some";

    int expire = 100;
    boolean result = client.set(key1, expire, num);
    assertTrue(result);
    result = client.set(key2, expire, some);
    assertTrue(result);

    Object res = client.get(key1);
    assertTrue(res instanceof String);
    assertEquals(num, (String) res);

    res = client.get(key2);
    assertTrue(res instanceof String);
    assertEquals(some, (String) res);

    long value = client.decr(key1, 20);
    assertEquals(80L, value);

    value = client.decr(key1, 200);
    assertEquals(0L, value);
    // negative
    try {
      value = client.decr(key1, -10);
      fail();
    } catch (MemcachedException e) {

    }
    // non - numeric value
    try {
      value = client.decr(key2, 10);
      fail();
    } catch (MemcachedException e) {

    }

    // non - existent key
    try {
      value = client.decr(key2 + 1, 10);
      fail();
    } catch (MemcachedException e) {

    }
  }

  @Test
  public void testSetMulti()
      throws IOException, InterruptedException, MemcachedException, TimeoutException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.set((key + i), expire, value);
        assertTrue(res);
      } else {
        client.setWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    Thread.sleep(500);
    end = System.currentTimeMillis();

    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(value.equals(map.get(x))));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    delete(n);
  }

  private List<String> getBatch(int batchNum, int batchSize) {
    List<String> batch = new ArrayList<String>();
    for (int i = 0; i < batchSize; i++) {
      batch.add("KEY:" + (batchNum * batchSize + i));
    }
    return batch;
  }

  @Test
  public void testAddMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();

    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(value.equals(map.get(x))));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    delete(n);
  }

  @Test
  public void testAppendMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      if (i % 100 == 0) {
        boolean res = client.append((key + i), value1);
        assertTrue(res);
      } else {
        client.appendWithNoReply((key + i), value1);
      }
    }
    start = System.currentTimeMillis();
    logger.info("APPEND Time={}ms", start - end);
    
    Thread.sleep(500);
    start = System.currentTimeMillis();
    int batchSize = 100;
    String newValue = value + value1;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(newValue.equals(map.get(x))));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);
    delete(n);
  }

  @Test
  public void testPrependMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      if (i % 100 == 0) {
        boolean res = client.prepend((key + i), value1);
        assertTrue(res);
      } else {
        client.prependWithNoReply((key + i), value1);
      }
    }
    start = System.currentTimeMillis();
    logger.info("PREPEND Time={}ms", start - end);

    Thread.sleep(500);
    start = System.currentTimeMillis();
    
    int batchSize = 100;
    String newValue = value1 + value;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(newValue.equals(map.get(x))));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);
    
    delete(n);
  }

  @Test
  public void testReplaceMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 20;

      if (i % 100 == 0) {
        boolean res = client.replace((key + i), expire, value1);
        assertTrue(res);
      } else {
        client.replaceWithNoReply((key + i), expire, value1);
      }
    }
    start = System.currentTimeMillis();
    logger.info("REPLACE Time={}ms", start - end);
    
    Thread.sleep(500);

    start = System.currentTimeMillis();
    int batchSize = 100;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(value1.equals(map.get(x))));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);
    
    delete(n);

  }

  @Test
  public void testTouchMulti()
      throws IOException, InterruptedException, TimeoutException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 10;
      boolean res = client.touch((key + i), expire);
      assertTrue(res);
    }
    start = System.currentTimeMillis();
    logger.info("TOUCH Time={}ms", start - end);
    int batchSize = 100;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(value.equals(map.get(x))));
    }
    end = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, end - start);

    Thread.sleep(10000);
    start = System.currentTimeMillis();
    // Must all expired
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map == null || map.size() == 0);
    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);
    
    delete(n);
  }

  @Test
  public void testDeleteMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);

    Thread.sleep(500);
    end = System.currentTimeMillis();
    int batchSize = 100;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach(x -> assertTrue(value.equals(map.get(x))));
    }
    start = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, start - end);

    for (int i = 0; i < n; i++) {
      if (i % 100 == 0) {
        boolean res = client.delete((key + i));
        assertTrue(res);
      } else {
        client.deleteWithNoReply((key + i));
      }
    }
    end = System.currentTimeMillis();
    logger.info("DELETE Time={}ms", end - start);

    Thread.sleep(500);

    start = System.currentTimeMillis();
    // Must all be deleted
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map == null || map.size() == 0);
    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);

  }
  
  @Test
  public void testFlushAll()
      throws IOException, InterruptedException, MemcachedException, TimeoutException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    
    for (int i = 0; i < 100; i++) {
      int expire = 100;
      boolean res = client.set((key + i), expire, value);
      assertTrue(res);
    }

    client.flushAll();
    
    for (int i = 0; i < 100; i++) {  
      Map<String, Object> map = client.get(key + i);
      assertNull(map);
    }

    for (int i = 0; i < 100; i++) {
      int expire = 100;
      boolean res = client.set((key + i), expire, value);
      assertTrue(res);
    }

    client.flushAll(5);
    Thread.sleep(6000);

    for (int i = 0; i < 100; i++) {  
      Map<String, Object> map = client.get(key + i);
      assertNull(map);
    }

    for (int i = 0; i < 100; i++) {
      int expire = 100;
      boolean res = client.set((key + i), expire, value);
      assertTrue(res);
    }

    client.flushAllWithNoReply();

    for (int i = 0; i < 100; i++) {  
      Map<String, Object> map = client.get(key + i);
      assertNull(map);
    }
    
    for (int i = 0; i < 100; i++) {
      int expire = 100;
      boolean res = client.set((key + i), expire, value);
      assertTrue(res);
    }

    client.flushAllWithNoReply(5);
    Thread.sleep(6000);

    for (int i = 0; i < 100; i++) {  
      Map<String, Object> map = client.get(key + i);
      assertNull(map);
    }
  }
  
  
  private void delete(int n) throws TimeoutException, InterruptedException, MemcachedException {
    
    String key = "KEY:";
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      if (i % 100 == 0) {
        boolean res = client.delete((key + i));
      } else {
        client.deleteWithNoReply((key + i));
      }
    }
    long end = System.currentTimeMillis();
    logger.info("DELETE Time={}ms", end - start);
    Thread.sleep(500);

  }

  @Test
  public void testIncrMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = "0";
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();

    for (int i = 0; i < n; i++) {
      if (i % 100 == 0) {
        long v = client.incr((key + i), 10);
        assertEquals(10L, v);
      } else {
        client.incrWithNoReply((key + i), 10);
      }
    }
    start = System.currentTimeMillis();
    logger.info("INCR Time={}ms", start - end);
    Thread.sleep(500);
    start = System.currentTimeMillis();

    for (int i = 0; i < n; i++) {
      long v = client.incr((key + i), 10);
      assertEquals(20L, v);
    }
    end = System.currentTimeMillis();
    logger.info("INCR Time={}ms", end - start);
    
    delete(n);
  }

  @Test
  public void testDecrMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = "100";
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);

    Thread.sleep(500);

    end = System.currentTimeMillis();

    for (int i = 0; i < n; i++) {
      if (i % 100 == 0) {
        long v = client.decr((key + i), 10);
        assertEquals(90L, v);
      } else {
        client.decrWithNoReply((key + i), 10);
      }
    }
    start = System.currentTimeMillis();
    logger.info("DECR Time={}ms", start - end);
    Thread.sleep(500);
    start = System.currentTimeMillis();

    for (int i = 0; i < n; i++) {
      long v = client.decr((key + i), 100);
      assertEquals(0L, v);
    }
    end = System.currentTimeMillis();
    logger.info("DECR Time={}ms", end - start);
    
    delete(n);
  }

  
  // There issues with CAS noreply in xmemcached library
  @Test
  public void testCASMulti() throws TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value + i);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value + i);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);

    Thread.sleep(500);
    end = System.currentTimeMillis();

    List<GetsResponse<String>> results = new ArrayList<GetsResponse<String>>();
    for (int i = 0; i < n; i++) {
      GetsResponse<String> res = client.gets(key + i);
      assertEquals(value + i, (String) res.getValue());
      results.add(res);
    }

    start = System.currentTimeMillis();
    logger.info("GET total={} time={}ms", n, start - end);

    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res =
            client.cas((key + i), expire, results.get(i).getValue() + "$", results.get(i).getCas());
        assertTrue(res);
      } else {
        client.casWithNoReply(key + i, new CASOperation<String>() {
          @Override
          public int getMaxTries() {
            return 2;
          }

          @Override
          public String getNewValue(long currentCAS, String currentValue) {
            return currentValue + "$";
          }
        });
      }
    }
    end = System.currentTimeMillis();
    logger.info("CAS Time={}ms", end - start);
    
    Thread.sleep(500);
    end = System.currentTimeMillis();

    for (int i = 0; i < n; i++) {
      Object res = client.get(key + i);
      assertTrue(res instanceof String);
      assertEquals(value + i + "$", (String) res);
    }
    start = System.currentTimeMillis();
    logger.info("GET total={} time={}ms", n, start - end);
    
    delete(n);
  }
  
  public static void main(String[] args)
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    TestXMemcachedClient client = new TestXMemcachedClient();

    client.setUp();
    client.testAddMulti();
    client.tearDown();
    client.setUp();
    client.testAppendMulti();
    client.tearDown();
    client.setUp();
    client.testCASMulti();
    client.tearDown();
    client.setUp();
    client.testDecrMulti();
    client.tearDown();
    client.setUp();
    client.testIncrMulti();
    client.tearDown();
    client.setUp();
    client.testDeleteMulti();
    client.tearDown();
    client.setUp();
    client.testPrependMulti();
    client.tearDown();
    client.setUp();
    client.testReplaceMulti();
    client.tearDown();
    client.setUp();
    client.testSetMulti();
    client.tearDown();
    client.setUp();
    client.testTouchMulti();
    client.tearDown();

  }
}
