package io.github.jimbozoomer.jugcraft.nouveau;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Native state/selection geometry for the supplied small decorations. */
public final class NouveauFixtures {
    private NouveauFixtures(){}
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,7);
    public static boolean light(net.minecraft.server.level.ServerLevel level,BlockPos at,BlockState state,net.minecraft.server.level.ServerPlayer p,boolean on){
        if(!(state.getBlock() instanceof Sconce)&&!(state.getBlock() instanceof Torch))return false;
        int color=p.getMainHandItem().getOrDefault(NouveauControls.COLOR,0);
        return level.setBlock(at,state.setValue(LIT,on).setValue(COLOR,Math.clamp(color,0,7)),Block.UPDATE_ALL);
    }
    private static void flame(BlockState s,Level l,double x,double y,double z){if(!s.getValue(LIT))return;l.addParticle(new net.minecraft.core.particles.DustParticleOptions(NouveauControls.COLORS[s.getValue(COLOR)],.65F),x,y,z,0,.02,0);}
    public static final class Sconce extends Block {
        public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
        public Sconce(Properties p){super(p.lightLevel(s->s.getValue(LIT)?14:0));registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false).setValue(COLOR,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT,COLOR);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){Direction d=c.getClickedFace();return defaultBlockState().setValue(FACING,d.getAxis().isHorizontal()?d:c.getHorizontalDirection().getOpposite());}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
        @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){case NORTH->Block.box(5,3,10,11,15,16);case SOUTH->Block.box(5,3,0,11,15,6);case EAST->Block.box(0,3,5,6,15,11);default->Block.box(10,3,5,16,15,11);};}
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){var d=s.getValue(FACING);flame(s,l,p.getX()+.5-d.getStepX()*.25,p.getY()+.8,p.getZ()+.5-d.getStepZ()*.25);}
    }
    public static final class Torch extends Block {
        public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
        public static final EnumProperty<Direction> FLAME=EnumProperty.create("flame_direction",Direction.class);
        public Torch(Properties p){super(p.lightLevel(s->s.getValue(LIT)?14:0));registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP).setValue(FLAME,Direction.UP).setValue(LIT,false).setValue(COLOR,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,FLAME,LIT,COLOR);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,15,14);}
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){var direction=s.getValue(FLAME);flame(s,l,p.getX()+.5+direction.getStepX()*.35,p.getY()+.5+direction.getStepY()*.35,p.getZ()+.5+direction.getStepZ()*.35);}
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s,Level l,BlockPos at,net.minecraft.world.entity.player.Player user,net.minecraft.world.phys.BlockHitResult hit){
            if(user instanceof net.minecraft.server.level.ServerPlayer p){if(!JugNouveau.enabled()||!io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(p.level(),p,at))return net.minecraft.world.InteractionResult.FAIL;var directions=Direction.values();l.setBlock(at,s.setValue(FLAME,directions[(s.getValue(FLAME).ordinal()+1)%directions.length]),Block.UPDATE_ALL);}return net.minecraft.world.InteractionResult.SUCCESS;
        }
    }
    public static final class Plush extends Block {
        public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
        public Plush(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,0,3,13,12,13);}
        @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    }
    public static final class ParticleBlock extends Block {
        public ParticleBlock(Properties p){super(p);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){var particle=NouveauResources.PARTICLES.get("glow");if(particle!=null)l.addParticle(particle,p.getX()+r.nextDouble(),p.getY()+r.nextDouble(),p.getZ()+r.nextDouble(),0,.02,0);}
    }
}
