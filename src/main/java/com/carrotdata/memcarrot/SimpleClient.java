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
package com.carrotdata.memcarrot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.carrotdata.memcarrot.commands.MemcachedCommand.*;

public class SimpleClient {
  private static Logger logger = LogManager.getLogger(SimpleClient.class);

  byte[] CRLF = new byte[] { (byte) '\r', (byte) '\n' };
  byte[] NOREPLY = "noreply".getBytes();
  byte[] SPACE = new byte[] { (byte) ' ' };
  byte[] END = "END\r\n".getBytes();

  static enum ResponseCode {
    DELETED, EXISTS, NOT_FOUND, NOT_STORED, STORED, TOUCHED, SAVED, OK, ERROR, CLIENT_ERROR,
    SERVER_ERROR
  }

  static class GetResult {
    public byte[] key;
    public byte[] value;
    public int flags;
    public OptionalLong cas;
  }

  SocketChannel conn;

  ByteBuffer buf = ByteBuffer.allocateDirect(16 * 1024 * 1024);

  public SimpleClient(String node) {
    try {
      conn = openConnection(node);
    } catch (IOException e) {
      logger.error(e);
    }
  }

  public SimpleClient(String host, int port) {
    try {
      conn = openConnection(host, port);
    } catch (IOException e) {
      logger.error(e);
    }
  }

  private SocketChannel openConnection(String node) throws IOException {
    String[] parts = node.split(":");
    String host = parts[0];
    int port = Integer.parseInt(parts[1]);
    return openConnection(host, port);
  }

  private SocketChannel openConnection(String host, int port) throws IOException {

    SocketChannel sc = SocketChannel.open(new InetSocketAddress(host, port));
    sc.configureBlocking(false);
    sc.setOption(StandardSocketOptions.TCP_NODELAY, true);
    sc.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
    sc.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
    logger.debug("Client opened connection to: {}", sc.getRemoteAddress().toString());
    return sc;
  }

  private ResponseCode getStorageResponse(ByteBuffer buf) {
    int pos = buf.position();
    if (pos == 8) {
      // STORED, EXISTS
      if (buf.get(0) == (byte) 'S') {
        return ResponseCode.STORED;
      } else if (buf.get(0) == (byte) 'E') {
        return ResponseCode.EXISTS;
      } else {
        return null;
      }
    } else if (pos == 11) {
      if (buf.get(4) == (byte) 'F') {
        return ResponseCode.NOT_FOUND;
      } else {
        return null;
      }
    } else if (pos == 12 && buf.get(4) == (byte) 'S') {
      return ResponseCode.NOT_STORED;
    } else if (pos == 12) {
      byte[] b = new byte[pos];
      buf.flip();
      buf.get(b);
      throw new RuntimeException(new String(b));
    } else if (pos > 0) {
      byte[] b = new byte[pos];
      buf.flip();
      buf.get(b);
      String s = new String(b);
      if (s.startsWith("CLIENT_ERROR")) {
        return ResponseCode.CLIENT_ERROR;
      } else if (s.startsWith("ERROR")) {
        return ResponseCode.ERROR;
      } else if (s.startsWith("SERVER_ERROR")) {
        return ResponseCode.SERVER_ERROR;
      } else {
        throw new RuntimeException(s);
      }
    }
    return null;
  }

  private ResponseCode getTouchResponse(ByteBuffer buf) {
    int pos = buf.position();
    if (pos == 9) {
      return ResponseCode.TOUCHED;
    } else if (pos == 11) {
      return ResponseCode.NOT_FOUND;
    } else if (pos > 9) {
      byte[] b = new byte[pos];
      buf.flip();
      buf.get(b);
      throw new RuntimeException(new String(b));
    }
    return null;
  }

  private ResponseCode getDeleteResponse(ByteBuffer buf) {
    int pos = buf.position();
    if (pos == 9) {
      return ResponseCode.DELETED;
    } else if (pos == 11) {
      return ResponseCode.NOT_FOUND;
    } else if (pos > 9) {
      byte[] b = new byte[pos];
      buf.flip();
      buf.get(b);
      throw new RuntimeException(new String(b));
    }
    return null;
  }

  private Object getIncrDecrResponse(ByteBuffer buf) {
    int pos = buf.position();
    if (pos == 0) return null;
    if (buf.get(pos - 1) != (byte) '\n') {
      return null;
    }

    if (pos == 11 && buf.get(0) == 'N') {
      return ResponseCode.NOT_FOUND;
    } else if (buf.get(0) == 'C') {
      return ResponseCode.CLIENT_ERROR;
    } else {
      byte[] b = new byte[pos - 2];
      buf.flip();
      buf.get(b);
      return Long.parseLong(new String(b));
    }
  }

