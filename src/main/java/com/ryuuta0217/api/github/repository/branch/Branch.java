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

package com.ryuuta0217.api.github.repository.branch;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.commit.SimpleCommitImpl;
import com.ryuuta0217.api.github.repository.commit.interfaces.SimpleCommit;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.List;

public class Branch {
    private final GitHubAPI api;
    private final RepositoryMinimal repository;
    private final boolean isProtected;
    private final String name;
    private final SimpleCommit commit;
    @Nullable
    private final Protection protection;
    @Nullable
    private final String protectionUrl;

    public Branch(GitHubAPI api, RepositoryMinimal repository, JSONObject data) {
        this.api = api;
        this.repository = repository;
        this.isProtected = data.getBoolean("protected");
        this.name = data.getString("name");
        this.commit = new SimpleCommitImpl(api, this.repository, data.getJSONObject("commit"));
        this.protection = data.has("protection") && !data.isNull("protection") ? new Protection(api, this, data.getJSONObject("protection")) : null;
        this.protectionUrl = data.has("protection_url") && !data.isNull("protection_url") ? data.getString("protection_url") : null;
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    public String getName() {
        return this.name;
    }

    public SimpleCommit getCommit() {
        return this.commit;
    }

    @Nullable
    public Protection getProtection() {
        return this.protection;
    }

    @Nullable
    public String getProtectionUrl() {
        return this.protectionUrl;
    }

    public List<com.ryuuta0217.api.github.repository.commit.interfaces.Commit> getCommits() {
        return this.api.getCommits(this.repository.getOwner().getLogin(), this.repository, this.getName());
    }

    @Nullable
    public Branch tryFetchCompleteData() {
        return this.repository.getBranch(this.getName());
    }
}
