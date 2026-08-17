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
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class HumanEntityHandler implements EntityEditorHandler<HumanEntity> {

    @Override
    public List<EntityEditor.Element<HumanEntity>> getElements(HumanEntity entity) {
        GameMode[] gameModes = GameMode.values();

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            GameMode current = targetEntity.getGameMode();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (GameMode mode : gameModes) {
                                lore.add(Component.text((mode == current ? "▶" : "■"), mode == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(mode.name())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.COMMAND_BLOCK)
                                    .displayName(Component.text("ゲームモード: " + current.name(), DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            GameMode current = targetEntity.getGameMode();
                            int index = -1;
                            for (int i = 0; i < gameModes.length; i++) {
                                if (gameModes[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setGameMode(gameModes[(index + 1) % gameModes.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setGameMode(gameModes[(index - 1 + gameModes.length) % gameModes.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.COOKED_BEEF)
                                .displayName(Component.text("満腹度: " + targetEntity.getFoodLevel(), DefinedTextColor.GREEN))
                                .lore(Component.text("クリックで値を入力 (0-20)", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            Player player = (Player) event.getWhoClicked();
                            editor.onceDeferUnregisterOnClose();
                            new SignGui()
                                    .withTarget(player)
                                    .withLines(Component.empty(), Component.text("^^^"), Component.text("満腹度 (0-20) を入力"), Component.empty())
                                    .onComplete(lines -> {
                                        try {
                                            String input = ((TextComponent) lines.get(0)).content();
                                            int val = Integer.parseInt(input);
                                            targetEntity.setFoodLevel(val);
                                        } catch (Exception ignored) {
                                        }
                                        editor.open(player);
                                    })
                                    .open();
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.GOLDEN_CARROT)
                                .displayName(Component.text("Saturation: " + targetEntity.getSaturation(), DefinedTextColor.GREEN))
                                .lore(Component.text("クリックで値を入力", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            Player player = (Player) event.getWhoClicked();
                            editor.onceDeferUnregisterOnClose();
                            new SignGui()
                                    .withTarget(player)
                                    .withLines(Component.empty(), Component.text("^^^"), Component.text("隠し満腹度を入力"), Component.empty())
                                    .onComplete(lines -> {
                                        try {
                                            String input = ((TextComponent) lines.get(0)).content();
                                            float val = Float.parseFloat(input);
                                            targetEntity.setSaturation(val);
                                        } catch (Exception ignored) {
                                        }
                                        editor.open(player);
                                    })
                                    .open();
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
