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

package net.unknown.survival.commands.village;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.Villages;
import net.unknown.survival.data.model.Village;
import net.unknown.survival.enums.Permissions;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;

public class VillagesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("villages");

        builder.requires(Permissions.COMMAND_VILLAGES::check)
                .executes(VillagesCommand::execList)
                .then(Commands.literal("list")
                        .executes(VillagesCommand::execList))
                .then(Commands.literal("add")
                        .requires(Permissions.COMMAND_VILLAGES_MANAGE::check)
                        .then(Commands.argument("識別子", IdentifierArgument.id())
                                .then(Commands.argument("名前", StringArgumentType.string())
                                        .then(Commands.argument("表示名", ComponentArgument.textComponent(buildContext))
                                                .then(Commands.argument("説明", ComponentArgument.textComponent(buildContext))
                                                        .then(Commands.argument("座標", Vec3Argument.vec3(true))
                                                                .executes(VillagesCommand::execAdd)
                                                                .then(Commands.argument("向き", RotationArgument.rotation())
                                                                        .executes(VillagesCommand::execAdd))))))))
                .then(Commands.literal("remove")
                        .requires(Permissions.COMMAND_VILLAGES_MANAGE::check)
                        .then(Commands.argument("識別子", IdentifierArgument.id())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(Villages.getVillages().keySet(), suggestionsBuilder))
                                .executes(VillagesCommand::execRemove)));

        dispatcher.register(builder);
    }

    private static int execList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<Village> villages = Villages.getVillages().values();
        if (villages.isEmpty()) {
            NewMessageUtil.sendMessage(ctx.getSource(), "公認村は登録されていません");
            return 0;
        }

        Component header = Component.text("=== 公認村一覧 (" + villages.size() + ") ===", DefinedTextColor.GREEN);
        List<TextComponent> villageLines = villages.stream().map(village -> {
            return Component.empty().appendNewline()
                    .append(Component.text(village.getIdentifier().toString()))
                    .append(Component.text(" - "))
                    .append(Component.text(village.getName()).hoverEvent(HoverEvent.showText(village.getDescription())))
                    .append(Component.text(" - "))
                    .append(Component.text(String.format("%s, %.2f, %.2f, %.2f", MessageUtil.getWorldName(village.getLocation().worldName()),
                            village.getLocation().x(),
                            village.getLocation().y(),
                            village.getLocation().z()
                    )));
        }).toList();

        Component message = Component.empty().append(header).append(villageLines);
        NewMessageUtil.sendMessage(ctx.getSource(), message, false);
        return -1;
    }

    private static int execAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "識別子");
        Village existVillage = Villages.getVillage(identifier);
        if (existVillage != null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty().append(Component.text("識別子").appendSpace().append(Component.text(identifier.toString()))).appendSpace().append(Component.text("は既に、村")).appendSpace().append(existVillage.getDisplayName()).appendSpace().append(Component.text("で使用されています")));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "名前");
        Component displayName = NewMessageUtil.convertMinecraft2Adventure(ComponentArgument.getRawComponent(ctx, "表示名"));
        Component description = NewMessageUtil.convertMinecraft2Adventure(ComponentArgument.getRawComponent(ctx, "説明"));
        Vec3 coordinates = Vec3Argument.getVec3(ctx, "座標");
        Vec2 rotation = BrigadierUtil.isArgumentKeyExists(ctx, "向き") ? RotationArgument.getRotation(ctx, "向き").getRotation(ctx.getSource()) : ctx.getSource().getRotation();
        Location location = MinecraftAdapter.location(ctx.getSource().getLevel(), coordinates, rotation);
        Villages.addVillage(identifier, name, displayName, description, location);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("公認村")).appendSpace().append(displayName).appendSpace().append(Component.text("を作成しました")));
        return 1;
    }

    private static int execRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "識別子");
        Village village = Villages.getVillage(identifier);
        if (village == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty().append(Component.text("識別子").appendSpace().append(Component.text(identifier.toString()))).appendSpace().append(Component.text("の村は見つかりませんでした")));
            return 0;
        }

        Villages.removeVillage(identifier);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("公認村")).appendSpace().append(village.getDisplayName()).appendSpace().append(Component.text("を削除しました")));
        return 1;
    }
}
