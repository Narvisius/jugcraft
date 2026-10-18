package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

public final class AlchemyMachinesBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE=BlockStateProperties.LIT, WALL=BooleanProperty.create("wall");
    public final AlchemyMachines.Kind kind;
    public final int tier;
    public AlchemyMachinesBlock(Properties props,AlchemyMachines.Kind kind,int tier) {
        super(props);this.kind=kind;this.tier=tier;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ACTIVE,false).setValue(WALL,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,ACTIVE,WALL);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){
        var s=defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());
        if(kind==AlchemyMachines.Kind.TORCH&&ctx.getClickedFace().getAxis().isHorizontal())s=s.setValue(WALL,true).setValue(FACING,ctx.getClickedFace());
        return s.canSurvive(ctx.getLevel(),ctx.getClickedPos())?s:null;
    }
    @Override protected boolean canSurvive(BlockState s,LevelReader level,BlockPos pos){
        return kind!=AlchemyMachines.Kind.TORCH||(s.getValue(WALL)?WallTorchBlock.canSurvive(level,pos,s.getValue(FACING)):Block.canSupportCenter(level,pos.below(),Direction.UP));
    }
    @Override protected BlockState updateShape(BlockState s,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbour,BlockState neighbourState,RandomSource random){
        return s.canSurvive(level,pos)?s:Blocks.AIR.defaultBlockState();
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(kind==AlchemyMachines.Kind.PEDESTAL)return Shapes.or(Block.box(3,0,3,13,2,13),Block.box(6,2,6,10,9,10),Block.box(5,9,5,11,10,11));
        if(kind==AlchemyMachines.Kind.TORCH)return s.getValue(WALL)?WallTorchBlock.getShape(s):Block.box(6,0,6,10,10,10);
        return Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AlchemyMachinesBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,AlchemyMachines.ENTITY,AlchemyMachinesBlockEntity::tick);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@Nullable LivingEntity who,ItemStack stack){
        super.setPlacedBy(l,p,s,who,stack);if(who instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof AlchemyMachinesBlockEntity be)be.placedBy(player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){
        if(who instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof AlchemyMachinesBlockEntity be&&RateGate.allow(player,"alchemy_machine_open",4)){
            if(!be.mayUse(player))return InteractionResult.FAIL;
            if(kind==AlchemyMachines.Kind.NOVA&&player.isShiftKeyDown())be.activateNova(player);
            else player.openMenu(new SimpleMenuProvider((id,inv,actor)->new AlchemyMachinesMenu(id,inv,be),getName()));
        }
        return InteractionResult.SUCCESS;
    }
}
