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

package com.ryuuta0217.api.github.user;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.user.interfaces.GitUser;
import com.ryuuta0217.api.github.user.interfaces.PublicUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class GitUserImpl implements GitUser {
    protected final GitHubAPI api;
    @Nullable
    private final String name;
    private final String email;
    @Nullable
    private final ZonedDateTime date;

    public GitUserImpl(GitHubAPI api, String name, String email, ZonedDateTime date) {
        this.api = api;
        this.name = name;
        this.email = email;
        this.date = date;
    }

    public GitUserImpl(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.name = data.getString("name");
        this.email = data.getString("email");
        this.date = data.has("date") && !data.isNull("date") ? ZonedDateTime.parse(data.getString("date")) : null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public ZonedDateTime getDate() {
        return this.date;
    }

    @Nullable
    @Override
    public PublicUser tryGetUser() {
        if (!this.getEmail().endsWith("@users.noreply.github.com")) return null;
        String userNameRaw = this.getEmail().substring(0, this.getEmail().indexOf('@'));
        String[] userNameParts = userNameRaw.split("\\+", 2);
        String userId = userNameParts[0];
        String userName = userNameParts[1];
        return this.api.getUser(userName);
    }
}
