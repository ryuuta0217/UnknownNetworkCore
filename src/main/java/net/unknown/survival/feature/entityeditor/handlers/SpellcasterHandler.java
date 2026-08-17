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
import org.bukkit.entity.Spellcaster;

import java.util.List;

public class SpellcasterHandler implements EntityEditorHandler<Spellcaster> {

    @Override
    public List<EntityEditor.Element<Spellcaster>> getElements(Spellcaster entity) {
        Spellcaster.Spell[] spells = Spellcaster.Spell.values();

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            Spellcaster.Spell current = targetEntity.getSpell();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Spellcaster.Spell s : spells) {
                                lore.add(Component.text((s == current ? "▶" : "■"), s == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(s.name())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("Left Click: Previous | Right Click: Next", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.ENCHANTED_BOOK)
                                    .displayName(Component.text("Spell", DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Spellcaster.Spell current = targetEntity.getSpell();
                            int index = -1;
                            for (int i = 0; i < spells.length; i++) {
                                if (spells[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setSpell(spells[(index + 1) % spells.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setSpell(spells[(index - 1 + spells.length) % spells.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
