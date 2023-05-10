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

package net.unknown.survival.enums;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.unknown.survival.commands.FlyCommand;
import net.unknown.survival.commands.SpawnCommand;
import net.unknown.survival.commands.TeleportPetCommand;
import net.unknown.survival.commands.admin.LastTpCommand;
import net.unknown.survival.commands.home.DelHomeCommand;
import net.unknown.survival.commands.home.HomeCommand;
import net.unknown.survival.commands.home.HomesCommand;
import net.unknown.survival.commands.home.SetHomeCommand;
import net.unknown.survival.commands.home.admin.*;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;

public enum Permissions {
    /* HOME */
    COMMAND_HOME("unknown.survival.command.home", 0, HomeCommand.class),
    COMMAND_HOMES("unknown.survival.command.homes", 0, HomesCommand.class),
    COMMAND_SETHOME("unknown.survival.command.sethome", 0, SetHomeCommand.class),
    COMMAND_DELHOME("unknown.survival.command.delhome", 0, DelHomeCommand.class),
    COMMAND_AHOME("unknown.survival.command.ahome", 2, AHomeCommand.class),
    COMMAND_AHOMES("unknown.survival.command.ahomes", 2, AHomesCommand.class),
    COMMAND_ADDHOME("unknown.survival.command.addhome", 4, AddHomeCommand.class),
    COMMAND_ADELHOME("unknown.survival.command.adelhome", 4, ADelHomeCommand.class),
    COMMAND_SETHOME_COUNT("unknown.survival.command.sethomecount", 4, SetHomeCountCommand.class),
    COMMAND_FINDHOME("unknown.survival.command.findhome", 4, FindHomeCommand.class),
    /* HOME */

    COMMAND_TELEPORT_PET("unknown.survival.command.tppet", 0, TeleportPetCommand.class),
    COMMAND_LASTTP("unknown.survival.command.lasttp", 2, LastTpCommand.class),
    COMMAND_SPAWN("unknown.survival.command.spawn", 0, SpawnCommand.class),
    COMMAND_FLY("unknown.survival.command.fly", 0, FlyCommand.class),

    NOTIFY_MODDED_PLAYER("unknown.survival.notify.mod", 2, null),
    ENTITY_EDITOR("unknown.survival.entity_editor", 2, null),
    OPEN_GUI("unknown.survival.open_gui", 0, null);

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

    public boolean checkAndIsLivingEntity(CommandSourceStack source) {
        if (!(source.getBukkitEntity() instanceof LivingEntity)) return false;
        return check(source);
    }
}