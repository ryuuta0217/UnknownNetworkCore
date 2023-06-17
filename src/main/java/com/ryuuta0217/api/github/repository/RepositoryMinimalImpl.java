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
import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.repository.interfaces.Repository;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;

public class RepositoryMinimalImpl implements RepositoryMinimal {
    protected final GitHubAPI api;

    private final String archiveUrl;
    private final String assigneesUrl;
    private final String blobsUrl;
    private final String branchesUrl;
    private final String collboratorsUrl;
    private final String commentsUrl;
    private final String commitsUrl;
    private final String compareUrl;
    private final String contentsUrl;
    private final String contributorsUrl;
    private final String deploymentsUrl;
    private final String description;
    private final String downloadsUrl;
    private final String eventsUrl;
    private final boolean fork;
    private final String forksUrl;
    private final String fullName;
    private final String gitCommitsUrl;
    private final String gitRefsUrl;
    private final String gitTagsUrl;
    private final String hooksUrl;
    private final String htmlUrl;
    private final long id;
    private final String nodeId;
    private final String issueCommentUrl;
    private final String issueEventsUrl;
    private final String issuesUrl;
    private final String keysUrl;
    private final String labelsUrl;
    private final String languagesUrl;
    private final String mergesUrl;
    private final String milestonesUrl;
    private final String name;
    private final String notificationsUrl;
    private final SimpleUser owner;
    private final boolean isPrivate;
    private final String pullsUrl;
    private final String releasesUrl;
    private final String stargazersUrl;
    private final String statusesUrl;
    private final String subscribersUrl;
    private final String subscriptionUrl;
    private final String tagsUrl;
    private final String teamsUrl;
    private final String treesUrl;
    private final String url;


    @Nullable private final String gitUrl;
    @Nullable private final String sshUrl;
    @Nullable private final String cloneUrl;
    @Nullable private final String mirrorUrl;
    @Nullable private final String svnUrl;
    @Nullable private final String homepage;
    @Nullable private final String language;
    private final long forksCount; // when null, -1
    private final long stargazersCount; // when null, -1
    private final long watchersCount; // when null, -1
    private final long size; // when null, -1
    @Nullable private final String defaultBranch;
    private final long openIssuesCount; // when null, -1
    private final boolean isTemplate; // when null, false
    @Nullable private final String[] topics;
    private final boolean hasIssues; // when null, false
    private final boolean hasProjects; // when null, false
    private final boolean hasWiki; // when null, false
    private final boolean hasPages; // when null, false
    private final boolean hasDownloads; // when null, false
    private final boolean hasDiscussions; // when null, false
    private final boolean archived; // when null, false
    private final boolean disabled; // when null, false
    @Nullable private final String visibility;
    @Nullable private final ZonedDateTime pushedAt;
    @Nullable private final ZonedDateTime createdAt;
    @Nullable private final ZonedDateTime updatedAt;
    @Nullable private final Permissions permissions;
    @Nullable private final String roleName;
    @Nullable private final String tempCloneToken;
    private final boolean deleteBranchOnMerge; // when null, false
    private final long subscribersCount; // when null, -1
    private final long networkCount; // when null, -1
    @Nullable private final CodeOfConduct codeOfConduct;
    @Nullable private final License license;
    private final long forks; // when null, -1
    private final long openIssues; // when null, -1
    private final long watchers; // when null, -1
    private final boolean allowForking; // when null, false
    private final boolean webCommitSignOffRequired; // when null, false
    @Nullable private final SecurityAndAnalysis securityAndAnalysis;

