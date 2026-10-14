package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real garden work, synchronized equipped-tool rendering, and frozen visual checkpoints. */
public final class PeepoHarvestClientTests implements FabricClientGameTest {
    private int checks;
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context){
        context.runOnClient(c->local.peepo.client.CompanionClipChecks.harvesting(this::check));
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();var base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            for(int variant=0;variant<2;variant++)tomatoes(server,base.offset(12,0,0),variant);
            new CompanionEggCollectionChecks(this::check,base.offset(-20,0,0)).run(server);
            new CompanionAnimalCareChecks(this::check,base.offset(-20,0,12)).run(context,server);
            context.getInput().pressKey(options->options.keyToggleGui);
            server.runOnServer(s->{for(var p:BlockPos.betweenClosed(base.offset(-5,-2,-5),base.offset(5,1,5)))s.overworld().setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);});
            var soil=base.east().below();
            server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f 100 12",soil.getX()+2.8,soil.getY()+.1,soil.getZ()+1.0));
            for(int variant=0;variant<3;variant++){
                final int v=variant;
                int id=server.computeOnServer(s->{var l=s.overworld();
                    l.setBlockAndUpdate(soil.north(),Blocks.WATER.defaultBlockState());
                    l.setBlockAndUpdate(base.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
                    l.setBlockAndUpdate(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
                    l.setBlockAndUpdate(soil.above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
                    var p=(v==1?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(l,EntitySpawnReason.COMMAND);
                    p.snapTo(soil.getX()+.5,base.getY(),soil.getZ()+2.5,180,0);l.addFreshEntity(p);
                    var owner=s.getPlayerList().getPlayers().getFirst();p.orders.tame(owner);p.orders.command(owner,3);p.preferences.social=false;p.preferences.carryMeals=0;
                    p.belongings.setItem(9,new ItemStack(v==1?Items.IRON_HOE:Items.DIAMOND_HOE));if(v==2)p.belongings.setItem(8,new ItemStack(Items.JACK_O_LANTERN));
                    p.assignments.assignGardenZone(base.below(),soil,false);return p.getId();
                });
                for(int checkpoint:new int[]{20,38}){
                    server.waitFor(s->{var p=(PeepoEntity)s.overworld().getEntity(id);
                        if(p.workAnimation()!=WorkAnimation.HARVEST || s.overworld().getGameTime()-p.workStarted()<checkpoint)return false;
                        s.tickRateManager().setFrozen(true);return true;
                    },900);
                    try{
                        context.waitFor(c->c.level.getEntity(id) instanceof PeepoEntity p && p.workAnimation()==WorkAnimation.HARVEST,100);
                        context.waitTicks(3);
                        server.runOnServer(s->{var p=(PeepoEntity)s.overworld().getEntity(id);
                            check(p.workTarget().equals(soil),"harvest targets actual crop instead of plot anchor");
                            check(s.overworld().getBlockState(soil.above()).getValue(CropBlock.AGE)==7,"crop remains until action completes");
                        });
                        context.runOnClient(c->{var p=(PeepoEntity)c.level.getEntity(id);
                            var renderer=(local.peepo.client.PeepoRenderer)c.getEntityRenderDispatcher().getRenderer(p);var state=renderer.createRenderState();renderer.extractRenderState(p,state,0);
                            check(state.work==WorkAnimation.HARVEST && !state.hoe.isEmpty() && state.held.isEmpty(),"equipped hoe renders exactly once during harvest");
                        });
                        context.takeScreenshot("harvest_"+v+"_"+checkpoint);
                    }finally{server.runOnServer(s->s.tickRateManager().setFrozen(false));}
                }
                server.waitFor(s->s.overworld().getBlockState(soil.above()).getValue(CropBlock.AGE)<7,200);
                server.runOnServer(s->{var p=(PeepoEntity)s.overworld().getEntity(id);
                    check(p.belongings.getItem(9).getDamageValue()==1,"harvest applies tool wear once");
                    int wheat=0;for(int i=0;i<8;i++)if(p.belongings.getItem(i).is(Items.WHEAT))wheat+=p.belongings.getItem(i).getCount();
                    check(wheat>=1,"harvest still produces real crops");check(p.workAnimation()!=WorkAnimation.HARVEST,"harvest pose clears after completion");p.discard();
                    s.overworld().setBlockAndUpdate(base,Blocks.AIR.defaultBlockState());
                });
            }
        }
        org.slf4j.LoggerFactory.getLogger("peepo-harvest-test").info("[peepo-harvest-test] PASS {} assertions",checks);
    }

    private void tomatoes(TestServerContext server,BlockPos soil,int variant){
        var tomato=(TomatoVineBlock)JugcraftAgriculture.TALL_CROPS.get(TallCrop.TOMATO);
        for(int scenario=0;scenario<4;scenario++){
            boolean rotten=scenario!=0,full=scenario==2,retire=scenario==3;
            final int sample=scenario;
            PeepoEntity[] worker={null};CompanionGarden.Job[] active={null};
            net.minecraft.world.level.block.state.BlockState[] before=new net.minecraft.world.level.block.state.BlockState[2];
            CompanionStatus[] result={CompanionStatus.WORKING};long[] lastTick={Long.MIN_VALUE};
            server.runOnServer(s->{var level=s.overworld();var owner=s.getPlayerList().getPlayers().getFirst();
                for(var pos:BlockPos.betweenClosed(soil.offset(-2,0,-2),soil.offset(2,3,2)))
                    level.setBlock(pos,pos.getY()==soil.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                level.setBlock(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),2);
                for(int section=1;section>=0;section--)level.setBlock(soil.above(section+1),tomato.stateFor(7,section),2);
                // Replacing the previous scenario's soil can drop its leftover plant; start each case without fixture debris.
                for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(soil).inflate(3)))item.discard();
                if(rotten)check(tomato.goOver(level,soil.above()),"tomato ripens past its picking window");
                before[0]=level.getBlockState(soil.above());before[1]=level.getBlockState(soil.above(2));
                var npc=(variant==0?PeepoMod.PEEPO:PeepoMod.JUGHEAD).create(level,EntitySpawnReason.COMMAND);
                worker[0]=npc;
                npc.setNoAi(true);npc.setNoGravity(true);npc.snapTo(soil.getX()+1.5,soil.getY()+1,soil.getZ()+.5,0,0);level.addFreshEntity(npc);
                npc.orders.tame(owner);npc.orders.command(owner,3);npc.preferences.social=false;npc.preferences.carryMeals=0;
                npc.assignments.assignGardenZone(soil,soil,false);
                if(retire)check(npc.assignments.selectGardenSeed(1,new ItemStack(Items.WHEAT_SEEDS)),"different crop can replace an overripe tomato plot");
                if(full)for(int slot=0;slot<8;slot++)npc.belongings.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
                var job=npc.garden.job(soil);
                active[0]=job;
                check(job!=null && job.workStatus(npc)==CompanionStatus.READY,"tomato harvest is ready");
                var at=job.approachPosition();npc.snapTo(at.x,at.y,at.z,0,0);
                check(job.claim(npc),"tomato plot is claimed");
            });
            try{
                server.waitFor(s->{
                    long now=s.overworld().getGameTime();if(lastTick[0]==now)return false;lastTick[0]=now;
                    result[0]=active[0].work(worker[0]);return result[0]!=CompanionStatus.WORKING;
                },CompanionGarden.WORK_TICKS+40);
                server.runOnServer(s->{var level=s.overworld();var npc=worker[0];
                    if(full){
                        check(result[0]==CompanionStatus.FULL,"full cargo rejects the complete harvest");
                        check(level.getBlockState(soil.above()).equals(before[0]) && level.getBlockState(soil.above(2)).equals(before[1]),"full cargo preserves both overripe vine sections");
                        for(int slot=0;slot<8;slot++)check(npc.belongings.getItem(slot).is(Items.COBBLESTONE) && npc.belongings.getItem(slot).getCount()==64,"full cargo is unchanged");
                    }else{
                        check(result[0]==CompanionStatus.IDLE,"tomato harvest completes: variant="+variant+", scenario="+sample+", result="+result[0]);
                        int count=0;
                        for(int slot=0;slot<8;slot++){
                            var stack=npc.belongings.getItem(slot);if(stack.isEmpty())continue;
                            check(stack.is(JugcraftAgriculture.item(rotten?"rotten_tomato":"tomato")),"harvest uses the vine's actual ripeness");count+=stack.getCount();
                        }
                        check(count>=2 && count<=4,"tomato yield stays within normal picking limits");
                        for(int section=0;section<2;section++){
                            var after=level.getBlockState(soil.above(section+1));
                            check(retire?after.is(JugcraftAgriculture.block("trellis")):after.is(tomato) && after.getValue(TallCropBlock.AGE)==TallCrop.TOMATO.pickReset
                            && after.getValue(TallCropBlock.SECTION)==section && !TomatoVineBlock.isOverripe(after),"both sections regrow fresh or restore the existing trellis: variant="+variant+", scenario="+sample+", section="+section+", state="+after);
                        }
                    }
                    check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(soil).inflate(3)).isEmpty(),"harvest never spills duplicate produce into the world: variant="+variant+", scenario="+sample);
                });
            }finally{server.runOnServer(s->{worker[0].resetCompanionRoutine();worker[0].discard();});}
        }
        server.runOnServer(s->{for(var pos:BlockPos.betweenClosed(soil.offset(-2,0,-2),soil.offset(2,3,2)))s.overworld().setBlock(pos,Blocks.AIR.defaultBlockState(),2);});
    }
}
