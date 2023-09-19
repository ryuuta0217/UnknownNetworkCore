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

package net.unknown.core.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.configuration.ConfigurationSection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Objects;

public class Skin {
    private final SkinSource source;
    private final String base64;
    private final String signature;
    private final String decodedBase64;

    public Skin(SkinSource source, String base64, String signature) {
        this.source = source;
        this.base64 = base64;
        this.signature = signature;
        this.decodedBase64 = new String(Base64.getDecoder().decode(base64));
    }

    public SkinSource getSource() {
        return this.source;
    }

    public String getBase64() {
        return this.base64;
    }

    public String getSignature() {
        return this.signature;
    }

    public String getDecodedBase64() {
        return this.decodedBase64;
    }

    public ProfileProperty asProfileProperty() {
        return new ProfileProperty("textures", base64, signature);
    }

    public void asConfig(ConfigurationSection output) {
        output.set("source", this.source.name());
        output.set("base64", this.base64);
        output.set("signature", this.signature);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Skin skin) this.equals(skin);
        if (obj instanceof ProfileProperty property) this.equals(property);
        if (obj instanceof PlayerProfile profile) this.equals(profile);
        return false;
    }

    public boolean equals(Skin skin) {
        if (this.base64.equals(skin.base64) && this.signature.equals(skin.signature)) return true;
        if (this.decodedBase64 != null) {
            try {
                DecodedTexturesProperty selfData = new DecodedTexturesProperty(new JSONObject(this.decodedBase64));
                DecodedTexturesProperty targetData = new DecodedTexturesProperty(new JSONObject(skin.decodedBase64));
                if (selfData.equals(targetData) || selfData.getTextures().equals(targetData.getTextures()) || Objects.equals(selfData.getTextures().getSkin(), targetData.getTextures().getSkin())) return true;
            } catch(JSONException ignored) {}
        }
        return false;
    }

    public boolean equals(ProfileProperty property) {
        return this.base64.equals(property.getValue()) && this.signature.equals(property.getSignature());
    }

    public boolean equals(PlayerProfile profile) {
        return profile.getProperties().stream().anyMatch(this::equals);
    }

    @Nullable
    public static Skin readFromConfig(ConfigurationSection readFrom) {
        if (readFrom.contains("source") && readFrom.contains("base64") && readFrom.contains("signature")) {
            return new Skin(SkinSource.valueOf(readFrom.getString("source")), readFrom.getString("base64"), readFrom.getString("signature"));
        } else {
            return null;
        }
    }

    private static class DecodedTexturesProperty {
        private final long timestamp;
        private final String profileId;
        private final String profileName;
        private final boolean signatureRequired;
        private final Textures textures;

        private DecodedTexturesProperty(long timestamp, String profileId, String profileName, boolean signatureRequired, Textures textures) {
            this.timestamp = timestamp;
            this.profileId = profileId;
            this.profileName = profileName;
            this.signatureRequired = signatureRequired;
            this.textures = textures;
        }

        private DecodedTexturesProperty(JSONObject decodedTexturesProperty) {
            this.timestamp = decodedTexturesProperty.has("timestamp") ? decodedTexturesProperty.getLong("timestamp") : -1;
            this.profileId = decodedTexturesProperty.has("profileId") ? decodedTexturesProperty.getString("profileId") : null;
            this.profileName = decodedTexturesProperty.has("profileName") ? decodedTexturesProperty.getString("profileName") : null;
            this.signatureRequired = decodedTexturesProperty.has("signatureRequired") && decodedTexturesProperty.getBoolean("signatureRequired");
            this.textures = decodedTexturesProperty.has("textures") ? new Textures((JSONObject) decodedTexturesProperty.get("textures")) : null;
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public String getProfileId() {
            return this.profileId;
        }

        public String getProfileName() {
            return this.profileName;
        }

        public boolean isSignatureRequired() {
            return this.signatureRequired;
        }

        public Textures getTextures() {
            return this.textures;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DecodedTexturesProperty decodedTexturesProperty) return Objects.equals(this.getTimestamp(), decodedTexturesProperty.getTimestamp()) && Objects.equals(this.getProfileId(), decodedTexturesProperty.getProfileId()) && Objects.equals(this.getProfileName(), decodedTexturesProperty.getProfileName()) && Objects.equals(this.isSignatureRequired(), decodedTexturesProperty.isSignatureRequired()) && Objects.equals(this.getTextures(), decodedTexturesProperty.getTextures());
            return super.equals(obj);
        }

        private static class Textures {
            @Nullable
            private final Textures.Texture skin;

            private Textures(Textures.Texture skin) {
                this.skin = skin;
            }

            private Textures(JSONObject textures) {
                this.skin = textures.has("SKIN") ? new Textures.Texture((JSONObject) textures.get("SKIN")) : null;
            }

            @Nullable
            public Textures.Texture getSkin() {
                return this.skin;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Textures textures) return Objects.equals(this.getSkin(), textures.getSkin());
                return super.equals(obj);
            }

            private static class Texture {
                private final String url;

                private Texture(String url) {
                    this.url = url;
                }

                private Texture(org.json.JSONObject texture) {
                    this.url = (String) texture.get("url");
                }

                public String getUrl() {
                    return this.url;
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj instanceof Texture texture) return Objects.equals(this.getUrl(), texture.getUrl());
                    return super.equals(obj);
                }
            }
        }
    }
}
