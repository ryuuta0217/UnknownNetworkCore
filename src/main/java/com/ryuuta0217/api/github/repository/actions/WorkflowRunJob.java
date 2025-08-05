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
import java.util.Map;

public class WorkflowRunJob {
    private final GitHubAPI api;
    private final WorkflowRun workflowRun;

    private final long id;
    private final String nodeId;
    private final long runId;
    private final String runUrl;
    private final String headSha;
    @Nullable private final String workflowName;
    @Nullable private final String headBranch;
    private final String name;
    private final String url;
    @Nullable private final String htmlUrl;
    private final Status status;
    private final Conclusion conclusion;
    private final ZonedDateTime startedAt;
    private final ZonedDateTime completedAt;
    private final String checkRunUrl;
    private final String[] labels;
    private final long runnerId; // when null, -1
    @Nullable private final String runnerName;
    private final long runnerGroupId; // when null, -1
    @Nullable private final String runnerGroupName;
    private final ZonedDateTime createdAt;

    private final long runAttempt; // when null -1
    @Nullable private final Step[] steps;

    public WorkflowRunJob(GitHubAPI api, WorkflowRun workflowRun, JSONObject data) {
        this.api = api;
        this.workflowRun = null;

        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.runId = data.getLong("run_id");
        this.runUrl = data.getString("run_url");
        this.headSha = data.getString("head_sha");
        this.workflowName = data.has("workflow_name") ? data.getString("workflow_name") : null;
        this.headBranch = data.has("head_branch") ? data.getString("head_branch") : null;
        this.name = data.getString("name");
        this.url = data.getString("url");
        this.htmlUrl = data.has("html_url") ? data.getString("html_url") : null;
        this.status = Status.valueOf(data.getString("status").toUpperCase());
        this.conclusion = data.has("conclusion") ? Conclusion.valueOf(data.getString("conclusion").toUpperCase()) : null;
        this.startedAt = ZonedDateTime.parse(data.getString("started_at"));
        this.completedAt = data.has("completed_at") ? ZonedDateTime.parse(data.getString("completed_at")) : null;
        this.checkRunUrl = data.getString("check_run_url");
        this.labels = data.getJSONArray("labels").toList()
                .stream()
                .filter(raw -> raw instanceof String)
                .map(String::valueOf)
                .toArray(String[]::new);
        this.runnerId = data.has("runner_id") ? data.getLong("runner_id") : -1;
        this.runnerName = data.has("runner_name") ? data.getString("runner_name") : null;
        this.runnerGroupId = data.has("runner_group_id") ? data.getLong("runner_group_id") : -1;
        this.runnerGroupName = data.has("runner_group_name") ? data.getString("runner_group_name") : null;
        this.createdAt = ZonedDateTime.parse(data.getString("created_at"));
        this.runAttempt = data.has("run_attempt") ? data.getLong("run_attempt") : -1;
        this.steps = data.has("steps") ? data.getJSONArray("steps").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?,?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new Step(api, this, json))
                .toArray(Step[]::new) : null;
    }

    public WorkflowRun getWorkflowRun() {
        return this.workflowRun;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public long getRunId() {
        return this.runId;
    }

    public String getRunUrl() {
        return this.runUrl;
    }

    public String getHeadSha() {
        return this.headSha;
    }

    @Nullable
    public String getWorkflowName() {
        return this.workflowName;
    }

    @Nullable
    public String getHeadBranch() {
        return this.headBranch;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    @Nullable
    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public Status getStatus() {
        return this.status;
    }

    public Conclusion getConclusion() {
        return this.conclusion;
    }

    public ZonedDateTime getStartedAt() {
        return this.startedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return this.completedAt;
    }

    public String getCheckRunUrl() {
        return this.checkRunUrl;
    }

    public String[] getLabels() {
        return this.labels;
    }

    public long getRunnerId() {
        return this.runnerId;
    }

    @Nullable
    public String getRunnerName() {
        return this.runnerName;
    }

    public long getRunnerGroupId() {
        return this.runnerGroupId;
    }

    @Nullable
    public String getRunnerGroupName() {
        return this.runnerGroupName;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public long getRunAttempt() {
        return this.runAttempt;
    }

    @Nullable
    public Step[] getSteps() {
        return this.steps;
    }
}
