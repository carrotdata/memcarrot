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
package com.carrotdata.memcarrot.commands;

import java.util.Random;

import org.junit.Test;

import com.carrotdata.memcarrot.util.TestUtils;
import static org.junit.Assert.*;

public class TestCommandParser extends TestBase{

  private void testStorageCommand(byte[] cmd, boolean withCAS, boolean withNoreply, FaultType type) {
    Random r = new Random();
    int keyMax = 100;
    int keyMin = 10;
    int valueMax = 1000;
    int valueMin = 100;
    for (int i = 0; i < 1000; i++) {
      inputBuffer.clear();
      String key = TestUtils.randomString(keyMin + r.nextInt(keyMax - keyMin));
      int vsize = valueMin + r.nextInt(valueMax  - valueMin);
      byte[] value = new byte [vsize];
      r.nextBytes(value);
      // Must be positive
      int flags = Math.abs(r.nextInt());
      long expire = r.nextLong();
      long cas = r.nextLong();
      writeStorageCommand(cmd, key, value, flags, expire, cas, withCAS, withNoreply, type, inputBuffer);
      MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
      if (type == FaultType.INCOMPLETE) {
        assertNull(c);
        continue;
      }
      assertTrue(c instanceof StorageCommand);
      StorageCommand sc = (StorageCommand) c;
      equalsKeys(key.getBytes(), sc);
      equalsValues(value, sc);
      assertEquals(flags, (int) sc.flags);
      assertEquals(expire, sc.exptime);
      if (withCAS) {
        assertEquals (cas, sc.cas);
      }
      if (withNoreply) {
        assertEquals(withNoreply, sc.noreply);
      }
    }
  }
  
  private void testRetrievalCommand(byte[] cmd, boolean withExpire, FaultType type) {
    Random r = new Random();
    int keyMax = 100;
    int keyMin = 10;
    int numKeysMax = 10;
    int numKeysMin = 1;
    
    for (int i = 0; i < 1000; i++) {
      inputBuffer.clear();
      int numKeys = numKeysMin + r.nextInt(numKeysMax - numKeysMin);
      String[] keys = new String[numKeys];
      for (int j = 0; j < numKeys; j++) {
        keys[j] = TestUtils.randomString(keyMin + r.nextInt(keyMax - keyMin));
      }      
      long expire = r.nextLong();
      writeRetrievalCommand(cmd, keys, expire, withExpire,  type, inputBuffer);
      MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
      if (type == FaultType.INCOMPLETE) {
        assertNull(c);
        continue;
      }
      assertTrue(c instanceof RetrievalCommand);
      RetrievalCommand rc = (RetrievalCommand) c;
      equalsKeys(keys, rc);
      if (withExpire) {
        assertEquals (expire, rc.exptime);
      }

    }
  }
  
  private void equalsKeys(String[] keys, RetrievalCommand c) {
    assertTrue (c.keys != null);
    for (int i = 0; i < c.keys.length; i++) {
      byte[] k = keys[i].getBytes();
      assertTrue (com.carrotdata.cache.util.Utils.compareTo(k, 0, k.length, c.keys[i], c.keySizes[i]) == 0);
    }
  }
  
  private void equalsKeys(byte[] key, AbstractMemcachedCommand c) {
    assertTrue (c.keyPtr > 0);
    assertTrue (com.carrotdata.cache.util.Utils.compareTo(key, 0, key.length, c.keyPtr, c.keySize) == 0);
  }

  
  private void equalsValues(byte[] value, StorageCommand c) {
    assertTrue (c.valPtr > 0);
    assertTrue (com.carrotdata.cache.util.Utils.compareTo(value, 0, value.length, c.valPtr, c.valSize) == 0);
  }
  
