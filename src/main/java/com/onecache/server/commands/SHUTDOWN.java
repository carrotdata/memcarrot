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

import com.onecache.server.support.IllegalFormatException;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.onecache.core.support.Memcached;
import com.onecache.core.util.UnsafeAccess;

/**
 * 
 * Format : shutdown\r\n
 * Reply: OK shutting down the server\r\n, OK shutdown is in progress\r\
 */
public class SHUTDOWN implements MemcachedCommand {
  private static final Logger log = LogManager.getLogger(SHUTDOWN.class);

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    if (bufferSize != SHUTDOWNCMD.length + 2) {
      throw new IllegalFormatException("malformed request");
    }
    return true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    int size = 0;
    String msg = null;
    try {
      support.getCache().shutdown();
      msg = support.getCache().shutdownStatusMsg();
    } catch (IOException e) {
      msg = "SERVER_ERROR " + e.getMessage() + "\r\n";
      // TODO log the error
      log.error(e);
    }
    size  = msg.length();
    byte[] buf = msg.getBytes();
    UnsafeAccess.copy(buf, 0, outBuffer, buf.length);
    return size;
  }

}
