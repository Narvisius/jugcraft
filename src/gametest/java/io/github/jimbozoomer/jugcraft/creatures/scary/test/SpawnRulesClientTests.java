package io.github.jimbozoomer.jugcraft.creatures.scary.test;
import io.github.jimbozoomer.jugcraft.creatures.scary.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;

public final class SpawnRulesClientTests implements FabricClientGameTest {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public void runTest(ClientGameTestContext context) {
  if("1".equals(System.getenv("SCARY_SHOWCASE")))return;
  check(!ScarySpawns.isNight(12999)&&ScarySpawns.isNight(13000)&&ScarySpawns.isNight(22999)&&!ScarySpawns.isNight(23000),"Night boundaries");
  try(TestSingleplayerContext world=context.worldBuilder().adjustSettings(c->c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
   world.getConnection().waitForChunksRender();var server=world.getServer();
   BlockPos p=context.computeOnClient(c->c.player.blockPosition()).offset(8,0,0);
   server.runCommand("difficulty normal");server.runCommand("weather clear");server.runCommand("time set noon");
   server.runOnServer(s->{var l=s.overworld();
    for(int h=0;h<10;h++)l.setBlockAndUpdate(p.above(h),Blocks.AIR.defaultBlockState());
    l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
    for(var t:new EntityType<?>[]{ScaryMod.SPACE_KOOK,ScaryMod.CAPTAIN_CUTLER,ScaryMod.PHANTOM_SHADOW,ScaryMod.BLACK_KNIGHT}) {
     check(t.builtInRegistryHolder().is(ScarySpawns.HALLOWEEN),"Missing Halloween tag: "+t);
     check(!SpawnPlacements.checkSpawnRules(t,l,EntitySpawnReason.NATURAL,p,RandomSource.create(4)),"Daytime spawn: "+t);
    }
   });
   server.runCommand("time set midnight");context.waitTicks(30);
   server.runOnServer(s->{var l=s.overworld();
    for(var t:new EntityType<?>[]{ScaryMod.SPACE_KOOK,ScaryMod.PHANTOM_SHADOW,ScaryMod.BLACK_KNIGHT}) {
     boolean allowed=false;var rng=RandomSource.create(5);
     for(int i=0;i<100;i++)allowed|=SpawnPlacements.checkSpawnRules(t,l,EntitySpawnReason.NATURAL,p,rng);
     check(allowed,"No valid night surface spawn: "+t);
    }
    io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason.setMode(io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason.Mode.OFF);
    check(!ScarySpawns.surface(ScaryMod.SPACE_KOOK,l,EntitySpawnReason.NATURAL,p,RandomSource.create()),"Off-season spawn");
    io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason.setMode(io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason.Mode.ON);
    l.setBlockAndUpdate(p.above(5),Blocks.STONE.defaultBlockState());
    check(!ScarySpawns.surface(ScaryMod.SPACE_KOOK,l,EntitySpawnReason.NATURAL,p,RandomSource.create()),"Roofed Kook spawn");
    check(!ScarySpawns.surface(ScaryMod.PHANTOM_SHADOW,l,EntitySpawnReason.NATURAL,p,RandomSource.create()),"Roofed Shadow spawn");
    check(!ScarySpawns.surface(ScaryMod.BLACK_KNIGHT,l,EntitySpawnReason.NATURAL,p,RandomSource.create()),"Untagged roofed Knight spawn");
    l.setBlockAndUpdate(p.above(5),Blocks.AIR.defaultBlockState());
    for(var t:new EntityType<?>[]{ScaryMod.SPACE_KOOK,ScaryMod.CAPTAIN_CUTLER,ScaryMod.BLACK_KNIGHT}) {
     Mob first=(Mob)t.create(l,EntitySpawnReason.COMMAND);first.setNoAi(true);first.snapTo(p.getX()+2,p.getY(),p.getZ());l.addFreshEntity(first);
     check(ScarySpawns.belowLocalLimit(t,l,p),"Second spawn rejected");
     Mob second=(Mob)t.create(l,EntitySpawnReason.COMMAND);second.setNoAi(true);second.snapTo(p.getX()+4,p.getY(),p.getZ());l.addFreshEntity(second);
     check(!ScarySpawns.belowLocalLimit(t,l,p),"Third spawn permitted");
     second.snapTo(p.getX()+70,p.getY(),p.getZ());
     check(ScarySpawns.belowLocalLimit(t,l,p),"Outside-radius mob counted");first.discard();second.discard();
    }
    check(!ScarySpawns.water(ScaryMod.CAPTAIN_CUTLER,l,EntitySpawnReason.NATURAL,p,RandomSource.create()),"Cutler on dry land");
    l.setBlockAndUpdate(p,Blocks.WATER.defaultBlockState());l.setBlockAndUpdate(p.above(),Blocks.WATER.defaultBlockState());
   });
   context.waitTicks(30);
   server.runOnServer(s->{var l=s.overworld();boolean allowed=false;var rng=RandomSource.create(9);
    for(int i=0;i<100;i++)allowed|=ScarySpawns.water(ScaryMod.CAPTAIN_CUTLER,l,EntitySpawnReason.NATURAL,p,rng);
    check(allowed,"No valid night water spawn");
    var cutler=ScaryMod.CAPTAIN_CUTLER.create(l,EntitySpawnReason.COMMAND);cutler.snapTo(p.getX()+.5,p.getY(),p.getZ()+.5);
    check(cutler.checkSpawnObstruction(l),"Water obstruction rejected");
   });
   server.runCommand("time set noon");context.waitTicks(5);
   server.runOnServer(s->check(!ScarySpawns.water(ScaryMod.CAPTAIN_CUTLER,s.overworld(),EntitySpawnReason.NATURAL,p,RandomSource.create()),"Daytime water spawn"));
  }
 }
}
