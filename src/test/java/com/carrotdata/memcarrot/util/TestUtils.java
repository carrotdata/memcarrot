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
// Copyright (c) Onecache, Inc.
// SPDX-License-Identifier: MPL-2.0

package com.carrotdata.memcarrot.util;

import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.mockito.Mockito;

import com.carrotdata.cache.Builder;
import com.carrotdata.cache.Cache;
import com.carrotdata.cache.index.CompactBaseWithExpireIndexFormat;
import com.carrotdata.cache.io.Segment;
import com.carrotdata.cache.util.CacheConfig;
import com.carrotdata.cache.util.UnsafeAccess;
import com.carrotdata.memcarrot.MemcarrotConf;

/**
 * Utility methods for unit tests
 */
public class TestUtils {

  public static Cache createCache(long maxSize, long segmentSize, boolean memory,
      boolean withExpireSupport) throws IOException {

    Builder b = new Builder("cache");
    b.withCacheMaximumSize(maxSize).withCacheDataSegmentSize(segmentSize).withTLSSupported(true);
    if (withExpireSupport) {
      b.withMainQueueIndexFormat(CompactBaseWithExpireIndexFormat.class.getName());
    }
    if (memory) {
      return b.buildMemoryCache();
    } else {
      return b.buildDiskCache();
    }
  }

  public static Cache createCache(String configPath) throws IOException {
    MemcarrotConf conf = MemcarrotConf.getConf(configPath);
    return fromConfig();
  }
  
  private static Cache fromConfig() throws IOException {
    CacheConfig conf = CacheConfig.getInstance();

    String[] cacheNames = conf.getCacheNames();
    Cache cache = null;
    for (String name : cacheNames) {
      if (!conf.isCacheTLSSupported(name)) {
        conf.setCacheTLSSupported(name, true);
      }
      Cache c = new Cache(name);
      if (cache != null) {
        cache.setVictimCache(c);
      }
      cache = c;
    }
    return cache;
  }
  
  /**
   * Creates new byte array and fill it with random data
   * @param size size of an array
   * @return array
   */
  public static byte[] randomBytes(int size) {
    byte[] bytes = new byte[size];
    Random r = new Random();
    r.nextBytes(bytes);
    return bytes;
  }

  public static String randomString(int size) {
    return com.carrotdata.cache.util.Utils.getRandomStr(new Random(), size);
  }

  public static String randomString(Random r, int size) {
    return com.carrotdata.cache.util.Utils.getRandomStr(r, size);
  }

  public static String toString(long ptr, int size) {
    byte[] b = com.carrotdata.cache.util.Utils.toBytes(ptr, size);
    return new String(b);
  }

  public static boolean equals(byte[] a, byte[] b) {
    return equals(a, 0, a.length, b, 0, b.length);
  }

  public static boolean equals(byte[] a, int aoff, int alen, byte[] b, int boff, int blen) {
    return com.carrotdata.cache.util.Utils.compareTo(a, aoff, alen, b, boff, blen) == 0;
  }

  /**
   * Creates new byte array and fill it with random data
   * @param size size of an array
   * @return array
   */
  public static byte[] randomBytes(int size, Random r) {
    byte[] bytes = new byte[size];
    r.nextBytes(bytes);
    return bytes;
  }

  /**
   * Copies an array
   * @param arr array of bytes
   * @return copy
   */
  public static byte[] copy(byte[] arr) {
    byte[] buf = new byte[arr.length];
    System.arraycopy(arr, 0, buf, 0, buf.length);
    return buf;
  }

  /**
   * Allocates memory and fills it with random data
   * @param size memory size
   * @return pointer
   */
  public static long randomMemory(int size) {
    byte[] bytes = randomBytes(size);
    long ptr = UnsafeAccess.malloc(size);
    UnsafeAccess.copy(bytes, 0, ptr, size);
    return ptr;
  }

  /**
   * Allocates memory and fills it with random data
   * @param size memory size
   * @return pointer
   */
  public static long randomMemory(int size, Random r) {
    byte[] bytes = randomBytes(size, r);
    long ptr = UnsafeAccess.malloc(size);
    UnsafeAccess.copy(bytes, 0, ptr, size);
    return ptr;
  }

