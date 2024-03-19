/*
 * Copyright (C) 2021-present Carrot, Inc. <p>This program is free software: you can redistribute it
 * and/or modify it under the terms of the Server Side Public License, version 1, as published by
 * MongoDB, Inc. <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the Server Side Public License for more details. <p>You should have received a copy
 * of the Server Side Public License along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package com.onecache.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.onecache.core.support.Memcached;
import com.onecache.core.util.UnsafeAccess;
import com.onecache.server.commands.CommandParser;
import com.onecache.server.commands.MemcachedCommand;
import com.onecache.server.support.IllegalFormatException;
import com.onecache.server.support.UnsupportedCommand;

public class CommandProcessor {
  private static Logger logger = LogManager.getLogger(CommandProcessor.class);

  public static class Result {
    int consumed;
    int produced;
  }
  
  static ThreadLocal<MemcachedCommand> lastCommand = new ThreadLocal<MemcachedCommand>();
  /**
   * Main method
   * @param in input buffer contains incoming Memcached command
   * @param out output buffer to return to a client (command response)
   * @return size of response or -1 if input is incomplete
   * @throws IllegalFormatException, UnsupportedCommand
   */
  static int touched;
  static int added = 0;
  public static int process(Memcached storage, long inputPtr, int inputSize, long outPtr,
      int outSize) throws IllegalFormatException {
    try {
    // Execute Memcached command
      MemcachedCommand cmd = CommandParser.parse(inputPtr, inputSize);
      if (cmd == null) {
        return -1; // input is incomplete
      }
      lastCommand.set(cmd);
      int result = cmd.execute(storage, outPtr, outSize);
      return result;
    } catch (UnsupportedCommand ee ) {
      byte[] buf = "ERROR\r\n".getBytes();
      UnsafeAccess.copy(buf, 0, outPtr, buf.length);
      return buf.length;
    } catch (IllegalFormatException eee){
      String msg = "CLIENT_ERROR " + eee.getMessage() + "\r\n";
      logger.error(msg);
      byte[] buf = msg.getBytes();
      UnsafeAccess.copy(buf, 0, outPtr, buf.length);
      return buf.length;
    }
  }

  public static MemcachedCommand getLastExecutedCommand() {
    return lastCommand.get();
  }
}
