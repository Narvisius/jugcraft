package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.mixin.OwnerMovingPistonAccess;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;

/** Bounded local assembly transactions with native piston collision/animation and carried inventories. */
public final class OwnerAssemblyMotion {
    public static BlockEntityType<Node> TYPE;
    private OwnerAssemblyMotion(){}
    public static void register(){TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("supps_moving_assembly"),FabricBlockEntityTypeBuilder.create(Node::new,OwnerSupplementaries.BLOCKS.get("moving_pulley_block")).build());}
    static boolean allowed(ServerLevel l,ServerPlayer p,BlockPos at){return l.hasChunkAt(at)&&l.isInWorldBounds(at)&&l.getWorldBorder().isWithinBounds(at)&&!io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation.blocked(l,at)&&Authority.mayChange(l,p,at);}
    static boolean inventory(BlockState s){return s.is(Blocks.BARREL)||s.is(Blocks.HOPPER)||s.is(Blocks.FURNACE)||s.is(Blocks.SMOKER)||s.is(Blocks.BLAST_FURNACE)||s.is(Blocks.CHEST)&&s.getValue(ChestBlock.TYPE)==ChestType.SINGLE||s.is(Blocks.TRAPPED_CHEST)&&s.getValue(ChestBlock.TYPE)==ChestType.SINGLE;}
    static boolean movable(ServerLevel l,BlockPos at){var s=l.getBlockState(at);if(s.isAir())return true;if(s.is(OwnerSupplementaries.BLOCKS.get("pulley_block"))||s.getBlock() instanceof MovingPistonBlock||s.getBlock() instanceof PistonHeadBlock||s.getDestroySpeed(l,at)<0||!s.getFluidState().isEmpty())return false;
        if(s.hasProperty(BlockStateProperties.EXTENDED)&&s.getValue(BlockStateProperties.EXTENDED)||s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)||s.hasProperty(BlockStateProperties.BED_PART))return false;
        for(var prop:s.getProperties())if(Set.of("part","section","segment").contains(prop.getName()))return false;
        if(s.hasBlockEntity())return inventory(s)&&l.getBlockEntity(at) instanceof BaseContainerBlockEntity||l.getBlockEntity(at) instanceof OwnerSupplementData d&&d.block().function.equals("slidy");
        return s.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.PUSH_PULL||s.getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.PUSH;
    }
    record Saved(BlockState state,CompoundTag data){}
    static Saved capture(ServerLevel l,BlockPos p){var node=l.getBlockEntity(p);return new Saved(l.getBlockState(p),node==null?null:node.saveWithFullMetadata(l.registryAccess()));}
    static void detach(ServerLevel l,Collection<BlockPos> positions){
        var containers=Collections.newSetFromMap(new IdentityHashMap<Container,Boolean>());for(var at:positions)if(l.getBlockEntity(at) instanceof Container c)containers.add(c);
        for(var player:l.players())if(player.containerMenu.slots.stream().anyMatch(slot->containers.contains(slot.container)))player.closeContainer();
        for(var at:positions){var node=l.getBlockEntity(at);if(node instanceof RandomizableContainerBlockEntity random)random.setLootTable(null);if(node instanceof Container c)c.clearContent();l.removeBlockEntity(at);}
    }
    static void restore(ServerLevel l,BlockPos pos,Saved saved){l.setBlock(pos,saved.state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);if(saved.data!=null){var node=BlockEntity.loadStatic(pos,saved.state,saved.data,l.registryAccess());if(node==null)throw new IllegalStateException("Cannot restore carried block at "+pos);l.setBlockEntity(node);node.setChanged();}}
    /** Rope add/remove edits share the same rollback as the carried blocks. One chunk is one save unit. */
    static boolean move(ServerLevel l,ServerPlayer actor,Collection<BlockPos> sources,Direction direction,int ticks,Map<BlockPos,BlockState> edits,UUID slideOwner){
        var src=new LinkedHashSet<>(sources);if(src.size()>128)return false;var all=new LinkedHashSet<BlockPos>(edits.keySet());all.addAll(src);for(var p:src)all.add(p.relative(direction));if(all.isEmpty())return true;
        var chunk=ChunkPos.containing(all.iterator().next());
        for(var p:all)if(!ChunkPos.containing(p).equals(chunk)||!allowed(l,actor,p))return false;
        for(var p:src){if(!movable(l,p))return false;var to=p.relative(direction);if(!src.contains(to)&&!edits.containsKey(to)&&!l.getBlockState(to).isAir())return false;}
        var before=new LinkedHashMap<BlockPos,Saved>();for(var p:all)before.put(p,capture(l,p));
        var moving=new LinkedHashMap<BlockPos,Node>();var block=OwnerSupplementaries.BLOCKS.get("moving_pulley_block");
        for(var p:src){var saved=before.get(p);var target=p.relative(direction);var state=block.defaultBlockState().setValue(MovingPistonBlock.FACING,direction).setValue(MovingPistonBlock.TYPE,PistonType.DEFAULT);var node=new Node(target,state,saved.state,direction);node.carried=saved.data;node.duration=Math.clamp(ticks,2,100);node.slideOwner=slideOwner;moving.put(target,node);}
        try{
            detach(l,all);
            int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_MOVE_BY_PISTON;
            for(var p:all)if(!l.getBlockState(p).isAir()&&!l.setBlock(p,Blocks.AIR.defaultBlockState(),flags))throw new IllegalStateException("Assembly source clear failed");
            for(var entry:edits.entrySet())if(!entry.getValue().isAir()&&!l.setBlock(entry.getKey(),entry.getValue(),flags))throw new IllegalStateException("Rope placement failed");
            for(var entry:moving.entrySet()){var node=entry.getValue();if(!l.setBlock(entry.getKey(),node.getBlockState(),flags))throw new IllegalStateException("Assembly target placement failed");l.setBlockEntity(node);node.setChanged();}
        }catch(RuntimeException failure){for(var p:all)l.removeBlockEntity(p);for(var entry:before.entrySet())restore(l,entry.getKey(),entry.getValue());for(var p:all)l.updateNeighborsAt(p,l.getBlockState(p).getBlock());return false;}
        for(var p:all)l.updateNeighborsAt(p,l.getBlockState(p).getBlock());return true;
    }
    public static final class Moving extends MovingPistonBlock {
        Moving(Properties p){super(p.strength(-1,3600000).noLootTable().noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Node(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return type==OwnerAssemblyMotion.TYPE?(world,pos,state,node)->Node.tick(world,pos,(Node)node):null;}
    }
    public static final class Node extends PistonMovingBlockEntity {
        CompoundTag carried;int duration=4;UUID slideOwner;boolean finished;
        Node(BlockPos pos,BlockState state){super(pos,state);}
        Node(BlockPos pos,BlockState state,BlockState moved,Direction dir){super(pos,state,moved,dir,true,false);}
        @Override public BlockEntityType<?> getType(){return TYPE;}
        @Override public boolean isValidBlockState(BlockState state){return TYPE!=null&&TYPE.isValid(state);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("Duration",duration);if(carried!=null)out.store("Carried",CompoundTag.CODEC,carried);if(slideOwner!=null)out.store("SlideOwner",UUIDUtil.CODEC,slideOwner);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);duration=Math.clamp(in.getIntOr("Duration",4),2,100);carried=in.read("Carried",CompoundTag.CODEC).orElse(null);slideOwner=in.read("SlideOwner",UUIDUtil.CODEC).orElse(null);}
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){var tag=super.getUpdateTag(lookup);tag.remove("Carried");tag.remove("SlideOwner");return tag;}
        @Override public void finalTick(){finish();}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState state){/* This internal, unbreakable carrier is completed by its own ticker. */}
        static void tick(Level level,BlockPos pos,Node node){var access=(OwnerMovingPistonAccess)(Object)node;float old=access.jugcraft$progress();access.jugcraft$previous(old);access.jugcraft$lastTick(level.getGameTime());if(old>=1){if(!level.isClientSide())node.finish();return;}float next=Math.min(1,old+1F/node.duration);OwnerMovingPistonAccess.jugcraft$push(level,pos,next,node);OwnerMovingPistonAccess.jugcraft$carry(level,pos,next,node);access.jugcraft$progress(next);node.setChanged();}
        private void finish(){if(finished||!(level instanceof ServerLevel server)||!server.getBlockState(worldPosition).is(OwnerSupplementaries.BLOCKS.get("moving_pulley_block")))return;
            var restored=carried==null?null:BlockEntity.loadStatic(worldPosition,getMovedState(),carried,server.registryAccess());if(carried!=null&&restored==null)throw new IllegalStateException("Invalid carried assembly inventory at "+worldPosition);
            finished=true;
            if(!server.setBlock(worldPosition,getMovedState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_MOVE_BY_PISTON)){finished=false;clearRemoved();server.setBlockEntity(this);setChanged();return;}
            setRemoved();
            if(restored!=null){server.setBlockEntity(restored);restored.setChanged();}carried=null;server.updateNeighborsAt(worldPosition,getMovedState().getBlock());server.getBlockState(worldPosition).updateNeighbourShapes(server,worldPosition,Block.UPDATE_ALL);
            if(slideOwner!=null&&getMovedState().is(OwnerSupplementaries.BLOCKS.get("slidy_block")))OwnerSlidingBlocks.continueSlide(server,worldPosition,getDirection(),slideOwner);
        }
    }
}
