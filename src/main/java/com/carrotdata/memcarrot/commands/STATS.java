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

import java.util.Iterator;
import java.util.List;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.cache.util.Utils;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
import com.carrotdata.memcarrot.support.IllegalFormatException;
import com.carrotdata.memcarrot.support.UnsupportedCommand;

/**
 * Format : stats\r\n Reply:STAT attr value\r\n ... END\r\n
 */
public class STATS implements MemcachedCommand {
  private static long STAT = UnsafeAccess.allocAndCopy("STAT ", 0, 5);
  private static long CRLF    = UnsafeAccess.allocAndCopy("\r\n", 0, 2);
  private static long END    = UnsafeAccess.allocAndCopy("END\r\n", 0, 5);
  
  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    if (bufferSize < 2) {
      return false;
    } else if (bufferSize > 2) {
      throw new UnsupportedCommand("STATS");
    }
    if (Utils.compareTo(CRLF, 2, inBuffer, 2) != 0) {
      throw new UnsupportedCommand("STATS");
    }
    return true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
    
    List<String> stats = support.stats();
    Iterator<String> it = stats.iterator();
    int off = 0;
    
    while(it.hasNext()) {
      String name = it.next();
      String value = it.next();
      UnsafeAccess.copy(STAT, outBuffer + off, 5);
      off += 5;
      byte[] b = name.getBytes();
      UnsafeAccess.copy(b,  0,  outBuffer + off, b.length);
      off += b.length;
      UnsafeAccess.putByte(outBuffer + off, (byte) ' ');
      off += 1;
      b = value.getBytes();
      UnsafeAccess.copy(b,  0,  outBuffer + off, b.length);
      off += b.length;
      UnsafeAccess.copy(CRLF, outBuffer + off, 2);
      off += 2;
    }
    UnsafeAccess.copy(END, outBuffer + off, 5);
    off += 5;
    return off;
  }

  @Override
  public int inputConsumed() {
    return 7;
  }

  @Override
  public int commandLength() {
    return 0;
  }

}