  private List<GetResult> getRetrievalResponse(ByteBuffer buf, boolean cas) {
    // FIXME: handling error response
    int pos = buf.position();
    if (pos < 5) {
      return null;
    }
    if (pos == 5) {
      buf.flip();
      if (com.carrotdata.cache.util.Utils.compareTo(buf, END.length, END, 0, END.length) == 0) {
        // empty result
        return new ArrayList<GetResult>();
      }
      buf.position(pos);
      return null;
    }
    // Check if it is complete response
    // we need to check last 5 bytes
    // FIXME: its dangerous: there is a chance that we can get this true before response is complete
    buf.position(pos - 5);
    if (com.carrotdata.cache.util.Utils.compareTo(buf, END.length, END, 0, END.length) != 0) {
      buf.position(pos);
      return null;// response is not complete
    }

    buf.position(pos);
    buf.flip();
    List<GetResult> result = new ArrayList<GetResult>();
    GetResult one = null;
    while ((one = parseLine(buf, cas)) != null) {
      result.add(one);
    }
    return result;
  }

  private GetResult parseLine(ByteBuffer buf, boolean cas) {

    int pos = buf.position();
    int limit = buf.limit();

    if (limit - pos == 5) {
      if (com.carrotdata.cache.util.Utils.compareTo(buf, END.length, END, 0, END.length) == 0) {
        return null;// end
      } else {
        throw new RuntimeException("malformed response");
      }
    }
    // search \r\n
    int off = pos;
    while (limit - off >= 2) {
      byte b1 = buf.get(off);
      byte b2 = buf.get(++off);
      if (b1 == CRLF[0] && b2 == CRLF[1]) {
        break;
      }
    }
    // copy line
    int newPos = off + 1;
    buf.position(pos);
    byte[] bb = new byte[newPos - pos - 2];
    buf.get(bb);
    String s = new String(bb);
    String[] splits = s.split(" ");
    GetResult res = new GetResult();
    res.key = splits[1].getBytes();
    res.flags = Integer.parseInt(splits[2]);
    if (splits.length == 5) {
      res.cas = OptionalLong.of(Long.parseLong(splits[4]));
    }
    int valueSize = Integer.parseInt(splits[3]);
    buf.get();
    buf.get();

    byte[] value = new byte[valueSize];
    buf.get(value);
    res.value = value;
    // skip \r\n
    buf.get();
    buf.get();
    return res;
  }

  /**
   * Get operation
   * @param keys keys to get
   * @return list of key values
   * @throws IOException
   */
  public List<GetResult> get(byte[][] keys) throws IOException {
    buf.clear();
    int numKeys = keys.length;
    buf.put(GETCMD);
    // SPACE
    buf.put(SPACE[0]);
    for (int i = 0; i < numKeys; i++) {
      buf.put(keys[i]);
      if (i < numKeys - 1) {
        buf.put(SPACE[0]);
      }
    }
    buf.put(CRLF);

    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();

    List<GetResult> result = null;
    do {
      // Hack
      channel.read(buf);
    } while ((result = getRetrievalResponse(buf, false)) == null);
    return result;
  }

  /**
   * Get with CAS operation
   * @param keys keys
   * @return list of key-value-cas
   * @throws IOException
   */
  public List<GetResult> gets(byte[][] keys) throws IOException {
    buf.clear();
    int numKeys = keys.length;
    buf.put(GETSCMD);
    // SPACE
    buf.put(SPACE[0]);
    for (int i = 0; i < numKeys; i++) {
      buf.put(keys[i]);
      if (i < numKeys - 1) {
        buf.put(SPACE[0]);
      }
    }
    buf.put(CRLF);

    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();

    List<GetResult> result = null;
    do {
      // Hack
      channel.read(buf);
    } while ((result = getRetrievalResponse(buf, true)) == null);
    return result;
  }

