package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.StorageCrateScreen;
import io.github.jimbozoomer.jugcraft.storage.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.List;

/** Actual client interaction, screenshots, final-slot persistence and a separate inspection-world export. */
public class StorageCrateClientGameTests implements FabricClientGameTest {
    private static final BlockPos START=new BlockPos(512,220,512), LARGE=START.east(18);
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;Path source;
        try(var world=c.worldBuilder().adjustSettings(creator->{
            creator.setName("Storage Crate Inspection");creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
            creator.setSeed("jugcraft-storage-crates");
        }).create()) {
            for(String cmd:List.of("gamerule minecraft:advance_time false","gamerule minecraft:advance_weather false","time set noon","weather clear"))world.getServer().runCommand(cmd);
            world.getServer().runOnServer(s->gallery(s.overworld()));
            world.getServer().runCommand("tp @a 521 223 502 0 10");
            world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();});
            c.waitTicks(25);world.getConnection().waitForChunksRender();c.getInput().lookAt(START.east(9));c.waitTicks(4);
            c.runOnClient(mc->{if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});c.waitTicks(3);c.takeScreenshot("jugcraft_storage_crates_gallery");
            c.runOnClient(mc->{if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.hud.getChat().clearMessages(true);});
            world.getServer().runCommand("tp @a 512.5 220 509.5 0 15");c.waitTicks(10);c.getInput().lookAt(START);c.getInput().pressKey(o->o.keyUse);c.waitForScreen(StorageCrateScreen.class);c.waitTicks(5);
            c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(true));c.waitTicks(2);c.takeScreenshot("jugcraft_storage_crate_bronze");close(c);
            world.getServer().runCommand("tp @a 530.5 220 509.5 0 15");c.waitTicks(10);c.getInput().lookAt(LARGE);c.getInput().pressKey(o->o.keyUse);c.waitForScreen(StorageCrateScreen.class);c.waitTicks(5);
            check(c.computeOnClient(mc->((StorageCrateScreen)mc.gui.screen()).getMenu().tier==StorageCrateTier.TUNGSTEN),"Correct network tier");
            c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(true));
            focusSearch(c);c.getInput().pressKey(InputConstants.KEY_E);c.waitTicks(2);c.waitForScreen(StorageCrateScreen.class);
            c.runOnClient(mc->((StorageCrateScreen)mc.gui.screen()).searchFor(""));c.getInput().typeChars("FINAL SLOT");c.waitTicks(5);
            check(c.computeOnClient(mc->((StorageCrateScreen)mc.gui.screen()).matchCount()==1),"Search finds custom name in slot 377");
            c.takeScreenshot("jugcraft_storage_crate_search");
            // Actual mouse pickup routes the filtered first tile to its stable backing slot 377.
            clickSlot(c,377);c.waitTicks(5);
            world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.containerMenu.getCarried().is(Items.DIAMOND_PICKAXE),"Filtered click takes actual tool");check(crate(s.overworld()).getItem(377).isEmpty(),"Exact backing slot removed");});
            c.runOnClient(mc->((StorageCrateScreen)mc.gui.screen()).searchFor(""));c.waitTicks(5);
            dragScrollbar(c);c.waitTicks(3);
            check(c.computeOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();return screen.scrollRow()==42-screen.visibleRows();}),"Actual scrollbar drag reaches bottom");
            c.runOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();for(int i=0;i<60;i++)screen.mouseScrolled((screen.width-202)/2.0+30,(screen.height-(152+18*screen.visibleRows()))/2.0+60,0,1);});
            check(c.computeOnClient(mc->((StorageCrateScreen)mc.gui.screen()).scrollRow()==0),"Wheel clamps at first row");
            c.runOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();for(int i=0;i<60;i++)screen.mouseScrolled((screen.width-202)/2.0+30,(screen.height-(152+18*screen.visibleRows()))/2.0+60,0,-1);});c.waitTicks(3);
            check(c.computeOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();return screen.scrollRow()==42-screen.visibleRows();}),"Scrollbar clamps at final row");
            clickSlot(c,377);c.waitTicks(5);
            world.getServer().runOnServer(s->check(crate(s.overworld()).getItem(377).is(Items.DIAMOND_PICKAXE),"Put tool back while scrolled"));
            c.takeScreenshot("jugcraft_storage_crate_last_rows");
            int scale=c.computeOnClient(mc->mc.options.guiScale().get());
            for(int size:List.of(1,4)) {
                c.runOnClient(mc->{mc.options.guiScale().set(size);mc.resizeGui();});c.waitTicks(3);
                check(c.computeOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();return 152+18*screen.visibleRows()<=screen.height
                    && screen.getMenu().slots.stream().filter(slot->slot.isActive()).count()==screen.visibleRows()*9L+36;}),"Rows and player inventory fit GUI scale "+size);
                c.takeScreenshot("jugcraft_storage_crate_scale_"+size);
            }
            c.runOnClient(mc->{mc.options.guiScale().set(scale);mc.resizeGui();});c.waitTicks(3);
            c.runOnClient(mc->((StorageCrateScreen)mc.gui.screen()).searchFor("no matching item 123"));c.waitTicks(3);
            check(c.computeOnClient(mc->((StorageCrateScreen)mc.gui.screen()).matchCount()==0),"Empty search view");
            c.takeScreenshot("jugcraft_storage_crate_no_matches");close(c);
            world.getServer().runCommand("tp @a 512.5 220 509.5 0 15");world.getServer().runCommand("setworldspawn 512 220 509");
            world.getServer().runCommand("give @a jugcraft:engineers_handbook");
            save=world.getWorldSave();source=world.getServer().computeOnServer(s->s.getWorldPath(LevelResource.ROOT));
        }
        try(var world=save.open()) {
            c.waitTicks(20);
            world.getServer().runOnServer(s->{var last=crate(s.overworld()).getItem(377);check(last.is(Items.DIAMOND_PICKAXE)&&last.getDamageValue()==37&&last.getHoverName().getString().equals("Final Slot Tool"),"Saved final slot retains complete stack");});
            c.takeScreenshot("jugcraft_storage_crates_reopened");
        }
        Path target=Path.of("../../storage-crates-preview/Storage Crate Inspection");
        try(var files=Files.walk(source)){for(var file:files.toList()) {
            Path out=target.resolve(source.relativize(file).toString());
            if(Files.isDirectory(file))Files.createDirectories(out);else if(!file.getFileName().toString().equals("session.lock"))Files.copy(file,out,StandardCopyOption.REPLACE_EXISTING);
        }}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    private static void focusSearch(ClientGameTestContext c){c.runOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();
        screen.setFocused(screen.children().stream().filter(net.minecraft.client.gui.components.EditBox.class::isInstance).findFirst().orElseThrow());});}
    private static void close(ClientGameTestContext c){focusSearch(c);c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);c.waitTicks(4);}
    private static void clickSlot(ClientGameTestContext c,int index){
        double[] xy=c.computeOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();var slot=screen.getMenu().slots.get(index);check(slot.isActive(),"Slot visible for click");return new double[]{
            ((screen.width-202)/2.0+slot.x+8)/MouseHandler.getScaledXPos(mc.getWindow(),1),
            ((screen.height-(152+screen.visibleRows()*18))/2.0+slot.y+8)/MouseHandler.getScaledYPos(mc.getWindow(),1)};});
        c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTick();c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
    private static void dragScrollbar(ClientGameTestContext c){
        double[] xy=c.computeOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();double top=(screen.height-(152+screen.visibleRows()*18))/2.0;
            return new double[]{((screen.width-202)/2.0+185)/MouseHandler.getScaledXPos(mc.getWindow(),1),
                (top+54)/MouseHandler.getScaledYPos(mc.getWindow(),1),
                (top+48+screen.visibleRows()*18-6)/MouseHandler.getScaledYPos(mc.getWindow(),1)};});
        c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTick();
        c.getInput().setCursorPos(xy[0],xy[2]);c.waitTicks(2);c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
    private static StorageCrateBlockEntity crate(ServerLevel level){return (StorageCrateBlockEntity)level.getBlockEntity(LARGE);}
    private static void gallery(ServerLevel level){
        for(int x=-4;x<25;x++)for(int z=-7;z<7;z++){
            level.setBlock(START.offset(x,-1,z),(z%3==0?Blocks.STONE_BRICKS:Blocks.SMOOTH_STONE).defaultBlockState(),3);
            for(int y=0;y<6;y++)level.setBlock(START.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        for(var tier:StorageCrateTier.values()){
            BlockPos pos=START.east(tier.ordinal()*3);level.setBlock(pos,JugcraftStorage.STORAGE_CRATES.get(tier).defaultBlockState(),3);
            var crate=(StorageCrateBlockEntity)level.getBlockEntity(pos);
            for(int i=0;i<crate.getContainerSize();i++)crate.setItem(i,new ItemStack(i%3==0?Items.IRON_INGOT:i%3==1?Items.OAK_PLANKS:Items.COBBLESTONE,32));
        }
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Final Slot Tool"));tool.setDamageValue(37);crate(level).setItem(377,tool);
        // A solid block above Bronze proves its front access does not need lid clearance.
        level.setBlock(START.above(),Blocks.STONE_BRICKS.defaultBlockState(),3);
    }
}
