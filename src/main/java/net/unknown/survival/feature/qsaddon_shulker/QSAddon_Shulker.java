/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.survival.feature.qsaddon_shulker;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.interaction.InteractionBehavior;
import com.ghostchu.quickshop.api.shop.interaction.InteractionManager;
import net.unknown.UnknownNetworkCorePlugin;
import org.bukkit.Bukkit;

public class QSAddon_Shulker {
    public static void inject() {
        if (!Bukkit.getPluginManager().isPluginEnabled("QuickShop-Hikari")) return;

        QuickShopAPI api = QuickShopAPI.getInstance();
        InteractionManager im = api.getInteractionManager();

        // 上書き
        InteractionBehavior origAll = im.behavior("TRADE_DIRECT_ALL").orElse(null);
        im.behavior(new CustomTradeDirectAll(origAll, UnknownNetworkCorePlugin.getInstance().getLogger()));
        System.out.println("QSAddon_Shulker: TRADE_DIRECT_ALL behavior injected.");

        InteractionBehavior origDirect = im.behavior("TRADE_DIRECT").orElse(null);
        im.behavior(new CustomTradeDirect(origDirect, UnknownNetworkCorePlugin.getInstance().getLogger()));
        System.out.println("QSAddon_Shulker: TRADE_DIRECT behavior injected.");

        InteractionBehavior origInteract = im.behavior("TRADE_INTERACTION").orElse(null);
        im.behavior(new CustomTradeInteraction(origInteract));
        System.out.println("QSAddon_Shulker: TRADE_INTERACTION behavior injected.");
    }
}
