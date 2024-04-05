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

package com.ryuuta0217.api.github.repository.pullreq;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.check.pr.BranchPointer;
import org.json.JSONObject;

public class PullRequestMinimal {
    private final GitHubAPI api;
    private final long id;
    private final long number;
    private final String url;
    private final BranchPointer head;
    private final BranchPointer base;

    public PullRequestMinimal(GitHubAPI api, JSONObject data) {
        this.api = api;

        this.id = data.getLong("id");
        this.number = data.getLong("number");
        this.url = data.getString("url");
        this.head = new BranchPointer(api, this, data.getJSONObject("head"));
        this.base = new BranchPointer(api, this, data.getJSONObject("base"));
    }

    public long getId() {
        return this.id;
    }

    public long getNumber() {
        return this.number;
    }

    public String getUrl() {
        return this.url;
    }

    public BranchPointer getHead() {
        return this.head;
    }

    public BranchPointer getBase() {
        return this.base;
    }
}
