package io.github.jimbozoomer.jugcraft.test;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.biome.JugcraftRegions;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhman;
import io.github.jimbozoomer.jugcraft.world.AltarKnolls;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adam-ondi-Ahman (docs/features/adam-ondi-ahman.md): the valley's biome and the knoll's template load, and the knoll's
 * footprint holds it; the biome is listed in the Overworld's table where no climate reaches it; the Ancient Altar
 * cannot be broken and only says it is dormant; the rare valleys' sites keep to their cells, in about the share the
 * settings give; and the climate a knoll needs. Where the valleys land in a real world is
 * AdamOndiAhmanClientGameTests'.
 */
public class AdamOndiAhmanGameTests {
	/** Reloading saved placement evidence must not turn old chunks into new terrain or relocate the start valley. */
	@GameTest
	public void valleyPlacementEvidenceSurvivesSerialization(GameTestHelper helper) {
		var codec = io.github.jimbozoomer.jugcraft.world.AdamOndiAhmanState.CODEC;
		var ops = com.mojang.serialization.JsonOps.INSTANCE;
		var input = com.google.gson.JsonParser.parseString(
				"{\"start\":[1024,0,-800],\"chosen\":true,\"new_chunks\":{\"42\":[17,18]},\"built\":[99]}");
		var loaded = codec.parse(ops, input).getOrThrow();
		var reloaded = codec.parse(ops, codec.encodeStart(ops, loaded).getOrThrow()).getOrThrow();
		helper.assertTrue(reloaded.chosen() && new BlockPos(1024, 0, -800).equals(reloaded.start()),
				"The chosen start valley must stay at its saved site");
		helper.assertTrue(reloaded.built(99) && !reloaded.built(42), "Built and pending knolls stay distinct");
		helper.assertTrue(reloaded.isNew(42, 17) && reloaded.isNew(42, 18) && !reloaded.isNew(42, 19),
				"Only recorded newly generated chunks may participate in deferred placement");
		var absent = codec.parse(ops, com.google.gson.JsonParser.parseString("{\"chosen\":true}")).getOrThrow();
		helper.assertTrue(absent.chosen() && absent.start() == null && !absent.isNew(42, 17),
				"A world with no suitable start valley stays chosen and gains no new-chunk evidence");
		helper.succeed();
	}
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");
	private static final BlockPos STAND = new BlockPos(1, 2, 1);
	/** tools/adam_ondi_ahman.py SIZE. */
	private static final Vec3i SIZE = new Vec3i(48, 18, 48);

	/** The biome loads and is an Overworld biome, with its own name. */
	@GameTest
	public void valleyBiomeLoads(GameTestHelper helper) {
		Holder<Biome> valley = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(AdamOndiAhman.BIOME);
		helper.assertTrue(valley.is(BiomeTags.IS_OVERWORLD), "Adam-ondi-Ahman is not an Overworld biome");
		helper.succeed();
	}

	/**
	 * A knoll's footprint (its pad and blend ring, 33 blocks either way) for a site at a chunk's corner is the 6 x 6 chunks
	 * round it, and its pad holds its template however it turns.
	 */
	@GameTest
	public void knollFootprintHoldsItsTemplate(GameTestHelper helper) {
		AdamOndiAhman.Site site = AdamOndiAhman.Site.at(1600, -512, 7L);
		List<ChunkPos> chunks = AltarKnolls.footprint(site);
		helper.assertTrue(chunks.size() == 36 && chunks.contains(new ChunkPos(97, -35)) && chunks.contains(new ChunkPos(102, -30)),
				"The knoll's footprint is " + chunks);
		helper.assertTrue(AltarKnolls.HALF >= AltarKnolls.CENTRE + 1 && AltarKnolls.HALF >= AltarKnolls.SIZE - AltarKnolls.CENTRE,
				"The knoll's pad does not hold its template turned");
		helper.assertTrue(AltarKnolls.key(site) != AltarKnolls.key(AdamOndiAhman.Site.at(1600, -496, 7L)), "Two sites share a key");
		helper.succeed();
	}