  /**
   * Get and Touch operation
   * @param expire new expiration time
   * @param keys keys to get
   * @return list of key-value
   * @throws IOException
   */
  public List<GetResult> gat(long expire, byte[][] keys) throws IOException {
    buf.clear();
    int numKeys = keys.length;
    buf.put(GATCMD);
    // SPACE
    buf.put(SPACE[0]);
    // expire
    buf.put(Long.toString(expire).getBytes());
    // SPACE
    buf.put(SPACE[0]);
    for (int i = 0; i < numKeys; i++) {
      buf.put(keys[i]);
      if (i < numKeys - 1) {
        buf.put(SPACE[0]);
      }
    }
    buf.put(CRLF);

    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();

    List<GetResult> result = null;
    do {
      // Hack
      channel.read(buf);
    } while ((result = getRetrievalResponse(buf, false)) == null);
    return result;
  }

  /**
   * Gat with CAS operation
   * @param expire new expiration time
   * @param keys keys to get
   * @return list of key-value-cas
   * @throws IOException
   */
  public List<GetResult> gats(long expire, byte[][] keys) throws IOException {
    buf.clear();
    int numKeys = keys.length;
    buf.put(GATSCMD);
    // SPACE
    buf.put(SPACE[0]);
    // expire
    buf.put(Long.toString(expire).getBytes());
    // SPACE
    buf.put(SPACE[0]);
    for (int i = 0; i < numKeys; i++) {
      buf.put(keys[i]);
      if (i < numKeys - 1) {
        buf.put(SPACE[0]);
      }
    }
    buf.put(CRLF);

    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();

    List<GetResult> result = null;
    do {
      // Hack
      channel.read(buf);
    } while ((result = getRetrievalResponse(buf, true)) == null);
    return result;
  }

  /**
   * Generic store command
   * @param cmd command name
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expire
   * @param noreply no reply
   * @param cas unique CAS
   * @return response code
   * @throws IOException
   */
  private ResponseCode store(byte[] cmd, byte[] key, byte[] value, int flags, long expire,
      boolean noreply, OptionalLong cas) throws IOException {
    buf.clear();
    buf.put(cmd);
    // SPACE
    buf.put(SPACE[0]);
    // Write key
    buf.put(key);
    // SPACE
    buf.put(SPACE[0]);
    // flags
    buf.put(Integer.toString(flags).getBytes());
    // SPACE
    buf.put(SPACE[0]);
    // expire
    buf.put(Long.toString(expire).getBytes());
    // SPACE
    buf.put(SPACE[0]);
    // bytes
    buf.put(Integer.toString(value.length).getBytes());
    if (cas.isPresent()) {
      // SPACE
      buf.put(SPACE[0]);
      // CAS
      String s = Long.toString(cas.getAsLong());
      buf.put(s.getBytes());
    }
    if (noreply) {
      // SPACE
      buf.put(SPACE[0]);
      buf.put(NOREPLY);
    }
    buf.put(CRLF);
    // Value
    buf.put(value);
    buf.put(CRLF);

    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();

    if (noreply) {
      return null;
    }

    ResponseCode response = null;
    do {
      // Hack
      channel.read(buf);
    } while ((response = getStorageResponse(buf)) == null);

    return response;
  }

  /**
   * Set command
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expiration time
   * @param noreply noreply
   * @return response code
   * @throws IOException
   */
  public ResponseCode set(byte[] key, byte[] value, int flags, long expire, boolean noreply)
      throws IOException {
    return store(SETCMD, key, value, flags, expire, noreply, OptionalLong.empty());
  }

