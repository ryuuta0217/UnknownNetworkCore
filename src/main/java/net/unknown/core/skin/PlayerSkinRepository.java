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
import com.ryuuta0217.util.HTTPFetch;
import com.ryuuta0217.util.HTTPUtil;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.configurations.SharedConfigurationBase;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;

public class PlayerSkinRepository extends SharedConfigurationBase {
    private final UUID ownerUniqueId;
    private Player owner;
    private Skin lastSeenOriginalSkin = null;
    private Skin customSkin = null;
    private TreeMap<Long, Skin> skinHistory;

    public PlayerSkinRepository(UUID owner) {
        super("skins/" + owner.toString() + ".yml", "PlayerSkinRepository");
        this.ownerUniqueId = owner;
        this.owner = Bukkit.getPlayer(this.ownerUniqueId);
    }

    @Override
    public void onLoad() {
        if (this.getConfig().contains("last-seen-remote-skin")) this.lastSeenOriginalSkin = Skin.readFromConfig(this.getConfig().getConfigurationSection("last-seen-remote-skin"));
        if (this.getConfig().contains("custom-skin")) this.customSkin = Skin.readFromConfig(this.getConfig().getConfigurationSection("custom-skin"));
        if (this.skinHistory != null) this.skinHistory.clear();
        else this.skinHistory = new TreeMap<>(Comparator.comparingLong(t -> t));
        if (this.getConfig().contains("skin-history")) {
            this.getConfig().getConfigurationSection("skin-history").getKeys(false).forEach(timestamp -> {
                this.skinHistory.put(Long.parseLong(timestamp), Skin.readFromConfig(this.getConfig().getConfigurationSection("skin-history." + timestamp)));
            });
        }
    }

    @Override
    public synchronized void save() {
        this.getConfig().set("last-seen-remote-skin", null);
        this.lastSeenOriginalSkin.asConfig(this.getConfig().createSection("last-seen-remote-skin"));

        this.getConfig().set("custom-skin", null);
        this.customSkin.asConfig(this.getConfig().createSection("custom-skin"));

        this.getConfig().set("skin-history", null);
        this.skinHistory.forEach((timestamp, skin) -> skin.asConfig(this.getConfig().createSection("skin-history." + timestamp)));
        super.save();
    }

    @Nullable
    public Player getOwner() {
        if (this.owner == null) this.owner = Bukkit.getPlayer(this.ownerUniqueId);
        return this.owner;
    }

    @Nullable
    public Skin getCustomSkin() {
        return this.customSkin;
    }

    public Skin getSeenSkin() {
        if (this.customSkin != null) return this.customSkin;
        return this.getOriginalSkin();
    }

    public Skin getOriginalSkin() {
        if (this.lastSeenOriginalSkin != null) return this.lastSeenOriginalSkin;
        return this.getRemoteSkin();
    }

    public Skin getRemoteSkin() {
        Skin remoteSkin = null;
        try {
            String remoteDataRaw = HTTPFetch.fetchGet("https://sessionserver.mojang.com/session/minecraft/profile/" + this.ownerUniqueId)
                    .addQueryParam("unsigned", "false")
                    .sentAndReadAsString();
            if (remoteDataRaw != null) {
                JSONObject remoteData = (JSONObject) UnknownNetworkCore.getJsonParser().parse(remoteDataRaw);
                if (remoteData.containsKey("properties")) {
                    JSONArray properties = (JSONArray) remoteData.get("properties");
                    remoteSkin = ((Stream<Object>) properties.stream())
                            .filter(obj -> obj instanceof JSONObject)
                            .map(obj -> ((JSONObject) obj))
                            .filter(data -> data.containsKey("value") && data.containsKey("signature"))
                            .findAny()
                            .map(data -> new Skin(SkinSource.OFFICIAL, data.get("value").toString(), data.get("signature").toString()))
                            .orElse(null);
                }
            }
        } catch(IOException | ParseException ignored) {}
        if (remoteSkin != null) {
            this.lastSeenOriginalSkin = remoteSkin;
            this.addSkinHistoryIfUpdated(remoteSkin);
        }
        return remoteSkin;
    }

    public void setLastSeenOriginalSkin(PlayerProfile profile) {
        this.setLastSeenOriginalSkin(PlayerSkinRepository.extractSkinFromProfile(profile));
    }

    public void setLastSeenOriginalSkin(Skin skin) {
        this.lastSeenOriginalSkin = skin;
    }

    public void setCustomSkin(Skin skin) {
        this.customSkin = skin;
        this.applySkin();
    }

    private void applySkin() {
        if (this.getOwner() != null) {
            this.getOwner().setPlayerProfile(this.applySkin(this.getOwner().getPlayerProfile(), true));
        }
    }

    private PlayerProfile applySkin(PlayerProfile profile, boolean applyOriginalSkinAsFallback) {
        if (this.customSkin != null) {
            profile.setProperty(this.lastSeenOriginalSkin.asProfileProperty());
        } else if (applyOriginalSkinAsFallback) {
            if (this.lastSeenOriginalSkin != null) {
                profile.setProperty(this.lastSeenOriginalSkin.asProfileProperty());
            } else {
                Skin originalSkin = this.getRemoteSkin();
                if (originalSkin != null) {
                    profile.setProperty(originalSkin.asProfileProperty());
                }
            }
        }
        return profile;
    }

    // for player profile
    public boolean addSkinHistoryIfUpdated(PlayerProfile profile) {
        return this.addSkinHistoryIfUpdated(PlayerSkinRepository.extractSkinFromProfile(profile));
    }

    // from skin, returns true if skin is updated (history added)
    public boolean addSkinHistoryIfUpdated(Skin skin) {
        if (isLatestSkinUsing(skin)) return false;
        this.skinHistory.put(System.currentTimeMillis(), skin);
        return true;
    }

    public boolean isLatestSkinUsing() {
        return this.isLatestSkinUsing(this.lastSeenOriginalSkin);
    }

    public boolean isLatestSkinUsing(Skin skin) {
        return this.skinHistory.lastEntry().getValue().equals(skin);
    }

    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (this.addSkinHistoryIfUpdated(event.getPlayerProfile())) { // if skin updated, add skin history
            ListenerManager.waitForEvent(PlayerJoinEvent.class, false, EventPriority.MONITOR, (e) -> {
                return e.getPlayer().getUniqueId().equals(event.getUniqueId());
            }, (e) -> {
                RunnableManager.runAsyncDelayed(() -> NewMessageUtil.sendMessage(e.getPlayer(), "スキンを変更しましたか？スキンが変更履歴に追加されました。"), 5);
            }, 3, ListenerManager.TimeType.SECONDS, () -> {
                // nothing to do
            });
            this.customSkin = null;
            RunnableManager.runAsyncDelayed(this::save, 20L * 3);
        } else {
            // If updated original skin, reset custom skin.
            event.setPlayerProfile(this.applySkin(event.getPlayerProfile(), false));
        }

        this.setLastSeenOriginalSkin(event.getPlayerProfile());
    }

    private static Skin extractSkinFromProfile(PlayerProfile profile) {
        String base64 = null;
        String signature = null;
        for (ProfileProperty property : profile.getProperties()) {
            if (property.getName().equals("textures")) {
                base64 = property.getValue();
                signature = property.getSignature();
                break;
            }
        }
        if (base64 == null || signature == null) return null; // if invalid profile provided?
        return new Skin(SkinSource.PLAYER_PROFILE, base64, signature);
    }
}
