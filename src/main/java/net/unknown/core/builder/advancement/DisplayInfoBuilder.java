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

package net.unknown.core.builder.advancement;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Objects;

public class DisplayInfoBuilder {
    private Component title;
    private Component description;
    private ItemStack icon;
    @Nullable
    private ResourceLocation background;
    private FrameType frame = FrameType.TASK;
    private boolean showToast = true;
    private boolean announceChat = true;
    private boolean hidden = false;
    private float x = 0;
    private float y = 0;

    public DisplayInfoBuilder() {}

    public DisplayInfoBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public DisplayInfoBuilder description(Component description) {
        this.description = description;
        return this;
    }

    public DisplayInfoBuilder icon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public DisplayInfoBuilder background(ResourceLocation background) {
        this.background = background;
        return this;
    }

    public DisplayInfoBuilder frame(FrameType frame) {
        this.frame = frame;
        return this;
    }

    public DisplayInfoBuilder showToast(boolean showToast) {
        this.showToast = showToast;
        return this;
    }

    public DisplayInfoBuilder announceChat(boolean announceChat) {
        this.announceChat = announceChat;
        return this;
    }

    public DisplayInfoBuilder hidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public DisplayInfoBuilder location(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public DisplayInfoBuilder x(float x) {
        this.x = x;
        return this;
    }

    public DisplayInfoBuilder y(float y) {
        this.y = y;
        return this;
    }

    public DisplayInfo build() {
        Objects.requireNonNull(this.title);
        Objects.requireNonNull(this.description);
        Objects.requireNonNull(this.icon);
        Objects.requireNonNull(this.frame);
        DisplayInfo displayInfo = new DisplayInfo(icon, title, description, background, frame, showToast, announceChat, hidden);
        displayInfo.setLocation(this.x, this.y);
        return displayInfo;
    }
}
