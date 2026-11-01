package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Supplied ceiling/wall pennants and four separately paid rope decorations. */
public final class OwnerBuntings {
    private OwnerBuntings(){}
    public static final class Placed extends HouseholdBlock {
        Placed(Properties p,JsonObject d){super(p,d);}
        private boolean wall(){return name.startsWith("bunting_wall_");}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){
            if(!wall())return set(defaultBlockState(),"axis",c.getHorizontalDirection().getAxis().getName());
            for(var dir:c.getNearestLookingDirections())if(dir.getAxis().isHorizontal()){
                var state=set(defaultBlockState(),"facing",dir.getOpposite().getName());if(state.canSurvive(c.getLevel(),c.getClickedPos()))return state;
            }return null;
        }
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var dir=wall()?facing(s).getOpposite():Direction.UP;var at=p.relative(dir);return !l.hasChunkAt(at)||l.getBlockState(at).isFaceSturdy(l,at,dir.getOpposite());}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,RandomSource r){return s.canSurvive(l,p)?s:Blocks.AIR.defaultBlockState();}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
            if(!wall())return value(s,"axis").equals("z")?Block.box(0,0,7,16,16,9):Block.box(7,0,0,9,16,16);
            return switch(facing(s)){case SOUTH->Block.box(0,0,0,16,16,1);case EAST->Block.box(0,0,0,1,16,16);case WEST->Block.box(15,0,0,16,16,16);default->Block.box(0,0,15,16,16,16);};
        }
        @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    }
    public static final class Pennant extends StandingAndWallBlockItem {
        public final DyeColor color;
        public Pennant(Block ceiling,Block wall,DyeColor color,Properties p){super(ceiling,wall,Direction.UP,p);this.color=color;}
        @Override public InteractionResult useOn(UseOnContext c){
            var state=c.getLevel().getBlockState(c.getClickedPos());
            if(OwnerSupplementRopes.rope(state)&&c.getPlayer()!=null){
                var hit=new BlockHitResult(c.getClickLocation(),c.getClickedFace(),c.getClickedPos(),false);
                return put(c.getLevel(),c.getClickedPos(),c.getPlayer(),c.getItemInHand(),hit);
            }return super.useOn(c);
        }
    }
    private static Direction side(BlockPos pos,BlockHitResult hit){
        double x=hit.getLocation().x-pos.getX()-.5,z=hit.getLocation().z-pos.getZ()-.5;
        return Math.abs(x)>Math.abs(z)?(x<0?Direction.WEST:Direction.EAST):(z<0?Direction.NORTH:Direction.SOUTH);
    }
    private static BlockState copyRope(BlockState from,BlockState to){for(var prop:from.getProperties())to=HouseholdBlock.set(to,prop.getName(),HouseholdBlock.value(from,prop.getName()));return to;}
    public static InteractionResult put(Level level,BlockPos pos,Player player,ItemStack held,BlockHitResult hit){
        if(!(held.getItem() instanceof Pennant)||!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
        var state=level.getBlockState(pos);Direction dir=side(pos,hit);if(!HouseholdBlock.flag(state,dir.getName()))return InteractionResult.FAIL;
        int slot=dir.get2DDataValue();
        if(level.getBlockEntity(pos) instanceof OwnerSupplementData data&&!data.getItem(slot).isEmpty())return InteractionResult.FAIL;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(!state.is(OwnerSupplementaries.BLOCKS.get("rope_buntings"))){
            if(!level.setBlockAndUpdate(pos,copyRope(state,OwnerSupplementaries.BLOCKS.get("rope_buntings").defaultBlockState())))return InteractionResult.FAIL;
        }
        if(!(level.getBlockEntity(pos) instanceof OwnerSupplementData data)){level.setBlockAndUpdate(pos,state);return InteractionResult.FAIL;}
        data.setItem(slot,held.copyWithCount(1));data.owner=player.getUUID();held.consume(1,player);data.changed();return InteractionResult.SUCCESS;
    }
    static InteractionResult take(OwnerSupplementData data,Player player,BlockHitResult hit){
        var dir=side(data.getBlockPos(),hit);int slot=dir.get2DDataValue();
        if(!data.getItem(slot).isEmpty()){player.getInventory().placeItemBackInInventory(data.removeItemNoUpdate(slot),net.minecraft.util.Prediction.SERVER_ONLY);data.changed();}
        if(data.isEmpty())data.getLevel().setBlockAndUpdate(data.getBlockPos(),copyRope(data.getBlockState(),OwnerSupplementaries.BLOCKS.get("rope").defaultBlockState()));
        return InteractionResult.SUCCESS;
    }
    static void tick(ServerLevel level,OwnerSupplementData data){
        if(level.getGameTime()%10!=0)return;
        var pos=data.getBlockPos();var state=data.getBlockState();
        if(!OwnerSupplementRopes.supported(level,pos)){level.destroyBlock(pos,true);return;}
        var next=OwnerSupplementRopes.connections(state,level,pos);
        if(next!=state)level.setBlockAndUpdate(pos,next);
        for(var dir:Direction.Plane.HORIZONTAL)if(!HouseholdBlock.flag(next,dir.getName())&&!data.getItem(dir.get2DDataValue()).isEmpty()){
            Block.popResource(level,pos,data.removeItemNoUpdate(dir.get2DDataValue()));data.changed();
        }
        if(data.isEmpty())level.setBlockAndUpdate(pos,copyRope(next,OwnerSupplementaries.BLOCKS.get("rope").defaultBlockState()));
    }
}
