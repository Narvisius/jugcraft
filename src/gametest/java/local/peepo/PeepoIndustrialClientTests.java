package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.machine.form.*;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.LoggerFactory;

/** Focused checks for expanded industrial companion opt-ins, logistics and multi-lane assistance. */
public final class PeepoIndustrialClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc, second;
    private FormMachineBlockEntity machine;
    private int assertions;
    private void check(boolean ok, String message) { assertions++; if (!ok) throw new AssertionError(message); }
    private void phase(String message) { LoggerFactory.getLogger("peepo-industrial-test").info("[peepo-industrial-test] {}",message); }
    private void setup(ServerLevel level, FormMachineBlock block, Direction facing) {
        if (npc != null) { npc.resetCompanionRoutine(); npc.discard(); }
        if (second != null) { second.resetCompanionRoutine(); second.discard(); second = null; }
        for (var p : BlockPos.betweenClosed(base.offset(-9,-1,-9),base.offset(9,9,9)))
            level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var state = block.defaultBlockState().setValue(MachineBlock.FACING,facing);
        level.setBlockAndUpdate(base,state); block.setPlacedBy(level,base,state,null,ItemStack.EMPTY);
        machine = (FormMachineBlockEntity)level.getBlockEntity(base);
        machine.serverTick(level,base,state);
        npc = worker(level,false);
        npc.assignments.assign(level,base);
    }
    private PeepoEntity worker(ServerLevel level, boolean jughead) {
        var result = (jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        result.snapTo(base.getX()+.5,base.getY(),base.getZ()-3.5,0,0); result.setNoAi(true); level.addFreshEntity(result);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();
        result.orders.tame(owner);result.orders.command(owner,3);result.preferences.social=false;result.preferences.carryMeals=0;
        return result;
    }
    private SimpleContainer supplies(FluidRecipes.Entry entry, int batches) {
        var source = new SimpleContainer(16); int slot=0;
        for (var part : entry.recipe().items()) source.setItem(slot++,new ItemStack(part.ingredient().items().findFirst().orElseThrow(),part.count()*batches));
        if (entry.recipe().tool().isPresent()) {
            var tool=entry.recipe().tool().get().items().filter(h->machine.form().sockets().stream().anyMatch(s->s.accepts(new ItemStack(h)))).findFirst().orElseThrow();
            source.setItem(slot,new ItemStack(tool));
        }
        return source;
    }
    private void fluids(FluidRecipes.Entry entry, int batches) {
        try(var tx=Transaction.openOuter()) {
            for (int i=0;i<entry.recipe().fluids().size();i++) {
                var fluid=entry.recipe().fluids().get(i);long amount=fluid.amount()*batches*(FluidConstants.BUCKET/1000);
                check(machine.tanks().input(i).insert(FluidVariant.of(fluid.fluid()),amount,tx)==amount,"native piped input accepted");
            }
            tx.commit();
        }
    }
    private void ports(ServerLevel level, FormMachineBlock block, Direction facing) {
        setup(level,block,facing);
        check(CompanionAssignments.work(level,base),"processor assignable: "+machine.form().id());
        check(machine.companionHelperCount()==2,"exactly two helper positions");
        check(machine.companionPort.accessible(npc),"complete form accessible");
        var entrances=machine.companionPort.entrances(npc);check(!entrances.isEmpty(),"exterior delivery positions");
        for(var at:entrances)check(!CompanionAssignments.canonical(level,at).equals(base),"delivery stays outside footprint");
        var recipes=machine.recipes(level.getServer());check(!recipes.isEmpty(),"registered form has recipes");
        var entry=recipes.getFirst();machine.selectRecipe(entry.id());fluids(entry,1);
        var source=supplies(entry,1);var port=machine.companionPort.forCompanion(npc);
        port.prepare(ContainerStorage.of(source,null));check(entry.id().equals(port.plan()),"native recipe lock respected");
        for(int i=0;i<source.getContainerSize();i++) {
            var stack=source.getItem(i);if(stack.isEmpty())continue;
            check(port.needed(stack)>=stack.getCount(),"ingredients and reusable tools requested");
            try(var tx=Transaction.openOuter()) { check(port.inputs().insert(ItemVariant.of(stack),stack.getCount(),tx)==stack.getCount(),"simulated delivery accepted"); }
            check(machine.container().isEmpty(),"simulation leaves native inventory unchanged");
        }
        for(int i=0;i<source.getContainerSize();i++) {
            var stack=source.getItem(i);if(stack.isEmpty())continue;
            try(var tx=Transaction.openOuter()) {check(port.inputs().insert(ItemVariant.of(stack),stack.getCount(),tx)==stack.getCount(),"real delivery accepted");tx.commit();}
        }
        check(!machine.companionAutomation.external(true),"companion insertion does not disable Auto");
        var outputs=entry.recipe().results();
        for(int i=0;i<outputs.size();i++)machine.container().setItem(machine.form().firstOutputSlot()+i,outputs.get(i).create());
        for(var result:outputs)try(var tx=Transaction.openOuter()) {
            check(port.outputs().extract(ItemVariant.of(result.create()),result.count(),tx)==result.count(),"finished product/byproduct extractable");tx.commit();
        }
        if(entry.recipe().tool().isPresent()) {
            var stack=machine.container().getItem(machine.form().firstSocketSlot());
            try(var tx=Transaction.openOuter()){check(port.outputs().extract(ItemVariant.of(stack),1,tx)==0,"current reusable tool retained");}
        }
        ItemStack icon=!outputs.isEmpty()?outputs.getFirst().create():entry.recipe().fluidResults().stream().map(f->f.fluid().getBucket()).filter(i->i!=Items.AIR).map(ItemStack::new).findFirst().orElse(ItemStack.EMPTY);
        if(!icon.isEmpty()) {
            check(CompanionFilters.set(machine,0,icon),"item/fluid-output filter accepted");
            port.prepare(ContainerStorage.of(source,null));check(entry.id().equals(port.plan()),"cached native filter retains matching recipe");
        }
        check(!CompanionFilters.set(machine,1,new ItemStack(Items.BEDROCK)),"invalid filter refused");
        machine.companionPort.filter.set(0,new ItemStack(Items.BEDROCK));
        port.prepare(ContainerStorage.of(source,null));
        check(port.plan()==null,"filter change invalidates stale delivery");
        if(!entry.recipe().items().isEmpty())check(!machine.container().canPlaceItem(0,source.getItem(0)),"filter revision invalidates cached native insertion recipes");
        machine.companionPort.filter.set(0,ItemStack.EMPTY);port.prepare(ContainerStorage.of(source,null));
        check(port.plan()!=null,"empty filter restores any available recipe");
        machine.togglePause();
        for(int i=0;i<source.getContainerSize();i++)check(port.needed(source.getItem(i))==0,"paused processor receives no extra stock");
    }
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()) {
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var spawn=context.computeOnClient(c->c.player.blockPosition());base=new BlockPos(spawn.getX(),spawn.getY()+4,spawn.getZ()+8);
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-8.5));
            var forms=new ArrayList<FormMachineBlock>();
            for(var field:IndustrialForms.class.getFields())if(field.getType()==FormMachineBlock.class)try {forms.add((FormMachineBlock)field.get(null));}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            int enabled=0;
            for(var block:forms) {
                if(!block.form().companionAssistance()) {check(block.form().generator() || block==IndustrialForms.COASTAL_SEAWATER_INTAKE,"only generators and special pump excluded");continue;}
                phase("ports: "+block.form().id());var facing=Direction.from2DDataValue(enabled++%4);
                server.runOnServer(s->ports(s.overworld(),block,facing));
            }
            check(enabled==40,"all forty registered processors enabled");
            phase("native automation and full-footprint permissions");server.runOnServer(s->automation(s.overworld()));
            phase("physical ingredients/tool to processor to Output");delivery(server);
            phase("four-lane bulk assistance");bulk(server);
            server.runOnServer(s->{npc.resetCompanionRoutine();npc.discard();if(second!=null){second.resetCompanionRoutine();second.discard();}});
        }
        phase("PASS "+assertions+" assertions");
    }
    private void automation(ServerLevel level) {
        setup(level,IndustrialForms.POLYMER_EXTRUDER,Direction.NORTH);
        var entry=machine.recipes(level.getServer()).getFirst();machine.selectRecipe(entry.id());
        var stack=new ItemStack(entry.recipe().items().getFirst().ingredient().items().findFirst().orElseThrow());
        var port=machine.form().ports().stream().filter(p->p.kind()==FormPort.Kind.ITEM_IN && p.reaches(0)).findFirst().orElseThrow();
        try(var tx=Transaction.openOuter()){check(machine.itemPort(port).insert(ItemVariant.of(stack),1,tx)==1,"native port accepts recipe input");}
        check(!machine.companionAutomation.external(true),"aborted external probe does not disable companion Supply");
        try(var tx=Transaction.openOuter()){machine.itemPort(port).insert(ItemVariant.of(stack),1,tx);tx.commit();}
        check(machine.companionAutomation.external(true),"committed native insertion detected");
        check(!npc.assignments.transportAllowed(npc.assignments.get(1),true),"Auto defers to native automation");
        npc.assignments.cycleTransport(1,true);check(npc.assignments.transportAllowed(npc.assignments.get(1),true),"On overrides automatic detection");
        var filter=entry.recipe().results().getFirst().create();CompanionFilters.set(machine,0,filter);
        var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        machine.companionPort.filter.save(saved);var restored=new CompanionRecipeFilter();
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult()));
        check(restored.get(0).is(filter.getItem()),"filter survives save/load");
        // Move the fixture to a chunk edge, so a distant casing has different ownership from the controller.
        base=new BlockPos((base.getX()>>4)<<4,base.getY(),base.getZ());setup(level,IndustrialForms.POLYMER_EXTRUDER,Direction.NORTH);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();
        var book=io.github.jimbozoomer.jugcraft.finance.Ledger.get(level.getServer()).book();
        var land=io.github.jimbozoomer.jugcraft.finance.LandRegistry.get(level.getServer());
        var stranger=net.fabricmc.fabric.api.entity.FakePlayer.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FactoryOwner"));
        var account=book.account(stranger.getUUID(),"FactoryOwner");var ownerAccount=book.account(owner.getUUID(),owner.getName().getString());
        long key=io.github.jimbozoomer.jugcraft.finance.LandRegistry.keyOf(base.west());
        try {
            check(land.assign(book,key,account,0).ok(),"foreign casing claim created");
            check(!machine.companionPort.accessible(npc),"foreign casing denies full machine access");
            check(land.trust(stranger,book,ownerAccount.name,true).ok(),"owner trust granted");
            check(machine.companionPort.accessible(npc),"trusted owner may work across claim edge");
            land.trust(stranger,book,ownerAccount.name,false);
            check(!machine.companionPort.accessible(npc),"revocation effective in same tick");
        } finally {land.abandon(owner,book,key,true,0);}
    }
    private void delivery(TestServerContext server) {
        Container[] output={null},source={null};FluidRecipes.Entry[] recipe={null};
        server.runOnServer(s->{var level=s.overworld();setup(level,IndustrialForms.POLYMER_EXTRUDER,Direction.NORTH);
            recipe[0]=machine.recipes(s).getFirst();machine.selectRecipe(recipe[0].id());fluids(recipe[0],1);
            level.setBlockAndUpdate(base.south(8),Blocks.BARREL.defaultBlockState());source[0]=(Container)level.getBlockEntity(base.south(8));
            level.setBlockAndUpdate(base.east(7),Blocks.BARREL.defaultBlockState());output[0]=(Container)level.getBlockEntity(base.east(7));
            var supplies=supplies(recipe[0],1);for(int i=0;i<supplies.getContainerSize();i++)source[0].setItem(i,supplies.getItem(i).copy());
            npc.assignments.assignContainer(level,base.south(8),Direction.UP,false);npc.assignments.assignContainer(level,base.east(7),Direction.UP,true);
            ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());npc.setNoAi(false);
        });
        try {
            server.waitFor(s->{((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());
                return recipe[0].recipe().results().stream().allMatch(r->count(output[0],r.create().getItem())>=r.count());},2400);
        } catch(AssertionError e) {throw new AssertionError(server.computeOnServer(s->"Industrial delivery stalled: "+npc.routineStatus()+" "+npc.transport.activity()+" pos="+npc.position()+" native="+machine.status()),e);}
        server.runOnServer(s->{
            check(source[0].isEmpty(),"all recipe inputs and tool delivered from real chest");
            for(var result:recipe[0].recipe().results())check(count(output[0],result.create().getItem())==result.count(),"physical product and byproduct yield conserved");
            check(!machine.container().getItem(machine.form().firstSocketSlot()).isEmpty(),"reusable die remains in socket");
        });
    }
    private int count(Container inventory,Item item) {int total=0;for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(item))total+=inventory.getItem(i).getCount();return total;}
    private void bulk(TestServerContext server) {
        server.runOnServer(s->{var level=s.overworld();setup(level,IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR,Direction.NORTH);
            var water=machine.recipes(s).stream().filter(e->e.id().getPath().equals("electrolysis/separator_water")).findFirst().orElseThrow();
            machine.selectRecipe(water.id());fluids(water,8);
            ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());npc.setNoAi(false);
            second=worker(level,true);second.assignments.assign(level,base);second.setNoAi(false);
        });
        server.waitFor(s->{((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());return machine.lane(3)!=null && machine.lane(3).progress()>40 && npc.stationStatus(base)==CompanionStatus.WORKING && second.stationStatus(base)==CompanionStatus.WORKING;},1000);
        long[] start=server.computeOnServer(s->new long[]{s.overworld().getGameTime(),machine.lane(0).progress(),npc.getEnergy(),second.getEnergy()});
        server.waitFor(s->{((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());return s.overworld().getGameTime()>=start[0]+40;},100);
        server.runOnServer(s->{long ticks=s.overworld().getGameTime()-start[0];
            for(int i=0;i<4;i++)check(Math.abs(machine.lane(i).progress()-start[1]-ticks*1.5)<=2,"both helpers accelerate each of four lanes by 50%");
            check(Math.abs(start[2]-npc.getEnergy()-ticks*64)<=128,"Peepo reserve scales to four lanes");
            check(Math.abs(start[3]-second.getEnergy()-ticks*64)<=128,"Jughead reserve scales to four lanes");
        });
        // Emulate independently escrowed recipes with different draw; production must bill each lane's own price.
        long[] weighted=server.computeOnServer(s->{
            try {
                var field=FormMachineBlockEntity.class.getDeclaredField("lanes");field.setAccessible(true);
                var lanes=(WorkLane[])field.get(machine);
                for(int i=0;i<4;i++) {var old=lanes[i];lanes[i]=new WorkLane(old.recipe(),old.inputs(),old.fluids(),old.itemResults(),old.fluidResults(),old.duration(),old.tool(),old.cold(),old.progress(),64<<i,old.drainWater(),old.emissions());}
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
            ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());
            return new long[]{s.overworld().getGameTime(),machine.energy().getAmount(),machine.lane(0).progress(),machine.lane(1).progress(),machine.lane(2).progress(),machine.lane(3).progress()};
        });
        server.waitFor(s->s.overworld().getGameTime()>=weighted[0]+20,100);
        server.runOnServer(s->{long expected=0;
            for(int i=0;i<4;i++)expected+=(machine.lane(i).progress()-weighted[i+2])*machine.form().profile().lanePerTick(64<<i);
            check(weighted[1]-machine.energy().getAmount()==expected,"mixed-price lanes pay exactly their own native price for ordinary and assisted work");
            machine.togglePause();
        });
        long[] pause=server.computeOnServer(s->new long[]{s.overworld().getGameTime(),machine.lane(0).progress(),npc.getEnergy()});
        server.waitFor(s->s.overworld().getGameTime()>=pause[0]+20,80);
        server.runOnServer(s->{check(machine.lane(0).progress()==pause[1],"pause stops assisted progress");check(npc.getEnergy()>=pause[2],"paused work stops reserve drain");});
    }
}
