package io.github.jimbozoomer.jugcraft.botany;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jspecify.annotations.Nullable;

public class BotanicalBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FILLED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("filled");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty CORE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("core");
    public final String kind;
    public boolean floating(){return kind.startsWith("floating_")||kind.endsWith("_floating_flower");}
    public String functionKind(){return kind.startsWith("floating_")?kind.substring(9):kind;}
    public BotanicalBlock(String kind,Properties p){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FILLED,false).setValue(CORE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FILLED,CORE);}
    private boolean flower(){return functionKind().equals("pure_daisy")||functionKind().equals("manastar")||BotanicalGenerators.TYPES.contains(functionKind())||BotanicalFunctions.TYPES.contains(functionKind());}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return floating()||!flower()||level.getBlockState(pos.below()).is(Jugtania.block("red_string_relay"))||Blocks.POPPY.defaultBlockState().canSurvive(level,pos);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){var state=super.getStateForPlacement(context);return state!=null&&state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;}
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,net.minecraft.util.RandomSource random){return flower()&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,level,ticks,pos,direction,neighbor,neighborState,random);}
    @Override protected RenderShape getRenderShape(BlockState state){return floating()||kind.equals("fabulous_pool")||kind.equals("fake_air")||kind.equals("brewery")||kind.contains("spreader")?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BotanicalWorkshop(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,Jugtania.WORKSHOP,BotanicalWorkshop::tick);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,@Nullable LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(placer instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof BotanicalWorkshop b){b.owner(p.getUUID());if(functionKind().equals("daffomill"))b.function.wind.face(p.getDirection());}}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.is(Jugtania.item("twig_wand")))return InteractionResult.PASS;
        return player instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.use(p,hand):InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return player instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.use(p,InteractionHand.MAIN_HAND):InteractionResult.SUCCESS;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(kind.equals("fake_air"))return Shapes.empty();
        if(floating())return Block.box(3,3,3,13,16,13);
        if(flower())return Block.box(4,0,4,12,12,12);
        if(kind.endsWith("pool"))return Shapes.or(Block.box(0,0,0,16,2,16),Block.box(0,2,0,2,8,16),Block.box(14,2,0,16,8,16),Block.box(2,2,0,14,8,2),Block.box(2,2,14,14,8,16));
        if(kind.startsWith("drum_"))return Block.box(3,1,3,13,15,13);
        if(kind.equals("incense_plate"))return Block.box(3,0,3,13,2,13);
        if(kind.equals("brewery"))return Block.box(1,0,1,15,16,15);
        if(kind.equals("hourglass"))return Block.box(4,1,4,12,15,12);
        if(kind.equals("bellows"))return Block.box(2,0,0,14,10,16);
        if(kind.equals("terra_plate"))return Block.box(0,0,0,16,2,16);
        if(kind.endsWith("_pylon"))return Block.box(2,0,2,14,16,14);
        if(kind.startsWith("apothecary"))return Shapes.or(Block.box(3,0,3,13,2,13),Block.box(6,2,6,10,10,10),Block.box(1,10,1,15,16,15));
        return Block.box(0,0,0,16,16,16);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof BotanicalWorkshop b?b.mana()==0?0:1+(int)(14L*b.mana()/b.capacity()):0;}
}
