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
import org.bukkit.Art;
import org.bukkit.Material;
import org.bukkit.entity.Painting;

import java.util.ArrayList;
import java.util.List;

public class PaintingHandler implements EntityEditorHandler<Painting> {

    @Override
    public List<EntityEditor.Element<Painting>> getElements(Painting entity) {
        List<EntityEditor.Element<Painting>> elements = new ArrayList<>();

        Art[] arts = Art.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    Art current = targetEntity.getArt();
                    List<Component> lore = new ArrayList<>();
                    // To prevent lore from getting too big and clipping off screen, just show surrounding arts
                    int currentIdx = -1;
                    for (int i = 0; i < arts.length; i++) {
                        if (arts[i] == current) {
                            currentIdx = i;
                            break;
                        }
                    }

                    for (int i = -3; i <= 3; i++) {
                        int idx = (currentIdx + i + arts.length) % arts.length;
                        Art a = arts[idx];
                        lore.add(Component.text((a == current ? "▶ " : "■ ") + a.name(), a == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }

                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.PAINTING)
                            .displayName(Component.text("アート: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    Art current = targetEntity.getArt();
                    int idx = -1;
                    for (int i = 0; i < arts.length; i++) {
                        if (arts[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        try {
                            if (event.isRightClick()) targetEntity.setArt(arts[(idx + 1) % arts.length]);
                            else if (event.isLeftClick())
                                targetEntity.setArt(arts[(idx - 1 + arts.length) % arts.length]);
                        } catch (IllegalArgumentException ignored) {
                            // Painting doesn't fit in the space
                        }
                    }
                }
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
