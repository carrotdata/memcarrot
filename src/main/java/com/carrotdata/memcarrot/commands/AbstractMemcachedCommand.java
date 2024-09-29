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
