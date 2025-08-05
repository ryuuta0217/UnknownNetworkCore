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
import com.ryuuta0217.api.github.user.GitUserImpl;
import com.ryuuta0217.api.github.user.interfaces.GitUser;
import org.json.JSONObject;

public class GitCommit {
    private final GitHubAPI api;
    private final long commentCount;
    private final GitUser committer;
    private final GitUser author;
    private final Tree tree;
    private final String message;
    private final String url;
    private final Verification verification;

    public GitCommit(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.commentCount = data.getLong("comment_count");
        this.committer = new GitUserImpl(api, data.getJSONObject("committer"));
        this.author = new GitUserImpl(api, data.getJSONObject("author"));
        this.tree = new Tree(api, data.getJSONObject("tree"));
        this.message = data.getString("message");
        this.url = data.getString("url");
        this.verification = new Verification(api, data.getJSONObject("verification"));
    }

    public long getCommentCount() {
        return this.commentCount;
    }

    public GitUser getCommitter() {
        return this.committer;
    }

    public GitUser getAuthor() {
        return this.author;
    }

    public Tree getTree() {
        return this.tree;
    }

    public String getMessage() {
        return this.message;
    }

    public String getUrl() {
        return this.url;
    }

    public Verification getVerification() {
        return this.verification;
    }
}
