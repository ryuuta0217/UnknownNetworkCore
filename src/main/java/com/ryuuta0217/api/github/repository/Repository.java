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

package com.ryuuta0217.api.github.repository;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;

public class Repository {
    private final GitHubAPI api;
    private final boolean allowForking;
    private final long stargazersCount;
    private final boolean isTemplate;
    private final ZonedDateTime pushedAt;
    private final String subscriptionUrl;
    @Nullable private final String language;
    private final String branchesUrl;
    private final String issueCommentUrl;
    @Nullable private final Boolean allowRebaseMerge;
    private final String labelsUrl;
    private final String subscribersUrl;
    private final Permissions permissions;
    @Nullable private final String tempCloneToken;
    private final String releasesUrl;
    private final String svnUrl;
    @Nullable private final String squashMergeCommitMessage;
    @Nullable private final Long subscribersCount;
    private final long id;
    private final boolean hasDiscussions;
    private final long forks;
    private final String archiveUrl;
    @Nullable private final Boolean allowMergeCommit;
    private final String gitRefsUrl;
    private final String forksUrl;
    private final String visibility;
    private final String statusesUrl;
    @Nullable private final Long networkCount;
    private final String sshUrl;
    @Nullable private final License license;
    private final String fullName;
    private final long size;
    @Nullable private final Boolean allowAutoMerge;
    private final String languagesUrl;
    private final String htmlUrl;
    private final String collaboratorsUrl;
    private final String cloneUrl;
    private final String name;
    private final String pullsUrl;
    private final String defaultBranch;
    private final String hooksUrl;
    private final String treesUrl;
    private final String tagsUrl;
    private final boolean isPrivate;
    private final String contributorsUrl;
    private final boolean hasDownloads;
    private final String notificationsUrl;
    private final long openIssuesCount;
    @Nullable private final String description;
    private final ZonedDateTime createdAt;
    private final long watchers;
    private final String keysUrl;
    private final String deploymentsUrl;
    private final boolean hasProjects;
    private final boolean archived;
    private final boolean hasWiki;
    private final ZonedDateTime updatedAt;
    @Nullable private final String mergeCommitTitle;
    private final String commentsUrl;
    private final String stargazersUrl;
    private final boolean disabled;
    @Nullable private final Boolean deleteBranchOnMerge;
    private final String gitUrl;
    private final boolean hasPages;
    private final SimpleUser owner;
    @Nullable private final Boolean allowSquashMerge;
    private final String commitsUrl;
    private final String compareUrl;
    private final String gitCommitsUrl;
    private final String[] topics;
    private final String blobsUrl;
    @Nullable private final Boolean allowUpdateBranch;
    private final String gitTagsUrl;
    private final String mergesUrl;
    private final String downloadsUrl;
    private final boolean hasIssues;
    private final boolean webCommitSignOffRequired;
    private final String url;
    @Nullable private final String contentsUrl;
    @Nullable private final String mirrorUrl;
    private final String milestonesUrl;
    private final String teamsUrl;
    private final boolean fork;
    private final String issuesUrl;
    private final String eventsUrl;
    @Nullable private final Boolean useSquashPrTitleAsDefault;
    private final String issueEventsUrl;
    @Nullable private final String mergeCommitMessage;
    private final String assigneesUrl;
    private final long openIssues;
    @Nullable private final String squashMergeCommitTitle;
    private final long watchersCount;
    private final String nodeId;
    @Nullable private final String homepage;
    private final long forksCount;

