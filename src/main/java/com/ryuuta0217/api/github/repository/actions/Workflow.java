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
