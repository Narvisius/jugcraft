package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.RecoveredWildlifeRenderer;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntitySpawnReason;

/** Real rendered adult/baby/every-variant gallery in a disposable world, never an owner save. */
public final class RecoveredCreatureClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            var start = c.computeOnClient(mc -> mc.player.blockPosition());
            int x = start.getX(), y = start.getY() + 6, z = start.getZ();
            server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-20,y-1,z-20,x+50,y-1,z+50));
            server.runCommand("gamemode spectator @a"); server.runCommand("time set noon");
            server.runCommand("weather clear"); server.runCommand("gamerule minecraft:spawn_mobs false");
            c.getInput().pressKey(o -> o.keyToggleGui);
            for (var entry : Menagerie.TYPES.entrySet()) {
                if (entry.getKey().startsWith("wildlife_") || !RecoveredWildlifeRenderer.supports(entry.getKey())) continue;
                var species = Menagerie.SPECIES.get(entry.getKey());
                double spacing = entry.getKey().equals("cavern_ferrouslime")?3.5:Math.max(1.4, species.width() * 2.2);
                double distance = Math.max(4, Math.max(spacing * 2.4, species.height() * 2));
                for(int firstVariant=0;firstVariant<species.variants().size();firstVariant+=2){
                final int first=firstVariant,variant=Math.min(first+1,species.variants().size()-1);
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    for (var old : p.level().getEntitiesOfClass(MenagerieAnimal.class, p.getBoundingBox().inflate(180))) old.discard();
                    for (int i = 0; i < 4; i++) {
                        var animal = entry.getValue().create(p.level(), EntitySpawnReason.COMMAND);
                        animal.snapTo(x + i % 2 * spacing + .5, y, z + i / 2 * spacing + .5, 0, 0);
                        animal.finalizeSpawn(p.level(),p.level().getCurrentDifficultyAt(animal.blockPosition()),EntitySpawnReason.COMMAND,null);
                        animal.variant(i % 2 == 0 ? first : variant); animal.setBaby(i >= 2);
                        animal.setNoAi(true); animal.setNoGravity(true); animal.setPermanentlyInvulnerable(true);
                        p.level().addFreshEntity(animal);
                    }
                });
                double tx = x + spacing / 2 + .5, tz = z + spacing / 2 + .5;
                server.runCommand("tp @p %f %f %f 155 18".formatted(tx + distance * .466,
                        y + species.height() * .45 + distance * .35 - 1.62, tz + distance));
                c.waitTicks(16);
                c.runOnClient(mc -> {
                    int seen = 0;
                    for (var entity : mc.level.entitiesForRendering()) if (entity instanceof MenagerieAnimal animal) {
                        if (!(mc.getEntityRenderDispatcher().getRenderer(animal) instanceof RecoveredWildlifeRenderer))
                            throw new AssertionError("Wildlife used reconstructed renderer: " + entry.getKey());
                        seen++;
                    }
                    if (seen != 4) throw new AssertionError("Missing adult/baby fixtures for " + entry.getKey() + ": " + seen);
                });
                c.takeScreenshot("recovered_" + entry.getKey()+"_v"+first+"_"+variant);
                if (first==0&&(entry.getKey().equals("mutant_mutant_zombie") || entry.getKey().equals("beast_grizzly_bear"))) {
                    server.runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(180)))RecoveredCreatureActions.play(a,"slam_ground","attack","maul","sniff");});
                    c.waitTicks(8); c.takeScreenshot("recovered_" + entry.getKey() + "_later_pose");
                }
            }
            }
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                for(var old:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(180)))old.discard();
                var wolf=RottenStorage.HUNTER_WOLF.create(p.level(),EntitySpawnReason.COMMAND);
                wolf.snapTo(x+.5,y,z+.5,0,0);wolf.setNoAi(true);wolf.setNoGravity(true);p.level().addFreshEntity(wolf);
                p.level().setBlock(new net.minecraft.core.BlockPos(x+2,y,z),Menagerie.BLOCKS.get("rotten_treasure_chest").defaultBlockState(),3);
                p.level().setBlock(new net.minecraft.core.BlockPos(x+3,y,z),Menagerie.BLOCKS.get("rotten_tnt_barrel").defaultBlockState(),3);
            });
            server.runCommand("tp @p %f %f %f 155 18".formatted(x+4.3,y+1.2,z+6.5));
            c.waitTicks(20);c.takeScreenshot("recovered_hunter_wolf_and_storage");
            c.getInput().pressKey(o -> o.keyToggleGui);
            server.runCommand("gamemode creative @a");
            for(String id:Menagerie.ITEMS.keySet().stream().filter(id->id.startsWith("rotten_")&&id.endsWith("_spawn_egg")).sorted().toList()){
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Menagerie.ITEMS.get(id)));});
                c.waitTicks(8);c.takeScreenshot("recovered_held_"+id);
            }
        }
    }
}
