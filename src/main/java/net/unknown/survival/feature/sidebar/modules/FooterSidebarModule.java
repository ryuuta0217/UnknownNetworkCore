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
import net.unknown.core.define.DefinedTextColor;
import net.unknown.survival.feature.sidebar.InternalSidebarLine;
import net.unknown.survival.feature.sidebar.SidebarModule;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FooterSidebarModule implements SidebarModule {
    public static final NamespacedKey IDENTIFIER = new NamespacedKey("unknown-network", "footer");

    private final int priority = 20;
    private final boolean useNumberFormat = true;
    private final boolean useTwoLines = false;

    @Override
    public boolean isEnabled(Player player) {
        return true;
    }

    @Override
    public void setEnabled(Player player, boolean enabled) {
        // disabled
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    public NamespacedKey getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public List<InternalSidebarLine> getLines(Player player) {
        return List.of(
                InternalSidebarLine.of("un_footer", Component.text("   play.mc-unknown.net  ", DefinedTextColor.GOLD))
        );
    }

    @Override
    public int getPriority(Player player) {
        return Integer.MIN_VALUE; // Always at the bottom
    }

    @Override
    public void tick(Player player) {

    }

    @Override
    public Set<String> getAvailableOptions() {
        return Set.of();
    }

    @Override
    public Map<String, String> getOptions(Player player) {
        return Map.of();
    }

    @Override
    public boolean isValidOptionValue(Player player, String option, String value) {
        return false;
    }
}
