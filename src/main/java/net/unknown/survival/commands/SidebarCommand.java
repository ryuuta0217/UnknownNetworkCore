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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.BuiltInExceptions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ryuuta0217.util.ComponentCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.feature.sidebar.Sidebar;
import net.unknown.survival.feature.sidebar.SidebarModule;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class SidebarCommand {
    private static final SuggestionProvider<CommandSourceStack> MODULES_SUGGESTION = (ctx, suggestionsBuilder) -> {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
        return SharedSuggestionProvider.suggestResource(
                Sidebar.getRegisteredModules()
                        .stream()
                        .sorted(Comparator.comparingInt((SidebarModule module) -> module.getPriority(player)).reversed())
                        .map(SidebarModule::getIdentifier)
                        .map(CraftNamespacedKey::toMinecraft),
                suggestionsBuilder
        );
    };
    private static final SuggestionProvider<CommandSourceStack> MODULE_OPTIONS_SUGGESTION = (ctx, suggestionsBuilder) -> {
        NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
        SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);

        if (module == null) {
            return suggestionsBuilder.buildFuture();
        }

        return SharedSuggestionProvider.suggest(module.getAvailableOptions(), suggestionsBuilder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("sidebar");

        builder.requires(Permissions.COMMAND_SIDEBAR::checkAndIsPlayer)
                .executes(SidebarCommand::toggle)
                .then(Commands.literal("show")
                        .executes(SidebarCommand::show))
                .then(Commands.literal("hide")
                        .executes(SidebarCommand::hide))
                .then(Commands.literal("toggle")
                        .executes(SidebarCommand::toggle))
                .then(Commands.literal("modules")
                        .executes(SidebarCommand::listModules)
                        .then(Commands.literal("list")
                                .executes(SidebarCommand::listModules))
                        .then(Commands.literal("enable")
                                .then(Commands.argument("module", IdentifierArgument.id())
                                        .suggests(MODULES_SUGGESTION)
                                        .executes(SidebarCommand::enableModule)))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("module", IdentifierArgument.id())
                                        .suggests(MODULES_SUGGESTION)
                                        .executes(SidebarCommand::disableModule)))
                        .then(Commands.literal("modify")
                                .then(Commands.argument("module", IdentifierArgument.id())
                                        .suggests(MODULES_SUGGESTION)
                                        .then(Commands.argument("option", StringArgumentType.string())
                                                .suggests(MODULE_OPTIONS_SUGGESTION)
                                                .then(Commands.literal("get")
                                                        .executes(SidebarCommand::getModuleOption))
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("value", StringArgumentType.string())
                                                                .suggests((ctx, suggestionsBuilder) -> {
                                                                    Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();
                                                                    NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
                                                                    String moduleOption = StringArgumentType.getString(ctx, "option");
                                                                    SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);
                                                                    if (module != null) {
                                                                        String currentOrDefaultValue = module.getOptions(player).getOrDefault(moduleOption, null);
                                                                        if (currentOrDefaultValue != null) {
                                                                            if (currentOrDefaultValue.matches("(true|false)")) {
                                                                                return SharedSuggestionProvider.suggest(new String[]{"true", "false"}, suggestionsBuilder);
                                                                            } else if (currentOrDefaultValue.matches("-?\\d+")) {
                                                                                return SharedSuggestionProvider.suggest(IntStream.range(-100, 100).mapToObj(String::valueOf), suggestionsBuilder);
                                                                            }
                                                                        }
                                                                    }
                                                                    return suggestionsBuilder.buildFuture();
                                                                })
                                                                .executes(SidebarCommand::setModuleOption)))
                                                .then(Commands.literal("reset")
                                                        .executes(SidebarCommand::resetModuleOption))))));

        dispatcher.register(builder);
    }

    public static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        if (Sidebar.getProvider().isSidebarEnabled(player)) {
            return hide(ctx);
        } else {
            return show(ctx);
        }
    }

    public static int show(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        Sidebar.getProvider().setSidebarEnabled(player, true);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("サイドバーを有効にしました", DefinedTextColor.GREEN));
        return 1;
    }

    public static int hide(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        Sidebar.getProvider().setSidebarEnabled(player, false);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("サイドバーを無効にしました", DefinedTextColor.YELLOW));
        return 1;
    }

    private static int listModules(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        Set<SidebarModule> modules = Sidebar.getRegisteredModules();

        Component modulesComponent = modules.stream()
                .sorted(Comparator.comparingInt((SidebarModule module) -> module.getPriority(player)).reversed())
                .map(module -> {
                    boolean enabled = module.isEnabled(player);
                    int priority = module.getPriority(player);
                    return Component.empty().color(enabled ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).decoration(TextDecoration.ITALIC, !enabled)
                            .append(Component.text(priority))
                            .appendSpace()
                            .append(Component.text(module.getIdentifier().toString()))
                            .appendSpace()
                            .append(Component.text(enabled ? "(有効)" : "(無効)", enabled ? DefinedTextColor.GREEN : DefinedTextColor.RED));
                })
                .collect(ComponentCollector.toComponent(Component.newline()))
                .asComponent();

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("===== サイドバーモジュール一覧(" + modules.size() + ") =====")).appendNewline()
                .append(modulesComponent), false);

        return modules.size();
    }

    private static int enableModule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
        SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " は見つかりませんでした"));
            return 2;
        }

        module.setEnabled(player, true);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("サイドバーモジュール " + moduleIdentifier + " を")
                .append(Component.text("有効", DefinedTextColor.GREEN))
                .append(Component.text("にしました"))));
        return 1;
    }

    private static int disableModule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
        SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " は見つかりませんでした"));
            return 2;
        }

        module.setEnabled(player, false);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("サイドバーモジュール " + moduleIdentifier + " を")
                .append(Component.text("無効", DefinedTextColor.RED))
                .append(Component.text("にしました"))));
        return 1;
    }

    private static int getModuleOption(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
        SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " は見つかりませんでした"));
            return 2;
        }

        String option = StringArgumentType.getString(ctx, "option");
        if (!module.getAvailableOptions().contains(option)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " にオプション " + option + " は存在しません"));
            return 3;
        }

        String value = module.getOption(player, option);
        Component valueComponent;
        if (value == null) {
            Map<String, String> currentOptions = module.getOptions(player); // its contained default values as well
            valueComponent = Component.text(currentOptions.getOrDefault(option, null) + " (デフォルト)");
        } else {
            valueComponent = Component.text(value);
        }

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("サイドバーモジュール " + moduleIdentifier + " のオプション " + option + " は "))
                .append(valueComponent)
                .append(Component.text(" に設定されています")));
        return 0;
    }

    private static int setModuleOption(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
        SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " は見つかりませんでした"));
            return 2;
        }

        String option = StringArgumentType.getString(ctx, "option");
        if (!module.getAvailableOptions().contains(option)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " にオプション " + option + " は存在しません"));
            return 3;
        }

        String value = StringArgumentType.getString(ctx, "value");
        try {
            module.setOption(player, option, value);
        } catch (Throwable t) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " のオプション " + option + " に値 " + value + " は無効です").hoverEvent(HoverEvent.showText(Component.text(t.getMessage()))));
            return 4;
        }
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("サイドバーモジュール " + moduleIdentifier + " のオプション " + option + " を "))
                .append(Component.text(value))
                .append(Component.text(" に設定しました")));
        return 0;
    }

    private static int resetModuleOption(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException().getBukkitEntity();

        NamespacedKey moduleIdentifier = CraftNamespacedKey.fromMinecraft(IdentifierArgument.getId(ctx, "module"));
        SidebarModule module = Sidebar.getRegisteredModule(moduleIdentifier);
        if (module == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " は見つかりませんでした"));
            return 2;
        }

        String option = StringArgumentType.getString(ctx, "option");
        if (!module.getAvailableOptions().contains(option)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("サイドバーモジュール " + moduleIdentifier + " にオプション " + option + " は存在しません"));
            return 3;
        }

        module.resetOption(player, option);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("サイドバーモジュール " + moduleIdentifier + " のオプション " + option + " をデフォルト値にリセットしました")));
        return 0;
    }
}
