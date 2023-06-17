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

package net.unknown.shared;

import com.ryuuta0217.api.github.repository.commit.CompareResult;
import net.unknown.UnknownNetworkCore;
import net.unknown.shared.util.UpdateUtil;

import javax.annotation.Nullable;

public record VersionInfo(String gitBranch, String commitSha) {
    public static VersionInfo parseFromString(String version) {
        String[] split = version.split("-", 4);
        if (split.length != 4) return null;
        String gitBranch = split[2];
        String commitSha = split[3];
        return new VersionInfo(gitBranch, commitSha);
    }

    public boolean diffVersion(VersionInfo other) {
        return this.sameBranch(other) && this.diffCommit(other);
    }

    public boolean sameBranch(VersionInfo other) {
        return gitBranch.equals(other.gitBranch);
    }

    public boolean diffBranch(VersionInfo other) {
        return !gitBranch.equals(other.gitBranch);
    }

    public boolean sameCommit(VersionInfo other) {
        return commitSha.equals(other.commitSha);
    }

    public boolean diffCommit(VersionInfo other) {
        return !commitSha.equals(other.commitSha);
    }

    @Nullable
    public CompareResult compareLatest() {
        if (UpdateUtil.GITHUB_API != null) {
            return UpdateUtil.GITHUB_API.getCompareResult(UpdateUtil.GITHUB_API.getRepository("ryuuta0217", "UnknownNetworkCore"), this.commitSha(), this.gitBranch());
        }
        return null;
    }
}
