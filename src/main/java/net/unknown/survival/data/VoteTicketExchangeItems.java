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

package net.unknown.survival.data;

import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.model.VoteTicketExchangeItem;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class VoteTicketExchangeItems extends ConfigurationBase {
    private static final VoteTicketExchangeItems INSTANCE = new VoteTicketExchangeItems();

    private Map<String, VoteTicketExchangeItem> exchangeItems;

    private VoteTicketExchangeItems() {
        super("vote-ticket-exchange-items.yml", false, "UNC/VoteTicketExchangeItems");
    }

    @Override
    public void onLoad() {
        this.exchangeItems = new HashMap<>();
        if (this.getConfig().isSet("exchange-items")) {
            ConfigurationSection exchangeItemsSection = this.getConfig().getConfigurationSection("exchange-items");
            exchangeItemsSection.getKeys(false).forEach(id -> {
                ConfigurationSection section = exchangeItemsSection.getConfigurationSection(id);
                VoteTicketExchangeItem item = VoteTicketExchangeItem.load(section);
                if (item != null) this.exchangeItems.put(id, item);
                else this.getLogger().warning("Failed to load vote ticket exchange item for ID " + id);
            });
        }
    }

    public static VoteTicketExchangeItems getInstance() {
        return VoteTicketExchangeItems.INSTANCE;
    }

    public static Map<String, VoteTicketExchangeItem> getExchangeItems() {
        return getInstance().exchangeItems;
    }

    public static boolean has(String id) {
        return getInstance().exchangeItems.containsKey(id);
    }

    public static void add(String id, VoteTicketExchangeItem item) {
        if (id.contains(".")) throw new IllegalArgumentException("ID cannot contain a '.'!");
        if (has(id)) throw new IllegalArgumentException("ID " + id + " is already taken!");
        getInstance().exchangeItems.put(id, item);
        RunnableManager.runAsync(getInstance()::save);
    }

    public static VoteTicketExchangeItem get(String id) {
        return getExchangeItems().getOrDefault(id, null);
    }

    public static VoteTicketExchangeItem remove(String id) {
        VoteTicketExchangeItem removed = getInstance().exchangeItems.remove(id);
        if (removed != null) RunnableManager.runAsync(getInstance()::save);
        return removed;
    }

    @Override
    public synchronized void save() {
        this.getConfig().set("exchange-items", null);
        ConfigurationSection exchangeItemsSection = this.getConfig().createSection("exchange-items");
        this.exchangeItems.forEach((id, item) -> {
            ConfigurationSection section = exchangeItemsSection.createSection(id);
            item.save(section);
        });
        super.save();
    }
}
