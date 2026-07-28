/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.NamespacedKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DataProviderServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProviderServer.class);
    private static HttpServer server;

    public static void start(int port) {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            server.createContext("/providers", exchange -> {
                if (!checkAuth(exchange)) return;
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                JsonArray array = new JsonArray();
                for (DataProvider<?, ?> provider : DataProviderRegistry.getInstance().getRegisteredProviders()) {
                    array.add(provider.id().toString());
                }
                sendResponse(exchange, 200, array.toString());
            });

            server.createContext("/provider/", exchange -> {
                if (!checkAuth(exchange)) return;
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String prefix = "/provider/";
                if (!path.startsWith(prefix)) {
                    sendResponse(exchange, 400, "Bad Request");
                    return;
                }

                String idStr = path.substring(prefix.length());
                String[] parts = idStr.split(":", 2);
                if (parts.length != 2) {
                    sendResponse(exchange, 400, "Invalid Provider ID format. Expected namespace:key");
                    return;
                }

                @SuppressWarnings("deprecation")
                NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                DataProvider<?, ?> provider = DataProviderRegistry.getInstance().getProvider(key);

                if (provider == null) {
                    sendResponse(exchange, 404, "Provider Not Found");
                    return;
                }

                try {
                    Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
                    handleProvider(exchange, provider, queryParams);
                } catch (Exception e) {
                    LOGGER.error("Error handling provider {}", key, e);
                    sendResponse(exchange, 500, "Internal Server Error");
                }
            });

            server.setExecutor(null); // default executor
            server.start();
            LOGGER.info("DataProvider HTTP Server started on 127.0.0.1:{}", port);
        } catch (IOException e) {
            LOGGER.error("Failed to start DataProvider HTTP Server", e);
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOGGER.info("DataProvider HTTP Server stopped.");
        }
    }

    private static <R, T> void handleProvider(HttpExchange exchange, DataProvider<R, T> provider, Map<String, String> queryParams) throws IOException {
        R req = provider.parseRequest(queryParams);
        T data = provider.provide(req);
        JsonElement json = provider.serialize(data);
        sendResponse(exchange, 200, json.toString());
    }

    private static boolean checkAuth(HttpExchange exchange) throws IOException {
        String token = "change-me-in-production";
        String authHeader = exchange.getRequestHeaders().getFirst("X-Internal-Token");
        
        if (token.isEmpty() || !token.equals(authHeader)) {
            sendResponse(exchange, 403, "Forbidden");
            return false;
        }
        return true;
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", statusCode == 200 ? "application/json" : "text/plain");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
