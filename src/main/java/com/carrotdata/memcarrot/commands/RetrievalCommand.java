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
package com.carrotdata.memcarrot.commands;

import com.carrotdata.memcarrot.support.IllegalFormatException;
import static com.carrotdata.cache.util.Utils.strToLongDirect;

import com.carrotdata.cache.util.UnsafeAccess;
import static com.carrotdata.cache.util.UnsafeAccess.toByte;

/**
 * This class covers 'get', 'gets', 'gat' and 'gats' commands
 */

public abstract class RetrievalCommand extends AbstractMemcachedCommand {

  static final long VALUE = UnsafeAccess.allocAndCopy("VALUE", 0, 5);
  static final long END = UnsafeAccess.allocAndCopy("END\r\n", 0, 5);
  public static long parseTime = 0;
  boolean isTouch;
  long[] keys;
  int[] keySizes;

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    long t1 = System.nanoTime();

    try {
      int count = calculateNumberOfKeys(inBuffer, bufferSize);
      if (count < 0) return false;
      if (isTouch) {
        count--; // we counted 'expire' also
      }
      // TODO optimize
      keys = new long[count];
      keySizes = new int[count];

      int start = 0;
      int end = 0;
      if (isTouch) {
        start = nextTokenStart(inBuffer + end, bufferSize - end);
        if (start < 0) return false;
        // start should be 0
        start += end;

        end = nextTokenEnd(inBuffer + start, bufferSize - start);
        if (end < 0) return false;
        throwIfEquals(end, 0, "malformed request");
        end += start;
        this.exptime = strToLongDirect(inBuffer + start, end - start);
      }
      for (int i = 0; i < count; i++) {
        start = nextTokenStart(inBuffer + end, bufferSize - end);
        if (start < 0) return false;
        if (i != 0) {
          throwIfNotEquals(start, 1, "malformed request");
        }
        start += end;

        end = nextTokenEnd(inBuffer + start, bufferSize - start);
        if (end < 0) return false;
        throwIfEquals(end, 0, "malformed request");
        end += start;
        keys[i] = inBuffer + start;
        keySizes[i] = end - start;
      }
      if (end > bufferSize - 2) return false;
      // skip \r\n
      if (UnsafeAccess.toByte(inBuffer + end) != '\r') {
        throw new IllegalFormatException("'\r\n' was expected");
      }
      end++;

      if (UnsafeAccess.toByte(inBuffer + end) != '\n') {
        throw new IllegalFormatException("'\r\n' was expected");
      }
      end++;
      this.consumed = end;
      return true;
    } catch (NumberFormatException e) {
      throw new IllegalFormatException("not a number");
    } finally {
      parseTime += System.nanoTime() - t1;
    }
  }

  public final boolean isMemorySafe(long memptr, int memsize) {
    for (int i = 0; i < keys.length; i++) {
      boolean safe = keys[i] > 0 && keySizes[i] > 0;
      safe = safe && (keys[i] > memptr && (keys[i] + keySizes[i]) < memptr + memsize);
      if (!safe) return false;
    }
    return true;
  }

  private int calculateNumberOfKeys(long inBuffer, int inBufferSize) {
    int count = 0;
    int start = 0;
    int end = 0;
    while (toByte(inBuffer + end) != '\r' && end < inBufferSize) {
      start = nextTokenStart(inBuffer + end, inBufferSize - end);
      if (start < 0) return -1;
      start += end;
      end = nextTokenEnd(inBuffer + start, inBufferSize - start);
      if (end < 0) return -1;
      end += start;
      if (end == start) break;
      count++;
    }
    return count;
  }
}
