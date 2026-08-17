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
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FireworkHandler implements EntityEditorHandler<Firework> {

    @Override
    public List<EntityEditor.Element<Firework>> getElements(Firework entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.CLOCK)
                                .displayName(Component.text("TicksToDetonate", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + targetEntity.getTicksToDetonate() + " ticks", DefinedTextColor.GRAY),
                                        Component.text("クリックで入力", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            int initTicks = targetEntity.getTicksToDetonate();
                            Player player = (Player) event.getWhoClicked();
                            editor.onceDeferUnregisterOnClose();
                            Dialog dialog = Dialog.create(builder -> builder.empty()
                                    .base(DialogBase.builder(Component.text("起爆ティックの設定"))
                                            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                            .body(Collections.singletonList(DialogBody.plainMessage(Component.text("起爆ティックを入力 (0~1000)"), 200)))
                                            .inputs(List.of(
                                                    DialogInput.numberRange("ticks", Component.text("Tick数"), 0f, 1000f).initial((float) initTicks).step(1f).build()
                                            ))
                                            .canCloseWithEscape(true)
                                            .build())
                                    .type(DialogType.confirmation(
                                            ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                                Float newTicks = response.getFloat("ticks");
                                                if (newTicks != null) {
                                                    targetEntity.setTicksToDetonate(newTicks.intValue());
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
                        targetEntity -> new ItemStackBuilder(Material.CROSSBOW)
                                .displayName(Component.text("ShotAtAngle", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + (targetEntity.isShotAtAngle() ? "はい" : "いいえ"), DefinedTextColor.GRAY),
                                        Component.text("クリックで切り替え", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setShotAtAngle(!targetEntity.isShotAtAngle())
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.TNT)
                                .displayName(Component.text("Detonate", DefinedTextColor.RED))
                                .lore(
                                        Component.text("クリックで即時爆発させます", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            targetEntity.detonate();
                            event.getWhoClicked().closeInventory();
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
