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

import java.nio.ByteBuffer;

import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.support.IllegalFormatException;
import com.carrotdata.memcarrot.support.UnsupportedCommand;
import com.carrotdata.memcarrot.util.Utils;
import static com.carrotdata.cache.util.Utils.compareTo;
import static com.carrotdata.cache.util.Utils.toBytes;

@SuppressWarnings("unused")

public class CommandParser {

  private static long get_cmd = UnsafeAccess.allocAndCopy("get", 0, 3);
  private static int get_cmd_len = 3;
  private static long gat_cmd = UnsafeAccess.allocAndCopy("gat", 0, 3);
  private static int gat_cmd_len = 3;
  private static long add_cmd = UnsafeAccess.allocAndCopy("add", 0, 3);
  private static int add_cmd_len = 3;
  private static long set_cmd = UnsafeAccess.allocAndCopy("set", 0, 3);
  private static int set_cmd_len = 3;
  private static long cas_cmd = UnsafeAccess.allocAndCopy("cas", 0, 3);
  private static int cas_cmd_len = 3;

  private static long gets_cmd = UnsafeAccess.allocAndCopy("gets", 0, 4);
  private static int gets_cmd_len = 4;
  private static long gats_cmd = UnsafeAccess.allocAndCopy("gats", 0, 4);
  private static int gats_cmd_len = 4;
  private static long incr_cmd = UnsafeAccess.allocAndCopy("incr", 0, 4);
  private static int incr_cmd_len = 4;
  private static long decr_cmd = UnsafeAccess.allocAndCopy("decr", 0, 4);
  private static int decr_cmd_len = 4;
  private static long save_cmd = UnsafeAccess.allocAndCopy("save", 0, 4);
  private static int save_cmd_len = 4;
  private static long save_flag = UnsafeAccess.allocAndCopy("save", 0, 4);
  private static int save_flag_len = 4;

  private static long touch_cmd = UnsafeAccess.allocAndCopy("touch", 0, 5);
  private static int touch_cmd_len = 5;

  private static long append_cmd = UnsafeAccess.allocAndCopy("append", 0, 6);
  private static int append_cmd_len = 6;
  private static long delete_cmd = UnsafeAccess.allocAndCopy("delete", 0, 6);
  private static int delete_cmd_len = 6;
  private static long bgsave_cmd = UnsafeAccess.allocAndCopy("bgsave", 0, 6);
  private static int bgsave_cmd_len = 6;
  private static long nosave_flag = UnsafeAccess.allocAndCopy("nosave", 0, 6);
  private static int nosave_flag_len = 6;

  private static long replace_cmd = UnsafeAccess.allocAndCopy("replace", 0, 7);
  private static int replace_cmd_len = 7;
  private static long prepend_cmd = UnsafeAccess.allocAndCopy("prepend", 0, 7);
  private static int prepend_cmd_len = 7;

  private static long shutdown_cmd = UnsafeAccess.allocAndCopy("shutdown", 0, 8);
  private static int shutdown_cmd_len = 8;
  
  private static long quit_cmd = UnsafeAccess.allocAndCopy("quit", 0, 4);
  private static int quit_cmd_len = 4;

  private static long version_cmd = UnsafeAccess.allocAndCopy("version", 0, 7);
  private static int version_cmd_len = 7;

  private static long stats_cmd = UnsafeAccess.allocAndCopy("stats", 0, 5);
  private static int stats_cmd_len = 5;
  
  /**
   * TODO: Add new commands support Parse input memory buffer
   * @param buffer address
   * @return memcached command
   */
  public static MemcachedCommand parse(long buf, int size)
      throws IllegalFormatException, UnsupportedCommand {
   
    if (size < 3) {
      return null;
    }
    int start = Utils.nextTokenStart(buf, size);
    if (start > 0) {
      // TODO check binary protocol
      throw new IllegalFormatException("malformed request");
    } else if (start < 0) {
      return null;// stream is incomplete
    }
    int end = Utils.nextTokenEnd(buf + start, size - start);
    if (end < 0) {
      return null;
    }
    int len = end - start;
    MemcachedCommand cmd = null;
    if (len == 3) {
      // Check get
      if (compareTo(buf, len, get_cmd, len) == 0) {
        cmd = new GET();
      } else if (compareTo(buf, len, gat_cmd, len) == 0) {
        cmd = new GAT();
      } else if (compareTo(buf, len, set_cmd, len) == 0) {
        cmd = new SET();
      } else if (compareTo(buf, len, add_cmd, len) == 0) {
        cmd = new ADD();
      } else if (compareTo(buf, len, cas_cmd, len) == 0) {
        cmd = new CAS();
      } else {
        throw new UnsupportedCommand(new String(toBytes(buf, len)));
      }
    } else if (len == 4) {
      if (compareTo(buf, len, gets_cmd, len) == 0) {
        cmd = new GETS();
      } else if (compareTo(buf, len, gats_cmd, len) == 0) {
        cmd = new GATS();
      } else if (compareTo(buf, len, incr_cmd, len) == 0) {
        cmd = new INCR();
      } else if (compareTo(buf, len, decr_cmd, len) == 0) {
        cmd = new DECR();
      } else if (compareTo(buf, len, save_cmd, len) == 0) {
        cmd = new SAVE();
      } else if (compareTo(buf, len, quit_cmd, len) == 0) {
        cmd = new QUIT();
      } else {
        throw new UnsupportedCommand(new String(toBytes(buf, len)));
      }
    } else if (len == 5) {
      if (compareTo(buf, len, touch_cmd, len) == 0) {
        cmd = new TOUCH();
      } else if (compareTo(buf, len, stats_cmd, len) == 0) {
        cmd = new STATS();
      } else {
        throw new UnsupportedCommand(new String(toBytes(buf, len)));
      }
    } else if (len == 6) {
      if (compareTo(buf, len, append_cmd, len) == 0) {
        cmd = new APPEND();
      } else if (compareTo(buf, len, delete_cmd, len) == 0) {
        cmd = new DELETE();
      } else if (compareTo(buf, len, bgsave_cmd, len) == 0) {
        cmd = new BGSAVE();
      } else {
        throw new UnsupportedCommand(new String(toBytes(buf, len)));
      }
    } else if (len == 7) {
      if (compareTo(buf, len, prepend_cmd, len) == 0) {
        cmd = new PREPEND();
      } else if (compareTo(buf, len, replace_cmd, len) == 0) {
        cmd = new REPLACE();
      } else if (compareTo(buf, len, version_cmd, len) == 0) {
        cmd = new VERSION();
      } else {
        throw new UnsupportedCommand(new String(toBytes(buf, len)));
      }
    } else if (len == 8) {
      if (compareTo(buf, len, shutdown_cmd, len) == 0) {
        cmd = new SHUTDOWN();
      } else {
        throw new UnsupportedCommand(new String(toBytes(buf, len)));
      }
    } else {
      throw new UnsupportedCommand(new String(toBytes(buf, len)));
    }

    start = Utils.nextTokenStart(buf + len, size - len);
    if (start > 1) {
      throw new IllegalFormatException("P2 malformed request");
    }
    boolean result = cmd.parse(buf + len + start, size - start - len);
    return result ? cmd : null;
  }

  private static boolean endcrlf(long ptr, int size) {
    if (size < 2) {
      return false;
    }
    return UnsafeAccess.toByte(ptr + size - 2) == (byte) '\r'
        && UnsafeAccess.toByte(ptr + size - 1) == (byte) '\n';
  }
}
