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

package com.ryuuta0217.api.github.repository.check;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class Output {
    @Nullable
    private final String title;
    @Nullable
    private final String summary;
    @Nullable
    private final String text;
    private final long annotationsCount;
    private final String annotationsUrl;
    private final GitHubAPI api;
    private final CheckRun checkRun;

    public Output(GitHubAPI api, CheckRun checkRun, JSONObject data) {
        this.api = api;
        this.checkRun = checkRun;

        this.title = data.has("title") && !data.isNull("title") ? data.getString("title") : null;
        this.summary = data.has("summary") && !data.isNull("summary") ? data.getString("summary") : null;
        this.text = data.has("text") && !data.isNull("text") ? data.getString("text") : null;
        this.annotationsCount = data.has("annotations_count") ? data.getLong("annotations_count") : 0;
        this.annotationsUrl = data.has("annotations_url") ? data.getString("annotations_url") : null;
    }

    public CheckRun getCheckRun() {
        return this.checkRun;
    }

    @Nullable
    public String getTitle() {
        return this.title;
    }

    @Nullable
    public String getSummary() {
        return this.summary;
    }

    @Nullable
    public String getText() {
        return this.text;
    }

    public long getAnnotationsCount() {
        return this.annotationsCount;
    }

    public String getAnnotationsUrl() {
        return this.annotationsUrl;
    }
}
