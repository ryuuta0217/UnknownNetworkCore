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

public class Stats {
    private final GitHubAPI api;
    private final long total;
    private final long additions;
    private final long deletions;

    public Stats(GitHubAPI api, long total, long additions, long deletions) {
        this.api = api;
        this.total = total;
        this.additions = additions;
        this.deletions = deletions;
    }

    public Stats(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.total = data.getLong("total");
        this.additions = data.getLong("additions");
        this.deletions = data.getLong("deletions");
    }

    public long getTotal() {
        return this.total;
    }

    public long getAdditions() {
        return this.additions;
    }

    public long getDeletions() {
        return this.deletions;
    }
}
