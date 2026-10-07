package io.github.jimbozoomer.jugcraft.world.design;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.TownPlanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Registrations and an explicit operator-only, local export. No HTTP service or remote writes. */
public final class WorldDesigner {
	private WorldDesigner() { }
	public static void register() {
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Jugcraft.id("design_density"), DesignDensity.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, Jugcraft.id("design"), DesignBiomeSource.CODEC);
		Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, Jugcraft.id("design_pin"), DesignPlacement.CODEC);
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal("jugcraft")
				.then(Commands.literal("design").requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))
						.then(Commands.literal("export").executes(c -> export(c.getSource())))
						.then(Commands.literal("info").executes(c -> {
							var source = c.getSource().getServer().overworld().getChunkSource().getGenerator().getBiomeSource();
							c.getSource().sendSuccess(() -> Component.literal(source instanceof DesignBiomeSource design
									? "Designed world: spawn " + design.spawn().toShortString() + "; town " + design.town().map(BlockPos::toShortString).orElse("none")
									: "This world does not use the Jugcraft designed-world preset."), false);
							return 1;
						})))));
	}
	public static JsonObject catalog(ServerLevel level) {
		var registries = level.registryAccess();
		var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
		JsonObject root = new JsonObject();
		root.addProperty("schema", 1);
		root.addProperty("minecraft", SharedConstants.getCurrentVersion().name());
		var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
		JsonArray packFormat = new JsonArray(); packFormat.add(format.major()); packFormat.add(format.minor());
		root.add("packFormat", packFormat);
		root.addProperty("townSize", TownData.get().size);
		JsonArray biomes = new JsonArray();
		registries.lookupOrThrow(Registries.BIOME).listElements().map(h -> h.key().identifier().toString()).sorted().forEach(biomes::add);
		root.add("biomes", biomes);
		JsonArray blocks = new JsonArray();
		BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString).sorted().forEach(blocks::add);
		root.add("blocks", blocks);
		JsonArray structures = new JsonArray();
		registries.lookupOrThrow(Registries.STRUCTURE).listElements().sorted(java.util.Comparator.comparing(h -> h.key().identifier().toString())).forEach(h -> {
			JsonObject entry = new JsonObject(); entry.addProperty("id", h.key().identifier().toString());
			JsonArray allowed = new JsonArray();
			h.value().biomes().stream().forEach(b -> b.unwrapKey().ifPresent(key -> allowed.add(key.identifier().toString())));
			entry.add("biomes", allowed); structures.add(entry);
		});
		root.add("structures", structures);
		root.add("noiseSettings", NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops,
				registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value()).getOrThrow());
		// Use the established seeded Overworld climate source, including Jugcraft's existing region integration.
		JsonObject base = new JsonObject(); base.addProperty("type", "minecraft:multi_noise"); base.addProperty("preset", "minecraft:overworld");
		root.add("biomeSource", base);
		return root;
	}
	private static int export(CommandSourceStack source) {
		try {
			Path folder = FabricLoader.getInstance().getConfigDir().resolve("jugcraft/world-designer")
					.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")));
			Files.createDirectories(folder);
			for (String name : new String[]{"index.html", "style.css", "model.js", "editor.js"}) {
				try (var input = WorldDesigner.class.getResourceAsStream("/jugcraft/world-designer/" + name)) {
					if (input == null) throw new IOException("Missing bundled editor " + name);
					Files.copy(input, folder.resolve(name));
				}
			}
			String json = new GsonBuilder().setPrettyPrinting().create().toJson(catalog(source.getServer().overworld()));
			Files.writeString(folder.resolve("catalog.json"), json, StandardCharsets.UTF_8);
			Files.writeString(folder.resolve("catalog.js"), "globalThis.JUGCRAFT_CATALOG = " + json + ";\n", StandardCharsets.UTF_8);
			source.sendSuccess(() -> Component.literal("Open this file in your browser: " + folder.resolve("index.html").toAbsolutePath()), false);
			return 1;
		} catch (IOException | RuntimeException e) {
			Jugcraft.LOGGER.error("World designer export failed", e);
			source.sendFailure(Component.literal("Could not export the world designer: " + e.getMessage()));
			return 0;
		}
	}
	/** Called before the ordinary alpine spawn search. Only a new designed world is changed. */
	public static boolean applySpawn(ServerLevel level) {
		if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof DesignBiomeSource design)) return false;
		if (level.getGameTime() == 0L) {
			BlockPos point = design.spawn();
			level.getChunk(point.getX() >> 4, point.getZ() >> 4);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, point.getX(), point.getZ());
			level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withSuppressedOutput(),
					"setworldspawn " + point.getX() + " " + y + " " + point.getZ());
		}
		return true;
	}
	/** Returns true for a designed world even when its plan deliberately has no city. */
	public static boolean applyTown(ServerLevel level) {
		if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof DesignBiomeSource design)) return false;
		design.town().ifPresent(point -> {
			int ground = Math.round(design.grid().height(point.getX(), point.getZ())) - 1;
			TownPlanner.place(level, TownPlanner.originAround(point.getX(), ground, point.getZ()));
		});
		return true;
	}
}
