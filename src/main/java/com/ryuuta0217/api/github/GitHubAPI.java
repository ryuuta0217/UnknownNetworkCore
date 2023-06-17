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

package com.ryuuta0217.api.github;

import com.ryuuta0217.api.github.repository.Repository;
import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.repository.check.CheckRun;
import com.ryuuta0217.api.github.repository.commit.Commit;
import com.ryuuta0217.api.github.repository.commit.CompareResult;
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
        return new Repository(this, json);
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
    public Commit getCommit(Repository repository, String sha) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", replaceArgumentPatternFromUrl(repository.getCommitsUrl(), "sha", sha));
        if (!(obj instanceof JSONObject json)) return null;
        return new Commit(this, repository, json);
    }

    @Nullable
    public List<Commit> getCommits(SimpleUser user, String repositoryName, @Nullable String sha) {
        if (user == null) return null;
        return getCommits(user.getLogin(), repositoryName, sha);
    }

    @Nullable
    public List<Commit> getCommits(SimpleUser user, Repository repository, @Nullable String sha) {
        if (user == null) return null;
        return getCommits(user.getLogin(), repository, sha);
    }

    @Nullable
    public List<Commit> getCommits(String owner, String repositoryName, @Nullable String sha) {
        return getCommits(owner, this.getRepository(owner, repositoryName), sha);
    }

    @Nullable
    public List<Commit> getCommits(String owner, Repository repository, @Nullable String sha) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", stripArgumentPatternFromUrl(repository.getCommitsUrl()), (sha != null ? "?sha=" + sha : ""));
        if (!(obj instanceof JSONArray arr)) return null;
        return arr.toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> (Map<?, ?>) raw)
                .map(raw -> new JSONObject(raw))
                .map(json -> new Commit(this, repository, json))
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
    public Branch getBranch(Repository repository, String branchName) {
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
    public List<Branch> getBranches(Repository repository) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET", repository.getBranchesUrl());
        if (!(obj instanceof JSONArray arr)) return null;
        return arr.toList().stream()
                .filter(raw -> raw instanceof Map<?, ?>)
                .map(raw -> (Map<?, ?>) raw)
                .map(raw -> new JSONObject(raw))
                .map(json -> new Branch(this, repository, json))
                .toList();
    }

    public List<CheckRun> getCheckRunsByCommit(Commit commit) {
        return getCheckRunsByCommit(commit.getRepository(), commit.getSha());
    }

    public List<CheckRun> getCheckRunsByCommit(String owner, String repositoryName, String ref) {
        return getCheckRunsByCommit(this.getRepository(owner, repositoryName), ref);
    }

    public List<CheckRun> getCheckRunsByCommit(Repository repository, String ref) {
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

    public CompareResult getCompareResult(Repository repository, String base, String head) {
        if (repository == null) return null;
        Object obj = this.requestAndParse("GET",
                replaceArgumentPatternFromUrl(replaceArgumentPatternFromUrl(repository.getCompareUrl(), "base", base), "head", head)
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new CompareResult(this, repository, json);
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
