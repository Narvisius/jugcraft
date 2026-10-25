package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** IC matter fabrication and item memories, constrained by the server's existing output economy policy. */
public final class MatterWorkshops {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static Item RAW_MEMORY,MEMORY;
    public static PetroFluids.Entry MATTER;
    public static BlockEntityType<Node> ENTITY;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    private MatterWorkshops(){}
    public static void register(){
        MATTER=PetroFluids.registerOwnerLiquid("jimta_uu_matter",0xFFB54ED3);
        for(String kind:List.of("mass_fabricator","matter_scanner","pattern_storage","matter_replicator","solid_canner","fluid_bottler")){
            var id=Jugcraft.id("jimta_"+kind);var key=ResourceKey.create(Registries.BLOCK,id);var block=new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key),kind);Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(kind,block);
            var ik=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
        }
        RAW_MEMORY=item("jimta_raw_crystal_memory",false);MEMORY=item("jimta_crystal_memory",true);
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_matter_workshop"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_matter_workshop"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity((node,side)->node.energy,ENTITY);
        FluidStorage.SIDED.registerForBlockEntity((node,side)->node.kind().equals("mass_fabricator")||node.kind().equals("matter_replicator")||node.kind().equals("fluid_bottler")?node.tank:null,ENTITY);
        ItemStorage.SIDED.registerForBlockEntity((node,side)->ContainerStorage.of(node,side),ENTITY);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{BLOCKS.values().forEach(out::accept);out.accept(RAW_MEMORY);out.accept(MEMORY);out.accept(MATTER.bucket());});
    }
    private static Item item(String id,boolean memory){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));return Registry.register(BuiltInRegistries.ITEM,key,new Item(new Item.Properties().setId(key).stacksTo(memory?1:64)){
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,display,lines,flag);if(memory){ItemStack target=remembered(stack);lines.accept(target.isEmpty()?Component.literal("Blank memory — put beside a sample in the scanner"):Component.literal("Pattern: ").append(target.getHoverName()));}}
    });}
    public static ItemStack remembered(ItemStack memory){if(!memory.is(MEMORY))return ItemStack.EMPTY;var id=Identifier.tryParse(memory.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("MatterPattern",""));if(id==null||!BuiltInRegistries.ITEM.containsKey(id))return ItemStack.EMPTY;return new ItemStack(BuiltInRegistries.ITEM.getValue(id));}
    public static ItemStack memory(ItemStack sample){var stack=new ItemStack(MEMORY);CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putString("MatterPattern",BuiltInRegistries.ITEM.getKey(sample.getItem()).toString()));return stack;}
    public static int cost(ItemStack sample){var material=AlchemicalWorkshopBlockEntity.material(sample);if(material==null||!material.formable()||!TabletOutputs.allowed(sample))return 0;long value=Assay.form(material,1);return value<=16000?(int)Math.max(1,value):0;}
    private static final class Device extends Block implements EntityBlock,EnergyConnectable,FluidConnectable,ItemConnectable {
        static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
        static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
        final String kind;Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState state){return new Node(p,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){return !level.isClientSide()&&type==ENTITY?(l,p,st,e)->{if(l.getGameTime()%20==0){var node=(Node)e;node.step((ServerLevel)l);boolean lit=node.status==4;if(st.getValue(LIT)!=lit)l.setBlock(p,st.setValue(LIT,lit),Block.UPDATE_CLIENTS);}}:null;}
        @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(placer!=null&&level.getBlockEntity(pos) instanceof Node node){node.owner=placer.getUUID();node.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(level instanceof ServerLevel server){if(!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;if(level.getBlockEntity(pos) instanceof Node node)player.openMenu(node);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos> {
        private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);public UUID owner;public int progress,amplification,status,mode;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(1_000_000,8192,0,this::setChanged);
        public final FluidTank tank;
        private final ContainerData data=new ContainerData(){public int get(int id){return switch(id){case 0->List.of("mass_fabricator","matter_scanner","pattern_storage","matter_replicator","solid_canner","fluid_bottler").indexOf(kind());case 1->progress/100;case 2->(int)(energy.getAmount()/100);case 3->tank.millibuckets();case 4->status;case 5->mode;default->0;};}public void set(int id,int value){}public int getCount(){return 6;}};
        public Node(BlockPos pos,BlockState state){super(ENTITY,pos,state);tank=new FluidTank(16000,!kind().equals("mass_fabricator"),v->kind().equals("fluid_bottler")||v.isOf(MATTER.source()),this::setChanged){@Override protected boolean canExtract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid){return kind().equals("fluid_bottler")||super.canExtract(fluid);}};}
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}
        @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
        @Override protected NonNullList<ItemStack> getItems(){return items;}
        @Override protected void setItems(NonNullList<ItemStack> values){items=values;}
        @Override public int getContainerSize(){return 9;}
        @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new Menu(id,inventory,worldPosition,this,data);}
        @Override public boolean stillValid(Player player){return super.stillValid(player)&&(!(level instanceof ServerLevel server)||Authority.mayChange(server,player,worldPosition));}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return switch(kind()){case "mass_fabricator"->slot==0&&(stack.is(IndustrialProduction.ITEMS.get("jimta_scrap"))||stack.is(IndustrialProduction.ITEMS.get("jimta_scrapbox")));case "matter_scanner"->slot==0&&cost(stack)>0||slot==1&&stack.is(MEMORY)&&remembered(stack).isEmpty();case "matter_replicator"->slot==0&&stack.is(MEMORY);case "solid_canner"->slot==0&&stack.has(DataComponents.FOOD)&&!stack.is(IndustrialProduction.ITEMS.get("jimta_filled_tin_can"))||slot==1&&stack.is(IndustrialProduction.ITEMS.get("jimta_tin_can"));case "fluid_bottler"->slot==0&&FluidStorage.ITEM.find(stack,net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack))!=null;default->stack.is(MEMORY);};}
        @Override public int[] getSlotsForFace(Direction side){return kind().equals("pattern_storage")?new int[]{0,1,2,3,4,5,6,7,8}:side==Direction.DOWN?new int[]{4,5,6,7,8}:new int[]{0,1};}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return canPlaceItem(slot,stack);}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return kind().equals("pattern_storage")||slot>=4;}
        private boolean room(ItemStack stack){return room(4,stack);}
        private boolean room(int slot,ItemStack stack){if(stack.isEmpty())return true;var out=items.get(slot);return out.isEmpty()||ItemStack.isSameItemSameComponents(out,stack)&&out.getCount()+stack.getCount()<=out.getMaxStackSize();}
        private void give(ItemStack stack){give(4,stack);}
        private void give(int slot,ItemStack stack){if(stack.isEmpty())return;if(items.get(slot).isEmpty())items.set(slot,stack.copy());else items.get(slot).grow(stack.getCount());}
        private ItemStack pattern(ServerLevel level){var own=remembered(items.get(0));if(!own.isEmpty())return own;var actor=Authority.answering(level,owner);if(actor==null)return ItemStack.EMPTY;
            for(Direction side:Direction.values()){var pos=worldPosition.relative(side);if(level.hasChunkAt(pos)&&ClaimTransfers.allowed(level,worldPosition,pos)&&Authority.mayChange(level,actor,pos)&&level.getBlockEntity(pos) instanceof Node node&&node.kind().equals("pattern_storage"))for(ItemStack card:node.items){var target=remembered(card);if(!target.isEmpty())return target;}}return ItemStack.EMPTY;}
        public void step(ServerLevel level){
            status=0;if(level.hasNeighborSignal(worldPosition)){status=1;return;}
            if(kind().equals("pattern_storage"))return;
            if(kind().equals("solid_canner")){canFood();return;}
            if(kind().equals("fluid_bottler")){bottle();return;}
            if(kind().equals("mass_fabricator")){
                if(!tank.fits(MATTER.source(),1)){status=2;return;}int spent=(int)Math.min(4000,energy.getAmount());if(spent==0){status=3;return;}
                if(amplification<=0&&canPlaceItem(0,items.get(0))){amplification=items.get(0).is(IndustrialProduction.ITEMS.get("jimta_scrapbox"))?54000:6000;items.get(0).shrink(1);}
                int boosted=Math.min(spent,amplification);amplification-=boosted;progress+=spent+boosted*5;energy.setAmount(energy.getAmount()-spent);
                if(progress>=1_000_000){progress-=1_000_000;tank.fill(MATTER.source(),1);}status=4;
            }else if(kind().equals("matter_scanner")){
                var sample=items.get(0);if(cost(sample)<=0||!items.get(1).is(MEMORY)||!remembered(items.get(1)).isEmpty()){status=5;progress=0;return;}
                var card=memory(sample);if(!room(card)){status=2;return;}if(energy.getAmount()<1000){status=3;return;}energy.setAmount(energy.getAmount()-1000);progress+=1000;
                if(progress>=10000){give(card);sample.shrink(1);items.get(1).shrink(1);progress=0;}status=4;
            }else{
                ItemStack target=pattern(level);int price=cost(target);if(price<=0){status=5;return;}if(!room(target)){status=2;return;}if(!tank.has(MATTER.source(),price)){status=6;return;}if(energy.getAmount()<4000){status=3;return;}
                // Output permission is re-evaluated every operation, including after tag/datapack changes.
                tank.drain(price);energy.setAmount(energy.getAmount()-4000);give(target);status=4;
            }setChanged();
        }
        private void canFood(){
            var input=items.get(0);var food=input.get(DataComponents.FOOD);if(food==null||input.is(IndustrialProduction.ITEMS.get("jimta_filled_tin_can"))||!items.get(1).is(IndustrialProduction.ITEMS.get("jimta_tin_can"))){status=5;return;}
            var canned=new ItemStack(IndustrialProduction.ITEMS.get("jimta_filled_tin_can"));canned.set(DataComponents.FOOD,food);canned.set(DataComponents.ITEM_NAME,Component.literal("Canned ").append(input.getHoverName()));
            if(input.has(DataComponents.CONSUMABLE))canned.set(DataComponents.CONSUMABLE,input.get(DataComponents.CONSUMABLE));
            if(input.has(DataComponents.SUSPICIOUS_STEW_EFFECTS))canned.set(DataComponents.SUSPICIOUS_STEW_EFFECTS,input.get(DataComponents.SUSPICIOUS_STEW_EFFECTS));
            var useRemainder=input.get(DataComponents.USE_REMAINDER);var craftingRemainder=input.getItem().getCraftingRemainder();
            ItemStack remainder=useRemainder!=null?useRemainder.convertInto().create():craftingRemainder!=null?craftingRemainder.create():ItemStack.EMPTY;
            if(!room(canned)||!room(5,remainder)){status=2;return;}if(energy.getAmount()<400){status=3;return;}
            energy.setAmount(energy.getAmount()-400);input.shrink(1);items.get(1).shrink(1);give(canned);give(5,remainder);status=4;setChanged();
        }
        private void bottle(){
            ItemStack input=items.get(0);if(input.isEmpty()){status=5;return;}if(energy.getAmount()<400){status=3;return;}
            var temporary=new SimpleContainer(input.copyWithCount(1));var context=net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofSingleSlot(ContainerStorage.of(temporary,null).getSlot(0));var container=context.find(FluidStorage.ITEM);if(container==null){status=5;return;}
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                long moved=mode==0?net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(tank,container,v->true,Long.MAX_VALUE,tx):net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(container,tank,v->true,Long.MAX_VALUE,tx);
                if(moved==0){status=6;return;}ItemStack result=temporary.getItem(0).copy();if(result.isEmpty()||!room(result)){status=2;return;}tx.commit();input.shrink(1);give(result);energy.setAmount(energy.getAmount()-400);status=4;setChanged();
            }
        }
        @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level!=null)Containers.dropContents(level,pos,this);clearContent();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);tank.writeValue(out.child("Matter"));out.putInt("Progress",progress);out.putInt("Amplification",amplification);out.putInt("Mode",mode);out.putLong("Energy",energy.getAmount());if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);tank.readValue(in.childOrEmpty("Matter"));progress=Math.clamp(in.getIntOr("Progress",0),0,1_000_000);amplification=Math.clamp(in.getIntOr("Amplification",0),0,54000);mode=Math.clamp(in.getIntOr("Mode",0),0,1);energy.setAmount(in.getLongOr("Energy",0));owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);}
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Container inventory;public final ContainerData data;public final BlockPos pos;
        public Menu(int id,Inventory player,BlockPos pos){this(id,player,pos,new SimpleContainer(9),new SimpleContainerData(6));}
        Menu(int id,Inventory player,BlockPos pos,Container inventory,ContainerData data){super(MENU,id);this.inventory=inventory;this.data=data;this.pos=pos;addDataSlots(data);
            for(int i=0;i<9;i++){final int n=i;addSlot(new Slot(inventory,i,19+i%3*18,38+i/3*18){@Override public boolean mayPlace(ItemStack stack){return inventory.canPlaceItem(n,stack);}});}
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,8+col*18,126+row*18));for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,184));
        }
        @Override public boolean stillValid(Player player){return player.level().isClientSide()||player.level().getBlockEntity(pos)==inventory&&inventory.stillValid(player);}
        @Override public boolean clickMenuButton(Player player,int id){if(id!=0||!stillValid(player)||!(player instanceof ServerPlayer server)||!Authority.mayChange(server.level(),player,pos)||!(inventory instanceof Node node)||!node.kind().equals("fluid_bottler"))return false;node.mode=1-node.mode;node.setChanged();return true;}
        @Override public ItemStack quickMoveStack(Player player,int slot){if(!stillValid(player)||slot<0||slot>=slots.size())return ItemStack.EMPTY;var from=slots.get(slot);if(!from.hasItem())return ItemStack.EMPTY;var held=from.getItem();var original=held.copy();if(slot<9?!moveItemStackTo(held,9,45,true):!moveItemStackTo(held,0,9,false))return ItemStack.EMPTY;if(held.isEmpty())from.set(ItemStack.EMPTY);else from.setChanged();return original;}
    }
}
