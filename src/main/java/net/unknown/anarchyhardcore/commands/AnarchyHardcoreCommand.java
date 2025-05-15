package net.unknown.anarchyhardcore.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.unknown.anarchyhardcore.UnknownNetworkAnarchyHardcore;
import net.unknown.anarchyhardcore.ban.BanData;

import java.util.Collections;
import java.util.Optional;

// /<anarchyhardcore|ahc> <unban> <player|ip>
public class AnarchyHardcoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("anarchyhardcore");
        builder.then(Commands.literal("unban")
                .then(Commands.argument("target", StringArgumentType.greedyString())
                        .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(UnknownNetworkAnarchyHardcore.getBanData().stream().map(BanData::target).map(Object::toString), sb))
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "target")))));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(builder);
        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("ahc");
        aliasBuilder.redirect(node);
        dispatcher.register(aliasBuilder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String target) {
        Optional<BanData<?>> banData = UnknownNetworkAnarchyHardcore.getBanData().stream()
                .filter(data -> data.target().toString().equals(target))
                .findFirst();

        if (banData.isPresent()) {
            UnknownNetworkAnarchyHardcore.getBanData().remove(banData.get());
            ctx.getSource().sendSuccess(() -> Component.literal("Unbanned " + target), true);
            return 0;
        } else {
            ctx.getSource().sendFailure(Component.literal("No ban data found for " + target));
            return -1;
        }
    }
}
