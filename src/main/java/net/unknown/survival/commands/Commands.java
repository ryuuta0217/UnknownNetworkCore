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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.survival.commands.admin.LastTpCommand;
import net.unknown.survival.commands.home.DelHomeCommand;
import net.unknown.survival.commands.home.HomeCommand;
import net.unknown.survival.commands.home.HomesCommand;
import net.unknown.survival.commands.home.SetHomeCommand;
import net.unknown.survival.commands.home.admin.*;
import net.unknown.survival.commands.village.VillageCommand;
import net.unknown.survival.commands.village.VillagesCommand;
import net.unknown.survival.commands.warp.WarpCommand;

public class Commands {
    public static void init(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        /* HOMES */
        DelHomeCommand.register(dispatcher);
        HomeCommand.register(dispatcher);
        HomesCommand.register(dispatcher);
        SetHomeCommand.register(dispatcher);
        /* HOMES end*/

        /* HOMES / for op */
        AddHomeCommand.register(dispatcher);
        ADelHomeCommand.register(dispatcher);
        AHomeCommand.register(dispatcher);
        AHomesCommand.register(dispatcher);
        FindHomeCommand.register(dispatcher);
        SetHomeCountCommand.register(dispatcher);
        /* HOMES / for op end */

        TeleportPetCommand.register(dispatcher);

        ChannelCommand.register(dispatcher, buildContext);

        SpawnCommand.register(dispatcher, buildContext);
        LastTpCommand.register(dispatcher);
        FlyCommand.register(dispatcher);

        MenuCommand.register(dispatcher);

        SuppressRaidCommand.register(dispatcher);

        VoteCommand.register(dispatcher, buildContext);

        ShulkerBoxCommand.register(dispatcher, buildContext);

        AutomatedRegenWorldCommand.register(dispatcher);

        VillageCommand.register(dispatcher);
        VillagesCommand.register(dispatcher, buildContext);

        WarpCommand.register(dispatcher);
        PrefixCommand.register(dispatcher, buildContext);

        AFKCommand.register(dispatcher);
        SidebarCommand.register(dispatcher);

        AdminStorageCommand.register(dispatcher, buildContext);
    }
}
