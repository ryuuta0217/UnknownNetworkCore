/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.feature.AdminStorage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminStorageCommand {
    private static final Map<UUID, String> DELETE_CONFIRMATION_MAP = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("adminstorage");
        builder.requires(Permissions.COMMAND_ADMINSTORAGE::checkAndIsPlayer);

        builder.executes(ctx -> openAdminStorage(ctx, null))
                .then(Commands.literal("open")
                        .executes(ctx -> openAdminStorage(ctx, null))
                        .then(Commands.argument("identifier", StringArgumentType.word())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(AdminStorage.getInstance().getInventories().keySet().stream().map(AdminStorage.StorageIdentifier::identifier), suggestionsBuilder))
                                .executes(ctx -> openAdminStorage(ctx, StringArgumentType.getString(ctx, "identifier")))))
                .then(Commands.literal("list")
                        .executes(AdminStorageCommand::showAdminStorageList))
                .then(Commands.literal("add")
                        .then(Commands.argument("identifier", StringArgumentType.word())
                                .then(Commands.argument("display_name", ComponentArgument.textComponent(buildContext))
                                        .then(Commands.argument("priority", IntegerArgumentType.integer())
                                                .executes(ctx -> addAdminStorage(ctx, StringArgumentType.getString(ctx, "identifier"), ComponentArgument.getRawComponent(ctx, "display_name"), IntegerArgumentType.getInteger(ctx, "priority"), 54))
                                                .then(Commands.argument("size", IntegerArgumentType.integer(9, 54))
                                                        .executes(ctx -> addAdminStorage(ctx, StringArgumentType.getString(ctx, "identifier"), ComponentArgument.getRawComponent(ctx, "display_name"), IntegerArgumentType.getInteger(ctx, "priority"), IntegerArgumentType.getInteger(ctx, "size"))))))))
                .then(Commands.literal("modify")
                        .then(Commands.argument("identifier", StringArgumentType.word())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(AdminStorage.getInstance().getInventories().keySet().stream().map(AdminStorage.StorageIdentifier::identifier), suggestionsBuilder))
                                .then(Commands.literal("display_name")
                                        .then(Commands.argument("new_display_name", ComponentArgument.textComponent(buildContext))
                                                .executes(ctx -> modifyAdminStorageDisplayName(ctx, StringArgumentType.getString(ctx, "identifier"), ComponentArgument.getRawComponent(ctx, "new_display_name")))))
                                .then(Commands.literal("priority")
                                        .then(Commands.argument("new_priority", IntegerArgumentType.integer())
                                                .executes(ctx -> modifyAdminStoragePriority(ctx, StringArgumentType.getString(ctx, "identifier"), IntegerArgumentType.getInteger(ctx, "new_priority")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("identifier", StringArgumentType.word())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(AdminStorage.getInstance().getInventories().keySet().stream().map(AdminStorage.StorageIdentifier::identifier), suggestionsBuilder))
                                .executes(ctx -> removeAdminStorage(ctx, StringArgumentType.getString(ctx, "identifier")))));

        LiteralCommandNode<CommandSourceStack> commandNode = dispatcher.register(builder);
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("as").requires(Permissions.COMMAND_ADMINSTORAGE::checkAndIsPlayer).executes(ctx -> openAdminStorage(ctx, null)).redirect(commandNode));
    }

    private static int openAdminStorage(CommandContext<CommandSourceStack> ctx, @Nullable String identifierStr) {
        if (identifierStr != null && !AdminStorage.getInstance().hasInventory(identifierStr)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), identifierStr + " という識別子の運営倉庫は存在しません");
            return -2;
        }

        if (AdminStorage.getInstance().getInventories().isEmpty()) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "運営倉庫がひとつも作成されていません");
            return -1;
        }

        Map.Entry<AdminStorage.StorageIdentifier, Inventory> adminStorage = identifierStr != null ? AdminStorage.getInstance().getInventory(identifierStr) : AdminStorage.getInstance().getInventories().firstEntry();

        if (ctx.getSource().getBukkitEntity() instanceof HumanEntity human) {
            human.openInventory(adminStorage.getValue());
            if (identifierStr != null) NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("運営倉庫「")).append(adminStorage.getKey().displayName()).append(Component.text("」を開きました")), true);
            else NewMessageUtil.sendMessage(ctx.getSource(), "運営倉庫を開きました", true);
            return adminStorage.getValue().getSize();
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "このコマンドはプレイヤーのみが実行できるコマンドです");
            return -3;
        }
    }

    private static int showAdminStorageList(CommandContext<CommandSourceStack> ctx) {
        if (AdminStorage.getInstance().getInventories().isEmpty()) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "運営倉庫がひとつも作成されていません");
            return -1;
        }

        List<TextComponent> lines = AdminStorage.getInstance().getInventories()
                .keySet()
                .stream()
                .map(storageIdentifier -> Component.empty()
                        .append(Component.text("[" + storageIdentifier.priority() + "]", DefinedTextColor.AQUA))
                        .appendSpace()
                        .append(storageIdentifier.displayName().hoverEvent(HoverEvent.showText(Component.text(storageIdentifier.identifier()))))
                        .appendSpace()
                        .append(Component.text("[開く]", DefinedTextColor.YELLOW).hoverEvent(HoverEvent.showText(Component.empty().append(Component.text("運営倉庫「")).append(storageIdentifier.displayName()).append(Component.text("」を開きます")))).clickEvent(ClickEvent.runCommand("/adminstorage open " + storageIdentifier.identifier()))))
                .toList();

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("======== 運営倉庫一覧 (" + AdminStorage.getInstance().getInventories().size() + ") ========")).appendNewline().append(Component.join(JoinConfiguration.newlines(), lines)), false);
        return AdminStorage.getInstance().getInventories().size();
    }

    private static int addAdminStorage(CommandContext<CommandSourceStack> ctx, String identifierStr, net.minecraft.network.chat.Component displayName, int priority, int size) {
        if (AdminStorage.getInstance().hasInventory(identifierStr)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), identifierStr + " という識別子の運営倉庫は既に存在します");
            return -2;
        }

        if (size % 9 != 0) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "インベントリのサイズは必ず9の倍数である必要があります(あなたの入力したサイズ: " + size + ")");
            return -4;
        }

        AdminStorage.getInstance().addInventory(identifierStr, NewMessageUtil.convertMinecraft2Adventure(displayName), priority, size);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("運営倉庫「")).append(NewMessageUtil.convertMinecraft2Adventure(displayName)).append(Component.text("」を作成しました")).appendSpace()
                .append(Component.text("[開く]", DefinedTextColor.YELLOW, TextDecoration.UNDERLINED).hoverEvent(HoverEvent.showText(Component.empty().append(Component.text("運営倉庫「")).append(NewMessageUtil.convertMinecraft2Adventure(displayName)).append(Component.text("」を開きます")))).clickEvent(ClickEvent.runCommand("/adminstorage open " + identifierStr))),
                true);
        return AdminStorage.getInstance().getInventories().size();
    }

    private static int modifyAdminStorageDisplayName(CommandContext<CommandSourceStack> ctx, String identifierStr, net.minecraft.network.chat.Component displayName) {
        if (!AdminStorage.getInstance().hasInventory(identifierStr)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), identifierStr + " という識別子の運営倉庫は存在しません");
            return -2;
        }

        Component oldDisplayName = AdminStorage.getInstance().getInventory(identifierStr).getKey().displayName();
        RunnableManager.runAsync(() -> {
            try {
                AdminStorage.getInstance().getInventory(identifierStr).getKey().displayName(NewMessageUtil.convertMinecraft2Adventure(displayName));
                AdminStorage.getInstance().applyInventoryTitles();
                AdminStorage.getInstance().save();
                NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("運営倉庫「")).append(oldDisplayName).append(Component.text("」の表示名を")).appendSpace().append(oldDisplayName).appendSpace().append(Component.text("から")).appendSpace().append(NewMessageUtil.convertMinecraft2Adventure(displayName)).appendSpace().append(Component.text("に変更しました")), true);
            } catch (Exception e) {
                NewMessageUtil.sendErrorMessage(ctx.getSource(), "運営倉庫「" + oldDisplayName + "」の表示名の変更に失敗しました: " + e.getMessage());
                e.printStackTrace();
            }
        });
        return AdminStorage.getInstance().getInventories().size();
    }

    private static int modifyAdminStoragePriority(CommandContext<CommandSourceStack> ctx, String identifierStr, int newPriority) {
        if (!AdminStorage.getInstance().hasInventory(identifierStr)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), identifierStr + " という識別子の運営倉庫は存在しません");
            return -2;
        }

        int oldPriority = AdminStorage.getInstance().getInventory(identifierStr).getKey().priority();
        RunnableManager.runAsync(() -> {
            try {
                Map.Entry<AdminStorage.StorageIdentifier, Inventory> adminStorage = AdminStorage.getInstance().getInventory(identifierStr);
                AdminStorage.getInstance().getInventories().remove(adminStorage.getKey());

                adminStorage.getKey().priority(newPriority);
                AdminStorage.getInstance().getInventories().put(adminStorage.getKey(), adminStorage.getValue());

                AdminStorage.getInstance().applyInventoryTitles();
                AdminStorage.getInstance().save();
                NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("運営倉庫「")).append(AdminStorage.getInstance().getInventory(identifierStr).getKey().displayName()).append(Component.text("」の優先度を")).appendSpace().append(Component.text(oldPriority)).appendSpace().append(Component.text("から")).appendSpace().append(Component.text(newPriority)).appendSpace().append(Component.text("に変更しました")), true);
            } catch(Exception e) {
                NewMessageUtil.sendErrorMessage(ctx.getSource(), "運営倉庫「" + AdminStorage.getInstance().getInventory(identifierStr).getKey().displayName() + "」の優先度の変更に失敗しました: " + e.getMessage());
                e.printStackTrace();
            }
        });
        return AdminStorage.getInstance().getInventories().size();
    }

    private static int removeAdminStorage(CommandContext<CommandSourceStack> ctx, String identifierStr) {
        if (!AdminStorage.getInstance().hasInventory(identifierStr)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), identifierStr + " という識別子の運営倉庫は存在しません");
            return -2;
        }

        if (AdminStorage.getInstance().getInventories().isEmpty()) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "運営倉庫がひとつも作成されていません");
            return -1;
        }

        if (DELETE_CONFIRMATION_MAP.containsKey(ctx.getSource().getEntity().getUUID()) && DELETE_CONFIRMATION_MAP.get(ctx.getSource().getEntity().getUUID()).equalsIgnoreCase(identifierStr)) {
            Map.Entry<AdminStorage.StorageIdentifier, Inventory> adminStorage = AdminStorage.getInstance().getInventory(identifierStr);
            AdminStorage.getInstance().removeInventory(identifierStr);
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("運営倉庫「")).append(adminStorage.getKey().displayName()).append(Component.text("」を削除しました")), true);
            return AdminStorage.getInstance().getInventories().size();
        } else {
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(Component.text("運営倉庫「")).append(AdminStorage.getInstance().getInventory(identifierStr).getKey().displayName()).append(Component.text("」を削除しようとしています。本当に削除する場合は、もう一度同じコマンドを実行してください。")), true);
            DELETE_CONFIRMATION_MAP.put(ctx.getSource().getEntity().getUUID(), identifierStr);
            RunnableManager.runAsyncDelayed(() -> DELETE_CONFIRMATION_MAP.remove(ctx.getSource().getEntity().getUUID()), 20 * 10); // 10秒後に削除確認をリセット
            return -5;
        }
    }
}
