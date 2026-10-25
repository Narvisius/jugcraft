package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Six bounded cable-face nodes. Each attachment owns real independent slots, filters and channel usage. */
public final class DigitalParts {
    private static final Set<String> SUPPORTED=Set.of("import_bus","export_bus","storage_bus","fluid_import_bus","fluid_export_bus","fluid_storage_bus",
            "terminal","fluid_terminal","crafting_terminal","pattern_encoding_terminal","pattern_access_terminal","storage_monitor","conversion_monitor","monitor","semi_dark_monitor","dark_monitor",
            "cable_interface","cable_energy_acceptor","cable_pattern_provider","formation_plane","annihilation_plane",
            "level_emitter","energy_level_emitter","me_p2p_tunnel","item_p2p_tunnel","fluid_p2p_tunnel","fe_p2p_tunnel","redstone_p2p_tunnel","light_p2p_tunnel",
            "quartz_fiber","toggle_bus","inverted_toggle_bus");
    private final DigitalFacades.Covers host;
    private final DigitalNode[] parts=new DigitalNode[6];
    DigitalParts(DigitalFacades.Covers host){this.host=host;}
    static void registerPorts(){
        io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlockEntity((host,side)->{
            var node=side==null?null:host.parts.get(side);if(node==null||!node.poweredSource())return null;
            return new io.github.jimbozoomer.jugcraft.energy.EnergyStorage(){
                private boolean live(){return !host.isRemoved()&&host.parts.contains(node)&&host.getLevel()!=null&&host.getLevel().getBlockEntity(host.getBlockPos())==host;}
                public boolean supportsInsertion(){return live();}public boolean supportsExtraction(){return live();}
                public long insert(long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return live()?node.energy.insert(amount,tx):0;}
                public long extract(long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return live()?node.energy.extract(amount,tx):0;}
                public long getAmount(){return live()?node.energy.getAmount():0;}public long getCapacity(){return node.energy.getCapacity();}
                public Object transferIdentity(){return node.energy;}
            };
        },DigitalFacades.TYPE);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((host,side)->{
            var node=side==null?null:host.parts.get(side);return node!=null&&node.kind().equals("interface")?new FluidCells.Port(node,null):null;
        },DigitalFacades.TYPE);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((host,side)->{
            var node=side==null?null:host.parts.get(side);if(node==null||!node.kind().equals("interface"))return null;
            var buffer=net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(node,null);
            return new net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>(){
                public long insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item,long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){var actor=node.actor();return actor!=null&&node.permitted(actor)?buffer.insert(item,amount,tx):0;}
                public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item,long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return 0;}
                public boolean supportsExtraction(){return false;}
                public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>> iterator(){return java.util.Collections.emptyIterator();}
            };
        },DigitalFacades.TYPE);
    }
    static boolean supported(String name){return SUPPORTED.contains(name);}
    private static boolean supported(BlockState state){return SUPPORTED.stream().anyMatch(name->DigitalStorage.BLOCKS.get(name)==state.getBlock());}
    public DigitalNode get(Direction face){return parts[face.ordinal()];}
    static DigitalNode pointed(UseOnContext context){
        var be=context.getLevel().getBlockEntity(context.getClickedPos());if(be instanceof DigitalNode n)return n;
        if(be instanceof DigitalFacades.Covers covers){var face=covers.parts.selected(new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()));return face==null?null:covers.parts.get(face);}return null;
    }
    public List<DigitalNode> nodes(){return Arrays.stream(parts).filter(Objects::nonNull).toList();}
    public boolean contains(DigitalNode node){return Arrays.stream(parts).anyMatch(part->part==node);}
    static List<DigitalNode> at(net.minecraft.world.level.Level level,BlockPos pos){
        if(!level.isLoaded(pos))return List.of();var block=level.getBlockEntity(pos);
        return block instanceof DigitalNode node?List.of(node):block instanceof DigitalFacades.Covers cable?cable.parts.nodes():List.of();
    }
    public void levelChanged(){if(host.getLevel()!=null)for(var node:nodes())node.setLevel(host.getLevel());}
    public void tick(){for(var node:nodes())node.tick();}
    public void removed(){for(var node:nodes())node.setRemoved();}
    public void revived(){for(var node:nodes())node.clearRemoved();}
    public boolean install(Direction face,BlockState state,ItemStack stack,ServerPlayer player){
        if(!supported(state)||stack.isEmpty()||!stack.is(state.getBlock().asItem())||host.isRemoved()||host.getLevel()!=player.level()||player.level().getBlockEntity(host.getBlockPos())!=host||!Authority.mayChange(player.level(),player,host.getBlockPos())||player.isSpectator()||!player.isAlive())return false;
        if(get(face)!=null||host.anchored(face)||!host.material(face).isAir()||host.getBlockState().getValue(DigitalCableBlock.PROPERTY_BY_DIRECTION.get(face))){
            player.sendOverlayMessage(Component.literal("Choose an uncovered cable face without a cable connection or attachment."));return false;
        }
        if(nodes().stream().anyMatch(n->!n.permitted(player)))return false;
        var node=new DigitalNode(host.getBlockPos(),state.setValue(DigitalNodeBlock.FACING,face));node.host=host;node.setLevel(player.level());node.owner(player.getUUID());
        node.energy.setAmount(stack.getOrDefault(DigitalWorkshops.ENERGY,0L));parts[face.ordinal()]=node;
        stack.consume(1,player);DigitalCableBlock.refresh(host);connectionsChanged();host.changed();return true;
    }
    public Direction selected(BlockHitResult hit){
        var local=hit.getLocation().subtract(host.getBlockPos().getX(),host.getBlockPos().getY(),host.getBlockPos().getZ());
        // Hitting the top edge of a north-facing plate must not select an unrelated UP attachment.
        Direction selected=null;double nearest=.34;
        for(var face:Direction.values())if(get(face)!=null&&DigitalFacades.attachment(get(face),face).bounds().inflate(.001).contains(local)){double distance=switch(face){case DOWN->local.y;case UP->1-local.y;case NORTH->local.z;case SOUTH->1-local.z;case WEST->local.x;case EAST->1-local.x;};
            if(distance>=-.001&&distance<nearest){nearest=distance;selected=face;}}
        return selected;
    }
    public boolean interact(Player player,InteractionHand hand,BlockHitResult hit){
        Direction face=selected(hit);if(face==null)return false;
        if(player instanceof ServerPlayer p){
            var node=get(face);if(!node.permitted(p))return true;var stack=p.getItemInHand(hand);
            if(DigitalDisplays.monitor(node.kind()))DigitalDisplays.interact(node,p,hand);
            else if(p.isShiftKeyDown()&&(node.kind().endsWith("_bus")||node.kind().endsWith("_plane"))){
                if(!stack.isEmpty()&&node.kind().startsWith("fluid_")&&DigitalFluidAutomation.filter(stack).isBlank())p.sendOverlayMessage(Component.literal("Choose a filled bucket; sneak-use an empty hand to clear."));
                else{node.filter(stack);p.sendOverlayMessage(stack.isEmpty()?Component.literal("Transfer filter cleared."):Component.literal("Transfer filter: ").append(stack.getHoverName()));}
            }else if(!node.kind().equals("interface")||!net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil.interactWithFluidStorage(new FluidCells.Port(node,p),p,hand))node.open(p);
        }return true;
    }
    boolean detach(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer p)||!p.isShiftKeyDown())return false;
        var hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
        var face=selected(hit);var node=face==null?null:get(face);if(node==null||!node.permitted(p))return false;
        parts[face.ordinal()]=null;var item=new ItemStack(node.getBlockState().getBlock());
        if(node.poweredSource())item.set(DigitalWorkshops.ENERGY,node.energy.getAmount());
        node.preRemoveSideEffects(host.getBlockPos(),node.getBlockState());node.setRemoved();
        p.getInventory().placeItemBackInInventory(item,net.minecraft.util.Prediction.SERVER_ONLY);DigitalCableBlock.refresh(host);connectionsChanged();host.changed();return true;
    }
    private void connectionsChanged(){
        if(host.getLevel()==null||host.getLevel().isClientSide())return;
        host.getBlockState().updateNeighbourShapes(host.getLevel(),host.getBlockPos(),Block.UPDATE_ALL);
        host.getLevel().updateNeighborsAt(host.getBlockPos(),host.getBlockState().getBlock());
        io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.invalidate(host.getLevel());
        io.github.jimbozoomer.jugcraft.logistics.ItemNetworks.invalidate(host.getLevel());
        io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.invalidate(host.getLevel());
    }
    void dropAll(){
        for(var face:Direction.values()){var node=get(face);if(node==null)continue;parts[face.ordinal()]=null;
            var item=new ItemStack(node.getBlockState().getBlock());if(node.poweredSource())item.set(DigitalWorkshops.ENERGY,node.energy.getAmount());
            node.preRemoveSideEffects(host.getBlockPos(),node.getBlockState());node.setRemoved();Block.popResource(host.getLevel(),host.getBlockPos(),item);
        }
    }
    void save(ValueOutput out,boolean publicOnly){
        for(var face:Direction.values()){var node=get(face);if(node==null)continue;out.store("part_state_"+face.ordinal(),BlockState.CODEC,node.getBlockState());
            if(host.getLevel()!=null)out.store("part_data_"+face.ordinal(),CompoundTag.CODEC,publicOnly?node.getUpdateTag(host.getLevel().registryAccess()):node.saveWithoutMetadata(host.getLevel().registryAccess()));}
    }
    void load(ValueInput in){
        removed();Arrays.fill(parts,null);
        for(var face:Direction.values()){
            var state=in.read("part_state_"+face.ordinal(),BlockState.CODEC).orElse(null);if(state==null||!supported(state))continue;
            var node=new DigitalNode(host.getBlockPos(),state.setValue(DigitalNodeBlock.FACING,face));node.host=host;
            if(host.getLevel()!=null)node.setLevel(host.getLevel());
            // Child input retains the world's registry lookup even when the host has not been attached to a level yet.
            // Public child payloads are themselves render_only and never include inventory or identity.
            in.child("part_data_"+face.ordinal()).ifPresent(node::loadAdditional);
            parts[face.ordinal()]=node;
        }
    }
    public static final class PartItem extends BlockItem {
        public PartItem(Block block,Properties properties){super(block,properties);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            super.appendHoverText(stack,context,display,out,flag);out.accept(Component.literal("Attach to a free ME cable face, or place separately."));out.accept(Component.literal("Sneak-wrench removes just the selected attachment."));
        }
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DigitalFacades.Covers host))return super.useOn(context);
            if(context.getLevel().isClientSide())return InteractionResult.SUCCESS;
            return context.getPlayer() instanceof ServerPlayer p&&host.parts.install(context.getClickedFace(),getBlock().defaultBlockState(),context.getItemInHand(),p)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
    }
}
