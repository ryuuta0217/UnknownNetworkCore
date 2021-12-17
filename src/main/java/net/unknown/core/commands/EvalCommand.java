/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mozilla.javascript.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

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
        builder.then(Commands.argument("スクリプト", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String scriptIn = StringArgumentType.getString(ctx, "スクリプト");

                    Scriptable scope = RHINO_CONTEXT.initStandardObjects(GLOBAL_SCOPE);

                    if(ctx.getSource().getEntityOrException() instanceof ServerPlayer) ScriptableObject.putProperty(scope, "nmsPlayer", ctx.getSource().getPlayerOrException());
                    ScriptableObject.putProperty(scope, "nmsEntity", ctx.getSource().getEntity());

                    if(ctx.getSource().getBukkitEntity() instanceof Player) ScriptableObject.putProperty(scope, "player", ctx.getSource().getPlayerOrException().getBukkitEntity());
                    ScriptableObject.putProperty(scope, "entity", ctx.getSource().getBukkitEntity());

                    ScriptableObject.putProperty(scope, "location", ctx.getSource().getBukkitLocation());

                    try {
                        Script script = RHINO_CONTEXT.compileString(scriptIn, "eval_" + ctx.getSource().getEntityOrException().getUUID(), 1, null);
                        Object result = script.exec(RHINO_CONTEXT, scope);
                        if(result instanceof Undefined) MessageUtil.sendAdminMessage(ctx.getSource(), "コードの実行が正常に完了しました", true);
                        else MessageUtil.sendAdminMessage(ctx.getSource(), "コードの実行が正常に完了しました: " + (result instanceof NativeJavaObject ? ((NativeJavaObject) result).unwrap() : result.toString()), true);
                        return result instanceof NativeJavaObject ? ((NativeJavaObject) result).unwrap().hashCode() : result.hashCode();
                    } catch(Exception e) {
                        StringWriter s = new StringWriter();
                        e.printStackTrace(new PrintWriter(s));

                        Style modifier = Style.EMPTY.withColor(ChatFormatting.RED)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(s.toString())));
                        ctx.getSource().sendFailure(new TextComponent("コードの評価中にエラーが発生しました: " + e.getLocalizedMessage()).withStyle(modifier));
                        return e.hashCode();
                    }
                }));
        dispatcher.register(builder);
    }
}
