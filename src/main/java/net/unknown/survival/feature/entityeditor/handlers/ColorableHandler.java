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
import net.kyori.adventure.text.format.TextColor;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.survival.feature.entityeditor.EntityEditor;
import net.unknown.survival.feature.entityeditor.EntityEditorHandler;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.material.Colorable;

import java.util.ArrayList;
import java.util.List;

public class ColorableHandler implements EntityEditorHandler<Colorable> {

    public static Material woolMaterial(DyeColor c) {
        return switch (c) {
            case WHITE -> Material.WHITE_WOOL;
            case ORANGE -> Material.ORANGE_WOOL;
            case MAGENTA -> Material.MAGENTA_WOOL;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_WOOL;
            case YELLOW -> Material.YELLOW_WOOL;
            case LIME -> Material.LIME_WOOL;
            case PINK -> Material.PINK_WOOL;
            case GRAY -> Material.GRAY_WOOL;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_WOOL;
            case CYAN -> Material.CYAN_WOOL;
            case PURPLE -> Material.PURPLE_WOOL;
            case BLUE -> Material.BLUE_WOOL;
            case BROWN -> Material.BROWN_WOOL;
            case GREEN -> Material.GREEN_WOOL;
            case RED -> Material.RED_WOOL;
            case BLACK -> Material.BLACK_WOOL;
        };
    }

    public static Material dyeMaterial(DyeColor c) {
        return switch (c) {
            case WHITE -> Material.WHITE_DYE;
            case ORANGE -> Material.ORANGE_DYE;
            case MAGENTA -> Material.MAGENTA_DYE;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_DYE;
            case YELLOW -> Material.YELLOW_DYE;
            case LIME -> Material.LIME_DYE;
            case PINK -> Material.PINK_DYE;
            case GRAY -> Material.GRAY_DYE;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_DYE;
            case CYAN -> Material.CYAN_DYE;
            case PURPLE -> Material.PURPLE_DYE;
            case BLUE -> Material.BLUE_DYE;
            case BROWN -> Material.BROWN_DYE;
            case GREEN -> Material.GREEN_DYE;
            case RED -> Material.RED_DYE;
            case BLACK -> Material.BLACK_DYE;
        };
    }

    public static Component colorName(DyeColor c) {
        return Component.translatable("color.minecraft." + c.name().toLowerCase(), textColor(c));
    }

    public static TextColor textColor(DyeColor c) {
        return TextColor.color(c.getColor().asRGB());
    }

    @Override
    public List<EntityEditor.Element<Colorable>> getElements(Colorable entity) {
        DyeColor[] all = DyeColor.values();

        return List.of(
                EntityEditor.Element.of(
                        targetEntity -> {
                            DyeColor current = targetEntity.getColor() != null ? targetEntity.getColor() : DyeColor.WHITE;
                            List<Component> colorLore = new ArrayList<>();
                            for (DyeColor c : all) {
                                colorLore.add(Component.text((c == current ? "▶" : "■"), c == current ? DefinedTextColor.GREEN : DefinedTextColor.GRAY).appendSpace().append(colorName(c)));
                            }
                            colorLore.add(Component.empty());
                            colorLore.add(Component.text("左クリック: 前の色 | 右クリック: 次の色", DefinedTextColor.YELLOW));

                            return new ItemStackBuilder(woolMaterial(current))
                                    .displayName(Component.text("Color:", textColor(current)).appendSpace().append(colorName(current)))
                                    .lore(colorLore.toArray(Component[]::new)).build();
                        },
                        (editor, targetEntity, event) -> {
                            DyeColor c = targetEntity.getColor() != null ? targetEntity.getColor() : DyeColor.WHITE;
                            if (event.isRightClick()) {
                                targetEntity.setColor(all[(c.ordinal() + 1) % all.length]);
                            } else if (event.isLeftClick()) {
                                targetEntity.setColor(all[(c.ordinal() - 1 + all.length) % all.length]);
                            }
                        })
        );
    }
}
