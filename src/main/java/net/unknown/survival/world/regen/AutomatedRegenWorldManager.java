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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.dependency.MultiverseCore;
import net.unknown.core.managers.EvalManager;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.ScriptableObject;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.options.*;
import org.mvplugins.multiverse.core.world.reasons.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AutomatedRegenWorldManager extends ConfigurationBase implements Listener {
    private static final LocalTime EXEC_TIME = LocalTime.of(0, 0);
    private static final AutomatedRegenWorldManager INSTANCE = new AutomatedRegenWorldManager();

    private String preGenerateWorldNamePattern;

    private String backupFolderPattern;
    private String backupFilePattern;

    private Map<String, Map<ScriptTiming, List<File>>> scripts;

    private long lastExec;

    private Timer timer;
    private Map<Long, Task> tasks;

    private AutomatedRegenWorldManager() {
        super("automated-regen-world.yml", true, "UNC/AutomatedRegenWorldManager");
        ListenerManager.registerListener(this);
    }

    public static AutomatedRegenWorldManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void onLoad() {
        this.preGenerateWorldNamePattern = this.getConfig().getString("pre-gen.name-format", "${worldName}_PRE");
        this.backupFolderPattern = this.getConfig().getString("backup.folder-pattern", "./backups/${worldName}");
        this.backupFilePattern = this.getConfig().getString("backup.file-pattern", "${year}-${month}-${day}.${extension}");

        this.scripts = new HashMap<>();
        ConfigurationSection scriptsSection = this.getConfig().getConfigurationSection("scripts");
        if (scriptsSection != null) {
            scriptsSection.getKeys(false).forEach(worldName -> {
                ConfigurationSection worldSection = scriptsSection.getConfigurationSection(worldName);

                worldSection.getKeys(false).forEach(timingStr -> {
                    ScriptTiming timing = ScriptTiming.valueOf(timingStr.toUpperCase());
                    List<File> scriptFiles = new ArrayList<>();
                    if (worldSection.isString(timingStr)) {
                        String scriptPath = worldSection.getString(timingStr);
                        if (scriptPath != null && !scriptPath.isBlank()) {
                            File scriptFile = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), scriptPath);
                            if (scriptFile.exists() && scriptFile.isFile()) {
                                scriptFiles.add(scriptFile);
                            } else {
                                this.getLogger().warning("Script file for " + timing.name() + " in world " + worldName + " does not exist: " + scriptPath);
                            }
                        }
                    }

                    if (worldSection.isList(timingStr)) {
                        List<String> scriptPaths = worldSection.getStringList(timingStr);
                        if (!scriptPaths.isEmpty()) {
                            for (String scriptPath : scriptPaths) {
                                if (scriptPath != null && !scriptPath.isBlank()) {
                                    File scriptFile = new File(scriptPath);
                                    if (scriptFile.exists() && scriptFile.isFile()) {
                                        scriptFiles.add(scriptFile);
                                    } else {
                                        this.getLogger().warning("Script file for " + timing.name() + " in world " + worldName + " does not exist: " + scriptPath);
                                    }
                                }
                            }
                        }
                    }

                    this.scripts.computeIfAbsent(worldName, k -> new HashMap<>()).put(timing, scriptFiles);
                });
            });
        }

        this.lastExec = this.getConfig().getLong("last-exec", 0L);
        this.timer = new Timer("AutomatedRegenWorldTimer", true);
        if (this.tasks != null) {
            this.tasks.forEach((time, task) -> {
                task.getRegenerationTask().cancel();
            });
        }
        this.tasks = new HashMap<>();

        ConfigurationSection schedulesSection = this.getConfig().getConfigurationSection("schedules");
        schedulesSection.getKeys(false).forEach(dateStr -> {
            long execTimeEpoch;
            if (dateStr.matches("^\\d+$")) {
                execTimeEpoch = Long.parseLong(dateStr);
            } else {
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
                execTimeEpoch = execDateTime.atZone(ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();
            }

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

            Task task = new Task(execTimeEpoch, worlds.toArray(new String[0]), seed, keepGameRules, preGenerate, this);
            this.tasks.put(execTimeEpoch, task);
        });
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
            ConfigurationSection scheduleSection = schedulesSection.createSection(String.valueOf(execTimeEpoch));
            scheduleSection.set("worlds", Arrays.asList(task.getWorldNames()));
            scheduleSection.set("seed", task.getSeed());
            scheduleSection.set("keep-game-rules", task.keepGameRule());
            scheduleSection.set("pre-generate", task.preGenerate());
        });

        this.getConfig().set("last-exec", this.lastExec);
        super.save();
    }

    public void addTask(long execEpochMillis, String[] worldNames, @Nullable String seed, boolean keepGameRule, boolean preGenerate) {
        Task task = new Task(execEpochMillis, worldNames, seed, keepGameRule, preGenerate, this);
        this.tasks.put(execEpochMillis, task);
        RunnableManager.runAsync(this::save);
    }

    public void removeTask(long execEpochMillis) {
        Task task = this.tasks.remove(execEpochMillis);
        if (task != null) task.getRegenerationTask().cancel();
        RunnableManager.runAsync(this::save);
    }

    public void removeTask(Task task) {
        this.removeTask(task.getExecEpochMillis());
    }

    public String getPreGenerateWorldNamePattern() {
        return this.preGenerateWorldNamePattern;
    }

    private String getPregenerateWorldName(String worldName) {
        return String.format(this.getPreGenerateWorldNamePattern()
                .replace("${worldName}", worldName), worldName);
    }

    public String getBackupFolderPattern() {
        return this.backupFolderPattern;
    }

    public String getBackupFolder(String worldName, LocalDateTime dateTime, LocalDateTime lastDateTime) {
        return this.getBackupFolderPattern()
                .replace("${worldName}", worldName)
                .replace("${lastYear}", DateTimeFormatter.ofPattern("yyyy").format(lastDateTime))
                .replace("${lastMonth}", DateTimeFormatter.ofPattern("MM").format(lastDateTime))
                .replace("${lastDay}", DateTimeFormatter.ofPattern("dd").format(lastDateTime))
                .replace("${lastHour}", DateTimeFormatter.ofPattern("HH").format(lastDateTime))
                .replace("${lastMinute}", DateTimeFormatter.ofPattern("mm").format(lastDateTime))
                .replace("${lastSecond}", DateTimeFormatter.ofPattern("ss").format(lastDateTime))
                .replace("${year}", DateTimeFormatter.ofPattern("yyyy").format(dateTime))
                .replace("${month}", DateTimeFormatter.ofPattern("MM").format(dateTime))
                .replace("${day}", DateTimeFormatter.ofPattern("dd").format(dateTime))
                .replace("${hour}", DateTimeFormatter.ofPattern("HH").format(dateTime))
                .replace("${minute}", DateTimeFormatter.ofPattern("mm").format(dateTime))
                .replace("${second}", DateTimeFormatter.ofPattern("ss").format(dateTime));
    }

    public String getBackupFilePattern() {
        return this.backupFilePattern;
    }

    public String getBackupFile(String worldName, LocalDateTime dateTime, LocalDateTime lastDateTime, String ext) {
        return this.getBackupFilePattern()
                .replace("${worldName}", worldName)
                .replace("${lastYear}", DateTimeFormatter.ofPattern("yyyy").format(lastDateTime))
                .replace("${lastMonth}", DateTimeFormatter.ofPattern("MM").format(lastDateTime))
                .replace("${lastDay}", DateTimeFormatter.ofPattern("dd").format(lastDateTime))
                .replace("${lastHour}", DateTimeFormatter.ofPattern("HH").format(lastDateTime))
                .replace("${lastMinute}", DateTimeFormatter.ofPattern("mm").format(lastDateTime))
                .replace("${lastSecond}", DateTimeFormatter.ofPattern("ss").format(lastDateTime))
                .replace("${year}", DateTimeFormatter.ofPattern("yyyy").format(dateTime))
                .replace("${month}", DateTimeFormatter.ofPattern("MM").format(dateTime))
                .replace("${day}", DateTimeFormatter.ofPattern("dd").format(dateTime))
                .replace("${hour}", DateTimeFormatter.ofPattern("HH").format(dateTime))
                .replace("${minute}", DateTimeFormatter.ofPattern("mm").format(dateTime))
                .replace("${second}", DateTimeFormatter.ofPattern("ss").format(dateTime))
                .replace("${extension}", ext);

    }

    public Map<ScriptTiming, List<File>> getScripts(String worldName) {
        return this.scripts.getOrDefault(worldName, Collections.emptyMap());
    }

    public long getLastExec() {
        return this.lastExec;
    }

    public void setLastExec(long epochMillis) {
        this.lastExec = epochMillis;
        this.save();
    }

    public LocalDateTime getLastExecDateTime() {
        return Instant.ofEpochMilli(this.getLastExec()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public Timer getTimer() {
        return this.timer;
    }

    public Map<Long, Task> getTasks() {
        return this.tasks;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        RunnableManager.runAsyncDelayed(() -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                this.tasks.values().forEach(task -> {
                    if (task.isNeedsToPreGenerate()) {
                        task.pregenerateWorlds();
                    }
                });
            }
        }, 20 * 3); // 3 seconds delay
    }
    public static class Task {

        private static int TASK_ID = 0;

        private final Logger logger = LoggerFactory.getLogger("UNC/AutomatedRegenWorldTask #" + TASK_ID++);
        private final TimerTask regenerationTask;
        private final long execEpochMillis;
        private final String[] worldNames;
        private final String seed;
        private final boolean keepGameRule;

        private final boolean preGenerate;

        private final AutomatedRegenWorldManager manager;

        public Task(long execEpochMillis, String[] worldNames, String seed, boolean keepGameRule, boolean preGenerate, AutomatedRegenWorldManager manager) {
            this.execEpochMillis = execEpochMillis;
            this.worldNames = worldNames;
            this.seed = seed;
            this.keepGameRule = keepGameRule;
            this.preGenerate = preGenerate;
            this.manager = manager;

            this.regenerationTask = new TimerTask() {
                @Override
                public void run() {
                    Bukkit.broadcast(Component.text("30秒後に、ワールドの再生成が実行されます。対象のワールドは次の通りです: " + Arrays.stream(Task.this.getWorldNames()).map(MessageUtil::getWorldName).collect(Collectors.joining(", ")), DefinedTextColor.YELLOW, TextDecoration.BOLD));
                    RunnableManager.runAsyncDelayed(() -> {
                        Task.this.run();
                    }, 20 * 30L);
                }
            };

            RunnableManager.runDelayed(() -> {
                this.manager.getTimer().schedule(this.regenerationTask, new Date(this.execEpochMillis));
                this.logger.info("Scheduled regeneration task for worlds: {}", String.join(", ", worldNames));
            }, 1L);
        }

        public void run() {
            this.logger.info("Running regeneration task for worlds: {}", String.join(", ", this.worldNames));
            for (String worldName : this.worldNames) {
                try {
                    if (this.isPreGenerated(worldName)) {
                        this.logger.info("World \"{}\" is pre-generated, using it.", worldName);
                        MultiverseCore.compressWorld(worldName, this.manager.getBackupFolder(worldName, LocalDateTime.now(), this.manager.getLastExecDateTime()), this.manager.getBackupFile(worldName, LocalDateTime.now(), this.manager.getLastExecDateTime(), "tar.zst"), false);
                        MultiverseCore.deleteWorld(worldName, Collections.emptyList(), true);
                        if (MultiverseCore.renameWorld(this.manager.getPregenerateWorldName(worldName), worldName, true, false) && !MultiverseCore.isWorldLoaded(worldName)) {
                            MultiverseCore.loadWorld(worldName);
                        }
                    } else {
                        this.logger.info("Regenerating world \"{}\"", worldName);
                        MultiverseCore.compressWorld(worldName, this.manager.getBackupFolder(worldName, LocalDateTime.now(), this.manager.getLastExecDateTime()), this.manager.getBackupFile(worldName, LocalDateTime.now(), this.manager.getLastExecDateTime(), "tar.zst"), false);
                        this.getManager().getScripts(worldName).getOrDefault(ScriptTiming.BEFORE, Collections.emptyList()).forEach(scriptFile -> {
                            this.executeScript(scriptFile, MultiverseCore.getWorldManager().getLoadedWorld(worldName).getOrNull(), Collections.emptyMap());
                        });
                        MultiverseCore.regenerateWorld(worldName, this.seed, this.keepGameRule);
                        this.getManager().getScripts(worldName).getOrDefault(ScriptTiming.AFTER, Collections.emptyList()).forEach(scriptFile -> {
                            this.executeScript(scriptFile, MultiverseCore.getWorldManager().getLoadedWorld(worldName).getOrNull(), Collections.emptyMap());
                        });
                        this.logger.info("World \"{}\" is regenerated.", worldName);
                    }
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            this.getManager().setLastExec(System.currentTimeMillis());
            this.getManager().removeTask(this);
        }

        public long getExecEpochMillis() {
            return this.execEpochMillis;
        }

        public LocalDateTime getExecDateTime() {
            return Instant.ofEpochMilli(this.execEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        public String[] getWorldNames() {
            return this.worldNames;
        }

        @Nullable
        public String getSeed() {
            return this.seed;
        }

        public boolean keepGameRule() {
            return this.keepGameRule;
        }

        public boolean preGenerate() {
            return this.preGenerate;
        }

        public AutomatedRegenWorldManager getManager() {
            return this.manager;
        }

        public TimerTask getRegenerationTask() {
            return this.regenerationTask;
        }

        public boolean isNeedsToPreGenerate() {
            return this.preGenerate && !this.isPreGenerated();
        }

        public boolean isNeedsToPreGenerate(String worldName) {
            return this.preGenerate && !this.isPreGenerated(worldName);
        }

        public boolean isPreGenerated() {
            return Arrays.stream(this.worldNames).allMatch(this::isPreGenerated);
        }

        public boolean isPreGenerated(String worldName) {
            MultiverseWorld mvWorld = MultiverseCore.getWorldManager().getWorld(this.manager.getPregenerateWorldName(worldName)).getOrNull();
            return mvWorld != null;
        }

        public boolean pregenerateWorlds() {
            if (!this.isNeedsToPreGenerate()) {
                return true;
            }

            this.logger.info("Pre-generating worlds: {}", String.join(", ", this.worldNames));
            boolean allSuccess = true;

            for (String worldName : this.worldNames) {
                if (!this.isPreGenerated(worldName)) {
                    this.logger.info("Pre-generating world \"{}\"", worldName);
                    Throwable thrown = null;
                    try {
                        boolean success = pregenerateWorld(this, worldName, this.seed, this.keepGameRule, false);
                        if (success) {
                            this.logger.info("World \"{}\" successfully pre-generated", worldName);
                            continue;
                        }
                    } catch(Throwable t) {
                        thrown = t;
                    }
                    this.logger.error("Failed to pre-generate world \"" + worldName + "\"", thrown);
                    allSuccess = false;
                } else {
                    this.logger.info("World \"{}\" is already pre-generated, skipping!", worldName);
                }
            }

            return allSuccess;
        }

        public static boolean pregenerateWorld(Task task, String worldName, @Nullable String seed, boolean keepGameRules, boolean loadWorld) throws IllegalArgumentException {
            LoadedMultiverseWorld baseLoadedMultiverseWorld = MultiverseCore.getWorldManager().getLoadedWorld(worldName).getOrElseThrow(() -> new IllegalArgumentException("World " + worldName + " is not loaded."));

            // Exec script before pre-generation
            task.getManager().getScripts(worldName).getOrDefault(ScriptTiming.BEFORE_PRE_GENERATE, Collections.emptyList()).forEach(scriptFile -> {
                task.executeScript(scriptFile, baseLoadedMultiverseWorld, Collections.emptyMap());
            });

            boolean isCloneSuccess;
            try {
                isCloneSuccess = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> MultiverseCore.cloneWorld(baseLoadedMultiverseWorld, task.getManager().getPregenerateWorldName(worldName), keepGameRules, true, true, true, true, true)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Failed to clone world " + worldName, e);
            }

            if (isCloneSuccess) {
                LoadedMultiverseWorld loadedCloneWorld = MultiverseCore.getWorldManager().getLoadedWorld(task.getManager().getPregenerateWorldName(worldName)).getOrElseThrow(() -> new IllegalStateException("Failed to get loaded world after cloning: " + task.getManager().getPregenerateWorldName(worldName)));
                loadedCloneWorld.setAlias(null);

                try {
                    boolean isRegenSuccess = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> MultiverseCore.regenerateWorld(loadedCloneWorld, seed, true)).get();

                    if (isRegenSuccess) {
                        // Exec script after pre-generation
                        task.getManager().getScripts(worldName).getOrDefault(ScriptTiming.AFTER_PRE_GENERATE, Collections.emptyList()).forEach(scriptFile -> {
                            task.executeScript(scriptFile, loadedCloneWorld, Collections.emptyMap());
                        });

                        LoadedMultiverseWorld regeneratedWorld = MultiverseCore.getWorldManager().getLoadedWorld(task.getManager().getPregenerateWorldName(worldName)).getOrElseThrow(() -> new IllegalStateException("Something went wrong. Failed to get regenerated world: " + task.getManager().getPregenerateWorldName(worldName)));
                        if (!loadWorld) {
                            MultiverseCore.unloadWorld(regeneratedWorld, true, true);
                            return MultiverseCore.getWorldManager().getLoadedWorld(regeneratedWorld).isEmpty();
                        }

                        return true;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    throw new IllegalStateException("Failed to regenerate world " + loadedCloneWorld.getName(), e);
                }
            }

            return false;
        }

        public void executeScript(File scriptFile, MultiverseWorld world, Map<String, Object> vars) {
            ScriptableObject scope = EvalManager.getRhinoContextFactory().enterContext().initStandardObjects();
            ScriptableObject.putConstProperty(scope, "Bukkit", new NativeJavaClass(scope, Bukkit.class));

            ScriptableObject.putConstProperty(scope, "Storage", EvalManager.getGlobalStorage());
            ScriptableObject.putConstProperty(scope, "world", world);
            ScriptableObject.putConstProperty(scope, "manager", this.getManager());
            ScriptableObject.putConstProperty(scope, "task", this);
            vars.forEach((k, v) -> ScriptableObject.putConstProperty(scope, k, v));
            try {
                this.logger.info("Executing script: " + scriptFile.getAbsolutePath());
                EvalManager.execFromString(scriptFile.getName(), Files.readString(scriptFile.toPath()), scope);
            } catch (IOException e) {
                this.logger.error("Failed to read script file: " + scriptFile.getAbsolutePath(), e);
            }
        }

        @Override
        public String toString() {
            return "Task{" +
                    "execEpochMillis=" + execEpochMillis +
                    ", worldNames=" + Arrays.toString(worldNames) +
                    ", seed='" + seed + '\'' +
                    ", keepGameRule=" + keepGameRule +
                    ", preGenerate=" + preGenerate +
                    ", manager=" + manager.getFileName() +
                    '}';
        }
    }

    public enum ScriptTiming {
        BEFORE_PRE_GENERATE,
        AFTER_PRE_GENERATE,
        BEFORE,
        AFTER
    }
}