	/** The valley is listed once in vanilla's Overworld table, where no climate reaches it: only its sites place it. */
	@GameTest
	public void valleyIsListedWhereNoClimateReaches(GameTestHelper helper) {
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> table = helper.getLevel().registryAccess()
				.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
				.value().parameters().values();
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> listed = table.stream().filter(entry -> entry.getSecond().is(AdamOndiAhman.BIOME)).toList();
		helper.assertTrue(listed.size() == 1 && listed.get(0).getFirst().equals(JugcraftRegions.UNREACHABLE),
				"Adam-ondi-Ahman is listed " + listed.size() + " times in the Overworld's table, not once where no climate reaches");
		helper.succeed();
	}

	/** The knoll's template loads, at the size tools/adam_ondi_ahman.py gives it, with the Ancient Altar in it, in this game's data version. */
	@GameTest
	public void knollTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Identifier path = Jugcraft.id("structure/adam_ondi_ahman/altar_knoll.nbt");
		CompoundTag tag;
		try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(path).open()) {
			tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			throw helper.assertionException("Could not read " + path + ": " + e);
		}
		StructureTemplate template = new StructureTemplate();
		template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
		helper.assertTrue(template.getSize().equals(SIZE), "The knoll's template is " + template.getSize() + ", not " + SIZE);
		CompoundTag saved = template.save(new CompoundTag());
		helper.assertTrue(tag.getIntOr("DataVersion", -1) == saved.getIntOr("DataVersion", -2), "The template's DataVersion is not the game's");
		ListTag palette = saved.getListOrEmpty("palette");
		List<String> names = new ArrayList<>();
		for (int i = 0; i < palette.size(); i++) {
			names.add(palette.getCompoundOrEmpty(i).getStringOr("id", "?"));
		}
		for (String wanted : List.of("jugcraft:ancient_altar", "minecraft:grass_block", "minecraft:mossy_cobblestone", "minecraft:stone_bricks",
				"minecraft:chiseled_stone_bricks", "minecraft:oak_log", "minecraft:water")) {
			helper.assertTrue(names.contains(wanted), "The knoll's template has no " + wanted + ": " + names);
		}
		helper.succeed();
	}

	/** The Ancient Altar cannot be broken or blown up and has no item; used, it only says it is dormant, and stays. */
	@GameTest
	public void ancientAltarIsDormantAndUnbreakable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(STAND, AdamOndiAhman.ANCIENT_ALTAR.defaultBlockState());
		BlockPos at = helper.absolutePos(STAND);
		BlockState state = level.getBlockState(at);
		helper.assertTrue(state.getDestroySpeed(level, at) < 0.0F, "The Ancient Altar can be broken");
		helper.assertTrue(AdamOndiAhman.ANCIENT_ALTAR.getExplosionResistance() >= 3600000.0F, "The Ancient Altar can be blown up");
		helper.assertTrue(AdamOndiAhman.ANCIENT_ALTAR.asItem() == Items.AIR, "The Ancient Altar has an item");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		InteractionResult result = state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
		helper.assertTrue(result.consumesAction(), "Using the Ancient Altar did nothing: " + result);
		helper.assertTrue(level.getBlockState(at).is(AdamOndiAhman.ANCIENT_ALTAR), "Using the Ancient Altar changed it");
		helper.succeed();
	}

	/**
	 * The rare valleys' sites: about the configured share of cells has them; each of a cell's tries is a chunk's corner,
	 * the same every time for a seed (and different for another seed), and its whole valley (its wavering edge too) stays
	 * inside its own cell.
	 */
	@GameTest
	public void rareValleySitesKeepToTheirCells(GameTestHelper helper) {
		int cells = AdamOndiAhman.SPACING;
		int share = (int) Math.round(AdamOndiAhman.CHANCE * 65536);
		long print = AdamOndiAhman.fingerprint(1838L);
		long other = AdamOndiAhman.fingerprint(1839L);
		int total = 0;
		int with = 0;
		int differ = 0;
		int reach = AdamOndiAhman.RADIUS + AdamOndiAhman.WOBBLE;
		for (int cx = -30; cx < 30; cx++) {
			for (int cz = -30; cz < 30; cz++) {
				total++;
				AdamOndiAhman.Site first = AdamOndiAhman.candidate(print, cx, cz, cells, share, 0);
				if (!Objects.equals(first, AdamOndiAhman.candidate(other, cx, cz, cells, share, 0))) {
					differ++;
				}
				if (first == null) {
					continue;
				}
				with++;
				for (int attempt = 0; attempt < AdamOndiAhman.TRIES; attempt++) {
					AdamOndiAhman.Site site = AdamOndiAhman.candidate(print, cx, cz, cells, share, attempt);
					if (site == null || !site.equals(AdamOndiAhman.candidate(print, cx, cz, cells, share, attempt))) {
						throw helper.assertionException("Cell " + cx + " " + cz + " has no try " + attempt + ", or a different one each time");
					}
					helper.assertTrue(Math.floorMod(site.x(), 16) == 0 && Math.floorMod(site.z(), 16) == 0, "Site " + site + " is not a chunk's corner");
					for (int[] edge : new int[][] {{-reach, 0}, {reach, 0}, {0, -reach}, {0, reach}}) {
						helper.assertTrue(Math.floorDiv(site.x() + edge[0], cells) == cx && Math.floorDiv(site.z() + edge[1], cells) == cz,
								"The valley round " + site + " reaches out of its cell " + cx + " " + cz);
					}
					helper.assertTrue(AdamOndiAhman.inValley(site, site.x(), site.z())
							&& AdamOndiAhman.inValley(site, site.x() + AdamOndiAhman.RADIUS - AdamOndiAhman.WOBBLE, site.z())
							&& !AdamOndiAhman.inValley(site, site.x(), site.z() + reach + 1), "The valley round " + site + " is not where its radius says");
				}
			}
		}
		double fraction = with / (double) total;
		LOGGER.info("Adam-ondi-Ahman: {} of {} cells hold a candidate site ({}; setting {}); {} differ with the next seed", with, total,
				String.format("%.3f", fraction), AdamOndiAhman.CHANCE, differ);
		helper.assertTrue(Math.abs(fraction - AdamOndiAhman.CHANCE) < 0.05, "The share of cells with a site is " + fraction);
		helper.assertTrue(differ > total / 4, "Another seed changes only " + differ + " of " + total + " cells");
		helper.succeed();
	}

	/** A knoll needs a temperate inland climate: not the sea, the mountains, the desert, the frozen north, the jungle or a river's bed. */
	@GameTest
	public void climateSuitsAKnoll(GameTestHelper helper) {
		helper.assertTrue(AdamOndiAhman.climateSuits(0.1F, 0.0F, 0.3F, 0.2F, 0.5F), "Temperate inland land does not suit");
		List<String> refused = new ArrayList<>();
		// temperature, humidity, continentalness, erosion, weirdness
		for (Object[] land : new Object[][] {
				{"the sea", new float[] {0.1F, 0.0F, -0.5F, 0.2F, 0.5F}},
				{"the mountains", new float[] {0.1F, 0.0F, 0.3F, -0.8F, 0.5F}},
				{"the desert", new float[] {0.8F, -0.5F, 0.3F, 0.2F, 0.5F}},
				{"the frozen north", new float[] {-0.6F, 0.0F, 0.3F, 0.2F, 0.5F}},
				{"the jungle", new float[] {0.1F, 0.5F, 0.3F, 0.2F, 0.5F}},
				{"a river's bed", new float[] {0.1F, 0.0F, 0.3F, 0.2F, 0.0F}}}) {
			float[] c = (float[]) land[1];
			if (AdamOndiAhman.climateSuits(c[0], c[1], c[2], c[3], c[4])) {
				refused.add((String) land[0]);
			}
		}
		helper.assertTrue(refused.isEmpty(), "These suit a knoll: " + refused);
		helper.assertTrue(Math.abs(AdamOndiAhman.peaksAndValleys(0.0F) + 1.0F) < 1.0E-5F && Math.abs(AdamOndiAhman.peaksAndValleys(2.0F / 3.0F) - 1.0F) < 1.0E-6F,
				"Peaks and valleys are not vanilla's");
		helper.succeed();
	}
}
