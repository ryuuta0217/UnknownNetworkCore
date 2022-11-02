/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.survival.enchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomEnchantUtil {
    private static final Map<String, Integer> ROMAN_TO_ARABIC = new HashMap<>() {{
        put("Ⅰ", 1);
        put("I", 1);
        put("Ⅱ", 2);
        put("II", 2);
        put("Ⅲ", 3);
        put("III", 3);
        put("Ⅳ", 4);
        put("IV", 4);
        put("Ⅴ", 5);
        put("V", 5);
        put("Ⅵ", 6);
        put("VI", 6);
        put("Ⅶ", 7);
        put("VII", 7);
        put("Ⅷ", 8);
        put("VIII", 8);
        put("Ⅸ", 9);
        put("IX", 9);
        put("Ⅹ", 10);
        put("X", 10);
    }};

    public static boolean isRoman(String input) {
        return ROMAN_TO_ARABIC.containsKey(input);
    }

    public static int convertRomanToArabic(String input) {
        return isRoman(input) ? ROMAN_TO_ARABIC.get(input) : 1;
    }

    public static int getEnchantmentLevel(String loreLine) {
        String[] split = loreLine.split(" ", 2);
        if (split.length == 1) return 1;

        if (isRoman(split[1])) {
            return convertRomanToArabic(split[1]);
        } else if (split[1].matches("\\d+")) {
            try {
                return Integer.parseInt(split[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        return -1;
    }

    public static String getEnchantmentLine(String loreLine, ItemStack itemStack) {
        List<Component> lore = itemStack.lore();
        if (lore != null) {
            return lore.stream()
                    .map(LegacyComponentSerializer.legacySection()::serialize)
                    .filter(line -> line.startsWith(loreLine))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    public static boolean hasEnchantment(String loreLine, ItemStack selectedItem) {
        if (selectedItem.lore() == null) return false;
        List<String> lore = selectedItem.lore().stream().map(LegacyComponentSerializer.legacySection()::serialize).toList();
        return lore.stream().anyMatch(s -> s.startsWith(loreLine));
    }
}
