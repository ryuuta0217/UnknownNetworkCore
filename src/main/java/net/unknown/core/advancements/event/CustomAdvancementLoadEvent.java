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

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.ResourceLocation;
import net.unknown.core.builder.advancement.DisplayInfoBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CustomAdvancementLoadEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ResourceLocation id;
    private Advancement advancement;
    private AdvancementHolder holder;
    private boolean cancelled;

    public CustomAdvancementLoadEvent(final ResourceLocation id, Advancement advancement) {
        super(!Bukkit.isPrimaryThread());
        this.id = id;
        this.advancement = advancement;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public Advancement getAdvancement() {
        return this.advancement;
    }

    public Advancement.Builder getAdvancementBuilder() {
        Advancement.Builder builder = Advancement.Builder.advancement()
                .parent(this.advancement.parent().get())
                .display(this.advancement.display().get())
                .rewards(this.advancement.rewards())
                .requirements(this.advancement.requirements());

        this.advancement.criteria().forEach(builder::addCriterion);
        return builder;
    }

    public DisplayInfoBuilder getDisplayInfoBuilder() {
        if (this.advancement.display().isEmpty()) return new DisplayInfoBuilder();
        DisplayInfo displayInfo = this.advancement.display().get();
        DisplayInfoBuilder builder = new DisplayInfoBuilder();
        builder.title(displayInfo.getTitle());
        builder.description(displayInfo.getDescription());
        builder.icon(displayInfo.getIcon());
        builder.background(displayInfo.getBackground().isEmpty() ? null : displayInfo.getBackground().get().id());
        builder.type(displayInfo.getType());
        builder.showToast(displayInfo.shouldShowToast());
        builder.announceChat(displayInfo.shouldAnnounceChat());
        builder.hidden(displayInfo.isHidden());
        return builder;
    }

    public void setAdvancement(Advancement advancement) {
        this.advancement = advancement;
        if (this.holder != null) this.holder = new AdvancementHolder(this.id, advancement);
    }

    public AdvancementHolder getHolder() {
        if (this.holder == null) this.holder = new AdvancementHolder(this.id, this.advancement);
        return this.holder;
    }

    public void setHolder(AdvancementHolder holder) {
        if (!Objects.equals(holder.id(), this.id)) throw new IllegalArgumentException("The id of holder must be the same as this event id.");
        this.holder = holder;
        this.advancement = holder.value();
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