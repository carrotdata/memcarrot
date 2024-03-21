package com.onecache.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.onecache.core.Cache;
import com.onecache.core.support.Memcached;
import com.onecache.server.util.TestUtils;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;


public class TestXMemcachedClient {
  private static Logger logger = LogManager.getLogger(TestXMemcachedClient.class);

  OnecacheServer server;
  XMemcachedClient client;
  
  @Before
  public void setUp() throws IOException {
    Cache c = TestUtils.createCache(400_000_000, 4_000_000, true, true);
    Memcached m = new Memcached(c);
    server = new OnecacheServer();
    server.setMemachedSupport(m);
    server.start();
    client = new XMemcachedClient(server.getHost(), server.getPort());
  }
  
  @After
  public void tearDown() throws IOException {
    client.shutdown();;
    server.stop();
  }
  
  @Test
  public void testSet() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    
    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);
    
    Object res = client.get(key);
    assertTrue (res instanceof String);
    assertEquals(value, (String) res);
    
    value += 1;
    client.setWithNoReply(key, expire, value);
    // Now get
    res = client.get(key);
    
    assertTrue (res instanceof String);
    assertEquals(value, (String) res);
    
  }

  @Test
  public void testGet() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    
    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);
    
    Object res = client.get(key);
    assertTrue (res instanceof String);
    assertEquals(value, (String) res);
    // Get non-existent key
    res = client.get(key + 1);
    assertNull(res);
    
  }
 
  @Test
  public void testGets() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    
    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);
    
    GetsResponse<?> res = client.gets(key);
    assertTrue (res.getValue() instanceof String);
    assertEquals(value, (String) res.getValue());
    long cas1 = res.getCas();
    assertTrue(cas1 != 0);
    
    value += 1;
    client.setWithNoReply(key, expire, value);
    // Now get
    res = client.gets(key);
    
    assertTrue (res.getValue() instanceof String);
    assertEquals(value, (String) res.getValue());
    long cas2 = res.getCas();
    assertTrue(cas2 != 0);
    assertTrue(cas1 != cas2);
  }

  @Test
  public void testAdd() throws IOException, InterruptedException, MemcachedException, TimeoutException {
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
  public void testAppend() throws IOException, TimeoutException, InterruptedException, MemcachedException {
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
  public void testPrepend() throws IOException, TimeoutException, InterruptedException, MemcachedException {
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
  public void testReplace() throws IOException, TimeoutException, InterruptedException, MemcachedException {
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
  public void testCAS() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    
    int expire = 100;
    
    boolean code;
    try {
      code = client.cas(key, expire, value, 0);
    } catch(MemcachedException e) {
      
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
  public void testDelete() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    
    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);
    
    Object res = client.get(key);
    assertTrue (res instanceof String);
    assertEquals(value, (String) res);
    
    result = client.delete(key);
    assertTrue(result);
    res = client.get(key);
    assertNull (res);
  }
  
  @Test
  public void testTouch() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    
    int expire = 100;
    boolean result = client.set(key, expire, value);
    assertTrue(result);
    
    Object res = client.get(key);
    assertTrue (res instanceof String);
    assertEquals(value, (String) res);
    
    result = client.touch(key, 2);
    assertTrue(result);
    
    res = client.get(key);
    assertTrue (res instanceof String);
    assertEquals(value, (String) res);
  
    Thread.sleep(2000);
    res = client.get(key);
    assertNull (res);
  }
  
  @Test
  public void testIncr() throws IOException, TimeoutException, InterruptedException, MemcachedException {
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
    assertTrue (res instanceof String);
    assertEquals(num, (String) res);
    
    res = client.get(key2);
    assertTrue (res instanceof String);
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
  public void testDecr() throws IOException, TimeoutException, InterruptedException, MemcachedException {
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
    assertTrue (res instanceof String);
    assertEquals(num, (String) res);
    
    res = client.get(key2);
    assertTrue (res instanceof String);
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
    
    // non - existent  key
    try {
      value = client.decr(key2 + 1, 10);
      fail();
    } catch (MemcachedException e) {
      
    }
  }
  
  @Test
  public void testSetMulti() throws IOException, InterruptedException, MemcachedException, TimeoutException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.set((key + i), expire, value);
        assertTrue(res);
      } else {
        client.setWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    
    int batchSize = 100;
    
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach( x-> assertTrue(value.equals(map.get(x))));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
  }
  
  private List<String> getBatch(int batchNum, int batchSize){
    List<String> batch = new ArrayList<String>();
    for (int i = 0; i < batchSize; i++) {
      batch.add("KEY:" + (batchNum * batchSize + i));
    }
    return batch;
  }
  
  @Test
  public void testAddMulti() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    int batchSize = 100;
    
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach( x-> assertTrue(value.equals(map.get(x))));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
  }
 
  @Test
  public void testAppendMulti() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
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

    int batchSize = 100;
    String newValue = value + value1;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach( x-> assertTrue(newValue.equals(map.get(x))));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);
    
    
  }
  
  @Test
  public void testPrependMulti() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
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

    int batchSize = 100;
    String newValue = value1 + value;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach( x-> assertTrue(newValue.equals(map.get(x))));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);
  }
 
  @Test
  public void testReplaceMulti() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
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

    int batchSize = 100;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach( x-> assertTrue(value1.equals(map.get(x))));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);
    
  }
  
  @Test
  public void testTouchMulti() throws IOException, InterruptedException, TimeoutException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 1000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    for (int i = 0; i < n; i++) {
      int expire = 2;
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
      map.keySet().forEach( x-> assertTrue(value.equals(map.get(x))));
    }
    end = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, end - start);

    Thread.sleep(2000);
    start = System.currentTimeMillis();
    // Must all expired
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map == null || map.size() == 0);
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);

  }
  
 
  @Test
  public void testDeleteMulti() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10_000;
    for (int i = 0; i < n; i++) {
      int expire = 100;
      if (i % 100 == 0) {
        boolean res = client.add((key + i), expire, value);
        assertTrue(res);
      } else {
        client.addWithNoReply((key + i), expire, value);
      }
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    int batchSize = 100;
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map.size() == batchSize);
      map.keySet().forEach( x-> assertTrue(value.equals(map.get(x))));
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

    start = System.currentTimeMillis();
    // Must all be deleted
    for (int i = 0; i < n / batchSize; i++) {
      List<String> keys = getBatch(i, batchSize);
      Map<String, Object> map = client.get(keys);
      assertTrue(map == null || map.size() == 0);
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);
    
  }
  
 
  @Test
  public void testIncrMulti()
      throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = "0";
    long start = System.currentTimeMillis();
    int n = 10_000;
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

    for (int i = 0; i < n; i++) {
      long v = client.incr((key + i), 10);
      assertEquals(20L, v);
    }
    end = System.currentTimeMillis();
    logger.info("INCR Time={}ms", end - start);
  }
  
  
  @Test
  public void testDecrMulti() throws IOException, TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = "100";
    long start = System.currentTimeMillis();
    int n = 10_000;
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

    for (int i = 0; i < n; i++) {
      long v = client.decr((key + i), 100);
      assertEquals(0L, v);
    }
    end = System.currentTimeMillis();
    logger.info("DECR Time={}ms", end - start);
  }

  // There issues with CAS noreply in xmemcached library
  @Test
  public void TestCASMulti() throws TimeoutException, InterruptedException, MemcachedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    long start = System.currentTimeMillis();
    int n = 10_000;
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

    for (int i = 0; i < n; i++) {
      Object res = client.get(key + i);
      assertTrue(res instanceof String);
      assertEquals(value + i + "$", (String) res);
    }
    start = System.currentTimeMillis();
    logger.info("GET total={} time={}ms", n, start - end);
  }
}
