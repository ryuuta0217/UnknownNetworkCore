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

package net.unknown.core.message.queue;

import net.minecraft.network.chat.Component;
import net.unknown.core.message.queue.interfaces.QueuedMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageQueue {
    private static final Map<UUID, Map<Long, QueuedMessage>> QUEUED_MESSAGES = new HashMap<>();

    public static void readQueuedMessages() {
        // TODO read from file
    }

    public static Map<Long, QueuedMessage> getQueuedMessages(UUID receiver) {
        return Collections.emptyMap();
    }

    public static synchronized void queue(UUID to, Component message) {
        if (Bukkit.getOfflinePlayer(to).isOnline()) {

        } else {
            synchronized (QUEUED_MESSAGES) {

            }
        }
    }

    public static synchronized void queue(UUID to, net.kyori.adventure.text.Component message) {
        if (Bukkit.getOfflinePlayer(to).isOnline()) {

        } else {
            synchronized (QUEUED_MESSAGES) {

            }
        }
    }

    public static synchronized void queue(UUID to, String message) {
        if (Bukkit.getOfflinePlayer(to).isOnline()) {

        } else {
            synchronized (QUEUED_MESSAGES) {

            }
        }
    }
}
