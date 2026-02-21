package net.unknown.core.managers;

import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BossBarManager implements Listener {
    private static final BossBarManager INSTANCE = new BossBarManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(BossBarManager.class);

    private final Set<CustomBossEvent> registeredBossBars = new HashSet<>();
    private VisibilityHandler visibilityHandler = new DefaultVisibilityHandler();

    private BossBarManager() {}

    public static BossBarManager getInstance() {
        return INSTANCE;
    }

    public Set<CustomBossEvent> getRegisteredBossBars() {
        return Collections.unmodifiableSet(this.registeredBossBars);
    }

    public void register(CustomBossEvent bossBar) {
        this.registeredBossBars.stream().filter(existingBossBar -> existingBossBar.getTextId().equals(bossBar.getTextId())).findAny().ifPresent(existingBossBar -> {
            throw new IllegalArgumentException("Identifier duplication detected for identifier " + bossBar.getTextId() + ". Cannot register same identifier boss bars. Existing boss bar: " + existingBossBar);
        });

        this.registeredBossBars.add(bossBar);
    }

    public boolean unregister(CustomBossEvent bossBar) {
        return this.registeredBossBars.remove(bossBar);
    }

    public void setVisibilityHandler(VisibilityHandler visibilityHandler) {
        LOGGER.info("Visibility handler set to {} from {}.", visibilityHandler.getClass().getName(), this.visibilityHandler.getClass().getName());
        this.visibilityHandler = visibilityHandler;
    }

    public VisibilityHandler getVisibilityHandler() {
        return this.visibilityHandler;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.registeredBossBars.forEach(bossBar -> {
            if (this.visibilityHandler.isVisible(event.getPlayer(), bossBar.getTextId())) {
                bossBar.addPlayer(MinecraftAdapter.player(event.getPlayer()));
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.registeredBossBars.forEach(bossBar -> bossBar.removePlayer(MinecraftAdapter.player(event.getPlayer())));
    }

    public interface VisibilityHandler {
        boolean isVisible(Player player, Identifier identifier);
        void setVisible(Player player, Identifier identifier, boolean visible);
        default void toggleVisibility(Player player, Identifier identifier) {
            boolean currentVisibility = this.isVisible(player, identifier);
            this.setVisible(player, identifier, !currentVisibility);
        }
    }

    private static class DefaultVisibilityHandler implements VisibilityHandler {
        private static final NamespacedKey PERSISTENT_CONTAINER_KEY = new NamespacedKey("unknown-network", "bossbar_visibility");

        @Override // default is true, can see if not set or set to true, can't see if set to false
        public boolean isVisible(Player player, Identifier identifier) {
            return !player.getPersistentDataContainer().has(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER) // プレイヤーがコンテナの初期化を完了していない場合は true を返して可視とする
                    || !Boolean.FALSE.equals(player.getPersistentDataContainer().get(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER).get(CraftNamespacedKey.fromMinecraft(identifier), PersistentDataType.BOOLEAN)); // プレイヤーがコンテナの初期化を完了している場合にここに到達する。コンテナ内の Identifier の値が false に設定されているかどうかを確認し、設定されていないか true に設定されている場合は可視とする
        }

        @Override
        public void setVisible(Player player, Identifier identifier, boolean visible) {
            PersistentDataContainer rootContainer = player.getPersistentDataContainer();

            if (!rootContainer.has(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER)) {
                rootContainer.set(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER, rootContainer.getAdapterContext().newPersistentDataContainer());
            }

            PersistentDataContainer dataContainer = rootContainer.get(PERSISTENT_CONTAINER_KEY, PersistentDataType.TAG_CONTAINER);
            if (dataContainer != null) {
                dataContainer.set(CraftNamespacedKey.fromMinecraft(identifier), PersistentDataType.BOOLEAN, visible);
            }
        }
    }
}
