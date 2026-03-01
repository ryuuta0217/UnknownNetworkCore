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

package net.unknown.survival.bossbar;

import net.minecraft.resources.Identifier;
import net.unknown.core.managers.BossBarManager;
import net.unknown.survival.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public class SurvivalVisibilityHandler implements BossBarManager.VisibilityHandler {
    private static final NamespacedKey REGISTRY_KEY = new NamespacedKey("survival", "bossbar_visibility");

    @Override
    public boolean isVisible(Player player, Identifier identifier) {
        return PlayerData.of(player).getRegistries().getOrDefault(REGISTRY_KEY, identifier.toString(), "true").equalsIgnoreCase("true");
    }

    @Override
    public void setVisible(Player player, Identifier identifier, boolean visible) {
        PlayerData.of(player).getRegistries().put(REGISTRY_KEY, identifier.toString(), Boolean.toString(visible));

        BossBarManager.getInstance().updateBossBarVisibility(player, identifier);
    }
}
