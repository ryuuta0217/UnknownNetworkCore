package net.unknown.survival.dependency;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.royawesome.jlibnoise.module.combiner.Min;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class MultiverseCore {
    private static final boolean MULTIVERSE_CORE_ENABLED = Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null && Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core");

    public static boolean isMultiverseCoreEnabled() {
        return MULTIVERSE_CORE_ENABLED;
    }

    public static Location getSpawnLocation(World world) {
        if (!MULTIVERSE_CORE_ENABLED) return world.getSpawnLocation();
        return getInstance().getMVWorldManager().getMVWorld(world).getSpawnLocation();
    }

    public static Location getSpawnLocation(Level level) {
        Vec3 positionVector3 = level.getSharedSpawnPos().getCenter();
        Vec2 rotationVector2 = new Vec2(level.getSharedSpawnAngle(), 0);
        if (MULTIVERSE_CORE_ENABLED) {
            Location multiverseSpawnLocation = getSpawnLocation(MinecraftAdapter.world(level));
            positionVector3 = MinecraftAdapter.vec3(multiverseSpawnLocation);
            rotationVector2 = MinecraftAdapter.vec2(multiverseSpawnLocation);
        }
        return MinecraftAdapter.location(level, positionVector3, rotationVector2);
    }

    public static com.onarandombox.MultiverseCore.MultiverseCore getInstance() {
        return (com.onarandombox.MultiverseCore.MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
    }
}
