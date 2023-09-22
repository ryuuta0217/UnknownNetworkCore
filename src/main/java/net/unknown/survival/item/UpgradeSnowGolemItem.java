package net.unknown.survival.item;

import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.item.UnknownNetworkItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

public class UpgradeSnowGolemItem extends UnknownNetworkItem implements Listener {
    public UpgradeSnowGolemItem(NamespacedKey id) {
        super(new NamespacedKey("survival", "upgrade_snow_golem"));
    }

    @Override
    public UnknownNetworkItemStack<? extends UnknownNetworkItem> createItemStack() {
        return null;
    }
}
