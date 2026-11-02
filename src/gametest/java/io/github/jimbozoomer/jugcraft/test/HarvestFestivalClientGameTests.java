package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.ItemStack;
public final class HarvestFestivalClientGameTests implements FabricClientGameTest {
 public void runTest(net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext context){
  try(var single=context.worldBuilder().adjustSettings(c->c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
   single.getConnection().waitForChunksRender();var server=single.getServer();var origin=context.computeOnClient(c->BlockPos.containing(c.player.position()));
   server.runCommand("gamerule minecraft:send_command_feedback false");server.runCommand("time set noon");server.runCommand("weather clear");
   context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);
   server.runOnServer(s->{var l=s.overworld();for(int x=-5;x<18;x++)for(int z=-6;z<12;z++){l.setBlock(origin.offset(x,0,z),Blocks.SPRUCE_PLANKS.defaultBlockState(),3);for(int y=1;y<6;y++)l.removeBlock(origin.offset(x,y,z),false);}
    int index=0;for(var food:HarvestFestival.FOODS.values()){var p=origin.offset(index++*2,1,0);l.setBlock(p,FoodServings.entries().get(food).defaultBlockState(),3);((ShowcaseBlockEntity)l.getBlockEntity(p)).put(0,new ItemStack(food));}
   });
   server.runCommand("setblock %d %d %d minecraft:barrier".formatted(origin.getX()+3,origin.getY()+3,origin.getZ()-4));
   server.runCommand("tp @p %s %s %s 0 40".formatted(origin.getX()+3.5,origin.getY()+4,origin.getZ()-3.5));context.waitTicks(20);single.getConnection().waitForChunksRender();context.takeScreenshot("harvest_four_dishes");
   server.runOnServer(s->{var l=s.overworld();var b=HarvestFestival.GIANT_TURKEY;int row=0;for(int left:new int[]{24,12,0}){var p=origin.offset(row++*5+2,1,5);for(int i=0;i<18;i++)l.setBlock(b.partPos(p,Direction.NORTH,i),b.defaultBlockState().setValue(GiantTurkeyFeastBlock.PART,i).setValue(GiantTurkeyFeastBlock.REMAINING,left),3);}});
   server.runCommand("setblock %d %d %d minecraft:barrier".formatted(origin.getX()+5,origin.getY()+5,origin.getZ()-3));server.runCommand("tp @p %s %s %s 0 28".formatted(origin.getX()+5.5,origin.getY()+6,origin.getZ()-2.5));context.waitTicks(20);single.getConnection().waitForChunksRender();context.takeScreenshot("harvest_turkey_stages");
   for(int i=0;i<2;i++)server.runCommand("summon minecraft:armor_stand %s %s %s {NoGravity:1b,ShowArms:1b,Rotation:[180f,0f],equipment:{head:{id:\"jugcraft:%s\",count:1}}}".formatted(origin.getX()+i*3,origin.getY()+1,origin.getZ()+1,i==0?"pilgrim_coat_costume":"pilgrim_dress_costume"));
   server.runCommand("summon jugcraft:townsfolk %s %s %s {NoAI:1b,Rotation:[180f,0f]}".formatted(origin.getX()+6,origin.getY()+1,origin.getZ()+1));
   server.runCommand("summon jugcraft:raider_grunt %s %s %s {NoAI:1b,Rotation:[180f,0f]}".formatted(origin.getX()+9,origin.getY()+1,origin.getZ()+1));
   server.runCommand("tp @p %s %s %s 0 8".formatted(origin.getX()+4.5,origin.getY()+1,origin.getZ()-5));
   context.runOnClient(c->HarvestFestival.setClientCostumes(true));context.waitTicks(20);context.takeScreenshot("harvest_costumes_active");
   server.runOnServer(s->{var area=new net.minecraft.world.phys.AABB(origin).inflate(24);s.overworld().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,area).forEach(e->e.discard());s.overworld().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.town.Townsfolk.class,area).forEach(e->e.discard());});
   server.runCommand("summon jugcraft:raider_grenadier %s %s %s {NoAI:1b,Rotation:[180f,0f]}".formatted(origin.getX()+3,origin.getY()+1,origin.getZ()+1));
   server.runCommand("summon jugcraft:raider_officer %s %s %s {NoAI:1b,Rotation:[180f,0f]}".formatted(origin.getX()+6,origin.getY()+1,origin.getZ()+1));
   context.waitTicks(15);context.takeScreenshot("harvest_raider_variants");
   context.runOnClient(c->HarvestFestival.setClientCostumes(false));context.waitTicks(5);context.takeScreenshot("harvest_costumes_reverted");
  }
 }
}
