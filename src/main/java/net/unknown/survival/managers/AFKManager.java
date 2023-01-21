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

package net.unknown.survival.managers;

import net.minecraft.Util;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.unknown.core.events.PacketReceivedEvent;
import net.unknown.core.events.interfaces.PacketListener;
import net.unknown.core.managers.PacketManager;
import net.unknown.survival.data.PlayerData;

public class AFKManager {
    private static boolean INITIALIZED = false;
    private static AFKManager INSTANCE = null;

    public static void initialize() {
        if (!INITIALIZED) {
            INSTANCE = new AFKManager();
            INITIALIZED = true;
        } else {
            throw new IllegalStateException("AFKManager is already initialized.");
        }
    }

    private final MoveListener moveListener = new MoveListener();

    public AFKManager() {
        PacketManager.getInstance().registerListener(ServerboundMovePlayerPacket.class, this.moveListener);
    }

    public static class MoveListener extends PacketListener<ServerboundMovePlayerPacket> {
        public MoveListener() {
            super(false);
        }

        @Override
        public void onPacketReceived(PacketReceivedEvent<ServerboundMovePlayerPacket> event) {
            PlayerData.of(event.getPlayer()).getSessionData().setLastActionTime(Util.getMillis());
        }
    }
}
