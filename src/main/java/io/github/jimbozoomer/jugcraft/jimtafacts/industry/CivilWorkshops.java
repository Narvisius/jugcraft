package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.*;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import io.github.jimbozoomer.jugcraft.tools.*;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Source stationary builders on native blueprints, JE and claims. One world mutation per work step. */
public final class CivilWorkshops {
    public static final List<String> KINDS=List.of("builder","filler","replacer","quarry","mining_well","flood_gate","terraformer");
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static Item AREA_MARKER;
    public static BlockEntityType<Node> ENTITY;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    private CivilWorkshops(){}
    public static void register(){
        for(String kind:KINDS){var id=Jugcraft.id("jimta_civil_"+kind);var key=ResourceKey.create(Registries.BLOCK,id);var block=new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key),kind);Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(kind,block);var ik=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));}
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_volume_marker"));AREA_MARKER=Registry.register(BuiltInRegistries.ITEM,key,new AreaMarker(new Item.Properties().setId(key).stacksTo(1)));
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_civil_workshop"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_civil_workshop"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.energy,ENTITY);ItemStorage.SIDED.registerForBlockEntity((n,side)->ContainerStorage.of(n,side),ENTITY);FluidStorage.SIDED.registerForBlockEntity((n,side)->n.kind().equals("flood_gate")||n.kind().equals("terraformer")?n.fluid:null,ENTITY);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{BLOCKS.values().forEach(out::accept);out.accept(AREA_MARKER);});
    }
    private static final class AreaMarker extends Item {
        AreaMarker(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext context){if(!(context.getLevel() instanceof ServerLevel level)||!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;var pos=context.getClickedPos();if(!Authority.mayChange(level,player,pos))return InteractionResult.FAIL;
            var stack=context.getItemInHand();var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();String dimension=level.dimension().identifier().toString();
            if(level.getBlockEntity(pos) instanceof Node node){if(!tag.contains("CornerA")||!tag.contains("CornerB")||!dimension.equals(tag.getStringOr("Dimension","")))return InteractionResult.FAIL;BlockPos a=BlockPos.of(tag.getLongOr("CornerA",0)),b=BlockPos.of(tag.getLongOr("CornerB",0));if(!node.setArea(a,b)){player.sendOverlayMessage(Component.literal("Choose a box at most 32 blocks wide/tall/deep, within 64 blocks of the machine."));return InteractionResult.FAIL;}player.sendOverlayMessage(Component.literal("Work area linked. Open the machine and press Start."));return InteractionResult.SUCCESS;}
            boolean first=!tag.contains("CornerA")||tag.contains("CornerB")||!dimension.equals(tag.getStringOr("Dimension",""));CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{if(first){t.putLong("CornerA",pos.asLong());t.remove("CornerB");t.putString("Dimension",dimension);}else t.putLong("CornerB",pos.asLong());});player.sendOverlayMessage(Component.literal(first?"First corner selected. Choose the opposite corner.":"Second corner selected. Use this on a civil workshop."));return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,display,lines,flag);lines.accept(Component.literal("Use on two blocks, then a civil machine. Max 32³; loaded areas only."));}
    }
    private static final class Device extends Block implements EntityBlock,EnergyConnectable,ItemConnectable,FluidConnectable {
        static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;static final BooleanProperty LIT=BlockStateProperties.LIT;final String kind;
        Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==ENTITY?(world,p,state,e)->{if(world.getGameTime()%5==0){var n=(Node)e;n.step((ServerLevel)world);boolean lit=n.status==4;if(state.getValue(LIT)!=lit)world.setBlock(p,state.setValue(LIT,lit),Block.UPDATE_CLIENTS);}}:null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){super.setPlacedBy(l,p,s,placer,stack);if(placer!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=placer.getUUID();n.setChanged();}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(l instanceof ServerLevel server){if(!Authority.mayChange(server,player,p))return InteractionResult.FAIL;if(l.getBlockEntity(p) instanceof Node n)player.openMenu(n);}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos> {
        private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);public UUID owner;public BlockPos minimum,maximum;public int cursor,status;public boolean running;
        public BlockPos visibleHead,visibleFrom; public long visibleAt;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(100_000,512,0,this::setChanged);
        public final FluidTank fluid=new FluidTank(16000,true,v->v.isOf(Fluids.WATER)||kind().equals("flood_gate")&&v.isOf(Fluids.LAVA),this::setChanged);
        private final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->KINDS.indexOf(kind());case 1->status;case 2->(int)(energy.getAmount()/10);case 3->Math.min(32767,cursor);case 4->Math.min(32767,volume());case 5->running?1:0;case 6->fluid.millibuckets();default->0;};}public void set(int i,int value){}public int getCount(){return 7;}};
        public Node(BlockPos p,BlockState s){super(ENTITY,p,s);}
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}
        public boolean setArea(BlockPos a,BlockPos b){var low=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));var high=new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));if(high.getX()-low.getX()>=32||high.getY()-low.getY()>=32||high.getZ()-low.getZ()>=32||low.distSqr(worldPosition)>4096||high.distSqr(worldPosition)>4096)return false;minimum=low;maximum=high;cursor=0;running=false;visibleHead=high;visibleFrom=high;visibleAt=level==null?0:level.getGameTime();setChanged();syncRender();return true;}
        public int volume(){if(kind().equals("builder")){var p=plan();return p==null?0:p.size();}return minimum==null||maximum==null?0:(maximum.getX()-minimum.getX()+1)*(maximum.getY()-minimum.getY()+1)*(maximum.getZ()-minimum.getZ()+1);}
        private Blueprint plan(){return items.get(0).getItem() instanceof BlueprintItem?Blueprint.get(BlueprintItem.idOf(items.get(0)),false):null;}
        private Rotation rotation(){return switch(getBlockState().getValue(Device.FACING)){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};}
        private BlockPos next(){if(kind().equals("builder")){var p=plan();if(p==null||p.large()||cursor>=p.size())return null;return (minimum==null?worldPosition.relative(getBlockState().getValue(Device.FACING),2):minimum).offset(p.offset(cursor,rotation()));}if(minimum==null||maximum==null)return null;int x=maximum.getX()-minimum.getX()+1,z=maximum.getZ()-minimum.getZ()+1;int y=kind().equals("quarry")||kind().equals("mining_well")?maximum.getY()-cursor/(x*z):minimum.getY()+cursor/(x*z);return new BlockPos(minimum.getX()+cursor%x,y,minimum.getZ()+cursor/x%z);}
        boolean allowed(ServerLevel l,ServerPlayer actor,BlockPos p){return l.hasChunkAt(p)&&p.distSqr(worldPosition)<=4096&&ClaimTransfers.allowed(l,worldPosition,p)&&Authority.mayChange(l,actor,p);}
        public void step(ServerLevel l){status=0;if(!running)return;if(l.hasNeighborSignal(worldPosition)){status=1;return;}var actor=Authority.answering(l,owner);if(actor==null||!allowed(l,actor,worldPosition)){status=2;return;}int workCost=kind().equals("terraformer")?4096:256;if(energy.getAmount()<workCost){status=3;return;}int total=volume();if(total==0){status=5;return;}
            for(int scan=0;scan<16;scan++){if(cursor>=total){running=false;status=9;setChanged();return;}BlockPos at=next();if(at==null){status=5;return;}if(!allowed(l,actor,at)){status=2;return;}if(at.equals(worldPosition)){cursor++;setChanged();continue;}var before=l.getBlockState(at);boolean changed;
                if(kind().equals("terraformer")){var result=Terraforming.work(this,l,actor,at,before);if(result==Terraforming.Result.SKIP){cursor++;setChanged();continue;}if(result==Terraforming.Result.WAIT)return;changed=true;}
                else if(kind().equals("quarry")||kind().equals("mining_well")){if(kind().equals("mining_well")&&(at.getX()!=minimum.getX()||at.getZ()!=minimum.getZ())||before.isAir()||!before.getFluidState().isEmpty()||before.getDestroySpeed(l,at)<0||l.getBlockEntity(at)!=null){cursor++;setChanged();continue;}changed=mine(l,actor,at,before);}
                else if(kind().equals("flood_gate")){if(!before.isAir()){cursor++;setChanged();continue;}if(fluid.millibuckets()<1000){status=7;return;}if(fluid.variant.isOf(Fluids.WATER)&&l.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,at)){status=7;return;}if(!containedFluid(l,actor,at)){status=6;return;}changed=l.setBlock(at,fluid.variant.getFluid().defaultFluidState().createLegacyBlock(),Block.UPDATE_ALL);if(changed)fluid.drain(1000);}
                else {if(!kind().equals("replacer")&&!before.isAir()){cursor++;setChanged();continue;}changed=place(l,actor,at,before);}
                if(changed){visibleFrom=visibleHead==null?at:visibleHead;visibleHead=at;visibleAt=l.getGameTime();syncRender();energy.setAmount(energy.getAmount()-workCost);cursor++;status=4;setChanged();}return;
            }
        }
        private boolean containedFluid(ServerLevel l,ServerPlayer actor,BlockPos at){
            // A flood gate may fill its selected reservoir, never send fluid into unselected/protected land.
            for(Direction side:Direction.values()){if(side==Direction.UP)continue;var next=at.relative(side);if(!allowed(l,actor,next))return false;var state=l.getBlockState(next);boolean within=next.getX()>=minimum.getX()&&next.getX()<=maximum.getX()&&next.getY()>=minimum.getY()&&next.getY()<=maximum.getY()&&next.getZ()>=minimum.getZ()&&next.getZ()<=maximum.getZ();if(!within&&!state.isFaceSturdy(l,next,side.getOpposite()))return false;}
            return true;
        }
        List<ItemStack> reserve(List<ItemStack> drops){List<ItemStack> next=items.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));for(var drop:drops){int remaining=drop.getCount();for(int slot=14;slot<27&&remaining>0;slot++){var old=next.get(slot);if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,drop))continue;int n=Math.min(remaining,drop.getMaxStackSize()-old.getCount());if(n<=0)continue;if(old.isEmpty())next.set(slot,drop.copyWithCount(n));else old.grow(n);remaining-=n;}if(remaining>0)return null;}return next;}
        void apply(List<ItemStack> next){for(int i=0;i<27;i++)items.set(i,next.get(i));}
        private boolean toolReady(BlockState state){var tool=items.get(0);return !tool.isEmpty()&&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state))&&(!(tool.getItem() instanceof PoweredToolItem p)||Chargeable.energy(tool)>=p.energyPerBlock(tool));}
        private void wear(ServerPlayer actor){var tool=items.get(0);if(tool.getItem() instanceof PoweredToolItem p)Chargeable.drain(tool,p.energyPerBlock(tool));else if(tool.isDamageableItem())tool.hurtAndBreak(1,actor,EquipmentSlot.MAINHAND);}
        private boolean mine(ServerLevel l,ServerPlayer actor,BlockPos at,BlockState before){if(before.getDestroySpeed(l,at)>50||!toolReady(before)){status=6;return false;}var next=reserve(Block.getDrops(before,l,at,null,actor,items.get(0)));if(next==null){status=8;return false;}if(!l.destroyBlock(at,false,actor))return false;apply(next);wear(actor);return true;}
        private boolean place(ServerLevel l,ServerPlayer actor,BlockPos at,BlockState before){
            Blueprint plan=kind().equals("builder")?plan():null;BlockState desired=plan==null?null:plan.state(cursor,rotation());TagKey<Block> tag=plan==null?null:plan.anyOf(cursor);
            for(int slot=1;slot<14;slot++){var stack=items.get(slot);if(!(stack.getItem() instanceof BlockItem bi))continue;var block=bi.getBlock();if(!Blueprint.allowed(block)||block instanceof EntityBlock||block instanceof DoorBlock||block instanceof BedBlock||block instanceof DoublePlantBlock)continue;if(desired!=null&&(tag==null?block!=desired.getBlock():!block.defaultBlockState().is(tag)))continue;
                BlockState wanted=desired==null?block.defaultBlockState():tag==null?desired:io.github.jimbozoomer.jugcraft.drone.BuildJobs.like(desired,block);if(wanted.hasProperty(BlockStateProperties.WATERLOGGED))wanted=wanted.setValue(BlockStateProperties.WATERLOGGED,false);int count=wanted.hasProperty(SlabBlock.TYPE)&&wanted.getValue(SlabBlock.TYPE)==SlabType.DOUBLE?2:1;if(stack.getCount()<count)continue;
                if(before.equals(wanted)){cursor++;setChanged();return false;}if(!before.isAir()&&(l.getBlockEntity(at)!=null||before.getDestroySpeed(l,at)<0||before.getDestroySpeed(l,at)>50||!before.getFluidState().isEmpty()||!toolReady(before))){status=6;return false;}
                var shape=wanted.getCollisionShape(l,at);if(!wanted.canSurvive(l,at)||!l.isUnobstructed(wanted,at,net.minecraft.world.phys.shapes.CollisionContext.empty())){status=6;return false;}
                var next=reserve(before.isAir()?List.of():Block.getDrops(before,l,at,null,actor,items.get(0)));if(next==null){status=8;return false;}next.get(slot).shrink(count);if(!l.setBlock(at,wanted,Block.UPDATE_ALL))return false;apply(next);if(!before.isAir())wear(actor);return true;
            }status=5;return false;
        }
        private void syncRender(){if(level!=null&&!level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){
            var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,lookup);out.putBoolean("render_only",true);
            if(minimum!=null&&maximum!=null){out.store("Minimum",BlockPos.CODEC,minimum);out.store("Maximum",BlockPos.CODEC,maximum);}
            if(visibleHead!=null)out.store("Head",BlockPos.CODEC,visibleHead);if(visibleFrom!=null)out.store("From",BlockPos.CODEC,visibleFrom);out.putLong("MovedAt",visibleAt);return out.buildResult();
        }
        @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
        @Override protected NonNullList<ItemStack> getItems(){return items;}
        @Override protected void setItems(NonNullList<ItemStack> values){items=values;}
        @Override public int getContainerSize(){return 27;}
        @Override public BlockPos getScreenOpeningData(ServerPlayer p){return worldPosition;}
        @Override protected AbstractContainerMenu createMenu(int id,Inventory player){return new Menu(id,player,worldPosition,this,data);}
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&(!(level instanceof ServerLevel l)||Authority.mayChange(l,p,worldPosition));}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){if(kind().equals("terraformer"))return slot==0?Terraforming.plan(stack):slot<14&&Terraforming.material(stack);return slot==0?kind().equals("builder")?stack.getItem() instanceof BlueprintItem:stack.has(DataComponents.TOOL):slot<14&&stack.getItem() instanceof BlockItem;}
        @Override public int[] getSlotsForFace(Direction side){return java.util.stream.IntStream.range(side==Direction.DOWN?14:0,side==Direction.DOWN?27:14).toArray();}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return canPlaceItem(slot,stack);}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot>=14;}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState state){if(level!=null)Containers.dropContents(level,p,this);clearContent();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);out.putLong("Energy",energy.getAmount());out.putInt("Cursor",cursor);out.putBoolean("Running",running);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);if(minimum!=null&&maximum!=null){out.store("Minimum",BlockPos.CODEC,minimum);out.store("Maximum",BlockPos.CODEC,maximum);}fluid.writeValue(out.child("Fluid"));}
        @Override protected void loadAdditional(ValueInput in){if(in.getBooleanOr("render_only",false)){
            minimum=in.read("Minimum",BlockPos.CODEC).orElse(null);maximum=in.read("Maximum",BlockPos.CODEC).orElse(null);
            visibleHead=in.read("Head",BlockPos.CODEC).orElse(maximum);visibleFrom=in.read("From",BlockPos.CODEC).orElse(visibleHead);visibleAt=in.getLongOr("MovedAt",0);return;}
            super.loadAdditional(in);items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);energy.setAmount(in.getLongOr("Energy",0));owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);var a=in.read("Minimum",BlockPos.CODEC).orElse(null);var b=in.read("Maximum",BlockPos.CODEC).orElse(null);if(a!=null&&b!=null)setArea(a,b);cursor=Math.clamp(in.getIntOr("Cursor",0),0,32768);running=in.getBooleanOr("Running",false);fluid.readValue(in.childOrEmpty("Fluid"));}
    }
    public static final class Menu extends AbstractContainerMenu {
        final Container container;public final BlockPos pos;public final ContainerData data;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,new SimpleContainer(27),new SimpleContainerData(7));}
        Menu(int id,Inventory inv,BlockPos pos,Container container,ContainerData data){super(MENU,id);this.container=container;this.pos=pos;this.data=data;addDataSlots(data);for(int i=0;i<27;i++){int slot=i;addSlot(new Slot(container,i,8+i%9*18,36+i/9*18){@Override public boolean mayPlace(ItemStack stack){return container.canPlaceItem(slot,stack);}});}for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,144+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,202));}
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||p.level().getBlockEntity(pos)==container&&container.stillValid(p);}
        @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p)||!(p instanceof ServerPlayer player)||!Authority.mayChange(player.level(),p,pos)||!(container instanceof Node node))return false;if(id==0)node.running=!node.running;else if(id==1&&!node.running)node.cursor=0;else return false;node.setChanged();return true;}
        @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var held=slot.getItem();var original=held.copy();if(index<27?!moveItemStackTo(held,27,63,true):!moveItemStackTo(held,0,14,false))return ItemStack.EMPTY;if(held.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return original;}
    }
}
