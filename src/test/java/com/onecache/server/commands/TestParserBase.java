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

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Before;

import com.onecache.core.util.UnsafeAccess;

public class TestParserBase {
  public static enum FaultType {
    NONE,
    INCOMPLETE,
    WRONG_COMMAND,
    FLAGS_NOT_NUMBER,
    EXPTIME_NOT_NUMBER,
    CAS_NOT_NUMBER,
    NO_FLAGS,
    NO_EXPTIME,
    NO_CAS,
    CORRUPTED
  }
  
  protected ByteBuffer inputBuffer;
  
  long inputPtr;
  long outputPtr;
  
  int bufferSize = 1 << 16;
  
  @Before
  public void setUp() {
    inputBuffer = ByteBuffer.allocateDirect(bufferSize);
    inputPtr = UnsafeAccess.address(inputBuffer);
  }
  
  /**
   * set, add, replace, append, prepend, cas
   * @param cmd command
   * @param key key
   * @param value value
   * @param flags flags
   * @param exptime expiration time
   * @param cas CAS unique
   * @param withCas with CAS command
   * @param noreply noreply: yes, no
   * @param fault fault type
   * @param inputBuffer buffer
   */
  public static void writeStorageCommand(byte[] cmd, String key, byte[] value, int flags, long exptime, 
      long cas, boolean withCas, boolean noreply, FaultType fault, ByteBuffer inputBuffer) {
    if (fault == FaultType.NONE || fault == FaultType.INCOMPLETE) {
      inputBuffer.put(cmd);
      space(inputBuffer);
      inputBuffer.put(key.getBytes());
      space(inputBuffer);
      String s = Integer.toString(flags);
      inputBuffer.put(s.getBytes());
      space(inputBuffer);
      s = Long.toString(exptime);
      inputBuffer.put(s.getBytes());
      space(inputBuffer);
      s = Integer.toString(value.length);
      inputBuffer.put(s.getBytes());

      if (withCas) {
        space(inputBuffer);
        s = Long.toString(cas);
        inputBuffer.put(s.getBytes());
      }
      if (noreply) {
        space(inputBuffer);
        inputBuffer.put("noreply".getBytes());
      }
      crlf(inputBuffer);
      inputBuffer.put(value);
      crlf(inputBuffer);
    }
    
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }
  
  /**
   * get, gets, gat, gats
   * @param cmd command
   * @param keys keys
   * @param expTime expiration time
   * @param withExpire with expiration time
   * @param fault fault type
   * @param inputBuffer buffer 
   */
  public static void writeRetrievalCommand(byte[] cmd, String[] keys, long expTime,  boolean withExpire, FaultType fault, ByteBuffer inputBuffer) {
    if (fault == FaultType.NONE || fault == FaultType.INCOMPLETE) {
      inputBuffer.put(cmd);
      space(inputBuffer);
      if (withExpire) {
        String s = Long.toString(expTime);
        inputBuffer.put(s.getBytes());
        space(inputBuffer);
      }
      for (int i = 0; i < keys.length; i++) {
        inputBuffer.put(keys[i].getBytes());
        space(inputBuffer);
      }
      crlf(inputBuffer);
    }
    
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }
  
  public static void writeDeleteCommand(String key, boolean noreply, FaultType fault, ByteBuffer inputBuffer) {
    byte[] cmd = "delete".getBytes();
    if (fault == FaultType.NONE || fault == FaultType.INCOMPLETE) {
      inputBuffer.put(cmd);
      space(inputBuffer);
      inputBuffer.put(key.getBytes());      
      if (noreply) {
        space(inputBuffer);
        inputBuffer.put("noreply".getBytes());
      }
      crlf(inputBuffer);
    }
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }
  
  public static void writeTouchCommand(String key, long exptime, 
      boolean noreply, FaultType fault, ByteBuffer inputBuffer) {
    byte[] cmd = "touch".getBytes();
    if (fault == FaultType.NONE || fault == FaultType.INCOMPLETE) {
      inputBuffer.put(cmd);
      space(inputBuffer);
      inputBuffer.put(key.getBytes());
      space(inputBuffer);
      String s = Long.toString(exptime);
      inputBuffer.put(s.getBytes());
      if (noreply) {
        space(inputBuffer);
        inputBuffer.put("noreply".getBytes());
      }
      crlf(inputBuffer);
    }
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }
  
  public static void writeIncrCommand(String key, long v, 
      boolean noreply, FaultType fault, ByteBuffer inputBuffer) {
    byte[] cmd = "incr".getBytes();
    if (fault == FaultType.NONE || fault == FaultType.INCOMPLETE) {
      inputBuffer.put(cmd);
      space(inputBuffer);
      inputBuffer.put(key.getBytes());
      space(inputBuffer);
      String s = Long.toString(v);
      inputBuffer.put(s.getBytes());
      if (noreply) {
        space(inputBuffer);
        inputBuffer.put("noreply".getBytes());
      }
      crlf(inputBuffer);
    }
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }
  
  public static void writeDecrCommand(String key, long v, 
      boolean noreply, FaultType fault, ByteBuffer inputBuffer) {
    byte[] cmd = "decr".getBytes();
    if (fault == FaultType.NONE || fault == FaultType.INCOMPLETE) {
      inputBuffer.put(cmd);
      space(inputBuffer);
      inputBuffer.put(key.getBytes());
      space(inputBuffer);
      String s = Long.toString(v);
      inputBuffer.put(s.getBytes());
      if (noreply) {
        space(inputBuffer);
        inputBuffer.put("noreply".getBytes());
      }
      crlf(inputBuffer);
    }
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }
  
  public static void writeShutdown(FaultType fault, ByteBuffer b) {
    byte[] cmd = "shutdown\r\n".getBytes();
    b.put(cmd);
    if (fault == FaultType.INCOMPLETE);
    makeIncomplete(b);
  }
  
  /**
   *  Utility methods
   */
  static void space(ByteBuffer b) {
    b.put((byte) ' ');
  }
  
  static void crlf(ByteBuffer b) {
    b.put((byte) '\r');
    b.put((byte) '\n');
  }
  
  static void makeIncomplete(ByteBuffer inputBuffer) {
    Random r = new Random();
    int pos = inputBuffer.position();
    pos = 1 + r.nextInt(pos - 1);
    inputBuffer.position(pos);
  }
}
