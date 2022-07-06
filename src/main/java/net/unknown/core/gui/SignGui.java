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

import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.events.interfaces.PacketListener;
import net.unknown.core.managers.PacketManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.events.PacketReceivedEvent;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private net.minecraft.network.chat.Component[] defaultLines = new net.minecraft.network.chat.Component[] {MutableComponent.create(LiteralContents.EMPTY), MutableComponent.create(LiteralContents.EMPTY), MutableComponent.create(LiteralContents.EMPTY), MutableComponent.create(LiteralContents.EMPTY)};
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

        ServerPlayer nmsTarget = ((CraftPlayer) this.target).getHandle();

        Location bukkitLoc = this.target.getLocation();
        bukkitLoc.setY(1);
        Vec3 nmsLoc = new Vec3(bukkitLoc.getX(), bukkitLoc.getY(), bukkitLoc.getZ());
        BlockPos blockPos = new BlockPos(nmsLoc);
        BlockState signBlock = CraftMagicNumbers.getBlock(this.signType).defaultBlockState();

        ClientboundBlockUpdatePacket blockUpdatePacket = new ClientboundBlockUpdatePacket(blockPos, signBlock);
        nmsTarget.connection.send(blockUpdatePacket); // set sign block

        SignBlockEntity sign = new SignBlockEntity(blockPos, signBlock);
        for (int i = 0; i < this.defaultLines.length; i++) {
            sign.setMessage(i, this.defaultLines[i]);
        }
        nmsTarget.connection.send(sign.getUpdatePacket()); // set lines

        ClientboundOpenSignEditorPacket signEditorPacket = new ClientboundOpenSignEditorPacket(blockPos);

        nmsTarget.connection.send(signEditorPacket); // show sign editor

        this.isOpened = true;
        SignGui.SIGN_GUI_OPENED.put(this.target.getUniqueId(), this);
    }

    private void convertAdventureToNMS() {
        this.defaultLines = Arrays.stream(this.defaultLines$adventure)
                .map(MessageUtil::convertAdventure2NMS)
                .toArray(net.minecraft.network.chat.Component[]::new);
    }

    public static class Listener extends PacketListener implements org.bukkit.event.Listener {
        public Listener() {
            PacketManager.getInstance().registerListener(ServerboundSignUpdatePacket.class, this);
        }

        @Override
        public void onPacketReceived(PacketReceivedEvent event) {
            if(event.getPacket() instanceof ServerboundSignUpdatePacket packet) {
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
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            if (SIGN_GUI_OPENED.containsKey(event.getPlayer().getUniqueId())) {
                SignGui gui = SIGN_GUI_OPENED.get(event.getPlayer().getUniqueId());
                gui.isOpened = false;
                SIGN_GUI_OPENED.remove(event.getPlayer().getUniqueId());
            }
        }
    }
}
