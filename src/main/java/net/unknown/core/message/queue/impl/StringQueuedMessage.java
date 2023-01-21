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

package net.unknown.core.message.queue.impl;

import net.kyori.adventure.text.Component;
import net.unknown.core.message.queue.MessageType;
import net.unknown.core.message.queue.interfaces.QueuedMessage;
import org.bukkit.configuration.ConfigurationSection;

public class StringQueuedMessage implements QueuedMessage {
    private String message;

    public StringQueuedMessage(String message) {
        this.message = message;
    }

    @Override
    public long getQueuedTime() {
        return 0;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.STRING;
    }

    @Override
    public Component getMessage() {
        return Component.text(this.message);
    }

    @Override
    public boolean save(ConfigurationSection section) {
        try {
            section.set("type", "string");
            section.set("value", this.message);
            return section.isSet("type") && section.isSet("value");
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
