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
import org.bukkit.entity.Enderman;

import java.util.ArrayList;
import java.util.List;

public class EndermanHandler implements EntityEditorHandler<Enderman> {

    @Override
    public List<EntityEditor.Element<Enderman>> getElements(Enderman entity) {
        List<EntityEditor.Element<Enderman>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GHAST_TEAR)
                        .displayName(Component.text("叫んでいるか: " + (targetEntity.isScreaming() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isScreaming() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setScreaming(!targetEntity.isScreaming())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.ENDER_EYE)
                        .displayName(Component.text("見つめられたか: " + (targetEntity.hasBeenStaredAt() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.hasBeenStaredAt() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setHasBeenStaredAt(!targetEntity.hasBeenStaredAt())
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.ENDER_PEARL)
                        .displayName(Component.text("teleportRandomly", DefinedTextColor.LIGHT_PURPLE))
                        .lore(Component.text("クリックで実行", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.teleportRandomly()
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
