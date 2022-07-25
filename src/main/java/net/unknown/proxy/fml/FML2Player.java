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

package net.unknown.proxy.fml;

import com.ryuuta0217.packets.C2SModListReply;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FML2Player implements ModdedPlayer {
    private final Set<String> mods;
    private final Map<String, String> channels;
    private final Map<String, String> registries;

    public FML2Player(Set<String> mods, Map<String, String> channels, Map<String, String> registries) {
        this.mods = mods;
        this.channels = channels;
        this.registries = registries;
    }

    public Set<String> getMods() {
        return this.mods;
    }

    public Map<String, String> getChannels() {
        return this.channels;
    }

    public Map<String, String> getRegistries() {
        return this.registries;
    }

    @Override
    public void getData(ByteBuf buf, UUID uniqueId) {
        DefinedPacket.writeUUID(uniqueId, buf);
        new C2SModListReply(this.mods, this.channels, this.registries).encode(buf);
    }

    @Override
    public int getFMLVersion() {
        return 2;
    }
}
