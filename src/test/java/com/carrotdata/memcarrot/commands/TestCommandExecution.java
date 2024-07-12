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
package com.carrotdata.memcarrot.commands;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carrotdata.cache.Cache;
import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.support.Memcached.OpResult;
import com.carrotdata.cache.support.Memcached.Record;
import com.carrotdata.memcarrot.support.IllegalFormatException;
import com.carrotdata.memcarrot.util.TestUtils;
import static org.junit.Assert.*;
import static com.carrotdata.cache.util.Utils.compareTo;

public class TestCommandExecution extends TestBase {
  private static final Logger log = LogManager.getLogger(TestCommandExecution.class);

  Memcached support;

  @Before
  public void setUp() {
    super.setUp();
    Cache c;
    try {
      c = TestUtils.createCache(400_000_000, 4_000_000, true, true);
      support = new Memcached(c);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void tearDown() {
    if (support != null) {
      support.dispose();
    }
  }

  @Test
  public void testSETCommand() {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof SET);
    assertEquals(bufSize, c.inputConsumed());
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, true, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    sc = (SET) c;
    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size == 0);
    byte[] bkey = key.getBytes();
    Record r = support.get(bkey, 0, bkey.length);
    assertNotNull(r.value);
    byte[] bvalue = value.getBytes();
    assertTrue(compareTo(bvalue, 0, bvalue.length, r.value, r.offset, r.size) == 0);
    assertEquals(flags, r.flags);
  }

