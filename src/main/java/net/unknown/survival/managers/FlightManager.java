/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.survival.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.dependency.Vault;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightManager {
    public static final int FLIGHT_PRICE_BASE = 200; // @see https://discord.com/channels/608774418440912896/907934241260728361/1003668624583708733
    public static final int FLIGHT_PRICE_PER_MINUTES = 43; // @see https://discord.com/channels/608774418440912896/907934241260728361/1003668570959527956
    public static final int MAX_ON_GROUND_MINUTES = 3;
    public static final int MAX_ON_GROUND_TICKS = (20 * 60) * MAX_ON_GROUND_MINUTES;

    private static final Map<UUID, State> FLIGHT_STATES = new HashMap<>();

    public static boolean isFlightEnabled(Player player) {
        return isFlightEnabled(player.getUniqueId());
    }

    public static boolean isFlightEnabled(ServerPlayer player) {
        return isFlightEnabled(player.getUUID());
    }

    public static boolean isFlightEnabled(UUID uniqueId) {
        return FLIGHT_STATES.containsKey(uniqueId);
    }

    public static Result enableFlight(ServerPlayer player, int maxFlightTimeMinutes, int maxFlightPrice) {
        return enableFlight(player.getBukkitEntity(), maxFlightTimeMinutes, maxFlightPrice);
    }

    public static Result enableFlight(Player player, int maxFlightTimeMinutes, int maxFlightPrice) {
        if (isFlightEnabled(player)) {
            return Result.FAILED;
        }

        try {
            State s = new State(player, maxFlightTimeMinutes, maxFlightPrice);
            FLIGHT_STATES.put(player.getUniqueId(), s);
            return Result.SUCCESS;
        } catch (IllegalStateException e) {
            try {
                return Result.valueOf(e.getMessage());
            } catch (IllegalArgumentException ignored) {
                return Result.FAILED;
            }
        } catch (Throwable e) {
            return Result.FAILED;
        }
    }

    public static Result disableFlight(ServerPlayer player, EndReason reason) {
        return disableFlight(player.getUUID(), reason);
    }

    public static Result disableFlight(Player player, EndReason reason) {
        return disableFlight(player.getUniqueId(), reason);
    }

    public static Result disableFlight(UUID uniqueId, EndReason reason) {
        if (!isFlightEnabled(uniqueId)) {
            return Result.FAILED;
        }

        FLIGHT_STATES.get(uniqueId).endFlight(reason);
        FLIGHT_STATES.remove(uniqueId);
        return Result.SUCCESS;
    }

    public enum EndReason {
        MAX_TIME("指定した時間が経過したため"),
        MAX_PRICE("指定した料金を超過するため"),
        EMPTY_BALANCE("所持金が不足しているため"),
        GAME_MODE_CHANGED("ゲームモードが変更されたため"),
        QUIT("ログアウトしたため"),
        IN_GROUND(MAX_ON_GROUND_MINUTES + "分間地上にいたため"),
        SIX_ENCOUNT_TOMATO("トマトと6回出会ったため"),
        SELF_END("");

        private final String message;

        EndReason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }
    }

    public enum Result {
        SUCCESS,
        FAILED,
        EMPTY_BALANCE,
        REQUIRES_SURVIVAL_OR_ADVENTURE
    }

    public static class State implements Listener {
        private final Player player;
        private final BukkitTask displayUpdateTask;
        private final long maxFlightTimeMinutes; // 最大飛行時間 /fly end-time 60 // 60分になったら終了する
        private final long maxFlightPrice; // 最大料金 /fly end-price 1100 // 1100円になったら終了する
        private BukkitTask flightTimeUpdateTask;
        private BukkitTask onGroundTimeUpdateTask;
        private long flightTicks = 0;
        private long flightMinutes = 0;
        private long onGroundTicks = 0;
        private long currentPrice = FLIGHT_PRICE_BASE; // 基本料金 300円

        public State(Player player, int maxFlightTimeMinutes, int maxFlightPrice) throws IllegalStateException {
            this.player = player;
            this.maxFlightTimeMinutes = maxFlightTimeMinutes;
            this.maxFlightPrice = maxFlightPrice;

            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                throw new IllegalStateException(Result.REQUIRES_SURVIVAL_OR_ADVENTURE.name());
            }

            if (Vault.getBalance(this.player) < this.currentPrice + 100) {
                throw new IllegalStateException(Result.EMPTY_BALANCE.name());
            }

            this.displayUpdateTask = RunnableManager.runAsyncRepeating(() -> {
                this.player.sendActionBar(Component.empty()
                        .append(Component.text("有料飛行" + (this.flightTimeUpdateTask != null ? "中[" + this.flightMinutes + "分]" : "が有効"), Style.style(TextColor.color(16755200), TextDecoration.BOLD.withState(true))))
                        .append(Component.text(" - "))
                        .append(Component.text("¥" + currentPrice, TextColor.color(16777045))));
            }, 0L, 40L);
            ListenerManager.registerListener(this);
            this.player.setAllowFlight(true);
            MessageUtil.sendMessage(this.player, "有料飛行が有効になりました。飛行時間1分ごとに、" + FLIGHT_PRICE_PER_MINUTES + "円が加算されます。");
            runOnGroundTimeTask();
        }

        public void endFlight(EndReason reason) {
            HandlerList.unregisterAll(this); // flight state change listener unregister
            if (this.flightTimeUpdateTask != null && !this.flightTimeUpdateTask.isCancelled()) {
                this.flightTimeUpdateTask.cancel();
                this.flightTimeUpdateTask = null;
            }

            if (this.player.isFlying()) {
                /* 落下ダメージを無効化する */
                Listener dummyListener = new Listener() {
                };
                Bukkit.getPluginManager().registerEvent(EntityDamageEvent.class, dummyListener, EventPriority.MONITOR, (l, e) -> {
                    if (e instanceof EntityDamageEvent event) {
                        if (event.getEntity() == this.player) {
                            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                                event.setCancelled(true);
                                HandlerList.unregisterAll(l);
                            }
                        }
                    }
                }, UnknownNetworkCore.getInstance());
                /* 最大 10秒 */
                RunnableManager.runAsyncDelayed(() -> HandlerList.unregisterAll(dummyListener), 20 * 10);

                this.player.setFlying(false); // 飛行オフ
            }
            this.player.setAllowFlight(false);
            this.displayUpdateTask.cancel();
            if (Vault.withdraw(this.player, this.currentPrice).transactionSuccess()) {
                MessageUtil.sendMessage(this.player, "有料飛行が" + reason.getMessage() + "無効になりました。" + this.currentPrice + "円を支払いました。");
            } else {
                MessageUtil.sendErrorMessage(this.player, "有料飛行が" + reason.getMessage() + "無効になりましたが、料金を支払えませんでした。運営に報告してください。");
            }
        }

        private void runOnGroundTimeTask() {
            if (this.onGroundTimeUpdateTask == null || this.onGroundTimeUpdateTask.isCancelled()) {
                this.onGroundTimeUpdateTask = RunnableManager.runAsyncRepeating(() -> {
                    this.onGroundTicks++;
                    if (this.onGroundTicks >= MAX_ON_GROUND_TICKS) {
                        this.onGroundTimeUpdateTask.cancel();
                        this.onGroundTimeUpdateTask = null;
                        this.onGroundTicks = 0;
                        FlightManager.disableFlight(this.player, EndReason.IN_GROUND);
                    }
                }, 0, 1L);
            }
        }

        @EventHandler
        public void onFlightStateChanged(PlayerToggleFlightEvent event) {
            if (event.getPlayer().equals(this.player)) {
                if (event.isFlying() && this.flightTimeUpdateTask == null) { // Flight started
                    this.onGroundTimeUpdateTask.cancel();
                    this.onGroundTimeUpdateTask = null;
                    this.onGroundTicks = 0;
                    this.flightTimeUpdateTask = RunnableManager.runAsyncRepeating(() -> {
                        this.flightTicks++; // increment flight ticks

                        if (this.flightTicks >= 20 * 60) {
                            this.currentPrice += FLIGHT_PRICE_PER_MINUTES; // 加算
                            this.flightMinutes++; // increment flight minutes
                            this.flightTicks = 0; // ticks reset

                            if ((this.currentPrice + FLIGHT_PRICE_PER_MINUTES) > Vault.getBalance(this.player)) {
                                FlightManager.disableFlight(this.player, EndReason.EMPTY_BALANCE);
                            }

                            if (this.maxFlightTimeMinutes > 0 && this.flightMinutes >= this.maxFlightTimeMinutes) {
                                FlightManager.disableFlight(this.player, EndReason.MAX_TIME); // End flight
                            }

                            if (this.maxFlightPrice > 0 && (this.currentPrice + FLIGHT_PRICE_PER_MINUTES) >= this.maxFlightPrice) {
                                FlightManager.disableFlight(this.player, EndReason.MAX_PRICE); // End flight
                            }
                        }
                    }, 0L, 1L);
                } else if (!event.isFlying() && this.flightTimeUpdateTask != null) { // Flight ended
                    if (!this.flightTimeUpdateTask.isCancelled()) {
                        this.flightTimeUpdateTask.cancel();
                        this.flightTimeUpdateTask = null;
                        this.runOnGroundTimeTask();
                    }
                }
            }
        }

        @EventHandler
        public void onGameModeChanged(PlayerGameModeChangeEvent event) {
            if (event.getPlayer().equals(this.player)) {
                if (event.getNewGameMode() != GameMode.SURVIVAL && event.getNewGameMode() != GameMode.ADVENTURE) {
                    FlightManager.disableFlight(this.player, EndReason.GAME_MODE_CHANGED);
                }
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            if (event.getPlayer().equals(this.player)) {
                FlightManager.disableFlight(this.player, EndReason.QUIT);
            }
        }
    }
}
