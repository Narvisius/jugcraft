package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.agriculture.CropGrowth;
import io.github.jimbozoomer.jugcraft.agriculture.GourdCanteenItem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.ecology.EcologyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.ecology.EcologyParser;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Factor;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Fit;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Graft;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Habitat;
import io.github.jimbozoomer.jugcraft.concordance.ecology.HabitatInspection;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Niche;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.ecology.SampleBudget;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Sampler;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Verdict;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.HabitatGaugeBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 14, ecological cultivation (docs/features/arcane-concordance-ecology.md), on a real server: a bed
 * placed by someone who does not know Verdant Husbandry stays dormant until a Greenwarden's touch wakes it and the
 * beds joined to it; a crop grows only within its niche and says which factor holds it back; every step costs its bed
 * nutrients and dries it, so a depleted bed stops growth until bone meal or fertilizer feeds it; a nitrogen fixer
 * feeds the poorest bed round it; the Verdant Heart beats nutrients into Verdance and pours it into a Ley Pylon, and
 * stops on a starved bed; the Mulch Maw turns plant matter into nutrients for the poorest bed and stops eating when
 * every bed is full; the Habitat Gauge gives the habitat as a signal; the Gleaner harvests a ripe crop for a Verdance
 * drawn from a Heart its keeper may use, and from no one else's; a level takes at most 16 area samples a tick; and
 * beds and devices keep what they hold through a save. Since Concordance Chunk C (C-07): the reading the Inspect page
 * shows judges each factor as growth does and names its reach, the light as sky and lamps; a bed forgets its verdict once
 * nothing that notes it stands on it; an item held on a bed is its own use, and a canteen's sip wets a bed, by permission
 * and rate. Concordance Chunk C (C-08): a mixed bed speeds only another kind's company and mints nothing; every harvest path
 * strains a bed once, rest, compost and tending ease it and a fixer's gift does not, strain never supplies disturbance,
 * never kills, never reads a machine, and survives a save. Concordance Chunk C (C-09): a Greenwarden grafts Dewlit onto a
 * Sunpetal's bed, which every niche reader sees, every harvest path and a save keep, and another plant's growth clears; a
 * graft changes the niche and nothing else.
 */
public class ConcordanceGardenGameTests {
	private static final UUID KEEPER = new UUID(14L, 1L);

	private static ServerLevel level(GameTestHelper helper) {
		return helper.getLevel();
	}

	/** An awake, wet bed holding {@code nutrients}. */
	private static VerdantBedBlockEntity bed(GameTestHelper helper, BlockPos at, int nutrients) {
		helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
		VerdantBedBlockEntity bed = (VerdantBedBlockEntity) level(helper).getBlockEntity(helper.absolutePos(at));
		bed.awaken(KEEPER);
		bed.setNutrients(nutrients);
		return bed;
	}

	private static OrganismCropBlock.Growth grow(GameTestHelper helper, BlockPos at) {
		BlockState state = helper.getBlockState(at);
		return ((OrganismCropBlock) state.getBlock()).grow(level(helper), helper.absolutePos(at), state, level(helper).getRandom(), true);
	}

	private static int age(GameTestHelper helper, BlockPos at) {
		return helper.getBlockState(at).getValue(OrganismCropBlock.AGE);
	}

