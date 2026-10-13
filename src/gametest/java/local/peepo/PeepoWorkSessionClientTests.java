package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real AI sessions: consecutive crop actions, deferred harvest delivery and immediate owner cancellation. */
public final class PeepoWorkSessionClientTests implements FabricClientGameTest {
    private BlockPos base,anchor;
    private PeepoEntity npc;
    private Container output;
    private int checks;
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private int crops(ServerLevel l,boolean ripe){
        int n=0;for(int i=0;i<3;i++){var state=l.getBlockState(anchor.east(i).above());
            if(state.getBlock() instanceof CropBlock crop && (!ripe || crop.isMaxAge(state)))n++;}
        return n;
    }
    private void setup(ServerLevel l,boolean jughead){
        if(npc!=null){npc.transport.stop();npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-5,-1,-5),base.offset(8,4,5)))l.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        anchor=base.below();
        for(int i=0;i<3;i++){
            l.setBlockAndUpdate(anchor.east(i),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
            l.setBlockAndUpdate(anchor.east(i).north(),Blocks.WATER.defaultBlockState());
        }
        l.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)l.getBlockEntity(base.east(6));
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(l,EntitySpawnReason.COMMAND);
        npc.snapTo(base.getX()+.5,base.getY(),base.getZ()+1.5,180,0);l.addFreshEntity(npc);
        var owner=l.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);
        npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.belongings.setItem(0,new ItemStack(Items.WHEAT_SEEDS,32));npc.belongings.setItem(9,new ItemStack(Items.DIAMOND_HOE));
        npc.assignments.assign(l,anchor);npc.assignments.assignContainer(l,base.east(6),Direction.UP,true);
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var s=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            s.runCommand("time set noon");s.runCommand("weather clear");s.runCommand("gamerule minecraft:random_tick_speed 0");
            for(boolean jughead:new boolean[]{false,true})s.runOnServer(v->{
                setup(v.overworld(),jughead);npc.setNoAi(true);npc.setOnGround(false);
                var probe=new CompanionRoutine(npc);probe.resetOrders();
                check(!probe.canUse(),"airborne worker waits before searching");
                check(probe.status(anchor)!=CompanionStatus.BLOCKED,"airborne start must not blacklist a reachable plot");
                npc.setOnGround(true);
                check(probe.canUse(),"landing permits immediate search without a failed-route cooldown");
                probe.stop();
            });
            for(boolean jughead:new boolean[]{false,true}){
                s.runOnServer(v->setup(v.overworld(),jughead));
                s.waitFor(v->crops(v.overworld(),false)==1,600);
                long first=s.computeOnServer(v->{check(npc.isUsingJobAt(anchor),"first seed keeps plot session");return v.overworld().getGameTime();});
                s.waitFor(v->{int planted=crops(v.overworld(),false);
                    if(planted<3 && !npc.isUsingJobAt(anchor))throw new AssertionError("planting released the plot between seeds");return planted==3;
                },220);
                s.runOnServer(v->{check(v.overworld().getGameTime()-first<180,"remaining seeds planted without full search cooldowns");
                    check(npc.belongings.getItem(0).getCount()==29,"each planted seed consumed once");
                    for(int i=0;i<3;i++)v.overworld().setBlockAndUpdate(anchor.east(i).above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
                    npc.resetCompanionRoutine();
                });
                s.waitFor(v->crops(v.overworld(),true)==2,600);
                s.runOnServer(v->{check(npc.isUsingJobAt(anchor),"first harvest keeps plot session");check(output.isEmpty(),"does not unload the first crop immediately");});
                s.waitFor(v->{int ripe=crops(v.overworld(),true);
                    if(ripe>0 && (!npc.isUsingJobAt(anchor) || !output.isEmpty()))throw new AssertionError("harvest session interrupted by an early delivery");return ripe==0;
                },240);
                s.runOnServer(v->check(crops(v.overworld(),false)==3,"harvest replants the whole plot"));
                s.waitFor(v->{for(int i=0;i<output.getContainerSize();i++)if(output.getItem(i).is(Items.WHEAT) && output.getItem(i).getCount()>=3)return true;return false;},600);
                s.runOnServer(v->{check(!npc.isUsingJobAt(anchor),"exhausted plot releases the job for delivery");
                    for(int i=0;i<3;i++)v.overworld().setBlockAndUpdate(anchor.east(i).above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
                    npc.resetCompanionRoutine();
                });
                s.waitFor(v->npc.workAnimation()==WorkAnimation.HARVEST,600);
                s.runOnServer(v->{check(npc.orders.command(v.getPlayerList().getPlayers().getFirst(),1),"Stay command accepted during session");
                    check(!npc.isUsingJobAt(anchor),"Stay cancels session immediately");});
            }
            for(boolean jughead:new boolean[]{false,true}){
                s.runOnServer(v->{setup(v.overworld(),jughead);
                    for(int i=0;i<3;i++)v.overworld().setBlockAndUpdate(anchor.east(i).above(),Blocks.WHEAT.defaultBlockState());
                    var stool=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(PeepoMod.id("wooden_stool"));
                    v.overworld().setBlockAndUpdate(base.south(3),stool.defaultBlockState());
                });
                try{s.waitFor(v->{
                    if(v.overworld().getGameTime()%80==0)org.slf4j.LoggerFactory.getLogger("peepo-session-test").info("seat search: variant={}, position={}, routine={}, ground={}, energy={}",jughead,npc.position(),npc.routineStatus(),npc.onGround(),npc.getEnergy());
                    return npc.getRestMode()==CompanionEnergy.Rest.SITTING;
                },600);}catch(AssertionError e){throw new AssertionError(s.computeOnServer(v->"seat search: variant="+jughead+", position="+npc.position()+", routine="+npc.routineStatus()+", ground="+npc.onGround()),e);}
                s.runOnServer(v->{v.overworld().setBlockAndUpdate(anchor.above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));npc.garden.inputsChanged(anchor);});
                long[] leftSeat={-1};
                try{s.waitFor(v->{
                    long now=v.overworld().getGameTime();
                    if(npc.getRestMode()==CompanionEnergy.Rest.NONE){
                        if(leftSeat[0]<0)leftSeat[0]=now;
                        if(now-leftSeat[0]>10 && !npc.isUsingJobAt(anchor))throw new AssertionError("seat handoff yielded to wandering instead of claiming ready garden");
                    }
                    return npc.workAnimation()==WorkAnimation.HARVEST;
                },240);}catch(AssertionError e){throw new AssertionError(s.computeOnServer(v->"seat resume: position="+npc.position()+", routine="+npc.routineStatus()+", rest="+npc.getRestMode()+", ground="+npc.onGround()+", job="+npc.garden.job(anchor).workStatus(npc)),e);}
                s.runOnServer(v->check(npc.getRestMode()==CompanionEnergy.Rest.NONE,"idle sitting yields to newly ready work"));
                s.waitFor(v->!((CropBlock)v.overworld().getBlockState(anchor.above()).getBlock()).isMaxAge(v.overworld().getBlockState(anchor.above())),100);
                s.runOnServer(v->{check(crops(v.overworld(),false)==3,"resumed work harvests and replants");npc.transport.stop();npc.resetCompanionRoutine();npc.discard();});
            }
            new CompanionAutomationChecks(this::check,(name,action)->{
                action.run();org.slf4j.LoggerFactory.getLogger("peepo-session-test").info("[peepo-session-test] PASS {}",name);
            },base.offset(0,0,20)).workSessionRegressions(s);
        }
        org.slf4j.LoggerFactory.getLogger("peepo-session-test").info("[peepo-session-test] PASS {} assertions",checks);
    }
}
