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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JISかな入力モードで入力された半角キーストロークを「かな文字」に変換するクラス
 *
 * <p>JIS配列キーボードのかな入力モードでは、各QWERTYキーに直接かな文字が割り当てられている。
 * このクラスは、半角モードのまま入力されたキー (例えば、「byiaf」)を
 * 対応するかな文字列(例の場合「こんにちは」)に変換する。</p>
 *
 * <p>濁点（゛）・半濁点（゜）は、Minecraftのチャット入力では直接入力が困難なため、
 * 前の文字と結合して濁音・半濁音に変換する処理を行う。</p>
 *
 * @see <a href="https://ja.wikipedia.org/wiki/JIS%E9%85%8D%E5%88%97">JIS配列</a>
 * @author ryuuta0217
 */
public class JisKanaConverter {

    // JISかな配列: キー → かな のマッピング（通常キー）
    private static final Map<String, String> KEY_MAP;

    // 濁点変換マップ: 清音 → 濁音
    private static final Map<String, String> DAKUTEN_MAP;

    // 半濁点変換マップ: は行 → ぱ行
    private static final Map<String, String> HANDAKUTEN_MAP;

    static {
        // --- 通常キーマッピング ---
        Map<String, String> keyMap = new LinkedHashMap<>();

        // 数字行
        keyMap.put("1", "ぬ");
        keyMap.put("2", "ふ");
        keyMap.put("3", "あ");
        keyMap.put("4", "う");
        keyMap.put("5", "え");
        keyMap.put("6", "お");
        keyMap.put("7", "や");
        keyMap.put("8", "ゆ");
        keyMap.put("9", "よ");
        keyMap.put("0", "わ");
        keyMap.put("-", "ほ");
        // "^" → "へ" (JISキーボード上の位置、USキーボードでは "=" の位置に相当)
        // "¥" → "ー" (JISキーボード固有キー)

        // 上段 (Q-P)
        keyMap.put("q", "た");
        keyMap.put("w", "て");
        keyMap.put("e", "い");
        keyMap.put("r", "す");
        keyMap.put("t", "か");
        keyMap.put("y", "ん");
        keyMap.put("u", "な");
        keyMap.put("i", "に");
        keyMap.put("o", "ら");
        keyMap.put("p", "せ");

        // 中段 (A-L, ;)
        keyMap.put("a", "ち");
        keyMap.put("s", "と");
        keyMap.put("d", "し");
        keyMap.put("f", "は");
        keyMap.put("g", "き");
        keyMap.put("h", "く");
        keyMap.put("j", "ま");
        keyMap.put("k", "の");
        keyMap.put("l", "り");
        keyMap.put(";", "れ");

        // 下段 (Z-M, , . /)
        keyMap.put("z", "つ");
        keyMap.put("x", "さ");
        keyMap.put("c", "そ");
        keyMap.put("v", "ひ");
        keyMap.put("b", "こ");
        keyMap.put("n", "み");
        keyMap.put("m", "も");
        keyMap.put(",", "ね");
        keyMap.put(".", "る");
        keyMap.put("/", "め");

        // Shift + 数字キー (小文字かな等)
        keyMap.put("!", "ぬ");   // Shift+1 (変化なし、環境依存)
        keyMap.put("\"", "ふ");  // Shift+2 (変化なし、環境依存)
        keyMap.put("#", "ぁ");   // Shift+3
        keyMap.put("$", "ぅ");   // Shift+4
        keyMap.put("%", "ぇ");   // Shift+5
        keyMap.put("&", "ぉ");   // Shift+6
        keyMap.put("'", "ゃ");   // Shift+7
        keyMap.put("(", "ゅ");   // Shift+8
        keyMap.put(")", "ょ");   // Shift+9
        // Shift+0 → を
        // "~" → へ (Shift+^)

        // Shift + アルファベットキー (大文字)
        keyMap.put("Q", "た");
        keyMap.put("W", "て");
        keyMap.put("E", "ぃ");   // Shift+E → 小さい「ぃ」
        keyMap.put("R", "す");
        keyMap.put("T", "か");
        keyMap.put("Y", "ん");
        keyMap.put("U", "な");
        keyMap.put("I", "に");
        keyMap.put("O", "ら");
        keyMap.put("P", "せ");

        keyMap.put("A", "ち");
        keyMap.put("S", "と");
        keyMap.put("D", "し");
        keyMap.put("F", "は");
        keyMap.put("G", "き");
        keyMap.put("H", "く");
        keyMap.put("J", "ま");
        keyMap.put("K", "の");
        keyMap.put("L", "り");

        keyMap.put("Z", "っ");   // Shift+Z → 小さい「っ」
        keyMap.put("X", "さ");
        keyMap.put("C", "そ");
        keyMap.put("V", "ひ");
        keyMap.put("B", "こ");
        keyMap.put("N", "み");
        keyMap.put("M", "も");
        keyMap.put("<", "、");   // Shift+, → 読点
        keyMap.put(">", "。");   // Shift+. → 句点
        keyMap.put("?", "・");   // Shift+/ → 中黒

        // JIS固有キー
        keyMap.put("@", "゛");   // 濁点キー (JIS配列の@位置)
        keyMap.put("[", "゜");   // 半濁点キー (JIS配列の[位置)
        keyMap.put("]", "む");   // ]キー
        keyMap.put("\\", "ろ");  // \キー (JIS配列)
        keyMap.put(":", "け");   // :キー (JIS配列)
        keyMap.put("=", "へ");   // =キー (USキーボードでの代替)
        keyMap.put("^", "へ");   // ^キー (JIS配列)
        keyMap.put("_", "ろ");   // Shift+\
        keyMap.put("{", "「");   // Shift+[
        keyMap.put("}", "」");   // Shift+]
        keyMap.put("+", "れ");   // Shift+;の代替
        keyMap.put("`", "ろ");   // バッククォート (代替)
        keyMap.put("~", "へ");   // Shift+^ (代替)

        KEY_MAP = Map.copyOf(keyMap);

        // --- 濁点変換マップ ---
        Map<String, String> dakutenMap = new LinkedHashMap<>();
        // か行 → が行
        dakutenMap.put("か", "が");
        dakutenMap.put("き", "ぎ");
        dakutenMap.put("く", "ぐ");
        dakutenMap.put("け", "げ");
        dakutenMap.put("こ", "ご");
        // さ行 → ざ行
        dakutenMap.put("さ", "ざ");
        dakutenMap.put("し", "じ");
        dakutenMap.put("す", "ず");
        dakutenMap.put("せ", "ぜ");
        dakutenMap.put("そ", "ぞ");
        // た行 → だ行
        dakutenMap.put("た", "だ");
        dakutenMap.put("ち", "ぢ");
        dakutenMap.put("つ", "づ");
        dakutenMap.put("て", "で");
        dakutenMap.put("と", "ど");
        // は行 → ば行
        dakutenMap.put("は", "ば");
        dakutenMap.put("ひ", "び");
        dakutenMap.put("ふ", "ぶ");
        dakutenMap.put("へ", "べ");
        dakutenMap.put("ほ", "ぼ");
        // う → ゔ
        dakutenMap.put("う", "ゔ");
        // 小さいかな
        dakutenMap.put("っ", "づ");

        DAKUTEN_MAP = Map.copyOf(dakutenMap);

        // --- 半濁点変換マップ ---
        Map<String, String> handakutenMap = new LinkedHashMap<>();
        // は行 → ぱ行
        handakutenMap.put("は", "ぱ");
        handakutenMap.put("ひ", "ぴ");
        handakutenMap.put("ふ", "ぷ");
        handakutenMap.put("へ", "ぺ");
        handakutenMap.put("ほ", "ぽ");

        HANDAKUTEN_MAP = Map.copyOf(handakutenMap);
    }

