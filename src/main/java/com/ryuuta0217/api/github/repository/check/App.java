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

        this.slug = data.has("slug") && !data.isNull("slug") ? data.getString("slug") : null;
        this.installationsCount = data.has("installations_count") ? data.getLong("installations_count") : -1;
        this.clientId = data.has("client_id") && !data.isNull("client_id") ? data.getString("client_id") : null;
        this.clientSecret = data.has("client_secret") && !data.isNull("client_secret") ? data.getString("client_secret") : null;
        this.webhookSecret = data.has("webhook_secret") && !data.isNull("webhook_secret") ? data.getString("webhook_secret") : null;
        this.pem = data.has("pem") && !data.isNull("pem") ? data.getString("pem") : null;
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
