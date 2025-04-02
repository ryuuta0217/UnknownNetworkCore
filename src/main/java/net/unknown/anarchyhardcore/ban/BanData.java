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

package net.unknown.anarchyhardcore.ban;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

public abstract class BanData<T> implements Serializable {
    private final T target;
    private final long timestamp;
    private final Component reason;

    public BanData(T target, long timestamp, Component reason) {
        this.target = target;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    @Override
    public abstract String toString();

    public T target() {
        return this.target;
    }

    public abstract boolean validatePlayer(OfflinePlayer player);

    public abstract boolean validate(InetAddress address, UUID uniqueId, String name);

    public boolean validate(T other) {
        return Objects.equals(this.target, other);
    }

    public long timestamp() {
        return this.timestamp;
    }

    public Component reason() {
        return this.reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BanData<?>) obj;
        return Objects.equals(this.target, that.target) &&
                Objects.equals(this.reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.target, this.reason);
    }
}
