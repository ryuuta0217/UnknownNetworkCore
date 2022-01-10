package net.unknown.core.gui;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SignGui {
    private static final Map<UUID, SignGui> SIGN_GUI_OPENED = new HashMap<>();

    private Player target;
    private Material signType = Material.OAK_SIGN;
    private Component[] defaultLines$adventure = new Component[]{Component.empty(), Component.empty(), Component.empty(), Component.empty()};
    private net.minecraft.network.chat.Component[] defaultLines = new net.minecraft.network.chat.Component[]{TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY, TextComponent.EMPTY};
    private Consumer<List<Component>> completeHandler;
    private final Logger logger = Logger.getLogger("SignGui@" + this.hashCode());
    private boolean isOpened = false;

    public SignGui withTarget(Player player) {
        this.target = player;
        return this;
    }

    public SignGui withSignType(Material signType) {
        if(signType.name().endsWith("_SIGN")) this.signType = signType;
        return this;
    }

    public SignGui withLines(Component lineOne, Component lineTwo, Component lineThree, Component lineFour) {
        this.defaultLines$adventure = new Component[] {
                lineOne != null ? lineOne : Component.empty(),
                lineTwo != null ? lineTwo : Component.empty(),
                lineThree != null ? lineThree : Component.empty(),
                lineFour != null ? lineFour : Component.empty()
        };
        convertAdventureToNMS();
        return this;
    }

    public SignGui onComplete(Consumer<List<Component>> onComplete) {
        this.completeHandler = onComplete;
        return this;
    }

    public void open() {
        if(this.target == null) throw new IllegalStateException("Target is null. Please use SignGui#withTarget to specific player.");
        if(this.completeHandler == null) logger.warning("Complete handler is null, it is stupid.");
        if(SignGui.SIGN_GUI_OPENED.containsKey(this.target.getUniqueId())) throw new IllegalStateException("Double Sign Gui?");

        Location bukkitLoc = this.target.getLocation();
        bukkitLoc.setY(1);
        Vec3 nmsLoc = new Vec3(bukkitLoc.getX(), bukkitLoc.getY(), bukkitLoc.getZ());
        BlockPos blockPos = new BlockPos(nmsLoc);
        BlockState signBlock = CraftMagicNumbers.getBlock(this.signType).defaultBlockState();

        SignBlockEntity sign = new SignBlockEntity(blockPos, signBlock);
        System.arraycopy(this.defaultLines, 0, sign.messages, 0, 4);

        ClientboundBlockUpdatePacket blockUpdatePacket = new ClientboundBlockUpdatePacket(blockPos, signBlock);
        ClientboundOpenSignEditorPacket signEditorPacket = new ClientboundOpenSignEditorPacket(blockPos);

        ServerPlayer nmsTarget = ((CraftPlayer) this.target).getHandle();
        nmsTarget.connection.send(blockUpdatePacket);
        nmsTarget.connection.send(signEditorPacket);

        this.isOpened = true;
        SignGui.SIGN_GUI_OPENED.put(this.target.getUniqueId(), this);
    }

    private void convertAdventureToNMS() {
        this.defaultLines = Arrays.stream(this.defaultLines$adventure)
                .map(advC -> (net.minecraft.network.chat.Component) net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(advC)))
                .toArray(net.minecraft.network.chat.Component[]::new);
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            ChannelDuplexHandler packetHandler = new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if(msg instanceof ServerboundSignUpdatePacket packet) {
                        if(SignGui.SIGN_GUI_OPENED.containsKey(event.getPlayer().getUniqueId())) {
                            SignGui gui = SignGui.SIGN_GUI_OPENED.get(event.getPlayer().getUniqueId());
                            if(gui.isOpened) {
                                gui.isOpened = false;
                                SignGui.SIGN_GUI_OPENED.remove(event.getPlayer().getUniqueId());
                                if (gui.completeHandler != null) gui.completeHandler.accept(Arrays.stream(packet.getLines()).map(line -> (Component) Component.text(line)).toList());
                            }
                        }
                    }
                    super.channelRead(ctx, msg);
                }
            };
            ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
            pipeline.addBefore("packet_handler", player.getName(), packetHandler);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Channel c = ((CraftPlayer) event.getPlayer()).getHandle().connection.connection.channel;
            c.eventLoop().submit(() -> c.pipeline().remove(event.getPlayer().getName()));
            if(SIGN_GUI_OPENED.containsKey(event.getPlayer().getUniqueId())) {
                SignGui gui = SIGN_GUI_OPENED.get(event.getPlayer().getUniqueId());
                gui.isOpened = false;
                SIGN_GUI_OPENED.remove(event.getPlayer().getUniqueId());
            }
        }
    }
}
