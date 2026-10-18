package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

/** Physical chest and low stone transmutation slab. Chest automation remains bounded; no remote inventory access. */
public final class AlchemyStorageBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final boolean chest;
    public AlchemyStorageBlock(Properties properties,boolean chest){super(properties);this.chest=chest;registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.OPEN,false).setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,BlockStateProperties.OPEN);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState state,Rotation rot){return state.setValue(FACING,rot.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState state){return chest?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return chest?Block.box(1,0,1,15,15,15):Block.box(0,0,0,16,4,16);}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){return chest?new AlchemyStorageBlockEntity(pos,state):null;}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return chest?createTickerHelper(type,AlchemyStorage.ENTITY,AlchemyStorageBlockEntity::tick):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,@Nullable LivingEntity by,ItemStack stack){
        super.setPlacedBy(level,pos,state,by,stack);if(by instanceof ServerPlayer player&&level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity be)be.placedBy(player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer p&&RateGate.allow(p,"alchemy_storage",4)){
            if(chest&&level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity be){
                if(!be.mayUse(p))return InteractionResult.FAIL;
                p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyStorageMenu(id,inv,be),getName()));
            }else if(!chest&&AlchemicalGear.ready(p)&&Authority.mayChange(p.level(),p,pos)){
                var origin=pos.immutable();var original=level;
                p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyPatternMenu(id,inv,()->p.level()==original
                        &&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin))<=64&&original.getBlockState(origin).is(this)
                        &&Authority.mayChange(p.level(),p,origin),ItemStack.EMPTY),getName()));
            }else return InteractionResult.FAIL;
        }return InteractionResult.SUCCESS;
    }
}
