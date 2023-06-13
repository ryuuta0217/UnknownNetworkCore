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

import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.repository.Repository;
import com.ryuuta0217.util.HTTPFetch;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class GitHubAPI {
    public static final String GITHUB_API_BASE = "https://api.github.com";
    private final String accessToken;

    public GitHubAPI(String accessToken) {
        this.accessToken = accessToken;
    }

    @Nullable
    public User getUser(String userName) {
        Object obj = this.requestAndParse("GET",
                "/users",
                "/" + userName
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new User(this, json);
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
    public Branch getBranch(String owner, String repositoryName, String branchName) {
        Object obj = this.requestAndParse("GET",
                "/repos",
                "/" + owner,
                "/" + repositoryName,
                "/branches",
                "/" + branchName
        );
        if (!(obj instanceof JSONObject json)) return null;
        return new Branch(this, this.getRepository(owner, repositoryName), json);
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

    public static String stripArgumentPatternFromUrl(String url) {
        return url.replaceAll("\\{.*?}", "");
    }

    public static String replaceArgumentPatternFromUrl(String url, String argumentName, String value) {
        return url.replaceAll("\\{/" + argumentName + "}", "/" + value);
    }
}
