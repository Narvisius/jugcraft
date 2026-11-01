package io.github.jimbozoomer.jugcraft.aquatic;
import net.minecraft.server.level.ServerLevel;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class OwnerAquaticStations {
    private OwnerAquaticStations(){}
    public static Station TACKLE,BOUNTY;public static BlockEntityType<Storage> STORAGE;public static MenuType<OwnerTackleStationMenu> MENU;
    public static void register(){
        TACKLE=station("tackle_box",true);BOUNTY=station("neptunes_bounty",false);
        STORAGE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("aquatic_station"),FabricBlockEntityTypeBuilder.create(Storage::new,TACKLE,BOUNTY).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("aquatic_station"),new MenuType<>((id,inv)->new OwnerTackleStationMenu(id,inv,new SimpleContainer(17)),FeatureFlags.DEFAULT_FLAGS));
    }
    private static Station station(String name,boolean tackle){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("aquatic_"+name));var block=Registry.register(BuiltInRegistries.BLOCK,key,new Station(BlockBehaviour.Properties.of().setId(key).strength(3.5f,8).sound(SoundType.METAL).noOcclusion(),tackle));OwnerAquaculture.item(name,p->new BlockItem(block,p));return block;}
    public static final class Station extends Block implements EntityBlock,SimpleWaterloggedBlock {
        public static final EnumProperty<net.minecraft.core.Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;public static final BooleanProperty OPEN=BlockStateProperties.OPEN,WATERLOGGED=BlockStateProperties.WATERLOGGED;
        public final boolean tackle;
        Station(Properties p,boolean tackle){super(p);this.tackle=tackle;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(OPEN,false).setValue(WATERLOGGED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,OPEN,WATERLOGGED);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);}
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState other,net.minecraft.util.RandomSource r){if(s.getValue(WATERLOGGED))ticks.scheduleTick(p,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(l));return s;}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos p,CollisionContext c){if(!tackle)return Block.box(1,0,1,15,14,15);return s.getValue(FACING).getAxis()==Direction.Axis.X?Block.box(4,0,1,12,10,15):Block.box(1,0,4,15,10,12);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){return type==STORAGE?(l,p,state,be)->{var box=(Storage)be;if(l.isClientSide()){box.previousLid=box.lid;box.lid=Math.clamp(box.lid+(state.getValue(OPEN)?.1f:-.1f),0,1);}else if(l.getGameTime()%20==0&&l instanceof ServerLevel server){box.viewers=(int)server.players().stream().filter(player->!player.isSpectator()&&player.containerMenu.slots.stream().anyMatch(slot->slot.container==box)).count();box.open(box.viewers>0);}}:null;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Storage(p,s);}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(player instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;if(level.getBlockEntity(pos) instanceof Storage storage){if(tackle&&storage.viewers>0){p.sendOverlayMessage(Component.literal("This tackle box is being used."));return InteractionResult.FAIL;}p.openMenu(storage);}}return InteractionResult.SUCCESS;
        }
    }
    public static final class Storage extends RandomizableContainerBlockEntity {
        private NonNullList<ItemStack> items;private int viewers;public float lid,previousLid;
        public Storage(BlockPos p,BlockState s){super(STORAGE,p,s);items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);}
        public boolean tackle(){return ((Station)getBlockState().getBlock()).tackle;}
        @Override public int getContainerSize(){return tackle()?17:27;}
        @Override protected NonNullList<ItemStack> getItems(){return items;}
        @Override protected void setItems(NonNullList<ItemStack> next){items=next;}
        @Override protected Component getDefaultName(){return Component.literal(tackle()?"Tackle box":"Neptune's bounty");}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return tackle()?new OwnerTackleStationMenu(id,inv,this):ChestMenu.threeRows(id,inv,this);}
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&(!(p instanceof ServerPlayer sp)||Authority.mayChange(sp.level(),sp,worldPosition));}
        @Override public boolean canPlaceItem(int slot,ItemStack s){return !tackle()||(slot==0?s.getItem() instanceof OwnerAquaticGear.Rod:OwnerTackleStationMenu.supply(s));}
        @Override public boolean canTakeItem(Container target,int slot,ItemStack stack){return !(tackle()&&viewers>0&&slot==0)&&super.canTakeItem(target,slot,stack);}
        @Override public void startOpen(ContainerUser user){if(!user.getLivingEntity().isSpectator()){viewers++;open(true);}}
        @Override public void stopOpen(ContainerUser user){if(!user.getLivingEntity().isSpectator()){viewers=Math.max(0,viewers-1);open(viewers>0);}}
        private void open(boolean value){if(level!=null&&!level.isClientSide()&&getBlockState().getValue(Station.OPEN)!=value){level.setBlock(worldPosition,getBlockState().setValue(Station.OPEN,value),3);level.playSound(null,worldPosition,tackle()?OwnerAquaticSounds.sound(value?"tackle_box_open":"tackle_box_close"):value?net.minecraft.sounds.SoundEvents.CHEST_OPEN:net.minecraft.sounds.SoundEvents.CHEST_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(!trySaveLootTable(out))ContainerHelper.saveAllItems(out,items);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);if(!tryLoadLootTable(in))ContainerHelper.loadAllItems(in,items);}
    }
}
