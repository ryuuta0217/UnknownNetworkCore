package net.unknown.survival.bossbar;

import net.minecraft.resources.Identifier;
import net.unknown.core.managers.BossBarManager;
import net.unknown.survival.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public class SurvivalVisibilityHandler implements BossBarManager.VisibilityHandler {
    private static final NamespacedKey REGISTRY_KEY = new NamespacedKey("survival", "bossbar_visibility");

    @Override
    public boolean isVisible(Player player, Identifier identifier) {
        return PlayerData.of(player).getRegistries().getOrDefault(REGISTRY_KEY, identifier.toString(), "true").equalsIgnoreCase("true");
    }

    @Override
    public void setVisible(Player player, Identifier identifier, boolean visible) {
        PlayerData.of(player).getRegistries().put(REGISTRY_KEY, identifier.toString(), Boolean.toString(visible));
    }
}
