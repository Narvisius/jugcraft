package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import local.peepo.client.*;

/** Focused real garden/water work plus synchronized client poses; never loads a player world. */
public final class PeepoActionClientTests implements FabricClientGameTest {
    private int checks;
    private BlockPos base;
    private PeepoEntity npc;
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private PeepoEntity reset(ServerLevel level,boolean jughead){
        if(npc!=null){npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-4,-1,-4),base.offset(4,3,4)))
            level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(5)).forEach(e->e.discard());
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        npc.setNoAi(true);npc.setNoGravity(true);npc.snapTo(base.getX()+.5,base.getY(),base.getZ()- .5,0,0);level.addFreshEntity(npc);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);
        npc.preferences.social=false;npc.preferences.carryMeals=0;return npc;
    }
    @Override public void runTest(ClientGameTestContext context){
        context.runOnClient(c->CompanionActionChecks.run(this::check));
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            for(boolean jughead:new boolean[]{false,true})work(server,jughead);
            context.getInput().pressKey(o->o.keyToggleGui);
            for(boolean jughead:new boolean[]{false,true})for(int sample=0;sample<9;sample++)visual(context,server,jughead,sample);
            visual(context,server,false,7,2);visual(context,server,false,7,34);
            server.runOnServer(s->{npc.setWorkAnimation(WorkAnimation.NONE,base);npc.showGesture(CompanionGesture.PUT,new ItemStack(Items.APPLE),base);});
            server.waitFor(s->npc.gesture()==CompanionGesture.NONE,40);
            server.runOnServer(s->{check(npc.gesture()==CompanionGesture.NONE,"short transfer pulse expires without inventory work");npc.discard();});
            context.getInput().pressKey(o->o.keyToggleGui);
        }
        org.slf4j.LoggerFactory.getLogger("peepo-action-test").info("[peepo-action-test] PASS {} assertions",checks);
    }
    private void work(TestServerContext server,boolean jughead){
        CompanionJob[] job={null};CompanionStatus[] result={CompanionStatus.WORKING};long[] last={Long.MIN_VALUE};int[] ticks={0};
        server.runOnServer(s->{var level=s.overworld();reset(level,jughead);var soil=base.below();
            level.setBlockAndUpdate(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
            npc.belongings.setItem(0,new ItemStack(Items.WHEAT_SEEDS,2));npc.assignments.assignGardenZone(soil,soil,false);
            job[0]=npc.garden.job(soil);check(job[0]!=null && job[0].workStatus(npc)==CompanionStatus.READY && job[0].claim(npc),"planting job claims actual empty soil");
            var entry=job[0].approachPosition();npc.snapTo(entry.x,entry.y,entry.z,0,0);
            check(job[0].animation()==WorkAnimation.PLANT,"empty farmland selects planting");
        });
        server.waitFor(s->{
            if(last[0]==s.overworld().getGameTime())return false;last[0]=s.overworld().getGameTime();
            npc.setWorkAnimation(job[0].animation(),job[0].animationTarget());result[0]=job[0].work(npc);
            if(++ticks[0]==5)check(npc.workPie().is(Items.WHEAT_SEEDS) && npc.workPie().getCount()==1,"seed display is a single item snapshot");
            return result[0]!=CompanionStatus.WORKING;
        },120);
        server.runOnServer(s->{var level=s.overworld();
            check(result[0]==CompanionStatus.IDLE && level.getBlockState(base).is(Blocks.WHEAT),"planting still completes with real wheat: "+result[0]);
            check(ticks[0]==CompanionGarden.WORK_TICKS && npc.belongings.getItem(0).getCount()==1,"planting keeps timing and consumes exactly one seed");job[0].release(npc);
            reset(level,jughead);level.setBlockAndUpdate(base.below(),Blocks.WATER.defaultBlockState());npc.belongings.setItem(0,new ItemStack(Items.BUCKET));
            check(npc.assignments.assign(level,base.below()).contains("assigned:"),"water source assigned");job[0]=npc.water.job(base.below());
            check(job[0]!=null && job[0].claim(npc),"water collection ready");var entry=job[0].approachPosition();npc.snapTo(entry.x,entry.y,entry.z,0,0);
            ticks[0]=0;last[0]=Long.MIN_VALUE;
        });
        server.waitFor(s->{
            if(last[0]==s.overworld().getGameTime())return false;last[0]=s.overworld().getGameTime();
            npc.setWorkAnimation(job[0].animation(),job[0].animationTarget());int energy=npc.getEnergy();result[0]=job[0].work(npc);ticks[0]++;
            check(npc.getEnergy()==energy-CompanionWater.ENERGY_PER_TICK,"water animation adds no energy drain");
            return result[0]!=CompanionStatus.WORKING;
        },80);
        server.runOnServer(s->{var level=s.overworld();
            check(result[0]==CompanionStatus.IDLE && ticks[0]==CompanionWater.WORK_TICKS,"water collection still finishes in forty ticks");
            check(npc.belongings.getItem(0).is(Items.WATER_BUCKET),"real filled bucket remains in inventory");job[0].release(npc);
            for(var facing:Direction.Plane.HORIZONTAL){
                reset(level,jughead);level.setBlockAndUpdate(base,JugcraftAgriculture.block("cider_press").defaultBlockState().setValue(CiderPressBlock.FACING,facing));
                var press=(CiderPressBlockEntity)level.getBlockEntity(base);press.addApple();npc.assignments.assign(level,base);
                var pressJob=press.companionJob.prepare(npc);var entry=pressJob.approachPosition();npc.snapTo(entry.x,entry.y,entry.z,0,0);
                check(pressJob.claim(npc) && pressJob.occupy(npc),"press is reachable: "+facing);int energy=npc.getEnergy();
                check(pressJob.animation()==WorkAnimation.PRESS && pressJob.work(npc)==CompanionStatus.WORKING,"press uses its specific clip");
                check(press.apples()==0 && press.pulp()==1 && npc.getEnergy()==energy-64,"press stroke retains material and energy behavior");
                check(Math.abs(press.visualHandleAngle(4)-45)<.001F,"handle smoothly reaches half a quarter turn");
                pressJob.work(npc);check(npc.getEnergy()==energy-64,"visual motion cannot bypass press cooldown");pressJob.release(npc);
            }
        });
    }
    private void visual(ClientGameTestContext context,TestServerContext server,boolean jughead,int sample){
        visual(context,server,jughead,sample,sample<5?6:18);
    }
    private void visual(ClientGameTestContext context,TestServerContext server,boolean jughead,int sample,int phaseTicks){
        var gesture=sample<5?CompanionGesture.values()[sample+1]:CompanionGesture.NONE;
        var action=sample<5?WorkAnimation.NONE:new WorkAnimation[]{WorkAnimation.PLANT,WorkAnimation.PICK,WorkAnimation.WATER_FILL,WorkAnimation.PRESS}[sample-5];
        int id=server.computeOnServer(s->{var l=s.overworld();reset(l,jughead);
            if(sample==4)l.setBlockAndUpdate(base,JugcraftAgriculture.block("kitchen_stove").defaultBlockState());
            else if(sample==3)l.setBlockAndUpdate(base,io.github.jimbozoomer.jugcraft.farming.JugcraftFarming.SPRINKLER.defaultBlockState());
            else if(sample==8){l.setBlockAndUpdate(base,JugcraftAgriculture.block("cider_press").defaultBlockState());var press=(CiderPressBlockEntity)l.getBlockEntity(base);press.addApple();press.grind(l.getGameTime());}
            else if(sample==7)l.setBlockAndUpdate(base.below(),Blocks.WATER.defaultBlockState());
            else if(sample<5)l.setBlockAndUpdate(base,Blocks.BARREL.defaultBlockState());
            else if(sample==5)l.setBlockAndUpdate(base.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
            else if(sample==6){l.setBlockAndUpdate(base.below(),Blocks.DIRT.defaultBlockState());l.setBlockAndUpdate(base,Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE,3));}
            npc.belongings.setItem(0,new ItemStack(Items.APPLE,17));return npc.getId();
        });
        if(sample==7)server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f -65 28",base.getX()-1.3,base.getY()-.3,base.getZ()+.5));
        else server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f 45 22",base.getX()+2.1,base.getY()+.0,base.getZ()-2.0));
        context.waitTicks(3);
        var target=sample==7?base.below():base;
        server.runOnServer(s->{npc.setWorkAnimation(action,target);npc.setWorkPie(new ItemStack(sample==7?(jughead?Items.GLASS_BOTTLE:Items.BUCKET):Items.WHEAT_SEEDS));
            if(gesture!=CompanionGesture.NONE)npc.showGesture(gesture,new ItemStack(sample==3?Items.WATER_BUCKET:Items.APPLE),target);
        });
        long started=server.computeOnServer(s->s.overworld().getGameTime());
        server.waitFor(s->{npc.setWorkAnimation(action,target);if(gesture==CompanionGesture.CARRY)npc.showGesture(gesture,new ItemStack(Items.APPLE),target);
            if(s.overworld().getGameTime()-started<phaseTicks)return false;s.tickRateManager().setFrozen(true);return true;
        },70);
        try{
            context.waitFor(c->c.level.getEntity(id) instanceof PeepoEntity p && (gesture==CompanionGesture.NONE?p.workAnimation()==action:p.gesture()==gesture),60);
            context.waitTicks(2);
            context.runOnClient(c->{var p=(PeepoEntity)c.level.getEntity(id);var r=(PeepoRenderer)c.getEntityRenderDispatcher().getRenderer(p);var state=r.createRenderState();r.extractRenderState(p,state,0);
                check(state.actionPose,"client received action "+sample);check(state.held.isEmpty(),"normal held item does not overlap action");
                if(sample==4)check(state.spatula && state.actionItem.isEmpty(),"stove uses spatula instead of duplicate raw food");
                if(sample==0 || sample==3 || sample==7)check(!state.actionItem.isEmpty(),"carry/water uses actual item model");
                if(sample==8)CompanionActionChecks.press(p,state,this::check);
                else check(!state.pressSeated && state.pressOffset.equals(net.minecraft.world.phys.Vec3.ZERO),"press offset clears outside press work");
            });
            if(!jughead || sample==8 || sample==0 || sample==7)context.takeScreenshot("companion_action_"+(jughead?"jughead_":"peepo_")+sample+"_"+phaseTicks);
            server.runOnServer(s->check(npc.belongings.getItem(0).getCount()==17,"client pose cannot alter cargo"));
        }finally{server.runOnServer(s->s.tickRateManager().setFrozen(false));}
    }
}
