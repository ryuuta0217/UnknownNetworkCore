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
