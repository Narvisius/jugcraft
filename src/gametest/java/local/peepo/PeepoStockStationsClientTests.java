package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.workshop.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
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

/** Stock extension only: native stations, physical transport, batches, utilities and shared reservations. */
public final class PeepoStockStationsClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc,other;
    private BlockEntity station;
    private Container source,output;
    private Item product;
    private int goal,assertions;
    private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private void phase(String text){LoggerFactory.getLogger("peepo-stock-stations").info("[peepo-stock-stations] {}",text);}
    private int count(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private CompanionProductionStock stock(){return CompanionProductionStock.of(station);}
    private CompanionRecipeFilter filter(){return CompanionFilters.get(station);}
    private CompanionLogistics.Port port(){return CompanionLogistics.resolve(npc,npc.assignments.get(1));}
    private List<ItemStack> batch(){var p=port();p.prepare(ContainerStorage.of(source,Direction.UP));return p.supplyBatch(ContainerStorage.of(source,Direction.UP),8);}
    private void reset(ServerLevel level,Block block,boolean jughead){
        for(var p:new PeepoEntity[]{npc,other})if(p!=null){p.transport.releaseStock();p.transport.stop();p.resetCompanionRoutine();p.discard();}npc=other=null;
        for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-7),base.offset(8,4,7)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var state=block.defaultBlockState();level.setBlockAndUpdate(base,state);
        if(block instanceof WorkshopBlock workshop)workshop.setPlacedBy(level,base,state,null,ItemStack.EMPTY);
        if(block instanceof MachineBlock machine){var fp=machine.footprint(state);for(int i=1;i<fp.size();i++)level.setBlock(fp.partPos(base,state.getValue(MachineBlock.FACING),i),state.setValue(LargeMachineBlock.PART,i),2);}
        station=level.getBlockEntity(base);
        level.setBlockAndUpdate(base.west(5),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(5));
        level.setBlockAndUpdate(base.east(5),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(5));
        npc=helper(level,jughead);
        check(stock()!=null,"stock support for "+BuiltInRegistries.BLOCK.getKey(block));
    }
    private PeepoEntity helper(ServerLevel level,boolean jughead){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);p.setNoAi(true);
        p.snapTo(base.getX()-2.5,base.getY(),base.getZ()-3.5,0,0);p.setOnGround(true);level.addFreshEntity(p);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();p.orders.tame(owner);p.orders.command(owner,3);p.preferences.social=false;p.preferences.carryMeals=0;
        p.assignments.assign(level,base);p.assignments.assignContainer(level,base.west(5),Direction.UP,false);p.assignments.assignContainer(level,base.east(5),Direction.UP,true);return p;
    }
    private void goal(Item item,int amount){product=item;goal=amount;filter().set(0,new ItemStack(item));stock().useOutputs(npc,npc.assignments.get(1));stock().set(new ItemStack(item),amount,0);}
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
    private void insert(List<ItemStack> stacks){
        try(var scope=MachineItemAutomation.companionTransfer();var tx=Transaction.openOuter()){
            for(var stack:stacks)check(port().inputs().insert(ItemVariant.of(stack),stack.getCount(),tx)==stack.getCount(),"committed delivery accepted: "+stack);tx.commit();
        }
    }
    private void cooking(ServerLevel level,Block block,boolean jughead){
        reset(level,block,jughead);
        if(station instanceof SkilletBlockEntity)level.setBlockAndUpdate(base.below(),Blocks.CAMPFIRE.defaultBlockState());
        if(station instanceof KitchenStoveBlockEntity)level.setBlockAndUpdate(base,station.getBlockState().setValue(KitchenStoveBlock.LIT,true));
        boolean blast=station instanceof BlastFurnaceBlockEntity;
        source.setItem(0,new ItemStack(blast?Items.IRON_ORE:Items.BEEF,16));source.setItem(1,new ItemStack(Items.COAL,8));goal(blast?Items.IRON_INGOT:Items.COOKED_BEEF,2);
    }
    private void cookingChecks(ServerLevel level,Block block,boolean jughead){
        cooking(level,block,jughead);var raw=source.getItem(0).getItem();output.setItem(0,new ItemStack(product));stock().invalidate();
        var planned=batch();check(count(planned,raw)==1,"one missing product bounds cooking inputs "+block);
        check(CompanionStockRecipes.inputs(station).stream().allMatch(ItemStack::isEmpty),"cooking probe rolls back input");
        check(count(source,raw)==16,"cooking probe rolls back source");
        other=helper(level,!jughead);carry(npc,planned);
        var second=CompanionLogistics.resolve(other,other.assignments.get(1));second.prepare(ContainerStorage.of(source,Direction.UP));
        check(second.pickupNeeded(new ItemStack(raw))==0,"other helper honors inbound cooking reservation");
        check(stock().count(other,station,new ItemStack(product)).underway()==1,"reserved cooking product counted once");
        carry(npc,List.of());insert(planned);check(stock().count(npc,station,new ItemStack(product)).underway()==1,"installed cooking product counted once");
        output.setItem(0,new ItemStack(product,2));stock().invalidate();
        check(port().pickupNeeded(new ItemStack(raw))==0,"stocked cooking stops fetching raw food");
        if(station instanceof AbstractFurnaceBlockEntity furnace){furnace.setItem(1,ItemStack.EMPTY);check(port().pickupNeeded(new ItemStack(Items.COAL))>0,"loaded cooking can still fetch fuel at target");}
    }
    private void workshop(ServerLevel level,int kind){
        reset(level,JugcraftWorkshop.BLOCKS[kind],kind==1);var w=(WorkshopBlockEntity)station;
        String recipe=kind==0?"copper_plate":kind==1?"fire_insulators":"brass_ingot";
        var entry=w.catalog().entries().stream().filter(e->e.id().getPath().endsWith("/"+recipe)).findFirst().orElseThrow();w.select(entry.id());
        var r=entry.recipe();goal(r.result().create().getItem(),r.result().create().getCount());int slot=0;
        for(var p:r.parts())source.setItem(slot++,new ItemStack(p.ingredient().items().findFirst().orElseThrow(),p.count()*8));
        if(!r.toolStack().isEmpty())source.setItem(slot++,r.toolStack());
        if(r.heated())source.setItem(slot++,new ItemStack(Items.COAL,8));
        if(kind==2){source.setItem(slot++,new ItemStack(JugcraftWorkshop.ITEMS.get("basic_ceramic_lining")));source.setItem(slot,new ItemStack(JugcraftWorkshop.ITEMS.get("ceramic_crucible")));}
    }
    private void workshopChecks(ServerLevel level,int kind){
        workshop(level,kind);var w=(WorkshopBlockEntity)station;var r=w.selection().recipe();var planned=batch();
        var block=(WorkshopBlock)w.getBlockState().getBlock();
        for(int i=0;i<block.footprint().size();i++){
            var part=block.footprint().partPos(base,w.getBlockState().getValue(WorkshopBlock.FACING),i);
            check(!level.getBlockState(part).isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND),
                "workshop shell must not be a walking passage: station "+kind+" part "+i);
        }
        for(var part:r.parts()){var item=part.ingredient().items().findFirst().orElseThrow().value();check(count(planned,item)==part.count(),"workshop ingredient bounded to one recipe batch");}
        check(!CompanionStockRecipes.busy(w),"workshop probe rolls back");
        other=helper(level,false);carry(npc,planned);
        var second=CompanionLogistics.resolve(other,other.assignments.get(1));
        check(second.supplyBatch(ContainerStorage.of(source,Direction.UP),8).isEmpty(),"workshop ingredients, tools and fuel reserved across helpers");
        carry(npc,List.of());insert(planned);
        if(kind!=0){w.tick(level);check(w.busy(),"native workshop starts real escrow batch");check(CompanionStockRecipes.inputs(w).stream().allMatch(ItemStack::isEmpty),"native workshop consumes raw ingredients at start");}
        check(stock().count(npc,w,new ItemStack(product)).underway()==goal,"workshop escrow/raw products counted once");
        check(batch().isEmpty(),"workshop does not fetch an extra batch during committed work");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());filter().save(saved);filter().load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult()));
        check(stock().target(new ItemStack(product))==goal && stock().outputs().size()==1,"workshop targets and counted Outputs survive save");
    }
    private void machine(ServerLevel level,MachineKind kind){
        reset(level,JugcraftMachines.MACHINES.get(kind),kind==MachineKind.ALLOY_SMELTER);var m=(MachineBlockEntity)station;
        goal(kind==MachineKind.ALLOY_SMELTER?BuiltInRegistries.ITEM.getValue(Identifier.parse("jugcraft:bronze_ingot")):Items.IRON_INGOT,kind==MachineKind.ALLOY_SMELTER?4:2);
        if(kind==MachineKind.ELECTRIC_FURNACE)source.setItem(0,new ItemStack(Items.IRON_ORE,16));
        else {m.companionPort.select(new ItemStack(product));int slot=0;for(var part:m.companionPort.selection().parts())source.setItem(slot++,new ItemStack(part.ingredient().items().findFirst().orElseThrow(),part.count()*8));}
        try(var tx=Transaction.openOuter()){for(int i=0;i<1000;i++)m.energyFor(null).insert(128,tx);tx.commit();}
    }
    private void machineChecks(ServerLevel level,MachineKind kind){
        machine(level,kind);if(kind==MachineKind.ELECTRIC_FURNACE){goal=1;stock().set(new ItemStack(product),goal,0);}
        var m=(MachineBlockEntity)station;var planned=batch();var recipe=m.companionPort.selection();
        check(!planned.isEmpty(),"processor can plan real recipe");
        other=helper(level,true);carry(npc,planned);var second=CompanionLogistics.resolve(other,other.assignments.get(1));
        for(var stack:planned)check(second.pickupNeeded(stack)==0,"processor does not duplicate reserved ingredient");
        carry(npc,List.of());insert(planned);
        check(stock().count(npc,m,new ItemStack(product)).underway()==recipe.results().getFirst().getCount(),"processor counts installed native recipe yield");
        for(int i=0;i<m.getContainerSize();i++)m.setItem(i,ItemStack.EMPTY);
        m.setItem(m.kind().outputSlot(),new ItemStack(product,goal));stock().invalidate();
        check(batch().isEmpty(),"finished processor output alone satisfies target");
        try(var tx=Transaction.openOuter()){check(port().outputs().extract(ItemVariant.of(product),goal,tx)==goal,"stocked processor still unloads finished products");}
    }
    private void priority(ServerLevel level){
        cooking(level,Blocks.FURNACE,false);filter().set(1,new ItemStack(Items.COOKED_PORKCHOP));stock().set(new ItemStack(Items.COOKED_PORKCHOP),4,0);
        output.setItem(0,new ItemStack(Items.COOKED_BEEF,2));source.setItem(2,new ItemStack(Items.PORKCHOP,8));stock().invalidate();
        check(count(batch(),Items.PORKCHOP)==4 && count(batch(),Items.BEEF)==0,"vanilla selects an available understocked recipe");
        output.setItem(1,new ItemStack(Items.COOKED_PORKCHOP,4));stock().invalidate();check(batch().isEmpty(),"all fulfilled recipe choices stop");
        check(CompanionJobs.inspect(npc,npc.assignments.get(1))==CompanionStatus.STOCKED,"idle cooking reports Stocked");
        stock().set(new ItemStack(Items.COOKED_BEEF),0,0);check(count(batch(),Items.BEEF)==8,"Unlimited fallback retains native cooking batch cap");
    }
    private void cycle(TestServerContext server,String name,java.util.function.Consumer<ServerLevel> setup){
        phase("physical cycle "+name);server.runOnServer(s->{setup.accept(s.overworld());npc.setNoAi(false);});
        try{server.waitFor(s->count(output,product)>=goal,2600);}catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->name+" stalled: "+npc.routineStatus()+" / "+npc.transport.activity()+" / "+port().status()+" at "+npc.position()+", inputs="+CompanionStockRecipes.inputs(station)+", stock="+stock().count(npc,station,new ItemStack(product))),e);}
        long until=server.computeOnServer(s->s.overworld().getGameTime()+180);server.waitFor(s->s.overworld().getGameTime()>=until,220);
        server.runOnServer(s->{check(count(output,product)==goal,name+" reaches target without extra deliveries");check(CompanionStockRecipes.inputs(station).stream().allMatch(ItemStack::isEmpty),name+" stops loading after target");check(stock().count(npc,station,new ItemStack(product)).underway()==0,name+" output transit accounted and released");});
    }
    private void charcoal(ServerLevel level){cooking(level,Blocks.FURNACE,true);goal(Items.CHARCOAL,2);source.clearContent();source.setItem(0,new ItemStack(Items.OAK_LOG,16));}
    void fuelTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            server.runOnServer(s->{
                charcoal(s.overworld());var planned=batch();check(count(planned,Items.OAK_LOG)==3,"two smelting logs plus one fuel log");
                check(((Container)station).isEmpty(),"dual-purpose fuel planning rolls back");
                carry(npc,planned);other=helper(s.overworld(),false);
                check(stock().count(other,station,new ItemStack(product)).underway()==2,"fuel log is not promised as charcoal");
                check(CompanionLogistics.resolve(other,other.assignments.get(1)).pickupNeeded(new ItemStack(Items.OAK_LOG))==0,"another helper does not duplicate raw/fuel logs");
                carry(npc,List.of());
                try(var tx=Transaction.openOuter()){check(port().inputs().insert(ItemVariant.of(Items.OAK_LOG),3,tx)==2,"deliver only intended logs to recipe slot");tx.commit();}
                try(var tx=Transaction.openOuter()){check(port().inputs().insert(ItemVariant.of(Items.OAK_LOG),1,tx)==1,"remaining log goes to fuel");tx.commit();}
                var furnace=(AbstractFurnaceBlockEntity)station;check(furnace.getItem(0).getCount()==2 && furnace.getItem(1).getCount()==1,"separate recipe and fuel quantities survive delivery");
            });
            cycle(server,"logs as ingredient and fuel",this::charcoal);
            server.runOnServer(s->check(count(source,Items.OAK_LOG)+count((Container)station,Items.OAK_LOG)+count(npc.belongings,Items.OAK_LOG)==12,"two raw logs and two short-burning fuel logs consumed; any buffered fuel remains real inventory"));
            server.runOnServer(s->{npc.transport.releaseStock();npc.discard();if(other!=null)other.discard();});
        }
        phase("FUEL PASS "+assertions+" assertions");
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            var cooking=List.of(Blocks.FURNACE,Blocks.SMOKER,Blocks.BLAST_FURNACE,Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE,JugcraftAgriculture.block("skillet"),JugcraftAgriculture.block("kitchen_stove"));
            phase("bounded cooking plans and reservations");server.runOnServer(s->{for(var block:cooking)cookingChecks(s.overworld(),block,block==Blocks.SMOKER);priority(s.overworld());});
            phase("workshop escrow, tools and fuel");server.runOnServer(s->{for(int i=0;i<3;i++)workshopChecks(s.overworld(),i);});
            phase("standard processors");server.runOnServer(s->{machineChecks(s.overworld(),MachineKind.ELECTRIC_FURNACE);machineChecks(s.overworld(),MachineKind.ALLOY_SMELTER);});
            for(var block:cooking)cycle(server,BuiltInRegistries.BLOCK.getKey(block).toString(),l->cooking(l,block,block==Blocks.SMOKER));
            for(int kind=0;kind<3;kind++){final int k=kind;cycle(server,"workshop "+kind,l->workshop(l,k));}
            cycle(server,"electric furnace",l->machine(l,MachineKind.ELECTRIC_FURNACE));cycle(server,"alloy smelter",l->machine(l,MachineKind.ALLOY_SMELTER));
            server.runOnServer(s->{npc.transport.releaseStock();npc.discard();if(other!=null)other.discard();});
        }
        phase("PASS "+assertions+" assertions");
    }
}
