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

package net.unknown.core.bossbar;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TPSBar implements Listener {
    private static final TPSBar INSTANCE = new TPSBar();

    public static final CustomBossEvent BAR = new CustomBossEvent(
            ResourceLocation.of("unknown-network:tps", ':'), buildDisplayName(0, 0));

    private static double LAST_MSPT = 0;

    public static void initialize() {
        BAR.setMax(20);

        ListenerManager.registerListener(INSTANCE);

        RunnableManager.runAsyncRepeating(() -> {
            double tps = Mth.clamp(1000 / LAST_MSPT, 0, 20);
            Component displayName = buildDisplayName(tps, LAST_MSPT);
            BAR.setName(displayName);
            BAR.setProgress((float) (tps / 20));
            if(tps > 15) BAR.setColor(BossEvent.BossBarColor.GREEN);
            else if(tps > 10) BAR.setColor(BossEvent.BossBarColor.YELLOW);
            else BAR.setColor(BossEvent.BossBarColor.RED);
        }, 10, 10);
    }

    // format: [HH:mm:ss] TPS: tps | MSPT: mspt
    private static Component buildDisplayName(double tps, double millisecondsPerTick) {
        String time = DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now().atZone(ZoneId.of("Asia/Tokyo")));
        String[] tpsStrArr = String.valueOf(tps).split("\\.");
        String tpsStr = tpsStrArr[0] + "." + tpsStrArr[1].substring(0, Mth.clamp(4, 1, tpsStrArr[1].length()));
        return Component.literal("")
                .append(Component.literal("[" + time + "]").withStyle(ChatFormatting.GRAY))
                .append(" ")
                .append(Component.literal("TPS: " + tpsStr).withStyle(ChatFormatting.GOLD))
                .append(" | ")
                .append(Component.literal("MSPT: " + millisecondsPerTick + "ms").withStyle(ChatFormatting.AQUA));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        BAR.addPlayer(MinecraftAdapter.player(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BAR.removePlayer(MinecraftAdapter.player(event.getPlayer()));
    }

    @EventHandler
    public void onTickEnd(ServerTickEndEvent event) {
        LAST_MSPT = event.getTickDuration();
    }
}
