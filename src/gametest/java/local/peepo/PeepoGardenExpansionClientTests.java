package local.peepo;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;

/** Focused garden expansion checks, including actual Supply/Tools/Output travel in a disposable world. */
public final class PeepoGardenExpansionClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private Container source,output;
    private int checks;
    private void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private int count(Container inventory,Item item){int n=0;for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(item))n+=inventory.getItem(i).getCount();return n;}
    private void reset(ServerLevel level,boolean jughead){
        if(npc!=null){npc.transport.stop();npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-6),base.offset(8,4,6)))level.setBlock(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        level.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(6));
        level.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(6));
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        npc.setNoAi(true);npc.snapTo(base.getX()-2.5,base.getY(),base.getZ()+2.5,0,0);npc.setOnGround(true);level.addFreshEntity(npc);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);
        npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.assignments.assignContainer(level,base.west(6),Direction.UP,false);
        npc.assignments.assignContainer(level,base.east(6),Direction.UP,true);
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runOnServer(s->assignments(s.overworld()));
            planter(server);
            mushrooms(server);
            herbs(server);
            blockedHarvest(server,false);
            blockedHarvest(server,true);
            server.runOnServer(s->{npc.transport.stop();npc.resetCompanionRoutine();npc.discard();});
        }
        org.slf4j.LoggerFactory.getLogger("peepo-garden-expansion-test").info("[peepo-garden-expansion-test] PASS {} assertions",checks);
    }
    private void assignments(ServerLevel level){
        reset(level,false);var soil=base.below();
        for(int i=0;i<10;i++)level.setBlock(soil.offset(i%5,0,i/5),JugcraftAgriculture.block("planter_box").defaultBlockState(),2);
        npc.assignments.assignGardenZone(soil,soil.east(4).south(),false);
        check(npc.assignments.get(1).gardenZone().width()==5 && npc.assignments.get(1).gardenZone().depth()==2,"planters use the selected zone");
        check(!GardenZone.valid(soil,soil.east(16)),"garden rejects more than sixteen blocks wide");
        check(npc.assignments.selectGardenSeed(1,new ItemStack(Items.WHEAT_SEEDS)),"ordinary garden filter accepts wheat");
        check(!npc.assignments.selectGardenSeed(1,new ItemStack(JugcraftAgriculture.item("basil_bundle"))),"planter filter rejects hanging bundles");
        reset(level,true);var beam=base.above(2);
        for(int i=0;i<10;i++){
            var p=beam.offset(i%5,0,i/5);level.setBlock(p,Blocks.OAK_PLANKS.defaultBlockState(),2);
            level.setBlock(p.below(),JugcraftAgriculture.block("basil_bundle").defaultBlockState(),2);
        }
        check(CompanionAssignments.canonical(level,beam.below()).equals(beam),"bundle assignment targets its permanent support");
        npc.assignments.assign(level,beam.below());var target=npc.assignments.get(1);
        check(target.garden() && CompanionGarden.hanging(target) && target.plot().size()==8,"hanging plot uses existing garden slot and eight-position cap");
        check(!npc.assignments.selectGardenSeed(1,new ItemStack(Items.WHEAT_SEEDS)),"drying filter rejects crop seeds");
        check(npc.assignments.selectGardenSeed(1,new ItemStack(JugcraftAgriculture.item("mint_bundle"))),"drying filter accepts selected bundles");
        var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());npc.assignments.save(save);
        npc.assignments.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),save.buildResult()));
        check(npc.assignments.get(1).equals(target) && npc.assignments.gardenSeed(target)==JugcraftAgriculture.item("mint_bundle"),"plot orientation, cells and filter survive reload");
        level.setBlock(beam.below(),Blocks.AIR.defaultBlockState(),2);
        check(target.present(level),"collecting the anchor bundle does not invalidate the drying assignment");
        var port=CompanionLogistics.resolve(npc,target);
        check(port.needed(new ItemStack(JugcraftAgriculture.item("mint_bundle")))==1,"Supply requests only empty drying spots");
        check(port.needed(new ItemStack(JugcraftAgriculture.item("basil_bundle")))==0,"bundle filter applies to Supply");
        level.setBlock(beam,Blocks.AIR.defaultBlockState(),2);
        check(!target.present(level),"missing support invalidates the saved assignment");
    }
    private void planter(TestServerContext server){
        var soil=base.below();
        server.runOnServer(s->{var level=s.overworld();reset(level,false);
            level.setBlock(soil,JugcraftAgriculture.block("planter_box").defaultBlockState(),2);
            npc.assignments.assignGardenZone(soil,soil,false);npc.assignments.selectGardenSeed(1,new ItemStack(Items.WHEAT_SEEDS));
            source.setItem(0,new ItemStack(Items.WHEAT_SEEDS,4));npc.belongings.setItem(9,new ItemStack(Items.IRON_HOE));npc.setNoAi(false);
        });
        server.waitFor(s->s.overworld().getBlockState(base).is(Blocks.WHEAT),1200);
        server.runOnServer(s->{check(count(source,Items.WHEAT_SEEDS)==3,"planter planting consumes exactly one supplied seed");s.overworld().setBlock(base,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),2);});
        server.waitFor(s->count(output,Items.WHEAT)>0,1200);
        server.runOnServer(s->{check(s.overworld().getBlockState(base).is(Blocks.WHEAT) && s.overworld().getBlockState(base).getValue(CropBlock.AGE)<7,"planter harvest replants wheat");check(npc.belongings.getItem(9).getDamageValue()==2,"planter hoe wears once per planting/harvest action");});
    }
    private void mushrooms(TestServerContext server){
        var soil=base.below();var colony=JugcraftAgriculture.colony(Items.BROWN_MUSHROOM);
        server.runOnServer(s->{var level=s.overworld();reset(level,true);
            level.setBlock(soil,JugcraftAgriculture.block("rich_soil").defaultBlockState(),2);
            npc.assignments.assign(level,soil);npc.assignments.selectGardenSeed(1,new ItemStack(Items.BROWN_MUSHROOM));
            source.setItem(0,new ItemStack(Items.BROWN_MUSHROOM,3));source.setItem(1,new ItemStack(Items.SHEARS));npc.assignments.supplies.toggleTools(0);
            check(CompanionLogistics.resolve(npc,npc.assignments.get(1)).needed(new ItemStack(Items.RED_MUSHROOM))==0,"mushroom Supply follows crop selection");
            npc.setNoAi(false);
        });
        server.waitFor(s->s.overworld().getBlockState(base).is(colony),1200);
        server.runOnServer(s->{check(count(source,Items.BROWN_MUSHROOM)==2,"planting consumes one real mushroom");s.overworld().setBlock(base,colony.defaultBlockState().setValue(MushroomColonyBlock.AGE,3),2);});
        server.waitFor(s->count(output,Items.BROWN_MUSHROOM)>0,1600);
        server.runOnServer(s->{
            check(npc.belongings.getItem(9).is(Items.SHEARS) && npc.belongings.getItem(9).getDamageValue()==1,"mushroom job fetches and wears shears exactly once");
            check(s.overworld().getBlockState(base).is(colony) && s.overworld().getBlockState(base).getValue(MushroomColonyBlock.AGE)==MushroomColonyBlock.PICK_RESET,"colony remains to regrow");
            int harvest=count(output,Items.BROWN_MUSHROOM)+count(npc.belongings,Items.BROWN_MUSHROOM);
            check(harvest>=2 && harvest<=3,"mushroom harvest conserves the normal yield");
        });
    }
    private void herbs(TestServerContext server){
        var beam=base.above(2);var bundle=JugcraftAgriculture.block("basil_bundle");
        server.runOnServer(s->{var level=s.overworld();reset(level,false);
            level.setBlock(beam,Blocks.OAK_PLANKS.defaultBlockState(),2);level.setBlock(beam.below(),bundle.defaultBlockState(),2);
            npc.assignments.assign(level,beam.below());source.setItem(0,new ItemStack(bundle,3));
            check(npc.garden.job(beam).workStatus(npc)==CompanionStatus.DRYING,"fresh bundles wait for natural drying");
            check(CompanionLogistics.resolve(npc,npc.assignments.get(1)).needed(new ItemStack(bundle))==0,"Supply does not replace a fresh bundle");
            HerbBundleBlock.dry(level,beam.below(),level.getBlockState(beam.below()));npc.garden.inputsChanged(beam);npc.setNoAi(false);
        });
        server.waitFor(s->count(output,JugcraftAgriculture.item("dried_herbs"))==4,1200);
        server.waitFor(s->s.overworld().getBlockState(beam.below()).is(bundle) && !s.overworld().getBlockState(beam.below()).getValue(HerbBundleBlock.DRIED),1200);
        server.runOnServer(s->{
            check(count(source,bundle.asItem())==2,"empty hanging spot is refilled from Supply with one real bundle");
            check(npc.assignments.get(1).present(s.overworld()),"drying assignment survives harvesting and replacement");
            check(npc.belongings.getItem(9).isEmpty(),"drying never fetches an unnecessary hoe");
        });
    }
    private void blockedHarvest(TestServerContext server,boolean hanging){
        var anchor=hanging?base.above(2):base.below();var crop=hanging?anchor.below():base;
        CompanionGarden.Job[] job={null};var before=new net.minecraft.world.level.block.state.BlockState[1];long[] last={Long.MIN_VALUE};CompanionStatus[] result={null};
        server.runOnServer(s->{var level=s.overworld();reset(level,hanging);
            level.setBlock(anchor,hanging?Blocks.OAK_PLANKS.defaultBlockState():JugcraftAgriculture.block("rich_soil").defaultBlockState(),2);
            before[0]=hanging?JugcraftAgriculture.block("mint_bundle").defaultBlockState().setValue(HerbBundleBlock.DRIED,true):JugcraftAgriculture.colony(Items.RED_MUSHROOM).defaultBlockState().setValue(MushroomColonyBlock.AGE,3);
            level.setBlock(crop,before[0],2);npc.assignments.assign(level,crop);
            if(!hanging){
                check(npc.garden.job(anchor).workStatus(npc)==CompanionStatus.NEEDS_CUTTER,"mushroom picking requires shears or a knife");
                npc.belongings.setItem(9,new ItemStack(Items.SHEARS));npc.garden.inputsChanged(anchor);
            }
            for(int i=0;i<8;i++)npc.belongings.setItem(i,new ItemStack(Items.COBBLESTONE,64));
            job[0]=npc.garden.job(anchor);check(job[0].workStatus(npc)==CompanionStatus.READY,"full-inventory case can approach the crop");
            var at=job[0].approachPosition();npc.snapTo(at.x,at.y,at.z,0,0);npc.setNoGravity(true);check(job[0].claim(npc),"full-inventory job claims its position");
        });
        server.waitFor(s->{long now=s.overworld().getGameTime();if(last[0]==now)return false;last[0]=now;result[0]=job[0].work(npc);return result[0]!=CompanionStatus.WORKING;},130);
        server.runOnServer(s->{
            check(result[0]==CompanionStatus.FULL,"full cargo blocks collection");check(s.overworld().getBlockState(crop).equals(before[0]),"failed collection leaves the crop/bundle untouched");
            check(count(npc.belongings,Items.COBBLESTONE)==512,"failed collection preserves all inventory");
            if(!hanging)check(npc.belongings.getItem(9).getDamageValue()==0,"failed harvest does not damage its tool");
        });
    }
}
