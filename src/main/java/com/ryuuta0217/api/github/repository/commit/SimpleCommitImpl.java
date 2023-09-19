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
