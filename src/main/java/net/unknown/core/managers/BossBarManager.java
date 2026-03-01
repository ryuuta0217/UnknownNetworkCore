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

package net.unknown.core.managers;

import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BossBarManager implements Listener {
    private static final BossBarManager INSTANCE = new BossBarManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(BossBarManager.class);

    private final Set<CustomBossEvent> registeredBossBars = new HashSet<>();
    private VisibilityHandler visibilityHandler = new DefaultVisibilityHandler();

    private BossBarManager() {
        ListenerManager.registerListener(this);
    }

    public static BossBarManager getInstance() {
        return INSTANCE;
    }

    public Set<CustomBossEvent> getRegisteredBossBars() {
        return Collections.unmodifiableSet(this.registeredBossBars);
    }

    public void register(CustomBossEvent bossBar) {
        this.registeredBossBars.stream().filter(existingBossBar -> existingBossBar.getTextId().equals(bossBar.getTextId())).findAny().ifPresent(existingBossBar -> {
            throw new IllegalArgumentException("Identifier duplication detected for identifier " + bossBar.getTextId() + ". Cannot register same identifier boss bars. Existing boss bar: " + existingBossBar);
        });

        this.registeredBossBars.add(bossBar);
    }

    public boolean unregister(CustomBossEvent bossBar) {
        return this.registeredBossBars.remove(bossBar);
    }

    public void setVisibilityHandler(VisibilityHandler visibilityHandler) {
        LOGGER.info("Visibility handler set to {} from {}.", visibilityHandler.getClass().getName(), this.visibilityHandler.getClass().getName());
        this.visibilityHandler = visibilityHandler;
    }

    public VisibilityHandler getVisibilityHandler() {
        return this.visibilityHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.registeredBossBars.forEach(bossBar -> {
            if (this.visibilityHandler.isVisible(event.getPlayer(), bossBar.getTextId())) {
                bossBar.addPlayer(MinecraftAdapter.player(event.getPlayer()));
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.registeredBossBars.forEach(bossBar -> bossBar.removePlayer(MinecraftAdapter.player(event.getPlayer())));
    }

    public interface VisibilityHandler {
        boolean isVisible(Player player, Identifier identifier);
        void setVisible(Player player, Identifier identifier, boolean visible);
        default void toggleVisibility(Player player, Identifier identifier) {
            boolean currentVisibility = this.isVisible(player, identifier);
            this.setVisible(player, identifier, !currentVisibility);
        }
    }

    private static class DefaultVisibilityHandler implements VisibilityHandler {
        private static final NamespacedKey PERSISTENT_CONTAINER_KEY = new NamespacedKey("unknown-network", "bossbar_visibility");

        @Override // default is true, can see if not set or set to true, can't see if set to false
        public boolean isVisible(Player player, Identifier identifier) {
            return !player.getPersistentDataContainer().has(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER) // プレイヤーがコンテナの初期化を完了していない場合は true を返して可視とする
                    || !Boolean.FALSE.equals(player.getPersistentDataContainer().get(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER).get(CraftNamespacedKey.fromMinecraft(identifier), PersistentDataType.BOOLEAN)); // プレイヤーがコンテナの初期化を完了している場合にここに到達する。コンテナ内の Identifier の値が false に設定されているかどうかを確認し、設定されていないか true に設定されている場合は可視とする
        }

        @Override
        public void setVisible(Player player, Identifier identifier, boolean visible) {
            PersistentDataContainer rootContainer = player.getPersistentDataContainer();

            if (!rootContainer.has(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER)) {
                rootContainer.set(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER, rootContainer.getAdapterContext().newPersistentDataContainer());
            }

            PersistentDataContainer dataContainer = rootContainer.get(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER);
            if (dataContainer != null) {
                dataContainer.set(CraftNamespacedKey.fromMinecraft(identifier), PersistentDataType.BOOLEAN, visible);
            }
        }
    }
}
