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
        if (this.delimiter != null) {
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
