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

import com.ryuuta0217.api.github.repository.commit.CompareResult;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.user.interfaces.GitUser;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public interface SimpleCommit {
    RepositoryMinimal getRepository();

    @Deprecated
    String getId();

    String getSha();

    String getTreeId();

    String getMessage();

    ZonedDateTime getTimestamp();

    @Nullable
    GitUser getAuthor();

    @Nullable
    GitUser getCommitter();

    @Nullable
    Commit tryGetCommit();

    CompareResult compare(SimpleCommit head);
}
