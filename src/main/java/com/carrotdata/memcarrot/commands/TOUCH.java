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

import static com.carrotdata.cache.util.Utils.compareTo;
import static com.carrotdata.cache.util.Utils.strToLongDirect;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
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
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
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
