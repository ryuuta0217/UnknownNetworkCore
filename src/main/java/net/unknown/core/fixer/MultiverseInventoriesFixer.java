/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
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

package net.unknown.core.fixer;

import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.mvplugins.multiverse.inventories.util.PlayerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiverseInventoriesFixer {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/MultiverseInventoriesFixer");

    public static void fixAll() {
        fixEnderChestMaxSize();
    }

    public static boolean fixEnderChestMaxSize() {
        if (Bukkit.getPluginManager().getPlugin("Multiverse-Inventories") != null) {
            try {
                ReflectionUtil.setStaticFinalObject(org.mvplugins.multiverse.inventories.util.PlayerStats.class.getDeclaredField("ENDER_CHEST_SIZE"), 9 * 6);
                boolean fixResult = org.mvplugins.multiverse.inventories.util.PlayerStats.ENDER_CHEST_SIZE == 9 * 6;
                if (fixResult) LOGGER.info("Fixed Multiverse-Inventories EnderChest size to " + (9 * 6));
                else LOGGER.warn("Failed to fix Multiverse-Inventories EnderChest size!");
                return fixResult;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
