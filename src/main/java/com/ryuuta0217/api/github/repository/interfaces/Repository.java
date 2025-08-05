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
