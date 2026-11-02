package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.ItemStack;
public final class FoodServingClientGameTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context) {
        try(var single=context.worldBuilder().adjustSettings(c->c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            single.getConnection().waitForChunksRender();var server=single.getServer();
            var origin=context.computeOnClient(c->BlockPos.containing(c.player.position()));
            server.runCommand("gamerule minecraft:send_command_feedback false");
            context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);
            server.runCommand("time set noon");server.runCommand("weather clear");
            var rows=new java.util.ArrayList<>(FoodServings.entries().entrySet());
            for(int group=0;group<(rows.size()+11)/12;group++) {
                final int start=group*12;
                server.runOnServer(s->{var l=s.overworld();
                    for(int i=0;i<12;i++) {
                        var p=origin.offset((i%4)*2,0,(i/4)*2);
                        l.setBlock(p,Blocks.SPRUCE_PLANKS.defaultBlockState(),Block.UPDATE_ALL);
                        l.removeBlock(p.above(),false);
                        if(start+i<rows.size()) {
                            var e=rows.get(start+i);l.setBlock(p.above(),e.getValue().defaultBlockState(),Block.UPDATE_ALL);
                            ((ShowcaseBlockEntity)l.getBlockEntity(p.above())).put(0,new ItemStack(e.getKey()));
                        }
                    }
                    l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(24)).forEach(e->e.discard());
                });
                server.runCommand("setblock %d %d %d minecraft:barrier".formatted(origin.getX()+3,origin.getY()+3,origin.getZ()-4));
                server.runCommand("tp @p %s %s %s 0 35".formatted(origin.getX()+3.5,origin.getY()+4,origin.getZ()-3.5));
                context.waitTicks(15);single.getConnection().waitForChunksRender();context.takeScreenshot("food_servings_"+group);
            }
            server.runOnServer(s->{var l=s.overworld();
                for(int i=0;i<4;i++) {
                    var at=origin.offset(i*2,1,0);
                    l.removeBlock(at,false);
                    l.setBlock(at,JugcraftAgriculture.block("feast_table").defaultBlockState(),Block.UPDATE_ALL);
                    var table=(FeastTableBlockEntity)l.getBlockEntity(at);
                    table.serve(0,new ItemStack(rows.get(i*10).getKey(),4));
                    table.serve(1,new ItemStack(rows.get(i*10+1).getKey(),4));
                }
                l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(24)).forEach(e->e.discard());
            });
            context.waitTicks(15);single.getConnection().waitForChunksRender();context.takeScreenshot("food_servings_feast_tables");

        }
    }
}
