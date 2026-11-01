package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Hollow, oil-bearing owner logs; carving preserves both oil and rainwater. */
public final class EdelwoodBlocks {
    private EdelwoodBlocks(){}
    public static final BooleanProperty OILY=BooleanProperty.create("oily"),LEAVES=BooleanProperty.create("leaves"),WATER=BlockStateProperties.WATERLOGGED;
    public static class Log extends Block implements SimpleWaterloggedBlock {
        private static final VoxelShape Y=Shapes.join(Shapes.block(),Block.box(2,0,2,14,16,14),BooleanOp.ONLY_FIRST),X=Shapes.join(Shapes.block(),Block.box(0,2,2,16,14,14),BooleanOp.ONLY_FIRST),Z=Shapes.join(Shapes.block(),Block.box(2,2,0,14,14,16),BooleanOp.ONLY_FIRST);
        public Log(Properties p){super(p.randomTicks());var s=defaultBlockState().setValue(OILY,false).setValue(WATER,false);registerDefaultState(s.hasProperty(BlockStateProperties.AXIS)?s.setValue(BlockStateProperties.AXIS,Direction.Axis.Y):s.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(LEAVES,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(BlockStateProperties.AXIS,OILY,WATER);}
        protected boolean carved(){return false;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(c.isHoldingItem(ForbiddenArcane.item("edelwood_log"))||c.isHoldingItem(ForbiddenArcane.item("carved_edelwood_log")))return Shapes.block();return carved()?Y:s.getValue(BlockStateProperties.AXIS)==Direction.Axis.X?X:s.getValue(BlockStateProperties.AXIS)==Direction.Axis.Z?Z:Y;}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState().setValue(WATER,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));return carved()?s.setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite()):s.setValue(BlockStateProperties.AXIS,c.getClickedFace().getAxis());}
        @Override protected boolean isRandomlyTicking(BlockState s){return !s.getValue(OILY);}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(r.nextFloat()<.025F)l.setBlock(p,s.setValue(OILY,true),3);}
        @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATER)?Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState other,RandomSource r){if(s.getValue(WATER))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,p,d,n,other,r);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            boolean oil=stack.is(Items.GLASS_BOTTLE)&&s.getValue(OILY),carve=!carved()&&s.getValue(BlockStateProperties.AXIS)==Direction.Axis.Y&&stack.is(ItemTags.AXES),shear=carved()&&s.getValue(LEAVES)&&stack.is(Items.SHEARS),grow=carved()&&!s.getValue(LEAVES)&&stack.getItem() instanceof BoneMealItem;
            if(!oil&&!carve&&!shear&&!grow)return InteractionResult.PASS;
            if(player instanceof ServerPlayer p){
                if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
                if(oil){p.setItemInHand(hand,ItemUtils.createFilledResult(stack,p,new ItemStack(ForbiddenArcane.item("edelwood_oil"))));l.setBlockAndUpdate(pos,s.setValue(OILY,false));l.playSound(null,pos,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,1,1);}
                else if(carve){var face=hit.getDirection().getAxis().isHorizontal()?hit.getDirection():p.getDirection().getOpposite();l.setBlockAndUpdate(pos,ForbiddenArcane.block("carved_edelwood_log").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face).setValue(OILY,s.getValue(OILY)).setValue(WATER,s.getValue(WATER)));stack.hurtAndBreak(1,p,hand.asEquipmentSlot());l.playSound(null,pos,SoundEvents.AXE_STRIP.value(),SoundSource.BLOCKS,1,1);}
                else {l.setBlockAndUpdate(pos,s.setValue(LEAVES,grow));if(grow)stack.consume(1,p);else stack.hurtAndBreak(1,p,hand.asEquipmentSlot());l.playSound(null,pos,grow?SoundEvents.BONE_MEAL_USE:SoundEvents.SHEEP_SHEAR,SoundSource.BLOCKS,1,1);}
            }return InteractionResult.SUCCESS;
        }
        @Override public void handlePrecipitation(BlockState s,Level l,BlockPos p,Biome.Precipitation rain){
            if(l.isClientSide()||rain!=Biome.Precipitation.RAIN||s.getValue(WATER)||!carved()&&s.getValue(BlockStateProperties.AXIS)!=Direction.Axis.Y||l.getRandom().nextFloat()>=.15F)return;
            BlockPos target=p;for(int i=0;i<64;i++){var below=target.below();if(!l.hasChunkAt(below))break;var b=l.getBlockState(below);if(!(b.getBlock() instanceof Log)||b.getValue(WATER)||b.hasProperty(BlockStateProperties.AXIS)&&b.getValue(BlockStateProperties.AXIS)!=Direction.Axis.Y)break;target=below;}
            l.setBlockAndUpdate(target,l.getBlockState(target).setValue(WATER,true));
        }
        @Override protected BlockState rotate(BlockState s,Rotation r){if(carved())return s.setValue(BlockStateProperties.HORIZONTAL_FACING,r.rotate(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));if(r==Rotation.CLOCKWISE_90||r==Rotation.COUNTERCLOCKWISE_90){var a=s.getValue(BlockStateProperties.AXIS);if(a!=Direction.Axis.Y)return s.setValue(BlockStateProperties.AXIS,a==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X);}return s;}
        @Override protected BlockState mirror(BlockState s,Mirror m){return carved()?rotate(s,m.getRotation(s.getValue(BlockStateProperties.HORIZONTAL_FACING))):s;}
    }
    public static final class Carved extends Log {
        public Carved(Properties p){super(p);}
        @Override protected boolean carved(){return true;}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(BlockStateProperties.HORIZONTAL_FACING,OILY,LEAVES,WATER);}
    }
    public static final class Branch extends Block implements SimpleWaterloggedBlock {
        public Branch(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(WATER,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(BlockStateProperties.HORIZONTAL_FACING,WATER);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){for(var d:c.getNearestLookingDirections())if(d.getAxis().isHorizontal()){var s=defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,d.getOpposite()).setValue(WATER,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));if(s.canSurvive(c.getLevel(),c.getClickedPos()))return s;}return null;}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var f=s.getValue(BlockStateProperties.HORIZONTAL_FACING);var support=p.relative(f.getOpposite());return l.getBlockState(support).isFaceSturdy(l,support,f);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(BlockStateProperties.HORIZONTAL_FACING)){case SOUTH->Block.box(6,5,0,10,16,11);case EAST->Block.box(0,5,6,11,16,10);case WEST->Block.box(5,5,6,16,16,10);default->Block.box(6,5,5,10,16,16);};}
        @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATER)?Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState other,RandomSource r){if(s.getValue(WATER))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return !s.canSurvive(l,p)?(s.getValue(WATER)?Blocks.WATER:Blocks.AIR).defaultBlockState():s;}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.HORIZONTAL_FACING,r.rotate(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
    }
}
