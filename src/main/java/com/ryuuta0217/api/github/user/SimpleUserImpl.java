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
import com.ryuuta0217.api.github.user.interfaces.PublicUser;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class SimpleUserImpl implements SimpleUser {
    protected final GitHubAPI api;

    @Nullable
    private final String name;
    @Nullable
    private final String email;
    private final String login;
    private final long id;
    private final String nodeId;
    private final String avatarUrl;
    private final String gravatarId;
    private final String url;
    private final String htmlUrl;
    private final String followersUrl;
    private final String followingUrl;
    private final String gistsUrl;
    private final String starredUrl;
    private final String subscriptionsUrl;
    private final String organizationsUrl;
    private final String reposUrl;
    private final String eventsUrl;
    private final String receivedEventsUrl;
    private final String type;
    private final boolean siteAdmin;
    @Nullable
    private final ZonedDateTime starredAt;

    public SimpleUserImpl(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.name = data.has("name") && !data.isNull("name") ? data.getString("name") : null;
        this.email = data.has("email") && !data.isNull("email") ? data.getString("email") : null;
        this.login = data.getString("login");
        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.avatarUrl = data.getString("avatar_url");
        this.gravatarId = data.getString("gravatar_id");
        this.url = data.getString("url");
        this.htmlUrl = data.getString("html_url");
        this.followersUrl = data.getString("followers_url");
        this.followingUrl = data.getString("following_url");
        this.gistsUrl = data.getString("gists_url");
        this.starredUrl = data.getString("starred_url");
        this.subscriptionsUrl = data.getString("subscriptions_url");
        this.organizationsUrl = data.getString("organizations_url");
        this.reposUrl = data.getString("repos_url");
        this.eventsUrl = data.getString("events_url");
        this.receivedEventsUrl = data.getString("received_events_url");
        this.type = data.getString("type");
        this.siteAdmin = data.getBoolean("site_admin");
        this.starredAt = data.has("starred_at") && !data.isNull("starred_at") ? ZonedDateTime.parse(data.getString("starred_at")) : null;
    }

    @Override
    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    @Override
    public String getEventsUrl() {
        return this.eventsUrl;
    }

    @Override
    public String getFollowersUrl() {
        return this.followersUrl;
    }

    @Override
    public String getFollowingUrl() {
        return this.followingUrl;
    }

    @Override
    public String getGistsUrl() {
        return this.gistsUrl;
    }

    @Nullable
    @Override
    public String getGravatarId() {
        return this.gravatarId;
    }

    @Override
    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public String getNodeId() {
        return this.nodeId;
    }

    @Override
    public String getLogin() {
        return this.login;
    }

    @Override
    public String getOrganizationsUrl() {
        return this.organizationsUrl;
    }

    @Override
    public String getReceivedEventsUrl() {
        return this.receivedEventsUrl;
    }

    @Override
    public String getReposUrl() {
        return this.reposUrl;
    }

    @Override
    public boolean isSiteAdmin() {
        return this.siteAdmin;
    }

    @Override
    public String getStarredUrl() {
        return this.starredUrl;
    }

    @Override
    public String getSubscriptionsUrl() {
        return this.subscriptionsUrl;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Nullable
    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public String getEmail() {
        return this.email;
    }

    @Deprecated
    @Nullable
    @Override
    public ZonedDateTime getDate() {
        return null;
    }

    @Nullable
    @Override
    public PublicUser tryGetUser() {
        return this.api.getUser(this.getLogin());
    }

    @Nullable
    @Override
    public ZonedDateTime getStarredAt() {
        return this.starredAt;
    }
}
