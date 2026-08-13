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
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
                        (targetEntity, event) -> targetEntity.setInvulnerable(!targetEntity.isInvulnerable())),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.NOTE_BLOCK)
                                .displayName(targetEntity.isSilent() ? Component.text("消音: 有効", DefinedTextColor.GREEN) : Component.text("消音: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (targetEntity, event) -> targetEntity.setSilent(!targetEntity.isSilent())),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.END_PORTAL_FRAME)
                                .displayName(targetEntity.hasGravity() ? Component.text("重力: 有効", DefinedTextColor.GREEN) : Component.text("重力: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (targetEntity, event) -> targetEntity.setGravity(!targetEntity.hasGravity())),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.hasNoPhysics() ? Material.STRUCTURE_VOID : Material.SCAFFOLDING)
                                .displayName(targetEntity.hasNoPhysics() ? Component.text("物理演算: 無効", DefinedTextColor.RED) : Component.text("物理演算: 有効", DefinedTextColor.GREEN))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (targetEntity, event) -> {
                            if (!targetEntity.hasNoPhysics() && targetEntity.hasGravity()) {
                                targetEntity.setGravity(false);
                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.text("自動的に [重力] が無効になりました。[重力] は有効にすることもできますが、NoClipが発生します。", DefinedTextColor.YELLOW));
                            }
                            targetEntity.setNoPhysics(!targetEntity.hasNoPhysics());
                        }
                ),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.isFrozen() ? Material.ICE : Material.GLASS)
                                .displayName(targetEntity.isFrozen() ? Component.text("凍結: 有効", DefinedTextColor.GREEN) : Component.text("凍結: 無効", DefinedTextColor.RED))
                                .lore(Component.text(targetEntity.isFrozen() ? "クリックで凍結解除 (残り" + targetEntity.getFreezeTicks() + "ﾃｨｯｸ)" : "クリックで30秒凍結", DefinedTextColor.YELLOW))
                                .build(),
                        (targetEntity, event) -> targetEntity.setFreezeTicks(targetEntity.isFrozen() ? 0 : 20 * 30)
                ),
                EntityEditor.Element.of(
                        (targetEntity) -> new ItemStackBuilder(targetEntity.isFreezeTickingLocked() ? Material.PACKED_ICE : Material.BARRIER)
                                .displayName(targetEntity.isFreezeTickingLocked() ? Component.text("凍結固定: 有効", DefinedTextColor.GREEN) : Component.text("凍結固定: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (targetEntity, event) -> targetEntity.lockFreezeTicks(!targetEntity.isFreezeTickingLocked())
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.LIGHT)
                                .displayName(targetEntity.isGlowing() ? Component.text("発光: 有効", DefinedTextColor.GREEN) : Component.text("発光: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (targetEntity, event) -> targetEntity.setGlowing(!targetEntity.isGlowing())),
                EntityEditor.Element.of(
                        targetEntity -> {
                            net.minecraft.world.entity.Entity nms = MinecraftAdapter.entity(targetEntity);
                            return new ItemStackBuilder(Material.COMPASS)
                                    .displayName(Component.text("向きを変更", DefinedTextColor.YELLOW))
                                    .lore(Component.text("X: " + nms.getXRot() + " | Y: " + nms.getYRot(), DefinedTextColor.GREEN),
                                            Component.empty(),
                                            Component.text("左クリック: X+  右クリック: Y+", DefinedTextColor.YELLOW),
                                            Component.text("Shift: デクリメント  中クリック: リセット", DefinedTextColor.YELLOW)).build();
                        },
                        (targetEntity, event) -> {
                            net.minecraft.world.entity.Entity h = ((CraftEntity) targetEntity).getHandle();
                            if (event.isLeftClick()) {
                                h.setXRot(loop(h.getXRot() + (event.isShiftClick() ? -10f : 10f), -180f, 180f));
                            } else if (event.isRightClick()) {
                                h.setYRot(loop(h.getYRot() + (event.isShiftClick() ? -10f : 10f), -180f, 180f));
                                h.setYHeadRot(h.getYRot());
                                h.setYBodyRot(h.getYRot());
                            } else if (event.getClick() == ClickType.MIDDLE) {
                                h.setXRot(0f);
                                h.setYRot(0f);
                                h.setYHeadRot(0f);
                                h.setYBodyRot(0f);
                            }
                        }),
                EntityEditor.Element.lineBreak()
        );
    }
}
