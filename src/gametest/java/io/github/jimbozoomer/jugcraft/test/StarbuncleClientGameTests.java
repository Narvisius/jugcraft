package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nouveau.*;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity.Route;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** A separate automated world checks real walking/delivery and original art; never touches the owner's game. */
public class StarbuncleClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().create()) {
            var server=world.getServer();world.getConnection().waitForChunksRender();
            var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),z=start.getZ(),y=101;
            var from=new BlockPos(x-5,y,z-5);var to=new BlockPos(x+5,y,z-5);
            var courier=new java.util.UUID[1];
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-15,y,z-12,x+15,y+10,z+20));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-15,y-1,z-12,x+15,y-1,z+20));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var level=p.level();p.getInventory().clearContent();
                p.getInventory().setItem(0,new ItemStack(JugNouveau.WAND));p.getInventory().setItem(1,new ItemStack(JugNouveau.STARBUNCLE_CHARM));p.inventoryMenu.broadcastChanges();
                level.setBlockAndUpdate(from,Blocks.CHEST.defaultBlockState());level.setBlockAndUpdate(to,Blocks.CHEST.defaultBlockState());
                var chest=(ChestBlockEntity)level.getBlockEntity(from);chest.setItem(0,new ItemStack(Items.DIAMOND,32));
                var star=JugNouveau.STARBUNCLE.create(level,EntitySpawnReason.MOB_SUMMONED);star.setOwner(p.getUUID());star.snapTo(x-5+.5,y,z-3+.5,0,0);star.setCustomName(Component.literal("Workshop Courier"));star.setCustomNameVisible(true);level.addFreshEntity(star);
                courier[0]=star.getUUID();
                // Put the owner on the platform before checking the bounded assignment distance.
                p.snapTo(x+.5,y,z+10.5,180,15);
                if(!star.assign(p,new Route(from,to,level.dimension().identifier().toString())))throw new AssertionError("Live player's route must be accepted");
                for(int i=0;i<16;i++) {
                    var display=JugNouveau.STARBUNCLE.create(level,EntitySpawnReason.MOB_SUMMONED);display.setOwner(p.getUUID());display.color(i);display.setNoAi(true);
                    display.snapTo(x-6+(i%4)*4+.5,y,z+2+(i/4)*3+.5,0,0);level.addFreshEntity(display);
                }
            });
            server.runCommand("tp @p %f %f %f 180 10".formatted(x+.5,y+1.0,z+1.5));c.waitTicks(90);
            c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("jug_nouveau_starbuncle_workshop");
            c.waitTicks(510);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var a=(ChestBlockEntity)p.level().getBlockEntity(from);var b=(ChestBlockEntity)p.level().getBlockEntity(to);
                if(!a.isEmpty()||b.getItem(0).getCount()!=32)throw new AssertionError("Real pathfinding must deliver both 16-item trips: source="+a.getItem(0)+", target="+b.getItem(0));});
            c.takeScreenshot("jug_nouveau_starbuncle_delivered");
            // The real two-trip assertion above is complete. Hold a fresh pickup for close visual inspection.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var star=(Starbuncle)p.level().getEntity(courier[0]);
                var a=(ChestBlockEntity)p.level().getBlockEntity(from);a.setItem(0,new ItemStack(Items.DIAMOND,16));
                star.setNoAi(true);star.snapTo(from.getX()+.5,y,from.getZ()+1.5,0,0);star.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);star.think(p.level());
                if(star.cargo().getCount()!=16)throw new AssertionError("Close-up must hold real picked-up cargo");
                p.getInventory().setSelectedSlot(2);p.inventoryMenu.broadcastChanges();});
            server.runCommand("tp @p %f %f %f 180 17".formatted(from.getX()+.5,(double)y,from.getZ()+4.5));c.waitTicks(15);
            c.runOnClient(mc->{if(mc.level.getEntitiesOfClass(Starbuncle.class,mc.player.getBoundingBox().inflate(5),star->star.cargoDisplay().getCount()==16).isEmpty())throw new AssertionError("Real cargo must synchronize to its display");});
            c.takeScreenshot("jug_nouveau_starbuncle_cargo_closeup");
            server.runCommand("tp @p %f %f %f 180 18".formatted(x+.5,y+3.0,z+18.5));c.waitTicks(25);
            c.runOnClient(mc->{if(mc.player.getY()<y-.1)throw new AssertionError("Gallery camera remains on solid ground");});
            c.takeScreenshot("jug_nouveau_starbuncle_sixteen_colors");
        }
    }
}
