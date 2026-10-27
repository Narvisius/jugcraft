package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;
import org.slf4j.LoggerFactory;

/** Only ordinary crafting-table companion work: native recipes, paid manual work and shared logistics. */
public final class PeepoCraftingTableClientTests implements FabricClientGameTest {
    private BlockPos base;
    private CraftingTableCompanionBlockEntity table;
    private PeepoEntity npc,other;
    private Container source,output;
    private int assertions;
    private boolean sawAnimation;
    private void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private void phase(String message){LoggerFactory.getLogger("peepo-crafting-table").info("[peepo-crafting-table] {}",message);}
    private CompanionProductionStock stock(){return CompanionProductionStock.of(table);}
    private CompanionLogistics.Port port(){return CompanionLogistics.resolve(npc,npc.assignments.get(1));}
    private static int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private static int count(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private int finished(Item item){int n=0;for(var v:table.outputStorage())if(!v.isResourceBlank() && v.getResource().getItem()==item)n+=(int)v.getAmount();return n;}
    private void cleanup(){for(var p:new PeepoEntity[]{npc,other})if(p!=null){p.transport.releaseStock();p.transport.stop();p.resetCompanionRoutine();p.discard();}npc=other=null;}
    private PeepoEntity helper(ServerLevel level,boolean jughead){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);p.setNoAi(true);
        p.snapTo(base.getX()-2.5,base.getY(),base.getZ()-3.5,0,0);p.setOnGround(true);level.addFreshEntity(p);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();p.orders.tame(owner);p.orders.command(owner,3);p.preferences.social=false;p.preferences.carryMeals=0;
        p.assignments.assign(level,base);p.assignments.assignContainer(level,base.west(5),Direction.UP,false);p.assignments.assignContainer(level,base.east(5),Direction.UP,true);return p;
    }
    private void setup(ServerLevel level,boolean jughead){
        cleanup();for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-7),base.offset(8,4,7)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(8)).forEach(e->e.discard());
        level.setBlockAndUpdate(base,Blocks.CRAFTING_TABLE.defaultBlockState());
        check(level.getBlockEntity(base) instanceof CraftingTableCompanionBlockEntity,"ordinary vanilla table creates companion state");table=(CraftingTableCompanionBlockEntity)level.getBlockEntity(base);
        level.setBlockAndUpdate(base.west(5),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(5));
        level.setBlockAndUpdate(base.east(5),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(5));
        npc=helper(level,jughead);check(npc.assignments.assignedWork(base),"planner assignment accepts regular crafting table");check(stock()!=null && port()!=null,"regular table uses stock and logistics adapters");
    }
    private void goal(Item item,int target){check(CompanionFilters.set(table,0,new ItemStack(item)),"Filter accepts ordinary crafted output "+item);stock().useOutputs(npc,npc.assignments.get(1));stock().set(new ItemStack(item),target,0);}
    private List<ItemStack> batch(){var p=port();p.prepare(ContainerStorage.of(source,Direction.UP));return p.supplyBatch(ContainerStorage.of(source,Direction.UP),8);}
    private void insert(List<ItemStack> stacks){try(var tx=Transaction.openOuter()){for(var s:stacks)check(port().inputs().insert(ItemVariant.of(s),s.getCount(),tx)==s.getCount(),"real recipe delivery fits: "+s);tx.commit();}}
    private void carry(PeepoEntity worker,List<ItemStack> stacks){
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,worker.registryAccess());
        for(int i=0;i<8;i++)worker.belongings.setItem(i,i<stacks.size()?stacks.get(i).copy():ItemStack.EMPTY);
        if(!stacks.isEmpty()){
            var c=saved.child("Transport");c.store("Manifest",ItemStack.CODEC,stacks.getFirst());c.putInt("Slot",0);c.putBoolean("Supply",true);c.putBoolean("StockTracked",true);
            CompanionTransport.saveTarget(c,"Work",worker.assignments.get(1));CompanionTransport.saveTarget(c,"Store",worker.assignments.get(CompanionAssignments.SUPPLY));
            if(port().plan()!=null)c.store("Recipe",net.minecraft.resources.Identifier.CODEC,port().plan());
            for(int i=1;i<stacks.size();i++){var e=c.child("Pending"+(i-1));e.putInt("Slot",i);e.store("Stack",ItemStack.CODEC,stacks.get(i));}
        }
        worker.transport.load(TagValueInput.create(ProblemReporter.DISCARDING,worker.registryAccess(),saved.buildResult()));
    }
    private void planning(ServerLevel level){
        setup(level,false);
        // Existing tables without a saved companion tag must gain empty state lazily too.
        level.removeBlockEntity(base);check(level.getBlockEntity(base) instanceof CraftingTableCompanionBlockEntity,"pre-existing table obtains empty companion state");table=(CraftingTableCompanionBlockEntity)level.getBlockEntity(base);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();var provider=level.getBlockState(base).getMenuProvider(level,base);
        check(provider!=null && provider.createMenu(99,owner.getInventory(),owner) instanceof CraftingMenu,"normal player crafting menu remains vanilla");
        check(!CompanionFilters.set(table,0,new ItemStack(Items.BEDROCK)),"Filter rejects output without supported crafting recipe");
        goal(Items.BREAD,2);source.setItem(0,new ItemStack(Items.WHEAT,32));var planned=batch();
        check(count(planned,Items.WHEAT)==6,"two bread targets request six wheat");check(count(source,Items.WHEAT)==32 && count(table.installed(),Items.WHEAT)==0,"planning rolls back source and table");
        other=helper(level,true);carry(npc,planned);check(stock().count(other,table,new ItemStack(Items.BREAD)).underway()==2,"shared stock counts inbound crafted products once");
        var p=CompanionLogistics.resolve(other,other.assignments.get(1));p.prepare(ContainerStorage.of(source,Direction.UP));check(p.supplyBatch(ContainerStorage.of(source,Direction.UP),8).isEmpty(),"second helper cannot reserve duplicate stock");
        carry(npc,List.of());insert(planned);check(stock().count(npc,table,new ItemStack(Items.BREAD)).underway()==2,"installed ingredients keep projected yield");check(batch().isEmpty(),"installed complete batches stop further pickups");
        var data=table.saveWithFullMetadata(level.registryAccess());table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
        check(stock().target(new ItemStack(Items.BREAD))==2 && stock().outputs().size()==1 && count(table.installed(),Items.WHEAT)==6,"selected recipe, inputs and targets survive save/reload");
        setup(level,false);goal(Items.STICK,1);source.setItem(0,new ItemStack(Items.OAK_PLANKS));source.setItem(1,new ItemStack(Items.SPRUCE_PLANKS));
        planned=batch();check(count(planned,Items.OAK_PLANKS)==1 && count(planned,Items.SPRUCE_PLANKS)==1,"tag recipe can use two available plank variants");insert(planned);
        check(stock().count(npc,table,new ItemStack(Items.STICK)).underway()==4,"whole four-stick yield may exceed target one");
        setup(level,false);goal(Items.BREAD,1);source.setItem(0,new ItemStack(Items.WHEAT,32));output.setItem(0,new ItemStack(Items.BREAD));stock().invalidate();
        check(batch().isEmpty(),"filled Output goal stops new supply");stock().set(new ItemStack(Items.BREAD),0,0);check(!batch().isEmpty(),"Unlimited keeps ordinary crafting available");
        setup(level,false);goal(Items.BREAD,1);source.setItem(0,new ItemStack(Items.WHEAT,3));insert(batch());
        var removedRecipe=table.saveWithFullMetadata(level.registryAccess());removedRecipe.putString("TableRecipe","minecraft:removed_companion_test_recipe");table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),removedRecipe));
        check(finished(Items.WHEAT)==3 && finished(Items.BREAD)==0,"missing saved recipe exposes real ingredients for safe Output recovery");
        try(var tx=Transaction.openOuter()){check(table.outputStorage().extract(ItemVariant.of(Items.WHEAT),3,tx)==3,"obsolete recipe ingredients can be unloaded through existing Output");tx.commit();}
        check(table.installed().stream().allMatch(ItemStack::isEmpty),"obsolete recipe recovery leaves no duplicated inputs");
        setup(level,false);source.setItem(0,new ItemStack(Items.OAK_LOG));List<ItemStack> automatic=List.of();
        for(int attempt=0;attempt<32 && automatic.isEmpty();attempt++)automatic=batch();
        check(!automatic.isEmpty() && port().plan()!=null,"empty Filter finds an available ordinary recipe through bounded planning pages");
    }
    private CompanionJob stand(){
        var job=CompanionJobs.resolve(npc,base);check(job!=null && job.animation()==WorkAnimation.CRAFT,"regular table uses crafting-bench animation");
        var at=job.approachPosition();check(CompanionHazards.safeAt(npc,at),"working edge is safe");npc.snapTo(at.x,at.y,at.z,0,0);npc.setOnGround(true);
        check(job.claim(npc) && job.occupy(npc),"helper can claim and stand at table edge");npc.setWorkAnimation(WorkAnimation.CRAFT,base);return job;
    }
    private void waitTicks(TestServerContext server,int ticks){long end=server.computeOnServer(s->s.overworld().getGameTime()+ticks);server.waitFor(s->s.overworld().getGameTime()>=end,ticks+40);}
    private void manual(TestServerContext server){
        phase("paid manual work, interruption and persistence");
        server.runOnServer(s->{setup(s.overworld(),false);goal(Items.BREAD,1);source.setItem(0,new ItemStack(Items.WHEAT,3));insert(batch());});
        waitTicks(server,65);
        server.runOnServer(s->{check(finished(Items.BREAD)==0,"table cannot craft autonomously while helper AI is disabled");var job=stand();other=helper(s.overworld(),true);check(!job.claim(other),"only one helper operates each table");
            npc.setStoredEnergy(0);npc.updateRecoveryState();int before=table.progress();job.work(npc);check(table.progress()==before && finished(Items.BREAD)==0,"no reserve energy means no progress or output");
            npc.setStoredEnergy(CompanionEnergy.CAPACITY);npc.updateRecoveryState();npc.setStoredEnergy(2);job.work(npc);
            check(npc.isRecovering() && npc.getEnergy()==2 && table.progress()==before && count(table.installed(),Items.WHEAT)==3,"partial reserve cannot buy a work tick and starts normal recovery");
            npc.setStoredEnergy(CompanionEnergy.CAPACITY);npc.updateRecoveryState();npc.belongings.setItem(8,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(PeepoMod.id("mechanic_uniform"))));npc.setStoredEnergy(2);job.work(npc);
            check(npc.isRecovering() && npc.getEnergy()==2 && table.progress()==before,"Mechanic still requires its full three-JE discounted payment");
            npc.belongings.setItem(8,ItemStack.EMPTY);npc.setStoredEnergy(CompanionEnergy.CAPACITY);npc.updateRecoveryState();});
        waitTicks(server,1);
        server.runOnServer(s->{var job=stand();int before=npc.getEnergy(),progress=table.progress();job.work(npc);check(table.progress()==progress+1 && npc.getEnergy()==before-4,"one active work tick costs four companion JE");
            job.work(npc);check(table.progress()==progress+1 && npc.getEnergy()==before-4,"repeated call in same tick cannot double-work or double-charge");});
        waitTicks(server,1);
        server.runOnServer(s->{npc.belongings.setItem(8,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(PeepoMod.id("mechanic_uniform"))));var job=stand();int before=npc.getEnergy(),progress=table.progress();job.work(npc);
            check(npc.uniform()==CompanionUniforms.Type.MECHANIC && table.progress()==progress+1 && npc.getEnergy()==before-3,"Mechanic uniform charges exactly three JE for the same manual work tick");npc.belongings.setItem(8,ItemStack.EMPTY);});
        server.waitFor(s->{var job=CompanionJobs.resolve(npc,base);job.occupy(npc);npc.setWorkAnimation(WorkAnimation.CRAFT,base);job.work(npc);return table.progress()>=20;},60);
        server.runOnServer(s->{int progress=table.progress();var data=table.saveWithFullMetadata(s.overworld().registryAccess());table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,s.overworld().registryAccess(),data));
            check(table.progress()==progress && count(table.installed(),Items.WHEAT)==3 && finished(Items.BREAD)==0,"paid partial work and real ingredients survive reload without premature output");
            CompanionJobs.resolve(npc,base).release(npc);npc.snapTo(base.getX()+4.5,base.getY(),base.getZ()+3.5,0,0);});
        final int[] paused={0};server.runOnServer(s->paused[0]=table.progress());waitTicks(server,25);
        server.runOnServer(s->{check(table.progress()==paused[0] && finished(Items.BREAD)==0,"unattended craft pauses without consuming ingredients");stand();});
        server.waitFor(s->{var job=CompanionJobs.resolve(npc,base);job.occupy(npc);npc.setWorkAnimation(WorkAnimation.CRAFT,base);job.work(npc);return finished(Items.BREAD)>0;},100);
        server.runOnServer(s->{check(finished(Items.BREAD)==1 && count(table.installed(),Items.WHEAT)==0,"resumed craft consumes exact native ingredients and makes one result");});
    }
    private void fullOutput(ServerLevel level){
        setup(level,false);goal(Items.BREAD,1);source.setItem(0,new ItemStack(Items.WHEAT,3));insert(batch());
        var saved=table.saveWithFullMetadata(level.registryAccess());var buffers=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        buffers.store("TableOutputs",ItemStack.OPTIONAL_CODEC.listOf(),java.util.stream.IntStream.range(0,12).mapToObj(i->new ItemStack(Items.COBBLESTONE,64)).toList());saved.merge(buffers.buildResult());table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved));
        var job=CompanionJobs.resolve(npc,base);int energy=npc.getEnergy(),progress=table.progress();
        check(job.workStatus(npc)==CompanionStatus.FULL,"full result buffer reports Output blocked");check(table.advance(npc)==CompanionStatus.FULL,"native craft preflight refuses a full output buffer");job.work(npc);
        check(npc.getEnergy()==energy && table.progress()==progress && count(table.installed(),Items.WHEAT)==3,"blocked output consumes neither energy nor ingredients");
    }
    private void breakCleanup(ServerLevel level){
        setup(level,false);goal(Items.BREAD,1);source.setItem(0,new ItemStack(Items.WHEAT,3));insert(batch());
        var saved=table.saveWithFullMetadata(level.registryAccess());var buffers=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        buffers.store("TableOutputs",ItemStack.OPTIONAL_CODEC.listOf(),List.of(new ItemStack(Items.BREAD),new ItemStack(Items.GLASS_BOTTLE,2)));saved.merge(buffers.buildResult());table.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved));
        level.destroyBlock(base,false);
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(2)).stream().map(e->e.getItem()).toList();
        check(count(drops,Items.WHEAT)==3 && count(drops,Items.BREAD)==1 && count(drops,Items.GLASS_BOTTLE)==2,"breaking table returns all real inputs, products and remainders once");
        check(table.installed().stream().allMatch(ItemStack::isEmpty) && finished(Items.BREAD)==0,"removed table cannot drop the same saved inventory again");
    }
    private void live(TestServerContext server,boolean jughead,boolean honey){
        var product=honey?Items.HONEY_BLOCK:Items.BREAD;var ingredient=honey?Items.HONEY_BOTTLE:Items.WHEAT;int per=honey?4:3;
        phase("physical Supply/work/Output "+(jughead?"Jughead":"Peepo")+(honey?" with container remainders":""));sawAnimation=false;
        server.runOnServer(s->{setup(s.overworld(),jughead);goal(product,1);source.setItem(0,new ItemStack(ingredient,per*4));npc.setNoAi(false);});
        try{server.waitFor(s->{sawAnimation|=npc.workAnimation()==WorkAnimation.CRAFT;return count(output,product)==1 && (!honey || count(output,Items.GLASS_BOTTLE)==4);},1800);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->"table cycle stalled: "+npc.routineStatus()+" / "+npc.transport.activity()+" / "+table.status()+" / progress "+table.progress()+" / "+stock().count(npc,table,new ItemStack(product))+" / inputs "+table.installed()),e);}
        waitTicks(server,140);
        server.runOnServer(s->{check(sawAnimation,"crafting-bench pose is shown during real crafting");check(count(output,product)==1 && count(source,ingredient)==per*3,"one target consumes exactly one recipe from Supply");
            check(stock().count(npc,table,new ItemStack(product)).underway()==0 && batch().isEmpty(),"completed delivery releases reservation and stops new supplies");
            check(CompanionJobs.inspect(npc,npc.assignments.get(1))==CompanionStatus.STOCKED,"finished crafting job reports Stocked");
            if(honey)check(count(output,Items.GLASS_BOTTLE)==4,"all four native glass-bottle remainders reach Output");
            check(CompanionHazards.safeAt(npc,npc.position()),"helper remains clear of the full-height table");});
        if(!honey){server.runOnServer(s->{output.clearContent();stock().invalidate();});server.waitFor(s->count(output,product)==1,1800);server.runOnServer(s->check(count(source,ingredient)==per*2,"consumed stock restarts another exact craft"));}
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            phase("planning, filters, migration and shared reservations");server.runOnServer(s->planning(s.overworld()));manual(server);server.runOnServer(s->{fullOutput(s.overworld());breakCleanup(s.overworld());});
            live(server,false,false);live(server,true,true);server.runOnServer(s->cleanup());
        }
        phase("PASS "+assertions+" assertions");
    }
}
