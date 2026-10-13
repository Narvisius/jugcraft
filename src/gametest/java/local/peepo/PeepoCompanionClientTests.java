package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.client.CookingPotScreen;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import local.peepo.client.CompanionScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;
import org.slf4j.LoggerFactory;

/** Run only with -PclientTests=PeepoCompanionClientTests. Uses disposable worlds, never a player's save. */
public final class PeepoCompanionClientTests implements FabricClientGameTest {
    private final List<String> failures = new ArrayList<>();
    private int assertions, groups;
    private BlockPos origin;
    private UUID savedNpc;
    private UUID seatedNpc;
    private Identifier recipeId;

    private void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private void group(String name, Runnable test) {
        try { test.run(); groups++; LoggerFactory.getLogger("peepo-test").info("[peepo-test] PASS {}", name); }
        catch (AssertionError | RuntimeException error) {
            failures.add(name + ": " + error);
            LoggerFactory.getLogger("peepo-test").error("[peepo-test] FAIL " + name, error);
        }
    }
    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        try (var world = context.worldBuilder().adjustSettings(creator -> {
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
                .lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            creator.setGenerateStructures(false);
        }).create()) {
            var server = world.getServer();
            world.getConnection().waitForChunksRender();
            context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});
            group("model sleep, costume and work poses", () -> context.runOnClient(c->modelPoses()));
            origin = context.computeOnClient(c -> c.player.blockPosition().above(4));
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runOnServer(s -> {
                var level = s.overworld();
                for (var p : BlockPos.betweenClosed(origin.offset(-14,-1,-14), origin.offset(14,-1,14)))
                    level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
                var player = s.getPlayerList().getPlayers().getFirst();
                player.snapTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5,0,0);
                group("feeding, taming, blush, costume, equipment", () -> feeding(level, player));
                group("energy, transactions, recovery and preferences", () -> energy(level));
                group("cargo transactions and inventory persistence", () -> inventory(level, player));
                group("home, four jobs and priorities", () -> assignments(level, player));
                group("lunch crate and chest cover conservation", () -> lunch(level, player));
                group("wheel conservation, exclusivity and side outputs", () -> wheel(level));
                group("stool and cardinal/diagonal rail seating", () -> seats(level));
                group("bounded server search and path admission", () -> budgets(level));
            });
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f 0 15",origin.getX()+.5,origin.getY(),origin.getZ()+.5));
            server.runCommand("time set midnight");
            server.runOnServer(s -> group("all bed colors, bunks, sleep and release", () -> beds(s.overworld())));
            server.runCommand("time set noon");
            server.runOnServer(s -> group("cooking recipe plan, gating and assistance", () -> cooking(s.overworld(),s.getPlayerList().getPlayers().getFirst())));
            group("ordinary pot GUI without recipe editing", () -> cookingGui(context, server));
            group("companion GUI synchronization and commands", () -> companionGui(context, server));
            group("automatic cooking approach and animation synchronization", () -> automaticCooking(context, server));
            group("automatic ground food pickup and eating", () -> groundFood(server));
            group("habitat groups and local spawn caps", () -> spawns(server));
            group("seated unload defers movement safely", () -> seatedUnload(server));
            new CompanionAutomationChecks(this::check, this::group, origin.offset(0,0,30)).run(context, server);
            new CompanionPerformanceChecks(this::check,this::group,origin.offset(0,0,30)).run(context,server);
            save = world.getWorldSave();
        }
        if (savedNpc != null) group("world save and reopen", () -> {
            try (var world = save.open()) {
                world.getConnection().waitForChunksRender();
                world.getServer().waitFor(s -> s.overworld().getEntity(savedNpc) instanceof PeepoEntity, 200);
                world.getServer().runOnServer(s -> {
                    var npc = (PeepoEntity)s.overworld().getEntity(savedNpc);
                    check(npc.orders.tamed(), "owner lost on disk reload");
                    check(npc.belongings.getItem(0).is(Items.DIAMOND) && npc.belongings.getItem(0).getCount()==3,"cargo lost on disk reload");
                    check(npc.preferences.carryMeals==4,"preferences lost on disk reload");
                    check(npc.assignments.get(1)!=null,"assignment lost on disk reload");
                    var pot=(CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4));
                    check(!pot.filter().empty(),"pot filter lost on disk reload");
                    if(seatedNpc!=null){
                        var seated=(PeepoEntity)s.overworld().getEntity(seatedNpc);
                        check(seated!=null && seated.isAlive(),"seated companion lost during unload");
                    }
                });
            }
        });
        group("dedicated server companion menus and reconnect", () -> dedicated(context));
        LoggerFactory.getLogger("peepo-test").info("[peepo-test] {} groups passed, {} assertions, {} failed groups",groups,assertions,failures.size());
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n",failures));
    }
    private PeepoEntity npc(ServerLevel level, boolean jughead) {
        var npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        npc.setNoAi(true); npc.setNoGravity(true);
        npc.snapTo(origin.getX()+.5,origin.getY()+2,origin.getZ()+.5,0,0);
        level.addFreshEntity(npc); return npc;
    }
    private PeepoEntity copy(ServerLevel level, PeepoEntity npc) {
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
        npc.saveWithoutId(out);
        var result=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);
        result.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),out.buildResult()));
        return result;
    }
    private void feeding(ServerLevel level, ServerPlayer player) {
        var npc=npc(level,false);
        check(Math.abs(npc.getBbHeight()-.6)<.001,"Peepo scale changed");
        check(!npc.canAttack(player) && !npc.hurtServer(level,level.damageSources().playerAttack(player),2),"friendly fire");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        npc.mobInteract(player,InteractionHand.MAIN_HAND);
        check(npc.isBlushing(),"empty hand did not blush");
        npc.belongings.setItem(9,new ItemStack(Items.TORCH));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BREAD,3));
        npc.mobInteract(player,InteractionHand.MAIN_HAND);
        check(npc.orders.owner(player) && npc.isEating(),"first feed did not tame and eat");
        for(int i=0;i<PeepoEntity.EAT_DURATION;i++)npc.tick();
        check(!npc.isEating() && npc.getMainHandItem().is(Items.TORCH),"eating did not restore held torch");
        check(npc.getFoodRegenBonus()>0 && npc.getFoodRegenTicks()>0,"meal has no energy buff");
        npc.mobInteract(player,InteractionHand.MAIN_HAND);
        check(!npc.isEating(),"full companion accepted more food");
        npc.setStoredEnergy(121600);check(!npc.needsFood(),"95% should decline food");
        npc.setStoredEnergy(121599);check(npc.needsFood(),"below 95% should accept food");
        npc.setStoredEnergy(128000);npc.setHealth(npc.getMaxHealth()-4);
        npc.mobInteract(player,InteractionHand.MAIN_HAND);
        for(int i=0;i<PeepoEntity.EAT_DURATION;i++)npc.tick();
        check(npc.getHealth()==npc.getMaxHealth(),"food failed to heal");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.JACK_O_LANTERN));
        npc.mobInteract(player,InteractionHand.MAIN_HAND);
        check(npc.isWearingPumpkin() && npc.belongings.getItem(8).is(Items.JACK_O_LANTERN),"costume equip");
        var jug=npc(level,true);check(jug.isJughead(),"Jughead variant");jug.discard();npc.discard();
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
    private void energy(ServerLevel level) {
        var npc=npc(level,false);
        check(npc.getEnergy()==128000,"initial reserve");
        npc.setStoredEnergy(1000);
        try(var tx=Transaction.openOuter()){check(npc.extractEnergy(80,tx)==64,"output cap");}
        check(npc.getEnergy()==1000,"aborted extraction lost energy");
        try(var tx=Transaction.openOuter()){check(npc.extractEnergy(60,tx)==60,"committed extraction");tx.commit();}
        try(var tx=Transaction.openOuter()){check(npc.extractEnergy(60,tx)==4,"per-tick shared cap");tx.commit();}
        check(npc.getEnergy()==936 && npc.getEnergyRegenPerTick()==0,"working regenerated energy");
        npc.setStoredEnergy(0);npc.updateRecoveryState();check(npc.isRecovering(),"exhaustion break");
        npc.setStoredEnergy(100000);npc.updateRecoveryState();check(npc.isRecovering(),"resumed below 80%");
        npc.setStoredEnergy(102400);npc.updateRecoveryState();check(!npc.isRecovering(),"did not resume at 80%");
        var idle=npc(level,false);idle.setStoredEnergy(1000);idle.setDeltaMovement(.1,0,0);
        check(idle.getEnergyRegenPerTick()==2,"moving passive regen");
        idle.setRestMode(CompanionEnergy.Rest.SITTING);check(idle.getEnergyRegenPerTick()==8,"chair regen");
        check(!idle.setRestMode(CompanionEnergy.Rest.SLEEPING),"daytime sleep allowed");
        var weak=CompanionEnergy.meal(new ItemStack(Items.APPLE).get(DataComponents.FOOD));
        var strong=CompanionEnergy.meal(new ItemStack(Items.COOKED_BEEF).get(DataComponents.FOOD));
        check(strong.bonusPerTick()>weak.bonusPerTick() && strong.durationTicks()>weak.durationTicks(),"food quality ordering");
        for(int i=0;i<20;i++)idle.preferences.command(45);
        check(idle.preferences.breakAt==70 && idle.preferences.resumeAt>=idle.preferences.breakAt+10,"break bounds");
        for(int i=0;i<20;i++)idle.preferences.command(46);
        check(idle.preferences.resumeAt>=idle.preferences.breakAt+10,"resume hysteresis");
        npc.discard();idle.discard();
    }
    private void inventory(ServerLevel level,ServerPlayer player) {
        var npc=npc(level,false);npc.orders.tame(player);
        try(var tx=Transaction.openOuter()){check(npc.food.store(new ItemStack(Items.BREAD,3),tx)==3,"store food");}
        check(npc.belongings.isEmpty(),"aborted cargo transaction retained items");
        try(var tx=Transaction.openOuter()){npc.food.store(new ItemStack(Items.BREAD,3),tx);tx.commit();}
        npc.belongings.setItem(8,new ItemStack(Items.JACK_O_LANTERN));npc.belongings.setItem(9,new ItemStack(Items.TORCH));
        var clone=copy(level,npc);
        check(clone.orders.owner(player) && clone.belongings.getItem(0).getCount()==3,"cargo or owner persistence");
        check(clone.isWearingPumpkin() && clone.getMainHandItem().is(Items.TORCH),"equipment persistence");
        var menu=new CompanionMenu(1,player.getInventory(),npc);
        check(menu.slots.size()==59 && menu.slots.get(8).getMaxStackSize()==1,"8 cargo + 2 equipment slots plus filter controls");
        check(!menu.slots.get(8).mayPlace(new ItemStack(Items.STONE)),"costume slot accepts junk");
        check(!menu.clickMenuButton(player,999) && menu.quickMoveStack(player,-1).isEmpty(),"invalid menu input");
        npc.discard();
    }
    private WheelBlockEntity placeWheel(ServerLevel level,BlockPos pos) {
        for(int i=0;i<4;i++)level.setBlock(WheelBlock.partPos(pos,Direction.NORTH,i),GeneratorWheel.BLOCK.defaultBlockState().setValue(WheelBlock.PART,i),3);
        return (WheelBlockEntity)level.getBlockEntity(pos);
    }
    private CookingPotBlockEntity placePot(ServerLevel level,BlockPos pos) {
        level.setBlockAndUpdate(pos,JugcraftAgriculture.block("cooking_pot").defaultBlockState());
        return (CookingPotBlockEntity)level.getBlockEntity(pos);
    }
    private void assignments(ServerLevel level,ServerPlayer player) {
        var npc=npc(level,false);npc.orders.tame(player);
        var bed=origin.offset(-10,0,-10);level.setBlockAndUpdate(bed,CompanionBeds.BLOCKS.getFirst().defaultBlockState());
        npc.assignments.assign(level,bed);check(npc.assignments.get(0)!=null,"home assignment");
        for(int i=0;i<5;i++){var pos=origin.offset(-10+i*2,0,-6);placePot(level,pos);npc.assignments.assign(level,pos);}
        check(npc.assignments.get(4)!=null && npc.assignments.get(5)==null,"four work slots");
        check(!npc.assignments.assignedWork(origin.offset(-2,0,-6)),"fifth workstation accepted");
        var second=npc.assignments.get(2);check(npc.assignments.moveWork(2,-1) && npc.assignments.get(1).equals(second),"priority reorder");
        check(!npc.assignments.moveWork(1,-1),"priority outside bounds");
        npc.assignments.clear(2);check(npc.assignments.get(3)!=null && npc.assignments.get(4)==null,"remove compaction");
        check(copy(level,npc).assignments.get(1).equals(second),"assignment serialization");
        npc.discard();
    }
    private void lunch(ServerLevel level,ServerPlayer player) {
        var npc=npc(level,false);npc.orders.tame(player);npc.preferences.carryMeals=4;
        var pos=origin.offset(-6,0,0);level.setBlockAndUpdate(pos,CompanionLunch.CRATE.defaultBlockState());
        var crate=(LunchBlockEntity)level.getBlockEntity(pos);crate.setOwner(player.getUUID());crate.setItem(0,new ItemStack(Items.BREAD,5));
        check(crate.stockMeal(npc) && crate.getItem(0).getCount()==4 && npc.food.meals()==1,"crate transfer conservation");
        for(int i=0;i<8;i++)npc.belongings.setItem(i,new ItemStack(Items.STONE,64));
        check(!crate.stockMeal(npc) && crate.getItem(0).getCount()==4,"full cargo consumed source food");
        npc.belongings.clearContent();crate.setOwner(UUID.randomUUID());check(!crate.stockMeal(npc),"foreign lunch owner");
        var chest=origin.offset(-6,0,3);level.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        ((Container)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.APPLE,4));
        level.setBlockAndUpdate(chest.above(),CompanionLunch.COVER.defaultBlockState());
        var cover=(LunchBlockEntity)level.getBlockEntity(chest.above());cover.setOwner(player.getUUID());
        check(cover.stockMeal(npc) && ((Container)level.getBlockEntity(chest)).getItem(0).getCount()==3,"chest cover transfer");
        npc.discard();
    }
    private void wheel(ServerLevel level) {
        var wheel=placeWheel(level,origin.offset(5,0,-5));var a=npc(level,false);var b=npc(level,true);
        check(wheel.claim(a) && wheel.occupy(a) && !wheel.claim(b),"wheel exclusive lease");
        int before=a.getEnergy();check(CompanionWork.transfer(a,wheel)==64,"wheel generation");
        check(a.getEnergy()==before-64 && wheel.output.getAmount()==64,"wheel energy conservation");
        check(CompanionWork.transfer(a,wheel)==0 && wheel.output.getAmount()==64,"duplicate tick generation");
        wheel.energy.setAmount(32000);check(!wheel.worthStarting(b) && CompanionWork.transfer(a,wheel)==0,"full wheel accepted work");
        var pos=wheel.getBlockPos();
        for(var side:Direction.values())check((EnergyStorage.SIDED.find(level,pos,side)!=null)==(side==Direction.WEST),"master port "+side);
        for(var side:Direction.values())check((EnergyStorage.SIDED.find(level,pos.east(),side)!=null)==(side==Direction.EAST),"secondary port "+side);
        wheel.release(a);check(wheel.claim(b),"wheel lease not released");wheel.release(b);a.discard();b.discard();
    }
    private void seats(ServerLevel level) {
        var npc=npc(level,false);var pos=origin.offset(8,0,0);
        var stool=BuiltInRegistries.BLOCK.getValue(PeepoMod.id("wooden_stool"));level.setBlockAndUpdate(pos,stool.defaultBlockState());
        var seat=new CompanionSeats.Surface(level,pos,level.getBlockState(pos),Direction.SOUTH);
        check(seat.claim(npc) && seat.occupy(npc),"stool seat");
        check(npc.getZ()>pos.getZ()+.7 && npc.getRestMode()==CompanionEnergy.Rest.SITTING,"legs not over edge");seat.release(npc);
        for(int diagonal=0;diagonal<2;diagonal++){
            var p=origin.offset(8,0,4+diagonal*3);var q=p.offset(1,0,diagonal);
            level.setBlockAndUpdate(p,Blocks.OAK_FENCE.defaultBlockState());level.setBlockAndUpdate(q,Blocks.OAK_FENCE.defaultBlockState());
            var rail=new CompanionSeats.Surface(level,p,level.getBlockState(p),1,diagonal,1);
            check(rail.claim(npc) && rail.occupy(npc),"rail seat diagonal="+diagonal);
            check(Math.abs(npc.getY()-(p.getY()+15/16.0-.094))<.001,"rail height");rail.release(npc);
        }
        npc.discard();
    }
    private void budgets(ServerLevel level) {
        int searches=0,paths=0;var npcs=new ArrayList<PeepoEntity>();
        for(int i=0;i<16;i++){var p=npc(level,false);npcs.add(p);if(CompanionBudget.search(p))searches++;if(CompanionBudget.path(p))paths++;}
        check(searches==4 && paths==8,"default budgets: "+searches+" searches, "+paths+" paths");
        npcs.forEach(PeepoEntity::discard);
    }
    private void beds(ServerLevel level) {
        check(CompanionBeds.BLOCKS.size()==16,"bed colors");
        var pos=origin.offset(-10,0,8);var state=CompanionBeds.BLOCKS.getFirst().defaultBlockState();
        level.setBlockAndUpdate(pos,state);level.setBlockAndUpdate(pos.above(),state);
        var lower=(CompanionBedEntity)level.getBlockEntity(pos);var upper=(CompanionBedEntity)level.getBlockEntity(pos.above());
        check(lower.entrance().equals(upper.entrance()),"upper bunk entrance");
        var npc=npc(level,true);check(upper.claim(npc) && upper.occupy(npc),"Jughead upper bunk");
        check(npc.getRestMode()==CompanionEnergy.Rest.SLEEPING && npc.getEnergyRegenPerTick()==16,"sleep regen");
        upper.release(npc);check(npc.getRestMode()==CompanionEnergy.Rest.NONE && Math.abs(npc.getY()-pos.getY())<.01,"bunk exit to ground");npc.discard();
    }
    private void cooking(ServerLevel level,ServerPlayer player) {
        var pot=placePot(level,origin.offset(0,0,4));
        var entry=CookingPotRecipe.catalog(level.getServer()).entrySet().stream()
            .filter(e->e.getValue().parts().size()<=6).findFirst().orElseThrow();recipeId=entry.getKey();
        pot.selectRecipe(recipeId);var plan=pot.supplyPlan().orElseThrow();
        check(plan.id().equals(recipeId) && !plan.ingredients().isEmpty(),"counted supply plan");
        pot.selectRecipe(Identifier.parse("peepo_companion:not_a_recipe"));check(pot.selectedRecipe().equals(recipeId),"invalid recipe accepted");
        check(pot.cookingStatus()==CompanionStatus.NO_HEAT,"cold pot status");
        level.setBlockAndUpdate(pot.getBlockPos().below(),Blocks.CAMPFIRE.defaultBlockState());
        check(pot.cookingStatus()==CompanionStatus.NO_INPUT,"missing inputs status");
        fill(pot,plan);
        check(pot.cookingStatus()==CompanionStatus.READY,"prepared recipe not ready");
        check(!pot.canPlaceItem(0,new ItemStack(Items.BEDROCK)),"recipe filter accepts unrelated items");
        for(int i=6;i<10;i++)pot.setItem(i,new ItemStack(Items.STONE,64));
        check(pot.cookingStatus()==CompanionStatus.FULL,"full output status");for(int i=6;i<10;i++)pot.setItem(i,ItemStack.EMPTY);
        var npc=npc(level,false);npc.orders.tame(player);npc.assignments.assign(level,pot.getBlockPos());
        var job=pot.companionJob.prepare(npc);var at=job.approachPosition();npc.snapTo(at.x,at.y,at.z,0,0);
        check(job.claim(npc) && job.occupy(npc),"pot claim and mount");
        var other=npc(level,true);other.orders.tame(player);other.assignments.assign(level,pot.getBlockPos());
        check(!job.claim(other),"pot allowed two helpers");
        int before=npc.getEnergy();check(pot.assist(npc) && npc.getEnergy()==before-16,"cooking energy debit");
        check(!pot.assist(npc) && npc.getEnergy()==before-16,"double assist same tick");
        job.release(npc);check(!npc.isNoGravity() && !pot.assist(npc),"pot release");
        npc.discard();other.discard();
    }
    private void fill(CookingPotBlockEntity pot,CookingPotPlan plan){
        for(int i=0;i<6;i++)pot.setItem(i,ItemStack.EMPTY);
        for(int i=0;i<plan.ingredients().size();i++){
            var part=plan.ingredients().get(i);var item=part.ingredient().items().findFirst().orElseThrow();
            pot.setItem(i,new ItemStack(item,part.count()*8));
        }
    }
    private void cookingGui(ClientGameTestContext context,TestServerContext server){
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().openMenu((CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4))));
        context.waitForScreen(CookingPotScreen.class);
        context.waitTicks(3);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(!p.containerMenu.clickMenuButton(p,0),"pot still accepts recipe selection buttons");});
        context.takeScreenshot("peepo_ordinary_pot_gui");
        closeMenu(context,server);
    }
    private void companionGui(ClientGameTestContext context,TestServerContext server){
        server.runOnServer(s->{
            var npc=npc(s.overworld(),false);savedNpc=npc.getUUID();npc.orders.tame(s.getPlayerList().getPlayers().getFirst());
            npc.belongings.setItem(0,new ItemStack(Items.DIAMOND,3));npc.preferences.carryMeals=4;
            npc.assignments.assign(s.overworld(),origin.offset(0,0,4));
            s.getPlayerList().getPlayers().getFirst().openMenu(new SimpleMenuProvider((id,inv,p)->new CompanionMenu(id,inv,npc),npc.getDisplayName()));
        });
        context.waitForScreen(CompanionScreen.class);
        context.waitFor(c->c.player.containerMenu.getSlot(0).getItem().getCount()==3,100);
        check(context.computeOnClient(c->c.player.containerMenu.slots.size()==59),"companion slot sync");
        context.takeScreenshot("peepo_companion_gui");
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,40));
        try { server.waitFor(s->((PeepoEntity)s.overworld().getEntity(savedNpc)).preferences.schedule==1,100); }
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var n=(PeepoEntity)s.overworld().getEntity(savedNpc);return "schedule="+n.preferences.schedule+", distance="+p.distanceTo(n)+", owner="+n.orders.owner(p)+", menu="+p.containerMenu.getClass()+", valid="+p.containerMenu.stillValid(p);}),e);}
        ghostRecipeGui(context,server);
        closeMenu(context,server);
    }
    private void ghostRecipeGui(ClientGameTestContext context,TestServerContext server){
        server.runOnServer(s->{
            var pot=(CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4));pot.selectRecipe(null);for(int i=0;i<9;i++)pot.filter().set(i,ItemStack.EMPTY);
            var output=CookingPotRecipe.catalog(s).get(recipeId).output().create().copyWithCount(4);
            s.getPlayerList().getPlayers().getFirst().getInventory().setItem(9,output);
        });
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,60));
        context.waitFor(c->c.player.containerMenu instanceof CompanionMenu m && m.filterRow()==0,100);
        context.waitFor(c->c.player.containerMenu.getSlot(10).getItem().getCount()==4 && c.player.containerMenu.getSlot(CompanionMenu.FILTER_START).getItem().isEmpty(),100);
        clickSlot(context,10,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
        context.waitFor(c->c.player.containerMenu.getCarried().getCount()==4,100);
        clickSlot(context,CompanionMenu.FILTER_START,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
        server.waitFor(s->!((CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4))).filter().empty(),100);
        context.waitFor(c->c.player.containerMenu.getSlot(CompanionMenu.FILTER_START).getItem().getCount()==1,100);
        check(context.computeOnClient(c->c.player.containerMenu.getCarried().getCount()==4),"ghost consumed cursor item");
        for(var type:new net.minecraft.world.inventory.ContainerInput[]{net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,net.minecraft.world.inventory.ContainerInput.CLONE,net.minecraft.world.inventory.ContainerInput.THROW,net.minecraft.world.inventory.ContainerInput.SWAP})clickSlot(context,CompanionMenu.FILTER_START,0,type);
        server.runOnServer(s->{
            var p=s.getPlayerList().getPlayers().getFirst();var menu=p.containerMenu;
            check(menu.getCarried().getCount()==4 && menu.getSlot(CompanionMenu.FILTER_START).getItem().getCount()==1,"ghost extraction/duplication");
            check(!menu.getSlot(CompanionMenu.FILTER_START).mayPickup(p) && !menu.getSlot(CompanionMenu.FILTER_START).mayPlace(new ItemStack(Items.APPLE)),"ghost became storage");
        });
        context.waitTicks(3);clickSlot(context,CompanionMenu.FILTER_START,1,net.minecraft.world.inventory.ContainerInput.PICKUP);
        server.waitFor(s->((CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4))).filter().empty(),100);
        context.waitFor(c->c.player.containerMenu.getSlot(CompanionMenu.FILTER_START).getItem().isEmpty(),100);
        check(context.computeOnClient(c->c.player.containerMenu.getCarried().getCount()==4),"clearing ghost changed cursor");
        context.waitTicks(3);clickSlot(context,CompanionMenu.FILTER_START,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
        context.waitFor(c->c.player.containerMenu.getSlot(CompanionMenu.FILTER_START).getItem().getCount()==1,100);
        clickSlot(context,10,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
        context.waitFor(c->c.player.containerMenu.getCarried().isEmpty() && c.player.containerMenu.getSlot(10).getItem().getCount()==4,100);
        context.takeScreenshot("peepo_companion_ghost_recipe");
    }
    private void clickSlot(ClientGameTestContext context,int slot,int button,net.minecraft.world.inventory.ContainerInput type){
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,button,type,c.player));
    }
    private void automaticCooking(ClientGameTestContext context,TestServerContext server){
        closeMenu(context,server);
        for(boolean jughead:new boolean[]{false,true}){
            UUID id=server.computeOnServer(s->{
                var npc=npc(s.overworld(),jughead);npc.orders.tame(s.getPlayerList().getPlayers().getFirst());
                npc.snapTo(origin.getX()+2.5,origin.getY(),origin.getZ()+4.5,0,0);npc.setNoGravity(false);npc.setOnGround(true);
                npc.assignments.assign(s.overworld(),origin.offset(0,0,4));((CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4))).selectRecipe(recipeId);npc.setNoAi(false);return npc.getUUID();
            });
            try { server.waitFor(s->s.overworld().getEntity(id) instanceof PeepoEntity p && p.workAnimation()==WorkAnimation.STIR,600); }
            catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->{
                var p=(PeepoEntity)s.overworld().getEntity(id);var pot=(CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4));
                return "cook "+jughead+": pos="+p.position()+", routine="+p.routineStatus()+", pot="+pot.cookingStatus()+", job="+pot.companionJob.prepare(p).workStatus(p)+", entry="+pot.companionJob.approachPosition()+", energy="+p.getEnergy()+", health="+p.getHealth()+", order="+p.orders.mode()+", budget="+CompanionBudget.statistics(s);
            }),e);}
            context.waitFor(c->{for(var e:c.level.entitiesForRendering())if(e.getUUID().equals(id) && e instanceof PeepoEntity p)return p.workAnimation()==WorkAnimation.STIR;return false;},100);
            check(context.computeOnClient(c->{for(var e:c.level.entitiesForRendering())if(e.getUUID().equals(id) && e instanceof PeepoEntity p)return p.workAnimation()==WorkAnimation.STIR;return false;}),"stir pose not synced");
            var standingAt=server.computeOnServer(s->s.overworld().getEntity(id).position());
            check(Math.abs(standingAt.y-origin.getY()-10.0/16-.001)<1.0E-6,"feet on pot rim surface");
            check(Math.abs(Math.max(Math.abs(standingAt.x-origin.getX()-.5),Math.abs(standingAt.z-origin.getZ()-4.5))-5.0/16)<1.0E-6,"standing on pot edge");
            long[] before=server.computeOnServer(s->{
                var pot=(CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4));
                pot.selectRecipe(null);pot.selectRecipe(recipeId);fill(pot,pot.supplyPlan().orElseThrow());
                return new long[]{s.overworld().getGameTime(),((PeepoEntity)s.overworld().getEntity(id)).getEnergy()};
            });
            server.waitFor(s->s.overworld().getGameTime()>=before[0]+40,100);
            server.runOnServer(s->{
                var p=(PeepoEntity)s.overworld().getEntity(id);var pot=(CookingPotBlockEntity)s.overworld().getBlockEntity(origin.offset(0,0,4));
                var player=s.getPlayerList().getPlayers().getFirst();var menu=(CookingPotMenu)pot.createMenu(99,player.getInventory(),player);
                long ticks=s.overworld().getGameTime()-before[0];int progress=menu.progress(pot.supplyPlan().orElseThrow().time());
                check(Math.abs(progress-ticks*1.5)<=3,"assisted speed: "+progress+" progress in "+ticks+" ticks");
                check(Math.abs(before[1]-p.getEnergy()-ticks*16)<=16,"assisted energy per tick");
                check(p.position().distanceToSqr(standingAt)<1.0E-8,"stirring companion must stay planted");
            });
            int previousFov=context.computeOnClient(c->c.options.fov().get());
            var previousParticles=context.computeOnClient(c->c.options.particles().get());
            var camera=server.computeOnServer(s->s.getPlayerList().getPlayers().getFirst().position());
            server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",origin.getX()+2.5,(double)origin.getY(),origin.getZ()+6.5));
            context.runOnClient(c->{c.options.fov().set(40);c.options.particles().set(net.minecraft.server.level.ParticleStatus.MINIMAL);});
            context.waitTicks(10);context.getInput().lookAt(origin.offset(0,0,4));context.waitTicks(15);
            context.takeScreenshot(jughead?"peepo_jughead_stirring":"peepo_stirring");
            context.waitTicks(20);
            context.takeScreenshot(jughead?"peepo_jughead_stirring_second_pose":"peepo_stirring_second_pose");
            context.runOnClient(c->{c.options.fov().set(previousFov);c.options.particles().set(previousParticles);});
            server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",camera.x,camera.y,camera.z));
            server.runOnServer(s->{var p=(PeepoEntity)s.overworld().getEntity(id);p.resetCompanionRoutine();p.discard();});
        }
    }
    private void closeMenu(ClientGameTestContext context,TestServerContext server){
        context.runOnClient(c->c.player.closeContainer());context.waitForScreen(null);
        // A close packet from the previous screen must arrive before the server opens the next one.
        server.waitFor(s->s.getPlayerList().getPlayers().getFirst().containerMenu instanceof net.minecraft.world.inventory.InventoryMenu,100);
    }
    private void modelPoses(){
        var root=local.peepo.client.PeepoGeometry.create().bakeRoot();var model=new local.peepo.client.PeepoModel(root);
        var s=new local.peepo.client.PeepoState();s.sleeping=true;model.setupAnim(s);
        check(root.getChild("head").getChild("sleeping_eyes").visible && !root.getChild("head").getChild("left_pupil").visible,"sleeping eyes");
        s.sleeping=false;s.jughead=true;model.setupAnim(s);
        check(root.getChild("jughead_shorts").visible && root.getChild("head").getChild("jughead_jug").visible && !root.getChild("left_arm").getChild("left_sleeve").visible,"Jughead clothing");
        s.pumpkin=true;model.setupAnim(s);check(root.getChild("pumpkin").visible && !root.getChild("body").visible && !root.getChild("jughead_shorts").visible,"pumpkin costume visibility");
        s.pumpkin=false;s.work=WorkAnimation.INTERACT;s.workPhase=0;model.setupAnim(s);float arm=root.getChild("left_arm").xRot;
        s.workPhase=.4F;model.setupAnim(s);check(Math.abs(arm-root.getChild("left_arm").xRot)>.1 && root.getChild("left_arm").xRot==root.getChild("right_arm").xRot,"general two-arm animation");
        s.work=WorkAnimation.STIR;model.setupAnim(s);check(root.getChild("left_arm").xRot> -1.5 && root.getChild("left_arm").xRot<-.5 && root.getChild("right_arm").yRot<0,"forward spoon grip");
        float stirringArm=root.getChild("left_arm").yRot;
        s.workPhase+=1.5F;model.setupAnim(s);
        check(Math.abs(stirringArm-root.getChild("left_arm").yRot)>.1,"arms move to stir spoon");
        check(root.getChild("left_leg").xRot==0 && root.getChild("right_leg").xRot==0,"stirring feet stay planted");
        s.work=WorkAnimation.NONE;s.sitting=true;s.ageInTicks=4;model.setupAnim(s);check(root.getChild("left_leg").xRot!=root.getChild("right_leg").xRot,"sitting leg kicks");
        s.sitting=false;s.wheelRunning=true;model.setupAnim(s);check(root.getChild("left_leg").xRot*root.getChild("right_leg").xRot<0,"running stride");
    }
    private void groundFood(TestServerContext server){
        UUID id=server.computeOnServer(s->{
            var npc=npc(s.overworld(),false);npc.snapTo(origin.getX()-3.5,origin.getY(),origin.getZ()-1.5,0,0);
            npc.setNoGravity(false);npc.setNoAi(false);npc.setHealth(10);npc.setStoredEnergy(60000);
            var item=new net.minecraft.world.entity.item.ItemEntity(s.overworld(),npc.getX()+.4,npc.getY(),npc.getZ(),new ItemStack(Items.BREAD,2));
            item.setNoPickUpDelay();s.overworld().addFreshEntity(item);return npc.getUUID();
        });
        server.waitFor(s->s.overworld().getEntity(id) instanceof PeepoEntity p && p.isEating(),400);
        server.waitFor(s->s.overworld().getEntity(id) instanceof PeepoEntity p && p.getFoodRegenTicks()>0,100);
        server.runOnServer(s->{var npc=(PeepoEntity)s.overworld().getEntity(id);check(npc.getHealth()>10 && npc.getEnergy()>60000,"ground food healing/energy");npc.discard();});
    }
    private void spawns(TestServerContext server){
        var pos=origin.offset(10,0,-10);
        server.runCommand(String.format(Locale.ROOT,"fillbiome %d %d %d %d %d %d minecraft:swamp",pos.getX()-4,pos.getY()-4,pos.getZ()-4,pos.getX()+4,pos.getY()+4,pos.getZ()+4));
        server.runOnServer(s->{
            var level=s.overworld();level.setBlockAndUpdate(pos.below(),Blocks.GRASS_BLOCK.defaultBlockState());
            var spawn=level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,pos)
                .getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(e->e.value().type()==PeepoMod.PEEPO).findFirst().orElseThrow();
            check(spawn.value().count().minInclusive()==1 && spawn.value().count().maxInclusive()==4,"spawn groups 1-4");
            check(PeepoSpawns.canSpawn(PeepoMod.PEEPO,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"swamp spawn");
            check(!PeepoSpawns.canSpawn(PeepoMod.JUGHEAD,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"Jughead swamp exclusion");
            var mobs=new ArrayList<PeepoEntity>();
            try{
                for(int i=0;i<6;i++){check(PeepoSpawns.belowLocalLimit(PeepoMod.PEEPO,level,pos),"early spawn cap");var p=npc(level,false);p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);p.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),EntitySpawnReason.NATURAL,null);mobs.add(p);}
                check(!PeepoSpawns.belowLocalLimit(PeepoMod.PEEPO,level,pos) && PeepoSpawns.belowLocalLimit(PeepoMod.JUGHEAD,level,pos),"six-per-variant spawn cap");
                check(copy(level,mobs.getFirst()).isNaturallySpawned(),"spawn origin persistence");
            }finally{mobs.forEach(PeepoEntity::discard);}
        });
    }
    private void seatedUnload(TestServerContext server){
        seatedNpc=server.computeOnServer(s->{
            var p=npc(s.overworld(),false);p.snapTo(origin.getX()+8.5,origin.getY(),origin.getZ()-1.5,0,0);
            p.orders.tame(s.getPlayerList().getPlayers().getFirst());p.preferences.schedule=2;p.setOnGround(true);p.setNoAi(false);p.setNoGravity(false);return p.getUUID();
        });
        server.waitFor(s->((PeepoEntity)s.overworld().getEntity(seatedNpc)).getRestMode()==CompanionEnergy.Rest.SITTING,400);
        server.runOnServer(s->{
            var p=(PeepoEntity)s.overworld().getEntity(seatedNpc);var before=p.position();p.setNoAi(true);p.unloadCompanionRoutine();
            check(p.position().equals(before),"unload moved companion during section removal");
            check(p.getRestMode()==CompanionEnergy.Rest.NONE && !p.isNoGravity(),"unload retained rest state");
            p.tick();check(p.getY()==origin.getY(),"deferred safe exit failed");p.setOnGround(true);p.setNoAi(false);
        });
        server.waitFor(s->((PeepoEntity)s.overworld().getEntity(seatedNpc)).getRestMode()==CompanionEnergy.Rest.SITTING,400);
    }
    private void dedicated(ClientGameTestContext context){
        try(var server=context.worldBuilder().adjustSettings(creator->{
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
                .lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));creator.setGenerateStructures(false);
        }).createServer()){
            try(var connection=server.connect()){
                connection.waitForChunksRender();origin=context.computeOnClient(c->c.player.blockPosition());
                server.runCommand("gamemode creative @a");server.runCommand("time set noon");
                server.runOnServer(s->{
                    var pot=placePot(s.overworld(),origin.offset(0,0,4));pot.selectRecipe(recipeId);
                    s.overworld().setBlockAndUpdate(pot.getBlockPos().below(),Blocks.CAMPFIRE.defaultBlockState());
                    fill(pot,pot.supplyPlan().orElseThrow());
                });
                cookingGui(context,server);companionGui(context,server);
            }
            try(var connection=server.connect()){
                connection.waitForChunksRender();
                server.waitFor(s->s.overworld().getEntity(savedNpc) instanceof PeepoEntity,200);
                server.runOnServer(s->{
                    var p=(PeepoEntity)s.overworld().getEntity(savedNpc);
                    check(p.orders.owner(s.getPlayerList().getPlayers().getFirst()),"dedicated reconnect owner");
                    check(p.preferences.schedule==1 && p.belongings.getItem(0).getCount()==3,"dedicated reconnect preferences/cargo");
                });
            }
        }
    }
}
