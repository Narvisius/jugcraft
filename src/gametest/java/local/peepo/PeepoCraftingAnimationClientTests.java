package local.peepo;

import java.util.*;
import local.peepo.client.PeepoRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import org.slf4j.LoggerFactory;

/** Actual paid crafting phases, safe tabletop access, metadata cadence and in-world visual captures. */
public final class PeepoCraftingAnimationClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private CraftingTableCompanionBlockEntity table;
    private int assertions;
    private void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private void cleanup(){if(npc!=null){if(table!=null)table.job.release(npc);npc.resetCompanionRoutine();npc.discard();npc=null;}}
    private void setup(ServerLevel level,boolean jughead){
        cleanup();
        for(var p:BlockPos.betweenClosed(base.offset(-5,-1,-5),base.offset(5,4,5)))
            level.setBlock(p,p.getY()<base.getY()?Blocks.OAK_PLANKS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(7)).forEach(e->e.discard());
        level.setBlockAndUpdate(base,Blocks.CRAFTING_TABLE.defaultBlockState());table=(CraftingTableCompanionBlockEntity)level.getBlockEntity(base);
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);npc.setNoAi(true);
        npc.snapTo(base.getX()+.5,base.getY(),base.getZ()+3.5,180,0);npc.setOnGround(true);level.addFreshEntity(npc);
        npc.orders.tame(level.getServer().getPlayerList().getPlayers().getFirst());npc.preferences.social=false;npc.preferences.carryMeals=0;npc.assignments.assign(level,base);
        check(CompanionFilters.set(table,0,new ItemStack(Items.WOODEN_PICKAXE)),"native pickaxe filter accepted");
        var supply=new SimpleContainer(new ItemStack(Items.OAK_PLANKS,3),new ItemStack(Items.STICK,2));
        var source=ContainerStorage.of(supply,Direction.UP);var port=CompanionLogistics.resolve(npc,npc.assignments.get(1));port.prepare(source);
        try(var tx=Transaction.openOuter()){
            check(port.inputs().insert(ItemVariant.of(Items.OAK_PLANKS),3,tx)==3 && port.inputs().insert(ItemVariant.of(Items.STICK),2,tx)==2,"real recipe ingredients installed");tx.commit();
        }
    }
    private CraftingTableJob mount(){
        var job=table.job.prepare(npc);var at=job.approachPosition();npc.snapTo(at.x,at.y,at.z,180,0);npc.setOnGround(true);
        check(job.claim(npc) && job.occupy(npc),"table entrance reaches a safe perch");return job;
    }
    private int installed(){return table.installed().stream().mapToInt(ItemStack::getCount).sum();}
    private int output(){int n=0;for(var view:table.outputStorage())if(!view.isResourceBlank())n+=view.getAmount();return n;}
    private void clearVisuals(String reason){
        check(npc.workAnimation()==WorkAnimation.NONE && npc.craftingStarted()==0
            && npc.craftingIngredient(0).isEmpty() && npc.craftingIngredient(1).isEmpty() && npc.craftingResult().isEmpty(),reason+" clears all render previews");
    }
    private void ground(String reason){
        check(Math.abs(npc.getY()-base.getY())<.001 && !npc.isNoGravity() && npc.level().noCollision(npc,npc.getBoundingBox()),reason+" returns safely to floor with gravity");
        clearVisuals(reason);
    }
    private void geometry(ServerLevel level,boolean jughead){
        setup(level,jughead);var job=mount();
        check(Math.abs(npc.getY()-(base.getY()+1.001))<.00001 && npc.isNoGravity(),"feet are just above the tabletop");
        var bounds=npc.getBoundingBox();
        check(bounds.minX>=base.getX() && bounds.maxX<=base.getX()+1 && bounds.minZ>=base.getZ() && bounds.maxZ<=base.getZ()+1,
            "standing companion stays wholly over the tabletop");
        check(level.noCollision(npc,npc.getBoundingBox()) && npc.getDirection()==Direction.NORTH,"full companion body clears table and faces inward");
        check(job.work(npc)==CompanionStatus.WORKING && table.progress()==1,"perched companion performs one paid work tick");
        check(npc.craftingIngredient(0).is(Items.OAK_PLANKS) && npc.craftingIngredient(1).is(Items.STICK)
            && npc.craftingResult().is(Items.WOODEN_PICKAXE),"previews use actual distinct ingredients and native result");
        check(npc.craftingIngredient(0).getCount()==1 && npc.craftingIngredient(1).getCount()==1 && installed()==5 && output()==0,"preview stacks neither consume nor create inventory");
        job.release(npc);ground("release");
        level.setBlockAndUpdate(base.above(),Blocks.STONE.defaultBlockState());job.approachFailed(npc);job.prepare(npc);
        check(job.workStatus(npc)==CompanionStatus.BLOCKED && !job.claim(npc),"blocked tabletop headroom prevents mounting both rigs");
        level.setBlockAndUpdate(base.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(base.above(2),Blocks.STONE.defaultBlockState());job.approachFailed(npc);job.prepare(npc);
        check(job.workStatus(npc)==CompanionStatus.READY,"one full air block above tabletop fits this companion");
        level.setBlockAndUpdate(base.above(2),Blocks.AIR.defaultBlockState());
        for(var side:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(base.relative(side).above(),Blocks.STONE.defaultBlockState());
        job.approachFailed(npc);job.prepare(npc);check(job.workStatus(npc)==CompanionStatus.BLOCKED,"outside lift cannot pass through an overhang to a clear tabletop");
        for(var side:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(base.relative(side).above(),Blocks.AIR.defaultBlockState());
        job.approachFailed(npc);mount();level.destroyBlock(base,false);ground("broken table");
    }
    private static Set<Integer> craftFields(){
        var result=new HashSet<Integer>();
        try{for(var name:new String[]{"CRAFT_STARTED","CRAFT_FIRST","CRAFT_SECOND","CRAFT_RESULT"}){
            var field=PeepoEntity.class.getDeclaredField(name);field.setAccessible(true);result.add(((EntityDataAccessor<?>)field.get(null)).id());
        }}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        return result;
    }
    private void paidUntil(TestServerContext server,int progress){
        server.waitFor(s->{var job=table.job.prepare(npc);check(job.occupy(npc),"claimed worker stays on perch");
            check(job.work(npc)==CompanionStatus.WORKING,"paid animation tick remains valid");return table.progress()>=progress;},progress+60);
    }
    private void metadata(TestServerContext server){
        server.runOnServer(s->{setup(s.overworld(),false);mount();});paidUntil(server,15);
        final long[] anchor={0};final int[] paused={0};
        server.runOnServer(s->{
            anchor[0]=npc.craftingStarted();paused[0]=table.progress();long elapsed=s.overworld().getGameTime()-anchor[0];check(elapsed>=paused[0]-1 && elapsed<=paused[0]+2,"visual phase anchors to already paid progress");
            var fields=craftFields();npc.getEntityData().packDirty();
            npc.setCraftingVisuals(npc.craftingIngredient(0).copyWithCount(64),npc.craftingIngredient(1).copyWithCount(32),npc.craftingResult().copy(),anchor[0]);
            var dirty=npc.getEntityData().packDirty();check(dirty==null || dirty.stream().noneMatch(v->fields.contains(v.id())),"unchanged preview types and clock do not resend metadata");
            table.job.release(npc);ground("paused craft");
        });
        long resume=server.computeOnServer(s->s.overworld().getGameTime()+12);server.waitFor(s->s.overworld().getGameTime()>=resume,60);
        server.runOnServer(s->{
            check(table.progress()==paused[0],"animation pause does not produce unpaid work");mount();table.job.work(npc);
            check(npc.craftingStarted()==s.overworld().getGameTime()-paused[0] && npc.craftingStarted()>anchor[0],"resumed visual starts at saved paid phase, with a corrected clock");
            check(installed()==5 && output()==0 && s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(3)).isEmpty(),"inspection previews have no physical item entities or early outputs");
            npc.setWorkAnimation(WorkAnimation.INTERACT,base);clearVisualsForOtherWork();table.job.release(npc);
        });
    }
    private void clearVisualsForOtherWork(){check(npc.craftingStarted()==0 && npc.craftingIngredient(0).isEmpty() && npc.craftingIngredient(1).isEmpty() && npc.craftingResult().isEmpty(),"switching to another work clip clears crafting previews");}
    private void settingsInterrupt(TestServerContext server,boolean jughead){
        server.runOnServer(s->{setup(s.overworld(),jughead);npc.setNoAi(false);});
        server.waitFor(s->npc.workAnimation()==WorkAnimation.CRAFT && table.progress()>2,500);
        server.runOnServer(s->{
            var owner=s.getPlayerList().getPlayers().getFirst();owner.snapTo(base.getX()+2,base.getY(),base.getZ()+2,180,0);
            var menu=new CompanionMenu(97,owner.getInventory(),npc);check(npc.editingSettings(),"opening actual companion settings pauses active craft");ground("settings interruption");
            int progress=table.progress();check(table.job.work(npc)==CompanionStatus.IDLE && table.progress()==progress,"settings cannot leave an unattended craft running");
            npc.setNoAi(true);menu.removed(owner);
        });
    }
    private void visual(ClientGameTestContext context,TestServerContext server,boolean jughead){
        server.runOnServer(s->{setup(s.overworld(),jughead);mount();});
        int id=server.computeOnServer(s->npc.getId());
        server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",base.getX()-1.0,base.getY()+.7,base.getZ()-1.5));
        context.waitTicks(3);context.getInput().lookAt(base.above());
        // Re-mount after positioning the camera; these are real paid crafts with only the server frozen for each image.
        server.runOnServer(s->{table.job.release(npc);mount();});
        for(int target:new int[]{13,36,54}){
            server.waitFor(s->{var job=table.job.prepare(npc);job.occupy(npc);job.work(npc);
                if(table.progress()<target)return false;s.tickRateManager().setFrozen(true);return true;},100);
            try{
                context.waitFor(c->c.level.getEntity(id) instanceof PeepoEntity p && p.workAnimation()==WorkAnimation.CRAFT && !p.craftingResult().isEmpty(),100);
                context.waitTicks(2);
                context.runOnClient(c->{
                    var p=(PeepoEntity)c.level.getEntity(id);var renderer=(PeepoRenderer)c.getEntityRenderDispatcher().getRenderer(p);var state=renderer.createRenderState();renderer.extractRenderState(p,state,0);
                    check(state.work==WorkAnimation.CRAFT && state.held.isEmpty(),"client uses dedicated crafting pose without duplicate held item");
                    check(!state.craftIngredient0.isEmpty() && !state.craftIngredient1.isEmpty() && !state.craftResult.isEmpty(),"client resolves native ingredient and result item models");
                    check(state.craftPose.ticks>=target-2 && state.craftPose.ticks<=target+1,"client phase follows server paid progress");
                });
                context.takeScreenshot("crafting_"+(jughead?"jughead_":"peepo_")+(target==13?"gather":target==36?"press":"inspect"));
                if(target==54){
                    context.runOnClient(c->c.options.fov().set(45));
                    server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",base.getX()+2.4,base.getY()+.15,base.getZ()+2.6));
                    context.waitTicks(3);context.getInput().lookAt(base.above());context.waitTicks(2);
                    context.takeScreenshot("crafting_"+(jughead?"jughead":"peepo")+"_standing_side");
                    context.runOnClient(c->c.options.fov().set(30));
                }
                server.runOnServer(s->check(installed()==5 && output()==0,"frozen recipe preview does not create early products"));
            }finally{server.runOnServer(s->s.tickRateManager().setFrozen(false));}
        }
        server.waitFor(s->{var job=table.job.prepare(npc);job.occupy(npc);job.work(npc);return output()==1;},100);
        server.runOnServer(s->{check(installed()==0,"real result appears only after all sixty paid ticks");table.job.release(npc);ground("finished craft");});
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));server.runCommand("time set noon");server.runCommand("weather clear");
            server.runOnServer(s->{geometry(s.overworld(),false);geometry(s.overworld(),true);});metadata(server);
            settingsInterrupt(server,false);settingsInterrupt(server,true);
            context.runOnClient(c->local.peepo.client.CraftingBenchChecks.run(this::check));
            int oldFov=context.computeOnClient(c->c.options.fov().get());context.runOnClient(c->c.options.fov().set(30));context.getInput().pressKey(o->o.keyToggleGui);server.runCommand("gamemode spectator @a");
            try{visual(context,server,false);visual(context,server,true);}
            finally{server.runOnServer(s->s.tickRateManager().setFrozen(false));context.runOnClient(c->c.options.fov().set(oldFov));context.getInput().pressKey(o->o.keyToggleGui);server.runOnServer(s->cleanup());}
        }
        LoggerFactory.getLogger("peepo-crafting-animation").info("[peepo-crafting-animation] PASS {} assertions",assertions);
    }
}
