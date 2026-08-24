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
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class WanderingTraderHandler implements EntityEditorHandler<WanderingTrader> {

    @Override
    public List<EntityEditor.Element<WanderingTrader>> getElements(WanderingTrader entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.CLOCK)
                                .displayName(Component.text("デスポーンまでの遅延", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + targetEntity.getDespawnDelay() + "ﾃｨｯｸ (" + (targetEntity.getDespawnDelay() / 20) + "秒)", DefinedTextColor.GRAY),
                                        Component.text("左/右クリックで増減(20) | 中クリックでリセット", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            int current = targetEntity.getDespawnDelay();
                            if (event.getClick() == ClickType.MIDDLE) {
                                targetEntity.setDespawnDelay(48000);
                            } else if (event.isLeftClick()) {
                                targetEntity.setDespawnDelay(Math.max(0, current - 20));
                            } else if (event.isRightClick()) {
                                targetEntity.setDespawnDelay(current + 20);
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.canDrinkPotion() ? Material.POTION : Material.GLASS_BOTTLE)
                                .displayName(Component.text("ポーションを飲めるか: " + (targetEntity.canDrinkPotion() ? "はい" : "いいえ"), targetEntity.canDrinkPotion() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setCanDrinkPotion(!targetEntity.canDrinkPotion())
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.canDrinkMilk() ? Material.MILK_BUCKET : Material.BUCKET)
                                .displayName(Component.text("ミルクを飲めるか: " + (targetEntity.canDrinkMilk() ? "はい" : "いいえ"), targetEntity.canDrinkPotion() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setCanDrinkMilk(!targetEntity.canDrinkMilk())
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
