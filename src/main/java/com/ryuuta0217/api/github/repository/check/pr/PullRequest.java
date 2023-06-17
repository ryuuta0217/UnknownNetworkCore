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
import com.ryuuta0217.api.github.repository.check.CheckRun;
import org.json.JSONObject;

public class PullRequest {
    private final GitHubAPI api;
    private final CheckRun checkRun;
    private final long id;
    private final long number;
    private final String url;
    private final BranchPointer head;
    private final BranchPointer base;

    public PullRequest(GitHubAPI api, CheckRun checkRun, JSONObject data) {
        this.api = api;
        this.checkRun = checkRun;

        this.id = data.getLong("id");
        this.number = data.getLong("number");
        this.url = data.getString("url");
        this.head = new BranchPointer(api, this, data.getJSONObject("head"));
        this.base = new BranchPointer(api, this, data.getJSONObject("base"));
    }

    public CheckRun getCheckRun() {
        return this.checkRun;
    }

    public long getId() {
        return this.id;
    }

    public long getNumber() {
        return this.number;
    }

    public String getUrl() {
        return this.url;
    }

    public BranchPointer getHead() {
        return this.head;
    }

    public BranchPointer getBase() {
        return this.base;
    }
}
