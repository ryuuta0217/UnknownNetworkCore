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

package net.unknown.core.gui.view;

import net.unknown.core.gui.GuiBase;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class PaymentView implements View {
    private final boolean compact;
    private final GuiBase gui;
    private final double price;
    private final Consumer<InventoryClickEvent> onPurchased;

    public PaymentView(boolean compact, GuiBase gui, double price, Consumer<InventoryClickEvent> onPurchased) {
        this.compact = compact;
        this.gui = gui;
        this.price = price;
        this.onPurchased = onPurchased;
    }

    @Override
    public void initialize() {
        if (compact) {
            // 9*3 = 27 slots (single chest)
            // Slot 12: Yes
            // Slot 14: No
        } else {
            // 9*6 = 54 slots (double chest)
            // Slot 21: Yes
            // Slot 23: No
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {

    }

    @Override
    public void clearInventory() {

    }
}