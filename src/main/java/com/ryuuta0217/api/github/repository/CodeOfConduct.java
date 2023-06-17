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
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class CodeOfConduct {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;

    private final String url;
    @Nullable private final String htmlUrl;
    private final String key;
    private final String name;

    @Nullable private final String body;

    public CodeOfConduct(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;

        this.url = data.getString("url");
        this.htmlUrl = data.has("html_url") && !data.isNull("html_url") ? data.getString("html_url") : null;
        this.key = data.getString("key");
        this.name = data.getString("name");

        this.body = data.has("body") && !data.isNull("body") ? data.getString("body") : null;
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public String getUrl() {
        return this.url;
    }

    @Nullable
    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public String getBody() {
        return this.body;
    }
}
