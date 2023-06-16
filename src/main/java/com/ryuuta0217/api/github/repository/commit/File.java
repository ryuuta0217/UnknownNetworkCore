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
