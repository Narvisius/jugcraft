package local.peepo;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;

public final class WheelBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,3);
    public static final BooleanProperty RUNNING=BooleanProperty.create("running");
    public WheelBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,0).setValue(RUNNING,false)); }
    public static BlockPos partPos(BlockPos master,Direction facing,int part) { return master.relative(facing.getClockWise(),part%2).above(part/2); }
    public static BlockPos master(BlockPos pos,BlockState state) { int part=state.getValue(PART);return pos.relative(state.getValue(FACING).getClockWise(),-part%2).below(part/2); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,PART,RUNNING); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return state.getValue(PART)==0?new WheelBlockEntity(pos,state):null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return state.getValue(PART)==0?createTickerHelper(type,GeneratorWheel.ENTITY,WheelBlockEntity::tick):null;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing=context.getHorizontalDirection().getOpposite();
        for(int i=0;i<4;i++) {
            var pos=partPos(context.getClickedPos(),facing,i);
            if(context.getLevel().isOutsideBuildHeight(pos) || !context.getLevel().getWorldBorder().isWithinBounds(pos)
              || !context.getLevel().getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context,pos,Direction.UP)))return null;
        }
        return defaultBlockState().setValue(FACING,facing);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if(!level.isClientSide())for(int i=1;i<4;i++)level.setBlock(partPos(pos,state.getValue(FACING),i),state.setValue(PART,i),Block.UPDATE_ALL);
        EnergyNetworks.invalidate(level);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved) {
        if(level.getBlockState(pos).is(this))return;
        BlockPos master=master(pos,state);
        for(int i=0;i<4;i++) {
            var other=partPos(master,state.getValue(FACING),i);var existing=level.getBlockState(other);
            if(!other.equals(pos) && existing.is(this) && existing.getValue(PART)==i && existing.getValue(FACING)==state.getValue(FACING))level.removeBlock(other,false);
        }
        EnergyNetworks.invalidate(level);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(PART)<2?Block.box(0,0,0,16,5,16):Shapes.empty();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide() && level.getBlockEntity(master(pos,state)) instanceof WheelBlockEntity wheel)
            player.openMenu(wheel);
        return InteractionResult.SUCCESS;
    }
}