    public Repository(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.allowForking = data.getBoolean("allow_forking");
        this.stargazersCount = data.getLong("stargazers_count");
        this.isTemplate = data.getBoolean("is_template");
        this.pushedAt = ZonedDateTime.parse(data.getString("pushed_at"));
        this.subscriptionUrl = data.getString("subscription_url");
        this.language = data.has("language") && !data.get("language").equals(JSONObject.NULL) ? data.getString("language") : null;
        this.branchesUrl = data.getString("branches_url");
        this.issueCommentUrl = data.getString("issue_comment_url");
        this.labelsUrl = data.getString("labels_url");
        this.subscribersUrl = data.getString("subscribers_url");
        this.permissions = new Permissions(this.api, data.getJSONObject("permissions"));
        this.releasesUrl = data.getString("releases_url");
        this.svnUrl = data.getString("svn_url");
        this.id = data.getLong("id");
        this.hasDiscussions = data.getBoolean("has_discussions");
        this.forks = data.getLong("forks");
        this.archiveUrl = data.getString("archive_url");
        this.gitRefsUrl = data.getString("git_refs_url");
        this.forksUrl = data.getString("forks_url");
        this.visibility = data.getString("visibility");
        this.statusesUrl = data.getString("statuses_url");
        this.sshUrl = data.getString("ssh_url");
        this.license = data.has("license") && !data.get("license").equals(JSONObject.NULL) ? new License(this.api, data.getJSONObject("license")) : null;
        this.fullName = data.getString("full_name");
        this.size = data.getLong("size");
        this.languagesUrl = data.getString("languages_url");
        this.htmlUrl = data.getString("html_url");
        this.collaboratorsUrl = data.getString("collaborators_url");
        this.cloneUrl = data.getString("clone_url");
        this.name = data.getString("name");
        this.pullsUrl = data.getString("pulls_url");
        this.defaultBranch = data.getString("default_branch");
        this.hooksUrl = data.getString("hooks_url");
        this.treesUrl = data.getString("trees_url");
        this.tagsUrl = data.getString("tags_url");
        this.isPrivate = data.getBoolean("private");
        this.contributorsUrl = data.getString("contributors_url");
        this.hasDownloads = data.getBoolean("has_downloads");
        this.notificationsUrl = data.getString("notifications_url");
        this.openIssuesCount = data.getLong("open_issues_count");
        this.description = data.has("description") && !data.get("description").equals(JSONObject.NULL) ? data.getString("description") : null;
        this.createdAt = ZonedDateTime.parse(data.getString("created_at"));
        this.watchers = data.getLong("watchers");
        this.keysUrl = data.getString("keys_url");
        this.deploymentsUrl = data.getString("deployments_url");
        this.hasProjects = data.getBoolean("has_projects");
        this.archived = data.getBoolean("archived");
        this.hasWiki = data.getBoolean("has_wiki");
        this.updatedAt = ZonedDateTime.parse(data.getString("updated_at"));
        this.commentsUrl = data.getString("comments_url");
        this.stargazersUrl = data.getString("stargazers_url");
        this.disabled = data.getBoolean("disabled");
        this.gitUrl = data.getString("git_url");
        this.hasPages = data.getBoolean("has_pages");
        this.owner = new SimpleUserImpl(api, data.getJSONObject("owner"));
        this.commitsUrl = data.getString("commits_url");
        this.compareUrl = data.getString("compare_url");
        this.gitCommitsUrl = data.getString("git_commits_url");
        this.topics = data.getJSONArray("topics").toList().stream().map(Object::toString).toArray(String[]::new);
        this.blobsUrl = data.getString("blobs_url");
        this.gitTagsUrl = data.getString("git_tags_url");
        this.mergesUrl = data.getString("merges_url");
        this.downloadsUrl = data.getString("downloads_url");
        this.hasIssues = data.getBoolean("has_issues");
        this.webCommitSignOffRequired = data.getBoolean("web_commit_signoff_required");
        this.url = data.getString("url");
        this.mirrorUrl = data.has("mirror_url") && !data.get("mirror_url").equals(JSONObject.NULL) ? data.getString("mirror_url") : null;
        this.milestonesUrl = data.getString("milestones_url");
        this.teamsUrl = data.getString("teams_url");
        this.fork = data.getBoolean("fork");
        this.issuesUrl = data.getString("issues_url");
        this.eventsUrl = data.getString("events_url");
        this.issueEventsUrl = data.getString("issue_events_url");
        this.assigneesUrl = data.getString("assignees_url");
        this.openIssues = data.getLong("open_issues");
        this.watchersCount = data.getLong("watchers_count");
        this.nodeId = data.getString("node_id");
        this.homepage = data.has("homepage") && !data.get("homepage").equals(JSONObject.NULL) ? data.getString("homepage") : null;
        this.forksCount = data.getLong("forks_count");

        /* When Single Repository (or Owner of Repository) */
        this.allowRebaseMerge = data.has("allow_rebase_merge") && !data.get("allow_rebase_merge").equals(JSONObject.NULL) ? data.getBoolean("allow_rebase_merge") : null;
        this.tempCloneToken = data.has("temp_clone_token") && !data.get("temp_clone_token").equals(JSONObject.NULL) ? data.getString("temp_clone_token") : null;
        this.squashMergeCommitMessage = data.has("squash_merge_commit_message") && !data.get("squash_merge_commit_message").equals(JSONObject.NULL) ? data.getString("squash_merge_commit_message") : null;
        this.subscribersCount = data.has("subscribers_count") && !data.get("subscribers_count").equals(JSONObject.NULL) ? data.getLong("subscribers_count") : null;
        this.allowMergeCommit = data.has("allow_merge_commit") && !data.get("allow_merge_commit").equals(JSONObject.NULL) ? data.getBoolean("allow_merge_commit") : null;
        this.networkCount = data.has("network_count") && !data.get("network_count").equals(JSONObject.NULL) ? data.getLong("network_count") : null;
        this.allowAutoMerge = data.has("allow_auto_merge") && !data.get("allow_auto_merge").equals(JSONObject.NULL) ? data.getBoolean("allow_auto_merge") : null;
        this.mergeCommitTitle = data.has("merge_commit_title") && !data.get("merge_commit_title").equals(JSONObject.NULL) ? data.getString("merge_commit_title") : null;
        this.deleteBranchOnMerge = data.has("delete_branch_on_merge") && !data.get("delete_branch_on_merge").equals(JSONObject.NULL) ? data.getBoolean("delete_branch_on_merge") : null;
        this.allowSquashMerge = data.has("allow_squash_merge") && !data.get("allow_squash_merge").equals(JSONObject.NULL) ? data.getBoolean("allow_squash_merge") : null;
        this.allowUpdateBranch = data.has("allow_update_branch") && !data.get("allow_update_branch").equals(JSONObject.NULL) ? data.getBoolean("allow_update_branch") : null;
        this.contentsUrl = data.has("contents_url") && !data.get("contents_url").equals(JSONObject.NULL) ? data.getString("contents_url") : null;
        this.useSquashPrTitleAsDefault = data.has("use_squash_merge") && !data.get("use_squash_merge").equals(JSONObject.NULL) ? data.getBoolean("use_squash_merge") : null;
        this.mergeCommitMessage = data.has("merge_commit_message") && !data.get("merge_commit_message").equals(JSONObject.NULL) ? data.getString("merge_commit_message") : null;
        this.squashMergeCommitTitle = data.has("squash_merge_commit_title") && !data.get("squash_merge_commit_title").equals(JSONObject.NULL) ? data.getString("squash_merge_commit_title") : null;
    }

