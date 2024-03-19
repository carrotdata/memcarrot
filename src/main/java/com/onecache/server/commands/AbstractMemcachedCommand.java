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

import com.onecache.core.util.UnsafeAccess;
import com.onecache.server.support.IllegalFormatException;

public abstract class AbstractMemcachedCommand implements MemcachedCommand {

  long keyPtr;
  int keySize;
  long valPtr;
  int valSize;
  //TODO: unsigned 32bit support 
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

  protected final void throwIfNotEquals(long n, int v, String msg) throws IllegalFormatException {
    if (n != v) throw new IllegalFormatException(msg);
  }

  protected final void throwIfEquals(long n, int v, String msg) throws IllegalFormatException {
    if (n == v) throw new IllegalFormatException(msg);
  }
}
