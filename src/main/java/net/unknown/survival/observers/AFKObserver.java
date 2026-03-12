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

package net.unknown.survival.observers;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Util;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.packet.event.PacketReceivedEvent;
import net.unknown.core.packet.listener.IncomingPacketListener;
import net.unknown.core.packet.PacketManager;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class AFKObserver {
    private AFKObserver() {}

    public static void initialize() {
        PacketManager.getInstance().registerIncomingC2SListener(ServerboundPlayerInputPacket.class, new IncomingPacketListener<>() {
            @Override
            public void onPacketReceived(PacketReceivedEvent<ServerboundPlayerInputPacket> event) {
                onAction(event.getPlayer(), Util.getMillis());
            }
        });

        PacketManager.getInstance().registerIncomingC2SListener(ServerboundPlayerActionPacket.class, new IncomingPacketListener<>() {
            @Override
            public void onPacketReceived(PacketReceivedEvent<ServerboundPlayerActionPacket> event) {
                onAction(event.getPlayer(), Util.getMillis());
            }
        });

        RunnableManager.runAsyncRepeating(() -> { // AFK state checker, run every seconds
            Bukkit.getOnlinePlayers().parallelStream().forEach(player -> {
                PlayerData data = PlayerData.of(player);
                long lastActionTime = data.getSessionData().getLastActionTime();
                if (lastActionTime == 0L) return; // No action recorded, skip

                boolean isAfk = data.getSessionData().isAfk();
                boolean shouldBeAfk = Util.getMillis() - lastActionTime >= TimeUnit.MINUTES.toMillis(5); // 5 minutes

                if (!isAfk && shouldBeAfk) {
                    data.getSessionData().setAfk(true, null, true);
                }
            });
        }, 0L, 20L);
    }

    private static void onAction(Player player, long lastActionTime) {
        PlayerData.of(player).getSessionData().setLastActionTime(lastActionTime);
        RunnableManager.runAsync(() -> {
            PlayerData data = PlayerData.of(player);
            if (data.getSessionData().isAfk()) {
                data.getSessionData().setAfk(false, null, true);
            }
        });
    }
}
