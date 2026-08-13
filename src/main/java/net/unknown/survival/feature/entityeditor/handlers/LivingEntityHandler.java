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
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class LivingEntityHandler implements EntityEditorHandler<LivingEntity> {

    @Override
    public List<EntityEditor.Element<LivingEntity>> getElements(LivingEntity entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.GLASS)
                                .displayName(targetEntity.isInvisible() ? Component.text("透明: 有効", DefinedTextColor.GREEN) : Component.text("透明: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW)).build(),
                        (targetEntity, event) -> {
                            targetEntity.setInvisible(!targetEntity.isInvisible());
                            boolean oldGlowing = targetEntity.isGlowing();
                            targetEntity.setGlowing(targetEntity.isInvisible());
                            if (!oldGlowing && targetEntity.isGlowing()) {
                                event.getWhoClicked().sendMessage(Component.text("自動的に [発光] が有効になりました。\n[発光] はオフにすることもできます。", DefinedTextColor.YELLOW));
                            }
                        }),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.PLAYER_HEAD)
                                .displayName(targetEntity.hasAI() ? Component.text("AI: 有効", DefinedTextColor.GREEN) : Component.text("AI: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW)).build(),
                        (targetEntity, event) -> targetEntity.setAI(!targetEntity.hasAI())),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.HOPPER)
                                .displayName(targetEntity.getCanPickupItems() ? Component.text("アイテム拾い: 有効", DefinedTextColor.GREEN) : Component.text("アイテム拾い: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW)).build(),
                        (targetEntity, event) -> targetEntity.setCanPickupItems(!targetEntity.getCanPickupItems())),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.BARRIER)
                                .displayName(targetEntity.isCollidable() ? Component.text("当たり判定: 有効", DefinedTextColor.GREEN) : Component.text("当たり判定: 無効", DefinedTextColor.RED))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW)).build(),
                        (targetEntity, event) -> targetEntity.setCollidable(!targetEntity.isCollidable())),
                EntityEditor.Element.lineBreak()
        );
    }
}
