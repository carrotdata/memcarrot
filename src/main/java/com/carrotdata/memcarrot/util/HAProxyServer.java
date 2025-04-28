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
package com.carrotdata.memcarrot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Simple class that holds the parsed PROXY protocol header information.
 */
class ProxyInfo {
    public final String protocol;
    public final String sourceAddress;
    public final String destinationAddress;
    public final int sourcePort;
    public final int destinationPort;

    public ProxyInfo(String protocol, String sourceAddress, String destinationAddress,
                     int sourcePort, int destinationPort) {
        this.protocol = protocol;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
    }

    @Override
    public String toString() {
        return "ProxyInfo{" +
                "protocol='" + protocol + '\'' +
                ", sourceAddress='" + sourceAddress + '\'' +
                ", destinationAddress='" + destinationAddress + '\'' +
                ", sourcePort=" + sourcePort +
                ", destinationPort=" + destinationPort +
                '}';
    }
}

/**
 * A simple parser for PROXY protocol (version 1) which reads a single header line
 * from an InputStream and parses connection metadata.
 */
class ProxyProtocolParser {

    /**
     * Parses the PROXY protocol header from the given InputStream.
     * Expects a header in the following format:
     * <pre>
     *   PROXY TCP4 192.168.0.1 192.168.0.2 12345 80
     * </pre>
     *
     * @param in the InputStream at the beginning of the connection
     * @return a ProxyInfo object holding the parsed data
     * @throws IOException if reading fails or the header is malformed
     */
    public static ProxyInfo parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));

        // Read the header line (terminated by CRLF)
        String headerLine = reader.readLine();
        System.out.println(headerLine);
        String message = reader.readLine();
        System.out.println(message);

        if (headerLine == null) {
            throw new IOException("No PROXY header received");
        }

        if (!headerLine.startsWith("PROXY ")) {
            throw new IOException("Invalid PROXY header: " + headerLine);
        }

        String[] tokens = headerLine.split("\\s+");
        if (tokens.length < 6) {
            throw new IOException("Incomplete PROXY header: " + headerLine);
        }

        String proto = tokens[1];
        // "UNKNOWN" means no useful address information is available.
        if ("UNKNOWN".equalsIgnoreCase(proto)) {
            return new ProxyInfo("UNKNOWN", null, null, 0, 0);
        } else {
            String srcAddress = tokens[2];
            String dstAddress = tokens[3];
            int srcPort;
            int dstPort;
            try {
                srcPort = Integer.parseInt(tokens[4]);
                dstPort = Integer.parseInt(tokens[5]);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid port in PROXY header: " + headerLine, e);
            }
            return new ProxyInfo(proto, srcAddress, dstAddress, srcPort, dstPort);
        }

    }
}

/**
 * A simple server that accepts connections, uses the PROXY protocol parser to extract
 * client connection details, then prints the header info and any subsequent messages.
 */
public class HAProxyServer {

    public static void main(String[] args) {
        // The server will listen on port 9000.
        int listenPort = 9000;

        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            System.out.println("Server listening on port " + listenPort);

            while (true) {
                // Accept a new client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());

                // Handle the connection in a new thread
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException ex) {
            System.err.println("Server error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

/**
 * Handles a single client connection: parses the PROXY protocol header and then
 * reads and prints subsequent messages.
 */
class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream()) {

            // Parse the PROXY protocol header.
            ProxyInfo proxyInfo = ProxyProtocolParser.parse(in);
            System.out.println("Parsed PROXY header: " + proxyInfo);

            // Continue reading the rest of the message (if any) from the client.
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Received message: " + message);
            }

        } catch (IOException e) {
            System.err.println("Error processing client connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
}
