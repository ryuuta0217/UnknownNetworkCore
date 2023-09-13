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

package net.unknown.core.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class UnknownNetworkItemStack {
    private final ItemStack handle;
    private final UnknownNetworkItem item;

    public UnknownNetworkItemStack(ItemStack handle, UnknownNetworkItem item) {
        if (!item.equals(handle)) throw new IllegalArgumentException("Item mismatch (expected: " + item.getId() + ", actual: " + handle.getItemMeta().getPersistentDataContainer().getOrDefault(UnknownNetworkItem.ID_CONTAINER_ID, PersistentDataType.STRING, "unknown (vanilla?)") + ")");
        this.handle = handle;
        this.item = item;
    }

    public ItemStack getHandle() {
        return this.handle;
    }

    public UnknownNetworkItem getItem() {
        return this.item;
    }
}
