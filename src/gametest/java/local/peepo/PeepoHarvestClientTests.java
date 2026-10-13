package local.peepo;

import java.util.*;
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
                    p.assignments.assign(l,base.below());return p.getId();
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
}
