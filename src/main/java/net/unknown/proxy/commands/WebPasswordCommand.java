/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.proxy.commands;

import com.google.gson.JsonObject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.proxy.UnknownNetworkProxyCore;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WebPasswordCommand implements SimpleCommand {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom secureRandom = new SecureRandom();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> passwordResetPendingPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("プレイヤーのみが実行できるコマンドです", NamedTextColor.RED));
            return;
        }

        if (!this.processingPlayers.add(player.getUniqueId())) {
            source.sendMessage(Component.text("以前のコマンドを処理中です。しばらくお待ちください。", NamedTextColor.YELLOW));
            return;
        }

        UnknownNetworkProxyCore.getInstance().getProxy().getScheduler().buildTask(UnknownNetworkProxyCore.getInstance(), () -> {
            try {
                String apiUrl = getApiUrl();
                String apiToken = getApiToken();

                if (!this.passwordResetPendingPlayers.contains(player.getUniqueId())) {
                    // アカウントが既に存在するかどうかをチェックする
                    HttpRequest accountExistsRequest = HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl + "/internal/webhook/account-exists?uuid=" + player.getUniqueId().toString()))
                            .header("X-Internal-Token", apiToken)
                            .GET()
                            .build();

                    HttpResponse<String> accountExistsResponse = this.httpClient.send(accountExistsRequest, HttpResponse.BodyHandlers.ofString());

                    if (accountExistsResponse.statusCode() == 200) {
                        player.sendMessage(Component.text("アカウントが既に作成されています。ログインしてパスワードを再設定するか、1分以内にもう一度コマンドを実行することで初期パスワードを再生成できます。", NamedTextColor.YELLOW));
                        this.passwordResetPendingPlayers.add(player.getUniqueId());
                        UnknownNetworkProxyCore.getInstance().getProxy().getScheduler().buildTask(UnknownNetworkProxyCore.getInstance(), () -> {
                            this.passwordResetPendingPlayers.remove(player.getUniqueId());
                        }).delay(1, TimeUnit.MINUTES).schedule();
                        return;
                    } else if (accountExistsResponse.statusCode() != 404) {
                        UnknownNetworkProxyCore.getInstance().getLogger().warning("Failed to check account existence for " + player.getUsername() + ". HTTP " + accountExistsResponse.statusCode() + ": " + accountExistsResponse.body());
                        player.sendMessage(Component.text("サーバー側で問題が発生し、アカウント情報を確認できませんでした。運営に問い合わせてください。", NamedTextColor.RED));
                        return;
                    }
                }

                // アカウントが存在しない、または再生成の確認済み
                // 初期パスワードを生成してプレイヤーに通知

                // パスワードを生成
                StringBuilder sb = new StringBuilder(12);
                for (int i = 0; i < 12; i++) {
                    sb.append(PASSWORD_CHARS.charAt(this.secureRandom.nextInt(PASSWORD_CHARS.length())));
                }
                String rawPassword = sb.toString();

                // 生成されたパスワードをハッシュ化
                String hashFormat = hash(rawPassword);

                // リクエストのボディの組み立て
                JsonObject pwdJson = new JsonObject();
                pwdJson.addProperty("uuid", player.getUniqueId().toString());
                pwdJson.addProperty("name", player.getUsername());
                pwdJson.addProperty("passwordHash", hashFormat);

                // リクエストオブジェクトの生成
                HttpRequest passwordRegisterRequest = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/internal/webhook/password-register"))
                        .header("Content-Type", "application/json")
                        .header("X-Internal-Token", apiToken)
                        .POST(HttpRequest.BodyPublishers.ofString(pwdJson.toString(), StandardCharsets.UTF_8))
                        .build();

                // リクエストの送信・レスポンスの取得
                HttpResponse<String> passwordRegisterResponse = this.httpClient.send(passwordRegisterRequest, HttpResponse.BodyHandlers.ofString());

                if (passwordRegisterResponse.statusCode() == 200 || passwordRegisterResponse.statusCode() == 204) {
                    this.passwordResetPendingPlayers.remove(player.getUniqueId());
                    String loginUrl = getLoginUrl();
                    player.sendMessage(Component.empty()
                            .append(Component.text("初期パスワードを生成しました。", NamedTextColor.GREEN))
                            .append(Component.text("[ここをクリックしてコピー]", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.copyToClipboard(rawPassword))
                                    .hoverEvent(HoverEvent.showText(Component.text("クリックするとパスワードをコピーします", DefinedTextColor.GREEN).append(Component.text("\n「コピーしました」などの表示はありません", DefinedTextColor.GRAY)))))
                            .append(Component.text(loginUrl, NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(loginUrl))
                                    .hoverEvent(HoverEvent.showText(Component.text("ログインページを開きます", DefinedTextColor.GREEN)))));
                } else {
                    UnknownNetworkProxyCore.getInstance().getLogger().warning("Failed to register password for " + player.getUsername() + ". HTTP " + passwordRegisterResponse.statusCode() + ": " + passwordRegisterResponse.body());
                    player.sendMessage(Component.text("サーバー側で問題が発生し、パスワードを更新できませんでした。運営に問い合わせてください。", NamedTextColor.RED));
                }
            } catch (Exception e) {
                UnknownNetworkProxyCore.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Error while processing /webpassword for " + player.getUsername(), e);
                player.sendMessage(Component.text("内部エラーが発生しました。運営に問い合わせてください。", NamedTextColor.RED).appendNewline().append(Component.text("Error: " + e.getMessage(), NamedTextColor.GRAY)));
            } finally {
                this.processingPlayers.remove(player.getUniqueId());
            }
        }).schedule();
    }

    private String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY)
                .withParallelism(PARALLELISM)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[HASH_LENGTH];
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        try {
            generator.generateBytes(passwordBytes, hash);
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }

        String saltBase64 = Base64.getEncoder().withoutPadding().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().withoutPadding().encodeToString(hash);

        return String.format("$argon2id$v=%d$m=%d,t=%d,p=%d$%s$%s",
                Argon2Parameters.ARGON2_VERSION_13, MEMORY, ITERATIONS, PARALLELISM, saltBase64, hashBase64);
    }

    private static String getApiUrl() {
        return UnknownNetworkProxyCore.getConfig().node("webapi", "url").getString("http://127.0.0.1:8080").replaceAll("/$", "");
    }

    private static String getApiToken() {
        return UnknownNetworkProxyCore.getConfig().node("webapi", "internal-token").getString("change-me-in-production");
    }

    private static String getLoginUrl() {
        return UnknownNetworkProxyCore.getSharedConfig().node("webapi", "login-url").getString("https://www.mc-unknown.net/login");
    }
}
