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
    public static final List<String> KINDS=List.of("builder","filler","replacer","quarry","mining_well","flood_gate","terraformer","advanced_miner");
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static Item AREA_MARKER;
    public static BlockEntityType<Node> ENTITY;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    private CivilWorkshops(){}
    public static void register(){
        for(String kind:KINDS){var id=Jugcraft.id("jimta_civil_"+kind);var key=ResourceKey.create(Registries.BLOCK,id);var block=new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key),kind);Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(kind,block);var ik=ResourceKey.create(Registries.ITEM,id);Registry.register(BuiltInRegistries.ITEM,ik,new BlockItem(block,new Item.Properties().setId(ik).useBlockDescriptionPrefix()));}
        var minerKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_civil_miner"));
        var miner=new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(minerKey),"mining_well");
        Registry.register(BuiltInRegistries.BLOCK,minerKey,miner);BLOCKS.put("miner",miner);
        var minerItem=ResourceKey.create(Registries.ITEM,minerKey.identifier());Registry.register(BuiltInRegistries.ITEM,minerItem,new BlockItem(miner,new Item.Properties().setId(minerItem).useBlockDescriptionPrefix()));
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
            if(level.getBlockEntity(pos) instanceof FactoryPlanning.Planner)return FactoryPlanning.useMarker(context);
            if(level.getBlockEntity(pos) instanceof Node node){if(!node.permitted(player))return InteractionResult.FAIL;if(!tag.contains("CornerA")||!tag.contains("CornerB")||!dimension.equals(tag.getStringOr("Dimension","")))return InteractionResult.FAIL;BlockPos a=BlockPos.of(tag.getLongOr("CornerA",0)),b=BlockPos.of(tag.getLongOr("CornerB",0));if(!node.setArea(a,b)){player.sendOverlayMessage(Component.literal("Choose a box at most 32 blocks wide/tall/deep, within 64 blocks of the machine."));return InteractionResult.FAIL;}player.sendOverlayMessage(Component.literal("Work area linked. Open the machine and press Start."));return InteractionResult.SUCCESS;}
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
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(l instanceof ServerLevel server){if(!Authority.mayChange(server,player,p))return InteractionResult.FAIL;if(l.getBlockEntity(p) instanceof Node n){if(!n.permitted(player))return InteractionResult.FAIL;player.openMenu(n);}}return InteractionResult.SUCCESS;}
    }
    public static final class Node extends BaseContainerBlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos> {
        private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);public UUID owner,surveyVolume;public BlockPos minimum,maximum;public int cursor,status;public boolean running;public boolean excludeFilter;public int fillerPattern;public boolean invertPattern;public boolean retracting;private final List<BlockPos> deployedPipes=new ArrayList<>();private final List<BlockPos> branchPlan=new ArrayList<>();private int branchRoot,scannedY=Integer.MAX_VALUE;
        public BlockPos visibleHead,visibleFrom; public long visibleAt;
        public final SimpleEnergyStorage energy=new SimpleEnergyStorage(100_000,512,0,this::setChanged);
        public final FluidTank fluid=new FluidTank(16000,true,v->v.isOf(Fluids.WATER)||kind().equals("flood_gate")&&v.isOf(Fluids.LAVA),this::setChanged);
        private final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->KINDS.indexOf(kind());case 1->status;case 2->(int)(energy.getAmount()/10);case 3->Math.min(32767,cursor);case 4->Math.min(32767,volume());case 5->running?1:0;case 6->fluid.millibuckets();case 7->excludeFilter?1:0;case 8->fillerPattern;case 9->invertPattern?1:0;case 10->pipeMiner()?1:0;case 11->deployedPipes.size();default->0;};}public void set(int i,int value){}public int getCount(){return 12;}};
        public Node(BlockPos p,BlockState s){super(ENTITY,p,s);}
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}
        /** Configuration cannot borrow the owner's authority merely because the machine stands on public land. */
        public boolean permitted(Player player){
            return player instanceof ServerPlayer p&&p.isAlive()&&!p.isSpectator()&&p.level()==level
                    &&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))<=64
                    &&owner!=null&&(owner.equals(p.getUUID())||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,p.getUUID()))
                    &&Authority.mayChange(p.level(),p,worldPosition);
        }
        private void releaseVolume(){if(surveyVolume!=null&&level instanceof ServerLevel server)FactoryVolumes.get(server).releaseMachine(surveyVolume,worldPosition);surveyVolume=null;}
        public boolean pipeMiner(){return getBlockState().is(BLOCKS.get("miner"));}
        public int deployedPipeCount(){return deployedPipes.size();}
        public boolean requestRetraction(){if(!pipeMiner()||running||deployedPipes.isEmpty())return false;retracting=true;branchRoot=0;branchPlan.clear();setChanged();return true;}
        public boolean setArea(BlockPos a,BlockPos b){if(pipeMiner()&&(!deployedPipes.isEmpty()||a.getX()!=worldPosition.getX()||b.getX()!=worldPosition.getX()||a.getZ()!=worldPosition.getZ()||b.getZ()!=worldPosition.getZ()||Math.max(a.getY(),b.getY())!=worldPosition.getY()-1))return false;var low=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));var high=new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));if(high.getX()-low.getX()>=32||high.getY()-low.getY()>=32||high.getZ()-low.getZ()>=32||low.distSqr(worldPosition)>4096||high.distSqr(worldPosition)>4096)return false;releaseVolume();minimum=low;maximum=high;cursor=0;scannedY=Integer.MAX_VALUE;branchRoot=0;branchPlan.clear();running=false;visibleHead=high;visibleFrom=high;visibleAt=level==null?0:level.getGameTime();setChanged();syncRender();return true;}
        public int volume(){if(kind().equals("builder")){var p=plan();return p==null?0:p.size();}return minimum==null||maximum==null?0:(maximum.getX()-minimum.getX()+1)*(maximum.getY()-minimum.getY()+1)*(maximum.getZ()-minimum.getZ()+1);}
        private Blueprint plan(){return items.get(0).getItem() instanceof BlueprintItem?Blueprint.get(BlueprintItem.idOf(items.get(0)),false):null;}
        private Rotation rotation(){return switch(getBlockState().getValue(Device.FACING)){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};}
        private BlockPos next(){if(kind().equals("builder")){var p=plan();if(p==null||p.large()||cursor>=p.size())return null;return (minimum==null?worldPosition.relative(getBlockState().getValue(Device.FACING),2):minimum).offset(p.offset(cursor,rotation()));}if(minimum==null||maximum==null)return null;int x=maximum.getX()-minimum.getX()+1,z=maximum.getZ()-minimum.getZ()+1;int y=kind().equals("quarry")||kind().equals("mining_well")||kind().equals("advanced_miner")?maximum.getY()-cursor/(x*z):minimum.getY()+cursor/(x*z);return new BlockPos(minimum.getX()+cursor%x,y,minimum.getZ()+cursor/x%z);}
        boolean allowed(ServerLevel l,ServerPlayer actor,BlockPos p){return l.hasChunkAt(p)&&p.distSqr(worldPosition)<=4096&&ClaimTransfers.allowed(l,worldPosition,p)&&Authority.mayChange(l,actor,p);}
        public void step(ServerLevel l){status=0;if(retracting){retract(l);return;}if(!running)return;if(l.hasNeighborSignal(worldPosition)){status=1;return;}var actor=Authority.answering(l,owner);if(actor==null||!allowed(l,actor,worldPosition)){status=2;return;}int workCost=kind().equals("terraformer")?4096:256;if(energy.getAmount()<(pipeMiner()&&branchRoot>0&&branchPlan.isEmpty()?32:workCost)){status=3;return;}int total=volume();if(total==0){status=5;return;}if(pipeMiner()&&scannerStep(l,actor))return;
            for(int scan=0;scan<16;scan++){if(cursor>=total){running=false;status=9;setChanged();return;}BlockPos at=next();if(at==null){status=5;return;}if(!allowed(l,actor,at)){status=2;return;}if(at.equals(worldPosition)){cursor++;setChanged();continue;}var before=l.getBlockState(at);boolean changed;
                  if(kind().equals("filler")&&!FillerPatterns.selected(fillerPattern,invertPattern,at,minimum,maximum,getBlockState().getValue(Device.FACING))){cursor++;setChanged();continue;}
                  if(pipeMiner()){
                      if(before.is(IndustrialConstruction.BLOCKS.get("mining_pipe"))||before.is(IndustrialConstruction.BLOCKS.get("mining_tip"))){cursor++;setChanged();continue;}
                      changed=extendPipe(l,actor,at,before);
                  }
                  else if(kind().equals("filler")&&fillerPattern==8){
                      if(before.isAir()){cursor++;setChanged();continue;}
                      if(!before.getFluidState().isEmpty()||before.getDestroySpeed(l,at)<0||l.getBlockEntity(at)!=null){status=6;return;}
                      changed=mine(l,actor,at,before);
                  }
                  else if(kind().equals("terraformer")){var result=Terraforming.work(this,l,actor,at,before);if(result==Terraforming.Result.SKIP){cursor++;setChanged();continue;}if(result==Terraforming.Result.WAIT)return;changed=true;}
                else if(kind().equals("quarry")||kind().equals("mining_well")||kind().equals("advanced_miner")){if(kind().equals("mining_well")&&(at.getX()!=minimum.getX()||at.getZ()!=minimum.getZ())||before.isAir()||!before.getFluidState().isEmpty()||before.getDestroySpeed(l,at)<0||l.getBlockEntity(at)!=null){cursor++;setChanged();continue;}changed=mine(l,actor,at,before);}
                else if(kind().equals("flood_gate")){if(!before.isAir()){cursor++;setChanged();continue;}if(fluid.millibuckets()<1000){status=7;return;}if(fluid.variant.isOf(Fluids.WATER)&&l.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,at)){status=7;return;}if(!containedFluid(l,actor,at)){status=6;return;}changed=l.setBlock(at,fluid.variant.getFluid().defaultFluidState().createLegacyBlock(),Block.UPDATE_ALL);if(changed)fluid.drain(1000);}
                else {if(!kind().equals("replacer")&&!before.isAir()){cursor++;setChanged();continue;}changed=place(l,actor,at,before);}
                if(changed){visibleFrom=visibleHead==null?at:visibleHead;visibleHead=at;visibleAt=l.getGameTime();syncRender();energy.setAmount(energy.getAmount()-workCost);cursor++;status=4;setChanged();}return;
            }
        }
        private boolean extendPipe(ServerLevel l,ServerPlayer actor,BlockPos at,BlockState before){return extendPipe(l,actor,at,before,false);}
        private boolean extendPipe(ServerLevel l,ServerPlayer actor,BlockPos at,BlockState before,boolean branch){
            var pipe=IndustrialConstruction.BLOCKS.get("mining_pipe");var tip=IndustrialConstruction.BLOCKS.get("mining_tip");
            if(deployedPipes.size()>=(branch?40:32)||l.getBlockEntity(at)!=null||!before.getFluidState().isEmpty()||before.getDestroySpeed(l,at)<0||before.getDestroySpeed(l,at)>50){status=6;return false;}
            int supply=-1;for(int i=1;i<14;i++)if(items.get(i).is(pipe.asItem())){supply=i;break;}
            if(supply<0){status=5;return false;}
            if(!before.isAir()&&!toolReady(before)){status=6;return false;}
            var previous=deployedPipes.isEmpty()?null:deployedPipes.getLast();
            if(previous!=null&&(!allowed(l,actor,previous)||!l.getBlockState(previous).is(tip))){status=6;return false;}
            if(previous==null&&!at.equals(worldPosition.below())){status=6;return false;}
            if(previous!=null&&(branch?previous.getY()!=at.getY()||previous.distManhattan(at)!=1:!at.equals(previous.below()))){status=6;return false;}
            var next=reserve(before.isAir()?List.of():Block.getDrops(before,l,at,null,actor,items.get(0)));if(next==null){status=8;return false;}
            // Replacing a non-container is one world write; reserve drops before spending the pipe/tool.
            if(!l.setBlock(at,tip.defaultBlockState(),Block.UPDATE_ALL))return false;
            next.get(supply).shrink(1);apply(next);if(!before.isAir())wear(actor);
            if(previous!=null)l.setBlock(previous,IndustrialConstruction.miningPipeState(l,previous),Block.UPDATE_ALL);
            deployedPipes.add(at.immutable());return true;
        }
        private void retract(ServerLevel l){
            if(!pipeMiner()||deployedPipes.isEmpty()){retracting=false;cursor=0;setChanged();return;}
            if(l.hasNeighborSignal(worldPosition)){status=1;return;}
            var actor=Authority.answering(l,owner);var at=deployedPipes.getLast();
            if(actor==null||!allowed(l,actor,worldPosition)||!allowed(l,actor,at)){status=2;return;}
            var pipe=IndustrialConstruction.BLOCKS.get("mining_pipe");var tip=IndustrialConstruction.BLOCKS.get("mining_tip");var state=l.getBlockState(at);
            if(!state.is(pipe)&&!state.is(tip)){deployedPipes.removeLast();setChanged();return;}
            if(branchRoot>0&&deployedPipes.size()>1&&!allowed(l,actor,deployedPipes.get(deployedPipes.size()-2))){status=2;return;}
            if(energy.getAmount()<32){status=3;return;}
            var next=reserve(List.of(new ItemStack(pipe)));if(next==null){status=8;return;}
            if(!l.removeBlock(at,false))return;apply(next);energy.setAmount(energy.getAmount()-32);deployedPipes.removeLast();
            // Retraction may be interrupted safely: remaining rods stay breakable and never carry cargo.
            if(branchRoot>0&&!deployedPipes.isEmpty())l.setBlock(deployedPipes.getLast(),tip.defaultBlockState(),Block.UPDATE_ALL);
            visibleFrom=visibleHead;visibleHead=deployedPipes.isEmpty()?worldPosition:deployedPipes.getLast();visibleAt=l.getGameTime();status=4;setChanged();syncRender();
        }
        /** One charged survey per depth, then one paid pipe move per step. No remote ore vacuuming. */
        private boolean scannerStep(ServerLevel l,ServerPlayer actor){
            if(branchRoot>0){
                if(!branchPlan.isEmpty()){
                    var at=branchPlan.getFirst();if(!allowed(l,actor,at)){status=2;return true;}
                    if(extendPipe(l,actor,at,l.getBlockState(at),true)){branchPlan.removeFirst();energy.setAmount(energy.getAmount()-256);visibleFrom=visibleHead;visibleHead=at;visibleAt=l.getGameTime();status=4;setChanged();syncRender();}return true;
                }
                if(deployedPipes.size()>branchRoot){retract(l);return true;}
                if(!deployedPipes.isEmpty()){var last=deployedPipes.getLast();if(!allowed(l,actor,last)){status=2;return true;}var state=l.getBlockState(last);if(!state.is(IndustrialConstruction.BLOCKS.get("mining_pipe"))&&!state.is(IndustrialConstruction.BLOCKS.get("mining_tip"))){status=6;return true;}l.setBlock(last,IndustrialConstruction.BLOCKS.get("mining_tip").defaultBlockState(),Block.UPDATE_ALL);}
                branchRoot=0;setChanged();return false;
            }
            if(deployedPipes.isEmpty())return false;var head=deployedPipes.getLast();if(head.getY()==scannedY)return false;
            var scanner=items.get(13);if(!(scanner.getItem() instanceof IndustrialHandTools.Scanner tool))return false;
            if(!Chargeable.drain(scanner,100)){status=3;return true;}
            scannedY=head.getY();setChanged();int radius=tool.capacity>=100000?4:2;
            var candidates=new ArrayList<BlockPos>();for(var pos:BlockPos.betweenClosed(head.offset(-radius,0,-radius),head.offset(radius,0,radius)))if(!pos.equals(head))candidates.add(pos.immutable());
            candidates.sort(java.util.Comparator.comparingInt((BlockPos pos)->pos.distManhattan(head)).thenComparingLong(BlockPos::asLong));
            for(var target:candidates){
                if(!allowed(l,actor,target))continue;var ore=l.getBlockState(target);
                if(!ore.getBlock().builtInRegistryHolder().tags().anyMatch(tag->tag.location().getPath().equals("ores")||tag.location().getPath().endsWith("_ores")))continue;
                var path=new ArrayList<BlockPos>();var point=head;
                while(point.getX()!=target.getX()){point=point.offset(Integer.signum(target.getX()-point.getX()),0,0);path.add(point);}
                while(point.getZ()!=target.getZ()){point=point.offset(0,0,Integer.signum(target.getZ()-point.getZ()));path.add(point);}
                boolean valid=true;for(var pos:path){if(!allowed(l,actor,pos)){valid=false;break;}var state=l.getBlockState(pos);if(l.getBlockEntity(pos)!=null||!state.getFluidState().isEmpty()||state.getDestroySpeed(l,pos)<0||state.getDestroySpeed(l,pos)>50||!state.isAir()&&!toolReady(state)||deployedPipes.contains(pos)){valid=false;break;}}
                if(valid){branchRoot=deployedPipes.size();branchPlan.addAll(path);setChanged();return true;}
            }
            return false;
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
        /** Filter samples are retained items, never output cargo. Empty include list safely mines nothing. */
        public boolean acceptsMiningDrops(List<ItemStack> drops){
            boolean matches=false;
            for(int slot=1;slot<14&&!matches;slot++)if(!items.get(slot).isEmpty())for(var drop:drops)
                if(!drop.isEmpty()&&FactoryFieldTools.matches(items.get(slot),drop.getItem())){matches=true;break;}
            return excludeFilter?!matches:matches;
        }
        private boolean mine(ServerLevel l,ServerPlayer actor,BlockPos at,BlockState before){if(before.getDestroySpeed(l,at)>50||!toolReady(before)){status=6;return false;}var drops=Block.getDrops(before,l,at,null,actor,items.get(0));if(kind().equals("advanced_miner")&&!acceptsMiningDrops(drops)){cursor++;setChanged();return false;}var next=reserve(drops);if(next==null){status=8;return false;}if(!l.destroyBlock(at,false,actor))return false;apply(next);wear(actor);return true;}
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
        @Override public boolean stillValid(Player p){return super.stillValid(p)&&(!(level instanceof ServerLevel)||permitted(p));}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){if(pipeMiner()&&slot==13&&stack.getItem() instanceof IndustrialHandTools.Scanner)return true;if(kind().equals("terraformer"))return slot==0?Terraforming.plan(stack):slot<14&&Terraforming.material(stack);if(kind().equals("advanced_miner"))return slot==0?stack.has(DataComponents.TOOL):slot>0&&slot<14;return slot==0?kind().equals("builder")?stack.getItem() instanceof BlueprintItem:stack.has(DataComponents.TOOL):slot<14&&stack.getItem() instanceof BlockItem;}
        @Override public int[] getSlotsForFace(Direction side){return java.util.stream.IntStream.range(side==Direction.DOWN?14:0,side==Direction.DOWN?27:14).toArray();}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return canPlaceItem(slot,stack);}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot>=14;}
        @Override public void preRemoveSideEffects(BlockPos p,BlockState state){releaseVolume();if(level!=null)Containers.dropContents(level,p,this);clearContent();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);if(surveyVolume!=null)out.store("SurveyVolume",UUIDUtil.CODEC,surveyVolume);out.putLong("Energy",energy.getAmount());out.putInt("Cursor",cursor);out.putBoolean("Running",running);out.putBoolean("ExcludeFilter",excludeFilter);out.store("DeployedPipes",BlockPos.CODEC.listOf(0,40),deployedPipes);out.store("BranchPlan",BlockPos.CODEC.listOf(0,8),branchPlan);out.putInt("BranchRoot",branchRoot);out.putInt("ScannedY",scannedY);out.putBoolean("Retracting",retracting);out.putInt("FillerPattern",fillerPattern);out.putBoolean("InvertPattern",invertPattern);if(owner!=null)out.store("Owner",UUIDUtil.CODEC,owner);if(minimum!=null&&maximum!=null){out.store("Minimum",BlockPos.CODEC,minimum);out.store("Maximum",BlockPos.CODEC,maximum);}fluid.writeValue(out.child("Fluid"));}
        @Override protected void loadAdditional(ValueInput in){if(in.getBooleanOr("render_only",false)){
            minimum=in.read("Minimum",BlockPos.CODEC).orElse(null);maximum=in.read("Maximum",BlockPos.CODEC).orElse(null);
            visibleHead=in.read("Head",BlockPos.CODEC).orElse(maximum);visibleFrom=in.read("From",BlockPos.CODEC).orElse(visibleHead);visibleAt=in.getLongOr("MovedAt",0);return;}
            super.loadAdditional(in);items=NonNullList.withSize(27,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);energy.setAmount(in.getLongOr("Energy",0));owner=in.read("Owner",UUIDUtil.CODEC).orElse(null);var a=in.read("Minimum",BlockPos.CODEC).orElse(null);var b=in.read("Maximum",BlockPos.CODEC).orElse(null);surveyVolume=null;deployedPipes.clear();minimum=null;maximum=null;boolean validArea=a!=null&&b!=null&&setArea(a,b);cursor=Math.clamp(in.getIntOr("Cursor",0),0,32768);running=in.getBooleanOr("Running",false)&&(validArea||kind().equals("builder"));excludeFilter=in.getBooleanOr("ExcludeFilter",false);fillerPattern=Math.clamp(in.getIntOr("FillerPattern",0),0,FillerPatterns.NAMES.size()-1);invertPattern=in.getBooleanOr("InvertPattern",false);fluid.readValue(in.childOrEmpty("Fluid"));
            branchPlan.clear();branchRoot=0;scannedY=in.getIntOr("ScannedY",Integer.MAX_VALUE);
            if(pipeMiner()){
                var path=in.read("DeployedPipes",BlockPos.CODEC.listOf(0,40)).orElse(List.of());int root=in.getIntOr("BranchRoot",0);boolean valid=root>=0&&root<=32&&root<=path.size();BlockPos previous=worldPosition;
                for(int i=0;i<path.size()&&valid;i++){var point=path.get(i);boolean column=root==0||i<root;valid=point.getY()<worldPosition.getY()&&point.getY()>=worldPosition.getY()-32&&previous.distManhattan(point)==1&&!deployedPipes.contains(point)&&(column?point.equals(previous.below()):point.getY()==previous.getY()&&Math.abs(point.getX()-worldPosition.getX())<=4&&Math.abs(point.getZ()-worldPosition.getZ())<=4);if(valid){deployedPipes.add(point);previous=point;}}
                if(!valid){deployedPipes.clear();running=false;}else if(root>0){branchRoot=root;for(var point:in.read("BranchPlan",BlockPos.CODEC.listOf(0,8)).orElse(List.of())){if(previous.distManhattan(point)!=1||point.getY()!=previous.getY()||Math.abs(point.getX()-worldPosition.getX())>4||Math.abs(point.getZ()-worldPosition.getZ())>4||deployedPipes.contains(point)||branchPlan.contains(point)){branchPlan.clear();break;}branchPlan.add(point);previous=point;}}
            }
            retracting=pipeMiner()&&in.getBooleanOr("Retracting",false)&&!deployedPipes.isEmpty();if(retracting){running=false;branchRoot=0;branchPlan.clear();}surveyVolume=in.read("SurveyVolume",UUIDUtil.CODEC).orElse(null);
        }
    }
    public static final class Menu extends AbstractContainerMenu {
        final Container container;public final BlockPos pos;public final ContainerData data;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,new SimpleContainer(27),new SimpleContainerData(12));}
        Menu(int id,Inventory inv,BlockPos pos,Container container,ContainerData data){super(MENU,id);this.container=container;this.pos=pos;this.data=data;addDataSlots(data);for(int i=0;i<27;i++){int slot=i;addSlot(new Slot(container,i,8+i%9*18,36+i/9*18){@Override public boolean mayPlace(ItemStack stack){return container.canPlaceItem(slot,stack);}});}for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,144+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,202));}
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||p.level().getBlockEntity(pos)==container&&container.stillValid(p);}
        @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p)||!(p instanceof ServerPlayer player)||!Authority.mayChange(player.level(),p,pos)||!(container instanceof Node node))return false;if(id==0&&!node.retracting)node.running=!node.running;else if(id==1&&!node.running)node.cursor=0;else if(id==2&&node.kind().equals("advanced_miner")&&!node.running){node.excludeFilter=!node.excludeFilter;node.cursor=0;}else if(id==5){return node.requestRetraction();}else if((id==3||id==4)&&node.kind().equals("filler")&&!node.running){if(id==3)node.fillerPattern=(node.fillerPattern+1)%FillerPatterns.NAMES.size();else node.invertPattern=!node.invertPattern;node.cursor=0;}else return false;node.setChanged();return true;}
        @Override public void clicked(int slot,int button,ContainerInput type,Player player){if(stillValid(player))super.clicked(slot,button,type,player);}
        @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var held=slot.getItem();var original=held.copy();if(index<27?!moveItemStackTo(held,27,63,true):!moveItemStackTo(held,0,14,false))return ItemStack.EMPTY;if(held.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return original;}
    }
}
