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
        this.ldapDistinguishedName = data.has("ldap_distinguished_name") && !data.get("ldap_distinguished_name").equals(JSONObject.NULL) ? data.getString("ldap_distinguished_name") : null;
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
