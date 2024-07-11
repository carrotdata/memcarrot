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

import java.nio.BufferOverflowException;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.support.Memcached.Record;
import com.carrotdata.cache.util.UnsafeAccess;

public class GET extends RetrievalCommand {

  /*
   * Retrieval command: ------------------ The retrieval commands "get" and "gets" operate like
   * this: get <key>*\r\n gets <key>*\r\n - <key>* means one or more key strings separated by
   * whitespace. After this command, the client expects zero or more items, each of which is
   * received as a text line followed by a data block. After all the items have been transmitted,
   * the server sends the string "END\r\n" to indicate the end of response. Each item sent by the
   * server looks like this: VALUE <key> <flags> <bytes> [<cas unique>]\r\n <data block>\r\n - <key>
   * is the key for the item being sent - <flags> is the flags value set by the storage command -
   * <bytes> is the length of the data block to follow, *not* including its delimiting \r\n - <cas
   * unique> is a unique 64-bit integer that uniquely identifies this specific item. - <data block>
   * is the data for this item. If some of the keys appearing in a retrieval request are not sent
   * back by the server in the item list this means that the server does not hold items with such
   * keys (because they were never stored, or stored but deleted to make space for more items, or
   * expired, or explicitly deleted by a client).
   */
  public static long executeTime = 0;

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    long t1 = System.nanoTime();
    int outSize = 0;
    int count = this.keys.length;
    for (int i = 0; i < count; i++) {
      Record r = support.get(keys[i], keySizes[i]);
      if (r.value == null) continue;
      int size = r.write(keys[i], keySizes[i], outBuffer + outSize, outBufferSize - outSize, isCAS);
      if (size > outBufferSize - outSize - 5 /* END\r\n */) {
        if (outSize > 0) {
          // FIXME: Partial return - problem
          break;
        } else {
          throw new BufferOverflowException();
        }
      }
      outSize += size;
    }
    UnsafeAccess.copy(END, outBuffer + outSize, 5);
    outSize += 5;
    executeTime += System.nanoTime() - t1;
    return outSize;
  }

  @Override
  public int commandLength() {
    return 4;
  }

}
