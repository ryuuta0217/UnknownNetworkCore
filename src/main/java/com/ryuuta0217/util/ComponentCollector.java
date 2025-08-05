/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.util;

import net.kyori.adventure.text.Component;

import java.util.stream.Collector;

public class ComponentCollector {
    private Component component = Component.empty();
    private final Component delimiter;

    public static Collector<Component, ComponentCollector, ComponentCollector> toComponent(Component delimiter) {
        return Collector.of(() -> new ComponentCollector(Component.empty(), delimiter), ComponentCollector::add, ComponentCollector::combine);
    }

    public ComponentCollector() {
        this.delimiter = null;
    }

    public ComponentCollector(Component component, Component delimiter) {
        this.component = component;
        this.delimiter = delimiter;
    }

    public void add(Component component) {
        if (this.delimiter != null && !this.component.equals(Component.empty())) {
            this.component = this.component.append(this.delimiter);
        }
        this.component = this.component.append(component);
    }

    public ComponentCollector combine(ComponentCollector other) {
        if (this.delimiter != null) {
            this.component = this.component.append(this.delimiter);
        }
        this.component = this.component.append(other.component);
        return this;
    }

    public Component asComponent() {
        return this.component;
    }
}
