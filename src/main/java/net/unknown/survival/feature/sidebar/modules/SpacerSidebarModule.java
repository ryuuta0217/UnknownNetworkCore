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

package net.unknown.survival.feature.sidebar.modules;

import net.kyori.adventure.text.Component;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.feature.sidebar.InternalSidebarLine;
import net.unknown.survival.feature.sidebar.SidebarModule;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpacerSidebarModule implements SidebarModule {
    private final int index;
    private final int priority;
    private final boolean defaultEnabled;

    public SpacerSidebarModule(int index, int priority, boolean defaultEnabled) {
        this.index = index;
        this.priority = priority;
        this.defaultEnabled = defaultEnabled;
    }

    @Override
    public NamespacedKey getIdentifier() {
        return new NamespacedKey("unknown-network", "spacer_" + index);
    }

    @Override
    public boolean isDefaultEnabled() {
        return this.defaultEnabled;
    }

    @Override
    public List<InternalSidebarLine> getLines(Player player) {
        return List.of(
                InternalSidebarLine.of("un_spacer_" + index, Component.empty())
        );
    }

    @Override
    public int getPriority(Player player) {
        return Integer.parseInt(getOptionOrDefault(player, "priority", String.valueOf(this.priority)));
    }

    @Override
    public void tick(Player player) {

    }

    @Override
    public Set<String> getAvailableOptions() {
        return Set.of("priority");
    }

    @Override
    public Map<String, String> getOptions(Player player) {
        Map<String, String> options = PlayerData.of(player).getRegistries().getRegistry(this.getPlayerDataRegistryKey());
        return Map.of(
                "priority", options.getOrDefault("priority", String.valueOf(this.priority))
        );
    }

    @Override
    public boolean isValidOptionValue(Player player, String option, String value) {
        if (option.equals("priority")) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
