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
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

public class BoatHandler implements EntityEditorHandler<Boat> {

    @Override
    public List<EntityEditor.Element<Boat>> getElements(Boat entity) {
        List<EntityEditor.Element<Boat>> elements = new ArrayList<>();

        Boat.Type[] types = Boat.Type.values();
        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    Boat.Type current = targetEntity.getBoatType();
                    List<Component> lore = new ArrayList<>();
                    for (Boat.Type t : types) {
                        lore.add(Component.text((t == current ? "▶ " : "■ ") + t.name(), t == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY));
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                    return new ItemStackBuilder(Material.OAK_BOAT)
                            .displayName(Component.text("種類: ", DefinedTextColor.GREEN).append(Component.text(current.name(), DefinedTextColor.GREEN)))
                            .lore(lore.toArray(Component[]::new))
                            .build();
                },
                (editor, targetEntity, event) -> {
                    Boat.Type current = targetEntity.getBoatType();
                    int idx = -1;
                    for (int i = 0; i < types.length; i++) {
                        if (types[i] == current) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        if (event.isRightClick()) targetEntity.setBoatType(types[(idx + 1) % types.length]);
                        else if (event.isLeftClick())
                            targetEntity.setBoatType(types[(idx - 1 + types.length) % types.length]);
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.FEATHER)
                        .displayName(Component.text("MaxSpeed: " + String.format("%.2f", targetEntity.getMaxSpeed()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -0.1 | 右クリック: +0.1", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setMaxSpeed(Math.max(0.0, targetEntity.getMaxSpeed() - 0.1));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setMaxSpeed(Math.min(100.0, targetEntity.getMaxSpeed() + 0.1));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("最高速度を入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        double val = Double.parseDouble(((TextComponent) lines.get(0)).content());
                                        targetEntity.setMaxSpeed(Math.max(0.0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.lineBreak());
        return elements;
    }
}
