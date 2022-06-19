/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.core.tab;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.stream.Collectors;

public class TabListPingManager implements Listener {
    public static final long UPDATE_INTERVAL = 20 * 3; // 3 seconds (60 ticks)

    public static final String PING_OBJECTIVE_NAME = "un_ping";
    public static final Component OBJECTIVE_DISPLAYNAME = MutableComponent.create(new LiteralContents("応答速度"));
    public static final Objective OBJECTIVE = new Objective(
            null,
            PING_OBJECTIVE_NAME,
            ObjectiveCriteria.DUMMY,
            OBJECTIVE_DISPLAYNAME,
            ObjectiveCriteria.RenderType.INTEGER
    );

    public static final ClientboundSetObjectivePacket CREATE_OBJECTIVE = new ClientboundSetObjectivePacket(OBJECTIVE, ClientboundSetObjectivePacket.METHOD_ADD);
    public static final ClientboundSetDisplayObjectivePacket DISPLAY_OBJECTIVE = new ClientboundSetDisplayObjectivePacket(0, OBJECTIVE);
    public static final ClientboundSetObjectivePacket REMOVE_OBJECTIVE = new ClientboundSetObjectivePacket(OBJECTIVE, ClientboundSetObjectivePacket.METHOD_REMOVE);

    private static BukkitTask UPDATE_TASK;

    public static BukkitTask startTask() {
        stopTask();

        UnknownNetworkCore.getDedicatedServer()
                .getPlayerList()
                .getPlayers()
                .forEach(TabListPingManager::createDummyObjective);

        UPDATE_TASK = RunnableManager.runAsyncRepeating(TabListPingManager::sendLatencies, 0L, UPDATE_INTERVAL);

        return UPDATE_TASK;
    }

    public static void stopTask() {
        if(UPDATE_TASK != null && !UPDATE_TASK.isCancelled()) UPDATE_TASK.cancel();

        UnknownNetworkCore.getDedicatedServer()
                .getPlayerList()
                .getPlayers()
                .forEach(TabListPingManager::removeDummyObjective);
    }

    private static void createDummyObjective(ServerPlayer player) {
        player.connection.send(CREATE_OBJECTIVE);
        player.connection.send(DISPLAY_OBJECTIVE);
    }

    private static void removeDummyObjective(ServerPlayer player) {
        player.connection.send(REMOVE_OBJECTIVE);
    }

    private static void sendLatencies() {
        Map<String, Integer> latencies = UnknownNetworkCore.getDedicatedServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .collect(Collectors.toMap(p -> p.getScoreboardName(), p -> p.latency));

        UnknownNetworkCore.getDedicatedServer()
                .getPlayerList()
                .getPlayers()
                .forEach(player -> {
                    latencies.entrySet().forEach(e -> {
                        ClientboundSetScorePacket setScore = new ClientboundSetScorePacket(
                                ServerScoreboard.Method.CHANGE,
                                PING_OBJECTIVE_NAME,
                                e.getKey(),
                                e.getValue()
                        );

                        player.connection.send(setScore);
                    });
                });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(UPDATE_TASK != null && !UPDATE_TASK.isCancelled()) createDummyObjective(((CraftPlayer) event.getPlayer()).getHandle());
    }
}
