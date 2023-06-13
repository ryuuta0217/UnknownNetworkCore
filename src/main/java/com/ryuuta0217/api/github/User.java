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

import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final String blog;
    @Nullable private final Long privateGists;
    @Nullable private final Long totalPrivateRepos;
    private final String subscriptionsUrl;
    private final ZonedDateTime updatedAt;
    private final boolean siteAdmin;
    @Nullable private final Long diskUsage;
    @Nullable private final Long collaborators;
    private final Object company;
    @Nullable private final Long ownedPrivateRepos;
    private final long id;
    private final long publicRepos;
    private final String gravatarId;
    @Nullable private final Plan plan;
    private final String email;
    private final String organizationsUrl;
    @Nullable private final Boolean hireable;
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
        this.twitterUsername = !data.get("twitter_username").equals(JSONObject.NULL) ? data.getString("twitter_username") : null;
        this.bio = !data.get("bio").equals(JSONObject.NULL) ? data.getString("bio") : null;
        this.createdAt = ZonedDateTime.parse(data.getString("created_at"));
        this.login = data.getString("login");
        this.type = data.getString("type");
        this.blog = data.getString("blog");
        this.subscriptionsUrl = data.getString("subscriptions_url");
        this.updatedAt = ZonedDateTime.parse(data.getString("updated_at"));
        this.siteAdmin = data.getBoolean("site_admin");
        this.company = data.get("company");
        this.id = data.getLong("id");
        this.publicRepos = data.getLong("public_repos");
        this.gravatarId = data.getString("gravatar_id");
        this.email = !data.get("email").equals(JSONObject.NULL) ? data.getString("email") : null;
        this.organizationsUrl = data.getString("organizations_url");
        this.hireable = !data.get("hireable").equals(JSONObject.NULL) ? data.getBoolean("hireable") : null;
        this.starredUrl = data.getString("starred_url");
        this.followersUrl = data.getString("followers_url");
        this.publicGists = data.getLong("public_gists");
        this.url = data.getString("url");
        this.receivedEventsUrl = data.getString("received_events_url");
        this.followers = data.getLong("followers");
        this.avatarUrl = data.getString("avatar_url");
        this.eventsUrl = data.getString("events_url");
        this.htmlUrl = data.getString("html_url");
        this.following = data.getLong("following");
        this.name = !data.get("name").equals(JSONObject.NULL) ? data.getString("name") : null;
        this.location = !data.get("location").equals(JSONObject.NULL) ? data.getString("location") : null;
        this.nodeId = data.getString("node_id");

        /* for self account */
        this.twoFactorAuthentication = data.has("two_factor_authentication") && !data.get("two_factor_authentication").equals(JSONObject.NULL) ? data.getBoolean("two_factor_authentication") : null;
        this.privateGists = data.has("private_gists") && !data.get("private_gists").equals(JSONObject.NULL) ? data.getLong("private_gists") : null;
        this.totalPrivateRepos = data.has("total_private_repos") && !data.get("total_private_repos").equals(JSONObject.NULL) ? data.getLong("total_private_repos") : null;
        this.diskUsage = data.has("disk_usage") && !data.get("disk_usage").equals(JSONObject.NULL) ? data.getLong("disk_usage") : null;
        this.collaborators = data.has("collaborators") && !data.get("collaborators").equals(JSONObject.NULL) ? data.getLong("collaborators") : null;
        this.ownedPrivateRepos = data.has("owned_private_repos") && !data.get("owned_private_repos").equals(JSONObject.NULL) ? data.getLong("owned_private_repos") : null;
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

    public String getBlog() {
        return this.blog;
    }

    @Nullable
    public Long getPrivateGists() {
        return this.privateGists;
    }

    @Nullable
    public Long getTotalPrivateRepos() {
        return this.totalPrivateRepos;
    }

    public String getSubscriptionsUrl() {
        return this.subscriptionsUrl;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public boolean isSiteAdmin() {
        return this.siteAdmin;
    }

    @Nullable
    public Long getDiskUsage() {
        return this.diskUsage;
    }

    @Nullable
    public Long getCollaborators() {
        return this.collaborators;
    }

    public Object getCompany() {
        return this.company;
    }

    @Nullable
    public Long getOwnedPrivateRepos() {
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

    @Nullable
    public Boolean getHireable() {
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
