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

package net.unknown.survival.feature.redirector;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;

public enum ShulkerBoxRedirectorMode {
    ANYTHING(Component.text("拾ったアイテムを").append(Component.text("任意のシュルカーボックス", DefinedTextColor.GREEN)).append(Component.text("に収納します")), Component.text("シュルカーボックス自動回収のモードを ").append(Component.text("任意のシュルカーボックスに収納", DefinedTextColor.GREEN)).append(Component.text(" に設定しました"))),
    ANYTHING_WHEN_FULL(Component.text("手持ちインベントリに空きがない場合のみ、").append(Component.text("任意のシュルカーボックス", DefinedTextColor.YELLOW)).append(Component.text("に収納します")), Component.text("シュルカーボックス自動回収のモードを ").append(Component.text("満杯時に任意のシュルカーボックスに収納", DefinedTextColor.YELLOW)).append(Component.text(" に設定しました"))),
    INSERT_MATCHING(Component.text("拾ったアイテムを").append(Component.text("同じ種類のアイテムが既に入っているシュルカーボックス", DefinedTextColor.GREEN)).append(Component.text("のみに収納します")), Component.text("シュルカーボックス自動回収のモードを ").append(Component.text("同一アイテムがあるボックスにのみ収納", DefinedTextColor.GREEN)).append(Component.text(" に設定しました"))),
    ONLY_MARKED(Component.text("拾ったアイテムを").append(Component.text("そのアイテムのマーカーが設定されたシュルカーボックス", DefinedTextColor.AQUA)).append(Component.text("のみに収納します")), Component.text("シュルカーボックス自動回収のモードを ").append(Component.text("マーカー一致のボックスにのみ収納", DefinedTextColor.AQUA)).append(Component.text(" に設定しました"))),
    DISABLED(Component.text("シュルカーボックスへの").append(Component.text("自動回収機能は無効化", DefinedTextColor.RED)).append(Component.text("されています")), Component.text("シュルカーボックスへの").append(Component.text("自動回収機能を無効化", DefinedTextColor.RED)).append(Component.text("しました")));

    private final Component description;
    private final Component modeChangedMessage;

    ShulkerBoxRedirectorMode(Component description, Component modeChangedMessage) {
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
