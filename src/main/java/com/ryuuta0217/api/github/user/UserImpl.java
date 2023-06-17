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

package com.ryuuta0217.api.github.user;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.RepositoryMinimalImpl;
import com.ryuuta0217.api.github.repository.interfaces.Repository;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.user.interfaces.PublicUser;
import com.ryuuta0217.api.github.user.interfaces.User;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class UserImpl extends SimpleUserImpl implements User {
    @Nullable
    private final String bio;
    private final ZonedDateTime createdAt;
    @Nullable
    private final String blog;
    private final ZonedDateTime updatedAt;
    @Nullable
    private final String company;
    private final long publicRepos;
    private final boolean hireable;
    private final long publicGists;
    private final long followers;
    private final long following;
    @Nullable
    private final String location;

    public UserImpl(GitHubAPI api, JSONObject data) {
        super(api, data);
        this.bio = data.has("bio") && !data.isNull("bio") ? data.getString("bio") : null;
        this.blog = data.has("blog") && !data.isNull("blog") ? data.getString("blog") : null;
        this.company = data.has("company") && !data.isNull("company") ? data.getString("company") : null;
        this.followers = data.has("followers") ? data.getLong("followers") : 0;
        this.following = data.has("following") ? data.getLong("following") : 0;
        this.hireable = data.has("hireable") && !data.isNull("hireable") && data.getBoolean("hireable");
        this.location = data.has("location") && !data.isNull("location") ? data.getString("location") : null;
        this.publicRepos = data.has("public_repos") ? data.getLong("public_repos") : 0;
        this.publicGists = data.has("public_gists") ? data.getLong("public_gists") : 0;
        this.createdAt = data.has("created_at") ? ZonedDateTime.parse(data.getString("created_at")) : ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
        this.updatedAt = data.has("updated_at") ? ZonedDateTime.parse(data.getString("updated_at")) : ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
    }

    @Nullable
    @Override
    public String getBio() {
        return this.bio;
    }

    @Nullable
    @Override
    public String getBlog() {
        return this.blog;
    }

    @Nullable
    @Override
    public String getCompany() {
        return this.company;
    }

    @Override
    public long getFollowers() {
        return this.followers;
    }

    @Override
    public long getFollowing() {
        return this.following;
    }

    @Override
    public boolean isHireable() {
        return this.hireable;
    }

    @Nullable
    @Override
    public String getLocation() {
        return this.location;
    }

    @Override
    public long getPublicRepos() {
        return this.publicRepos;
    }

    @Override
    public long getPublicGists() {
        return this.publicGists;
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Nullable
    public PublicUser tryFetchCompleteData() {
        return this.api.getUser(this.getLogin());
    }

    @Nullable
    public Repository getRepository(String repositoryName) {
        return this.api.getRepository(this.getLogin(), repositoryName);
    }

    public List<RepositoryMinimal> getRepositories() {
        Object obj = this.api.requestAndParse("GET", this.getReposUrl());
        if (!(obj instanceof JSONArray jsonArray)) return Collections.emptyList();
        return jsonArray.toList().stream()
                .filter(raw -> raw instanceof HashMap<?, ?>)
                .map(raw -> (HashMap<?, ?>) raw)
                .map(map -> new JSONObject(map))
                .map(json -> new RepositoryMinimalImpl(this.api, json))
                .map(impl -> (RepositoryMinimal) impl)
                .toList();
    }
}
