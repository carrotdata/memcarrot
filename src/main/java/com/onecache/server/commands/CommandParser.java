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
import com.onecache.server.support.UnsupportedCommand;
@SuppressWarnings("unused")

public class CommandParser {

  private static long get_cmd = UnsafeAccess.allocAndCopy("get", 0, 3);
  private static int get_cmd_len = 3;
  
  private static long gets_cmd = UnsafeAccess.allocAndCopy("gets", 0, 4);
  private static int gets_cmd_len = 4;
  
  private static long gat_cmd = UnsafeAccess.allocAndCopy("gat", 0, 3);
  private static int gat_cmd_len = 3;
  
  private static long gats_cmd = UnsafeAccess.allocAndCopy("gats", 0, 4);
  private static int gats_cmd_len = 4;
  
  private static long add_cmd = UnsafeAccess.allocAndCopy("add", 0, 3);
  private static int add_cmd_len = 3;
  
  private static long set_cmd = UnsafeAccess.allocAndCopy("set", 0, 3);
  private static int set_cmd_len = 3;
  
  private static long replace_cmd = UnsafeAccess.allocAndCopy("replace", 0, 7);
  private static int replace_cmd_len = 7;
  
  private static long append_cmd = UnsafeAccess.allocAndCopy("append", 0, 6);
  private static int append_cmd_len = 6;
  
  private static long prepend_cmd = UnsafeAccess.allocAndCopy("prepend", 0, 7);
  private static int prepend_cmd_len = 7;
  
  private static long cas_cmd = UnsafeAccess.allocAndCopy("append", 0, 3);
  private static int cas_cmd_len = 3;
  
  private static long delete_cmd = UnsafeAccess.allocAndCopy("delete", 0, 6);
  private static int delete_cmd_len = 6;
  
  private static long touch_cmd = UnsafeAccess.allocAndCopy("touch", 0, 5);
  private static int touch_cmd_len = 5;
  
  private static long incr_cmd = UnsafeAccess.allocAndCopy("incr", 0, 4);
  private static int incr_cmd_len = 4;
  
  private static long decr_cmd = UnsafeAccess.allocAndCopy("decr", 0, 4);
  private static int decr_cmd_len = 4;
  
  private static long shutdown_cmd = UnsafeAccess.allocAndCopy("shutdown", 0, 8);
  private static int shutdown_cmd_len = 8;
  
  private static long bgsave_cmd = UnsafeAccess.allocAndCopy("bgsave", 0, 6);
  private static int bgsave_cmd_len = 6;
  
  private static long save_cmd = UnsafeAccess.allocAndCopy("save", 0, 4);
  private static int save_cmd_len = 4;
  
  private static long nosave_flag = UnsafeAccess.allocAndCopy("nosave", 0, 6);
  private static int nosave_flag_len = 6;
  
  private static long save_flag = UnsafeAccess.allocAndCopy("save", 0, 4);
  private static int save_flag_len = 4;
  
  /**
   * Parse input memory buffer
   * @param buffer address
   * @return memcached command
   */
  public static MemcachedCommand parse(long buffer) throws IllegalFormatException, UnsupportedCommand {
    return null;
  }
  
}
