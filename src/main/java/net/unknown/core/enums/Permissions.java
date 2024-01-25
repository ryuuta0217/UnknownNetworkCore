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

package net.unknown.core.enums;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.unknown.core.commands.*;
import net.unknown.core.commands.vanilla.GamemodeCommand;
import net.unknown.core.commands.vanilla.MsgCommand;
import net.unknown.core.commands.vanilla.TimeCommand;
import net.unknown.core.fireworks.ProgrammedFireworksCommand;

public enum Permissions {
    COMMAND_CRASH("unknown.core.command.crash", "minecraft.command.crash", 4, CrashCommand.class),
    COMMAND_EVAL("unknown.core.command.eval", "minecraft.command.eval", 4, EvalCommand.class),
    COMMAND_PACKET("unknown.core.command.packet", "minecraft.command.packet", 2, PacketCommand.class),
    COMMAND_SKIN("unknown.core.command.skin", 2, SkinCommand.class),
    COMMAND_NICK("unknown.core.command.nick", 2, NickCommand.class),
    COMMAND_SKULL("unknown.core.command.skull", "minecraft.command.skull", 0, SkullCommand.class),

    COMMAND_MSG("minecraft.command.msg", 0, MsgCommand.class),
    COMMAND_GAMEMODE("minecraft.command.gamemode", 2, GamemodeCommand.class),
    COMMAND_TIME("minecraft.command.time", 2, TimeCommand.class),
    COMMAND_REPLY("unknown.core.command.reply", 0, MsgCommand.class),
    COMMAND_TELEPORTWORLD("unknown.core.command.teleportworld", 2, TeleportWorldCommand.class),
    COMMAND_TRASH("unknown.core.command.trash", "minecraft.command.trash", 0, TrashCommand.class),
    COMMAND_PROGRAMMEDFIREWORKS("unknown.core.command.programmedfireworks", "minecraft.command.programmedfireworks", 2, ProgrammedFireworksCommand.class),
    COMMAND_VANISH("unknown.core.command.vanish", "minecraft.command.vanish", 2, VanishCommand.class),

    FEATURE_USE_COLOR_CODE("unknown.feature.use_color_code", 2, null),
    FEATURE_SEE_VANISHED_PLAYERS("unknown.feature.see_vanished_players", 2, null);

    private final int opLevel;
    private final String[] permissionNodes;
    private final Class<?> commandClass;

    Permissions(String permissionNode, int opLevel, Class<?> commandClass) {
        this.opLevel = opLevel;
        this.permissionNodes = new String[]{ permissionNode };
        this.commandClass = commandClass;
    }

    Permissions(String permissionNode, String commandPermissionNode, int opLevel, Class<?> commandClass) {
        this.opLevel = opLevel;
        this.permissionNodes = new String[]{ permissionNode, commandPermissionNode };
        this.commandClass = commandClass;
    }

    public String getPermissionNode() {
        return this.permissionNodes[0];
    }

    public String[] getPermissionNodes() {
        return this.permissionNodes;
    }

    public boolean check(CommandContext<CommandSourceStack> context) {
        return check(context.getSource());
    }

    public boolean check(CommandSourceStack commandSourceStack) {
        return (testPermissionNode(commandSourceStack) && testCommandPermissionNode(commandSourceStack)) && testOpLevel(commandSourceStack);
    }

    private boolean testOpLevel(CommandSourceStack commandSourceStack) {
        return (this.opLevel <= 4 && this.opLevel >= 0 ? commandSourceStack.hasPermission(this.opLevel) : true);
    }

    private boolean testPermissionNode(CommandSourceStack commandSourceStack) {
        return commandSourceStack.getBukkitSender().hasPermission(this.permissionNodes[0]);
    }

    private boolean testCommandPermissionNode(CommandSourceStack commandSourceStack) {
        if (this.commandClass == null || this.permissionNodes.length == 1) return true; // When not command permission, always return true.
        return commandSourceStack.getBukkitSender().hasPermission(this.permissionNodes[1]);
    }

    public boolean checkAndIsPlayer(CommandSourceStack clw) {
        if (!(clw.source instanceof Player)) return false;
        return check(clw);
    }
}
