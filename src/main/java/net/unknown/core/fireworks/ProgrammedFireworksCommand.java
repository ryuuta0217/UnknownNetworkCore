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

package net.unknown.core.fireworks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.commands.Suggestions;
import net.unknown.core.enums.Permissions;
import net.unknown.core.fireworks.model.Program;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

// /<programmedfireworks|pf> <add|remove|run|stop|list>
// /pf add <program|firework|explosion>
// /pf add program <id>
// /pf add firework <programId: StringArgumentType> <tick: IntegerArgumentType> <world: StringArgumentType> <<x> <y> <z>: Vec3Argument> <locationYOffset: IntegerArgumentType> <randomizeLaunchLocation: BooleanArgumentType> <lifetime: IntegerArgumentType> <shotAtAngle: BooleanArgumentType>
// /pf add explosion <programId> <tick> <type> <flicker: bool> <trail: bool> <colors: string> <fadeColors: string>
public class ProgrammedFireworksCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("programmedfireworks");
        builder.requires(Permissions.COMMAND_PROGRAMMEDFIREWORKS::check);

        LiteralArgumentBuilder<CommandSourceStack> addNode = Commands.literal("add");

        addNode.then(Commands.literal("program")
                .then(Commands.argument("program-id", StringArgumentType.word())
                        .executes(ProgrammedFireworksCommand::addProgram)));

        addNode.then(Commands.literal("firework")
                .then(Commands.argument("program-id", StringArgumentType.word())
                        .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ProgrammedFireworks.getPrograms().keySet(), suggestionsBuilder))
                        .then(Commands.argument("tick", IntegerArgumentType.integer(0))
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests(Suggestions.WORLD_SUGGEST)
                                        .then(Commands.argument("launch-location", Vec3Argument.vec3())
                                                .then(Commands.argument("y-offset", IntegerArgumentType.integer())
                                                        .then(Commands.argument("xz-randomize", BoolArgumentType.bool())
                                                                .then(Commands.argument("lifetime", IntegerArgumentType.integer(0))
                                                                        .then(Commands.argument("shot-at-angle", BoolArgumentType.bool())
                                                                                .executes(ProgrammedFireworksCommand::addFirework))))))))));

        for (FireworkRocketItem.Shape shape : FireworkRocketItem.Shape.values()) {
            addNode.then(Commands.literal("explosion")
                    .then(Commands.argument("program-id", StringArgumentType.word())
                            .then(Commands.argument("tick", IntegerArgumentType.integer(0))
                                    .then(Commands.literal(shape.getName())
                                            .then(Commands.argument("flicker", BoolArgumentType.bool())
                                                    .then(Commands.argument("trail", BoolArgumentType.bool())
                                                            .then(Commands.argument("colors", StringArgumentType.word())
                                                                    /*.suggests((ctx, suggestionsBuilder) -> {
                                                                        // Suggest command input. Valid command input is: red,blue,green
                                                                        // If player is not typed any chars, suggest all colors.
                                                                        // If player is typed any chars with matched color, suggest comma.
                                                                        // If player is typed any chars with not matched color, suggest matched colors.
                                                                        Program program = ProgrammedFireworks.getProgram(BrigadierUtil.getArgumentOrDefault(ctx, String.class, "program-id", ""));
                                                                        if (program != null) {
                                                                            String input = suggestionsBuilder.getInput().substring(suggestionsBuilder.getStart());
                                                                            String[] sliced = input.split(", ?");

                                                                            Map<String, Color> definedColors = program.getColors();

                                                                            if (sliced.length == 1) {
                                                                                if (definedColors.containsKey(sliced[0])) {
                                                                                    return SharedSuggestionProvider.suggest(input + ",");
                                                                                } else if (definedColors.keySet().stream().anyMatch(s -> s.contains(sliced[0]))) {
                                                                                    return SharedSuggestionProvider.suggest(program.getColors().keySet(), suggestionsBuilder);
                                                                                }
                                                                            } else if (sliced.length > 1) {

                                                                            }
                                                                        }
                                                                        return suggestionsBuilder.buildFuture();
                                                                    })*/
                                                                    .then(Commands.argument("fade-colors", StringArgumentType.word())
                                                                            /*.suggests((ctx, suggestionsBuilder) -> {
                                                                                return suggestionsBuilder.buildFuture();
                                                                            })*/
                                                                            .executes(ProgrammedFireworksCommand::addExplosion)))))))));
        }
        builder.then(addNode);

        LiteralArgumentBuilder<CommandSourceStack> modifyNode = Commands.literal("modify");
        modifyNode.then(Commands.literal("location")
                .then(Commands.literal("global")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ProgrammedFireworks.getColors().keySet(), suggestionsBuilder))
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests(Suggestions.WORLD_SUGGEST)
                                        .then(Commands.argument("location", Vec3Argument.vec3())
                                                .executes(ProgrammedFireworksCommand::modifyGlobalDefinedLocation)))))
                .then(Commands.argument("program-id", StringArgumentType.word())
                        .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ProgrammedFireworks.getPrograms().keySet(), suggestionsBuilder))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, suggestionsBuilder) -> {
                                    Program program = ProgrammedFireworks.getProgram(StringArgumentType.getString(ctx, "program-id"));
                                    if (program != null) {
                                        return SharedSuggestionProvider.suggest(program.getLocations().keySet(), suggestionsBuilder);
                                    }
                                    return suggestionsBuilder.buildFuture();
                                })
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests(Suggestions.WORLD_SUGGEST)
                                        .then(Commands.argument("location", Vec3Argument.vec3())
                                                .executes(ProgrammedFireworksCommand::modifyProgramLocation))))));
        builder.then(modifyNode);

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("pf");
        aliasBuilder.requires(Permissions.COMMAND_PROGRAMMEDFIREWORKS::check);
        aliasBuilder.redirect(dispatcher.register(builder));
        dispatcher.register(aliasBuilder);
    }

    private static int addProgram(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int addFirework(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int addExplosion(CommandContext<CommandSourceStack> ctx) {
        return 0;
    }

    private static int modifyGlobalDefinedLocation(CommandContext<CommandSourceStack> ctx) {
        String name = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "name", "");
        if (!ProgrammedFireworks.hasLocation(name)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "指定された名前で定義された打ち上げ場所は見つかりませんでした");
            return 3;
        }

        World world = Bukkit.getWorld(StringArgumentType.getString(ctx, "world"));
        if (world == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ワールドが見つかりませんでした");
            return 2;
        }

        Vec3 pos = Vec3Argument.getVec3(ctx, "location");

        ProgrammedFireworks.removeLocation(name);
        ProgrammedFireworks.addLocation(name, new Location(world, pos.x(), pos.y(), pos.z()));
        NewMessageUtil.sendMessage(ctx.getSource(), "打ち上げ場所の事前定義 " + name + " の場所を変更しました");
        return 0;
    }

    private static int modifyProgramLocation(CommandContext<CommandSourceStack> ctx) {
        String programId = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "program-id", "");
        if (!ProgrammedFireworks.hasProgram(programId)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "指定されたIDのプログラムは見つかりませんでした");
            return 3;
        }
        Program program = ProgrammedFireworks.getProgram(programId);

        String name = BrigadierUtil.getArgumentOrDefault(ctx, String.class, "name", "");
        if (!program.hasLocation(name)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "指定された名前で定義された打ち上げ場所は見つかりませんでした");
            return 3;
        }

        World world = Bukkit.getWorld(StringArgumentType.getString(ctx, "world"));
        if (world == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "ワールドが見つかりませんでした");
            return 2;
        }

        Vec3 pos = Vec3Argument.getVec3(ctx, "location");

        program.removeLocation(name);
        program.addLocation(name, new Location(world, pos.x(), pos.y(), pos.z()));
        NewMessageUtil.sendMessage(ctx.getSource(), "プログラム " + programId + " の打ち上げ場所の事前定義 " + name + " の場所を変更しました");
        return 0;
    }
}
