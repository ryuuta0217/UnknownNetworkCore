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
import net.kyori.adventure.text.TextComponent;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.Material;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

public class PigZombieHandler implements EntityEditorHandler<PigZombie> {

    @Override
    public List<EntityEditor.Element<PigZombie>> getElements(PigZombie entity) {
        List<EntityEditor.Element<PigZombie>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GOLDEN_SWORD)
                        .displayName(Component.text("怒り時間: " + targetEntity.getAnger() + "ﾃｨｯｸ", DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -100 | 右クリック: +100", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setAnger(Math.max(0, targetEntity.getAnger() - 100));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setAnger(Math.min(100000, targetEntity.getAnger() + 100));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("怒り時間を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        int val = Integer.parseInt(((TextComponent) lines.get(0)).content());
                                        targetEntity.setAnger(Math.max(0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.GOLD_NUGGET)
                        .displayName(Component.text("isAngry: " + (targetEntity.isAngry() ? "有効 (ON)" : "無効 (OFF)"), targetEntity.isAngry() ? DefinedTextColor.GREEN : DefinedTextColor.RED))
                        .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setAngry(!targetEntity.isAngry())
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
