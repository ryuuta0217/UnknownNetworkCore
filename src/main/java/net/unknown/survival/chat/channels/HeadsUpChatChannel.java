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

package net.unknown.survival.chat.channels;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.Position;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class HeadsUpChatChannel extends ChatChannel implements Listener {
    private static final HolographicDisplaysAPI holoApi = HolographicDisplaysAPI.get(Bukkit.getPluginManager().getPlugin("HolographicDisplays"));
    //private static final int MAX_LINES = 4;

    private final UUID target;
    private Hologram hologram;
    private BukkitTask hologramUpdater;

    public HeadsUpChatChannel(UUID target) {
        super("頭上", ChannelType.HEADS_UP);
        this.target = target;
        Bukkit.getPluginManager().registerEvents(this, UnknownNetworkCore.getInstance());
    }

    private static double getYPos(double base, int count, double per) {
        return base + (count * per);
    }

    @Override
    public void processChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(this.target)) return;
        event.setCancelled(true);
        String message;
        if (event.getPlayer().hasPermission(Permissions.FEATURE_USE_COLOR_CODE.getPermissionNode())) {
            message = LegacyComponentSerializer.legacyAmpersand().serialize(event.message());
        } else {
            message = PlainTextComponentSerializer.plainText().serialize(event.message());
        }

        List<String> messages = new ArrayList<>();

        if (message.length() > 16) {
            while (message.length() > 16) {
                messages.add(message.substring(0, 15));
                message = message.substring(15);
            }
        } else {
            messages.add(message);
        }

        if (this.hologram == null) {
            try {
                this.hologram = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCore.getInstance(), () -> {
                    return holoApi.createHologram(Position.of(event.getPlayer()).add(0, getYPos(2.5, messages.size(), 0.25), 0));
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                MessageUtil.sendMessage(event.getPlayer(), "An Internal error occurred, your message not delivered to heads-up.");
                event.setCancelled(false);
                return;
            }
            messages.forEach(line -> this.hologram.getLines().appendText(line));

            putUpdater(event);
        } else {
            messages.forEach(m -> {
                this.hologram.getLines().appendText(m);
                setHologramPosition(event.getPlayer());
            });

            putUpdater(event);
        }
    }

    private void putUpdater(AsyncChatEvent event) {
        if (this.hologramUpdater != null && !this.hologramUpdater.isCancelled()) this.hologramUpdater.cancel();
        this.hologramUpdater = RunnableManager.runAsyncRepeating(() -> {
            if (this.hologram.getLines().size() > 0) {
                this.hologram.getLines().remove(0);
                if (this.hologram.getLines().size() > 10) {
                    while (this.hologram.getLines().size() > 10) {
                        this.hologram.getLines().remove(0);
                    }
                }
                setHologramPosition(event.getPlayer());
            } else {
                this.hologram.delete();
                this.hologram = null;
                this.hologramUpdater.cancel();
                this.hologramUpdater = null;
            }
        }, 20 * 3, 20 * 3);
    }

    private void setHologramPosition(Player player) {
        if (this.hologram == null) return;
        this.hologram.setPosition(Position.of(player).add(0, getYPos(2.5, this.hologram.getLines().size(), 0.25), 0));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.getPlayer().getUniqueId().equals(this.target)) return;
        if (this.hologram == null) return;
        setHologramPosition(event.getPlayer());
    }

    @Override
    public void onChannelSwitch(ChatChannel newChannel) {
        super.onChannelSwitch(newChannel);
        HandlerList.unregisterAll(this);
    }
}
