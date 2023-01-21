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

package net.unknown.core.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.unknown.UnknownNetworkCore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BrigadierUtil {
    @SuppressWarnings("unchecked")
    public static <S> boolean isArgumentKeyExists(CommandContext<S> context, String argumentName) {
        try {
            // get Field CommandContext.class 's arguments (Map<String, ParsedArgument<S, ?>>)
            Field argumentsF = context.getClass().getDeclaredField("arguments");

            // [FORCE] allow access to CommandContext<S>.arguments
            argumentsF.setAccessible(true);

            // get Object. YES.
            Map<String, ParsedArgument<S, ?>> arguments = (Map<String, ParsedArgument<S, ?>>) argumentsF.get(context);

            return arguments != null && arguments.containsKey(argumentName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static <S, T> T getArgumentOrDefault(@Nonnull CommandContext<S> context, @Nonnull Class<T> argType, @Nonnull String argName, @Nullable T def) {
        try {
            return context.getArgument(argName, argType);
        } catch (IllegalArgumentException ignored) {
            return def;
        }
    }

    public static void forceUnregisterCommand(String commandName) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.trySetAccessible();

            Map<String, CommandNode<CommandSourceStack>> children = (Map<String, CommandNode<CommandSourceStack>>) childrenField.get(UnknownNetworkCore.getBrigadier().getRoot());
            Set<String> toRemoveCommands = new HashSet<>();
            children.keySet().stream()
                    .filter(command -> command.contains(":"))
                    .filter(command -> command.split(":")[1].equalsIgnoreCase(commandName))
                    .forEachOrdered(toRemoveCommands::add);
            toRemoveCommands.forEach(children::remove);

            children.remove(commandName);
            UnknownNetworkCore.getBrigadier().getRoot().removeCommand(commandName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

