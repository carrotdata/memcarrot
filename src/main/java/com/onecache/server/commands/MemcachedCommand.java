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


import com.onecache.core.util.UnsafeAccess;
import com.onecache.server.support.IllegalFormatException;
import com.onecache.core.support.Memcached;
import com.onecache.server.support.UnsupportedCommand;


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
  

  
  /**
   * Each command MUST implement this method
   *
   * @param support memcached support
   * @param inBufferPtr input buffer (Memcached request)
   * @param outBufferPtr output buffer (Memcached command output)
   * @param outBufferSize output buffer size
   */
  public default void execute(Memcached support, long inBufferPtr, long outBufferPtr, int outBufferSize) 
    throws IllegalFormatException, UnsupportedCommand
  {
    
    MemcachedCommand cmd = CommandParser.parse(inBufferPtr);
    cmd.execute(support, outBufferPtr, outBufferSize);
  }

  public void execute(Memcached support, long outBuffer, int outBufferSize);
  
  public default long nextTokenStart(long ptr) {
    while(UnsafeAccess.toByte(ptr) == (byte) ' ') ptr++;
    return ptr;
  }
  
  public default long nextTokenEnd(long ptr) {
    while(UnsafeAccess.toByte(ptr) != (byte) ' ' && UnsafeAccess.toByte(ptr) != (byte) '\r') ptr++;
    return ptr;
  }
  
  /**
   * Parse command from native buffer
   * @param inBuffer buffer address in memory
   * @throws IllegalFormatException
   */
  public void parse(long inBuffer) throws IllegalFormatException;
  
}
