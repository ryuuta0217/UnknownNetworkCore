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
import org.bukkit.entity.Fox;

import java.util.ArrayList;
import java.util.List;

public class FoxHandler implements EntityEditorHandler<Fox> {

    @Override
    public List<EntityEditor.Element<Fox>> getElements(Fox entity) {
        List<EntityEditor.Element<Fox>> elements = new ArrayList<>();

        Fox.Type[] types = Fox.Type.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    Fox.Type current = targetEntity.getFoxType();
                    List<Component> lore = new ArrayList<>();
                    for (Fox.Type t : types) {
                        lore.add(Component.text((t == current ? "▶ " : "■ ") + t.name(), t == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.FOX_SPAWN_EGG)
                            .displayName(Component.text("種類: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    Fox.Type current = targetEntity.getFoxType();
                    int idx = -1;
                    for (int i = 0; i < types.length; i++) {
                        if (types[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setFoxType(types[(idx + 1) % types.length]);
                        else if (event.isLeftClick())
                            targetEntity.setFoxType(types[(idx - 1 + types.length) % types.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GRASS_BLOCK)
                        .displayName(Component.text("丸まり: " + (targetEntity.isCrouching() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isCrouching() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setCrouching(!targetEntity.isCrouching())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.WHITE_BED)
                        .displayName(Component.text("睡眠: " + (targetEntity.isSleeping() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isSleeping() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setSleeping(!targetEntity.isSleeping())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.SHIELD)
                        .displayName(Component.text("防御: " + (targetEntity.isDefending() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isDefending() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setDefending(!targetEntity.isDefending())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.RABBIT_FOOT)
                        .displayName(Component.text("跳躍: " + (targetEntity.isLeaping() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isLeaping() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setLeaping(!targetEntity.isLeaping())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.DIRT)
                        .displayName(Component.text("顔面着地: " + (targetEntity.isFaceplanted() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isFaceplanted() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setFaceplanted(!targetEntity.isFaceplanted())
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
