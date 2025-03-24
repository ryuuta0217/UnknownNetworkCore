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
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.managers.RunnableManager;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomaticWorldRegeneration extends ConfigurationBase {
    private static final String PRE_WORLD_NAME_FORMAT = "%s_PRE";
    private static final LocalTime EXEC_TIME = LocalTime.of(0, 0);
    private static final AutomaticWorldRegeneration INSTANCE = new AutomaticWorldRegeneration();

    public static AutomaticWorldRegeneration getInstance() {
        return INSTANCE;
    }

    private String backupFolderPattern;
    private String backupFilePattern;

    private Map<String, Map<ScriptTiming, List<File>>> scripts;

    private long lastExec;

    private Timer timer;
    private Map<Long, Task> tasks;

    private AutomaticWorldRegeneration() {
        super("automatic-world-regeneration.yml", true, "UNC/AutomaticWorldRegeneration");
    }

    @Override
    public void onLoad() {
        this.backupFolderPattern = this.getConfig().getString("backup.folder-pattern");
        this.backupFilePattern = this.getConfig().getString("backup.file-pattern");
        // Start script configuration load logic
        this.scripts = new HashMap<>();
        ConfigurationSection scriptsSection = this.getConfig().getConfigurationSection("scripts");
        scriptsSection.getKeys(false).forEach(worldName -> {
            ConfigurationSection scriptsWorldSection = scriptsSection.getConfigurationSection(worldName);
            Map<ScriptTiming, List<File>> worldScripts = new HashMap<>();
            for (ScriptTiming timing : ScriptTiming.values()) {
                if (!scriptsWorldSection.contains(timing.name().toLowerCase())) {
                    this.getLogger().warning("Script configuration for " + worldName + " at " + timing.name().toLowerCase() + " is not found. But proceeding.");
                }

                if (scriptsWorldSection.isList(timing.name().toLowerCase())) {
                    // Multiple script file detected (list)
                    List<File> scriptFiles = scriptsWorldSection.getStringList(timing.name().toLowerCase())
                            .stream()
                            .map(File::new)
                            .filter(file -> {
                                if (!file.isFile()) {
                                    this.getLogger().severe("Script file " + file.getName() + " for " + worldName + " at " + timing.name().toLowerCase() + " is not found or not a file.");
                                    return false;
                                }

                                if (!file.exists()) {
                                    this.getLogger().severe("Script file " + file.getName() + " for " + worldName + " at " + timing.name().toLowerCase() + " is not found.");
                                    return false;
                                }

                                return true;
                            })
                            .toList();
                    worldScripts.put(timing, scriptFiles);
                } else {
                    // Single script file detected
                    File scriptFile = new File(scriptsWorldSection.getString(timing.name().toLowerCase()));
                    if (!scriptFile.exists()) {
                        this.getLogger().severe("Script file " + scriptFile.getName() + " for " + worldName + " at " + timing.name().toLowerCase() + " is not found.");
                        return;
                    }
                    worldScripts.put(timing, Collections.singletonList(scriptFile));
                }
            }
            this.scripts.put(worldName, worldScripts);
        });
        // End script configuration load logic
        // Start schedules(tasks) load logic
        if (this.tasks != null) this.tasks.entrySet().removeIf(e -> e.getValue().cancel());
        else this.tasks = new HashMap<>();

        if (this.timer == null) this.timer = new Timer();

        if (this.tasks.isEmpty()) {
            ConfigurationSection schedulesSection = this.getConfig().getConfigurationSection("schedules");
            schedulesSection.getKeys(false).forEach(dateStr -> {
                String[] dateStrSplit = dateStr.split(" ", 2);

                LocalDate execDate;
                if(dateStrSplit[0].matches("\\d{4}/\\d{2}/\\d{2}")) {
                    execDate = LocalDate.parse(dateStrSplit[0], DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                } else if (dateStrSplit[0].matches("\\d{4}/\\d/\\d")) {
                    execDate = LocalDate.parse(dateStrSplit[0], DateTimeFormatter.ofPattern("yyyy/M/d"));
                } else {
                    this.getLogger().warning("Invalid date format: " + dateStr + ". Skipping.");
                    return;
                }

                LocalTime execTime = EXEC_TIME;
                if (dateStrSplit.length == 2) {
                    String[] timeSplit = dateStrSplit[1].split(":", 3);

                    for (int i = 0; i < timeSplit.length; i++) {
                        if (!timeSplit[i].matches("\\d{1,2}")) {
                            this.getLogger().warning("Invalid time format: " + dateStrSplit[1] + ". Skipping.");
                            break;
                        }
                        int value = Integer.parseInt(timeSplit[i]);

                        if (i == 0 && value > 23) { // hour
                            this.getLogger().warning("Invalid hour value: " + value + ". Skipping.");
                            break;
                        } else if (i == 1 && value > 59) { // minute
                            this.getLogger().warning("Invalid minute value: " + value + ". Skipping.");
                            break;
                        } else if (i == 2 && value > 59) { // second
                            this.getLogger().warning("Invalid second value: " + value + ". Skipping.");
                            break;
                        }

                        execTime = switch (i) {
                            case 0 -> execTime.withHour(value);
                            case 1 -> execTime.withMinute(value);
                            case 2 -> execTime.withSecond(value);
                            default -> execTime;
                        };
                    }
                }

                LocalDateTime execDateTime = LocalDateTime.of(execDate, execTime);
                long execTimeEpoch = execDateTime.atZone(ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();

                ConfigurationSection dateSection = schedulesSection.getConfigurationSection(dateStr);
                List<String> worlds;
                if (dateSection.isList("worlds")) {
                    worlds = dateSection.getStringList("worlds");
                } else {
                    worlds = Collections.singletonList(dateSection.getString("worlds"));
                }

                String seed;
                if (dateSection.isString("seed")) {
                    seed = dateSection.getString("seed");
                } else {
                    seed = null;
                }

                boolean keepGameRules;
                if (dateSection.isBoolean("keep-game-rules")) {
                    keepGameRules = dateSection.getBoolean("keep-game-rules");
                } else {
                    keepGameRules = true;
                }

                boolean preGenerate;
                if (dateSection.isBoolean("pre-generate")) {
                    preGenerate = dateSection.getBoolean("pre-generate");
                } else {
                    preGenerate = false;
                }

                Task task = new Task(execTimeEpoch, worlds.toArray(String[]::new), seed, keepGameRules, preGenerate);
                this.timer.schedule(task, new Date(execTimeEpoch));
                this.tasks.put(execTimeEpoch, task);
            });
        } else {
            this.getLogger().warning("Something went wrong. " + this.tasks.size() + " task(s) are still running, failed to cancel. Please try cold boot to fix.");
        }
        this.lastExec = this.getConfig().getLong("last-exec");
    }

    @Override
    public synchronized void save() {
        ConfigurationSection schedulesSection;
        if (this.getConfig().isConfigurationSection("schedules")) {
            schedulesSection = this.getConfig().getConfigurationSection("schedules");
            schedulesSection.getKeys(false).forEach(key -> schedulesSection.set(key, null));
        } else {
            schedulesSection = this.getConfig().createSection("schedules");
        }
        this.tasks.forEach((execTimeEpoch, task) -> {
            ZonedDateTime execDateTime = LocalDateTime.ofEpochSecond(execTimeEpoch, 0, ZoneOffset.ofHours(9)).atZone(ZoneId.of("Asia/Tokyo"));
            DateTimeFormatter formatter = execDateTime.toLocalTime() == EXEC_TIME ? DateTimeFormatter.ofPattern("yyyy/MM/dd") : DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            String dateStr = execDateTime.format(formatter);

            ConfigurationSection scheduleSection = schedulesSection.createSection(dateStr);
            scheduleSection.set("worlds", Arrays.asList(task.worldNames));
            if (task.seed != null) scheduleSection.set("seed", task.seed);
            scheduleSection.set("keep-game-rules", task.keepGameRules);
            scheduleSection.set("pre-generate", task.preGenerate);
        });
        this.getConfig().set("last-exec", this.lastExec);
        super.save();
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

    public static Map<Long, Task> getSchedules() {
        return Collections.unmodifiableMap(getInstance().tasks);
    }

    public static boolean addSchedule(long execEpochMillis, String[] worlds, @Nullable String seed, boolean keepGameRules, boolean preGenerate) {
        LocalDateTime execDateTime = convertEpochMillisToLocalDateTime(execEpochMillis);
        if (Duration.between(LocalDateTime.now(), execDateTime).isNegative()) return false;

        getInstance().tasks.put(execEpochMillis, new Task(execEpochMillis, worlds, seed, keepGameRules, preGenerate));
        getInstance().timer.schedule(getInstance().tasks.get(execEpochMillis), new Date(execEpochMillis));
        RunnableManager.runAsync(getInstance()::save);
        return true;
    }

    public static boolean remove(long execEpochMillis) {
        if (getInstance().tasks.containsKey(execEpochMillis)) return false;
        Task task = getInstance().tasks.remove(execEpochMillis);
        task.cancel();
        RunnableManager.runAsync(getInstance()::save);
        return true;
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

        private final long execTimeEpoch;
        private final Logger logger = LoggerFactory.getLogger("AutomaticWorldRegenerationTask #" + TASK_ID++);
        private final String[] worldNames;
        private final Map<String, Path> worldPaths;
        private final String seed;
        private final boolean keepGameRules;
        private final boolean preGenerate;

        private final Map<String, Boolean> preGenerated = new HashMap<>();
        private boolean running = false;

        /**
         * ワールド再生成タスクを生成します
         *
         * @param worldNames 再生成対象のワールド名 (複数指定可能)
         * @param seed 再生成時に使用するシード値
         * @param keepGameRules 再生成時にゲームルールを保持するかどうか
         * @param preGenerate 事前生成を行うかどうか
         */
        public Task(long execTimeEpoch, String[] worldNames, @Nullable String seed, boolean keepGameRules, boolean preGenerate) {
            this.execTimeEpoch = execTimeEpoch;
            this.worldNames = worldNames;
            this.worldPaths = Arrays.stream(this.worldNames).parallel()
                    .map(worldName -> Map.entry(worldName, Bukkit.getWorld(worldName)))
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().getWorldFolder()))
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().toPath()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.seed = seed;
            this.keepGameRules = keepGameRules;
            this.preGenerate = preGenerate;
            this.checkPreGenerated();
        }

        public boolean isPreGenerated(String worldName) {
            if (!this.preGenerate) return false;
            File worldFolder = new File(Bukkit.getWorldContainer(), String.format(PRE_WORLD_NAME_FORMAT, worldName));
            return worldFolder.exists() && worldFolder.isDirectory() && new File(worldFolder, "level.dat").exists();
        }

        public void checkPreGenerated() {
            this.preGenerated.clear();
            this.preGenerated.putAll(Arrays.stream(this.worldNames)
                    .map(worldName -> String.format(PRE_WORLD_NAME_FORMAT, worldName))
                    .collect(Collectors.toMap(worldName -> worldName, this::isPreGenerated)));
        }

        public boolean runPreGenerate(String worldName) {
            if (this.isWorldLoaded(worldName)) return false;
            World.Environment env = Bukkit.getWorld(worldName).getEnvironment();

            boolean result = this.isPreGenerated(worldName) || MultiverseCore.getInstance().getMVWorldManager().addWorld(String.format(PRE_WORLD_NAME_FORMAT, worldName), env, this.seed, WorldType.NORMAL, true, null, true);
            this.checkPreGenerated();
            return result;
        }

        /**
         * ワールド再生成タスクを実行します
         */
        @Override
        public void run() {
            if (this.running) return;
            this.running = true;

            // TODO: execBeforeScript

            if (this.preGenerate && this.preGenerated.entrySet().stream().allMatch(e -> e.getValue() || this.runPreGenerate(e.getKey()))) {
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
            AutomaticWorldRegeneration.getInstance().tasks.remove(this.execTimeEpoch);
            AutomaticWorldRegeneration.getInstance().setLastExecutionTime(System.currentTimeMillis());
            this.cancel();
            this.running = false;
        }

        public boolean unloadWorld(String worldName) {
            try {
                return this.isWorldLoaded(worldName) && Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> MultiverseCore.getInstance().getMVWorldManager().unloadWorld(worldName, true)).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean unloadWorlds() {
            int fails = 0;
            for (String worldName : this.worldNames) {
                if (!this.unloadWorld(worldName)) fails++;
            }
            return fails == 0;
        }

        public boolean loadWorld(String worldName) {
            try {
                return this.isWorldLoaded(worldName) || Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> MultiverseCore.getInstance().getMVWorldManager().loadWorld(worldName)).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return false;
            }
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
            if (this.isPreGenerated(worldName)) {
                this.logger.info("World {} is already pre-generated. Skipping regeneration.", worldName);
                return true;
            }
            if (!this.isWorldLoaded(worldName) && !this.loadWorld(worldName)) {
                this.logger.error("Failed to load world {}.", worldName);
                return false;
            }
            this.logger.info("Regenerating world {} with seed {}...", worldName, this.seed);
            boolean result;
            try {
                result = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> MultiverseCore.getInstance().getMVWorldManager().regenWorld(worldName, true, this.seed == null, this.seed, this.keepGameRules)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (!result) {
                this.logger.error("Failed to regenerate world {}.", worldName);
            }
            return result;
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
            System.out.println("LastExec: " + AutomaticWorldRegeneration.getInstance().getLastExecuteTime());
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

    private enum ScriptTiming {
        PRE_GENERATED,
        BEFORE,
        AFTER
    }
}
