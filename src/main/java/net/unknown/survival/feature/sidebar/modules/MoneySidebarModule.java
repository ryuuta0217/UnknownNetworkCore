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
import net.minecraft.network.chat.numbers.FixedFormat;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.economy.VaultEconomy;
import net.unknown.survival.feature.sidebar.InternalSidebarLine;
import net.unknown.survival.feature.sidebar.Sidebar;
import net.unknown.survival.feature.sidebar.SidebarModule;
import net.unknown.survival.observers.PerformanceObserver;
import net.unknown.survival.wrapper.economy.WrappedEconomy;
import net.unknown.survival.wrapper.economy.event.PlayerBalanceChangedEvent;
import net.unknown.survival.wrapper.economy.event.PlayerDepositEvent;
import net.unknown.survival.wrapper.economy.event.PlayerWithdrawEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MoneySidebarModule implements SidebarModule, Listener {
    public static final NamespacedKey IDENTIFIER = new NamespacedKey("unknown-network", "money");

    private final int priority = 0;
    private final boolean useNumberFormat = false;
    private final boolean useTwoLines = true;

    @Override
    public NamespacedKey getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public List<InternalSidebarLine> getLines(Player player) {
        List<InternalSidebarLine> lines = new ArrayList<>();

        if (this.isUseTwoLines(player)) {
            lines.add(InternalSidebarLine.of("un_money_label", Component.text("所持金:")));

            if (this.isUseNumberFormat(player)) {
                lines.add(InternalSidebarLine.of("un_money_nf", Component.empty(), new FixedFormat(NewMessageUtil.convertAdventure2Minecraft(Component.text(getBalanceFormatted(player))))));
            } else {
                lines.add(InternalSidebarLine.of("un_money_value", Component.text("  " + getBalanceFormatted(player))));
            }
        } else {
            if (this.isUseNumberFormat(player)) {
                lines.add(InternalSidebarLine.of("un_money_nf", Component.text("所持金:"), new FixedFormat(NewMessageUtil.convertAdventure2Minecraft(Component.text(getBalanceFormatted(player))))));
            } else {
                lines.add(InternalSidebarLine.of("un_money", Component.text("所持金: " + getBalanceFormatted(player))));
            }
        }

        return lines;
    }

    public String getBalanceFormatted(Player player) {
        double balance = WrappedEconomy.INSTANCE.getBalance(player);
        long numberLong = (long) (balance * 100);
        long major = numberLong / 100;
        long minor = numberLong % 100;
        String majorStr = NumberFormat.getNumberInstance().format(major);
        String minorStr = minor == 0 ? "0" : (minor < 10 ? new String(new char[]{'0', (char) (minor + '0')}) : ((minor % 10) == 0 ? String.valueOf(minor / 10) : String.valueOf(minor)));

        return majorStr + "円" + " " + minorStr + "銭";
    }

    @Override
    public int getPriority(Player player) {
        return Integer.parseInt(getOptionOrDefault(player, "priority", String.valueOf(this.priority)));
    }

    @Override
    public void tick(Player player) {

    }

    public boolean isUseNumberFormat(Player player) {
        return Boolean.parseBoolean(getOptionOrDefault(player, "use_number_format", String.valueOf(this.useNumberFormat)));
    }

    public boolean isUseTwoLines(Player player) {
        return Boolean.parseBoolean(getOptionOrDefault(player, "use_two_lines", String.valueOf(this.useTwoLines)));
    }

    @Override
    public Set<String> getAvailableOptions() {
        return Set.of("priority", "use_number_format", "use_two_lines");
    }

    @Override
    public Map<String, String> getOptions(Player player) {
        Map<String, String> options = PlayerData.of(player).getRegistries().getRegistry(this.getPlayerDataRegistryKey());
        return Map.of(
                "priority", options.getOrDefault("priority", String.valueOf(this.priority)),
                "use_number_format", options.getOrDefault("use_number_format", String.valueOf(this.useNumberFormat)),
                "use_two_lines", options.getOrDefault("use_two_lines", String.valueOf(this.useTwoLines))
        );
    }

    @Override
    public boolean isValidOptionValue(Player player, String option, String value) {
        switch (option) {
            case "priority" -> {
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            case "use_number_format", "use_two_lines" -> {
                return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerWithdraw(PlayerWithdrawEvent event) {
        this.onBalanceChanged(event);
    }

    @EventHandler
    public void onPlayerDeposit(PlayerDepositEvent event) {
        this.onBalanceChanged(event);
    }

    private void onBalanceChanged(PlayerBalanceChangedEvent event) {
        if (event.getPlayer() instanceof Player player) {
            InternalSidebarLine line;

            if (this.isUseTwoLines(player)) {
                if (this.isUseNumberFormat(player)) {
                    line = InternalSidebarLine.of("un_money_nf", Component.empty(), new FixedFormat(NewMessageUtil.convertAdventure2Minecraft(Component.text(getBalanceFormatted(player)))));
                } else {
                    line = InternalSidebarLine.of("un_money_value", Component.text("  " + getBalanceFormatted(player)));
                }
            } else {
                if (this.isUseNumberFormat(player)) {
                    line = InternalSidebarLine.of("un_money_nf", Component.text("所持金:"), new FixedFormat(NewMessageUtil.convertAdventure2Minecraft(Component.text(getBalanceFormatted(player)))));
                } else {
                    line = InternalSidebarLine.of("un_money", Component.text("所持金: " + getBalanceFormatted(player)));
                }
            }

            Sidebar.getProvider().deltaUpdate(player, this, line.identifier(), line);
        }
    }
}
