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

package net.unknown.core.message.queue.impl;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;
import net.unknown.core.message.queue.MessageType;
import net.unknown.core.message.queue.interfaces.QueuedMessage;
import org.bukkit.configuration.ConfigurationSection;

public class JSONQueuedMessage implements QueuedMessage {
    private long queuedTime = -1;
    private final MessageType messageType = MessageType.JSON;
    private final String json;

    protected JSONQueuedMessage(String json) {
        this.json = json;
    }

    public JSONQueuedMessage(Component minecraft) {
        this.json = Component.Serializer.toJson(minecraft);
    }

    public JSONQueuedMessage(net.kyori.adventure.text.Component adventure) {
        this.json = GsonComponentSerializer.gson().serialize(adventure);
    }

    @Override
    public MessageType getMessageType() {
        return this.messageType;
    }

    @Override
    public net.kyori.adventure.text.Component getMessage() {
        return GsonComponentSerializer.gson().deserialize(this.json);
    }

    @Override
    public long getQueuedTime() {
        return this.queuedTime;
    }

    @Override
    public boolean save(ConfigurationSection section) {
        try {
            section.set("type", "json");
            section.set("value", this.json);
            return section.isSet("type") && section.isSet("value");
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getJson() {
        return this.json;
    }

    private void setQueuedTime(long queuedTime) {
        this.queuedTime = queuedTime;
    }
}