    public boolean isAllowForking() {
        return this.allowForking;
    }

    public long getStargazersCount() {
        return this.stargazersCount;
    }

    public boolean isTemplate() {
        return this.isTemplate;
    }

    public ZonedDateTime getPushedAt() {
        return this.pushedAt;
    }

    public String getSubscriptionUrl() {
        return this.subscriptionUrl;
    }

    @Nullable
    public String getLanguage() {
        return this.language;
    }

    public String getBranchesUrl() {
        return this.branchesUrl;
    }

    public String getIssueCommentUrl() {
        return this.issueCommentUrl;
    }

    @Nullable
    public Boolean getAllowRebaseMerge() {
        return this.allowRebaseMerge;
    }

    public String getLabelsUrl() {
        return this.labelsUrl;
    }

    public String getSubscribersUrl() {
        return this.subscribersUrl;
    }

    public Permissions getPermissions() {
        return this.permissions;
    }

    @Nullable
    public String getTempCloneToken() {
        return this.tempCloneToken;
    }

    public String getReleasesUrl() {
        return this.releasesUrl;
    }

    public String getSvnUrl() {
        return this.svnUrl;
    }

    @Nullable
    public String getSquashMergeCommitMessage() {
        return this.squashMergeCommitMessage;
    }

    @Nullable
    public Long getSubscribersCount() {
        return this.subscribersCount;
    }

