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
import org.bukkit.entity.Phantom;

import java.util.List;

public class PhantomHandler implements EntityEditorHandler<Phantom> {

    @Override
    public List<EntityEditor.Element<Phantom>> getElements(Phantom entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.PHANTOM_MEMBRANE)
                                .displayName(Component.text("Size", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + targetEntity.getSize(), DefinedTextColor.GRAY),
                                        Component.text("左/右クリックで増減(0〜64)", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            int size = targetEntity.getSize();
                            if (event.isLeftClick()) {
                                targetEntity.setSize(Math.max(0, size - 1));
                            } else if (event.isRightClick()) {
                                targetEntity.setSize(Math.min(64, size + 1));
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.shouldBurnInDay() ? Material.CAMPFIRE : Material.SOUL_CAMPFIRE)
                                .displayName(Component.text("ShouldBurnInDay", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + (targetEntity.shouldBurnInDay() ? "はい" : "いいえ"), DefinedTextColor.GRAY),
                                        Component.text("クリックで切り替え", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setShouldBurnInDay(!targetEntity.shouldBurnInDay())
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
