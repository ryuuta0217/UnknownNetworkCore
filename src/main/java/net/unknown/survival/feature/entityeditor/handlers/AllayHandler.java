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
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AllayHandler implements EntityEditorHandler<Allay> {

    @Override
    public List<EntityEditor.Element<Allay>> getElements(Allay entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.canDuplicate() ? Material.AMETHYST_SHARD : Material.FLINT)
                                .displayName(Component.text("CanDuplicate: " + (targetEntity.canDuplicate() ? "はい" : "いいえ"), targetEntity.canDuplicate() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setCanDuplicate(!targetEntity.canDuplicate())
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.CLOCK)
                                .displayName(Component.text("DuplicationCooldown: " + targetEntity.getDuplicationCooldown() + " ticks", DefinedTextColor.GREEN))
                                .lore(Component.text("クリックで入力", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            long initCooldown = targetEntity.getDuplicationCooldown();
                            Player player = (Player) event.getWhoClicked();
                            editor.onceDeferUnregisterOnClose();
                            Dialog dialog = Dialog.create(builder -> builder.empty()
                                    .base(DialogBase.builder(Component.text("増殖クールダウンの設定"))
                                            .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                            .body(Collections.singletonList(DialogBody.plainMessage(Component.text("増殖クールダウンを入力"), 200)))
                                            .inputs(List.of(
                                                    DialogInput.numberRange("cooldown", Component.text("Tick数"), -1f, 1000000f).initial((float) initCooldown).step(20f).build()
                                            ))
                                            .canCloseWithEscape(true)
                                            .build())
                                    .type(DialogType.confirmation(
                                            ActionButton.create(Component.text("保存", DefinedTextColor.GREEN), Component.empty(), 128, DialogAction.customClick((response, audience) -> {
                                                Float newCooldown = response.getFloat("cooldown");
                                                if (newCooldown != null) {
                                                    targetEntity.setDuplicationCooldown(newCooldown.longValue());
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
                        targetEntity -> new ItemStackBuilder(targetEntity.isDancing() ? Material.JUKEBOX : Material.NOTE_BLOCK)
                                .displayName(Component.text("Dancing: " + (targetEntity.isDancing() ? "はい" : "いいえ"), targetEntity.isDancing() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            if (targetEntity.isDancing()) {
                                targetEntity.stopDancing();
                            } else {
                                targetEntity.startDancing();
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.EGG)
                                .displayName(Component.text("Duplicate", DefinedTextColor.LIGHT_PURPLE))
                                .lore(Component.text("クリックで直ちに増殖させます", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            targetEntity.duplicateAllay();
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
