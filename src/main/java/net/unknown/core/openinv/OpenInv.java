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

package net.unknown.core.openinv;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class OpenInv {
    public static Player load(OfflinePlayer offline) { // TODO Move method to Utility class, Util.getOfflinePlayer(OfflinePlayer player) -> Player
        if (!offline.hasPlayedBefore()) return null; // 以前にプレイしたことないプレイヤーの場合はnullを返す, 読み込めるデータがないので
        if (offline.isOnline()) return Bukkit.getPlayer(offline.getUniqueId());

        GameProfile dummyProfile = new GameProfile(offline.getUniqueId(), (offline.getName() != null ? offline.getName() : offline.getUniqueId().toString()));
        ServerPlayer dummyPlayer = new ServerPlayer(MinecraftServer.getServer(), MinecraftServer.getServer().getLevel(Level.OVERWORLD), dummyProfile, ClientInformation.createDefault());
        dummyPlayer.getAdvancements().stopListening(); // 進捗のトリガーリスナーを停止する, メモリリークを防ぐ

        CraftPlayer dummyBukkitEntity = new CraftPlayer(dummyPlayer.server.server, dummyPlayer) {
            @Override
            public void loadData() {
                System.out.println("loadData - " + this.getUniqueId());
                CompoundTag loadedData = this.server.getHandle().playerIo.load(this.getHandle());

                if (loadedData != null) {
                    this.getHandle().readAdditionalSaveData(loadedData);
                }
            }

            @Override
            public void saveData() {
                System.out.println("saveData - " + this.getUniqueId());
                ServerPlayer handle = this.getHandle();

                try {
                    PlayerDataStorage playerIo = handle.server.getPlayerList().playerIo;

                    CompoundTag playerData = handle.saveWithoutId(new CompoundTag());
                    this.setExtraData(playerData);

                    File tempFile = File.createTempFile(handle.getStringUUID() + "-", ".dat", playerIo.getPlayerDir());
                    NbtIo.writeCompressed(playerData, tempFile);

                    File newFile = new File(playerIo.getPlayerDir(), handle.getStringUUID() + ".dat");
                    File oldFile = new File(playerIo.getPlayerDir(), handle.getStringUUID() + ".dat_old");
                    Util.safeReplaceFile(newFile, tempFile, oldFile);
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            Field bukkitEntityField = Entity.class.getDeclaredField("bukkitEntity");
            if (bukkitEntityField.trySetAccessible()) {
                bukkitEntityField.set(dummyPlayer, dummyBukkitEntity);

                CompoundTag playerData = MinecraftServer.getServer().getPlayerList().playerIo.load(dummyPlayer);
                if (playerData != null) {
                    dummyPlayer.readAdditionalSaveData(playerData);
                    return dummyPlayer.getBukkitEntity();
                }
            }
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
