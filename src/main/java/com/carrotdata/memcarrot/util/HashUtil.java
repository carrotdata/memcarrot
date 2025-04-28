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
package com.carrotdata.memcarrot.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class HashUtil {

    /**
     * Hashes the input string using SHA-256 and returns the hash as a hex string.
     *
     * @param input the string to be hashed
     * @return the hexadecimal representation of the hash
     */
    public static String hashString(String input) {
        try {
            // Create a SHA-256 message digest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash the input string bytes using UTF-8 encoding
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array into a hex string
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in Java, so this exception should not occur.
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Converts an array of bytes into its hexadecimal string representation.
     *
     * @param bytes the byte array to convert
     * @return the hex string
     */
    private static String bytesToHex(byte[] bytes) {
      return com.carrotdata.cache.util.Utils.toHex(bytes);
    }

    public static void main(String[] args) {
        String originalText = "password";
        String hashedText = hashString(originalText);

        System.out.println("Original: " + originalText);
        System.out.println("Hashed  : " + hashedText);
    }
}
