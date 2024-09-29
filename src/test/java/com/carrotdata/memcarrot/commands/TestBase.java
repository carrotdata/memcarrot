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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Before;

import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.support.IllegalFormatException;
import com.carrotdata.cache.support.Memcached.Record;
import com.carrotdata.cache.support.Memcached.OpResult;
import static com.carrotdata.cache.util.Utils.compareTo;
import static com.carrotdata.cache.util.Utils.strToLongDirect;
import static com.carrotdata.memcarrot.util.Utils.nextTokenEnd;
import static com.carrotdata.memcarrot.util.Utils.nextTokenStart;

public class TestBase {

  public static class KeyRecord {

    byte[] key;
    Record record;

    private KeyRecord(long keyPtr, int keySize, Record r) {
      this.key = new byte[keySize];
      UnsafeAccess.copy(keyPtr, key, 0, keySize);
      this.record = new Record();
      this.record.cas = r.cas;
      this.record.error = r.error;
      this.record.flags = r.flags;
      this.record.expire = r.expire;
      this.record.value = new byte[r.size];
      System.arraycopy(r.value, r.offset, this.record.value, 0, r.size);
      this.record.size = r.size;
      this.record.offset = r.offset;
    }

    static KeyRecord of(long keyPtr, int keySize, Record r) {
      return new KeyRecord(keyPtr, keySize, r);
    }
  }

  static final byte[] STORED = "STORED\r\n".getBytes();
  static final byte[] NOT_STORED = "NOT_STORED\r\n".getBytes();
  static final byte[] EXISTS = "EXISTS\r\n".getBytes();
  static final byte[] NOT_FOUND = "NOT_FOUND\r\n".getBytes();
  static final byte[] DELETED = "DELETED\r\n".getBytes();
  static final byte[] TOUCHED = "TOUCHED\r\n".getBytes();
  static final byte[] ERROR = "ERROR\r\n".getBytes();
  static final byte[] END = "END\r\n".getBytes();

  public static enum FaultType {
    NONE, INCOMPLETE, WRONG_COMMAND, FLAGS_NOT_NUMBER, VALUE_NOT_NUMBER, EXPTIME_NOT_NUMBER,
    CAS_NOT_NUMBER, NO_FLAGS, NO_EXPTIME, NO_CAS, CORRUPTED
  }

  protected ByteBuffer inputBuffer;
  protected ByteBuffer outputBuffer;

  long inputPtr;
  long outputPtr;

  int bufferSize = 1 << 16;

