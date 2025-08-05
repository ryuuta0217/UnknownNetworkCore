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
