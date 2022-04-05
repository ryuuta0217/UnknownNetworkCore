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

package net.unknown.survival.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.core.commands.Suggestions;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.PlayerDataUtil;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class LastTpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("lasttp");
        builder.requires(Permissions.COMMAND_LASTTP::checkAndIsLivingEntity);
        builder.then(Commands.argument("target", StringArgumentType.word())
                .suggests(Suggestions.ALL_PLAYER_SUGGEST)
                .executes(ctx -> {
                    String targetNameStr = StringArgumentType.getString(ctx, "target");
                    UUID targetUniqueId = Bukkit.getPlayerUniqueId(targetNameStr);

                    if (targetUniqueId != null) {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUniqueId);
                        if (!target.isOnline()) {
                            try {
                                Location loc = PlayerDataUtil.getLastLocation(target);
                                ctx.getSource().getBukkitEntity().teleportAsync(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
                                MessageUtil.sendAdminMessage(ctx.getSource(), targetNameStr + " の最終地点にテレポートしました");
                            } catch (Exception e) {
                                e.printStackTrace();
                                MessageUtil.sendAdminErrorMessage(ctx.getSource(), e.getMessage());
                            }
                        } else {
                            ctx.getSource().getBukkitEntity().teleportAsync(target.getPlayer().getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                            MessageUtil.sendAdminMessage(ctx.getSource(), targetNameStr + " はオンラインのため、現在地点にテレポートしました");
                        }
                    } else {
                        MessageUtil.sendAdminErrorMessage(ctx.getSource(), "プレイヤーが見つかりません");
                    }
                    return 0;
                }));
        dispatcher.register(builder);
    }
}
