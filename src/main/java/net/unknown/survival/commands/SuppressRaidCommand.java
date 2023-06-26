package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unknown.core.commands.Suggestions;
import net.unknown.core.commands.brigadier.CustomBrigadierExceptions;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.enums.Permissions;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidTriggerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// /suppressraid
// /suppressraid add <BlockPos: center position> <double: radius of suppress raids from center position> [string: world]
// /suppressraid remove <BlockPos: center position>
public class SuppressRaidCommand extends ConfigurationBase implements Listener {
    private static final SuppressRaidCommand INSTANCE = new SuppressRaidCommand();
    private static final SuggestionProvider<CommandSourceStack> SUPPRESSED_POSITIONS_SUGGEST = (ctx, builder) -> {
        ServerLevel level = ctx.getSource().getLevel();
        if (BrigadierUtil.isArgumentKeyExists(ctx, "world")) {
            String worldName = StringArgumentType.getString(ctx, "world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                level = MinecraftAdapter.level(world);
            }
        }

        if (INSTANCE.suppressRaids.containsKey(level.dimension())) {
            INSTANCE.suppressRaids.get(level.dimension()).forEach(suppress -> {
                builder.suggest(suppress.getLeft().getX() + " " + suppress.getLeft().getY() + " " + suppress.getLeft().getZ(), Component.literal("distance: " + suppress.getRight()));
            });
        }
        return builder.buildFuture();
    };

    static {
        ListenerManager.registerListener(INSTANCE);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("suppressraid");
        builder.requires(Permissions.COMMAND_SUPPRESSRAID::check);

        builder.executes(SuppressRaidCommand::showList)
                .then(Commands.literal("list")
                        .executes(SuppressRaidCommand::showList)
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(Suggestions.WORLD_SUGGEST)
                                .executes(SuppressRaidCommand::showList)));

