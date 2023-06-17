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
