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

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Deprecated
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

    public HTTPUtil() {
    }

    public String getMethod() {
        return method;
    }

    public HTTPUtil setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public HTTPUtil setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
        return this;
    }

    public HTTPUtil setQueryMap(Map<String, String> query) {
        this.query = query;
        return this;
    }

    public HTTPUtil addQuery(String key, Object value) {
        if (query == null) this.query = new HashMap<>();
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
        if (headers == null) this.headers = new HashMap<>();
        headers.put(key, value.toString());
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestData() {
        return requestData;
    }

    public HTTPUtil setRequestData(String data) {
        this.requestData = data;
        return this;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }

    public URL getUrl() {
        return url;
    }

    public String request() {
        if (method == null || method.isEmpty()) throw new NullPointerException("メソッドが設定されていません。");
        if (targetAddress == null || targetAddress.isEmpty()) throw new NullPointerException("URLが設定されていません。");

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

            if ((query == null && requestData != null) && method.equalsIgnoreCase("POST")) {
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

                //System.out.println("エラー: " + responseCode + "\n" + sb);
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
            } catch (Exception ignored) {
            }

            System.out.println("エラーが発生しました: " + e.getLocalizedMessage() + "\n" + sb);
        } finally {
            if (connection != null) connection.disconnect();
        }

        return null;
    }
}
