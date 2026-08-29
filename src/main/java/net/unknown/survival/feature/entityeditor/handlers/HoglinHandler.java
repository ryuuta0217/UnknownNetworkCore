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
import org.bukkit.entity.Hoglin;

import java.util.ArrayList;
import java.util.List;

public class HoglinHandler implements EntityEditorHandler<Hoglin> {

    @Override
    public List<EntityEditor.Element<Hoglin>> getElements(Hoglin entity) {
        List<EntityEditor.Element<Hoglin>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.WARPED_FUNGUS)
                        .displayName(Component.text("ゾンビ化免除: " + (targetEntity.isImmuneToZombification() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isImmuneToZombification() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setImmuneToZombification(!targetEntity.isImmuneToZombification())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.PORKCHOP)
                        .displayName(Component.text("IsAbleToBeHunted: " + (targetEntity.isAbleToBeHunted() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isAbleToBeHunted() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setIsAbleToBeHunted(!targetEntity.isAbleToBeHunted())
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
