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

package net.unknown.anarchyhardcore.ban;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.OfflinePlayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class IPBanData extends BanData<InetAddress> {
    public static final String IDENTIFIER = "IPBan";

    public IPBanData(InetAddress addr, long timestamp, Component reason) {
        super(addr, timestamp, reason);
    }

    @Override
    public String toString() {
        return IDENTIFIER + "|" + this.target().getHostAddress() + "|" + this.timestamp() + "|" + GsonComponentSerializer.gson().serialize(this.reason());
    }

    @Override
    public boolean validatePlayer(OfflinePlayer player) {
        return player.isOnline() && this.validate(player.getPlayer().getAddress().getAddress());
    }

    @Override
    public boolean validate(InetAddress address, UUID uniqueId, String name) {
        return this.validate(address);
    }

    public static BanData<InetAddress> fromString(String str) {
        try {
            String[] strs = str.split("\\|", 4);
            if (strs[0].equals(IDENTIFIER) && strs.length == 4) {
                String ipStr = strs[1];
                InetAddress ip = InetAddress.getByName(ipStr);

                String timestampStr = strs[2];
                long timestamp = Long.parseLong(timestampStr);

                String reasonStr = strs[3];
                Component reason = GsonComponentSerializer.gson().deserialize(reasonStr);

                return new IPBanData(ip, timestamp, reason);
            }
        } catch (UnknownHostException ignored) {
        }
        return null;
    }
}