    public long getId() {
        return this.id;
    }

    public boolean isHasDiscussions() {
        return this.hasDiscussions;
    }

    public long getForks() {
        return this.forks;
    }

    public String getArchiveUrl() {
        return this.archiveUrl;
    }

    @Nullable
    public Boolean getAllowMergeCommit() {
        return this.allowMergeCommit;
    }

    public String getGitRefsUrl() {
        return this.gitRefsUrl;
    }

    public String getForksUrl() {
        return this.forksUrl;
    }

    public String getVisibility() {
        return this.visibility;
    }

    public String getStatusesUrl() {
        return this.statusesUrl;
    }

    @Nullable
    public Long getNetworkCount() {
        return this.networkCount;
    }

    public String getSshUrl() {
        return this.sshUrl;
    }

    @Nullable
    public License getLicense() {
        return this.license;
    }

    public String getFullName() {
        return this.fullName;
    }

    public long getSize() {
        return this.size;
    }

    @Nullable
    public Boolean getAllowAutoMerge() {
        return this.allowAutoMerge;
    }

    public String getLanguagesUrl() {
        return this.languagesUrl;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getCollaboratorsUrl() {
        return this.collaboratorsUrl;
    }

    public String getCloneUrl() {
        return this.cloneUrl;
    }

    public String getName() {
        return this.name;
    }

    public String getPullsUrl() {
        return this.pullsUrl;
    }

    public String getDefaultBranch() {
        return this.defaultBranch;
    }

    public String getHooksUrl() {
        return this.hooksUrl;
    }

    public String getTreesUrl() {
        return this.treesUrl;
    }

    public String getTagsUrl() {
        return this.tagsUrl;
    }

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public String getContributorsUrl() {
        return this.contributorsUrl;
    }

    public boolean isHasDownloads() {
        return this.hasDownloads;
    }

    public String getNotificationsUrl() {
        return this.notificationsUrl;
    }

    public long getOpenIssuesCount() {
        return this.openIssuesCount;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public long getWatchers() {
        return this.watchers;
    }

    public String getKeysUrl() {
        return this.keysUrl;
    }

    public String getDeploymentsUrl() {
        return this.deploymentsUrl;
    }

    public boolean isHasProjects() {
        return this.hasProjects;
    }

    public boolean isArchived() {
        return this.archived;
    }

    public boolean isHasWiki() {
        return this.hasWiki;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Nullable
    public String getMergeCommitTitle() {
        return this.mergeCommitTitle;
    }

    public String getCommentsUrl() {
        return this.commentsUrl;
    }

    public String getStargazersUrl() {
        return this.stargazersUrl;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    @Nullable
    public Boolean getDeleteBranchOnMerge() {
        return this.deleteBranchOnMerge;
    }

    public String getGitUrl() {
        return this.gitUrl;
    }

    public boolean isHasPages() {
        return this.hasPages;
    }

    public SimpleUser getOwner() {
        return this.owner;
    }

    @Nullable
    public Boolean getAllowSquashMerge() {
        return this.allowSquashMerge;
    }

    public String getCommitsUrl() {
        return this.commitsUrl;
    }

    public String getCompareUrl() {
        return this.compareUrl;
    }

    public String getGitCommitsUrl() {
        return this.gitCommitsUrl;
    }

    public String[] getTopics() {
        return this.topics;
    }

    public String getBlobsUrl() {
        return this.blobsUrl;
    }

    @Nullable
    public Boolean getAllowUpdateBranch() {
        return this.allowUpdateBranch;
    }

    public String getGitTagsUrl() {
        return this.gitTagsUrl;
    }

    public String getMergesUrl() {
        return this.mergesUrl;
    }

    public String getDownloadsUrl() {
        return this.downloadsUrl;
    }

    public boolean isHasIssues() {
        return this.hasIssues;
    }

    public boolean isWebCommitSignOffRequired() {
        return this.webCommitSignOffRequired;
    }

    public String getUrl() {
        return this.url;
    }

    @Nullable
    public String getContentsUrl() {
        return this.contentsUrl;
    }

    @Nullable
    public String getMirrorUrl() {
        return this.mirrorUrl;
    }

    public String getMilestonesUrl() {
        return this.milestonesUrl;
    }

    public String getTeamsUrl() {
        return this.teamsUrl;
    }

    public boolean isFork() {
        return this.fork;
    }

    public String getIssuesUrl() {
        return this.issuesUrl;
    }

    public String getEventsUrl() {
        return this.eventsUrl;
    }

    @Nullable
    public Boolean getUseSquashPrTitleAsDefault() {
        return this.useSquashPrTitleAsDefault;
    }

    public String getIssueEventsUrl() {
        return this.issueEventsUrl;
    }

    @Nullable
    public String getMergeCommitMessage() {
        return this.mergeCommitMessage;
    }

    public String getAssigneesUrl() {
        return this.assigneesUrl;
    }

    public long getOpenIssues() {
        return this.openIssues;
    }

    @Nullable
    public String getSquashMergeCommitTitle() {
        return this.squashMergeCommitTitle;
    }

    public long getWatchersCount() {
        return this.watchersCount;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    @Nullable
    public String getHomepage() {
        return this.homepage;
    }

    public long getForksCount() {
        return this.forksCount;
    }

    @Nullable
    public Branch getBranch(String branchName) {
        return this.api.getBranch(this, branchName);
    }

    public List<Branch> getBranches() {
        return this.api.getBranches(this);
    }

    public static class Permissions {
        private final GitHubAPI api;
        private final boolean pull;
        private final boolean maintain;
        private final boolean admin;
        private final boolean triage;
        private final boolean push;

        public Permissions(GitHubAPI api, JSONObject data) {
            this.api = api;
            this.pull = data.getBoolean("pull");
            this.maintain = data.getBoolean("maintain");
            this.admin = data.getBoolean("admin");
            this.triage = data.getBoolean("triage");
            this.push = data.getBoolean("push");
        }

        public boolean canPull() {
            return this.pull;
        }

        public boolean canMaintain() {
            return this.maintain;
        }

        public boolean isAdmin() {
            return this.admin;
        }

        public boolean canTriage() {
            return this.triage;
        }

        public boolean canPush() {
            return this.push;
        }
    }

    public static class License {
        private final GitHubAPI api;
        private final String name;
        private final String spdxId;
        private final String key;
        @Nullable private final String url;
        private final String nodeId;

        public License(GitHubAPI api, JSONObject data) {
            this.api = api;
            this.name = data.getString("name");
            this.spdxId = data.getString("spdx_id");
            this.key = data.getString("key");
            this.url = data.has("url") && !data.get("url").equals(JSONObject.NULL) ? data.getString("url") : null;
            this.nodeId = data.getString("node_id");
        }

        public String getName() {
            return this.name;
        }

        public String getSpdxId() {
            return this.spdxId;
        }

        public String getKey() {
            return this.key;
        }

        @Nullable
        public String getUrl() {
            return this.url;
        }

        public String getNodeId() {
            return this.nodeId;
        }
    }
}
