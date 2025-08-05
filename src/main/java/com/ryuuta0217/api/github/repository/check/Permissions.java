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
