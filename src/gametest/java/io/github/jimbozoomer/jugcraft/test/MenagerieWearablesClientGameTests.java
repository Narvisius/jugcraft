package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

/** Checks the armor rendering path as well as the inventory icon. Disposable world only. */
public final class MenagerieWearablesClientGameTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c){try(var world=c.worldBuilder().create()){
        var server=world.getServer();world.getConnection().waitForChunksRender();
        var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),z=start.getZ(),y=120;
        server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-8,y-1,z-8,x+8,y-1,z+8));
        server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
        c.getInput().pressKey(options->options.keyToggleGui);
        for(String gear:java.util.List.of("companion_acorn_hat","grove_flower_crown","grove_glowing_flower_crown","grove_sporeling_backpack")){
            var slot=gear.endsWith("backpack")?EquipmentSlot.CHEST:EquipmentSlot.HEAD;
            c.runOnClient(mc->{var stack=new ItemStack(Menagerie.item(gear));
                if(!net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer.shouldRender(stack,slot))throw new AssertionError(gear+" bypasses the armor rendering path");
                if(com.geckolib.animatable.client.GeoRenderProvider.of(stack).getGeoArmorRenderer(stack,slot)==null)throw new AssertionError(gear+" lacks its supplied model renderer");
            });
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                for(var stand:p.level().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,new net.minecraft.world.phys.AABB(x-8,y-1,z-8,x+8,y+5,z+8)))stand.discard();
                var stand=EntityTypes.ARMOR_STAND.create(p.level(),EntitySpawnReason.COMMAND);
                stand.snapTo(x+.5,y,z+.5,0,0);stand.setNoGravity(true);stand.setPermanentlyInvulnerable(true);stand.setItemSlot(slot,new ItemStack(Menagerie.item(gear)));p.level().addFreshEntity(stand);
            });
            server.runCommand("tp @p %f %f %f 135 8".formatted(x+2.7,y+.2,z+2.7));c.waitTicks(20);c.takeScreenshot(gear+"_front");
            server.runCommand("tp @p %f %f %f -45 8".formatted(x-1.7,y+.2,z-1.7));c.waitTicks(15);c.takeScreenshot(gear+"_back");
        }
    }}
}
