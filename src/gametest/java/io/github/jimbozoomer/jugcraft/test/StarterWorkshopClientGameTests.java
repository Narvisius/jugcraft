package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.WorkshopScreen;
import io.github.jimbozoomer.jugcraft.workshop.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/** Real click-toggle input, menu-close pause, network-synced screen, save/reopen and a separate inspection export. */
public class StarterWorkshopClientGameTests implements FabricClientGameTest {
    private static final BlockPos BENCH = new BlockPos(512,240,512), KILN=BENCH.east(15), FURNACE=BENCH.east(19);
    private static final Path EXPORT=Path.of("../../starter-workshop-toggle-preview/Starter Workshop Inspection");
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save; Path source; int[] saved;
        try(var world=c.worldBuilder().adjustSettings(creator->{
            creator.setName("Starter Workshop Inspection"); creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
            creator.setSeed("jugcraft-starter-workshop-1");
        }).create()) {
            for(String command:List.of("gamerule minecraft:advance_time false","gamerule minecraft:advance_weather false","gamerule minecraft:send_command_feedback false","time set noon","weather clear")) world.getServer().runCommand(command);
            world.getServer().runOnServer(s->gallery(s.overworld()));
            world.getServer().runCommand("tp @a 512.5 240 509.5 0 15"); c.waitTicks(30); world.getConnection().waitForChunksRender();
            // Aim at the tabletop; the centre of this open-legged bench is deliberately empty.
            c.runOnClient(client->client.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                net.minecraft.world.phys.Vec3.atCenterOf(BENCH).add(0,.4,0)));
            c.waitTicks(2); c.getInput().pressKey(o->o.keyUse); c.waitForScreen(WorkshopScreen.class); c.waitTicks(8);
            clickWork(c); c.waitTicks(25); c.takeScreenshot("jugcraft_starter_workshop_work_enabled"); clickWork(c); c.waitTicks(5);
            int paused=world.getServer().computeOnServer(s->be(s.overworld(),BENCH).progress());
            check(paused>0 && paused<100,"A released click must keep making partial progress until toggled off, got "+paused);
            c.waitTicks(30); check(world.getServer().computeOnServer(s->be(s.overworld(),BENCH).progress())==paused,"Second click did not pause");
            c.takeScreenshot("jugcraft_starter_workshop_bench_paused");
            clickWork(c); c.waitTicks(110-paused); clickWork(c); c.waitTicks(5);
            world.getServer().runOnServer(s->check(be(s.overworld(),BENCH).getItem(5).getCount()==1,"Real manual plate must finish exactly once"));
            c.takeScreenshot("jugcraft_starter_workshop_manual_plate_complete"); close(c);
            // Leave a partially worked plate for the owner to resume in the delivered world.
            world.getServer().runOnServer(s->{ var b=be(s.overworld(),BENCH);b.setItem(0,new ItemStack(Items.IRON_INGOT));b.setItem(5,ItemStack.EMPTY); });
            c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(5);clickWork(c);c.waitTicks(25);close(c);
            int left=world.getServer().computeOnServer(s->be(s.overworld(),BENCH).progress());
            c.waitTicks(20);check(world.getServer().computeOnServer(s->be(s.overworld(),BENCH).progress())==left,"Leaving the bench must stop enabled work");
            c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(20);
            check(world.getServer().computeOnServer(s->be(s.overworld(),BENCH).progress())==left,"Reopening must start with work disabled");close(c);
            world.getServer().runCommand("tp @a 527.5 240 509.5 0 15"); c.waitTicks(10);c.getInput().lookAt(KILN);c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(5);c.takeScreenshot("jugcraft_starter_workshop_kiln");close(c);
            world.getServer().runCommand("tp @a 531.5 240 509.5 0 15");c.waitTicks(10);c.getInput().lookAt(FURNACE.above());c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(5);c.takeScreenshot("jugcraft_starter_workshop_crucible_furnace");close(c);
            world.getServer().runCommand("tp @a 518 243 504 0 20");
            world.getServer().runOnServer(s->{ var p=s.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities(); });
            c.waitTicks(25);c.getInput().lookAt(BENCH.east(10));c.waitTicks(8);
            c.getInput().pressKey(InputConstants.KEY_F1);c.waitTicks(2);c.takeScreenshot("jugcraft_starter_workshop_gallery");
            c.getInput().pressKey(InputConstants.KEY_F1);c.waitTicks(2);
            world.getServer().runCommand("tp @a 512.5 240 508.5 0 15");
            world.getServer().runCommand("setworldspawn 512 240 508");world.getServer().runCommand("gamerule minecraft:respawn_radius 0");
            for(String item:List.of("jugcraft:engineers_handbook","jugcraft:workshop_hammer","jugcraft:wire_cutters","jugcraft:soldering_tool","minecraft:coal 64","minecraft:clay_ball 64","jugcraft:small_crucible_furnace","jugcraft:workshop_bench","jugcraft:small_ceramic_kiln")) world.getServer().runCommand("give @a "+item);
            saved=world.getServer().computeOnServer(s->new int[]{be(s.overworld(),BENCH).progress(),be(s.overworld(),KILN).fuelCredit(),be(s.overworld(),FURNACE).fuelCredit()});
            source=world.getServer().computeOnServer(s->s.getWorldPath(LevelResource.ROOT));save=world.getWorldSave();
        }
        try(var world=save.open()) {
            c.waitTicks(30);world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(s->{
                check(be(s.overworld(),BENCH).progress()==saved[0],"Saved manual work advanced offline");
                check(be(s.overworld(),KILN).fuelCredit()==saved[1],"Idle kiln lost stored heat");
                check(be(s.overworld(),FURNACE).fuelCredit()==saved[2],"Idle furnace lost stored heat");
                check(be(s.overworld(),FURNACE).getItem(5).getCount()==4,"Alloy lost across reopen");
            });
            c.getInput().lookAt(BENCH.east(9));c.takeScreenshot("jugcraft_starter_workshop_reopened");
        }
        copy(source,EXPORT);
    }
    private static void clickWork(ClientGameTestContext c) {
        double[] xy=c.computeOnClient(client->{var screen=(WorkshopScreen)client.gui.screen();return new double[]{
            ((screen.width-300)/2.0+50)/MouseHandler.getScaledXPos(client.getWindow(),1),
            ((screen.height-236)/2.0+197)/MouseHandler.getScaledYPos(client.getWindow(),1)};});
        c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTick();c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
    private static void close(ClientGameTestContext c) {c.runOnClient(client->client.player.closeContainer());c.waitTicks(5);}
    private static WorkshopBlockEntity be(ServerLevel l,BlockPos p){return (WorkshopBlockEntity)l.getBlockEntity(p);}
    private static WorkshopBlockEntity place(ServerLevel l,BlockPos p,int station,String recipe){
        var b=JugcraftWorkshop.BLOCKS[station];var state=b.defaultBlockState();l.setBlock(p,state,3);b.setPlacedBy(l,p,state,null,ItemStack.EMPTY);
        var be=be(l,p);check(be.select(Jugcraft.id("starter_workshop/"+recipe)),"Missing preview recipe");return be;
    }
    private static void gallery(ServerLevel l) {
        for(int x=-5;x<24;x++) for(int z=-7;z<8;z++) {
            l.setBlock(BENCH.offset(x,-1,z),(z%4==0?Blocks.STONE_BRICKS:Blocks.OAK_PLANKS).defaultBlockState(),3);
            for(int y=0;y<6;y++) l.setBlock(BENCH.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        var hammer=place(l,BENCH,0,"iron_plate");hammer.setItem(0,new ItemStack(Items.IRON_INGOT));hammer.setItem(3,StarterWorkshopGameTests.stack("jugcraft:workshop_hammer",1));
        var wire=place(l,BENCH.east(5),0,"copper_wire");wire.setItem(0,new ItemStack(Items.COPPER_INGOT,16));wire.setItem(3,StarterWorkshopGameTests.stack("jugcraft:wire_cutters",1));
        var circuit=place(l,BENCH.east(10),0,"basic_circuit");circuit.setItem(0,StarterWorkshopGameTests.stack("jugcraft:silicon",8));circuit.setItem(1,StarterWorkshopGameTests.stack("jugcraft:copper_wire",24));circuit.setItem(2,StarterWorkshopGameTests.stack("jugcraft:solder_ingot",8));circuit.setItem(3,StarterWorkshopGameTests.stack("jugcraft:soldering_tool",1));circuit.setItem(4,new ItemStack(Items.COAL,8));
        var kiln=place(l,KILN,1,"fire_insulators");kiln.setItem(0,StarterWorkshopGameTests.stack("jugcraft:unfired_insulator",2));kiln.setItem(4,new ItemStack(Items.COAL));
        var furnace=place(l,FURNACE,2,"bronze_ingot");furnace.setItem(0,new ItemStack(Items.COPPER_INGOT,3));furnace.setItem(1,StarterWorkshopGameTests.stack("jugcraft:tin_ingot",1));furnace.setItem(4,new ItemStack(Items.COAL));furnace.setItem(6,StarterWorkshopGameTests.stack("jugcraft:basic_ceramic_lining",1));furnace.setItem(7,StarterWorkshopGameTests.stack("jugcraft:ceramic_crucible",1));
        // Prepare completed products/leftover heat through normal server processing, not forged state.
        for(int i=0;i<800;i++){kiln.tick(l);furnace.tick(l);}
        String[][] signs={{"1. HAMMER","Iron plate","Click Start Work","5 seconds"},{"2. CUTTERS","3 copper wire","Click Start Work","5 seconds"},{"3. SOLDER","Basic circuit","Start + fuel","10 seconds"},{"4. CERAMICS","Shape clay first","Select + fuel","10 / 20 / 30 s"},{"5. ALLOYS","Install ceramics","Metals + fuel","10 / 6 seconds"}};
        int[] offsets={0,5,10,15,19};for(int i=0;i<5;i++)sign(l,BENCH.east(offsets[i]).south(2),signs[i]);
        BlockPos chest=BENCH.offset(7,0,-4);l.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);
        var supplies=(ChestBlockEntity)l.getBlockEntity(chest);int slot=0;
        for(String id:List.of("minecraft:clay_ball","minecraft:iron_ingot","minecraft:copper_ingot","jugcraft:tin_ingot","jugcraft:zinc_ingot","jugcraft:lead_ingot","minecraft:coal","jugcraft:unfired_ceramic_lining","jugcraft:unfired_ceramic_crucible","jugcraft:unfired_insulator","jugcraft:basic_ceramic_lining","jugcraft:ceramic_crucible","jugcraft:silicon","jugcraft:copper_wire","jugcraft:solder_ingot"))supplies.setItem(slot++,StarterWorkshopGameTests.stack(id,32));
        l.setBlock(chest.east(),Blocks.CRAFTING_TABLE.defaultBlockState(),3);l.setBlock(chest.east(2),Blocks.FURNACE.defaultBlockState(),3);
        sign(l,chest.west(),"STARTER WORKSHOP","Supplies here","B: Handbook","Batch 1 only");
    }
    private static void sign(ServerLevel l,BlockPos p,String... lines){
        l.setBlock(p,Blocks.OAK_SIGN.defaultBlockState(),3);if(l.getBlockEntity(p) instanceof SignBlockEntity sign){
            var text=Arrays.stream(lines).map(Component::literal).map(v->(Component)v).toList();sign.setText(new SignText(text,text,DyeColor.BLACK,false),SignTextSlot.FRONT);sign.setChanged();}
    }
    private static void copy(Path source,Path target){try(var files=Files.walk(source)){for(var f:files.toList()){
        var to=target.resolve(source.relativize(f).toString());if(Files.isDirectory(f))Files.createDirectories(to);else if(!f.getFileName().toString().equals("session.lock"))Files.copy(f,to,StandardCopyOption.REPLACE_EXISTING);
    }}catch(IOException e){throw new RuntimeException(e);}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
