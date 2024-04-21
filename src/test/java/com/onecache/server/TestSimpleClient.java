package com.onecache.server;

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

import com.onecache.core.Cache;
import com.onecache.core.support.Memcached;
import com.onecache.server.SimpleClient.GetResult;
import com.onecache.server.SimpleClient.ResponseCode;
import com.onecache.server.util.TestUtils;


public class TestSimpleClient {
  private static Logger logger = LogManager.getLogger(TestSimpleClient.class);

  OnecacheServer server;
  SimpleClient client;
  
  @Before
  public void setUp() throws IOException {
    Cache c = TestUtils.createCache(800_000_000, 4_000_000, true, true);
    Memcached m = new Memcached(c);
    server = new OnecacheServer();
    server.setMemachedSupport(m);
    server.start();
    client = new SimpleClient(server.getHost(), server.getPort());
  }
  
  @After
  public void tearDown() throws IOException {
    client.close();
    server.stop();
  }
  
  @Test
  public void testSetGet() throws IOException {
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
    List<GetResult> result = client.get(new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    
  }
  
  @Test
  public void testSetGets() throws IOException {
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
    List<GetResult> result = client.gets(new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    assertTrue(gr.cas.isPresent());
    assertTrue(gr.cas.getAsLong() != 0);
    
  }

  @Test
  public void testSetGats() throws IOException {
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
    List<GetResult> result = client.gats(10, new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    assertTrue(gr.cas.isPresent());
    assertTrue(gr.cas.getAsLong() != 0);
    // expire item, but returns current value
    result = client.gats(-1, new byte[][] {bkey});
    assertEquals(1, result.size());
    // Expired
    result = client.get(new byte[][] {bkey});
    assertEquals(0, result.size());
  }
  
  @Test
  public void testSetGat() throws IOException {
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
    List<GetResult> result = client.gat(10, new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    assertNull(gr.cas);
    // expire item, but returns current value
    result = client.gat(-1, new byte[][] {bkey});
    assertEquals(1, result.size());
    // Expired
    result = client.get(new byte[][] {bkey});
    assertEquals(0, result.size());
  }
  
  @Test
  public void testAddGet() throws IOException {
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
    List<GetResult> result = client.get(new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    
  }
  
  @Test
  public void testAppendGet() throws IOException {
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
    List<GetResult> result = client.get(new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals((value + value1).getBytes(), gr.value));
    assertEquals(flags, gr.flags);
    
  }
  
  @Test
  public void testPrependGet() throws IOException {
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
    List<GetResult> result = client.get(new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals((value1 + value).getBytes(), gr.value));
    assertEquals(flags, gr.flags);
    
  }
  
  @Test
  public void testReplaceGet() throws IOException {
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
    List<GetResult> result = client.get(new byte[][] {bkey});
    assertEquals(1, result.size());
    GetResult gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bvalue, gr.value));
    assertEquals(flags, gr.flags);
    
  }
  
  @Test
  public void testCAS() throws IOException {
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
    List<GetResult> result = client.gets(new byte[][] {bkey});
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
    
    result = client.get(new byte[][] {bkey});
    assertEquals(1, result.size());
    gr = result.get(0);
    assertTrue(TestUtils.equals(bkey, gr.key));
    assertTrue(TestUtils.equals(bkey, gr.value));
    assertEquals(flags, gr.flags);
    
  }
  
  @Test
  public void testSetGetMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.set((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    
    int batchSize = 100;
    long getTime = 0;
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      long t1 = System.nanoTime();
      List<GetResult> list = client.get(keys);
      getTime += System.nanoTime() - t1;
      assertTrue(list.size() == batchSize);
      list.stream().forEach( x-> assertTrue(TestUtils.equals(bvalue, x.value)));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms get_time={}", n, batchSize, 
      getend - end, getTime / 1_000_000);
    
  }
  @Test
  public void testAddGetMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    int batchSize = 100;
    
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == batchSize);
      list.stream().forEach( x-> assertTrue(TestUtils.equals(bvalue, x.value)));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
  }
  
  @Test
  public void testAppendGetMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    byte[] bvalue1 = value1.getBytes();
    byte[] bv = (value + value1).getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
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
      assertTrue(list.size() == batchSize);
      list.stream().forEach( x-> assertTrue(TestUtils.equals(bv, x.value)));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);
    
  }
  
  @Test
  public void testPrependGetMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(20);
    String value1 = TestUtils.randomString(20);
    byte[] bvalue = value.getBytes();
    byte[] bvalue1 = value1.getBytes();
    byte[] bv = (value1 + value).getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.prepend((key + i).getBytes(), bvalue1, flags, expire, noreply);
      assertTrue(code == null);
    }
    start = System.currentTimeMillis();
    logger.info("PREPEND Time={}ms", start - end);

    
    int batchSize = 100;
    
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == batchSize);
      for(int j = 0; j < list.size(); j++) {
        assertTrue(TestUtils.equals(bv, list.get(j).value));
      }      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - start);
    
  }
  
  @Test
  public void testReplaceGetMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.set((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.replace((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    end  = System.currentTimeMillis();
    logger.info("REPLACE Time={}ms", end - start);
    
    int batchSize = 100;
    
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == batchSize);
      list.stream().forEach( x-> assertTrue(TestUtils.equals(bvalue, x.value)));
      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
  }
  
  @Test
  public void testTouchGetMulti() throws IOException, InterruptedException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      long expire = expireIn(100);
      boolean noreply = false;
      ResponseCode code = client.touch((key + i).getBytes(), expire, noreply);
      assertTrue(code == ResponseCode.NOT_FOUND);
    }
    long end  = System.currentTimeMillis();
    logger.info("TOUCH Time={}ms", end - start);

    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    start  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", start - end);
    
    for (int i = 0; i < n; i++) {
      long expire = expireIn(10);
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
      assertTrue(list.size() == batchSize);
      list.stream().forEach( x-> assertTrue(TestUtils.equals(bvalue, x.value)));
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
    Thread.sleep(10000);
    start = System.currentTimeMillis();
    // All must expire
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == 0);
    }
    end  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      end - start);
    
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
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == batchSize);
      list.stream().forEach( x-> assertTrue(TestUtils.equals(bvalue, x.value)));
    }
    getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
    // Expire immediately
    for (int i = 0; i < n; i++) {
      long expire = expireIn(-1);
      boolean noreply = true;
      ResponseCode code = client.touch((key + i).getBytes(), expire, noreply);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("TOUCH Time={}ms", end - getend);
    
    // All must expire
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == 0);
    }
    start  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      start - end);
  }
  
  @Test
  public void testDeleteGetMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(20);
    byte[] bvalue = value.getBytes();
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.delete((key + i).getBytes(), noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("DELETE Time={}ms", end - start);
    
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), bvalue, flags, expire, noreply);
      assertTrue(code == null);
    }
    start = System.currentTimeMillis();
    logger.info("ADD Time={}ms", start - end);

    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.delete((key + i).getBytes(), noreply);
      assertTrue(code == null);
    }
    end  = System.currentTimeMillis();
    logger.info("DELETE Time={}ms", end - start);
    
    int batchSize = 100;
    
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.get(keys);
      assertTrue(list.size() == 0);      
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, 
      getend - end);
    
  }
  
  @Test
  public void testIncrGetMulti() throws IOException {
    String key = "KEY:";
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
    
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), "0".getBytes(), 0, 100, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
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
    end  = System.currentTimeMillis();
    logger.info("INCR Time={}ms", end - start);
    
    for (int i = 0; i < n; i++) {
      boolean noreply = false;
      res = client.incr((key + i).getBytes(), 10, noreply);
      assertTrue(res instanceof Long);
      assertEquals(30L, ((Long) res).longValue());
    }
    start = System.currentTimeMillis();
    logger.info("INCR Time={}ms", start - end);
    
  }
  
  @Test
  public void testDecrGetMulti() throws IOException {
    String key = "KEY:";
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
    
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      boolean noreply = true;
      ResponseCode code = client.add((key + i).getBytes(), "20".getBytes(), 0, 100, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("ADD Time={}ms", end - start);
    
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
    end  = System.currentTimeMillis();
    logger.info("DECR Time={}ms", end - start);
    
    for (int i = 0; i < n; i++) {
      boolean noreply = false;
      res = client.decr((key + i).getBytes(), 10, noreply);
      assertTrue(res instanceof Long);
      assertEquals(0L, ((Long) res).longValue());
    }
    start = System.currentTimeMillis();
    logger.info("DECR Time={}ms", start - end);
  }
  
  @Test
  public void testCASGMulti() throws IOException {
    String key = "KEY:";
    String value = TestUtils.randomString(200);
    String value1 = TestUtils.randomString(200);
    
    long start = System.currentTimeMillis();
    int n = 1_000_000;
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.set((key + i).getBytes(), (value + i).getBytes(), flags, expire, noreply);
      assertTrue(code == null);
    }
    long end  = System.currentTimeMillis();
    logger.info("SET Time={}ms", end - start);
    
    int batchSize = 100;
    long getTime = 0;
    int count = 0;
    long[] casArray = new long[n];
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      long t1 = System.nanoTime();
      List<GetResult> list = client.gets(keys);
      getTime += System.nanoTime() - t1;
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        GetResult r = list.get(j);
        assertTrue(TestUtils.equals((value + count).getBytes(), r.value));
        casArray[count++] = list.get(j).cas.getAsLong();
      }
    }
    long getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms get_time={}", n, batchSize, 
      getend - end, getTime / 1_000_000);
    
    start = System.currentTimeMillis();
    // Wrong CAS
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.cas((key + i).getBytes(), (value1 + i).getBytes(), flags, expire, noreply, casArray[i] + 1);
      assertTrue(code == null);
    }
    end = System.currentTimeMillis();
    logger.info("CAS Time={}ms", end - start);
    
    count = 0;
    // OLD values
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.gets(keys);
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        GetResult r = list.get(j);
        assertTrue(TestUtils.equals((value + count++).getBytes(), r.value));
      }
    }
    getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    // Correct CAS
    for (int i = 0; i < n; i++) {
      int flags = 1;
      long expire = expireIn(100);
      boolean noreply = true;
      ResponseCode code = client.cas((key + i).getBytes(), (value1 + i).getBytes(), flags, expire, noreply, casArray[i]);
      assertTrue(code == null);
    }
    
    end  = System.currentTimeMillis();
    
    count = 0;
    // NEW values
    for (int i = 0; i < n / batchSize; i++) {
      byte[][] keys = getBatch(i, batchSize);
      List<GetResult> list = client.gets(keys);
      assertTrue(list.size() == batchSize);
      for (int j = 0; j < list.size(); j++) {
        GetResult r = list.get(j);
        assertTrue(TestUtils.equals((value1 + count++).getBytes(), r.value));
      }
    }
    getend  = System.currentTimeMillis();
    logger.info("GET total={} batch={} time={}ms", n, batchSize, getend - end);
    
    
  }
  
  private byte[][] getBatch(int batchNumber, int batchSize) {
    byte[][] batch = new byte[batchSize][];
    for (int i = 0; i < batchSize; i++) {
      batch[i] = ("KEY:" + (batchNumber * batchSize + i)).getBytes();
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
}
