package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Supplied eight-age flax: the upper shoot appears at age four and shares one harvest. */
public final class OwnerSupplementFlax extends CropBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    OwnerSupplementFlax(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(HALF);}
    @Override protected ItemLike getBaseSeedId(){return OwnerSupplementaries.ITEMS.get("flax_seeds");}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        if(s.getValue(HALF)==DoubleBlockHalf.UPPER){var below=l.getBlockState(p.below());return below.is(this)&&below.getValue(HALF)==DoubleBlockHalf.LOWER&&getAge(below)>=4;}
        return super.canSurvive(s,l,p);
    }
    private boolean room(LevelReader l,BlockPos p){var above=l.getBlockState(p.above());return above.isAir()||above.is(this)&&above.getValue(HALF)==DoubleBlockHalf.UPPER;}
    @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,BonemealSource source){if(s.getValue(HALF)==DoubleBlockHalf.UPPER){p=p.below();s=l.getBlockState(p);if(!s.is(this))return false;}return getAge(s)<7&&(getAge(s)<3||room(l,p));}
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){if(s.getValue(HALF)==DoubleBlockHalf.LOWER&&l.getRawBrightness(p,0)>=9&&isValidBonemealTarget(l,p,s,null)&&random.nextInt(io.github.jimbozoomer.jugcraft.agriculture.CropGrowth.chanceDivisor(io.github.jimbozoomer.jugcraft.agriculture.CropGrowth.speed(l,p,false),1F))==0)grow(l,p,s,1);}
    @Override public void growCrops(Level l,BlockPos p,BlockState s){grow(l,p,s,getBonemealAgeIncrease(l));}
    private void grow(Level l,BlockPos p,BlockState s,int amount){
        if(s.getValue(HALF)==DoubleBlockHalf.UPPER){p=p.below();s=l.getBlockState(p);if(!s.is(this))return;}
        int age=Math.min(7,getAge(s)+amount);if(age>=4&&!room(l,p))age=Math.min(3,age);
        if(age<=getAge(s))return;
        if(age>=4)l.setBlock(p.above(),getStateForAge(age).setValue(HALF,DoubleBlockHalf.UPPER),Block.UPDATE_CLIENTS);
        l.setBlock(p,getStateForAge(age).setValue(HALF,DoubleBlockHalf.LOWER),Block.UPDATE_ALL);
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos other,BlockState neighbor,RandomSource random){
        if(s.getValue(HALF)==DoubleBlockHalf.UPPER&&dir==Direction.DOWN){return neighbor.is(this)&&getAge(neighbor)>=4?getStateForAge(getAge(neighbor)).setValue(HALF,DoubleBlockHalf.UPPER):Blocks.AIR.defaultBlockState();}
        if(s.getValue(HALF)==DoubleBlockHalf.LOWER&&getAge(s)>=4&&dir==Direction.UP&&!neighbor.is(this))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,l,ticks,p,dir,other,neighbor,random);
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide()&&player.isCreative()&&s.getValue(HALF)==DoubleBlockHalf.UPPER&&l.getBlockState(p.below()).is(this))l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);return super.playerWillDestroy(l,p,s,player);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,s.getValue(HALF)==DoubleBlockHalf.LOWER?(getAge(s)<4?4+getAge(s)*3:16):4+(getAge(s)-4)*4,14);}
}