    public RepositoryMinimalImpl(GitHubAPI api, JSONObject data) {
        this.api = api;

        this.archiveUrl = data.getString("archive_url");
        this.assigneesUrl = data.getString("assignees_url");
        this.blobsUrl = data.getString("blobs_url");
        this.branchesUrl = data.getString("branches_url");
        this.collboratorsUrl = data.getString("collaborators_url");
        this.commentsUrl = data.getString("comments_url");
        this.commitsUrl = data.getString("commits_url");
        this.compareUrl = data.getString("compare_url");
        this.contentsUrl = data.getString("contents_url");
        this.contributorsUrl = data.getString("contributors_url");
        this.deploymentsUrl = data.getString("deployments_url");
        this.description = data.has("description") && !data.isNull("description") ? data.getString("description") : null;
        this.downloadsUrl = data.getString("downloads_url");
        this.eventsUrl = data.getString("events_url");
        this.fork = data.getBoolean("fork");
        this.forksUrl = data.getString("forks_url");
        this.fullName = data.getString("full_name");
        this.gitCommitsUrl = data.getString("git_commits_url");
        this.gitRefsUrl = data.getString("git_refs_url");
        this.gitTagsUrl = data.getString("git_tags_url");
        this.hooksUrl = data.getString("hooks_url");
        this.htmlUrl = data.getString("html_url");
        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.issueCommentUrl = data.getString("issue_comment_url");
        this.issueEventsUrl = data.getString("issue_events_url");
        this.issuesUrl = data.getString("issues_url");
        this.keysUrl = data.getString("keys_url");
        this.labelsUrl = data.getString("labels_url");
        this.languagesUrl = data.getString("languages_url");
        this.mergesUrl = data.getString("merges_url");
        this.milestonesUrl = data.getString("milestones_url");
        this.name = data.getString("name");
        this.notificationsUrl = data.getString("notifications_url");
        this.owner = new SimpleUserImpl(api, data.getJSONObject("owner"));
        this.isPrivate = data.getBoolean("private");
        this.pullsUrl = data.getString("pulls_url");
        this.releasesUrl = data.getString("releases_url");
        this.stargazersUrl = data.getString("stargazers_url");
        this.statusesUrl = data.getString("statuses_url");
        this.subscribersUrl = data.getString("subscribers_url");
        this.subscriptionUrl = data.getString("subscription_url");
        this.tagsUrl = data.getString("tags_url");
        this.teamsUrl = data.getString("teams_url");
        this.treesUrl = data.getString("trees_url");
        this.url = data.getString("url");

        this.gitUrl = data.has("git_url") && !data.isNull("git_url") ? data.getString("git_url") : null;
        this.sshUrl = data.has("ssh_url") && !data.isNull("ssh_url") ? data.getString("ssh_url") : null;
        this.cloneUrl = data.has("clone_url") && !data.isNull("clone_url") ? data.getString("clone_url") : null;
        this.mirrorUrl = data.has("mirror_url") && !data.isNull("mirror_url") ? data.getString("mirror_url") : null;
        this.svnUrl = data.has("svn_url") && !data.isNull("svn_url") ? data.getString("svn_url") : null;
        this.homepage = data.has("homepage") && !data.isNull("homepage") ? data.getString("homepage") : null;
        this.language = data.has("language") && !data.isNull("language") ? data.getString("language") : null;
        this.forksCount = data.has("forks_count") && !data.isNull("forks_count") ? data.getLong("forks_count") : -1;
        this.stargazersCount = data.has("stargazers_count") && !data.isNull("stargazers_count") ? data.getLong("stargazers_count") : -1;
        this.watchersCount = data.has("watchers_count") && !data.isNull("watchers_count") ? data.getLong("watchers_count") : -1;
        this.size = data.has("size") && !data.isNull("size") ? data.getLong("size") : -1;
        this.defaultBranch = data.has("default_branch") && !data.isNull("default_branch") ? data.getString("default_branch") : null;
        this.openIssuesCount = data.has("open_issues_count") && !data.isNull("open_issues_count") ? data.getLong("open_issues_count") : -1;
        this.isTemplate = data.has("is_template") && !data.isNull("is_template") && data.getBoolean("is_template");
        this.topics = data.has("topics") && !data.isNull("topics") ? data.getJSONArray("topics").toList().stream().filter(raw -> raw instanceof String).map(String::valueOf).toArray(String[]::new) : null;
        this.hasIssues = data.has("has_issues") && !data.isNull("has_issues") && data.getBoolean("has_issues");
        this.hasProjects = data.has("has_projects") && !data.isNull("has_projects") && data.getBoolean("has_projects");
        this.hasWiki = data.has("has_wiki") && !data.isNull("has_wiki") && data.getBoolean("has_wiki");
        this.hasPages = data.has("has_pages") && !data.isNull("has_pages") && data.getBoolean("has_pages");
        this.hasDownloads = data.has("has_downloads") && !data.isNull("has_downloads") && data.getBoolean("has_downloads");
        this.hasDiscussions = data.has("has_discussions") && !data.isNull("has_discussions") && data.getBoolean("has_discussions");
        this.archived = data.has("archived") && !data.isNull("archived") && data.getBoolean("archived");
        this.disabled = data.has("disabled") && !data.isNull("disabled") && data.getBoolean("disabled");
        this.visibility = data.has("visibility") && !data.isNull("visibility") ? data.getString("visibility") : null;
        this.pushedAt = data.has("pushed_at") && !data.isNull("pushed_at") ? ZonedDateTime.parse(data.getString("pushed_at")) : null;
        this.createdAt = data.has("created_at") && !data.isNull("created_at") ? ZonedDateTime.parse(data.getString("created_at")) : null;
        this.updatedAt = data.has("updated_at") && !data.isNull("updated_at") ? ZonedDateTime.parse(data.getString("updated_at")) : null;
        this.permissions = data.has("permissions") && !data.isNull("permissions") ? new Permissions(api, data.getJSONObject("permissions")) : null;
        this.roleName = data.has("role") && !data.isNull("role") ? data.getString("role") : null;
        this.tempCloneToken = data.has("temp_clone_token") && !data.isNull("temp_clone_token") ? data.getString("temp_clone_token") : null;
        this.deleteBranchOnMerge = data.has("delete_branch_on_merge") && !data.isNull("delete_branch_on_merge") && data.getBoolean("delete_branch_on_merge");
        this.subscribersCount = data.has("subscribers_count") && !data.isNull("subscribers_count") ? data.getLong("subscribers_count") : -1;
        this.networkCount = data.has("network_count") && !data.isNull("network_count") ? data.getLong("network_count") : -1;
        this.codeOfConduct = data.has("code_of_conduct") && !data.isNull("code_of_conduct") ? new CodeOfConduct(api, this, data.getJSONObject("code_of_conduct")) : null;
        this.license = data.has("license") && !data.isNull("license") ? new License(api, this, data.getJSONObject("license")) : null;
        this.forks = data.has("forks") && !data.isNull("forks") ? data.getLong("forks") : -1;
        this.openIssues = data.has("open_issues") && !data.isNull("open_issues") ? data.getLong("open_issues") : -1;
        this.watchers = data.has("watchers") && !data.isNull("watchers") ? data.getLong("watchers") : -1;
        this.allowForking = data.has("allow_forking") && !data.isNull("allow_forking") && data.getBoolean("allow_forking");
        this.webCommitSignOffRequired = data.has("web_commit_signoff_required") && !data.isNull("web_commit_signoff_required") && data.getBoolean("web_commit_signoff_required");
        this.securityAndAnalysis = data.has("security_and_analysis") && !data.isNull("security_and_analysis") ? new SecurityAndAnalysis(api, this, data.getJSONObject("security_and_analysis")) : null;
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
    public String getName() {
        return this.name;
    }

    @Override
    public String getFullName() {
        return this.fullName;
    }

    @Override
    public SimpleUser getOwner() {
        return this.owner;
    }

    @Override
    public boolean isPrivate() {
        return this.isPrivate;
    }

    @Override
    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    @Nullable
    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean isFork() {
        return this.fork;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public String getArchiveUrl() {
        return this.archiveUrl;
    }

    @Override
    public String getAssigneesUrl() {
        return this.assigneesUrl;
    }

    @Override
    public String getBlobsUrl() {
        return this.blobsUrl;
    }

    @Override
    public String getBranchesUrl() {
        return this.branchesUrl;
    }

    @Override
    public String getCollaboratorsUrl() {
        return this.collboratorsUrl;
    }

    @Override
    public String getCommentsUrl() {
        return this.commentsUrl;
    }

    @Override
    public String getCommitsUrl() {
        return commitsUrl;
    }

    @Override
    public String getCompareUrl() {
        return this.compareUrl;
    }

    @Override
    public String getContentsUrl() {
        return this.contentsUrl;
    }

    @Override
    public String getContributorsUrl() {
        return this.contributorsUrl;
    }

    @Override
    public String getDeploymentsUrl() {
        return this.deploymentsUrl;
    }

    @Override
    public String getDownloadsUrl() {
        return this.downloadsUrl;
    }

    @Override
    public String getEventsUrl() {
        return this.eventsUrl;
    }

    @Override
    public String getForksUrl() {
        return this.forksUrl;
    }

    @Override
    public String getGitCommitsUrl() {
        return this.gitCommitsUrl;
    }

    @Override
    public String getGitRefsUrl() {
        return this.gitRefsUrl;
    }

    @Override
    public String getGitTagsUrl() {
        return this.gitTagsUrl;
    }

    @Override
    public String getGitUrl() {
        return this.gitUrl;
    }

    @Override
    public String getIssueCommentUrl() {
        return this.issueCommentUrl;
    }

    @Override
    public String getIssueEventsUrl() {
        return this.issueEventsUrl;
    }

    @Override
    public String getIssuesUrl() {
        return this.issuesUrl;
    }

    @Override
    public String getKeysUrl() {
        return this.keysUrl;
    }

    @Override
    public String getLabelsUrl() {
        return this.labelsUrl;
    }

    @Override
    public String getLanguagesUrl() {
        return this.languagesUrl;
    }

    @Override
    public String getMergesUrl() {
        return this.mergesUrl;
    }

    @Override
    public String getMilestonesUrl() {
        return this.milestonesUrl;
    }

    @Override
    public String getNotificationsUrl() {
        return this.notificationsUrl;
    }

    @Override
    public String getPullsUrl() {
        return this.pullsUrl;
    }

    @Override
    public String getReleasesUrl() {
        return this.releasesUrl;
    }

    @Override
    public String getSshUrl() {
        return this.sshUrl;
    }

    @Override
    public String getStargazersUrl() {
        return this.stargazersUrl;
    }

    @Override
    public String getStatusesUrl() {
        return this.statusesUrl;
    }

    @Override
    public String getSubscribersUrl() {
        return this.subscribersUrl;
    }

    @Override
    public String getSubscriptionUrl() {
        return this.subscriptionUrl;
    }

    @Override
    public String getTagsUrl() {
        return this.tagsUrl;
    }

    @Override
    public String getTeamsUrl() {
        return this.teamsUrl;
    }

    @Override
    public String getTreesUrl() {
        return this.treesUrl;
    }

    @Override
    public String getCloneUrl() {
        return this.cloneUrl;
    }

    @Nullable
    @Override
    public String getMirrorUrl() {
        return this.mirrorUrl;
    }

    @Override
    public String getHooksUrl() {
        return this.hooksUrl;
    }

    @Override
    public String getSvnUrl() {
        return this.svnUrl;
    }

    @Nullable
    @Override
    public String getHomepage() {
        return this.homepage;
    }

    @Nullable
    @Override
    public String getLanguage() {
        return this.language;
    }

    @Override
    public long getForksCount() {
        return this.forksCount;
    }

    @Override
    public long getStargazersCount() {
        return this.stargazersCount;
    }

    @Override
    public long getWatchersCount() {
        return this.watchersCount;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public String getDefaultBranch() {
        return this.defaultBranch;
    }

    @Override
    public long getOpenIssuesCount() {
        return this.openIssuesCount;
    }

    @Override
    public boolean isTemplate() {
        return this.isTemplate;
    }

    @Override
    public String[] getTopics() {
        return this.topics;
    }

    @Override
    public boolean hasIssues() {
        return this.hasIssues;
    }

    @Override
    public boolean hasProjects() {
        return this.hasProjects;
    }

    @Override
    public boolean hasWiki() {
        return this.hasWiki;
    }

    @Override
    public boolean hasPages() {
        return this.hasPages;
    }

    @Override
    public boolean hasDownloads() {
        return this.hasDownloads;
    }

    @Override
    public boolean hasDiscussions() {
        return this.hasDiscussions;
    }

    @Override
    public boolean isArchived() {
        return this.archived;
    }

    @Override
    public boolean isDisabled() {
        return this.disabled;
    }

    @Override
    public String getVisibility() {
        return this.visibility;
    }

    @Nullable
    @Override
    public ZonedDateTime getPushedAt() {
        return this.pushedAt;
    }

    @Nullable
    @Override
    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Nullable
    @Override
    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public Permissions getPermissions() {
        return this.permissions;
    }

    @Override
    public String getRoleName() {
        return this.roleName;
    }

    @Override
    public String getTempCloneToken() {
        return this.tempCloneToken;
    }

    @Override
    public boolean isDeleteBranchOnMerge() {
        return deleteBranchOnMerge;
    }

    @Override
    public long getSubscribersCount() {
        return this.subscribersCount;
    }

    @Override
    public long getNetworkCount() {
        return this.networkCount;
    }

    @Override
    public CodeOfConduct getCodeOfConduct() {
        return this.codeOfConduct;
    }

    @Nullable
    @Override
    public License getLicense() {
        return this.license;
    }

    @Override
    public long getForks() {
        return this.forks;
    }

    @Override
    public long getOpenIssues() {
        return this.openIssues;
    }

    @Override
    public long getWatchers() {
        return this.watchers;
    }

    @Override
    public boolean isAllowForking() {
        return this.allowForking;
    }

    @Override
    public boolean isWebCommitSignOffRequired() {
        return this.webCommitSignOffRequired;
    }

    @Nullable
    @Override
    public SecurityAndAnalysis getSecurityAndAnalysis() {
        return this.securityAndAnalysis;
    }

    @Override
    public Repository tryGetRepository() {
        return this.api.getRepository(this.getOwner().getLogin(), this.getName());
    }


    @Nullable
    @Override
    public Branch getBranch(String branchName) {
        return this.api.getBranch(this, branchName);
    }

    public List<Branch> getBranches() {
        return this.api.getBranches(this);
    }
}
