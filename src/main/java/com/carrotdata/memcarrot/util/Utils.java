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
package com.carrotdata.memcarrot.util;

import java.nio.ByteBuffer;

import com.carrotdata.cache.util.UnsafeAccess;

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
    if (limit == 0) return -1;
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
    if (limit == 0) return -1;
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
