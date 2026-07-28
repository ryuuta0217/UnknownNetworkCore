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

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidTriggerEvent;

import java.util.*;

public class SuppressRaids extends ConfigurationBase implements Listener {
    private static final SuppressRaids INSTANCE = new SuppressRaids();

    public static SuppressRaids getInstance() {
        return INSTANCE;
    }

    public static void registerListener() {
        ListenerManager.registerListener(INSTANCE);
    }

    private Map<ResourceKey<Level>, Set<Pair<BlockPos, Double>>> suppressRaids;

    private SuppressRaids() {
        super("suppress-raids.yml", false, "UNC/SuppressRaids");
    }

    @Override
    public void onLoad() {
        if (this.suppressRaids == null) this.suppressRaids = new HashMap<>();
        this.suppressRaids.clear();
        if (this.getConfig().contains("suppress-raids")) {
            ConfigurationSection suppressRaidsSection = this.getConfig().getConfigurationSection("suppress-raids");
            suppressRaidsSection.getKeys(false).forEach(levelKey -> {
                ConfigurationSection levelSection = suppressRaidsSection.getConfigurationSection(levelKey);
                ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(levelKey));
                Set<Pair<BlockPos, Double>> leveledSuppressRaids = new HashSet<>();
                levelSection.getKeys(false).forEach(centerPosKey -> {
                    ConfigurationSection suppressRaidSection = levelSection.getConfigurationSection(centerPosKey);
                    String[] centerPosStr = centerPosKey.split(",");
                    BlockPos centerPos = new BlockPos(Integer.parseInt(centerPosStr[0]), Integer.parseInt(centerPosStr[1]), Integer.parseInt(centerPosStr[2]));
                    double suppressRadius = suppressRaidSection.getDouble("radius");
                    leveledSuppressRaids.add(Pair.of(centerPos, suppressRadius));
                });
                this.suppressRaids.put(level, leveledSuppressRaids);
            });
        }
    }

    public Map<ResourceKey<Level>, Set<Pair<BlockPos, Double>>> getSuppressRaids() {
        return Collections.unmodifiableMap(this.suppressRaids);
    }

    @Override
    public synchronized void save() {
        this.getConfig().set("suppress-raids", null);
        ConfigurationSection suppressRaidsSection = this.getConfig().createSection("suppress-raids");
        this.suppressRaids.forEach((levelKey, leveledSuppressRaids) -> {
            ConfigurationSection levelSection = suppressRaidsSection.createSection(levelKey.identifier().toString());
            leveledSuppressRaids.forEach(suppressRaid -> {
                BlockPos centerPos = suppressRaid.getLeft();
                ConfigurationSection suppressRaidSection = levelSection.createSection(centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ());
                suppressRaidSection.set("radius", suppressRaid.getRight());
            });
        });
        super.save();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRaidTriggered(RaidTriggerEvent event) {
        Location raidLocationBukkit = event.getRaid().getLocation();
        ServerLevel raidLevel = MinecraftAdapter.level(raidLocationBukkit.getWorld());
        Vec3 raidLocation = MinecraftAdapter.vec3(raidLocationBukkit);
        if (this.suppressRaids.containsKey(raidLevel.dimension())) {
            boolean suppress = this.suppressRaids.get(raidLevel.dimension()).stream()
                    .anyMatch(suppressRaid -> {
                        double distance = Vec3.atCenterOf(suppressRaid.getLeft()).distanceTo(raidLocation);
                        System.out.println(distance + " <= " + suppressRaid.getRight());
                        return distance <= suppressRaid.getRight();
                    });
            event.setCancelled(suppress);
            if (suppress) {
                raidLocationBukkit.getWorld().getPlayers().forEach(player -> {
                    NewMessageUtil.sendMessage(player, net.kyori.adventure.text.Component.text(raidLocation.x() + ", " + raidLocation.y() + ", " + raidLocation.z() + "で発生した襲撃が抑制されました。", DefinedTextColor.AQUA), false);
                });
            }
        }
    }
}
