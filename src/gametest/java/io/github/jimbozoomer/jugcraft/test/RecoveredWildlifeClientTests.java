package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.RecoveredWildlifeRenderer;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EntitySpawnReason;

/** Real rendered adult/baby/base/dye gallery in a disposable world, never an owner save. */
public final class RecoveredWildlifeClientTests implements FabricClientGameTest {
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
                if (!entry.getKey().startsWith("wildlife_")) continue;
                var species = Menagerie.SPECIES.get(entry.getKey());
                double spacing = Math.max(1.4, species.width() * 2.2);
                double distance = Math.max(4, Math.max(spacing * 2.4, species.height() * 2));
                int otherVariant = 0;
                for (int i = 1; i < species.variants().size(); i++) {
                    if (species.variants().get(i).name().equals("dye_blue")) { otherVariant = i; break; }
                    otherVariant = i;
                }
                final int variant = otherVariant;
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    for (var old : p.level().getEntitiesOfClass(MenagerieAnimal.class, p.getBoundingBox().inflate(180))) old.discard();
                    for (int i = 0; i < 4; i++) {
                        var animal = entry.getValue().create(p.level(), EntitySpawnReason.COMMAND);
                        animal.snapTo(x + i % 2 * spacing + .5, y, z + i / 2 * spacing + .5, 0, 0);
                        animal.variant(i % 2 == 0 ? 0 : variant); animal.setBaby(i >= 2);
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
                c.takeScreenshot("recovered_" + entry.getKey());
                if (entry.getKey().equals("wildlife_snail") || entry.getKey().equals("wildlife_bear")) {
                    c.waitTicks(35); c.takeScreenshot("recovered_" + entry.getKey() + "_later_pose");
                }
            }
            c.getInput().pressKey(o -> o.keyToggleGui);
        }
    }
}
