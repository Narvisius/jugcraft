package io.github.jimbozoomer.jugcraft.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.NetherBiomes;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * Jugcraft's Nether and End biomes, placed through Fabric's biome API (not Jugcraft regions, which are the
 * Overworld's). The placements are data ({@code /jugcraft/dimension_biomes.json}, generated from tools/biomes.py):
 * a Nether biome takes a point in the Nether's climate (temperature, humidity and an offset that makes it rarer), an
 * End biome a zone of the outer End and a weight. Placed at startup when {@code biomes.enabled} is on; turning the
 * feature off stops them in new chunks, and the biomes stay registered.
 */
public final class JugcraftDimensions {
	private static final String FILE = "/jugcraft/dimension_biomes.json";
	private static final List<ResourceKey<Biome>> NETHER = new ArrayList<>();
	private static final List<ResourceKey<Biome>> END = new ArrayList<>();

	private JugcraftDimensions() {
	}

	public static void register() {
		if (!JugcraftRegions.enabled()) {
			Jugcraft.LOGGER.info("Jugcraft Nether and End biomes disabled by config");
			return;
		}
		JsonObject data;
		try (InputStream stream = JugcraftDimensions.class.getResourceAsStream(FILE)) {
			if (stream == null) {
				throw new IOException("missing");
			}
			data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			Jugcraft.LOGGER.error("Could not read {}; no Jugcraft Nether or End biomes", FILE, e);
			return;
		}
		for (JsonElement element : data.getAsJsonArray("nether")) {
			JsonObject biome = element.getAsJsonObject();
			ResourceKey<Biome> key = key(biome.get("biome").getAsString());
			NetherBiomes.addNetherBiome(key, Climate.parameters(biome.get("temperature").getAsFloat(), biome.get("humidity").getAsFloat(),
					0.0F, 0.0F, 0.0F, 0.0F, biome.get("offset").getAsFloat()));
			NETHER.add(key);
		}
		for (JsonElement element : data.getAsJsonArray("end")) {
			JsonObject biome = element.getAsJsonObject();
			ResourceKey<Biome> key = key(biome.get("biome").getAsString());
			double weight = biome.get("weight").getAsDouble();
			switch (biome.get("zone").getAsString()) {
				case "highlands" -> TheEndBiomes.addHighlandsBiome(key, weight);
				case "midlands" -> TheEndBiomes.addMidlandsBiome(key(biome.get("highlands").getAsString()), key, weight);
				case "barrens" -> TheEndBiomes.addBarrensBiome(key(biome.get("highlands").getAsString()), key, weight);
				case "small_islands" -> TheEndBiomes.addSmallIslandsBiome(key, weight);
				default -> {
					Jugcraft.LOGGER.error("{}: unknown End zone for {}", FILE, key.identifier());
					continue;
				}
			}
			END.add(key);
		}
	}

	/** The Nether biomes placed, in order. */
	public static List<ResourceKey<Biome>> nether() {
		return List.copyOf(NETHER);
	}

	/** The End biomes placed, in order. */
	public static List<ResourceKey<Biome>> end() {
		return List.copyOf(END);
	}

	private static ResourceKey<Biome> key(String id) {
		return ResourceKey.create(Registries.BIOME, Identifier.parse(id));
	}
}
