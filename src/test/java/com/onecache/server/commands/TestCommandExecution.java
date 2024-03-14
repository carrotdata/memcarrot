/*
 Copyright (C) 2023-present Onecache, Inc.

 <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 Server Side Public License, version 1, as published by MongoDB, Inc.

 <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 Server Side Public License for more details.

 <p>You should have received a copy of the Server Side Public License along with this program. If
 not, see <http://www.mongodb.com/licensing/server-side-public-license>.
*/
package com.onecache.server.commands;

import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.onecache.core.Cache;
import com.onecache.core.support.Memcached;
import com.onecache.core.support.Memcached.OpResult;
import com.onecache.core.support.Memcached.Record;
import com.onecache.server.util.TestUtils;
import static org.junit.Assert.*;
import static com.onecache.core.util.Utils.compareTo;

public class TestCommandExecution extends TestBase{
  
  Memcached support;
  
  @Before
  public void setUp() {
    super.setUp();
    Cache c;
    try {
      c = TestUtils.createCache(400_000_000, 4_000_000, true, false);
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
    long expire = System.currentTimeMillis() + 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE, inputBuffer);
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof SET);
    SET sc = (SET) c;
    
    int size = sc.execute(support, outputPtr, bufferSize);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);
    
    inputBuffer.clear();
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, true, FaultType.NONE, inputBuffer);
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof SET);
    sc = (SET) c;
    size = sc.execute(support, outputPtr, bufferSize);
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
    long expire = System.currentTimeMillis() + 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE, inputBuffer);
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof ADD);
    ADD sc = (ADD) c;
    
    int size = sc.execute(support, outputPtr, bufferSize);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.STORED, result);
    
    inputBuffer.clear();
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE, inputBuffer);
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof ADD);
    sc = (ADD) c;
    size = sc.execute(support, outputPtr, bufferSize);
    assertTrue(size > 0);
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
    
    inputBuffer.clear();
    String key = TestUtils.randomString(20);
    String value = TestUtils.randomString(200);
    int flags = 1111;
    long expire = System.currentTimeMillis() + 1000;
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE, inputBuffer);
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof REPLACE);
    REPLACE sc = (REPLACE) c;
    
    int size = sc.execute(support, outputPtr, bufferSize);
    assertTrue(size > 0);
    OpResult result = readStorageCommandResponse(outputPtr, size);
    assertEquals(OpResult.NOT_STORED, result);
    
    inputBuffer.clear();
    writeStorageCommand(cmd, key, value.getBytes(), flags, expire, 0, false, false, FaultType.NONE, inputBuffer);
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue(c instanceof REPLACE);
    sc = (REPLACE) c;
    size = sc.execute(support, outputPtr, bufferSize);
    assertTrue(size > 0);
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
 
  }
  
  @Test
  public void testPREPENDCommand() {
    byte[] cmd = "prepend".getBytes();
  }
  
  @Test
  public void testCASCommand() {
    byte[] cmd = "cas".getBytes();

  }
  
  @Test
  public void testGETCommand() {
    byte[] cmd = "get".getBytes();

  }
  
  
  @Test
  public void testGETSCommand() {
    byte[] cmd = "get".getBytes();
 
  }
  
  @Test
  public void testGATCommand() {
    byte[] cmd = "gat".getBytes();

  }
  
  @Test
  public void testGATSCommand() {
    byte[] cmd = "gats".getBytes();

  }
  
  @Test
  public void testTOUCHCommand() {

  }
  
  @Test
  public void testDELETECommand() {

  }
  
  @Test
  public void testINCRCommand() {
 
  }
  
  @Test
  public void testDECRCommand() {
 
  }
  
 
}
