/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.enums;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.core.commands.CrashCommand;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;

public enum Permissions {
    COMMAND_CRASH("unknown.command.crash", 4, CrashCommand.class);

    private final int opLevel;
    private final String permissionNode;

    Permissions(String permissionNode, int opLevel, Class<?> commandClass) {
        this.opLevel = opLevel;
        this.permissionNode = permissionNode;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    public boolean check(CommandContext<CommandSourceStack> context) {
        return check(context.getSource());
    }

    public boolean check(CommandSourceStack commandSourceStack) {
        if (opLevel <= 4 && opLevel >= 0) {
            return commandSourceStack.hasPermission(opLevel) || commandSourceStack.getBukkitSender().hasPermission(permissionNode);
        } else {
            return commandSourceStack.getBukkitSender().hasPermission(permissionNode);
        }
    }

    public boolean checkAndIsPlayer(CommandSourceStack clw) {
        if (!(clw.getBukkitEntity() instanceof CraftPlayer)) return false;
        return check(clw);
    }
}
