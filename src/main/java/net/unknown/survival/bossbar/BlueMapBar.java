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

package net.unknown.survival.bossbar;

import de.bluecolored.bluemap.bukkit.BukkitPlugin;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.world.BossEvent;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.List;

public class BlueMapBar implements Listener {
    private static final BlueMapBar INSTANCE = new BlueMapBar();
    public static final CustomBossEvent BAR = new CustomBossEvent(
            ResourceLocation.of("unknown-network:bluemap_progress", ':'), buildDisplayName(Collections.emptyList(), null));
    public static BukkitTask UPDATE_TASK;

    public static void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("BlueMap") || UPDATE_TASK != null) return;

        BAR.setMax(100);
        BAR.setColor(BossEvent.BossBarColor.YELLOW);

        ListenerManager.registerListener(INSTANCE);

        UPDATE_TASK = RunnableManager.runAsyncRepeating(() -> {
            RenderManager renderManager = BukkitPlugin.getInstance().getPlugin().getRenderManager();
            if (renderManager.getCurrentRenderTask() != null) BAR.setProgress((float) renderManager.getCurrentRenderTask().estimateProgress());
            else BAR.setProgress(1.0f);
            BAR.setName(buildDisplayName(renderManager.getScheduledRenderTasks(), renderManager.getCurrentRenderTask()));
            BAR.setVisible(renderManager.getScheduledRenderTasks().size() != 0);
        }, 10, 10);
    }

    private static Component buildDisplayName(List<RenderTask> renderingTasks, RenderTask currentTask) {
        MutableComponent baseComponent = Component.empty()
                .append(Component.literal("[BlueMap]").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD))
                .append(Component.literal(" "));

        if (renderingTasks.size() == 0 || currentTask == null) {
            return baseComponent.append("Rendering is not running.");
        } else {
            int otherRenderingTaskCount = renderingTasks.size() - 1;
            return baseComponent.append(Component.literal(currentTask.getDetail().orElse(currentTask.getDescription())))
                    .append((otherRenderingTaskCount > 0 ? Component.literal(" | Remaining " + otherRenderingTaskCount + " task" + (otherRenderingTaskCount > 1 ? "s": "")) : Component.empty()));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        BAR.addPlayer(MinecraftAdapter.player(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BAR.removePlayer(MinecraftAdapter.player(event.getPlayer()));
    }
}
