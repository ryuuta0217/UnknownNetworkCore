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
import com.ryuuta0217.api.github.repository.RepositoryMinimalImpl;
import com.ryuuta0217.api.github.repository.commit.SimpleCommitImpl;
import com.ryuuta0217.api.github.repository.commit.interfaces.SimpleCommit;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.repository.pullreq.PullRequestMinimal;
import com.ryuuta0217.api.github.repository.shared.Conclusion;
import com.ryuuta0217.api.github.repository.shared.Status;
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class WorkflowRun {
    private final GitHubAPI api;

    private final long id;
    private final String nodeId;
    @Nullable private final String headBranch;
    private final long runNumber;
    private final String displayTitle;
    private final String event;
    @Nullable private final Status status;
    @Nullable private final Conclusion conclusion;
    private final String headSha;
    private final String path;
    private final long workflowId;
    private final String url;
    private final String htmlUrl;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    @Nullable private final SimpleCommit headCommit;
    private final RepositoryMinimal headRepository;
    private final RepositoryMinimal repository;
    private final String jobsUrl;
    private final String logsUrl;
    private final String checkSuiteUrl;
    private final String cancelUrl;
    private final String rerunUrl;
    private final String artifactsUrl;
    private final String workflowUrl;
    @Nullable private final PullRequestMinimal[] pullRequests;


    @Nullable private final String name;
    private final long checkSuiteId; // when null, -1
    @Nullable private final String checkSuiteNodeId;
    private final long runAttempt; // when null, -1
    @Nullable private final Workflow[] referencedWorkflows;
    @Nullable private final SimpleUser actor;
    @Nullable private final SimpleUser triggeringActor;
    @Nullable private final ZonedDateTime runStartedAt;
    @Nullable private final String previousAttemptUrl;
    private final long headRepositoryId; // when nul, -1

    public WorkflowRun(GitHubAPI api, JSONObject data) {
        this.api = api;

        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.headBranch = data.optString("head_branch", null);
        this.runNumber = data.getLong("run_number");
        this.displayTitle = data.getString("display_title");
        this.event = data.getString("event");
        this.status = data.has("status") ? Status.valueOf(data.getString("status").toUpperCase()) : null;
        this.conclusion = data.has("conclusion") ? (!data.isNull("conclusion") ? Conclusion.valueOf(data.getString("conclusion").toUpperCase()) : null) : null;
        this.headSha = data.getString("head_sha");
        this.path = data.getString("path");
        this.workflowId = data.getLong("workflow_id");
        this.url = data.getString("url");
        this.htmlUrl = data.getString("html_url");
        this.createdAt = ZonedDateTime.parse(data.getString("created_at"));
        this.updatedAt = ZonedDateTime.parse(data.getString("updated_at"));
        this.headRepository = new RepositoryMinimalImpl(api, data.getJSONObject("head_repository"));
        this.repository = new RepositoryMinimalImpl(api, data.getJSONObject("repository"));
        this.headCommit = data.has("head_commit") ? new SimpleCommitImpl(api, this.repository, data.getJSONObject("head_commit")) : null;
        this.jobsUrl = data.getString("jobs_url");
        this.logsUrl = data.getString("logs_url");
        this.checkSuiteUrl = data.getString("check_suite_url");
        this.cancelUrl = data.getString("cancel_url");
        this.rerunUrl = data.getString("rerun_url");
        this.artifactsUrl = data.getString("artifacts_url");
        this.workflowUrl = data.getString("workflow_url");
        this.pullRequests = data.has("pull_requests") ? data.getJSONArray("pull_requests").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?,?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new PullRequestMinimal(api, json))
                .toArray(PullRequestMinimal[]::new) : null;

        this.name = data.optString("name", null);
        this.checkSuiteId = data.has("check_suite_id") ? data.getLong("check_suite_id") : -1;
        this.checkSuiteNodeId = data.optString("check_suite_node_id", null);
        this.runAttempt = data.has("run_attempt") ? data.getLong("run_attempt") : -1;
        this.referencedWorkflows = data.has("referenced_workflows") ? data.getJSONArray("referenced_workflows").toList().stream().filter(raw -> raw instanceof Map<?,?>).map(raw -> ((Map<?, ?>) raw)).map(map -> new JSONObject(map)).map(json -> new Workflow(api, repository, json)).toArray(Workflow[]::new) : null;
        this.actor = data.has("actor") ? new SimpleUserImpl(api, data.getJSONObject("actor")) : null;
        this.triggeringActor = data.has("triggering_actor") ? new SimpleUserImpl(api, data.getJSONObject("triggering_actor")) : null;
        this.runStartedAt = data.has("run_started_at") ? ZonedDateTime.parse(data.getString("run_started_at")) : null;
        this.previousAttemptUrl = data.optString("previous_attempt_url", null);
        this.headRepositoryId = data.has("head_repository_id") ? data.getLong("head_repository_id") : -1;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    @Nullable
    public String getHeadBranch() {
        return this.headBranch;
    }

    public long getRunNumber() {
        return this.runNumber;
    }

    public String getDisplayTitle() {
        return this.displayTitle;
    }

    public String getEvent() {
        return this.event;
    }

    @Nullable
    public Status getStatus() {
        return this.status;
    }

    @Nullable
    public Conclusion getConclusion() {
        return this.conclusion;
    }

    public String getHeadSha() {
        return this.headSha;
    }

    public String getPath() {
        return this.path;
    }

    public long getWorkflowId() {
        return this.workflowId;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Nullable
    public SimpleCommit getHeadCommit() {
        return this.headCommit;
    }

    public RepositoryMinimal getHeadRepository() {
        return this.headRepository;
    }

    public RepositoryMinimal getRepository() {
        return this.repository;
    }

    public String getJobsUrl() {
        return this.jobsUrl;
    }

    public String getLogsUrl() {
        return this.logsUrl;
    }

    public String getCheckSuiteUrl() {
        return this.checkSuiteUrl;
    }

    public String getCancelUrl() {
        return this.cancelUrl;
    }

    public String getRerunUrl() {
        return this.rerunUrl;
    }

    public String getArtifactsUrl() {
        return this.artifactsUrl;
    }

    public String getWorkflowUrl() {
        return this.workflowUrl;
    }

    @Nullable
    public PullRequestMinimal[] getPullRequests() {
        return this.pullRequests;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public long getCheckSuiteId() {
        return this.checkSuiteId;
    }

    @Nullable
    public String getCheckSuiteNodeId() {
        return this.checkSuiteNodeId;
    }

    public long getRunAttempt() {
        return this.runAttempt;
    }

    @Nullable
    public Workflow[] getReferencedWorkflows() {
        return this.referencedWorkflows;
    }

    @Nullable
    public SimpleUser getActor() {
        return this.actor;
    }

    @Nullable
    public SimpleUser getTriggeringActor() {
        return this.triggeringActor;
    }

    @Nullable
    public ZonedDateTime getRunStartedAt() {
        return this.runStartedAt;
    }

    @Nullable
    public String getPreviousAttemptUrl() {
        return this.previousAttemptUrl;
    }

    public long getHeadRepositoryId() {
        return this.headRepositoryId;
    }

    @Nullable
    public List<WorkflowRunJob> getJobs() {
        return this.api.getWorkflowRunJobs(this);
    }
}
