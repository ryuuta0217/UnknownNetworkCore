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
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import net.unknown.survival.feature.entityeditor.EntityEditorRegistry;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisplayHandler implements EntityEditorHandler<Display> {



    @Override
    public List<EntityEditor.Element<Display>> getElements(Display entity) {
        List<EntityEditor.Element<Display>> elements = new ArrayList<>();

        Display.Billboard[] billboards = Display.Billboard.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    Display.Billboard current = targetEntity.getBillboard();
                    List<Component> lore = new ArrayList<>();
                    for (Display.Billboard b : billboards) {
                        lore.add(Component.text((b == current ? "▶ " : "■ ") + b.name(), b == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.PAINTING)
                            .displayName(Component.text("Billboard: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    Display.Billboard current = targetEntity.getBillboard();
                    int idx = -1;
                    for (int i = 0; i < billboards.length; i++) {
                        if (billboards[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setBillboard(billboards[(idx + 1) % billboards.length]);
                        else if (event.isLeftClick())
                            targetEntity.setBillboard(billboards[(idx - 1 + billboards.length) % billboards.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.ENDER_EYE)
                        .displayName(Component.text("ViewRange: " + String.format("%.2f", targetEntity.getViewRange()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -0.1 | 右クリック: +0.1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setViewRange((float) Math.max(0.0, targetEntity.getViewRange() - 0.1f));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setViewRange((float) Math.min(100.0, targetEntity.getViewRange() + 0.1f));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("描画距離を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        float val = Float.parseFloat(((TextComponent) lines.get(0)).content());
                                        targetEntity.setViewRange(Math.max(0.0f, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.BLACK_CONCRETE)
                        .displayName(Component.text("ShadowRadius: " + String.format("%.2f", targetEntity.getShadowRadius()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -0.1 | 右クリック: +0.1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setShadowRadius((float) Math.max(0.0, targetEntity.getShadowRadius() - 0.1f));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setShadowRadius((float) Math.min(100.0, targetEntity.getShadowRadius() + 0.1f));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("影の半径を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        float val = Float.parseFloat(((TextComponent) lines.get(0)).content());
                                        targetEntity.setShadowRadius(Math.max(0.0f, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));
        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.IRON_TRAPDOOR)
                        .displayName(Component.text("DisplayWidth: " + String.format("%.2f", targetEntity.getDisplayWidth()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -0.1 | 右クリック: +0.1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setDisplayWidth((float) Math.max(0.0, targetEntity.getDisplayWidth() - 0.1f));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setDisplayWidth((float) Math.min(100.0, targetEntity.getDisplayWidth() + 0.1f));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("幅(Width)を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        float val = Float.parseFloat(((TextComponent) lines.get(0)).content());
                                        targetEntity.setDisplayWidth(Math.max(0.0f, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.OAK_TRAPDOOR)
                        .displayName(Component.text("DisplayHeight: " + String.format("%.2f", targetEntity.getDisplayHeight()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -0.1 | 右クリック: +0.1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setDisplayHeight((float) Math.max(0.0, targetEntity.getDisplayHeight() - 0.1f));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setDisplayHeight((float) Math.min(100.0, targetEntity.getDisplayHeight() + 0.1f));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("高さ(Height)を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        float val = Float.parseFloat(((TextComponent) lines.get(0)).content());
                                        targetEntity.setDisplayHeight(Math.max(0.0f, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.OBSIDIAN)
                        .displayName(Component.text("ShadowStrength: " + String.format("%.2f", targetEntity.getShadowStrength()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -0.1 | 右クリック: +0.1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setShadowStrength((float) Math.max(0.0, targetEntity.getShadowStrength() - 0.1f));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setShadowStrength((float) Math.min(100.0, targetEntity.getShadowStrength() + 0.1f));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("影の濃さを入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        float val = Float.parseFloat(((TextComponent) lines.get(0)).content());
                                        targetEntity.setShadowStrength(Math.max(0.0f, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.CLOCK)
                        .displayName(Component.text("補間遅延: " + targetEntity.getInterpolationDelay() + "ﾃｨｯｸ", DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -1 | 右クリック: +1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setInterpolationDelay(Math.max(0, targetEntity.getInterpolationDelay() - 1));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setInterpolationDelay(Math.min(100000, targetEntity.getInterpolationDelay() + 1));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("補間遅延を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setInterpolationDelay(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.REPEATER)
                        .displayName(Component.text("補間時間: " + targetEntity.getInterpolationDuration() + "ﾃｨｯｸ", DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -1 | 右クリック: +1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setInterpolationDuration(Math.max(0, targetEntity.getInterpolationDuration() - 1));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setInterpolationDuration(Math.min(100000, targetEntity.getInterpolationDuration() + 1));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("補間時間を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setInterpolationDuration(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.ENDER_PEARL)
                        .displayName(Component.text("テレポート補間時間: " + targetEntity.getTeleportDuration() + "ﾃｨｯｸ", DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -1 | 右クリック: +1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setTeleportDuration(Math.max(0, targetEntity.getTeleportDuration() - 1));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setTeleportDuration(Math.min(100000, targetEntity.getTeleportDuration() + 1));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("テレポート時間を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setTeleportDuration(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                }).open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_TRANSLATION_X, Material.RED_DYE, 0.1f, 0.01f));
        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_TRANSLATION_Y, Material.GREEN_DYE, 0.1f, 0.01f));
        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_TRANSLATION_Z, Material.BLUE_DYE, 0.1f, 0.01f));
        elements.add(EntityEditor.Element.empty());

        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_SCALE_X, Material.RED_TERRACOTTA, 0.1f, 0.01f));
        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_SCALE_Y, Material.GREEN_TERRACOTTA, 0.1f, 0.01f));
        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_SCALE_Z, Material.BLUE_TERRACOTTA, 0.1f, 0.01f));
        elements.add(EntityEditor.Element.lineBreak());

        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_LEFT_ROTATION_X, Material.RED_GLAZED_TERRACOTTA, 1.0f, 0.1f));
        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_LEFT_ROTATION_Y, Material.GREEN_GLAZED_TERRACOTTA, 1.0f, 0.1f));
        elements.add(EntityEditor.Element.numberEditor(this, EntityEditorRegistry.ToolAction.DISPLAY_LEFT_ROTATION_Z, Material.BLUE_GLAZED_TERRACOTTA, 1.0f, 0.1f));
        elements.add(EntityEditor.Element.empty());

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GLOWSTONE)
                        .displayName(Component.text("Brightness: Block=" + (targetEntity.getBrightness() != null ? targetEntity.getBrightness().getBlockLight() : "未設定") + ", Sky=" + (targetEntity.getBrightness() != null ? targetEntity.getBrightness().getSkyLight() : "未設定"), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("クリックで設定ダイアログを開く", DefinedTextColor.YELLOW),
                                Component.text("中クリックで設定解除(リセット)", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick() == ClickType.MIDDLE) {
                        targetEntity.setBrightness(null);
                        return;
                    }
                    Player player = (Player) event.getWhoClicked();
                    editor.onceDeferUnregisterOnClose();

                    int initBlock = targetEntity.getBrightness() != null ? targetEntity.getBrightness().getBlockLight() : 0;
                    int initSky = targetEntity.getBrightness() != null ? targetEntity.getBrightness().getSkyLight() : 0;

                    Dialog dialog = Dialog.create(builder -> builder.empty()
                            .base(DialogBase.builder(Component.text("明るさの設定"))
                                    .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                    .body(Collections.singletonList(DialogBody.plainMessage(Component.text("ブロック光と空の光を入力 (0~15)"), 200)))
                                    .inputs(List.of(
                                            DialogInput.numberRange("block", Component.text("ブロック光"), 0f, 15f).initial((float) initBlock).step(1f).build(),
                                            DialogInput.numberRange("sky", Component.text("空の光"), 0f, 15f).initial((float) initSky).step(1f).build()
                                    ))
                                    .canCloseWithEscape(true)
                                    .build())
                            .type(DialogType.confirmation(
                                    ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                                        Float bf = response.getFloat("block");
                                        Float sf = response.getFloat("sky");
                                        if (bf != null && sf != null) {
                                            targetEntity.setBrightness(new Display.Brightness(bf.intValue(), sf.intValue()));
                                        }
                                        editor.open(player);
                                    }, ClickCallback.Options.builder().uses(1).build())),
                                    ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.empty(), 128, io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                                        editor.open(player);
                                    }, ClickCallback.Options.builder().uses(1).build()))
                            ))
                    );
                    player.showDialog(dialog);
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GLOW_INK_SAC)
                        .displayName(Component.text("GlowColorOverride: " + (targetEntity.getGlowColorOverride() != null ? "RGB(" + targetEntity.getGlowColorOverride().getRed() + "," + targetEntity.getGlowColorOverride().getGreen() + "," + targetEntity.getGlowColorOverride().getBlue() + ")" : "未設定"), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("クリックで設定ダイアログを開く", DefinedTextColor.YELLOW),
                                Component.text("中クリックで設定解除(リセット)", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick() == ClickType.MIDDLE) {
                        targetEntity.setGlowColorOverride(null);
                        return;
                    }
                    Player player = (Player) event.getWhoClicked();
                    editor.onceDeferUnregisterOnClose();

                    int initR = targetEntity.getGlowColorOverride() != null ? targetEntity.getGlowColorOverride().getRed() : 255;
                    int initG = targetEntity.getGlowColorOverride() != null ? targetEntity.getGlowColorOverride().getGreen() : 255;
                    int initB = targetEntity.getGlowColorOverride() != null ? targetEntity.getGlowColorOverride().getBlue() : 255;

                    Dialog dialog = Dialog.create(builder -> builder.empty()
                            .base(DialogBase.builder(Component.text("発光色の設定"))
                                    .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                    .body(Collections.singletonList(DialogBody.plainMessage(Component.text("RGB(0~255)を入力してください"), 200)))
                                    .inputs(List.of(
                                            DialogInput.numberRange("r", Component.text("R(赤)"), 0f, 255f).initial((float) initR).step(1f).build(),
                                            DialogInput.numberRange("g", Component.text("G(緑)"), 0f, 255f).initial((float) initG).step(1f).build(),
                                            DialogInput.numberRange("b", Component.text("B(青)"), 0f, 255f).initial((float) initB).step(1f).build()
                                    ))
                                    .canCloseWithEscape(true)
                                    .build())
                            .type(DialogType.confirmation(
                                    ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                                        Float rf = response.getFloat("r");
                                        Float gf = response.getFloat("g");
                                        Float bf = response.getFloat("b");
                                        if (rf != null && gf != null && bf != null) {
                                            targetEntity.setGlowColorOverride(Color.fromRGB(rf.intValue(), gf.intValue(), bf.intValue()));
                                        }
                                        editor.open(player);
                                    }, ClickCallback.Options.builder().uses(1).build())),
                                    ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.empty(), 128, io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                                        editor.open(player);
                                    }, ClickCallback.Options.builder().uses(1).build()))
                            ))
                    );
                    player.showDialog(dialog);
                }
        ));

        return elements;
    }
}
