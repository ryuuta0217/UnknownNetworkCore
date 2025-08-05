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

package net.unknown.anarchyhardcore;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.net.InetAddresses;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.anarchyhardcore.ban.BanData;
import net.unknown.anarchyhardcore.ban.IPBanData;
import net.unknown.anarchyhardcore.ban.NameBanData;
import net.unknown.anarchyhardcore.ban.UUIDBanData;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.packet.PacketManager;
import net.unknown.core.packet.event.PacketSendingEvent;
import net.unknown.core.packet.listener.OutgoingPacketListener;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class UnknownNetworkAnarchyHardcore {
    private static final Logger LOGGER = Logger.getLogger("UNC/AnarchyHardcore");
    private static final File BAN_ENTRY_FILE = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "hc_ban_entry.yml");
    private static final Set<BanData<?>> BAN_DATA = new HashSet<>();

    public static void onLoad() {

    }

    public static void onEnable() {
        loadBanEntry();

        PacketManager.getInstance().registerOutgoingS2CListener(ClientboundLoginPacket.class, new OutgoingPacketListener<>() {
            @Override
            public void onSendingPacket(PacketSendingEvent<ClientboundLoginPacket> event) {
                if (event.getPacket().hardcore()) return;
                ClientboundLoginPacket original = event.getPacket();
                ClientboundLoginPacket clone = new ClientboundLoginPacket(original.playerId(), true, original.levels(), original.maxPlayers(), original.chunkRadius(), original.simulationDistance(), original.reducedDebugInfo(), original.showDeathScreen(), original.doLimitedCrafting(), original.commonPlayerSpawnInfo(), original.enforcesSecureChat());
                event.setPacket(clone);
            }
        });

        ListenerManager.registerEventListener(AsyncPlayerPreLoginEvent.class, null, EventPriority.MONITOR, false, (l, event) -> {
            if (Bukkit.getBanList(BanListType.IP).isBanned(event.getAddress())) { // if ip banned
                BanEntry<InetAddress> ipBanEntry = Bukkit.getBanList(BanListType.IP).getBanEntry(event.getAddress());
                if (ipBanEntry != null && !Bukkit.getBanList(BanListType.PROFILE).isBanned(event.getPlayerProfile())) { // but not profile banned
                    Bukkit.getBanList(BanListType.PROFILE).addBan(event.getPlayerProfile(), ipBanEntry.getReason(), (Date) null, "SERVER"); // ban as profile
                }
            }

            if (Bukkit.getBanList(BanListType.PROFILE).isBanned(event.getPlayerProfile())) { // if profile banned
                BanEntry<PlayerProfile> profileBanEntry = Bukkit.getBanList(BanListType.PROFILE).getBanEntry(event.getPlayerProfile());
                if (profileBanEntry != null && !Bukkit.getBanList(BanListType.IP).isBanned(event.getAddress())) { // but not ip banned
                    Bukkit.getBanList(BanListType.IP).addBan(event.getAddress(), profileBanEntry.getReason(), (Date) null, "SERVER"); // ban as ip
                    BAN_DATA.stream()
                            .filter(data -> data.validate(null, event.getUniqueId(), null))
                            .findAny()
                            .ifPresent(data -> {
                                BAN_DATA.add(new IPBanData(event.getAddress(), System.currentTimeMillis(), data.reason()));
                                RunnableManager.runAsync(UnknownNetworkAnarchyHardcore::writeBanEntry);
                            });
                }
            }

            BAN_DATA.stream()
                    .filter(data -> data.validate(event.getAddress(), event.getUniqueId(), event.getName()))
                    .findFirst()
                    .ifPresent(banData -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banData.reason()));
        });

        ListenerManager.registerEventListener(PlayerDeathEvent.class, null, EventPriority.MONITOR, false, (l, event) -> {
            RunnableManager.runDelayed(() -> ban(event.getPlayer(), event.deathMessage()), 1L);
        });

        LOGGER.info("Server launched as ANARCHY HARDCORE mode.");
        LOGGER.info("All players will be banned after death.");

        RunnableManager.runDelayed(() -> Bukkit.getWorlds().forEach(world -> {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setDifficulty(Difficulty.HARD);
            world.setHardcore(true);
        }), 1L);
    }

    public static void onDisable() {

    }

    public static void loadBanEntry() {
        BAN_DATA.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(BAN_ENTRY_FILE);
        if (config.contains("entries") && config.isList("entries")) {
            BAN_DATA.addAll(config.getStringList("entries")
                    .stream()
                    .map(str -> {
                        if (str.startsWith(IPBanData.IDENTIFIER)) return IPBanData.fromString(str);
                        if (str.startsWith(UUIDBanData.IDENTIFIER)) return UUIDBanData.fromString(str);
                        if (str.startsWith(NameBanData.IDENTIFIER)) return NameBanData.fromString(str);
                        LOGGER.warning("Failed to parse ban entry (unknown identifier): " + str);
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList());
        }
    }

    public static synchronized void writeBanEntry() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> entries = BAN_DATA.stream()
                .map(BanData::toString)
                .toList();
        config.set("entries", entries);
        try {
            config.save(BAN_ENTRY_FILE);
        } catch (Exception e) {
            LOGGER.warning("Failed to save ban entry: " + e.getMessage());
        }
    }

    public static Set<BanData<?>> getBanData() {
        return BAN_DATA;
    }

    public static boolean ban(OfflinePlayer player, Component deathMessage) {
        int playSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        Component reason = Component.empty()
                .append(deathMessage)
                .appendNewline()
                .appendNewline()
                .append(Component.empty()
                        .append(Component.translatable("stat.minecraft.play_time"))
                        .append(Component.text(": "))
                        .append(formatDuration(Duration.ofSeconds(playSeconds))))
                .appendNewline()
                .append(Component.empty()
                        .append(Component.text(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(player.getFirstPlayed()), ZoneOffset.ofHours(9)).atZone(ZoneId.of("Asia/Tokyo")))))
                        .append(Component.text(" -> "))
                        .append(Component.text(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z").format(ZonedDateTime.now()))));
        BAN_DATA.add(new IPBanData(player.getPlayer().getAddress().getAddress(), System.currentTimeMillis(), Component.translatable("multiplayer.disconnect.banned.reason", reason)));
        BAN_DATA.add(new UUIDBanData(player.getUniqueId(), System.currentTimeMillis(), Component.translatable("multiplayer.disconnect.banned.reason", reason)));
        RunnableManager.runAsync(UnknownNetworkAnarchyHardcore::writeBanEntry); // Save entries

        // Vanilla Ban System
        if (player.isOnline()) {
            Bukkit.getBanList(BanListType.IP).addBan(player.getPlayer().getAddress().getAddress(), PlainTextComponentSerializer.plainText().serialize(reason), (Date) null, "SERVER").save();
            player.getPlayer().kick(Component.translatable("multiplayer.disconnect.banned.reason", reason), PlayerKickEvent.Cause.IP_BANNED);
        }
        player.ban(PlainTextComponentSerializer.plainText().serialize(reason), (Date) null, "SERVER");
        return true;
    }

    private static Component formatDuration(Duration duration) {
        return Component.empty()
                .append(Component.translatable("gui.hours", Component.text(duration.toHours())))
                .appendSpace()
                .append(Component.translatable("gui.minutes", Component.text(duration.toMinutesPart())))
                .appendSpace()
                .append(Component.text(duration.toSecondsPart()));
    }
}
