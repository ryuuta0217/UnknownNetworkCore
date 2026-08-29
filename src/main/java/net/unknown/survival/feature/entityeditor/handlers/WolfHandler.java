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
import org.bukkit.entity.Wolf;

import java.util.List;

public class WolfHandler implements EntityEditorHandler<Wolf> {
    @Override
    public List<EntityEditor.Element<Wolf>> getElements(Wolf entity) {
        Wolf.Variant[] variants = new Wolf.Variant[]{
                Wolf.Variant.ASHEN, Wolf.Variant.BLACK, Wolf.Variant.CHESTNUT,
                Wolf.Variant.PALE, Wolf.Variant.RUSTY, Wolf.Variant.SNOWY,
                Wolf.Variant.SPOTTED, Wolf.Variant.STRIPED, Wolf.Variant.WOODS
        };
        DyeColor[] colors = DyeColor.values();

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            Wolf.Variant current = targetEntity.getVariant();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Wolf.Variant v : variants) {
                                lore.add(Component.text((v == current ? "▶" : "■"), v == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(v.getKey().value())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("Left Click: Previous | Right Click: Next", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.BONE)
                                    .displayName(Component.text("Variant: " + current.getKey().value(), DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Wolf.Variant current = targetEntity.getVariant();
                            int index = -1;
                            for (int i = 0; i < variants.length; i++) {
                                if (variants[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setVariant(variants[(index + 1) % variants.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setVariant(variants[(index - 1 + variants.length) % variants.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> {
                            DyeColor current = targetEntity.getCollarColor();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (DyeColor c : colors) {
                                lore.add(Component.text((c == current ? "▶" : "■"), c == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(ColorableHandler.colorName(c)));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(ColorableHandler.dyeMaterial(current))
                                    .displayName(Component.text("CollarColor:", ColorableHandler.textColor(current)).appendSpace().append(ColorableHandler.colorName(current)))
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
