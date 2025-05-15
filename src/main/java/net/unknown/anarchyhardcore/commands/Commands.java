package net.unknown.anarchyhardcore.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public class Commands {
    public static void init(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        AnarchyHardcoreCommand.register(dispatcher);
    }
}
