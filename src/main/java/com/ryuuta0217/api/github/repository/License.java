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

import javax.annotation.Nullable;

public class License {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;

    private final String name;
    private final String spdxId;
    private final String key;
    @Nullable
    private final String url;
    private final String nodeId;

    public License(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;

        this.name = data.getString("name");
        this.spdxId = data.getString("spdx_id");
        this.key = data.getString("key");
        this.url = data.has("url") && !data.isNull("url") ? data.getString("url") : null;
        this.nodeId = data.getString("node_id");
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public String getName() {
        return this.name;
    }

    public String getSpdxId() {
        return this.spdxId;
    }

    public String getKey() {
        return this.key;
    }

    @Nullable
    public String getUrl() {
        return this.url;
    }

    public String getNodeId() {
        return this.nodeId;
    }
}
