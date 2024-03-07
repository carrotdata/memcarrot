/*
 Copyright (C) 2021-present Carrot, Inc.

 <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 Server Side Public License, version 1, as published by MongoDB, Inc.

 <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 Server Side Public License for more details.

 <p>You should have received a copy of the Server Side Public License along with this program. If
 not, see <http://www.mongodb.com/licensing/server-side-public-license>.
*/
package com.onecache.server;

import java.nio.ByteBuffer;
import java.util.HashMap;

import com.onecache.core.util.UnsafeAccess;
import com.onecache.server.commands.MemcachedCommand;
import com.onecache.server.commands.SHUTDOWN;
import com.onecache.core.support.Memcached;
import com.onecache.server.util.Key;
import com.onecache.server.util.Utils;

public class CommandProcessor {

  /*
   * Default memory buffer size for IO operations
   */
  private static final int BUFFER_SIZE = 1024 * 1024; // 1 MB

  /** Keeps thread local Key instance */
  private static ThreadLocal<Key> keyTLS =
      new ThreadLocal<Key>() {
        @Override
        protected Key initialValue() {
          return new Key(0, 0);
        }
      };

  /** Input buffer per thread TODO: floating size */
  private static ThreadLocal<Long> inBufTLS =
      new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
          long ptr = UnsafeAccess.malloc(BUFFER_SIZE);
          return ptr;
        }
      };

  /*
   * Output buffer per thread TODO: floating size
   */
  private static ThreadLocal<Long> outBufTLS =
      new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
          long ptr = UnsafeAccess.malloc(BUFFER_SIZE);
          return ptr;
        }
      };

  /*
   * Redis command map.
   */
  private static ThreadLocal<HashMap<Key, MemcachedCommand>> commandMapTLS =
      new ThreadLocal<HashMap<Key, MemcachedCommand>>() {
        @Override
        protected HashMap<Key, MemcachedCommand> initialValue() {
          return new HashMap<Key, MemcachedCommand>();
        }
      };

  //TODO: change to Memcached    
  private static final byte[] UNSUPPORTED_COMMAND = "-ERR: Unsupported command: ".getBytes();

  /**
   * Main method
   *
   * @param in input buffer contains incoming Redis command
   * @param out output buffer to return to a client (command response)
   * @return true , if shutdown was requested, false - otherwise
   */
  static long executeTotal = 0;

  static int count = 0;

  @SuppressWarnings("deprecation")
  public static boolean process(Memcached storage, ByteBuffer in, ByteBuffer out) {
    count++;
    long inbuf = inBufTLS.get();

    HashMap<Key, MemcachedCommand> map = commandMapTLS.get();
    Key key = getCommandKey(inbuf);
    MemcachedCommand cmd = map.get(key);
    if (cmd == null) {
      String cmdName = com.onecache.core.util.Utils.toString(key.address, key.length);
      try {
        @SuppressWarnings("unchecked")
        Class<MemcachedCommand> cls =
            (Class<MemcachedCommand>) Class.forName("com.onecache.server.commands." + cmdName);
        cmd = cls.newInstance();
        map.put(key, cmd);
      } catch (Throwable e) {
        out.put(UNSUPPORTED_COMMAND);
        out.put(cmdName.getBytes());
        out.put((byte) '\r');
        out.put((byte) '\n');
        return false;
      }
    }
    long outbuf = outBufTLS.get();
    // Execute Memcached command
    long start = System.nanoTime();
    cmd.execute(storage, inbuf, outbuf, BUFFER_SIZE);
    executeTotal += System.nanoTime() - start;
    if (count % 10000 == 0) {
//       log.debug(" command exe avg={}", executeTotal / (1000L * count));
    }
    // Done.
    return cmd instanceof SHUTDOWN;
  }

  /**
   * Extract command name from an input buffer
   *
   * @param inbuf input buffer
   * @return command name as a Key
   */
  private static Key getCommandKey(long inbuf) {
    Key key = keyTLS.get();
    //TODO
    // To upper case
    Utils.toUpperCase(key.address, key.length);
    return key;
  }
}
