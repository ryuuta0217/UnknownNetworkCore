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

package com.ryuuta0217.api.github.repository.branch;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.commit.SimpleCommitImpl;
import com.ryuuta0217.api.github.repository.commit.interfaces.SimpleCommit;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.List;

public class Branch {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;
    private final boolean isProtected;
    private final String name;
    private final SimpleCommit commit;
    @Nullable
    private final Protection protection;
    @Nullable
    private final String protectionUrl;

    public Branch(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;
        this.isProtected = data.getBoolean("protected");
        this.name = data.getString("name");
        this.commit = new SimpleCommitImpl(api, this.repository, data.getJSONObject("commit"));
        this.protection = data.has("protection") && !data.isNull("protection") ? new Protection(api, this, data.getJSONObject("protection")) : null;
        this.protectionUrl = data.has("protection_url") && !data.isNull("protection_url") ? data.getString("protection_url") : null;
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    public String getName() {
        return this.name;
    }

    public SimpleCommit getCommit() {
        return this.commit;
    }

    @Nullable
    public Protection getProtection() {
        return this.protection;
    }

    @Nullable
    public String getProtectionUrl() {
        return this.protectionUrl;
    }

    public List<com.ryuuta0217.api.github.repository.commit.interfaces.Commit> getCommits() {
        return this.api.getCommits(this.repository.getOwner().getLogin(), this.repository, this.getName());
    }

    @Nullable
    public Branch tryFetchCompleteData() {
        return this.repository.getBranch(this.getName());
    }
}
