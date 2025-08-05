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

package com.ryuuta0217.api.github.repository.commit.interfaces;

import com.ryuuta0217.api.github.repository.check.CheckRun;
import com.ryuuta0217.api.github.repository.commit.File;
import com.ryuuta0217.api.github.repository.commit.GitCommit;
import com.ryuuta0217.api.github.repository.commit.Parent;
import com.ryuuta0217.api.github.repository.commit.Stats;
import com.ryuuta0217.api.github.user.interfaces.GitUser;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;

import javax.annotation.Nullable;
import java.util.List;

public interface Commit extends SimpleCommit {
    @Deprecated
    GitUser getCommitter();

    SimpleUser getCommitterUser();

    @Nullable
    Stats getStats();

    @Deprecated
    GitUser getAuthor();

    SimpleUser getAuthorUser();

    String getHtmlUrl();

    GitCommit getCommit();

    String getCommentsUrl();

    @Nullable
    File[] getFiles();

    String getUrl();

    String getNodeId();

    Parent[] getParents();

    List<CheckRun> tryGetCheckRuns();
}
