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

package com.ryuuta0217.api.github.repository.branch;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

public class Protection {
    private final GitHubAPI api;
    private final Branch branch;
    private final RequiredStatusChecks requiredStatusChecks;
    private final boolean enabled;

    public Protection(GitHubAPI api, Branch branch, JSONObject data) {
        this.api = api;
        this.branch = branch;
        this.requiredStatusChecks = new RequiredStatusChecks(api, this, data.getJSONObject("required_status_checks"));
        this.enabled = data.getBoolean("enabled");
    }

    public Branch getBranch() {
        return this.branch;
    }

    public RequiredStatusChecks getRequiredStatusChecks() {
        return this.requiredStatusChecks;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public static class RequiredStatusChecks {
        private final GitHubAPI api;
        private final Protection protection;
        private final String enforcementLevel;
        private final Object[] checks;
        private final Object[] contexts;

        public RequiredStatusChecks(GitHubAPI api, Protection protection, JSONObject data) {
            this.api = api;
            this.protection = protection;
            this.enforcementLevel = data.getString("enforcement_level");
            this.checks = data.getJSONArray("contexts").toList().toArray();
            this.contexts = data.getJSONArray("contexts").toList().toArray();
        }

        public Protection getProtection() {
            return this.protection;
        }

        public String getEnforcementLevel() {
            return this.enforcementLevel;
        }

        public Object[] getChecks() {
            return this.checks;
        }

        public Object[] getContexts() {
            return this.contexts;
        }
    }
}
