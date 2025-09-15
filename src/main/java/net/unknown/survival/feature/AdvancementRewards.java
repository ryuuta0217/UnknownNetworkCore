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

package net.unknown.survival.feature;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.queue.ItemGiveQueue;
import net.unknown.survival.wrapper.economy.WrappedEconomy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class AdvancementRewards implements Listener {
    public static final NamespacedKey PLAYER_REGISTRY_STATISTICS_KEY = new NamespacedKey("survival", "advancement_rewards");
    private static List<Advancement> AVAILABLE_ADVANCEMENTS;
    private static List<Advancement> AVAILABLE_ADVANCEMENT_RECIPES;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        test(event.getPlayer());
        List<Advancement> uncompletedAdvancements = getUncompletedAdvancements(event.getPlayer());
        List<Advancement> uncompletedAdvancementRecipes = getUncompletedAdvancementRecipes(event.getPlayer());
        if (!uncompletedAdvancements.isEmpty()) {
            int remainingCriteria = 0;
            for (Advancement advancement : uncompletedAdvancements) {
                remainingCriteria += event.getPlayer().getAdvancementProgress(advancement).getRemainingCriteria().size();
            }
            final int remainingCriteriaFinal = remainingCriteria;
            RunnableManager.runDelayed(() -> NewMessageUtil.sendMessage(event.getPlayer(), Component.text("未達成の進捗が" + uncompletedAdvancements.size() + "(" + remainingCriteriaFinal + ")個あります。 すべて達成すると、報酬を獲得できます。").color(DefinedTextColor.YELLOW), false), 20L);
        }

        if (!uncompletedAdvancementRecipes.isEmpty()) {
            RunnableManager.runDelayed(() -> NewMessageUtil.sendMessage(event.getPlayer(), Component.text("未開放のレシピが" + uncompletedAdvancementRecipes.size() + "個あります。").color(DefinedTextColor.YELLOW), false), 20L);
        }
    }

    @EventHandler
    public void onAdvancementCriteriaDone(PlayerAdvancementCriterionGrantEvent event) {
        if (event.getAdvancement().getKey().getKey().startsWith("recipes/")) {
            List<Advancement> uncompletedAdvancementRecipes = getUncompletedAdvancementRecipes(event.getPlayer());
            if (event.getAdvancementProgress().isDone()) event.getPlayer().sendActionBar(Component.text("レシピを開放しました。 解放されていないレシピはあと" + uncompletedAdvancementRecipes.size() + "個あります。").color(DefinedTextColor.YELLOW));
            return;
        }
        if (event.getAdvancementProgress().isDone()) test(event.getPlayer());
        List<Advancement> uncompletedAdvancements = getUncompletedAdvancements(event.getPlayer());
        int remainingCriteria = 0;
        for (Advancement advancement : uncompletedAdvancements) {
            remainingCriteria += event.getPlayer().getAdvancementProgress(advancement).getRemainingCriteria().size();
        }
        if (event.getAdvancementProgress().isDone()) event.getPlayer().sendActionBar(Component.text("進捗を達成しました。 達成していない進捗はあと" + uncompletedAdvancements.size() + "(" + remainingCriteria + ")個あります。").color(DefinedTextColor.YELLOW));
        else event.getPlayer().sendActionBar(Component.empty().append(Component.text("進捗")).append(event.getAdvancement().displayName()).append(Component.text("の進捗条件 " + event.getCriterion() + " を達成しました。残りの条件は" + event.getAdvancementProgress().getRemainingCriteria().size() + "個あります。")));
    }

    private static void test(Player player) {
        if (getUncompletedAdvancements(player).isEmpty()) {
            if (isCompleted(player)) return;

            Bukkit.broadcast(Component.empty().append(player.displayName()).append(Component.text("さんがすべての進捗を達成しました")).color(DefinedTextColor.GOLD));
            setCompleted(player, true);
            setCompletedAt(player, System.currentTimeMillis());
            setEarnedAdvancementCount(player, getAvailableAdvancements().size());
            setCompletionCount(player, getCompletionCount(player) + 1);
            List<ItemStack> rewards = new ArrayList<>() {{
                add(new ItemStackBuilder(Material.NETHERITE_BLOCK, 16)
                        .lore(Component.text("全進捗達成報酬").color(DefinedTextColor.YELLOW))
                        .build());

                add(new ItemStackBuilder(Material.NETHERITE_PICKAXE, 1)
                        .lore(Component.text("全進捗達成報酬").color(DefinedTextColor.YELLOW))
                        .addEnchantment(Enchantment.EFFICIENCY, 10)
                        .addEnchantment(Enchantment.FORTUNE, 10)
                        .custom(is -> {
                            is.canRepair(new ItemStack(Material.AIR));
                            is.setData(DataComponentTypes.REPAIR_COST, Integer.MAX_VALUE);
                            is.editMeta(meta -> meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(UUID.randomUUID(), "block_break_speed", 10.0D, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND)));
                        })
                        .build());
            }};

            Component message = Component.text("報酬を受け取りました:");
            for (ItemStack reward : rewards) {
                ItemGiveQueue.queue(player.getUniqueId(), reward);
                message = message.appendNewline().append(Component.text(" - ")).append(reward.displayName().hoverEvent(reward.asHoverEvent()));
            }
            WrappedEconomy.INSTANCE.depositPlayer(player, 50_000);
            message = message.appendNewline().append(Component.text(" - 50,000円"));
            player.giveExp(100_000, true);
            message = message.appendNewline().append(Component.text(" - 100,000xp"));

            NewMessageUtil.sendMessage(player, message, false);
        }
    }

    public static void updateAvailableAdvancements() {
        List<Advancement> advancements = new ArrayList<>();
        List<Advancement> advancementRecipes = new ArrayList<>();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        iterator.forEachRemaining(advancement -> {
            if (!advancement.getKey().getKey().startsWith("recipes/")) {
                advancements.add(advancement);
            } else {
                advancementRecipes.add(advancement);
            }
        });
        AVAILABLE_ADVANCEMENTS = advancements;
        AVAILABLE_ADVANCEMENT_RECIPES = advancementRecipes;
    }

    public static List<Advancement> getAvailableAdvancements() {
        if (AVAILABLE_ADVANCEMENTS == null) {
            updateAvailableAdvancements();
        }
        return AVAILABLE_ADVANCEMENTS;
    }

    public static List<Advancement> getAvailableAdvancementRecipes() {
        if (AVAILABLE_ADVANCEMENT_RECIPES == null) {
            updateAvailableAdvancements();
        }
        return AVAILABLE_ADVANCEMENT_RECIPES;
    }

    public static List<Advancement> getUncompletedAdvancements(Player player) {
        return getAvailableAdvancements()
                .stream()
                .filter(advancement -> !player.getAdvancementProgress(advancement).isDone())
                .toList();
    }

    public static List<Advancement> getUncompletedAdvancementRecipes(Player player) {
        return getAvailableAdvancementRecipes()
                .stream()
                .filter(advancement -> !player.getAdvancementProgress(advancement).isDone())
                .toList();
    }

    public static boolean resetProgress(Player player) {
        if (isCompleted(player)) {
            addCompletionHistory(player, getCompletedAt(player), getEarnedAdvancementCount(player));
            setCompleted(player, false);
            setCompletedAt(player, -1);
            setEarnedAdvancementCount(player, 0);
        }

        getAvailableAdvancements().forEach(advancement -> {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            progress.getAwardedCriteria().forEach(progress::revokeCriteria);
        });
        return getUncompletedAdvancements(player).size() == getAvailableAdvancements().size();
    }

    public static boolean isCompleted(Player player) {
        Map<String, String> playerRegistry = PlayerData.of(player).getRegistries().getRegistry(PLAYER_REGISTRY_STATISTICS_KEY);
        return Boolean.parseBoolean(playerRegistry.getOrDefault("completed", "false"));
    }

    public static void setCompleted(Player player, boolean completed) {
        PlayerData.of(player).getRegistries().put(PLAYER_REGISTRY_STATISTICS_KEY, "completed", String.valueOf(completed));
    }

    public static int getEarnedAdvancementCount(Player player) {
        Map<String, String> playerRegistry = PlayerData.of(player).getRegistries().getRegistry(PLAYER_REGISTRY_STATISTICS_KEY);
        return Integer.parseInt(playerRegistry.getOrDefault("earned_advancement_count", "0"));
    }

    public static void setEarnedAdvancementCount(Player player, int count) {
        PlayerData.of(player).getRegistries().put(PLAYER_REGISTRY_STATISTICS_KEY, "earned_advancement_count", String.valueOf(count));
    }

    public static int getCompletionCount(Player player) {
        Map<String, String> playerRegistry = PlayerData.of(player).getRegistries().getRegistry(PLAYER_REGISTRY_STATISTICS_KEY);
        return Integer.parseInt(playerRegistry.getOrDefault("completion_count", "0"));
    }

    public static void setCompletionCount(Player player, int count) {
        PlayerData.of(player).getRegistries().put(PLAYER_REGISTRY_STATISTICS_KEY, "completion_count", String.valueOf(count));
    }

    public static long getCompletedAt(Player player) {
        Map<String, String> playerRegistry = PlayerData.of(player).getRegistries().getRegistry(PLAYER_REGISTRY_STATISTICS_KEY);
        return Long.parseLong(playerRegistry.getOrDefault("completed_at", "0"));
    }

    public static void setCompletedAt(Player player, long timestamp) {
        PlayerData.of(player).getRegistries().put(PLAYER_REGISTRY_STATISTICS_KEY, "completed_at", String.valueOf(timestamp));
    }

    public static LinkedHashMap<Long, Integer> getCompletionHistory(Player player) {
        Map<String, String> playerRegistry = PlayerData.of(player).getRegistries().getRegistry(PLAYER_REGISTRY_STATISTICS_KEY);
        JSONObject historyJson = new JSONObject(playerRegistry.getOrDefault("history", "{}"));
        LinkedHashMap<Long, Integer> history = historyJson.toMap()
                .entrySet()
                .parallelStream()
                .map(e -> Map.entry(Long.parseLong(e.getKey()), e.getValue() instanceof Integer ? (Integer) e.getValue() : 0))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (before, after) -> before, LinkedHashMap::new));
        return history;
    }

    public static void setCompletionHistory(Player player, Map<Long, Integer> history) {
        PlayerData.of(player).getRegistries().put(PLAYER_REGISTRY_STATISTICS_KEY, "history", new JSONObject(history).toString());
    }

    public static void addCompletionHistory(Player player, long completedAt, int advancementCount) {
        LinkedHashMap<Long, Integer> history = getCompletionHistory(player);
        history.put(completedAt, advancementCount);
        setCompletionHistory(player, history);
    }

    public static void removeCompletionHistory(Player player, long completedAt) {
        LinkedHashMap<Long, Integer> history = getCompletionHistory(player);
        history.remove(completedAt);
        setCompletionHistory(player, history);
    }
}

