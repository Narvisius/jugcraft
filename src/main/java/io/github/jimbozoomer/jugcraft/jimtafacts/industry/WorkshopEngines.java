package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.kinetic.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
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

/** Source engine silhouettes/animation on finite native JE, fuel and KE. Hot engines stop instead of exploding. */
public final class WorkshopEngines {
    public static final List<String> KINDS=List.of("redstone_engine","stirling_engine","combustion_engine","electric_engine","creative_engine","kinetic_dynamo");
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();public static Item ASHES;
    public static BlockEntityType<Node> TYPE;public static ExtendedMenuType<Menu,BlockPos> MENU;
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,4);
    private WorkshopEngines(){}
    public static void register(){
        for(String name:KINDS){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+name));var block=Registry.register(BuiltInRegistries.BLOCK,key,new Engine(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),name));BLOCKS.put(name,block);
            var ik=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));}
        var ash=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_ashes"));ASHES=Registry.register(BuiltInRegistries.ITEM,ash,new Item(new Item.Properties().setId(ash)));
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_engine"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_engine"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity(Node::powerPort,TYPE);FluidStorage.SIDED.registerForBlockEntity(Node::fluidPort,TYPE);
        ItemStorage.SIDED.registerForBlockEntity((n,side)->n.kind().equals("stirling_engine")?ContainerStorage.of(n,side):null,TYPE);
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,s,be)->!(be instanceof Node n)||n.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{BLOCKS.values().forEach(out::accept);out.accept(ASHES);});
    }
    static final class Engine extends Block implements EntityBlock,EnergyConnectable {
        final String kind;Engine(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP).setValue(LIT,false).setValue(STAGE,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,LIT,STAGE);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){if(kind.equals("creative_engine")&&(c.getPlayer()==null||!c.getPlayer().isCreative()))return null;return defaultBlockState().setValue(FACING,c.getClickedFace());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return !l.isClientSide()&&t==TYPE?(world,p,state,e)->((Node)e).step():null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){if(actor!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=actor.getUUID();n.setChanged();}}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){EnergyNetworks.invalidate(l);KineticNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){EnergyNetworks.invalidate(l);KineticNetworks.invalidate(l);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(l.getBlockEntity(p) instanceof Node n)||!n.permitted(player))return InteractionResult.FAIL;
            if(n.kind().equals("combustion_engine")&&FluidStorage.ITEM.find(stack,net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack))!=null){if(!l.isClientSide())FluidStorageUtil.interactWithFluidStorage(n.fluidPort(hit.getDirection()),player,hand);return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!(l.getBlockEntity(p) instanceof Node n)||!n.permitted(player))return InteractionResult.FAIL;if(player instanceof ServerPlayer sp){if(n.owner==null){n.owner=sp.getUUID();n.setChanged();}sp.openMenu(n);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos>,KineticConsumer {
        public UUID owner;private NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(32000,256,2048,this::setChanged);
        public final FluidTank fuel=new FluidTank(8000,true,v->FluidFuels.jePerMb(MachineKind.DIESEL_GENERATOR,v.getFluid())>0,this::setChanged);
        public final FluidTank water=new FluidTank(8000,true,v->v.isOf(Fluids.WATER)||IndustrialFluidServices.DISTILLED!=null&&v.isOf(IndustrialFluidServices.DISTILLED.source()),this::setChanged);
        public int burn,temperature,kinetic,conversionRemainder;public boolean tripped;private long lastStep=Long.MIN_VALUE,lastKinetic=Long.MIN_VALUE;private int receivedKinetic;
        private final PowerQuota powerQuota=new PowerQuota();
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}public String kind(){return ((Engine)getBlockState().getBlock()).kind;}
        public Direction facing(){return getBlockState().getValue(FACING);}
        boolean identity(Player p){return owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID());}
        public boolean permitted(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&identity(p)&&Authority.mayChange(level,p,worldPosition);}
        boolean automation(){var p=level instanceof ServerLevel s?Authority.answering(s,owner):null;return p!=null&&permitted(p)&&(!kind().equals("creative_engine")||p.isCreative());}
        public EnergyStorage powerPort(Direction side){return new EnergyStorage(){
            public boolean supportsInsertion(){return kind().equals("electric_engine")&&side!=null&&side!=facing();}public boolean supportsExtraction(){return side==facing();}
            public long insert(long max,TransactionContext tx){if(!supportsInsertion()||!automation()||max<=0)return 0;long moved=energy.insert(Math.min(max,powerQuota.remaining(true)),tx);if(moved>0)powerQuota.use(true,moved,tx);return moved;}
            public long extract(long max,TransactionContext tx){if(!supportsExtraction()||!automation()||max<=0||!level.hasNeighborSignal(worldPosition)||tripped)return 0;long moved=energy.extract(Math.min(max,powerQuota.remaining(false)),tx);if(moved>0)powerQuota.use(false,moved,tx);return moved;}
            public long getAmount(){return energy.getAmount();}public long getCapacity(){return energy.getCapacity();}public Object transferIdentity(){return energy;}
        };}
        private final class PowerQuota extends SnapshotParticipant<long[]> {
            private long tick=Long.MIN_VALUE,in,out;
            long remaining(boolean input){long used=tick==level.getGameTime()?(input?in:out):0;return Math.max(0,(input||!kind().equals("creative_engine")?256:2048)-used);}
            void use(boolean input,long amount,TransactionContext tx){updateSnapshots(tx);if(tick!=level.getGameTime()){tick=level.getGameTime();in=out=0;}if(input)in+=amount;else out+=amount;}
            protected long[] createSnapshot(){return new long[]{tick,in,out};}protected void readSnapshot(long[] s){tick=s[0];in=s[1];out=s[2];}
        }
        public Storage<FluidVariant> fluidPort(Direction side){if(!kind().equals("combustion_engine"))return null;var tank=side==Direction.DOWN?water:fuel;return new SingleSlotStorage<>(){
            public long insert(FluidVariant v,long max,TransactionContext tx){return automation()?tank.insert(v,max,tx):0;}public long extract(FluidVariant v,long max,TransactionContext tx){return 0;}
            public boolean supportsExtraction(){return false;}public boolean isResourceBlank(){return tank.variant.isBlank();}public FluidVariant getResource(){return tank.variant;}public long getAmount(){return tank.amount;}public long getCapacity(){return tank.getCapacity();}
        };}
        @Override public long acceptKinetic(Direction side,long max){if(!kind().equals("kinetic_dynamo")||side!=facing().getOpposite()||!automation()||!level.hasNeighborSignal(worldPosition)||max<=0)return 0;
            if(lastKinetic!=level.getGameTime()){lastKinetic=level.getGameTime();receivedKinetic=0;}int accepted=(int)Math.min(max,Math.min(64-receivedKinetic,4096-kinetic));if(accepted>0){kinetic+=accepted;receivedKinetic+=accepted;setChanged();}return Math.max(0,accepted);}
        public void step(){if(!(level instanceof ServerLevel s)||lastStep==s.getGameTime())return;lastStep=s.getGameTime();boolean changed=false,working=false;
            boolean enabled=automation()&&s.hasNeighborSignal(worldPosition)&&!tripped;
            long room=energy.getCapacity()-energy.getAmount();
            if(enabled&&room>0)switch(kind()){
                case "redstone_engine"->{if(s.getGameTime()%20==0){energy.setAmount(energy.getAmount()+1);working=changed=true;}}
                case "stirling_engine"->{if(room>=4){if(burn<=0&&GeneratorFuels.burnTicks(items.get(0))>0&&(items.get(1).isEmpty()||items.get(1).is(ASHES)&&items.get(1).getCount()<64)){
                        burn=GeneratorFuels.burnTicks(items.get(0));items.get(0).shrink(1);if(items.get(1).isEmpty())items.set(1,new ItemStack(ASHES));else items.get(1).grow(1);}
                    if(burn>0){burn--;energy.setAmount(energy.getAmount()+4);working=changed=true;}}}
                case "combustion_engine"->{int value=fuel.variant.isBlank()?0:FluidFuels.jePerMb(MachineKind.DIESEL_GENERATOR,fuel.variant.getFluid());
                    if(value>0&&fuel.millibuckets()>=1&&room>=value){fuel.drain(1);energy.setAmount(energy.getAmount()+value);working=changed=true;}}
                case "electric_engine"->working=energy.getAmount()>0;
                case "creative_engine"->{energy.setAmount(Math.min(energy.getCapacity(),energy.getAmount()+2048));working=changed=true;}
                case "kinetic_dynamo"->{int use=Math.min(64,kinetic);int generated=(use*85+conversionRemainder)/100;if(use>0&&generated<=room){conversionRemainder=(use*85+conversionRemainder)%100;kinetic-=use;energy.setAmount(energy.getAmount()+generated);working=changed=true;}}
                default->{}
            }
            if(s.getGameTime()%20==0){if(kind().equals("combustion_engine")){if(water.millibuckets()>0&&temperature>0){water.drain(1);temperature=Math.max(0,temperature-4);changed=true;}temperature=Math.clamp(temperature+(working?2:-1),0,100);if(temperature>=100)tripped=true;else if(temperature<=40)tripped=false;}
                else temperature=Math.clamp(temperature+(working?1:-2),0,60);changed=true;}
            long moved=enabled?EnergyNetworks.pushToNeighbors(s,worldPosition,powerPort(facing()),kind().equals("creative_engine")?2048:256,List.of(facing())):0;
            boolean active=!tripped&&enabled&&(working||moved>0||kind().equals("redstone_engine")&&room>0);int stage=tripped?4:Math.clamp(temperature/25,0,3);
            if(getBlockState().getValue(LIT)!=active||getBlockState().getValue(STAGE)!=stage)s.setBlockAndUpdate(worldPosition,getBlockState().setValue(LIT,active).setValue(STAGE,stage));if(changed||moved>0)setChanged();
        }
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&permitted(p);}
        @Override public int getContainerSize(){return 2;}@Override protected NonNullList<ItemStack> getItems(){return items;}@Override protected void setItems(NonNullList<ItemStack> list){items=list;}
        @Override public boolean canPlaceItem(int i,ItemStack stack){return i==0&&kind().equals("stirling_engine")&&GeneratorFuels.burnTicks(stack)>0;}
        @Override public int[] getSlotsForFace(Direction d){return d==Direction.DOWN?new int[]{1}:new int[]{0};}
        @Override public boolean canPlaceItemThroughFace(int i,ItemStack stack,Direction d){return automation()&&canPlaceItem(i,stack);}@Override public boolean canTakeItemThroughFace(int i,ItemStack stack,Direction d){return automation()&&i==1;}
        @Override protected Component getDefaultName(){return Component.translatable("block.jugcraft.jimta_"+kind());}public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->KINDS.indexOf(kind());case 1->(int)energy.getAmount();case 2->burn;case 3->fuel.millibuckets();case 4->water.millibuckets();case 5->temperature;case 6->tripped?1:0;case 7->kinetic;case 8->level!=null&&level.hasNeighborSignal(worldPosition)?1:0;default->0;};}public void set(int i,int v){}public int getCount(){return 9;}};
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new Menu(id,inv,worldPosition,this,data);}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level!=null&&!level.isClientSide()){Containers.dropContents(level,p,this);clearContent();}}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putLong("energy",energy.getAmount());out.putInt("burn",burn);out.putInt("heat",temperature);out.putBoolean("tripped",tripped);out.putInt("kinetic",kinetic);out.putInt("remainder",conversionRemainder);fuel.writeValue(out.child("fuel"));water.writeValue(out.child("water"));}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(2,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);energy.setAmount(in.getLongOr("energy",0));burn=Math.clamp(in.getIntOr("burn",0),0,16000);temperature=Math.clamp(in.getIntOr("heat",0),0,100);tripped=in.getBooleanOr("tripped",false);kinetic=Math.clamp(in.getIntOr("kinetic",0),0,4096);conversionRemainder=Math.clamp(in.getIntOr("remainder",0),0,99);fuel.readValue(in.childOrEmpty("fuel"));water.readValue(in.childOrEmpty("water"));}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final Container contents;public final ContainerData data;private final BlockPos pos;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,new SimpleContainer(2),new SimpleContainerData(9));}
        Menu(int id,Inventory inv,BlockPos pos,Container contents,ContainerData data){super(MENU,id);this.pos=pos;this.contents=contents;this.data=data;addDataSlots(data);
            addSlot(new Slot(contents,0,8,42){public boolean mayPlace(ItemStack s){return data.get(0)==1&&GeneratorFuels.burnTicks(s)>0;}public boolean isActive(){return data.get(0)==1;}});addSlot(new Slot(contents,1,8,62){public boolean mayPlace(ItemStack s){return false;}public boolean isActive(){return data.get(0)==1;}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,94+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,152));}
        public boolean stillValid(Player p){return p.level().isClientSide()||p.level().getBlockEntity(pos)==contents&&contents.stillValid(p);}
        @Override public void clicked(int i,int b,ContainerInput type,Player p){if(stillValid(p))super.clicked(i,b,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);var s=slot.getItem();var before=s.copy();if(s.isEmpty())return ItemStack.EMPTY;
            boolean moved=i<2?moveItemStackTo(s,2,slots.size(),true):data.get(0)==1&&GeneratorFuels.burnTicks(s)>0&&moveItemStackTo(s,0,1,false);if(!moved)return ItemStack.EMPTY;slot.setByPlayer(s.isEmpty()?ItemStack.EMPTY:s);slot.setChanged();return before;}
    }
}
