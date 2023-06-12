package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.PositionImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class SwapLocationCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("swaplocation");
        builder.requires(source -> source.hasPermission(2));
        builder.then(Commands.argument("target", EntityArgument.entity())
                        .executes(ctx -> execute(ctx.getSource(), ctx.getSource().getEntityOrException(), EntityArgument.getEntity(ctx, "target")))
                        .then(Commands.argument("destination", EntityArgument.entity())
                                .executes(ctx -> execute(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), EntityArgument.getEntity(ctx, "destination")))));
        dispatcher.register(builder);
    }

    private static int execute(CommandSourceStack source, Entity a, Entity b) {
        a.teleportTo((ServerLevel) b.level(), new PositionImpl(b.position().x(), b.position().y(), b.position().z()));
        if (a instanceof ServerPlayer player) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty().append(b.getName()).append(" と場所が入れ替わりました")));
        b.teleportTo((ServerLevel) a.level(), new PositionImpl(a.position().x(), a.position().y(), a.position().z()));
        if (b instanceof ServerPlayer player) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty().append(a.getName()).append(" と場所が入れ替わりました")));
        if (a.equals(source.getEntity()) || b.equals(source.getEntity())) {
            source.sendSuccess(() -> Component.empty().append(a.equals(source.getEntity()) ? b.getName() : a.getName()).append(" と場所を入れ替えました"), true);
        } else {
            source.sendSuccess(() -> Component.empty().append(a.getName()).append(" と ").append(b.getName()).append(" の場所を交換しました"), true);
        }
        return 0;
    }
}
