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

import com.carrotdata.memcarrot.support.IllegalFormatException;

import static com.carrotdata.cache.util.Utils.compareTo;
import static com.carrotdata.cache.util.Utils.strToLongDirect;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;

/**
 * Commands "incr" and "decr" are used to change data for some item in-place, incrementing or
 * decrementing it. The data for the item is treated as decimal representation of a 64-bit unsigned
 * integer. If the current data value does not conform to such a representation, the incr/decr
 * commands return an error (memcached <= 1.2.6 treated the bogus value as if it were 0, leading to
 * confusion). Also, the item must already exist for incr/decr to work; these commands won't pretend
 * that a non-existent key exists with value 0; instead, they will fail. The client sends the
 * command line: incr key value [noreply]\r\n or decr key value [noreply]\r\n - key is the key of
 * the item the client wishes to change - value is the amount by which the client wants to
 * increase/decrease the item. It is a decimal representation of a 64-bit unsigned integer. -
 * "noreply" optional parameter instructs the server to not send the reply. See the note in Storage
 * commands regarding malformed requests. The response will be one of: - "NOT_FOUND\r\n" to indicate
 * the item with this value was not found - value\r\n , where value is the new value of the item's
 * data, after the increment/decrement operation was carried out. Note that underflow in the "decr"
 * command is caught: if a client tries to decrease the value below 0, the new value will be 0.
 * Overflow in the "incr" command will wrap around the 64 bit mark. Note also that decrementing a
 * number such that it loses length isn't guaranteed to decrement its returned length. The number
 * MAY be space-padded at the end, but this is purely an implementation optimization, so you also
 * shouldn't rely on that.
 */
public class INCR extends AbstractMemcachedCommand {

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    try {
      int start = 0;
      int end = 0;

      this.keyPtr = inBuffer;

      end = nextTokenEnd(inBuffer, bufferSize);
      if (end <= 0) return false;
      // throwIfEquals(end, 0, "malformed request");

      end += start; // start == 0 ?
      this.keySize = end - start;

      start = nextTokenStart(inBuffer + end, bufferSize - end);
      if (start <= 0) return false;
      throwIfNotEquals(start, 1, "malformed request");

      start += end;

      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      if (end <= 0) return false;
      // throwIfEquals(end, 0, "malformed request");

      end += start;
      this.value = strToLongDirect(inBuffer + start, end - start);
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
        return false;// new IllegalFormatException("malformed request");
      }
      if (end > bufferSize - 2) {
        return false;// new IllegalFormatException("malformed request");
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
    }
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    try {
      long result = support.incr(keyPtr, keySize, value);
      if (!this.noreply) {
        if (result >= 0) {
          int size =
              com.carrotdata.cache.util.Utils.longToStrDirect(outBuffer, outBufferSize, result);
          outBuffer += size;
          // Add \r\n
          crlf(outBuffer);
          return size + 2;
        } else {
          UnsafeAccess.copy(NOT_FOUND, outBuffer, 11 /* NOT_FOUND\r\n length */);
          return 11;
        }
      }
      return 0;
    } catch (NumberFormatException e) {
      if (!this.noreply) {
        String msg = NUMBER_FORMAT_ERROR;
        UnsafeAccess.copy(msg.getBytes(), 0, outBuffer, msg.length());
        return msg.length();
      }
    } catch (IllegalArgumentException e) {
      if (!this.noreply) {
        String msg = CLIENT_ERROR + e.getMessage() + CRLF;
        UnsafeAccess.copy(msg.getBytes(), 0, outBuffer, msg.length());
        return msg.length();
      }
    }
    return 0;
  }

  @Override
  public int commandLength() {
    return 5;
  }
}
