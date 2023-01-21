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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.WorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

public class ProtectedAreaTestStick implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getPlayer().isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.STICK) return;

        event.setCancelled(true);

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR -> {
                // TODO: Notification player's current location is protected area
                // TODO: More protected region information message sent to player
                sendRegionsInformation(event.getPlayer(), null);
            }

            case RIGHT_CLICK_BLOCK -> {
                // TODO: Notification clicked block is protected area
                sendRegionsInformation(event.getPlayer(), event.getInteractionPoint());
            }
        }
    }

    private static void sendRegionsInformation(Player player, @Nullable Location location) {
        Location loc = Optional.ofNullable(location).orElse(player.getLocation());

        Set<ProtectedRegion> regions = WorldGuard.getRegionManager(loc.getWorld())
                .getApplicableRegions(BukkitAdapter.asBlockVector(loc))
                .getRegions();

        if (regions.size() > 0) {
            if (regions.size() == 1) {
                NewMessageUtil.sendMessage(player, Component.text(String.format("%s, %s, %s は保護されています。", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
            } else {
                NewMessageUtil.sendMessage(player, Component.text(String.format("%s, %s, %s には %s 個の保護が存在します。", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), regions.size())));
            }

            regions.forEach(region -> sendRegionInformation(player, region));
        } else {
            NewMessageUtil.sendMessage(player, Component.text(String.format("%s, %s, %s は保護されていません。", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
        }
    }

    private static void sendRegionInformation(Player player, ProtectedRegion region) {
        Matcher idMatcher = WorldGuard.ID_PATTERN.matcher(region.getId());
        UUID createdBy = idMatcher.matches() ? UUID.fromString(region.getId().split(WorldGuard.SPLITTER, 2)[0]) : null;
        String regionName = idMatcher.matches() ? region.getId().split(WorldGuard.SPLITTER, 2)[1] : region.getId();
        NewMessageUtil.sendMessage(player, Component.text("保護領域 " + regionName + " | 作成者: " + (createdBy != null ? Bukkit.getOfflinePlayer(createdBy).getName() : "?")));
    }
}
