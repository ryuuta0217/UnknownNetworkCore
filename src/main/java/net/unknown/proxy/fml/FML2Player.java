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

package net.unknown.proxy.fml;

import com.ryuuta0217.packets.forge.v2.C2SModListReply;
import com.ryuuta0217.packets.forge.v2.S2CModList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.LoginPayloadRequest;
import net.md_5.bungee.protocol.packet.LoginPayloadResponse;
import net.unknown.proxy.ModdedInitialHandler;

import java.util.*;

public class FML2Player extends ModdedPlayer implements ModdedHandshakeProcessor {
    private ModdedInitialHandler handler;
    private Set<String> mods;
    private Map<String, String> channels;
    private Map<String, String> registries;

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
    public void onPluginMessageReceived(PluginMessageEvent event) {
        // unused
    }

    @Override
    public void onPreLogin(PreLoginEvent event) {
        this.handler = (ModdedInitialHandler) event.getConnection();

        ModdedInitialHandler handler = (ModdedInitialHandler) event.getConnection();
        String logPrefix = "[" + handler.getName() + "|" + handler.getSocketAddress() + "]";

        LOGGER.info(logPrefix + "  -> Connected as using FML2 protocol");
        LOGGER.info(logPrefix + "  -  Initializing FML2 Handshake (S2CModList)");

        LoginPayloadRequest loginPayloadRequest = new LoginPayloadRequest();
        S2CModList packet = new S2CModList(new HashSet<>() {{
            add("minecraft");
            add("forge");
        }}, new HashMap<>() {{
            put("forge:tier_sorting", "1.0");
        }}, new HashSet<>());
        ByteBuf buf = Unpooled.buffer();
        packet.encode(buf);
        loginPayloadRequest.setData(ByteBufUtil.getBytes(buf));
        loginPayloadRequest.setChannel("fml:handshake");

        int id = RANDOM.nextInt(Short.MAX_VALUE);
        if (ModdedInitialHandler.PENDING_FORGE_PLAYER_CONNECTIONS.containsKey(id)) {
            LOGGER.info(logPrefix + "  -  FML2 handshake Message ID " + id + " is duplicated, re-generating.");
            while (ModdedInitialHandler.PENDING_FORGE_PLAYER_CONNECTIONS.containsKey(id)) {
                id = RANDOM.nextInt(Integer.MAX_VALUE);
                LOGGER.info(logPrefix + "  -  FML2 handshake new Message ID is " + id);
            }
            LOGGER.info(logPrefix + "  -  FML2 handshake Message ID duplicate is fixed.");
        } else {
            LOGGER.info(logPrefix + "  -  FML2 handshake Message ID is " + id);
        }
        ModdedInitialHandler.PENDING_FORGE_PLAYER_CONNECTIONS.put(id, this);
        loginPayloadRequest.setId(id);
        event.getConnection().unsafe().sendPacket(loginPayloadRequest);
        LOGGER.info(logPrefix + " <-  LoginPayloadRequest (FML2 Handshake S2CModList) Sent");
    }

    @Override
    public void onLoginPayloadResponseReceived(LoginPayloadResponse response) {
        LOGGER.info("[" + this.handler.getName() + "|" + this.handler.getSocketAddress() + "]  -> FML2 handshake Message ID is validated with " + response.getId());
        ModdedInitialHandler.PENDING_FORGE_PLAYER_CONNECTIONS.remove(response.getId());

        ByteBuf buf = Unpooled.wrappedBuffer(response.getData());

        try {
            C2SModListReply handshake = C2SModListReply.decode(buf);
            this.mods = handshake.getMods();
            this.channels = handshake.getChannels();
            this.registries = handshake.getRegistries();

            ForgeListener.ESTABLISHING_MODDED_PLAYERS.remove(this.handler.getName());
            ModdedInitialHandler.FORGE_PLAYERS.put(this.handler.getName(), this);
            LOGGER.info("[" + this.handler.getName() + "|" + this.handler.getSocketAddress() + "] <-> Successfully FML2 handshake completed");
            LOGGER.info("[" + this.handler.getName() + "|" + this.handler.getSocketAddress() + "] <-> Connected as using mods: " + handshake.getMods());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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
