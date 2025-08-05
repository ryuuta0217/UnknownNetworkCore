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
        this.twitterUsername = data.has("twitter_username") && !data.isNull("twitter_username") ? data.getString("twitter_username") : null;
        this.plan = data.has("plan") && !data.isNull("plan") ? new PlanImpl(data.getJSONObject("plan")) : null;
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
