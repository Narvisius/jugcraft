package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Fixed wood colours, generated together with the block and inventory models by tools/vanilla_logs.py. */
public final class WoodColors {
	private WoodColors() {
	}

	public static void register() {
		try (var reader = new InputStreamReader(Objects.requireNonNull(
				WoodColors.class.getResourceAsStream("/assets/jugcraft/wood_tints.json")), StandardCharsets.UTF_8)) {
			for (var entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
				var colours = entry.getValue().getAsJsonArray();
				var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("jugcraft", entry.getKey()));
				BlockColorRegistry.register(List.of(BlockTintSources.constant(0xFF000000 | colours.get(0).getAsInt()),
						BlockTintSources.constant(0xFF000000 | colours.get(1).getAsInt())), block);
			}
		} catch (java.io.IOException exception) {
			throw new IllegalStateException("Cannot load Jugcraft wood colours", exception);
		}
	}
}
