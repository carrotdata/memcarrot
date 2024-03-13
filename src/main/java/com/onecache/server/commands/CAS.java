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

import com.onecache.core.support.Memcached;
import com.onecache.core.support.Memcached.OpResult;
import com.onecache.core.util.UnsafeAccess;

public class CAS extends StorageCommand {

  public CAS() {
    this.isCAS = true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    OpResult result = support.cas(keyPtr, keySize, valPtr, valSize, (int) flags, exptime, cas);
    if (!this.noreply) {
      switch (result) {
        case EXISTS:
          UnsafeAccess.copy(EXISTS, outBuffer, 8 /* EXISTS\r\n length */);
          return 8;
        case NOT_FOUND:
          UnsafeAccess.copy(NOT_FOUND, outBuffer, 11 /* NOT_FOUND\r\n length */);
          return 11;
        case NOT_STORED:
          UnsafeAccess.copy(NOT_STORED, outBuffer, 12 /* NOT_STORED\r\n length */);
          return 12;
        default:
          UnsafeAccess.copy(STORED, outBuffer, 8 /* STORED\r\n length */);
          return 8;
      }
    }
    return 0;
  }
}
