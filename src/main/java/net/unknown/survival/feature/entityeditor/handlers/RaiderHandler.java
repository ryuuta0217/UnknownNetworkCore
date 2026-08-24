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
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Raider;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.List;

public class RaiderHandler implements EntityEditorHandler<Raider> {

    @Override
    public List<EntityEditor.Element<Raider>> getElements(Raider entity) {
        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            ItemStackBuilder builder;
                            if (targetEntity.isPatrolLeader()) {
                                builder = new ItemStackBuilder(Material.WHITE_BANNER).custom(is -> is.editMeta(BannerMeta.class, meta -> {
                                    meta.addPattern(new Pattern(DyeColor.CYAN, PatternType.RHOMBUS));
                                    meta.addPattern(new Pattern(DyeColor.LIGHT_GRAY, PatternType.STRIPE_BOTTOM));
                                    meta.addPattern(new Pattern(DyeColor.GRAY, PatternType.STRIPE_CENTER));
                                    meta.addPattern(new Pattern(DyeColor.LIGHT_GRAY, PatternType.BORDER));
                                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
                                    meta.addPattern(new Pattern(DyeColor.LIGHT_GRAY, PatternType.HALF_HORIZONTAL));
                                    meta.addPattern(new Pattern(DyeColor.LIGHT_GRAY, PatternType.CIRCLE));
                                    meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.BORDER));
                                }));
                            } else {
                                builder = new ItemStackBuilder(Material.BLACK_BANNER);
                            }

                            return builder
                                    .displayName(Component.text("PatrolLeader: " + (targetEntity.isPatrolLeader() ? "はい" : "いいえ"), targetEntity.isPatrolLeader() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                    .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                    .build();
                        },
                        (editor, targetEntity, event) -> targetEntity.setPatrolLeader(!targetEntity.isPatrolLeader())
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.isCanJoinRaid() ? Material.CROSSBOW : Material.STICK)
                                .displayName(Component.text("CanJoinRaid: " + (targetEntity.isCanJoinRaid() ? "はい" : "いいえ"), targetEntity.isCanJoinRaid() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setCanJoinRaid(!targetEntity.isCanJoinRaid())
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(targetEntity.isCelebrating() ? Material.FIREWORK_ROCKET : Material.GUNPOWDER)
                                .displayName(Component.text("Celebrating: " + (targetEntity.isCelebrating() ? "はい" : "いいえ"), targetEntity.isCelebrating() ? DefinedTextColor.GREEN : DefinedTextColor.YELLOW))
                                .lore(Component.text("クリックで切り替え", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> targetEntity.setCelebrating(!targetEntity.isCelebrating())
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
