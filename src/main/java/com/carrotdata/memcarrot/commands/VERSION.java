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
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
import com.carrotdata.memcarrot.Memcarrot;
import com.carrotdata.memcarrot.support.IllegalFormatException;

/**
 * Format : version\r\n Reply:VERSION xxx\r\n
 */
public class VERSION implements MemcachedCommand {
  private static long VERSION = UnsafeAccess.allocAndCopy("VERSION ", 0, 8);
  private static long CRLF    = UnsafeAccess.allocAndCopy("\r\n", 0, 2);

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    if (bufferSize != 2) {
      return false;
    }
    return true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize,
      OutputConsumer consumer) {
    String version = Memcarrot.class.getPackage().getImplementationVersion();
    if (version == null) {
      version = "unknown";
    }
    UnsafeAccess.copy(VERSION, outBuffer, 8);
    int len = version.length();
    UnsafeAccess.copy(version.getBytes(), 0, outBuffer + 8, len);
    UnsafeAccess.copy(CRLF, outBuffer + 8 + len, 2);
    return 10 + len;
  }

  @Override
  public int inputConsumed() {
    return 9;
  }

  @Override
  public int commandLength() {
    return 0;
  }

}
