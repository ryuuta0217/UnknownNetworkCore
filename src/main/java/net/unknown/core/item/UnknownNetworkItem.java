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

import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.builder.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public abstract class UnknownNetworkItem {
    public static final NamespacedKey ID_CONTAINER_ID = new NamespacedKey(UnknownNetworkCorePlugin.getInstance(), "custom_item_id");
    public static final UnknownNetworkItem EMPTY = new UnknownNetworkItem(new NamespacedKey("core", "air")) {
        @Override
        public UnknownNetworkItemStack<? extends UnknownNetworkItem> createItemStack() {
            return new UnknownNetworkItemStack<>(new ItemStack(Material.AIR), this);
        }
    };

    private final NamespacedKey id;

    public UnknownNetworkItem(NamespacedKey id) {
        if (id == null) throw new IllegalArgumentException("Can't create null id Item!");
        this.id = id;
    }

    public NamespacedKey getId() {
        return this.id;
    }

    public abstract UnknownNetworkItemStack<? extends UnknownNetworkItem> createItemStack();

    protected ItemStackBuilder createItemStackBuilder(Material type) {
        return new ItemStackBuilder(type)
                .custom(is -> is.editMeta(meta -> meta.getPersistentDataContainer().set(ID_CONTAINER_ID, PersistentDataType.STRING, this.getId().asString())));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof String str) return this.equals(str);
        if (obj instanceof NamespacedKey key) return this.equals(key);
        if (obj instanceof UnknownNetworkItem item) return this.equals(item);
        if (obj instanceof ItemStack stack) return this.equals(stack);
        return super.equals(obj);
    }

    public boolean equals(String str) {
        return this.equals(NamespacedKey.fromString(str != null ? str : ""));
    }

    public boolean equals(NamespacedKey key) {
        return this.getId().equals(key);
    }

    public boolean equals(UnknownNetworkItem item) {
        return super.equals(item) || (item != null && this.equals(item.getId()));
    }

    public boolean equals(ItemStack stack) {
        if (stack != null && stack.getItemMeta() != null) {
            PersistentDataContainer dataContainer = stack.getItemMeta().getPersistentDataContainer();
            if (dataContainer.has(ID_CONTAINER_ID)) {
                return this.equals(NamespacedKey.fromString(dataContainer.getOrDefault(ID_CONTAINER_ID, PersistentDataType.STRING, "")));
            }
        }
        return false;
    }
}
