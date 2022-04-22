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

package net.unknown.core.util;

import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.unknown.core.define.DefinedTextColor;

public class NewMessageUtil {
    public static void sendMessage(Player player, Component component) {

    }

    public static void sendMessage(CommandSourceStack source, Component component, boolean broadcastToOps) {
        source.sendSuccess(NewMessageUtil.format(component), broadcastToOps);
    }

    private static Component format(Component component) {
        return new TextComponent("")
                .append(new TextComponent("[").withStyle(ChatFormatting.GRAY))
                .append(new TextComponent("U.N.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(new TextComponent("]").withStyle(ChatFormatting.GRAY))
                .append(new TextComponent(" "))
                .append(component);
    }

    private static net.kyori.adventure.text.Component format(net.kyori.adventure.text.Component component) {
        return net.kyori.adventure.text.Component.empty()
                .append(net.kyori.adventure.text.Component.text("[", DefinedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text("U.N.", net.kyori.adventure.text.format.Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD.as(true))))
                .append(net.kyori.adventure.text.Component.text("]", DefinedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(" "))
                .append(component);
    }
}
