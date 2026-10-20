package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Disposable render world; never launches or controls the owner's Modrinth profile. */
public final class MenagerieClientGameTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c){try(var world=c.worldBuilder().create()){
        var server=world.getServer();world.getConnection().waitForChunksRender();var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),z=start.getZ(),y=120;
        server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-5,y-1,z-5,x+32,y-1,z+30));server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
        var types=Menagerie.TYPES.values().stream().toList();
        c.getInput().pressKey(options->options.keyToggleGui);
        for(int page=0;page<(types.size()+5)/6;page++){
            final int offset=page*6;
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var e:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(100)))e.discard();
                for(int i=0;i<6&&i+offset<types.size();i++){var a=types.get(i+offset).create(p.level(),EntitySpawnReason.COMMAND);if(a==null)throw new AssertionError("Missing entity");a.snapTo(x+(i%3)*8+.5,y,z+(i/3)*9+.5,0,0);a.setNoAi(true);a.setNoGravity(true);a.setPermanentlyInvulnerable(true);a.setCustomName(net.minecraft.network.chat.Component.literal(a.species().name()));a.setCustomNameVisible(true);p.level().addFreshEntity(a);}});
            server.runCommand("tp @p %f %f %f 180 13".formatted(x+8.5,y+2.5,z+21.0));c.waitTicks(25);c.takeScreenshot("menagerie_creatures_"+page);
        }
        for(String id:java.util.List.of("wildlife_bear","wildlife_giraffe","wildlife_snail","mount_griffon","grove_cindershell","grove_sporeling","grove_yeti","grove_lizard","grove_end_whale","fable_jinx","fable_scarecrow","fable_scorpion","fable_wisp","companion_ferret","companion_red_panda","companion_dragonfly","companion_roly_poly","companion_snail_baby","mutant_mutant_zombie","beast_grizzly_bear","beast_void_worm","cavern_gummy_bear","cavern_tremorzilla","landscape_goblin_king","ecology_camel")){
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var e:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(100)))e.discard();var a=Menagerie.TYPES.get(id.replace("_baby","")).create(p.level(),EntitySpawnReason.COMMAND);if(id.endsWith("_baby")){a.setBaby(true);a.tame(p.getUUID());a.sit(true);}a.snapTo(x+.5,y,z+.5,0,0);a.setNoAi(true);a.setNoGravity(true);a.setPermanentlyInvulnerable(true);p.level().addFreshEntity(a);
                if(id.equals("companion_roly_poly")){a.tame(p.getUUID());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.CHEST));a.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);}
                if(id.equals("companion_dragonfly")){a.tame(p.getUUID());a.equipBody(new ItemStack(Menagerie.item("companion_diamond_dragonfly_armor")));a.refreshArmor();}
            });
            var spec=Menagerie.SPECIES.get(id.replace("_baby",""));double distance=Math.max(2.4,Math.max(spec.width()*2.5,spec.height()*1.6)),target=spec.height()*.6,feet=Math.max(0,target-1.62),pitch=Math.toDegrees(Math.atan2(feet+1.62-target,distance));
            server.runCommand("tp @p %f %f %f 135 %f".formatted(x+.5+distance*.7071,y+feet,z+.5+distance*.7071,pitch));c.waitTicks(20);c.takeScreenshot(id+"_detail");
        }
        c.getInput().pressKey(options->options.keyToggleGui);
        for(String gear:java.util.List.of("companion_acorn_hat","grove_flower_crown","grove_sporeling_backpack")){
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,ItemStack.EMPTY);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ItemStack.EMPTY);p.setItemSlot(gear.endsWith("backpack")?net.minecraft.world.entity.EquipmentSlot.CHEST:net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Menagerie.item(gear)));p.inventoryMenu.broadcastChanges();});
            c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));c.waitTicks(5);c.takeScreenshot(gear+"_worn");c.runOnClient(mc->mc.gui.setScreen(null));
        }
        var items=Menagerie.ITEMS.values().stream().distinct().toList();
        for(int page=0;page<(items.size()+26)/27;page++){
            final int offset=page*27;server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();for(int i=0;i<27&&offset+i<items.size();i++)p.getInventory().setItem(9+i,new ItemStack(items.get(offset+i)));p.inventoryMenu.broadcastChanges();});
            c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));c.waitTicks(3);c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("menagerie_items_"+page);c.runOnClient(mc->mc.gui.setScreen(null));
        }
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();int i=0;for(var block:Menagerie.BLOCKS.values()){var at=new BlockPos(x+(i%9)*3,y,z+(i++/9)*3);p.level().setBlockAndUpdate(at,block.defaultBlockState());}});
        server.runCommand("tp @p %f %f %f 180 30".formatted(x+12.5,y+12.0,z+35.0));c.waitTicks(25);c.takeScreenshot("menagerie_blocks");
    }}
}


