package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import net.minecraft.world.entity.LivingEntity;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import java.util.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
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

/** Original component-grid reactor, sharing JE and radiation; bounded to one 54-slot pass per second. */
public final class CompactReactor {
    public record Spec(String kind,int cells,int capacity,int selfCooling,int hullTransfer,int neighborTransfer,int plating,int lifetime){}
    public static final Map<String,Part> PARTS=new LinkedHashMap<>();
    public static Block CORE,CHAMBER;
    public static BlockEntityType<Core> ENTITY;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    private CompactReactor(){}
    public static void register(){
        CORE=block("jimta_compact_reactor",true);CHAMBER=block("jimta_reactor_chamber",false);
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_compact_reactor"),FabricBlockEntityTypeBuilder.create(Core::new,CORE).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_compact_reactor"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity((core,side)->core.energy,ENTITY);
        try(var stream=CompactReactor.class.getClassLoader().getResourceAsStream("jugcraft/jimtafacts/reactor.json")){
            if(stream==null)throw new IllegalStateException("Missing reactor catalog");
            for(var entry:JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().entrySet()){
                var v=entry.getValue().getAsJsonObject();var s=new Spec(v.get("kind").getAsString(),v.get("cells").getAsInt(),v.get("capacity").getAsInt(),v.get("self").getAsInt(),v.get("hull").getAsInt(),v.get("neighbor").getAsInt(),v.get("plating").getAsInt(),v.get("life").getAsInt());
                var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(entry.getKey()));var p=new Item.Properties().setId(key).stacksTo(1);
                if(s.kind.equals("uranium")||s.kind.equals("mox")||s.kind.equals("spent")||s.kind.equals("plutonium")||s.kind.equals("rtg"))p.component(JugcraftNuclear.RADIOACTIVITY,s.kind.equals("spent")?5F:1F);
                PARTS.put(entry.getKey(),Registry.register(BuiltInRegistries.ITEM,key,new Part(p,s)));
            }
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        ReactorVessel.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{out.accept(CORE);out.accept(CHAMBER);PARTS.values().forEach(out::accept);});
    }
    private static Block block(String id,boolean core){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var properties=BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).strength(5F,20F);
        Block block=core?new ReactorBlock(properties):new Block(properties);Registry.register(BuiltInRegistries.BLOCK,key,block);var ik=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));return block;}
    public static final class Part extends Item {
        public final Spec spec;Part(Properties p,Spec spec){super(p);this.spec=spec;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            super.appendHoverText(stack,context,display,lines,flag);int heat=stored(stack,"Heat"),life=stored(stack,"Used");
            if(spec.capacity>0)lines.accept(Component.literal("Stored heat: "+heat+" / "+spec.capacity));
            if(spec.lifetime>0)lines.accept(Component.literal("Remaining: "+Math.max(0,spec.lifetime-life)+" reactor seconds"));
            if(spec.cells>0)lines.accept(Component.literal(spec.cells+" fuel cell(s); adjacent fuel or reflectors increase output and heat."));
            if(spec.plating>0)lines.accept(Component.literal("Hull heat capacity +"+spec.plating));
            if(spec.selfCooling>0||spec.hullTransfer>0||spec.neighborTransfer>0)lines.accept(Component.literal("Cooling "+spec.selfCooling+" / hull transfer "+spec.hullTransfer+" / neighbor transfer "+spec.neighborTransfer));
        }
    }
    public static int stored(ItemStack stack,String key){return Math.max(0,stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0));}
    private static void store(ItemStack stack,String key,int value){CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putInt(key,Math.max(0,value)));}
    private static Spec spec(ItemStack stack){return stack.getItem() instanceof Part p?p.spec:null;}
    private static boolean fuel(Spec s){return s!=null&&(s.kind.equals("uranium")||s.kind.equals("mox"));}
    private static final class ReactorBlock extends Block implements EntityBlock,EnergyConnectable {
        ReactorBlock(Properties p){super(p);}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){if(placer!=null&&l.getBlockEntity(p) instanceof Core core){core.owner=placer.getUUID();core.setChanged();}}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Core(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide()&&type==ENTITY?(l,p,s,e)->{if(l.getGameTime()%20==0)((Core)e).step((ServerLevel)l);}:null;}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){if(level instanceof ServerLevel server){if(!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;if(level.getBlockEntity(pos) instanceof Core core){if(core.owner==null){core.owner=player.getUUID();core.setChanged();}player.openMenu(core);}}return InteractionResult.SUCCESS;}
    }
    public static final class Core extends BaseContainerBlockEntity implements ExtendedMenuProvider<BlockPos>,WorldlyContainer {
        private NonNullList<ItemStack> grid=NonNullList.withSize(54,ItemStack.EMPTY);
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(1_000_000,0,8192,this::setChanged);
        public int heat,maximumHeat=10000,output,columns=3;public boolean enabled,fault,fluidMode;public UUID owner;
        public final FluidTank coolant=new FluidTank(16000,true,v->v.isOf(IndustrialHeat.COOLANT.source()),this::setChanged);
        public final FluidTank hotCoolant=new FluidTank(16000,false,v->true,this::setChanged);
        @Override public int[] getSlotsForFace(Direction side){if(enabled)return new int[0];return java.util.stream.IntStream.range(0,54).filter(i->i%9<columns).toArray();}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return !enabled;}

        private final ContainerData data=new ContainerData(){
            public int get(int index){return switch(index){case 0->heat/10;case 1->maximumHeat/10;case 2->output/5;case 3->columns;case 4->enabled?1:0;case 5->fault?1:0;case 6->(int)(energy.getAmount()/100);case 7->fluidMode?1:0;case 8->coolant.millibuckets();case 9->hotCoolant.millibuckets();default->0;};}
            public void set(int index,int value){}public int getCount(){return 10;}};
        public Core(BlockPos pos,BlockState state){super(ENTITY,pos,state);}
        @Override protected Component getDefaultName(){return Component.literal("Compact Component Reactor");}
        @Override protected NonNullList<ItemStack> getItems(){return grid;}
        @Override protected void setItems(NonNullList<ItemStack> list){grid=list;}
        @Override public int getContainerSize(){return 54;}
        @Override public int getMaxStackSize(){return 1;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return !enabled&&slot%9<columns&&spec(stack)!=null&&!spec(stack).kind.equals("plutonium")&&!spec(stack).kind.equals("rtg");}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new Menu(id,inventory,worldPosition,this,data);}
        @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
        @Override public boolean stillValid(Player player){return super.stillValid(player)&&(!(level instanceof ServerLevel server)||Authority.mayChange(server,player,worldPosition));}
        private List<Integer> neighbors(int slot){int x=slot%9,y=slot/9;var list=new ArrayList<Integer>(4);if(x>0)list.add(slot-1);if(x+1<columns)list.add(slot+1);if(y>0)list.add(slot-9);if(y<5)list.add(slot+9);return list;}
        private int addHeat(int slot,int amount){var stack=grid.get(slot);var s=spec(stack);if(s==null||s.capacity<=0)return amount;int prior=stored(stack,"Heat"),after=Math.clamp(prior+amount,0,s.capacity);store(stack,"Heat",after);return amount-(after-prior);}
        private void distribute(int slot,int generated){var sinks=neighbors(slot).stream().filter(n->{var s=spec(grid.get(n));return s!=null&&s.capacity>0;}).toList();int left=generated;for(int i=0;i<sinks.size();i++){int offer=(left+sinks.size()-i-1)/(sinks.size()-i);left-=offer-addHeat(sinks.get(i),offer);}heat+=left;}
        private void wear(int slot,int amount){ItemStack stack=grid.get(slot);var s=spec(stack);if(s==null||s.lifetime<=0)return;int used=stored(stack,"Used")+amount;if(used<s.lifetime){store(stack,"Used",used);return;}
            if(fuel(s)){String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()+"_depleted";grid.set(slot,new ItemStack(PARTS.get(id)));}else grid.set(slot,ItemStack.EMPTY);}
        public void step(ServerLevel level){
            var vessel=ReactorVessel.inspect(level,this);
            if(fluidMode&&!vessel.complete()){enabled=false;fault=true;}
            // Building the shell switches to heat production; opening it forces a deliberate manual restart.
            fluidMode=vessel.complete();
            columns=3;for(Direction d:Direction.values()){var at=worldPosition.relative(d);if(level.hasChunkAt(at)&&level.getBlockState(at).is(CHAMBER)&&ClaimTransfers.allowed(level,worldPosition,at))columns++;}
            maximumHeat=10000;boolean blocked=false;
            for(int slot=0;slot<54;slot++){var s=spec(grid.get(slot));if(slot%9>=columns&&!grid.get(slot).isEmpty())blocked=true;else if(s!=null)maximumHeat+=s.plating;}
            if(blocked||heat>=maximumHeat){enabled=false;fault=true;}
            output=0;boolean run=enabled&&!fault&&!level.hasNeighborSignal(worldPosition)&&!vessel.redstone()
                &&(fluidMode?coolant.has(IndustrialHeat.COOLANT.source(),1)&&hotCoolant.fits(IndustrialHeat.HOT_COOLANT.source(),1):energy.getAmount()<energy.getCapacity());
            if(run)for(int slot=0;slot<54;slot++){
                var stack=grid.get(slot);var s=spec(stack);if(slot%9>=columns||!fuel(s))continue;
                int pulses=1+s.cells/2;
                for(int n:neighbors(slot)){var other=spec(grid.get(n));if(fuel(other))pulses++;else if(other!=null&&other.kind.equals("reflector")){pulses++;wear(n,s.cells);}}
                int multiplier=s.kind.equals("mox")?100+400*Math.min(heat,maximumHeat)/maximumHeat:100;
                output+=5*s.cells*pulses*multiplier/100;
                distribute(slot,s.cells*2*pulses*(pulses+1));wear(slot,1);
                for(int n:neighbors(slot))if(spec(grid.get(n))!=null&&spec(grid.get(n)).kind.equals("lithium")){var lithium=grid.get(n);int used=stored(lithium,"Used")+s.cells;store(lithium,"Used",used);if(used>=10000)grid.set(n,new ItemStack(PARTS.get("jimta_tritium_cell")));}
            }
            if(fluidMode)heat+=output*40;
            // Cooling continues while stopped, making shutdown useful rather than erasing heat.
            for(int slot=0;slot<54;slot++){
                var stack=grid.get(slot);var s=spec(stack);if(slot%9>=columns||s==null)continue;
                if(s.kind.equals("vent")){int take=Math.min(heat,s.hullTransfer);heat-=take-addHeat(slot,take);addHeat(slot,-s.selfCooling);}
                if(s.kind.equals("spread_vent"))for(int n:neighbors(slot))addHeat(n,-s.neighborTransfer);
                if(s.kind.equals("exchanger")){
                    int own=stored(stack,"Heat");int hullTarget=heat*s.capacity/maximumHeat;int transfer=Math.clamp(hullTarget-own,-s.hullTransfer,s.hullTransfer);if(transfer>0)transfer=Math.min(transfer,heat);heat-=transfer-addHeat(slot,transfer);
                    for(int n:neighbors(slot)){var other=spec(grid.get(n));if(other==null||other.capacity<=0)continue;own=stored(stack,"Heat");int otherHeat=stored(grid.get(n),"Heat"),target=(int)((long)otherHeat*s.capacity/other.capacity),move=Math.clamp(target-own,-s.neighborTransfer,s.neighborTransfer);
                        if(move>0){move=Math.min(move,otherHeat);int accepted=move-addHeat(slot,move);addHeat(n,-accepted);}else{move=Math.min(-move,own);int accepted=move-addHeat(n,move);addHeat(slot,-accepted);}}
                }
                if(s.capacity>0&&stored(stack,"Heat")>=s.capacity&&!s.kind.equals("condensator")){grid.set(slot,ItemStack.EMPTY);fault=true;enabled=false;}
            }
            if(fluidMode){
                int amount=Math.min(2048,Math.min(heat/4,Math.min(coolant.millibuckets(),hotCoolant.capacityMb()-hotCoolant.millibuckets())));
                if(amount>0&&coolant.has(IndustrialHeat.COOLANT.source(),amount)&&hotCoolant.fits(IndustrialHeat.HOT_COOLANT.source(),amount)){
                    coolant.drain(amount);hotCoolant.fill(IndustrialHeat.HOT_COOLANT.source(),amount);heat-=amount*4;
                }
            }
            heat=Math.max(0,heat);if(heat>=maximumHeat){fault=true;enabled=false;}
            if(!fluidMode)energy.setAmount(energy.getAmount()+output*20L);setChanged();
        }
        public boolean toggle(){if(enabled){enabled=false;setChanged();return true;}if(heat>=maximumHeat/2)return false;for(int i=0;i<54;i++)if(i%9>=columns&&!grid.get(i).isEmpty())return false;fault=false;enabled=true;setChanged();return true;}
        @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level!=null)Containers.dropContents(level,pos,this);clearContent();energy.setAmount(0);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,grid);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);out.putBoolean("FluidMode",fluidMode);coolant.writeValue(out.child("Coolant"));hotCoolant.writeValue(out.child("HotCoolant"));out.putInt("Heat",heat);out.putBoolean("Enabled",enabled);out.putBoolean("Fault",fault);out.putLong("Energy",energy.getAmount());}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);grid=NonNullList.withSize(54,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,grid);owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);fluidMode=in.getBooleanOr("FluidMode",false);coolant.readValue(in.childOrEmpty("Coolant"));hotCoolant.readValue(in.childOrEmpty("HotCoolant"));heat=Math.clamp(in.getIntOr("Heat",0),0,1_000_000);enabled=in.getBooleanOr("Enabled",false);fault=in.getBooleanOr("Fault",false);energy.setAmount(in.getLongOr("Energy",0));}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final BlockPos pos;private final Container core;public final ContainerData data;
        public Menu(int id,Inventory inventory,BlockPos pos){this(id,inventory,pos,new SimpleContainer(54),new SimpleContainerData(10));}
        Menu(int id,Inventory inventory,BlockPos pos,Container core,ContainerData data){super(MENU,id);this.pos=pos;this.core=core;this.data=data;addDataSlots(data);
            for(int row=0;row<6;row++)for(int col=0;col<9;col++){final int column=col;addSlot(new Slot(core,row*9+col,26+18*col,25+18*row){@Override public boolean mayPlace(ItemStack stack){return data.get(4)==0&&column<data.get(3)&&spec(stack)!=null&&!spec(stack).kind.equals("plutonium")&&!spec(stack).kind.equals("rtg");}@Override public boolean mayPickup(Player p){return data.get(4)==0;}@Override public int getMaxStackSize(){return 1;}});}
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,26+18*col,161+18*row));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,26+18*col,219));
        }
        @Override public boolean stillValid(Player player){return player.level().isClientSide()||player.level().getBlockEntity(pos)==core&&core.stillValid(player);}
        @Override public boolean clickMenuButton(Player player,int id){return id==0&&player instanceof ServerPlayer server&&stillValid(player)&&Authority.mayChange(server.level(),player,pos)&&core instanceof Core c&&c.toggle();}
        @Override public ItemStack quickMoveStack(Player player,int slot){if(!stillValid(player)||slot<0||slot>=slots.size()||data.get(4)!=0)return ItemStack.EMPTY;Slot from=slots.get(slot);if(!from.hasItem())return ItemStack.EMPTY;ItemStack stack=from.getItem(),copy=stack.copy();if(slot<54?!moveItemStackTo(stack,54,90,true):!moveItemStackTo(stack,0,54,false))return ItemStack.EMPTY;if(stack.isEmpty())from.set(ItemStack.EMPTY);else from.setChanged();return copy;}
    }
}
