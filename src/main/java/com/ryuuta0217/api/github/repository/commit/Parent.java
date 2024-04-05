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

package com.ryuuta0217.api.github.repository.commit;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class Parent {
    private final GitHubAPI api;
    private final String sha;
    private final String url;
    @Nullable
    private final String htmlUrl;

    public Parent(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.sha = data.getString("sha");
        this.url = data.getString("url");
        this.htmlUrl = data.has("html_url") && !data.isNull("html_url") ? data.getString("html_url") : null;
    }

    public String getSha() {
        return this.sha;
    }

    public String getUrl() {
        return this.url;
    }

    @Nullable
    public String getHtmlUrl() {
        return this.htmlUrl;
    }
}
