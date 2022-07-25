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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mozilla.javascript.*;

import java.lang.reflect.Method;
import java.util.*;

public class EvalCommand {
    public static final Map<String, Object> GLOBAL_STORAGE = new HashMap<>();
    private static final ContextFactory RHINO_CONTEXT_FACTORY = new ContextFactory();
    private static final Context RHINO_CONTEXT = RHINO_CONTEXT_FACTORY.enterContext();
    private static final ScriptableObject GLOBAL_SCOPE = RHINO_CONTEXT.initStandardObjects();

    static {
        ScriptableObject.putProperty(GLOBAL_SCOPE, "Bukkit", Bukkit.getServer());
        ScriptableObject.putProperty(GLOBAL_SCOPE, "plugin", UnknownNetworkCore.getInstance());
        ScriptableObject.putProperty(GLOBAL_SCOPE, "Storage", GLOBAL_STORAGE);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("eval");
        builder.requires(Permissions.COMMAND_EVAL::check);

        builder.then(Commands.argument("スクリプト", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String scriptIn = StringArgumentType.getString(ctx, "スクリプト");

                    Scriptable scope = RHINO_CONTEXT.initStandardObjects(GLOBAL_SCOPE);

                    // <Variables>
                    // nmsPlayer, nmsEntity
                    // player, entity
                    // location
                    // Util

                    if (ctx.getSource().getEntity() != null) {
                        if (ctx.getSource().getEntity() instanceof ServerPlayer player)
                            ScriptableObject.putProperty(scope, "nmsPlayer", player);
                        ScriptableObject.putProperty(scope, "nmsEntity", ctx.getSource().getEntity());

                        if (ctx.getSource().getBukkitEntity() instanceof Player player)
                            ScriptableObject.putProperty(scope, "player", player);
                        ScriptableObject.putProperty(scope, "entity", ctx.getSource().getBukkitEntity());
                    }

                    ScriptableObject.putProperty(scope, "location", ctx.getSource().getBukkitLocation());

                    ScriptableObject.putProperty(scope, "Util", new Util());

                    try {
                        Script script = RHINO_CONTEXT.compileString(scriptIn, "eval", 1, null);
                        Object result = script.exec(RHINO_CONTEXT, scope);
                        if (result instanceof Undefined || result == null)
                            MessageUtil.sendAdminMessage(ctx.getSource(), "コードの実行が正常に完了しました", true);
                        else
                            MessageUtil.sendAdminMessage(ctx.getSource(), "コードの実行が正常に完了しました: " + (result instanceof NativeJavaObject ? ((NativeJavaObject) result).unwrap() : (result == null ? "null" : result.toString())), true);
                        return result instanceof NativeJavaObject java ? (java.unwrap() instanceof Integer i ? i : java.unwrap().hashCode()) : (result == null ? 0 : result.hashCode());
                    } catch (Exception e) {
                        List<StackTraceElement> sts = new ArrayList<>(List.of(e.getStackTrace()));
                        Collections.reverse(sts); // trace reverse
                        List<Component> componentTraces = new ArrayList<>();
                        for (StackTraceElement element : sts) {
                            componentTraces.add(MutableComponent.create(new LiteralContents(element.toString())));
                        }
                        Collections.reverse(componentTraces);

                        MutableComponent c = MutableComponent.create(new LiteralContents(""));
                        componentTraces.forEach(trace -> {
                            c.append(trace).append("\n");
                        });

                        //StringWriter s = new StringWriter();
                        //e.printStackTrace(new PrintWriter(s));

                        Style modifier = Style.EMPTY.withColor(ChatFormatting.RED)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c));
                        ctx.getSource().sendFailure(MutableComponent.create(new LiteralContents("コードの評価中にエラーが発生しました: " + e.getLocalizedMessage())).withStyle(modifier));
                        return e.hashCode();
                    }
                }));
        dispatcher.register(builder);
    }

    public static class Util {
        public Method[] getMethods(Object targetObj) {
            return Arrays.stream(targetObj.getClass().getMethods())
                    .filter(m -> m.canAccess(targetObj))
                    .toArray(Method[]::new);
        }
    }
}
