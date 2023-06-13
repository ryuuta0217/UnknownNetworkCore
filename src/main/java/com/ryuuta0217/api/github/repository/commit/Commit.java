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
import com.ryuuta0217.api.github.repository.Repository;
import com.ryuuta0217.api.github.User;
import org.json.JSONObject;

import java.util.Map;

public class Commit {
    private final GitHubAPI api;
    private final Repository repository;
    private final User committer;
    private final Stats stats;
    private final User author;
    private final String htmlUrl;
    private final GitCommit commit;
    private final String commentsUrl;
    private final File[] files;
    private final String sha;
    private final String url;
    private final String nodeId;
    private final Parent[] parents;

    public Commit(GitHubAPI api, Repository repository, JSONObject data) {
        this.api = api;
        this.repository = repository;
        this.committer = new User(api, data.getJSONObject("committer"));
        this.stats = new Stats(api, data.getJSONObject("stats"));
        this.author = new User(api, data.getJSONObject("author"));
        this.htmlUrl = data.getString("html_url");
        this.commit = new GitCommit(api, data.getJSONObject("commit"));
        this.commentsUrl = data.getString("comments_url");
        this.files = data.getJSONArray("files").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?,?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new File(api, json))
                .toArray(File[]::new);
        this.sha = data.getString("sha");
        this.url = data.getString("url");
        this.nodeId = data.getString("node_id");
        this.parents = data.getJSONArray("parents").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?,?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new Parent(api, json))
                .toArray(Parent[]::new);
    }

    public User getCommitter() {
        return this.committer;
    }

    public Stats getStats() {
        return this.stats;
    }

    public User getAuthor() {
        return this.author;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public GitCommit getCommit() {
        return this.commit;
    }

    public String getCommentsUrl() {
        return this.commentsUrl;
    }

    public File[] getFiles() {
        return this.files;
    }

    public String getSha() {
        return this.sha;
    }

    public String getUrl() {
        return this.url;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public Parent[] getParents() {
        return this.parents;
    }
}
