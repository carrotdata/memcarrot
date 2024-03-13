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

import com.onecache.core.support.Memcached;
import com.onecache.core.util.UnsafeAccess;

/**
 * 
Commands "incr" and "decr" are used to change data for some item
in-place, incrementing or decrementing it. The data for the item is
treated as decimal representation of a 64-bit unsigned integer.  If
the current data value does not conform to such a representation, the
incr/decr commands return an error (memcached <= 1.2.6 treated the
bogus value as if it were 0, leading to confusion). Also, the item
must already exist for incr/decr to work; these commands won't pretend
that a non-existent key exists with value 0; instead, they will fail.

The client sends the command line:

incr key value [noreply]\r\n

or

decr key value [noreply]\r\n

- key is the key of the item the client wishes to change

- value is the amount by which the client wants to increase/decrease
the item. It is a decimal representation of a 64-bit unsigned integer.

- "noreply" optional parameter instructs the server to not send the
  reply.  See the note in Storage commands regarding malformed
  requests.

The response will be one of:

- "NOT_FOUND\r\n" to indicate the item with this value was not found

- value\r\n , where value is the new value of the item's data,
  after the increment/decrement operation was carried out.

Note that underflow in the "decr" command is caught: if a client tries
to decrease the value below 0, the new value will be 0.  Overflow in
the "incr" command will wrap around the 64 bit mark.

Note also that decrementing a number such that it loses length isn't
guaranteed to decrement its returned length.  The number MAY be
space-padded at the end, but this is purely an implementation
optimization, so you also shouldn't rely on that.
 *
 */
public class DECR extends INCR {

  @Override
  public int execute(Memcached support, long outBuffer, int outBufferSize) {
    long result = support.decr(keyPtr, keySize, exptime);
    if (!this.noreply) {
      if (result >= 0) {
        int size = com.onecache.core.util.Utils.longToStrDirect(outBuffer, outBufferSize, result);
        outBuffer  += size;
        // Add \r\n
        crlf(outBuffer);
        return size + 2;
      } else {
        UnsafeAccess.copy(NOT_FOUND, outBuffer, 11 /* NOT_FOUND\r\n length */);
        return 11;
      }
    }
    return 0;  
  }

}
