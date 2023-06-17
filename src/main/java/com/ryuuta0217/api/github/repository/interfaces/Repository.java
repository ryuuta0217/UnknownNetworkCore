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

import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;

import javax.annotation.Nullable;
import java.util.List;

public interface Repository extends RepositoryMinimal {
    boolean isAllowRebaseMerge();
    @Nullable Repository getTemplateRepository();
    @Nullable String getTempCloneToken();
    boolean isAllowSquashMerge();
    boolean isAllowAutoMerge();
    boolean isDeleteBranchOnMerge();
    boolean isAllowMergeCommit();
    boolean isAllowUpdateBranch();
    boolean useSquashPrTitleAsDefault();
    String getSquashMergeCommitTitle();
    String getSquashMergeCommitMessage();
    String getMergeCommitTitle();
    String getMergeCommitMessage();
    @Nullable SimpleUser getOrganization();
    @Nullable Repository getParent();
    @Nullable Repository getSource();
    String getMaterBranch();
    boolean isAnonymousAccessEnabled();

    @Override
    default Repository tryGetRepository() { return this; }
}
