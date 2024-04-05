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
import com.ryuuta0217.api.github.repository.commit.interfaces.Commit;
import com.ryuuta0217.api.github.repository.commit.interfaces.SimpleCommit;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.user.GitUserImpl;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.user.interfaces.GitUser;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class SimpleCommitImpl implements SimpleCommit {
    protected final GitHubAPI api;
    protected final RepositoryMinimal repository;

    private final String sha;
    @Nullable private final String treeId;
    @Nullable private final String message;
    @Nullable private final ZonedDateTime timestamp;
    @Nullable private final GitUser author;
    @Nullable private final GitUser committer;

    public SimpleCommitImpl(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;

        this.sha = (data.has("id") ? data.getString("id") : (data.has("sha") ? data.getString("sha"): null));
        this.treeId = data.has("tree_id") ? data.getString("tree_id") : null;
        this.message = data.has("message") ? data.getString("message") : null;
        this.timestamp = data.has("timestamp") ? ZonedDateTime.parse(data.getString("timestamp")) : null;
        GitUser author;
        try {
            author = data.has("author") ? new SimpleUserImpl(api, data.getJSONObject("author")) : null;
        } catch (JSONException e) {
            author = data.has("author") ? new GitUserImpl(api, data.getJSONObject("author")) : null;
        }
        this.author = author;

        GitUser committer = null;
        if (data.has("committer") && !data.isNull("committer")) {
            try {
                committer = data.has("committer") ? new SimpleUserImpl(api, data.getJSONObject("committer")) : null;
            } catch (JSONException e) {
                committer = data.has("committer") ? new GitUserImpl(api, data.getJSONObject("committer")) : null;
            }
        }
        this.committer = committer;
    }

    @Override
    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    @Deprecated
    @Override
    public String getId() {
        return this.sha;
    }

    @Override
    public String getSha() {
        return this.sha;
    }

    @Nullable
    @Override
    public String getTreeId() {
        return this.treeId;
    }

    @Nullable
    @Override
    public String getMessage() {
        return this.message;
    }

    @Nullable
    @Override
    public ZonedDateTime getTimestamp() {
        return this.timestamp;
    }

    @Nullable
    @Override
    public GitUser getAuthor() {
        return this.author;
    }

    @Nullable
    @Override
    public GitUser getCommitter() {
        return this.committer;
    }

    @Nullable
    @Override
    public Commit tryGetCommit() {
        return this.api.getCommit(this.getRepository(), this.getId());
    }

    @Override
    public CompareResult compare(SimpleCommit head) {
        return this.api.getCompareResult(this.getRepository(), this.getId(), head.getId());
    }
}
