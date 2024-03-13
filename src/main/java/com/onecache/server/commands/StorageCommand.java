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
import static com.onecache.core.util.Utils.compareTo;


import com.onecache.core.util.UnsafeAccess;

/**
 * 
 * command_name key flags exptime bytes [noreply]\r\n
 * cas key flags exptime bytes cas [noreply]\r\n
 *
 * After sending the command line and the data block the client awaits
 * the reply, which may be:

 * - "STORED\r\n", to indicate success.

 * - "NOT_STORED\r\n" to indicate the data was not stored, but not
 * because of an error. This normally means that the
 * condition for an "add" or a "replace" command wasn't met.

 * "EXISTS\r\n" to indicate that the item you are trying to store with
 * a "cas" command has been modified since you last fetched it.

 * "NOT_FOUND\r\n" to indicate that the item you are trying to store
 * with a "cas" command did not exist.
 *
 */

public abstract class StorageCommand extends AbstractMemcachedCommand{
 
  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    try {
      int start = 0;
      int end = 0;

      this.keyPtr = inBuffer;

      end = nextTokenEnd(inBuffer, bufferSize);
      this.keySize = end - start;

      start = nextTokenStart(inBuffer + end, bufferSize - end);
      // start = 0 and start > 1 format error
      throwIfNotEquals(start, 1, "malformed request");

      start += end;

      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      throwIfEquals(end, 0, "malformed request");

      end += start;
      // unsigned 32 bit
      long v = strToLongDirect(inBuffer + start, end - start);
      if (v > 0xffffffffL || v < 0) {
        throw new IllegalFormatException("flags is not 32 - bit unsigned");
      }
      this.flags = v;

      start = nextTokenStart(inBuffer + end, bufferSize - end);
      throwIfNotEquals(start, 1, "malformed request");

      start += end;

      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      throwIfEquals(end, 0, "malformed request");

      end += start;
      this.exptime = strToLongDirect(inBuffer + start, end - start);
      // Now get bytes 
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      throwIfNotEquals(start, 1, "malformed request");
      start += end;
      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      throwIfEquals(end, 0, "malformed request");
      end += start;

      v = strToLongDirect(inBuffer + start, end - start);
      if (v > Integer.MAX_VALUE || v < 0) {
        throw new IllegalFormatException("illegal value size: " + v);
      }
      this.valSize = (int) v;

      if (isCAS) {
        start = nextTokenStart(inBuffer + end, bufferSize - end);
        throwIfNotEquals(start, 1, "malformed request");

        start += end;

        end = nextTokenEnd(inBuffer + start, bufferSize - start);
        throwIfEquals(end, 0, "malformed request");
        end += start;
        this.cas = strToLongDirect(inBuffer + start, end - start);
      }

      // check noreply
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      // start = 0 ?
      start += end;
      if (UnsafeAccess.toByte(inBuffer + start) == 'n'
          && bufferSize - start >= 9 /* noreply\r\n */) {
        if (compareTo(inBuffer + start, 7, NOREPLY, 7) == 0) {
          this.noreply = true;
          end += start + 7;
        } else {
          new IllegalFormatException("malformed request");
        }
      } else if (UnsafeAccess.toByte(inBuffer + start) == 'n') {
        new IllegalFormatException("malformed request");
      }
      if (end < bufferSize - 2) {
        new IllegalFormatException("malformed request");
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
      this.valPtr = inBuffer + end;
      if (this.valSize > bufferSize - end - 2) {
        return false;
      }
      if (this.valSize < bufferSize - end - 2) {
        new IllegalFormatException("malformed request");
      }
      return true;
    } catch (NumberFormatException e) {
      throw new IllegalFormatException("not a number");
    }
  }
}
