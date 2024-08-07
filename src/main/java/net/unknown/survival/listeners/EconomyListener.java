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

package net.unknown.survival.listeners;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.survival.wrapper.economy.event.PlayerDepositEvent;
import net.unknown.survival.wrapper.economy.event.PlayerWithdrawEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class EconomyListener implements Listener {
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0円.00銭");

    @EventHandler
    public void onWithdraw(PlayerWithdrawEvent event) {
        Player player = event.getPlayer().getPlayer();
        if (player != null) { // Player is online
            Component amount = Component.text(format(event.getAmount()), DefinedTextColor.GREEN);
            Component beforeBalance = Component.text(format(event.getBeforeBalance()), DefinedTextColor.GRAY);
            Component afterBalance = Component.text(format(event.getAfterBalance()), DefinedTextColor.GRAY);
            player.sendActionBar(Component.empty()
                    .append(amount)
                    .appendSpace()
                    .append(Component.text("を支払いました"))
                    .appendSpace()
                    .append(Component.text("(", DefinedTextColor.GRAY)
                            .append(beforeBalance)
                            .append(Component.text(" -> "))
                            .append(afterBalance)
                            .append(Component.text(")", DefinedTextColor.GRAY))));
        }
    }

    @EventHandler
    public void onDeposit(PlayerDepositEvent event) {
        Player player = event.getPlayer().getPlayer();
        if (player != null) { // Player is online
            Component amount = Component.text(format(event.getAmount()), DefinedTextColor.GREEN);
            Component beforeBalance = Component.text(format(event.getBeforeBalance()), DefinedTextColor.GRAY);
            Component afterBalance = Component.text(format(event.getAfterBalance()), DefinedTextColor.GRAY);
            player.sendActionBar(Component.empty()
                    .append(amount)
                    .appendSpace()
                    .append(Component.text("が入金されました"))
                    .appendSpace()
                    .append(Component.text("(", DefinedTextColor.GRAY)
                            .append(beforeBalance)
                            .append(Component.text(" -> "))
                            .append(afterBalance)
                            .append(Component.text(")", DefinedTextColor.GRAY))));
        }
    }

    private static String format(double number) {
        long numberLong = double2long(number);
        long major = numberLong / 100;
        long minor = numberLong % 100;
        String majorStr = NumberFormat.getNumberInstance().format(major);
        String minorStr = minor == 0 ? "0" : (minor < 10 ? new String(new char[]{'0', (char) (minor + '0')}) : ((minor % 10) == 0 ? String.valueOf(minor / 10) : String.valueOf(minor)));

        return majorStr + "円" + " " + minorStr + "銭";
    }

    private static long double2long(double d) {
        return (long) (d * 100);
    }
}
