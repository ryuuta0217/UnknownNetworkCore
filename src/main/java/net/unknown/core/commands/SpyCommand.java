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

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.feature.admin.spy.Spy;
import net.unknown.core.feature.admin.spy.SpyModule;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;

import java.util.Set;

public class SpyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("spy");
        builder.requires(Permissions.COMMAND_SPY::checkAndIsPlayer)
                .then(Commands.literal("list")
                        .executes(SpyCommand::showSpyModules))
                .then(Commands.literal("enable")
                        .then(Commands.argument("module", IdentifierArgument.id())
                                .suggests((ctx, suggestionBuilder) -> {
                                    if (ctx.getSource().isPlayer()) {
                                        Player player = ctx.getSource().getPlayer().getBukkitEntity();
                                        return SharedSuggestionProvider.suggestResource(
                                                Spy.getDisabledModules(player),
                                                suggestionBuilder,
                                                module -> CraftNamespacedKey.toMinecraft(module.getIdentifier()),
                                                (module) -> NewMessageUtil.convertAdventure2Minecraft(module.getDisplayName())
                                        );
                                    }

                                    return suggestionBuilder.buildFuture();
                                })
                                .executes(SpyCommand::enableModule)))
                .then(Commands.literal("disable")
                        .then(Commands.argument("module", IdentifierArgument.id())
                                .suggests((ctx, suggestionBuilder) -> {
                                    if (ctx.getSource().isPlayer()) {
                                        Player player = ctx.getSource().getPlayer().getBukkitEntity();
                                        return SharedSuggestionProvider.suggestResource(
                                                Spy.getEnabledModules(player),
                                                suggestionBuilder,
                                                module -> CraftNamespacedKey.toMinecraft(module.getIdentifier()),
                                                (module) -> NewMessageUtil.convertAdventure2Minecraft(module.getDisplayName())
                                        );
                                    }

                                    return suggestionBuilder.buildFuture();
                                })
                                .executes(SpyCommand::disableModule)));
        dispatcher.register(builder);
    }

    private static int showSpyModules(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Set<SpyModule> modules = Spy.getRegisteredModules();
        if (modules.isEmpty()) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "監視モジュールが一つも登録されていません。バグの可能性があります。");
            return -1;
        }

        Set<SpyModule> enabledModules = Spy.getEnabledModules(ctx.getSource().getPlayerOrException().getBukkitEntity());
        Component enabledModulesMessage = Component.empty().append(Component.text("===== 有効な監視モジュール (" + enabledModules.size() + ") =====", DefinedTextColor.AQUA));
        for (SpyModule module : enabledModules) {
            enabledModulesMessage = enabledModulesMessage.appendNewline().append(module.getDisplayName()).appendSpace().append(Component.text("-")).appendSpace().append(Component.text(module.getIdentifier().toString()));
        }
        NewMessageUtil.sendMessage(ctx.getSource(), enabledModulesMessage);

        Set<SpyModule> disabledModules = Spy.getDisabledModules(ctx.getSource().getPlayerOrException().getBukkitEntity());
        Component disabledModulesMessage = Component.empty().append(Component.text("===== 無効な監視モジュール (" + disabledModules.size() + ") =====", DefinedTextColor.RED));
        for (SpyModule module : disabledModules) {
            disabledModulesMessage = disabledModulesMessage.appendNewline().append(module.getDisplayName()).appendSpace().append(Component.text("-")).appendSpace().append(Component.text(module.getIdentifier().toString()));
        }
        NewMessageUtil.sendMessage(ctx.getSource(), disabledModulesMessage);
        return enabledModules.size() + disabledModules.size();
    }

    private static int enableModule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifierRL = IdentifierArgument.getId(ctx, "module");
        NamespacedKey identifier = CraftNamespacedKey.fromMinecraft(identifierRL);
        SpyModule module = Spy.getModule(identifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "指定された監視モジュールは存在しません。");
            return -1;
        }
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        if (Spy.isModuleEnabled(player, module)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty().append(Component.text("監視モジュール ", DefinedTextColor.YELLOW)).append(module.getDisplayName()).appendSpace().append(Component.text("(" + module.getIdentifier().toString() + ")", DefinedTextColor.GRAY)).appendSpace().append(Component.text(" は既に有効になっています", DefinedTextColor.YELLOW)));
            return -2;
        } else {
            Spy.enableModule(player, module);
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("監視モジュール ", DefinedTextColor.GREEN)).append(module.getDisplayName()).appendSpace().append(Component.text("(" + module.getIdentifier().toString() + ")", DefinedTextColor.GRAY)).appendSpace().append(Component.text(" を有効にしました", DefinedTextColor.GREEN)), true);
            return 0;
        }
    }

    private static int disableModule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifierRL = IdentifierArgument.getId(ctx, "module");
        NamespacedKey identifier = CraftNamespacedKey.fromMinecraft(identifierRL);
        SpyModule module = Spy.getModule(identifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "指定された監視モジュールは存在しません。");
            return -1;
        }
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        if (!Spy.isModuleEnabled(player, module)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty().append(Component.text("監視モジュール ", DefinedTextColor.YELLOW)).append(module.getDisplayName()).appendSpace().append(Component.text("(" + module.getIdentifier().toString() + ")", DefinedTextColor.GRAY)).appendSpace().append(Component.text(" は既に無効になっています", DefinedTextColor.YELLOW)));
            return -2;
        } else {
            Spy.disableModule(player, module);
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("監視モジュール ", DefinedTextColor.RED)).append(module.getDisplayName()).appendSpace().append(Component.text("(" + module.getIdentifier().toString() + ")", DefinedTextColor.GRAY)).appendSpace().append(Component.text(" を無効にしました", DefinedTextColor.RED)), true);
            return 0;
        }
    }
}
