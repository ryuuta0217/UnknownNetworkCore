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

package com.ryuuta0217.api.github.user.interfaces;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

public interface SimpleUser extends GitUser {
    String getAvatarUrl();

    String getEventsUrl();

    String getFollowersUrl();

    String getFollowingUrl();

    String getGistsUrl();

    @Nullable
    String getGravatarId();

    String getHtmlUrl();

    long getId();

    String getNodeId();

    String getLogin();

    String getOrganizationsUrl();

    String getReceivedEventsUrl();

    String getReposUrl();

    boolean isSiteAdmin();

    String getStarredUrl();

    String getSubscriptionsUrl();

    String getType();

    String getUrl();

    /* NOT REQUIRED UNDER */
    @Nullable
    ZonedDateTime getStarredAt();
}
