package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.WorkshopComputers;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Run with the final combined suite, not during source intake. Original-model bake coverage. */
public final class JimtafactsComputersClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(mc -> {
                for (var entry : WorkshopComputers.BLOCKS.entrySet()) {
                    for (var state : entry.getValue().getStateDefinition().getPossibleStates()) {
                        var parts = new ArrayList<BlockStateModelPart>();
                        mc.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(0), parts);
                        if (parts.isEmpty()) throw new AssertionError("Empty original computer model: " + entry.getKey() + " " + state);
                    }
                }
                for (String part : new String[]{"crafting_table", "speaker", "modem_normal_off", "modem_normal_on", "modem_advanced_off", "modem_advanced_on"}) {
                    for (String side : new String[]{"left", "right"}) {
                        var item = new ItemStack(Items.CRAFTING_TABLE);
                        item.set(DataComponents.ITEM_MODEL, Jugcraft.id("jimtafacts/turtle_" + part + "_" + side));
                        var state = new ItemStackRenderState();
                        mc.getItemModelResolver().updateForTopItem(state, item, ItemDisplayContext.NONE, mc.level, mc.player, 0);
                        if (state.isEmpty()) throw new AssertionError("Empty original turtle upgrade: " + part + " " + side);
                    }
                }
            });
        }
    }
}
