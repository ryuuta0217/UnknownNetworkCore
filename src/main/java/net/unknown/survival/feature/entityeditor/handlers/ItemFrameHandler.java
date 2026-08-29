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
import org.bukkit.Rotation;
import org.bukkit.entity.ItemFrame;

import java.util.ArrayList;
import java.util.List;

public class ItemFrameHandler implements EntityEditorHandler<ItemFrame> {

    @Override
    public List<EntityEditor.Element<ItemFrame>> getElements(ItemFrame entity) {
        List<EntityEditor.Element<ItemFrame>> elements = new ArrayList<>();

        Rotation[] rotations = Rotation.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    Rotation current = targetEntity.getRotation();
                    List<Component> lore = new ArrayList<>();
                    for (Rotation r : rotations) {
                        lore.add(Component.text((r == current ? "▶ " : "■ ") + r.name(), r == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.ITEM_FRAME)
                            .displayName(Component.text("回転: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    Rotation current = targetEntity.getRotation();
                    int idx = -1;
                    for (int i = 0; i < rotations.length; i++) {
                        if (rotations[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setRotation(rotations[(idx + 1) % rotations.length]);
                        else if (event.isLeftClick())
                            targetEntity.setRotation(rotations[(idx - 1 + rotations.length) % rotations.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.BARRIER)
                        .displayName(Component.text("Fixed: " + (targetEntity.isFixed() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isFixed() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setFixed(!targetEntity.isFixed())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GLASS)
                        .displayName(Component.text("Visible: " + (targetEntity.isVisible() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isVisible() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setVisible(!targetEntity.isVisible())
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
