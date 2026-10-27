package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
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
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Source start/middle/end geometry; two real fluid circuits exchange heat without making JE. */
public final class FactoryHeatExchanger {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<Node> TYPE;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    public static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    private FactoryHeatExchanger(){}
    public static void register(){
        for(String part:List.of("start","middle","end")){
            var id=Jugcraft.id("jimta_factory_heat_"+part);var key=ResourceKey.create(Registries.BLOCK,id);
            var block=new Part(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion().pushReaction(PushReaction.IMMOVEABLE),part);
            Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(part,block);
            var ik=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
        }
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_factory_heat"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_factory_heat"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        FluidStorage.SIDED.registerForBlockEntity((node,side)->node.port(side),TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->BLOCKS.values().forEach(out::accept));
    }
    public static final class Part extends Block implements EntityBlock,FluidConnectable {
        final String part;
        Part(Properties p,String part){super(p);this.part=part;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return !l.isClientSide()&&t==TYPE?(w,p,state,n)->{if(w.getGameTime()%5==0)((Node)n).step((ServerLevel)w);}:null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){if(e!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=e.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(p instanceof ServerPlayer sp&&l.getBlockEntity(pos) instanceof Node n&&n.mayUse(sp))sp.openMenu(n);return InteractionResult.SUCCESS;}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
            if(!(l.getBlockEntity(pos) instanceof Node n)||part.equals("middle"))return InteractionResult.PASS;
            if(FluidStorage.ITEM.find(stack,net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack))==null)return InteractionResult.PASS;
            if(p instanceof ServerPlayer sp&&n.mayUse(sp))FluidStorageUtil.interactWithFluidStorage(n.port(hit.getDirection()),p,hand);
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
        public UUID owner;
        public final FluidTank input=new FluidTank(16000,true,this::accepts,this::setChanged);
        public final FluidTank output=new FluidTank(16000,false,v->true,this::setChanged);
        public int sections,status;
        Node(BlockPos p,BlockState s){super(TYPE,p,s);}
        public String part(){return ((Part)getBlockState().getBlock()).part;}
        private Direction direction(){return getBlockState().getValue(FACING);}
        private boolean accepts(FluidVariant v){return part().equals("start")?v.isOf(IndustrialHeat.HOT_COOLANT.source()):part().equals("end")&&(v.isOf(Fluids.WATER)||v.isOf(IndustrialFluidServices.DISTILLED.source()));}
        public boolean permitted(){if(!(level instanceof ServerLevel l)||isRemoved()||l.getBlockEntity(worldPosition)!=this)return false;var p=Authority.answering(l,owner);return p!=null&&Authority.mayChange(l,p,worldPosition);}
        public boolean mayUse(ServerPlayer p){return !p.isSpectator()&&p.level()==level&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64&&Authority.mayChange(p.level(),p,worldPosition)&&(owner==null||owner.equals(p.getUUID())||Authority.answering(p.level(),owner)==p);}
        public Storage<FluidVariant> port(Direction side){
            if(part().equals("middle"))return null;
            var tank=side==Direction.DOWN?output:input;
            return new Storage<>(){
                @Override public long insert(FluidVariant v,long amount,TransactionContext tx){return permitted()?tank.insert(v,amount,tx):0;}
                @Override public long extract(FluidVariant v,long amount,TransactionContext tx){return permitted()?tank.extract(v,amount,tx):0;}
                @Override public Iterator<StorageView<FluidVariant>> iterator(){return Collections.<StorageView<FluidVariant>>singletonList(new StorageView<FluidVariant>(){
                    public boolean isResourceBlank(){return !permitted()||tank.isResourceBlank();}
                    public FluidVariant getResource(){return permitted()?tank.getResource():FluidVariant.blank();}
                    public long getAmount(){return permitted()?tank.getAmount():0;}
                    public long getCapacity(){return permitted()?tank.getCapacity():0;}
                    public long extract(FluidVariant resource,long maxAmount,TransactionContext tx){return permitted()?tank.extract(resource,maxAmount,tx):0;}
                }).iterator();}
            };
        }
        public Node start(){
            if(level==null)return null;
            if(part().equals("start"))return this;
            for(int i=1;i<=4;i++){var p=worldPosition.relative(direction().getOpposite(),i);if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof Node n)||n.direction()!=direction())return null;if(n.part().equals("start"))return n;if(!n.part().equals("middle"))return null;}
            return null;
        }
        /** At most five already-loaded parts; every owner, facing and protected boundary must agree. */
        public Node end(){
            sections=0;if(!part().equals("start")||!permitted())return null;
            for(int i=1;i<=4;i++){
                var at=worldPosition.relative(direction(),i);
                if(!level.hasChunkAt(at)||!(level.getBlockEntity(at) instanceof Node n)||n.direction()!=direction()||!Objects.equals(owner,n.owner)||!n.permitted()||!ClaimTransfers.allowed(level,worldPosition,at))return null;
                if(n.part().equals("end")){if(i<2)return null;sections=i-1;return n;}
                if(!n.part().equals("middle"))return null;
            }return null;
        }
        public void step(ServerLevel l){
            if(!part().equals("start"))return;
            var end=end();status=end==null?0:1;
            if(end!=null){
                boolean paused=false;for(int i=0;i<=sections+1;i++)paused|=l.hasNeighborSignal(worldPosition.relative(direction(),i));
                if(paused)status=2;
                else if(input.variant.isOf(IndustrialHeat.HOT_COOLANT.source())&&(end.input.variant.isOf(Fluids.WATER)||end.input.variant.isOf(IndustrialFluidServices.DISTILLED.source()))){
                    int amount=Math.min(new int[]{0,5,10,20}[sections],Math.min(input.millibuckets(),end.input.millibuckets()));
                    amount=Math.min(amount,Math.min(output.capacityMb()-output.millibuckets(),(end.output.capacityMb()-end.output.millibuckets())/10));
                    if(amount>0&&output.fits(IndustrialHeat.COOLANT.source(),amount)&&end.output.fits(IndustrialHeat.STEAM.fluid(),amount*10)){
                        input.drain(amount);output.fill(IndustrialHeat.COOLANT.source(),amount);
                        end.input.drain(amount);end.output.fill(IndustrialHeat.STEAM.fluid(),amount*10);
                        end.setChanged();setChanged();status=3;
                    }else status=4;
                }
            }
            boolean active=status==3;if(getBlockState().getValue(LIT)!=active)l.setBlockAndUpdate(worldPosition,getBlockState().setValue(LIT,active));
        }
        @Override public Component getDisplayName(){return Component.literal("Counterflow Heat Exchanger");}
        @Override public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new Menu(id,inv,worldPosition,this);}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);input.writeValue(out.child("input"));output.writeValue(out.child("output"));}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);input.readValue(in.childOrEmpty("input"));output.readValue(in.childOrEmpty("output"));}
    }
    public static final class Menu extends AbstractContainerMenu {
        public final BlockPos pos;private final Node node;public final ContainerData data;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,null);}
        Menu(int id,Inventory inv,BlockPos pos,Node node){super(MENU,id);this.pos=pos;this.node=node;
            data=node==null?new SimpleContainerData(6):new ContainerData(){
                @Override public int get(int i){var start=node.start();var end=start==null?null:start.end();return switch(i){case 0->start==null?0:start.status;case 1->start==null?0:start.sections;case 2->start==null?0:start.input.millibuckets();case 3->start==null?0:start.output.millibuckets();case 4->end==null?0:end.input.millibuckets();default->end==null?0:end.output.millibuckets();};}
                @Override public void set(int i,int v){}@Override public int getCount(){return 6;}
            };addDataSlots(data);
        }
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||p instanceof ServerPlayer sp&&p.level().getBlockEntity(pos)==node&&node.mayUse(sp);}
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    }
}
