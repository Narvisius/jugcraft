package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.raiders.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Focused runtime rendering evidence for the November food, decor and seated raider crews. */
public final class HolidayPolishClientTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c) {
        try (var world = c.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server = world.getServer();
            world.getConnection().waitForChunksRender();
            BlockPos origin = c.computeOnClient(mc -> mc.player.blockPosition());
            c.getInput().resizeWindow(1280,720);
            c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runOnServer(s -> {
                var l = s.overworld();
                for (int x=-8;x<=22;x++) for(int z=-8;z<=18;z++) {
                    l.setBlock(origin.offset(x,-1,z), Blocks.SPRUCE_PLANKS.defaultBlockState(),3);
                    for(int y=0;y<12;y++) l.removeBlock(origin.offset(x,y,z),false);
                }
                String[] foods={"cornbread_stuffing","vegetable_noodles","squid_ink_pasta","bacon_and_eggs","steak_and_potatoes","roasted_mutton_chops","pasta_with_meatballs","tomato_sauce"};
                for(int i=0;i<foods.length;i++) {
                    var p=origin.offset((i%4)*2,0,(i/4)*2);
                    var block=JugcraftAgriculture.block(foods[i]);
                    if(!(block instanceof PlacedDishBlock)) throw new AssertionError("No placed dish: "+foods[i]);
                    l.setBlock(p,block.defaultBlockState(),3);
                }
                String[] props={"pumpkin_and_gourd_stack","wicker_apple_basket","pear_basket","root_vegetable_hamper","harvest_place_setting","napkin_and_cutlery_caddy","bread_basket","gravy_boat","covered_butter_dish","pie_stand","pumpkin_candle_centerpiece","turkey_statuette","squirrel_acorn_figurine","table_cornucopia","amber_jar_lantern"};
                for(int i=0;i<props.length;i++) l.setBlock(origin.offset((i%5)*2,0,7+(i/5)*2),JugcraftAgriculture.block(props[i]).defaultBlockState(),3);
                String[] wall={"autumn_leaf_wall_garland","harvest_market_sign","copper_pan_rack","acorn_leaf_mobile"};
                for(int i=0;i<wall.length;i++) {
                    var p=origin.offset(i*2,2,14);
                    l.setBlock(p.south(),Blocks.BRICKS.defaultBlockState(),3);
                    l.setBlock(p,JugcraftAgriculture.block(wall[i]).defaultBlockState(),3);
                }
                var table=origin.offset(10,0,9);
                l.setBlock(table,JugcraftAgriculture.block("feast_table").defaultBlockState().setValue(FeastTableBlock.RUNNER,FeastTableBlock.Runner.LEAVES),3);
                for(int i=0;i<2;i++) {
                    var mob=i==0?JugcraftRaiders.WALKER.create(l,EntitySpawnReason.COMMAND):JugcraftRaiders.BLIMP.create(l,EntitySpawnReason.COMMAND);
                    var crew=JugcraftRaiders.GRUNT.create(l,EntitySpawnReason.COMMAND);
                    if(mob==null||crew==null) throw new AssertionError("Missing raider entity");
                    mob.snapTo(origin.getX()+16,origin.getY()+(i==0?0:4),origin.getZ()+i*8,180,0);
                    mob.setNoAi(true); mob.setNoGravity(true); l.addFreshEntity(mob);
                    crew.snapTo(mob.position());crew.setNoAi(true);l.addFreshEntity(crew);RaiderCrew.board(mob,crew);
                    if(crew.getVehicle()!=mob) throw new AssertionError("Crew did not board");
                }
            });
            int[][] views={{3,3,-3,0,45},{4,4,4,0,35},{4,3,11,0,0},{12,5,-5,-38,15},{12,4,4,-40,0}};
            String[] names={"holiday_eight_meals","holiday_table_decor","holiday_wall_decor","holiday_walker_crew","holiday_blimp_crew"};
            for(int i=0;i<views.length;i++) {
                var v=views[i];var p=origin.offset(v[0],v[1],v[2]);
                server.runCommand("setblock %d %d %d minecraft:barrier".formatted(p.getX(),p.getY()-1,p.getZ()));
                server.runCommand("tp @p %s %s %s %d %d".formatted(p.getX()+.5,p.getY(),p.getZ()+.5,v[3],v[4]));
                c.waitTicks(30);world.getConnection().waitForChunksRender();c.takeScreenshot(names[i]);
            }
            c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);
        }
    }
}
