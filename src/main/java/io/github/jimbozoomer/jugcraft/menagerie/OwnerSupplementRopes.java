package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.phys.AABB;
import java.util.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Supplied connected rope geometry, climbing and bounded manual extension/retrieval. */
public final class OwnerSupplementRopes {
    private OwnerSupplementRopes(){}
    public static boolean rope(BlockState state){return state.is(OwnerSupplementaries.BLOCKS.get("rope"))||state.is(OwnerSupplementaries.BLOCKS.get("rope_buntings"));}
    private static boolean connects(LevelReader level,BlockPos pos,Direction direction){
        var target=pos.relative(direction);if(!level.hasChunkAt(target))return false;var other=level.getBlockState(target);
        if(rope(other))return direction!=Direction.UP||HouseholdBlock.flag(other,"down");
        return other.isFaceSturdy(level,target,direction.getOpposite())||other.getBlock() instanceof FenceBlock||other.getBlock() instanceof WallBlock||other.getBlock() instanceof IronBarsBlock||other.is(Blocks.IRON_CHAIN);
    }
    static BlockState connections(BlockState state,LevelReader level,BlockPos pos){
        int sides=0;
        for(var direction:Direction.Plane.HORIZONTAL){boolean attached=connects(level,pos,direction);if(attached)sides++;state=HouseholdBlock.set(state,direction.getName(),Boolean.toString(attached));}
        boolean top=connects(level,pos,Direction.UP);
        state=HouseholdBlock.set(state,"up",Boolean.toString(top));
        // A sheared loose end stays short until rope is explicitly attached again.
        boolean down=level.hasChunkAt(pos.below())&&rope(level.getBlockState(pos.below()))||!HouseholdBlock.flag(state,"knot")&&(sides==0||top);
        return HouseholdBlock.set(state,"down",Boolean.toString(down));
    }
    static boolean supported(LevelReader level,BlockPos pos){
        // Follow upward/horizontal rope edges to a real attachment, not a circular pair of loose ropes.
        var pending=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();pending.add(pos);
        while(!pending.isEmpty()){
            var current=pending.removeFirst();if(!visited.add(current))continue;
            if(visited.size()>128)return true; // Unresolved long networks remain intact.
            for(var direction:new Direction[]{Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
                var target=current.relative(direction);if(!level.hasChunkAt(target))return true;
                var state=level.getBlockState(target);
                if(rope(state)){if(direction!=Direction.UP||HouseholdBlock.flag(state,"down"))pending.add(target);}
                else if(connects(level,current,direction))return true;
            }
        }
        return false;
    }
    static InteractionResult tie(Level level,BlockPos pos,Player player){
        if(player.isShiftKeyDown()||!Menagerie.mayChange(level,player,pos))return InteractionResult.PASS;
        var nearby=new ArrayList<Entity>();
        level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Entity.class),new AABB(pos).inflate(7),
                e->e instanceof Leashable leash&&leash.getLeashHolder()==player,nearby,16);
        nearby.removeIf(e->!Menagerie.mayChange(level,player,e.blockPosition()));
        if(nearby.isEmpty())return InteractionResult.PASS;
        if(level instanceof ServerLevel){
            var knot=LeashFenceKnotEntity.getOrCreateKnot(level,pos);
            for(var entity:nearby)((Leashable)entity).setLeashedTo(knot,true);
            knot.playPlacementSound();
        }
        return InteractionResult.SUCCESS;
    }
    static InteractionResult extend(BlockState state,Level level,BlockPos pos,Player player,ItemStack held,InteractionHand hand){
        if(held.is(Items.LEAD)){var tied=tie(level,pos,player);if(tied!=InteractionResult.PASS)return tied;}
        if(held.getItem() instanceof ShearsItem&&HouseholdBlock.flag(state,"down")){
            if(!level.isClientSide()){level.setBlockAndUpdate(pos,HouseholdBlock.set(HouseholdBlock.set(state,"down","false"),"knot","true"));held.hurtAndBreak(1,player,hand.asEquipmentSlot());}
            return InteractionResult.SUCCESS;
        }
        if(!held.is(OwnerSupplementaries.BLOCKS.get("rope").asItem()))return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockPos end=pos.below();int length=0;
        while(level.hasChunkAt(end)&&rope(level.getBlockState(end))&&length++<128)end=end.below();
        if(length>=128||!level.hasChunkAt(end)||end.getY()<level.getMinY()||!Menagerie.mayChange(level,player,end))return InteractionResult.FAIL;
        var old=level.getBlockState(end);if(!old.isAir()&&!old.is(Blocks.WATER))return InteractionResult.FAIL;
        if(!level.isClientSide()){
            var next=HouseholdBlock.set(OwnerSupplementaries.BLOCKS.get("rope").defaultBlockState(),"waterlogged",Boolean.toString(level.getFluidState(end).is(FluidTags.WATER)));
            if(!level.setBlockAndUpdate(end,connections(next,level,end)))return InteractionResult.FAIL;held.consume(1,player);
            level.setBlockAndUpdate(pos,HouseholdBlock.set(state,"down","true"));
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,.8F);
        }
        return InteractionResult.SUCCESS;
    }
    static InteractionResult retrieve(Level level,BlockPos pos,Player player){
        var tied=tie(level,pos,player);if(tied!=InteractionResult.PASS)return tied;
        BlockPos end=pos;int length=0;
        while(level.hasChunkAt(end.below())&&rope(level.getBlockState(end.below()))&&length++<128)end=end.below();
        if(length==0||length>=128||!level.hasChunkAt(end.below())||!Menagerie.mayChange(level,player,end))return InteractionResult.PASS;
        if(!level.isClientSide()){
            var state=level.getBlockState(end);
            if(!level.setBlockAndUpdate(end,HouseholdBlock.flag(state,"waterlogged")?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState()))return InteractionResult.FAIL;
            if(!player.isCreative())player.getInventory().placeItemBackInInventory(new ItemStack(OwnerSupplementaries.BLOCKS.get("rope")),net.minecraft.util.Prediction.SERVER_ONLY);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,.6F);
        }
        return InteractionResult.SUCCESS;
    }
    static VoxelShape collision(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context,VoxelShape outline){
        var surface=Block.box(0,0,0,16,13,16);
        if(!HouseholdBlock.flag(state,"up")&&(context.isAbove(surface,pos,true)||!HouseholdBlock.flag(state,"down")))return outline;
        return Shapes.empty();
    }
}
