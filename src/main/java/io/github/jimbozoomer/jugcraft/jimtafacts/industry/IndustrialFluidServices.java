package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
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

/** Supplied industrial housings with transactional native fluid controls and finite recovery. */
public final class IndustrialFluidServices {
    public static final List<String> KINDS=List.of("fluid_regulator","fluid_distributor","solar_distiller","steam_condenser");
    public static final int[] RATES={1,10,50,100,250,500,1000};
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<Node> TYPE;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    public static PetroFluids.Entry DISTILLED;
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    private IndustrialFluidServices(){}
    public static void register(){
        DISTILLED=PetroFluids.registerOwnerLiquid("distilled_water",0xffa0c8db);
        for(String kind:KINDS){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+kind));
            var block=Registry.register(BuiltInRegistries.BLOCK,key,new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key),kind));BLOCKS.put(kind,block);
            var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(block,new Item.Properties().setId(item).useBlockDescriptionPrefix()));}
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_fluid_service"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_fluid_service"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        FluidStorage.SIDED.registerForBlockEntity(Node::port,TYPE);
        EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.energy,TYPE);
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,s,be)->!(be instanceof Node n)||n.identity(p));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{BLOCKS.values().forEach(out::accept);out.accept(DISTILLED.bucket());});
    }
    static final class Device extends Block implements EntityBlock,EnergyConnectable {
        final String kind;
        Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return !l.isClientSide()&&t==TYPE?(world,p,state,e)->((Node)e).step():null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity actor,ItemStack stack){if(actor!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=actor.getUUID();n.setChanged();}}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){EnergyNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){EnergyNetworks.invalidate(l);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(l.getBlockEntity(p) instanceof Node n)||!n.permitted(player))return InteractionResult.FAIL;
            if(FluidStorage.ITEM.find(stack,net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack))==null)return InteractionResult.PASS;
            if(!l.isClientSide())FluidStorageUtil.interactWithFluidStorage(n.port(hit.getDirection()),player,hand);return InteractionResult.SUCCESS;
        }
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(!(l.getBlockEntity(p) instanceof Node n)||!n.permitted(player))return InteractionResult.FAIL;
            if(player instanceof ServerPlayer sp){if(n.owner==null){n.owner=sp.getUUID();n.setChanged();}sp.openMenu(n);}return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
        public UUID owner;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(10000,128,0,this::setChanged);
        // Internal extraction is available only to the machine. External views cannot drain past a regulator's limit.
        public final FluidTank input=new FluidTank(10000,true,this::accepts,this::setChanged){@Override protected boolean canExtract(FluidVariant v){return true;}};
        public final FluidTank output=new FluidTank(10000,false,v->true,this::setChanged);
        public int rateIndex=3;public boolean alternate;private int cursor,solarTicks;private long lastStep=Long.MIN_VALUE;public int status;
        public final ContainerData data=new ContainerData(){
            public int get(int i){return switch(i){case 0->KINDS.indexOf(kind());case 1->input.millibuckets();case 2->output.millibuckets();case 3->(int)energy.getAmount();case 4->rateIndex;case 5->alternate?1:0;case 6->status;case 7->input.variant.isBlank()?0:BuiltInRegistries.FLUID.getId(input.variant.getFluid());default->0;};}
            public void set(int i,int v){}public int getCount(){return 8;}};
        public Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}
        public Direction facing(){return getBlockState().getValue(FACING);}
        public boolean routing(){return kind().equals("fluid_regulator")||kind().equals("fluid_distributor");}
        private boolean accepts(FluidVariant v){if(routing())return true;if(kind().equals("solar_distiller"))return v.isOf(Fluids.WATER);return v.isOf(IndustrialHeat.STEAM.fluid())||v.isOf(IndustrialHeat.SUPERHEATED_STEAM.fluid());}
        boolean identity(Player p){return owner==null||owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID());}
        public boolean permitted(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.isAlive()&&!p.isSpectator()&&identity(p)&&Authority.mayChange(level,p,worldPosition);}
        boolean automation(){var actor=level instanceof ServerLevel s?Authority.answering(s,owner):null;return actor!=null&&permitted(actor);}
        public Storage<FluidVariant> port(Direction side){
            boolean entrance=routing()?side!=facing():side!=Direction.DOWN;
            if(kind().equals("fluid_distributor"))entrance=side==null||side==facing();
            final boolean in=entrance;final FluidTank tank=in?input:output;
            return new SingleSlotStorage<>(){
                public long insert(FluidVariant v,long max,TransactionContext tx){return in&&automation()?tank.insert(v,max,tx):0;}
                public long extract(FluidVariant v,long max,TransactionContext tx){return !in&&!routing()&&automation()?tank.extract(v,max,tx):0;}
                public boolean isResourceBlank(){return tank.variant.isBlank();}public FluidVariant getResource(){return tank.variant;}
                public long getAmount(){return tank.amount;}public long getCapacity(){return tank.getCapacity();}
                public boolean supportsInsertion(){return in;}public boolean supportsExtraction(){return !in&&!routing();}
            };
        }
        public boolean configure(Player p,int id){if(!permitted(p)||p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)>64||!routing())return false;
            if(id==0)rateIndex=Math.max(0,rateIndex-1);else if(id==1)rateIndex=Math.min(RATES.length-1,rateIndex+1);else if(id==2)alternate=!alternate;else return false;setChanged();return true;}
        public void step(){
            if(!(level instanceof ServerLevel s)||s.getGameTime()==lastStep)return;lastStep=s.getGameTime();
            if(!automation()){setActive(false,1);return;}if(s.hasNeighborSignal(worldPosition)){setActive(false,2);return;}
            // Finished water still leaves a full or dark machine; production readiness must not trap its output.
            if(!routing()&&s.getGameTime()%20==0&&push(s,output,1000*FluidNetworks.DROPLETS_PER_MB,List.of(Direction.DOWN))>0)setChanged();
            boolean changed=false;
            if(routing()){
                int period=kind().equals("fluid_regulator")&&alternate?1:20;if(s.getGameTime()%period!=0)return;
                if(kind().equals("fluid_regulator")&&energy.getAmount()<10){setActive(false,3);return;}
                long limit=(long)RATES[rateIndex]*FluidNetworks.DROPLETS_PER_MB,moved=0;
                if(kind().equals("fluid_regulator"))moved=push(s,input,limit,List.of(facing()));
                else{
                    var sides=new ArrayList<Direction>();for(int i=0;i<6;i++){Direction d=Direction.values()[(cursor+i)%6];if(d!=facing()&&s.hasChunkAt(worldPosition.relative(d)))sides.add(d);}
                    if(alternate){for(Direction d:sides){moved=push(s,input,limit,List.of(d));if(moved>0)break;}}
                    else {long share=Math.max(1,Math.min(limit,input.amount)/Math.max(1,sides.size()));for(Direction d:sides)moved+=push(s,input,Math.min(share,limit-moved),List.of(d));}
                    cursor=(cursor+1)%6;
                }
                if(moved>0){if(kind().equals("fluid_regulator"))energy.setAmount(energy.getAmount()-10);changed=true;}setActive(changed,changed?0:4);
            }else if(kind().equals("solar_distiller")){
                boolean sun=s.isBrightOutside()&&!s.isRaining()&&s.canSeeSky(worldPosition.above())&&s.dimensionType().hasSkyLight();
                if(!sun){setActive(false,5);return;}if(!input.has(Fluids.WATER,1)||!output.fits(DISTILLED.source(),1)){setActive(false,4);return;}
                if(++solarTicks>=72){solarTicks=0;input.drain(1);output.fill(DISTILLED.source(),1);changed=true;}else setChanged();setActive(true,0);
            }else if(s.getGameTime()%20==0){
                // The existing boiler expands 1 water to 10 steam. Recover at that same ratio, never the legacy 100:1 ratio.
                int batch=Math.min(100,input.millibuckets()/10);if(batch>0&&output.fits(DISTILLED.source(),batch)){input.drain(batch*10);output.fill(DISTILLED.source(),batch);changed=true;}setActive(changed,changed?0:4);
            }
            if(changed)setChanged();
        }
        private long push(ServerLevel s,Storage<FluidVariant> from,long amount,List<Direction> sides){return amount<=0?0:FluidNetworks.pushToNeighbors(s,worldPosition,from,amount,sides.stream().filter(d->s.hasChunkAt(worldPosition.relative(d))).toList());}
        private void setActive(boolean lit,int state){status=state;if(getBlockState().getValue(LIT)!=lit)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(LIT,lit));}
        @Override public Component getDisplayName(){return Component.translatable("block.jugcraft.jimta_"+kind());}
        @Override public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new Menu(id,inv,worldPosition,this,data);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putLong("energy",energy.getAmount());input.writeValue(out.child("input"));output.writeValue(out.child("output"));out.putInt("rate",rateIndex);out.putBoolean("alternate",alternate);out.putInt("cursor",cursor);out.putInt("sun_ticks",solarTicks);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);energy.setAmount(in.getLongOr("energy",0));input.readValue(in.childOrEmpty("input"));output.readValue(in.childOrEmpty("output"));input.amount=Math.clamp(input.amount,0,input.getCapacity());output.amount=Math.clamp(output.amount,0,output.getCapacity());rateIndex=Math.clamp(in.getIntOr("rate",3),0,RATES.length-1);alternate=in.getBooleanOr("alternate",false);cursor=Math.clamp(in.getIntOr("cursor",0),0,5);solarTicks=Math.clamp(in.getIntOr("sun_ticks",0),0,71);}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final ContainerData data;private final Node node;private final BlockPos pos;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,null,new SimpleContainerData(8));}
        Menu(int id,Inventory inv,BlockPos pos,Node node,ContainerData data){super(MENU,id);this.pos=pos;this.node=node;this.data=data;addDataSlots(data);
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+18*col,84+18*row));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,142));}
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||node!=null&&p.level().getBlockEntity(pos)==node&&node.permitted(p)&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
        @Override public boolean clickMenuButton(Player p,int id){return stillValid(p)&&node!=null&&node.configure(p,id);}
        @Override public void clicked(int i,int b,ContainerInput type,Player p){if(stillValid(p))super.clicked(i,b,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
    }
}
