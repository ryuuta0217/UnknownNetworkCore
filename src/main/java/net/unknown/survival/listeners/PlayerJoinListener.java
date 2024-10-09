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

package net.unknown.survival.listeners;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.ipinfo.api.model.IPResponse;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.SharedConstants;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.shared.whois.Whois;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PlayerJoinListener implements Listener {
    private final Map<UUID, Long> LAST_SEEN = new HashMap<>();

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getUniqueId() != null) {
            LAST_SEEN.put(event.getUniqueId(), Bukkit.getOfflinePlayer(event.getUniqueId()).getLastSeen());
        }
    }

    @EventHandler
    public void onPlayerJoined(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            event.joinMessage(Component.text(event.getPlayer().getName() + " が初めてゲームに参加しました", DefinedTextColor.YELLOW));
            if (Bukkit.getWorld("tutorial") != null) {
                RunnableManager.runDelayed(() -> {
                    if (event.getPlayer().teleport(MultiverseCore.getSpawnLocation(Bukkit.getWorld("tutorial")))) {
                        NewMessageUtil.sendMessage(event.getPlayer(), "自動的にチュートリアルに転送されました");
                    }
                }, 5L);
            }
        } else {
            long lastLogin = LAST_SEEN.getOrDefault(event.getPlayer().getUniqueId(), 0L);
            long now = System.currentTimeMillis();
            long diff = now - lastLogin;
            if (lastLogin > 0 && diff > TimeUnit.HOURS.toMillis(1)) {
                // 1時間以上前にログインしていた場合
                if (event.joinMessage() != null) event.joinMessage(Component.text(event.getPlayer().getName() + " が" + getFormattedTime(diff) + "ぶりにログインしました", DefinedTextColor.YELLOW));
            }
        }
        LAST_SEEN.remove(event.getPlayer().getUniqueId());
        if (!event.getPlayer().hasPermission(Permissions.FEATURE_WHOIS.getPermissionNode())) {
            IPResponse ipInfo = Whois.getIpInformation(event.getPlayer().getAddress().getAddress());
            Set<UUID> sameIpPlayers = Whois.getUsersByIp(event.getPlayer().getAddress().getAddress()).keySet();

            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(player -> player.hasPermission(Permissions.FEATURE_WHOIS.getPermissionNode()))
                    .forEach(player -> {
                        Date firstPlayed = new Date(event.getPlayer().getFirstPlayed());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
                        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
                        String firstPlayedFormatted = sdf.format(firstPlayed);
                        Duration relativeTime = Duration.between(firstPlayed.toInstant(), new Date().toInstant());
                        player.sendMessage(Component.empty()
                                .append(Component.text("===== Whois Information =====", DefinedTextColor.AQUA)).appendNewline()
                                .append(Component.text("ID: " + event.getPlayer().getName() + "(" + event.getPlayer().getUniqueId() + ")", DefinedTextColor.YELLOW)).appendNewline()
                                .append(Component.text("IPアドレス: " + Whois.maskIpAddress(event.getPlayer().getAddress().getAddress()), DefinedTextColor.YELLOW)).appendNewline()
                                .append(Component.text("ホスト名: " + (ipInfo != null ? Whois.maskHostName(ipInfo.getHostname()) : "不明"), DefinedTextColor.YELLOW)).appendNewline()
                                .append(Component.text("国: " + (ipInfo != null ? ipInfo.getCountryName() + ", " + ipInfo.getRegion() : "不明"), DefinedTextColor.YELLOW)).appendNewline()
                                .append(Component.text("初回ログイン: " + firstPlayedFormatted + " (" + relativeTime.toDays() + "日前)", DefinedTextColor.YELLOW)).appendNewline()
                                .append(Component.text("同じIPの他のプレイヤー: " + (!sameIpPlayers.isEmpty() ? sameIpPlayers.stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "なし"), DefinedTextColor.YELLOW)));
                    });
            Whois.addUserByIp(event.getPlayer().getAddress().getAddress(), event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }

        int playerUsedProtocolVersion = Via.getAPI().getPlayerVersion(event.getPlayer().getUniqueId());
        int serverProtocolVersion = SharedConstants.getProtocolVersion();
        ProtocolVersion viaPlayerUsedProtocolVersion = ProtocolVersion.getProtocol(playerUsedProtocolVersion);
        ProtocolVersion viaServerProtocolVersion = ProtocolVersion.getProtocol(serverProtocolVersion);
        if (Via.getAPI().getPlayerVersion(event.getPlayer().getUniqueId()) != SharedConstants.getProtocolVersion()) {
            RunnableManager.runDelayed(() -> event.getPlayer().sendMessage(Component.text("あなたは現在、互換機能を使用してサーバーに接続しています。", DefinedTextColor.RED, TextDecoration.BOLD).appendNewline()
                    .append(Component.text("Unknown Networkは、これを起因として起こった問題に対処しません。")).appendNewline()
                    .append(Component.text("あなたが接続に使用しているバージョン: " + playerUsedProtocolVersion + "(" + viaPlayerUsedProtocolVersion.getName() + ")")).appendNewline()
                    .append(Component.text("サーバーのバージョン: " + serverProtocolVersion + "(" + viaServerProtocolVersion.getName() + ")"))), 1L);
        }
    }

    private static String getFormattedTime(long time) {
        long days = time / (1000 * 60 * 60 * 24);
        long hours = (time / (1000 * 60 * 60)) % 24;
        long minutes = (time / (1000 * 60)) % 60;
        long seconds = (time / 1000) % 60;
        return (days > 0 ? days + "日" : "") + (hours > 0 ? getZeroPaddedString(hours) + "時間" : "") + getZeroPaddedString(minutes) + "分" + getZeroPaddedString(seconds) + "秒";
    }

    private static String getZeroPaddedString(long num) {
        return num < 10 ? "0" + num : String.valueOf(num);
    }
}
