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

package net.unknown.core.advancements;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.*;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StrictJsonParser;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.advancements.event.*;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.packet.event.PacketSendingEvent;
import net.unknown.core.packet.listener.OutgoingPacketListener;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.ReflectionUtil;
import net.unknown.launchwrapper.mixininterfaces.IMixinCriterionTrigger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AdvancementManager extends OutgoingPacketListener<ClientboundUpdateAdvancementsPacket> implements Listener {
    public static final AdvancementManager INSTANCE = new AdvancementManager();
    private static final Logger LOGGER = Logger.getLogger("UNC/Advancements");

    private static final Map<ResourceLocation, AdvancementHolder> ADVANCEMENTS = new HashMap<>();
    private static final Map<ResourceLocation, Pair<Float, Float>> ADVANCEMENT_POSITIONS = new HashMap<>();
    private static AdvancementPlacingBehaviour PLACING_BEHAVIOUR = AdvancementPlacingBehaviour.ROOT_LEFT_TOP_GRID;
    private static final Map<UUID, Map<ResourceLocation, AdvancementProgress>> PROGRESSES = new HashMap<>();

    private AdvancementManager() {
        if (!UnknownNetworkCorePlugin.isBootstrapped()) throw new IllegalStateException("To use AdvancementManager must be server launched by UnknownNetworkBootstrap environment.");
    }

    /**
     * Load advancements from the advancements directory.
     * If clearExists is true, existing advancements will be cleared and listeners will be unregistered.
     *
     * @param clearExists Whether to clear existing advancements and unregister listeners.
     */
    public static synchronized void loadAdvancements(boolean clearExists) {
        if (clearExists) {
            Bukkit.getOnlinePlayers().parallelStream().map(MinecraftAdapter::player).forEach(AdvancementManager::unregisterListeners);
            ClientboundUpdateAdvancementsPacket clearPacket = new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), ADVANCEMENTS.keySet(), Collections.emptyMap(), true);
            Bukkit.getOnlinePlayers().parallelStream().map(MinecraftAdapter::player).forEach(player -> player.connection.send(clearPacket));
            ADVANCEMENTS.clear();
            ADVANCEMENT_POSITIONS.clear();
        }

        final File customAdvancementsDir = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "advancement/custom");
        final Path customAdvancementsDirPath = customAdvancementsDir.toPath();
        final RegistryOps<JsonElement> serializationContext = MinecraftServer.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        try {
            Map<ResourceLocation, Advancement> advancements = new HashMap<>();
            AtomicInteger loadCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            try (Stream<Path> tree = Files.walk(customAdvancementsDirPath)) {
                tree.parallel().map(Path::toFile).filter(file -> !file.isDirectory() && file.getName().endsWith(".json")).forEach(file -> {
                    try {
                        Pair<ResourceLocation, Advancement> parseResult = loadAdvancement(null, customAdvancementsDirPath, serializationContext, file);
                        advancements.put(parseResult.getFirst(), parseResult.getSecond());
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "An error occurred while loading custom advancement from file " + file.getAbsolutePath(), e);
                        failCount.incrementAndGet();
                    }
                });
            }

            advancements.forEach((id, advancement) -> {
                register(new AdvancementHolder(id, advancement));
                LOGGER.info("Loaded advancement " + id);
                loadCount.incrementAndGet();
            });

            LOGGER.info("Loaded " + loadCount.get() + " custom advancement(s)" + (failCount.get() > 0 ? ", but failed to load " + failCount.get() + " custom advancement(s)" : "") + ".");
            Bukkit.getOnlinePlayers().parallelStream().map(MinecraftAdapter::player).forEach(AdvancementManager::registerListeners);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "An error occurred while loading custom advancements", e);
        }
    }

    public static Pair<ResourceLocation, Advancement> loadAdvancement(ResourceLocation id, @Nullable RegistryOps<JsonElement> serializationContext, String customAdvancementJson) throws IOException {
        if (id == null) throw new IllegalArgumentException("Advancement ID cannot be null.");
        LOGGER.info("Loading advancement " + id);
        CustomAdvancementPreLoadEvent preLoadEvent = new CustomAdvancementPreLoadEvent(id, customAdvancementJson);
        if (!preLoadEvent.callEvent()) return null;
        id = preLoadEvent.getId();

        /* Raw String JSON -> JSON Object */
        JsonElement elementObj = preLoadEvent.getJson(true);
        if (elementObj == null || !elementObj.isJsonObject() || !(elementObj instanceof JsonObject jsonObj)) { // Error Handling
            throw new IllegalArgumentException("Invalid JSON in advancement");
        }

        /* Custom Criteria Parsing */
        // e.g.
        // { "0..100": { "trigger": "minecraft:impossible" }}
        // expand to:
        // { "0": { "trigger": "minecraft:impossible" }, "1": { "trigger": "minecraft:impossible" }, ..., "100": { "trigger": "minecraft:impossible" }}
        if (jsonObj.has("criteria") && jsonObj.get("criteria").isJsonObject() && jsonObj.get("criteria") instanceof JsonObject criteriaObjList) {
            criteriaObjList.keySet().stream().filter(AdvancementManager::isRangedKey).forEach(rangedCriteria -> {
                JsonObject basedCriteria = criteriaObjList.getAsJsonObject(rangedCriteria);
                criteriaObjList.remove(rangedCriteria);

                try {
                    parseRangedKey(rangedCriteria).forEach(i -> criteriaObjList.add(String.valueOf(i), basedCriteria));
                } catch(IllegalArgumentException e) {
                    throw new IllegalStateException("CustomParsing: RangedCriteria: Invalid number format in criteria range");
                }
            });
        }

        // e.g.
        // { "requirements": [ ["0..10", "20..30"], ["50..60"] ] }
        // expand to:
        // { "requirements": [ ["0", "1", ..., "9", "10", "20", "21", ..., "29", "30"], ["50", "51", ..., "59", "60"] ] }
        if (jsonObj.has("requirements") && jsonObj.get("requirements").isJsonArray() && jsonObj.get("requirements") instanceof JsonArray requirementsArrList) {
            requirementsArrList.forEach(element -> {
                if (element.isJsonArray() && element instanceof JsonArray requirements) {
                    requirements.deepCopy().forEach(mayCriteriaElement -> {
                        String criteria = mayCriteriaElement.getAsString();
                        if (!isRangedKey(criteria)) return;

                        requirements.remove(mayCriteriaElement);

                        try {
                            parseRangedKey(criteria).forEach(i -> requirements.add(String.valueOf(i)));
                        } catch(IllegalArgumentException e) {
                            throw new IllegalStateException("CustomParsing: RangedRequirements: Invalid number format in requirements range");
                        }
                    });
                }
            });
        }

        /* JSON Object -> Advancement */
        DataResult<Pair<Advancement, JsonElement>> result = CustomAdvancementNetworkCodecs.ADVANCEMENT_CODEC.decode(serializationContext == null ? MinecraftServer.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE) : serializationContext, jsonObj);
        Optional<Pair<Advancement, JsonElement>> advancement = result.result();
        advancement.orElseThrow(() -> new IllegalArgumentException("Failed to decode advancement from JSON", new IllegalStateException(result.error().get().message())));

        return Pair.of(id, advancement.get().getFirst());
    }

    public static Pair<ResourceLocation, Advancement> loadAdvancement(ResourceLocation id, @Nullable RegistryOps<JsonElement> serializationContext, File customAdvancementFile) throws IOException {
        return loadAdvancement(id, serializationContext, String.join("\n", Files.readAllLines(customAdvancementFile.toPath())));
    }

    public static Pair<ResourceLocation, Advancement> loadAdvancement(@Nullable String namespace, Path root, @Nullable RegistryOps<JsonElement> serializationContext, File customAdvancementFile) throws IOException {
        ResourceLocation id = parseIdFromPath(namespace == null ? "unknown-network" : namespace, root, customAdvancementFile.toPath());
        if (id == null) throw new IllegalArgumentException("Failed to parse advancement ID from path: root=" + root + ", file=" + customAdvancementFile.toPath());
        return loadAdvancement(id, serializationContext, customAdvancementFile);
    }

    /**
     * Load advancement progress for a player.
     *
     * @param playerUniqueId Player UUID
     */
    public static void loadProgress(UUID playerUniqueId) {
        File dir = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "advancement/progresses");
        File progressFile = new File(dir, playerUniqueId.toString() + ".json");
        if (progressFile.exists()) {
            try {
                JsonElement element = JsonParser.parseString(String.join("\n", Files.readAllLines(progressFile.toPath())));
                if (element != null && element.isJsonObject() && element instanceof JsonObject object) {
                    object.keySet().stream().map(ResourceLocation::tryParse).forEach(id -> {
                        try {
                            AdvancementProgress progress = AdvancementProgress.CODEC.decode(MinecraftServer.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE), object.get(id.toString())).getOrThrow().getFirst();
                            Optional.ofNullable(getAdvancement(id)).ifPresentOrElse(advHolder -> progress.update(advHolder.value().requirements()), () -> LOGGER.warning("Unknown advancement " + id + " found in progress file for player " + playerUniqueId + ", skipping requirements update. May its bug, inspect the file."));
                            PROGRESSES.computeIfAbsent(playerUniqueId, k -> new HashMap<>()).put(id, progress);
                        } catch(Throwable t) {
                            LOGGER.warning("An error occurred while loading progress for advancement " + id + " for player " + playerUniqueId + ": " + t.getLocalizedMessage());
                            t.printStackTrace();
                        }
                    });
                }
            } catch(IOException e) {
                LOGGER.warning("An error occurred while loading progress file for player " + playerUniqueId + ": " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }


    /**
     * Save advancement progress for a player.
     *
     * @param playerUniqueId Player UUID
     */
    public static synchronized void saveProgress(UUID playerUniqueId) {
        File dir = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "advancement/progresses");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create advancements directory: " + dir.getAbsolutePath());
        }
        File progressFile = new File(dir, playerUniqueId.toString() + ".json");
        JsonObject jsonObject = new JsonObject();
        PROGRESSES.getOrDefault(playerUniqueId, Collections.emptyMap()).forEach((id, progress) -> {
            DataResult<JsonElement> result = AdvancementProgress.CODEC.encodeStart(MinecraftServer.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE), progress);
            result.result().ifPresent(jsonElement -> jsonObject.add(id.toString(), jsonElement));
        });
        try {
            Files.write(progressFile.toPath(), Collections.singletonList(jsonObject.toString()));
        } catch (Exception e) {
            LOGGER.warning("An error occurred while saving progress for player " + playerUniqueId + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Purge advancement progress for a player.
     * This will remove all progress data from memory.
     *
     * @param playerUniqueId Player UUID
     * @param save Whether to save progress before purging
     */
    public static void purgeProgress(UUID playerUniqueId, boolean save) {
        if (save) saveProgress(playerUniqueId);
        PROGRESSES.remove(playerUniqueId);
    }

    /**
     * Register an advancement.
     * Duplicate IDs will overwrite existing advancements.
     *
     * @param advancementHolder AdvancementHolder to register
     */
    public static void register(AdvancementHolder advancementHolder) {
        CustomAdvancementLoadEvent loadEvent = new CustomAdvancementLoadEvent(advancementHolder.id(), advancementHolder.value());
        if (!loadEvent.callEvent()) return;

        if (ADVANCEMENTS.containsKey(loadEvent.getId())) LOGGER.warning("Advancement " + loadEvent.getId() + " is already registered. Overwriting.");
        ADVANCEMENTS.put(loadEvent.getId(), loadEvent.getHolder());
    }

    public static void register(ResourceLocation id, Advancement advancement) {
        register(new AdvancementHolder(id, advancement));
    }

    public static void register(Pair<ResourceLocation, Advancement> pair) {
        register(new AdvancementHolder(pair.getFirst(), pair.getSecond()));
    }

    /**
     * Reset advancement progress for a player.
     * This will set all criteria to uncompleted state.
     *
     * @param uniqueId Player UUID
     * @param id Advancement ID
     */
    public static void resetProgress(UUID uniqueId, ResourceLocation id) {
        if (!ADVANCEMENTS.containsKey(id)) throw new IllegalArgumentException("Unknown advancement " + id);
        Advancement advancement = ADVANCEMENTS.get(id).value();
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(advancement.requirements());
        PROGRESSES.computeIfAbsent(uniqueId, k -> new HashMap<>()).put(id, progress);
        RunnableManager.runAsync(() -> saveProgress(uniqueId));
    }

    /**
     * Reset advancement progress for a player.
     * This will set all criteria to uncompleted state.
     *
     * @param uniqueId Player UUID
     * @param holder AdvancementHolder
     */
    public static void resetProgress(UUID uniqueId, AdvancementHolder holder) {
        resetProgress(uniqueId, holder.id());
    }

    /**
     * Reset advancement progress for a player.
     * This will set all criteria to uncompleted state.
     *
     * @param uniqueId Player UUID
     * @param advancement Advancement
     */
    public static void resetProgress(UUID uniqueId, Advancement advancement) {
        resetProgress(uniqueId, ADVANCEMENTS.entrySet().stream().filter(e -> e.getValue().value().equals(advancement)).findAny().orElseThrow(() -> new IllegalArgumentException("Unknown advancement " + advancement)).getKey());
    }

    /**
     * Get an advancement by its ID.
     *
     * @param id Advancement ID
     * @return AdvancementHolder or null if not found
     */
    public static AdvancementHolder getAdvancement(ResourceLocation id) {
        return ADVANCEMENTS.getOrDefault(id, null);
    }

    /**
     * Get advancement progress for a player.
     * If the player has no progress for the advancement, a new progress will be created and returned.
     *
     * @param player ServerPlayer
     * @param id Advancement ID
     * @return AdvancementProgress
     */
    public static AdvancementProgress getProgress(ServerPlayer player, ResourceLocation id) {
        if (getAdvancement(id) == null) throw new IllegalArgumentException("Unknown advancement " + id);
        Map<ResourceLocation, AdvancementProgress> progresses = PROGRESSES.getOrDefault(player.getUUID(), null);
        if (progresses == null || !progresses.containsKey(id)) {
            resetProgress(player.getUUID(), id);
            progresses = PROGRESSES.getOrDefault(player.getUUID(), Collections.emptyMap()); // Reload after reset
        }
        AdvancementProgress progress = progresses.get(id);
        progress.update(getAdvancement(id).value().requirements());
        return progress;
    }

    /**
     * Get advancement progress for a player.
     * If the player has no progress for the advancement, a new progress will be created and returned.
     *
     * @param player ServerPlayer
     * @param advancementHolder AdvancementHolder
     * @return AdvancementProgress
     */
    public static AdvancementProgress getProgress(ServerPlayer player, AdvancementHolder advancementHolder) {
        return getProgress(player, advancementHolder.id());
    }

    /**
     * Get advancement progress for a player.
     * If the player has no progress for the advancement, a new progress will be created and returned.
     *
     * @param player ServerPlayer
     * @param advancement Advancement
     * @return AdvancementProgress
     */
    public static AdvancementProgress getProgress(ServerPlayer player, Advancement advancement) {
        return getProgress(player, ADVANCEMENTS.entrySet().stream().filter(e -> e.getValue().value().equals(advancement)).findAny().orElseThrow(() -> new IllegalArgumentException("Unknown advancement provided")).getKey());
    }

    /**
     * Grant a criterion for a player.
     * If the criterion is already granted, nothing will happen.
     *
     * @param player ServerPlayer
     * @param id Advancement ID
     * @param name Criterion name
     * @return true if the criterion was granted, false if it was already granted
     */
    public static boolean grantProgress(ServerPlayer player, ResourceLocation id, String name) {
        AdvancementProgress progress = getProgress(player, id);
        boolean granted = progress.grantProgress(name);
        RunnableManager.runAsync(() -> saveProgress(player.getUUID()));
        AdvancementHolder holder = getAdvancement(id);

        if (granted || progress.getCriterion(name).isDone()) {
            unregisterListener(player, holder, name);
        }

        if (granted) {
            ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Collections.emptySet(), Collections.singletonMap(id, progress), true);
            player.connection.send(packet);
            Bukkit.getPluginManager().callEvent(new CustomAdvancementCriteriaGrantedEvent(!Bukkit.isPrimaryThread(), player, getAdvancement(id), progress, name));
            if (progress.isDone()) {
                Bukkit.getPluginManager().callEvent(new CustomAdvancementCompletedEvent(!Bukkit.isPrimaryThread(), player, getAdvancement(id), progress));
            }
            return true;
        }
        return false;
    }

    /**
     * Grant a criterion for a player.
     * If the criterion is already granted, nothing will happen.
     *
     * @param player ServerPlayer
     * @param advancementHolder AdvancementHolder
     * @param name Criterion name
     * @return true if the criterion was granted, false if it was already granted
     */
    public static boolean grantProgress(ServerPlayer player, AdvancementHolder advancementHolder, String name) {
        return grantProgress(player, advancementHolder.id(), name);
    }

    /**
     * Grant a criterion for a player.
     * If the criterion is already granted, nothing will happen.
     *
     * @deprecated To be slow; use AdvancementHolder or ResourceLocation variant instead. Because it needs to search the map for the advancement.
     * @param player ServerPlayer
     * @param advancement Advancement
     * @param name Criterion name
     * @return true if the criterion was granted, false if it was already granted
     */
    @Deprecated
    public static boolean grantProgress(ServerPlayer player, Advancement advancement, String name) {
        return grantProgress(player, ADVANCEMENTS.entrySet().stream().filter(e -> e.getValue().value().equals(advancement)).findAny().orElseThrow(() -> new IllegalArgumentException("Unknown advancement provided")).getKey(), name);
    }

    /**
     * Revoke a criterion for a player.
     * If the criterion is not granted, nothing will happen.
     *
     * @param player ServerPlayer
     * @param id Advancement ID
     * @param name Criterion name
     * @return true if the criterion was revoked, false if it was not granted
     */
    public static boolean revokeProgress(ServerPlayer player, ResourceLocation id, String name) {
        AdvancementProgress progress = getProgress(player, id);
        boolean revoked = progress.revokeProgress(name);
        RunnableManager.runAsync(() -> saveProgress(player.getUUID()));
        if (revoked) {
            AdvancementHolder holder = getAdvancement(id);
            registerListener(player, holder, name);
            ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), Collections.emptySet(), Collections.singletonMap(id, progress), true);
            player.connection.send(packet);
            Bukkit.getPluginManager().callEvent(new CustomAdvancementCriteriaRevokedEvent(!Bukkit.isPrimaryThread(), player, getAdvancement(id), progress, name));
            return true;
        }

        return false;
    }

    /**
     * Parse a ResourceLocation from a file path.
     * The path will be relativized to the base path, and the namespace will be prepended.
     * The file extension will be removed, and any leading "../" or "./" will be removed.
     *
     * @param namespace Namespace to use
     * @param a Base path
     * @param b File path
     * @return ResourceLocation or null if the path is invalid
     */
    public static ResourceLocation parseIdFromPath(String namespace, Path a, Path b) {
        String relativePath = a.relativize(b).toString();
        if (File.separatorChar != '/') relativePath = relativePath.replace(File.separatorChar, '/');
        if (relativePath.endsWith(".json")) relativePath = relativePath.substring(0, relativePath.length() - 5);
        if (relativePath.startsWith("../")) relativePath = relativePath.substring(3);
        if (relativePath.startsWith("./")) relativePath = relativePath.substring(2);
        return ResourceLocation.tryBuild(namespace, relativePath);
    }

    /**
     * Register listeners for a player.
     * This will register listeners for all advancements and their criteria.
     *
     * @param player ServerPlayer
     */
    public static void registerListeners(ServerPlayer player) {
        ADVANCEMENTS.forEach((id, adv) -> registerListener(player, adv));
    }

    /**
     * Register a listener for a player, an advancement, and a criterion.
     * This will register a listener for the specified criterion of the advancement.
     * If the criterion is already completed, nothing will happen.
     *
     * @param player ServerPlayer
     * @param holder AdvancementHolder
     * @param criterionKey Criterion key
     */
    public static void registerListener(ServerPlayer player, AdvancementHolder holder, String criterionKey) {
        Criterion<?> criterion = holder.value().criteria().get(criterionKey);
        if (criterion == null) throw new IllegalArgumentException("Unknown criterion " + criterionKey + " for advancement " + holder.id());
        AdvancementProgress progress = getProgress(player, holder.id());
        if (progress == null || progress.getCriterion(criterionKey) == null || !progress.getCriterion(criterionKey).isDone() || !progress.isDone()) {
            registerListener(player, holder, criterionKey, criterion);
        }
    }

    /**
     * Register listeners for a player and an advancement.
     * This will register listeners for all criteria of the advancement.
     *
     * @param player ServerPlayer
     * @param holder AdvancementHolder
     */
    public static void registerListener(ServerPlayer player, AdvancementHolder holder) {
        holder.value().criteria().forEach((key, criterion) -> {
            AdvancementProgress progress = getProgress(player, holder.id());
            if (progress == null || progress.getCriterion(key) == null || !progress.getCriterion(key).isDone() || !progress.isDone()) {
                registerListener(player, holder, key, criterion);
            }
        });
    }

    /**
     * Register a listener for a player's criterion.
     * This method uses mixins to inject a custom listener into the vanilla listener.
     *
     * @param player ServerPlayer
     * @param advancement AdvancementHolder
     * @param criterionKey Criterion key
     * @param criterion Criterion
     * @param <T> CriterionTriggerInstance type
     */
    private static <T extends CriterionTriggerInstance> void registerListener(ServerPlayer player, AdvancementHolder advancement, String criterionKey, Criterion<T> criterion) {
        unregisterListener(player, advancement, criterion, criterionKey);
        CriterionTrigger.Listener<T> vanillaListener = new CriterionTrigger.Listener<>(criterion.triggerInstance(), advancement, criterionKey);
        if (((Object) vanillaListener) instanceof IMixinCriterionTrigger.Listener bootstrappedListener) {
            bootstrappedListener.setCustomListener((playerAdvancements) -> {
                AdvancementManager.grantProgress(player, vanillaListener.advancement(), vanillaListener.criterion());
            });
            criterion.trigger().addPlayerListener(player.getAdvancements(), vanillaListener);
        } else {
            throw new IllegalStateException("Unsupported environment: CriterionTrigger.Listener is not bootstrapped. Try updating or use UnknownNetworkBootstrap.");
        }
    }


    /**
     * Unregister listeners for a player.
     * This will unregister listeners for all advancements and their criteria.
     *
     * @param player ServerPlayer
     */
    public static <C extends CriterionTriggerInstance, T extends CriterionTrigger<C>> void unregisterListeners(ServerPlayer player) {
        new HashMap<>(player.getAdvancements().criterionData).forEach((criterion, listeners) -> {
            new HashSet<>(listeners).stream().filter(listener -> ADVANCEMENTS.containsKey(listener.advancement().id())).forEach(listener -> {
                unregisterListener(player, (T) criterion, (CriterionTrigger.Listener<C>) listener);
            });
        });
    }

    /**
     * Unregister a listener for a player's criterion by criterion key.
     * This will unregister the listener for the specified criterion of the advancement.
     * If the criterion is not found, nothing will happen.
     *
     * @param player ServerPlayer
     * @param criterionKey Criterion key
     * @param <C> CriterionTriggerInstance type
     * @param <T> CriterionTrigger type
     */
    public static <C extends CriterionTriggerInstance, T extends CriterionTrigger<C>>  void unregisterListener(ServerPlayer player, String criterionKey) {
        new HashMap<>(player.getAdvancements().criterionData).forEach((criterion, listeners) -> {
            new HashSet<>(listeners).stream().filter(listener -> ADVANCEMENTS.containsKey(listener.advancement().id()) && listener.criterion().equals(criterionKey)).forEach(listener -> {
                unregisterListener(player, (T) criterion, (CriterionTrigger.Listener<C>) listener);
            });
        });
    }

    /**
     * Unregister a listener for a player's criterion by advancement and criterion key.
     * This will unregister the listener for the specified criterion of the advancement.
     * If the criterion is not found, nothing will happen.
     *
     * @param player ServerPlayer
     * @param holder AdvancementHolder
     * @param criterionKey Criterion key
     * @param <C> CriterionTriggerInstance type
     * @param <T> CriterionTrigger type
     */
    public static <C extends CriterionTriggerInstance, T extends CriterionTrigger<C>>  void unregisterListener(ServerPlayer player, AdvancementHolder holder, String criterionKey) {
        new HashMap<>(player.getAdvancements().criterionData).forEach((criterion, listeners) -> {
            new HashSet<>(listeners).stream().filter(listener -> listener.advancement().id().equals(holder.id()) && listener.criterion().equals(criterionKey)).forEach(listener -> {
                unregisterListener(player, (T) criterion, (CriterionTrigger.Listener<C>) listener);
            });
        });
    }

    /**
     * Unregister a listener for a player's criterion by advancement, criterion, and criterion key.
     * This will unregister the listener for the specified criterion of the advancement.
     * If the criterion is not found, nothing will happen.
     *
     * @param player ServerPlayer
     * @param holder AdvancementHolder
     * @param criterion Criterion
     * @param criterionKey Criterion key
     * @param <C> CriterionTriggerInstance type
     * @param <T> CriterionTrigger type
     */
    public static <C extends CriterionTriggerInstance, T extends CriterionTrigger<C>> void unregisterListener(ServerPlayer player, AdvancementHolder holder, Criterion<C> criterion, String criterionKey) {
        new HashMap<>(player.getAdvancements().criterionData).forEach((c, listeners) -> {
            if (!c.equals(criterion)) return;
            new HashSet<>(listeners).stream().filter(listener -> listener.advancement().id().equals(holder.id()) && listener.criterion().equals(criterionKey)).forEach(listener -> {
                unregisterListener(player, (T) criterion.trigger(), (CriterionTrigger.Listener<C>) listener);
            });
        });
    }

    private static <T extends CriterionTriggerInstance> void unregisterListener(ServerPlayer player, Criterion<T> criterion, CriterionTrigger.Listener<T> listener) {
        unregisterListener(player, criterion.trigger(), listener);
    }

    private static <C extends CriterionTriggerInstance, T extends CriterionTrigger<C>> void unregisterListener(ServerPlayer player, T trigger, CriterionTrigger.Listener<C> listener) {
        trigger.removePlayerListener(player.getAdvancements(), listener);
    }

    /**
     * Send advancements to a player.
     *
     * @param clearExists Whether to clear existing advancements on the client
     * @param player ServerPlayer
     * @param showAdvancements Whether to show the advancements UI on the client
     * @param mergeVanilla Whether to merge vanilla advancements with custom advancements.
     */
    public static void send(boolean clearExists, ServerPlayer player, boolean showAdvancements, boolean mergeVanilla) {
        List<AdvancementHolder> toEarn = new ArrayList<>();
        Set<ResourceLocation> toRemove = new HashSet<>();
        Map<ResourceLocation, AdvancementProgress> toSetProgress = new HashMap<>();

        if (mergeVanilla) {
            VanillaAdvancementLoader loader = new VanillaAdvancementLoader(player.getAdvancements(), MinecraftServer.getServer().getAdvancements());
            try {
                loader.load();
            } catch(Throwable t) {
                LOGGER.warning("An error occurred while loading vanilla advancements for player " + player.getUUID() + ": " + t.getLocalizedMessage());
                t.printStackTrace();
            }

            toEarn.addAll(loader.toAdd());
            toRemove.addAll(loader.toRemove());
            toSetProgress.putAll(loader.toSetProgress());

            // TODO: its not working, need to investigate
        }

        ADVANCEMENTS.forEach((id, advancement) -> {
            if(!PROGRESSES.containsKey(id)) return;
            toEarn.add(advancement);
            toSetProgress.put(id, getProgress(player, id));
        });

        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(clearExists, toEarn, toRemove, toSetProgress, showAdvancements);
        player.connection.send(packet);
    }

    private static boolean isRangedKey(String key) {
        return key.matches("^\\d+\\.\\.\\d+$");
    }

    private static IntStream parseRangedKey(String key) throws IllegalArgumentException, NumberFormatException {
        if (!isRangedKey(key)) throw new IllegalArgumentException("Invalid ranged key: " + key);
        String[] parts = key.split("\\.\\.", 2);
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[1]);
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return IntStream.range(min, max);
    }

    /**
     * Initialize advancement positions for placing behaviour.
     *
     * @deprecated This method is unused. Use data-driven custom positioning instead. {"display": { "x": 1.0, "y": 2.0 }}
     * @param advancements Map of advancements
     */
    @Deprecated
    private static void initAdvancementPosition(Map<ResourceLocation, Advancement> advancements) {
        advancements.entrySet()
                .stream()
                .filter(e -> e.getValue().isRoot())
                .forEach(e -> {
                    ADVANCEMENT_POSITIONS.put(e.getKey(), Pair.of(0f, 0f));
                    e.getValue().display().ifPresent(display -> display.setLocation(0f, 0f));
                });
    }

    /**
     * Place advancement gracefully according to the placing behaviour.
     *
     * @deprecated This method is unused. Use data-driven custom positioning instead. {"display": { "x": 1.0, "y": 2.0 }}
     * @param id Advancement ID
     * @param advancement Advancement
     */
    @Deprecated
    private static void placeAdvancementGracefully(ResourceLocation id, Advancement advancement) {
        if (PLACING_BEHAVIOUR == AdvancementPlacingBehaviour.ROOT_LEFT_TOP_GRID) {
            if (!advancement.isRoot()) {
                ResourceLocation parentId = advancement.parent().orElse(null);
                if (parentId == null) {
                    LOGGER.warning("Could not find parent advancement for advancement " + id + ", skipping positioning.");
                    return;
                }

                Pair<Float, Float> pos = ADVANCEMENT_POSITIONS.getOrDefault(parentId, null);
                if (pos == null) {
                    LOGGER.warning("Could not find position for parent advancement " + parentId + " of advancement " + id + ", skipping positioning.");
                    return;
                }

                float x = pos.getFirst();
                float y = pos.getSecond();

                if (x == 0 && y == 0) y = 1;
                else if (x < 7 && y != 0) x += 1;
                else {
                    x = 0;
                    y += 1;
                }
                final float fX = x, fY = y;
                advancement.display().ifPresent(display -> display.setLocation(fX, fY));
                ADVANCEMENT_POSITIONS.put(parentId, Pair.of(x, y));
            }
        } else {
            LOGGER.warning("Unsupported advancement placing behaviour: " + PLACING_BEHAVIOUR + ", skipping positioning.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        registerListeners(MinecraftAdapter.player(event.getPlayer()));
        loadProgress(event.getPlayer().getUniqueId());
    }

    /**
     * Handle outgoing ClientboundUpdateAdvancementsPacket to merge existing advancements and progress if the packet is a full reset.
     * This ensures that custom advancements and progress are not lost when the server sends a full reset packet.
     * PlayerAdvancements#flushDirty is calling after PlayerJoinEvent, so this listener will be called after that.
     *
     * @param event PacketSendingEvent
     */
    @Override
    public void onSendingPacket(PacketSendingEvent<ClientboundUpdateAdvancementsPacket> event) {
        boolean clearExisting = event.getPacket().shouldReset();
        List<AdvancementHolder> added = new ArrayList<>(event.getPacket().getAdded());
        Set<ResourceLocation> removed = new HashSet<>(event.getPacket().getRemoved());
        Map<ResourceLocation, AdvancementProgress> progressMap = new HashMap<>(event.getPacket().getProgress());
        boolean showAdvancements = event.getPacket().shouldShowAdvancements();

        if (clearExisting) {
            added.addAll(ADVANCEMENTS.values());

            UUID player = event.getPlayer().getUniqueId();
            progressMap.putAll(PROGRESSES.get(player));
            event.setPacket(new ClientboundUpdateAdvancementsPacket(true, added, removed, progressMap, showAdvancements));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unregisterListeners(MinecraftAdapter.player(event.getPlayer()));
        purgeProgress(event.getPlayer().getUniqueId(), true);
    }

    private static class VanillaAdvancementLoader {
        private final PlayerAdvancements parent;
        private final ServerAdvancementManager manager;
        private final Path playerSavePath;
        private AdvancementTree tree;
        private final Codec<?> codec;

        private final Set<AdvancementHolder> toAdd = new HashSet<>();
        private final Set<ResourceLocation> toRemove = new HashSet<>();
        private final Map<ResourceLocation, AdvancementProgress> toSetProgress = new HashMap<>();

        public VanillaAdvancementLoader(PlayerAdvancements parent, ServerAdvancementManager manager) {
            this.parent = parent;
            this.manager = manager;

            try {
                Field playerSavePathField = PlayerAdvancements.class.getDeclaredField("playerSavePath");
                Field treeField = PlayerAdvancements.class.getDeclaredField("tree");
                Field codecField = PlayerAdvancements.class.getDeclaredField("codec");
                if (playerSavePathField.trySetAccessible() && treeField.trySetAccessible() && codecField.trySetAccessible()) {
                    this.playerSavePath = (Path) playerSavePathField.get(this.parent);
                    this.tree = (AdvancementTree) treeField.get(this.parent);
                    this.codec = (Codec<?>) codecField.get(this.parent);
                } else {
                    throw new IllegalStateException("Could not access fields in PlayerAdvancements");
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public void load() {
            if (Files.isRegularFile(this.playerSavePath)) {
                try (Reader bufferedReader = Files.newBufferedReader(this.playerSavePath, StandardCharsets.UTF_8)) {
                    JsonElement jsonElement = StrictJsonParser.parse(bufferedReader);
                    Object data = this.codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(JsonParseException::new);

                    List<AdvancementNode> roots = new ArrayList<>();
                    Map<AdvancementHolder, AdvancementProgress> progresses = new HashMap<>();
                    Data$forEach(data, (path, progress) -> {
                        AdvancementHolder holder = this.manager.get(path);
                        if (holder != null) {
                            progress.update(holder.value().requirements());
                            progresses.put(holder, progress);
                            AdvancementNode node = this.tree.get(holder);
                            if (node != null) roots.add(node.root());
                        }
                    });

                    this.toAdd.clear();
                    this.toRemove.clear();

                    roots.forEach(root -> {
                        AdvancementVisibilityEvaluator.evaluateVisibility(root, node -> {
                            if (progresses.containsKey(node.holder().id())) {
                                return progresses.get(node.holder().id()).isDone();
                            }
                            return false;
                        }, (node, isVisible) -> {
                            if (isVisible) {
                                this.toAdd.add(node.holder());
                            }
                        });
                    });

                    this.toSetProgress.clear();

                    progresses.forEach((holder, progress) -> {
                        if (this.toAdd.contains(holder)) {
                            this.toSetProgress.put(holder.id(), progress);
                        }
                    });
                    progresses.clear();
                } catch(IOException | JsonParseException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        public Set<AdvancementHolder> toAdd() {
            return this.toAdd;
        }

        public Set<ResourceLocation> toRemove() {
            return this.toRemove;
        }

        public Map<ResourceLocation, AdvancementProgress> toSetProgress() {
            return this.toSetProgress;
        }

        private static void Data$forEach(Object target, BiConsumer<ResourceLocation, AdvancementProgress> action) {
            if (!target.getClass().getName().endsWith("PlayerAdvancements$Data")) throw new IllegalArgumentException("Target must be an instance of PlayerAdvancements.Data");
            try {
                Method forEachMethod = target.getClass().getDeclaredMethod("forEach", BiConsumer.class);
                if (forEachMethod.trySetAccessible()) {
                    forEachMethod.invoke(target, action);
                    System.out.println("ok");
                } else {
                    throw new IllegalStateException("Could not access forEach method in PlayerAdvancements.Data");
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
