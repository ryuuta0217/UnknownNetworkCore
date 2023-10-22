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

package net.unknown.core.configurations;

import net.unknown.UnknownNetworkCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class ConfigurationBase {
    private final Logger logger;
    private final String configurationFileName;
    private final File configurationFile;
    private FileConfiguration configuration;

    public ConfigurationBase(String fileName, boolean ifNotExistsExtractFromJar, String loggerName) {
        this(fileName, ifNotExistsExtractFromJar, loggerName, '.');
    }

    public ConfigurationBase(String fileName, boolean ifNotExistsExtractFromJar, String loggerName, char pathSeparator) {
        this(new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), fileName), ifNotExistsExtractFromJar, loggerName, pathSeparator);
    }


    public ConfigurationBase(File file, boolean ifNotExistsExtractFromJar, String loggerName, char configurationPathSeparator) {
        this.configurationFileName = file.getName();
        this.configurationFile = file;
        this.logger = Logger.getLogger(loggerName);

        if (!configurationFile.exists()) {
            if (ifNotExistsExtractFromJar) UnknownNetworkCorePlugin.getInstance().saveResource(configurationFileName, false);
            else {
                if (!configurationFile.getParentFile().exists()) {
                    if (!configurationFile.getParentFile().mkdirs())
                        logger.severe("フォルダー " + configurationFile.getParentFile().getName() + " を作成できませんでした");
                }

                try {
                    if (!configurationFile.createNewFile()) logger.severe(configurationFileName + " を作成できませんでした");
                } catch (IOException e) {
                    logger.warning(configurationFileName + " の作成に失敗しました: " + e.getLocalizedMessage());
                    return;
                }
            }
        }

        // TODO Use FileConfigurationOptions#pathSeparator(char) to allows contains dot<.> values
        // TODO this.CONFIG.options().pathSeparator(configurationPathSeparator);
        this.configuration = YamlConfiguration.loadConfiguration(configurationFile);
        onLoad();
    }

    public FileConfiguration getConfig() {
        return configuration;
    }

    public Logger getLogger() {
        return logger;
    }

    public File getFile() {
        return configurationFile;
    }

    public String getFileName() {
        return configurationFileName;
    }

    public synchronized void save() {
        try {
            configuration.save(configurationFile);
        } catch (IOException e) {
            logger.severe(configurationFileName + " の保存に失敗しました: " + e.getLocalizedMessage());
        }
    }

    public abstract void onLoad();
}

