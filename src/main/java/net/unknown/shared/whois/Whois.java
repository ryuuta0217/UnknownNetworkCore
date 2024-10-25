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

package net.unknown.shared.whois;

import io.ipinfo.api.IPinfo;
import io.ipinfo.api.cache.Cache;
import io.ipinfo.api.cache.SimpleCache;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import net.unknown.shared.SharedConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Whois {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/Whois");
    private static final String IPINFO_ACCESS_TOKEN;
    private static final IPinfo IPINFO_CLIENT;
    private static final Cache IPINFO_CACHE;
    private static final File USERS_BY_IP_FILE = new File(SharedConstants.DATA_FOLDER, "users_by_ip.json");
    private static final Map<InetAddress, Map<UUID, Long>> USERS_BY_IP = new HashMap<>();

    static {
        LOGGER.info("Reading ipinfo.io API access token from tokens.txt...");
        String ipInfoApiTokenTemp = null;
        try {
            File tokensFile = new File(SharedConstants.DATA_FOLDER, "tokens.txt");
            if ((tokensFile.getParentFile().exists() || tokensFile.getParentFile().mkdirs()) && (tokensFile.exists() || tokensFile.createNewFile())) {
                ipInfoApiTokenTemp = Files.readAllLines(tokensFile.toPath())
                        .stream()
                        .filter(line -> line.matches("^ipinfo-api-access-token: .+$"))
                        .map(line -> line.replaceFirst("^ipinfo-api-access-token: ?", ""))
                        .findFirst()
                        .orElse(null);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        IPINFO_ACCESS_TOKEN = ipInfoApiTokenTemp;

        LOGGER.info("Initializing ipinfo.io API client...");
        IPINFO_CACHE = new SimpleCache(Duration.ofDays(3));
        IPINFO_CLIENT = new IPinfo.Builder()
                .setToken(IPINFO_ACCESS_TOKEN)
                .setCache(IPINFO_CACHE)
                .build();

        LOGGER.info("Loading IP to players database...");
        USERS_BY_IP.clear();
        USERS_BY_IP.putAll(loadUsersByIp());
    }

    public static Cache getIpInfoCache() {
        return IPINFO_CACHE;
    }

    /**
     * 引数に渡したIPアドレスの情報を取得する。IPInfo.io のAPI Clientを使用する。
     *
     * @param ip IPアドレス
     * @return 引数に渡したIPアドレスの情報。取得できなかった場合(レートリミットなどの理由で)は、nullが返却される。
     */
    public static IPResponse getIpInformation(InetAddress ip) {
        try {
            return IPINFO_CLIENT.lookupIP(ip.getHostAddress());
        } catch (RateLimitedException e) {
            return null;
        }
    }

    /**
     * 引数に渡したIPアドレスでログインしたユーザーの最終ログイン日時のMapを返却する。
     *
     * @param ip IPアドレス
     * @return ユーザーのUUIDと最終ログイン日時のMap
     */
    public static Map<UUID, Long> getUsersByIp(InetAddress ip) {
        return Collections.unmodifiableMap(USERS_BY_IP.getOrDefault(ip.getHostAddress(), Collections.emptyMap()));
    }

    public static String maskIpAddress(InetAddress address) {
        if (address instanceof Inet4Address v4Addr) return maskV4Address(v4Addr.getHostAddress());
        return maskV6Address(address.getHostAddress());
    }

    /**
     * 引数に渡したIPv6アドレスをマスクします。IPv6アドレスにのみ対応。
     * @param v6Addr マスクするIPv6アドレス
     * @return マスクされたIPv6アドレス
     */
    public static String maskV6Address(String v6Addr) {
        int hideStart;

        if (v6Addr.matches("([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}")) hideStart = v6Addr.indexOf(":", v6Addr.indexOf(":", v6Addr.indexOf(":", v6Addr.indexOf(":") + 1) + 1) + 1) + 1;
        else hideStart = v6Addr.indexOf(":", v6Addr.indexOf(":", v6Addr.indexOf(":", v6Addr.indexOf(":", v6Addr.indexOf(":") + 1) + 1) + 1) + 1) + 1;
        String visible = v6Addr.substring(0, hideStart);
        String hidden = v6Addr.substring(hideStart).replaceAll("[0-9a-fA-F]", "*");
        return visible + hidden;
    }

    /**
     * 引数に渡したIPv4アドレスをマスクします。IPv4アドレスにのみ対応。
     * @param v4Addr マスクするIPv4アドレス
     * @return マスクされたIPv4アドレス
     */
    public static String maskV4Address(String v4Addr) {
        int hideStart;

        if (v4Addr.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) hideStart = v4Addr.indexOf(".", v4Addr.indexOf(".") + 1) + 1;
        else hideStart = v4Addr.indexOf(":", v4Addr.indexOf(":", v4Addr.indexOf(":", v4Addr.indexOf(":") + 1) + 1) + 1) + 1;
        String visible = v4Addr.substring(0, hideStart);
        String hidden = v4Addr.substring(hideStart).replaceAll("\\d", "*");
        return visible + hidden;
    }

    /**
     * 引数に渡した任意のホスト名をマスクします。
     * @param hostName マスクするホスト名
     * @return マスクされたホスト名
     */
    public static String maskHostName(String hostName) {
        if (hostName == null) return "null";

        int dotCount = Math.toIntExact(hostName.chars().filter(ch -> ch == '.').count());
        int hideDotPos = dotCount / 2;

        AtomicReference<String> s = new AtomicReference<>("");
        AtomicInteger curDot = new AtomicInteger(0);
        hostName.chars().forEach(ch -> {
            if (ch == '.') curDot.addAndGet(1);
            if (curDot.get() >= hideDotPos) {
                s.updateAndGet(v -> v + (char) ch);
            } else {
                s.updateAndGet(v -> v + "*");
            }
        });

        return s.get();
    }

    private static Map<InetAddress, Map<UUID, Long>> loadUsersByIp() {
        try {
            if ((USERS_BY_IP_FILE.getParentFile().exists() || USERS_BY_IP_FILE.getParentFile().mkdirs()) && (USERS_BY_IP_FILE.exists() || USERS_BY_IP_FILE.createNewFile())) {
                String plainJson = String.join("\n", Files.readAllLines(USERS_BY_IP_FILE.toPath()));
                Map<InetAddress, Map<UUID, Long>> usersByIp = new HashMap<>();
                JSONObject json = new JSONObject(plainJson);
                json.keySet().forEach(ip -> {
                    JSONObject users = json.getJSONObject(ip);

                    try {
                        usersByIp.put(InetAddress.getByName(ip), users.toMap().entrySet().stream().map(e -> Map.entry(UUID.fromString(e.getKey()), Long.parseLong(String.valueOf(e.getValue())))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                    } catch (UnknownHostException ignored) {}
                });
                return Collections.unmodifiableMap(usersByIp);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    public static void addUserByIp(InetAddress ip, UUID uuid, long lastLogin) {
        USERS_BY_IP.compute(ip, (k, v) -> {
            if (v == null) v = new HashMap<>();
            v.put(uuid, lastLogin);
            return v;
        });
        saveUsersByIp();
    }

    private static void saveUsersByIp() {
        try {
            if ((USERS_BY_IP_FILE.getParentFile().exists() || USERS_BY_IP_FILE.getParentFile().mkdirs()) && (USERS_BY_IP_FILE.exists() || USERS_BY_IP_FILE.createNewFile())) {
                JSONObject json = new JSONObject(USERS_BY_IP);
                Files.write(USERS_BY_IP_FILE.toPath(), Collections.singleton(json.toString()));
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
