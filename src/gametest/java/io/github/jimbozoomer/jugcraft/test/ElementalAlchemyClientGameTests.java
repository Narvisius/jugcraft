package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.KleinStarScreen;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import java.util.Arrays;

/** Disposable automated world; the owner receives a separate saved copy to play freely. */
public class ElementalAlchemyClientGameTests implements FabricClientGameTest {
    private static void kit(ServerPlayer p){
        p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);
        for(int i=0;i<6;i++)p.getInventory().setItem(i,new ItemStack(KleinStars.ITEMS.get("klein_star_"+(i+1))));
        p.getInventory().setItem(6,new ItemStack(ElementalAmulets.EVERTIDE));p.getInventory().setItem(7,new ItemStack(ElementalAmulets.VOLCANITE));
        p.getInventory().setItem(8,new ItemStack(AlchemicalGear.STONE));
        Item[] supplies={Items.BUCKET,Items.GOLD_INGOT,Items.LAPIS_LAZULI,DarkMatter.ITEMS.get("dark_matter"),DarkMatter.ITEMS.get("high_covalence_dust"),Items.COOKED_BEEF};
        for(int i=0;i<supplies.length;i++)p.getInventory().setItem(9+i,new ItemStack(supplies[i],i==0?16:32));
        Assaying.setBalance(p,100000);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
    }
    private static void sign(ServerPlayer p,BlockPos at,String... words){
        var l=p.level();l.setBlockAndUpdate(at,Blocks.DARK_OAK_SIGN.defaultBlockState());var sign=(SignBlockEntity)l.getBlockEntity(at);
        var lines=Arrays.stream(words).map(Component::literal).map(c->(Component)c).toList();
        for(var side:SignTextSlot.values())sign.setText(new SignText(lines,lines,DyeColor.WHITE,false),side);
        sign.setWaxed(true);sign.setChanged();l.sendBlockUpdated(at,sign.getBlockState(),sign.getBlockState(),3);
    }
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),y=100,z=start.getZ();var base=new BlockPos(x,y,z);
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-12,y-3,z-9,x+12,y+6,z+10));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-12,y-3,z-9,x+12,y-1,z+10));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:advance_time false");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("setworldspawn %d %d %d".formatted(x,y,z+5));server.runCommand("tp @p %d %d %d 180 5".formatted(x,y,z));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var l=p.level();
                ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);kit(p);
                // Two contained basins. Water reaches eye height; stairs provide a way out. Lava is deliberately shallow.
                for(int a=-8;a<=-5;a++)for(int b=-5;b<=-2;b++)for(int dy=-2;dy<=-1;dy++)l.setBlockAndUpdate(base.offset(a,dy,b),Blocks.WATER.defaultBlockState());
                l.setBlockAndUpdate(base.offset(-6,-1,-2),Blocks.STONE_BRICK_STAIRS.defaultBlockState());
                for(int a=5;a<=8;a++)for(int b=-5;b<=-2;b++)l.setBlockAndUpdate(base.offset(a,-1,b),Blocks.LAVA.defaultBlockState());
                l.setBlockAndUpdate(base.offset(-2,0,4),Blocks.CRAFTING_TABLE.defaultBlockState());l.setBlockAndUpdate(base.offset(0,0,4),Blocks.SMITHING_TABLE.defaultBlockState());
                l.setBlockAndUpdate(base.offset(2,0,4),Assaying.SCALE.defaultBlockState());l.setBlockAndUpdate(base.offset(4,0,4),Blocks.CHEST.defaultBlockState());
                var chest=(ChestBlockEntity)l.getBlockEntity(base.offset(4,0,4));
                Item[] supplies={Items.GOLD_INGOT,Items.LAPIS_LAZULI,Items.WATER_BUCKET,Items.WATER_BUCKET,Items.LAVA_BUCKET,Items.LAVA_BUCKET,DarkMatter.ITEMS.get("dark_matter"),DarkMatter.ITEMS.get("high_covalence_dust"),Items.RAW_IRON,Items.BUCKET,AlchemicalGear.TALISMAN,AlchemicalGear.BAND,DarkMatter.ITEMS.get("dark_matter_pickaxe")};
                for(int i=0;i<supplies.length;i++)chest.setItem(i,new ItemStack(supplies[i],i==0||i==1||i==6||i==7||i==8?64:1));
                sign(p,base.offset(-6,0,0),"EVERTIDE","Sneak-use: ward","Hold / wear Charm","4 essence / sec");
                sign(p,base.offset(6,0,0),"VOLCANITE","Enable before lava","Ward costs 8 / sec","Keep ledger filled");
                sign(p,base.offset(0,0,-4),"KLEIN STARS","Use to store / take","Withdraw for gear","100,000 essence");
                sign(p,base.offset(-1,0,6),"UPGRADE STARS","Smithing: dust","+ current star","+ Dark Matter");
            });
            c.waitTicks(40);c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(KleinStarScreen.class);c.waitTicks(8);
            c.clickScreenButton("screen.jugcraft.star.button_0");c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var m=(KleinStarMenu)p.containerMenu;
                if(m.stored()!=1024||Assaying.balance(p)!=98976||p.getInventory().countItem(KleinStars.ITEMS.get("klein_star_1"))!=0)throw new AssertionError("Real use opens escrow; store moves exact essence");});
            c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("klein_star_storage");
            c.clickScreenButton("screen.jugcraft.star.button_2");c.waitTicks(6);c.takeScreenshot("klein_star_full");
            c.clickScreenButton("screen.jugcraft.star.button_5");c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(Assaying.balance(p)!=100000||((KleinStarMenu)p.containerMenu).stored()!=0)throw new AssertionError("Take all restores original balance");});
            int scale=c.computeOnClient(mc->mc.options.guiScale().get());
            c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("klein_star_scale1");
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("klein_star_scale2");
            c.getInput().pressKey(InputConstants.KEY_TAB);c.waitTicks(2);
            c.runOnClient(mc->{if(mc.gui.screen().getFocused()==null)throw new AssertionError("Native controls must accept keyboard focus");mc.options.guiScale().set(scale);mc.resizeGui();});
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.getInventory().countItem(KleinStars.ITEMS.get("klein_star_1"))!=1)throw new AssertionError("Closing returns exactly one star");p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET));});
            c.getInput().pressKey(InputConstants.KEY_7);c.waitTicks(40);c.takeScreenshot("evertide_amulet");c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitTicks(12);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.getOffhandItem().is(Items.WATER_BUCKET)||Assaying.balance(p)!=99936)throw new AssertionError("Real amulet use fills bucket once");p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET));});
            c.getInput().pressKey(InputConstants.KEY_8);c.waitTicks(40);c.takeScreenshot("volcanite_amulet");c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitTicks(12);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.getOffhandItem().is(Items.LAVA_BUCKET)||Assaying.balance(p)!=95840)throw new AssertionError("Real lava use charges once");});
            c.getInput().holdKey(options->options.keyShift);c.waitTicks(2);c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.getInput().releaseKey(options->options.keyShift);c.waitTicks(12);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.getMainHandItem().has(AlchemicalGear.ACTIVE))throw new AssertionError("Sneak-use activates ward");kit(p);});
            c.getInput().pressKey(InputConstants.KEY_1);c.waitTicks(40);c.getInput().pressKey(options->options.keyInventory);c.waitForScreen(InventoryScreen.class);c.waitTicks(5);c.takeScreenshot("elemental_alchemy_items");
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);server.runCommand("tp @p %d %d %d 180 8".formatted(x,y,z+3));c.waitTicks(80);
            c.runOnClient(mc->mc.gui.toastManager().clear());world.getConnection().waitForChunksRender();c.takeScreenshot("elemental_alchemy_workshop");
        }
    }
}
