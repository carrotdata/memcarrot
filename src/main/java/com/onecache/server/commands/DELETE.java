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

import com.onecache.server.support.IllegalFormatException;

import static com.onecache.core.util.Utils.compareTo;

import com.onecache.core.support.Memcached;
import com.onecache.core.support.Memcached.OpResult;
import com.onecache.core.util.UnsafeAccess;

/**
 * 
The command "delete" allows for explicit deletion of items:

delete key [noreply]\r\n

- key is the key of the item the client wishes the server to delete

- "noreply" optional parameter instructs the server to not send the
  reply.  See the note in Storage commands regarding malformed
  requests.

The response line to this command can be one of:

- "DELETED\r\n" to indicate success

- "NOT_FOUND\r\n" to indicate that the item with this key was not
  found.

See the "flush_all" command below for immediate invalidation
of all existing items. *
 */
public class DELETE extends AbstractMemcachedCommand {

  @Override
  public boolean parse(long inBuffer, int bufferSize) throws IllegalFormatException {
    int start = 0;
    int end  = 0;
    
    this.keyPtr = inBuffer;
    
    end = nextTokenEnd(inBuffer, bufferSize);
    end += start; // start should be 0?
    
    throwIfEquals(end, 0, "malformed request");
    
    this.keySize = end - start;
    
    start = nextTokenStart(inBuffer + end, bufferSize - end);
    throwIfNotEquals(start, 1, "malformed request");
    start += end;
    
    // start = 0 ?
    if (UnsafeAccess.toByte(inBuffer + start) == 'n' && bufferSize  - start == 9 /* noreply\r\n*/) {
      if (compareTo(inBuffer + start, 7, NOREPLY, 7) == 0) {
        this.noreply = true;
        end += start + 7;
      } else {
        new IllegalFormatException("malformed request");
      }
    } else if (UnsafeAccess.toByte(inBuffer + start) == 'n') {
      new IllegalFormatException("malformed request");
    }
    if (end != bufferSize - 2) {
      new IllegalFormatException("malformed request");
    }
    // skip \r\n
    if (UnsafeAccess.toByte(inBuffer + end) != '\r') {
      throw new IllegalFormatException("'\r\n' was expected");
    }
    
    end++;
    if (UnsafeAccess.toByte(inBuffer + end) != '\n') {
      throw new IllegalFormatException("'\r\n' was expected");
    }
    return true;
  }

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    OpResult result = support.delete(keyPtr, keySize);
    if (!this.noreply) {
      if (result == OpResult.DELETED) {
        UnsafeAccess.copy(DELETED, outBuffer, 9 /* TOUCHED\r\n length */);
        return 9;
      } else {
        UnsafeAccess.copy(NOT_FOUND, outBuffer, 11 /* NOT_FOUND\r\n length */);
        return 11;
      }
    }
    return 0;  
  }

}