  /**
   * Add command
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expiration time
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode add(byte[] key, byte[] value, int flags, long expire, boolean noreply)
      throws IOException {
    return store(ADDCMD, key, value, flags, expire, noreply, OptionalLong.empty());
  }

  /**
   * Replace command
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expiration time
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode replace(byte[] key, byte[] value, int flags, long expire, boolean noreply)
      throws IOException {
    return store(REPLACECMD, key, value, flags, expire, noreply, OptionalLong.empty());
  }

  /**
   * Append command
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expiration time
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode append(byte[] key, byte[] value, int flags, long expire, boolean noreply)
      throws IOException {
    return store(APPENDCMD, key, value, flags, expire, noreply, OptionalLong.empty());
  }

  /**
   * Prepend command
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expiration time
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode prepend(byte[] key, byte[] value, int flags, long expire, boolean noreply)
      throws IOException {
    return store(PREPENDCMD, key, value, flags, expire, noreply, OptionalLong.empty());
  }

  /**
   * CAS command
   * @param key key
   * @param value value
   * @param flags flags
   * @param expire expiration time
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode cas(byte[] key, byte[] value, int flags, long expire, boolean noreply,
      long cas) throws IOException {
    return store(CASCMD, key, value, flags, expire, noreply, OptionalLong.of(cas));
  }

  /**
   * Delete operation
   * @param key key
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode delete(byte[] key, boolean noreply) throws IOException {
    buf.clear();
    buf.put(DELETECMD);
    // SPACE
    buf.put(SPACE[0]);
    // Write key
    buf.put(key);
    if (noreply) {
      // SPACE
      buf.put(SPACE[0]);
      buf.put(NOREPLY);
    }
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();
    if (noreply) {
      return null;
    }
    ResponseCode response = null;
    do {
      // Hack
      channel.read(buf);
    } while ((response = getDeleteResponse(buf)) == null);
    return response;
  }

  /**
   * Touch operation
   * @param key key
   * @param noreply no reply
   * @return response code
   * @throws IOException
   */
  public ResponseCode touch(byte[] key, long expire, boolean noreply) throws IOException {
    buf.clear();
    buf.put(TOUCHCMD);
    // SPACE
    buf.put(SPACE[0]);
    // Write key
    buf.put(key);
    buf.put(SPACE[0]);
    buf.put(Long.toString(expire).getBytes());
    if (noreply) {
      // SPACE
      buf.put(SPACE[0]);
      buf.put(NOREPLY);
    }
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();
    if (noreply) {
      return null;
    }
    ResponseCode response = null;
    do {
      // Hack
      channel.read(buf);
    } while ((response = getTouchResponse(buf)) == null);
    return response;
  }

  /**
   * Increment operation
   * @param key key
   * @param noreply no reply
   * @return response code (NOT_FOUND) or new value
   * @throws IOException
   */
  public Object incr(byte[] key, long value, boolean noreply) throws IOException {
    buf.clear();
    buf.put(INCRCMD);
    // SPACE
    buf.put(SPACE[0]);
    // Write key
    buf.put(key);
    buf.put(SPACE[0]);
    buf.put(Long.toString(value).getBytes());
    if (noreply) {
      // SPACE
      buf.put(SPACE[0]);
      buf.put(NOREPLY);
    }
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();
    if (noreply) {
      return null;
    }
    Object response = null;
    do {
      // Hack
      channel.read(buf);
    } while ((response = getIncrDecrResponse(buf)) == null);
    return response;
  }

  /**
   * Increment operation
   * @param key key
   * @param noreply no reply
   * @return response code (NOT_FOUND) or new value
   * @throws IOException
   */
  public Object decr(byte[] key, long value, boolean noreply) throws IOException {
    buf.clear();
    buf.put(DECRCMD);
    // SPACE
    buf.put(SPACE[0]);
    // Write key
    buf.put(key);
    buf.put(SPACE[0]);
    buf.put(Long.toString(value).getBytes());
    if (noreply) {
      // SPACE
      buf.put(SPACE[0]);
      buf.put(NOREPLY);
    }
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();
    if (noreply) {
      return null;
    }
    Object response = null;
    do {
      // Hack
      channel.read(buf);
    } while ((response = getIncrDecrResponse(buf)) == null);
    return response;
  }

  public void close() throws IOException {
    conn.close();
  }

  /**
   * Shutdown onecache server
   * @throws IOException
   */
  public void shutdown() throws IOException {
    buf.clear();
    buf.put("shutdown".getBytes());
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    // this command does not return
    //buf.clear();
    //while (buf.position() != 4) {
    //  channel.read(buf);
    //}
  }

  /**
   * Save (persist data) onecache server
   * @throws IOException
   */
  public ResponseCode save() throws IOException {
    buf.put("save".getBytes());
    // SPACE
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();
    while (buf.position() == 0) {
      channel.read(buf);
    }
    // either SAVED\r\n or SERVER_ERROR\r\n
    if (buf.position() == 7) {
      return ResponseCode.SAVED;
    } else if (buf.position() == 14) {
      return ResponseCode.SERVER_ERROR;
    }
    throw new RuntimeException("something wrong");
  }

  /**
   * Save (persist data) onecache server
   * @throws IOException
   */
  public ResponseCode bgsave() throws IOException {
    buf.put("bgsave".getBytes());
    // SPACE
    buf.put(CRLF);
    SocketChannel channel = conn;
    buf.flip();
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
    buf.clear();
    while (buf.position() == 0) {
      channel.read(buf);
    }
    // either OK\r\n or SERVER_ERROR\r\n
    if (buf.position() == 4) {
      return ResponseCode.OK;
    } else if (buf.position() == 14) {
      return ResponseCode.SERVER_ERROR;
    }
    throw new RuntimeException("something wrong");
  }

}
