package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.control.*;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.*;
import io.github.jimbozoomer.jugcraft.logistics.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.*;

/** Source gates share the controller UI and colored data network; filters share real transfer APIs. */
public final class IndustrialAutomation {
    public static final Map<String,Block> BLOCKS = new LinkedHashMap<>();
    private IndustrialAutomation() {}
    public static void register() {
        IndustrialGateCopier.register();
        IndustrialGateExpansions.register();
        IndustrialGateParameters.register();
        for (String material : List.of("clay_brick","iron","nether_brick","gold")) {
            int rows = switch(material) { case "clay_brick" -> 1; case "nether_brick" -> 2; case "iron" -> 4; default -> 8; };
            for(boolean and:List.of(true,false)) {
                String id="jimta_gate_"+material+"_"+(and?"and":"or");
                var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));
                var block=new Gate(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),and,rows);
                put(id,block);
                ((FabricBlockEntityType)JugcraftControl.CONTROLLER_ENTITY).addValidBlock(block);
            }
        }
        var ik=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_item_filter"));
        var itemFilter=new ItemSorterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(ik).noOcclusion());
        put("jimta_item_filter",itemFilter);
        ((FabricBlockEntityType)JugcraftLogistics.SORTER_ENTITY).addValidBlock(itemFilter);
        var sortingKey=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_sorting_machine"));
        var sortingMachine=new ItemSorterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(sortingKey));
        put("jimta_sorting_machine",sortingMachine);
        ((FabricBlockEntityType)JugcraftLogistics.SORTER_ENTITY).addValidBlock(sortingMachine);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlocks(
                (level,pos,state,entity,side)->entity instanceof ItemSorterBlockEntity node?node.itemsFor(side):null,itemFilter,sortingMachine);
        var fk=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_fluid_filter"));
        var fluidFilter=new FluidFilterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(fk).noOcclusion(),1000);
        put("jimta_fluid_filter",fluidFilter);
        ((FabricBlockEntityType)JugcraftFluids.FILTER_ENTITY).addValidBlock(fluidFilter);
        for(String id:List.of("light_sensor","timer","pulsar")){
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("jimta_"+id));
            put("jimta_"+id,new SignalSensor(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key).noOcclusion(),id));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->BLOCKS.values().forEach(out::accept));
    }
    /** Environmental plugs supply ordinary 0–100 controller readings and real redstone output. */
    public static final class SignalSensor extends SensorBlock {
        public static final net.minecraft.world.level.block.state.properties.IntegerProperty PERIOD=net.minecraft.world.level.block.state.properties.IntegerProperty.create("period",1,60);
        private final String kind;
        public SignalSensor(Properties p,String kind){super(p);this.kind=kind;registerDefaultState(defaultBlockState().setValue(PERIOD,5));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(PERIOD);}
        @Override protected int reading(Level level,BlockPos pos,BlockState state){
            if(kind.equals("light_sensor"))return level.getMaxLocalRawBrightness(pos)*100/15;
            long period=state.getValue(PERIOD)*20L,phase=Math.floorMod(level.getGameTime(),period);
            return kind.equals("timer")?(int)(phase*100/period):phase<20?100:0;
        }
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
            if(!kind.equals("light_sensor")&&player.isShiftKeyDown()){
                if(level instanceof ServerLevel server){
                    if(!Authority.mayChange(server,player,pos))return net.minecraft.world.InteractionResult.FAIL;
                    int period=state.getValue(PERIOD)%60+1;level.setBlock(pos,state.setValue(PERIOD,period),Block.UPDATE_ALL);
                    player.sendOverlayMessage(Component.literal("Period: "+period+" seconds. Dye changes the controller channel."));
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return super.useWithoutItem(state,level,pos,player,hit);
        }
    }
    private static void put(String id,Block block) {
        Registry.register(BuiltInRegistries.BLOCK,Jugcraft.id(id),block);
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));
        Registry.register(BuiltInRegistries.ITEM,key,new BlockItem(block,new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        BLOCKS.put(id,block);
    }
    public static final class Gate extends LogicControllerBlock {
        public static final net.minecraft.world.level.block.state.properties.IntegerProperty EXPANSION = net.minecraft.world.level.block.state.properties.IntegerProperty.create("expansion", 0, 5);
        public static final net.minecraft.world.level.block.state.properties.IntegerProperty MODIFIER = net.minecraft.world.level.block.state.properties.IntegerProperty.create("modifier", 0, 3);
        public final boolean and;
        public final int rows;
        private static final Map<Direction,VoxelShape> SHAPES=Shapes.rotateHorizontal(Block.box(5,5,0,11,11,4.3));
        public Gate(Properties properties,boolean and,int rows) {super(properties);this.and=and;this.rows=rows;registerDefaultState(defaultBlockState().setValue(EXPANSION,0).setValue(MODIFIER,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { super.createBlockStateDefinition(b); b.add(EXPANSION,MODIFIER); }
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
            if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof GateEntity gate && gate.expansion()>0) {
                if (level instanceof ServerLevel server) {
                    if (player.isSpectator() || !Authority.mayChange(server,player,pos)) return net.minecraft.world.InteractionResult.FAIL;
                    var chip = new ItemStack(IndustrialGateExpansions.ITEMS.get(IndustrialGateExpansions.KINDS.get(gate.expansion()-1)));
                    gate.installExpansion(0);
                    if (!player.getInventory().add(chip)) player.drop(chip,false,net.minecraft.util.Prediction.SERVER_ONLY);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if(level.getBlockEntity(pos) instanceof GateEntity gate&&gate.modifier()>0) {
                if(player instanceof net.minecraft.server.level.ServerPlayer server) {
                    if(!Authority.mayChange(server.level(),player,pos)||player.isSpectator())return net.minecraft.world.InteractionResult.FAIL;
                    if(player.isShiftKeyDown()) {
                        var chip=new ItemStack(IndustrialGateParameters.ITEMS.get(IndustrialGateParameters.KINDS.get(gate.modifier()-1)));
                        gate.installModifier(0);if(!player.getInventory().add(chip))player.drop(chip,false,net.minecraft.util.Prediction.SERVER_ONLY);
                    } else IndustrialGateParameters.open(server,gate);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return super.useWithoutItem(state,level,pos,player,hit);
        }
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new GateEntity(pos,state);}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPES.get(state.getValue(FACING));}
        @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
            super.setPlacedBy(level,pos,state,placer,stack);
            if(placer!=null&&level.getBlockEntity(pos) instanceof GateEntity gate)gate.owner(placer.getUUID());
        }
    }
    public static final class GateEntity extends LogicControllerBlockEntity {
        private UUID owner;
        private int period=5, expansionChannel;
        private final int[] smoothed = new int[16];
        private final int[] parameters=new int[8*IndustrialGateParameters.FIELDS];
        public GateEntity(BlockPos pos,BlockState state) {super(pos,state);Arrays.fill(smoothed,-1);}
        public void owner(UUID value) {owner=value;setChanged();}
        private Gate gate() {return (Gate)getBlockState().getBlock();}
        public int modifier(){return getBlockState().getValue(Gate.MODIFIER);}
        public int ruleRows(){return Math.max(1,gate().rows/(modifier()>=2?2:1));}
        public int parameter(int row,int field){return parameters[row*IndustrialGateParameters.FIELDS+field];}
        public void parameter(int row,int field,int value){parameters[row*IndustrialGateParameters.FIELDS+field]=value;setChanged();}
        public void installModifier(int value){
            if(level==null||level.isClientSide())return;
            resetOutputs();
            Arrays.fill(parameters,0);
            level.setBlock(worldPosition,getBlockState().setValue(Gate.MODIFIER,Math.clamp(value,0,3)),Block.UPDATE_ALL);
            for(int row=ruleRows();row<8;row++)setRule(row,false,DyeColor.WHITE,true,50,DyeColor.WHITE,true);
            setChanged();
        }
        public int expansion() { return getBlockState().getValue(Gate.EXPANSION); }
        public void installExpansion(int value) {
            if (level==null || level.isClientSide()) return;
            resetOutputs();
            Arrays.fill(smoothed,-1);
            level.setBlock(worldPosition,getBlockState().setValue(Gate.EXPANSION,Math.clamp(value,0,5)),Block.UPDATE_ALL); setChanged();
        }
        public void adjustExpansion(boolean channel) { if(channel) expansionChannel=(expansionChannel+1)%16; else period=period%60+1; setChanged(); }
        public String expansionDescription() { return "channel "+DyeColor.byId(expansionChannel).getName()+", period "+period+" s (fader step "+period+"%)"; }
        private void resetOutputs(){if(level instanceof ServerLevel server)for(var channel:DyeColor.values())if(isOn(channel))toggle(server,channel);}
        @Override public void preRemoveSideEffects(BlockPos pos,BlockState state) {
            resetOutputs();
            if(level!=null&&!level.isClientSide()&&expansion()>0) Block.popResource(level,pos,new ItemStack(IndustrialGateExpansions.ITEMS.get(IndustrialGateExpansions.KINDS.get(expansion()-1))));
            if(level!=null&&!level.isClientSide()&&modifier()>0) Block.popResource(level,pos,new ItemStack(IndustrialGateParameters.ITEMS.get(IndustrialGateParameters.KINDS.get(modifier()-1))));
            super.preRemoveSideEffects(pos,state);
        }
        @Override public boolean click(int row,int operation) {return row>=0&&row<ruleRows()&&super.click(row,operation);}
        @Override public Component getDisplayName() {return getBlockState().getBlock().getName();}
        @Override protected ControlNetwork.Devices devicesFor(ServerLevel level,BlockPos pos) {
            var devices=super.devicesFor(level,pos);
            var actor=Authority.answering(level,owner);
            return new ControlNetwork.Devices(
                devices.sensors().stream().filter(p->ClaimTransfers.allowed(level,pos,p)&&Authority.mayChange(level,actor,p)).toList(),
                devices.switches().stream().filter(p->ClaimTransfers.allowed(level,pos,p)&&Authority.mayChange(level,actor,p)).toList(),
                devices.controllers(),devices.cables());
        }
        @Override protected boolean prepareReadings(int[] readings) {
            int[] before=readings.clone();
            if(level!=null && expansion()>0) {
                long cycle=period*20L, phase=Math.floorMod(level.getGameTime(),cycle);
                int local=switch(expansion()) {
                    case 1 -> level.getMaxLocalRawBrightness(worldPosition)*100/15;
                    case 2 -> (int)(phase*100/cycle);
                    case 3 -> phase<20?100:0;
                    case 4 -> (int)(Math.floorMod(level.getDefaultClockTime(),24000L)*100/24000);
                    default -> -1;
                };
                if(local>=0) readings[expansionChannel]=local;
                if(expansion()==5) for(int i=0;i<16;i++) {
                    if(readings[i]<0) smoothed[i]=-1;
                    else if(smoothed[i]<0) smoothed[i]=readings[i];
                    else smoothed[i]+=Math.clamp(readings[i]-smoothed[i],-period,period);
                    readings[i]=smoothed[i];
                }
            }
            return !Arrays.equals(before,readings);
        }
        /** Rows sharing an output channel are combined. OFF negates a condition, missing sensors always fail closed. */
        @Override public RuleResult evaluateRules(int previous,int[] readings) {
            int targets=0,states=previous;
            for(int channel=0;channel<16;channel++) {
                boolean any=false,result=gate().and;
                for(int row=0;row<ruleRows();row++) {
                    if(rule(row,ENABLED)==0)continue;
                    int reading=readings[rule(row,SENSOR)];
                    boolean available=reading>=0,condition=available&&(rule(row,ABOVE)==1?reading>rule(row,THRESHOLD):reading<rule(row,THRESHOLD));
                    for(int p=0;p<IndustrialGateParameters.conditions(modifier());p++) {
                        int offset=p*4;if(parameter(row,offset)==0)continue;
                        int sample=readings[parameter(row,offset+1)];available&=sample>=0;
                        condition&=sample>=0&&(parameter(row,offset+2)==1?sample>parameter(row,offset+3):sample<parameter(row,offset+3));
                    }
                    if(rule(row,TARGET)==channel) {
                        any=true;boolean holds=available&&(condition==(rule(row,ACTION)==1));result=gate().and?result&&holds:result||holds;
                    }
                    for(int p=0;p<IndustrialGateParameters.actions(modifier());p++) {
                        int offset=12+p*3;if(parameter(row,offset)==0||parameter(row,offset+1)!=channel)continue;
                        any=true;boolean holds=available&&(condition==(parameter(row,offset+2)==1));result=gate().and?result&&holds:result||holds;
                    }
                }
                if(any){targets|=1<<channel;states=result?states|(1<<channel):states&~(1<<channel);}
            }
            return new RuleResult(states,targets);
        }
        @Override protected void saveAdditional(ValueOutput output) {super.saveAdditional(output);if(owner!=null)output.store("GateOwner",UUIDUtil.CODEC,owner);output.putInt("ExpansionPeriod",period);output.putInt("ExpansionChannel",expansionChannel);output.putIntArray("ExpansionSmooth",smoothed.clone());output.putIntArray("GateParameters",parameters.clone());}
        @Override protected void loadAdditional(ValueInput input) {super.loadAdditional(input);owner=input.read("GateOwner",UUIDUtil.CODEC).orElse(null);period=Math.clamp(input.getIntOr("ExpansionPeriod",5),1,60);expansionChannel=Math.clamp(input.getIntOr("ExpansionChannel",0),0,15);Arrays.fill(smoothed,-1);int[] saved=input.getIntArray("ExpansionSmooth").orElse(new int[0]);for(int i=0;i<Math.min(16,saved.length);i++)smoothed[i]=Math.clamp(saved[i],-1,100);
            Arrays.fill(parameters,0);int[] params=input.getIntArray("GateParameters").orElse(new int[0]);for(int i=0;i<Math.min(params.length,parameters.length);i++){int f=i%IndustrialGateParameters.FIELDS;int max=f<12?(f%4==1?15:f%4==3?100:1):((f-12)%3==1?15:1);parameters[i]=Math.clamp(params[i],0,max);}}
    }
}
