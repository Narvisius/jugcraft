package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import java.util.HashSet;
import java.util.ArrayList;

/** Actual stitched sprites, not merely the existence of PNG files on disk. */
public final class ImportedModelAtlasClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        c.runOnClient(mc -> {
            var resources = mc.getResourceManager();
            var blocks = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            var items = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
            var checked = new HashSet<Identifier>();
            var missing = new ArrayList<String>();
            for (var entry : resources.listResources("models", id -> id.getNamespace().equals("jugcraft") && id.getPath().endsWith(".json")).entrySet()) {
                try (var reader = entry.getValue().openAsReader()) {
                    var model = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!model.has("textures")) continue;
                    for (var texture : model.getAsJsonObject("textures").entrySet()) {
                        if (!texture.getValue().isJsonPrimitive()) continue;
                        String value = texture.getValue().getAsString();
                        if (!value.startsWith("jugcraft:")) continue;
                        var id = Identifier.parse(value);
                        if (checked.add(id) && blocks.getSprite(id) == blocks.missingSprite() && items.getSprite(id) == items.missingSprite())
                            missing.add(id + " used by " + entry.getKey());
                    }
                } catch (java.io.IOException e) { throw new AssertionError(entry.getKey().toString(), e); }
            }
            if (!missing.isEmpty()) throw new AssertionError("Unstitched model textures: " + missing);
            if (checked.size() < 11000) throw new AssertionError("Incomplete model audit: " + checked.size());
            System.out.println("Jugcraft runtime model atlas audit: " + checked.size() + " unique textures resolved.");
            var blockModels = mc.getModelManager().getBlockStateModelSet();
            int states = 0;
            for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
                if (!id.getNamespace().equals("jugcraft") || !id.getPath().startsWith("jimta_")) continue;
                for (var state : block.getStateDefinition().getPossibleStates()) {
                    if (state.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) continue;
                    var model = blockModels.get(state);
                    if (model == blockModels.missingModel()) throw new AssertionError("Rejected block model: " + state);
                    var parts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
                    model.collectParts(net.minecraft.util.RandomSource.create(0), parts);
                    for (var part : parts) for (int face = -1; face < 6; face++)
                        for (var quad : part.getQuads(face == -1 ? null : net.minecraft.core.Direction.values()[face]))
                            if (quad.materialInfo().sprite() == blocks.missingSprite())
                                throw new AssertionError("Missing baked face texture: " + state);
                    states++;
                }
            }
            System.out.println("Jugcraft baked imported block audit: " + states + " states resolved without missing faces.");
        });
        try (var world = c.worldBuilder().adjustSettings(s -> s.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var inventory = new net.minecraft.world.SimpleContainer(54);
                int slot = 0;
                for (String name : java.util.List.of("certus_quartz_crystal", "charged_certus_quartz_crystal",
                        "engineering_processor_press", "calculation_processor_press", "printed_silicon",
                        "black_paint_ball", "blue_lumen_paint_ball", "quantum_ring", "quantum_link",
                        "quantum_entangled_singularity", "item_storage_cell_1k", "fluid_storage_cell_1k",
                        "charged_staff", "color_applicator", "matter_cannon", "annihilation_core", "formation_core",
                        "crafting_card", "advanced_card", "basic_card", "spellbooks_portal_frame", "spellbooks_blaze_spell_book",
                        "power_tester", "mimic_spawn_egg", "spellbooks_firefly_jar", "spellbooks_brazier", "iron_fence")) {
                    var id = Identifier.fromNamespaceAndPath("jugcraft", "jimta_" + name);
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                    if (item == net.minecraft.world.item.Items.AIR) throw new AssertionError("Missing reported item " + id);
                    inventory.setItem(slot++, new net.minecraft.world.item.ItemStack(item));
                }
                player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) ->
                        new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x6,
                                id, inv, inventory, 6), net.minecraft.network.chat.Component.literal("Reported missing assets — original art")));
            });
            c.waitTicks(15);
            c.takeScreenshot("oct29_reported_item_models");
            c.runOnClient(mc -> mc.player.closeContainer());
        }
    }
}
