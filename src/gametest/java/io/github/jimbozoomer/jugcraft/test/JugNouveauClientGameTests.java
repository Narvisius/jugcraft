package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.StorageLecternScreen;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Independent disposable test world; never operates the owner's Modrinth playtest. */
public class JugNouveauClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),z=start.getZ(),y=101;
            var home=new BlockPos(x,y,z);
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-12,y,z-10,x+12,y+8,z+10));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-12,y-1,z-10,x+12,y-1,z+20));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(JugNouveau.WAND));p.getInventory().setItem(1,new ItemStack(JugNouveau.LECTERN_ITEM));p.getInventory().setItem(2,new ItemStack(JugNouveau.CHARM,4));
                p.level().setBlockAndUpdate(home,JugNouveau.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
                var be=(StorageLectern)p.level().getBlockEntity(home);be.placedBy(p);be.bind(p);be.bind(p);
                for(int i=0;i<10;i++){
                    var pos=home.offset(-5+i,0,-4);p.level().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
                    var chest=(ChestBlockEntity)p.level().getBlockEntity(pos);chest.setItem(0,new ItemStack(i%2==0?Items.DIAMOND:Items.EMERALD,16+i));
                    if(!be.link(p,pos))throw new AssertionError("Showroom chest did not link: "+i);
                }
                for(int i=0;i<6;i++){
                    var pos=home.offset(-10+4*i,0,6);p.level().setBlockAndUpdate(pos,JugNouveau.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
                    var display=(StorageLectern)p.level().getBlockEntity(pos);display.placedBy(p);display.bind(p);
                    var wyrm=(BookWyrm)p.level().getEntity(display.wyrms().getFirst());wyrm.color(i);
                }
                p.inventoryMenu.broadcastChanges();
            });
            server.runCommand("tp @p %f %f %f 180 12".formatted(x+.5,y+1.0,z+5.0));c.waitTicks(90);
            c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("jug_nouveau_lectern_and_wyrms");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(StorageLectern)p.level().getBlockEntity(home);
                p.openMenu(new SimpleMenuProvider((id,inv,who)->new StorageLecternMenu(id,inv,be),Component.translatable("screen.jugcraft.bookwyrm.title")));});
            c.waitForScreen(StorageLecternScreen.class);c.waitTicks(10);
            c.runOnClient(mc->{if(!(mc.player.containerMenu instanceof StorageLecternMenu menu)||menu.pages()!=10||!menu.online()||menu.slots.get(0).getItem().getCount()!=16)throw new AssertionError("Live linked inventory must reach client");});
            c.takeScreenshot("jug_nouveau_storage_linked");
            c.runOnClient(mc->mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,1));c.waitTicks(8);
            c.runOnClient(mc->{var menu=(StorageLecternMenu)mc.player.containerMenu;if(menu.page()!=1||!menu.slots.get(0).getItem().is(Items.EMERALD))throw new AssertionError("Next chest must change the live page");});
            c.takeScreenshot("jug_nouveau_storage_next_chest");
            int scale=c.computeOnClient(mc->mc.options.guiScale().get());
            for(int size:new int[]{1,2}){c.runOnClient(mc->{mc.options.guiScale().set(size);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("jug_nouveau_storage_scale"+size);}
            c.getInput().pressKey(InputConstants.KEY_TAB);c.runOnClient(mc->{if(mc.gui.screen().getFocused()==null)throw new AssertionError("Keyboard navigation");mc.options.guiScale().set(scale);mc.resizeGui();});
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);
            server.runCommand("tp @p %f %f %f 180 5".formatted(x+.5,(double)y,z+17.0));c.waitTicks(20);
            c.runOnClient(mc->{if(mc.player.getY()<y-.1)throw new AssertionError("Gallery camera must remain on its platform");});
            c.takeScreenshot("jug_nouveau_six_colors");
        }
    }
}
