package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.workshop.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;
import org.slf4j.LoggerFactory;

/** Only the stock-target extension: native crafting patterns, deliveries, power and redstone. */
public final class PeepoCraftingStockClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc,other;
    private BlockEntity station;
    private Container grid,source,output;
    private int assertions;
    private void check(boolean ok,String text){assertions++;if(!ok)throw new AssertionError(text);}
    private void phase(String text){LoggerFactory.getLogger("peepo-crafting-stock").info("[peepo-crafting-stock] {}",text);}
    private CompanionProductionStock stock(){return CompanionProductionStock.of(station);}
    private CompanionRecipeFilter filter(){return CompanionFilters.get(station);}
    private CompanionLogistics.Port port(){return CompanionLogistics.resolve(npc,npc.assignments.get(1));}
    private static int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private static int count(List<ItemStack> items,Item item){return items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private void cleanup(){for(var p:new PeepoEntity[]{npc,other})if(p!=null){p.transport.releaseStock();p.transport.stop();p.resetCompanionRoutine();p.discard();}npc=other=null;}
    private PeepoEntity helper(ServerLevel level,boolean jughead){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);p.setNoAi(true);
        p.snapTo(base.getX()-2.5,base.getY(),base.getZ()-3.5,0,0);p.setOnGround(true);level.addFreshEntity(p);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();p.orders.tame(owner);p.orders.command(owner,3);p.preferences.social=false;p.preferences.carryMeals=0;
        p.assignments.assign(level,base);p.assignments.assignContainer(level,base.west(5),Direction.UP,false);p.assignments.assignContainer(level,base.east(5),Direction.UP,true);return p;
    }
    private void reset(ServerLevel level,Block block){
        cleanup();for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-7),base.offset(8,4,7)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var state=block.defaultBlockState();level.setBlockAndUpdate(base,state);
        if(block instanceof MachineBlock m){var fp=m.footprint(state);for(int i=1;i<fp.size();i++)level.setBlock(fp.partPos(base,state.getValue(MachineBlock.FACING),i),state.setValue(LargeMachineBlock.PART,i),2);}
        if(block instanceof WorkshopBlock w)w.setPlacedBy(level,base,state,null,ItemStack.EMPTY);
        station=level.getBlockEntity(base);grid=(Container)station;
        level.setBlockAndUpdate(base.west(5),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(5));
        level.setBlockAndUpdate(base.east(5),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(5));
        npc=helper(level,station instanceof MachineBlockEntity);check(stock()!=null,"station exposes stock controls: "+block);
    }
    private void goal(Item item,int n){filter().set(0,new ItemStack(item));stock().useOutputs(npc,npc.assignments.get(1));stock().set(new ItemStack(item),n,0);}
    private void seed(ServerLevel level,boolean auto,boolean honey){
        reset(level,auto?JugcraftMachines.MACHINES.get(MachineKind.AUTO_CRAFTER):Blocks.CRAFTER);
        int[] used=honey?new int[]{0,1,3,4}:new int[]{0,1,2};
        for(int i:used)grid.setItem(i,new ItemStack(honey?Items.HONEY_BOTTLE:Items.WHEAT));
        if(station instanceof CrafterBlockEntity c)for(int i=0;i<9;i++)if(c.getItem(i).isEmpty())c.setSlotState(i,false);
        source.setItem(0,new ItemStack(honey?Items.HONEY_BOTTLE:Items.WHEAT,honey?16:48));
        port().prepare(ContainerStorage.of(source,Direction.UP));goal(honey?Items.HONEY_BLOCK:Items.BREAD,auto?1:2);
    }
    private List<ItemStack> batch(){var port=port();port.prepare(ContainerStorage.of(source,Direction.UP));return port.supplyBatch(ContainerStorage.of(source,Direction.UP),8);}
    private void insert(List<ItemStack> items){try(var scope=MachineItemAutomation.companionTransfer();var tx=Transaction.openOuter()){
        for(var stack:items)check(port().inputs().insert(ItemVariant.of(stack),stack.getCount(),tx)==stack.getCount(),"physical delivery fits native grid: "+stack);tx.commit();
    }}
    private void carry(PeepoEntity p,List<ItemStack> stacks){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());
        for(int i=0;i<8;i++)p.belongings.setItem(i,i<stacks.size()?stacks.get(i).copy():ItemStack.EMPTY);
        if(!stacks.isEmpty()){
            var c=out.child("Transport");c.store("Manifest",ItemStack.CODEC,stacks.getFirst());c.putInt("Slot",0);c.putBoolean("Supply",true);c.putBoolean("StockTracked",true);
            CompanionTransport.saveTarget(c,"Work",p.assignments.get(1));CompanionTransport.saveTarget(c,"Store",p.assignments.get(CompanionAssignments.SUPPLY));
            if(port().plan()!=null)c.store("Recipe",Identifier.CODEC,port().plan());
            for(int i=1;i<stacks.size();i++){var entry=c.child("Pending"+(i-1));entry.putInt("Slot",i);entry.store("Stack",ItemStack.CODEC,stacks.get(i));}
        }
        p.transport.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),out.buildResult()));
    }
    private void planning(ServerLevel level,boolean auto){
        seed(level,auto,false);var result=new ItemStack(Items.BREAD);
        check(stock().count(npc,station,result).underway()==(auto?0:1),"only spendable ingredients count; Auto-Crafter retains its template");
        var planned=batch();check(count(planned,Items.WHEAT)==3,"one complete bread batch fetched: auto="+auto+", batch="+planned+", native need="+port().needed(new ItemStack(Items.WHEAT))+", pickup="+port().pickupNeeded(new ItemStack(Items.WHEAT))+", stock="+stock().count(npc,station,result));
        check(count(source,Items.WHEAT)==48 && count(grid,Items.WHEAT)==3,"planning rolls back both real inventories");
        other=helper(level,!auto);carry(npc,planned);
        check(stock().count(other,station,result).underway()==(auto?1:2),"physical inbound batch counted once");
        var second=CompanionLogistics.resolve(other,other.assignments.get(1));second.prepare(ContainerStorage.of(source,Direction.UP));
        check(second.pickupNeeded(new ItemStack(Items.WHEAT))==0,"shared reservation blocks duplicate delivery");
        carry(npc,List.of());insert(planned);
        check(stock().count(npc,station,result).underway()==(auto?1:2),"installed grid keeps same projected yield");
        check(batch().isEmpty(),"complete queued crafts stop restocking");
        for(int i=0;i<3;i++)grid.setItem(i,auto?new ItemStack(Items.WHEAT):ItemStack.EMPTY);
        stock().set(result,1,0);planned=batch();check(count(planned,Items.WHEAT)==3,"one-batch refill after consumption");insert(planned);
        for(int i=0;i<3;i++)check(grid.getItem(i).getCount()==(auto?2:1),"one layer distributed across repeated ingredient slots "+i);
        for(int i=0;i<3;i++)grid.setItem(i,auto?new ItemStack(Items.WHEAT):ItemStack.EMPTY);
        grid.setItem(0,new ItemStack(Items.WHEAT,auto?4:3));
        check(stock().count(npc,station,result).underway()==0,"uneven ingredients do not falsely count as a complete craft");
        check(count(batch(),Items.WHEAT)==2,"surplus in one slot does not replace missing ingredients in other slots");
        output.setItem(0,result.copy());stock().invalidate();check(batch().isEmpty(),"counted Output stops new pickups");
        check(port().needed(new ItemStack(Items.WHEAT))>0,"previously committed cargo still accepted after target changes");
        stock().set(result,0,0);check(!batch().isEmpty(),"Unlimited preserves existing restocking");
        stock().set(result,1,0);
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        if(auto)((MachineBlockEntity)station).companionPort.save(saved);else VanillaCompanionPort.of(station).save(saved);
        var input=TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult());
        if(auto)((MachineBlockEntity)station).companionPort.load(input);else VanillaCompanionPort.of(station).load(input);
        check(stock().target(result)==1 && stock().outputs().size()==1,"goals and counted Outputs survive native port reload");
        check(CompanionCraftingStock.plan(station)!=null,"configured pattern survives reload");
        // Pattern components, disabled slots and native output yield all remain authoritative.
        seed(level,auto,false);if(!auto)for(int i=0;i<3;i++)grid.setItem(i,ItemStack.EMPTY);
        var named=new ItemStack(Items.WHEAT);named.set(DataComponents.CUSTOM_NAME,Component.literal("Different wheat"));
        source.setItem(0,named.copyWithCount(12));check(batch().isEmpty(),"same item with different components cannot replace exact pattern ingredients");
        if(!auto){check(VanillaCompanionPort.of(station).acceptsFilter(result),"empty remembered grid can still edit its product target");((CrafterBlockEntity)station).setSlotState(0,false);check(batch().isEmpty(),"disabling a required slot stops restocking safely");}
        seed(level,auto,false);for(int i=0;i<9;i++)grid.setItem(i,ItemStack.EMPTY);
        if(station instanceof CrafterBlockEntity c)for(int i=0;i<9;i++)c.setSlotState(i,i==0 || i==3);
        grid.setItem(0,new ItemStack(Items.OAK_PLANKS));grid.setItem(3,new ItemStack(Items.OAK_PLANKS));source.clearContent();source.setItem(0,new ItemStack(Items.OAK_PLANKS,32));goal(Items.STICK,1);batch();
        if(!auto){grid.setItem(0,ItemStack.EMPTY);grid.setItem(3,ItemStack.EMPTY);}
        var sticks=batch();check(count(sticks,Items.OAK_PLANKS)==2,"whole craft retained when its four-stick yield exceeds target of one");insert(sticks);
        check(stock().count(npc,station,new ItemStack(Items.STICK)).underway()==4,"native multi-item yield counted once");
    }
    private void coverage(ServerLevel level){
        for(var b:JugcraftWorkshop.BLOCKS){reset(level,b);check(CompanionStockRecipes.supports(station),"all three starter workshops covered");}
        for(var kind:List.of(MachineKind.ELECTRIC_FURNACE,MachineKind.CRUSHER,MachineKind.PULVERIZER,MachineKind.SAWMILL,MachineKind.ALLOY_SMELTER,MachineKind.METAL_PRESS,MachineKind.WIRE_DRAWER,MachineKind.CIRCUIT_ASSEMBLER)){
            reset(level,JugcraftMachines.MACHINES.get(kind));check(((MachineBlockEntity)station).companionPort.supportsStockTargets(),"ordinary processor catalog covered: "+kind);
        }
    }
    private void power(){if(station instanceof MachineBlockEntity m)try(var tx=Transaction.openOuter()){for(int i=0;i<100;i++)m.energyFor(null).insert(128,tx);tx.commit();}}
    private void pulse(ServerLevel level){
        if(!(station instanceof CrafterBlockEntity))return;
        var pos=base.east();var block=level.getGameTime()%20<5?Blocks.REDSTONE_BLOCK:Blocks.AIR;
        if(!level.getBlockState(pos).is(block))level.setBlockAndUpdate(pos,block.defaultBlockState());
    }
    private void waitTicks(TestServerContext server,int ticks){long end=server.computeOnServer(s->s.overworld().getGameTime()+ticks);server.waitFor(s->s.overworld().getGameTime()>=end,ticks+40);}
    private void cycle(TestServerContext server,boolean auto,boolean honey){
        var item=honey?Items.HONEY_BLOCK:Items.BREAD;int target=auto?1:2;phase("physical cycle "+(auto?"Auto-Crafter":"Crafter")+(honey?" with remainders":""));
        server.runOnServer(s->{seed(s.overworld(),auto,honey);npc.setNoAi(false);});
        server.waitFor(s->CompanionCraftingStock.plan(station).batches(List.of())>=target,1400);
        waitTicks(server,35);
        server.runOnServer(s->{check(count(output,item)==0 && CompanionStockRecipes.finished(station,item)==0,"no crafting without native "+(auto?"power":"redstone pulses"));power();});
        try{server.waitFor(s->{pulse(s.overworld());return count(output,item)>=target && (!honey || count(output,Items.GLASS_BOTTLE)>=target*4);},2400);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->"crafting cycle stalled "+npc.routineStatus()+" / "+npc.transport.activity()+" / "+stock().count(npc,station,new ItemStack(item))+" grid "+CompanionStockRecipes.inputs(station)),e);}
        long end=server.computeOnServer(s->s.overworld().getGameTime()+160);server.waitFor(s->{pulse(s.overworld());return s.overworld().getGameTime()>=end;},200);
        server.runOnServer(s->{
            check(count(output,item)==target,"real physical cycle stops at target");check(CompanionStockRecipes.inputs(station).stream().allMatch(ItemStack::isEmpty),"no additional raw ingredients left after reaching target");
            check(stock().count(npc,station,new ItemStack(item)).underway()==0,"completed deliveries release reservations");
            if(honey)check(count(output,Items.GLASS_BOTTLE)==target*4,"all native container remainders reach Output");
            check(count(source,honey?Items.HONEY_BOTTLE:Items.WHEAT)==(honey?12:45),"only one recipe batch taken from Supply");
            if(auto)check(count(grid,honey?Items.HONEY_BOTTLE:Items.WHEAT)==(honey?4:3),"Auto-Crafter preserves every pattern item");
            else check(VanillaCompanionPort.of(station).acceptsFilter(new ItemStack(item)),"finished empty Crafter retains editable pattern");
            check(CompanionJobs.inspect(npc,npc.assignments.get(1))==CompanionStatus.STOCKED,"resting crafter reports Stocked");
        });
        if(!honey){
            server.runOnServer(s->{output.clearContent();stock().invalidate();power();});
            server.waitFor(s->{pulse(s.overworld());return count(output,item)>=target;},2400);
            server.runOnServer(s->check(count(output,item)==target,"target resumes after counted stock is consumed"));
        }
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            phase("group 2/3 coverage and crafting plans");server.runOnServer(s->{coverage(s.overworld());planning(s.overworld(),false);planning(s.overworld(),true);});
            cycle(server,false,false);cycle(server,true,false);cycle(server,false,true);cycle(server,true,true);
            server.runOnServer(s->cleanup());
        }
        phase("PASS "+assertions+" assertions");
    }
}
