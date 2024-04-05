/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.util;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HTTPFetch {
    public static HTTPFetch fetch(Method method, String url) throws MalformedURLException {
        return new HTTPFetch(method, url);
    }

    public static HTTPFetch fetchGet(String url) throws MalformedURLException {
        return fetch(Method.GET, url);
    }

    public static HTTPFetch fetchPost(String url) throws MalformedURLException {
        return fetch(Method.POST, url);
    }

    private Method method;
    private String urlStr;
    private URL url;
    private final Map<String, String> queryParameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private String body;
    private HttpURLConnection connection;
    private boolean processing = false;
    private BiConsumer<HttpURLConnection, Exception> onError;
    private Consumer<HttpURLConnection> preRequest;
    private BiConsumer<HttpURLConnection, InputStream> onSuccess;

    protected HTTPFetch(Method method, String url) throws MalformedURLException {
        this.method = method;
        this.urlStr = url;
        this.buildURL();
    }

    public HTTPFetch setMethod(Method method) {
        this.method = method;
        return this;
    }

    public HTTPFetch setUrl(String url) throws MalformedURLException {
        this.urlStr = url;
        this.buildURL();
        return this;
    }

    public HTTPFetch addQueryParam(String key, String value) throws MalformedURLException {
        this.queryParameters.put(key, value);
        this.buildURL();
        return this;
    }

    public HTTPFetch removeQueryParam(String key) throws MalformedURLException {
        this.queryParameters.remove(key);
        this.buildURL();
        return this;
    }

    public HTTPFetch addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public HTTPFetch removeHeader(String key) {
        this.headers.remove(key);
        return this;
    }

    public HTTPFetch setBody(String body) {
        this.body = body;
        return this;
    }

    public HTTPFetch setOnError(BiConsumer<HttpURLConnection, Exception> onError) {
        this.onError = onError;
        return this;
    }

    public HTTPFetch setPreRequest(Consumer<HttpURLConnection> preRequest) {
        this.preRequest = preRequest;
        return this;
    }

    public HTTPFetch setOnSuccess(BiConsumer<HttpURLConnection, InputStream> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    private void buildURL() throws MalformedURLException {
        if (this.urlStr != null) {
            AtomicReference<String> tempUrlStr = new AtomicReference<>(this.urlStr);
            this.queryParameters.entrySet()
                    .stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .reduce((a, b) -> a + "&" + b)
                    .ifPresent(queryParam -> tempUrlStr.set(tempUrlStr.get() + "?" + queryParam));
            this.url = new URL(tempUrlStr.get());
        }
    }

    public boolean validate() {
        return this.method != null && this.urlStr != null && !this.urlStr.isEmpty() && this.urlStr.startsWith("http") && this.url != null;
    }

    public String sentAndReadAsString() throws IOException {
        return readStream(this.sent(), this.connection.getContentEncoding() == null ? "UTF-8" : this.connection.getContentEncoding());
    }

    public InputStream sent() throws IOException {
        if (!validate()) throw new IllegalStateException("Method or URL not set or invalid url provided!");
        if (this.processing) throw new IllegalStateException("Request already sent!");
        this.processing = true;

        this.connection = (HttpURLConnection) this.url.openConnection();
        this.connection.setDoOutput(true);
        this.connection.setDoInput(true);
        this.connection.setUseCaches(false);
        this.connection.setRequestMethod(this.method.name());
        this.headers.forEach(connection::addRequestProperty);

        if (this.body != null) {
            PrintWriter writer = new PrintWriter(connection.getOutputStream());
            writer.print(this.body);
            writer.close();
        }

        if (this.preRequest != null) this.preRequest.accept(connection);

        this.connection.connect();
        this.processing = false;
        if (this.connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return this.connection.getInputStream();
        }

        // Error Handling
        String errorMessage = readStream(this.connection.getInputStream(), this.connection.getContentEncoding() == null ? "UTF-8" : this.connection.getContentEncoding());
        throw new IllegalStateException("HTTP " + this.connection.getResponseCode() + (!errorMessage.isEmpty() && !errorMessage.isBlank() ? errorMessage : ""));
    }

    private static String readStream(InputStream stream, String charsetName) throws IOException {
        if (stream == null || charsetName == null || charsetName.isEmpty() || charsetName.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        InputStreamReader streamReader = new InputStreamReader(stream, charsetName);
        BufferedReader reader = new BufferedReader(streamReader);
        reader.lines().forEach(sb::append);
        streamReader.close();
        reader.close();
        stream.close();
        return sb.toString();
    }

    public void sentAsync() {
        Thread thread = new Thread(() -> {
            try {
                if (this.onSuccess != null) this.onSuccess.accept(this.connection, this.sent());
            } catch (Exception e) {
                if (this.onError != null) this.onError.accept(this.connection, e);
            }
        });
        thread.setName("HTTPFetch Async Request Thread - " + this.url.getHost());
        thread.start();
    }

    public enum Method {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE
    }
}
