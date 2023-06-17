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

package com.ryuuta0217.api.github.repository.interfaces;

import com.ryuuta0217.api.github.repository.CodeOfConduct;
import com.ryuuta0217.api.github.repository.License;
import com.ryuuta0217.api.github.repository.Permissions;
import com.ryuuta0217.api.github.repository.SecurityAndAnalysis;
import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;

public interface RepositoryMinimal {
    long getId();
    String getNodeId();
    String getName();
    String getFullName();
    SimpleUser getOwner();
    boolean isPrivate();
    String getHtmlUrl();
    @Nullable String getDescription();
    boolean isFork();
    String getUrl();
    String getArchiveUrl();
    String getAssigneesUrl();
    String getBlobsUrl();
    String getBranchesUrl();
    String getCollaboratorsUrl();
    String getCommentsUrl();
    String getCommitsUrl();
    String getCompareUrl();
    String getContentsUrl();
    String getContributorsUrl();
    String getDeploymentsUrl();
    String getDownloadsUrl();
    String getEventsUrl();
    String getForksUrl();
    String getGitCommitsUrl();
    String getGitRefsUrl();
    String getGitTagsUrl();
    String getGitUrl();
    String getIssueCommentUrl();
    String getIssueEventsUrl();
    String getIssuesUrl();
    String getKeysUrl();
    String getLabelsUrl();
    String getLanguagesUrl();
    String getMergesUrl();
    String getMilestonesUrl();
    String getNotificationsUrl();
    String getPullsUrl();
    String getReleasesUrl();
    String getSshUrl();
    String getStargazersUrl();
    String getStatusesUrl();
    String getSubscribersUrl();
    String getSubscriptionUrl();
    String getTagsUrl();
    String getTeamsUrl();
    String getTreesUrl();
    String getCloneUrl();
    @Nullable String getMirrorUrl();
    String getHooksUrl();
    String getSvnUrl();
    @Nullable String getHomepage();
    @Nullable String getLanguage();
    long getForksCount();
    long getStargazersCount();
    long getWatchersCount();
    long getSize();
    String getDefaultBranch();
    long getOpenIssuesCount();
    boolean isTemplate();
    String[] getTopics();
    boolean hasIssues();
    boolean hasProjects();
    boolean hasWiki();
    boolean hasPages();
    boolean hasDownloads();
    boolean hasDiscussions();
    boolean isArchived();
    boolean isDisabled();
    String getVisibility();
    @Nullable ZonedDateTime getPushedAt();
    @Nullable ZonedDateTime getCreatedAt();
    @Nullable ZonedDateTime getUpdatedAt();
    Permissions getPermissions();
    String getRoleName();
    String getTempCloneToken();
    boolean isDeleteBranchOnMerge();
    long getSubscribersCount();
    long getNetworkCount();
    CodeOfConduct getCodeOfConduct();
    @Nullable License getLicense();
    long getForks();
    long getOpenIssues();
    long getWatchers();
    boolean isAllowForking();
    boolean isWebCommitSignOffRequired();
    @Nullable
    SecurityAndAnalysis getSecurityAndAnalysis();
    Repository tryGetRepository();


    @Nullable
    Branch getBranch(String branchName);
    List<Branch> getBranches();
}
