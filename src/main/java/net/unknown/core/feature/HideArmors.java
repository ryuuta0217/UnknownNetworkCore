package net.unknown.core.feature;

import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.packet.PacketManager;
import net.unknown.core.packet.event.PacketSendingEvent;
import net.unknown.core.packet.listener.OutgoingPacketListener;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class HideArmors implements Listener {
    private static final HideArmors INSTANCE = new HideArmors();

    private final Map<UUID, Mode> players = new HashMap<>();
    private final Map<UUID, OutgoingPacketListener<ClientboundSetEquipmentPacket>> listeners = new HashMap<>();

    public static HideArmors getInstance() {
        return INSTANCE;
    }

    public Mode getMode(Player player) {
        return getMode(player.getUniqueId());
    }

    public Mode getMode(UUID player) {
        return this.players.getOrDefault(player, Mode.SHOW);
    }

    public void enable(Player player, Mode mode) {
        enable(player.getUniqueId(), mode);
    }

    public void enable(UUID player, Mode mode) {
        if (mode == Mode.SHOW) {
            disable(player);
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
        if (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore()) throw new IllegalStateException("UUID " + player + " is not player or has never joined this server.");
        if (this.players.containsKey(player) && this.players.get(player) == mode) return;
        this.players.put(player, mode);
        if (offlinePlayer.isOnline()) this.registerListener(offlinePlayer.getPlayer(), mode);
    }

    public void disable(Player player) {
        disable(player.getUniqueId());
    }

    public void disable(UUID player) {
        if (!this.players.containsKey(player)) return;
        this.players.remove(player);
        this.unregisterListener(player);
    }

    private void registerListener(Player player, Mode mode) {
        OutgoingPacketListener<ClientboundSetEquipmentPacket> listener = new OutgoingPacketListener<>() {
            @Override
            public void onSendingPacket(PacketSendingEvent<ClientboundSetEquipmentPacket> event) {
                if (event.getPacket().getEntity() == player.getEntityId()) {
                    if (mode == Mode.COMPLETELY_HIDE) {
                        event.setPacket(new ClientboundSetEquipmentPacket(player.getEntityId(), Collections.emptyList(), true));
                        return;
                    }

                    if (mode == Mode.HIDE_OTHERS && !event.getReceiver().getUUID().equals(player.getUniqueId())) {
                        event.setPacket(new ClientboundSetEquipmentPacket(player.getEntityId(), Collections.emptyList(), true));
                    }
                }
            }
        };

        this.unregisterListener(player);

        PacketManager.getInstance().registerOutgoingS2CListener(ClientboundSetEquipmentPacket.class, listener);
        this.listeners.put(player.getUniqueId(), listener);
    }

    private void unregisterListener(Player player) {
        this.unregisterListener(player.getUniqueId());
    }

    private void unregisterListener(UUID player) {
        if (this.listeners.containsKey(player)) {
            PacketManager.getInstance().unregisterOutgoingS2CListener(ClientboundSetEquipmentPacket.class, this.listeners.get(player));
            this.listeners.remove(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.players.containsKey(event.getPlayer().getUniqueId())) {
            Mode hideArmorMode = this.players.get(event.getPlayer().getUniqueId());
            this.registerListener(event.getPlayer(), hideArmorMode);

            RunnableManager.runAsyncDelayed(() -> {
                NewMessageUtil.sendMessage(event.getPlayer(), (hideArmorMode == Mode.HIDE_OTHERS ? "あなたの装備状態は他人から見えない状態に設定されています。" : "あなたの装備は見えない状態に設定されています。") + "/hidearmors を使用して設定を変更できます。");
            }, 10L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.unregisterListener(event.getPlayer());
    }

    public enum Mode {
        SHOW,
        HIDE_OTHERS,
        COMPLETELY_HIDE
    }
}