	/** Stone all round {@code at} and over it, so no light reaches it. */
	private static void shut(GameTestHelper helper, BlockPos at) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					helper.setBlock(at.offset(dx, 0, dz), Blocks.STONE);
				}
				helper.setBlock(at.offset(dx, 1, dz), Blocks.STONE);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T entity(GameTestHelper helper, BlockPos at) {
		return (T) level(helper).getBlockEntity(helper.absolutePos(at));
	}

	/** The top face of the block at {@code at}, as a use from above hits it. */
	private static BlockHitResult top(GameTestHelper helper, BlockPos at) {
		BlockPos absolute = helper.absolutePos(at);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
	}

	/**
	 * {@code player} uses {@code held}, put in the main hand, on the top of the block at {@code at} by the server's own path
	 * (ServerPlayerGameMode.useItemOn), plainly or sneaking, its rate limit forgotten first.
	 */
	private static InteractionResult handUse(GameTestHelper helper, ServerPlayer player, BlockPos at, ItemStack held, boolean sneaking) {
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		player.setShiftKeyDown(sneaking);
		try {
			return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, top(helper, at));
		} finally {
			player.setShiftKeyDown(false);
		}
	}

	/** One random tick of the block at {@code at}, as the level gives it. */
	private static void tickBed(GameTestHelper helper, BlockPos at) {
		helper.getBlockState(at).randomTick(level(helper), helper.absolutePos(at), level(helper).getRandom());
	}

	/** A copy of {@code bed} read back from {@code saved}: a save, or an update tag as a client reads it. */
	private static VerdantBedBlockEntity reload(GameTestHelper helper, VerdantBedBlockEntity bed, CompoundTag saved) {
		VerdantBedBlockEntity copy = new VerdantBedBlockEntity(bed.getBlockPos(), bed.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level(helper).registryAccess(), saved));
		return copy;
	}

	private static ItemStack canteen(int water) {
		ItemStack stack = new ItemStack(JugcraftAgriculture.item("gourd_canteen"));
		stack.set(JugcraftAgriculture.CANTEEN_WATER, water);
		return stack;
	}

	/** Reads each bed's area early (ticks 10, 20, 30): a level takes only so many samples a tick, and any one serves 200. */
	private static void prefetch(GameTestHelper helper, VerdantBedBlockEntity... beds) {
		for (int tick : new int[] {10, 20, 30}) {
			helper.runAtTickTime(tick, () -> {
				for (VerdantBedBlockEntity bed : beds) {
					bed.habitat(level(helper));
				}
			});
		}
	}

	/** Re-wets a bed (its block entity stays: the same block, another state). */
	private static void wet(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
	}

	private static void crop(GameTestHelper helper, BlockPos at, Block crop, int age) {
		helper.setBlock(at, ((OrganismCropBlock) crop).getStateForAge(age));
	}

	/** Grows the Sunpetal at {@code at} from step 2 to ripe, as growth does: its bed notes the ripening. */
	private static void ripen(GameTestHelper helper, BlockPos at, VerdantBedBlockEntity bed) {
		crop(helper, at, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE - 1);
		bed.setStrain(0);
		bed.setNutrients(10);
		helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.GREW && age(helper, at) == OrganismCropBlock.MAX_AGE
				&& bed.ripened() && bed.strain() == 0, "The crop at " + at + " ripens by growth, and its bed notes it");
	}

	/** A survival player at the test, with Verdant Husbandry understood when {@code greenwarden}, and no recent use. */
	private static ServerPlayer gardener(GameTestHelper helper, boolean greenwarden) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		if (greenwarden) {
			ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(player, Garden.RESEARCH, ResearchState.UNDERSTOOD);
		}
		RateGate.forget(player.getUUID());
		return player;
	}

	private static Organism organism(String id) {
		Organism organism = Garden.catalog().organism(id);
		if (organism == null) {
			throw new IllegalStateException("no organism " + id);
		}
		return organism;
	}

	/** Fills the energy store at {@code relative}, as JugcraftGameTests.charge does (its own is private). */
	private static void charge(GameTestHelper helper, BlockPos relative, Direction side) {
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(relative), side);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "No energy storage at " + relative + " on " + side);
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
	}

	/**
	 * After a harvest by {@code path}: the crop is below ripe or gone, its bed shows one strain not yet counted (notes and
	 * Jade), growth (replanted if need be) counts it once, and, the crop still growing (never ripe again here, so no later
	 * path can take it), its bed's random tick counts nothing more and rests nothing.
	 */
	private static void strained(GameTestHelper helper, BlockPos at, VerdantBedBlockEntity bed, String path) {
		BlockState state = helper.getBlockState(at);
		helper.assertTrue(!(state.getBlock() instanceof OrganismCropBlock c) || !c.isMaxAge(state), path + " took the ripe crop: " + state);
		helper.assertTrue(bed.strain() == 0 && bed.ripened() && bed.strainNow() == 1,
				path + ": a harvest not yet counted shows as one strain: " + bed.strain() + ", " + bed.strainNow());
		helper.assertTrue(bed.notes(null).contains(Text.of("ecology.strain", 1, Habitat.MAX_STRAIN)), path + ": recently harvested: " + bed.notes(null));
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			helper.assertTrue(ConcordanceDataProvider.bedSnapshot(bed, level(helper)).getIntOr("strain", 0) == 1, path + ": Jade is told the strain");
		}
		if (!(state.getBlock() instanceof OrganismCropBlock)) {
			helper.setBlock(at, Garden.SUNPETAL_CROP);
		}
		helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.GREW && bed.strain() == 1 && !bed.ripened(),
				path + ": the next growth counted it once: " + bed.strain());
		tickBed(helper, at.below());
		helper.assertTrue(bed.strain() == 1 && age(helper, at) < OrganismCropBlock.MAX_AGE, path + ": and nothing more: " + bed.strain());
	}

	/**
	 * With {@code near} two blocks east of the crop at {@code at} and {@code far} three: a fresh sample, its verdict noted;
	 * the speed is CropGrowth's for its pace, times 1.25 in a mixed bed and unchanged otherwise. Returns whether mixed.
	 */
	private static boolean mixedSpeed(GameTestHelper helper, BlockPos at, VerdantBedBlockEntity bed, Block near, Block far) {
		ServerLevel level = level(helper);
		helper.setBlock(at.offset(2, 0, 0), near);
		helper.setBlock(at.offset(3, 0, 0), far);
		wet(helper, at.below());
		bed.forgetSample();
		Habitat habitat = bed.habitat(level);
		OrganismCropBlock crop = (OrganismCropBlock) helper.getBlockState(at).getBlock();
		Organism definition = crop.organism();
		helper.assertTrue(habitat != null && definition != null, "The crop's bed is read");
		Verdict verdict = Verdict.of(definition.niche(), habitat);
		bed.note(verdict, habitat, definition);
		helper.assertTrue(verdict.growth() != Verdict.Growth.STALLED, "It can grow here: " + habitat + ", " + bed.reasons());
		BlockPos plant = helper.absolutePos(at);
		float base = CropGrowth.speed(level, plant, definition.fix() > 0) * verdict.growth().pace / 2.0F;
		float speed = crop.speed(level, plant, definition, verdict, bed);
		boolean mixed = bed.mixed(definition);
		helper.assertTrue(mixed ? Math.abs(speed - base * 1.25F) < 1.0E-4F : speed == base,
				(mixed ? "A mixed bed steps 25% more often: " : "No bonus: ") + speed + " for " + base);
		return mixed;
	}

	// ---------------------------------------------------------------- waking

	/**
	 * A bed placed by a player who has not understood Verdant Husbandry is dormant, and a crop on it does not grow; a
	 * Greenwarden's empty hand wakes it and the dormant beds joined to it, and no others.
	 */
	@GameTest(maxTicks = 40)
	public void aGreenwardenWakesTheBeds(GameTestHelper helper) {
		ServerPlayer novice = TestPlayers.mock(helper);
		novice.setGameMode(GameType.SURVIVAL);
		List<BlockPos> row = List.of(new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1));
		BlockPos apart = new BlockPos(6, 1, 1);
		for (BlockPos at : List.of(row.get(0), row.get(1), row.get(2), apart)) {
			helper.setBlock(at, Garden.VERDANT_BED);
			Garden.VERDANT_BED.setPlacedBy(level(helper), helper.absolutePos(at), helper.getBlockState(at), novice, ItemStack.EMPTY);
		}
		VerdantBedBlockEntity first = entity(helper, row.get(0));
		helper.assertTrue(!first.awake() && first.keeper() != null, "A novice's bed is dormant and remembers who placed it");
		helper.setBlock(row.get(0).above(), Garden.SUNPETAL_CROP);
		helper.assertTrue(grow(helper, row.get(0).above()) == OrganismCropBlock.Growth.DORMANT, "Nothing grows in a dormant bed");
		ServerPlayer greenwarden = TestPlayers.mock(helper);
		greenwarden.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(greenwarden, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(greenwarden, Garden.RESEARCH, ResearchState.UNDERSTOOD);
		helper.assertTrue(Garden.knows(greenwarden) && !Garden.knows(novice), "Only the Greenwarden knows Verdant Husbandry");
		RateGate.forget(greenwarden.getUUID());
		int woken = VerdantBedBlock.awaken(level(helper), helper.absolutePos(row.get(1)), greenwarden);
		helper.assertTrue(woken == 3, "One touch wakes the three joined beds: " + woken);
		for (BlockPos at : row) {
			helper.assertTrue(((VerdantBedBlockEntity) entity(helper, at)).awake(), "Bed " + at + " is awake");
		}
		helper.assertTrue(!((VerdantBedBlockEntity) entity(helper, apart)).awake(), "A bed not joined to them stays dormant");
		helper.succeed();
	}

	// ---------------------------------------------------------------- niches

	/**
	 * Two sunpetals on rich, wet, awake beds: the one lit by glowstone grows a step, paying a nutrient and drying its
	 * bed; the one shut in the dark does not, and its bed records why (too little light, with the reading and the
	 * least it needs), which its tooltip shows.
	 */
	@GameTest(maxTicks = 80)
	public void aCropGrowsOnlyWithinItsNiche(GameTestHelper helper) {
		BlockPos lit = new BlockPos(1, 1, 1);
		BlockPos dark = new BlockPos(5, 1, 5);
		VerdantBedBlockEntity litBed = bed(helper, lit, 10);
		VerdantBedBlockEntity darkBed = bed(helper, dark, 10);
		helper.setBlock(lit.above(), Garden.SUNPETAL_CROP);
		helper.setBlock(lit.above().north(), Blocks.GLOWSTONE);
		helper.setBlock(dark.above(), Garden.SUNPETAL_CROP);
		shut(helper, dark.above());
		// Give the light engine time to light (and darken) the new blocks. A vanilla random tick may have grown a step
		// or dried a bed meanwhile, so what follows is measured from the state just before.
		helper.runAtTickTime(40, () -> {
			ServerLevel level = level(helper);
			int light = level.getRawBrightness(helper.absolutePos(lit.above()), 0);
			int shade = level.getRawBrightness(helper.absolutePos(dark.above()), 0);
			helper.assertTrue(light >= 13 && shade <= 2, "Glowstone lights one (" + light + "), stone shuts out the other (" + shade + ")");
			int age = age(helper, lit.above());
			int nutrients = litBed.nutrients();
			int moisture = litBed.moisture();
			helper.assertTrue(age < OrganismCropBlock.MAX_AGE && grow(helper, lit.above()) == OrganismCropBlock.Growth.GREW
					&& age(helper, lit.above()) == age + 1, "The lit sunpetal grows a step");
			helper.assertTrue(litBed.nutrients() == nutrients - 1 && litBed.moisture() == Math.max(0, moisture - 1),
					"The step cost a nutrient and dried the bed: " + litBed.nutrients() + ", " + litBed.moisture());
			int darkNutrients = darkBed.nutrients();
			helper.assertTrue(grow(helper, dark.above()) == OrganismCropBlock.Growth.STALLED && age(helper, dark.above()) == 0,
					"The sunpetal in the dark does not grow");
			helper.assertTrue(darkBed.nutrients() == darkNutrients, "A stalled crop costs nothing");
			helper.assertTrue(darkBed.growth().equals("stalled") && !darkBed.reasons().isEmpty()
					&& darkBed.reasons().get(0).startsWith("ecology.short|light|") && darkBed.reasons().get(0).endsWith("|11"),
					"Its bed says why: too little light, needing at least 11: " + darkBed.reasons());
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- depleted inputs constrain output

	/**
	 * A sunpetal on a bed with one nutrient grows one step and then stops: the empty bed is outside its niche. Bone meal
	 * on the bed feeds it two; fertilizer feeds each crop's bed two a dose; and it grows again.
	 */
	@GameTest(maxTicks = 80)
	public void anEmptyBedStopsGrowth(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, at, 1);
		helper.setBlock(at.above(), Garden.SUNPETAL_CROP);
		helper.setBlock(at.above().east(), Blocks.GLOWSTONE);
		helper.runAtTickTime(40, () -> {
			// Set the bed again: a random tick may have grown a step meanwhile.
			helper.setBlock(at.above(), Garden.SUNPETAL_CROP);
			bed.setNutrients(1);
			helper.assertTrue(grow(helper, at.above()) == OrganismCropBlock.Growth.GREW && bed.nutrients() == 0, "One nutrient, one step");
			helper.assertTrue(grow(helper, at.above()) == OrganismCropBlock.Growth.STALLED && age(helper, at.above()) == 1,
					"The empty bed stops it");
			helper.assertTrue(bed.reasons().stream().anyMatch(reason -> reason.startsWith("ecology.short|nutrients|0|")),
					"and says it is short of nutrients: " + bed.reasons());
			helper.assertTrue(bed.give(Garden.BONE_MEAL_NUTRIENTS) == 2 && bed.nutrients() == 2, "Bone meal's worth feeds the bed");
			helper.assertTrue(grow(helper, at.above()) == OrganismCropBlock.Growth.GREW && age(helper, at.above()) == 2, "and it grows again");
			int fed = FertilizerItem.fertilize(level(helper), helper.absolutePos(at.above()));
			helper.assertTrue(fed == 1 && bed.nutrients() == 1 + 2 * Garden.BONE_MEAL_NUTRIENTS,
					"Fertilizer feeds the crop's bed two doses' worth, never a free step: " + fed + ", " + bed.nutrients());
			helper.assertTrue(age(helper, at.above()) == 2, "Fertilizer did not force a step");
			helper.succeed();
		});
	}

	/** A Mendvetch's step costs nothing and gives a nutrient to the poorest bed of its own and those beside it. */
	@GameTest(maxTicks = 80)
	public void aFixerFeedsThePoorestBed(GameTestHelper helper) {
		BlockPos centre = new BlockPos(3, 1, 3);
		VerdantBedBlockEntity own = bed(helper, centre, 4);
		VerdantBedBlockEntity east = bed(helper, centre.east(), 1);
		VerdantBedBlockEntity west = bed(helper, centre.west(), 6);
		helper.setBlock(centre.above(), Garden.MENDVETCH_CROP);
		helper.setBlock(centre.above().north(), Blocks.GLOWSTONE);
		helper.runAtTickTime(40, () -> {
			helper.setBlock(centre.above(), Garden.MENDVETCH_CROP);
			own.setNutrients(4);
			east.setNutrients(1);
			west.setNutrients(6);
			helper.assertTrue(grow(helper, centre.above()) == OrganismCropBlock.Growth.GREW, "The vetch grows");
			helper.assertTrue(own.nutrients() == 4 && east.nutrients() == 2 && west.nutrients() == 6,
					"Its nutrient went to the poorest bed: " + own.nutrients() + "/" + east.nutrients() + "/" + west.nutrients());
			helper.succeed();
		});
	}

	/**
	 * Roadmap step 29: a step hastened by a Verdant Spire grows by the crop's own rules but costs its bed at least a
	 * nutrient and fixes none. A Mendvetch, free and fixing when time grows it, pays one and feeds no neighbour; on an
	 * empty bed nothing is hastened.
	 */
	@GameTest(maxTicks = 80)
	public void aHastenedStepPaysAndFixesNothing(GameTestHelper helper) {
		BlockPos centre = new BlockPos(3, 1, 3);
		VerdantBedBlockEntity own = bed(helper, centre, 4);
		VerdantBedBlockEntity east = bed(helper, centre.east(), 1);
		VerdantBedBlockEntity empty = bed(helper, new BlockPos(6, 1, 6), 0);
		helper.setBlock(centre.above(), Garden.MENDVETCH_CROP);
		helper.setBlock(centre.above().north(), Blocks.GLOWSTONE);
		helper.setBlock(new BlockPos(6, 2, 6), Garden.MENDVETCH_CROP);
		helper.setBlock(new BlockPos(6, 2, 5), Blocks.GLOWSTONE);
		helper.runAtTickTime(40, () -> {
			ServerLevel level = level(helper);
			helper.setBlock(centre.above(), Garden.MENDVETCH_CROP);
			helper.setBlock(new BlockPos(6, 2, 6), Garden.MENDVETCH_CROP);
			own.setNutrients(4);
			east.setNutrients(1);
			empty.setNutrients(0);
			BlockState vetch = helper.getBlockState(centre.above());
			helper.assertTrue(((OrganismCropBlock) vetch.getBlock()).hasten(level, helper.absolutePos(centre.above()), vetch, level.getRandom())
					== OrganismCropBlock.Growth.GREW && age(helper, centre.above()) == 1, "The vetch is hastened a step");
			helper.assertTrue(own.nutrients() == 4 - OrganismCropBlock.HASTENED_COST && east.nutrients() == 1,
					"It paid its bed a nutrient and fed no neighbour: " + own.nutrients() + "/" + east.nutrients());
			BlockState starved = helper.getBlockState(new BlockPos(6, 2, 6));
			helper.assertTrue(((OrganismCropBlock) starved.getBlock()).hasten(level, helper.absolutePos(new BlockPos(6, 2, 6)), starved,
					level.getRandom()) == OrganismCropBlock.Growth.STALLED && age(helper, new BlockPos(6, 2, 6)) == 0 && empty.nutrients() == 0,
					"On an empty bed nothing is hastened");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the producer

	/**
	 * A Verdant Heart on a lit, fed bed among three other kinds of crop beats a nutrient into Verdance; beside a Ley Pylon
	 * it pours whole batches (3 Verdance for 2 Ley Charge); on a starved bed it stops and spends nothing.
	 */
	@GameTest(maxTicks = 80)
	public void theHeartBeatsVerdanceIntoAPylon(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		VerdantBedBlockEntity bed = bed(helper, at, 10);
		helper.setBlock(at.above(), Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = entity(helper, at.above());
		helper.setBlock(at.above().north(), Blocks.GLOWSTONE);
		// Company: three kinds of crop round it.
		Block[] kinds = {Garden.SUNPETAL_CROP, Garden.DEWMOSS_CROP, Garden.MENDVETCH_CROP};
		for (int i = 0; i < kinds.length; i++) {
			BlockPos spot = at.offset(i - 1, 0, 2);
			bed(helper, spot, 0);
			helper.setBlock(spot.above(), kinds[i]);
		}
		helper.setBlock(at.above().east(), JugcraftConcordance.LEY_PYLON);
		LeyPylonBlockEntity pylon = entity(helper, at.above().east());
		helper.runAtTickTime(40, () -> {
			ServerLevel level = level(helper);
			// Woken only now, so its own pulse has not beaten before these do.
			heart.awaken(KEEPER);
			bed.setNutrients(10);
			bed.forgetSample();
			Habitat habitat = bed.habitat(level);
			helper.assertTrue(habitat != null && habitat.diversity() == 3 && habitat.disturbance() == 4,
					"Three kinds round it; the Heart and the pylon disturb it by 4: " + habitat);
			helper.assertTrue(heart.beat(level).equals("working") && heart.verdance() == 2 && bed.nutrients() == 9,
					"A thriving beat: a nutrient for 2 Verdance: " + heart.verdance() + ", " + bed.nutrients());
			helper.assertTrue(pylon.ley() == 0, "Two Verdance is not a whole batch for the pylon");
			heart.beat(level);
			helper.assertTrue(pylon.ley() == 2 && heart.verdance() == 1 && bed.nutrients() == 8,
					"Four Verdance pours one batch: 3 for 2 Ley Charge: " + pylon.ley() + ", " + heart.verdance());
			bed.setNutrients(0);
			helper.assertTrue(heart.beat(level).equals("stalled") && heart.verdance() == 1, "On a starved bed it makes nothing");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the consumer

	/**
	 * Fed Verdant Chaff (two quarters each), a Mulch Maw eats one a pulse and gives each whole nutrient to the poorest
	 * bed in reach; with every bed full it holds what it digested and eats no more.
	 */
	@GameTest(maxTicks = 40)
	public void theMawFeedsThePoorestBed(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		helper.setBlock(at, Garden.MULCH_MAW);
		MulchMawBlockEntity maw = entity(helper, at);
		maw.awaken(KEEPER);
		VerdantBedBlockEntity rich = bed(helper, at.offset(2, 0, 0), 5);
		VerdantBedBlockEntity poor = bed(helper, at.offset(-2, 0, 0), 3);
		helper.assertTrue(maw.feed(new ItemStack(Garden.VERDANT_CHAFF, 4)) == 4, "It takes the chaff");
		helper.assertTrue(maw.feed(new ItemStack(Blocks.STONE)) == 0, "but not stone");
		ServerLevel level = level(helper);
		helper.assertTrue(maw.digest(level).equals("working") && maw.held() == 2 && poor.nutrients() == 3, "Half a nutrient digested");
		maw.digest(level);
		helper.assertTrue(maw.held() == 0 && poor.nutrients() == 4 && rich.nutrients() == 5,
				"A whole one goes to the poorest bed: " + poor.nutrients() + "/" + rich.nutrients());
		rich.setNutrients(VerdantBedBlockEntity.CAPACITY);
		poor.setNutrients(VerdantBedBlockEntity.CAPACITY);
		maw.digest(level);
		maw.digest(level);
		helper.assertTrue(maw.digest(level).equals("beds_full") && maw.held() == 4 && maw.food().isEmpty(),
				"With every bed full it keeps its four quarters and has eaten the rest: " + maw.held());
		helper.succeed();
	}

	// ---------------------------------------------------------------- the sensor

	/** The gauge gives the bed's moisture as a signal, and judges an attuned crop: 0 in the dark. */
	@GameTest(maxTicks = 80)
	public void theGaugeReadsTheHabitat(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		bed(helper, at, 10);
		helper.setBlock(at.above(), Garden.HABITAT_GAUGE);
		HabitatGaugeBlockEntity gauge = entity(helper, at.above());
		gauge.awaken(KEEPER);
		shut(helper, at.above());
		helper.runAtTickTime(40, () -> {
			ServerLevel level = level(helper);
			gauge.cycle(level);
			int moisture = helper.getBlockState(at).getValue(VerdantBedBlock.MOISTURE);
			helper.assertTrue(gauge.mode() == HabitatGaugeBlockEntity.Mode.MOISTURE && gauge.signal() == moisture * 15 / 7
					&& gauge.signal() > 0, "Reading moisture, the bed's " + moisture + " gives " + gauge.signal());
			helper.assertTrue(helper.getBlockState(at.above()).getValue(io.github.jimbozoomer.jugcraft.concordance.garden.HabitatGaugeBlock.OPEN),
					"The bulb opens while it gives a signal");
			gauge.attune(level, "jugcraft:sunpetal");
			helper.assertTrue(gauge.mode() == HabitatGaugeBlockEntity.Mode.SUITABILITY && gauge.signal() == 0,
					"A sunpetal would not grow in the dark: 0");
			gauge.attune(level, "jugcraft:gloamcap");
			helper.assertTrue(gauge.signal() == 0, "Nor a gloamcap with no magic near it");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the collector

	/**
	 * A Gleaner harvests a ripe sunpetal within reach for one Verdance, drawn from a Verdant Heart its keeper shares;
	 * the crop falls back to step 1 to grow again. A Gleaner kept by someone else cannot draw from that Heart. It
	 * harvests as its keeper, who is here (roadmap step 28).
	 */
	@GameTest(maxTicks = 40)
	public void theGleanerHarvestsForVerdance(GameTestHelper helper) {
		ServerLevel level = level(helper);
		ServerPlayer keeper = TestPlayers.mock(helper);
		BlockPos crop = new BlockPos(2, 2, 2);
		bed(helper, crop.below(), 10);
		helper.setBlock(crop, ((OrganismCropBlock) Garden.SUNPETAL_CROP).getStateForAge(OrganismCropBlock.MAX_AGE));
		BlockPos heartBed = new BlockPos(5, 1, 2);
		bed(helper, heartBed, 10);
		helper.setBlock(heartBed.above(), Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = entity(helper, heartBed.above());
		heart.awaken(keeper.getUUID());
		heart.setVerdance(5);
		BlockPos stranger = new BlockPos(7, 2, 5);
		helper.setBlock(stranger, Garden.GLEANER);
		GleanerBlockEntity theirs = entity(helper, stranger);
		theirs.awaken(TestPlayers.mock(helper).getUUID());
		helper.assertTrue(theirs.glean(level).equals("no_verdance") && heart.verdance() == 5,
				"A stranger's Gleaner may not draw on the Heart");
		BlockPos at = new BlockPos(3, 2, 3);
		helper.setBlock(at, Garden.GLEANER);
		GleanerBlockEntity gleaner = entity(helper, at);
		gleaner.awaken(keeper.getUUID());
		helper.assertTrue(gleaner.glean(level).equals("working"), "It gleans: " + gleaner.status());
		helper.assertTrue(heart.verdance() == 0 && gleaner.verdance() == 4, "It drew the Heart's 5 and spent one: " + gleaner.verdance());
		List<ItemStack> held = new ArrayList<>();
		for (int i = 0; i < gleaner.getContainerSize(); i++) {
			if (!gleaner.getItem(i).isEmpty()) {
				held.add(gleaner.getItem(i));
			}
		}
		helper.assertTrue(held.size() == 2 && held.get(0).is(Garden.SUNPETAL) && held.get(0).getCount() == 2
				&& held.get(1).is(Garden.VERDANT_CHAFF), "Two sunpetals and a chaff: " + held);
		helper.assertTrue(age(helper, crop) == 1, "The crop falls back to step 1");
		helper.assertTrue(gleaner.glean(level).equals("idle"), "Nothing else is ripe");
		helper.succeed();
	}

	/**
	 * Roadmap step 28: a Gleaner whose keeper is away waits and draws nothing; with the server's option a stand-in
	 * answers for them, and harvests only a crop they could harvest by hand (a neighbour's claim refuses it).
	 */
	@GameTest(maxTicks = 40)
	public void theGleanerWaitsForItsKeeper(GameTestHelper helper) {
		ServerLevel level = level(helper);
		BlockPos crop = new BlockPos(2, 2, 2);
		bed(helper, crop.below(), 10);
		helper.setBlock(crop, ((OrganismCropBlock) Garden.SUNPETAL_CROP).getStateForAge(OrganismCropBlock.MAX_AGE));
		BlockPos heartBed = new BlockPos(5, 1, 2);
		bed(helper, heartBed, 10);
		helper.setBlock(heartBed.above(), Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = entity(helper, heartBed.above());
		heart.awaken(KEEPER);
		heart.setVerdance(5);
		BlockPos at = new BlockPos(3, 2, 3);
		helper.setBlock(at, Garden.GLEANER);
		GleanerBlockEntity gleaner = entity(helper, at);
		gleaner.awaken(KEEPER);
		helper.assertValueEqual(gleaner.glean(level), "keeper_away", "its keeper away, it waits");
		helper.assertTrue(heart.verdance() == 5 && gleaner.verdance() == 0 && age(helper, crop) == OrganismCropBlock.MAX_AGE,
				"and draws and harvests nothing");
		JugcraftConfig.setOption(Authority.ABSENT_OPTION, true);
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(crop), new UUID(14L, 3L));
			helper.assertValueEqual(gleaner.glean(level), "idle", "a stand-in answers for its keeper: a neighbour's claimed crop is refused");
			helper.assertTrue(age(helper, crop) == OrganismCropBlock.MAX_AGE, "and left ripe");
			claims.close();
			helper.assertValueEqual(gleaner.glean(level), "working", "unclaimed, it is harvested for its keeper");
		} finally {
			JugcraftConfig.setOption(Authority.ABSENT_OPTION, false);
		}
		helper.assertTrue(age(helper, crop) == 1, "the crop falls back to step 1");
		helper.succeed();
	}

	// ---------------------------------------------------------------- budget and saving

	/**
	 * Twenty beds asking for a fresh reading in one tick get at most 16 samples between them (the level's allowance);
	 * the rest keep waiting and are read in the next ticks.
	 */
	@GameTest(maxTicks = 40)
	public void samplingStaysWithinTheBudget(GameTestHelper helper) {
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			beds.add(bed(helper, new BlockPos(i % 5, 1, i / 5), 5));
		}
		helper.runAtTickTime(5, () -> {
			ServerLevel level = level(helper);
			int read = 0;
			for (VerdantBedBlockEntity bed : beds) {
				bed.forgetSample();
			}
			for (VerdantBedBlockEntity bed : beds) {
				if (bed.habitat(level) != null) {
					read++;
				}
			}
			int used = Garden.budget(level).used(level.getGameTime());
			helper.assertTrue(read <= SampleBudget.PER_TICK && used <= SampleBudget.PER_TICK && read < beds.size(),
					"At most 16 samples this tick: " + read + " read, " + used + " used");
			// Concordance Chunk C (C-08): a sample still reads exactly 100 positions; the kinds a mixed bed reads are the 25 it
			// already read for diversity.
			int[] calls = {0};
			Sampler.Area every = Sampler.sample(new Sampler.Probe() {
				@Override
				public String species(int dx, int dz) {
					calls[0]++;
					return "test:kind_" + dx + "_" + dz;
				}

				@Override
				public int disturbance(int dx, int dy, int dz) {
					calls[0]++;
					return 0;
				}
			});
			helper.assertTrue(calls[0] == Sampler.READS && Sampler.READS == 100 && every.reads() == 100 && every.kinds().size() == 25
					&& every.diversity() == Sampler.MAX_DIVERSITY, "Still 100 reads, and its 25 kinds: " + calls[0] + ", " + every.kinds().size());
		});
		helper.succeedWhen(() -> {
			ServerLevel level = level(helper);
			for (VerdantBedBlockEntity bed : beds) {
				helper.assertTrue(bed.habitat(level) != null, "Every bed is read in a later tick");
			}
		});
	}

	/** A bed keeps its nutrients and whether it is awake through a save; a Heart its Verdance. */
	@GameTest
	public void bedsAndHeartsKeepWhatTheyHold(GameTestHelper helper) {
		ServerLevel level = level(helper);
		BlockPos at = new BlockPos(2, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, at, 17);
		helper.setBlock(at.above(), Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = entity(helper, at.above());
		heart.awaken(KEEPER);
		heart.setVerdance(9);
		CompoundTag savedBed = bed.saveWithoutMetadata(level.registryAccess());
		VerdantBedBlockEntity bedCopy = new VerdantBedBlockEntity(bed.getBlockPos(), bed.getBlockState());
		bedCopy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedBed));
		helper.assertTrue(bedCopy.nutrients() == 17 && bedCopy.awake() && KEEPER.equals(bedCopy.keeper()), "The bed is saved exactly");
		CompoundTag savedHeart = heart.saveWithoutMetadata(level.registryAccess());
		VerdantHeartBlockEntity heartCopy = new VerdantHeartBlockEntity(heart.getBlockPos(), heart.getBlockState());
		heartCopy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), savedHeart));
		helper.assertTrue(heartCopy.verdance() == 9 && heartCopy.awake(), "The Heart keeps its Verdance");
		helper.succeed();
	}

	// ---------------------------------------------------------------- habitat made visible (Concordance Chunk C, C-07)

	/**
	 * C-07 (C-P14, C-P54): the reading the Inspect page shows. A sunpetal walled and roofed in stone and lit by one glowstone
	 * reads its light from lamps alone, and its habitat's light is the greater of sky and lamps; a dewmoss 2 blocks away counts
	 * for diversity and a gloamcap 3 away does not; a Ley Pylon in the 5 by 5 disturbs it by 2 and one 3 away does not. Each
	 * factor is judged against the sunpetal's own range by the verdict growth reaches from the same reading and names how
	 * far it reaches; reading takes, dries and notes nothing.
	 */
	@GameTest(maxTicks = 80)
	public void aBedSaysWhatItsPlantNeedsAndHowFar(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		VerdantBedBlockEntity bed = bed(helper, at, 10);
		helper.setBlock(at.above(), Garden.SUNPETAL_CROP);
		shut(helper, at.above());
		helper.setBlock(at.above().north(), Blocks.GLOWSTONE);
		bed(helper, new BlockPos(5, 1, 3), 0);
		helper.setBlock(new BlockPos(5, 2, 3), Garden.DEWMOSS_CROP);
		bed(helper, new BlockPos(6, 1, 3), 0);
		helper.setBlock(new BlockPos(6, 2, 3), Garden.GLOAMCAP_CROP);
		helper.setBlock(new BlockPos(1, 2, 3), JugcraftConcordance.LEY_PYLON);
		helper.setBlock(new BlockPos(0, 2, 3), JugcraftConcordance.LEY_PYLON);
		// The area is built now and never changes: its sample is taken early, away from the other tests' sampling ticks.
		prefetch(helper, bed);
		helper.runAtTickTime(44, () -> {
			ServerLevel level = level(helper);
			helper.setBlock(at.above(), Garden.SUNPETAL_CROP); // a random tick may have grown it meanwhile
			bed.setNutrients(10);
			bed.note("waiting", List.of()); // so a reading that noted would be caught
			BlockPos plant = helper.absolutePos(at.above());
			int moisture = bed.moisture();
			HabitatInspection.State state = bed.inspect(level);
			helper.assertTrue(state != null, "The bed is read");
			Habitat habitat = state.habitat();
			helper.assertTrue(state.sky() <= 2 && state.lamps() >= 13 && state.sky() == level.getBrightness(LightLayer.SKY, plant)
					&& state.lamps() == level.getBrightness(LightLayer.BLOCK, plant) && habitat.light() == Math.max(state.sky(), state.lamps()),
					"Walled and roofed, its light is the glowstone's: sky " + state.sky() + ", lamps " + state.lamps() + ", light " + habitat.light());
			helper.assertTrue(habitat.diversity() == 2 && habitat.disturbance() == 2,
					"The dewmoss 2 away counts, the gloamcap 3 away not; the pylon in the 5 by 5 disturbs it by 2, the one 3 away not: " + habitat);
			helper.assertTrue(bed.nutrients() == 10 && bed.moisture() == moisture && bed.growth().equals("waiting") && age(helper, at.above()) == 0,
					"Reading takes, dries and notes nothing");
			Organism sunpetal = Garden.catalog().organism("jugcraft:sunpetal");
			Verdict verdict = Verdict.of(sunpetal.niche(), habitat);
			List<Text> lines = HabitatInspection.judged(state, sunpetal.niche(), verdict);
			helper.assertTrue(lines.size() == Factor.values().length, "A line for each factor");
			for (Factor factor : Factor.values()) {
				Text row = lines.get(factor.ordinal());
				Text said = (Text) row.args().get(0);
				Text judged = (Text) row.args().get(1);
				helper.assertTrue(row.key().equals("ecology.inspect.judged") && said.key().equals("ecology.inspect." + factor.id)
						&& said.args().get(0).equals(habitat.get(factor)) && judged.key().equals("ecology.inspect." + verdict.fit(factor).id),
						factor.id + " is judged as growth judges it: " + row);
			}
			helper.assertValueEqual(((Text) lines.get(Factor.LIGHT.ordinal()).args().get(0)).args(),
					List.<Object>of(habitat.light(), state.sky(), state.lamps()), "light as sky and lamps");
			helper.assertValueEqual(((Text) lines.get(Factor.DIVERSITY.ordinal()).args().get(0)).args(), List.<Object>of(2, Sampler.RADIUS),
					"diversity names its reach");
			helper.assertValueEqual(((Text) lines.get(Factor.DISTURBANCE.ordinal()).args().get(0)).args(), List.<Object>of(2, 5, 5, Sampler.HEIGHT),
					"disturbance names its reach");
			helper.assertValueEqual(((Text) lines.get(Factor.MOISTURE.ordinal()).args().get(0)).args(), List.<Object>of(moisture, 7, Garden.WATER_REACH),
					"moisture is the bed's own and names the water's reach");
			Text diversity = (Text) lines.get(Factor.DIVERSITY.ordinal()).args().get(1);
			helper.assertTrue(diversity.key().equals("ecology.inspect.tolerable") && diversity.args().equals(List.of(3, 8, 1, 8)),
					"Two kinds are only tolerable, best 3 to 8: " + diversity);
			Text light = (Text) lines.get(Factor.LIGHT.ordinal()).args().get(1);
			helper.assertTrue(light.key().equals("ecology.inspect.ideal") && light.args().equals(List.of(13, 15, 11, 15)), "Lamps alone light it well: " + light);
			Text said = HabitatInspection.verdict(verdict);
			helper.assertTrue(verdict.growth() == Verdict.Growth.TOLERATING && said.key().equals("ecology.inspect.verdict")
					&& ((Text) said.args().get(0)).key().equals("ecology.growth.tolerating"), "Tolerating, for its diversity: " + said);
			helper.assertTrue(grow(helper, at.above()) != OrganismCropBlock.Growth.WAITING && bed.growth().equals(verdict.growth().id),
					"Growth reaches the same verdict from the same reading: " + bed.growth());
			helper.succeed();
		});
	}

	/**
	 * C-07 (C-P55): a bed's verdict, sent to clients for its crop's tooltip, stays until the bed's next random tick and then
	 * clears once nothing that notes it stands on it, synced and saved cleared; a Verdant Heart's note stays (the Heart notes
	 * its bed too); a crop planted again notes it again. A step of the same crop keeps the verdict; another plant set on the
	 * bed forgets it at once (the unit review), so Jade never shows one plant's reasons on another.
	 */
	@GameTest(maxTicks = 80)
	public void aVerdictClearsWhenThePlantGoes(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, at, 10);
		helper.setBlock(at.above(), Garden.SUNPETAL_CROP);
		shut(helper, at.above());
		BlockPos heartAt = new BlockPos(6, 1, 6);
		VerdantBedBlockEntity heartBed = bed(helper, heartAt, 10);
		helper.setBlock(heartAt.above(), Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = entity(helper, heartAt.above());
		heart.awaken(KEEPER);
		prefetch(helper, bed, heartBed);
		helper.runAtTickTime(45, () -> {
			ServerLevel level = level(helper);
			helper.assertTrue(grow(helper, at.above()) == OrganismCropBlock.Growth.STALLED && bed.growth().equals("stalled") && !bed.reasons().isEmpty(),
					"The sunpetal in the dark stalls and its bed says why: " + bed.reasons());
			helper.setBlock(at.above(), Blocks.AIR);
			helper.assertTrue(bed.growth().equals("stalled"), "Until the bed's next random tick the verdict stays");
			tickBed(helper, at);
			helper.assertTrue(bed.growth().isEmpty() && bed.reasons().isEmpty(), "With nothing on it, its random tick forgets the verdict");
			CompoundTag sent = bed.getUpdateTag(level.registryAccess());
			helper.assertTrue(sent.getStringOr("growth", "?").isEmpty() && sent.getStringOr("reasons", "?").isEmpty(), "Clients are sent none: " + sent);
			VerdantBedBlockEntity copy = reload(helper, bed, bed.saveWithoutMetadata(level.registryAccess()));
			helper.assertTrue(copy.growth().isEmpty() && copy.reasons().isEmpty() && copy.nutrients() == bed.nutrients(), "and it is saved cleared");
			heart.beat(level);
			String noted = heartBed.growth();
			tickBed(helper, heartAt);
			helper.assertTrue(!noted.isEmpty() && heartBed.growth().equals(noted), "A Verdant Heart's note stays: " + noted);
			helper.setBlock(at.above(), Garden.SUNPETAL_CROP);
			helper.assertTrue(grow(helper, at.above()) == OrganismCropBlock.Growth.STALLED && bed.growth().equals("stalled"), "Planted again, it is noted again");
			crop(helper, at.above(), Garden.SUNPETAL_CROP, 2);
			helper.assertTrue(bed.growth().equals("stalled"), "A step of the same crop keeps its verdict");
			helper.setBlock(at.above(), Garden.DEWMOSS_CROP);
			helper.assertTrue(bed.growth().isEmpty() && bed.reasons().isEmpty(), "Another plant set there forgets the last one's verdict at once");
			helper.succeed();
		});
	}

	/**
	 * C-07 (C-P15, C-P56): an item held on a Verdant Bed is its own use. A Gourd Canteen's sip wets a dry bed to 7, by a plain
	 * use and a sneaking one alike; a wet bed takes no sip; two sips in one tick are rate-limited to one; a neighbour's claim
	 * refuses it and spends nothing; and a seed in hand plants on the bed without sneaking.
	 */
	@GameTest(maxTicks = 20)
	public void aCanteenWatersABed(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		BlockPos second = new BlockPos(4, 1, 2);
		helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, 0));
		ServerPlayer gardener = TestPlayers.mock(helper);
		gardener.setGameMode(GameType.SURVIVAL);
		ItemStack sips = canteen(3);
		helper.assertTrue(handUse(helper, gardener, at, sips, false).consumesAction(), "A plain use pours: the bed passes a held item on");
		helper.assertTrue(helper.getBlockState(at).getValue(VerdantBedBlock.MOISTURE) == VerdantBedBlock.WET && GourdCanteenItem.water(sips) == 2,
				"A sip wets the dry bed to 7: " + helper.getBlockState(at));
		handUse(helper, gardener, at, sips, false);
		helper.assertTrue(GourdCanteenItem.water(sips) == 2, "A wet bed takes no sip");
		helper.setBlock(at, helper.getBlockState(at).setValue(VerdantBedBlock.MOISTURE, 1));
		handUse(helper, gardener, at, sips, true);
		helper.assertTrue(helper.getBlockState(at).getValue(VerdantBedBlock.MOISTURE) == VerdantBedBlock.WET && GourdCanteenItem.water(sips) == 1,
				"A sneaking use pours too");
		ItemStack full = canteen(3);
		helper.setBlock(at, helper.getBlockState(at).setValue(VerdantBedBlock.MOISTURE, 0));
		helper.setBlock(second, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, 0));
		handUse(helper, gardener, at, full, false);
		gardener.gameMode.useItemOn(gardener, helper.getLevel(), full, InteractionHand.MAIN_HAND, top(helper, second));
		helper.assertTrue(helper.getBlockState(second).getValue(VerdantBedBlock.MOISTURE) == 0 && GourdCanteenItem.water(full) == 2,
				"A second sip in the same tick is refused and spends nothing");
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(second), new UUID(14L, 9L));
			helper.assertTrue(!handUse(helper, gardener, second, full, false).consumesAction()
					&& helper.getBlockState(second).getValue(VerdantBedBlock.MOISTURE) == 0 && GourdCanteenItem.water(full) == 2,
					"A claimed bed refuses the sip and spends nothing");
		}
		ItemStack seed = new ItemStack(Garden.SUNPETAL, 2);
		handUse(helper, gardener, at, seed, false);
		helper.assertTrue(helper.getBlockState(at.above()).is(Garden.SUNPETAL_CROP) && seed.getCount() == 1, "A seed in hand plants on the bed, not sneaking");
		helper.succeed();
	}

	// ---------------------------------------------------------------- a mixed bed and harvest strain (Concordance Chunk C, C-08)

	/**
	 * C-08 (C-P16, C-P58): a Dewmoss in the dark steps 25% more often with a Gloamcap two blocks off, and only then: not
	 * alone, not beside another Dewmoss (copies never count), a Sunpetal or a Mendvetch, nor with a Gloamcap three blocks
	 * off, outside the 5 by 5. Each way: the Gloamcap has the Dewmoss for company. No Mendvetch or Sunpetal pairing.
	 */
	@GameTest(maxTicks = 80)
	public void aMixedBedGrowsFasterOnlyForAnotherKind(GameTestHelper helper) {
		BlockPos moss = new BlockPos(2, 2, 2);
		VerdantBedBlockEntity bed = bed(helper, moss.below(), 10);
		VerdantBedBlockEntity nearBed = bed(helper, moss.offset(2, -1, 0), 10);
		bed(helper, moss.offset(3, -1, 0), 10);
		helper.setBlock(moss, Garden.DEWMOSS_CROP);
		shut(helper, moss);
		// One fresh sample a tick (41 to 47): the area changes between them, and each reading is asserted present.
		List<Block> unmixed = List.of(Blocks.AIR, Garden.DEWMOSS_CROP, Garden.SUNPETAL_CROP, Garden.MENDVETCH_CROP);
		for (int i = 0; i < unmixed.size(); i++) {
			Block other = unmixed.get(i);
			helper.runAtTickTime(41 + i, () -> helper.assertTrue(!mixedSpeed(helper, moss, bed, other, Blocks.AIR),
					other + " two blocks off makes no mixed bed for a Dewmoss"));
		}
		helper.runAtTickTime(45, () -> helper.assertTrue(!mixedSpeed(helper, moss, bed, Blocks.AIR, Garden.GLOAMCAP_CROP),
				"A Gloamcap three blocks off is outside the 5x5"));
		helper.runAtTickTime(46, () -> {
			helper.assertTrue(mixedSpeed(helper, moss, bed, Garden.GLOAMCAP_CROP, Blocks.AIR), "A Gloamcap two blocks off makes a mixed bed");
			helper.assertTrue(bed.reasons().contains("ecology.mixed|25"), "and the bed says so: " + bed.reasons());
		});
		helper.runAtTickTime(47, () -> {
			nearBed.forgetSample();
			helper.assertTrue(nearBed.habitat(level(helper)) != null, "The Gloamcap's bed is read");
			Organism gloamcap = organism("jugcraft:gloamcap");
			helper.assertTrue(nearBed.mixed(gloamcap) && gloamcap.neighbours().percent() == 25, "Each way: the Gloamcap has the Dewmoss for company");
			EcologyCatalog catalog = Garden.catalog();
			Set<String> every = Set.of("jugcraft:sunpetal_crop", "jugcraft:dewmoss_crop", "jugcraft:gloamcap_crop", "jugcraft:mendvetch_crop");
			Organism mendvetch = organism("jugcraft:mendvetch");
			Organism sunpetal = organism("jugcraft:sunpetal");
			helper.assertTrue(!catalog.mixed(mendvetch, every) && !catalog.mixed(sunpetal, every) && !mendvetch.neighbours().any()
					&& !sunpetal.neighbours().any(), "No Mendvetch or Sunpetal pairing (C-P16)");
			helper.assertTrue(!catalog.mixed(gloamcap, Set.of("jugcraft:gloamcap_crop")), "A Gloamcap's own kind never counts");
			helper.succeed();
		});
	}

	/**
	 * C-08 (C-P58): a Dewmoss and a Gloamcap two blocks apart, each the other's company, grow ripe step by step: every step
	 * costs its crop's own price and nothing is made; both beds say mixed bed; ripe and unharvested, neither is strained.
	 */
	@GameTest(maxTicks = 80)
	public void theBonusSpendsNoExtraNutrientAndMintsNone(GameTestHelper helper) {
		BlockPos moss = new BlockPos(2, 2, 2);
		BlockPos cap = new BlockPos(4, 2, 2);
		VerdantBedBlockEntity mossBed = bed(helper, moss.below(), 10);
		VerdantBedBlockEntity capBed = bed(helper, cap.below(), 10);
		helper.setBlock(moss, Garden.DEWMOSS_CROP);
		helper.setBlock(cap, Garden.GLOAMCAP_CROP);
		shut(helper, moss);
		shut(helper, cap);
		// Working magic for the Gloamcap alone: three budding amethyst (1 each) beside its bed, outside the Dewmoss's 5x5.
		for (int dz = -1; dz <= 1; dz++) {
			helper.setBlock(cap.offset(1, -1, dz), Blocks.BUDDING_AMETHYST);
		}
		prefetch(helper, mossBed, capBed);
		helper.runAtTickTime(49, () -> {
			crop(helper, moss, Garden.DEWMOSS_CROP, 0);
			crop(helper, cap, Garden.GLOAMCAP_CROP, 0);
			wet(helper, moss.below());
			wet(helper, cap.below());
			for (VerdantBedBlockEntity bed : List.of(mossBed, capBed)) {
				bed.setNutrients(10);
				bed.setStrain(0);
			}
			Organism dewmoss = organism("jugcraft:dewmoss");
			Organism gloamcap = organism("jugcraft:gloamcap");
			helper.assertTrue(mossBed.mixed(dewmoss) && capBed.mixed(gloamcap), "Each is the other's company");
			int before = mossBed.nutrients() + capBed.nutrients();
			for (int step = 1; step <= OrganismCropBlock.MAX_AGE; step++) {
				helper.assertTrue(grow(helper, moss) == OrganismCropBlock.Growth.GREW && grow(helper, cap) == OrganismCropBlock.Growth.GREW,
						"Both grow step " + step + ": " + mossBed.reasons() + " / " + capBed.reasons());
			}
			helper.assertTrue(mossBed.nutrients() == 10 - OrganismCropBlock.MAX_AGE * dewmoss.cost()
					&& capBed.nutrients() == 10 - OrganismCropBlock.MAX_AGE * gloamcap.cost(),
					"Every step cost its crop's own price: " + mossBed.nutrients() + ", " + capBed.nutrients());
			helper.assertTrue(mossBed.nutrients() + capBed.nutrients() == before - 9, "and nothing was made");
			helper.assertTrue(mossBed.reasons().contains("ecology.mixed|25") && capBed.reasons().contains("ecology.mixed|25"), "Both beds say mixed bed");
			helper.assertTrue(mossBed.ripened() && capBed.ripened() && mossBed.strain() == 0 && capBed.strain() == 0, "Ripe, unharvested: no strain");
			helper.succeed();
		});
	}

	/**
	 * C-08 (C-P59): however a ripe crop is taken, its bed shows one strain not yet counted, and the next growth counts it
	 * once: an empty hand, a Gleaner, a sickle's area harvest (sneaking: a plain use is the hand's harvest), a reap (as an
	 * arm weapon and the harvesting spell make it), and breaking it and planting it again from its item without sneaking.
	 */
	@GameTest(maxTicks = 80)
	public void everyHarvestPathStrainsTheBedOnce(GameTestHelper helper) {
		BlockPos byHand = new BlockPos(1, 2, 1), byGleaner = new BlockPos(6, 2, 1), bySickle = new BlockPos(1, 2, 6);
		BlockPos byReap = new BlockPos(3, 2, 4), byBreaking = new BlockPos(6, 2, 6);
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (BlockPos at : List.of(byHand, byGleaner, bySickle, byReap, byBreaking)) {
			beds.add(bed(helper, at.below(), 10));
			crop(helper, at, Garden.SUNPETAL_CROP, 1);
			helper.setBlock(at.south(), Blocks.GLOWSTONE);
		}
		VerdantBedBlockEntity handBed = beds.get(0), gleanerBed = beds.get(1), sickleBed = beds.get(2), reapBed = beds.get(3);
		VerdantBedBlockEntity breakingBed = beds.get(4);
		BlockPos heartAt = new BlockPos(4, 2, 2); // within the Gleaner's draw (4 blocks); not a crop
		bed(helper, heartAt.below(), 10);
		helper.setBlock(heartAt, Garden.VERDANT_HEART);
		BlockPos gleanerAt = new BlockPos(6, 2, 0); // reach 3: of these crops, only its own
		helper.setBlock(gleanerAt, Garden.GLEANER);
		prefetch(helper, handBed, gleanerBed, sickleBed, reapBed, breakingBed);
		helper.runAtTickTime(42, () -> {
			ServerLevel level = level(helper);
			ServerPlayer keeper = gardener(helper, false);
			// Woken only now, so neither works before this tick.
			VerdantHeartBlockEntity heart = entity(helper, heartAt);
			heart.awaken(keeper.getUUID());
			heart.setVerdance(5);
			GleanerBlockEntity gleaner = entity(helper, gleanerAt);
			gleaner.awaken(keeper.getUUID());
			ripen(helper, byHand, handBed);
			helper.assertTrue(handUse(helper, keeper, byHand, ItemStack.EMPTY, false).consumesAction() && age(helper, byHand) == 1, "harvested by hand");
			strained(helper, byHand, handBed, "by hand");
			ripen(helper, byGleaner, gleanerBed);
			helper.assertValueEqual(gleaner.glean(level), "working", "the Gleaner harvests it");
			strained(helper, byGleaner, gleanerBed, "by a Gleaner");
			ripen(helper, bySickle, sickleBed);
			helper.assertTrue(handUse(helper, keeper, bySickle, new ItemStack(JugcraftAgriculture.item("flint_sickle")), true).consumesAction(),
					"a sneaking sickle reaps it (a plain use is the hand's harvest)");
			strained(helper, bySickle, sickleBed, "by a sickle");
			ripen(helper, byReap, reapBed);
			ArmItem.reap(level, helper.absolutePos(byReap), null, ItemStack.EMPTY);
			strained(helper, byReap, reapBed, "by a reap");
			ripen(helper, byBreaking, breakingBed);
			helper.assertTrue(level.destroyBlock(helper.absolutePos(byBreaking), true, keeper) && helper.getBlockState(byBreaking).isAir()
					&& breakingBed.strainNow() == 1, "Broken ripe: its harvest shows before it is counted");
			helper.assertTrue(handUse(helper, keeper, byBreaking.below(), new ItemStack(Garden.SUNPETAL), false).consumesAction()
					&& helper.getBlockState(byBreaking).is(Garden.SUNPETAL_CROP), "planted again, not sneaking");
			strained(helper, byBreaking, breakingBed, "by breaking and planting again");
			helper.succeed();
		});
	}

	/** C-08 (C-P59): a Crop Harvester takes a ripe Sunpetal from its field, and its bed counts that harvest once. */
	@GameTest(maxTicks = 200)
	public void theCropHarvesterStrainsTheBedOnce(GameTestHelper helper) {
		if (TestFocus.skip("machines")) {
			helper.succeed();
			return;
		}
		BlockPos master = new BlockPos(4, 1, 0);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.CROP_HARVESTER);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
		block.setPlacedBy(level(helper), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		charge(helper, master.above(), Direction.UP);
		BlockPos field = new BlockPos(3, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, field.below(), 10);
		crop(helper, field, Garden.SUNPETAL_CROP, 1);
		helper.setBlock(field.west(), Blocks.GLOWSTONE);
		prefetch(helper, bed);
		helper.runAtTickTime(40, () -> ripen(helper, field, bed));
		// Checked every tick: the first tick the crop stands below ripe is the harvester's (a block entity ticks after the
		// random ticks), so its bed has not rested; a strain of 2 is never accepted.
		helper.succeedWhen(() -> {
			BlockState state = helper.getBlockState(field);
			helper.assertTrue(bed.strainNow() == 1 && (!(state.getBlock() instanceof OrganismCropBlock c) || !c.isMaxAge(state)),
					"The harvester has taken the ripe crop: " + state + ", strain " + bed.strainNow());
			if (!(state.getBlock() instanceof OrganismCropBlock)) {
				helper.setBlock(field, Garden.SUNPETAL_CROP);
			}
			grow(helper, field);
			helper.assertTrue(bed.strain() == 1 && !bed.ripened(), "Growth counted the harvester's harvest once: " + bed.strain());
		});
	}

	/** C-08 (C-P61): a Mendvetch's fixed nutrient feeds the poorest bed and eases no strain; nor does any gift or nourishing. */
	@GameTest(maxTicks = 80)
	public void fixingDoesNotEaseStrain(GameTestHelper helper) {
		BlockPos centre = new BlockPos(3, 1, 3);
		VerdantBedBlockEntity own = bed(helper, centre, 4);
		VerdantBedBlockEntity east = bed(helper, centre.east(), 1);
		VerdantBedBlockEntity west = bed(helper, centre.west(), 6);
		helper.setBlock(centre.above(), Garden.MENDVETCH_CROP);
		helper.setBlock(centre.above().north(), Blocks.GLOWSTONE);
		prefetch(helper, own);
		helper.runAtTickTime(44, () -> {
			helper.setBlock(centre.above(), Garden.MENDVETCH_CROP);
			wet(helper, centre);
			own.setNutrients(4);
			east.setNutrients(1);
			west.setNutrients(6);
			for (VerdantBedBlockEntity bed : List.of(own, east, west)) {
				bed.setStrain(2);
			}
			helper.assertTrue(grow(helper, centre.above()) == OrganismCropBlock.Growth.GREW && east.nutrients() == 2,
					"The vetch grows and feeds the poorest bed: " + east.nutrients());
			helper.assertTrue(own.strain() == 2 && east.strain() == 2 && west.strain() == 2,
					"A fixer's nutrient is not compost: " + own.strain() + "/" + east.strain() + "/" + west.strain());
			helper.assertTrue(east.give(3) == 3 && east.strain() == 2, "Nor is any gift");
			helper.assertTrue(Garden.nourish(List.of(west), 2) == 2 && west.strain() == 2, "Nor nourishing");
			helper.succeed();
		});
	}

	/**
	 * C-08 (C-P60, C-P61): strain eases by rest (one a bed random tick, only while no crop grows on it; a tick that counts a
	 * harvest rests nothing; nothing switched off), by compost (bone meal on the bed and, a plain use, on its crop;
	 * fertilizer's two doses; a Mulch Maw's nutrient, for the bed it feeds only; a full bed takes none) and by a
	 * Greenwarden's empty hand, once every TEND_TICKS a bed, on the bed or on its growing crop (the unit review: a crop covers
	 * its bed from above). An item in hand is its own use and tends nothing; a novice's hand only reads; a bed its tender may
	 * not change is neither tended, on it or through its crop, nor fed by bone meal on its crop.
	 */
	@GameTest(maxTicks = 80)
	public void restCompostAndTendingEaseStrain(GameTestHelper helper) {
		BlockPos fallowAt = new BlockPos(1, 1, 1);
		BlockPos growingAt = new BlockPos(3, 1, 1);
		BlockPos ripeAt = new BlockPos(5, 1, 1);
		BlockPos compostAt = new BlockPos(1, 1, 4);
		BlockPos poorAt = new BlockPos(3, 1, 4);
		BlockPos mawAt = new BlockPos(4, 1, 4);
		BlockPos richAt = new BlockPos(6, 1, 4);
		BlockPos tendAt = new BlockPos(6, 1, 7);
		BlockPos claimedAt = new BlockPos(3, 1, 7);
		VerdantBedBlockEntity fallowBed = bed(helper, fallowAt, 10);
		VerdantBedBlockEntity growingBed = bed(helper, growingAt, 10);
		VerdantBedBlockEntity ripeBed = bed(helper, ripeAt, 10);
		VerdantBedBlockEntity compostBed = bed(helper, compostAt, 10);
		VerdantBedBlockEntity poorBed = bed(helper, poorAt, 3);
		VerdantBedBlockEntity richBed = bed(helper, richAt, 5);
		VerdantBedBlockEntity tendBed = bed(helper, tendAt, 10);
		VerdantBedBlockEntity claimedBed = bed(helper, claimedAt, 10);
		crop(helper, growingAt.above(), Garden.SUNPETAL_CROP, 1);
		crop(helper, ripeAt.above(), Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		crop(helper, compostAt.above(), Garden.SUNPETAL_CROP, 1);
		helper.setBlock(fallowAt.above().north(), Blocks.GLOWSTONE);
		helper.setBlock(mawAt, Garden.MULCH_MAW);
		MulchMawBlockEntity maw = entity(helper, mawAt);
		prefetch(helper, fallowBed);
		helper.runAtTickTime(46, () -> {
			ServerLevel level = level(helper);
			// Rest: a fallow bed rests one strain a random tick, down to none.
			fallowBed.setStrain(2);
			tickBed(helper, fallowAt);
			helper.assertTrue(fallowBed.strain() == 1, "A fallow bed rests a strain a random tick: " + fallowBed.strain());
			tickBed(helper, fallowAt);
			tickBed(helper, fallowAt);
			helper.assertTrue(fallowBed.strain() == 0, "down to none: " + fallowBed.strain());
			// A crop growing below ripe keeps its bed working; a ripe one, or a stalled one, lets it rest.
			growingBed.setStrain(2);
			ripeBed.setStrain(2);
			crop(helper, growingAt.above(), Garden.SUNPETAL_CROP, 1);
			growingBed.note(Verdict.Growth.TOLERATING.id, List.of()); // its last note says it grows
			tickBed(helper, growingAt);
			tickBed(helper, ripeAt);
			helper.assertTrue(growingBed.strain() == 2 && ripeBed.strain() == 1, "Rest only while no crop grows: " + growingBed.strain() + ", " + ripeBed.strain());
			growingBed.note(Verdict.Growth.STALLED.id, List.of());
			tickBed(helper, growingAt);
			helper.assertTrue(growingBed.strain() == 1, "A stalled crop's bed rests: " + growingBed.strain());
			// The bed's random tick counts a harvest it saw, resting nothing that tick; the next rests.
			ripen(helper, fallowAt.above(), fallowBed);
			level.destroyBlock(helper.absolutePos(fallowAt.above()), false);
			tickBed(helper, fallowAt);
			helper.assertTrue(fallowBed.strain() == 1 && !fallowBed.ripened(), "The random tick counted the harvest: " + fallowBed.strain());
			tickBed(helper, fallowAt);
			helper.assertTrue(fallowBed.strain() == 0, "and the next rested it: " + fallowBed.strain());
			// Switched off, nothing changes.
			boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
			try {
				fallowBed.setStrain(2);
				tickBed(helper, fallowAt);
				helper.assertTrue(fallowBed.strain() == 2, "Switched off, a bed does not rest: " + fallowBed.strain());
			} finally {
				JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
			}
			// Compost: bone meal on the bed; on its crop, by a plain use; fertilizer's two doses.
			ServerPlayer greenwarden = gardener(helper, true);
			crop(helper, compostAt.above(), Garden.SUNPETAL_CROP, 1); // growing, whatever random ticks made of it
			compostBed.setNutrients(10); // nor what a random step took from it
			compostBed.setStrain(3);
			ItemStack meal = new ItemStack(Items.BONE_MEAL, 2);
			handUse(helper, greenwarden, compostAt, meal, false);
			helper.assertTrue(compostBed.strain() == 2 && compostBed.nutrients() == 10 + Garden.BONE_MEAL_NUTRIENTS && meal.getCount() == 1,
					"Bone meal on the bed is compost: " + compostBed.strain());
			handUse(helper, greenwarden, compostAt.above(), meal, false);
			helper.assertTrue(compostBed.strain() == 1 && compostBed.nutrients() == 10 + 2 * Garden.BONE_MEAL_NUTRIENTS && meal.isEmpty()
					&& age(helper, compostAt.above()) == 1, "A plain use of bone meal on its crop feeds the bed and eases one: " + compostBed.strain());
			compostBed.setStrain(3);
			helper.assertTrue(FertilizerItem.fertilize(level, helper.absolutePos(compostAt.above())) == 1 && compostBed.strain() == 1,
					"Fertilizer's two doses ease two: " + compostBed.strain());
			compostBed.setNutrients(VerdantBedBlockEntity.CAPACITY);
			compostBed.setStrain(2);
			ItemStack more = new ItemStack(Items.BONE_MEAL);
			handUse(helper, greenwarden, compostAt, more, false);
			helper.assertTrue(compostBed.strain() == 2 && more.getCount() == 1, "A full bed takes no compost, and keeps the bone meal");
			// A Maw's nutrient is compost for the bed it feeds, and only that bed.
			for (VerdantBedBlockEntity other : List.of(fallowBed, growingBed, ripeBed, tendBed, claimedBed)) {
				other.setNutrients(10);
			}
			poorBed.setNutrients(3);
			poorBed.setStrain(2);
			richBed.setNutrients(5);
			richBed.setStrain(2);
			maw.awaken(KEEPER);
			helper.assertTrue(maw.feed(new ItemStack(Garden.VERDANT_CHAFF, 2)) == 2, "The Maw takes the chaff");
			maw.digest(level);
			maw.digest(level);
			helper.assertTrue(poorBed.nutrients() == 4 && poorBed.strain() == 1 && richBed.nutrients() == 5 && richBed.strain() == 2,
					"The Maw's nutrient eased the bed it fed only: " + poorBed.strain() + "/" + richBed.strain());
			// Tending: a Greenwarden's empty hand, once every TEND_TICKS a bed.
			tendBed.setStrain(2);
			handUse(helper, greenwarden, tendAt, ItemStack.EMPTY, false);
			helper.assertTrue(tendBed.strain() == 1, "A Greenwarden's empty hand tends it: " + tendBed.strain());
			handUse(helper, greenwarden, tendAt, ItemStack.EMPTY, false);
			helper.assertTrue(tendBed.strain() == 1, "but not again so soon: " + tendBed.strain());
			long now = level.getGameTime();
			helper.assertTrue(!tendBed.tend(now + Garden.TEND_TICKS - 1) && tendBed.tend(now + Garden.TEND_TICKS) && tendBed.strain() == 0,
					"once every " + Garden.TEND_TICKS + " ticks: " + tendBed.strain());
			// An item in hand is its own use: a Sunpetal plants without sneaking and tends nothing.
			tendBed.setStrain(2);
			handUse(helper, greenwarden, tendAt, new ItemStack(Garden.SUNPETAL), false);
			helper.assertTrue(helper.getBlockState(tendAt.above()).is(Garden.SUNPETAL_CROP) && tendBed.strain() == 2,
					"A Sunpetal in hand plants on the bed and tends nothing");
			handUse(helper, gardener(helper, false), tendAt, ItemStack.EMPTY, false);
			helper.assertTrue(tendBed.strain() == 2, "A novice's empty hand only reads it");
			claimedBed.setStrain(2);
			crop(helper, claimedAt.above(), Garden.SUNPETAL_CROP, 1);
			// The fallow bed (its area read early) under a growing crop: tended through the crop, as on the bed.
			crop(helper, fallowAt.above(), Garden.SUNPETAL_CROP, 1);
			fallowBed.setStrain(2);
			try (TestClaims claims = TestClaims.open()) {
				claims.block(helper.absolutePos(claimedAt), new UUID(14L, 3L));
				claims.block(helper.absolutePos(fallowAt), new UUID(14L, 3L));
				handUse(helper, greenwarden, fallowAt.above(), ItemStack.EMPTY, false);
				helper.assertTrue(fallowBed.strain() == 2, "A bed its tender may not change is not tended through its crop: " + fallowBed.strain());
				handUse(helper, greenwarden, claimedAt, ItemStack.EMPTY, false);
				helper.assertTrue(claimedBed.strain() == 2, "A bed its tender may not change is not tended: " + claimedBed.strain());
				ItemStack refused = new ItemStack(Items.BONE_MEAL);
				helper.assertTrue(handUse(helper, greenwarden, claimedAt.above(), refused, false).consumesAction() && claimedBed.strain() == 2
						&& claimedBed.nutrients() == 10 && refused.getCount() == 1,
						"Bone meal on its crop asks as bone meal on the bed: refused, it feeds and eases nothing and is kept");
			}
			handUse(helper, greenwarden, fallowAt.above(), ItemStack.EMPTY, false);
			helper.assertTrue(fallowBed.strain() == 1 && age(helper, fallowAt.above()) == 1,
					"A Greenwarden's empty hand on a growing crop tends the bed beneath: " + fallowBed.strain());
			helper.succeed();
		});
	}

	/**
	 * C-08 (C-P59): strain counts only against a plant's disturbance limit. A Gloamcap with no working magic near it stalls,
	 * strained or not; strain never lifts a little magic to its best; a Sunpetal on a quiet bed thrives up to strain 2 and
	 * goes at half pace at 3, saying why, and the Inspect page names the strain against the limit; where the area already
	 * disturbs it, strain can stop it; a Mendvetch thrives and a Dewmoss never minds.
	 */
	@GameTest(maxTicks = 80)
	public void strainNeverSuppliesDisturbance(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		VerdantBedBlockEntity bed = bed(helper, at.below(), 10);
		helper.setBlock(at, Garden.GLOAMCAP_CROP);
		shut(helper, at);
		prefetch(helper, bed);
		helper.runAtTickTime(48, () -> {
			helper.setBlock(at, Garden.GLOAMCAP_CROP);
			wet(helper, at.below());
			bed.setStrain(Habitat.MAX_STRAIN);
			helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.STALLED && age(helper, at) == 0, "No working magic: it stalls, strained or not");
			helper.assertTrue(bed.reasons().contains("ecology.short|disturbance|0|1"), "short of disturbance; the strain counted as none: " + bed.reasons());
			Organism gloamcap = organism("jugcraft:gloamcap");
			Organism sunpetal = organism("jugcraft:sunpetal");
			Organism mendvetch = organism("jugcraft:mendvetch");
			Organism dewmoss = organism("jugcraft:dewmoss");
			Verdict quiet = Verdict.of(gloamcap.niche(), new Habitat(7, 3, 10, 1, 0, 3));
			helper.assertTrue(quiet.fit(Factor.DISTURBANCE) == Fit.SHORT && !quiet.strained(), "Strain never supplies the magic a Gloamcap needs");
			Verdict low = Verdict.of(gloamcap.niche(), new Habitat(7, 3, 10, 1, 2, 3));
			helper.assertTrue(low.fit(Factor.DISTURBANCE) == Fit.TOLERABLE && !low.strained(), "nor lifts a little to its best");
			for (int strain = 0; strain <= 2; strain++) {
				helper.assertTrue(Verdict.of(sunpetal.niche(), new Habitat(7, 15, 10, 3, 0, strain)).growth() == Verdict.Growth.THRIVING,
						"A Sunpetal on a quiet bed thrives at strain " + strain);
			}
			Habitat three = new Habitat(7, 15, 10, 3, 0, 3);
			Verdict slowed = Verdict.of(sunpetal.niche(), three);
			helper.assertTrue(slowed.growth() == Verdict.Growth.TOLERATING && slowed.strained()
					&& slowed.reasons(sunpetal.niche(), three).equals(List.of(Text.of("ecology.strained.tolerable", new Text.Ref("factor", "disturbance"), 0, 3, 0, 2))),
					"and at half pace at 3, saying why: " + slowed.reasons(sunpetal.niche(), three));
			Habitat loud = new Habitat(7, 15, 10, 3, 3, 3);
			Verdict stopped = Verdict.of(sunpetal.niche(), loud);
			helper.assertTrue(stopped.growth() == Verdict.Growth.STALLED
					&& stopped.reasons(sunpetal.niche(), loud).equals(List.of(Text.of("ecology.strained.excess", new Text.Ref("factor", "disturbance"), 3, 3, 5))),
					"Where the area already disturbs it, strain can stop it: " + stopped.reasons(sunpetal.niche(), loud));
			helper.assertTrue(Verdict.of(sunpetal.niche(), new Habitat(7, 15, 10, 3, 3, 0)).growth() == Verdict.Growth.TOLERATING, "(the area alone slows it)");
			for (int strain = 0; strain <= Habitat.MAX_STRAIN; strain++) {
				helper.assertTrue(Verdict.of(mendvetch.niche(), new Habitat(7, 15, 10, 1, 0, strain)).growth() == Verdict.Growth.THRIVING, "A Mendvetch thrives at strain " + strain);
				helper.assertTrue(Verdict.of(dewmoss.niche(), new Habitat(7, 5, 10, 1, 15, strain)).fit(Factor.DISTURBANCE) == Fit.IDEAL, "A Dewmoss never minds");
			}
			List<Text> rows = HabitatInspection.judged(new HabitatInspection.State(three, 15, 0, Garden.WATER_REACH), sunpetal.niche(), slowed);
			Text row = rows.get(Factor.DISTURBANCE.ordinal());
			helper.assertTrue(((Text) row.args().get(0)).key().equals("ecology.inspect.disturbance_strained")
					&& ((Text) row.args().get(0)).args().subList(0, 2).equals(List.of(0, 3))
					&& ((Text) row.args().get(1)).key().equals("ecology.inspect.tolerable"), "The Inspect page names the strain against the limit: " + row);
			helper.succeed();
		});
	}

	/**
	 * C-08 (C-P59, C-P60): a battery box beside a Sunpetal disturbs and strains nothing. Harvested again and again, its bed's
	 * strain rises one a harvest and stops at its cap; where the area also disturbs it the strain stalls it, yet the crop
	 * stands, its bed rests while it is stalled, and it grows again: strain never stops a plant for good.
	 */
	@GameTest(maxTicks = 80)
	public void strainNeverKillsAndNeverReadsPollution(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		VerdantBedBlockEntity bed = bed(helper, at.below(), 10);
		crop(helper, at, Garden.SUNPETAL_CROP, 1);
		helper.setBlock(at.north(), Blocks.GLOWSTONE);
		helper.setBlock(at.east(), JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX));
		prefetch(helper, bed);
		helper.runAtTickTime(50, () -> {
			ServerLevel level = level(helper);
			Habitat habitat = bed.habitat(level);
			helper.assertTrue(habitat != null && habitat.disturbance() == 0 && habitat.strain() == 0, "A machine beside it disturbs and strains nothing: " + habitat);
			OrganismCropBlock sunpetal = (OrganismCropBlock) Garden.SUNPETAL_CROP;
			bed.setStrain(0);
			for (int round = 1; round <= Habitat.MAX_STRAIN + 2; round++) {
				crop(helper, at, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE - 1);
				wet(helper, at.below());
				bed.setNutrients(10);
				helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.GREW && bed.ripened(), "Round " + round + ": it ripens");
				helper.assertTrue(!sunpetal.harvest(level, helper.absolutePos(at), helper.getBlockState(at)).isEmpty(), "and is harvested");
				grow(helper, at);
				helper.assertTrue(bed.strain() == Math.min(round, Habitat.MAX_STRAIN), "After " + round + " harvests: strain " + bed.strain());
			}
			for (int dz = -1; dz <= 1; dz++) {
				helper.setBlock(at.offset(-1, -1, dz), Blocks.BUDDING_AMETHYST);
			}
			bed.forgetSample();
		});
		helper.runAtTickTime(52, () -> {
			wet(helper, at.below());
			bed.setNutrients(10);
			helper.assertTrue(bed.habitat(level(helper)) != null, "The bed is read again, the amethyst in its area");
			for (int attempt = 0; attempt < 20; attempt++) {
				helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.STALLED && helper.getBlockState(at).is(Garden.SUNPETAL_CROP),
						"Stalled at its cap, attempt " + attempt + ": it stands");
			}
			helper.assertTrue(bed.strain() == Habitat.MAX_STRAIN && bed.reasons().contains("ecology.strained.excess|disturbance|3|3|5"),
					"Its area's 3 and strain's 3 pass a Sunpetal's 5: " + bed.reasons());
			tickBed(helper, at.below());
			helper.assertTrue(bed.strain() == Habitat.MAX_STRAIN - 1, "A stalled crop's bed rests: " + bed.strain());
			wet(helper, at.below());
			helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.GREW && bed.growth().equals(Verdict.Growth.TOLERATING.id),
					"so strain never stops a plant for good: " + bed.growth());
			helper.succeed();
		});
	}

	/**
	 * C-08 (C-P59): a strained bed whose crop has ripened keeps both through a save, at version 1, and sends both to clients
	 * (never the keeper), who read them back; an unstrained bed writes neither key.
	 */
	@GameTest(maxTicks = 80)
	public void strainSurvivesASave(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		VerdantBedBlockEntity bed = bed(helper, at.below(), 10);
		crop(helper, at, Garden.SUNPETAL_CROP, 1);
		helper.setBlock(at.north(), Blocks.GLOWSTONE);
		prefetch(helper, bed);
		helper.runAtTickTime(53, () -> {
			ServerLevel level = level(helper);
			crop(helper, at, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE - 1);
			bed.setStrain(2);
			bed.setNutrients(10);
			helper.assertTrue(grow(helper, at) == OrganismCropBlock.Growth.GREW && bed.ripened() && bed.strain() == 2, "Strained 2 and ripe");
			CompoundTag saved = bed.saveWithoutMetadata(level.registryAccess());
			helper.assertTrue(saved.getIntOr(Saved.VERSION, 0) == 1 && saved.getIntOr(VerdantBedBlockEntity.STRAIN, -1) == 2
					&& saved.getBooleanOr(VerdantBedBlockEntity.RIPENED, false), "Saved at version 1 with its strain and ripening: " + saved);
			VerdantBedBlockEntity copy = reload(helper, bed, saved);
			helper.assertTrue(copy.strain() == 2 && copy.ripened() && copy.nutrients() == bed.nutrients() && copy.awake(), "Read back exactly");
			CompoundTag sent = bed.getUpdateTag(level.registryAccess());
			helper.assertTrue(sent.getIntOr(VerdantBedBlockEntity.STRAIN, -1) == 2 && sent.getBooleanOr(VerdantBedBlockEntity.RIPENED, false)
					&& !sent.contains("keeper"), "A client is sent the strain and ripening, never the keeper: " + sent);
			VerdantBedBlockEntity client = reload(helper, bed, sent);
			helper.assertTrue(client.strain() == 2 && client.ripened() && client.keeper() == null, "and reads them as a client does");
			bed.setStrain(0);
			CompoundTag plain = bed.saveWithoutMetadata(level.registryAccess());
			helper.assertTrue(!plain.contains(VerdantBedBlockEntity.STRAIN) && !plain.contains(VerdantBedBlockEntity.RIPENED), "An unstrained bed writes neither key");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- grafting (Concordance Chunk C, C-09)

	private static final String DEWLIT = "jugcraft:dewlit";
	private static final String SUNPETAL = "jugcraft:sunpetal";

	private static OrganismCropBlock sunpetal() {
		return (OrganismCropBlock) Garden.SUNPETAL_CROP;
	}

	/** {@code bed} still carries Dewlit after {@code after}, and gives a Sunpetal Dewlit's niche. */
	private static void kept(GameTestHelper helper, VerdantBedBlockEntity bed, Organism petal, Niche grafted, String after) {
		helper.assertTrue(DEWLIT.equals(bed.graft()) && bed.niche(petal).equals(grafted), "After " + after + " the bed still carries Dewlit: " + bed.graft());
	}

	/**
	 * C-09 (C-P62, C-P65): a Greenwarden's plain use of a Dewmoss on a Sunpetal grafts Dewlit onto its bed, spends one Dewmoss
	 * and leaves the crop as it was; clients are told the graft and its host (never the keeper), and Jade the graft. A second
	 * use is refused and spends nothing (one graft a bed); a Dewmoss on a Gloamcap grafts nothing; switched off, nothing
	 * grafts; and a graft the rules no longer know is replaced by the next. The graft forgets the ungrafted plant's verdict
	 * (the unit review).
	 */
	@GameTest(maxTicks = 20)
	public void aGraftTakesOnceAndConsumesItsDonor(GameTestHelper helper) {
		ServerLevel level = level(helper);
		BlockPos at = new BlockPos(2, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, at, 10);
		crop(helper, at.above(), Garden.SUNPETAL_CROP, 1);
		ServerPlayer gardener = gardener(helper, true);
		ItemStack donor = new ItemStack(Garden.DEWMOSS, 2);
		bed.note(Verdict.Growth.STALLED.id, List.of()); // the ungrafted plant's last verdict
		helper.assertTrue(handUse(helper, gardener, at.above(), donor, false).consumesAction(), "The graft consumes the use");
		helper.assertTrue(DEWLIT.equals(bed.graft()) && donor.getCount() == 1 && age(helper, at.above()) == 1,
				"Dewlit is grafted, one Dewmoss spent, the crop as it was: " + bed.graft() + ", " + donor.getCount());
		helper.assertTrue(bed.growth().isEmpty(), "and the bed forgets the ungrafted plant's verdict: " + bed.growth());
		CompoundTag sent = bed.getUpdateTag(level.registryAccess());
		helper.assertTrue(sent.getStringOr(VerdantBedBlockEntity.GRAFT, "").equals(DEWLIT) && sent.getStringOr(VerdantBedBlockEntity.GRAFT_HOST, "").equals(SUNPETAL)
				&& !sent.contains("keeper"), "Clients are told the graft and its host, never the keeper: " + sent);
		VerdantBedBlockEntity client = reload(helper, bed, sent);
		helper.assertTrue(DEWLIT.equals(client.graft()) && SUNPETAL.equals(client.graftFor()), "and read them as a client does");
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			helper.assertValueEqual(ConcordanceDataProvider.bedSnapshot(bed, level).getStringOr("graft", ""), DEWLIT, "Jade's graft");
		}
		helper.assertTrue(handUse(helper, gardener, at.above(), donor, false).consumesAction() && donor.getCount() == 1 && DEWLIT.equals(bed.graft())
				&& age(helper, at.above()) == 1, "A second graft is refused and spends nothing: one graft a bed");
		BlockPos capAt = new BlockPos(5, 1, 2);
		VerdantBedBlockEntity capBed = bed(helper, capAt, 10);
		crop(helper, capAt.above(), Garden.GLOAMCAP_CROP, 1);
		handUse(helper, gardener, capAt.above(), donor, false);
		helper.assertTrue(capBed.graft().isEmpty() && donor.getCount() == 1, "A Dewmoss on a Gloamcap grafts nothing");
		BlockPos offAt = new BlockPos(5, 1, 5);
		VerdantBedBlockEntity offBed = bed(helper, offAt, 10);
		crop(helper, offAt.above(), Garden.SUNPETAL_CROP, 1);
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		try {
			handUse(helper, gardener, offAt.above(), donor, false);
			helper.assertTrue(offBed.graft().isEmpty() && donor.getCount() == 1 && DEWLIT.equals(bed.graft()), "Switched off, nothing grafts");
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		bed.setGraft("jugcraft:gone");
		helper.assertTrue(handUse(helper, gardener, at.above(), donor, false).consumesAction() && DEWLIT.equals(bed.graft()) && donor.isEmpty(),
				"A graft the rules no longer know is replaced by the next: " + bed.graft());
		helper.succeed();
	}

	/**
	 * C-09 (C-P62): sneaking with a Dewmoss never reaches the crop (the server suppresses a block's use for a sneaking player
	 * holding an item), so it neither grafts nor, a crop being no bed, plants anything; a plain use then grafts.
	 */
	@GameTest(maxTicks = 20)
	public void aSneakingUseNeitherGraftsNorPlaces(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, at, 10);
		crop(helper, at.above(), Garden.SUNPETAL_CROP, 1);
		ServerPlayer gardener = gardener(helper, true);
		ItemStack donor = new ItemStack(Garden.DEWMOSS);
		handUse(helper, gardener, at.above(), donor, true);
		helper.assertTrue(bed.graft().isEmpty() && donor.getCount() == 1 && helper.getBlockState(at.above(2)).isAir()
				&& helper.getBlockState(at.above()).is(Garden.SUNPETAL_CROP) && age(helper, at.above()) == 1,
				"Sneaking, the Dewmoss neither grafts nor plants: " + bed.graft() + ", " + helper.getBlockState(at.above(2)));
		handUse(helper, gardener, at.above(), donor, false);
		helper.assertTrue(DEWLIT.equals(bed.graft()) && donor.isEmpty(), "A plain use grafts it");
		helper.succeed();
	}

	/**
	 * C-09 (C-P62): a Dewmoss used on a ripe Sunpetal grafts it and harvests nothing; any other item (a stick) on a ripe crop
	 * still harvests it as an empty hand does; and an empty hand harvests the grafted one, whose bed keeps its graft.
	 */
	@GameTest(maxTicks = 20)
	public void aDonorOnARipeCropGraftsAndAnyOtherItemStillHarvests(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 2);
		BlockPos b = new BlockPos(5, 1, 2);
		VerdantBedBlockEntity bedA = bed(helper, a, 10);
		VerdantBedBlockEntity bedB = bed(helper, b, 10);
		crop(helper, a.above(), Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		crop(helper, b.above(), Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		ServerPlayer gardener = gardener(helper, true);
		handUse(helper, gardener, a.above(), new ItemStack(Garden.DEWMOSS), false);
		helper.assertTrue(DEWLIT.equals(bedA.graft()) && age(helper, a.above()) == OrganismCropBlock.MAX_AGE,
				"A Dewmoss on a ripe Sunpetal grafts its bed and leaves it ripe");
		helper.assertItemEntityNotPresent(Garden.SUNPETAL, a.above(), 1.5);
		ItemStack stick = new ItemStack(Items.STICK);
		handUse(helper, gardener, b.above(), stick, false);
		helper.assertTrue(bedB.graft().isEmpty() && age(helper, b.above()) == 1 && stick.getCount() == 1, "A stick harvests a ripe crop as an empty hand does");
		helper.assertItemEntityPresent(Garden.SUNPETAL, b.above(), 1.5);
		helper.assertItemEntityPresent(Garden.VERDANT_CHAFF, b.above(), 1.5);
		handUse(helper, gardener, a.above(), ItemStack.EMPTY, false);
		helper.assertTrue(age(helper, a.above()) == 1 && DEWLIT.equals(bedA.graft()), "An empty hand harvests the grafted crop, and its bed keeps the graft");
		helper.assertItemEntityPresent(Garden.SUNPETAL, a.above(), 1.5);
		helper.succeed();
	}

	/** C-09 (C-P62): to someone who has not understood Verdant Husbandry a Dewmoss is just an item: it grafts nothing. */
	@GameTest(maxTicks = 20)
	public void anUnresearchedGardenerCannotGraft(GameTestHelper helper) {
		BlockPos ripeAt = new BlockPos(1, 1, 2);
		BlockPos youngAt = new BlockPos(5, 1, 2);
		VerdantBedBlockEntity ripeBed = bed(helper, ripeAt, 10);
		VerdantBedBlockEntity youngBed = bed(helper, youngAt, 10);
		crop(helper, ripeAt.above(), Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		crop(helper, youngAt.above(), Garden.SUNPETAL_CROP, 1);
		ServerPlayer novice = gardener(helper, false);
		helper.assertTrue(!Garden.knows(novice), "A novice has not understood Verdant Husbandry");
		ItemStack donor = new ItemStack(Garden.DEWMOSS, 2);
		handUse(helper, novice, youngAt.above(), donor, false);
		helper.assertTrue(youngBed.graft().isEmpty() && donor.getCount() == 2 && age(helper, youngAt.above()) == 1,
				"On a growing crop: no graft, nothing spent (the crop reports)");
		handUse(helper, novice, ripeAt.above(), donor, false);
		helper.assertTrue(ripeBed.graft().isEmpty() && donor.getCount() == 2 && age(helper, ripeAt.above()) == 1,
				"On a ripe one: no graft, nothing spent, and it is harvested as an empty hand would");
		helper.assertItemEntityPresent(Garden.SUNPETAL, ripeAt.above(), 1.5);
		helper.succeed();
	}

	/**
	 * C-09 (C-P62): a graft asks Authority at the crop and at its bed: a neighbour's claim on either refuses it, the use
	 * consumed and nothing spent (so the Dewmoss is never planted instead); unclaimed, it grafts.
	 */
	@GameTest(maxTicks = 20)
	public void aGraftIsRefusedWhereTheGardenerMayNotBuild(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 2);
		BlockPos b = new BlockPos(5, 1, 2);
		VerdantBedBlockEntity bedA = bed(helper, a, 10);
		VerdantBedBlockEntity bedB = bed(helper, b, 10);
		crop(helper, a.above(), Garden.SUNPETAL_CROP, 1);
		crop(helper, b.above(), Garden.SUNPETAL_CROP, 1);
		ServerPlayer gardener = gardener(helper, true);
		ItemStack donor = new ItemStack(Garden.DEWMOSS, 2);
		try (TestClaims claims = TestClaims.open()) {
			UUID neighbour = new UUID(14L, 9L);
			claims.block(helper.absolutePos(a), neighbour).block(helper.absolutePos(b.above()), neighbour);
			helper.assertTrue(handUse(helper, gardener, a.above(), donor, false).consumesAction() && bedA.graft().isEmpty() && donor.getCount() == 2,
					"A claimed bed refuses the graft: the use is consumed and nothing spent");
			helper.assertTrue(handUse(helper, gardener, b.above(), donor, false).consumesAction() && bedB.graft().isEmpty() && donor.getCount() == 2,
					"A claimed crop refuses it too");
			helper.assertTrue(helper.getBlockState(a.above(2)).isAir() && helper.getBlockState(b.above(2)).isAir()
					&& age(helper, a.above()) == 1 && age(helper, b.above()) == 1, "Nothing was planted, harvested or grown");
		}
		helper.assertTrue(handUse(helper, gardener, a.above(), donor, false).consumesAction() && DEWLIT.equals(bedA.graft()) && donor.getCount() == 1,
				"Unclaimed, it grafts");
		helper.succeed();
	}

	/**
	 * C-09 (C-P64, C-P65): a graft lives on its bed, so every way a ripe Sunpetal is taken keeps it: an empty hand, a Gleaner,
	 * a sickle's reap (and planting again where it left the bed bare), breaking it and planting again by a plain use, and a
	 * Peepo's or a machine's reset; so do its host's growth and a save, which writes the graft and never its host. An
	 * ungrafted bed writes no graft.
	 */
	@GameTest(maxTicks = 20)
	public void aGraftSurvivesEveryHarvestPathAndASave(GameTestHelper helper) {
		ServerLevel level = level(helper);
		Organism petal = organism(SUNPETAL);
		Niche grafted = Garden.catalog().niche(petal, DEWLIT);
		helper.assertTrue(!grafted.equals(petal.niche()), "Dewlit changes the Sunpetal's niche");
		BlockPos byHand = new BlockPos(1, 2, 1);
		BlockPos byGleaner = new BlockPos(3, 2, 3);
		BlockPos bySickle = new BlockPos(6, 2, 1);
		BlockPos byBreaking = new BlockPos(1, 2, 6);
		BlockPos byReset = new BlockPos(6, 2, 6);
		ServerPlayer gardener = gardener(helper, true);
		ItemStack donor = new ItemStack(Garden.DEWMOSS, 5);
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (BlockPos at : List.of(byHand, byGleaner, bySickle, byBreaking, byReset)) {
			VerdantBedBlockEntity bed = bed(helper, at.below(), 10);
			crop(helper, at, Garden.SUNPETAL_CROP, 1);
			handUse(helper, gardener, at, donor, false);
			helper.assertTrue(DEWLIT.equals(bed.graft()), "Grafted at " + at);
			beds.add(bed);
		}
		helper.assertTrue(donor.isEmpty(), "Five grafts spent five Dewmoss");
		VerdantBedBlockEntity handBed = beds.get(0), gleanerBed = beds.get(1), sickleBed = beds.get(2), breakingBed = beds.get(3);
		VerdantBedBlockEntity resetBed = beds.get(4);
		// An empty hand.
		crop(helper, byHand, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		helper.assertTrue(handUse(helper, gardener, byHand, ItemStack.EMPTY, false).consumesAction() && age(helper, byHand) == petal.replant(),
				"harvested by hand");
		kept(helper, handBed, petal, grafted, "a hand's harvest");
		// A Gleaner (reach 3: of these crops only its own is ripe), drawing on a Heart its keeper shares.
		BlockPos heartAt = new BlockPos(4, 2, 6);
		bed(helper, heartAt.below(), 10);
		helper.setBlock(heartAt, Garden.VERDANT_HEART);
		VerdantHeartBlockEntity heart = entity(helper, heartAt);
		heart.awaken(gardener.getUUID());
		heart.setVerdance(5);
		BlockPos gleanerAt = new BlockPos(4, 2, 4);
		helper.setBlock(gleanerAt, Garden.GLEANER);
		GleanerBlockEntity gleaner = entity(helper, gleanerAt);
		gleaner.awaken(gardener.getUUID());
		crop(helper, byGleaner, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		helper.assertValueEqual(gleaner.glean(level), "working", "the Gleaner harvests it");
		helper.assertTrue(age(helper, byGleaner) == petal.replant(), "and it falls back to its replanting step");
		kept(helper, gleanerBed, petal, grafted, "a Gleaner's harvest");
		// A sickle's reap (sneaking: a plain use is the hand's harvest); it replants, or leaves the bed bare to plant again.
		crop(helper, bySickle, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		helper.assertTrue(handUse(helper, gardener, bySickle, new ItemStack(JugcraftAgriculture.item("flint_sickle")), true).consumesAction(),
				"a sneaking sickle reaps it");
		BlockState cut = helper.getBlockState(bySickle);
		helper.assertTrue(cut.isAir() || cut.is(Garden.SUNPETAL_CROP) && age(helper, bySickle) == 0, "reaped: " + cut);
		kept(helper, sickleBed, petal, grafted, "a sickle's reap");
		if (cut.isAir()) {
			handUse(helper, gardener, bySickle.below(), new ItemStack(Garden.SUNPETAL), false);
		}
		helper.assertTrue(helper.getBlockState(bySickle).is(Garden.SUNPETAL_CROP), "a Sunpetal grows there again");
		kept(helper, sickleBed, petal, grafted, "planting again after the sickle");
		// Breaking it ripe, then planting again from its item without sneaking.
		crop(helper, byBreaking, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		helper.assertTrue(level.destroyBlock(helper.absolutePos(byBreaking), true, gardener) && helper.getBlockState(byBreaking).isAir(), "broken");
		kept(helper, breakingBed, petal, grafted, "breaking the crop");
		helper.assertTrue(handUse(helper, gardener, byBreaking.below(), new ItemStack(Garden.SUNPETAL), false).consumesAction()
				&& helper.getBlockState(byBreaking).is(Garden.SUNPETAL_CROP) && age(helper, byBreaking) == 0, "planted again, not sneaking");
		kept(helper, breakingBed, petal, grafted, "planting again");
		// A Peepo's reset (to its replanting step) and a machine's (to its first): plain block changes.
		crop(helper, byReset, Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
		level.setBlock(helper.absolutePos(byReset), sunpetal().getStateForAge(petal.replant()), Block.UPDATE_CLIENTS);
		kept(helper, resetBed, petal, grafted, "a Peepo's reset");
		level.setBlock(helper.absolutePos(byReset), sunpetal().getStateForAge(0), Block.UPDATE_ALL);
		kept(helper, resetBed, petal, grafted, "a machine's reset");
		// Its host's growth (only another organism's clears it).
		OrganismCropBlock.Growth growth = grow(helper, byHand);
		helper.assertTrue(growth != OrganismCropBlock.Growth.NO_BED && growth != OrganismCropBlock.Growth.DISABLED
				&& growth != OrganismCropBlock.Growth.DORMANT, "Its host's growth reaches the bed: " + growth);
		kept(helper, handBed, petal, grafted, "its host's growth");
		// A save.
		CompoundTag saved = handBed.saveWithoutMetadata(level.registryAccess());
		helper.assertTrue(saved.getStringOr(VerdantBedBlockEntity.GRAFT, "").equals(DEWLIT) && !saved.contains(VerdantBedBlockEntity.GRAFT_HOST),
				"A save writes the graft, never its host: " + saved);
		VerdantBedBlockEntity copy = reload(helper, handBed, saved);
		kept(helper, copy, petal, grafted, "a save");
		VerdantBedBlockEntity plain = bed(helper, new BlockPos(3, 1, 0), 10);
		helper.assertTrue(!plain.saveWithoutMetadata(level.registryAccess()).contains(VerdantBedBlockEntity.GRAFT), "An ungrafted bed writes no graft");
		helper.succeed();
	}

	/**
	 * C-09 (C-P62 to C-P65): every reader of the niche a Sunpetal grows by on a Dewlit bed reads Dewlit's. At moisture 3, below
	 * Dewlit's least but within the Sunpetal's own, the report and the note it writes say a grafted Sunpetal stalls for want of
	 * moisture, its growth stalls, and a Habitat Gauge on a Dewlit bed reads 0; on plain beds the Sunpetal grows slowly and a
	 * gauge reads 8. A gauge set on a grafted bed keeps the graft.
	 */
	@GameTest(maxTicks = 80)
	public void everyNicheReaderSeesTheGraft(GameTestHelper helper) {
		BlockPos cropAt = new BlockPos(2, 1, 2);
		BlockPos gaugeAt = new BlockPos(3, 1, 2);
		BlockPos plainCropAt = new BlockPos(2, 1, 5);
		BlockPos plainGaugeAt = new BlockPos(3, 1, 5);
		List<BlockPos> spots = List.of(cropAt, gaugeAt, plainCropAt, plainGaugeAt);
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (BlockPos at : spots) {
			beds.add(bed(helper, at, 10));
		}
		VerdantBedBlockEntity cropBed = beds.get(0), gaugeBed = beds.get(1), plainCropBed = beds.get(2), plainGaugeBed = beds.get(3);
		// Light 14 at the crops and 13 at the gauges, both ideal for Dewlit and for the Sunpetal's own niche.
		helper.setBlock(new BlockPos(2, 2, 3), Blocks.GLOWSTONE);
		helper.setBlock(new BlockPos(2, 2, 4), Blocks.GLOWSTONE);
		crop(helper, cropAt.above(), Garden.SUNPETAL_CROP, 1);
		crop(helper, plainCropAt.above(), Garden.SUNPETAL_CROP, 1);
		ServerPlayer gardener = gardener(helper, true);
		handUse(helper, gardener, cropAt.above(), new ItemStack(Garden.DEWMOSS), false);
		gaugeBed.setGraft(DEWLIT); // a gauge stands where a plant would, so its bed is grafted directly
		for (BlockPos at : List.of(gaugeAt, plainGaugeAt)) {
			helper.setBlock(at.above(), Garden.HABITAT_GAUGE);
			HabitatGaugeBlockEntity gauge = entity(helper, at.above());
			gauge.awaken(KEEPER);
		}
		helper.assertTrue(DEWLIT.equals(cropBed.graft()) && DEWLIT.equals(gaugeBed.graft()) && plainCropBed.graft().isEmpty()
				&& plainGaugeBed.graft().isEmpty(), "Two beds carry Dewlit and two none; a gauge set on a grafted bed keeps it");
		prefetch(helper, cropBed, gaugeBed, plainCropBed, plainGaugeBed);
		helper.runAtTickTime(54, () -> {
			ServerLevel level = level(helper);
			Organism petal = organism(SUNPETAL);
			Niche dewlit = Garden.catalog().niche(petal, DEWLIT);
			helper.assertTrue(dewlit.range(Factor.LIGHT).equals(new Niche.Range(9, 11, 15, 15)) && dewlit.range(Factor.MOISTURE).equals(new Niche.Range(4, 5, 7, 7)),
					"Dewlit: light from 9 (best 11), moisture from 4 (best 5)");
			crop(helper, cropAt.above(), Garden.SUNPETAL_CROP, 1);
			crop(helper, plainCropAt.above(), Garden.SUNPETAL_CROP, 1);
			for (int i = 0; i < spots.size(); i++) {
				BlockPos at = helper.absolutePos(spots.get(i));
				level.setBlock(at, level.getBlockState(at).setValue(VerdantBedBlock.MOISTURE, 3), Block.UPDATE_CLIENTS);
				beds.get(i).setNutrients(10);
				beds.get(i).setStrain(0);
			}
			helper.assertTrue(cropBed.niche(petal).equals(dewlit) && gaugeBed.niche(petal).equals(dewlit) && plainCropBed.niche(petal).equals(petal.niche())
					&& plainGaugeBed.niche(petal).equals(petal.niche()), "Each bed gives a Sunpetal its niche");
			// The report, and the note it writes.
			cropBed.note("waiting", List.of());
			plainCropBed.note("waiting", List.of());
			sunpetal().report(level, helper.absolutePos(cropAt.above()), helper.getBlockState(cropAt.above()), gardener);
			sunpetal().report(level, helper.absolutePos(plainCropAt.above()), helper.getBlockState(plainCropAt.above()), gardener);
			helper.assertTrue(cropBed.growth().equals("stalled") && cropBed.reasons().stream().anyMatch(reason -> reason.startsWith("ecology.short|moisture|3|4")),
					"On Dewlit moisture 3 is too little: " + cropBed.growth() + " " + cropBed.reasons());
			helper.assertTrue(plainCropBed.growth().equals("tolerating")
					&& plainCropBed.reasons().stream().anyMatch(reason -> reason.startsWith("ecology.tolerable|moisture|3|4|7")),
					"On its own it is only tolerable: " + plainCropBed.growth() + " " + plainCropBed.reasons());
			// Growth.
			helper.assertTrue(grow(helper, cropAt.above()) == OrganismCropBlock.Growth.STALLED && age(helper, cropAt.above()) == 1
					&& cropBed.growth().equals("stalled") && DEWLIT.equals(cropBed.graft()), "Growth stalls on Dewlit, which its host's growth keeps");
			helper.assertTrue(grow(helper, plainCropAt.above()) == OrganismCropBlock.Growth.GREW && plainCropBed.growth().equals("tolerating"),
					"and goes on slowly on a plain bed");
			// The gauges.
			HabitatGaugeBlockEntity gauge = entity(helper, gaugeAt.above());
			HabitatGaugeBlockEntity plainGauge = entity(helper, plainGaugeAt.above());
			gauge.attune(level, SUNPETAL);
			plainGauge.attune(level, SUNPETAL);
			helper.assertTrue(gauge.signal() == 0 && plainGauge.signal() == HabitatGaugeBlockEntity.TOLERATING,
					"A gauge reads its bed's niche: " + gauge.signal() + " on Dewlit, " + plainGauge.signal() + " on a plain bed");
			helper.succeed();
		});
	}

	/**
	 * C-09 (C-P63): a graft changes the niche and nothing else. The parser refuses a graft naming anything but a host, a donor
	 * and ranges, or changing nothing; the catalog gives Dewlit's two ranges to a Sunpetal only, and nothing for a graft it does
	 * not know; and on a Dewlit bed a Sunpetal grows at the same cost, dries its bed the same and yields the same plain produce,
	 * still an alchemical ingredient, as on a plain bed.
	 */
	@GameTest(maxTicks = 80)
	public void aGraftChangesOnlyTheNiche(GameTestHelper helper) {
		EcologyParser parser = new EcologyParser();
		helper.assertTrue(parser.graft("test:greedy", JsonParser.parseString("{\"schema\": 1, \"host\": \"jugcraft:sunpetal\", \"donor\": \"jugcraft:dewmoss\","
				+ " \"niche\": {\"light\": [9, 11, 15, 15]}, \"produce\": 4}")) == null
				&& parser.graft("test:idle", JsonParser.parseString("{\"schema\": 1, \"host\": \"jugcraft:sunpetal\", \"donor\": \"jugcraft:dewmoss\","
				+ " \"niche\": {}}")) == null, "A graft with a yield, or one changing nothing, is refused");
		helper.assertTrue(parser.problems().size() == 2 && parser.problems().get(0).startsWith("graft test:greedy: ")
				&& parser.problems().get(1).startsWith("graft test:idle: "), "and each refusal is named: " + parser.problems());
		EcologyCatalog catalog = Garden.catalog();
		Organism petal = organism(SUNPETAL);
		Organism moss = organism("jugcraft:dewmoss");
		Graft dewlit = catalog.graft(DEWLIT);
		helper.assertTrue(dewlit != null && dewlit.host().equals(SUNPETAL) && dewlit.donor().equals("jugcraft:dewmoss")
				&& dewlit.overrides().keySet().equals(EnumSet.of(Factor.MOISTURE, Factor.LIGHT)), "Dewlit: a Dewmoss in a Sunpetal, two ranges: " + dewlit);
		helper.assertTrue(catalog.graft(SUNPETAL, "jugcraft:dewmoss") == dewlit && catalog.graft(SUNPETAL, "jugcraft:gloamcap") == null,
				"The gesture finds it by its host and its donor");
		Niche grafted = catalog.niche(petal, DEWLIT);
		for (Factor factor : Factor.values()) {
			Niche.Range expected = factor == Factor.LIGHT ? new Niche.Range(9, 11, 15, 15)
					: factor == Factor.MOISTURE ? new Niche.Range(4, 5, 7, 7) : petal.niche().range(factor);
			helper.assertValueEqual(grafted.range(factor), expected, "Dewlit's " + factor.id);
		}
		helper.assertTrue(catalog.niche(moss, DEWLIT).equals(moss.niche()) && catalog.niche(petal, "jugcraft:no_such_graft").equals(petal.niche()),
				"Another organism, or a graft the rules do not know, keeps its own niche");
		List<BlockPos> spots = List.of(new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
		List<VerdantBedBlockEntity> beds = new ArrayList<>();
		for (BlockPos at : spots) {
			beds.add(bed(helper, at, 10));
			crop(helper, at.above(), Garden.SUNPETAL_CROP, 0);
			helper.setBlock(at.above().north(), Blocks.GLOWSTONE);
		}
		beds.get(0).setGraft(DEWLIT);
		prefetch(helper, beds.get(0), beds.get(1));
		helper.runAtTickTime(55, () -> {
			ServerLevel level = level(helper);
			List<List<ItemStack>> yields = new ArrayList<>();
			for (int i = 0; i < spots.size(); i++) {
				BlockPos at = spots.get(i);
				VerdantBedBlockEntity bed = beds.get(i);
				crop(helper, at.above(), Garden.SUNPETAL_CROP, 0);
				wet(helper, at);
				bed.setNutrients(10);
				bed.setStrain(0);
				helper.assertTrue(grow(helper, at.above()) == OrganismCropBlock.Growth.GREW && bed.nutrients() == 10 - petal.cost()
						&& bed.moisture() == VerdantBedBlock.WET - 1, "A step costs and dries the same at " + at + ": " + bed.reasons());
				crop(helper, at.above(), Garden.SUNPETAL_CROP, OrganismCropBlock.MAX_AGE);
				yields.add(sunpetal().harvest(level, helper.absolutePos(at.above()), helper.getBlockState(at.above())));
			}
			List<ItemStack> onDewlit = yields.get(0);
			List<ItemStack> alone = yields.get(1);
			helper.assertTrue(onDewlit.size() == 2 && alone.size() == 2, "Both yield produce and chaff: " + onDewlit + " / " + alone);
			for (int i = 0; i < 2; i++) {
				ItemStack a = onDewlit.get(i);
				ItemStack b = alone.get(i);
				helper.assertTrue(ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount() && Assaying.patched(a).isEmpty(),
						"The same plain stack, grafted or not: " + a + " / " + b);
			}
			helper.assertTrue(onDewlit.get(0).is(Garden.SUNPETAL) && onDewlit.get(0).getCount() == petal.produce() && onDewlit.get(1).is(Garden.VERDANT_CHAFF),
					"Sunpetals and chaff: " + onDewlit);
			helper.assertTrue(Alchemy.catalog().ingredient(ConcordanceProgress.itemId(onDewlit.get(0))) != null, "still the crucible's Sunpetal");
			helper.assertTrue(DEWLIT.equals(beds.get(0).graft()) && age(helper, spots.get(0).above()) == petal.replant(),
					"The bed keeps its graft, the crop falls back to its replanting step");
			helper.succeed();
		});
	}

	/**
	 * C-09 (C-P64): a graft lasts until a crop of another organism grows on its bed. A Dewmoss set there, reading it (a report,
	 * Jade) and the Dewmoss's own niche leave it; the Dewmoss's first growth attempt clears it, and clients are told; a Sunpetal
	 * planted again grows by its own niche. A graft the rules no longer know is kept, and read by nothing.
	 */
	@GameTest(maxTicks = 20)
	public void anotherPlantClearsTheGraft(GameTestHelper helper) {
		ServerLevel level = level(helper);
		BlockPos at = new BlockPos(2, 1, 2);
		VerdantBedBlockEntity bed = bed(helper, at, 10);
		crop(helper, at.above(), Garden.SUNPETAL_CROP, 1);
		ServerPlayer gardener = gardener(helper, true);
		handUse(helper, gardener, at.above(), new ItemStack(Garden.DEWMOSS), false);
		helper.assertTrue(DEWLIT.equals(bed.graft()), "Grafted");
		Organism petal = organism(SUNPETAL);
		Organism moss = organism("jugcraft:dewmoss");
		helper.assertTrue(level.destroyBlock(helper.absolutePos(at.above()), false, gardener), "The Sunpetal is broken");
		helper.setBlock(at.above(), Garden.DEWMOSS_CROP);
		helper.assertTrue(DEWLIT.equals(bed.graft()) && bed.niche(moss).equals(moss.niche()), "A Dewmoss set there keeps the graft and grows by its own niche");
		BlockState mossState = helper.getBlockState(at.above());
		((OrganismCropBlock) mossState.getBlock()).report(level, helper.absolutePos(at.above()), mossState, gardener);
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			ConcordanceDataProvider.bedSnapshot(bed, level);
		}
		helper.assertTrue(DEWLIT.equals(bed.graft()), "Reading it clears nothing");
		helper.assertTrue(grow(helper, at.above()) != OrganismCropBlock.Growth.NO_BED && bed.graft().isEmpty(), "The Dewmoss's growth clears it");
		helper.assertTrue(!bed.getUpdateTag(level.registryAccess()).contains(VerdantBedBlockEntity.GRAFT), "and clients are told");
		helper.setBlock(at.above(), Garden.SUNPETAL_CROP);
		helper.assertTrue(bed.niche(petal).equals(petal.niche()), "A Sunpetal planted again grows by its own niche");
		bed.setGraft("jugcraft:gone");
		helper.setBlock(at.above(), Garden.DEWMOSS_CROP);
		grow(helper, at.above());
		helper.assertTrue(bed.graft().equals("jugcraft:gone") && bed.niche(petal).equals(petal.niche()) && Garden.catalog().graftLines(bed.graft()).isEmpty(),
				"A graft the rules no longer know is kept by another plant's growth, and read by nothing");
		helper.succeed();
	}
}
