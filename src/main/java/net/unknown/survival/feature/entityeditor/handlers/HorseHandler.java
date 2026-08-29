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
import org.bukkit.entity.Horse;

import java.util.List;

public class HorseHandler implements EntityEditorHandler<Horse> {
    @Override
    public List<EntityEditor.Element<Horse>> getElements(Horse entity) {
        Horse.Color[] colors = Horse.Color.values();
        Horse.Style[] styles = Horse.Style.values();

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            Horse.Color current = targetEntity.getColor();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Horse.Color c : colors) {
                                lore.add(Component.text((c == current ? "▶" : "■"), c == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(c.name())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.LEATHER_HORSE_ARMOR)
                                    .displayName(Component.text("毛色: " + current.name(), DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Horse.Color current = targetEntity.getColor();
                            int index = -1;
                            for (int i = 0; i < colors.length; i++) {
                                if (colors[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setColor(colors[(index + 1) % colors.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setColor(colors[(index - 1 + colors.length) % colors.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> {
                            Horse.Style current = targetEntity.getStyle();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Horse.Style s : styles) {
                                lore.add(Component.text((s == current ? "▶" : "■"), s == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(s.name())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.IRON_HORSE_ARMOR)
                                    .displayName(Component.text("模様: " + current.name(), DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Horse.Style current = targetEntity.getStyle();
                            int index = -1;
                            for (int i = 0; i < styles.length; i++) {
                                if (styles[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setStyle(styles[(index + 1) % styles.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setStyle(styles[(index - 1 + styles.length) % styles.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
