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
