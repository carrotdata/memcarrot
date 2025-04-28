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

import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;
import com.carrotdata.memcarrot.support.IllegalFormatException;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.util.UnsafeAccess;

/**
 * Format : shutdown\r\n Reply: OK shutting down the server\r\n, OK shutdown is in progress\r\
 */
public class SHUTDOWN implements MemcachedCommand {
  private static final Logger log = LogManager.getLogger(SHUTDOWN.class);

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    if (bufferSize != 2) {
      return false;
    }
    return true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
    log.info("Shutting down the server ...");
    long start = System.currentTimeMillis();
    int size = 0;
    String msg = null;
    try {
      support.getCache().shutdown();
      log.info("Done in {}ms", System.currentTimeMillis() - start);
      System.exit(0);
    } catch (IOException e) {
      msg = "SERVER_ERROR " + e.getMessage() + "\r\n";
      // TODO log the error
      log.error(e);
      size = msg.length();
      byte[] buf = msg.getBytes();
      UnsafeAccess.copy(buf, 0, outBuffer, buf.length);
      return size;
    }
    // Unreachable code
    return 0;
  }

  @Override
  public int inputConsumed() {
    return 10;
  }

  @Override
  public int commandLength() {
    return 0;
  }

}
