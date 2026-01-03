package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.core.enums.Permissions;
import net.unknown.core.feature.HideArmors;
import net.unknown.core.util.NewMessageUtil;

public class HideArmorsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("hidearmors");

        LiteralArgumentBuilder<CommandSourceStack> setNode = Commands.literal("set");
        for (HideArmors.Mode mode : HideArmors.Mode.values()) {
            setNode.then(Commands.literal(mode.name().toLowerCase())
                    .executes(ctx -> setMode(ctx, mode)));
        }

        builder.requires(Permissions.COMMAND_HIDEARMORS::checkAndIsPlayer)
                .executes(HideArmorsCommand::showMode)
                .then(setNode);

        dispatcher.register(builder);
    }

    private static int showMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        HideArmors.Mode currentMode = HideArmors.getInstance().getMode(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), "HideArmors は " + currentMode + " に設定されています");
        return currentMode.ordinal();
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, HideArmors.Mode newMode) throws CommandSyntaxException {
        HideArmors.Mode currentMode = HideArmors.getInstance().getMode(ctx.getSource().getPlayerOrException().getUUID());
        if (currentMode == newMode) {
            NewMessageUtil.sendMessage(ctx.getSource(), "HideArmors は既に " + newMode + " に設定されています");
            return currentMode.ordinal();
        } else {
            HideArmors.getInstance().enable(ctx.getSource().getPlayerOrException().getBukkitEntity(), newMode);
            NewMessageUtil.sendMessage(ctx.getSource(), "HideArmors の設定を " + newMode + " に変更しました");
            return newMode.ordinal();
        }
    }
}
