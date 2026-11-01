package io.github.jimbozoomer.jugcraft.arcane;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Each visible part opens the same central inventory; no duplicated inventories. */
public class ArcaneClibanoBlock extends BaseEntityBlock {
    public enum Face implements StringRepresentable {FRONT_OFF,FRONT_FIRE,FRONT_SOUL_FIRE,FRONT_ENCHANTED_FIRE,SIDE,TOP;public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}}
    public enum Heat implements StringRepresentable {OFF,FIRE,SOUL_FIRE,ENCHANTED_FIRE;public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}}
    public static final EnumProperty<Face> FACE=EnumProperty.create("type",Face.class);
    public static final EnumProperty<Heat> HEAT=EnumProperty.create("type",Heat.class);
    public static final BooleanProperty MIRRORED=BooleanProperty.create("mirrored"),BOTTOM=BooleanProperty.create("bottom");
    public ArcaneClibanoBlock(Properties p){super(p);}
    @Override protected RenderShape getRenderShape(BlockState s){return s.is(ForbiddenArcane.block("clibano_main_part"))?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArcaneClibano(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,ForbiddenArcane.CLIBANO,ArcaneClibano::tick);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof ArcaneClibano part){var main=part.master();if(main==null||!Authority.mayChange(sp.level(),sp,p)||!main.stillValid(sp))return InteractionResult.FAIL;sp.openMenu(main);}return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack item,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return useWithoutItem(s,l,p,player,hit);}
    public static final class Center extends ArcaneClibanoBlock {
        public Center(Properties p){super(p.lightLevel(s->switch(s.getValue(FACE)){case FRONT_FIRE->13;case FRONT_SOUL_FIRE->9;case FRONT_ENCHANTED_FIRE->14;default->0;}));registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING,Direction.NORTH).setValue(FACE,Face.SIDE));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING,FACE);}
    }
    public static final class Side extends ArcaneClibanoBlock {
        public Side(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(HEAT,Heat.OFF).setValue(MIRRORED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING,HEAT,MIRRORED);}
    }
    public static final class Corner extends ArcaneClibanoBlock {
        public Corner(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BOTTOM,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING,BOTTOM);}
    }
    public static final class Core extends HorizontalDirectionalBlock {
        public Core(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    }
}
