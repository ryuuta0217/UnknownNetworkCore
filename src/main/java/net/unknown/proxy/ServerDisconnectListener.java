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

package net.unknown.proxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerDisconnectListener {
    private final Map<UUID, AtomicInteger> disconnectCounter = new HashMap<>();

    @Subscribe
    public void onPlayerDisconnectedFromServer(KickedFromServerEvent event) {
        if (!event.getServer().getServerInfo().getName().equals("lobby")) {
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(UnknownNetworkProxyCore.getLobbyServer(), Component.text(event.getServer().getServerInfo().getName() + " から切断されました: ", DefinedTextColor.RED).append(event.getServerKickReason().orElse(Component.text("(理由は不明)")))));
            if (this.disconnectCounter.computeIfAbsent(event.getPlayer().getUniqueId(), uuid -> new AtomicInteger(0)).incrementAndGet() == 3) {
                event.getPlayer().sendMessage(Component.text("何度もサーバーから切断されているようです。一度切断していただき、再度サーバーに参加することで改善する場合があります。", DefinedTextColor.YELLOW));
            }

            UnknownNetworkProxyCore.getInstance().getProxy().getScheduler().buildTask(UnknownNetworkProxyCore.getInstance(), () -> {
                        if (this.disconnectCounter.containsKey(event.getPlayer().getUniqueId())) {
                            AtomicInteger counter = this.disconnectCounter.get(event.getPlayer().getUniqueId());
                            if (counter.decrementAndGet() == 0) {
                                this.disconnectCounter.remove(event.getPlayer().getUniqueId());
                            }
                        }
                    })
                    .delay(10, TimeUnit.SECONDS)
                    .schedule();
        }
    }
}
