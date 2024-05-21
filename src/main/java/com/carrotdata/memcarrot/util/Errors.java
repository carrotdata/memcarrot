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
package com.carrotdata.memcarrot.util;

import com.carrotdata.cache.util.UnsafeAccess;

public class Errors {
  public static final String SERVER_ERROR = "SERVER_ERROR ";
  
  public static final String CLIENT_ERROR = "CLIENT_ERROR ";
  /**
   * Client sent non-existent command
   */
  public static final byte[] ERROR = "ERROR\r\n".getBytes();
  
  public static final byte[] INPUT_TOO_LARGE = "SERVER_ERROR Input is too large, increase value of 'onecache.io-buffer-size' configuration option\r\n".getBytes();
  public static final byte[]  OUTPUT_TOO_LARGE = "SERVER_ERROR Command result is too large, increase value of 'onecache.io-buffer-size' configuration option\r\n".getBytes();
  
  
  public static int error(long ptr) {
    UnsafeAccess.copy(ERROR, 0, ptr, ERROR.length);
    return ERROR.length;
  }
  
}
