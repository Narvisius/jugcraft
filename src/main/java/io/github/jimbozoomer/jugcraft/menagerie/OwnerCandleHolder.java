package io.github.jimbozoomer.jugcraft.menagerie;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** All supplied colors and floor/wall/ceiling meshes, retaining native candle interaction. */
final class OwnerCandleHolder extends CandleBlock {
    static final EnumProperty<AttachFace> FACE=BlockStateProperties.ATTACH_FACE;
    static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final Map<BlockState,VoxelShape> shapes=new IdentityHashMap<>();
    OwnerCandleHolder(Properties p,JsonObject definition){super(p);registerDefaultState(defaultBlockState().setValue(FACE,AttachFace.FLOOR).setValue(FACING,Direction.NORTH));
        for(var state:stateDefinition.getPossibleStates()){
            VoxelShape shape=Shapes.empty();for(var entry:definition.getAsJsonObject("shapes").entrySet()){
                boolean match=true;for(String pair:entry.getKey().split(",")){if(pair.isEmpty())continue;var kv=pair.split("=");if(!HouseholdBlock.value(state,kv[0]).equals(kv[1])){match=false;break;}}
                if(match)for(var element:entry.getValue().getAsJsonArray()){var a=element.getAsJsonArray();shape=Shapes.or(shape,Block.box(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble(),a.get(3).getAsDouble(),a.get(4).getAsDouble(),a.get(5).getAsDouble()));}
            }shapes.put(state,shape);
        }
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACE,FACING);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shapes.getOrDefault(s,Shapes.block());}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var dir=s.getValue(FACE)==AttachFace.CEILING?Direction.UP:s.getValue(FACE)==AttachFace.FLOOR?Direction.DOWN:s.getValue(FACING).getOpposite();return l.getBlockState(p.relative(dir)).isFaceSturdy(l,p.relative(dir),dir.getOpposite());}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var old=c.getLevel().getBlockState(c.getClickedPos());if(old.is(this))return old.setValue(CANDLES,Math.min(4,old.getValue(CANDLES)+1));
        for(var dir:c.getNearestLookingDirections()){
            var s=defaultBlockState().setValue(FACE,dir==Direction.UP?AttachFace.CEILING:dir==Direction.DOWN?AttachFace.FLOOR:AttachFace.WALL).setValue(FACING,dir.getAxis().isVertical()?c.getHorizontalDirection():dir.getOpposite()).setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER));
            if(s.canSurvive(c.getLevel(),c.getClickedPos()))return s;
        }return null;
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return Menagerie.mayChange(l,player,p)?super.useItemOn(held,s,l,p,player,hand,hit):InteractionResult.FAIL;}
    @Override protected Iterable<Vec3> getParticleOffsets(BlockState s){
        int n=s.getValue(CANDLES);var face=s.getValue(FACE);double[][] a=switch(face){
            case FLOOR->switch(n){case 1->new double[][]{{.5,.6875,.5}};case 2->new double[][]{{.3125,.875,.5},{.6875,.875,.5}};case 3->new double[][]{{.1875,.9375,.5},{.5,.9375,.5},{.8125,.9375,.5}};default->new double[][]{{.1875,1,.5},{.8125,1,.5},{.5,.9375,.25},{.5,.9375,.75}};};
            case WALL->switch(n){case 1->new double[][]{{.5,.9375,.1875}};case 2->new double[][]{{.3125,.9375,.1875},{.6875,.9375,.1875}};case 3->new double[][]{{.8125,.9375,.1875},{.1875,.9375,.1875},{.5,.9375,.3125}};default->new double[][]{{.1875,1,.1875},{.8125,1,.1875},{.3125,.875,.3125},{.6875,.875,.3125}};};
            case CEILING->switch(n){case 1->new double[][]{{.5,.5625,.5}};case 2->new double[][]{{.25,.875,.5},{.75,.875,.5}};case 3->new double[][]{{.5,.875,.75},{.75,.875,.375},{.25,.875,.375}};default->new double[][]{{.1875,.8125,.1875},{.8125,.8125,.1875},{.8125,.8125,.8125},{.1875,.8125,.8125}};};
        };
        var result=new ArrayList<Vec3>();float angle=(float)Math.toRadians(s.getValue(FACING).toYRot());for(var v:a)result.add(new Vec3(v[0]-.5,v[1]-.5,v[2]-.5).yRot(-angle).add(.5,.5,.5));return result;
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
}
