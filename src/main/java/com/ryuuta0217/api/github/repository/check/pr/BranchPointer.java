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

package com.ryuuta0217.api.github.repository.check.pr;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.pullreq.PullRequestMinimal;
import org.json.JSONObject;

public class BranchPointer {
    private final GitHubAPI api;
    private final PullRequestMinimal pullRequest;

    private final String ref;
    private final String sha;
    private final Repository repo;

    public BranchPointer(GitHubAPI api, PullRequestMinimal pullRequest, JSONObject data) {
        this.api = api;
        this.pullRequest = pullRequest;

        this.ref = data.getString("ref");
        this.sha = data.getString("sha");
        this.repo = new Repository(api, pullRequest, data.getJSONObject("repo"));
    }

    public PullRequestMinimal getPullRequest() {
        return this.pullRequest;
    }

    public String getRef() {
        return this.ref;
    }

    public String getSha() {
        return this.sha;
    }

    public Repository getRepo() {
        return this.repo;
    }
}
