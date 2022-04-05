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

package net.unknown.core.gui;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SignGui {
    private static final Map<UUID, SignGui> SIGN_GUI_OPENED = new HashMap<>();
    private final Logger logger = Logger.getLogger("SignGui@" + this.hashCode());
    private Player target;
    private Material signType = Material.OAK_SIGN;
    private Component[] defaultLines$adventure = new Component[] {Component.empty(), Component.empty(), Component.empty(), Component.empty()};
    private net.minecraft.network.chat.Component[] defaultLines = new net.minecraft.network.chat.Component[] {TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY};
    private Consumer<List<Component>> completeHandler;
    private boolean isOpened = false;

    public SignGui withTarget(Player player) {
        this.target = player;
        return this;
    }

    public SignGui withSignType(Material signType) {
        if (signType.name().endsWith("_SIGN")) this.signType = signType;
        return this;
    }

    public SignGui withLines(Component lineOne, Component lineTwo, Component lineThree, Component lineFour) {
        this.defaultLines$adventure = new Component[] {
                lineOne != null ? lineOne : Component.empty(),
                lineTwo != null ? lineTwo : Component.empty(),
                lineThree != null ? lineThree : Component.empty(),
                lineFour != null ? lineFour : Component.empty()
        };
        convertAdventureToNMS();
        return this;
    }

    public SignGui onComplete(Consumer<List<Component>> onComplete) {
        this.completeHandler = onComplete;
        return this;
    }

    public void open() {
        if (this.target == null)
            throw new IllegalStateException("Target is null. Please use SignGui#withTarget to specific player.");
        if (this.completeHandler == null) logger.warning("Complete handler is null, it is stupid.");
        if (SignGui.SIGN_GUI_OPENED.containsKey(this.target.getUniqueId()))
            throw new IllegalStateException("Double Sign Gui?");

        Location bukkitLoc = this.target.getLocation();
        bukkitLoc.setY(1);
        Vec3 nmsLoc = new Vec3(bukkitLoc.getX(), bukkitLoc.getY(), bukkitLoc.getZ());
        BlockPos blockPos = new BlockPos(nmsLoc);
        BlockState signBlock = CraftMagicNumbers.getBlock(this.signType).defaultBlockState();

        SignBlockEntity sign = new SignBlockEntity(blockPos, signBlock);
        System.arraycopy(this.defaultLines, 0, sign.messages, 0, 4);

        ClientboundBlockUpdatePacket blockUpdatePacket = new ClientboundBlockUpdatePacket(blockPos, signBlock);
        ClientboundOpenSignEditorPacket signEditorPacket = new ClientboundOpenSignEditorPacket(blockPos);

        ServerPlayer nmsTarget = ((CraftPlayer) this.target).getHandle();
        nmsTarget.connection.send(blockUpdatePacket); // set sign block
        nmsTarget.connection.send(sign.getUpdatePacket()); // set lines
        nmsTarget.connection.send(signEditorPacket); // show sign editor

        this.isOpened = true;
        SignGui.SIGN_GUI_OPENED.put(this.target.getUniqueId(), this);
    }

    private void convertAdventureToNMS() {
        this.defaultLines = Arrays.stream(this.defaultLines$adventure)
                .map(MessageUtil::convertAdventure2NMS)
                .toArray(net.minecraft.network.chat.Component[]::new);
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            ChannelDuplexHandler packetHandler = new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof ServerboundSignUpdatePacket packet) {
                        if (SignGui.SIGN_GUI_OPENED.containsKey(event.getPlayer().getUniqueId())) {
                            SignGui gui = SignGui.SIGN_GUI_OPENED.get(event.getPlayer().getUniqueId());
                            if (gui.isOpened) {
                                gui.isOpened = false;
                                SignGui.SIGN_GUI_OPENED.remove(event.getPlayer().getUniqueId());
                                if (gui.completeHandler != null) {
                                    RunnableManager.runDelayed(() -> {
                                        gui.completeHandler.accept(Arrays.stream(packet.getLines()).map(line -> (Component) Component.text(line)).toList());
                                    }, 1L);
                                }
                            }
                        }
                    }
                    super.channelRead(ctx, msg);
                }
            };
            ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
            pipeline.addBefore("packet_handler", player.getName(), packetHandler);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Channel c = ((CraftPlayer) event.getPlayer()).getHandle().connection.connection.channel;
            c.eventLoop().submit(() -> c.pipeline().remove(event.getPlayer().getName()));
            if (SIGN_GUI_OPENED.containsKey(event.getPlayer().getUniqueId())) {
                SignGui gui = SIGN_GUI_OPENED.get(event.getPlayer().getUniqueId());
                gui.isOpened = false;
                SIGN_GUI_OPENED.remove(event.getPlayer().getUniqueId());
            }
        }
    }
}
