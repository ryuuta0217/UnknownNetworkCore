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

package net.unknown.survival.item;

import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.managers.ListenerManager;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Items {
    private static final Logger LOGGER = Logger.getLogger(Items.class.getName());
    private static final Map<NamespacedKey, UnknownNetworkItem> REGISTRY = new HashMap<>();
    private static boolean FROZEN = false;

    public static MendingSupportStickItem MENDING_SUPPORT_STICK;
    public static UpgradeSnowGolemItem UPGRADE_SNOW_GOLEM_ITEM;

    public static void init() {
        if (FROZEN && !REGISTRY.isEmpty()) throw new IllegalStateException("Can't init items twice!");
        MENDING_SUPPORT_STICK = register(new MendingSupportStickItem());
        UPGRADE_SNOW_GOLEM_ITEM = register(new UpgradeSnowGolemItem());
        FROZEN = true;
    }

    private static <T extends UnknownNetworkItem> T register(NamespacedKey id, T item) {
        if (FROZEN) throw new IllegalStateException("Can't register item after init!");
        if (REGISTRY.containsKey(id)) {
            LOGGER.warning("Item " + id + " is already registered! Overwriting this.");
        }
        if (!id.equals(item.getId())) throw new IllegalArgumentException("Item id mismatch (expected: " + id + ", actual: " + item.getId() + ")");
        REGISTRY.put(id, item);
        if (item instanceof Listener itemListener) {
            ListenerManager.registerListener(itemListener);
        }
        return item;
    }

    private static <T extends UnknownNetworkItem> T register(T item) {
        return register(item.getId(), item);
    }
}
