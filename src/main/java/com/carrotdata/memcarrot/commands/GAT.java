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
package com.carrotdata.memcarrot.commands;

import com.carrotdata.cache.support.Memcached;
import com.carrotdata.cache.support.Memcached.Record;
import com.carrotdata.cache.util.UnsafeAccess;

/*
 * The "gat" and "gats" commands are used to fetch items and update the
expiration time of an existing items.

gat exptime key*\r\n
gats exptime key*\r\n

- exptime is expiration time.

- key* means one or more key strings separated by whitespace.

After this command, the client expects zero or more items, each of
which is received as a text line followed by a data block. After all
the items have been transmitted, the server sends the string

"END\r\n"

to indicate the end of response.

Each item sent by the server looks like this:

VALUE key flags bytes [cas unique]\r\n
data block\r\n

- key is the key for the item being sent

- flags is the flags value set by the storage command

- bytes is the length of the data block to follow, *not* including
  its delimiting \r\n

- cas unique is a unique 64-bit integer that uniquely identifies
  this specific item.

- data block is the data for this item.

 */
public class GAT extends GET {

 public GAT() {
   this.isTouch = true;
 }

 @Override
 public int execute(Memcached support, long outBuffer, int outBufferSize) {
   int outSize = 0;
   int count = this.keys.length;
   for (int i = 0; i < count; i++) {
     Record r = support.gat(keys[i], keySizes[i], exptime);
     if (r.value == null) continue;
     int size = r.write(keys[i], keySizes[i], outBuffer + outSize, outBufferSize - outSize, isCAS);
     if (size > outBufferSize - outSize - 5 /*END\r\n*/) {
       break;
     }
     outSize += size;
   }
   UnsafeAccess.copy(END, outBuffer + outSize, 5);
   outSize += 5;
   return outSize;
 }
}
