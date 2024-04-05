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
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import org.json.JSONObject;

public class SecurityAndAnalysis {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;

    private final boolean advancedSecurityEnabled;
    private final boolean secretScanningEnabled;
    private final boolean secretScanningPushProtectionEnabled;

    public SecurityAndAnalysis(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;

        this.advancedSecurityEnabled = data.has("advanced_security") && data.getJSONObject("advanced_security").has("status") && data.getJSONObject("advanced_security").getString("status").equals("enabled");
        this.secretScanningEnabled = data.has("secret_scanning") && data.getJSONObject("secret_scanning").has("status") && data.getJSONObject("secret_scanning").getString("status").equals("enabled");
        this.secretScanningPushProtectionEnabled = data.has("secret_scanning_push_protection") && data.getJSONObject("secret_scanning_push_protection").has("status") && data.getJSONObject("secret_scanning_push_protection").getString("status").equals("enabled");
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public boolean isAdvancedSecurityEnabled() {
        return this.advancedSecurityEnabled;
    }

    public boolean isSecretScanningEnabled() {
        return this.secretScanningEnabled;
    }

    public boolean isSecretScanningPushProtectionEnabled() {
        return this.secretScanningPushProtectionEnabled;
    }
}
