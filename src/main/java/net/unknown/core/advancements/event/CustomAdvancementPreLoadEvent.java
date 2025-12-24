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

package net.unknown.core.advancements.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;

public class CustomAdvancementPreLoadEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    @Nonnull private Identifier id;
    @Nonnull private String rawJson;
    @Nullable private JsonElement json;
    private boolean cancelled;

    public CustomAdvancementPreLoadEvent(@Nonnull Identifier id, @Nonnull String rawJson) {
        super(!Bukkit.isPrimaryThread());
        this.id = id;
        this.rawJson = rawJson;
    }

    public Identifier getId() {
        return this.id;
    }

    public void setId(@Nonnull Identifier id) {
        this.id = id;
    }

    public String getRawJson() {
        return this.rawJson;
    }

    public void setRawJson(@Nonnull String rawJson) {
        this.rawJson = rawJson;
        this.json = null;
    }

    @Nullable
    public JsonElement getJson() {
        return this.json;
    }

    @Nullable
    public JsonElement getJson(boolean parse) {
        if (parse) {
            this.json = JsonParser.parseString(this.rawJson);
        }
        return this.json;
    }

    public void setJson(@Nullable JsonElement json) {
        this.json = json;
        if (this.json != null) {
            this.rawJson = this.json.toString();
        }
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}