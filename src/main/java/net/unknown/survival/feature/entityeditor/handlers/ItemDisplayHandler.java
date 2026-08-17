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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemDisplayHandler implements EntityEditorHandler<ItemDisplay> {

    @Override
    public List<EntityEditor.Element<ItemDisplay>> getElements(ItemDisplay entity) {
        List<EntityEditor.Element<ItemDisplay>> elements = new ArrayList<>();

        ItemDisplay.ItemDisplayTransform[] transforms = ItemDisplay.ItemDisplayTransform.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    ItemDisplay.ItemDisplayTransform current = targetEntity.getItemDisplayTransform();
                    List<Component> lore = new ArrayList<>();
                    for (ItemDisplay.ItemDisplayTransform t : transforms) {
                        lore.add(Component.text((t == current ? "▶ " : "■ ") + t.name(), t == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.ITEM_FRAME)
                            .displayName(Component.text("描画タイプ: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    ItemDisplay.ItemDisplayTransform current = targetEntity.getItemDisplayTransform();
                    int idx = -1;
                    for (int i = 0; i < transforms.length; i++) {
                        if (transforms[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick())
                            targetEntity.setItemDisplayTransform(transforms[(idx + 1) % transforms.length]);
                        else if (event.isLeftClick())
                            targetEntity.setItemDisplayTransform(transforms[(idx - 1 + transforms.length) % transforms.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(targetEntity.getItemStack() != null && targetEntity.getItemStack().getType() != Material.AIR ? targetEntity.getItemStack().getType() : Material.BARRIER)
                        .displayName(Component.text("表示アイテムの変更", DefinedTextColor.GREEN))
                        .lore(Component.text("マウスカーソルで掴んでいるアイテムを適用します", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR && cursor.getType() != Material.DEBUG_STICK) {
                        targetEntity.setItemStack(cursor.clone());
                    }
                }
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
