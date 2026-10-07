package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.creatures.scary.DropClientTests;
import io.github.jimbozoomer.jugcraft.creatures.scary.test.ModelScreenshots;
import io.github.jimbozoomer.jugcraft.creatures.scary.test.SpawnRulesClientTests;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ScaryCreaturesClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var previous=HalloweenSeason.mode();
        try {
            HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
            new DropClientTests().runTest(context);
            new SpawnRulesClientTests().runTest(context);
            new ModelScreenshots().runTest(context);
        } finally {
            HalloweenSeason.setMode(previous);
        }
    }
}
