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
package com.carrotdata.memcarrot.commands;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
import com.carrotdata.memcarrot.support.IllegalFormatException;

/**
 * Format : version\r\n Reply:VERSION xxx\r\n
 */
public class VERSION implements MemcachedCommand {
  private static long VERSION = UnsafeAccess.allocAndCopy("VERSION ", 0, 8);
  private static long CRLF    = UnsafeAccess.allocAndCopy("\r\n", 0, 2);

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    if (bufferSize != 2) {
      return false;
    }
    return true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
    String version = System.getProperty("MEMCARROT_VERSION");
    if (version == null) {
      version = "unknown";
    }
    UnsafeAccess.copy(VERSION, outBuffer, 8);
    int len = version.length();
    UnsafeAccess.copy(version.getBytes(), 0, outBuffer + 8, len);
    UnsafeAccess.copy(CRLF, outBuffer + 8 + len, 2);
    return 10 + len;
  }

  @Override
  public int inputConsumed() {
    return 9;
  }

  @Override
  public int commandLength() {
    return 0;
  }

}
