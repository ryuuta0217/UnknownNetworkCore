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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

public class LivingEntityHandler implements EntityEditorHandler<LivingEntity> {

    @Override
    public List<EntityEditor.Element<LivingEntity>> getElements(LivingEntity entity) {
        List<EntityEditor.Element<LivingEntity>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.BRAIN_CORAL)
                        .displayName(Component.text("AI: " + (targetEntity.hasAI() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.hasAI() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setAI(!targetEntity.hasAI())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.SLIME_BALL)
                        .displayName(Component.text("当たり判定: " + (targetEntity.isCollidable() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isCollidable() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setCollidable(!targetEntity.isCollidable())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GLASS_BOTTLE)
                        .displayName(Component.text("残空気量: " + targetEntity.getRemainingAir(), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -10 | 右クリック: +10", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setRemainingAir(Math.max(0, targetEntity.getRemainingAir() - 10));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setRemainingAir(Math.min(targetEntity.getMaximumAir(), targetEntity.getRemainingAir() + 10));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("空気量を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setRemainingAir(Math.max(0, Math.min(targetEntity.getMaximumAir(), val)));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.POTION)
                        .displayName(Component.text("最大空気量: " + targetEntity.getMaximumAir(), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -10 | 右クリック: +10", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setMaximumAir(Math.max(0, targetEntity.getMaximumAir() - 10));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setMaximumAir(Math.min(1000000, targetEntity.getMaximumAir() + 10));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("最大空気量を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setMaximumAir(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.ARROW)
                        .displayName(Component.text("刺さっている矢の数: " + targetEntity.getArrowsInBody(), DefinedTextColor.GREEN))
                        .lore(Component.text("左クリック: -1 | 右クリック: +1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setArrowsInBody(Math.max(0, targetEntity.getArrowsInBody() - 1));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setArrowsInBody(targetEntity.getArrowsInBody() + 1);
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.text(targetEntity.getArrowsInBody()), Component.text("^^^"), Component.text("刺さっている矢の数を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.getFirst()).content());
                                        targetEntity.setArrowsInBody(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.BREEZE_ROD)
                        .displayName(Component.text("刺さっている鉢の針の数: " + targetEntity.getBeeStingersInBody(), DefinedTextColor.GREEN))
                        .lore(Component.text("左クリック: -1 | 右クリック: +1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setBeeStingersInBody(Math.max(0, targetEntity.getBeeStingersInBody() - 1));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setBeeStingersInBody(targetEntity.getBeeStingersInBody() + 1);
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.text(targetEntity.getBeeStingersInBody()), Component.text("^^^"), Component.text("刺さっている鉢の針の数を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.getFirst()).content());
                                        targetEntity.setBeeStingersInBody(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
