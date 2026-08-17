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
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ExplosiveHandler implements EntityEditorHandler<Explosive> {

    @Override
    public List<EntityEditor.Element<Explosive>> getElements(Explosive entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.TNT)
                                .displayName(Component.text("Yield", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + String.format("%.2f", targetEntity.getYield()), DefinedTextColor.GRAY),
                                        Component.text("クリックで入力", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            float initYield = targetEntity.getYield();
                            Player player = (Player) event.getWhoClicked();
                            editor.onceDeferUnregisterOnClose();
                            Dialog dialog = Dialog.create(builder -> builder.empty()
                                    .base(DialogBase.builder(Component.text("爆発威力の規定"))
                                            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                            .body(Collections.singletonList(DialogBody.plainMessage(Component.text("爆発威力を入力 (0~100)"), 200)))
                                            .inputs(List.of(
                                                    DialogInput.numberRange("yield", Component.text("威力"), 0f, 100f).initial(initYield).step(0.5f).build()
                                            ))
                                            .canCloseWithEscape(true)
                                            .build())
                                    .type(DialogType.confirmation(
                                            ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                                Float newYield = response.getFloat("yield");
                                                if (newYield != null) {
                                                    targetEntity.setYield(newYield);
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
                        targetEntity -> new ItemStackBuilder(targetEntity.isIncendiary() ? Material.FLINT_AND_STEEL : Material.COAL)
                                .displayName(Component.text("Incendiary", DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("現在: " + (targetEntity.isIncendiary() ? "はい" : "いいえ"), DefinedTextColor.GRAY),
                                        Component.text("クリックで切り替え", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setIsIncendiary(!targetEntity.isIncendiary())
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
