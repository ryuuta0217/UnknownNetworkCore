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

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.MessageUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PacketCommand {
    private static final Map<String, Map<String, Float>> DEFINED_GAME_EVENT_PACKET_PARAMS = new HashMap<>() {{
        put("CHANGE_GAME_MODE", new HashMap<>() {{
            put("SURVIVAL", 0f);
            put("CREATIVE", 1f);
            put("ADVENTURE", 2f);
            put("SPECTATOR", 3f);
        }});

        put("WIN_GAME", new HashMap<>() {{
            put("RESPAWN", 0f);
            put("ROLL_CREDIT_AND_RESPAWN", 1f);
        }});

        put("DEMO_EVENT", new HashMap<>() {{
            put("DEMO_PARAM_INTRO", 0f);
            put("DEMO_PARAM_HINT_1", 101f);
            put("DEMO_PARAM_HINT_2", 102f);
            put("DEMO_PARAM_HINT_3", 103f);
            put("DEMO_PARAM_HINT_4", 104f);
        }});

        put("IMMEDIATE_RESPAWN", new HashMap<>() {{
            put("SHOW_DEATH_SCREEN", 0f);
            put("SKIP_DEATH_SCREEN", 1f);
        }});
    }};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("packet");
        builder.requires(Permissions.COMMAND_PACKET::check);

        for (Packets packet : Packets.values()) {
            if (packet == Packets.GAME_EVENT) {
                for (GameEventType gameEventType : GameEventType.values()) {
                    if (!gameEventType.name().equals("RAIN_LEVEL_CHANGE") && !gameEventType.name().equals("THUNDER_LEVEL_CHANGE")) {
                        builder = builder.then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal(packet.getLiteral())
                                        .then(Commands.literal(gameEventType.name())
                                                .then(Commands.argument("customParamValue", FloatArgumentType.floatArg())
                                                        .executes(ctx -> sendGameEventPacket(ctx, gameEventType.ordinal(), FloatArgumentType.getFloat(ctx, "customParamValue")))))));
                    }

                    switch (gameEventType) {
                        case CHANGE_GAME_MODE:
                        case WIN_GAME:
                        case DEMO_EVENT:
                        case IMMEDIATE_RESPAWN:
                            for (Map.Entry<String, Float> definedParamsEntry : DEFINED_GAME_EVENT_PACKET_PARAMS.get(gameEventType.name()).entrySet()) {
                                String friendlyParamName = definedParamsEntry.getKey();
                                float internalParamValue = definedParamsEntry.getValue();

                                builder = builder.then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.literal(packet.getLiteral())
                                                .then(Commands.literal(gameEventType.name())
                                                        .then(Commands.literal(friendlyParamName)
                                                                .executes(ctx -> sendGameEventPacket(ctx, gameEventType.ordinal(), internalParamValue))))));
                            }
                            break;
                        case RAIN_LEVEL_CHANGE:
                        case THUNDER_LEVEL_CHANGE:
                            builder = builder.then(Commands.argument("targets", EntityArgument.players())
                                    .then(Commands.literal(packet.getLiteral())
                                            .then(Commands.literal(gameEventType.name())
                                                    .then(Commands.argument("level", FloatArgumentType.floatArg(0.0f, 1.0f))
                                                            .executes(ctx -> sendGameEventPacket(ctx, gameEventType.ordinal(), FloatArgumentType.getFloat(ctx, "level")))))));
                            break;

                    }
                }

                builder = builder.then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.literal(packet.getLiteral())
                                .then(Commands.argument("customTypeNumber", IntegerArgumentType.integer())
                                        .then(Commands.argument("customParamValue", FloatArgumentType.floatArg())
                                                .executes(ctx -> sendGameEventPacket(ctx, IntegerArgumentType.getInteger(ctx, "customTypeNumber"), FloatArgumentType.getFloat(ctx, "customParamValue")))))));
            }
        }

        dispatcher.register(builder);
    }

    private static int sendGameEventPacket(CommandContext<CommandSourceStack> ctx, int type, float param) throws CommandSyntaxException {
        ClientboundGameEventPacket packet = new ClientboundGameEventPacket(new ClientboundGameEventPacket.Type(type), param);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        targets.forEach(player -> player.connection.send(packet));
        MessageUtil.sendAdminMessage(ctx.getSource(), (targets.size() > 1 ? targets.size() + " 人のプレイヤー" : new ArrayList<>(targets).get(0).getScoreboardName()) + "にパケットを送信しました");
        return packet.hashCode();
    }

    public enum Packets {
        GAME_EVENT("GameEvent"),
        CUSTOM_PAYLOAD("CustomPayload");

        private final String literal;

        Packets(String literal) {
            this.literal = literal;
        }

        public String getLiteral() {
            return literal;
        }
    }

    public enum GameEventType {
        NO_RESPAWN_BLOCK_AVAILABLE,
        START_RAINING,
        STOP_RAINING,
        CHANGE_GAME_MODE,
        WIN_GAME,
        DEMO_EVENT,
        ARROW_HIT_PLAYER,
        RAIN_LEVEL_CHANGE,
        THUNDER_LEVEL_CHANGE,
        PUFFER_FISH_STING,
        GUARDIAN_ELDER_EFFECT,
        IMMEDIATE_RESPAWN
    }
}
