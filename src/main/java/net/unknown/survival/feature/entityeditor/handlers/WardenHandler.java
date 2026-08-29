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
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;

import java.util.ArrayList;
import java.util.List;

public class WardenHandler implements EntityEditorHandler<Warden> {

    @Override
    public List<EntityEditor.Element<Warden>> getElements(Warden entity) {
        List<EntityEditor.Element<Warden>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.SCULK_SENSOR)
                        .displayName(Component.text("プレイヤーへの怒りを追加 (+10)", DefinedTextColor.GREEN))
                        .lore(Component.text("クリックで実行", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> {
                    Player player = (Player) event.getWhoClicked();
                    targetEntity.setAnger(player, Math.min(150, targetEntity.getAnger(player) + 10));
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.SCULK_SHRIEKER)
                        .displayName(Component.text("clearAnger", DefinedTextColor.LIGHT_PURPLE))
                        .lore(Component.text("クリックで実行", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.clearAnger(targetEntity.getEntityAngryAt())
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
