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

package net.unknown.core.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import net.minecraft.advancements.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unknown.core.advancements.event.CustomAdvancementCompletedEvent;
import net.unknown.core.advancements.event.CustomAdvancementCriteriaGrantedEvent;
import net.unknown.core.advancements.event.CustomAdvancementCriteriaRevokedEvent;
import net.unknown.core.builder.advancement.DisplayInfoBuilder;
import net.unknown.shared.SharedConstants;
import org.bukkit.Bukkit;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class AdvancementManager {
    private static final Logger LOGGER = Logger.getLogger("UNC/Advancements");
    private static final AdvancementProgress.Serializer PROGRESS_SERIALIZER = new AdvancementProgress.Serializer();

    private static final Map<ResourceLocation, Advancement> ADVANCEMENTS = new HashMap<>();
    private static final Map<ResourceLocation, Map<UUID, AdvancementProgress>> PROGRESSES = new HashMap<>();

    public static void register(Advancement advancement) {
        if (ADVANCEMENTS.containsKey(advancement.getId())) LOGGER.warning("Advancement " + advancement.getId() + " is already registered. Overwriting.");
        ADVANCEMENTS.put(advancement.getId(), advancement);
        PROGRESSES.put(advancement.getId(), new HashMap<>());
    }

    public static void resetProgress(UUID uniqueId, ResourceLocation id) {
        if (!ADVANCEMENTS.containsKey(id)) throw new IllegalArgumentException("Unknown advancement " + id);
        Advancement advancement = ADVANCEMENTS.get(id);
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(advancement.getCriteria(), advancement.getRequirements());
        PROGRESSES.get(id).put(uniqueId, progress);
    }

    public static void resetProgress(UUID uniqueId, Advancement advancement) {
        resetProgress(uniqueId, advancement.getId());
    }

    public static Advancement getAdvancement(ResourceLocation id) {
        return ADVANCEMENTS.getOrDefault(id, null);
    }

    public static AdvancementProgress getProgress(ServerPlayer player, ResourceLocation id) {
        if (!PROGRESSES.containsKey(id)) throw new IllegalArgumentException("Unknown advancement " + id);
        Map<UUID, AdvancementProgress> progresses = PROGRESSES.get(id);
        if (!progresses.containsKey(player.getUUID())) resetProgress(player.getUUID(), id);
        return progresses.get(player.getUUID());
    }

    public static AdvancementProgress getProgress(ServerPlayer player, Advancement advancement) {
        return getProgress(player, advancement.getId());
    }

    public static boolean grantProgress(ServerPlayer player, ResourceLocation id, String name) {
        AdvancementProgress progress = getProgress(player, id);
        boolean granted = progress.grantProgress(name);
        if (granted) {
            ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Collections.emptySet(), Collections.singletonMap(id, progress));
            player.connection.send(packet);
            Bukkit.getPluginManager().callEvent(new CustomAdvancementCriteriaGrantedEvent(!Bukkit.isPrimaryThread(), player, getAdvancement(id), progress, name));
            if (progress.isDone()) {
                Bukkit.getPluginManager().callEvent(new CustomAdvancementCompletedEvent(!Bukkit.isPrimaryThread(), player, getAdvancement(id), progress));
            }
            return true;
        }
        return false;
    }

    public static boolean grantProgress(ServerPlayer player, Advancement advancement, String name) {
        return grantProgress(player, advancement.getId(), name);
    }

    public static boolean revokeProgress(ServerPlayer player, ResourceLocation id, String name) {
        AdvancementProgress progress = getProgress(player, id);
        boolean revoked = progress.revokeProgress(name);
        if (revoked) {
            ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Collections.emptySet(), Collections.singletonMap(id, progress));
            player.connection.send(packet);
            Bukkit.getPluginManager().callEvent(new CustomAdvancementCriteriaRevokedEvent(!Bukkit.isPrimaryThread(), player, getAdvancement(id), progress, name));
            return true;
        }

        return false;
    }

    public static boolean revokeProgress(ServerPlayer player, Advancement advancement, String name) {
        return revokeProgress(player, advancement.getId(), name);
    }

    public static void save(ServerPlayer player) {
        File folder = new File(SharedConstants.DATA_FOLDER, "advancements");
        if (folder.exists() || (!folder.exists() && folder.mkdirs())) {
            ADVANCEMENTS.forEach((id, advancement) -> {
                AdvancementProgress progress = getProgress(player, id);
                JsonElement progressJson = PROGRESS_SERIALIZER.serialize(progress, AdvancementProgress.class, new JsonSerializationContext() {
                    @Override
                    public JsonElement serialize(Object o) {
                        return new JsonPrimitive(o.toString());
                    }

                    @Override
                    public JsonElement serialize(Object o, Type type) {
                        return new JsonPrimitive(o.toString());
                    }
                });
            });
        }
    }

    public static void registerDebug() {
        Advancement.Builder builder = Advancement.Builder
                .advancement()
                .display(new DisplayInfoBuilder()
                        .icon(new ItemStack(Items.TORCH))
                        .title(Component.literal("UN Pass"))
                        .description(Component.literal("Unknown Network Pass"))
                        .background(ResourceLocation.of("minecraft:textures/block/dirt.png", ':'))
                        .announceChat(false)
                        .showToast(false)
                        .build());

        IntStream.rangeClosed(1, 10).forEach(i -> {
            builder.addCriterion(String.valueOf(i), CriteriaTriggers.IMPOSSIBLE.createInstance(null, null));
        });

        Advancement adv = builder.build(ResourceLocation.of("unpass:root", ':'));
        register(adv);
    }

    public static void send(ServerPlayer player) {
        List<Advancement> toEarn = new ArrayList<>();
        Set<ResourceLocation> toRemove = new HashSet<>();
        Map<ResourceLocation, AdvancementProgress> toSetProgress = new HashMap<>();

        ADVANCEMENTS.forEach((id, advancement) -> {
            if(!PROGRESSES.containsKey(id)) return;
            toEarn.add(advancement);
            toRemove.add(advancement.getId());
            toSetProgress.put(id, getProgress(player, id));
        });

        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(false, toEarn, toRemove, toSetProgress);
        player.connection.send(packet);
    }
}
