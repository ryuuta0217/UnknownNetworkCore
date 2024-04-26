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

package com.ryuuta0217.api.github.repository.actions;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.shared.Conclusion;
import com.ryuuta0217.api.github.repository.shared.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class Step {
    private final GitHubAPI api;
    private final WorkflowRunJob job;

    private final Status status;
    @Nullable private final Conclusion conclusion;
    private final String name;
    private final long number;
    @Nullable private final ZonedDateTime startedAt;

    public Step(GitHubAPI api, WorkflowRunJob job, JSONObject data) {
        this.api = api;
        this.job = job;

        this.status = Status.valueOf(data.getString("status").toUpperCase());
        this.conclusion = data.has("conclusion") && !data.isNull("conclusion") ? Conclusion.valueOf(data.getString("conclusion").toUpperCase()) : null;
        this.name = data.getString("name");
        this.number = data.getLong("number");
        this.startedAt = data.has("started_at") && !data.isNull("started_at") ? ZonedDateTime.parse(data.getString("started_at")) : null;
    }

    public WorkflowRunJob getJob() {
        return this.job;
    }

    public Status getStatus() {
        return this.status;
    }

    @Nullable
    public Conclusion getConclusion() {
        return this.conclusion;
    }

    public String getName() {
        return this.name;
    }

    public long getNumber() {
        return this.number;
    }

    @Nullable
    public ZonedDateTime getStartedAt() {
        return this.startedAt;
    }
}
