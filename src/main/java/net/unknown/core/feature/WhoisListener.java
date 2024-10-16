package net.unknown.core.feature;

import io.ipinfo.api.model.IPResponse;
import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.shared.whois.Whois;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class WhoisListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Whois.addUserByIp(event.getPlayer().getAddress().getAddress(), event.getPlayer().getUniqueId(), System.currentTimeMillis());

        if (!event.getPlayer().hasPermission(Permissions.FEATURE_WHOIS.getPermissionNode())) {
            Component whoisMessage = buildWhoisInformationMessage(event.getPlayer());
            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(player -> player.hasPermission(Permissions.FEATURE_WHOIS.getPermissionNode()))
                    .forEach(player -> player.sendMessage(whoisMessage));
        }
    }

    public static Component buildWhoisInformationMessage(Player target) {
        IPResponse ipInfo = Whois.getIpInformation(target.getAddress().getAddress());
        Set<UUID> sameIpPlayers = Whois.getUsersByIp(target.getAddress().getAddress()).keySet();

        Date firstPlayed = new Date(target.getFirstPlayed());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        String firstPlayedFormatted = sdf.format(firstPlayed);
        Duration relativeTime = Duration.between(firstPlayed.toInstant(), new Date().toInstant());
        return Component.empty()
                .append(Component.text("===== Whois Information =====", DefinedTextColor.AQUA)).appendNewline()
                .append(Component.text("ID: " + target.getName() + " (" + target.getUniqueId() + ")", DefinedTextColor.YELLOW)).appendNewline()
                .append(Component.text("IPアドレス: " + Whois.maskIpAddress(target.getAddress().getAddress()), DefinedTextColor.YELLOW)).appendNewline()
                .append(Component.text("ホスト名: " + (ipInfo != null ? Whois.maskHostName(ipInfo.getHostname()) + " (" + ipInfo.getCompany().getName() + ")" : "不明"), DefinedTextColor.YELLOW)).appendNewline()
                .append(Component.text("国/地域: " + (ipInfo != null ? ipInfo.getCountryName() + ", " + ipInfo.getRegion() : "不明"), DefinedTextColor.YELLOW)).appendNewline()
                .append(Component.text("初回ログイン: " + firstPlayedFormatted + " (" + relativeTime.toDays() + "日前)", DefinedTextColor.YELLOW)).appendNewline()
                .append(Component.text("同じIPの他のプレイヤー: " + (!sameIpPlayers.isEmpty() ? sameIpPlayers.stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).filter(Objects::nonNull).collect(Collectors.joining(", ")) : "なし"), DefinedTextColor.YELLOW));
    }
}
