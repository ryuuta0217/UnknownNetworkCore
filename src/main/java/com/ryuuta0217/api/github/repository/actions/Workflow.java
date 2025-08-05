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

package com.ryuuta0217.api.github.repository.actions;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class Workflow {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;
    
    private final long id;
    private final String nodeId;
    private final String name;
    private final String path;
    private final State state;
    private final ZonedDateTime createAt;
    private final ZonedDateTime updatedAt;
    private final String url;
    private final String htmlUrl;
    private final String badgeUrl;
    @Nullable private final ZonedDateTime deletedAt;

    public Workflow(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;
        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.name = data.getString("name");
        this.path = data.getString("path");
        this.state = State.valueOf(data.getString("state").toUpperCase());
        this.createAt = ZonedDateTime.parse(data.getString("created_at"));
        this.updatedAt = ZonedDateTime.parse(data.getString("updated_at"));
        this.url = data.getString("url");
        this.htmlUrl = data.getString("html_url");
        this.badgeUrl = data.getString("badge_url");
        this.deletedAt = data.isNull("deleted_at") ? null : ZonedDateTime.parse(data.getString("deleted_at"));
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public State getState() {
        return this.state;
    }

    public ZonedDateTime getCreateAt() {
        return this.createAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getBadgeUrl() {
        return this.badgeUrl;
    }

    @Nullable
    public ZonedDateTime getDeletedAt() {
        return this.deletedAt;
    }
}
