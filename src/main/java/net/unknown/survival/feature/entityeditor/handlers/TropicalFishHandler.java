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
import org.bukkit.entity.TropicalFish;

import java.util.ArrayList;
import java.util.List;

public class TropicalFishHandler implements EntityEditorHandler<TropicalFish> {

    @Override
    public List<EntityEditor.Element<TropicalFish>> getElements(TropicalFish entity) {
        List<EntityEditor.Element<TropicalFish>> elements = new ArrayList<>();

        TropicalFish.Pattern[] patterns = TropicalFish.Pattern.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    TropicalFish.Pattern current = targetEntity.getPattern();
                    List<Component> lore = new ArrayList<>();
                    for (TropicalFish.Pattern p : patterns) {
                        lore.add(Component.text((p == current ? "▶ " : "■ ") + p.name(), p == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.TROPICAL_FISH)
                            .displayName(Component.text("Pattern: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    TropicalFish.Pattern current = targetEntity.getPattern();
                    int idx = -1;
                    for (int i = 0; i < patterns.length; i++) {
                        if (patterns[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setPattern(patterns[(idx + 1) % patterns.length]);
                        else if (event.isLeftClick())
                            targetEntity.setPattern(patterns[(idx - 1 + patterns.length) % patterns.length]);
                    }
                }
        ));

        DyeColor[] colors = DyeColor.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    DyeColor current = targetEntity.getBodyColor();
                    List<Component> lore = new ArrayList<>();
                    for (DyeColor c : colors) {
                        lore.add(Component.text((c == current ? "▶ " : "■ ") + c.name(), c == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.ORANGE_DYE)
                            .displayName(Component.text("体の色: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    DyeColor current = targetEntity.getBodyColor();
                    int idx = -1;
                    for (int i = 0; i < colors.length; i++) {
                        if (colors[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setBodyColor(colors[(idx + 1) % colors.length]);
                        else if (event.isLeftClick())
                            targetEntity.setBodyColor(colors[(idx - 1 + colors.length) % colors.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    DyeColor current = targetEntity.getPatternColor();
                    List<Component> lore = new ArrayList<>();
                    for (DyeColor c : colors) {
                        lore.add(Component.text((c == current ? "▶ " : "■ ") + c.name(), c == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.CYAN_DYE)
                            .displayName(Component.text("模様の色: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    DyeColor current = targetEntity.getPatternColor();
                    int idx = -1;
                    for (int i = 0; i < colors.length; i++) {
                        if (colors[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setPatternColor(colors[(idx + 1) % colors.length]);
                        else if (event.isLeftClick())
                            targetEntity.setPatternColor(colors[(idx - 1 + colors.length) % colors.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
