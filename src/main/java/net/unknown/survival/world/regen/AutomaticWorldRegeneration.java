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

package net.unknown.survival.world.regen;

import com.ryuuta0217.file.ArchiveUtil;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.managers.RunnableManager;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomaticWorldRegeneration extends ConfigurationBase {
    private static final AutomaticWorldRegeneration INSTANCE = new AutomaticWorldRegeneration();
    private static final String PRE_WORLD_NAME_FORMAT = "%s_PRE";

    public static AutomaticWorldRegeneration getInstance() {
        return INSTANCE;
    }

    private String backupFolderPattern;
    private String backupFilePattern;

    private long lastExec;

    private final Map<Long, Task> tasks = new HashMap<>();

    private AutomaticWorldRegeneration() {
        super("automatic-world-regeneration.yml", true, "UNC/AutomaticWorldRegeneration");
    }

    @Override
    public void onLoad() {
        this.backupFolderPattern = this.getConfig().getString("backup.folder-pattern");
        this.backupFilePattern = this.getConfig().getString("backup.file-pattern");
        this.lastExec = this.getConfig().getLong("last-execution-time");
    }

    public String getBackupFolderPattern() {
        return this.backupFolderPattern;
    }

    public String getBackupFolderFormatted(String worldName, LocalDateTime lastExec, LocalDateTime now) {
        return parsePattern(this.backupFolderPattern, worldName, lastExec, now, null);
    }

    public String getBackupFilePattern() {
        return this.backupFilePattern;
    }

    public String getBackupFileFormatted(String worldName, LocalDateTime lastExec, LocalDateTime now, String extension) {
        return parsePattern(this.backupFilePattern, worldName, lastExec, now, extension);
    }

    public LocalDateTime getLastExecuteTime() {
        return convertEpochMillisToLocalDateTime(this.lastExec);
    }

    public void setLastExecutionTime(long epochMillis) {
        this.lastExec = epochMillis;
        RunnableManager.runAsync(this::save);
    }

    private static LocalDateTime convertEpochMillisToLocalDateTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("Asia/Tokyo")).toLocalDateTime();
    }

    private static String parsePattern(String pattern, String worldName, LocalDateTime lastExec, LocalDateTime now, String extension) {
        return pattern.replace("${worldName}", worldName)
                .replace("${lastYear}", DateTimeFormatter.ofPattern("yyyy").format(lastExec))
                .replace("${lastMonth}", DateTimeFormatter.ofPattern("MM").format(lastExec))
                .replace("${lastDay}", DateTimeFormatter.ofPattern("dd").format(lastExec))
                .replace("${year}", DateTimeFormatter.ofPattern("yyyy").format(now))
                .replace("${month}", DateTimeFormatter.ofPattern("MM").format(now))
                .replace("${day}", DateTimeFormatter.ofPattern("dd").format(now))
                .replace("${extension}", extension == null ? "" : extension);
    }

    public static class Task extends TimerTask {
        private static int TASK_ID = 0;

        private final Logger logger = LoggerFactory.getLogger("AutomaticWorldRegenerationTask #" + TASK_ID++);
        private final String[] worldNames;
        private final Map<String, Path> worldPaths;
        private final String seed;
        private final boolean keepGameRules;
        private boolean preGenerated;
        private boolean running = false;

        /**
         * ワールド再生成タスクを生成します
         *
         * @param worldNames 再生成対象のワールド名 (複数指定可能)
         * @param seed 再生成時に使用するシード値
         * @param keepGameRules 再生成時にゲームルールを保持するかどうか
         * @param preGenerated ワールドがすでに生成されているかどうか
         */
        public Task(String[] worldNames, @Nullable String seed, boolean keepGameRules, boolean preGenerated) {
            this.worldNames = worldNames;
            this.worldPaths = Arrays.stream(this.worldNames).parallel()
                    .map(worldName -> Map.entry(worldName, Bukkit.getWorld(worldName)))
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().getWorldFolder()))
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().toPath()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.seed = seed;
            this.keepGameRules = keepGameRules;
            this.preGenerated = preGenerated;
        }

        public void checkPreGenerated() {
            this.preGenerated = Arrays.stream(this.worldNames)
                    .map(worldName -> String.format(PRE_WORLD_NAME_FORMAT, worldName))
                    .allMatch(worldName -> {
                        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                        return worldFolder.exists() && worldFolder.isDirectory() && new File(worldFolder, "level.dat").exists();
                    });
        }

        /**
         * ワールド再生成タスクを実行します
         */
        @Override
        public void run() {
            if (this.running) return;
            this.running = true;

            // TODO: execBeforeScript

            if (this.preGenerated) {
                Map<String, Map<String, String>> gameRules = Arrays.stream(this.worldNames)
                        .map(worldName -> Map.entry(worldName, Arrays.stream(GameRule.values())
                                .map(rule -> Map.entry(rule.getName(), Bukkit.getWorld(worldName).getGameRuleValue(rule.getName())))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                this.unloadWorlds();
                this.backupWorlds();
                if (this.replaceWorldsWithPreGenerated() && this.keepGameRules) {
                    gameRules.forEach((worldName, rules) -> {
                        World world = Bukkit.getWorld(worldName);
                        rules.forEach((rule, value) -> world.setGameRuleValue(rule, value));
                    });
                }
                // this.loadWorlds(); - world is already loaded in replaceWorld();
            } else {
                this.unloadWorlds();
                this.backupWorlds();
                this.loadWorlds();
                this.regenerateWorlds();
            }

            // TODO: execAfterScript
            AutomaticWorldRegeneration.getInstance().setLastExecutionTime(System.currentTimeMillis());
            this.running = false;
        }

        public boolean unloadWorld(String worldName) {
            return this.isWorldLoaded(worldName) && MultiverseCore.getInstance().getMVWorldManager().unloadWorld(worldName, true);
        }

        public boolean unloadWorlds() {
            int fails = 0;
            for (String worldName : this.worldNames) {
                if (!this.unloadWorld(worldName)) fails++;
            }
            return fails == 0;
        }

        public boolean loadWorld(String worldName) {
            return this.isWorldLoaded(worldName) || MultiverseCore.getInstance().getMVWorldManager().loadWorld(worldName);
        }

        public boolean loadWorlds() {
            int fails = 0;
            for (String worldName : this.worldNames) {
                if (!this.loadWorld(worldName)) fails++;
            }
            return fails == 0;
        }

        public boolean deleteWorld(String worldName, boolean removeFromConfig, boolean deleteFolder) {
            return this.isWorldLoaded(worldName) && MultiverseCore.getInstance().getMVWorldManager().deleteWorld(worldName, removeFromConfig, deleteFolder);
        }

        public boolean copyWorld(String fromWorldName, String toWorldName) {
            return this.isWorldLoaded(fromWorldName) && MultiverseCore.getInstance().getMVWorldManager().cloneWorld(fromWorldName, toWorldName);
        }

        public boolean replaceWorldWithPreGenerated(String worldName) {
            return this.isWorldLoaded(worldName) &&
                    this.deleteWorld(worldName, true, true) &&
                    this.copyWorld(String.format(PRE_WORLD_NAME_FORMAT, worldName), worldName) &&
                    this.deleteWorld(String.format(PRE_WORLD_NAME_FORMAT, worldName), true, true);
        }

        public boolean replaceWorldsWithPreGenerated() {
            int fails = 0;
            for (String worldName : this.worldNames) {
                if (!this.replaceWorldWithPreGenerated(worldName)) fails++;
            }
            return fails == 0;
        }

        public boolean regenerateWorld(String worldName) {
            return (this.isWorldLoaded(worldName) || this.loadWorld(worldName)) && MultiverseCore.getInstance().getMVWorldManager().regenWorld(worldName, this.seed != null, this.seed == null, this.seed, this.keepGameRules);
        }

        public boolean regenerateWorlds() {
            int fails = 0;
            for (String worldName : this.worldNames) {
                if (!this.regenerateWorld(worldName)) fails++;
            }
            return fails == 0;
        }

        public boolean isWorldLoaded(String worldName) {
            return MultiverseCore.getInstance().getMVWorldManager().isMVWorld(worldName) && Bukkit.getWorld(worldName) != null;
        }

        public void backupWorld(String worldName) {
            // TODO: if installed Multiverse-Inventories plugin, include inventories (grab from "plugins/Multiverse-Inventories/worlds/<worldName>/**"
            File backupFolder = new File(AutomaticWorldRegeneration.getInstance().getBackupFolderFormatted(worldName, AutomaticWorldRegeneration.getInstance().getLastExecuteTime(), LocalDateTime.now()));
            File backupFile = new File(backupFolder, AutomaticWorldRegeneration.getInstance().getBackupFileFormatted(worldName, AutomaticWorldRegeneration.getInstance().getLastExecuteTime(), LocalDateTime.now(), "tar.zst"));
            String backupFilePath = backupFile.getPath();

            try {
                Path worldPath = this.worldPaths.getOrDefault(worldName, new File(Bukkit.getWorldContainer().getParentFile(), worldName).toPath());
                ArchiveUtil.createArchiveWithZstd(Files.walk(worldPath).map(Path::toAbsolutePath).map(Path::toFile).toList(), backupFile, worldPath.toFile());
            } catch(IOException e) {
                this.logger.warn("An exception occurred while opening the backup file.", e);
            }
        }

        public void backupWorlds() {
            for (String worldName : this.worldNames) {
                this.backupWorld(worldName);
            }
        }
    }
}
