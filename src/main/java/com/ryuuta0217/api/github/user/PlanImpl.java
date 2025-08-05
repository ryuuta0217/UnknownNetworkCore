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

import com.ryuuta0217.api.github.user.interfaces.Plan;
import org.json.JSONObject;

public class PlanImpl implements Plan {
    private final long collaborators;
    private final String name;
    private final long space;
    private final long privateRepos;

    public PlanImpl(JSONObject data) {
        this.collaborators = data.getLong("collaborators");
        this.name = data.getString("name");
        this.space = data.getLong("space");
        this.privateRepos = data.getLong("private_repos");
    }

    @Override
    public long getCollaborators() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public long getSpace() {
        return 0;
    }

    @Override
    public long getPrivateRepos() {
        return 0;
    }
}
