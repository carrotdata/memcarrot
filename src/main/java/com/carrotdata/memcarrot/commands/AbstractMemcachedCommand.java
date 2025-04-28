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

import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.support.IllegalFormatException;

public abstract class AbstractMemcachedCommand implements MemcachedCommand {

  // key address
  long keyPtr;
  // key size
  int keySize;

  // value address
  long valPtr;
  // value size
  int valSize;

  // TODO: unsigned 32bit support
  long flags; // unsigned 32 bits
  long exptime;
  long cas;
  long value; // for INCR/DECR commands
  boolean noreply;
  boolean isCAS;

  int consumed;

  @Override
  public int inputConsumed() {
    if (consumed == 0) return 0;
    return consumed + commandLength();
  }

  protected void crlf(long ptr) {
    UnsafeAccess.putByte(ptr, (byte) '\r');
    UnsafeAccess.putByte(ptr + 1, (byte) '\n');
  }


  public boolean isMemorySafe(long memptr, int memsize) {
    boolean safe = this.keyPtr > 0 && this.keySize > 0;
    safe = safe && (this.keyPtr > memptr) && (this.keyPtr + this.keySize < memptr + memsize);
    if (this instanceof StorageCommand) {
      safe = safe && (this.valPtr > 0 && this.valSize >= 0 /* special case for null value*/);
      safe = safe && (this.valPtr > memptr && this.valPtr + this.valSize < memptr + memsize);
      safe = safe && (this.valPtr > this.keyPtr + this.keySize);
    }
    return safe;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ka=" + keyPtr);
    sb.append(" ks=" + keySize);
    sb.append(" va=" + valPtr);
    sb.append(" vs=" + valSize);
    sb.append(" flags=" + flags);
    sb.append(" exptime==" + exptime);
    sb.append(" vs=" + valSize);
    sb.append(" cas=" + cas);
    sb.append(" noreply=" + noreply);
    return sb.toString();
  }
}
