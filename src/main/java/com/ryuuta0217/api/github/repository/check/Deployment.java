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
import java.time.ZonedDateTime;

public class Deployment {
    private final GitHubAPI api;
    private final CheckRun checkRun;

    private final long id;
    private final String nodeId;
    private final String task;
    private final String environment;
    @Nullable
    private final String description;
    private final String statusesUrl;
    private final String repositoryUrl;
    private final String url;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    @Nullable
    private final String originalEnvironment;
    private final boolean transientEnvironment; // default : false
    private final boolean productionEnvironment; // default : false
    @Nullable
    private final App performedViaGithubApp;

    public Deployment(GitHubAPI api, CheckRun checkRun, JSONObject data) {
        this.api = api;
        this.checkRun = checkRun;

        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.task = data.getString("task");
        this.environment = data.getString("environment");
        this.description = data.has("description") ? data.getString("description") : null;
        this.statusesUrl = data.getString("statuses_url");
        this.repositoryUrl = data.getString("repository_url");
        this.url = data.getString("url");
        this.createdAt = ZonedDateTime.parse(data.getString("created_at"));
        this.updatedAt = ZonedDateTime.parse(data.getString("updated_at"));
        this.originalEnvironment = data.has("original_environment") ? data.getString("original_environment") : null;
        this.transientEnvironment = data.has("transient_environment") && data.getBoolean("transient_environment");
        this.productionEnvironment = data.has("production_environment") && data.getBoolean("production_environment");
        this.performedViaGithubApp = data.has("performed_via_github_app") ? new App(api, checkRun, data.getJSONObject("performed_via_github_app")) : null;
    }

    public CheckRun getCheckRun() {
        return this.checkRun;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getTask() {
        return this.task;
    }

    public String getEnvironment() {
        return this.environment;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public String getStatusesUrl() {
        return this.statusesUrl;
    }

    public String getRepositoryUrl() {
        return this.repositoryUrl;
    }

    public String getUrl() {
        return this.url;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Nullable
    public String getOriginalEnvironment() {
        return this.originalEnvironment;
    }

    public boolean isTransientEnvironment() {
        return this.transientEnvironment;
    }

    public boolean isProductionEnvironment() {
        return this.productionEnvironment;
    }

    @Nullable
    public App getPerformedViaGithubApp() {
        return this.performedViaGithubApp;
    }
}
