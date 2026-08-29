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
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

public class DamageableHandler implements EntityEditorHandler<Damageable> {

    @Override
    public List<EntityEditor.Element<Damageable>> getElements(Damageable entity) {
        List<EntityEditor.Element<Damageable>> elements = new ArrayList<>();

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.APPLE)
                        .displayName(Component.text("体力: " + String.format("%.2f", targetEntity.getHealth()), DefinedTextColor.GREEN))
                        .lore(
                                Component.text("左クリック: -1.0 | 右クリック: +1.0", DefinedTextColor.YELLOW),
                                Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                        )
                        .build(),
                (editor, targetEntity, event) -> {
                    AttributeInstance maxHealthAttr = targetEntity instanceof Attributable attr ? attr.getAttribute(Attribute.MAX_HEALTH) : null;
                    double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : targetEntity.getHealth();
                    if (event.getClick().isLeftClick()) {
                        targetEntity.setHealth(Math.max(0.0, targetEntity.getHealth() - 1.0));
                    } else if (event.getClick().isRightClick()) {
                        targetEntity.setHealth(Math.min(maxHealth, targetEntity.getHealth() + 1.0));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("HPを入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        double val = Double.parseDouble(((TextComponent) lines.get(0)).content());
                                        targetEntity.setHealth(Math.max(0.0, Math.min(maxHealth, val)));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> {
                    AttributeInstance maxHealthAttr = targetEntity instanceof Attributable attr ? attr.getAttribute(Attribute.MAX_HEALTH) : null;
                    double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 0.0;
                    return new ItemStackBuilder(Material.GOLDEN_APPLE)
                            .displayName(Component.text("最大体力: " + String.format("%.2f", maxHealth), DefinedTextColor.GREEN))
                            .lore(
                                    Component.text("左クリック: -1.0 | 右クリック: +1.0", DefinedTextColor.YELLOW),
                                    Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                            )
                            .build();
                },
                (editor, targetEntity, event) -> {
                    AttributeInstance maxHealthAttr = targetEntity instanceof Attributable attr ? attr.getAttribute(Attribute.MAX_HEALTH) : null;
                    if (maxHealthAttr == null) return;
                    if (event.getClick().isLeftClick()) {
                        maxHealthAttr.setBaseValue(Math.max(1.0, maxHealthAttr.getBaseValue() - 1.0));
                    } else if (event.getClick().isRightClick()) {
                        maxHealthAttr.setBaseValue(Math.min(1000000.0, maxHealthAttr.getBaseValue() + 1.0));
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        Player player = (Player) event.getWhoClicked();
                        editor.onceDeferUnregisterOnClose();
                        new SignGui()
                                .withTarget(player)
                                .withLines(Component.empty(), Component.text("^^^"), Component.text("最大HPを入力"), Component.empty())
                                .onComplete(lines -> {
                                    try {
                                        double val = Double.parseDouble(((TextComponent) lines.get(0)).content());
                                        maxHealthAttr.setBaseValue(Math.max(1.0, val));
                                    } catch (Exception ignored) {
                                    }
                                    editor.open(player);
                                })
                                .open();
                    }
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.POTION)
                        .displayName(Component.text("体力回復", DefinedTextColor.LIGHT_PURPLE))
                        .lore(Component.text("クリックで実行", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> {
                    AttributeInstance maxHealthAttr = targetEntity instanceof Attributable attr ? attr.getAttribute(Attribute.MAX_HEALTH) : null;
                    double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
                    targetEntity.setHealth(maxHealth);
                }
        ));

        elements.add(EntityEditor.Element.of(
                targetEntity -> new ItemStackBuilder(Material.IRON_SWORD)
                        .displayName(Component.text("Kill", DefinedTextColor.RED))
                        .lore(Component.text("クリックで実行", DefinedTextColor.YELLOW))
                        .build(),
                (editor, targetEntity, event) -> targetEntity.setHealth(0.0)
        ));
        return elements;
    }
}
