/*
 * Copyright (C) 2023-present Onecache, Inc. <p>This program is free software: you can redistribute
 * it and/or modify it under the terms of the Server Side Public License, version 1, as published by
 * MongoDB, Inc. <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the Server Side Public License for more details. <p>You should have received a copy
 * of the Server Side Public License along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package com.carrotdata.memcarrot.util;

import java.nio.ByteBuffer;

import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.support.IllegalFormatException;

public class Utils {
  public static boolean requestIsComplete(ByteBuffer buf) {
    return false;
  }

  /**
   * To upper case
   * @param addr address of a string byte array
   * @param len length of an array
   */
  public static long toUpperCase(long addr, int len) {
    int min = 'a'; // 97
    int max = 'z'; // 122
    for (int i = 0; i < len; i++) {
      byte v = UnsafeAccess.toByte(addr + i);
      if (v <= max && v >= min) {
        v -= 32;
        UnsafeAccess.putByte(addr + i, v);
      }
    }
    return addr;
  }

  public static long adjustExpirationTime(long exptime) {
    if (exptime < 0) return exptime; // expire request
    long maxRelativeTime = 60 * 60 * 24 * 30;
    if (exptime <= maxRelativeTime) {
      return System.currentTimeMillis() + exptime * 1000;
    }
    return exptime * 1000;
  }

  public static int nextTokenStart(long ptr, int limit) {
    int off = 0;
    while (UnsafeAccess.toByte(ptr + off) == (byte) ' ') {
      off++;
      if (off == limit) {
        return -1;// incomplete
      }
    }
    return off;
  }

  public static int nextTokenEnd(long ptr, int limit) {
    int off = 0;
    while (UnsafeAccess.toByte(ptr + off) != (byte) ' '
        && UnsafeAccess.toByte(ptr + off) != (byte) '\r') {
      off++;
      if (off == limit) {
        return -1;// incomplete
      }
    }
    return off;
  }
}
