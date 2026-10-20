package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntitySpawnReason;
/** Full-port visual smoke test in a disposable test world, separate from the owner's launcher. */
public final class NouveauExpansionClientGameTests implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var world=c.worldBuilder().create()){
  var server=world.getServer();world.getConnection().waitForChunksRender();var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),z=start.getZ(),y=111;
  server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-4,y-1,z-4,x+46,y-1,z+54));
  server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x+9,y,z+15,x+12,y+5,z+17));
  server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x+9,y,z+45,x+12,y+4,z+47));
  server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
  var stations=NouveauCatalog.BLOCK_ENTRIES.values().stream().filter(e->Set.of("station","chest","animated_decor").contains(e.kind())).toList();
  for(int page=0;page<(stations.size()+11)/12;page++){final int offset=page*12;
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int i=0;i<12&&i+offset<stations.size();i++){var e=stations.get(i+offset);var at=new BlockPos(x+(i%6)*4,y,z+(i/6)*5);p.level().setBlockAndUpdate(at,NouveauCatalog.block(e.source()).defaultBlockState());if(p.level().getBlockEntity(at) instanceof NouveauWorkshop b)b.placedBy(p);}});
   server.runCommand("tp @p %f %f %f 180 26".formatted(x+10.5,y+6.0,z+16.0));c.waitTicks(35);c.takeScreenshot("jug_nouveau_workshops_"+page);
  }
  var items=NouveauCatalog.ITEMS.values().stream().distinct().toList();
  for(int page=0;page<(items.size()+26)/27;page++){final int offset=page*27;
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();for(int i=0;i<27&&offset+i<items.size();i++)p.getInventory().setItem(9+i,new ItemStack(items.get(offset+i)));p.inventoryMenu.broadcastChanges();});
   c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));c.waitTicks(3);c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("jug_nouveau_inventory_"+page);c.runOnClient(mc->mc.gui.setScreen(null));
  }
  server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();int i=0;for(var type:NouveauHelpers.TYPES.values()){var e=type.create(p.level(),EntitySpawnReason.COMMAND);if(e!=null){e.snapTo(x+(i++%6)*4+.5,y,z+23.5,180,0);e.setNoAi(true);p.level().addFreshEntity(e);}}for(var type:NouveauEncounters.TYPES.values()){var e=type.create(p.level(),EntitySpawnReason.COMMAND);if(e!=null){e.snapTo(x+(i%6)*4+.5,y,z+23.5+(i++/6)*6,180,0);e.setNoAi(true);p.level().addFreshEntity(e);}}});
  server.runCommand("tp @p %f %f %f 180 23".formatted(x+10.5,y+5.0,z+46.0));c.waitTicks(30);c.runOnClient(mc->mc.gui.toastManager().clear());c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);c.waitTicks(2);c.takeScreenshot("jug_nouveau_creatures");c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);
  var station=new BlockPos(x,y,z);server.runCommand("tp @p %f %f %f 180 10".formatted(x+.5,y+1.0,z+2.5));c.waitTicks(5);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().setBlockAndUpdate(station,NouveauCatalog.block("imbuement_chamber").defaultBlockState());var b=(NouveauWorkshop)p.level().getBlockEntity(station);b.placedBy(p);p.snapTo(x+.5,y+1,z+2.5,180,0);p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new NouveauWorkshopMenu(id,inv,b),net.minecraft.network.chat.Component.literal("Imbuement Chamber")));});
  c.waitForScreen(NouveauWorkshopScreen.class);c.waitTicks(10);c.runOnClient(mc->{if(((NouveauWorkshopMenu)mc.player.containerMenu).recipes()==0)throw new AssertionError("Workshop recipes must reach client");});c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("jug_nouveau_recipe_picker");c.runOnClient(mc->mc.player.closeContainer());c.waitForScreen(null);
  server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().setBlockAndUpdate(station,JugNouveau.LECTERN.defaultBlockState());var home=(StorageLectern)p.level().getBlockEntity(station);home.placedBy(p);if(!home.bind(p))throw new AssertionError("Gallery lectern must bind");var at=station.east(2);p.level().setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)p.level().getBlockEntity(at);chest.setItem(0,new ItemStack(Items.DIAMOND,19));if(!home.link(p,at))throw new AssertionError("Gallery chest must link");if(new NouveauStorageTerminal.Menu(99,p.getInventory(),home).count(0)!=19)throw new AssertionError("Server catalog must contain 19 diamonds");p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new NouveauStorageTerminal.Menu(id,inv,home),net.minecraft.network.chat.Component.literal("Book Wyrm Storage")));});
  c.waitForScreen(NouveauStorageTerminalScreen.class);c.waitTicks(10);c.runOnClient(mc->{if(((NouveauStorageTerminal.Menu)mc.player.containerMenu).count(0)!=19)throw new AssertionError("Terminal catalog must synchronize 19 diamonds; received "+((NouveauStorageTerminal.Menu)mc.player.containerMenu).count(0)+", links "+((NouveauStorageTerminal.Menu)mc.player.containerMenu).connected());});c.takeScreenshot("jug_nouveau_storage_catalog");c.runOnClient(mc->mc.player.closeContainer());c.waitForScreen(null);
 }}
}
