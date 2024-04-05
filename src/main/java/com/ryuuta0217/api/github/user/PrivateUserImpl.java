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

package com.ryuuta0217.api.github.user;

import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.user.interfaces.PrivateUser;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class PrivateUserImpl extends PublicUserImpl implements PrivateUser {
    private final long ownedPrivateRepos;
    private final boolean twoFactorAuthentication;
    private final boolean businessPlus;
    @Nullable
    private final String ldapDistinguishedName;

    public PrivateUserImpl(GitHubAPI api, JSONObject data) {
        super(api, data);
        this.ownedPrivateRepos = data.has("owned_private_repos") ? data.getLong("owned_private_repos") : -1;
        this.twoFactorAuthentication = data.has("two_factor_authentication") && data.getBoolean("two_factor_authentication");
        this.businessPlus = data.has("is_business_plus") && data.getBoolean("is_business_plus");
        this.ldapDistinguishedName = data.has("ldap_distinguished_name") && !data.isNull("ldap_distinguished_name") ? data.getString("ldap_distinguished_name") : null;
    }

    @Override
    public long getOwnedPrivateRepos() {
        return 0;
    }

    @Override
    public boolean isTwoFactorAuthentication() {
        return false;
    }

    @Override
    public boolean isBusinessPlus() {
        return false;
    }

    @Nullable
    @Override
    public String getLdapDistinguishedName() {
        return null;
    }
}
