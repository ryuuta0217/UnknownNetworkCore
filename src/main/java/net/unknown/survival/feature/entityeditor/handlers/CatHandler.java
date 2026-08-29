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
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Cat;

import java.util.List;

public class CatHandler implements EntityEditorHandler<Cat> {
    @Override
    public List<EntityEditor.Element<Cat>> getElements(Cat entity) {
        Cat.Type[] types = new Cat.Type[]{
                Cat.Type.ALL_BLACK, Cat.Type.BLACK, Cat.Type.BRITISH_SHORTHAIR,
                Cat.Type.CALICO, Cat.Type.JELLIE, Cat.Type.PERSIAN,
                Cat.Type.RAGDOLL, Cat.Type.RED, Cat.Type.SIAMESE,
                Cat.Type.TABBY, Cat.Type.WHITE
        };
        DyeColor[] colors = DyeColor.values();

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            Cat.Type current = targetEntity.getCatType();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Cat.Type t : types) {
                                lore.add(Component.text((t == current ? "▶" : "■"), t == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(t.getKey().value())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.COD)
                                    .displayName(Component.text("種類: " + current.getKey().value(), DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Cat.Type current = targetEntity.getCatType();
                            int index = -1;
                            for (int i = 0; i < types.length; i++) {
                                if (types[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setCatType(types[(index + 1) % types.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setCatType(types[(index - 1 + types.length) % types.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> {
                            DyeColor current = targetEntity.getCollarColor();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            lore.add(Component.text("※手懐けられている状態にする必要があります", DefinedTextColor.YELLOW));
                            for (DyeColor c : colors) {
                                lore.add(Component.text((c == current ? "▶" : "■"), c == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(ColorableHandler.colorName(c)));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(ColorableHandler.dyeMaterial(current))
                                    .displayName(Component.text("首輪の色:", ColorableHandler.textColor(current)).appendSpace().append(ColorableHandler.colorName(current)))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            DyeColor current = targetEntity.getCollarColor();
                            int index = -1;
                            for (int i = 0; i < colors.length; i++) {
                                if (colors[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setCollarColor(colors[(index + 1) % colors.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setCollarColor(colors[(index - 1 + colors.length) % colors.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
