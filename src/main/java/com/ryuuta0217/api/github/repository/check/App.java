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
import com.ryuuta0217.api.github.user.SimpleUserImpl;
import com.ryuuta0217.api.github.user.interfaces.SimpleUser;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public class App {
    private final GitHubAPI api;
    private final CheckRun checkRun;

    private final long id;
    private final String nodeId;
    @Nullable
    private final SimpleUser owner;
    private final String name;
    @Nullable
    private final String description;
    private final String externalUrl;
    private final String htmlUrl;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    private final Permissions permissions;
    private final String[] events;

    @Nullable
    private final String slug;
    private final long installationsCount;
    @Nullable
    private final String clientId;
    @Nullable
    private final String clientSecret;
    @Nullable
    private final String webhookSecret;
    @Nullable
    private final String pem;

    public App(GitHubAPI api, CheckRun checkRun, JSONObject data) {
        this.api = api;
        this.checkRun = checkRun;

        this.id = data.getLong("id");
        this.nodeId = data.getString("node_id");
        this.owner = data.has("owner") ? new SimpleUserImpl(api, data.getJSONObject("owner")) : null;
        this.name = data.getString("name");
        this.description = data.has("description") ? data.getString("description") : null;
        this.externalUrl = data.getString("external_url");
        this.htmlUrl = data.getString("html_url");
        this.createdAt = ZonedDateTime.parse(data.getString("created_at"));
        this.updatedAt = ZonedDateTime.parse(data.getString("updated_at"));
        this.permissions = new Permissions(api, this, data.getJSONObject("permissions"));
        this.events = data.getJSONArray("events").toList()
                .stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        this.slug = data.has("slug") && !data.get("slug").equals(JSONObject.NULL) ? data.getString("slug") : null;
        this.installationsCount = data.has("installations_count") ? data.getLong("installations_count") : -1;
        this.clientId = data.has("client_id") && !data.get("client_id").equals(JSONObject.NULL) ? data.getString("client_id") : null;
        this.clientSecret = data.has("client_secret") && !data.get("client_secret").equals(JSONObject.NULL) ? data.getString("client_secret") : null;
        this.webhookSecret = data.has("webhook_secret") && !data.get("webhook_secret").equals(JSONObject.NULL) ? data.getString("webhook_secret") : null;
        this.pem = data.has("pem") && !data.get("pem").equals(JSONObject.NULL) ? data.getString("pem") : null;
    }

    public CheckRun getCheckRun() {
        return this.checkRun;
    }

    public long getId() {
        return this.id;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    @Nullable
    public SimpleUser getOwner() {
        return this.owner;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public String getExternalUrl() {
        return this.externalUrl;
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

    public Permissions getPermissions() {
        return this.permissions;
    }

    public String[] getEvents() {
        return this.events;
    }

    @Nullable
    public String getSlug() {
        return this.slug;
    }

    public long getInstallationsCount() {
        return this.installationsCount;
    }

    @Nullable
    public String getClientId() {
        return this.clientId;
    }

    @Nullable
    public String getClientSecret() {
        return this.clientSecret;
    }

    @Nullable
    public String getWebhookSecret() {
        return this.webhookSecret;
    }

    @Nullable
    public String getPem() {
        return this.pem;
    }
}
