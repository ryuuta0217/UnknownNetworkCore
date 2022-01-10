/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.SelectorComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CrashCommand implements Listener {
    private static final Map<UUID, BukkitTask> TASKS = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("crash");
        builder.requires(Permissions.COMMAND_CRASH::check);
        builder.then(Commands.argument("対象", EntityArgument.players())
                .executes(ctx -> {
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "対象");
                    targets.forEach(target -> {
                        if(TASKS.containsKey(target.getUUID())) {
                            TASKS.get(target.getUUID()).cancel();
                            TASKS.remove(target.getUUID());
                        }
                        UUID targetUniqueId = target.getUUID();
                        BukkitTask task = RunnableManager.runAsyncRepeating(() -> {
                            /*Player p = Bukkit.getPlayer(targetUniqueId);
                            if(p == null) {
                                TASKS.get(targetUniqueId).cancel();
                                TASKS.remove(targetUniqueId);
                                return;
                            }
                            Location particleLoc = p.getLocation();*/

                            BossEvent bossEvent = new CustomBossEvent(new ResourceLocation("null"), new SelectorComponent("@p", Optional.of(new TextComponent(""))));
                            bossEvent.setProgress(-5.0f);
                            bossEvent.setDarkenScreen(true);
                            bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
                            ClientboundBossEventPacket bossEventPacket = ClientboundBossEventPacket.createAddPacket(bossEvent);
                            ClientboundCooldownPacket coolDown = new ClientboundCooldownPacket(null ,100) {
                                @Override
                                public void write(FriendlyByteBuf buf) {
                                    buf.writeVarInt(Integer.MAX_VALUE);
                                    buf.writeVarInt(super.getDuration());
                                }
                            };
                            target.connection.send(bossEventPacket);
                            //p.spawnParticle(Particle.TOTEM, particleLoc.getX(), particleLoc.getY(), particleLoc.getZ(), 10000, 1, 1, 1, 10000, null);
                        }, 0, 10);
                        TASKS.put(target.getUUID(), task);
                        MessageUtil.broadcast(
                                Component.empty()
                                        .append(Component.text("[!]", TextColor.color(16733525), TextDecoration.BOLD, TextDecoration.UNDERLINED))
                                        .append(Component.text(" "))
                                        .append((ctx.getSource().getBukkitEntity() == null ? ctx.getSource().getBukkitSender().name() : ctx.getSource().getBukkitEntity().name()))
                                        .append(Component.text(" が ", TextColor.color(16777045), TextDecoration.BOLD))
                                        .append(target.adventure$displayName)
                                        .append(Component.text(" をクラッシュさせた", TextColor.color(16777045), TextDecoration.BOLD)),
                                ctx.getSource().getEntity() == null ? null : ctx.getSource().getEntity().getUUID(),
                                false);
                    });
                    return targets.size();
                }));
        dispatcher.register(builder);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if(TASKS.containsKey(event.getPlayer().getUniqueId())) {
            TASKS.get(event.getPlayer().getUniqueId()).cancel();
            TASKS.remove(event.getPlayer().getUniqueId());
        }
    }
}
