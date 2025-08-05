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
import com.ryuuta0217.api.github.repository.check.CheckRun;
import com.ryuuta0217.api.github.repository.commit.interfaces.Commit;
import com.ryuuta0217.api.github.repository.commit.interfaces.SimpleCommit;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.user.interfaces.GitUser;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class CommitImpl implements Commit, SimpleCommit {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;

    @Nullable private final SimpleUser committer;
    @Nullable
    private final Stats stats;
    @Nullable private final SimpleUser author;
    private final String htmlUrl;
    private final GitCommit commit;
    private final String commentsUrl;
    @Nullable
    private final File[] files;
    private final String sha;
    private final String url;
    private final String nodeId;
    private final Parent[] parents;

    public CommitImpl(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;

        this.committer = data.has("committer") ? new SimpleUserImpl(api, data.getJSONObject("committer")) : null;
        this.stats = data.has("stats") ? new Stats(api, data.getJSONObject("stats")) : null;
        this.author = data.has("author") ? new SimpleUserImpl(api, data.getJSONObject("author")) : null;
        this.htmlUrl = data.getString("html_url");
        this.commit = new GitCommit(api, data.getJSONObject("commit"));
        this.commentsUrl = data.getString("comments_url");
        this.files = data.has("files") ? data.getJSONArray("files").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new File(api, json))
                .toArray(File[]::new) : null;
        this.sha = data.getString("sha");
        this.url = data.getString("url");
        this.nodeId = data.getString("node_id");
        this.parents = data.getJSONArray("parents").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new Parent(api, json))
                .toArray(Parent[]::new);
    }

    @Override
    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    @Deprecated
    @Nullable
    @Override
    public GitUser getCommitter() {
        return this.committer;
    }

    @Nullable
    @Override
    public SimpleUser getCommitterUser() {
        return this.committer;
    }

    @Nullable
    @Override
    public Stats getStats() {
        return this.stats;
    }

    @Deprecated
    @Nullable
    @Override
    public GitUser getAuthor() {
        return this.author;
    }

    @Nullable
    @Override
    public SimpleUser getAuthorUser() {
        return this.author;
    }

    @Override
    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    @Override
    public GitCommit getCommit() {
        return this.commit;
    }

    @Override
    public String getCommentsUrl() {
        return this.commentsUrl;
    }

    @Nullable
    @Override
    public File[] getFiles() {
        return this.files;
    }

    @Override
    public String getSha() {
        return this.sha;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public String getNodeId() {
        return this.nodeId;
    }

    @Override
    public Parent[] getParents() {
        return this.parents;
    }

    @Deprecated
    @Override
    public String getId() {
        return this.sha;
    }

    @Deprecated
    @Override
    public String getTreeId() {
        return this.commit.getTree().getSha();
    }

    @Deprecated
    @Override
    public String getMessage() {
        return this.commit.getMessage();
    }

    @Deprecated
    @Override
    public ZonedDateTime getTimestamp() {
        return this.commit.getCommitter().getDate();
    }

    @Nullable
    @Override
    public Commit tryGetCommit() {
        return this.api.getCommit(this.repository, this.sha);
    }

    @Override
    public List<CheckRun> tryGetCheckRuns() {
        return this.api.getCheckRunsByCommit(this);
    }

    @Override
    public CompareResult compare(SimpleCommit head) {
        return this.api.getCompareResult(this.getRepository(), this.getSha(), head.getId());
    }
}
