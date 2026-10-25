package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Paired, loaded 3×3 quantum rings share real storage. No chunk tickets or dimension-wide scan. */
public final class DigitalQuantum {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed"), CORNER = BooleanProperty.create("corner");
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    private DigitalQuantum() {}
    public static void register() {
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_quantum_ring"));
        Block ring = Registry.register(BuiltInRegistries.BLOCK, key, new Ring(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().setId(key)));
        DigitalStorage.BLOCKS.put("quantum_ring", ring); var ik = ResourceKey.create(Registries.ITEM, key.identifier());
        Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(ring, new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
        var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_quantum_entangled_singularity"));
        DigitalStorage.ITEMS.put("quantum_entangled_singularity", Registry.register(BuiltInRegistries.ITEM, itemKey, new Key(new Item.Properties().setId(itemKey))));
    }
    public static final class Core extends DigitalNodeBlock implements io.github.jimbozoomer.jugcraft.energy.EnergyConnectable {
        public Core(Properties p) { super(p, "quantum_link"); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(FORMED); SIDES.values().forEach(b::add); }
    }
    public static final class Ring extends Block {
        Ring(Properties p) { super(p); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FORMED, CORNER, DigitalNodeBlock.LIT); SIDES.values().forEach(b::add); }
    }
    /** Returns the eight verified frame positions or empty; permission is checked at every use. */
    static Map<BlockPos, Boolean> frame(DigitalNode core, ServerPlayer actor) {
        var world = core.getLevel(); var at = core.getBlockPos();
        for (var axis : Direction.Axis.values()) {
            var positions = new LinkedHashMap<BlockPos, Boolean>();
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) if (a != 0 || b != 0) {
                var p = switch (axis) { case X -> at.offset(0, a, b); case Y -> at.offset(a, 0, b); case Z -> at.offset(a, b, 0); };
                if (!world.isLoaded(p) || !world.getBlockState(p).is(DigitalStorage.BLOCKS.get("quantum_ring")) || !Authority.mayChange(actor.level(), actor, p)) { positions.clear(); a = 2; break; }
                positions.put(p, a != 0 && b != 0);
            }
            if (positions.size() == 8) return positions;
        }
        return Map.of();
    }
    static List<DigitalNetwork> networks(DigitalNetwork local) {
        if (!local.online() || local.actor() == null) return List.of();
        var networks = new ArrayList<DigitalNetwork>(); var seen = new HashSet<GlobalPos>();
        local.nodes().forEach(n -> seen.add(GlobalPos.of(n.getLevel().dimension(), n.getBlockPos())));
        int links = 0;
        for (var core : local.nodes()) {
            if (!core.kind().equals("quantum_link") || ++links > 8 || core.peer == null || !core.getItem(0).is(DigitalStorage.ITEMS.get("quantum_entangled_singularity")) || frame(core, local.actor()).isEmpty()) continue;
            var position = core.peer.position(); var world = local.actor().level().getServer().getLevel(position.dimension());
            if (world == null || !world.isLoaded(position.pos()) || seen.contains(position)) continue;
            if (!(world.getBlockEntity(position.pos()) instanceof DigitalNode peer) || !peer.kind().equals("quantum_link") || !peer.linkIdentity.equals(core.peer.identity())
                    || peer.peer == null || !peer.peer.equals(DigitalLinks.identity(core)) || !peer.getItem(0).is(DigitalStorage.ITEMS.get("quantum_entangled_singularity"))) continue;
            var actor = world == local.actor().level() ? local.actor() : peer.actor();
            if (actor == null || !(actor.getUUID().equals(local.actor().getUUID()) || JugcraftParties.sameParty(actor.getUUID(), local.actor().getUUID())) || !peer.permitted(actor) || frame(peer, actor).isEmpty()) continue;
            var remote = DigitalNetwork.findLocal(peer, actor);
            if (!remote.online() || remote.nodes().stream().anyMatch(n -> seen.contains(GlobalPos.of(n.getLevel().dimension(), n.getBlockPos())))) continue;
            remote.nodes().forEach(n -> seen.add(GlobalPos.of(n.getLevel().dimension(), n.getBlockPos()))); networks.add(remote);
        }
        return List.copyOf(networks);
    }
    static void tick(DigitalNode core, ServerPlayer actor) {
        if (!core.kind().equals("quantum_link")) return;
        var frame = frame(core, actor); var world = actor.level(); boolean formed = !frame.isEmpty();
        var state = core.getBlockState().setValue(FORMED, formed);
        for (var entry : SIDES.entrySet()) state = state.setValue(entry.getValue(), formed && frame.containsKey(core.getBlockPos().relative(entry.getKey())));
        if (!state.equals(core.getBlockState())) world.setBlock(core.getBlockPos(), state, Block.UPDATE_CLIENTS);
        if (!formed) { clearFrame(core); return; }
        for (var entry : frame.entrySet()) {
            var pos = entry.getKey(); var old = world.getBlockState(pos); var next = old.setValue(FORMED, true).setValue(CORNER, entry.getValue()).setValue(DigitalNodeBlock.LIT, core.getBlockState().getValue(DigitalNodeBlock.LIT) && core.peer != null);
            for (var side : SIDES.entrySet()) next = next.setValue(side.getValue(), frame.containsKey(pos.relative(side.getKey())) || pos.relative(side.getKey()).equals(core.getBlockPos()));
            if (!next.equals(old)) world.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }
    static void clearFrame(DigitalNode core) {
        var world = core.getLevel(); if (world == null || world.isClientSide()) return;
        for (var p : BlockPos.betweenClosed(core.getBlockPos().offset(-1, -1, -1), core.getBlockPos().offset(1, 1, 1))) {
            if (!world.isLoaded(p)) continue; var s = world.getBlockState(p);
            if (s.getBlock() instanceof Ring && s.getValue(FORMED)) {
                var next = s.setValue(FORMED, false).setValue(DigitalNodeBlock.LIT, false); for (var side : SIDES.values()) next = next.setValue(side, false);
                world.setBlock(p, next, Block.UPDATE_CLIENTS);
            }
        }
    }
    static boolean open(DigitalNode node, ServerPlayer player) {
        if (!node.kind().equals("quantum_link")) return false;
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && !node.getItem(0).isEmpty()) {
            var key = node.removeItem(0, 1); key.remove(DigitalLinks.LINK); node.peer = null; node.setChanged(); player.getInventory().placeItemBackInInventory(key, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        player.sendOverlayMessage(Component.literal("3×3 frame: eight Quantum Rings around this core. Use two entangled singularities, one on each core. Both networks need power and loaded chunks.")); return true;
    }
    public static final class Key extends Item {
        Key(Properties p) { super(p); }
        @Override public InteractionResult useOn(UseOnContext c) {
            if (!(c.getPlayer() instanceof ServerPlayer player) || !(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof DigitalNode core) || !core.kind().equals("quantum_link") || !core.permitted(player)) return InteractionResult.PASS;
            if (!core.getItem(0).isEmpty() || frame(core, player).isEmpty()) { player.sendOverlayMessage(Component.literal("Build the eight-ring frame; the core must have an empty singularity slot.")); return InteractionResult.FAIL; }
            var held = c.getItemInHand(); var source = held.get(DigitalLinks.LINK);
            if (source == null) {
                if (held.getCount() < 2) { player.sendOverlayMessage(Component.literal("Hold both entangled singularities when selecting the first core.")); return InteractionResult.FAIL; }
                core.setItem(0, held.split(1)); held.set(DigitalLinks.LINK, DigitalLinks.identity(core));
                player.sendOverlayMessage(Component.literal("First core selected. Use the remaining singularity on the second loaded, permitted core.")); return InteractionResult.SUCCESS;
            }
            var world = player.level().getServer().getLevel(source.position().dimension());
            if (world == null || !world.isLoaded(source.position().pos()) || !(world.getBlockEntity(source.position().pos()) instanceof DigitalNode first) || first == core || !first.kind().equals("quantum_link") || !first.linkIdentity.equals(source.identity()) || first.peer != null || first.getItem(0).isEmpty()) return InteractionResult.FAIL;
            var actor = world == player.level() ? player : first.actor();
            if (actor == null || !(actor.getUUID().equals(player.getUUID()) || JugcraftParties.sameParty(actor.getUUID(), player.getUUID())) || !first.permitted(actor) || frame(first, actor).isEmpty()) {
                player.sendOverlayMessage(Component.literal("The first core needs its permitted owner present there (or the server's absent-owner option).")); return InteractionResult.FAIL;
            }
            core.setItem(0, held.split(1)); first.peer = DigitalLinks.identity(core); core.peer = DigitalLinks.identity(first); first.setChanged(); core.setChanged();
            player.sendOverlayMessage(Component.literal("Quantum storage bridge paired. Shared items and fluids; each side keeps its own crafting and power.")); return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level, Player p, InteractionHand hand) { if (p.isShiftKeyDown()) { p.getItemInHand(hand).remove(DigitalLinks.LINK); return InteractionResult.SUCCESS; } return InteractionResult.PASS; }
    }
}
