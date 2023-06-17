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

package com.ryuuta0217.api.github.repository.check.pr;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

public class BranchPointer {
    private final GitHubAPI api;
    private final PullRequest pullRequest;

    private final String ref;
    private final String sha;
    private final Repository repo;

    public BranchPointer(GitHubAPI api, PullRequest pullRequest, JSONObject data) {
        this.api = api;
        this.pullRequest = pullRequest;

        this.ref = data.getString("ref");
        this.sha = data.getString("sha");
        this.repo = new Repository(api, pullRequest, data.getJSONObject("repo"));
    }

    public PullRequest getPullRequest() {
        return this.pullRequest;
    }

    public String getRef() {
        return this.ref;
    }

    public String getSha() {
        return this.sha;
    }

    public Repository getRepo() {
        return this.repo;
    }
}