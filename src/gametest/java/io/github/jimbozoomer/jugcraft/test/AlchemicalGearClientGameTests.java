package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.AlchemyPatternScreen;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Disposable saved workshop. The owner's independent copy is never controlled by this test. */
public class AlchemicalGearClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            world.getConnection().waitForChunksRender(); var server=world.getServer();
            var start=c.computeOnClient(mc->mc.player.blockPosition()); int x=start.getX(), y=100, z=start.getZ();
            server.runCommand("time set noon"); server.runCommand("weather clear");
            server.runCommand("gamerule minecraft:advance_time false"); server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-9,y,z-9,x+9,y+8,z+9));
            server.runCommand("fill %d %d %d %d %d %d minecraft:dark_oak_planks".formatted(x-9,y-1,z-9,x+9,y-1,z+9));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-5,y-1,z-5,x+5,y-1,z+5));
            server.runCommand("setworldspawn %d %d %d".formatted(x,y,z));
            server.runCommand("tp @p %d %d %d 180 10".formatted(x,y,z+3));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst(); var level=p.level();
                ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);
                ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);
                Assaying.setBalance(p,100000); p.getInventory().clearContent();
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AlchemicalGear.STONE));
                p.getInventory().setItem(1,new ItemStack(AlchemicalGear.TALISMAN));
                p.getInventory().setItem(2,new ItemStack(AlchemicalGear.BAND));
                var pick=new ItemStack(Items.DIAMOND_PICKAXE); pick.setDamageValue(250);
                pick.set(DataComponents.CUSTOM_NAME,Component.literal("Workshop test pick")); p.getInventory().setItem(3,pick);
                p.getInventory().setItem(4,new ItemStack(Items.IRON_INGOT,32)); p.getInventory().setItem(5,new ItemStack(Items.COBBLESTONE,64));
                p.getInventory().setItem(6,new ItemStack(Items.COOKED_BEEF,32)); p.getInventory().setItem(7,new ItemStack(Items.GOLD_INGOT,16));
                for(int side:new int[]{-1,1}) for(int depth:new int[]{-1,1}) {
                    var at=new BlockPos(x+side*7,y,z+depth*7);
                    level.setBlockAndUpdate(at,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                    level.setBlockAndUpdate(at.above(),Blocks.LANTERN.defaultBlockState());
                }
                var chestPos=new BlockPos(x-3,y,z-3); level.setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
                var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);
                for(int i=0;i<5;i++) chest.setItem(i,new ItemStack(new net.minecraft.world.item.Item[]{Items.IRON_INGOT,Items.COPPER_INGOT,Items.COAL,Items.REDSTONE,Items.COBBLESTONE}[i],64));
                for(int i=5;i<9;i++){var armor=new ItemStack(new net.minecraft.world.item.Item[]{Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS}[i-5]);armor.setDamageValue(60);chest.setItem(i,armor);}
                level.setBlockAndUpdate(new BlockPos(x,y,z-3),Blocks.CRAFTING_TABLE.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(x+3,y,z-3),Assaying.SCALE.defaultBlockState());
                // A wall for the owner to verify the magnet cannot pull through solid blocks.
                for(int a=0;a<4;a++)for(int b=0;b<3;b++)level.setBlockAndUpdate(new BlockPos(x+6,y+b,z-1+a),Blocks.STONE_BRICKS.defaultBlockState());
                AlchemicalGear.STONE.use(level,p,InteractionHand.MAIN_HAND);
                var m=(AlchemyPatternMenu)p.containerMenu;
                m.getSlot(0).set(new ItemStack(Items.TORCH,8)); m.broadcastChanges();
            });
            c.waitForScreen(AlchemyPatternScreen.class); c.waitTicks(8);
            c.runOnClient(mc->{
                String balance=Component.translatable("screen.jugcraft.alchemy.balance",100000).getString();
                if(!balance.equals("100000 essence"))throw new AssertionError("Tablet must display essence with the existing balance");
            });
            c.runOnClient(mc->mc.gui.toastManager().clear()); c.takeScreenshot("alchemy_stone_ready");
            c.clickScreenButton("screen.jugcraft.pattern.learn"); c.waitTicks(6);
            c.takeScreenshot("alchemy_stone_confirm");
            c.clickScreenButton("screen.jugcraft.pattern.confirm"); c.waitTicks(6);
            c.clickScreenButton("screen.jugcraft.pattern.make"); c.waitTicks(8);
            boolean paid=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var m=(AlchemyPatternMenu)p.containerMenu;
                return Assaying.balance(p)==100063 && m.getSlot(0).getItem().isEmpty() && m.getSlot(1).getItem().is(Items.TORCH) && m.getSlot(1).getItem().getCount()==1 && AlchemyPatterns.known(p).contains("minecraft:torch");});
            if(!paid)throw new AssertionError("Server menu consumes eight torches, learns torches and forms one at its exact 9 essence value");
            c.runOnClient(mc->mc.gui.toastManager().clear()); c.takeScreenshot("alchemy_stone_result");
            int scale=c.computeOnClient(mc->mc.options.guiScale().get());
            c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("alchemy_tablet_scale1");
            int smallGuiWidth=c.computeOnClient(mc->mc.gui.screen().width);
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(5);c.takeScreenshot("alchemy_tablet_scale2");
            c.runOnClient(mc->{if(mc.gui.screen().width>=smallGuiWidth)throw new AssertionError("Test must exercise two genuinely different GUI scales");});
            c.getInput().pressKey(InputConstants.KEY_TAB); c.waitTicks(2);
            c.runOnClient(mc->{if(mc.gui.screen().getFocused()==null)throw new AssertionError("Menu actions must be keyboard focusable");mc.options.guiScale().set(scale);mc.resizeGui();});
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);c.waitTicks(5);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.getInventory().countItem(Items.IRON_INGOT)!=32||p.getInventory().countItem(Items.TORCH)!=1)throw new AssertionError("Closing returns the actual torch output without manufacturing a retained sample");});
            // Exercise the real server tick hook, not just the pure per-player pulse used by game tests.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                p.getInventory().setItem(8,p.getMainHandItem()); p.setItemInHand(InteractionHand.MAIN_HAND,p.getInventory().getItem(3));p.getInventory().setItem(3,ItemStack.EMPTY);
                p.setItemInHand(InteractionHand.OFF_HAND,p.getInventory().getItem(1));p.getInventory().setItem(1,ItemStack.EMPTY);
            });
            c.waitTicks(8); c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            c.waitTicks(45);
            if(!server.computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getMainHandItem().getDamageValue()<=242))
                throw new AssertionError("Equipped talisman must repair on the real server tick");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getOffhandItem().remove(AlchemicalGear.ACTIVE);
                p.getInventory().setItem(1,p.getOffhandItem());p.setItemInHand(InteractionHand.OFF_HAND,p.getInventory().getItem(2));p.getInventory().setItem(2,ItemStack.EMPTY);
                var drop=new net.minecraft.world.entity.item.ItemEntity(p.level(),p.getX()+3,p.getY()+.2,p.getZ(),new ItemStack(Items.EMERALD));
                drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);drop.setNoPickUpDelay();p.level().addFreshEntity(drop);
            });
            c.waitTicks(8); c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            c.waitTicks(100);
            if(!server.computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.EMERALD)==1))
                throw new AssertionError("Band must attract a drop until vanilla pickup collects it");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getOffhandItem().remove(AlchemicalGear.ACTIVE);
                p.getInventory().setItem(2,p.getOffhandItem());p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                p.getInventory().setItem(3,p.getMainHandItem());p.getInventory().getItem(3).setDamageValue(250);
                p.setItemInHand(InteractionHand.MAIN_HAND,p.getInventory().getItem(8));p.getInventory().setItem(8,ItemStack.EMPTY);
            });
            c.runOnClient(mc->mc.gui.toastManager().clear());world.getConnection().waitForChunksRender();c.takeScreenshot("alchemy_workshop");
        }
    }
}
