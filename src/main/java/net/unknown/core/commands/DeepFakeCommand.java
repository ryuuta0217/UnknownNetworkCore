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

package net.unknown.core.commands;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.SkinManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class DeepFakeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("deepfake");
        builder.requires(source -> source.hasPermission(4) && source.getEntity() instanceof Player);
        builder.then(Commands.argument("プレイヤー名", StringArgumentType.word())
                .suggests(Suggestions.ALL_PLAYER_SUGGEST)
                .executes(ctx -> {
                    org.bukkit.entity.Player source = (org.bukkit.entity.Player) ctx.getSource().getBukkitEntity();
                    String executorName = source.getName();
                    String playerName = StringArgumentType.getString(ctx, "プレイヤー名");
                    UUID playerUniqueId = Bukkit.getPlayerUniqueId(playerName);
                    if (playerUniqueId == null) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが見つかりません");
                        return 1;
                    }

                    ListenerManager.waitForEvent(AsyncPlayerPreLoginEvent.class, false, EventPriority.LOWEST,
                            (preLoginEvent) -> {
                                return preLoginEvent.getUniqueId().equals(source.getUniqueId());
                            }, (preLoginEvent) -> {
                                PlayerProfile profile = new CraftPlayerProfile(playerUniqueId, playerName);
                                SkinManager.Skin skinData = SkinManager.getSkinInRemote(playerUniqueId);
                                profile.setProperty(skinData.asProfileProperty());
                                preLoginEvent.setPlayerProfile(profile);
                                ListenerManager.waitForEvent(PlayerJoinEvent.class, false, EventPriority.MONITOR, (joinEvent) -> {
                                    return joinEvent.getPlayer().getUniqueId().equals(playerUniqueId);
                                }, (e) -> {
                                    MessageUtil.sendMessage(e.getPlayer(), source.getName() + " ではなく、" + playerName + " に成りすましてログインしています。");
                                }, 5, ListenerManager.TimeType.SECONDS, () -> {
                                });
                            }, 1, ListenerManager.TimeType.MINUTES, () -> {
                                if (Bukkit.getOfflinePlayer(source.getUniqueId()).isOnline()) {
                                    MessageUtil.sendErrorMessage(ctx.getSource(), "1分が経過しました。");
                                }
                            });

                    MessageUtil.sendMessage(ctx.getSource(), "1分以内にリログしてください。");
                    return 0;
                }));
        dispatcher.register(builder);
    }
}
