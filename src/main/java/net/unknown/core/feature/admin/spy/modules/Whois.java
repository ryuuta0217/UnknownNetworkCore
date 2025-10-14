package net.unknown.core.feature.admin.spy.modules;

import net.kyori.adventure.text.Component;
import net.unknown.core.feature.admin.spy.SpyModule;
import org.bukkit.NamespacedKey;

public class Whois implements SpyModule {
    public static final NamespacedKey IDENTIFIER = NamespacedKey.fromString("unknown-network:whois");

    @Override
    public void onRegistering() {

    }

    @Override
    public void onUnRegistering() {

    }

    @Override
    public NamespacedKey getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Whois");
    }
}
