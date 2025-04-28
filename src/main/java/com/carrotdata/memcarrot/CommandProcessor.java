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
      lastCommand.set(null);
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
      logger.error("UnsupportedCommand:", ee);
      UnsafeAccess.copy(buf, 0, outPtr, buf.length);
      return buf.length;
    } catch (IllegalFormatException eee) {
      String msg = "CLIENT_ERROR " + eee.getMessage() + "\r\n";
      logger.error(msg, eee);
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
