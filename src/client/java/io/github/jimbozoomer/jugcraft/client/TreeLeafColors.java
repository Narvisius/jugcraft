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

/** Fixed evergreen colours for the owner's grayscale foliage; authored full-colour leaves keep their own pixels. */
public final class TreeLeafColors {
	private TreeLeafColors() { }
	public static void register() {
		try (var reader = new InputStreamReader(Objects.requireNonNull(TreeLeafColors.class.getResourceAsStream(
				"/assets/jugcraft/tree_leaf_tints.json")), StandardCharsets.UTF_8)) {
			for (var entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
				var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("jugcraft", entry.getKey()));
				BlockColorRegistry.register(List.of(BlockTintSources.constant(0xFF000000 | entry.getValue().getAsInt())), block);
			}
		} catch (java.io.IOException e) { throw new IllegalStateException("Cannot load tree leaf colours", e); }
	}
}
