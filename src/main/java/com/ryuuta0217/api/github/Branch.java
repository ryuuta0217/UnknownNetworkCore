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

import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Map;

public class Branch {
    private final GitHubAPI api;
    private final boolean isProtected;
    private final String name;
    private final LastCommit commit;
    @Nullable private final Protection protection;
    @Nullable private final String protectionUrl;

    public Branch(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.isProtected = data.getBoolean("protected");
        this.name = data.getString("name");
        this.commit = new LastCommit(api, this, data.getJSONObject("commit"));
        this.protection = data.has("protection") && !data.get("protection").equals(JSONObject.NULL) ? new Protection(api, this, data.getJSONObject("protection")) : null;
        this.protectionUrl = data.has("protection_url") && !data.get("protection_url").equals(JSONObject.NULL) ? data.getString("protection_url") : null;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    public String getName() {
        return this.name;
    }

    public LastCommit getCommit() {
        return this.commit;
    }

    public Protection getProtection() {
        return this.protection;
    }

    public String getProtectionUrl() {
        return this.protectionUrl;
    }

    public Repository tryFetchRepository() {
        Object obj = this.api.requestAndParse("GET", this.getProtectionUrl().replaceFirst("/branches.*$", ""));
        if (!(obj instanceof JSONObject json)) return null;
        return new Repository(this.api, json);
    }

    @Nullable
    public Branch tryFetchCompleteData() {
        Object obj = this.api.requestAndParse("GET", this.getProtectionUrl().replace("/protection", ""));
        if (!(obj instanceof JSONObject json)) return null;
        return new Branch(this.api, json);
    }

    public static class LastCommit {
        private final GitHubAPI api;
        private final Branch branch;
        @Nullable private final Committer committer;
        @Nullable private final Author author;
        @Nullable private final String htmlUrl;
        @Nullable private final Commit commit;
        @Nullable private final String commentsUrl;
        private final String sha;
        private final String url;
        @Nullable private final String nodeId;
        @Nullable private final Parent[] parents;

        public LastCommit(GitHubAPI api, Branch branch, JSONObject data) {
            this.api = api;
            this.branch = branch;
            this.committer = data.isNull("committer") ? null : new Committer(api, this, data.getJSONObject("committer"));
            this.author = data.isNull("author") ? null : new Author(api, this, data.getJSONObject("author"));
            this.htmlUrl = data.isNull("html_url") ? null : data.getString("html_url");
            this.commit = data.isNull("commit") ? null : new Commit(api, this, data.getJSONObject("commit"));
            this.commentsUrl = data.isNull("comments_url") ? null : data.getString("comments_url");
            this.sha = data.getString("sha");
            this.url = data.getString("url");
            this.nodeId = data.isNull("node_id") ? null : data.getString("node_id");
            this.parents = data.isNull("parents") ? null : data.getJSONArray("parents").toList().stream().map(raw -> new Parent(api, this, new JSONObject((Map<?, ?>) raw))).toArray(Parent[]::new);
        }

        public Branch getBranch() {
            return this.branch;
        }

        @Nullable
        public Committer getCommitter() {
            return this.committer;
        }

        @Nullable
        public Author getAuthor() {
            return this.author;
        }

        @Nullable
        public String getHtmlUrl() {
            return this.htmlUrl;
        }

        @Nullable
        public Commit getCommit() {
            return this.commit;
        }

        @Nullable
        public String getCommentsUrl() {
            return this.commentsUrl;
        }

        public String getSha() {
            return this.sha;
        }

        public String getUrl() {
            return this.url;
        }

        @Nullable
        public String getNodeId() {
            return this.nodeId;
        }

        @Nullable
        public Parent[] getParents() {
            return this.parents;
        }

        public static class Committer {
            private final GitHubAPI api;
            private final LastCommit commit;
            private final String gistsUrl;
            private final String reposUrl;
            private final String followingUrl;
            private final String starredUrl;
            private final String login;
            private final String followersUrl;
            private final String type;
            private final String url;
            private final String subscriptionsUrl;
            private final String receivedEventsUrl;
            private final String avatarUrl;
            private final String eventsUrl;
            private final String htmlUrl;
            private final boolean siteAdmin;
            private final long id;
            private final String gravatarId;
            private final String nodeId;
            private final String organizationsUrl;

            public Committer(GitHubAPI api, LastCommit commit, JSONObject data) {
                this.api = api;
                this.commit = commit;
                this.gistsUrl = data.getString("gists_url");
                this.reposUrl = data.getString("repos_url");
                this.followingUrl = data.getString("following_url");
                this.starredUrl = data.getString("starred_url");
                this.login = data.getString("login");
                this.followersUrl = data.getString("followers_url");
                this.type = data.getString("type");
                this.url = data.getString("url");
                this.subscriptionsUrl = data.getString("subscriptions_url");
                this.receivedEventsUrl = data.getString("received_events_url");
                this.avatarUrl = data.getString("avatar_url");
                this.eventsUrl = data.getString("events_url");
                this.htmlUrl = data.getString("html_url");
                this.siteAdmin = data.getBoolean("site_admin");
                this.id = data.getLong("id");
                this.gravatarId = data.getString("gravatar_id");
                this.nodeId = data.getString("node_id");
                this.organizationsUrl = data.getString("organizations_url");
            }

            public LastCommit getCommit() {
                return this.commit;
            }

            public String getGistsUrl() {
                return this.gistsUrl;
            }

            public String getReposUrl() {
                return this.reposUrl;
            }

            public String getFollowingUrl() {
                return this.followingUrl;
            }

            public String getStarredUrl() {
                return this.starredUrl;
            }

            public String getLogin() {
                return this.login;
            }

            public String getFollowersUrl() {
                return this.followersUrl;
            }

            public String getType() {
                return this.type;
            }

            public String getUrl() {
                return this.url;
            }

            public String getSubscriptionsUrl() {
                return this.subscriptionsUrl;
            }

            public String getReceivedEventsUrl() {
                return this.receivedEventsUrl;
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

            public boolean isSiteAdmin() {
                return this.siteAdmin;
            }

            public long getId() {
                return this.id;
            }

            public String getGravatarId() {
                return this.gravatarId;
            }

            public String getNodeId() {
                return this.nodeId;
            }

            public String getOrganizationsUrl() {
                return this.organizationsUrl;
            }
        }

        public static class Author {
            private final GitHubAPI api;
            private final LastCommit commit;
            private final String gistsUrl;
            private final String reposUrl;
            private final String followingUrl;
            private final String starredUrl;
            private final String login;
            private final String followersUrl;
            private final String type;
            private final String url;
            private final String subscriptionsUrl;
            private final String receivedEventsUrl;
            private final String avatarUrl;
            private final String eventsUrl;
            private final String htmlUrl;
            private final boolean siteAdmin;
            private final long id;
            private final String gravatarId;
            private final String nodeId;
            private final String organizationsUrl;

            public Author(GitHubAPI api, LastCommit commit, JSONObject data) {
                this.api = api;
                this.commit = commit;
                this.gistsUrl = data.getString("gists_url");
                this.reposUrl = data.getString("repos_url");
                this.followingUrl = data.getString("following_url");
                this.starredUrl = data.getString("starred_url");
                this.login = data.getString("login");
                this.followersUrl = data.getString("followers_url");
                this.type = data.getString("type");
                this.url = data.getString("url");
                this.subscriptionsUrl = data.getString("subscriptions_url");
                this.receivedEventsUrl = data.getString("received_events_url");
                this.avatarUrl = data.getString("avatar_url");
                this.eventsUrl = data.getString("events_url");
                this.htmlUrl = data.getString("html_url");
                this.siteAdmin = data.getBoolean("site_admin");
                this.id = data.getLong("id");
                this.gravatarId = data.getString("gravatar_id");
                this.nodeId = data.getString("node_id");
                this.organizationsUrl = data.getString("organizations_url");
            }

            public LastCommit getCommit() {
                return this.commit;
            }

            public String getGistsUrl() {
                return this.gistsUrl;
            }

            public String getReposUrl() {
                return this.reposUrl;
            }

            public String getFollowingUrl() {
                return this.followingUrl;
            }

            public String getStarredUrl() {
                return this.starredUrl;
            }

            public String getLogin() {
                return this.login;
            }

            public String getFollowersUrl() {
                return this.followersUrl;
            }

            public String getType() {
                return this.type;
            }

            public String getUrl() {
                return this.url;
            }

            public String getSubscriptionsUrl() {
                return this.subscriptionsUrl;
            }

            public String getReceivedEventsUrl() {
                return this.receivedEventsUrl;
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

            public boolean isSiteAdmin() {
                return this.siteAdmin;
            }

            public long getId() {
                return this.id;
            }

            public String getGravatarId() {
                return this.gravatarId;
            }

            public String getNodeId() {
                return this.nodeId;
            }

            public String getOrganizationsUrl() {
                return this.organizationsUrl;
            }

            public User getUser() {
                return this.api.getUser(this.getLogin());
            }
        }

        public static class Commit {
            private final GitHubAPI api;
            private final LastCommit commit;
            private final long commentCount;
            private final Committer committer;
            private final Author author;
            private final Tree tree;
            private final String message;
            private final String url;
            private final Verification verification;

            public Commit(GitHubAPI api, LastCommit commit, JSONObject data) {
                this.api = api;
                this.commit = commit;
                this.commentCount = data.getLong("comment_count");
                this.committer = new Committer(api, this,data.getJSONObject("committer"));
                this.author = new Author(api, this, data.getJSONObject("author"));
                this.tree = new Tree(api, this, data.getJSONObject("tree"));
                this.message = data.getString("message");
                this.url = data.getString("url");
                this.verification = new Verification(api, this, data.getJSONObject("verification"));
            }

            public LastCommit getCommit() {
                return this.commit;
            }

            public long getCommentCount() {
                return this.commentCount;
            }

            public Committer getCommitter() {
                return this.committer;
            }

            public Author getAuthor() {
                return this.author;
            }

            public Tree getTree() {
                return this.tree;
            }

            public String getMessage() {
                return this.message;
            }

            public String getUrl() {
                return this.url;
            }

            public Verification getVerification() {
                return this.verification;
            }

            public static class Committer {
                private final GitHubAPI api;
                private final Commit commit;
                private final ZonedDateTime date;
                private final String name;
                private final String email;

                public Committer(GitHubAPI api, Commit commit, JSONObject data) {
                    this.api = api;
                    this.commit = commit;
                    this.date = ZonedDateTime.parse(data.getString("date"));
                    this.name = data.getString("name");
                    this.email = data.getString("email");
                }

                public Commit getCommit() {
                    return this.commit;
                }

                public ZonedDateTime getDate() {
                    return this.date;
                }

                public String getName() {
                    return this.name;
                }

                public String getEmail() {
                    return this.email;
                }
            }

            public static class Author {
                private final GitHubAPI api;
                private final Commit commit;
                private final ZonedDateTime date;
                private final String name;
                private final String email;

                public Author(GitHubAPI api, Commit commit, JSONObject data) {
                    this.api = api;
                    this.commit = commit;
                    this.date = ZonedDateTime.parse(data.getString("date"));
                    this.name = data.getString("name");
                    this.email = data.getString("email");
                }

                public Commit getCommit() {
                    return this.commit;
                }

                public ZonedDateTime getDate() {
                    return this.date;
                }

                public String getName() {
                    return this.name;
                }

                public String getEmail() {
                    return this.email;
                }
            }

            public static class Tree {
                private final GitHubAPI api;
                private final Commit commit;
                private final String sha;
                private final String url;

                public Tree(GitHubAPI api, Commit commit, JSONObject data) {
                    this.api = api;
                    this.commit = commit;
                    this.sha = data.getString("sha");
                    this.url = data.getString("url");
                }

                public Commit getCommit() {
                    return this.commit;
                }

                public String getSha() {
                    return this.sha;
                }

                public String getUrl() {
                    return this.url;
                }
            }

            public static class Verification {
                private final GitHubAPI api;
                private final Commit commit;
                private final String reason;
                @Nullable private final String signature;
                @Nullable private final String payload;
                private final boolean verified;

                public Verification(GitHubAPI api, Commit commit, JSONObject data) {
                    this.api = api;
                    this.commit = commit;
                    this.reason = data.getString("reason");
                    this.signature = !data.get("signature").equals(JSONObject.NULL) ? data.getString("signature") : null;
                    this.payload = !data.get("payload").equals(JSONObject.NULL) ? data.getString("payload") : null;
                    this.verified = data.getBoolean("verified");
                }

                public Commit getCommit() {
                    return this.commit;
                }

                public String getReason() {
                    return this.reason;
                }

                public String getSignature() {
                    return this.signature;
                }

                public String getPayload() {
                    return this.payload;
                }

                public boolean isVerified() {
                    return this.verified;
                }
            }
        }

        public static class Parent {
            private final GitHubAPI api;
            private final LastCommit commit;
            private final String htmlUrl;
            private final String sha;
            private final String url;

            public Parent(GitHubAPI api, LastCommit commit, JSONObject data) {
                this.api = api;
                this.commit = commit;
                this.htmlUrl = data.getString("html_url");
                this.sha = data.getString("sha");
                this.url = data.getString("url");
            }

            public LastCommit getCommit() {
                return this.commit;
            }

            public String getHtmlUrl() {
                return this.htmlUrl;
            }

            public String getSha() {
                return this.sha;
            }

            public String getUrl() {
                return this.url;
            }
        }
    }

    public static class Protection {
        private final GitHubAPI api;
        private final Branch branch;
        private final RequiredStatusChecks requiredStatusChecks;
        private final boolean enabled;

        public Protection(GitHubAPI api, Branch branch, JSONObject data) {
            this.api = api;
            this.branch = branch;
            this.requiredStatusChecks = new RequiredStatusChecks(api, this, data.getJSONObject("required_status_checks"));
            this.enabled = data.getBoolean("enabled");
        }

        public Branch getBranch() {
            return this.branch;
        }

        public RequiredStatusChecks getRequiredStatusChecks() {
            return this.requiredStatusChecks;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public static class RequiredStatusChecks {
            private final GitHubAPI api;
            private final Protection protection;
            private final String enforcementLevel;
            private final Object[] checks;
            private final Object[] contexts;

            public RequiredStatusChecks(GitHubAPI api, Protection protection, JSONObject data) {
                this.api = api;
                this.protection = protection;
                this.enforcementLevel = data.getString("enforcement_level");
                this.checks = data.getJSONArray("contexts").toList().toArray();
                this.contexts = data.getJSONArray("contexts").toList().toArray();
            }

            public Protection getProtection() {
                return this.protection;
            }

            public String getEnforcementLevel() {
                return this.enforcementLevel;
            }

            public Object[] getChecks() {
                return this.checks;
            }

            public Object[] getContexts() {
                return this.contexts;
            }
        }
    }
}
