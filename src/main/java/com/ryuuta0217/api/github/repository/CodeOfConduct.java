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

public class CodeOfConduct {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;

    private final String url;
    @Nullable private final String htmlUrl;
    private final String key;
    private final String name;

    @Nullable private final String body;

    public CodeOfConduct(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;

        this.url = data.getString("url");
        this.htmlUrl = data.has("html_url") && !data.isNull("html_url") ? data.getString("html_url") : null;
        this.key = data.getString("key");
        this.name = data.getString("name");

        this.body = data.has("body") && !data.isNull("body") ? data.getString("body") : null;
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public String getUrl() {
        return this.url;
    }

    @Nullable
    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public String getBody() {
        return this.body;
    }
}
