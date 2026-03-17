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

//import io.papermc.paper.threadedregions.ThreadedRegionizer;
//import io.papermc.paper.threadedregions.TickData;
//import io.papermc.paper.threadedregions.TickRegionScheduler;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.managers.BossBarManager;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.observers.PerformanceObserver;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class TPSBar {
    private static final TPSBar INSTANCE = new TPSBar();

    public static final CustomBossEvent BAR = new CustomBossEvent(
            Identifier.tryBySeparator("unknown-network:tps", ':'), buildDisplayName(0, 0));

    public static void initialize() {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            Folia.initialize();
            return;
        }
        BAR.setMax(20);

        BossBarManager.getInstance().register(BAR);

        RunnableManager.runAsyncRepeating(() -> {
            double tps = PerformanceObserver.getTPS();
            Component displayName = buildDisplayName(PerformanceObserver.getTPS(), PerformanceObserver.getMSPT());
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

    public static class Folia implements Listener {
        public static final Folia INSTANCE = new Folia();
        private static final Map<UUID, CustomBossEvent> BARS = new HashMap<>();
        private static final ThreadLocal<DecimalFormat> TWO_DECIMAL_PLACES = ThreadLocal.withInitial(() -> {
            return new DecimalFormat("#,##0.00");
        });

        public static void initialize() {
            ListenerManager.registerListener(INSTANCE);
        }

        private Folia() {

        }
/*
        private static synchronized void createBossBar(Player player) {
            CustomBossEvent bar = new CustomBossEvent(
                    Identifier.tryBySeparator("unknown-network:tps", ':'), Component.literal("..."));
            bar.setMax(20);
            bar.setColor(BossEvent.BossBarColor.RED);
            BARS.put(player.getUniqueId(), bar);
        }

        private static CustomBossEvent getBossBar(Player player) {
            return BARS.getOrDefault(player.getUniqueId(), null);
        }

        public static void showTPSBar(Player player) {
            if (!BARS.containsKey(player.getUniqueId())) createBossBar(player);
            BARS.get(player.getUniqueId()).addPlayer(MinecraftAdapter.player(player));
        }

        public static void hideTPSBar(Player player) {
            if (!BARS.containsKey(player.getUniqueId())) return;
            BARS.get(player.getUniqueId()).removePlayer(MinecraftAdapter.player(player));
        }

        private static Component buildDisplayName() {
            long currentTime = System.nanoTime();
            String time = DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now().atZone(ZoneId.of("Asia/Tokyo")));

            ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> region = TickRegionScheduler.getCurrentRegion();
            TickData.TickReportData tickReport5s = region.getData().getRegionSchedulingHandle().getTickReport5s(currentTime);

            Component timeComponent = Component.literal("[" + time + "]").withStyle(ChatFormatting.GRAY);

            long id = region.getData().id;
            int playerCount = region.getData().getRegionStats().getPlayerCount();
            int entityCount = region.getData().getRegionStats().getEntityCount();
            int chunkCount = region.getData().getRegionStats().getChunkCount();
            Component regionInfoComponent = Component.literal("([" + id + "] E: " + entityCount + "(P: " + playerCount + "), C: " + chunkCount + ")").withStyle(ChatFormatting.GREEN);

            double tps = tickReport5s.tpsData().segmentAll().average();
            Component tpsComponent = Component.literal(TWO_DECIMAL_PLACES.get().format(tps)).withStyle(ChatFormatting.GOLD);

            double mspt = region.getData().getRegionSchedulingHandle().getTickReport5s(currentTime).timePerTickData().segmentAll().average() / 1.0E6;
            Component msptComponent = Component.literal(TWO_DECIMAL_PLACES.get().format(mspt) + "ms").withStyle(ChatFormatting.AQUA);

            return Component.empty().append(timeComponent).append(" ").append(regionInfoComponent).append(" | TPS: ").append(tpsComponent).append(" | MSPT: ").append(msptComponent);
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            if (SELECTED_HIDE_PLAYERS.contains(event.getPlayer().getUniqueId())) return;
            showTPSBar(event.getPlayer());

            event.getPlayer().getScheduler().runAtFixedRate(UnknownNetworkCorePlugin.getInstance(), (task) -> {
                CustomBossEvent bar = getBossBar(event.getPlayer());
                if (bar != null) {
                    bar.setName(buildDisplayName());
                    double tps = TickRegionScheduler.getCurrentRegion().getData().getRegionSchedulingHandle().getTickReport5s(System.nanoTime()).tpsData().segmentAll().average();
                    bar.setProgress((float) (Mth.clamp(tps, 0, 20) / 20));
                    if(tps > 15) bar.setColor(BossEvent.BossBarColor.GREEN);
                    else if(tps > 10) bar.setColor(BossEvent.BossBarColor.YELLOW);
                    else bar.setColor(BossEvent.BossBarColor.RED);
                }
            }, null, 20, 20);
        }*/
    }
}
