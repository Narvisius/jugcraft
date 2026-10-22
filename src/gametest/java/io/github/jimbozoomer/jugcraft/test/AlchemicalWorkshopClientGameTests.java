package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.AlchemicalWorkshopScreen;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;

/** Disposable test instance. Its independent saved copy is left for the owner's manual playtest. */
public class AlchemicalWorkshopClientGameTests implements FabricClientGameTest {
    private static void welcomePack(java.nio.file.Path save, BlockPos base) {
        var root=save.resolve("datapacks/jugcraft_workshop_welcome");
        var functions=root.resolve("data/jugcraft_playtest/function");
        var tags=root.resolve("data/minecraft/tags/function");
        var positions=java.util.List.of(base.offset(-8,0,-4),base.offset(-4,0,-4),base.offset(0,0,-4),base.offset(4,0,-4),base.offset(8,0,-4),base.offset(0,0,3),base.offset(1,0,3));
        StringBuilder tick=new StringBuilder("execute unless data storage jugcraft_playtest:setup bound as @a[limit=1] at @s");
        StringBuilder bind=new StringBuilder();
        for(var pos:positions){
            String at=pos.getX()+" "+pos.getY()+" "+pos.getZ();
            tick.append(" if loaded ").append(at);
            bind.append("data modify block ").append(at).append(" owner set from entity @s UUID\n");
        }
        tick.append(" run function jugcraft_playtest:bind\n");
        bind.append("data modify storage jugcraft_playtest:setup bound set value 1b\n");
        try {
            java.nio.file.Files.createDirectories(functions);java.nio.file.Files.createDirectories(tags);
            java.nio.file.Files.writeString(root.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Local showroom: assign its seven workshops to the first player once\",\"min_format\":[121,0],\"max_format\":[121,0]}}");
            java.nio.file.Files.writeString(tags.resolve("tick.json"),"{\"values\":[\"jugcraft_playtest:tick\"]}");
            java.nio.file.Files.writeString(functions.resolve("tick.mcfunction"),tick);
            java.nio.file.Files.writeString(functions.resolve("bind.mcfunction"),bind);
        } catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static AlchemicalWorkshopBlockEntity place(ServerPlayer p,BlockPos at,String id) {
        p.level().setBlockAndUpdate(at,AlchemicalWorkshops.BLOCKS.get(id).defaultBlockState().setValue(AlchemicalWorkshopBlock.FACING,Direction.SOUTH));
        var be=(AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(at);be.placedBy(p);return be;
    }
    private static AlchemicalWorkshopBlockEntity be(ServerPlayer p,BlockPos at) {return (AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(at);}
    private static void sign(ServerPlayer p,BlockPos at,String... words) {
        p.level().setBlockAndUpdate(at,Blocks.DARK_OAK_SIGN.defaultBlockState());var sign=(SignBlockEntity)p.level().getBlockEntity(at);
        var text=Arrays.stream(words).map(Component::literal).map(v->(Component)v).toList();
        for(var side:SignTextSlot.values())sign.setText(new SignText(text,text,DyeColor.WHITE,false),side);
        sign.setWaxed(true);sign.setChanged();p.level().sendBlockUpdated(at,sign.getBlockState(),sign.getBlockState(),3);
    }
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            world.getConnection().waitForChunksRender();var server=world.getServer();var start=c.computeOnClient(mc->mc.player.blockPosition());
            int x=start.getX(),y=101,z=start.getZ();var base=new BlockPos(x,y,z);
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-14,y-3,z-11,x+14,y+7,z+9));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-14,y-3,z-11,x+14,y-1,z+9));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:advance_time false");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("setworldspawn %d %d %d".formatted(x,y,z+5));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);
                ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);
                p.getInventory().clearContent();Assaying.setBalance(p,2000000);
                for(int i=1;i<=3;i++){
                    var col=place(p,base.offset(-12+4*i,0,-4),"collector_mk"+i);
                    col.setItem(0,new ItemStack(KleinStars.ITEMS.get("klein_star_"+(i+1))));
                    sign(p,base.offset(-12+4*i,0,-2),"COLLECTOR MK "+i,"Open to inspect","Daylight -> essence","Star charges here");
                }
                for(int i=1;i<=2;i++){
                    var con=place(p,base.offset(4*i,0,-4),"condenser_mk"+i);con.setItem(1,new ItemStack(Items.OAK_LOG));con.setItem(2,new ItemStack(Items.COAL,64));con.setEssence(8000);
                    sign(p,base.offset(4*i,0,-2),"CONDENSER MK "+i,"Target is kept","Inputs dissolve","Output to factory");
                }
                // Independent automation demonstration with top input and bottom output hoppers.
                var auto=place(p,base.offset(1,0,3),"condenser_mk1");auto.setItem(1,new ItemStack(Items.OAK_LOG));auto.setEssence(4000);
                place(p,base.offset(0,0,3),"collector_mk3");
                p.level().setBlockAndUpdate(base.offset(1,1,3),Blocks.HOPPER.defaultBlockState());
                ((HopperBlockEntity)p.level().getBlockEntity(base.offset(1,1,3))).setItem(0,new ItemStack(Items.COAL,64));
                p.level().setBlockAndUpdate(base.offset(1,-1,3),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.SOUTH));
                p.level().setBlockAndUpdate(base.offset(1,-1,4),Blocks.CHEST.defaultBlockState());
                sign(p,base.offset(-2,0,3),"CONNECTED LINE","Coal from above","Iron chest below","Collector touches");
                p.level().setBlockAndUpdate(base.offset(-5,0,5),Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)p.level().getBlockEntity(base.offset(-5,0,5));
                int slot=0;for(var b:AlchemicalWorkshops.BLOCKS.values())chest.setItem(slot++,new ItemStack(b));
                for(int i=1;i<=6;i++)chest.setItem(slot++,new ItemStack(KleinStars.ITEMS.get("klein_star_"+i)));
                for(Item item:new Item[]{Items.OAK_LOG,Items.COAL,Items.DIAMOND,Items.GLASS,Items.GLOWSTONE,Items.OBSIDIAN,Items.FURNACE,Items.CHEST,Items.HOPPER,Items.LEVER,DarkMatter.ITEMS.get("dark_matter"),DarkMatter.ITEMS.get("high_covalence_dust")})chest.setItem(slot++,new ItemStack(item,32));
                p.level().setBlockAndUpdate(base.offset(-8,0,5),Blocks.CRAFTING_TABLE.defaultBlockState());p.level().setBlockAndUpdate(base.offset(-7,0,5),Blocks.SMITHING_TABLE.defaultBlockState());
                sign(p,base.offset(-5,0,6),"SUPPLIES","All five workshops","Stars / ingredients","Your world to test");
            });
            server.runCommand("tp @p %.1f %d %.1f 180 29".formatted(x+4.5,y,z-1.5));c.waitTicks(80);
            // The sign is one block in front; aim from the side directly at the condenser instead.
            server.runCommand("tp @p %.1f %d %.1f 90 29".formatted(x+6.5,y,z-3.5));c.waitTicks(30);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemicalWorkshopScreen.class);c.waitTicks(8);
            c.clickScreenButton("screen.jugcraft.workshop.pause");c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!be(p,base.offset(4,0,-4)).paused())throw new AssertionError("Real pause button");});
            long conserved=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return Assaying.balance(p)+be(p,base.offset(4,0,-4)).essence();});
            c.clickScreenButton("screen.jugcraft.workshop.deposit");c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var condenser=be(p,base.offset(4,0,-4));
                if(condenser.essence()!=condenser.capacity())throw new AssertionError("Deposit fills actual capacity");
                if(Assaying.balance(p)+condenser.essence()!=conserved)throw new AssertionError("Deposit must debit exactly the essence moved into the condenser");});
            c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("essence_condenser_interface");
            c.clickScreenButton("screen.jugcraft.workshop.withdraw");c.waitTicks(5);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(be(p,base.offset(4,0,-4)).essence()!=0)throw new AssertionError("Take empties buffer");
                if(Assaying.balance(p)!=conserved)throw new AssertionError("Withdrawal must return all deposited essence exactly once");});
            c.clickScreenButton("screen.jugcraft.workshop.deposit");c.waitTicks(5);c.clickScreenButton("screen.jugcraft.workshop.resume");c.waitTicks(25);
            int scale=c.computeOnClient(mc->mc.options.guiScale().get());
            c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("condenser_scale1");
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("condenser_scale2");
            c.getInput().pressKey(InputConstants.KEY_TAB);c.waitTicks(2);
            c.runOnClient(mc->{if(mc.gui.screen().getFocused()==null)throw new AssertionError("Keyboard navigation");mc.options.guiScale().set(scale);mc.resizeGui();});
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);
            server.runCommand("tp @p %.1f %d %.1f 90 29".formatted(x-5.5,y,z-3.5));c.waitTicks(30);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemicalWorkshopScreen.class);c.waitTicks(6);c.takeScreenshot("essence_collector_interface");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var col=be(p,base.offset(-8,0,-4));if(col.sunlight()<12||KleinStars.stored(col.getItem(0))<=0)throw new AssertionError("Real daylight charges star");});
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);
            server.runCommand("time set midnight");c.waitTicks(40);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(be(p,base.offset(-8,0,-4)).status()!=AlchemicalWorkshopBlockEntity.NO_SUN)throw new AssertionError("Night stops generation");});
            server.runCommand("time set noon");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().setBlockAndUpdate(base.offset(-8,1,-4),Blocks.STONE.defaultBlockState());});c.waitTicks(45);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(be(p,base.offset(-8,0,-4)).status()!=AlchemicalWorkshopBlockEntity.NO_SUN)throw new AssertionError("Opaque roof stops generation");p.level().setBlockAndUpdate(base.offset(-8,1,-4),Blocks.AIR.defaultBlockState());
                var chest=(ChestBlockEntity)p.level().getBlockEntity(base.offset(1,-1,4));if(chest.countItem(Items.OAK_LOG)<1)throw new AssertionError("Live output hopper supplies chest");
                var auto=be(p,base.offset(1,0,3));var in=(HopperBlockEntity)p.level().getBlockEntity(base.offset(1,1,3));if(in.getItem(0).getCount()>=64)throw new AssertionError("Live input hopper feeds materials");
                // Fabric transfer transaction also observes the exposed slots and rolls back without changing items.
                var storage=ItemStorage.SIDED.find(p.level(),auto.getBlockPos(),Direction.UP);int before=auto.countItem(Items.COAL);
                try(var t=Transaction.openOuter()){if(storage.insert(ItemVariant.of(Items.COAL),1,t)!=1)throw new AssertionError("Sided item transfer accepts ordinary feed");}
                if(auto.countItem(Items.COAL)!=before)throw new AssertionError("Aborted item transfer rolls back");
                auto.setPaused(true);try(var t=Transaction.openOuter()){if(storage.insert(ItemVariant.of(Items.COAL),1,t)!=0)throw new AssertionError("Paused automation refuses input");}auto.setPaused(false);
                for(int i=0;i<5;i++)p.getInventory().setItem(i,new ItemStack(AlchemicalWorkshops.BLOCKS.values().stream().toList().get(i)));
                p.getInventory().setItem(5,new ItemStack(AlchemicalGear.STONE));p.getInventory().setItem(6,new ItemStack(KleinStars.ITEMS.get("klein_star_3")));p.getInventory().setItem(7,new ItemStack(Items.COOKED_BEEF,64));
                Assaying.setBalance(p,200000);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
            });
            server.runCommand("tp @p %.1f %d %.1f 180 14".formatted(x+.5,y+2,z+5.5));c.waitTicks(80);world.getConnection().waitForChunksRender();c.takeScreenshot("collector_condenser_workshop");
            welcomePack(world.getWorldSave().getSaveDirectory(),base);
            server.runCommand("data remove block %d %d %d owner".formatted(x-8,y,z-4));
            server.runCommand("reload");
            server.waitFor(s->{var p=s.getPlayerList().getPlayers().getFirst();return p.getUUID().equals(be(p,base.offset(-8,0,-4)).owner());},600);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!be(p,base.offset(-8,0,-4)).mayUse(p))throw new AssertionError("The playtest world's first-join pack binds the real account");});
            server.runCommand("tp @p %.1f %d %.1f 180 0".formatted(x+.5,y,z+.5));c.waitTicks(10);
        }
    }
}
