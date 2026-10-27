package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.machine.form.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.registries.BuiltInRegistries;
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

/** Focused remaining-station stock targets; never executes unrelated mod tests. */
public final class PeepoRemainingStockClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc,other;
    private BlockEntity station;
    private Container grid,source,output;
    private int assertions;
    private void check(boolean ok,String text){assertions++;if(!ok)throw new AssertionError(text);}
    private void phase(String text){LoggerFactory.getLogger("peepo-remaining-stock").info("[peepo-remaining-stock] {}",text);}
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
        if(block instanceof FormMachineBlock f){f.setPlacedBy(level,base,state,null,ItemStack.EMPTY);((FormMachineBlockEntity)level.getBlockEntity(base)).serverTick(level,base,state);}
        else if(block instanceof MachineBlock m){var fp=m.footprint(state);for(int i=1;i<fp.size();i++)level.setBlock(fp.partPos(base,state.getValue(MachineBlock.FACING),i),state.setValue(LargeMachineBlock.PART,i),2);}
        if(block instanceof WorkshopBlock w)w.setPlacedBy(level,base,state,null,ItemStack.EMPTY);
        station=level.getBlockEntity(base);grid=station instanceof Container c?c:null;
        level.setBlockAndUpdate(base.west(5),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(5));
        level.setBlockAndUpdate(base.east(5),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(5));
        npc=helper(level,station instanceof MachineBlockEntity);check(stock()!=null,"station exposes stock controls: "+block);
    }
    private void goal(ItemStack item,int n){check(CompanionFilters.set(station,0,item),"native filter accepts product "+item);stock().useOutputs(npc,npc.assignments.get(1));stock().set(filter().get(0),n,0);}
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
    private ItemStack icon(String id){return new ItemStack(JugcraftAgriculture.item(id));}
    private int underway(ItemStack icon){return stock().count(npc,station,icon).underway();}
    private ItemStack preserve(ServerLevel l){var jar=new ItemStack(BuiltInRegistries.ITEM.stream().filter(i->i instanceof PreserveJarItem).findFirst().orElseThrow());PreserveJarItem.cooked(jar,l.getGameTime());return jar;}
    private void prepareFood(ServerLevel l,String kind){
        reset(l,JugcraftAgriculture.block(kind));
        switch(kind){
            case "cider_press"->{goal(icon("sweet_cider"),2);source.setItem(0,new ItemStack(Items.APPLE,16));source.setItem(1,new ItemStack(Items.GLASS_BOTTLE,16));}
            case "cider_barrel"->{goal(icon("aged_cider"),2);source.setItem(0,icon("sweet_cider").copyWithCount(12));}
            case "witchs_brew_punch_bowl"->{goal(icon("witchs_brew_punch"),3);source.setItem(0,new ItemStack(Items.SWEET_BERRIES,16));source.setItem(1,new ItemStack(Items.GLASS_BOTTLE,16));}
            case "spinning_wheel"->{goal(Knitting.yarn(DyeColor.WHITE.getTextureDiffuseColor(),1),3);source.setItem(0,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool")),12));source.setItem(1,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_wool")),12));}
            case "canning_kettle"->{l.setBlockAndUpdate(base.below(),Blocks.CAMPFIRE.defaultBlockState());((CanningKettleBlockEntity)station).fill();var jar=preserve(l);goal(jar,2);source.setItem(0,jar.copyWithCount(8));}
            case "candy_kettle"->{l.setBlockAndUpdate(base.below(),Blocks.CAMPFIRE.defaultBlockState());goal(new ItemStack(CandyKind.HARD_CANDY.item()),3);source.setItem(0,new ItemStack(Items.SUGAR,16));source.setItem(1,PotionContents.createItemStack(Items.POTION,Potions.WATER));source.setItem(2,icon("candy_tray"));}
            default->throw new AssertionError(kind);
        }
    }
    private void plans(ServerLevel l){
        for(var kind:List.of("cider_press","cider_barrel","witchs_brew_punch_bowl","spinning_wheel","canning_kettle","candy_kettle")){
            phase("planning "+kind);prepareFood(l,kind);var product=filter().get(0);var plan=batch();check(!plan.isEmpty(),kind+" has bounded plan");
            if(kind.equals("cider_press") || kind.equals("canning_kettle")){
                check(!station.getBlockState().isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND),kind+" shell blocks walking");
                var previous=npc.position();npc.snapTo(base.getX()-2.5,base.getY(),base.getZ()+.5,0,0);npc.setOnGround(true);
                var path=npc.getNavigation().createPath(base.east(3),0,64);
                check(path!=null && path.canReach(),kind+" has a usable route around its shell");
                for(int i=0;i<path.getNodeCount();i++){
                    var point=path.getNode(i).asBlockPos();
                    check(!point.equals(base) && !point.equals(base.above()),kind+" delivery route avoids the mechanism and its top");
                }
                npc.getNavigation().stop();npc.snapTo(previous.x,previous.y,previous.z,0,0);npc.setOnGround(true);
            }
            if(kind.equals("cider_press"))check(count(plan,Items.APPLE)==2,"press only requests two apples");
            if(kind.equals("cider_barrel"))check(count(plan,JugcraftAgriculture.item("sweet_cider"))==2,"barrel only fills missing servings");
            if(kind.equals("witchs_brew_punch_bowl"))check(count(plan,Items.SWEET_BERRIES)==2 && count(plan,Items.GLASS_BOTTLE)==4,"punch retains whole two-serving berries and all bottles");
            if(kind.equals("spinning_wheel"))check(count(plan,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool")))==1 && count(plan,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_wool")))==0,"spinning preserves yarn color and whole wool yield");
            if(kind.equals("canning_kettle"))check(count(plan,product.getItem())==2,"only missing sealed jars planned");
            if(kind.equals("candy_kettle"))check(count(plan,Items.SUGAR)==2,"two sugar make four pieces for target three");
            check(!CompanionAdditionalStock.busy(station),kind+" simulated stock leaves no real batch");
            other=helper(l,true);carry(npc,plan);check(underway(product)>0,kind+" physical cargo reserves expected product");
            var second=CompanionLogistics.resolve(other,other.assignments.get(1));second.prepare(ContainerStorage.of(source,Direction.UP));
            check(second.supplyBatch(ContainerStorage.of(source,Direction.UP),8).isEmpty(),kind+" another helper cannot duplicate reserved batch");
            carry(npc,List.of());insert(plan);check(underway(product)>0,kind+" native loaded goods still counted");
            if(kind.equals("cider_press")){var c=(CiderPressBlockEntity)station;c.grind(l.getGameTime());c.grind(l.getGameTime());c.turn(l.getGameTime());c.turn(l.getGameTime());check(underway(product)==2,"partially pressed juice counted once");}
            if(kind.equals("cider_barrel")){var c=(CiderBarrelBlockEntity)station;c.setStarted(l.getGameTime()-CiderBarrelBlockEntity.AGED_TICKS);check(underway(product)==2,"aged output not counted twice");}
            // Fresh, empty station at its goal does not start another batch; legacy Unlimited stays available.
            prepareFood(l,kind);product=filter().get(0);output.setItem(0,product.copyWithCount(stock().target(product)));stock().invalidate();
            check(batch().isEmpty(),kind+" stops new supplies when Output target is covered");stock().set(product,0,0);check(!batch().isEmpty(),kind+" Unlimited still works");
        }
        prepareFood(l,"canning_kettle");var sealed=filter().get(0);output.setItem(0,preserve(l).copyWithCount(8));stock().invalidate();
        check(stock().count(npc,station,sealed).stored()==0,"unsealed jars cannot satisfy sealed stock");check(!batch().isEmpty(),"sealed stock still refills with raw jars present in Output");
        prepareFood(l,"spinning_wheel");var white=filter().get(0);output.setItem(0,Knitting.yarn(DyeColor.RED.getTextureDiffuseColor(),12));stock().invalidate();check(stock().count(npc,station,white).stored()==0,"red yarn does not satisfy white yarn target");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());filter().save(saved);filter().load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),saved.buildResult()));check(stock().target(white)==3 && filter().stockKey(white).equals(filter().stockKey(filter().get(0))),"component-aware goal and ghost survive reload");
        var legacy=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());legacy.store("CompanionFilter",ItemStack.OPTIONAL_CODEC.listOf(),List.of(new ItemStack(Items.BREAD)));var rule=legacy.child("ProductionStock").child("Rule0");rule.store("Item",Identifier.CODEC,Identifier.withDefaultNamespace("bread"));rule.putInt("Target",32);rule.putInt("Restock",8);
        var old=new CompanionRecipeFilter();old.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),legacy.buildResult()));check(old.stock.target(new ItemStack(Items.BREAD))==32 && old.stock.restock(new ItemStack(Items.BREAD))==8,"previous identifier-only stock rules still load");
    }
    private void brewing(ServerLevel l){
        reset(l,Blocks.BREWING_STAND);var swift=PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS);goal(swift,1);
        filter().set(1,PotionContents.createItemStack(Items.POTION,Potions.HEALING));stock().set(filter().get(1),2,0);
        for(int i=0;i<3;i++)source.setItem(i,PotionContents.createItemStack(Items.POTION,Potions.WATER));source.setItem(3,new ItemStack(Items.NETHER_WART,8));source.setItem(4,new ItemStack(Items.SUGAR,8));source.setItem(5,new ItemStack(Items.BLAZE_POWDER,8));
        output.setItem(0,filter().get(1));output.setItem(1,filter().get(1));stock().invalidate();check(stock().count(npc,station,swift).stored()==0,"healing stock does not count as swiftness");check(stock().target(swift)==1 && stock().target(filter().get(1))==2,"same potion item has independent goals");
        var plan=batch();check(count(plan,Items.POTION)==1,"only one starting bottle fetched for one missing final potion");
        check(count(plan,Items.NETHER_WART)==1 && count(plan,Items.BLAZE_POWDER)==1,"first brew step retains reagent and fuel");check(count(plan,Items.SUGAR)==0,"later chain reagent not inserted prematurely");
        other=helper(l,true);carry(npc,plan);check(underway(swift)==1,"inbound water promises exactly one final potion");
        check(CompanionLogistics.resolve(other,other.assignments.get(1)).supplyBatch(ContainerStorage.of(source,Direction.UP),8).isEmpty(),"brewing shared reservation blocks duplicate bottles/reagents");
        carry(npc,List.of());insert(plan);check(underway(swift)==1,"loaded water remains projected as final potion");
        var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());VanillaCompanionPort.of(station).save(save);VanillaCompanionPort.of(station).load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),save.buildResult()));check(stock().target(swift)==1 && stock().target(filter().get(1))==2,"potion-specific goals and active chain survive reload");
    }
    private void crucible(ServerLevel l){
        reset(l,JugcraftConcordance.CRUCIBLE);var c=(CrucibleBlockEntity)station;
        var saved=c.saveWithFullMetadata(l.registryAccess());saved.putString("program","water 2; add minecraft:glowstone_dust jugcraft:raw; stir hot");c.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),saved));
        goal(new ItemStack(JugcraftConcordance.DRAUGHT),1);source.setItem(0,new ItemStack(Items.GLOWSTONE_DUST,8));source.setItem(1,new ItemStack(Items.GLASS_BOTTLE,8));
        var plan=batch();check(count(plan,Items.GLOWSTONE_DUST)==1 && count(plan,Items.GLASS_BOTTLE)==2,"crucible reserves whole written formula and its containers");
        carry(npc,plan);check(underway(filter().get(0))==2,"formula doses are reserved while supplies travel");carry(npc,List.of());insert(plan);
        output.setItem(0,new ItemStack(JugcraftConcordance.DRAUGHT));stock().invalidate();check(underway(filter().get(0))==2,"formula already supplied can still finish after external target fulfillment");
        c.clearContent();check(batch().isEmpty(),"uncommitted next formula stops after target");
        filter().set(1,new ItemStack(JugcraftConcordance.SALVE));stock().set(filter().get(1),1,0);source.setItem(2,new ItemStack(Items.BOWL,8));var salves=batch();
        check(c.companion.salve() && count(salves,Items.BOWL)==2 && count(salves,Items.GLASS_BOTTLE)==0,"idle crucible chooses an understocked allowed salve using correct containers");
    }
    private void industrial(ServerLevel l){
        FormMachineBlock chosen=null;
        for(var field:IndustrialForms.class.getFields())if(field.getType()==FormMachineBlock.class)try{
            var b=(FormMachineBlock)field.get(null);if(b.form().companionAssistance() && b.form().itemInputs()>0){
                // Choose a real item-producing form rather than treating fluid bucket icons as physical output.
                var probe=new FormMachineBlockEntity(base,b.defaultBlockState());probe.setLevel(l);
                if(probe.recipes(l.getServer()).stream().anyMatch(e->!e.recipe().items().isEmpty() && !e.recipe().results().isEmpty())){chosen=b;break;}
            }
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        check(chosen!=null,"registered industrial item recipes found");reset(l,chosen);var m=(FormMachineBlockEntity)station;
        var entry=m.recipes(l.getServer()).stream().filter(e->!e.recipe().items().isEmpty() && !e.recipe().results().isEmpty()).findFirst().orElseThrow();m.selectRecipe(entry.id());var r=entry.recipe();var product=r.results().getFirst().create();goal(product,product.getCount());int slot=0;
        for(var part:r.items())source.setItem(slot++,new ItemStack(part.ingredient().items().findFirst().orElseThrow(),part.count()*8));
        if(r.tool().isPresent())source.setItem(slot,new ItemStack(r.tool().get().items().filter(h->m.form().sockets().stream().anyMatch(s->s.accepts(new ItemStack(h)))).findFirst().orElseThrow()));
        try(var tx=Transaction.openOuter()){for(int i=0;i<r.fluids().size();i++){var f=r.fluids().get(i);m.tanks().input(i).insert(FluidVariant.of(f.fluid()),f.amount()*8*FluidConstants.BUCKET/1000,tx);}tx.commit();}
        var plan=batch();check(!plan.isEmpty(),"industrial limited recipe delivery");insert(plan);check(underway(product)==product.getCount(),"industrial installed batch counted");check(batch().isEmpty(),"industrial does not restock already promised batch");
        // Native processing escrows ingredients and reserves output in its own lanes.
        try(var tx=Transaction.openOuter()){var energy=m.energy();if(energy!=null)for(int i=0;i<200;i++)energy.insert(1_000_000,tx);tx.commit();}
        for(int i=0;i<4;i++)m.serverTick(l,base,m.getBlockState());
        check(underway(product)==product.getCount(),"industrial escrow/output keeps the promised amount stable");
        for(var f:r.fluidResults())if(f.fluid().getBucket()!=Items.AIR)check(!CompanionProductionStock.supportsProduct(m,new ItemStack(f.fluid().getBucket())),"fluid targets remain native machine controls");
    }
    private void plating(ServerLevel l){
        reset(l,JugcraftMachines.MACHINES.get(MachineKind.ELECTROPLATING_BATH));goal(new ItemStack(Items.IRON_SWORD),1);
        source.setItem(0,new ItemStack(Items.IRON_SWORD));source.setItem(1,new ItemStack(Items.GOLD_INGOT,8));output.setItem(0,new ItemStack(Items.IRON_SWORD));stock().invalidate();
        check(stock().count(npc,station,filter().get(0)).stored()==0,"unplated tools in Output do not count as plated products");
        var plan=batch();check(count(plan,Items.IRON_SWORD)==1 && count(plan,Items.GOLD_INGOT)==1,"plating requests one tool and its coating metal");insert(plan);
        check(underway(filter().get(0))==1,"plating recipe input counts one pending tool");check(batch().isEmpty(),"plating does not duplicate committed tool");
    }
    private void liveBrewing(TestServerContext server){
        phase("physical potion chain");server.runOnServer(s->{
            reset(s.overworld(),Blocks.BREWING_STAND);goal(PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS),1);
            for(int i=0;i<3;i++)source.setItem(i,PotionContents.createItemStack(Items.POTION,Potions.WATER));source.setItem(3,new ItemStack(Items.NETHER_WART,8));source.setItem(4,new ItemStack(Items.SUGAR,8));source.setItem(5,new ItemStack(Items.BLAZE_POWDER,8));npc.setNoAi(false);
        });
        server.waitFor(s->stock().count(npc,station,filter().get(0)).stored()==1,2400);
        long end=server.computeOnServer(s->s.overworld().getGameTime()+140);server.waitFor(s->s.overworld().getGameTime()>=end,180);
        server.runOnServer(s->{check(count(source,Items.POTION)==2,"only one water bottle consumed through full chain");check(count(source,Items.NETHER_WART)==7 && count(source,Items.SUGAR)==7,"both native chain stages consumed exactly one reagent");check(underway(filter().get(0))==0 && batch().isEmpty(),"finished potion target stops and clears reservations");});
    }
    private void liveCrucible(TestServerContext server){
        phase("physical alchemy formula");server.runOnServer(s->{
            var l=s.overworld();reset(l,JugcraftConcordance.CRUCIBLE);var c=(CrucibleBlockEntity)station;
            var saved=c.saveWithFullMetadata(l.registryAccess());saved.putString("program","water 2; add minecraft:glowstone_dust jugcraft:raw");c.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),saved));
            goal(new ItemStack(JugcraftConcordance.DRAUGHT),1);source.setItem(0,new ItemStack(Items.GLOWSTONE_DUST,8));source.setItem(1,new ItemStack(Items.GLASS_BOTTLE,8));
            try(var tx=Transaction.openOuter()){c.water.insert(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),FluidConstants.BUCKET,tx);tx.commit();}npc.setNoAi(false);
        });
        server.waitFor(s->count(output,JugcraftConcordance.DRAUGHT)==2,2000);
        server.runOnServer(s->{var c=(CrucibleBlockEntity)station;try(var tx=Transaction.openOuter()){c.water.insert(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),FluidConstants.BUCKET,tx);tx.commit();}});
        long end=server.computeOnServer(s->s.overworld().getGameTime()+160);server.waitFor(s->s.overworld().getGameTime()>=end,200);
        server.runOnServer(s->{check(count(source,Items.GLOWSTONE_DUST)==7 && count(source,Items.GLASS_BOTTLE)==6,"written formula consumes one ingredient and its two containers");check(count(output,JugcraftConcordance.DRAUGHT)==2 && batch().isEmpty(),"passive refill water cannot start another companion batch above target");});
    }
    private void physical(TestServerContext server,String kind,int expected){
        phase("physical "+kind);server.runOnServer(s->{prepareFood(s.overworld(),kind);npc.setNoAi(false);});
        try{server.waitFor(s->{if(station instanceof CiderBarrelBlockEntity c && c.servings()==2)c.setStarted(s.overworld().getGameTime()-CiderBarrelBlockEntity.AGED_TICKS);return count(output,filter().get(0).getItem())>=expected;},3000);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->kind+" stalled "+npc.routineStatus()+" / "+npc.transport.activity()+" / "+port().status()+" / "+stock().count(npc,station,filter().get(0))),e);}
        long end=server.computeOnServer(s->s.overworld().getGameTime()+140);server.waitFor(s->s.overworld().getGameTime()>=end,180);
        server.runOnServer(s->{check(count(output,filter().get(0).getItem())==expected,kind+" native production stops at target/whole-batch yield");check(underway(filter().get(0))==0,kind+" completed cargo cleared");check(batch().isEmpty(),kind+" no surplus pickup after deliveries");});
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            server.runOnServer(s->{plans(s.overworld());phase("brewing");brewing(s.overworld());phase("crucible");crucible(s.overworld());phase("industrial");industrial(s.overworld());phase("plating");plating(s.overworld());});
            physical(server,"cider_press",2);physical(server,"cider_barrel",2);physical(server,"witchs_brew_punch_bowl",4);physical(server,"spinning_wheel",4);physical(server,"canning_kettle",2);physical(server,"candy_kettle",4);
            liveBrewing(server);liveCrucible(server);
            server.runOnServer(s->cleanup());
        }
        phase("PASS "+assertions+" assertions");
    }
}
