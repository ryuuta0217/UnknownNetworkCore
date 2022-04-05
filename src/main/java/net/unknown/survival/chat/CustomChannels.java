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

package net.unknown.survival.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.unknown.UnknownNetworkCore;
import net.unknown.survival.chat.channels.CustomChannel;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class CustomChannels {
    private static final Logger LOGGER = Logger.getLogger("CCLoader");

    private static final Map<String, CustomChannel> CHANNELS = new HashMap<>();

    private static final File CONFIG_FILE = new File(UnknownNetworkCore.getInstance().getDataFolder(), "custom_channels.yml");
    private static YamlConfiguration CONFIG;

    public static void load() {
        try {
            if (CONFIG_FILE.exists() || CONFIG_FILE.createNewFile()) {
                CONFIG = YamlConfiguration.loadConfiguration(CONFIG_FILE);

                CONFIG.getConfigurationSection("channels").getKeys(false).forEach(channelName -> {
                    UUID owner = UUID.fromString(CONFIG.getString(channelName + ".owner"));
                    Component displayName = GsonComponentSerializer.gson().deserialize(CONFIG.getString(channelName + ".display_name"));
                    List<UUID> players = CONFIG.getStringList(channelName + ".players").stream().map(rawUniqueId -> UUID.fromString(rawUniqueId)).toList();
                    CHANNELS.put(channelName, new CustomChannel(channelName, displayName, owner, players));
                });
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to load channels - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public static Set<String> getChannelNames() {
        return CHANNELS.keySet();
    }

    public static void save() {
        CONFIG.set("channels", null);
        CHANNELS.forEach((channelName, channel) -> {
            CONFIG.set("channels." + channelName + ".display_name", GsonComponentSerializer.gson().serialize(channel.getDisplayName()));
            CONFIG.set("channels." + channelName + ".owner", channel.getOwner());
            CONFIG.set("channels." + channelName + ".players", channel.getPlayers().stream().filter(p -> !p.equals(channel.getOwner())).map(UUID::toString).toList());
        });
        try {
            CONFIG.save(CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.warning("Failed to save file - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
