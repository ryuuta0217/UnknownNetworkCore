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

package net.unknown.core.events;

import net.unknown.core.prefix.Prefix;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PrefixChangedEvent extends Event {
    private final HandlerList handlers = new HandlerList();
    private final UUID player;
    private final @Nullable Prefix oldPrefix;
    private final Prefix newPrefix;

    public PrefixChangedEvent(UUID player, @Nullable Prefix oldPrefix, @Nullable Prefix newPrefix) {
        this.player = player;
        this.oldPrefix = oldPrefix;
        this.newPrefix = newPrefix;
    }

    @Nullable
    public Player getPlayer() {
        if (Bukkit.getOfflinePlayer(this.player).isOnline()) {
            return Bukkit.getPlayer(this.player);
        }

        return null;
    }

    @Nonnull
    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(this.player);
    }

    @Nullable
    public Prefix getOldPrefix() {
        return this.oldPrefix;
    }

    @Nullable
    public Prefix getNewPrefix() {
        return this.newPrefix;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return new HandlerList();
    }

    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return handlers;
    }
}
