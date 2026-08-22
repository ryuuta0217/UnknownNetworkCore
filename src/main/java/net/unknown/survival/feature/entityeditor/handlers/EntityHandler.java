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
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import net.unknown.survival.feature.entityeditor.EntityEditorRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class EntityHandler implements EntityEditorHandler<Entity> {

    public static float loop(float exp, float min, float max) {
        if (exp >= max) return min + (exp - max);
        if (exp <= min) return max + (exp - min);
        return exp;
    }

    @Override
    public List<EntityEditor.Element<Entity>> getElements(Entity entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.SHIELD)
                                .displayName(targetEntity.isInvulnerable() ? Component.text("無敵: 有効", DefinedTextColor.GREEN) : Component.text("無敵: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setInvulnerable(!targetEntity.isInvulnerable())),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.NOTE_BLOCK)
                                .displayName(targetEntity.isSilent() ? Component.text("消音: 有効", DefinedTextColor.GREEN) : Component.text("消音: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setSilent(!targetEntity.isSilent())),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.END_PORTAL_FRAME)
                                .displayName(targetEntity.hasGravity() ? Component.text("重力: 有効", DefinedTextColor.GREEN) : Component.text("重力: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setGravity(!targetEntity.hasGravity())),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.hasNoPhysics() ? Material.STRUCTURE_VOID : Material.SCAFFOLDING)
                                .displayName(targetEntity.hasNoPhysics() ? Component.text("物理演算: 無効", DefinedTextColor.RED) : Component.text("物理演算: 有効", DefinedTextColor.GREEN))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            if (!targetEntity.hasNoPhysics() && targetEntity.hasGravity()) {
                                targetEntity.setGravity(false);
                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.text("自動的に [重力] が無効になりました。[重力] は有効にすることもできますが、NoClipが発生します。", DefinedTextColor.YELLOW));
                            }
                            targetEntity.setNoPhysics(!targetEntity.hasNoPhysics());
                        }
                ),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.isFrozen() ? Material.ICE : Material.GLASS)
                                .displayName(targetEntity.isFrozen() ? Component.text("凍結: 有効 (残り" + targetEntity.getFreezeTicks() + "ﾃｨｯｸ)", DefinedTextColor.GREEN) : Component.text("凍結: 無効", DefinedTextColor.RED))
                                .lore(
                                        Component.text("左クリック: 解除 | 右クリック: 30秒追加", DefinedTextColor.YELLOW),
                                        Component.text("中クリック: 直接入力 (Ticks)", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            if (event.getClick().isLeftClick()) {
                                targetEntity.setFreezeTicks(0);
                            } else if (event.getClick().isRightClick()) {
                                targetEntity.setFreezeTicks(targetEntity.getFreezeTicks() + 600);
                            } else if (event.getClick() == ClickType.MIDDLE) {
                                Player player = (Player) event.getWhoClicked();
                                editor.onceDeferUnregisterOnClose();
                                new SignGui()
                                        .withTarget(player)
                                        .withLines(Component.empty(), Component.text("^^^"), Component.text("凍結Tick数を入力"), Component.empty())
                                        .onComplete(lines -> {
                                            try {
                                                int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                                targetEntity.setFreezeTicks(Math.max(0, val));
                                            } catch (Exception ignored) {
                                            }
                                            editor.open(player);
                                        })
                                        .open();
                            }
                        }
                ),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.isFreezeTickingLocked() ? Material.PACKED_ICE : Material.BARRIER)
                                .displayName(targetEntity.isFreezeTickingLocked() ? Component.text("凍結固定: 有効", DefinedTextColor.GREEN) : Component.text("凍結固定: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.lockFreezeTicks(!targetEntity.isFreezeTickingLocked())
                ),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.getFireTicks() > 0 ? Material.CAMPFIRE : Material.FLINT_AND_STEEL)
                                .displayName(targetEntity.getFireTicks() > 0 ? Component.text("炎上: 有効 (残り" + targetEntity.getFireTicks() + "ﾃｨｯｸ)", DefinedTextColor.GREEN) : Component.text("炎上: 無効", DefinedTextColor.RED))
                                .lore(
                                        Component.text("左クリック: 鎮火 | 右クリック: 10秒追加", DefinedTextColor.YELLOW),
                                        Component.text("中クリック: 直接入力 (Ticks)", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            if (event.getClick().isLeftClick()) {
                                targetEntity.setFireTicks(0);
                            } else if (event.getClick().isRightClick()) {
                                targetEntity.setFireTicks(targetEntity.getFireTicks() + 200);
                            } else if (event.getClick() == ClickType.MIDDLE) {
                                Player player = (Player) event.getWhoClicked();
                                editor.onceDeferUnregisterOnClose();
                                new SignGui()
                                        .withTarget(player)
                                        .withLines(Component.empty(), Component.text("^^^"), Component.text("炎上Tick数を入力"), Component.empty())
                                        .onComplete(lines -> {
                                            try {
                                                int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                                targetEntity.setFireTicks(Math.max(0, val));
                                            } catch (Exception ignored) {
                                            }
                                            editor.open(player);
                                        })
                                        .open();
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.GLASS)
                                .displayName(targetEntity.isInvisible() ? Component.text("透明: 有効", DefinedTextColor.GREEN) : Component.text("透明: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            targetEntity.setInvisible(!targetEntity.isInvisible());
                            if (targetEntity.isInvisible()) {
                                targetEntity.setGlowing(true);
                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.text("透明化を有効にしたため、発光を自動的に有効にしました。", DefinedTextColor.YELLOW));
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.LIGHT)
                                .displayName(targetEntity.isGlowing() ? Component.text("発光: 有効", DefinedTextColor.GREEN) : Component.text("発光: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setGlowing(!targetEntity.isGlowing())),
                EntityEditor.Element.lineBreak(),
                EntityEditor.Element.numberEditor(EntityEditorRegistry.ToolAction.ENTITY_ROTATION_PITCH, Material.COMPASS, 1.0f, 0.1f),
                EntityEditor.Element.numberEditor(EntityEditorRegistry.ToolAction.ENTITY_ROTATION_YAW, Material.RECOVERY_COMPASS, 1.0f, 0.1f),
                EntityEditor.Element.empty(),
                EntityEditor.Element.numberEditor(EntityEditorRegistry.ToolAction.ENTITY_POSITION_X, Material.RED_WOOL, 1.0d, 0.1d),
                EntityEditor.Element.numberEditor(EntityEditorRegistry.ToolAction.ENTITY_POSITION_Y, Material.GREEN_WOOL, 1.0d, 0.1d),
                EntityEditor.Element.numberEditor(EntityEditorRegistry.ToolAction.ENTITY_POSITION_Z, Material.BLUE_WOOL, 1.0d, 0.1d),
                EntityEditor.Element.empty(),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.LAVA_BUCKET)
                                .displayName(Component.text("エンティティを削除", DefinedTextColor.RED, TextDecoration.BOLD))
                                .lore(Component.text("注意: エンティティが**確認なしで**即消去されます", DefinedTextColor.RED, TextDecoration.BOLD), Component.empty(), Component.text("クリックで削除", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            targetEntity.remove();
                            event.getWhoClicked().closeInventory();
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