        builder.then(Commands.literal("add")
                .then(Commands.argument("center-position", Vec3Argument.vec3())
                        .then(Commands.argument("suppress-radius", DoubleArgumentType.doubleArg())
                                .executes(SuppressRaidCommand::add)
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests(Suggestions.WORLD_SUGGEST)
                                        .executes(SuppressRaidCommand::add)))));

        builder.then(Commands.literal("remove")
                .then(Commands.argument("world", StringArgumentType.word())
                        .suggests(Suggestions.WORLD_SUGGEST)
                        .then(Commands.argument("center-position", Vec3Argument.vec3())
                                .suggests(SUPPRESSED_POSITIONS_SUGGEST)
                                .executes(SuppressRaidCommand::remove))
                .then(Commands.argument("center-position", BlockPosArgument.blockPos())
                        .suggests(SUPPRESSED_POSITIONS_SUGGEST)
                        .executes(SuppressRaidCommand::remove))));

        dispatcher.register(builder);
    }

    private static int showList(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = BrigadierUtil.getArgumentOrDefault(ctx, (worldName, defaultValue) -> {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return MinecraftAdapter.level(world);
            }
            return defaultValue;
        }, String.class, "world", null);

        if (level == null) {
            INSTANCE.suppressRaids.keySet().forEach(levelKey -> {
                ServerLevel world = MinecraftServer.getServer().getLevel(levelKey);
                showList(ctx, world);
            });
        } else {
            showList(ctx, level);
        }
        return 0;
    }

    private static void showList(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
        Set<Pair<BlockPos, Double>> suppressRaids = INSTANCE.suppressRaids.getOrDefault(level.dimension(), new HashSet<>());

        MutableComponent message = Component.empty();
        message.append(Component.literal("====== " + level.getWorld().getName() + "の襲撃抑制地点リスト (" + suppressRaids.size() + ") ======").withStyle(ChatFormatting.GOLD));

        suppressRaids.forEach(suppressRaid -> {
            message.append("\n");
            message.append(Component.literal("中心座標: " + suppressRaid.getLeft().getX() + ", " + suppressRaid.getLeft().getY() + ", " + suppressRaid.getLeft().getZ()).withStyle(ChatFormatting.AQUA));
            message.append(" | ");
            message.append(Component.literal("抑制半径: " + suppressRaid.getRight() + "m").withStyle(ChatFormatting.LIGHT_PURPLE));
        });

        NewMessageUtil.sendMessage(ctx.getSource(), message, false);
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = getLevelOrThrow(ctx, "world");
        BlockPos centerPos = BlockPosArgument.getBlockPos(ctx, "center-position");
        double suppressRadius = DoubleArgumentType.getDouble(ctx, "suppress-radius");

        if (!INSTANCE.suppressRaids.containsKey(level.dimension())) INSTANCE.suppressRaids.put(level.dimension(), new HashSet<Pair<net.minecraft.core.BlockPos, Double>>());
        INSTANCE.suppressRaids.get(level.dimension()).add(Pair.of(centerPos, suppressRadius));
        RunnableManager.runAsync(INSTANCE::save);
        return 0;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException{
        ServerLevel level = getLevelOrThrow(ctx, "world");
        BlockPos centerPos = BlockPosArgument.getBlockPos(ctx, "center-position");

        if (INSTANCE.suppressRaids.containsKey(level.dimension())) {
            INSTANCE.suppressRaids.get(level.dimension()).removeIf(suppress -> suppress.getLeft().equals(centerPos));
            return 0;
        } else {
            return 1;
        }
    }

    private static ServerLevel getLevelOrThrow(CommandContext<CommandSourceStack> ctx, String argName) throws CommandSyntaxException {
        ServerLevel level = BrigadierUtil.getArgumentOrDefault(ctx, (worldName, defaultValue) -> {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return MinecraftAdapter.level(world);
            }
            return null;
        }, String.class, argName, ctx.getSource().getLevel());

        if (BrigadierUtil.isArgumentKeyExists(ctx, argName) && level == null) {
            throw CustomBrigadierExceptions.UNKNOWN_DIMENSION.create(StringArgumentType.getString(ctx, argName));
        }
        return level;
    }

    private final Map<ResourceKey<Level>, Set<Pair<BlockPos, Double>>> suppressRaids = new HashMap<>();

    private SuppressRaidCommand() {
        super("suppress-raids.yml", false, "UNC/SuppressRaids");
    }

    @Override
    public void onLoad() {
        if (this.getConfig().contains("suppress-raids")) {
            ConfigurationSection suppressRaidsSection = this.getConfig().getConfigurationSection("suppress-raids");
            suppressRaidsSection.getKeys(false).forEach(levelKey -> {
                ConfigurationSection levelSection = suppressRaidsSection.getConfigurationSection(levelKey);
                ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(levelKey));
                Set<Pair<BlockPos, Double>> leveledSuppressRaids = new HashSet<>();
                levelSection.getKeys(false).forEach(centerPosKey -> {
                    ConfigurationSection suppressRaidSection = levelSection.getConfigurationSection(centerPosKey);
                    String[] centerPosStr = centerPosKey.split(",");
                    BlockPos centerPos = new BlockPos(Integer.parseInt(centerPosStr[0]), Integer.parseInt(centerPosStr[1]), Integer.parseInt(centerPosStr[2]));
                    double suppressRadius = suppressRaidSection.getDouble("radius");
                    leveledSuppressRaids.add(Pair.of(centerPos, suppressRadius));
                });
                this.suppressRaids.put(level, leveledSuppressRaids);
            });
        }
    }

    @Override
    public synchronized void save() {
        this.getConfig().set("suppress-raids", null);
        ConfigurationSection suppressRaidsSection = this.getConfig().createSection("suppress-raids");
        this.suppressRaids.forEach((levelKey, leveledSuppressRaids) -> {
            ConfigurationSection levelSection = suppressRaidsSection.createSection(levelKey.location().toString());
            leveledSuppressRaids.forEach(suppressRaid -> {
                BlockPos centerPos = suppressRaid.getLeft();
                ConfigurationSection suppressRaidSection = levelSection.createSection(centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ());
                suppressRaidSection.set("radius", suppressRaid.getRight());
            });
        });
        super.save();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRaidTriggered(RaidTriggerEvent event) {
        Location raidLocationBukkit = event.getRaid().getLocation();
        ServerLevel raidLevel = MinecraftAdapter.level(raidLocationBukkit.getWorld());
        Vec3 raidLocation = MinecraftAdapter.vec3(raidLocationBukkit);
        if (this.suppressRaids.containsKey(raidLevel.dimension())) {
            boolean suppress = this.suppressRaids.get(raidLevel.dimension()).stream()
                    .anyMatch(suppressRaid -> suppressRaid.getLeft().getCenter().distanceTo(raidLocation) <= suppressRaid.getRight());
            event.setCancelled(suppress);
        }
    }
}
