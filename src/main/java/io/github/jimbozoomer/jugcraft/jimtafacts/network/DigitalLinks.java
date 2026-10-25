package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Native paired loaded endpoints. Identity checks make replacement/unlink fail closed. */
public final class DigitalLinks {
    public static final List<String> BLOCKS = List.of("me_p2p_tunnel", "item_p2p_tunnel", "fluid_p2p_tunnel", "fe_p2p_tunnel", "redstone_p2p_tunnel", "light_p2p_tunnel");
    public static final IntegerProperty OUTPUT = IntegerProperty.create("link_output", 0, 15);
    public static DataComponentType<Peer> LINK;
    public record Peer(GlobalPos position, UUID identity) {
        public static final Codec<Peer> CODEC = RecordCodecBuilder.create(i -> i.group(GlobalPos.CODEC.fieldOf("position").forGetter(Peer::position), UUIDUtil.CODEC.fieldOf("identity").forGetter(Peer::identity)).apply(i, Peer::new));
    }
    private DigitalLinks() {}
    public static void register() { LINK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_tunnel_link"), DataComponentType.<Peer>builder().persistent(Peer.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Peer.CODEC)).build()); }
    public static boolean tunnel(String kind) { return BLOCKS.contains(kind); }
    public static boolean signal(String kind) { return kind.equals("redstone_p2p_tunnel") || kind.equals("light_p2p_tunnel"); }
    static Peer identity(DigitalNode n) { return new Peer(GlobalPos.of(n.getLevel().dimension(), n.getBlockPos()), n.linkIdentity); }
    public static InteractionResult memory(UseOnContext c, DigitalNode node, ServerPlayer player) {
        if (player.isShiftKeyDown()) { c.getItemInHand().set(LINK, identity(node)); player.sendOverlayMessage(Component.literal("Tunnel input selected. Use this card on a matching output.")); return InteractionResult.SUCCESS; }
        var source = c.getItemInHand().get(LINK);
        var input=source==null?null:resolve(player.level(),source);
        if (source == null || input==null || input == node || !input.linkIdentity.equals(source.identity())
                || !input.kind().equals(node.kind()) || !input.permitted(player) || !node.permitted(player)) {
            player.sendOverlayMessage(Component.literal("Select a loaded input of the same tunnel type in this dimension first.")); return InteractionResult.FAIL;
        }
        input.peer = identity(node); node.peer = identity(input); input.linkInput = true; node.linkInput = false; input.setChanged(); node.setChanged();
        player.sendOverlayMessage(Component.literal("Paired input → output. Uses real stock and loaded, permitted endpoints only.")); return InteractionResult.SUCCESS;
    }
    static DigitalNode peer(DigitalNode node, ServerPlayer player) {
        var reference = node.peer;
        if (reference == null || !(node.getLevel() instanceof ServerLevel level) || !level.dimension().equals(reference.position().dimension()) || !level.isLoaded(reference.position().pos())) return null;
        var other=resolve(level,reference);
        if (other==null || !node.kind().equals(other.kind()) || !other.linkIdentity.equals(reference.identity())
                || other.peer == null || !other.peer.equals(identity(node)) || !other.permitted(player) || !node.permitted(player)) return null;
        return other;
    }
    private static DigitalNode resolve(ServerLevel level,Peer reference){
        if(!level.dimension().equals(reference.position().dimension()))return null;
        return DigitalParts.at(level,reference.position().pos()).stream().filter(n->n.linkIdentity.equals(reference.identity())).findFirst().orElse(null);
    }
    static List<BlockPos> bridges(ServerLevel level, BlockPos at, ServerPlayer player) {
        if(level.hasNeighborSignal(at))return List.of();var result=new ArrayList<BlockPos>();
        for(var node:DigitalParts.at(level,at))if(node.kind().equals("me_p2p_tunnel")){var other=peer(node,player);if(other!=null&&!level.hasNeighborSignal(other.getBlockPos())&&!result.contains(other.getBlockPos()))result.add(other.getBlockPos());}
        return result;
    }
    static void reset(DigitalNode node) { output(node, 0); }
    private static void output(DigitalNode node, int value) { var state = node.getBlockState(); if (state.hasProperty(OUTPUT) && state.getValue(OUTPUT) != value) node.updateSignal(state.setValue(OUTPUT, value)); }
    public static void tick(DigitalNode node, DigitalNetwork network, ServerPlayer actor) {
        if (!tunnel(node.kind()) || node.kind().equals("me_p2p_tunnel")) return;
        var other = peer(node, actor);
        if (other == null) { reset(node); return; }
        if (!node.linkInput) {
            if (signal(node.kind()) && (!DigitalNetwork.find(other, actor).online() || !other.linkInput)) reset(node);
            return;
        }
        if (!network.online() || node.getLevel().hasNeighborSignal(node.getBlockPos()) && !signal(node.kind())) { reset(other); return; }
        var level = actor.level(); var inputFace = node.getBlockState().getValue(DigitalNodeBlock.FACING); var outputFace = other.getBlockState().getValue(DigitalNodeBlock.FACING);
        var from = node.getBlockPos().relative(inputFace); var to = other.getBlockPos().relative(outputFace);
        if (from.equals(to) || !level.isLoaded(from) || !level.isLoaded(to) || !Authority.mayChange(level, actor, from) || !Authority.mayChange(level, actor, to)
                || !ClaimTransfers.allowed(level, from, to) || level.getBlockEntity(from) instanceof DigitalNode || level.getBlockEntity(to) instanceof DigitalNode
                || level.getBlockEntity(from) instanceof DigitalFacades.Covers || level.getBlockEntity(to) instanceof DigitalFacades.Covers) { reset(other); return; }
        try (var tx = Transaction.openOuter()) {
            if (!network.pay(10, tx)) { reset(other); return; }
            boolean moved = false;
            switch (node.kind()) {
                case "item_p2p_tunnel" -> {
                    var source = ItemStorage.SIDED.find(level, from, inputFace.getOpposite()); var target = ItemStorage.SIDED.find(level, to, outputFace.getOpposite());
                    if (source != null && target != null && source != target) { int seen = 0; for (var view : source) {
                        if (++seen > 128) break; if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                        long n = target.insert(view.getResource(), Math.min(64, view.getAmount()), tx);
                        if (n > 0 && source.extract(view.getResource(), n, tx) == n) moved = true;
                        break;
                    } }
                }
                case "fluid_p2p_tunnel" -> {
                    var source = FluidStorage.SIDED.find(level, from, inputFace.getOpposite()); var target = FluidStorage.SIDED.find(level, to, outputFace.getOpposite());
                    if (source != null && target != null && source != target) { int seen = 0; for (var view : source) {
                        if (++seen > 64) break; if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                        long n = target.insert(view.getResource(), Math.min(FluidConstants.BUCKET * 4, view.getAmount()), tx);
                        if (n > 0 && source.extract(view.getResource(), n, tx) == n) moved = true;
                        break;
                    } }
                }
                case "fe_p2p_tunnel" -> {
                    var source = EnergyStorage.SIDED.find(level, from, inputFace.getOpposite()); var target = EnergyStorage.SIDED.find(level, to, outputFace.getOpposite());
                    if (source != null && target != null && source.transferIdentity() != target.transferIdentity()) { long n = target.insert(Math.min(4096, source.getAmount()), tx); moved = n > 0 && source.extract(n, tx) == n; }
                }
                case "redstone_p2p_tunnel" -> { output(other, level.getSignal(from, inputFace)); moved = true; }
                case "light_p2p_tunnel" -> { output(other, level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, from)); moved = true; }
                default -> { }
            }
            if (moved) tx.commit();
        }
    }
    public static final class SignalBlock extends DigitalNodeBlock {
        public SignalBlock(Properties p, String kind) { super(p.lightLevel(s -> kind.equals("light_p2p_tunnel") ? s.getValue(OUTPUT) : 0), kind); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(OUTPUT); }
        @Override protected boolean isSignalSource(BlockState s) { return kind.equals("redstone_p2p_tunnel"); }
        @Override protected int getSignal(BlockState s, BlockGetter w, BlockPos p, Direction d) { return kind.equals("redstone_p2p_tunnel") ? s.getValue(OUTPUT) : 0; }
    }
}
