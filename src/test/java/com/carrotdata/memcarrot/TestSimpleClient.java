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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carrotdata.cache.Cache;
import com.carrotdata.cache.support.Memcached;
import com.carrotdata.memcarrot.SimpleClient.GetResult;
import com.carrotdata.memcarrot.SimpleClient.ResponseCode;
import com.carrotdata.memcarrot.util.TestUtils;

public class TestSimpleClient {
  private static Logger logger = LogManager.getLogger(TestSimpleClient.class);

  MemcarrotServer server;
  SimpleClient client;
  
  boolean localRun = true;
  
  static long startTime = System.nanoTime();
  
  @Before
  public void setUp() throws IOException {
    System.setProperty(MemcarrotConf.CONF_KV_SIZE_MAX, "262144");
    String host;
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
    } else {
      localRun = false;
    }
    logger.info("kv-max-size={}", MemcarrotConf.getConf().getKeyValueMaxSize());
    client = new SimpleClient(host, port);
  }

  @After
  public void tearDown() throws IOException {
    client.close();
    if (server != null) {
      server.stop();
    }
  }

  private String getIdString() {
    return startTime + "." + Thread.currentThread().getId() + "."; 
  }
  
  @Test
  public void testSetGet() throws IOException {
    logger.info("Running testSetGet");
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.set(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    noreply = true;
    code = client.set(bkey, bvalue, flags, expire, noreply);
    assertNull(code);
    // Now get
    List<GetResult> result = client.get(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    logger.info("Finished testSetGet");

  }

  @Test
  public void testInputTooLarge() throws IOException {
    logger.info("Running testInputTooLarge");

    MemcarrotConf conf = MemcarrotConf.getConf();
    int max_kv_size = conf.getKeyValueMaxSize();
    
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(max_kv_size);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.set(bkey, bvalue, flags, expire, noreply);
    if (localRun) {
      assertTrue(code == ResponseCode.CLIENT_ERROR);
    } else {
      assertTrue(code == ResponseCode.STORED);      
    }
    logger.info("Finished testInputTooLarge");

  }
  
  @Test
  public void testSetGets() throws IOException {
    logger.info("Running testSetGets");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.set(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    noreply = true;
    code = client.set(bkey, bvalue, flags, expire, noreply);
    assertNull(code);
    // Now get
    List<GetResult> result = client.gets(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    assertTrue(gr.cas.isPresent());
    assertTrue(gr.cas.getAsLong() != 0);
    logger.info("Finished testSetGets");

  }

  @Test
  public void testSetGats() throws IOException {
    logger.info("Running testSetGats");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    // Absolute expiration time
    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.set(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    // Relative expiration time
    List<GetResult> result = client.gats(10, new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    assertTrue(gr.cas.isPresent());
    assertTrue(gr.cas.getAsLong() != 0);
    // expire item, but returns current value
    result = client.gats(-1, new byte[][] { bkey });
    assertEquals(1, result.size());
    // Expired
    result = client.get(new byte[][] { bkey });
    assertEquals(0, result.size());
    logger.info("Finished testSetGats");

  }

  @Test
  public void testSetGat() throws IOException {
    logger.info("Running testSetGat");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    // Absolute expiration time
    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.set(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    // Relative expiration time
    List<GetResult> result = client.gat(10, new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    assertNull(gr.cas);
    // expire item, but returns current value
    result = client.gat(-1, new byte[][] { bkey });
    assertEquals(1, result.size());
    // Expired
    result = client.get(new byte[][] { bkey });
    assertEquals(0, result.size());
    logger.info("Finished testSetGat");

  }

  @Test
  public void testAddGet() throws IOException {
    logger.info("Running testAddGet");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.add(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    code = client.add(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.NOT_STORED);

    noreply = true;
    code = client.set(bkey, bvalue, flags, expire, noreply);
    assertNull(code);
    // Now get
    List<GetResult> result = client.get(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    logger.info("Finished testAddGet");

  }

  @Test
  public void testAppendGet() throws IOException {
    logger.info("Running testAppendGet");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);

    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();
    byte[] bvalue1 = value1.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.append(bkey, bvalue1, flags, expire, noreply);
    assertTrue(code == ResponseCode.NOT_STORED);

    noreply = true;
    code = client.set(bkey, bvalue, flags, expire, noreply);
    assertNull(code);

    flags += 1;
    code = client.append(bkey, bvalue1, flags, expire, noreply);
    assertTrue(code == null);
    // Now get
    List<GetResult> result = client.get(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals((value + value1).getBytes(), gr.value));
    assertEquals(flags, gr.flags);
    logger.info("Finished testAppendGet");

  }

  @Test
  public void testPrependGet() throws IOException {
    logger.info("Running testPrependGet");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);

    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();
    byte[] bvalue1 = value1.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.prepend(bkey, bvalue1, flags, expire, noreply);
    assertTrue(code == ResponseCode.NOT_STORED);

    noreply = true;
    code = client.set(bkey, bvalue, flags, expire, noreply);
    assertNull(code);

    flags += 1;
    code = client.prepend(bkey, bvalue1, flags, expire, noreply);
    assertTrue(code == null);
    // Now get
    List<GetResult> result = client.get(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals((value1 + value).getBytes(), gr.value));
    assertEquals(flags, gr.flags);
    logger.info("Finished testPrependGet");

  }

  @Test
  public void testReplaceGet() throws IOException {
    logger.info("Running testReplaceGet");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;
    ResponseCode code = client.replace(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.NOT_STORED);

    code = client.add(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    code = client.replace(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    noreply = true;
    code = client.set(bkey, bvalue, flags, expire, noreply);
    assertNull(code);
    // Now get
    List<GetResult> result = client.get(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    logger.info("Finished testReplaceGet");

  }

  @Test
  public void testCAS() throws IOException {
    logger.info("Running testCAS");

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    byte[] bkey = key.getBytes();
    byte[] bvalue = value.getBytes();

    int flags = 1;
    long expire = expireIn(100);
    boolean noreply = false;

    ResponseCode code = client.add(bkey, bvalue, flags, expire, noreply);
    assertTrue(code == ResponseCode.STORED);

    // Now get
    List<GetResult> result = client.gets(new byte[][] { bkey });
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    long cas = gr.cas.getAsLong();

    code = client.cas(bkey, bvalue, flags, expire, noreply, cas + 1);
    assertTrue(code == ResponseCode.EXISTS);

    code = client.cas(bkey, bkey, flags, expire, noreply, cas);
    assertTrue(code == ResponseCode.STORED);

    result = client.get(new byte[][] { bkey });
    assertEquals(1, result.size());
    gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bkey, gr.value));
    assertEquals(flags, gr.flags);
    logger.info("Finished testCAS");

  }

  @Test
  public void testSetGetMulti() throws IOException, InterruptedException {
    logger.info("Running testSetGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.set((key + i).getBytes(), bvalue, flags, expire, noreply);
      
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    int batchSize = 100;
    long getTime = 0;
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      long t1 = System.nanoTime();
      List<GetResult> list = client.get(keys);
      getTime += System.nanoTime() - t1;
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bvalue, x.value)));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms get_time={}", n, batchSize, getend - end,
      getTime / 1_000_000);
    
    deleteAll(n);
    logger.info("Finished testSetGetMulti");

  }

  @Test
  public void testSetGetMultiWithOverflow() throws IOException, InterruptedException {
    logger.info("Running testSetGetMultiWithOverflow");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.set((key + i).getBytes(), bvalue, flags, expire, noreply);
      
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    // Total response size > 1MB (5000 * 200 value size + some extra), 
    // I/O buffer size is 256kb
    // Must be OK, we handle such cases
    int batchSize = 5000;
    long getTime = 0;
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      long t1 = System.nanoTime();
      List<GetResult> list = client.get(keys);
      getTime += System.nanoTime() - t1;
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bvalue, x.value)));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms get_time={}", n, batchSize, getend - end,
      getTime / 1_000_000);
    
    deleteAll(n);
    logger.info("Finished testSetGetMultiWithOverflow");

  }
  
  @Test
  public void testAddGetMulti() throws IOException, InterruptedException {
    logger.info("Running testAddGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 0;
      long expire = expireIn(100);
      boolean noreply = true;//i % 100 == 0? false: true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      //assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    //Thread.sleep(5000);
    end = System.currentTimeMillis();
    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bvalue, x.value)));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    deleteAll(n);
    logger.info("Finished testAddGetMulti");

  }

  @Test
  public void testAppendGetMulti() throws IOException, InterruptedException {
    logger.info("Running testAppendGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    byte[] bvalue1 = value1.getBytes();
    byte[] bv = (value + value1).getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    
    logger.info("ADD Time={}ms", end - start);
    Thread.sleep(1000);
    
    end = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.append((key + i).getBytes(), bvalue1, flags, expire, noreply);
      assertTrue(code == null);
    }
    start = System.currentTimeMillis();
    logger.info("APPEND Time={}ms", start - end);

    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bv, x.value)));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);
    deleteAll(n);
    logger.info("Finished testAppendGetMulti");

  }

  @Test
  public void testPrependGetMulti() throws IOException, InterruptedException {
    logger.info("Running testPrependGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(20);
    String value1 = TestUtils.randomString(20);
    byte[] bvalue = value.getBytes();
    byte[] bvalue1 = value1.getBytes();
    byte[] bv = (value1 + value).getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.prepend((key + i).getBytes(), bvalue1, flags, expire, noreply);
      assertTrue(code == null);
    }
    start = System.currentTimeMillis();
    logger.info("PREPEND Time={}ms", start - end);

    Thread.sleep(1000);
    start = System.currentTimeMillis();
    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        assertTrue(TestUtils.equals(bv, list.get(j).value));
      }
    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - start);
    deleteAll(n);
    logger.info("Finished testPrependGetMulti");

  }

  @Test
  public void testReplaceGetMulti() throws IOException, InterruptedException {
    logger.info("Running testReplaceGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.set((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    
    Thread.sleep(1000);
    
    start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.replace((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("REPLACE Time={}ms", end - start);

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bvalue, x.value)));

    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    deleteAll(n);
    logger.info("Finished testReplaceGetMulti");

  }

  @Test
  public void testTouchGetMulti() throws IOException, InterruptedException {
    logger.info("Running testTouchGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      long expire = expireIn(100);
      boolean noreply = false;
      ResponseCode code = client.touch((key + i).getBytes(), expire, noreply);
      assertTrue(code == ResponseCode.NOT_FOUND);
      if ((i) % 10000 == 0) {
        logger.info("{}", i);
      }
    }
    long end = System.currentTimeMillis();
    logger.info("TOUCH Time={}ms", end - start);
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    start = System.currentTimeMillis();
    logger.info("ADD Time={}ms", start - end);
    
    Thread.sleep(1000);
    start = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      long expire = expireIn(5);
      boolean noreply = true;
      ResponseCode code = client.touch((key + i).getBytes(), expire, noreply);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("TOUCH Time={}ms", end - start);

    int batchSize = 100;

    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bvalue, x.value)));
    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);

    Thread.sleep(5000);
    start = System.currentTimeMillis();
    // All must expire
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == 0);
    }
    end = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, end - start);

    // Add again
    start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      list.stream().forEach(x -> assertTrue(TestUtils.equals(bvalue, x.value)));
    }
    getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);

    // Expire immediately
    for (int i = 0; i < n; i++) {
      long expire = expireIn(-1);
      boolean noreply = true;
      ResponseCode code = client.touch((key + i).getBytes(), expire, noreply);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("TOUCH Time={}ms", end - getend);

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    // All must expire
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == 0);
    }
    start = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, start - end);
    deleteAll(n);
    logger.info("Finished testTouchGetMulti");

  }

  @Test
  public void testDeleteGetMulti() throws IOException, InterruptedException {
    logger.info("Running testDeleteGetMulti");

    String key = getIdString();
    String value = TestUtils.randomString(20);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.delete((key + i).getBytes(), noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("DELETE Time={}ms", end - start);

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    start = System.currentTimeMillis();
    logger.info("ADD Time={}ms", start - end);
    
    Thread.sleep(1000);
    start = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.delete((key + i).getBytes(), noreply);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("DELETE Time={}ms", end - start);

    int batchSize = 100;

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == 0);
    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    deleteAll(n);
    logger.info("Finished testDeleteGetMulti");

  }

  @Test
  public void testIncrGetMulti() throws IOException, InterruptedException {
    logger.info("Running testIncrGetMulti");

    String key = getIdString();
    long start = System.currentTimeMillis();

    // Check INCR non-existent key
    Object res = client.incr(key.getBytes(), 1, false);
    assertTrue(res instanceof ResponseCode);
    assertTrue(((ResponseCode) res) == ResponseCode.NOT_FOUND);

    // Insert some value
    client.add(key.getBytes(), "some".getBytes(), 0, 100, false);

    // Check format
    res = client.incr(key.getBytes(), 1, false);
    assertTrue(res instanceof ResponseCode);
    assertTrue(((ResponseCode) res) == ResponseCode.CLIENT_ERROR);

    // Insert some numeric value
    client.add(key.getBytes(), "10".getBytes(), 0, 100, false);

    // Check negative increment
    res = client.incr(key.getBytes(), -1, false);
    assertTrue(res instanceof ResponseCode);
    assertTrue(((ResponseCode) res) == ResponseCode.CLIENT_ERROR);

    client.delete(key.getBytes(), true);
    
    int n = 100000;
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), "0".getBytes(), 0, 1000, noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      boolean noreply = false;
      res = client.incr((key + i).getBytes(), 10, noreply);
      assertTrue(res instanceof Long);
      assertEquals(10L, ((Long) res).longValue());
    }
    start = System.currentTimeMillis();
    logger.info("INCR Time={}ms", start - end);

    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      res = client.incr((key + i).getBytes(), 10, noreply);
      assertTrue(res == null);
    }
    end = System.currentTimeMillis();
    logger.info("INCR Time={}ms", end - start);
    
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      boolean noreply = false;
      res = client.incr((key + i).getBytes(), 10, noreply);
      assertTrue(res instanceof Long);
      assertEquals(30L, ((Long) res).longValue());
    }
    start = System.currentTimeMillis();
    logger.info("INCR Time={}ms", start - end);
    deleteAll(n);
    logger.info("Finished testIncrGetMulti");

  }

  @Test
  public void testDecrGetMulti() throws IOException, InterruptedException {
    logger.info("Running testDecrGetMulti");

    String key = getIdString();
    long start = System.currentTimeMillis();

    // Check DECR non-existent key
    Object res = client.decr(key.getBytes(), 1, false);
    assertTrue(res instanceof ResponseCode);
    assertTrue(((ResponseCode) res) == ResponseCode.NOT_FOUND);

    // Insert some value
    client.add(key.getBytes(), "some".getBytes(), 0, 100, false);

    // Check format
    res = client.decr(key.getBytes(), 1, false);
    assertTrue(res instanceof ResponseCode);
    assertTrue(((ResponseCode) res) == ResponseCode.CLIENT_ERROR);

    // Insert some numeric value
    client.add(key.getBytes(), "10".getBytes(), 0, 100, false);

    // Check negative increment
    res = client.decr(key.getBytes(), -1, false);
    assertTrue(res instanceof ResponseCode);
    assertTrue(((ResponseCode) res) == ResponseCode.CLIENT_ERROR);

    client.delete(key.getBytes(), true);

    int n = 100000;
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), "20".getBytes(), 0, 1000, noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      boolean noreply = false;
      res = client.decr((key + i).getBytes(), 10, noreply);
      assertTrue(res instanceof Long);
      assertEquals(10L, ((Long) res).longValue());
    }
    start = System.currentTimeMillis();
    logger.info("DECR Time={}ms", start - end);

    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      res = client.decr((key + i).getBytes(), 9, noreply);
      assertTrue(res == null);
    }
    end = System.currentTimeMillis();
    logger.info("DECR Time={}ms", end - start);

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    for (int i = 0; i < n; i++) {
      boolean noreply = false;
      res = client.decr((key + i).getBytes(), 10, noreply);
      assertTrue(res instanceof Long);
      assertEquals(0L, ((Long) res).longValue());
    }
    start = System.currentTimeMillis();
    logger.info("DECR Time={}ms", start - end);
    deleteAll(n);
    logger.info("Finished testDecrGetMulti");

  }

  @Test
  public void testCASGMulti() throws IOException, InterruptedException {
    logger.info("Running testCASGMulti");

    String key = getIdString();
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);

    long start = System.currentTimeMillis();
    int n = 100000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code =
          client.set((key + i).getBytes(), (value + i).getBytes(), flags, expire, noreply);
      assertTrue(code == null);
    }
    long end = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    int batchSize = 100;
    long getTime = 0;
    int count = 0;
    long[] casArray = new long[n];
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      long t1 = System.nanoTime();
      List<GetResult> list = client.gets(keys);
      getTime += System.nanoTime() - t1;
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        GetResult r = list.get(j);
        assertTrue(TestUtils.equals((value + count).getBytes(), r.value));
        casArray[count++] = list.get(j).cas.getAsLong();
      }
    }
    long getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms get_time={}", n, batchSize, getend - end,
      getTime / 1_000_000);

    start = System.currentTimeMillis();
    // Wrong CAS
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.cas((key + i).getBytes(), (value1 + i).getBytes(), flags, expire,
        noreply, casArray[i] + 1);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("CAS Time={}ms", end - start);
    
    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    count = 0;
    // OLD values
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.gets(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        GetResult r = list.get(j);
        assertTrue(TestUtils.equals((value + count++).getBytes(), r.value));
      }
    }
    getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    // Correct CAS
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.cas((key + i).getBytes(), (value1 + i).getBytes(), flags, expire,
        noreply, casArray[i]);
      assertTrue(code == null);
    }

    Thread.sleep(1000);
    end = System.currentTimeMillis();
    
    count = 0;
    // NEW values
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.gets(keys);
      if (list.size() != batchSize) {
        logger.error("list size={} i={}", list.size(), i);
      }
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        GetResult r = list.get(j);
        assertTrue(TestUtils.equals((value1 + count++).getBytes(), r.value));
      }
    }
    getend = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    deleteAll(n);
    logger.info("Finished testCASGMulti");


  }

  private byte[][] getBatch(int batchNumber, int batchSize) {
    byte[][] batch = new byte[batchSize][];
    String key = getIdString();
    for (int i = 0; i < batchSize; i++) {
      batch[i] = (key + (batchNumber * batchSize + i)).getBytes();
    }
    return batch;
  }
  
  private long expireIn(long sec) {
    if (sec <= 0) return sec;
    if (sec <= 60 * 60 * 24 * 30) {
      return sec;
    }
    return System.currentTimeMillis() / 1000 + sec;
  }
  
  private void deleteAll(int n) throws IOException, InterruptedException {
    long start = System.currentTimeMillis();
    boolean noreply = true;
    String key = getIdString();

    for (int i = 0; i < n; i++) {
      client.delete((key + i).getBytes(), noreply);
    }
    long end = System.currentTimeMillis();
    logger.info("DELETE {} in {} ms", n, end - start);
    Thread.sleep(1000);
  }
  
  public static void main(String[] args) throws IOException, InterruptedException {
    
    TestSimpleClient test = new TestSimpleClient();
    test.setUp();
    test.testAddGet();
    test.tearDown();
    
    //for (int i=0; i < 20; i++) {
      test.setUp();
      test.testAddGetMulti();
      test.tearDown();
    //}
    test.setUp();
    test.testAppendGet();
    test.tearDown();
    test.setUp();
    test.testAppendGetMulti();
    test.tearDown();
    test.setUp();
    test.testCAS();
    test.tearDown();
    test.setUp();
    test.testCASGMulti();
    test.tearDown();
    test.setUp();
    test.testDecrGetMulti();
    test.tearDown();
    test.setUp();
    test.testDeleteGetMulti();
    test.tearDown();
    test.setUp();
    test.testIncrGetMulti();
    test.tearDown();
    test.setUp();
    test.testPrependGet();
    test.tearDown();
    test.setUp();
    test.testPrependGetMulti();
    test.tearDown();
    test.setUp();
    test.testReplaceGet();
    test.tearDown();
    test.setUp();
    test.testSetGat();
    test.tearDown();
    test.setUp();
    test.testSetGats();
    test.tearDown();
    test.setUp();
    test.testSetGet();
    test.tearDown();
    test.setUp();
    test.testSetGetMulti();
    test.tearDown();
    test.setUp();
    test.testSetGets();
    test.tearDown();
    test.setUp();
    test.testSetGetMultiWithOverflow();
    test.tearDown();
    test.setUp();
    test.testInputTooLarge();
    test.tearDown();
    test.setUp();
    test.testTouchGetMulti();
    test.tearDown();
    
  }
}
