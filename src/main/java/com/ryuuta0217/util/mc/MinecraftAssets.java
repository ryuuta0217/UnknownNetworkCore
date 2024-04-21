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

package com.ryuuta0217.util.mc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ryuuta0217.util.mc.model.Asset;

import javax.annotation.RegEx;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MinecraftAssets {
    private final Logger logger = Logger.getLogger("MinecraftAssets");
    private final Gson gson = new Gson();
    private final Map<String, Asset> assets = new HashMap<>();

    public MinecraftAssets() {
    }

    public void init(String minecraftVersion) throws IOException {
        logger.info("Initializing for version " + minecraftVersion);

        String versionMetaUrl;
        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> versionManifestTree = this.gson.fromJson(new InputStreamReader(new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").openStream()), type);
            Map<String, Object> versionInfo = ((List<Map<String, Object>>) versionManifestTree.get("versions"))
                    .stream()
                    .filter(v -> v.get("id").equals(minecraftVersion))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid version: Minecraft " + minecraftVersion));
            versionMetaUrl = (String) versionInfo.get("url");
        } catch(IOException e) {
            throw new IOException("Failed to fetch version manifest, offline or mojang servers are down?", e);
        }

        String assetIndexUrl;
        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> versionMetaTree = this.gson.fromJson(new InputStreamReader(new URL(versionMetaUrl).openStream()), type);
            assetIndexUrl = (String) ((Map<String, Object>) versionMetaTree.get("assetIndex")).get("url");
        } catch(IOException e) {
            throw new IOException("Failed to fetch version meta, offline or mojang servers are down?", e);
        }

        try {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Map<String, Map<String, Object>>> assetIndex = this.gson.fromJson(new InputStreamReader(new URL(assetIndexUrl).openStream()), type);
            Map<String, Map<String, Object>> objects = assetIndex.get("objects");
            this.assets.clear();
            this.assets.putAll(objects.entrySet()
                    .stream()
                    .parallel()
                    .map(e -> Map.entry(e.getKey(), new Asset(e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        } catch(IOException e) {
            throw new IOException("Failed to fetch assets, offline or mojang servers are down?", e);
        }

        logger.info("Successfully initialized for version " + minecraftVersion);
    }

    public Asset getAsset(String path) {
        return this.assets.getOrDefault(path, null);
    }

    public Map<String, Asset> getAssets() {
        return new HashMap<>(this.assets);
    }

    public Asset getAssetMatching(@RegEx String regex) {
        return this.getAssetsMatching(regex).values()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Map<String, Asset> getAssetsMatching(@RegEx String regex) {
        return this.assets.entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(regex))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
