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
import com.ryuuta0217.api.github.GitUser;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.repository.Repository;
import com.ryuuta0217.api.github.repository.commit.Parent;
import com.ryuuta0217.api.github.repository.commit.Tree;
import com.ryuuta0217.api.github.repository.commit.Verification;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class Branch {
    private final GitHubAPI api;
    private final Repository repository;
    private final boolean isProtected;
    private final String name;
    private final Commit commit;
    @Nullable private final Protection protection;
    @Nullable private final String protectionUrl;

    public Branch(GitHubAPI api, Repository repository, JSONObject data) {
        this.api = api;
        this.repository = repository;
        this.isProtected = data.getBoolean("protected");
        this.name = data.getString("name");
        this.commit = new Commit(api, this, data.getJSONObject("commit"));
        this.protection = data.has("protection") && !data.get("protection").equals(JSONObject.NULL) ? new Protection(api, this, data.getJSONObject("protection")) : null;
        this.protectionUrl = data.has("protection_url") && !data.get("protection_url").equals(JSONObject.NULL) ? data.getString("protection_url") : null;
    }

    public Repository getRepository() {
        return this.repository;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    public String getName() {
        return this.name;
    }

    public Commit getCommit() {
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

    public List<com.ryuuta0217.api.github.repository.commit.Commit> getCommits() {
        return this.api.getCommits(this.repository.getOwner().getLogin(), this.repository, this.getName());
    }

    @Nullable
    public Branch tryFetchCompleteData() {
        return this.repository.getBranch(this.getName());
    }

    public static class Commit {
        private final GitHubAPI api;
        private final Branch branch;
        @Nullable private final SimpleUser committer;
        @Nullable private final SimpleUser author;
        @Nullable private final String htmlUrl;
        @Nullable private final GitCommit commit;
        @Nullable private final String commentsUrl;
        private final String sha;
        private final String url;
        @Nullable private final String nodeId;
        @Nullable private final Parent[] parents;

        public Commit(GitHubAPI api, Branch branch, JSONObject data) {
            this.api = api;
            this.branch = branch;
            this.committer = data.isNull("committer") ? null : new SimpleUserImpl(api, data.getJSONObject("committer"));
            this.author = data.isNull("author") ? null : new SimpleUserImpl(api, data.getJSONObject("author"));
            this.htmlUrl = data.isNull("html_url") ? null : data.getString("html_url");
            this.commit = data.isNull("commit") ? null : new GitCommit(api, this, data.getJSONObject("commit"));
            this.commentsUrl = data.isNull("comments_url") ? null : data.getString("comments_url");
            this.sha = data.getString("sha");
            this.url = data.getString("url");
            this.nodeId = data.isNull("node_id") ? null : data.getString("node_id");
            this.parents = data.isNull("parents") ? null : data.getJSONArray("parents").toList().stream().map(raw -> new Parent(api, new JSONObject((Map<?, ?>) raw))).toArray(Parent[]::new);
        }

        public Branch getBranch() {
            return this.branch;
        }

        @Nullable
        public SimpleUser getCommitter() {
            return this.committer;
        }

        @Nullable
        public SimpleUser getAuthor() {
            return this.author;
        }

        @Nullable
        public String getHtmlUrl() {
            return this.htmlUrl;
        }

        @Nullable
        public GitCommit getCommit() {
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

        public com.ryuuta0217.api.github.repository.commit.Commit tryFetchCompleteData() {
            Object obj = this.api.requestAndParse("GET", this.getUrl());
            if (!(obj instanceof JSONObject json)) return null;
            return new com.ryuuta0217.api.github.repository.commit.Commit(this.api, this.getBranch().getRepository(), json);
        }

        public static class GitCommit {
            private final GitHubAPI api;
            private final Commit commit;
            private final long commentCount;
            private final GitUser committer;
            private final GitUser author;
            private final Tree tree;
            private final String message;
            private final String url;
            private final Verification verification;

            public GitCommit(GitHubAPI api, Commit commit, JSONObject data) {
                this.api = api;
                this.commit = commit;
                this.commentCount = data.getLong("comment_count");
                this.committer = new GitUser(this.api, data.getJSONObject("committer"));
                this.author = new GitUser(this.api, data.getJSONObject("author"));
                this.tree = new Tree(this.api, data.getJSONObject("tree"));
                this.message = data.getString("message");
                this.url = data.getString("url");
                this.verification = new Verification(this.api, data.getJSONObject("verification"));
            }

            public Commit getCommit() {
                return this.commit;
            }

            public long getCommentCount() {
                return this.commentCount;
            }

            public GitUser getCommitter() {
                return this.committer;
            }

            public GitUser getAuthor() {
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

            public com.ryuuta0217.api.github.repository.commit.Commit tryFetchCompleteData() {
                Object obj = this.api.requestAndParse("GET", this.getUrl());
                if (!(obj instanceof JSONObject json)) return null;
                return new com.ryuuta0217.api.github.repository.commit.Commit(this.api, this.getCommit().getBranch().getRepository(), json);
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
