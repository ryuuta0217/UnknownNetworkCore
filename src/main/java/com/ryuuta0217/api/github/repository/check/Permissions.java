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

package com.ryuuta0217.api.github.repository.check;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Collectors;

public class Permissions {
    private final GitHubAPI api;
    private final App app;

    private final String issues;
    private final String checks;
    private final String metadata;
    private final String contents;
    private final String deployments;
    private final Map<String, String> additionalProperties;

    public Permissions(GitHubAPI api, App app, JSONObject data) {
        this.api = api;
        this.app = app;

        this.issues = data.getString("issues");
        this.checks = data.getString("checks");
        this.metadata = data.getString("metadata");
        this.contents = data.getString("contents");
        this.deployments = data.getString("deployments");
        this.additionalProperties = data.toMap().entrySet()
                .stream()
                .filter(entry -> !entry.getKey().matches("issues|checks|metadata|contents|deployments"))
                .filter(entry -> entry.getValue() instanceof String)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public App getApp() {
        return this.app;
    }

    public String getIssuesPermission() {
        return this.issues;
    }

    public String getChecksPermission() {
        return this.checks;
    }

    public String getMetadataPermission() {
        return this.metadata;
    }

    public String getContentsPermission() {
        return this.contents;
    }

    public String getDeploymentsPermission() {
        return this.deployments;
    }

    @Nullable
    public String getAdditionalPermission(String key) {
        return this.additionalProperties.getOrDefault(key, null);
    }
}
