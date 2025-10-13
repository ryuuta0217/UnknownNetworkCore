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

package net.unknown.core.util;

import com.ryuuta0217.util.mc.MinecraftAssets;
import com.ryuuta0217.util.mc.MinecraftUtil;
import com.ryuuta0217.util.mc.model.Asset;
import net.kyori.adventure.translation.Translator;
import net.minecraft.SharedConstants;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Languages {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/Languages");
    private static final MinecraftAssets ASSETS = new MinecraftAssets();
    private static final Map<Locale, Language> LOADED_LANGUAGES = new HashMap<>() {{
        put(Locale.ENGLISH, Language.getInstance());
    }};
    private static final Set<Locale> AVAILABLE_LANGUAGES = new HashSet<>() {{
        add(Locale.ENGLISH);
    }};

    public static void init() {
        try {
            LOGGER.info("Initializing...");

            ASSETS.init(Bukkit.getMinecraftVersion());
            ASSETS.getAssetsMatching("^minecraft/lang/.*").forEach((fileName, asset) -> {
                String[] filePath = fileName.split("/");
                String langCode = filePath[filePath.length - 1].split("\\.")[0];
                Locale locale = Translator.parseLocale(langCode);
                if (locale == Locale.ENGLISH) return;
                AVAILABLE_LANGUAGES.add(locale);
            });

            LOGGER.info("Successfully initialized. Environment: Minecraft {}", Bukkit.getMinecraftVersion());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void load(Locale locale) {
        if (LOADED_LANGUAGES.containsKey(locale)) return;
        try {
            LOGGER.info("Loading language {}...", locale);
            Asset languageAsset = ASSETS.getAssetMatching("^minecraft/lang/" + locale.toString().toLowerCase() + "." + (MinecraftUtil.isModernVersion(Bukkit.getMinecraftVersion()) ? "json" : "lang"));
            if (languageAsset == null) {
                throw new IllegalArgumentException("Language " + locale + " is not available.");
            }
            Map<String, String> translations = new HashMap<>();
            Language.loadFromJson(new URL(languageAsset.url()).openStream(), translations::put);
            LOADED_LANGUAGES.put(locale, new Language() {
                @Nonnull
                @Override
                public String getOrDefault(@Nonnull String key, @Nonnull String fallback) {
                    return translations.getOrDefault(key, fallback);
                }

                @Override
                public boolean has(@Nonnull String key) {
                    return translations.containsKey(key);
                }

                @Override
                public boolean isDefaultRightToLeft() {
                    return false;
                }

                @Nonnull
                @Override
                public FormattedCharSequence getVisualOrder(@Nonnull FormattedText text) {
                    return (visitor) -> text.visit((style, string) -> StringDecomposer.iterateFormatted(string, style, visitor) ? Optional.empty() : FormattedText.STOP_ITERATION, Style.EMPTY).isPresent();
                }
            });
            LOGGER.info("Language {} loaded successfully.", locale);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static Language getLanguage(Locale locale) {
        return LOADED_LANGUAGES.getOrDefault(locale, null);
    }

    public static boolean isLoaded(Locale locale) {
        return LOADED_LANGUAGES.containsKey(locale);
    }

    public static Set<Locale> getLoadedLanguages() {
        return Collections.unmodifiableSet(LOADED_LANGUAGES.keySet());
    }

    public static boolean isAvailable(Locale locale) {
        return AVAILABLE_LANGUAGES.contains(locale);
    }

    public static Set<Locale> getAvailableLanguages() {
        return Collections.unmodifiableSet(AVAILABLE_LANGUAGES);
    }
}