  @Before
  public void setUp() {
    inputBuffer = ByteBuffer.allocateDirect(bufferSize);
    inputPtr = UnsafeAccess.address(inputBuffer);
    outputBuffer = ByteBuffer.allocateDirect(bufferSize);
    outputPtr = UnsafeAccess.address(outputBuffer);
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
  public static void writeStorageCommand(byte[] cmd, String key, byte[] value, int flags,
      long exptime, long cas, boolean withCas, boolean noreply, FaultType fault,
      ByteBuffer inputBuffer) {
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
   * Read storage command response
   * @param buf memory buffer address
   * @param size size
   * @return response code
   */
  public static OpResult readStorageCommandResponse(long buf, int size) {
    // STORED, NOT_STORED, EXISTS, NOT_FOUND, DELETED, ERROR;
    if (size == 8) {
      if (compareTo(STORED, 0, size, buf, size) == 0) {
        return OpResult.STORED;
      } else if (compareTo(EXISTS, 0, size, buf, size) == 0) {
        return OpResult.EXISTS;
      } else {
        org.junit.Assert.fail("Unrecognized storage command response");
      }
    } else if (size == 9) {
      if (compareTo(DELETED, 0, size, buf, size) == 0) {
        return OpResult.DELETED;
      } else if (compareTo(TOUCHED, 0, size, buf, size) == 0) {
        return OpResult.TOUCHED;
      } else {
        org.junit.Assert.fail("Unrecognized storage command response");
      }
    } else if (size == 11) {
      if (compareTo(NOT_FOUND, 0, size, buf, size) == 0) {
        return OpResult.NOT_FOUND;
      } else {
        org.junit.Assert.fail("Unrecognized storage command response");
      }
    } else if (size == 12) {
      if (compareTo(NOT_STORED, 0, size, buf, size) == 0) {
        return OpResult.NOT_STORED;
      } else {
        org.junit.Assert.fail("Unrecognized storage command response");
      }
    } else if (size == 7) {
      if (compareTo(ERROR, 0, size, buf, size) == 0) {
        return OpResult.ERROR;
      } else {
        org.junit.Assert.fail("Unrecognized storage command response");
      }
    } else {
      org.junit.Assert.fail("Unrecognized storage command response");
    }
    return null;
  }

  /**
   * Reads touch command response
   * @param buf memory buffer address
   * @param size size
   * @return response code
   */
  public static OpResult readTouchCommandResponse(long buf, int size) {
    return readStorageCommandResponse(buf, size);
  }

  /**
   * Reads delete command response
   * @param buf memory buffer address
   * @param size size
   * @return response code
   */
  public static OpResult readDeleteCommandResponse(long buf, int size) {
    return readStorageCommandResponse(buf, size);
  }

  /**
   * Reads incr decr response
   * @param buf memory buffer address
   * @param size size
   * @return response code
   */
  public static Object readIncrDecrCommandResponse(long buf, int size) {
    if (NOT_FOUND.length == size) {
      if (compareTo(NOT_FOUND, 0, size, buf, size) == 0) {
        return OpResult.NOT_FOUND;
      }
    }
    // Try long value
    try {
      long value = com.carrotdata.cache.util.Utils.strToLongDirect(buf, size - 2);
      return Long.valueOf(value);
    } catch (NumberFormatException e) {
      return new String(com.carrotdata.cache.util.Utils.toBytes(buf, size));
    }
  }

  public static List<KeyRecord> readRetrievalCommandResponse(long buf, int size, boolean withCAS) {
    List<KeyRecord> result = new ArrayList<KeyRecord>();
    if (size == 5) {
      if (compareTo(END, 0, size, buf, size) == 0) {
        return result;
      } else {
        org.junit.Assert.fail("Unrecognized retrieval command response");
      }
    }
    int off = 0;
    while (off < size - 5) {
      int $off = readKeyRecordInto(result, buf + off, size - off, withCAS);
      off += $off;
    }
    // last 5
    if (compareTo(END, 0, 5, buf + off, 5) == 0) {
      return result;
    } else {
      org.junit.Assert.fail("Unrecognized retrieval command response");
    }
    return null;
  }

  private static int readKeyRecordInto(List<KeyRecord> result, long buf, int size,
      boolean withCAS) {
    // VALUE <key> <flags> <bytes> [<cas unique>]\r\n
    // <data block>\r\n
    try {
      long keyPtr = 0;
      int keySize = 0;
      int flags = 0;
      int valSize = 0;
      long valPtr = 0;
      long cas = 0;

      int start = 0;
      int end = 0;

      buf += 6; // skip 'VALUE '
      size -= 6;

      keyPtr = buf;

      end = nextTokenEnd(buf, size);
      keySize = end - start;

      start = nextTokenStart(buf + end, size - end);
      start += end;

      end = nextTokenEnd(buf + start, size);
      end += start;
      // unsigned 32 bit
      long v = strToLongDirect(buf + start, end - start);
      if (v > 0xffffffffL || v < 0) {
        throw new IllegalFormatException("flags is not 32 - bit unsigned");
      }
      flags = (int) v;
      // Now get bytes
      start = nextTokenStart(buf + end, size - end);
      start += end;
      end = nextTokenEnd(buf + start, size - start);
      end += start;

      v = strToLongDirect(buf + start, end - start);
      if (v > Integer.MAX_VALUE || v < 0) {
        throw new IllegalFormatException("illegal value size: " + v);
      }
      valSize = (int) v;

      if (withCAS) {
        start = nextTokenStart(buf + end, size - end);
        start += end;
        end = nextTokenEnd(buf + start, size - start);
        end += start;
        cas = strToLongDirect(buf + start, end - start);
      }
      // skip \r\n
      if (UnsafeAccess.toByte(buf + end) != '\r') {
        throw new IllegalFormatException("'\r\n' was expected");
      }

      end++;
      if (UnsafeAccess.toByte(buf + end) != '\n') {
        throw new IllegalFormatException("'\r\n' was expected");
      }
      end++;
      valPtr = buf + end;

      Record r = new Record();
      r.cas = cas;
      r.error = false;
      r.flags = flags;
      r.value = new byte[valSize];
      r.offset = 0;
      r.size = valSize;
      UnsafeAccess.copy(valPtr, r.value, 0, valSize);
      result.add(KeyRecord.of(keyPtr, keySize, r));

      return 6 + end + valSize + 2;
    } catch (NumberFormatException e) {
      throw new IllegalFormatException("not a number");
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
  public static void writeRetrievalCommand(byte[] cmd, String[] keys, long expTime,
      boolean withExpire, FaultType fault, ByteBuffer inputBuffer) {
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
        if (i < keys.length - 1) {
          space(inputBuffer);
        }
      }
      crlf(inputBuffer);
    }

    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }

  public static void writeDeleteCommand(String key, boolean noreply, FaultType fault,
      ByteBuffer inputBuffer) {
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

  public static void writeTouchCommand(String key, long exptime, boolean noreply, FaultType fault,
      ByteBuffer inputBuffer) {
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

  public static void writeIncrCommand(String key, long v, boolean noreply, FaultType fault,
      ByteBuffer inputBuffer) {
    byte[] cmd = "incr".getBytes();
    inputBuffer.put(cmd);
    space(inputBuffer);
    inputBuffer.put(key.getBytes());
    space(inputBuffer);
    String s = fault != FaultType.VALUE_NOT_NUMBER ? Long.toString(v) : "some";
    inputBuffer.put(s.getBytes());
    if (noreply) {
      space(inputBuffer);
      inputBuffer.put("noreply".getBytes());
    }
    crlf(inputBuffer);

    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }

  public static void writeDecrCommand(String key, long v, boolean noreply, FaultType fault,
      ByteBuffer inputBuffer) {
    byte[] cmd = "decr".getBytes();
    inputBuffer.put(cmd);
    space(inputBuffer);
    inputBuffer.put(key.getBytes());
    space(inputBuffer);
    String s = fault != FaultType.VALUE_NOT_NUMBER ? Long.toString(v) : "some";
    inputBuffer.put(s.getBytes());
    if (noreply) {
      space(inputBuffer);
      inputBuffer.put("noreply".getBytes());
    }
    crlf(inputBuffer);

    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(inputBuffer);
    }
  }

  public static void writeShutdownCommand(FaultType fault, ByteBuffer b) {
    byte[] cmd = "shutdown\r\n".getBytes();
    b.put(cmd);
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(b);
    }
  }

  public static void writeVersionCommand(FaultType fault, ByteBuffer b) {
    byte[] cmd = "version\r\n".getBytes();
    b.put(cmd);
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(b);
    }
  }
  
  public static void writeStatsCommand(FaultType fault, ByteBuffer b) {
    byte[] cmd = "stats\r\n".getBytes();
    b.put(cmd);
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(b);
    }
  }
  
  public static void writeFlushAllCommand(int delay, boolean noreply, FaultType fault, ByteBuffer b) {
    byte[] cmd = "flush_all".getBytes();
    b.put(cmd);
    String s = fault != FaultType.VALUE_NOT_NUMBER ? Long.toString(delay) : "some";
    if (delay > 0 || fault == FaultType.VALUE_NOT_NUMBER) {
      space(b);
      b.put(s.getBytes());
    }
    if (noreply) {
      space(b);
      b.put("noreply".getBytes());
    }
    crlf(b);
    if (fault == FaultType.INCOMPLETE) {
      makeIncomplete(b);
    }
  }
  
  /**
   * Utility methods
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
