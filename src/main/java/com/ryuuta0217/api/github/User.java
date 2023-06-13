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

package com.ryuuta0217.api.github;

import com.ryuuta0217.api.github.repository.Repository;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class User {
    private final GitHubAPI api;
    private final String gistsUrl;
    private final String reposUrl;
    @Nullable private final Boolean twoFactorAuthentication;
    private final String followingUrl;
    @Nullable private final String twitterUsername;
    @Nullable private final String bio;
    private final ZonedDateTime createdAt;
    private final String login;
    private final String type;
    @Nullable private final String blog;
    private final long privateGists;
    private final long totalPrivateRepos;
    @Nullable private final String subscriptionsUrl;
    private final ZonedDateTime updatedAt;
    private final boolean siteAdmin;
    private final long diskUsage;
    private final long collaborators;
    @Nullable private final Object company;
    private final long ownedPrivateRepos;
    private final long id;
    private final long publicRepos;
    private final String gravatarId;
    @Nullable private final Plan plan;
    private final String email;
    private final String organizationsUrl;
    private final boolean hireable;
    private final String starredUrl;
    private final String followersUrl;
    private final long publicGists;
    private final String url;
    private final String receivedEventsUrl;
    private final long followers;
    private final String avatarUrl;
    private final String eventsUrl;
    private final String htmlUrl;
    private final long following;
    @Nullable private final String name;
    @Nullable private final String location;
    private final String nodeId;

    public User(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.gistsUrl = data.getString("gists_url");
        this.reposUrl = data.getString("repos_url");
        this.followingUrl = data.getString("following_url");
        this.twitterUsername = data.has("twitter_username") && !data.get("twitter_username").equals(JSONObject.NULL) ? data.getString("twitter_username") : null;
        this.bio = data.has("bio") && !data.get("bio").equals(JSONObject.NULL) ? data.getString("bio") : null;
        this.createdAt = data.has("created_at") ? ZonedDateTime.parse(data.getString("created_at")) : ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
        this.login = data.getString("login");
        this.type = data.getString("type");
        this.blog = data.has("blog") && !data.get("blog").equals(JSONObject.NULL) ? data.getString("blog") : null;
        this.subscriptionsUrl = data.has("subscriptions_url") && !data.get("subscriptions_url").equals(JSONObject.NULL) ? data.getString("subscriptions_url") : null;
        this.updatedAt = data.has("updated_at") ? ZonedDateTime.parse(data.getString("updated_at")) : ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault());
        this.siteAdmin = data.getBoolean("site_admin");
        this.company = data.has("company") ? data.get("company") : null;
        this.id = data.getLong("id");
        this.publicRepos = data.has("public_repos") ? data.getLong("public_repos") : 0;
        this.gravatarId = data.getString("gravatar_id");
        this.email = data.has("email") && !data.get("email").equals(JSONObject.NULL) ? data.getString("email") : null;
        this.organizationsUrl = data.getString("organizations_url");
        this.hireable = data.has("hireable") && !data.get("hireable").equals(JSONObject.NULL) && data.getBoolean("hireable");
        this.starredUrl = data.getString("starred_url");
        this.followersUrl = data.getString("followers_url");
        this.publicGists = data.has("public_gists") ? data.getLong("public_gists") : 0;
        this.url = data.getString("url");
        this.receivedEventsUrl = data.getString("received_events_url");
        this.followers = data.has("followers") ? data.getLong("followers") : 0;
        this.avatarUrl = data.getString("avatar_url");
        this.eventsUrl = data.getString("events_url");
        this.htmlUrl = data.getString("html_url");
        this.following = data.has("following") ? data.getLong("following") : 0;
        this.name = data.has("name") && !data.get("name").equals(JSONObject.NULL) ? data.getString("name") : null;
        this.location = data.has("location") && !data.get("location").equals(JSONObject.NULL) ? data.getString("location") : null;
        this.nodeId = data.getString("node_id");

        /* for self account */
        this.twoFactorAuthentication = data.has("two_factor_authentication") ? data.getBoolean("two_factor_authentication") : null;
        this.privateGists = data.has("private_gists") ? data.getLong("private_gists") : -1;
        this.totalPrivateRepos = data.has("total_private_repos") ? data.getLong("total_private_repos") : -1;
        this.diskUsage = data.has("disk_usage") ? data.getLong("disk_usage") : -1;
        this.collaborators = data.has("collaborators") ? data.getLong("collaborators") : -1;
        this.ownedPrivateRepos = data.has("owned_private_repos") ? data.getLong("owned_private_repos") : -1;
        this.plan = data.has("plan") && !data.get("plan").equals(JSONObject.NULL) ? new Plan(this.api, data.getJSONObject("plan")) : null;
    }

    public String getGistsUrl() {
        return this.gistsUrl;
    }

    public String getReposUrl() {
        return this.reposUrl;
    }

    @Nullable
    public Boolean getTwoFactorAuthentication() {
        return this.twoFactorAuthentication;
    }

    public String getFollowingUrl() {
        return this.followingUrl;
    }

    @Nullable
    public String getTwitterUsername() {
        return this.twitterUsername;
    }

    @Nullable
    public String getBio() {
        return this.bio;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public String getLogin() {
        return this.login;
    }

    public String getType() {
        return this.type;
    }

    @Nullable
    public String getBlog() {
        return this.blog;
    }

    public long getPrivateGists() {
        return this.privateGists;
    }

    public long getTotalPrivateRepos() {
        return this.totalPrivateRepos;
    }

    @Nullable
    public String getSubscriptionsUrl() {
        return this.subscriptionsUrl;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public boolean isSiteAdmin() {
        return this.siteAdmin;
    }

    public long getDiskUsage() {
        return this.diskUsage;
    }

    public long getCollaborators() {
        return this.collaborators;
    }

    @Nullable
    public Object getCompany() {
        return this.company;
    }

    public long getOwnedPrivateRepos() {
        return this.ownedPrivateRepos;
    }

    public long getId() {
        return this.id;
    }

    public long getPublicRepos() {
        return this.publicRepos;
    }

    public String getGravatarId() {
        return this.gravatarId;
    }

    @Nullable
    public Plan getPlan() {
        return this.plan;
    }

    public String getEmail() {
        return this.email;
    }

    public String getOrganizationsUrl() {
        return this.organizationsUrl;
    }

    public boolean isHireable() {
        return this.hireable;
    }

    public String getStarredUrl() {
        return this.starredUrl;
    }

    public String getFollowersUrl() {
        return this.followersUrl;
    }

    public long getPublicGists() {
        return this.publicGists;
    }

    public String getUrl() {
        return this.url;
    }

    public String getReceivedEventsUrl() {
        return this.receivedEventsUrl;
    }

    public long getFollowers() {
        return this.followers;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    public String getEventsUrl() {
        return this.eventsUrl;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public long getFollowing() {
        return this.following;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getLocation() {
        return this.location;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    @Nullable
    public User tryFetchCompleteData() {
        return this.api.getUser(this.getLogin());
    }

    @Nullable
    public Repository getRepository(String repositoryName) {
        return this.api.getRepository(this.getLogin(), repositoryName);
    }

    public List<Repository> getRepositories() {
        Object obj = this.api.requestAndParse("GET", this.getReposUrl());
        if (!(obj instanceof JSONArray jsonArray)) return Collections.emptyList();
        return jsonArray.toList().stream()
                .filter(raw -> raw instanceof HashMap<?,?>)
                .map(raw -> (HashMap<?,?>) raw)
                .map(map -> new JSONObject(map))
                .map(json -> new Repository(this.api, json))
                .toList();
    }
    
    public static class Plan {
        private GitHubAPI api;
        private final long privateRepos;
        private final String name;
        private final long collaborators;
        private final long space;
        
        public Plan(GitHubAPI api, JSONObject data) {
            this.api = api;
            this.privateRepos = data.getLong("private_repos");
            this.name = data.getString("name");
            this.collaborators = data.getLong("collaborators");
            this.space = data.getLong("space");
        }

        public long getPrivateRepos() {
            return this.privateRepos;
        }

        public String getName() {
            return this.name;
        }

        public long getCollaborators() {
            return this.collaborators;
        }

        public long getSpace() {
            return this.space;
        }
    }
}
