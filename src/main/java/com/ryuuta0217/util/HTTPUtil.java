/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package com.ryuuta0217.util;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HTTPUtil {
    private String method;
    private String targetAddress;
    private String requestData;
    private Map<String, String> query;
    private Map<String, String> headers;

    private HttpURLConnection connection;
    private URL url;

    public HTTPUtil(@Nonnull String method, @Nonnull String targetAddress, Map<String, String> query, Map<String, String> headers) {
        this.method = method;
        this.targetAddress = targetAddress;
        this.query = query;
        this.headers = headers;
    }

    public HTTPUtil(@Nonnull String method, @Nonnull String targetAddress) {
        this.method = method;
        this.targetAddress = targetAddress;
    }

    public HTTPUtil() {}

    public HTTPUtil setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public HTTPUtil setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
        return this;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public HTTPUtil setQueryMap(Map<String, String> query) {
        this.query = query;
        return this;
    }

    public HTTPUtil addQuery(String key, Object value) {
        if(query == null) this.query = new HashMap<>();
        query.put(key, value.toString());
        return this;
    }

    public Map<String, String> getQuery() {
        return query;
    }

    public HTTPUtil setHeaderMap(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HTTPUtil addHeader(String key, Object value) {
        if(headers == null) this.headers = new HashMap<>();
        headers.put(key, value.toString());
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public HTTPUtil setRequestData(String data) {
        this.requestData = data;
        return this;
    }

    public String getRequestData() {
        return requestData;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }

    public URL getUrl() {
        return url;
    }

    public String request() {
        if(method == null  || method.isEmpty()) throw new NullPointerException("メソッドが設定されていません。");
        if(targetAddress == null || targetAddress.isEmpty()) throw new NullPointerException("URLが設定されていません。");

        String params = "";

        try {
            url = new URL(targetAddress);
            if (query != null) {
                StringBuilder sb = new StringBuilder();
                query.forEach((key, val) -> {
                    if (sb.length() == 0) sb.append(key).append("=").append(val);
                    else sb.append("&").append(key).append("=").append(val);
                });
                params = sb.toString().replaceFirst("&$", "");
            }

            if (query != null && method.equalsIgnoreCase("GET")) {
                url = new URL(targetAddress + (params.isEmpty() ? "" : "?" + params));
            }

            connection = (HttpURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod(method);
            if (headers != null) headers.forEach(connection::addRequestProperty);

            if ((query != null && requestData == null) && method.equalsIgnoreCase("POST")) {
                PrintWriter pw = new PrintWriter(connection.getOutputStream());
                pw.print(params);
                pw.close();
            }

            if((query == null && requestData != null) && method.equalsIgnoreCase("POST")) {
                PrintWriter pw = new PrintWriter(connection.getOutputStream());
                pw.print(requestData);
                pw.close();
            }

            connection.connect();

            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                InputStream response = connection.getInputStream();
                InputStreamReader isr = new InputStreamReader(response, (connection.getContentEncoding() == null ? "UTF-8" : connection.getContentEncoding()));
                BufferedReader br = new BufferedReader(isr);

                StringBuilder sb = new StringBuilder();
                br.lines().forEach(sb::append);
                isr.close();
                br.close();
                response.close();
                connection.disconnect();
                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                if (connection.getInputStream() != null) {
                    InputStream response = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(response, (connection.getContentEncoding() == null ? "UTF-8" : connection.getContentEncoding()));
                    BufferedReader br = new BufferedReader(isr);

                    br.lines().forEach(sb::append);
                    isr.close();
                    br.close();
                    response.close();
                }

                System.out.println("エラー: " + responseCode + "\n" + sb.toString());
            }
        } catch (MalformedURLException e) {
            throw new NullPointerException("URLが不正です: " + e.getLocalizedMessage());
        } catch (ProtocolException e) {
            throw new NullPointerException("メソッド名が不正です: " + e.getLocalizedMessage());
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            try {
                if (connection.getInputStream() != null) {
                    InputStream response = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(response, (connection.getContentEncoding() == null ? "UTF-8" : connection.getContentEncoding()));
                    BufferedReader br = new BufferedReader(isr);

                    br.lines().forEach(sb::append);
                    isr.close();
                    br.close();
                    response.close();
                }
            } catch(Exception ignored) {}

            System.out.println("エラーが発生しました: " + e.getLocalizedMessage() + "\n" + sb.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }

        return null;
    }
}
