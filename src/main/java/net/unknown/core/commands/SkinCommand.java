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

import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ryuuta0217.util.HTTPUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.BiomeManager;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SkinCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("skin");
        builder.then(Commands.argument("skinPlayerName", StringArgumentType.word())
                .executes(ctx -> {
                    if(!(ctx.getSource().getEntity() instanceof ServerPlayer)) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが実行する必要があります。");
                        return 1;
                    }

                    String skinPlayerName = StringArgumentType.getString(ctx, "skinPlayerName");
                    UUID skinPlayerUniqueId = Bukkit.getPlayerUniqueId(skinPlayerName);
                    if(skinPlayerUniqueId == null) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤー " + skinPlayerName + " は見つかりませんでした");
                        return -1;
                    }

                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    HTTPUtil hu = new HTTPUtil("GET", "https://sessionserver.mojang.com/session/minecraft/profile/" + skinPlayerUniqueId + "?unsigned=false");
                    AtomicReference<String> stoleSkinBase64 = new AtomicReference<>("");
                    AtomicReference<String> stoleSkinSignature = new AtomicReference<>("");
                    try {
                        JSONObject json = (JSONObject) UnknownNetworkCore.getJsonParser().parse(hu.request());
                        if(json.containsKey("properties")) {
                            JSONArray properties = (JSONArray) json.get("properties");
                            stoleSkinBase64.set(((JSONObject) properties.get(0)).get("value").toString());
                            stoleSkinSignature.set(((JSONObject) properties.get(0)).get("signature").toString());
                        }
                    } catch (ParseException e) {
                        MessageUtil.sendErrorMessage(ctx.getSource(), "エラーが発生しました: " + e.getLocalizedMessage());
                        return e.hashCode();
                    }

                    new HashMap<>(player.getGameProfile().getProperties().asMap()).forEach((key, values) -> {
                        new ArrayList<>(values).forEach(value -> {
                           if(key.equals("textures")) {
                               player.getGameProfile().getProperties().remove(key, value);
                               player.getGameProfile().getProperties().put(key, new Property(value.getName(), stoleSkinBase64.get(), stoleSkinSignature.get()));
                           }
                        });
                    });

                    /* Update self view */
                    ClientboundPlayerInfoPacket toRemove = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, player);
                    ClientboundPlayerInfoPacket toAdd = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, player);
                    ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                            player.getLevel().dimensionType(),
                            player.getLevel().dimension(),
                            BiomeManager.obfuscateSeed(player.getLevel().getSeed()),
                            player.gameMode.getGameModeForPlayer(),
                            player.gameMode.getPreviousGameModeForPlayer(), player.getLevel().isDebug(), player.getLevel().isFlat(),
                            true);

                    player.connection.send(toRemove);
                    player.connection.send(toAdd);
                    player.connection.send(respawn);

                    /* Update other view */
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        if(p.getUniqueId().equals(player.getUUID())) return;
                        p.hidePlayer(UnknownNetworkCore.getInstance(), player.getBukkitEntity());
                        p.showPlayer(UnknownNetworkCore.getInstance(), player.getBukkitEntity());
                    });

                    MessageUtil.sendMessage(ctx.getSource(), "スキンを変更しました");
                    return 0;
                }));
        dispatcher.register(builder);
    }
}
