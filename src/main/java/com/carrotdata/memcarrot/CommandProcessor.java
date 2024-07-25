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
package com.carrotdata.memcarrot;

import java.io.IOException;
import java.nio.BufferOverflowException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.commands.AbstractMemcachedCommand;
import com.carrotdata.memcarrot.commands.CommandParser;
import com.carrotdata.memcarrot.commands.MemcachedCommand;
import com.carrotdata.memcarrot.support.IllegalFormatException;
import com.carrotdata.memcarrot.support.UnsupportedCommand;

public class CommandProcessor {
  private static Logger logger = LogManager.getLogger(CommandProcessor.class);

  public static interface OutputConsumer {
    public void consume(int upto) throws IOException;
  }
  
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
      int outSize, OutputConsumer consumer) throws IllegalFormatException, BufferOverflowException, IOException {
    try {

      // Execute Memcached command
      MemcachedCommand cmd = CommandParser.parse(inputPtr, inputSize);
      if (cmd == null) {
        return -1; // input is incomplete
      }
      lastCommand.set(cmd);
      boolean safe = isMemorySafe(cmd, inputPtr, inputSize);
      if (!safe) {
        logger.error("SERVER_ERROR memory not safe, cmd={}", cmd.getClass().getName());
        byte[] buf = "SERVER_ERROR internal error\r\n".getBytes();
        UnsafeAccess.copy(buf, 0, outPtr, buf.length);
        return buf.length;
      }
      int result = cmd.execute(storage, outPtr, outSize, consumer);
      return result;
    } catch (UnsupportedCommand ee) {
      byte[] buf = "ERROR\r\n".getBytes();
      logger.error(ee);
      UnsafeAccess.copy(buf, 0, outPtr, buf.length);
      return buf.length;
    } catch (IllegalFormatException eee) {
      String msg = "CLIENT_ERROR " + eee.getMessage() + "\r\n";
      logger.error(msg);
      byte[] buf = msg.getBytes();
      UnsafeAccess.copy(buf, 0, outPtr, buf.length);
      return buf.length;
    }
  }

  private static boolean isMemorySafe(MemcachedCommand cmd, long in, int size) {
    if (!(cmd instanceof AbstractMemcachedCommand)) {
      return true;
    }
    AbstractMemcachedCommand c = (AbstractMemcachedCommand) cmd;
    return c.isMemorySafe(in, size);
  }

  public static MemcachedCommand getLastExecutedCommand() {
    return lastCommand.get();
  }
}
