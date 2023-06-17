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
