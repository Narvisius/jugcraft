package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.AlchemyPatternScreen;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.*;

/** Real rendered local client; no interaction with the owner's launcher or live server. */
public final class ConvenienceTabletClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            int originalScale=c.computeOnClient(mc->mc.options.guiScale().get());
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();});
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();Assaying.setBalance(p,4000);
                for(var item:new Item[]{Items.OAK_LOG,Items.SPRUCE_LOG,Items.BIRCH_LOG,Items.TORCH,Items.GLASS,Items.STONE_BRICKS,Items.COBBLESTONE,Items.STONE,Items.LADDER,Items.TERRACOTTA,Items.SAND,Items.BRICKS,Items.OAK_PLANKS,Items.DIAMOND})AlchemyPatterns.learn(p,new ItemStack(item));
                p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyPatternMenu(id,inv),Component.literal("Transmutation Tablet")));
            });
            c.waitForScreen(AlchemyPatternScreen.class);c.waitTicks(15);
            c.runOnClient(mc->{mc.gui.toastManager().clear();
                var menu=(AlchemyPatternMenu)mc.player.containerMenu;
                for(int i=0;i<AlchemyPatternMenu.PAGE_SIZE;i++)if(menu.getSlot(AlchemyPatternMenu.GHOST_START+i).getItem().is(Items.DIAMOND))throw new AssertionError("Diamond appeared in client catalogue");
            });
            c.takeScreenshot("convenience_tablet_imported_interface");
            c.clickScreenButton("screen.jugcraft.pattern.next");c.waitTicks(5);
            server.runOnServer(s->{var m=(AlchemyPatternMenu)s.getPlayerList().getPlayers().getFirst().containerMenu;if(m.page()!=1)throw new AssertionError("Next page did not reach server");});
            c.takeScreenshot("convenience_tablet_last_page");
            c.clickScreenButton("screen.jugcraft.pattern.previous");c.waitTicks(5);
            server.runOnServer(s->{var m=(AlchemyPatternMenu)s.getPlayerList().getPlayers().getFirst().containerMenu;m.getSlot(0).set(new ItemStack(Items.TORCH,4));m.broadcastChanges();});c.waitTicks(5);
            c.clickScreenButton("screen.jugcraft.pattern.learn");c.waitTicks(5);c.takeScreenshot("convenience_tablet_confirm_dissolve");
            c.clickScreenButton("screen.jugcraft.pattern.confirm");c.waitTicks(5);
            c.clickScreenButton("screen.jugcraft.pattern.make");c.waitTicks(5);
            server.runOnServer(s->{var m=(AlchemyPatternMenu)s.getPlayerList().getPlayers().getFirst().containerMenu;if(!m.getSlot(0).getItem().isEmpty()||!m.getSlot(1).getItem().is(Items.TORCH))throw new AssertionError("Actual UI dissolve/withdraw transaction failed");});
            c.takeScreenshot("convenience_tablet_torch_withdrawal");
            c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("convenience_tablet_scale1");
            c.getInput().pressKey(InputConstants.KEY_TAB);c.waitTicks(2);
            c.runOnClient(mc->{if(mc.gui.screen().getFocused()==null)throw new AssertionError("Missing keyboard focus");mc.options.guiScale().set(originalScale);mc.resizeGui();});
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);
        }
    }
}
