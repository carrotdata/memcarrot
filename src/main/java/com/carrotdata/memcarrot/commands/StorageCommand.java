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
import static com.carrotdata.cache.util.Utils.compareTo;

import com.carrotdata.cache.util.UnsafeAccess;

/**
 * command_name key flags exptime bytes [noreply]\r\n cas key flags exptime bytes cas [noreply]\r\n
 * After sending the command line and the data block the client awaits the reply, which may be: -
 * "STORED\r\n", to indicate success. - "NOT_STORED\r\n" to indicate the data was not stored, but
 * not because of an error. This normally means that the condition for an "add" or a "replace"
 * command wasn't met. "EXISTS\r\n" to indicate that the item you are trying to store with a "cas"
 * command has been modified since you last fetched it. "NOT_FOUND\r\n" to indicate that the item
 * you are trying to store with a "cas" command did not exist.
 */

public abstract class StorageCommand extends AbstractMemcachedCommand {

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {

    try {
      int start = 0;
      int end = 0;

      this.keyPtr = inBuffer;

      end = nextTokenEnd(inBuffer, bufferSize);
      this.keySize = end - start;
      end += start;
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      // start = 0 and start > 1 format error
      if (start <= 0) {
        return false;
      }
      throwIfNotEquals(start, 1, "1 malformed request");

      start += end;

      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      if (end <= 0) {
        return false;
      }

      end += start;
      // unsigned 32 bit
      long v = strToLongDirect(inBuffer + start, end - start);
      if (v > 0xffffffffL || v < 0) {
        throw new IllegalFormatException("flags is not 32 - bit unsigned");
      }
      this.flags = v;

      start = nextTokenStart(inBuffer + end, bufferSize - end);
      if (start <= 0) {
        return false;
      }
      throwIfNotEquals(start, 1, "2 malformed request");

      start += end;

      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      if (end <= 0) {
        return false;
      }

      end += start;
      this.exptime = strToLongDirect(inBuffer + start, end - start);
      // Now get bytes
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      if (start <= 0) {
        return false;
      }
      throwIfNotEquals(start, 1, "3 malformed request");
      start += end;
      end = nextTokenEnd(inBuffer + start, bufferSize - start);
      if (end <= 0) {
        return false;
      }
      end += start;

      v = strToLongDirect(inBuffer + start, end - start);
      if (v > Integer.MAX_VALUE || v < 0) {
        throw new IllegalFormatException("illegal value size: " + v);
      }
      this.valSize = (int) v;

      if (isCAS) {
        start = nextTokenStart(inBuffer + end, bufferSize - end);
        if (start <= 0) {
          return false;
        }
        throwIfNotEquals(start, 1, "4 malformed request");

        start += end;

        end = nextTokenEnd(inBuffer + start, bufferSize - start);
        if (end <= 0) {
          return false;
        }
        end += start;
        this.cas = strToLongDirect(inBuffer + start, end - start);
      }

      // check noreply
      start = nextTokenStart(inBuffer + end, bufferSize - end);
      if (start < 0) return false;
      // start = 0 ?
      start += end;
      if (UnsafeAccess.toByte(inBuffer + start) == 'n'
          && bufferSize - start >= 9 /* noreply\r\n */) {
        if (compareTo(inBuffer + start, 7, NOREPLY, 7) == 0) {
          this.noreply = true;
          end = start + 7;
        } else {
          new IllegalFormatException("5 malformed request");
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
      this.valPtr = inBuffer + end;

      if (this.valSize > bufferSize - end - 2) {
        return false;
      }
      this.consumed = this.valSize + end + 2;
      return true;
    } catch (NumberFormatException e) {
      throw new IllegalFormatException("not a number");
    }
  }
}
