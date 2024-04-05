/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.api.github.repository.commit;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class Verification {
    private final GitHubAPI api;
    private final String reason;
    @Nullable
    private final String signature;
    @Nullable
    private final String payload;
    private final boolean verified;

    public Verification(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.reason = data.getString("reason");
        this.signature = data.has("signature") && !data.isNull("signature") ? data.getString("signature") : null;
        this.payload = data.has("payload") && !data.isNull("payload") ? data.getString("payload") : null;
        this.verified = data.getBoolean("verified");
    }

    public String getReason() {
        return this.reason;
    }

    @Nullable
    public String getSignature() {
        return this.signature;
    }

    @Nullable
    public String getPayload() {
        return this.payload;
    }

    public boolean isVerified() {
        return this.verified;
    }
}
