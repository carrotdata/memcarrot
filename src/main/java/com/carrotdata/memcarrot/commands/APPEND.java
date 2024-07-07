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

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.support.Memcached.OpResult;
import com.carrotdata.cache.util.UnsafeAccess;

public class APPEND extends StorageCommand {

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    OpResult result = support.append(keyPtr, keySize, valPtr, valSize, (int) flags, exptime);
    if (!this.noreply) {
      if (result == OpResult.NOT_STORED) {
        UnsafeAccess.copy(NOT_STORED, outBuffer, 12 /* NOT_STORED\r\n length */);
        return 12;
      } else {
        UnsafeAccess.copy(STORED, outBuffer, 8 /* STORED\r\n length */);
        return 8;
      }
    }
    return 0;
  }

  @Override
  public int commandLength() {
    return 7;
  }

}
