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

package net.unknown.core.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.ProblemReporter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class PlayerDataUtil {
    public static Location getLastLocation(OfflinePlayer offlinePlayer) {
        CompoundTag tag = getData(offlinePlayer);
        if (tag == null) throw new IllegalArgumentException("プレイヤーが見つかりません");

        if (tag.contains("WorldUUIDMost") && tag.contains("WorldUUIDLeast") && tag.contains("Pos") && tag.contains("Rotation")) {
            UUID worldUniqueId = new UUID(tag.getLongOr("WorldUUIDMost", 0L), tag.getLongOr("WorldUUIDLeast", 0L));
            World world = Bukkit.getWorld(worldUniqueId);
            if (world == null) throw new IllegalStateException("ワールドが見つかりません");

            ListTag position = tag.getListOrEmpty("Pos");
            double x = position.getDoubleOr(0, 0);
            double y = position.getDoubleOr(1, 120);
            double z = position.getDoubleOr(2, 0);

            ListTag rotation = tag.getListOrEmpty("Rotation");
            float yaw = rotation.getFloatOr(0, 0);
            float pitch = rotation.getFloatOr(1, 0);

            return new Location(world, x, y, z, yaw, pitch);
        }
        throw new IllegalStateException("プレイヤーデータに異常があります");
    }

    public static List<ItemStack> getInventoryItems(OfflinePlayer offlinePlayer) {
        CompoundTag tag = getData(offlinePlayer);
        if (tag == null) throw new IllegalArgumentException("プレイヤーが見つかりません");

        if (tag.contains("Inventory")) {
            ListTag items = tag.getListOrEmpty("Inventory");

        }
        throw new IllegalStateException("プレイヤーデータに異常があります");
    }

    @Nullable
    public static CompoundTag getData(OfflinePlayer offlinePlayer) {
        return offlinePlayer.hasPlayedBefore() ? DedicatedServer.getServer().playerDataStorage.load(offlinePlayer.getName(), offlinePlayer.getUniqueId().toString(), ProblemReporter.DISCARDING).orElse(null) : null;
    }
}
