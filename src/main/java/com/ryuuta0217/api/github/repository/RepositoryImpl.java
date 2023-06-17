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
import com.ryuuta0217.api.github.repository.interfaces.Repository;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class RepositoryImpl extends RepositoryMinimalImpl implements Repository {
    private final boolean allowRebaseMerge;
    private final Repository templateRepository;
    private final boolean allowSquashMerge;
    private final boolean allowAutoMerge;
    private final boolean allowMergeCommit;
    private final boolean allowUpdateBranch;
    private final boolean useSquashPrTitleAsDefault;
    private final String squashMergeCommitTitle;
    private final String squashMergeCommitMessage;
    private final String mergeCommitTitle;
    private final String mergeCommitMessage;
    @Nullable private final SimpleUser organization;
    @Nullable private final Repository parent;
    @Nullable private final Repository source;
    private final String masterBranch;
    private final boolean anonymousAccessEnabled;


    public RepositoryImpl(GitHubAPI api, JSONObject data) {
        super(api, data);

        this.allowRebaseMerge = data.has("allow_rebase_merge") && !data.isNull("allow_rebase_merge") && data.getBoolean("allow_rebase_merge");
        this.templateRepository = data.has("template_repository") && !data.isNull("template_repository") ? new RepositoryImpl(api, data.getJSONObject("template_repository")) : null;
        this.allowSquashMerge = data.has("allow_squash_merge") && !data.isNull("allow_squash_merge") && data.getBoolean("allow_squash_merge");
        this.allowAutoMerge = data.has("allow_auto_merge") && !data.isNull("allow_auto_merge") && data.getBoolean("allow_auto_merge");
        this.allowMergeCommit = data.has("allow_merge_commit") && !data.isNull("allow_merge_commit") && data.getBoolean("allow_merge_commit");
        this.allowUpdateBranch = data.has("allow_update_branch") && !data.isNull("allow_update_branch") && data.getBoolean("allow_update_branch");
        this.useSquashPrTitleAsDefault = data.has("use_squash_merge_commit_title") && !data.isNull("use_squash_merge_commit_title") && data.getBoolean("use_squash_merge_commit_title");
        this.squashMergeCommitTitle = data.has("squash_merge_commit_title") && !data.isNull("squash_merge_commit_title") ? data.getString("squash_merge_commit_title") : null;
        this.squashMergeCommitMessage = data.has("squash_merge_commit_message") && !data.isNull("squash_merge_commit_message") ? data.getString("squash_merge_commit_message") : null;
        this.mergeCommitTitle = data.has("merge_commit_title") && !data.isNull("merge_commit_title") ? data.getString("merge_commit_title") : null;
        this.mergeCommitMessage = data.has("merge_commit_message") && !data.isNull("merge_commit_message") ? data.getString("merge_commit_message") : null;
        this.organization = data.has("organization") && !data.isNull("organization") ? new SimpleUserImpl(api, data.getJSONObject("organization")) : null;
        this.parent = data.has("parent") && !data.isNull("parent") ? new RepositoryImpl(api, data.getJSONObject("parent")) : null;
        this.source = data.has("source") && !data.isNull("source") ? new RepositoryImpl(api, data.getJSONObject("source")) : null;
        this.masterBranch = data.has("master_branch") && !data.isNull("master_branch") ? data.getString("master_branch") : null;
        this.anonymousAccessEnabled = data.has("anonymous_access_enabled") && !data.isNull("anonymous_access_enabled") && data.getBoolean("anonymous_access_enabled");
    }

    @Override
    public boolean isAllowRebaseMerge() {
        return this.allowRebaseMerge;
    }

    @Nullable
    @Override
    public Repository getTemplateRepository() {
        return this.templateRepository;
    }

    @Override
    public boolean isAllowSquashMerge() {
        return this.allowSquashMerge;
    }

    @Override
    public boolean isAllowAutoMerge() {
        return this.allowAutoMerge;
    }

    @Override
    public boolean isAllowMergeCommit() {
        return this.allowMergeCommit;
    }

    @Override
    public boolean isAllowUpdateBranch() {
        return this.allowUpdateBranch;
    }

    @Override
    public boolean useSquashPrTitleAsDefault() {
        return this.useSquashPrTitleAsDefault;
    }

    @Override
    public String getSquashMergeCommitTitle() {
        return this.squashMergeCommitTitle;
    }

    @Override
    public String getSquashMergeCommitMessage() {
        return this.squashMergeCommitMessage;
    }

    @Override
    public String getMergeCommitTitle() {
        return this.mergeCommitTitle;
    }

    @Override
    public String getMergeCommitMessage() {
        return this.mergeCommitMessage;
    }

    @Nullable
    @Override
    public SimpleUser getOrganization() {
        return this.organization;
    }

    @Nullable
    @Override
    public Repository getParent() {
        return this.parent;
    }

    @Nullable
    @Override
    public Repository getSource() {
        return this.source;
    }

    @Override
    public String getMaterBranch() {
        return this.masterBranch;
    }

    @Override
    public boolean isAnonymousAccessEnabled() {
        return this.anonymousAccessEnabled;
    }
}
