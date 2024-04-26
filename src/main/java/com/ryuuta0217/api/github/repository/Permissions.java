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

package com.ryuuta0217.api.github.repository;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

public class Permissions {
    private final GitHubAPI api;
    private final boolean pull;
    private final boolean maintain;
    private final boolean admin;
    private final boolean triage;
    private final boolean push;

    public Permissions(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.pull = data.getBoolean("pull");
        this.maintain = data.getBoolean("maintain");
        this.admin = data.getBoolean("admin");
        this.triage = data.getBoolean("triage");
        this.push = data.getBoolean("push");
    }

    public boolean canPull() {
        return this.pull;
    }

    public boolean canMaintain() {
        return this.maintain;
    }

    public boolean isAdmin() {
        return this.admin;
    }

    public boolean canTriage() {
        return this.triage;
    }

    public boolean canPush() {
        return this.push;
    }
}
