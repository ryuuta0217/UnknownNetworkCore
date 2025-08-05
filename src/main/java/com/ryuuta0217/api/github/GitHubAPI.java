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

package com.ryuuta0217.api.github;

import com.ryuuta0217.api.github.repository.RepositoryImpl;
import com.ryuuta0217.api.github.repository.actions.Workflow;
import com.ryuuta0217.api.github.repository.actions.WorkflowRun;
import com.ryuuta0217.api.github.repository.actions.WorkflowRunJob;
import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.repository.check.CheckRun;
import com.ryuuta0217.api.github.repository.commit.CommitImpl;
import com.ryuuta0217.api.github.repository.commit.CompareResult;
import com.ryuuta0217.api.github.repository.commit.interfaces.Commit;
import com.ryuuta0217.api.github.repository.commit.interfaces.SimpleCommit;
import com.ryuuta0217.api.github.repository.interfaces.Repository;
import com.ryuuta0217.api.github.repository.interfaces.RepositoryMinimal;
import com.ryuuta0217.api.github.user.PublicUserImpl;
import com.ryuuta0217.api.github.user.interfaces.PublicUser;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import com.ryuuta0217.util.HTTPFetch;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GitHubAPI {
    public static final String GITHUB_API_BASE = "https://api.github.com";
    private final String accessToken;

    public GitHubAPI(String accessToken) {
        this.accessToken = accessToken;
    }

    public static String stripArgumentPatternFromUrl(String url) {
        return url.replaceAll("\\{.*?}", "");
    }

    public static String replaceArgumentPatternFromUrl(String url, String argumentName, String value) {
        if (url.contains("{/" + argumentName + "}")) {
            return url.replaceAll("\\{/" + argumentName + "}", "/" + value);
        } else {
            return url.replaceAll("\\{" + argumentName + "}", value);
        }
    }

    @Nullable
    public PublicUser getUser(String userName) {
        Object obj = this.requestAndParse("GET",
                "/users",
                "/" + userName
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new PublicUserImpl(this, json);
    }

    @Nullable
    public Repository getRepository(String owner, String repositoryName) {
        Object obj = this.requestAndParse("GET",
                "/repos",
                "/" + owner,
                "/" + repositoryName
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new RepositoryImpl(this, json);
    }

    @Nullable
    public Commit getCommit(String owner, String repositoryName, String sha) {
        return getCommit(this.getRepository(owner, repositoryName), sha);
    }

    @Nullable
    public Commit getCommit(SimpleUser user, String repositoryName, String sha) {
        if (user == null) return null;
        return getCommit(this.getRepository(user.getLogin(), repositoryName), sha);
    }

    @Nullable
    public Commit getCommit(RepositoryMinimal repository, String sha) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", replaceArgumentPatternFromUrl(repository.getCommitsUrl(), "sha", sha));
        if (!(obj instanceof JSONObject json)) return null;
        return new CommitImpl(this, repository, json);
    }

    @Nullable
    public List<Commit> getCommits(SimpleUser user, String repositoryName, @Nullable String sha) {
        if (user == null) return null;
        return getCommits(user.getLogin(), repositoryName, sha);
    }

    @Nullable
    public List<Commit> getCommits(SimpleUser user, RepositoryMinimal repository, @Nullable String sha) {
        if (user == null) return null;
        return getCommits(user.getLogin(), repository, sha);
    }

    @Nullable
    public List<Commit> getCommits(String owner, String repositoryName, @Nullable String sha) {
        return getCommits(owner, this.getRepository(owner, repositoryName), sha);
    }

    @Nullable
    public List<Commit> getCommits(String owner, RepositoryMinimal repository, @Nullable String sha) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", stripArgumentPatternFromUrl(repository.getCommitsUrl()), (sha != null ? "?sha=" + sha : ""));
        if (!(obj instanceof JSONArray arr)) return null;
        return arr.toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> (Map<?, ?>) raw)
                .map(raw -> new JSONObject(raw))
                .map(json -> new CommitImpl(this, repository, json))
                .map(commit -> (Commit) commit)
                .toList();
    }

    @Nullable
    public Branch getBranch(SimpleUser user, String repositoryName, String branchName) {
        if (user == null) return null;
        return getBranch(user.getLogin(), repositoryName, branchName);
    }

    @Nullable
    public Branch getBranch(String owner, String repositoryName, String branchName) {
        return getBranch(this.getRepository(owner, repositoryName), branchName);
    }

    @Nullable
    public Branch getBranch(RepositoryMinimal repository, String branchName) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", replaceArgumentPatternFromUrl(repository.getBranchesUrl(), "branch", branchName));
        if (!(obj instanceof JSONObject json)) return null;
        return new Branch(this, repository, json);
    }

    @Nullable
    public List<Branch> getBranches(String owner, String repositoryName) {
        return getBranches(this.getRepository(owner, repositoryName));
    }

    @Nullable
    public List<Branch> getBranches(SimpleUser user, String repositoryName) {
        if (user == null) return null;
        return getBranches(this.getRepository(user.getLogin(), repositoryName));
    }

    @Nullable
    public List<Branch> getBranches(RepositoryMinimal repository) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", stripArgumentPatternFromUrl(repository.getBranchesUrl()));
        if (!(obj instanceof JSONArray arr)) return null;
        return arr.toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> (Map<?, ?>) raw)
                .map(raw -> new JSONObject(raw))
                .map(json -> new Branch(this, repository, json))
                .toList();
    }

    @Nullable
    public List<CheckRun> getCheckRunsByCommit(SimpleCommit commit) {
        Commit full = commit.tryGetCommit();
        if (full == null) return null;
        return getCheckRunsByCommit(full.getRepository(), full.getSha());
    }

    @Nullable
    public List<CheckRun> getCheckRunsByCommit(String owner, String repositoryName, String ref) {
        return getCheckRunsByCommit(this.getRepository(owner, repositoryName), ref);
    }

    @Nullable
    public List<CheckRun> getCheckRunsByCommit(RepositoryMinimal repository, String ref) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET",
                replaceArgumentPatternFromUrl(repository.getCommitsUrl(), "sha", ref),
                "/check-runs"
        );
        if (!(obj instanceof JSONObject data)) return null;
        return data.getJSONArray("check_runs").toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> (Map<?, ?>) raw)
                .map(raw -> new JSONObject(raw))
                .map(json -> new CheckRun(this, json))
                .toList();
    }

    public CompareResult getCompareResult(RepositoryMinimal repository, String base, String head) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET",
                replaceArgumentPatternFromUrl(replaceArgumentPatternFromUrl(repository.getCompareUrl(), "base", base), "head", head)
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new CompareResult(this, repository, json);
    }

    public List<WorkflowRunJob> getWorkflowRunJobs(WorkflowRun workflowRun) {
        if (workflowRun == null) return null;
        Object obj = this.requestAndParse("GET", workflowRun.getJobsUrl());
        if (!(obj instanceof JSONObject json)) return null;
        return json.getJSONArray("jobs").toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> (Map<?, ?>) raw)
                .map(raw -> new JSONObject(raw))
                .map(jsonObj -> new WorkflowRunJob(this, workflowRun, jsonObj))
                .toList();
    }

    public WorkflowRun getWorkflowRun(String owner, String repo, String runId) {
        Object obj = this.requestAndParse("GET",
                "/repos",
                "/" + owner,
                "/" + repo,
                "/actions",
                "/runs",
                "/" + runId
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new WorkflowRun(this, json);
    }

    @Nullable
    public Object requestAndParse(String methodStr, String... endpoints) {
        HTTPFetch.Method method = HTTPFetch.Method.valueOf(methodStr);

        String endpoint = String.join("", endpoints);
        if (endpoint.contains(GITHUB_API_BASE)) endpoint = endpoint.replace(GITHUB_API_BASE, "");

        try {
            //System.out.println(endpoint);
            HTTPFetch fetch = HTTPFetch.fetch(method, GITHUB_API_BASE + endpoint);
            if (this.accessToken != null) fetch.addHeader("Authorization", "Bearer " + accessToken);
            String response = fetch.sentAndReadAsString();
            //System.out.println(response);
            if (response == null) return null;
            if (response.startsWith("[")) return new JSONArray(response);
            else return new JSONObject(response);
        } catch (IOException e) {
            return null;
        }
    }
}
