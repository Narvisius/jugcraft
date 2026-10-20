package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
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
import org.jspecify.annotations.Nullable;

public final class NouveauWorkshopBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE=BlockStateProperties.LIT;
    public static final IntegerProperty FILL=IntegerProperty.create("fill",0,11);
    public final NouveauCatalog.Entry entry;
    public NouveauWorkshopBlock(NouveauCatalog.Entry entry,Properties p){super(p);this.entry=entry;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ACTIVE,false).setValue(FILL,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,ACTIVE,FILL);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,java.util.Set.of("arcane_pedestal","arcane_platform","spell_prism","void_prism").contains(entry.source())?c.getClickedFace():c.getHorizontalDirection().getOpposite());}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return switch(entry.source()){
        case "rune"->Block.box(0,0,0,16,1,16);
        case "arcane_platform"->switch(s.getValue(FACING)){case UP->Block.box(0,0,0,16,2,16);case DOWN->Block.box(0,14,0,16,16,16);case NORTH->Block.box(0,0,14,16,16,16);case SOUTH->Block.box(0,0,0,16,16,2);case EAST->Block.box(0,0,0,2,16,16);case WEST->Block.box(14,0,0,16,16,16);};
        case "arcane_pedestal"->net.minecraft.world.phys.shapes.Shapes.or(Block.box(5,0,5,11,13,11),Block.box(2,11,2,14,14,14));
        case "source_jar","creative_source_jar","potion_jar","mob_jar","planarium"->Block.box(3,0,3,13,15,13);
        case "spell_prism","void_prism","relay","relay_collector","relay_deposit","relay_splitter","relay_warp","redstone_relay","brazier_relay"->Block.box(4,2,4,12,14,12);
        case "potion_diffuser","scryers_crystal","spell_sensor"->Block.box(3,0,3,13,12,13);
        default->super.getShape(s,l,p,c);};}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState s){return entry.geo().isEmpty()?RenderShape.MODEL:RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new NouveauWorkshop(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,NouveauWorkshops.ENTITY,NouveauWorkshop::tick);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@Nullable LivingEntity who,ItemStack item){super.setPlacedBy(l,p,s,who,item);if(who instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof NouveauWorkshop be){be.placedBy(player);var name=item.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);if(name!=null)be.customName(name.getString());}}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(l.getBlockEntity(pos) instanceof NouveauWorkshop jar&&jar.kind().equals("mob_jar")&&player instanceof ServerPlayer p){var result=NouveauContainment.interact(p,jar,hand);if(result!=InteractionResult.PASS)return result;}
        if(l.getBlockEntity(pos) instanceof NouveauWorkshop be&&be.kind().equals("potion_jar"))return player instanceof ServerPlayer p?NouveauAlchemy.interact(p,be,hand):InteractionResult.SUCCESS;
        if(player instanceof ServerPlayer p&&l.getBlockEntity(pos) instanceof NouveauWorkshop be)return NouveauWorkshopInteractions.use(p,be,hand);
        return InteractionResult.PASS;
    }
    @Override protected boolean isSignalSource(BlockState s){return java.util.Set.of("redstone_relay","spell_sensor","item_detector","spell_prism").contains(entry.source());}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos pos,Direction direction){if(entry.source().equals("redstone_relay")&&direction==s.getValue(FACING))return 0;return l.getBlockEntity(pos) instanceof NouveauWorkshop be?be.signal():0;}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos pos,Direction side){if(!(l.getBlockEntity(pos) instanceof NouveauWorkshop be))return 0;return be.kind().equals("potion_jar")?(be.doses()==0?0:1+be.doses()*14/100):(be.ley()==0?0:1+(int)(be.ley()*14/be.capacity()));}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof NouveauWorkshop be){
            if(!be.mayUse(server)||!RateGate.allow(server,"nouveau_workshop_open",4))return InteractionResult.FAIL;
            var interaction=NouveauWorkshopInteractions.use(server,be,InteractionHand.MAIN_HAND);if(interaction!=InteractionResult.PASS)return interaction;
            if(be.kind().equals("scryers_crystal"))return NouveauScrying.open(server,GlobalPos.of(l.dimension(),p))?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(be.kind().equals("scryers_oculus")){NouveauScrying.oculus(server,be);return InteractionResult.SUCCESS;}
            if(be.kind().equals("planarium")&&player.isShiftKeyDown())return NouveauPlanarium.enter(server,be)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            if(be.kind().equals("repository_controller")){server.openMenu(new SimpleMenuProvider((id,inv,who)->new NouveauRepositories.Menu(id,inv,be),getName()));return InteractionResult.SUCCESS;}
            if(be.kind().equals("mob_jar")&&be.captured()!=null&&player.isShiftKeyDown())return NouveauContainment.release(server.level(),server,be)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            server.openMenu(new SimpleMenuProvider((id,inv,who)->new NouveauWorkshopMenu(id,inv,be),getName()));
        }return InteractionResult.SUCCESS;
    }
}
