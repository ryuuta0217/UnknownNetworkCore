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

package net.unknown.survival.feature.entityeditor.handlers;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextDisplayHandler implements EntityEditorHandler<TextDisplay> {

    @Override
    public List<EntityEditor.Element<TextDisplay>> getElements(TextDisplay entity) {
        List<EntityEditor.Element<TextDisplay>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.NAME_TAG)
                        .displayName(Component.text("テキスト編集", DefinedTextColor.GREEN))
                        .lore(Component.text("クリックでテキストを入力", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> {
                    Player player = (Player) event.getWhoClicked();
                    editor.onceDeferUnregisterOnClose();

                    Component currentComponent = targetEntity.text();
                    String initialText = currentComponent != null ? MiniMessage.miniMessage().serialize(currentComponent) : "";

                    Dialog dialog = Dialog.create(builder -> builder.empty()
                            .base(DialogBase.builder(Component.text("テキストの編集"))
                                    .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                    .body(Collections.singletonList(DialogBody.plainMessage(Component.text("表示するテキストを入力してください"), 200)))
                                    .inputs(Collections.singletonList(DialogInput.text(
                                            "text",
                                            300,
                                            Component.text("テキスト"),
                                            true,
                                            initialText,
                                            Integer.MAX_VALUE,
                                            TextDialogInput.MultilineOptions.create(null, null)
                                    )))
                                    .canCloseWithEscape(true)
                                    .build())
                            .type(DialogType.confirmation(
                                    ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                        String val = response.getText("text");
                                        if (val != null) {
                                            targetEntity.text(MiniMessage.miniMessage().deserialize(val));
                                        }
                                        editor.open(player);
                                    }, ClickCallback.Options.builder().uses(1).build())),
                                    ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                        editor.open(player);
                                    }, ClickCallback.Options.builder().uses(1).build()))
                            ))
                    );
                    player.showDialog(dialog);
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.STRING)
                        .displayName(Component.text("LineWidth: " + targetEntity.getLineWidth(), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -10 | 右クリック: +10", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setLineWidth(Math.max(0, targetEntity.getLineWidth() - 10));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setLineWidth(Math.min(10000, targetEntity.getLineWidth() + 10));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("行の幅を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setLineWidth(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.LIGHT_GRAY_STAINED_GLASS)
                        .displayName(Component.text("テキストの不透明度: " + (targetEntity.getTextOpacity() & 0xFF), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -10 | 右クリック: +10", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    int current = targetEntity.getTextOpacity() & 0xFF;
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setTextOpacity((byte) Math.max(0, current - 10));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setTextOpacity((byte) Math.min(255, current + 10));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("不透明度を入力 (0-255)"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setTextOpacity((byte) Math.clamp(val, 0, 255));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(targetEntity.isDefaultBackground() ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE)
                        .displayName(targetEntity.isDefaultBackground() ? Component.text("デフォルト背景: 有効", DefinedTextColor.GREEN) : Component.text("デフォルト背景: 無効", DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setDefaultBackground(!targetEntity.isDefaultBackground())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(targetEntity.isSeeThrough() ? Material.GLASS : Material.STONE)
                        .displayName(targetEntity.isSeeThrough() ? Component.text("壁透過 (SeeThrough): 有効", DefinedTextColor.GREEN) : Component.text("壁透過 (SeeThrough): 無効", DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setSeeThrough(!targetEntity.isSeeThrough())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(targetEntity.isShadowed() ? Material.WITHER_SKELETON_SKULL : Material.SKELETON_SKULL)
                        .displayName(targetEntity.isShadowed() ? Component.text("テキストの影: 有効", DefinedTextColor.GREEN) : Component.text("テキストの影: 無効", DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setShadowed(!targetEntity.isShadowed())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.PAPER)
                        .displayName(Component.text("Alignment: " + targetEntity.getAlignment().name(), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("クリックで切り替え", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    TextDisplay.TextAlignment[] alignments = TextDisplay.TextAlignment.values();
                    int nextOrd = (targetEntity.getAlignment().ordinal() + 1) % alignments.length;
                    targetEntity.setAlignment(alignments[nextOrd]);
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.BLACK_DYE)
                        .displayName(Component.text("BackgroundColor: " + (targetEntity.getBackgroundColor() != null ? "ARGB(" + targetEntity.getBackgroundColor().getAlpha() + "," + targetEntity.getBackgroundColor().getRed() + "," + targetEntity.getBackgroundColor().getGreen() + "," + targetEntity.getBackgroundColor().getBlue() + ")" : "デフォルト(未設定)"), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("クリックで設定ダイアログを開く", DefinedTextColor.YELLOW),
                                Component.text("中クリックで設定解除(リセット)", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick() == ClickType.MIDDLE) {
                        targetEntity.setBackgroundColor(null);
                        return;
                    }
                    Player player = (Player) event.getWhoClicked();
                    editor.onceDeferUnregisterOnClose();

                    int initA = targetEntity.getBackgroundColor() != null ? targetEntity.getBackgroundColor().getAlpha() : 64;
                    int initR = targetEntity.getBackgroundColor() != null ? targetEntity.getBackgroundColor().getRed() : 0;
                    int initG = targetEntity.getBackgroundColor() != null ? targetEntity.getBackgroundColor().getGreen() : 0;
                    int initB = targetEntity.getBackgroundColor() != null ? targetEntity.getBackgroundColor().getBlue() : 0;

                    io.papermc.paper.dialog.Dialog dialog = io.papermc.paper.dialog.Dialog.create(builder -> builder.empty()
                            .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(Component.text("背景色の設定"))
                                    .afterAction(io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                    .body(java.util.Collections.singletonList(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(Component.text("ARGB(0~255)を入力してください"), 200)))
                                    .inputs(java.util.List.of(
                                            io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange("a", Component.text("A(不透明)"), 0f, 255f).initial((float) initA).step(1f).build(),
                                            io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange("r", Component.text("R(赤)"), 0f, 255f).initial((float) initR).step(1f).build(),
                                            io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange("g", Component.text("G(緑)"), 0f, 255f).initial((float) initG).step(1f).build(),
                                            io.papermc.paper.registry.data.dialog.input.DialogInput.numberRange("b", Component.text("B(青)"), 0f, 255f).initial((float) initB).step(1f).build()
                                    ))
                                    .canCloseWithEscape(true)
                                    .build())
                            .type(io.papermc.paper.registry.data.dialog.type.DialogType.confirmation(
                                    io.papermc.paper.registry.data.dialog.ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                                        Float af = response.getFloat("a");
                                        Float rf = response.getFloat("r");
                                        Float gf = response.getFloat("g");
                                        Float bf = response.getFloat("b");
                                        if (af != null && rf != null && gf != null && bf != null) {
                                            targetEntity.setBackgroundColor(Color.fromARGB(af.intValue(), rf.intValue(), gf.intValue(), bf.intValue()));
                                        }
                                        editor.open(player);
                                    }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build())),
                                    io.papermc.paper.registry.data.dialog.ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.empty(), 128, io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                                        editor.open(player);
                                    }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                            ))
                    );
                    player.showDialog(dialog);
                }
        ));

        return elements;
    }
}
