package net.unknown.core.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.Arrays;

public class Suggestions {
    public static final SuggestionProvider<CommandSourceStack> ALL_PLAYER_SUGGEST = (ctx, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName), builder);
    };
    public static final SuggestionProvider<CommandSourceStack> WORLD_SUGGEST = (ctx, builder) -> {
        return SharedSuggestionProvider.suggest(Bukkit.getWorlds().stream().map(World::getName), builder);
    };
}
