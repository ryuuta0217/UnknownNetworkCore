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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class ZombieVillagerHandler implements EntityEditorHandler<ZombieVillager> {

    private Material getProfessionMaterial(Villager.Profession profession) {
        if (profession == Villager.Profession.ARMORER) return Material.BLAST_FURNACE;
        if (profession == Villager.Profession.BUTCHER) return Material.SMOKER;
        if (profession == Villager.Profession.CARTOGRAPHER) return Material.CARTOGRAPHY_TABLE;
        if (profession == Villager.Profession.CLERIC) return Material.BREWING_STAND;
        if (profession == Villager.Profession.FARMER) return Material.COMPOSTER;
        if (profession == Villager.Profession.FISHERMAN) return Material.BARREL;
        if (profession == Villager.Profession.FLETCHER) return Material.FLETCHING_TABLE;
        if (profession == Villager.Profession.LEATHERWORKER) return Material.CAULDRON;
        if (profession == Villager.Profession.LIBRARIAN) return Material.LECTERN;
        if (profession == Villager.Profession.MASON) return Material.STONECUTTER;
        if (profession == Villager.Profession.NITWIT) return Material.GREEN_BED;
        if (profession == Villager.Profession.SHEPHERD) return Material.LOOM;
        if (profession == Villager.Profession.TOOLSMITH) return Material.SMITHING_TABLE;
        if (profession == Villager.Profession.WEAPONSMITH) return Material.GRINDSTONE;
        return Material.VILLAGER_SPAWN_EGG;
    }

    @Override
    public List<EntityEditor.Element<ZombieVillager>> getElements(ZombieVillager entity) {
        Villager.Type[] types = new Villager.Type[]{
                Villager.Type.DESERT, Villager.Type.JUNGLE, Villager.Type.PLAINS,
                Villager.Type.SAVANNA, Villager.Type.SNOW, Villager.Type.SWAMP, Villager.Type.TAIGA
        };
        Villager.Profession[] professions = new Villager.Profession[]{
                Villager.Profession.ARMORER, Villager.Profession.BUTCHER, Villager.Profession.CARTOGRAPHER,
                Villager.Profession.CLERIC, Villager.Profession.FARMER, Villager.Profession.FISHERMAN,
                Villager.Profession.FLETCHER, Villager.Profession.LEATHERWORKER, Villager.Profession.LIBRARIAN,
                Villager.Profession.MASON, Villager.Profession.NITWIT, Villager.Profession.NONE,
                Villager.Profession.SHEPHERD, Villager.Profession.TOOLSMITH, Villager.Profession.WEAPONSMITH
        };

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            Villager.Type current = targetEntity.getVillagerType();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Villager.Type t : types) {
                                lore.add(Component.text((t == current ? "▶" : "■"), t == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.text(t.getKey().value())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(Material.EMERALD)
                                    .displayName(Component.text("地域: " + current.getKey().value(), DefinedTextColor.GREEN))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Villager.Type current = targetEntity.getVillagerType();
                            int index = -1;
                            for (int i = 0; i < types.length; i++) {
                                if (types[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setVillagerType(types[(index + 1) % types.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setVillagerType(types[(index - 1 + types.length) % types.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> {
                            Villager.Profession current = targetEntity.getVillagerProfession();
                            java.util.List<Component> lore = new java.util.ArrayList<>();
                            for (Villager.Profession p : professions) {
                                lore.add(Component.text((p == current ? "▶" : "■"), p == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(Component.translatable("entity.minecraft.villager." + p.getKey().value())));
                            }
                            lore.add(Component.empty());
                            lore.add(Component.text("左クリック: 前 | 右クリック: 次", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(getProfessionMaterial(current))
                                    .displayName(Component.text("職業: ", DefinedTextColor.GREEN).append(Component.translatable("entity.minecraft.villager." + current.getKey().value(), DefinedTextColor.GREEN)))
                                    .lore(lore.toArray(Component[]::new))
                                    .build();
                        },
                        (editor, targetEntity, event) -> {
                            Villager.Profession current = targetEntity.getVillagerProfession();
                            int index = -1;
                            for (int i = 0; i < professions.length; i++) {
                                if (professions[i] == current) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                if (event.isRightClick()) {
                                    targetEntity.setVillagerProfession(professions[(index + 1) % professions.length]);
                                } else if (event.isLeftClick()) {
                                    targetEntity.setVillagerProfession(professions[(index - 1 + professions.length) % professions.length]);
                                }
                            }
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.GOLDEN_APPLE)
                                .displayName(Component.text("Cure", DefinedTextColor.GOLD))
                                .lore(Component.text("クリックで村人に治療", DefinedTextColor.YELLOW))
                                .build(),
                        (editor, targetEntity, event) -> {
                            targetEntity.setConversionTime(1);
                            Player player = (Player) event.getWhoClicked();
                            Bukkit.getScheduler().runTaskLater(net.unknown.UnknownNetworkCorePlugin.getInstance(), () -> {
                                java.util.Collection<Entity> entities = targetEntity.getLocation().getWorld().getNearbyEntities(targetEntity.getLocation(), 0.5, 0.5, 0.5, e -> e instanceof Villager);
                                if (!entities.isEmpty()) {
                                    new EntityEditor<>(player, (Villager) entities.iterator().next()).open(player);
                                }
                            }, 5L);
                        }
                ),
                EntityEditor.Element.of(
                        targetEntity -> new ItemStackBuilder(Material.WATER_BUCKET)
                                .displayName(Component.text("村人に戻るまでの時間: " + (targetEntity.isConverting() ? targetEntity.getConversionTime() + "ﾃｨｯｸ" : "非変換中"), DefinedTextColor.GREEN))
                                .lore(
                                        Component.text("左クリック: -100 | 右クリック: +100", DefinedTextColor.YELLOW),
                                        Component.text("中クリック: 直接入力", DefinedTextColor.YELLOW)
                                )
                                .build(),
                        (editor, targetEntity, event) -> {
                            if (event.getClick().isLeftClick()) {
                                int current = targetEntity.isConverting() ? targetEntity.getConversionTime() : 0;
                                if (current > 0) targetEntity.setConversionTime(Math.max(0, current - 100));
                            } else if (event.getClick().isRightClick()) {
                                int current = targetEntity.isConverting() ? targetEntity.getConversionTime() : 0;
                                targetEntity.setConversionTime(Math.min(100000, current + 100));
                            } else if (event.getClick() == ClickType.MIDDLE) {
                                Player player = (Player) event.getWhoClicked();
                                editor.onceDeferUnregisterOnClose();
                                new net.unknown.core.gui.SignGui()
                                        .withTarget(player)
                                        .withLines(Component.empty(), Component.text("^^^"), Component.text("変換時間を入力"), Component.empty())
                                        .onComplete(lines -> {
                                            try {
                                                int val = Integer.parseInt(((net.kyori.adventure.text.TextComponent) lines.get(0)).content());
                                                targetEntity.setConversionTime(Math.max(0, val));
                                            } catch (Exception ignored) {
                                            }
                                            editor.open(player);
                                        })
                                        .open();
                            }
                        }
                ),
                EntityEditor.Element.lineBreak()
        );
    }
}
