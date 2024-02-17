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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.EvalManager;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.VoteTicketExchangeItems;
import net.unknown.survival.data.model.vote.*;
import net.unknown.survival.data.model.vote.impl.ContainerExchangeItem;
import net.unknown.survival.data.model.vote.impl.ScriptExchangeItem;
import net.unknown.survival.data.model.vote.impl.SimpleExchangeItem;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.vote.gui.VoteTicketExchangeGui;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class VoteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("vote");
        builder.executes(VoteCommand::showVoteLinks)
                .requires(Permissions.COMMAND_VOTE::check)
                .then(Commands.literal("exchange")
                        .requires(Permissions.COMMAND_VOTE::checkAndIsPlayer)
                        .executes(VoteCommand::openExchangeGui)
                        .then(Commands.literal("add")
                                .requires(Permissions.COMMAND_VOTE_MANAGE::check)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.literal("simple")
                                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> addSimpleExchangeItem(ctx, StringArgumentType.getString(ctx, "id"), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "count"), IntegerArgumentType.getInteger(ctx, "price")))))))
                                        .then(Commands.literal("random")
                                                .then(Commands.argument("display-item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> addSimpleRandomExchangeItem(ctx, StringArgumentType.getString(ctx, "id"), ItemArgument.getItem(ctx, "display-item"), IntegerArgumentType.getInteger(ctx, "count"), IntegerArgumentType.getInteger(ctx, "price")))))))
                                        .then(Commands.literal("container")
                                                .then(Commands.argument("display-item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("container", ItemArgument.item(buildContext))
                                                                .then(Commands.argument("stacks", IntegerArgumentType.integer(1))
                                                                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                                        .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                                                                                .executes(ctx -> addContainerExchangeItem(ctx, StringArgumentType.getString(ctx, "id"), ItemArgument.getItem(ctx, "display-item"), ItemArgument.getItem(ctx, "container"), IntegerArgumentType.getInteger(ctx, "stacks"), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "count"), IntegerArgumentType.getInteger(ctx, "price"))))))))))
                                        .then(Commands.literal("container_selectable")
                                                .then(Commands.argument("display-item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("container", ItemArgument.item(buildContext))
                                                                .then(Commands.argument("stacks", IntegerArgumentType.integer(1))
                                                                        .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                                                                .executes(ctx -> addContainerSelectableExchangeItem(ctx, StringArgumentType.getString(ctx, "id"), ItemArgument.getItem(ctx, "display-item"), ItemArgument.getItem(ctx, "container"), IntegerArgumentType.getInteger(ctx, "stacks"), IntegerArgumentType.getInteger(ctx, "price"))))))))
                                        .then(Commands.literal("script")
                                                .then(Commands.argument("getItem", StringArgumentType.string())
                                                        .then(Commands.argument("getDisplayItem", StringArgumentType.string())
                                                                .then(Commands.argument("getPrice", StringArgumentType.string())
                                                                        .then(Commands.argument("onExchanged", StringArgumentType.string())
                                                                                .executes(ctx -> addScriptExchangeItem(ctx, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "getItem"), StringArgumentType.getString(ctx, "getDisplayItem"), StringArgumentType.getString(ctx, "getPrice"), StringArgumentType.getString(ctx, "onExchanged"))))))))))
                        .then(Commands.literal("modify")
                                .requires(Permissions.COMMAND_VOTE_MANAGE::check)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(VoteTicketExchangeItems.getExchangeItems().keySet(), suggestionsBuilder))
                                        .then(Commands.literal("item")
                                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> modifyExchangeItemItem(ctx, StringArgumentType.getString(ctx, "id"), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "count"))))))
                                        .then(Commands.literal("container")
                                                .then(Commands.argument("container", ItemArgument.item(buildContext))
                                                        .executes(ctx -> modifyExchangeItemContainer(ctx, StringArgumentType.getString(ctx, "id"), ItemArgument.getItem(ctx, "container")))))
                                        .then(Commands.literal("price")
                                                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyExchangeItemPrice(ctx, StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "price")))))
                                        .then(Commands.literal("choices")
                                                .then(Commands.literal("add")
                                                        .then(Commands.argument("choice-id", StringArgumentType.word())
                                                                .then(Commands.argument("choice", ItemArgument.item(buildContext))
                                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                                .executes(ctx -> addExchangeItemChoice(ctx, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "choice-id"), ItemArgument.getItem(ctx, "choice"), IntegerArgumentType.getInteger(ctx, "count")))))))
                                                .then(Commands.literal("remove")
                                                        .then(Commands.argument("choice-id", StringArgumentType.word())
                                                                .suggests((ctx, suggestionsBuilder) -> {
                                                                    String id = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "id", null);
                                                                    if (VoteTicketExchangeItems.has(id)) {
                                                                        ExchangeItem item = VoteTicketExchangeItems.getExchangeItems().get(id);
                                                                        if (item instanceof SelectableItem selectableItem) {
                                                                            selectableItem.getChoices().keySet().forEach(choiceId -> suggestionsBuilder.suggest(choiceId, net.minecraft.network.chat.Component.literal(item.getType().name())));
                                                                        }
                                                                    }
                                                                    return suggestionsBuilder.buildFuture();
                                                                })
                                                                .executes(ctx -> removeExchangeItemChoice(ctx, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "choice-id")))))
                                                .then(Commands.literal("list")
                                                        .executes(ctx -> showExchangeItemChoices(ctx, StringArgumentType.getString(ctx, "id")))))))
                        .then(Commands.literal("remove")
                                .requires(Permissions.COMMAND_VOTE_MANAGE::check)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, suggestionsBuilder) -> {
                                            VoteTicketExchangeItems.getExchangeItems().forEach((id, item) -> {
                                                suggestionsBuilder.suggest(id, net.minecraft.network.chat.Component.literal(item.getType().name()));
                                            });
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .executes(ctx -> removeExchangeItem(ctx, StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("list")
                                .requires(Permissions.COMMAND_VOTE_MANAGE::check)
                                .executes(VoteCommand::showExchangeItems)));

        dispatcher.register(builder);
    }

    private static int showVoteLinks(CommandContext<CommandSourceStack> ctx) {
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票リンク:", DefinedTextColor.AQUA))
                .appendSpace()
                .append(Component.text("JMS", DefinedTextColor.GOLD).clickEvent(ClickEvent.openUrl("https://minecraft.jp/servers/play.mc-unknown.net/vote")))
                .appendSpace()
                .append(Component.text("monocraft", DefinedTextColor.GOLD).clickEvent(ClickEvent.openUrl("https://monocraft.net/servers/hWvNPIBskVkZ743kWt8S/vote"))));
        return 0;
    }

    private static int openExchangeGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        new VoteTicketExchangeGui(null, ctx.getSource().getPlayerOrException().getBukkitEntity()).open();
        return 0;
    }

    private static int addSimpleExchangeItem(CommandContext<CommandSourceStack> ctx, String id, ItemInput item, int count, int price) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は既に使用されています");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容からアイテムのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftStack = item.createItemStack(count, false);
        org.bukkit.inventory.ItemStack bukkitStack = MinecraftAdapter.ItemStack.itemStack(minecraftStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("VoteTicketExchangeItemのインスタンスを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        SimpleExchangeItem exchangeItem = ExchangeItem.ofSimple(bukkitStack, price);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたインスタンス(" + exchangeItem.hashCode() + ")を追加しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        VoteTicketExchangeItems.add(id, exchangeItem);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム"))
                .appendSpace()
                .append(Component.text("[" + id + "]"))
                .appendSpace()
                .append(bukkitStack.displayName().hoverEvent(bukkitStack.asHoverEvent()).appendSpace().append(Component.text("x" + bukkitStack.getAmount())))
                .appendSpace()
                .append(Component.text("(チケット x" + exchangeItem.getPrice(null) + ")"))
                .append(Component.text("を追加しました")));
        return 0;
    }

    private static int addSimpleRandomExchangeItem(CommandContext<CommandSourceStack> ctx, String id, ItemInput displayItem, int count, int price) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は既に使用されています");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容から表示アイテムのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftDisplayStack = displayItem.createItemStack(count, false);
        org.bukkit.inventory.ItemStack bukkitDisplayStack = MinecraftAdapter.ItemStack.itemStack(minecraftDisplayStack);


        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("VoteTicketExchangeItemのインスタンスを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        SimpleExchangeItem exchangeItem = ExchangeItem.ofSimpleRandom(bukkitDisplayStack, Collections.emptyMap(), price);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたインスタンス(" + exchangeItem.hashCode() + ")を追加しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        VoteTicketExchangeItems.add(id, exchangeItem);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム(ランダム)"))
                .appendSpace()
                .append(Component.text("[" + id + "]"))
                .appendSpace()
                .append(bukkitDisplayStack.displayName().hoverEvent(bukkitDisplayStack.asHoverEvent()).appendSpace().append(Component.text("x" + bukkitDisplayStack.getAmount())))
                .appendSpace()
                .append(Component.text("(チケット x" + exchangeItem.getPrice(null) + ")"))
                .append(Component.text("を追加しました")));

        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("注意: /vote exchange modify " + id + " choices <add|remove|list> を使用して、選択肢を追加してください", DefinedTextColor.YELLOW), false);
        return 0;
    }

    private static int addContainerExchangeItem(CommandContext<CommandSourceStack> ctx, String id, ItemInput displayItem, ItemInput container, int stacks, ItemInput item, int count, int price) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は既に使用されています");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容から表示アイテムのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftDisplayItemStack = displayItem.createItemStack(1, false);
        org.bukkit.inventory.ItemStack bukkitDisplayItemStack = MinecraftAdapter.ItemStack.itemStack(minecraftDisplayItemStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容からコンテナのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftContainerStack = container.createItemStack(1, false);
        org.bukkit.inventory.ItemStack bukkitContainerStack = MinecraftAdapter.ItemStack.itemStack(minecraftContainerStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容からアイテムのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftStack = item.createItemStack(count, false);
        org.bukkit.inventory.ItemStack bukkitStack = MinecraftAdapter.ItemStack.itemStack(minecraftStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("VoteTicketExchangeItemのインスタンスを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        SimpleExchangeItem exchangeItem = ExchangeItem.ofContainer(bukkitDisplayItemStack, bukkitContainerStack, bukkitStack, stacks, price);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたインスタンス(" + exchangeItem.hashCode() + ")を追加しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        VoteTicketExchangeItems.add(id, exchangeItem);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム(コンテナ型)"))
                .appendSpace()
                .append(Component.text("[" + id + "]"))
                .appendSpace()
                .append(bukkitContainerStack.displayName().hoverEvent(bukkitContainerStack.asHoverEvent()))
                .appendSpace()
                .append(bukkitStack.displayName().hoverEvent(bukkitStack.asHoverEvent()).appendSpace().append(Component.text("x" + bukkitStack.getAmount())))
                .appendSpace()
                .append(Component.text("(" + stacks + "スタック)"))
                .appendSpace()
                .append(Component.text("(チケット x" + exchangeItem.getPrice(null) + ")"))
                .append(Component.text("を追加しました")));
        return 0;
    }

    private static int addContainerSelectableExchangeItem(CommandContext<CommandSourceStack> ctx, String id, ItemInput displayItem, ItemInput container, int stacks, int price) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は既に使用されています");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容から表示アイテムのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftDisplayItemStack = displayItem.createItemStack(1, false);
        org.bukkit.inventory.ItemStack bukkitDisplayItemStack = MinecraftAdapter.ItemStack.itemStack(minecraftDisplayItemStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力内容からコンテナのItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftContainerStack = container.createItemStack(1, false);
        org.bukkit.inventory.ItemStack bukkitContainerStack = MinecraftAdapter.ItemStack.itemStack(minecraftContainerStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("VoteTicketExchangeItemのインスタンスを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        SimpleExchangeItem exchangeItem = ExchangeItem.ofSelectableContainer(bukkitDisplayItemStack, bukkitContainerStack, Collections.emptyMap(), stacks, price);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたインスタンス(" + exchangeItem.hashCode() + ")を追加しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        VoteTicketExchangeItems.add(id, exchangeItem);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム(選択型コンテナ)"))
                .appendSpace()
                .append(Component.text("[" + id + "]"))
                .appendSpace()
                .append(bukkitContainerStack.displayName().hoverEvent(bukkitContainerStack.asHoverEvent()))
                .appendSpace()
                .append(Component.text("(" + stacks + "スタック)"))
                .appendSpace()
                .append(Component.text("(チケット x" + exchangeItem.getPrice(null) + ")"))
                .append(Component.text("を追加しました")));

        if (bukkitContainerStack.getType() != bukkitDisplayItemStack.getType()) NewMessageUtil.sendMessage(ctx.getSource(), Component.text("注意: コンテナと表示アイテムが異なっています。利用者のUXを考えて、同一のものを利用するべきです。", DefinedTextColor.YELLOW), false);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("注意: /vote exchange modify " + id + " choices <add|remove|list> を使用して、選択肢を追加してください", DefinedTextColor.YELLOW), false);
        return 0;
    }

    private static int addScriptExchangeItem(CommandContext<CommandSourceStack> ctx, String id, String getItem, String getDisplayItem, String getPrice, String onExchanged) {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は既に使用されています");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), MiniMessage.miniMessage().deserialize("<color:gray><italic><color:#CAD69A>getItem</color><color:#FFD00B>(player, choice)</color> をコンパイルしています...</italic></color>"));
        try {
            if (!getItem.contains("return")) throw new RuntimeException("return 文がありません");
            EvalManager.compileFunction("VoteCommand#new", getItem);
        } catch(RuntimeException e) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty()
                    .append(MiniMessage.miniMessage().deserialize("<color:#CAD69A>getItem<color:#FFD00B>(player, choice)</color>"))
                    .appendSpace()
                    .append(Component.text("のスクリプトをコンパイル中にエラーが発生しました: ")
                            .append(Component.text(e.getMessage()))));
            return 2;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), MiniMessage.miniMessage().deserialize("<color:gray><italic><color:#CAD69A>getDisplayItem</color><color:#FFD00B>(player)</color> をコンパイルしています...</italic></color>"));
        try {
            if (!getDisplayItem.contains("return")) throw new RuntimeException("return 文がありません");
            EvalManager.compileFunction("VoteCommand#new", getDisplayItem);
        } catch(RuntimeException e) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty()
                    .append(MiniMessage.miniMessage().deserialize("<color:#CAD69A>getDisplayItem<color:#FFD00B>(player)</color>"))
                    .appendSpace()
                    .append(Component.text("のスクリプトをコンパイル中にエラーが発生しました: ")
                            .append(Component.text(e.getMessage()))));
            return 3;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), MiniMessage.miniMessage().deserialize("<color:gray><italic><color:#CAD69A>getPrice</color><color:#FFD00B>(player)</color> をコンパイルしています...</italic></color>"));
        try {
            if (!getPrice.contains("return")) throw new RuntimeException("return 文がありません");
            EvalManager.compileFunction("VoteCommand#new", getPrice);
        } catch(RuntimeException e) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty()
                    .append(MiniMessage.miniMessage().deserialize("<color:#CAD69A>getPrice<color:#FFD00B>(player)</color>"))
                    .appendSpace()
                    .append(Component.text("のスクリプトをコンパイル中にエラーが発生しました: ")
                            .append(Component.text(e.getMessage()))));
            return 4;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), MiniMessage.miniMessage().deserialize("<color:gray><italic><color:#CAD69A>onExchanged</color><color:#FFD00B>(player, choice)</color> をコンパイルしています...</italic></color>"));
        try {
            EvalManager.compileFunction("VoteCommand#new", onExchanged);
        } catch(RuntimeException e) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.empty()
                    .append(MiniMessage.miniMessage().deserialize("<color:#CAD69A>onExchanged<color:#FFD00B>(player, choice)</color>"))
                    .appendSpace()
                    .append(Component.text("のスクリプトをコンパイル中にエラーが発生しました: ")
                            .append(Component.text(e.getMessage()))));
            return 5;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("VoteTicketExchangeItemのインスタンスを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ScriptExchangeItem exchangeItem = ExchangeItem.ofScript(getItem, getPrice, getDisplayItem, onExchanged);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたインスタンス(" + exchangeItem.hashCode() + ")を追加しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        VoteTicketExchangeItems.add(id, exchangeItem);
        return 0;
    }

    private static int modifyExchangeItemItem(CommandContext<CommandSourceStack> ctx, String id, ItemInput item, int count) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力された内容からItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftItemStack = item.createItemStack(1, false);
        org.bukkit.inventory.ItemStack bukkitItemStack = MinecraftAdapter.ItemStack.itemStack(minecraftItemStack);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたItemStackをVoteTicketExchangeItemに設定しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ExchangeItem exchangeItem = VoteTicketExchangeItems.get(id);
        exchangeItem.setItem(bukkitItemStack);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム " + id + " のアイテムを"))
                .appendSpace()
                .append(bukkitItemStack.displayName().hoverEvent(bukkitItemStack.asHoverEvent()))
                .appendSpace()
                .append(Component.text("に変更しました")));
        return 0;
    }

    private static int modifyExchangeItemContainer(CommandContext<CommandSourceStack> ctx, String id, ItemInput container) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        ExchangeItem exchangeItem = VoteTicketExchangeItems.get(id);

        if (exchangeItem.getType() == ExchangeItemType.SCRIPT) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " はスクリプトによってアイテムが決定されるため、コンテナを設定することはできません");
            return 2;
        }

        if (exchangeItem.getType() != ExchangeItemType.CONTAINER && exchangeItem.getType() != ExchangeItemType.SELECTABLE_CONTAINER) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " はコンテナを設定することができません");
            return 3;
        }

        ContainerExchangeItem containerExchangeItem = ((ContainerExchangeItem) exchangeItem);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力された内容からItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftContainer = container.createItemStack(1, false);
        org.bukkit.inventory.ItemStack bukkitContainer = MinecraftAdapter.ItemStack.itemStack(minecraftContainer);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたItemStackをVoteTicketExchangeItemに設定しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        containerExchangeItem.setContainer(bukkitContainer);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム " + id + " のコンテナを"))
                .appendSpace()
                .append(bukkitContainer.displayName().hoverEvent(bukkitContainer.asHoverEvent()))
                .appendSpace()
                .append(Component.text("に設定しました")));
        return 0;
    }

    private static int modifyExchangeItemPrice(CommandContext<CommandSourceStack> ctx, String id, int price) {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        if (VoteTicketExchangeItems.get(id).getType() == ExchangeItemType.SCRIPT) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "スクリプトによって価格が決定されるアイテムの価格はコマンドから変更できません");
            return 2;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("新しい価格をVoteTicketExchangeItemに設定しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ExchangeItem exchangeItem = VoteTicketExchangeItems.get(id);
        exchangeItem.setPrice(price);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム " + id + " の価格を"))
                .appendSpace()
                .append(Component.text(price))
                .appendSpace()
                .append(Component.text("に設定しました")));
        return 0;
    }

    private static int addExchangeItemChoice(CommandContext<CommandSourceStack> ctx, String id, String choiceId, ItemInput choice, int count) throws CommandSyntaxException {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        ExchangeItem exchangeItem = VoteTicketExchangeItems.get(id);

        if (exchangeItem.getType() != ExchangeItemType.SELECTABLE_CONTAINER && exchangeItem.getType() != ExchangeItemType.SIMPLE_RANDOM) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は選択可能な種類ではないため、選択肢を追加することができません");
            return 2;
        }

        SelectableItem selectableItem = ((SelectableItem) exchangeItem);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("入力された内容からItemStackを作成しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        ItemStack minecraftChoice = choice.createItemStack(count, false);
        org.bukkit.inventory.ItemStack bukkitChoice = MinecraftAdapter.ItemStack.itemStack(minecraftChoice);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("作成されたItemStackをVoteTicketExchangeItemの選択肢に追加しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        selectableItem.addChoice(choiceId, bukkitChoice);

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("投票チケット交換アイテム " + id + " の選択肢を追加しました: [" + choiceId + "]"))
                .appendSpace()
                .append(bukkitChoice.displayName().hoverEvent(bukkitChoice.asHoverEvent())));
        return 0;
    }

    private static int removeExchangeItemChoice(CommandContext<CommandSourceStack> ctx, String id, String choiceId) {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        ExchangeItem exchangeItem = VoteTicketExchangeItems.get(id);

        if (exchangeItem.getType() != ExchangeItemType.SELECTABLE_CONTAINER && exchangeItem.getType() != ExchangeItemType.SIMPLE_RANDOM) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は選択可能な種類ではないため、選択肢を削除することができません");
            return 2;
        }

        SelectableItem selectableItem = ((SelectableItem) exchangeItem);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("選択肢 " + choiceId + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!selectableItem.hasChoice(choiceId)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "選択肢 " + choiceId + " は存在しません");
            return 3;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("選択肢 " + choiceId + " をVoteTicketExchangeItemから削除しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (selectableItem.removeChoice(choiceId)) {
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                    .append(Component.text("投票チケット交換アイテム " + id + " の選択肢 " + choiceId + " を削除しました")));
            return 0;
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "選択肢 " + choiceId + " の削除に失敗しました");
            return 4;
        }
    }

    private static int showExchangeItemChoices(CommandContext<CommandSourceStack> ctx, String id) {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        ExchangeItem exchangeItem = VoteTicketExchangeItems.get(id);

        if (exchangeItem.getType() != ExchangeItemType.SELECTABLE_CONTAINER && exchangeItem.getType() != ExchangeItemType.SIMPLE_RANDOM) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は選択可能な種類ではないため、選択肢を表示することができません");
            return 2;
        }

        SelectableItem selectableItem = ((SelectableItem) exchangeItem);

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("投票チケット交換アイテム " + id + " の選択肢を表示しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));

        Map<String, org.bukkit.inventory.ItemStack> choices = selectableItem.getChoices();

        AtomicReference<Component> message = new AtomicReference<>(Component.empty()
                .append(Component.text("投票チケット交換アイテム " + id + " の選択肢一覧 (" + choices.size() + ")", DefinedTextColor.GREEN)));

        selectableItem.getChoices().forEach((choiceId, choice) -> {
            message.set(message.get().append(Component.newline())
                    .append(Component.text("[" + choiceId + "] ", DefinedTextColor.GOLD))
                    .append(choice.displayName().hoverEvent(choice.asHoverEvent())));
        });

        NewMessageUtil.sendMessage(ctx.getSource(), message.get(), false);
        return 0;
    }

    private static int removeExchangeItem(CommandContext<CommandSourceStack> ctx, String id) {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("ID " + id + " が存在するか調べています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        if (!VoteTicketExchangeItems.has(id)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ID " + id + " は存在しません");
            return 1;
        }

        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("投票チケット交換アイテム " + id + " を削除しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));
        VoteTicketExchangeItems.remove(id);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.text("投票チケット交換アイテム " + id + " を削除しました", DefinedTextColor.GREEN));
        return 0;
    }

    private static int showExchangeItems(CommandContext<CommandSourceStack> ctx) {
        NewMessageUtil.sendVerboseMessage(ctx.getSource(), Component.text("投票チケット交換アイテム一覧を表示しています...", DefinedTextColor.GRAY, TextDecoration.ITALIC));

        Map<String, ExchangeItem> exchangeItems = VoteTicketExchangeItems.getExchangeItems();

        AtomicReference<Component> message = new AtomicReference<>(Component.empty()
                .append(Component.text("投票チケット交換アイテム一覧 (" + exchangeItems.size() + ")", DefinedTextColor.GREEN)));

        exchangeItems.forEach((id, exchangeItem) -> {
            message.set(message.get().append(Component.newline())
                    .append(Component.text("[" + id + "]", DefinedTextColor.GOLD))
                    .appendSpace()
                    .append(Component.text("[" + exchangeItem.getType().name() + "]", DefinedTextColor.GRAY)));
        });

        NewMessageUtil.sendMessage(ctx.getSource(), message.get(), false);
        return 0;
    }
}
