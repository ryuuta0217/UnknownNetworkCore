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

package net.unknown.core.advancements.event;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Asynchronous で呼び出される可能性があります。
 * AdvancementProgress を直接編集しないでください。(#grantProgress #revokeProgress)
 * Criteriaをgrantまたはrevokeする場合は、AdvancementManagerを介して行ってください。
 */
public abstract class CustomAdvancementEvent extends Event {
    private final ServerPlayer player;
    private final Advancement advancement;
    private final AdvancementProgress progress;

    public CustomAdvancementEvent(boolean isAsync, ServerPlayer player, Advancement advancement, AdvancementProgress progress) {
        super(isAsync);
        this.player = player;
        this.advancement = advancement;
        this.progress = progress;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public Player getBukkitPlayer() {
        return this.player.getBukkitEntity();
    }

    public Advancement getAdvancement() {
        return this.advancement;
    }

    public AdvancementProgress getProgress() {
        return this.progress;
    }
}
