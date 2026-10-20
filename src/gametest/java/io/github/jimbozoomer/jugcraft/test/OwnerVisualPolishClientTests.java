package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

/** Detailed adult/baby and repaired hollow-furniture views in a disposable flat world. */
public final class OwnerVisualPolishClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),y=start.getY()+5,z=start.getZ();
            server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-12,y-1,z-12,x+48,y-1,z+48));
            server.runCommand("gamemode spectator @a");server.runCommand("time set noon");server.runCommand("gamerule minecraft:spawn_mobs false");c.getInput().pressKey(o->o.keyToggleGui);
            for(var entry:Menagerie.TYPES.entrySet()){
                if(!entry.getKey().startsWith("wildlife_"))continue;
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(180)))a.discard();
                    for(int i=0;i<4;i++){var a=entry.getValue().create(p.level(),EntitySpawnReason.COMMAND);a.snapTo(x+i%2*10+.5,y,z+i/2*10+.5,0,0);a.variant(i%2);a.setBaby(i>=2);a.setNoAi(true);a.setNoGravity(true);a.setPermanentlyInvulnerable(true);p.level().addFreshEntity(a);}
                });
                server.runCommand("tp @p %f %f %f 155 17".formatted(x+14.0,y+6.0,z+26.0));c.waitTicks(15);c.takeScreenshot("polish_"+entry.getKey());
            }
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(180)))a.discard();});
            for(String id:java.util.List.of("household_acacia_basin","household_acacia_kitchen_sink","household_acacia_storage_jar","household_acacia_toilet","household_acacia_bath","beast_hummingbird_feeder","ecology_spile")){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int dx=-2;dx<4;dx++)for(int dy=0;dy<4;dy++)for(int dz=-2;dz<4;dz++)p.level().setBlockAndUpdate(new BlockPos(x+dx,y+dy,z+dz),Blocks.AIR.defaultBlockState());var b=Menagerie.BLOCKS.get(id);if(b==null)throw new AssertionError("Unknown fixture "+id);p.level().setBlockAndUpdate(new BlockPos(x,y,z),b.defaultBlockState());b.setPlacedBy(p.level(),new BlockPos(x,y,z),p.level().getBlockState(new BlockPos(x,y,z)),p,new net.minecraft.world.item.ItemStack(b));});
                if(id.equals("ecology_spile"))server.runCommand("setblock %d %d %d jugcraft:ecology_spile[has_bucket=true]".formatted(x,y,z));
                for(int angle:new int[]{0,1}){server.runCommand("tp @p %f %f %f %d %d".formatted(x+(angle==0?2.4:.5),y+(angle==0?1.8:3.2),z+3.6,angle==0?150:180,angle==0?25:65));c.waitTicks(10);c.runOnClient(mc->{if(!mc.player.isAlive())throw new AssertionError("Visual fixture camera died");});c.takeScreenshot("polish_"+id+"_"+angle);}
            }
            c.getInput().pressKey(o->o.keyToggleGui);
        }
    }
}