    protected JisKanaConverter() {
    }

    /**
     * JISかな入力モードのキーストロークを「かな文字」に変換する
     *
     * <p>各キーを1文字ずつかなに変換し、濁点（゛）・半濁点（゜）が現れた場合は
     * 直前の文字と結合して濁音・半濁音に変換する。</p>
     *
     * @param input JISかな入力モードで入力された半角キーストローク
     * @return 変換後の「かな文字」
     */
    public static String conv(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            String key = String.valueOf(input.charAt(i));
            String kana = KEY_MAP.get(key);

            if (kana == null) {
                // マッピングに存在しないキーはそのまま出力
                result.append(key);
                continue;
            }

            if ("゛".equals(kana)) {
                // 濁点: 直前の文字を濁音に変換
                if (result.length() > 0) {
                    String lastChar = String.valueOf(result.charAt(result.length() - 1));
                    String dakuon = DAKUTEN_MAP.get(lastChar);
                    if (dakuon != null) {
                        result.setCharAt(result.length() - 1, dakuon.charAt(0));
                    } else {
                        // 濁音に変換できない場合は濁点をそのまま追加
                        result.append(kana);
                    }
                } else {
                    result.append(kana);
                }
            } else if ("゜".equals(kana)) {
                // 半濁点: 直前の文字を半濁音に変換
                if (result.length() > 0) {
                    String lastChar = String.valueOf(result.charAt(result.length() - 1));
                    String handakuon = HANDAKUTEN_MAP.get(lastChar);
                    if (handakuon != null) {
                        result.setCharAt(result.length() - 1, handakuon.charAt(0));
                    } else {
                        // 半濁音に変換できない場合は半濁点をそのまま追加
                        result.append(kana);
                    }
                } else {
                    result.append(kana);
                }
            } else {
                result.append(kana);
            }
        }

        return result.toString();
    }
}
