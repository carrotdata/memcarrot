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
import com.carrotdata.cache.support.Memcached.OpResult;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.CommandProcessor.OutputConsumer;

public class ADD extends StorageCommand {

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize, OutputConsumer consumer) {
    OpResult result = support.add(keyPtr, keySize, valPtr, valSize, (int) flags, exptime);
    if (!this.noreply) {
      if (result == OpResult.NOT_STORED) {
        UnsafeAccess.copy(NOT_STORED, outBuffer, 12 /* NOT_STORED\r\n length */);
        return 12;
      } else {
        UnsafeAccess.copy(STORED, outBuffer, 8 /* STORED\r\n length */);
        return 8;
      }
    }
    return 0;
  }

  @Override
  public int commandLength() {
    return 4;
  }

}
