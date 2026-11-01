package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import eu.pb4.trinkets.api.*;
import eu.pb4.trinkets.impl.slots.TrinketSlot;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;

/** Native inventory PICKUP packets, never a fixture that directly equips an accessory. */
public final class EquipmentAccessClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();var server=world.getServer();
            server.runCommand("gamemode survival @a");server.runCommand("gamerule minecraft:spawn_mobs false");
            var samples=server.computeOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var out=new java.util.TreeMap<String,String>();
                for(var e:TrinketsApi.getAttachment(p).getInventories().entrySet()) {
                    if(e.getValue().getContainerSize()==0)continue;
                    var tag=TagKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath("trinkets",e.getKey()));
                    for(var h:BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                        if(TrinketSlotUtils.mayPlace(e.getValue().getOrCreateSlotAccess(0),new ItemStack(h.value()))) {
                            out.put(e.getKey(),h.getRegisteredName());break;
                        }
                    }
                }
                return out;
            });
            if(samples.size()<8)throw new AssertionError("Missing accessory slot families: "+samples);
            c.getInput().pressKey(o->o.keyInventory);c.waitForScreen(InventoryScreen.class);
            for(var sample:samples.entrySet()) {
                String type=sample.getKey();var itemId=Identifier.parse(sample.getValue());
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new ItemStack(BuiltInRegistries.ITEM.getValue(itemId)));p.inventoryMenu.broadcastChanges();});
                c.waitFor(mc->mc.player.inventoryMenu.getSlot(36).getItem().is(BuiltInRegistries.ITEM.getValue(itemId)));
                c.runOnClient(mc->{
                    var menu=mc.player.inventoryMenu;
                    int target=menu.slots.stream().filter(slot->slot instanceof TrinketSlot t&&!t.isDecorativeMode()&&t.getType().getId().equals(type)).findFirst().orElseThrow(()->new AssertionError("No inventory slot for "+type)).index;
                    if(!menu.getSlot(target).mayPlace(menu.getSlot(36).getItem()))throw new AssertionError("Client refuses "+type);
                    mc.gameMode.handleContainerInput(menu.containerId,36,0,ContainerInput.PICKUP,mc.player);
                    mc.gameMode.handleContainerInput(menu.containerId,target,0,ContainerInput.PICKUP,mc.player);
                });
                c.waitFor(mc->TrinketsApi.getAttachment(mc.player).getInventory(type).getItem(0).is(BuiltInRegistries.ITEM.getValue(itemId))&&mc.player.inventoryMenu.getCarried().isEmpty());
                c.waitTicks(10); // Wait beyond client prediction for authoritative packet handling.
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!TrinketsApi.getAttachment(p).getInventory(type).getItem(0).is(BuiltInRegistries.ITEM.getValue(itemId))||!p.getInventory().getItem(0).isEmpty())throw new AssertionError("Server did not equip "+type+": worn="+TrinketsApi.getAttachment(p).getInventory(type).getItem(0)+", hotbar="+p.getInventory().getItem(0)+", cursor="+p.inventoryMenu.getCarried());});
                c.runOnClient(mc->{
                    var menu=mc.player.inventoryMenu;int target=menu.slots.stream().filter(slot->slot instanceof TrinketSlot t&&!t.isDecorativeMode()&&t.getType().getId().equals(type)).findFirst().orElseThrow().index;
                    mc.gameMode.handleContainerInput(menu.containerId,target,0,ContainerInput.PICKUP,mc.player);
                    mc.gameMode.handleContainerInput(menu.containerId,36,0,ContainerInput.PICKUP,mc.player);
                });
                c.waitFor(mc->TrinketsApi.getAttachment(mc.player).getInventory(type).getItem(0).isEmpty()&&mc.player.inventoryMenu.getSlot(36).getItem().is(BuiltInRegistries.ITEM.getValue(itemId))&&mc.player.inventoryMenu.getCarried().isEmpty());
                c.waitTicks(10); // Wait beyond client prediction for authoritative packet handling.
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!TrinketsApi.getAttachment(p).getInventory(type).getItem(0).isEmpty()||p.getInventory().getItem(0).getCount()!=1)throw new AssertionError("Removal lost/duplicated "+type);});
            }
            c.takeScreenshot("equipment_slots_no_research");
            System.out.println("Equipment inventory packets passed for "+samples);
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);
        }
    }
}
