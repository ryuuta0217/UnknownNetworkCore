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

package net.unknown.survival.feature.sidebar;

import net.unknown.survival.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SidebarModule {
    NamespacedKey getIdentifier();

    default boolean isEnabled(Player player) {
        return PlayerData.of(player).getRegistries().getRegistry(this.getPlayerDataRegistryKey()).getOrDefault("enabled", String.valueOf(this.isDefaultEnabled())).equals("true");
    }

    default void setEnabled(Player player, boolean enabled) {
        PlayerData.of(player).getRegistries().put(this.getPlayerDataRegistryKey(), "enabled", String.valueOf(enabled));
    }

    default boolean isDefaultEnabled() {
        return true;
    }

    List<InternalSidebarLine> getLines(Player player);

    int getPriority(Player player); // Higher to top

    void tick(Player player);

    default NamespacedKey getPlayerDataRegistryKey() {
        return new NamespacedKey(getIdentifier().getNamespace(), "sidebar/" + getIdentifier().getKey());
    }

    Set<String> getAvailableOptions();

    Map<String, String> getOptions(Player player);

    default @Nullable String getOption(Player player, String option) {
        return getOptions(player).getOrDefault(option, null);
    }

    @Contract("_,_,!null -> !null") default String getOptionOrDefault(Player player, String option, String fallback) {
        String value = getOption(player, option);
        if (value == null) return fallback;
        return value;
    }

    default void setOption(Player player, String option, String value) {
        if (isValidOptionValue(player, option, value)) {
            PlayerData.of(player).getRegistries().put(this.getPlayerDataRegistryKey(), option, value);
        } else {
            throw new IllegalArgumentException("Invalid option value provided for option: " + option + " with value " + value);
        }
    }

    default void resetOption(Player player, String option) {
        PlayerData.of(player).getRegistries().getRegistry(this.getPlayerDataRegistryKey()).remove(option);
    }

    boolean isValidOptionValue(Player player, String option, String value);
}
