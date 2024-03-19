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

import com.onecache.server.support.IllegalFormatException;
import static com.onecache.core.util.Utils.strToLongDirect;


import com.onecache.core.util.UnsafeAccess;
import static com.onecache.core.util.UnsafeAccess.toByte;


/**
 * This class covers 'get', 'gets', 'gat' and 'gats' commands
 *
 */

public abstract class RetrievalCommand extends AbstractMemcachedCommand{
 
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
      if (isTouch) {
        count --; // we counted 'expire' also
      }
      // TODO optimize
      keys = new long[count];
      keySizes = new int[count];

      int start = 0;
      int end = 0;
      if (isTouch) {
        start = nextTokenStart(inBuffer + end, bufferSize - end);
        // start should be 0
        start += end;

        end = nextTokenEnd(inBuffer + start, bufferSize - start);
        throwIfEquals(end, 0, "malformed request");
        end += start;
        this.exptime = strToLongDirect(inBuffer + start, end - start);
      }
      for (int i = 0; i < count; i++) {
        start = nextTokenStart(inBuffer + end, bufferSize - end);
        if (i != 0) {
          throwIfNotEquals(start, 1, "malformed request");
        }
        start += end;

        end = nextTokenEnd(inBuffer + start, bufferSize - start);
        throwIfEquals(end, 0, "malformed request");
        end += start;
        keys[i] = inBuffer + start;
        keySizes[i] = end - start;
      }
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      // not sure if space here
      start += end;
      if (start != bufferSize - 2) {
        throw new IllegalFormatException("malformed request");
      }
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
    } finally{
      parseTime += System.nanoTime() - t1;
    }
  }
  
  private int calculateNumberOfKeys(long inBuffer, int inBufferSize) {
    int count = 0;
    int start = 0;
    int end = 0;
    while(toByte(inBuffer + end) != '\r' && end < inBufferSize) {
      start = nextTokenStart(inBuffer + end, inBufferSize - end);
      start += end;
      end = nextTokenEnd(inBuffer + start, inBufferSize- start);
      end += start;
      if (end == start) break;
      count++;
    }
    return count;
  }
}
