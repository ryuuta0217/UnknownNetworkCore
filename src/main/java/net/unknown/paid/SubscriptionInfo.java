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

package net.unknown.paid;

import org.json.JSONObject;

public class SubscriptionInfo {
    public static final SubscriptionInfo NO_SUBSCRIPTION = new SubscriptionInfo(null, -1, -1);

    private final SubscriptionType type;
    private final long since;
    private final long until;

    public static SubscriptionInfo read(JSONObject json) {
        if (json.has("type") && json.has("since") && json.has("until")) {
            SubscriptionType type = json.getEnum(SubscriptionType.class, "type");
            long since = json.getLong("since");
            long until = json.getLong("until");
            return new SubscriptionInfo(type, since, until);
        } else {
            return NO_SUBSCRIPTION;
        }
    }

    public SubscriptionInfo(SubscriptionType type, long since, long until) {
        this.type = type;
        this.since = since;
        this.until = until;
    }

    public SubscriptionType getType() {
        return this.type;
    }

    public long getSince() {
        return this.since;
    }

    public long getUntil() {
        return this.until;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        if (this.type != null) {
            json.put("type", this.type.name());
            json.put("since", this.since);
            json.put("until", this.until);
        }
        return json;
    }
}
