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

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AbstractArrowHandler implements EntityEditorHandler<AbstractArrow> {

    @Override
    public List<EntityEditor.Element<AbstractArrow>> getElements(AbstractArrow entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.IRON_SWORD)
                                .displayName(Component.text("Damage", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + String.format("%.2f", targetEntity.getDamage()), DefinedTextColor.GRAY),
                                        Component.text("クリックで入力", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            double initDamage = targetEntity.getDamage();
                            Player player = (Player) event.getWhoClicked();
                            editor.onceDeferUnregisterOnClose();
                            Dialog dialog = Dialog.create(builder -> builder.empty()
                                    .base(DialogBase.builder(Component.text("ダメージの設定"))
                                            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                            .body(Collections.singletonList(DialogBody.plainMessage(Component.text("ダメージを入力 (0~1000)"), 200)))
                                            .inputs(List.of(
                                                    DialogInput.numberRange("damage", Component.text("ダメージ"), 0f, 1000f).initial((float) initDamage).step(0.5f).build()
                                            ))
                                            .canCloseWithEscape(true)
                                            .build())
                                    .type(DialogType.confirmation(
                                            ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                                Float newDamage = response.getFloat("damage");
                                                if (newDamage != null) {
                                                    targetEntity.setDamage(newDamage);
                                                }
                                                editor.open(player);
                                            }, ClickCallback.Options.builder().uses(1).build())),
                                            ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                                editor.open(player);
                                            }, ClickCallback.Options.builder().uses(1).build()))
                                    ))
                            );
                            player.showDialog(dialog);
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.ANVIL)
                                .displayName(Component.text("PierceLevel", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + targetEntity.getPierceLevel(), DefinedTextColor.GRAY),
                                        Component.text("左/右クリックで増減(0〜127)", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            int level = targetEntity.getPierceLevel();
                            if (event.isLeftClick()) {
                                targetEntity.setPierceLevel(Math.max(0, level - 1));
                            } else if (event.isRightClick()) {
                                targetEntity.setPierceLevel(Math.min(127, level + 1));
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.isCritical() ? Material.END_CRYSTAL : Material.NETHER_STAR)
                                .displayName(Component.text("Critical", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + (targetEntity.isCritical() ? "はい" : "いいえ"), DefinedTextColor.GRAY),
                                        Component.text("クリックで切り替え", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setCritical(!targetEntity.isCritical())
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
