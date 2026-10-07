package io.github.jimbozoomer.jugcraft.creatures.scary.test;
import io.github.jimbozoomer.jugcraft.creatures.scary.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.*;

public final class ModelScreenshots implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context) {
  context.restoreDefaultGameOptions();
  try(TestSingleplayerContext world=context.worldBuilder().adjustSettings(c->c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
   world.getConnection().waitForChunksRender();var server=world.getServer();
   var origin=context.computeOnClient(c->c.player.blockPosition());int x=origin.getX(),y=origin.getY(),z=origin.getZ();
   server.runCommand("time set noon");server.runCommand("weather clear");
   server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_quartz".formatted(x-16,y-1,z-16,x+16,y-1,z+16));
   server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-16,y,z-16,x+16,y+15,z+16));
   context.runOnClient(c->{if(!c.gui.hud.isHidden())c.gui.hud.toggle();c.options.fov().set(45);});
   String[] names={"space_kook","captain_cutler","phantom_shadow","black_knight"};
   EntityType<?>[] types={ScaryMod.SPACE_KOOK,ScaryMod.CAPTAIN_CUTLER,ScaryMod.PHANTOM_SHADOW,ScaryMod.BLACK_KNIGHT};
   for(int i=0;i<4;i++) {
    final var type=types[i];final double lift=i==2?.45:0;
    server.runOnServer(s->{var m=(Mob)type.create(s.overworld(),EntitySpawnReason.COMMAND);m.setNoAi(true);m.setNoGravity(true);m.snapTo(x+.5,y+lift,z+.5,180,0);m.yBodyRot=180;m.setYHeadRot(180);s.overworld().addFreshEntity(m);});
    server.runCommand("tp @p %s %s %s -20 3".formatted(x-1.55,y+.05,z-5.1));
    context.waitTicks(30);context.takeScreenshot("model_"+names[i]);
    server.runOnServer(s->{for(var e:s.overworld().getAllEntities())if(e instanceof Mob)e.discard();});
   }
  }
 }
}
