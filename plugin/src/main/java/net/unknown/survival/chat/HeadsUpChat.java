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

package net.unknown.survival.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.filoghost.holographicdisplays.api.beta.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.beta.Position;
import me.filoghost.holographicdisplays.api.beta.hologram.Hologram;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class HeadsUpChat implements Listener {
    private static final HolographicDisplaysAPI holoApi = HolographicDisplaysAPI.get(Bukkit.getPluginManager().getPlugin("HolographicDisplays"));
    private static final Map<UUID, Hologram> CHAT_HOLOGRAMS = new HashMap<>();
    private static final Map<UUID, BukkitTask> CHAT_HOLOGRAM_UPDATER = new HashMap<>();
    private static final int MAX_LINES = 4;

    public static void processChat(AsyncChatEvent event) {
        event.setCancelled(true);
        String message;
        if(event.getPlayer().hasPermission(Permissions.FEATURE_USE_COLOR_CODE.getPermissionNode())) {
            message = LegacyComponentSerializer.legacyAmpersand().serialize(event.message());
        } else {
            message = PlainTextComponentSerializer.plainText().serialize(event.message());
        }

        List<String> messages = new ArrayList<>();

        if(message.length() > 16) {
            while(message.length() > 16) {
                messages.add(message.substring(0, 15));
                message = message.substring(15);
            }
        } else {
            messages.add(message);
        }

        if(!CHAT_HOLOGRAMS.containsKey(event.getPlayer().getUniqueId())) {
            Hologram chatHolo;
            try {
                chatHolo = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCore.getInstance(), () -> {
                    return holoApi.createHologram(Position.of(event.getPlayer()).add(0, getYPos(2.5, messages.size(), 0.25), 0));
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                MessageUtil.sendMessage(event.getPlayer(), "An Internal error occured, your message not deliveryed to heads-up.");
                event.setCancelled(false);
                return;
            }
            messages.forEach(line -> chatHolo.getLines().appendText(line));
            CHAT_HOLOGRAMS.put(event.getPlayer().getUniqueId(), chatHolo);

            putUpdater(event);
        } else {
            Hologram holo = CHAT_HOLOGRAMS.get(event.getPlayer().getUniqueId());
            messages.forEach(m -> {
                holo.getLines().appendText(m);
                setHologramPosition(event.getPlayer());
            });

            CHAT_HOLOGRAM_UPDATER.get(event.getPlayer().getUniqueId()).cancel();
            putUpdater(event);
        }
    }

    private static void putUpdater(AsyncChatEvent event) {
        CHAT_HOLOGRAM_UPDATER.put(event.getPlayer().getUniqueId(), RunnableManager.runAsyncRepeating(() -> {
            Hologram holo = CHAT_HOLOGRAMS.get(event.getPlayer().getUniqueId());
            if (holo.getLines().size() > 0) {
                holo.getLines().remove(0);
                if(holo.getLines().size() > 10) {
                    while(holo.getLines().size() > 10) {
                        holo.getLines().remove(0);
                    }
                }
                setHologramPosition(event.getPlayer());
            } else {
                holo.delete();
                CHAT_HOLOGRAMS.remove(event.getPlayer().getUniqueId());
                BukkitTask task = CHAT_HOLOGRAM_UPDATER.get(event.getPlayer().getUniqueId());
                CHAT_HOLOGRAM_UPDATER.remove(event.getPlayer().getUniqueId());
                task.cancel();
            }
        }, 20 * 3, 20 * 3));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!CHAT_HOLOGRAMS.containsKey(event.getPlayer().getUniqueId())) return;
        setHologramPosition(event.getPlayer());
    }

    private static void setHologramPosition(Player player) {
        if(!CHAT_HOLOGRAMS.containsKey(player.getUniqueId())) return;
        Hologram holo = CHAT_HOLOGRAMS.get(player.getUniqueId());
        holo.setPosition(Position.of(player).add(0, getYPos(2.5, holo.getLines().size(), 0.25), 0));
    }

    private static double getYPos(double base, int count, double per) {
        return base + (count * per);
    }
}
