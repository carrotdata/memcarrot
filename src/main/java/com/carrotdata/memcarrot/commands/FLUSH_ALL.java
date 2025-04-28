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
import static com.carrotdata.cache.util.Utils.strToLongDirect;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;


public class FLUSH_ALL implements MemcachedCommand {
  
  boolean noreply = false;
  int consumed;
  int delay;
  
  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    try {
      int start = 0;
      int end = 0;
      boolean withDelay = false;
      end = nextTokenEnd(inBuffer, bufferSize);
      if (end > 0) {
        end += start; // start == 0 ?
        // can be noreply or delay
        if (UnsafeAccess.toByte(inBuffer + start) != (byte) 'n') {
          this.delay = (int) strToLongDirect(inBuffer + start, end - start);
          if (this.delay < 0) {
            throw new IllegalFormatException("delay is negative");
          }
          start = nextTokenStart(inBuffer + end, bufferSize - end);
          if (start > 0) {
            start += end;
          }
          withDelay = true;
        }
        // check noreply
        //start = nextTokenStart(inBuffer + end, bufferSize - end);
        if (start > 0 || !withDelay) {
          //start += end;
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
        }
      } else if (end == -1) {
        // incomplete
        return false;
      }
      
      if (end > bufferSize - 2) {
        return false;
      } else if (end == 0) {
        consumed = -1;
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
      this.consumed += end;
      return true;
    } catch (NumberFormatException e) {
      throw new IllegalFormatException("delay is not a number");
    }
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize,
      OutputConsumer consumer) {
    support.flushAll(delay);
    if (!this.noreply) {
      // Add \r\n
      ok(outBuffer);
      return 4;
    }
    return 0;
  }

  @Override
  public int commandLength() {
    return 10;
  }

  @Override
  public int inputConsumed() {
    return consumed + commandLength();
  }
}