  @Test
  public void testADDCommand() {
    inputBuffer.clear();
    byte[] cmd = "add".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof ADD);
    ADD sc = (ADD) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof ADD);
    sc = (ADD) c;
    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_STORED, result);

    byte[] bkey = key.getBytes();
    Record r = support.get(bkey, 0, bkey.length);
    assertNotNull(r.value);
    byte[] bvalue = value.getBytes();
    assertTrue(compareTo(bvalue, 0, bvalue.length, r.value, r.offset, r.size) == 0);
    assertEquals(flags, r.flags);
  }

  @Test
  public void testREPLACECommand() {
    byte[] cmd = "replace".getBytes();
    byte[] set = "set".getBytes();

    inputBuffer.clear();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof REPLACE);
    REPLACE sc = (REPLACE) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_STORED, result);

    inputBuffer.clear();
    writeStorageCommand(set, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    SET s = (SET) c;
    size = s.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    value = TestUtils.randomString(200);
    flags = 1112;
    expire = 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof REPLACE);
    sc = (REPLACE) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    byte[] bkey = key.getBytes();
    Record r = support.get(bkey, 0, bkey.length);
    assertNotNull(r.value);
    byte[] bvalue = value.getBytes();
    assertTrue(compareTo(bvalue, 0, bvalue.length, r.value, r.offset, r.size) == 0);
    assertEquals(flags, r.flags);
  }

  @Test
  public void testAPPENDCommand() {
    byte[] cmd = "append".getBytes();
    byte[] set = "set".getBytes();

    inputBuffer.clear();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof APPEND);
    APPEND sc = (APPEND) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_STORED, result);

    inputBuffer.clear();
    writeStorageCommand(set, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    SET s = (SET) c;
    size = s.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    String value1 = TestUtils.randomString(200);
    flags = 1112;
    expire = 1000;
    writeStorageCommand(cmd, key, value1.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof APPEND);
    sc = (APPEND) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    byte[] bkey = key.getBytes();
    Record r = support.get(bkey, 0, bkey.length);
    assertNotNull(r.value);
    byte[] bvalue = (value + value1).getBytes();
    assertTrue(compareTo(bvalue, 0, bvalue.length, r.value, r.offset, r.size) == 0);
    assertEquals(flags, r.flags);
  }

  @Test
  public void testPREPENDCommand() {
    byte[] cmd = "prepend".getBytes();
    byte[] set = "set".getBytes();

    inputBuffer.clear();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof PREPEND);
    PREPEND sc = (PREPEND) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_STORED, result);

    inputBuffer.clear();
    writeStorageCommand(set, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    SET s = (SET) c;
    size = s.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    String value1 = TestUtils.randomString(200);
    flags = 1112;
    expire = 1000;
    writeStorageCommand(cmd, key, value1.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof PREPEND);
    sc = (PREPEND) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    byte[] bkey = key.getBytes();
    Record r = support.get(bkey, 0, bkey.length);
    assertNotNull(r.value);
    byte[] bvalue = (value1 + value).getBytes();
    assertTrue(compareTo(bvalue, 0, bvalue.length, r.value, r.offset, r.size) == 0);
    assertEquals(flags, r.flags);
  }

  @Test
  public void testCASCommand() throws IOException {
    byte[] cas = "cas".getBytes();
    byte[] gets = "gets".getBytes();
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    String key = TestUtils.randomString(20);
    long seed = System.currentTimeMillis();// 1710527677110L
    Random rnd = new Random(seed);
    String value = TestUtils.randomString(rnd, 200);
    byte[] bvalue = value.getBytes();

    int flags = 1111;
    long expire = 1000000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();

    writeRetrievalCommand(gets, new String[] { key }, 0, false, FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GETS);
    GETS g = (GETS) c;

    size = g.execute(support, outputPtr, bufferSize, null);

    assertTrue(size > 0);
    List<KeyRecord> values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(1, values.size());
    KeyRecord kr = values.get(0);
    Record r = kr.record;
    assertTrue(compareTo(bvalue, 0, bvalue.length, r.value, r.offset, r.size) == 0);

    long $cas = kr.record.cas;

    inputBuffer.clear();
    value = value + 1;
    writeStorageCommand(cas, key, value.getBytes(), flags, expire, $cas, true, false,
      FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof CAS);
    CAS cs = (CAS) c;

    size = cs.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    // check new value
    inputBuffer.clear();

    writeRetrievalCommand(gets, new String[] { key }, 0, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GETS);
    g = (GETS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(1, values.size());
    kr = values.get(0);

    bvalue = value.getBytes();
    assertTrue(
      compareTo(bvalue, 0, bvalue.length, kr.record.value, kr.record.offset, kr.record.size) == 0);

    // CAS with wrong cas
    inputBuffer.clear();
    writeStorageCommand(cas, key, value.getBytes(), flags, expire, $cas / 2, true, false,
      FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof CAS);
    cs = (CAS) c;

    size = cs.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.EXISTS, result);

    // CAS non-existent key
    inputBuffer.clear();
    writeStorageCommand(cas, key + 1, value.getBytes(), flags, expire, $cas / 2, true, false,
      FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof CAS);
    cs = (CAS) c;

    size = cs.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_FOUND, result);
  }

  @Test
  public void testGETCommand() throws IOException {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    byte[] get = "get".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);

    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeRetrievalCommand(get, new String[] { key }, 0, false, FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GET);
    GET g = (GET) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    List<KeyRecord> values = readRetrievalCommandResponse(outputPtr, size, false);
    assertEquals(1, values.size());
    KeyRecord kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);

    // get 3 keys, 2 - nonexistent
    inputBuffer.clear();
    writeRetrievalCommand(get, new String[] { key, key + "1", key + "2" }, 0, false, FaultType.NONE,
      inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GET);
    g = (GET) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, false);
    assertEquals(1, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);

    // Add one more key - key2
    String key2 = TestUtils.randomString(20);
    String value2 = TestUtils.randomString(200);
    int flags2 = 1111;
    long expire2 = 1000000;

    inputBuffer.clear();
    writeStorageCommand(cmd, key2, value2.getBytes(), flags2, expire2, 0, false, false,
      FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    // get 4 keys, 2 - nonexistent
    inputBuffer.clear();
    writeRetrievalCommand(get, new String[] { key, key2, key + "1", key + "2" }, 0, false,
      FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GET);
    g = (GET) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, false);
    assertEquals(2, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);

    kr = values.get(1);

    assertEquals(key2, new String(kr.key));
    assertEquals(value2, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags2, kr.record.flags);
  }

  @Test
  public void testGETSCommand() throws IOException {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    byte[] gets = "gets".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeRetrievalCommand(gets, new String[] { key }, 0, false, FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GET);
    GETS g = (GETS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    List<KeyRecord> values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(1, values.size());
    KeyRecord kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);
    assertTrue(kr.record.cas != 0); // in theory it could be 0, but the chances are less than
                                    // 10**(-18) :)

    // get 3 keys, 2 - nonexistent
    inputBuffer.clear();
    writeRetrievalCommand(gets, new String[] { key, key + "1", key + "2" }, 0, false,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GET);
    g = (GETS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(1, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);
    assertTrue(kr.record.cas != 0); // in theory it could be 0, but the chances are less than
                                    // 10**(-18) :)

    // Add one more key - key2
    String key2 = TestUtils.randomString(20);
    String value2 = TestUtils.randomString(200);
    int flags2 = 1111;
    long expire2 = 1000000;

    inputBuffer.clear();
    writeStorageCommand(cmd, key2, value2.getBytes(), flags2, expire2, 0, false, false,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    // get 4 keys, 2 - nonexistent
    inputBuffer.clear();
    writeRetrievalCommand(gets, new String[] { key, key2, key + "1", key + "2" }, 0, false,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof GET);
    g = (GETS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(2, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);
    assertTrue(kr.record.cas != 0); // in theory it could be 0, but the chances are less than
                                    // 10**(-18) :)

    kr = values.get(1);

    assertEquals(key2, new String(kr.key));
    assertEquals(value2, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags2, kr.record.flags);
    assertTrue(kr.record.cas != 0); // in theory it could be 0, but the chances are less than
                                    // 10**(-18) :)

  }

  @Test
  public void testGATCommand() throws IOException {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    byte[] gat = "gat".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    long newExpire = expire + 100000;
    inputBuffer.clear();
    writeRetrievalCommand(gat, new String[] { key }, newExpire, true, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof GAT);
    GAT g = (GAT) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    List<KeyRecord> values = readRetrievalCommandResponse(outputPtr, size, false);
    assertEquals(1, values.size());
    KeyRecord kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);

    Cache cache = support.getCache();
    byte[] bkey = key.getBytes();
    long exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;
    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);

    // get 3 keys, 2 - nonexistent
    inputBuffer.clear();
    newExpire = newExpire + 10000;
    writeRetrievalCommand(gat, new String[] { key, key + "1", key + "2" }, newExpire, true,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof GAT);
    g = (GAT) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, false);
    assertEquals(1, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);
    exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;

    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);

    // Add one more key - key2
    String key2 = TestUtils.randomString(20);
    String value2 = TestUtils.randomString(200);
    int flags2 = 1111;
    long expire2 = 1000000;

    inputBuffer.clear();
    writeStorageCommand(cmd, key2, value2.getBytes(), flags2, expire2, 0, false, false,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    newExpire += 10000;
    // get 4 keys, 2 - nonexistent
    inputBuffer.clear();
    writeRetrievalCommand(gat, new String[] { key, key2, key + "1", key + "2" }, newExpire, true,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof GAT);
    g = (GAT) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, false);
    assertEquals(2, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);

    exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;

    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);

    kr = values.get(1);

    assertEquals(key2, new String(kr.key));
    assertEquals(value2, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags2, kr.record.flags);
    bkey = key2.getBytes();
    exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;
    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);
  }

  @Test
  public void testGATSCommand() throws IOException {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    byte[] gats = "gats".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    long newExpire = expire + 100000;
    inputBuffer.clear();
    writeRetrievalCommand(gats, new String[] { key }, newExpire, true, FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GATS);
    GATS g = (GATS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    List<KeyRecord> values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(1, values.size());
    KeyRecord kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);
    assertTrue(kr.record.cas != 0);

    Cache cache = support.getCache();
    byte[] bkey = key.getBytes();
    long exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;
    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);

    // get 3 keys, 2 - nonexistent
    inputBuffer.clear();
    newExpire = newExpire + 10000;
    writeRetrievalCommand(gats, new String[] { key, key + "1", key + "2" }, newExpire, true,
      FaultType.NONE, inputBuffer);

    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());

    assertTrue(c instanceof GATS);
    g = (GATS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(1, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);
    exptime = cache.getExpire(bkey, 0, bkey.length);
    
    exptime = (exptime - System.currentTimeMillis()) / 1000;

    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);
    assertTrue(kr.record.cas != 0);

    // Add one more key - key2
    String key2 = TestUtils.randomString(20);
    String value2 = TestUtils.randomString(200);
    int flags2 = 1111;
    long expire2 = 1000000;

    inputBuffer.clear();
    writeStorageCommand(cmd, key2, value2.getBytes(), flags2, expire2, 0, false, false,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    newExpire += 10000;
    // get 4 keys, 2 - nonexistent
    inputBuffer.clear();
    writeRetrievalCommand(gats, new String[] { key, key2, key + "1", key + "2" }, newExpire, true,
      FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof GATS);
    g = (GATS) c;

    size = g.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    values = readRetrievalCommandResponse(outputPtr, size, true);
    assertEquals(2, values.size());
    kr = values.get(0);

    assertEquals(key, new String(kr.key));
    assertEquals(value, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags, kr.record.flags);

    exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;

    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);
    assertTrue(kr.record.cas != 0);

    kr = values.get(1);

    assertEquals(key2, new String(kr.key));
    assertEquals(value2, new String(kr.record.value, kr.record.offset, kr.record.size));
    assertEquals(flags2, kr.record.flags);
    bkey = key2.getBytes();
    exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;

    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);
    assertTrue(kr.record.cas != 0);

  }

  @Test
  public void testTOUCHCommand() {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    int size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();

    long newExpire = expire + 10000;
    writeTouchCommand(key, newExpire, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof TOUCH);
    TOUCH t = (TOUCH) c;

    size = t.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readTouchCommandResponse(outputPtr, size);
    assertEquals(OpResult.TOUCHED, result);

    Cache cache = support.getCache();
    byte[] bkey = key.getBytes();
    long exptime = cache.getExpire(bkey, 0, bkey.length);
    exptime = (exptime - System.currentTimeMillis()) / 1000;
    assertTrue(Math.abs(newExpire - exptime) <= 0.01 * exptime);

    // TOUCH non-existent key
    inputBuffer.clear();
    newExpire = expire + 10000;
    writeTouchCommand(key + 1, newExpire, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof TOUCH);
    t = (TOUCH) c;

    size = t.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readTouchCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_FOUND, result);

  }

  @Test
  public void testDELETECommand() {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();

    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = 1000;

    inputBuffer.clear();
    writeDeleteCommand(key, false, FaultType.NONE, inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof DELETE);
    DELETE d = (DELETE) c;
    int size = d.execute(support, outputPtr, bufferSize, null);
    OpResult result = readDeleteCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_FOUND, result);

    inputBuffer.clear();

    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeDeleteCommand(key, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof DELETE);
    d = (DELETE) c;
    size = d.execute(support, outputPtr, bufferSize, null);
    result = readDeleteCommandResponse(outputPtr, size);
    assertEquals(OpResult.DELETED, result);

    byte[] bkey = key.getBytes();
    Record r = support.get(bkey, 0, bkey.length);
    assertNull(r.value);
  }

  @Test
  public void testINCRCommand() {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();

    String key = TestUtils.randomString(20);
    String value = "10";
    int flags = 1111;
    long expire = 1000;

    inputBuffer.clear();
    writeIncrCommand(key, 10, false, FaultType.NONE, inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof INCR);
    INCR d = (INCR) c;
    int size = d.execute(support, outputPtr, bufferSize, null);
    Object result = readIncrDecrCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_FOUND, result);

    inputBuffer.clear();

    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeIncrCommand(key, 10, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof INCR);
    d = (INCR) c;
    size = d.execute(support, outputPtr, bufferSize, null);
    result = readIncrDecrCommandResponse(outputPtr, size);
    assertEquals(20L, ((Long) result).longValue());

    // Try not a number increment

    inputBuffer.clear();
    writeIncrCommand(key, 10, false, FaultType.VALUE_NOT_NUMBER, inputBuffer);

    try {
      c = CommandParser.parse(inputPtr, inputBuffer.position());
      fail();
    } catch (IllegalFormatException e) {
      // expected
    }

    // Try not a number original
    inputBuffer.clear();
    value = "value";
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    sc = (SET) c;
    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeIncrCommand(key, 10, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof INCR);
    d = (INCR) c;
    size = d.execute(support, outputPtr, bufferSize, null);
    result = readIncrDecrCommandResponse(outputPtr, size);
    assertTrue(result instanceof String);
    assertTrue(((String) result).startsWith("CLIENT_ERROR "));
  }

  @Test
  public void testDECRCommand() {
    inputBuffer.clear();
    byte[] cmd = "set".getBytes();

    String key = TestUtils.randomString(20);
    String value = "10";
    int flags = 1111;
    long expire = 1000;

    inputBuffer.clear();
    writeDecrCommand(key, 10, false, FaultType.NONE, inputBuffer);
    int bufSize = inputBuffer.position();
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof DECR);
    DECR d = (DECR) c;
    int size = d.execute(support, outputPtr, bufferSize, null);
    Object result = readIncrDecrCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_FOUND, result);

    inputBuffer.clear();

    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;

    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeDecrCommand(key, 9, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof DECR);
    d = (DECR) c;
    size = d.execute(support, outputPtr, bufferSize, null);
    result = readIncrDecrCommandResponse(outputPtr, size);
    assertEquals(1L, ((Long) result).longValue());

    inputBuffer.clear();
    writeDecrCommand(key, 9, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof DECR);
    d = (DECR) c;
    size = d.execute(support, outputPtr, bufferSize, null);
    result = readIncrDecrCommandResponse(outputPtr, size);
    assertEquals(0L, ((Long) result).longValue());

    // Try not a number increment

    inputBuffer.clear();
    writeDecrCommand(key, 10, false, FaultType.VALUE_NOT_NUMBER, inputBuffer);

    try {
      c = CommandParser.parse(inputPtr, inputBuffer.position());
      fail();
    } catch (IllegalFormatException e) {
      // expected
    }

    // Try not a number original
    inputBuffer.clear();
    value = "value";
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE,
      inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof SET);
    sc = (SET) c;
    size = sc.execute(support, outputPtr, bufferSize, null);
    assertTrue(size > 0);
    result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);

    inputBuffer.clear();
    writeDecrCommand(key, 10, false, FaultType.NONE, inputBuffer);
    bufSize = inputBuffer.position();
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertEquals(bufSize, c.inputConsumed());
    assertTrue(c instanceof DECR);
    d = (DECR) c;
    size = d.execute(support, outputPtr, bufferSize, null);
    result = readIncrDecrCommandResponse(outputPtr, size);
    assertTrue(result instanceof String);
    assertTrue(((String) result).startsWith("CLIENT_ERROR "));
  }

}
