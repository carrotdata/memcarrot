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

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;

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
public class DECR extends INCR {

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
    try {
      long result = support.decr(keyPtr, keySize, value);
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

}