  /**
   * Creates copy of a memory buffer
   * @param ptr memory buffer
   * @param size size of a memory buffer
   * @return copy of a memory buffer (pointer)
   */
  public static long copyMemory(long ptr, int size) {
    long mem = UnsafeAccess.malloc(size);
    UnsafeAccess.copy(ptr, mem, size);
    return mem;
  }

  public static long copyToMemory(byte[] arr) {
    long mem = UnsafeAccess.malloc(arr.length);
    UnsafeAccess.copy(arr, 0, mem, arr.length);
    return mem;
  }

  public static long copyToMemory(String s) {
    byte[] arr = s.getBytes();
    long mem = UnsafeAccess.malloc(arr.length);
    UnsafeAccess.copy(arr, 0, mem, arr.length);
    return mem;
  }

  public static DataOutputStream getOutputStreamForTest() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    return new DataOutputStream(baos);
  }

  public static RandomAccessFile saveToFile(Segment s) throws IOException {
    File f = File.createTempFile("segment", null);
    f.deleteOnExit();
    RandomAccessFile raf = new RandomAccessFile(f, "rw");
    s.save(raf);
    return raf;
  }

  public static CacheConfig mockConfigForTests(long segmentSize, long maxCacheSize)
      throws IOException {
    CacheConfig mock = Mockito.mock(CacheConfig.class, CALLS_REAL_METHODS);
    mock.init();
    // define segment size
    Mockito.when(mock.getCacheSegmentSize(Mockito.anyString())).thenReturn(segmentSize);
    // define maximum cache size
    Mockito.when(mock.getCacheMaximumSize(Mockito.anyString())).thenReturn(maxCacheSize);
    // data directory
    Path path = Files.createTempDirectory(null);
    File dir = path.toFile();
    dir.deleteOnExit();
    Mockito.when(mock.getDataDirs(Mockito.anyString())).thenReturn(new String[] {dir.getAbsolutePath()});
    return mock;
  }

  public static CacheConfig mockConfigForTests(long segmentSize, long maxCacheSize, String dataDir)
      throws IOException {
    CacheConfig mock = Mockito.mock(CacheConfig.class, CALLS_REAL_METHODS);
    mock.init();
    // define segment size
    Mockito.when(mock.getCacheSegmentSize(Mockito.anyString())).thenReturn(segmentSize);
    // define maximum cache size
    Mockito.when(mock.getCacheMaximumSize(Mockito.anyString())).thenReturn(maxCacheSize);
    Mockito.when(mock.getDataDirs(Mockito.anyString())).thenReturn(new String[] {dataDir});
    return mock;
  }

  public static void deleteDir(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    Stream<Path> stream = Files.list(dir);
    stream.forEach(x -> {
      try {
        Files.delete(x);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });
    Files.delete(dir);
    if (Files.exists(dir)) {
      System.err.printf("Could not delete dir=%s\n", dir.toString());
    } else {
      System.out.printf("Deleted dir=%s\n", dir.toString());
    }
    stream.close();
  }

  public static void deleteCacheFiles(Cache cache) throws IOException {
    String snapshotDir = cache.getCacheConfig().getSnapshotDir(cache.getName());
    Path p = Paths.get(snapshotDir);
    deleteDir(p);
    String[] dataDirs = cache.getCacheConfig().getDataDirs(cache.getName());
    for (String dataDir: dataDirs) {
      p = Paths.get(dataDir);
      deleteDir(p);
    }
  }

  public static List<byte[]> loadGithubDataAsBytes() throws URISyntaxException, IOException {

    File dir = new File("./src/test/resources/github");
    File[] list = dir.listFiles();
    ArrayList<byte[]> dataList = new ArrayList<byte[]>();
    for (File ff : list) {
      String s = Files.readString(Paths.get(ff.toURI()));
      dataList.add(s.getBytes());
    }
    return dataList;
  }

  public static List<Long> loadGithubDataAsMemory() throws URISyntaxException, IOException {

    File dir = new File("./src/test/resources/github");
    File[] list = dir.listFiles();
    ArrayList<Long> dataList = new ArrayList<Long>();
    for (File ff : list) {
      String s = Files.readString(Paths.get(ff.toURI()));
      long ptr = copyToMemory(s);
      dataList.add(ptr);
    }
    return dataList;
  }

}
