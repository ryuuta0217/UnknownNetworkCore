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

package com.ryuuta0217.api.github.repository.commit;

import com.ryuuta0217.api.github.GitHubAPI;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class File {
    private final GitHubAPI api;
    private final String fileName;
    private final long additions;
    private final long deletions;
    private final long changes;
    private final String sha;
    @Nullable
    private final String blobUrl;
    @Nullable
    private final String rawUrl;
    private final Status status;
    private final String contentsUrl;

    public File(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.fileName = data.getString("filename");
        this.additions = data.getLong("additions");
        this.deletions = data.getLong("deletions");
        this.changes = data.getLong("changes");
        this.sha = data.getString("sha");
        this.blobUrl = data.has("blob_url") ? data.getString("blob_url") : null;
        this.rawUrl = data.has("raw_url") ? data.getString("raw_url") : null;
        this.status = Status.valueOf(data.getString("status").toUpperCase());
        this.contentsUrl = data.getString("contents_url");
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getAdditions() {
        return this.additions;
    }

    public long getDeletions() {
        return this.deletions;
    }

    public long getChanges() {
        return this.changes;
    }

    public String getSha() {
        return this.sha;
    }

    @Nullable
    public String getBlobUrl() {
        return this.blobUrl;
    }

    @Nullable
    public String getRawUrl() {
        return this.rawUrl;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getContentsUrl() {
        return this.contentsUrl;
    }

    public enum Status {
        ADDED, REMOVED, MODIFIED, RENAMED, COPIED, CHANGED, UNCHANGED
    }
}
