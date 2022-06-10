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

package net.unknown.survival.listeners;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.filoghost.holographicdisplays.api.beta.hologram.Hologram;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.UnknownNetworkSurvival;
import net.unknown.survival.dependency.HolographicDisplays;
import org.bukkit.*;
import org.bukkit.block.Skull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerDeathListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger("UNC/Graveyard");

    private static final Map<UUID, Map<Location, List<String>>> DEATH_ITEMS = new HashMap<>();
    private static final Map<UUID, Map<Location, Long>> REMOVAL_TIMES = new HashMap<>();
    private static final Map<UUID, Map<Location, BukkitTask>> REMOVAL_TASKS = new HashMap<>();

    private static final Map<Location, Hologram> HOLOGRAMS = new HashMap<>();

    private static final File CONFIG_FILE = new File(UnknownNetworkCore.getInstance().getDataFolder(), "death-items.yml");
    private static YamlConfiguration CONFIG;

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(event.getKeepInventory()) return;
        if(event.getDrops().size() == 0) return;

        List<ItemStack> items = event.getDrops()
                .stream()
                .map(bis -> ((CraftItemStack) bis).handle)
                .toList();

        Location playerPos = event.getPlayer().getLocation().toBlockLocation();
        Location blockPos = new Location(playerPos.getWorld(), playerPos.getX(), playerPos.getY(), playerPos.getZ()); // for Block Event Location Normalize
        if(blockPos.getBlock().getType() != Material.AIR) {
            Set<Location> airs = new HashSet<>();

            for(int x = -3; x <= 3; x++) {
                for(int y = -3; y <= 3; y++) {
                    for(int z = -3; z <= 3; z++) {
                        Location l = event.getPlayer().getLocation().toBlockLocation().add(x, y, z);
                        if(l.getBlock().getType() == Material.AIR) {
                            airs.add(l);
                        }
                    }
                }
            }

            Optional<Location> optionalBlockPos = airs.stream().findAny();
            if(optionalBlockPos.isPresent()) {
                Location loc = optionalBlockPos.get().toBlockLocation();
                blockPos = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()); // for Block Event Location Normalize
            } else {
                NewMessageUtil.sendErrorMessage(event.getPlayer(), "死亡地点周辺にスペースがなかったため、墓を生成できませんでした。");
                return;
            }
        }

        blockPos.getBlock().setType(Material.PLAYER_HEAD);
        Skull skull = ((Skull) blockPos.getBlock().getState());
        skull.setOwningPlayer(event.getPlayer());
        skull.update();

        List<String> itemJsonSet = items.stream().map(is -> {
            return is.save(new CompoundTag()).getAsString(); // for load, use ItemStack.of(TagParser.parse(...))
        }).toList();

        event.getDrops().clear();

        if(!DEATH_ITEMS.containsKey(event.getPlayer().getUniqueId())) DEATH_ITEMS.put(event.getPlayer().getUniqueId(), new HashMap<Location, List<String>>());
        DEATH_ITEMS.get(event.getPlayer().getUniqueId()).put(blockPos, itemJsonSet);

        if(!REMOVAL_TIMES.containsKey(event.getPlayer().getUniqueId())) REMOVAL_TIMES.put(event.getPlayer().getUniqueId(), new HashMap<>());
        REMOVAL_TIMES.get(event.getPlayer().getUniqueId()).put(blockPos, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30));

        if(!REMOVAL_TASKS.containsKey(event.getPlayer().getUniqueId())) REMOVAL_TASKS.put(event.getPlayer().getUniqueId(), new HashMap<>());
        REMOVAL_TASKS.get(event.getPlayer().getUniqueId()).put(blockPos, getTask(event.getPlayer().getUniqueId(), blockPos, millisToTicks(TimeUnit.MINUTES.toMillis(30))));

        NewMessageUtil.sendMessage(event.getPlayer(), new TextComponent("")
                .append("死亡地点に ")
                .append(getGraveyardComponent(blockPos))
                .append(" が生成されました。\n" +
                        millisToFormatted(REMOVAL_TIMES.get(event.getPlayer().getUniqueId()).get(blockPos)) + " に自然に還ります。"));

        if(UnknownNetworkSurvival.isHolographicDisplaysEnabled()) {
            Hologram holo = HolographicDisplays.get().createHologram(blockPos.clone().add(0.5, 1, 0.5));
            HOLOGRAMS.put(blockPos, holo);
            holo.getLines().appendText(ChatColor.RED + "" + event.getPlayer().getName() + " の墓");
        }
        RunnableManager.runAsync(PlayerDeathListener::save);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(event.getBlock().getType() == Material.PLAYER_HEAD) {
            if (event.getBlock().getState() instanceof Skull skull) {
                OfflinePlayer oPlayer = skull.getOwningPlayer();
                if (oPlayer == null) return;
                Location blockPos = event.getBlock().getLocation();
                if (DEATH_ITEMS.get(oPlayer.getUniqueId()).containsKey(blockPos)) {
                    if(!oPlayer.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }
                    ServerLevel level = ((CraftWorld) event.getPlayer().getLocation().getWorld()).getHandle();
                    DEATH_ITEMS.get(event.getPlayer().getUniqueId())
                            .get(blockPos)
                            .stream()
                            .map(json -> {
                                try {
                                    return ItemStack.of(TagParser.parseTag(json));
                                } catch (CommandSyntaxException e) {
                                    e.printStackTrace();
                                    LOGGER.severe("Failed to parse Item from JSON");
                                }
                                return ItemStack.EMPTY;
                            })
                            .map(is -> {
                                if (is.getItem() != null) {
                                    Location loc = event.getPlayer().getLocation();
                                    return new ItemEntity(level, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), is, 0, 0, 0);
                                }
                                return null;
                            }).forEach(entity -> {
                                if (entity != null) {
                                    level.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.DEFAULT);
                                }
                            });

                    REMOVAL_TASKS.get(event.getPlayer().getUniqueId()).get(blockPos).cancel();
                    REMOVAL_TASKS.get(event.getPlayer().getUniqueId()).remove(blockPos);
                    REMOVAL_TIMES.get(event.getPlayer().getUniqueId()).remove(blockPos);
                    DEATH_ITEMS.get(event.getPlayer().getUniqueId()).remove(blockPos);
                    if(UnknownNetworkSurvival.isHolographicDisplaysEnabled()) {
                        HOLOGRAMS.get(blockPos).delete();
                        HOLOGRAMS.remove(blockPos);
                    }
                    event.setDropItems(false);
                    RunnableManager.runAsync(PlayerDeathListener::save);
                } else {
                    if (DEATH_ITEMS.entrySet().stream().anyMatch(e -> e.getValue().containsKey(event.getBlock().getLocation()))) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private static BukkitTask getTask(UUID uniqueId, Location location, long delay) {
        return RunnableManager.runDelayed(() -> {
            DEATH_ITEMS.get(uniqueId).remove(location);
            REMOVAL_TIMES.get(uniqueId).remove(location);
            REMOVAL_TASKS.get(uniqueId).remove(location);
            location.getBlock().setType(Material.AIR);
            if(Bukkit.getOfflinePlayer(uniqueId).isOnline()) {
                NewMessageUtil.sendMessage(Bukkit.getPlayer(uniqueId), new TextComponent("")
                        .append(getGraveyardComponent(location))
                        .append(" は自然に還りました。アイテムは回収できません。"));
            }
        }, delay);
    }

    private static long millisToTicks(long millis) {
        // 1 second = 1000 ms
        // 1 second = 20 tick
        // 1 tick = 50 ms
        return millis / 50;
    }

    private static String millisToFormatted(long millis) {
        return DateTimeFormatter.ofPattern("MM/dd HH:mm:ss")
                .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis),
                        ZoneId.of("Asia/Tokyo")));
    }

    private static Component getGraveyardComponent(Location loc) {
        return new TextComponent("[墓]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new TextComponent("")
                                        .append(new TextComponent("====== 墓の情報 =====\n").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                                        .append(new TextComponent("ワールド: " + MessageUtil.getWorldName(loc.getWorld()) + "\n").withStyle(ChatFormatting.YELLOW)
                                                .append("座標: " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ())))));
    }

    public static void load() {
        DEATH_ITEMS.clear();
        REMOVAL_TIMES.clear();
        REMOVAL_TASKS.forEach((p, m) -> {
            m.forEach((l, t) -> {
                t.cancel();
            });
        });
        REMOVAL_TASKS.clear();
        if(UnknownNetworkSurvival.isHolographicDisplaysEnabled()) {
            HOLOGRAMS.forEach((l, h) -> {
                if(!h.isDeleted()) h.delete();
            });
            HOLOGRAMS.clear();
        }

        try {
            if(CONFIG_FILE.exists() || !CONFIG_FILE.exists() && CONFIG_FILE.createNewFile()) {
                CONFIG = YamlConfiguration.loadConfiguration(CONFIG_FILE);

                if (CONFIG.contains("death_items")) {
                    ConfigurationSection deathItems = CONFIG.getConfigurationSection("death_items");
                    deathItems.getKeys(false).forEach(uuid -> {
                        UUID uniqueId = UUID.fromString(uuid);
                        ConfigurationSection numbers = deathItems.getConfigurationSection(uuid);
                        numbers.getKeys(false).forEach(i -> {
                            ConfigurationSection data = numbers.getConfigurationSection(i);
                            long toRemovalMillis = data.getLong("removal_time");
                            if(toRemovalMillis > System.currentTimeMillis()) {
                                Location loc = ConfigurationSerializer.getLocationData(data, "location");
                                List<String> items = data.getStringList("items");

                                Map<Location, List<String>> deathItemsMap = new HashMap<>();
                                deathItemsMap.put(loc, new ArrayList<>(items));
                                DEATH_ITEMS.put(uniqueId, deathItemsMap);

                                Map<Location, Long> removalTimesMap = new HashMap<>();
                                removalTimesMap.put(loc, toRemovalMillis);
                                REMOVAL_TIMES.put(uniqueId, removalTimesMap);

                                Map<Location, BukkitTask> removalTasksMap = new HashMap<>();
                                removalTasksMap.put(loc, getTask(uniqueId, loc, millisToTicks(toRemovalMillis - System.currentTimeMillis())));
                                REMOVAL_TASKS.put(uniqueId, removalTasksMap);
                            }
                        });
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.warning("Failed to load file.");
        }
    }

    private synchronized static void save() {
        CONFIG.set("death_items", null);

        DEATH_ITEMS.forEach((uuid, deaths) -> {
            AtomicInteger num = new AtomicInteger(1);
            Map<Location, Long> removalTimes = REMOVAL_TIMES.get(uuid);

            deaths.forEach((loc, items) -> {
                long removalTime = removalTimes.get(loc);
                CONFIG.set("death_items." + uuid.toString() + "." + num.get() + ".removal_time", removalTime);
                ConfigurationSerializer.setLocationData(CONFIG, "death_items." + uuid + "." + num.get() + ".location", loc);
                CONFIG.set("death_items." + uuid.toString() + "." + num.get() + ".items", items);
                num.incrementAndGet();
            });
        });

        try {
            CONFIG.save(CONFIG_FILE);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.warning("Failed to save death items");
        }
    }
}
