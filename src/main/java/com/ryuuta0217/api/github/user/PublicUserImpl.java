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
import com.ryuuta0217.api.github.user.interfaces.Plan;
import com.ryuuta0217.api.github.user.interfaces.PublicUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class PublicUserImpl extends UserImpl implements PublicUser {
    @Nullable
    private final ZonedDateTime suspendedAt;
    @Nullable
    private final String twitterUsername;
    @Nullable
    private final Plan plan;
    private final long privateGists;
    private final long totalPrivateRepos;
    private final long diskUsage;
    private final long collaborators;

    public PublicUserImpl(GitHubAPI api, JSONObject data) {
        super(api, data);
        this.suspendedAt = data.has("suspended_at") ? ZonedDateTime.parse(data.getString("suspended_at")) : null;
        this.twitterUsername = data.has("twitter_username") && !data.get("twitter_username").equals(JSONObject.NULL) ? data.getString("twitter_username") : null;
        this.plan = data.has("plan") && !data.get("plan").equals(JSONObject.NULL) ? new PlanImpl(data.getJSONObject("plan")) : null;
        this.privateGists = data.has("private_gists") ? data.getLong("private_gists") : -1;
        this.totalPrivateRepos = data.has("total_private_repos") ? data.getLong("total_private_repos") : -1;
        this.diskUsage = data.has("disk_usage") ? data.getLong("disk_usage") : -1;
        this.collaborators = data.has("collaborators") ? data.getLong("collaborators") : -1;
    }

    @Nullable
    @Override
    public ZonedDateTime getSuspendedAt() {
        return this.suspendedAt;
    }

    @Nullable
    @Override
    public String getTwitterUsername() {
        return this.twitterUsername;
    }

    @Nullable
    @Override
    public Plan getPlan() {
        return this.plan;
    }

    @Override
    public long getPrivateGists() {
        return this.privateGists;
    }

    @Override
    public long getTotalPrivateRepos() {
        return this.totalPrivateRepos;
    }

    @Override
    public long getDiskUsage() {
        return this.diskUsage;
    }

    @Override
    public long getCollaborators() {
        return this.collaborators;
    }
}
