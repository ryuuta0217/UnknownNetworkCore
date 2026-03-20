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

package net.unknown.survival.feature.sidebar;

import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.unknown.core.util.NewMessageUtil;

import javax.annotation.Nullable;
import java.util.Objects;

public final class InternalSidebarLine {
    private final String identifier;
    private final Component line;
    @Nullable
    private final NumberFormat format;
    private final int score;

    private InternalSidebarLine(String identifier, Component line, @Nullable NumberFormat format, int score) {
        this.identifier = identifier;
        this.line = line;
        this.format = format;
        this.score = score;
    }

    public static InternalSidebarLine of(String identifier, Component line) {
        return new InternalSidebarLine(identifier, line, null, -1);
    }

    public static InternalSidebarLine of(String identifier, Component line, NumberFormat format) {
        return new InternalSidebarLine(identifier, line, format, -1);
    }

    public static InternalSidebarLine of(String identifier, Component line, @Nullable NumberFormat format, int score) {
        return new InternalSidebarLine(identifier, line, format, score);
    }

    public static InternalSidebarLine ofScore(InternalSidebarLine source, int score) {
        return new InternalSidebarLine(source.identifier, source.line, source.format, score);
    }

    public String identifier() {
        return identifier;
    }

    public Component line() {
        return line;
    }

    public net.minecraft.network.chat.Component minecraftLine() {
        return NewMessageUtil.convertAdventure2Minecraft(this.line);
    }

    @Nullable
    public NumberFormat format() {
        return format;
    }

    public int score() {
        return score;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (InternalSidebarLine) obj;
        return Objects.equals(this.identifier, that.identifier) &&
                Objects.equals(this.line, that.line) &&
                Objects.equals(this.format, that.format) &&
                this.score == that.score;
    }

    @Override
    public InternalSidebarLine clone() throws CloneNotSupportedException {
        return new InternalSidebarLine(this.identifier, this.line, this.format, this.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, line, format, score);
    }

    @Override
    public String toString() {
        return "InternalSidebarLine[" +
                "identifier=" + identifier + ", " +
                "line=" + line + ", " +
                "format=" + format + ", " +
                "score=" + score + ']';
    }
}
