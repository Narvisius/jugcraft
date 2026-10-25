package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Explicit same-dimension binding; the handset never loads a distant network's chunks. */
public final class WirelessTerminal extends Item {
    public static DataComponentType<GlobalPos> LINK;
    public static DataComponentType<Integer> FACE;
    private final boolean crafting;
    public WirelessTerminal(Properties p, boolean crafting) { super(p); this.crafting = crafting; }
    public static void register() {
        LINK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_wireless_link"), DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(GlobalPos.CODEC)).build());
        FACE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("jimta_wireless_face"),DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.intRange(-1,5)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var node=DigitalParts.pointed(context);
        if (context.getPlayer() instanceof ServerPlayer player && node!=null && node.permitted(player)) {
            context.getItemInHand().set(LINK, GlobalPos.of(player.level().dimension(), context.getClickedPos()));
            context.getItemInHand().set(FACE,node.host==null?-1:node.getBlockState().getValue(DigitalNodeBlock.FACING).ordinal());
            player.sendOverlayMessage(Component.literal("Wireless terminal linked. Base range: 64 blocks; connected access points extend coverage. Keep the network loaded and powered.")); return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public static boolean held(ServerPlayer p, DigitalNode node) {
        if (node.isRemoved() || !p.level().isLoaded(node.getBlockPos()) || !node.permitted(p)) return false;
        boolean covered = p.distanceToSqr(Vec3.atCenterOf(node.getBlockPos())) <= 64 * 64;
        if (!covered) {
            var network = DigitalNetwork.find(node, p);
            covered = network.online() && network.nodes().stream().anyMatch(n -> n.kind().equals("wireless_access_point")
                    && !p.level().hasNeighborSignal(n.getBlockPos()) && p.distanceToSqr(Vec3.atCenterOf(n.getBlockPos())) <= range(n) * range(n));
        }
        if (!covered) return false;
        var target = GlobalPos.of(p.level().dimension(), node.getBlockPos());
        for (var hand : InteractionHand.values()) { var stack = p.getItemInHand(hand); if (stack.getItem() instanceof WirelessTerminal && target.equals(stack.get(LINK))&&resolve(p.level(),target.pos(),stack)==node) return true; }
        return false;
    }
    public static int range(DigitalNode node) { return 64 + (node.getItem(0).is(DigitalStorage.ITEMS.get("wireless_booster")) ? Math.min(64, node.getItem(0).getCount()) * 2 : 0); }
    private static DigitalNode resolve(Level level,BlockPos pos,ItemStack stack){
        if(!level.isLoaded(pos))return null;var block=level.getBlockEntity(pos);int face=stack.getOrDefault(FACE,-1);
        if(face==-1)return block instanceof DigitalNode node?node:null;
        return face>=0&&face<6&&block instanceof DigitalFacades.Covers covers?covers.parts.get(Direction.values()[face]):null;
    }
    @Override public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            var link = player.getItemInHand(hand).get(LINK);
            var node=link!=null&&link.dimension().equals(world.dimension())?resolve(world,link.pos(),player.getItemInHand(hand)):null;
            if (node!=null && held(server, node)) {
                server.openMenu(new SimpleMenuProvider((id, inv, p) -> new DigitalTerminal.Menu(id, inv, node, true, crafting), Component.literal("Wireless ME Storage")));
            } else server.sendOverlayMessage(Component.literal("Link this to your loaded network, then stay within 64 blocks or a connected access point's coverage."));
        }
        return InteractionResult.SUCCESS;
    }
}
