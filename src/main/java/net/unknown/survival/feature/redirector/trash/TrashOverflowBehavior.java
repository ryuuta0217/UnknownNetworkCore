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

package net.unknown.survival.feature.redirector.trash;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;

public enum TrashOverflowBehavior {
    VOID(Component.text("ゴミ箱満杯時、").append(Component.text("対象アイテムを完全消去", DefinedTextColor.RED)),
            Component.text("ゴミ箱満杯時の動作を ").append(Component.text("対象アイテムを完全消去", DefinedTextColor.RED)).append(Component.text(" に設定しました"))),
    STOP_INSERT(Component.text("ゴミ箱満杯時、").append(Component.text("自動転送を停止し、通常インベントリに回収", DefinedTextColor.YELLOW)),
            Component.text("ゴミ箱満杯時の動作を ").append(Component.text("自動転送を停止し、通常インベントリに回収", DefinedTextColor.YELLOW)).append(Component.text(" に設定しました")));

    private final Component description;
    private final Component modeChangedMessage;

    TrashOverflowBehavior(Component description, Component modeChangedMessage) {
        this.description = description;
        this.modeChangedMessage = modeChangedMessage;
    }

    public Component getDescription() {
        return this.description;
    }

    public Component getModeChangedMessage() {
        return this.modeChangedMessage;
    }
}
