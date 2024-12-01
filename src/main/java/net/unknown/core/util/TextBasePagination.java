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

import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.define.DefinedTextColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TextBasePagination<E> {
    private final boolean alwaysShowControl;
    private final Component title;
    private final String prevIcon;
    private final String nextIcon;
    @Nullable private final Function<Component, Component> headerModifier;
    @Nullable private final Function<Component, Component> footerModifier;
    private final Function<Integer, String> pageCommandMapper;
    private final List<Set<E>> pages;
    private final BiFunction<E, Integer, Component> mapper;
    private int page;
    private final List<Component> textLines = new ArrayList<>();

    @ParametersAreNonnullByDefault
    public TextBasePagination(Collection<E> elements, BiFunction<E, Integer, Component> mapper, int elementPerPage, int currentPage, boolean alwaysShowControl, Component title, String prevIcon, String nextIcon, @Nullable Function<Component, Component> headerModifier, @Nullable Function<Component, Component> footerModifier, Function<Integer, String> pageCommandMapper) {
        this.alwaysShowControl = alwaysShowControl;
        this.title = title;
        this.prevIcon = prevIcon;
        this.nextIcon = nextIcon;
        this.headerModifier = headerModifier;
        this.footerModifier = footerModifier;
        this.pageCommandMapper = pageCommandMapper;

        this.pages = ListUtil.splitListAsLinkedSet(elements, elementPerPage);
        this.mapper = mapper;
        this.page = currentPage;
        this.setPage(currentPage);
    }

    public TextBasePagination(Collection<E> elements, BiFunction<E, Integer, Component> mapper, int elementPerPage, int currentPage, boolean alwaysShowControl, Component title, String prevIcon, String nextIcon, Function<Integer, String> pageCommandMapper) {
        this(elements, mapper, elementPerPage, currentPage, alwaysShowControl, title, prevIcon, nextIcon, null, null, pageCommandMapper);
    }

    public TextBasePagination(Collection<E> elements, BiFunction<E, Integer, Component> mapper, int elementPerPage, int currentPage, boolean alwaysShowControl, Component title, Function<Integer, String> pageCommandMapper) {
        this(elements, mapper, elementPerPage, currentPage, alwaysShowControl, title, "◀", "▶", null, null, pageCommandMapper);
    }

    public TextBasePagination(Collection<E> elements, BiFunction<E, Integer, Component> mapper, int elementPerPage, boolean alwaysShowControl, Component title, Function<Integer, String> pageCommandMapper) {
        this(elements, mapper, elementPerPage, 1, alwaysShowControl, title, "◀", "▶", null, null, pageCommandMapper);
    }

    public int getPage() {
        return this.page;
    }

    public void setPage(int page) {
        this.page = page;
        this.textLines.clear();

        int splitterChars = 40;

        String headerStr = "=".repeat(splitterChars - PlainTextComponentSerializer.plainText().serialize(this.title).length());
        Component header = Component.text(headerStr.substring(0, headerStr.length() / 2)).appendSpace().append(this.title).appendSpace().append(Component.text(headerStr.substring(headerStr.length() / 2)));
        if (this.headerModifier != null) header = this.headerModifier.apply(header);
        this.textLines.add(header);

        this.pages.get(this.page - 1).forEach(e -> this.textLines.add(this.mapper.apply(e, this.textLines.size())));

        Component footerController = null;
        if (this.alwaysShowControl || this.pages.size() > 1) {
            footerController = Component.empty()
                    .append(Component.text(this.prevIcon, this.page > 1 ? DefinedTextColor.GREEN : DefinedTextColor.DARK_GRAY)
                            .clickEvent(this.page > 1 ? ClickEvent.runCommand(this.pageCommandMapper.apply(this.page - 1)) : null))
                    .appendSpace()
                    .append(Component.text("|" + this.page + "/" + this.pages.size() + "|", DefinedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text(this.nextIcon, this.page < this.pages.size() ? DefinedTextColor.GREEN : DefinedTextColor.DARK_GRAY)
                            .clickEvent(this.page < this.pages.size() ? ClickEvent.runCommand(this.pageCommandMapper.apply(this.page + 1)) : null));
        }
        String footerStr = "=".repeat(splitterChars - (footerController != null ? PlainTextComponentSerializer.plainText().serialize(footerController).length() : 0));
        Component footer = Component.text(footerStr.substring(0, footerStr.length() / 2)).appendSpace().append(footerController != null ? footerController : Component.empty()).appendSpace().append(Component.text(footerStr.substring(footerStr.length() / 2)));
        if (this.footerModifier != null) footer = this.footerModifier.apply(footer);
        this.textLines.add(footer);
    }

    public List<Component> getTextLines() {
        return Collections.unmodifiableList(this.textLines);
    }
}
