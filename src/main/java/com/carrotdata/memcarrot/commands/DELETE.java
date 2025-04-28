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

import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
import com.carrotdata.memcarrot.support.IllegalFormatException;

import static com.carrotdata.cache.util.Utils.compareTo;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.support.Memcached.OpResult;
import com.carrotdata.cache.util.UnsafeAccess;

/**
 * The command "delete" allows for explicit deletion of items: delete key [noreply]\r\n - key is the
 * key of the item the client wishes the server to delete - "noreply" optional parameter instructs
 * the server to not send the reply. See the note in Storage commands regarding malformed requests.
 * The response line to this command can be one of: - "DELETED\r\n" to indicate success -
 * "NOT_FOUND\r\n" to indicate that the item with this key was not found. See the "flush_all"
 * command below for immediate invalidation of all existing items. *
 */
public class DELETE extends AbstractMemcachedCommand {

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    int start = 0;
    int end = 0;

    this.keyPtr = inBuffer;

    end = nextTokenEnd(inBuffer, bufferSize);
    if (end <= 0) return false;
    end += start; // start should be 0?

    this.keySize = end - start;

    start = nextTokenStart(inBuffer + end, bufferSize - end);
    if (start < 0) return false;
    start += end;
    // start = 0 ?
    if (UnsafeAccess.toByte(inBuffer + start) == 'n' && bufferSize - start >= 9 /* noreply\r\n */) {
      if (compareTo(inBuffer + start, 7, NOREPLY, 7) == 0) {
        this.noreply = true;
        end = start + 7;
      } else {
        new IllegalFormatException("malformed request");
      }
    } else if (UnsafeAccess.toByte(inBuffer + start) == 'n') {
      return false;
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
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
    OpResult result = support.delete(keyPtr, keySize);
    if (!this.noreply) {
      if (result == OpResult.DELETED) {
        UnsafeAccess.copy(DELETED, outBuffer, 9 /* TOUCHED\r\n length */);
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
    return 7;
  }

}
