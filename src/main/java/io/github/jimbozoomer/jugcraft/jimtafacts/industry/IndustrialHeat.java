package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.energy.*;
import io.github.jimbozoomer.jugcraft.chemistry.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.world.level.material.Fluids;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.kinetic.*;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
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
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;

/** Source industrial heat/kinetic casings on bounded native heat buffers, JE and the existing shaft network. */
public final class IndustrialHeat {
    public static final List<String> KINDS=List.of("solid_heat_generator","electric_heat_generator","stirling_generator","electric_kinetic_generator","manual_kinetic_generator","stirling_kinetic_generator","fluid_heat_generator","liquid_heat_exchanger","steam_generator","steam_kinetic_generator","radioisotope_heat_generator","wind_kinetic_generator","water_kinetic_generator","radioisotope_generator");
    public static final Map<String,Rotor> ROTORS=new LinkedHashMap<>();
    public static final Map<String,Item> BLADES=new LinkedHashMap<>();
    public static final List<String> ROTOR_NAMES=List.of("wood","iron","steel","carbon");
    public static final class Rotor extends Item {
        public final int tier,diameter,lifetime;
        Rotor(Properties p,int tier){super(p);this.tier=tier;diameter=5+2*tier;lifetime=new int[]{10800,86400,172800,604800}[tier];}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            super.appendHoverText(stack,context,display,lines,flag);lines.accept(Component.literal(diameter+" block diameter · "+(tier==0?"wind only":"wind or flowing water")));
            int used=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("RotorTicks",0);
            lines.accept(Component.literal(Math.max(0,lifetime-used/20)+" working seconds remaining"));
        }
    }
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<Node> TYPE;
    public static ExtendedMenuType<Menu,BlockPos> MENU;
    public static PetroFluids.Entry COOLANT,HOT_COOLANT,HOT_WATER,PAHOEHOE_LAVA,CONSTRUCTION_FOAM;
    public static PetroFluids.Gas STEAM,SUPERHEATED_STEAM;
    private IndustrialHeat(){}
    public static void register(){
        for(int tier=0;tier<ROTOR_NAMES.size();tier++){
            String name=ROTOR_NAMES.get(tier);var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+name+"_rotor"));
            ROTORS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new Rotor(new Item.Properties().setId(key).stacksTo(1),tier)));
            var blade=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+name+"_rotor_blade"));BLADES.put(name,Registry.register(BuiltInRegistries.ITEM,blade,new Item(new Item.Properties().setId(blade))));
        }
        COOLANT=PetroFluids.registerOwnerLiquid("jimta_coolant",0xFF467CB4);
        HOT_COOLANT=PetroFluids.registerOwnerLiquid("jimta_hot_coolant",0xFFDB6A35);
        HOT_WATER=PetroFluids.registerOwnerLiquid("jimta_hot_water",0xFF6198C4);
        PAHOEHOE_LAVA=PetroFluids.registerOwnerLiquid("jimta_pahoehoe_lava",0xFF9B542B);
        CONSTRUCTION_FOAM=PetroFluids.registerOwnerLiquid("jimta_construction_foam_fluid",0xFFD8D4C6);
        STEAM=PetroFluids.registerOwnerGas("jimta_steam",0xFFD9E0E4);
        SUPERHEATED_STEAM=PetroFluids.registerOwnerGas("jimta_superheated_steam",0xFFD0DCE0);
        for(String kind:KINDS){var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+kind));var block=new Device(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).setId(key),kind);Registry.register(BuiltInRegistries.BLOCK,key,block);BLOCKS.put(kind,block);var item=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,item,new BlockItem(block,new Item.Properties().setId(item).useBlockDescriptionPrefix()));}
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("jimta_heat_workshop"),FabricBlockEntityTypeBuilder.create(Node::new,BLOCKS.values().toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("jimta_heat_workshop"),new ExtendedMenuType<>(Menu::new,BlockPos.STREAM_CODEC));
        EnergyStorage.SIDED.registerForBlockEntity((n,side)->n.energy,TYPE);
        FluidStorage.SIDED.registerForBlockEntity((n,side)->n.fluidStorage(side),TYPE);
        ItemStorage.SIDED.registerForBlockEntity((n,side)->n.usesFuel()?ContainerStorage.of(n,side):null,TYPE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{BLOCKS.values().forEach(out::accept);out.accept(COOLANT.bucket());out.accept(HOT_COOLANT.bucket());out.accept(HOT_WATER.bucket());out.accept(PAHOEHOE_LAVA.bucket());out.accept(CONSTRUCTION_FOAM.bucket());ROTORS.values().forEach(out::accept);BLADES.values().forEach(out::accept);});
    }
    static final class Device extends Block implements EntityBlock,EnergyConnectable {
        static final EnumProperty<Direction> FACING=HorizontalDirectionalBlock.FACING;
        static final BooleanProperty LIT=BlockStateProperties.LIT;final String kind;
        Device(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Node(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide()&&type==TYPE?(world,p,state,e)->((Node)e).step((ServerLevel)world):null;}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){if(placer!=null&&l.getBlockEntity(p) instanceof Node n){n.owner=placer.getUUID();n.setChanged();}}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){if(!s.is(old.getBlock()))KineticNetworks.invalidate(l);}
        @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){KineticNetworks.invalidate(l);EnergyNetworks.invalidate(l);}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(level.getBlockEntity(pos) instanceof Node n&&n.hasFluids()){
                var held=FluidStorage.ITEM.find(stack,net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(stack));
                if(held!=null){
                    if(!Authority.mayChange(level,player,pos)||player.isSpectator())return InteractionResult.FAIL;
                    if(!level.isClientSide())FluidStorageUtil.interactWithFluidStorage(n.fluidStorage(hit.getDirection()),player,hand);
                    return InteractionResult.SUCCESS;
                }
            }
            if(!(level.getBlockEntity(pos) instanceof Node fuelNode)||!fuelNode.canPlaceItem(0,stack))return InteractionResult.PASS;
            if(level instanceof ServerLevel&&level.getBlockEntity(pos) instanceof Node n&&Authority.mayChange(level,player,pos)){
                var fuel=n.fuel;if(!fuel.isEmpty()&&!ItemStack.isSameItemSameComponents(fuel,stack))return InteractionResult.FAIL;
                int count=Math.min(stack.getCount(),stack.getMaxStackSize()-fuel.getCount());if(count<=0)return InteractionResult.FAIL;
                n.fuel=stack.split(count).copyWithCount(count+fuel.getCount());n.setChanged();return InteractionResult.SUCCESS;
            }return level.isClientSide()?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(player instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof Node n&&Authority.mayChange(level,p,pos)){
                if(kind.equals("manual_kinetic_generator"))n.crank(p);
                else p.openMenu(n);
                p.sendOverlayMessage(Component.literal(n.description()));
            }return InteractionResult.SUCCESS;
        }
    }
    public static final class Node extends BlockEntity implements WorldlyContainer,ExtendedMenuProvider<BlockPos> {
        public UUID owner;public int heat,burn;private long crankedAt=Long.MIN_VALUE;private ItemStack fuel=ItemStack.EMPTY;private boolean superheated;
        public final SimpleEnergyStorage energy;
        public final FluidTank input,output;private int kineticCredit;public int visibleRotor=-1;private long lastRotorCheck=Long.MIN_VALUE;private int rotorRate;
        private final ContainerData data=new ContainerData(){
            public int get(int id){return switch(id){case 0->KINDS.indexOf(kind());case 1->heat;case 2->(int)(energy.getAmount()/10);case 3->burn;case 4->input.millibuckets();case 5->output.millibuckets();case 6->getBlockState().getValue(Device.LIT)?1:0;case 7->level!=null&&level.hasNeighborSignal(worldPosition)?1:0;case 8->superheated?1:0;case 9->input.variant.isOf(SUPERHEATED_STEAM.fluid())?1:0;default->0;};}
            public void set(int id,int value){}public int getCount(){return 10;}
        };
        @Override public Component getDisplayName(){return Component.translatable("block.jugcraft.jimta_"+kind());}
        @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new Menu(id,inventory,worldPosition,this,data);}

        public Node(BlockPos p,BlockState s){super(TYPE,p,s);boolean receivesEnergy=kind().startsWith("electric_");energy=new SimpleEnergyStorage(40000,receivesEnergy?256:0,(kind().equals("stirling_generator")||kind().equals("radioisotope_generator"))?128:0,this::setChanged);
            input=new FluidTank(16000,true,this::acceptsFluid,this::setChanged);
            output=new FluidTank(16000,false,v->true,this::setChanged);
        }
        public boolean usesFuel(){return switch(kind()){case "solid_heat_generator","radioisotope_heat_generator","radioisotope_generator","wind_kinetic_generator","water_kinetic_generator"->true;default->false;};}
        public boolean isRotor(){return kind().equals("wind_kinetic_generator")||kind().equals("water_kinetic_generator");}
        public boolean hasFluids(){return switch(kind()){case "fluid_heat_generator","liquid_heat_exchanger","steam_generator","steam_kinetic_generator"->true;default->false;};}
        private boolean acceptsFluid(FluidVariant v){return switch(kind()){
            case "fluid_heat_generator"->fuelHeat(v)>0;
            case "liquid_heat_exchanger"->cooledFluid(v)!=null;
            case "steam_generator"->v.isOf(Fluids.WATER)||v.isOf(HOT_WATER.source())||IndustrialFluidServices.DISTILLED!=null&&v.isOf(IndustrialFluidServices.DISTILLED.source());
            case "steam_kinetic_generator"->v.isOf(STEAM.fluid())||v.isOf(SUPERHEATED_STEAM.fluid());default->false;};}
        public Storage<FluidVariant> fluidStorage(Direction side){
            if(!hasFluids())return null;
            if(side==null)return new CombinedStorage<>(List.of(input,output));
            return side==Direction.DOWN?output:input;
        }
        private static net.minecraft.world.level.material.Fluid cooledFluid(FluidVariant v){
            if(v.isOf(HOT_COOLANT.source()))return COOLANT.source();
            if(v.isOf(Fluids.LAVA))return PAHOEHOE_LAVA.source();
            if(v.isOf(HOT_WATER.source()))return Fluids.WATER;
            return null;
        }
        private static int fuelHeat(FluidVariant v){
            if(v.isBlank())return 0;
            // Same fuel budget as the existing generator, before Stirling's 50% conversion loss.
            return Math.max(FluidFuels.jePerMb(MachineKind.DIESEL_GENERATOR,v.getFluid()),FluidFuels.jePerMb(MachineKind.GAS_TURBINE,v.getFluid()));
        }
        public String kind(){return ((Device)getBlockState().getBlock()).kind;}
        private Direction facing(){return getBlockState().getValue(Device.FACING);}
        private boolean heatSource(){return switch(kind()){case "solid_heat_generator","electric_heat_generator","fluid_heat_generator","liquid_heat_exchanger","radioisotope_heat_generator"->true;default->false;};}
        private Node heatInput(ServerLevel l){
            var at=worldPosition.relative(facing());
            if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof Node source&&source.heatSource()&&source.facing()==facing().getOpposite()&&source.permitted(l)&&ClaimTransfers.allowed(l,at,worldPosition))return source;
            return null;
        }
        private boolean permitted(ServerLevel l){var p=Authority.answering(l,owner);return p!=null&&Authority.mayChange(l,p,worldPosition);}
        public void step(ServerLevel l){
            boolean active=false;
            if(permitted(l)&&!l.hasNeighborSignal(worldPosition)){
                if(kind().equals("solid_heat_generator")&&heat<=4096-8){
                    if(burn==0&&GeneratorFuels.burnTicks(fuel)>0){burn=GeneratorFuels.burnTicks(fuel);fuel.shrink(1);}
                    if(burn>0){burn--;heat+=8;active=true;}
                }else if(kind().equals("radioisotope_heat_generator")&&heat<=4092){
                    if(burnIsotope()){heat+=4;active=true;}
                }else if(kind().equals("radioisotope_generator")&&energy.getAmount()<=energy.getCapacity()-2){
                    if(burnIsotope()){energy.setAmount(energy.getAmount()+2);active=true;}
                }else if(kind().equals("electric_heat_generator")){
                    int batches=(int)Math.min(64,(4096-heat)/4);batches=(int)Math.min(batches,energy.getAmount()/5);
                    if(batches>0){energy.setAmount(energy.getAmount()-batches*5L);heat+=batches*4;active=true;}
                }else if(kind().equals("fluid_heat_generator")){
                    int value=fuelHeat(input.variant);
                    if(value>0&&input.millibuckets()>=1&&heat<=4096-value){input.drain(1);heat+=value;active=true;}
                }else if(kind().equals("liquid_heat_exchanger")){
                    var cooled=cooledFluid(input.variant);int perMb=input.variant.isOf(HOT_WATER.source())?1:4;
                    int count=Math.min(64,Math.min(input.millibuckets(),Math.min((4096-heat)/perMb,output.capacityMb()-output.millibuckets())));
                    if(cooled!=null&&count>0&&output.fits(cooled,count)){input.drain(count);output.fill(cooled,count);heat+=count*perMb;active=true;}
                }else if(kind().equals("steam_generator")){
                    Node source=heatInput(l);
                    int heatCost=superheated?12:4;var produced=superheated?SUPERHEATED_STEAM.fluid():STEAM.fluid();
                    // Native pressure interlock: leave 5% gas headroom instead of venting or exploding builds.
                    int count=source==null?0:Math.max(0,Math.min(32,Math.min(source.heat/heatCost,Math.min(input.millibuckets(),(output.capacityMb()*95/100-output.millibuckets())/10))));
                    if(count>0&&acceptsFluid(input.variant)&&input.millibuckets()>=count&&output.fits(produced,count*10)){
                        source.heat-=count*heatCost;source.setChanged();input.drain(count);output.fill(produced,count*10);active=true;
                    }
                }else if(kind().equals("steam_kinetic_generator")){
                    // Reserve a batch only when its condensate fits; retained KE survives a disconnected shaft.
                    boolean hot=input.variant.isOf(SUPERHEATED_STEAM.fluid());var produced=hot?STEAM.fluid():Fluids.WATER;int ratio=hot?10:1,ke=hot?4:2;
                    int batches=Math.min(64/ke,Math.min(input.millibuckets()/10,(output.capacityMb()-output.millibuckets())/ratio));
                    if(kineticCredit==0&&batches>0&&(hot||input.has(STEAM.fluid(),batches*10))&&output.fits(produced,batches*ratio)){
                        input.drain(batches*10);output.fill(produced,batches*ratio);kineticCredit=batches*ke;setChanged();
                    }
                    if(kineticCredit>0){long accepted=KineticNetworks.push(l,worldPosition,facing(),kineticCredit);kineticCredit-=Math.toIntExact(accepted);active=accepted>0;}
                }else if(isRotor()&&fuel.getItem() instanceof Rotor rotor){
                    if(lastRotorCheck==Long.MIN_VALUE||l.getGameTime()-lastRotorCheck>=20){lastRotorCheck=l.getGameTime();rotorRate=rotorOutput(l,rotor);}
                    if(rotorRate>0&&KineticNetworks.push(l,worldPosition,facing().getOpposite(),rotorRate)>0){
                        active=true;var tag=fuel.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                        int used=Math.clamp(tag.getIntOr("RotorTicks",0),0,rotor.lifetime*20)+1;
                        if(used>=rotor.lifetime*20)fuel=ItemStack.EMPTY;
                        else net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,fuel,value->value.putInt("RotorTicks",used));
                    }
                }else if(kind().equals("electric_kinetic_generator")){
                    long available=Math.min(64,energy.getAmount()*4/5);long accepted=KineticNetworks.push(l,worldPosition,facing(),available);
                    if(accepted>0){energy.setAmount(energy.getAmount()-(accepted*5+3)/4);active=true;}
                }else if(kind().startsWith("stirling_")){
                    var at=worldPosition.relative(facing());
                    if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof Node source&&source.heatSource()&&source.facing()==facing().getOpposite()&&source.permitted(l)&&ClaimTransfers.allowed(l,at,worldPosition)){
                        int available=Math.min(64,source.heat/2);long accepted;
                        if(kind().equals("stirling_generator")){accepted=Math.min(available,energy.getCapacity()-energy.getAmount());energy.setAmount(energy.getAmount()+accepted);}
                        else accepted=KineticNetworks.push(l,worldPosition,facing().getOpposite(),available);
                        if(accepted>0){source.heat-=Math.toIntExact(accepted*2);source.setChanged();active=true;}
                    }
                }
                if(kind().equals("stirling_generator")||kind().equals("radioisotope_generator"))EnergyNetworks.pushToNeighbors(l,worldPosition,energy,128,EnumSet.complementOf(EnumSet.of(facing())));
            }
            if(kind().equals("manual_kinetic_generator"))active=crankedAt!=Long.MIN_VALUE&&l.getGameTime()-crankedAt<5;
            if(getBlockState().getValue(Device.LIT)!=active)l.setBlock(worldPosition,getBlockState().setValue(Device.LIT,active),Block.UPDATE_CLIENTS);
            if(active)setChanged();
        }
        private boolean burnIsotope(){
            if(!fuel.is(CompactReactor.PARTS.get("jimta_rtg_pellet")))return false;
            var data=fuel.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            int used=Math.clamp(data.getIntOr("RtgTicks",0),0,400000);if(used==400000)return false;
            int next=used+1;if(next==400000)fuel=ItemStack.EMPTY;
            else net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,fuel,tag->{tag.putInt("RtgTicks",next);tag.putInt("Used",next/20);});
            return true;
        }
        private int rotorOutput(ServerLevel l,Rotor rotor){
            boolean water=kind().equals("water_kinetic_generator");if(water&&rotor.tier==0)return 0;
            int radius=(rotor.diameter+1)/2,waterCount=0,flowing=0;var actor=Authority.answering(l,owner);var hub=worldPosition.relative(facing());
            if(actor==null||!l.hasChunkAt(hub)||!water&&!l.canSeeSky(hub.above(radius)))return 0;
            for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++){
                var at=hub.relative(facing().getClockWise(),x).above(y);
                if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at)||!ClaimTransfers.allowed(l,worldPosition,at))return 0;
                var state=l.getBlockState(at);var liquid=state.getFluidState();
                if(!state.getCollisionShape(l,at).isEmpty())return 0;
                if(water){if(liquid.is(net.minecraft.tags.FluidTags.WATER)){waterCount++;if(!liquid.isSource())flowing++;}else if(!liquid.isEmpty())return 0;}
                else if(!liquid.isEmpty())return 0;
            }
            int rate;
            if(water){if(waterCount<rotor.diameter*rotor.diameter/2||flowing<2)return 0;rate=Math.min(64,flowing*4);}
            else {rate=Math.min(MachineKind.WIND_MAX_PER_TICK,MachineKind.WIND_BASE_PER_TICK+Math.max(0,worldPosition.getY()-l.getSeaLevel())/2);if(l.isThundering())rate*=2;else if(l.isRaining())rate=rate*3/2;}
            return Math.max(1,rate*(rotor.tier+1)/4);
        }
        @Override public void setChanged(){
            super.setChanged();if(level!=null&&!level.isClientSide()&&isRotor()){
                int tier=fuel.getItem() instanceof Rotor rotor?rotor.tier:-1;
                if(tier!=visibleRotor){visibleRotor=tier;lastRotorCheck=Long.MIN_VALUE;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
            }
        }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("render_only",true);tag.putInt("rotor",fuel.getItem() instanceof Rotor rotor?rotor.tier:-1);return tag;}
        public long crank(ServerPlayer player){
            var l=player.level();long now=l.getGameTime();if(player.isSpectator()||player.level()!=level||!Authority.mayChange(l,player,worldPosition)||l.hasNeighborSignal(worldPosition)||crankedAt!=Long.MIN_VALUE&&now-crankedAt<5)return 0;
            crankedAt=now;return KineticNetworks.push(l,worldPosition,facing(),16);
        }
        public String description(){return switch(kind()){
            case "fluid_heat_generator","liquid_heat_exchanger","steam_generator","steam_kinetic_generator"->
                "Input "+input.millibuckets()+" / 16000 mB · output "+output.millibuckets()+" / 16000 mB · heat "+heat+" / 4096 · bottom: fluid output; other sides: input";
            case "wind_kinetic_generator","water_kinetic_generator"->"Rotor in front · shaft output behind · keep full swept area clear · "+rotorRate+" KE/t available";
            case "radioisotope_generator"->energy.getAmount()+" / 40000 JE · 2 JE/t · finite RTG pellet";
            case "radioisotope_heat_generator"->"4 heat per tick · finite RTG pellet · pauses when full";
            case "solid_heat_generator"->"Fuel: "+fuel.getCount()+" · heat "+heat+" / 4096 · hot face points into Stirling's inlet";
            case "electric_heat_generator"->"Heat "+heat+" / 4096 · "+energy.getAmount()+" JE · 5 JE produces 4 heat";
            case "stirling_generator"->energy.getAmount()+" / 40000 JE · 2 heat produces 1 JE · front faces the heat source";
            case "stirling_kinetic_generator"->"2 heat produces 1 KE · front receives heat, rear drives shafts";
            case "electric_kinetic_generator"->energy.getAmount()+" JE · front drives shafts at 80% efficiency";
            default->"Hand-cranked shaft power · 16 KE per use, at most every five ticks";
        };}
        @Override public int getContainerSize(){return 1;}@Override public boolean isEmpty(){return fuel.isEmpty();}
        @Override public ItemStack getItem(int slot){return slot==0?fuel:ItemStack.EMPTY;}
        @Override public ItemStack removeItem(int slot,int amount){if(slot!=0)return ItemStack.EMPTY;var out=fuel.split(amount);setChanged();return out;}
        @Override public ItemStack removeItemNoUpdate(int slot){if(slot!=0)return ItemStack.EMPTY;var out=fuel;fuel=ItemStack.EMPTY;return out;}
        @Override public void setItem(int slot,ItemStack stack){if(slot==0){fuel=stack;setChanged();}}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&(kind().equals("solid_heat_generator")&&GeneratorFuels.burnTicks(stack)>0||(kind().equals("radioisotope_heat_generator")||kind().equals("radioisotope_generator"))&&stack.is(CompactReactor.PARTS.get("jimta_rtg_pellet"))||isRotor()&&stack.getItem() instanceof Rotor rotor&&(!kind().equals("water_kinetic_generator")||rotor.tier>0));}
        @Override public boolean stillValid(Player p){return Container.stillValidBlockEntity(this,p)&&Authority.mayChange(level,p,worldPosition);}
        @Override public void clearContent(){fuel=ItemStack.EMPTY;setChanged();}
        @Override public int[] getSlotsForFace(Direction side){return new int[]{0};}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
        @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level!=null&&!level.isClientSide())Containers.dropContents(level,pos,this);clearContent();}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putBoolean("superheated",superheated);out.store("fuel",ItemStack.OPTIONAL_CODEC,fuel);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);input.writeValue(out.child("fluid_input"));output.writeValue(out.child("fluid_output"));out.putInt("kinetic_credit",kineticCredit);out.putInt("heat",heat);out.putInt("burn",burn);out.putLong("energy",energy.getAmount());}
        @Override protected void loadAdditional(ValueInput in){if(in.getBooleanOr("render_only",false)){visibleRotor=Math.clamp(in.getIntOr("rotor",-1),-1,3);return;}super.loadAdditional(in);superheated=in.getBooleanOr("superheated",false);fuel=in.read("fuel",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);input.readValue(in.childOrEmpty("fluid_input"));output.readValue(in.childOrEmpty("fluid_output"));kineticCredit=Math.clamp(in.getIntOr("kinetic_credit",0),0,64);heat=Math.clamp(in.getIntOr("heat",0),0,4096);burn=Math.clamp(in.getIntOr("burn",0),0,16000);energy.setAmount(in.getLongOr("energy",0));}
    }
    public static final class Menu extends AbstractContainerMenu {
        private final Container node;public final BlockPos pos;public final ContainerData data;
        public Menu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,new SimpleContainer(1),new SimpleContainerData(10));}
        Menu(int id,Inventory inv,BlockPos pos,Container node,ContainerData data){super(MENU,id);this.node=node;this.pos=pos;this.data=data;addDataSlots(data);
            addSlot(new Slot(node,0,20,59){@Override public boolean mayPlace(ItemStack s){return acceptsFuel(s);}@Override public boolean isActive(){return data.get(0)==0||data.get(0)>=10;}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,143+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,201));
        }
        private boolean acceptsFuel(ItemStack s){return data.get(0)==0?GeneratorFuels.burnTicks(s)>0:(data.get(0)==10||data.get(0)==13)?s.is(CompactReactor.PARTS.get("jimta_rtg_pellet")):(data.get(0)==11||data.get(0)==12)&&s.getItem() instanceof Rotor rotor&&(data.get(0)!=12||rotor.tier>0);}
        @Override public boolean clickMenuButton(Player p,int id){
            if(id!=0||!(p instanceof ServerPlayer player)||!stillValid(p)||!(node instanceof Node boiler)||!boiler.kind().equals("steam_generator")
                    ||!io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(player,"jimta_boiler_mode",4))return false;
            if(boiler.output.millibuckets()>0){player.sendOverlayMessage(Component.literal("Drain the output before changing steam mode."));return false;}
            boiler.superheated=!boiler.superheated;boiler.setChanged();broadcastChanges();return true;
        }
        @Override public boolean stillValid(Player p){return p.level().isClientSide()||p.level().getBlockEntity(pos)==node&&node.stillValid(p);}
        @Override public void clicked(int slot,int button,ContainerInput type,Player p){if(stillValid(p))super.clicked(slot,button,type,p);}
        @Override public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);var s=slot.getItem();var copy=s.copy();if(i==0?!moveItemStackTo(s,1,slots.size(),true):!acceptsFuel(s)||!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;slot.setByPlayer(s.isEmpty()?ItemStack.EMPTY:s);slot.setChanged();return copy;}
    }
}
