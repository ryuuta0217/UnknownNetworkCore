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

package com.ryuuta0217.api.github.repository.check;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.actions.WorkflowRun;
import com.ryuuta0217.api.github.repository.pullreq.PullRequestMinimal;
import com.ryuuta0217.api.github.repository.shared.Conclusion;
import com.ryuuta0217.api.github.repository.shared.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Map;

public class CheckRun {
    private final GitHubAPI api;

    private final long id;
    private final String headSha;
    private final String nodeId;
    private final String externalId;
    private final String url;
    private final String htmlUrl;
    private final String detailsUrl;
    private final Status status;
    @Nullable
    private final Conclusion conclusion;
    private final ZonedDateTime startedAt;
    @Nullable
    private final ZonedDateTime completedAt;
    private final Output output;
    private final String name;
    private final CheckSuite checkSuite;
    @Nullable
    private final App app;
    private final PullRequestMinimal[] pullRequests;
    @Nullable
    private final Deployment deployment;

    public CheckRun(GitHubAPI api, JSONObject data) {
        this.api = api;
        this.app = new App(api, this, data.getJSONObject("app"));
        this.externalId = data.getString("external_id");
        this.detailsUrl = data.getString("details_url");
        this.headSha = data.getString("head_sha");
        this.url = data.getString("url");
        this.conclusion = data.has("conclusion") && !data.isNull("conclusion") ? Conclusion.valueOf(data.getString("conclusion").toUpperCase()) : null;
        this.output = new Output(api, this, data.getJSONObject("output"));
        this.completedAt = data.has("completed_at") ? ZonedDateTime.parse(data.getString("completed_at")) : null;
        this.pullRequests = data.getJSONArray("pull_requests").toList()
                .stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> ((Map<?, ?>) raw))
                .map(map -> new JSONObject(map))
                .map(json -> new PullRequestMinimal(api, json))
                .toArray(PullRequestMinimal[]::new);
        this.htmlUrl = data.getString("html_url");
        this.name = data.getString("name");
        this.startedAt = ZonedDateTime.parse(data.getString("started_at"));
        this.id = data.getLong("id");
        this.checkSuite = new CheckSuite(api, this, data.getJSONObject("check_suite"));
        this.nodeId = data.getString("node_id");
        this.status = Status.valueOf(data.getString("status").toUpperCase());
        this.deployment = data.has("deployment") ? new Deployment(api, this, data.getJSONObject("deployment")) : null;
    }

    public long getId() {
        return this.id;
    }

    public String getHeadSha() {
        return this.headSha;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public String getDetailsUrl() {
        return this.detailsUrl;
    }

    public Status getStatus() {
        return this.status;
    }

    @Nullable
    public Conclusion getConclusion() {
        return this.conclusion;
    }

    public ZonedDateTime getStartedAt() {
        return this.startedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return this.completedAt;
    }

    public Output getOutput() {
        return this.output;
    }

    public String getName() {
        return this.name;
    }

    public CheckSuite getCheckSuite() {
        return this.checkSuite;
    }

    @Nullable
    public App getApp() {
        return this.app;
    }

    public PullRequestMinimal[] getPullRequests() {
        return this.pullRequests;
    }

    @Nullable
    public Deployment getDeployment() {
        return this.deployment;
    }

    @Nullable
    public WorkflowRun tryGetWorkflowRun() {
        if (this.getDetailsUrl().startsWith("https://github.com/") && this.getDetailsUrl().contains("/actions/runs/")) {
            String[] urlParts = this.getDetailsUrl().replace("https://", "").split("/");
            String owner = urlParts[1];
            String repo = urlParts[2];
            String runId = urlParts[5];
            return this.api.getWorkflowRun(owner, repo, runId);
        }
        return null;
    }
}
