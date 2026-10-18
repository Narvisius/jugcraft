package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;

/** Real menu packets and rendered owner art in the disposable, independently playable showroom. */
final class ConnectedAlchemyClientChecks {
    private static void close(ClientGameTestContext c){c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);c.waitTicks(6);}
    private static void click(ClientGameTestContext c,String key){c.clickScreenButton(key);c.waitTicks(8);}
    private static void shot(ClientGameTestContext c,String name){c.runOnClient(mc->mc.gui.toastManager().clear());c.waitTicks(3);c.takeScreenshot(name);}
    static void run(ClientGameTestContext c,TestServerContext server,BlockPos base){
        server.runCommand("tp @p %.1f %d %.1f 0 -40".formatted(base.getX()+.5,base.getY(),base.getZ()+5.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,new ItemStack(AlchemyStorage.ITEMS.get("transmutation_tablet")));p.inventoryMenu.broadcastChanges();});c.waitTicks(12);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyPatternScreen.class);c.waitTicks(5);
        c.runOnClient(mc->{
            if(mc.getWindow().getGuiScaledWidth()<360||mc.getWindow().getGuiScaledHeight()<238)throw new AssertionError("Whole tablet fits the actual GUI scale");
            for(var slot:mc.player.containerMenu.slots)if(slot.x<0||slot.y<0||slot.x+16>360||slot.y+16>238)throw new AssertionError("All tablet slots fit the panel");
        });
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.containerMenu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));p.containerMenu.broadcastChanges();});c.waitTicks(8);
        click(c,"screen.jugcraft.pattern.learn");click(c,"screen.jugcraft.pattern.confirm");click(c,"screen.jugcraft.pattern.favorite");click(c,"screen.jugcraft.pattern.make");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!AlchemyPatterns.known(p).contains("minecraft:iron_ingot")||!AlchemyPatterns.favorites(p).contains("minecraft:iron_ingot")||!p.containerMenu.getSlot(1).getItem().is(Items.IRON_INGOT))throw new AssertionError("Networked learn, favorite and paid formation");
            for(Item item:new Item[]{Items.GOLD_INGOT,Items.COAL,Items.STONE,Items.GLASS,Items.DIAMOND,Items.OAK_LOG})AlchemyPatterns.learn(p,new ItemStack(item));p.containerMenu.broadcastChanges();});c.waitTicks(8);
        shot(c,"learned_transmutation_patterns");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.containerMenu.getSlot(1).set(ItemStack.EMPTY);Assaying.setBalance(p,10000);p.containerMenu.broadcastChanges();});c.waitTicks(8);
        click(c,"screen.jugcraft.pattern.batch_1");click(c,"screen.jugcraft.pattern.make");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.containerMenu.getSlot(1).getItem().getCount()!=16||Assaying.balance(p)!=5904)throw new AssertionError("Networked 16-item withdrawal costs exactly 4096");});
        shot(c,"tablet_bulk_withdrawal");
        c.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof EditBox edit)edit.setValue("gold");});c.waitTicks(15);
        int savedScale=c.computeOnClient(mc->mc.options.guiScale().get());
        c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.waitTicks(6);
        c.runOnClient(mc->{for(var child:mc.gui.screen().children())if(child instanceof EditBox edit&&!edit.getValue().equals("gold"))throw new AssertionError("Resizing retains tablet search");});shot(c,"tablet_search_scale1");
        c.runOnClient(mc->{mc.options.guiScale().set(savedScale);mc.resizeGui();});c.waitTicks(6);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.containerMenu.getSlot(38).getItem().is(Items.GOLD_INGOT)||p.containerMenu.getSlot(39).hasItem())throw new AssertionError("Search packet filters real server ghosts");});
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,38,0,ContainerInput.PICKUP,mc.player));c.waitTicks(5);
        c.runOnClient(mc->{if(!mc.player.containerMenu.getCarried().isEmpty())throw new AssertionError("Pattern ghost cannot be taken");});close(c);
        // Real converter filter clicks copy identity without consuming the held input stack.
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new ItemStack(AlchemyStorage.ITEMS.get("gem_of_eternal_density")));p.inventoryMenu.broadcastChanges();});c.waitTicks(12);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyAutomationScreen.class);c.waitTicks(6);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.containerMenu.setCarried(new ItemStack(Items.COAL,13));p.containerMenu.broadcastChanges();});c.waitTicks(6);
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,0,0,ContainerInput.PICKUP,mc.player));c.waitTicks(6);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.containerMenu.getCarried().getCount()!=13||!p.getMainHandItem().getOrDefault(AlchemyAutomation.FILTER,java.util.List.of()).contains("minecraft:coal"))throw new AssertionError("Filter copies identity, retains real stack");});
        click(c,"screen.jugcraft.automation.start");
        click(c,"Input limit: 16/s");click(c,"Reserve: 0");
        server.runOnServer(s->{var item=s.getPlayerList().getPlayers().getFirst().getMainHandItem();if(item.getOrDefault(AlchemyAutomation.BUDGET,16)!=64||item.getOrDefault(AlchemyAutomation.RESERVE,0)!=1024)throw new AssertionError("Converter budget and reserve packets update the real item");});
        server.runOnServer(s->{if(!s.getPlayerList().getPlayers().getFirst().getMainHandItem().has(AlchemicalGear.ACTIVE))throw new AssertionError("Converter start packet enables the real item");});
        shot(c,"density_converter_filters");close(c);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new ItemStack(AlchemyArtifacts.ITEMS.get("mercurial_eye")));p.inventoryMenu.broadcastChanges();});c.waitTicks(12);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyPatternScreen.class);c.waitTicks(8);
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,38,0,ContainerInput.PICKUP,mc.player));c.waitTicks(6);click(c,"screen.jugcraft.pattern.choose");
        // A hidden transaction socket must not swallow real inventory items on a shift-click packet.
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,7));p.containerMenu.broadcastChanges();});c.waitTicks(6);
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,2,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(6);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.containerMenu.getSlot(0).getItem().isEmpty()||p.getInventory().getItem(9).getCount()!=7)throw new AssertionError("Eye shift-click preserves visible player inventory");});
        click(c,"Area: 3 x 3");
        server.runOnServer(s->{if(s.getPlayerList().getPlayers().getFirst().getMainHandItem().getOrDefault(AlchemyPatterns.AREA,3)!=5)throw new AssertionError("Eye area packet persists the selected 5x5 footprint");});
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.getMainHandItem().has(AlchemyPatterns.TARGET))throw new AssertionError("Eye retains selected learned building pattern");});shot(c,"mercurial_building_palette");close(c);
        // Use the existing showroom collector: its real GUI now has fuel input, tier selection and output.
        server.runCommand("tp @p %.1f %d %.1f".formatted(base.getX()+3.5,base.getY(),base.getZ()+3.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(3,0,1));be.setPaused(true);be.setFuelTarget(1);be.setEssence(10000);be.setItem(2,new ItemStack(Items.COAL,8));p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemicalWorkshopMenu(id,inv,be),Component.literal("Collector")));});c.waitForScreen(AlchemicalWorkshopScreen.class);c.waitTicks(8);click(c,"screen.jugcraft.workshop.fuel_1");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(((AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(3,0,1))).fuelTarget()!=2)throw new AssertionError("Collector mode button persists actual target");});
        shot(c,"collector_fuel_upgrade_controls");close(c);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(3,0,1));be.setFuelTarget(0);be.setPaused(false);});
        server.runCommand("tp @p %.1f %d %.1f".formatted(base.getX()-8.5,base.getY(),base.getZ()+2.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemyStorageBlockEntity)p.level().getBlockEntity(base.offset(-8,0,0));p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyStorageMenu(id,inv,be),Component.literal("Alchemical Chest")));});c.waitForScreen(AlchemyStorageScreen.class);c.waitTicks(15);
        c.runOnClient(mc->{var be=(AlchemyStorageBlockEntity)mc.level.getBlockEntity(base.offset(-8,0,0));if(be.lid.value<.99f)throw new AssertionError("Client lid reaches open state from actual viewer packet");});shot(c,"animated_chest_inventory");close(c);c.waitTicks(15);
        c.runOnClient(mc->{var be=(AlchemyStorageBlockEntity)mc.level.getBlockEntity(base.offset(-8,0,0));if(be.lid.value>.01f)throw new AssertionError("Client lid closes after last viewer");});
        // Hold one synthetic viewer after verifying the real open/close path, solely to photograph the native hinge.
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();((AlchemyStorageBlockEntity)p.level().getBlockEntity(base.offset(-8,0,0))).viewers=1;});
        server.runCommand("tp @p %.1f %d %.1f 180 20".formatted(base.getX()-7.5,base.getY(),base.getZ()+3.5));c.waitTicks(18);shot(c,"owner_chest_open_model");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();((AlchemyStorageBlockEntity)p.level().getBlockEntity(base.offset(-8,0,0))).viewers=0;});c.waitTicks(15);shot(c,"owner_chest_closed_model");
        server.runCommand("tp @p %.1f %d %.1f 180 20".formatted(base.getX()+10.5,base.getY(),base.getZ()+9.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();((AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(10,0,6))).viewers=1;});c.waitTicks(18);shot(c,"owner_condenser_open_model");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();((AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(10,0,6))).viewers=0;});
        server.runCommand("tp @p %.1f %d %.1f 180 35".formatted(base.getX()+8.5,base.getY(),base.getZ()-5.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(8,0,-8));p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyMachinesMenu(id,inv,be),Component.literal("Dark Matter Pedestal")));});c.waitForScreen(AlchemyMachinesScreen.class);c.waitTicks(8);
        click(c,"Range: 6 blocks");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(((AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(8,0,-8))).range()!=2)throw new AssertionError("Pedestal range action persists");});
        c.runOnClient(mc->{if(((AlchemyMachinesBlockEntity)mc.level.getBlockEntity(base.offset(8,0,-8))).range()!=2)throw new AssertionError("Public range display synchronizes");});shot(c,"pedestal_effect_controls");close(c);c.waitTicks(12);shot(c,"pedestal_range_and_power_hud");
        server.runCommand("tp @p %.1f %d %.1f".formatted(base.getX()+10.5,base.getY(),base.getZ()+8.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(10,0,6));p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemicalWorkshopMenu(id,inv,be),Component.literal("Essence Condenser")));});
        c.waitForScreen(AlchemicalWorkshopScreen.class);c.waitTicks(8);shot(c,"refined_condenser_interface");close(c);
        server.runCommand("tp @p %.1f %d %.1f".formatted(base.getX()+4.5,base.getY(),base.getZ()+3.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(4,0,1));p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyMachinesMenu(id,inv,be),Component.literal("Essence Relay")));});
        c.waitForScreen(AlchemyMachinesScreen.class);c.waitTicks(8);shot(c,"refined_relay_interface");close(c);
        // Look into open sky so use-in-air tests the star rather than reopening the relay in front of us.
        server.runCommand("tp @p %.1f %d %.1f 0 -40".formatted(base.getX()+.5,base.getY(),base.getZ()+5.5));
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new ItemStack(KleinStars.ITEMS.get("klein_star_6")));p.inventoryMenu.broadcastChanges();});c.waitTicks(8);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(KleinStarScreen.class);c.waitTicks(8);
        click(c,"screen.jugcraft.star.button_0");shot(c,"refined_star_interface");close(c);
        c.runOnClient(mc->{
            for(int i=0;i<6;i++)if(mc.font.split(Component.translatable("screen.jugcraft.alchemy_storage.manual.body_"+i),206).size()>11)
                throw new AssertionError("Guide topic exceeds its readable content panel: "+i);
        });
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(Assaying.knows(p))throw new AssertionError("Whole workshop route must work without research");p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ExtendedAlchemyEquipment.ITEMS.get("gem_helmet")));});c.waitTicks(8);
        c.runOnClient(mc->mc.gui.setScreen(new GemAbilitiesScreen()));shot(c,"gem_armor_controls");click(c,"Night vision | Off");c.waitTicks(25);close(c);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Gem button causes real paid helmet effect");p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);});
    }
}
