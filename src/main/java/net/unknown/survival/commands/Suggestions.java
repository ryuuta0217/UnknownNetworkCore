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

package net.unknown.survival.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.chat.CustomChannels;
import net.unknown.survival.chat.channels.ChatChannel;
import net.unknown.survival.chat.channels.CustomChannel;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.HomeGroup;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class Suggestions {
    public static final SuggestionProvider<CommandSourceStack> HOME_SUGGEST = (ctx, builder) -> {
        ServerPlayer target = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : ctx.getSource().getPlayerOrException();
        PlayerData.HomeData data = PlayerData.of(target.getBukkitEntity()).getHomeData();
        HomeGroup group = BrigadierUtil.isArgumentKeyExists(ctx, "グループ") ? data.getGroup(StringArgumentType.getString(ctx, "グループ")) : data.getDefaultGroup();

        if (group == null) return SharedSuggestionProvider.suggest(new String[0], builder);
        else return SharedSuggestionProvider.suggest(group.getHomes().keySet().toArray(new String[0]), builder);
    };
    public static final SuggestionProvider<CommandSourceStack> HOME_GROUP_SUGGEST = (ctx, builder) -> {
        ServerPlayer target = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : ctx.getSource().getPlayerOrException();
        PlayerData.HomeData data = PlayerData.of(target).getHomeData();
        return SharedSuggestionProvider.suggest(data.getGroups().keySet().toArray(new String[0]), builder);
    };
    public static final SuggestionProvider<CommandSourceStack> OFFLINE_HOME_SUGGEST = (ctx, builder) -> {
        try {
            String playerId = StringArgumentType.getString(ctx, "対象");
            UUID uniqueId = Bukkit.getPlayerUniqueId(playerId);
            if (uniqueId != null) {
                PlayerData.HomeData data = PlayerData.of(uniqueId).getHomeData();
                return SharedSuggestionProvider.suggest(data.getDefaultGroup().getHomes().keySet().toArray(new String[0]), builder);
            }
        } catch (IllegalArgumentException ignored) {
        }

        return SharedSuggestionProvider.suggest(new String[0], builder);
    };
    public static final SuggestionProvider<CommandSourceStack> JOINED_CHANNELS_SUGGEST = (ctx, builder) -> {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            return SharedSuggestionProvider.suggest(
                    CustomChannels.getChannels()
                            .values()
                            .stream()
                            .filter(channel -> channel.getPlayers().contains(player.getUUID()))
                            .collect(Collectors.toUnmodifiableSet()),
                    builder,
                    ChatChannel::getChannelName, channel -> MessageUtil.convertAdventure2NMS(channel.getDisplayName()));
        }

        return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
    };

    public static final SuggestionProvider<CommandSourceStack> OWNED_CHANNELS_SUGGEST = (ctx, builder) -> {
        if (ctx.getSource().getEntity() != null && ctx.getSource().getEntity() instanceof Player player) {
            return SharedSuggestionProvider.suggest(
                    CustomChannels.getChannels()
                            .values()
                            .stream()
                            .filter(channel -> channel.getOwner().equals(player.getUUID()))
                            .collect(Collectors.toUnmodifiableSet()),
                    builder, CustomChannel::getChannelName, channel -> MessageUtil.convertAdventure2NMS(channel.getDisplayName()));
        }

        return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
    };
}
