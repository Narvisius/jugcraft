package io.github.jimbozoomer.jugcraft.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The blueprint design kit (docs/features/blueprint-system.md): the AI guide and example kept in the jar
 * ({@code assets/jugcraft/blueprint/}), and the lists of allowed blocks and block tags made from the game's own registry,
 * so whatever mods are installed are included. The Blueprint Table writes them to the player's computer.
 */
public final class BlueprintKit {
	public static final String GUIDE = "/assets/jugcraft/blueprint/ai_guide.md";
	public static final String EXAMPLE = "/assets/jugcraft/blueprint/example.jugbp.json";
	/** A block tag written as a palette names it, "#namespace:path". */
	private static final Codec<TagKey<Block>> TAG_NAME = TagKey.hashedCodec(Registries.BLOCK);

	private BlueprintKit() {
	}

	/** A text file kept in the jar. */
	public static String resource(String path) {
		try (InputStream in = BlueprintKit.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}

	/** Every block a blueprint may use, one a line, sorted, with its state properties and their values. */
	public static String blocks() {
		List<String> lines = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (!Blueprint.allowed(block)) {
				continue;
			}
			StringBuilder line = new StringBuilder(BuiltInRegistries.BLOCK.getKey(block).toString());
			List<String> properties = new ArrayList<>();
			for (Property<?> property : block.getStateDefinition().getProperties()) {
				properties.add(property.getName() + "=" + property.getPossibleValues().stream()
						.map(value -> name(property, value)).collect(Collectors.joining("|")));
			}
			if (!properties.isEmpty()) {
				line.append(" [").append(String.join(", ", properties)).append(']');
			}
			lines.add(line.toString());
		}
		lines.sort(null);
		return "# Blocks a Jugcraft blueprint may use: id [property=value|value, ...]\n" + String.join("\n", lines) + "\n";
	}

	/** Every block tag that holds an allowed block and nothing forbidden, with how many blocks it holds, sorted. */
	public static String tags() {
		Map<String, int[]> counts = new TreeMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			boolean ok = Blueprint.allowed(block);
			block.builtInRegistryHolder().tags().forEach((TagKey<Block> tag) -> {
				int[] count = counts.computeIfAbsent(TAG_NAME.encodeStart(JsonOps.INSTANCE, tag).getOrThrow().getAsString(), key -> new int[2]);
				count[ok ? 0 : 1]++;
			});
		}
		StringBuilder out = new StringBuilder("# Block tags an \"any material\" slot may name: #tag (blocks)\n");
		counts.forEach((tag, count) -> {
			if (count[0] > 0 && count[1] == 0) {
				out.append(tag).append(" (").append(count[0]).append(")\n");
			}
		});
		return out.toString();
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> String name(Property<T> property, Object value) {
		return property.getName((T) value);
	}
}
