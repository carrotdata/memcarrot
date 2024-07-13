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

import java.io.IOException;
import java.nio.BufferOverflowException;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
import com.carrotdata.memcarrot.support.IllegalFormatException;
import com.carrotdata.memcarrot.util.Utils;

public interface MemcachedCommand {

  public final static byte[] GETCMD = "get".getBytes();
  public final static byte[] GATCMD = "gat".getBytes();
  public final static byte[] GETSCMD = "gets".getBytes();
  public final static byte[] GATSCMD = "gats".getBytes();
  public final static byte[] SETCMD = "set".getBytes();
  public final static byte[] ADDCMD = "add".getBytes();
  public final static byte[] REPLACECMD = "replace".getBytes();
  public final static byte[] APPENDCMD = "append".getBytes();
  public final static byte[] PREPENDCMD = "prepend".getBytes();
  public final static byte[] CASCMD = "cas".getBytes();
  public final static byte[] TOUCHCMD = "touch".getBytes();
  public final static byte[] INCRCMD = "incr".getBytes();
  public final static byte[] DECRCMD = "decr".getBytes();
  public final static byte[] DELETECMD = "delete".getBytes();
  public final static byte[] SHUTDOWNCMD = "shutdown".getBytes();

  public final static long NOREPLY = UnsafeAccess.allocAndCopy("noreply", 0, 7);

  public final static long STORED = UnsafeAccess.allocAndCopy("STORED\r\n", 0, 8);
  public final static long NOT_STORED = UnsafeAccess.allocAndCopy("NOT_STORED\r\n", 0, 12);
  public final static long EXISTS = UnsafeAccess.allocAndCopy("EXISTS\r\n", 0, 8);
  public final static long NOT_FOUND = UnsafeAccess.allocAndCopy("NOT_FOUND\r\n", 0, 11);
  public final static long ERROR = UnsafeAccess.allocAndCopy("ERROR\r\n", 0, 11);
  public final static long TOUCHED = UnsafeAccess.allocAndCopy("TOUCHED\r\n", 0, 9);
  public final static long DELETED = UnsafeAccess.allocAndCopy("DELETED\r\n", 0, 9);

  public final static String CLIENT_ERROR = "CLIENT_ERROR ";
  public final static String SERVER_ERROR = "SERVER_ERROR ";
  public final static String CRLF = "\r\n";

  public final static String NUMBER_FORMAT_ERROR = "CLIENT_ERROR number format error\r\n";

  /**
   * Execute command, write response to the buffer
   * @param support mamcached object
   * @param outBuffer address of the buffer
   * @param bufferSize size
   * @return total bytes written
   */
  public int execute(Memcached support, long outBuffer, int bufferSize, OutputConsumer consumer) 
      throws BufferOverflowException, IOException;

  public default int nextTokenStart(long ptr, int limit) {
    return Utils.nextTokenStart(ptr, limit);
  }

  public default int nextTokenEnd(long ptr, int limit) {
    return Utils.nextTokenEnd(ptr, limit);
  }

  /**
   * Parse command from native buffer
   * @param inBuffer buffer address in memory
   * @return true on complete parsing, false - command is incomplete
   * @throws IllegalFormatException on command format error
   */
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException;

  /**
   * Consumed input in bytes
   * @return bytes consumed during parsing
   */
  public int inputConsumed();

  /**
   * Command name length + space
   * @return length
   */
  public int commandLength();
}
