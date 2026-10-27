package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import java.util.UUID;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Paid care for the same 9x9 field layout as the shared harvester. No chunk loading or synthetic fertilizer. */
public final class CropMatron {
    public static Block BLOCK;
    public static BlockEntityType<Node> TYPE;
    private CropMatron(){}
    public static void register(){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_cropmatron"));
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,key,new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key)));
        var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(BLOCK,new Item.Properties().setId(item).useBlockDescriptionPrefix()));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,key.identifier(),FabricBlockEntityTypeBuilder.create(Node::new,BLOCK).build());
        EnergyStorage.SIDED.registerForBlockEntity((node,side)->node.energy,TYPE);
        FluidStorage.SIDED.registerForBlockEntity((node,side)->node.water,TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->out.accept(BLOCK));
    }
    private static final class Device extends Block implements EntityBlock,EnergyConnectable,FluidConnectable {
        static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
        static final BooleanProperty LIT=BlockStateProperties.LIT;
        Device(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,pos,state,entity)->{if(world.getGameTime()%5==0)((Node)entity).step((ServerLevel)world);}:null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){if(entity instanceof ServerPlayer player&&l.getBlockEntity(p) instanceof Node node){node.owner=player.getUUID();node.setChanged();}}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(l.getBlockEntity(p) instanceof Node node)||!node.permitted(player))return InteractionResult.FAIL;
            return FluidStorageUtil.interactWithFluidStorage(node.water,player,hand)?InteractionResult.SUCCESS:InteractionResult.PASS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof Node node&&node.permitted(player)){
                server.openMenu(node);server.sendOverlayMessage(Component.literal("Cropmatron: "+node.water.millibuckets()+" / 4000 mB · "+node.energy.getAmount()+" JE · fertilizer / Weed-EX · redstone pauses"));
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends RandomizableContainerBlockEntity {
        public UUID owner;public int cursor;
        private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(16000,128,0,this::setChanged);
        public final FluidTank water=new FluidTank(4000,true,v->v.isOf(Fluids.WATER),this::setChanged);
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public boolean permitted(Player player){return owner!=null&&player.isAlive()&&!player.isSpectator()&&(owner.equals(player.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,player.getUUID()))&&Authority.mayChange(level,player,worldPosition);}
        @Override protected Component getDefaultName(){return Component.literal("Cropmatron · Fertilizer / Weed-EX");}
        @Override protected NonNullList<ItemStack> getItems(){return items;}
        @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
        @Override public int getContainerSize(){return 9;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return fertilizer(stack)||stack.is(IndustrialCrops.ITEMS.get("jimta_weed_ex"));}
        @Override public boolean stillValid(Player player){return super.stillValid(player)&&permitted(player);}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new ChestMenu(MenuType.GENERIC_9x1,id,inventory,this,1);}
        private static boolean fertilizer(ItemStack stack){return stack.is(Items.BONE_MEAL)||BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(Jugcraft.id("fertilizer"));}
        private ItemStack supply(boolean weed){for(var stack:items)if(weed?stack.is(IndustrialCrops.ITEMS.get("jimta_weed_ex")):fertilizer(stack))return stack;return ItemStack.EMPTY;}
        public void step(ServerLevel level){
            boolean worked=false;
            var actor=Authority.answering(level,owner);
            if(actor!=null&&permitted(actor)&&!level.hasNeighborSignal(worldPosition)&&energy.getAmount()>=40){
                int index=cursor;cursor=(cursor+1)%81;setChanged();
                var front=getBlockState().getValue(Device.FACING);var pos=worldPosition.relative(front,1+index/9).relative(front.getClockWise(),index%9-4);
                if(level.hasChunkAt(pos)&&level.hasChunkAt(pos.below())&&ClaimTransfers.allowed(level,worldPosition,pos)&&Authority.mayChange(level,actor,pos)&&Authority.mayChange(level,actor,pos.below())){
                    var state=level.getBlockState(pos);var soil=level.getBlockState(pos.below());
                    if(state.getBlock() instanceof CropBlock crop){
                        boolean weed=crop instanceof IndustrialCrops.IndustrialCrop special&&special.variety.id().equals("weed");
                        if(weed&&!supply(true).isEmpty()){
                            worked=level.setBlock(pos,IndustrialCrops.CROPS.get("cross_sticks").defaultBlockState(),Block.UPDATE_ALL);
                            if(worked)supply(true).shrink(1);
                        }else if(soil.is(Blocks.FARMLAND)&&soil.getValue(FarmlandBlock.MOISTURE)<7&&water.has(Fluids.WATER,25)){
                            worked=level.setBlock(pos.below(),soil.setValue(FarmlandBlock.MOISTURE,7),Block.UPDATE_CLIENTS);
                            if(worked)water.drain(25);
                        }else if(!weed&&!crop.isMaxAge(state)&&!supply(false).isEmpty()
                                &&(crop instanceof IndustrialCrops.IndustrialCrop special?!special.variety.id().equals("cross_sticks")&&special.canRipen(level,pos):level.getRawBrightness(pos,0)>=9)){
                            worked=level.setBlock(pos,crop.getStateForAge(crop.getAge(state)+1),Block.UPDATE_CLIENTS);
                            if(worked)supply(false).shrink(1);
                        }
                    }
                }
            }
            if(worked){energy.setAmount(energy.getAmount()-40);setChanged();}
            if(getBlockState().getValue(Device.LIT)!=worked)level.setBlock(worldPosition,getBlockState().setValue(Device.LIT,worked),Block.UPDATE_CLIENTS);
        }
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null&&!level.isClientSide())Containers.dropContents(level,p,this);items.clear();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);out.putInt("Cursor",cursor);out.putLong("Energy",energy.getAmount());water.writeValue(out.child("Water"));}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);cursor=Math.clamp(in.getIntOr("Cursor",0),0,80);energy.setAmount(in.getLongOr("Energy",0));water.readValue(in.childOrEmpty("Water"));}
    }
}
