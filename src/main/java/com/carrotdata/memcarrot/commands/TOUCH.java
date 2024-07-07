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

import static com.carrotdata.cache.util.Utils.compareTo;
import static com.carrotdata.cache.util.Utils.strToLongDirect;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.support.IllegalFormatException;

/**
 * The "touch" command is used to update the expiration time of an existing item without fetching
 * it. touch key exptime [noreply]\r\n - key is the key of the item the client wishes the server to
 * touch - exptime is expiration time. Works the same as with the update commands (set/add/etc).
 * This replaces the existing expiration time. If an existing item were to expire in 10 seconds, but
 * then was touched with an expiration time of "20", the item would then expire in 20 seconds. -
 * "noreply" optional parameter instructs the server to not send the reply. See the note in Storage
 * commands regarding malformed requests. The response line to this command can be one of: -
 * "TOUCHED\r\n" to indicate success - "NOT_FOUND\r\n" to indicate that the item with this key was
 * not found.
 */
public class TOUCH extends AbstractMemcachedCommand {

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    try {
      int start = 0;
      int end = 0;

      this.keyPtr = inBuffer;

      end = nextTokenEnd(inBuffer, bufferSize);
      if (end <= 0) {
        return false;
      }
      end += start; // start == 0?
      this.keySize = end - start;

      start = nextTokenStart(inBuffer + end, bufferSize - end);
      if (start <= 0) return false;
      throwIfNotEquals(start, 1, "malformed request");

      start += end;

      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      if (end <= 0) {
        return false;
      }
      end += start;
      this.exptime = strToLongDirect(inBuffer + start, end - start);
      // check noreply
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      if (start < 0) return false;
      start += end;
      // start = 0 ?
      if (UnsafeAccess.toByte(inBuffer + start) == 'n'
          && bufferSize - start >= 9 /* noreply\r\n */) {
        if (compareTo(inBuffer + start, 7, NOREPLY, 7) == 0) {
          this.noreply = true;
          end = start + 7;
        } else {
          new IllegalFormatException("malformed request");
        }
      } else if (UnsafeAccess.toByte(inBuffer + start) == 'n') {
        return false;
      }
      if (end > bufferSize - 2) {
        return false;
      }
      // skip \r\n
      if (UnsafeAccess.toByte(inBuffer + end) != '\r') {
        throw new IllegalFormatException("'\\r\\n' was expected");
      }
      end++;
      if (UnsafeAccess.toByte(inBuffer + end) != '\n') {
        throw new IllegalFormatException("'\\r\\n' was expected");
      }
      end++;
      this.consumed = end;
      return true;
    } catch (NumberFormatException e) {
      throw new IllegalFormatException("not a number");
    }
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    long result = support.touch(keyPtr, keySize, exptime);
    if (!this.noreply) {
      if (result >= 0) {
        UnsafeAccess.copy(TOUCHED, outBuffer, 9 /* TOUCHED\r\n length */);
        return 9;
      } else {
        UnsafeAccess.copy(NOT_FOUND, outBuffer, 11 /* NOT_FOUND\r\n length */);
        return 11;
      }
    }
    return 0;
  }

  @Override
  public int commandLength() {
    return 6;
  }

}