  @Test
  public void testSETCommand() {
    byte[] cmd = "set".getBytes();
    testStorageCommand(cmd, false, false, FaultType.NONE);
    testStorageCommand(cmd, false, true, FaultType.NONE);
    testStorageCommand(cmd, false, false, FaultType.INCOMPLETE);
    testStorageCommand(cmd, false, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testADDCommand() {
    byte[] cmd = "add".getBytes();
    testStorageCommand(cmd, false, false, FaultType.NONE);
    testStorageCommand(cmd, false, true, FaultType.NONE);
    testStorageCommand(cmd, false, false, FaultType.INCOMPLETE);
    testStorageCommand(cmd, false, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testREPLACECommand() {
    byte[] cmd = "replace".getBytes();
    testStorageCommand(cmd, false, false, FaultType.NONE);
    testStorageCommand(cmd, false, true, FaultType.NONE);
    testStorageCommand(cmd, false, false, FaultType.INCOMPLETE);
    testStorageCommand(cmd, false, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testAPPENDCommand() {
    byte[] cmd = "append".getBytes();
    testStorageCommand(cmd, false, false, FaultType.NONE);
    testStorageCommand(cmd, false, true, FaultType.NONE);
    testStorageCommand(cmd, false, false, FaultType.INCOMPLETE);
    testStorageCommand(cmd, false, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testPREPENDCommand() {
    byte[] cmd = "prepend".getBytes();
    testStorageCommand(cmd, false, false, FaultType.NONE);
    testStorageCommand(cmd, false, true, FaultType.NONE);
    testStorageCommand(cmd, false, false, FaultType.INCOMPLETE);
    testStorageCommand(cmd, false, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testCASCommand() {
    byte[] cmd = "cas".getBytes();
    testStorageCommand(cmd, true, false, FaultType.NONE);
    testStorageCommand(cmd, true, true, FaultType.NONE);
    testStorageCommand(cmd, true, false, FaultType.INCOMPLETE);
    testStorageCommand(cmd, true, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testGETCommand() {
    byte[] cmd = "get".getBytes();
    testRetrievalCommand(cmd, false, FaultType.NONE);
    testRetrievalCommand(cmd, false, FaultType.INCOMPLETE);
  }
  
  
  @Test
  public void testGETSCommand() {
    byte[] cmd = "get".getBytes();
    testRetrievalCommand(cmd, false, FaultType.NONE);
    testRetrievalCommand(cmd, false, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testGATCommand() {
    byte[] cmd = "gat".getBytes();
    testRetrievalCommand(cmd, true, FaultType.NONE);
    testRetrievalCommand(cmd, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testGATSCommand() {
    byte[] cmd = "gats".getBytes();
    testRetrievalCommand(cmd, true, FaultType.NONE);
    testRetrievalCommand(cmd, true, FaultType.INCOMPLETE);
  }
  
  @Test
  public void testTOUCHCommand() {
    Random r = new Random();
    int keyMax = 100;
    int keyMin = 10;
    for (int i = 0; i < 1000; i++) {
      inputBuffer.clear();
      String key = TestUtils.randomString(keyMin + r.nextInt(keyMax - keyMin));
      boolean withNoreply = r.nextBoolean();
      FaultType type = r.nextBoolean()? FaultType.NONE: FaultType.INCOMPLETE;
      long expire = r.nextLong();
      writeTouchCommand(key, expire, withNoreply, type, inputBuffer);
      MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
      if (type == FaultType.INCOMPLETE) {
        assertNull(c);
        continue;
      }
      assertTrue(c instanceof TOUCH);
      TOUCH sc = (TOUCH) c;
      equalsKeys(key.getBytes(), sc);
      assertEquals(expire, sc.exptime);
      if (withNoreply) {
        assertEquals(withNoreply, sc.noreply);
      }
    }
  }
  
  @Test
  public void testDELETECommand() {
    Random r = new Random();
    int keyMax = 100;
    int keyMin = 10;
    for (int i = 0; i < 1000; i++) {
      inputBuffer.clear();
      String key = TestUtils.randomString(keyMin + r.nextInt(keyMax - keyMin));
      boolean withNoreply = r.nextBoolean();
      FaultType type = r.nextBoolean()? FaultType.NONE: FaultType.INCOMPLETE;
      writeDeleteCommand(key, withNoreply, type, inputBuffer);
      MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
      if (type == FaultType.INCOMPLETE) {
        assertNull(c);
        continue;
      }
      assertTrue(c instanceof DELETE);
      DELETE sc = (DELETE) c;
      equalsKeys(key.getBytes(), sc);
      if (withNoreply) {
        assertEquals(withNoreply, sc.noreply);
      }
    }
  }
  
  @Test
  public void testINCRCommand() {
    Random r = new Random();
    int keyMax = 100;
    int keyMin = 10;
    for (int i = 0; i < 1000; i++) {
      inputBuffer.clear();
      String key = TestUtils.randomString(keyMin + r.nextInt(keyMax - keyMin));
      boolean withNoreply = r.nextBoolean();
      FaultType type = r.nextBoolean()? FaultType.NONE: FaultType.INCOMPLETE;
      long v = r.nextLong();
      writeIncrCommand(key, v, withNoreply, type, inputBuffer);
      MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
      if (type == FaultType.INCOMPLETE) {
        assertNull(c);
        continue;
      }
      assertTrue(c instanceof INCR);
      INCR sc = (INCR) c;
      equalsKeys(key.getBytes(), sc);
      assertEquals(v, sc.value);
      if (withNoreply) {
        assertEquals(withNoreply, sc.noreply);
      }
    }
  }
  
  @Test
  public void testDECRCommand() {
    Random r = new Random();
    int keyMax = 100;
    int keyMin = 10;
    for (int i = 0; i < 1000; i++) {
      inputBuffer.clear();
      String key = TestUtils.randomString(keyMin + r.nextInt(keyMax - keyMin));
      boolean withNoreply = r.nextBoolean();
      FaultType type = r.nextBoolean()? FaultType.NONE: FaultType.INCOMPLETE;
      long v = r.nextLong();
      writeDecrCommand(key, v, withNoreply, type, inputBuffer);
      MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
      if (type == FaultType.INCOMPLETE) {
        assertNull(c);
        continue;
      }
      assertTrue(c instanceof DECR);
      DECR sc = (DECR) c;
      equalsKeys(key.getBytes(), sc);
      assertEquals(v, sc.value);
      if (withNoreply) {
        assertEquals(withNoreply, sc.noreply);
      }
    }
  }
  
  @Test
  public void testSHUTDOWNCommand() {
    inputBuffer.clear();
    writeShutdown(FaultType.NONE, inputBuffer);
    MemcachedCommand c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue (c instanceof SHUTDOWN);
    writeShutdown(FaultType.INCOMPLETE, inputBuffer);
    c = CommandParser.parse(inputPtr, inputBuffer.position());
    assertTrue (c == null);
  }
}
