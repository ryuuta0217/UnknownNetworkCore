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

package net.unknown.core.feature;

import com.ryuuta0217.util.ComponentCollector;
import io.ipinfo.api.model.IPResponse;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.shared.util.NameHistory;
import net.unknown.shared.whois.Whois;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class WhoisListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Whois.addUserByIp(event.getPlayer().getAddress().getAddress(), event.getPlayer().getUniqueId(), System.currentTimeMillis());

        if (!event.getPlayer().hasPermission(Permissions.FEATURE_WHOIS.getPermissionNode())) {
            Component whoisMessage = buildWhoisInformationMessage(event.getPlayer(), true); // Automatic show whois message is always masked
            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(player -> player.hasPermission(Permissions.FEATURE_WHOIS.getPermissionNode()))
                    .forEach(player -> player.sendMessage(whoisMessage));
        }
    }

    public static Component buildWhoisInformationMessage(Player target) {
        return buildWhoisInformationMessage(target, true); // default masked
    }

    public static Component buildWhoisInformationMessage(Player target, boolean mask) {
        IPResponse ipInfo = Whois.getIpInformation(target.getAddress().getAddress());
        Set<UUID> sameIpPlayers = Whois.getUsersByIp(target.getAddress().getAddress()).keySet();

        Date firstPlayed = new Date(target.getFirstPlayed());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        String firstPlayedFormatted = sdf.format(firstPlayed);
        Duration relativeTime = Duration.between(firstPlayed.toInstant(), new Date().toInstant());

        Component headerComponent = Component.text("===== Whois Information =====", DefinedTextColor.AQUA);
        Component idComponent = Component.text("ID: ", DefinedTextColor.YELLOW).append(Component.text(target.getName()).hoverEvent(HoverEvent.showText(Component.text("UUID: " + target.getUniqueId() + "\n" + "クリックでIDとUUIDをコピー"))).clickEvent(ClickEvent.copyToClipboard(target.getName() + " (" + target.getUniqueId() + ")")));
        Component nameHistoryComponent = Component.empty();
        if (!NameHistory.getNameHistory(target.getUniqueId(), target.getName()).isEmpty()) {
            nameHistoryComponent = Component.text("以前の名前: ")
                    .append(NameHistory.getNameHistory(target.getUniqueId(), target.getName())
                            .entrySet()
                            .stream()
                            .map(e -> Component.text(e.getKey()).hoverEvent(HoverEvent.showText(Component.text("最終ログイン: " + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(Instant.ofEpochMilli(e.getValue()).atZone(ZoneId.of("Asia/Tokyo")))))))
                            .collect(ComponentCollector.toComponent(Component.text(", ")))
                            .asComponent())
                    .appendNewline();
        }
        Component ipAddrComponent = Component.text("IPアドレス: " + (mask ? Whois.maskIpAddress(target.getAddress().getAddress()) : target.getAddress().getAddress().getHostAddress()), DefinedTextColor.YELLOW);
        Component hostNameComponent = Component.text("ホスト名: " + (ipInfo != null ? (mask ? Whois.maskHostName(ipInfo.getHostname()) : ipInfo.getHostname()) : "不明"), DefinedTextColor.YELLOW);
        Component countryComponent = Component.text("国/地域: " + (ipInfo != null ? ipInfo.getCountryName() + ", " + ipInfo.getRegion() : "不明"), DefinedTextColor.YELLOW);
        Component firstLoginComponent = Component.text("初回ログイン: " + firstPlayedFormatted + " (" + relativeTime.toDays() + "日前)", DefinedTextColor.YELLOW);
        Component sameIpOtherPlayerComponent = Component.text("同じIPの他のプレイヤー: " + (!sameIpPlayers.isEmpty() ? sameIpPlayers.stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "なし"), DefinedTextColor.YELLOW);

        return Component.empty()
                .append(headerComponent).appendNewline()
                .append(idComponent).appendNewline()
                .append(nameHistoryComponent)
                .append(ipAddrComponent).appendNewline()
                .append(hostNameComponent).appendNewline()
                .append(countryComponent).appendNewline()
                .append(firstLoginComponent).appendNewline()
                .append(sameIpOtherPlayerComponent);
    }
}
