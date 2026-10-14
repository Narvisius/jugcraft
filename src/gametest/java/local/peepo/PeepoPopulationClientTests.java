package local.peepo;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Focused livestock population and settings regression suite. */
public final class PeepoPopulationClientTests implements FabricClientGameTest {
    private int checks;
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();var base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:random_tick_speed 0");
            new CompanionPopulationChecks(this::check,base).run(context,server);
        }
        org.slf4j.LoggerFactory.getLogger("peepo-population-test").info("PASS: {} livestock population assertions",checks);
    }
}
