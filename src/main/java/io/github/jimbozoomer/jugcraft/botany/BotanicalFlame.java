package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;
public final class BotanicalFlame extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,15);
    BotanicalFlame(Properties p){super(p.noCollision().noOcclusion().lightLevel(s->15));registerDefaultState(stateDefinition.any().setValue(COLOR,5).setValue(BlockStateProperties.WATERLOGGED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(COLOR,BlockStateProperties.WATERLOGGED);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(5,3,5,11,10,11);}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos at,Direction side,BlockPos neighbor,BlockState other,RandomSource random){if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(at,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,at,side,neighbor,other,random);}
    @Override public void animateTick(BlockState s,Level level,BlockPos at,RandomSource random){level.addParticle(new DustParticleOptions(net.minecraft.world.item.DyeColor.byId(s.getValue(COLOR)).getTextureDiffuseColor(),1.2F),at.getX()+.5+(random.nextDouble()-.5)*.15,at.getY()+.35+random.nextDouble()*.2,at.getZ()+.5+(random.nextDouble()-.5)*.15,0,.01,0);}
}
