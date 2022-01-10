/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.configurations;

import net.unknown.UnknownNetworkCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class Config {
    private final Logger LOGGER;
    private final String FILE_NAME;
    private final File FILE;
    private FileConfiguration CONFIG;

    public Config(String fileName, boolean ifNotExistsExtractFromJar, String loggerName) {
        this.FILE_NAME = fileName;
        this.FILE = new File(UnknownNetworkCore.getInstance().getDataFolder(), FILE_NAME);
        this.LOGGER = Logger.getLogger(loggerName);

        if (!FILE.exists()) {
            if (ifNotExistsExtractFromJar) UnknownNetworkCore.getInstance().saveResource(FILE_NAME, false);
            else {
                if (!FILE.getParentFile().exists()) {
                    if (!FILE.getParentFile().mkdirs())
                        LOGGER.severe("フォルダー " + FILE.getParentFile().getName() + " を作成できませんでした");
                }

                try {
                    if (!FILE.createNewFile()) LOGGER.severe(FILE_NAME + " を作成できませんでした");
                } catch (IOException e) {
                    LOGGER.warning(FILE_NAME + " の作成に失敗しました: " + e.getLocalizedMessage());
                    return;
                }
            }
        }

        this.CONFIG = YamlConfiguration.loadConfiguration(FILE);
        onLoad();
    }

    public FileConfiguration getConfig() {
        return CONFIG;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public File getFile() {
        return FILE;
    }

    public String getFileName() {
        return FILE_NAME;
    }

    public synchronized void save() {
        try {
            CONFIG.save(FILE);
        } catch (IOException e) {
            LOGGER.severe(FILE_NAME + " の保存に失敗しました: " + e.getLocalizedMessage());
        }
    }

    public abstract void onLoad();
}

