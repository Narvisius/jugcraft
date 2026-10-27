package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Brew;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleStandBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Operation;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Product;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Attunement;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Organism;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.progression.Route;
import io.github.jimbozoomer.jugcraft.concordance.progression.StageDefinition;
import io.github.jimbozoomer.jugcraft.concordance.progression.Stages;
import io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Readiness;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireRecord;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactHistory;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactKeeping;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Roster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StarReckoning;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.lair.MoltenSlagBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.styx.JugcraftStyx;
import io.github.jimbozoomer.jugcraft.styx.StyxState;
import io.github.jimbozoomer.jugcraft.styx.Styxhexenhammer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 31, the player journey (docs/features/arcane-concordance-journey.md), on a real server: each of the three
 * routes in tools/concordance_journey.py climbs the live stage rules its own way and founds its own Spire.
 * <p>
 * A fresh player understands First Light by ordinary evidence (three luminous specimens examined in the dark). The
 * route's other research is then granted in the states the route takes; each entry's own evidence and practice are
 * exercised by its system's tests. After each part the stage is the one the route expects, by the stage route it names
 * and not another. The Spire founded is the route's own, and a Spire whose research the route never took is refused.
 * Raising a Spire takes days and is played through by {@code ConcordanceSpireGameTests}; here its milestone is recorded
 * as raising records it, and the Architect stage follows.
 * <p>
 * Since Concordance Chunk C (C-10), the living workshop (tools/concordance_journey.py WORKSHOP): one survival player plays it
 * from a manual start with real blocks, and every recipe its rows name loads.
 * <p>
 * Since Concordance Chunk D (D-09), the pact hall (tools/concordance_journey.py PACT): one ordinary survival player walks it
 * whole, from Styx's hint to Stolas's departure, and every recipe its rows name loads.
 */
public class ConcordanceJourneyGameTests {
	/** A route: what reaching Adept takes and by which stage route, what reaching Master takes, its Spire and the ones refused. */
	private record Journey(String name, List<String> adeptUnderstood, List<String> adeptMastered, String adept,
			List<String> masterUnderstood, List<String> masterMastered, String spire, List<String> refused) {
	}

	private static final Journey CULTIVATION = new Journey("cultivation", List.of("jugcraft:verdant_husbandry"),
			List.of("jugcraft:first_light", "jugcraft:verdant_husbandry"), "specialist",
			List.of("jugcraft:alembic_arts", "jugcraft:binding_arts", "jugcraft:circle_lore"),
			List.of("jugcraft:alembic_arts", "jugcraft:binding_arts", "jugcraft:circle_lore"), "jugcraft:verdant_spire",
			List.of("jugcraft:star_spire"));
	private static final Journey EXPLORATION = new Journey("exploration and combat",
			List.of("jugcraft:celestial_attunement", "jugcraft:crimson_rites", "jugcraft:relic_lore", "jugcraft:sympathy"), List.of(),
			"generalist", List.of("jugcraft:circle_lore"),
			List.of("jugcraft:first_light", "jugcraft:celestial_attunement", "jugcraft:crimson_rites", "jugcraft:relic_lore",
					"jugcraft:sympathy"), "jugcraft:star_spire", List.of("jugcraft:verdant_spire"));
	private static final Journey INFRASTRUCTURE = new Journey("crafting and infrastructure", List.of("jugcraft:circle_lore"),
			List.of("jugcraft:first_light"), "attuned", List.of("jugcraft:runesmithing", "jugcraft:assay", "jugcraft:binding_arts"),
			List.of("jugcraft:circle_lore", "jugcraft:runesmithing", "jugcraft:assay", "jugcraft:binding_arts"), "jugcraft:lantern_spire",
			List.of("jugcraft:verdant_spire", "jugcraft:star_spire"));

	private static String stage(ServerPlayer player) {
		StageDefinition stage = StageProgress.stage(player);
		return stage == null ? "none" : stage.id();
	}

	private static Route route(String stage, String id) {
		StageDefinition definition = StageProgress.catalog().stages().get(stage);
		if (definition == null) {
			throw new IllegalStateException("no stage " + stage);
		}
		return definition.routes().stream().filter(route -> route.id().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalStateException("no route " + stage + "/" + id));
	}

	private static void grant(ServerPlayer player, List<String> research, ResearchState state) {
		for (String id : research) {
			ConcordanceProgress.grant(player, id, state);
		}
		StageProgress.refresh(player);
	}

	/** Which of the stage's routes hold for {@code player} now. */
	private static List<String> holding(ServerPlayer player, String stage) {
		Stages.Situation situation = StageProgress.situation(player);
		return StageProgress.catalog().stages().get(stage).routes().stream().filter(route -> Stages.missing(route, situation).isEmpty())
				.map(Route::id).toList();
	}

	private static void climb(GameTestHelper helper, Journey journey, BlockPos at) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		RateGate.forget(player.getUUID());
		for (String specimen : List.of("minecraft:amethyst_shard", "minecraft:glow_berries", "minecraft:glow_ink_sac")) {
			ConcordanceProgress.record(player, new Evidence.Examined(specimen, 0));
		}
		helper.assertValueEqual(ConcordanceProgress.knowledge(player).state("jugcraft:first_light"), ResearchState.UNDERSTOOD,
				journey.name() + ": three luminous specimens examined in the dark understand First Light");
		helper.assertValueEqual(stage(player), "practitioner", journey.name() + ": understanding First Light makes a Practitioner");

		grant(player, journey.adeptUnderstood(), ResearchState.UNDERSTOOD);
		grant(player, journey.adeptMastered(), ResearchState.MASTERED);
		helper.assertValueEqual(stage(player), "adept", journey.name() + ": its first part makes an Adept");
		helper.assertValueEqual(holding(player, "adept"), List.of(journey.adept()), journey.name() + ": by its own route alone");
		helper.assertValueEqual(holding(player, "master"), List.of(), journey.name() + ": and no Master yet");

		grant(player, journey.masterUnderstood(), ResearchState.UNDERSTOOD);
		grant(player, journey.masterMastered(), ResearchState.MASTERED);
		helper.assertValueEqual(stage(player), "master", journey.name() + ": its second part makes a Master");
		helper.assertTrue(holding(player, "master").contains("specialist"), journey.name() + ": five entries mastered in three traditions");

		BlockPos heart = helper.absolutePos(at);
		for (String other : journey.refused()) {
			helper.assertValueEqual(ConcordSpire.found(player, level, heart, other), "research",
					journey.name() + ": a Spire whose research the route never took is refused: " + other);
		}
		helper.assertValueEqual(ConcordSpire.found(player, level, heart, journey.spire()), "",
				journey.name() + ": the route's own Spire is founded: " + journey.spire());
		SpireRecord.of(level.getServer()).remove(ConcordSpire.id(level, heart));
		helper.assertValueEqual(holding(player, "architect"), List.of(), journey.name() + ": founding alone is not raising");
		StageProgress.milestone(player, ConcordSpire.MILESTONE);
		helper.assertValueEqual(stage(player), "architect", journey.name() + ": a raised Spire makes an Architect");
	}

	/** The three routes, each its own way to its own Spire. */
	@GameTest(maxTicks = 20)
	public void eachRouteClimbsItsOwnWay(GameTestHelper helper) {
		climb(helper, CULTIVATION, new BlockPos(1, 2, 1));
		climb(helper, EXPLORATION, new BlockPos(3, 2, 1));
		climb(helper, INFRASTRUCTURE, new BlockPos(5, 2, 1));
		helper.succeed();
	}

	// ---------------------------------------------------------------- the living workshop (Concordance Chunk C, C-10)

	/**
	 * Every recipe the workshop's rows name (tools/acquisition.py WORKSHOP and FACTORY; check_acquisition holds this list to
	 * the rows): the Thallite plate's is thallite_plate_by_hand; the Kindled Lantern is a bench working and the foundation a
	 * rite, so neither has one.
	 */
	private static final List<String> WORKSHOP_RECIPES = List.of("mendvetch", "crucible", "formula", "lampwright_bench", "ley_pylon",
			"circle_anchor", "warding_stone", "thallite_plate_by_hand", "crucible_stand", "courier_post", "clockwork_porter", "porter_key",
			"verdant_bed", "sunpetal", "dewmoss", "gloamcap", "coal_generator", "copper_cable", "machine_casing", "metal_press",
			"electric_pump", "bronze_fluid_pipe");

	// The yard (jugcraft-test:drone_depot, 16 by 8 by 16, all air; a stone floor at y 0 is laid first).
	/** The hand crucibles: the Kiln Draught's over a campfire, the poultice's over a magma block. */
	private static final BlockPos KILN = new BlockPos(2, 1, 2);
	private static final BlockPos POULTICE = new BlockPos(5, 1, 2);
	/** The Lesser Circle's anchor, and where its one participant stands inside it (no part there). */
	private static final BlockPos ANCHOR = new BlockPos(4, 1, 10);
	private static final BlockPos IN_CIRCLE = new BlockPos(5, 1, 11);
	/** The powered crucible's stand (the crucible above it), its coal generator and cable, the pump's spring and its generator. */
	private static final BlockPos STAND = new BlockPos(11, 1, 3);
	private static final BlockPos STAND_GENERATOR = new BlockPos(11, 1, 1);
	private static final BlockPos CABLE = new BlockPos(11, 1, 2);
	private static final BlockPos SPRING = new BlockPos(9, 1, 3);
	private static final BlockPos PUMP_GENERATOR = new BlockPos(9, 3, 3);
	/** A hopper over the powered crucible, holding its one bowl. */
	private static final BlockPos HOPPER = new BlockPos(11, 3, 3);
	/** The Courier Post, its chest, the pylon that fuels its porter, and where the porter is set down (all within its reach). */
	private static final BlockPos POST = new BlockPos(13, 1, 3);
	private static final BlockPos CHEST = new BlockPos(13, 1, 2);
	private static final BlockPos POST_PYLON = new BlockPos(14, 1, 3);
	private static final BlockPos PORTER_AT = new BlockPos(12, 1, 2);
	/** The garden: a Dewmoss and a Gloamcap two blocks apart (a mixed bed), and a Sunpetal to graft. */
	private static final BlockPos MOSS_BED = new BlockPos(10, 1, 11);
	private static final BlockPos CAP_BED = new BlockPos(12, 1, 11);
	private static final BlockPos SUN_BED = new BlockPos(10, 1, 14);
	private static final String DEWLIT = "jugcraft:dewlit";
	/** Root and Rivet's four steps and a margin (ConcordanceRitualGameTests.rootAndRivetAttunesOnePlateOnce waits the same). */
	private static final long RITE_DONE = RitualMachine.STEP_TICKS * 4L + 20;

	/** The top face of the block at {@code at}, as a use from above hits it. */
	private static BlockHitResult top(GameTestHelper helper, BlockPos at) {
		BlockPos absolute = helper.absolutePos(at);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
	}

	/**
	 * {@code player} uses {@code held}, put in the main hand, on the top of the block at {@code at} by the server's own path
	 * (ServerPlayerGameMode.useItemOn: the block's use, then an empty hand's, then the item's), its rate limits forgotten.
	 */
	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos at, ItemStack held) {
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND, top(helper, at));
	}

	/** {@code player} places {@code held} on the top of the block at {@code at}, as its item's own use does (a block above it, a porter). */
	private static InteractionResult place(GameTestHelper helper, ServerPlayer player, BlockPos at, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top(helper, at)));
	}

	/**
	 * What a crafting table makes of {@code grid} (three rows of three, null for an empty cell) by the server's own recipe
	 * matcher, asserted to be {@code count} of {@code item}; taking it uses up one of each ingredient.
	 */
	private static ItemStack craft(GameTestHelper helper, Item item, int count, ItemStack... grid) {
		List<ItemStack> cells = new ArrayList<>();
		for (ItemStack cell : grid) {
			cells.add(cell == null ? ItemStack.EMPTY : cell);
		}
		CraftingInput input = CraftingInput.of(3, 3, cells);
		Optional<RecipeHolder<CraftingRecipe>> recipe = RecipeManager.createCheck(RecipeType.CRAFTING).getRecipeFor(input, helper.getLevel());
		ItemStack made = recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
		helper.assertTrue(made.is(item) && made.getCount() == count, "A crafting table makes " + count + " " + item + ": " + made);
		for (ItemStack cell : cells) {
			if (!cell.isEmpty()) {
				cell.shrink(1);
			}
		}
		return made;
	}

	private static ItemStack of(Item item) {
		return new ItemStack(item);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	/** Takes the first stack of {@code item} out of {@code player}'s inventory (a product, a record or a return), or EMPTY. */
	private static ItemStack take(ServerPlayer player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack fullLantern(ServerLevel level) {
		ItemStack lantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		KindledLanternItem.set(lantern, KindledLanternItem.CAPACITY, ConcordanceProgress.now(level), false);
		return lantern;
	}

	private static Organism organism(String id) {
		Organism organism = Garden.catalog().organism(id);
		if (organism == null) {
			throw new IllegalStateException("no organism " + id);
		}
		return organism;
	}

	/**
	 * A fresh survival player standing in the circle: First Light understood by three luminous specimens examined in the
	 * dark, then, granted as the routes grant their research, the Alembic Arts, Circle Lore and the Binding Arts (still a
	 * Practitioner: four entries) and Verdant Husbandry (five entries in five traditions: the generalist's Adept), as
	 * tools/concordance_journey.py WORKSHOP's steps say.
	 */
	private static ServerPlayer workshopper(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos feet = helper.absolutePos(IN_CIRCLE);
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
		RateGate.forget(player.getUUID());
		for (String specimen : List.of("minecraft:amethyst_shard", "minecraft:glow_berries", "minecraft:glow_ink_sac")) {
			ConcordanceProgress.record(player, new Evidence.Examined(specimen, 0));
		}
		helper.assertValueEqual(stage(player), "practitioner", "the workshop: understanding First Light makes a Practitioner");
		grant(player, List.of(Alchemy.RESEARCH, "jugcraft:circle_lore", Workers.RESEARCH), ResearchState.UNDERSTOOD);
		helper.assertValueEqual(stage(player), "practitioner", "the workshop: four entries understood are still a Practitioner's");
		grant(player, List.of(Garden.RESEARCH), ResearchState.UNDERSTOOD);
		helper.assertValueEqual(stage(player), "adept", "the workshop: Verdant Husbandry makes the fifth entry in a fifth tradition");
		helper.assertValueEqual(holding(player, "adept"), List.of("generalist"), "the workshop: an Adept by the generalist's route alone");
		return player;
	}

	/** C-10: every recipe the workshop's rows name loads (check_acquisition holds WORKSHOP_RECIPES to tools/acquisition.py). */
	@GameTest(maxTicks = 20)
	public void theWorkshopsRecipesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> missing = new ArrayList<>();
		for (String name : WORKSHOP_RECIPES) {
			if (level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isEmpty()) {
				missing.add(name);
			}
		}
		helper.assertTrue(missing.isEmpty(), "These workshop recipes did not load: " + missing);
		helper.succeed();
	}

	/**
	 * Concordance Chunk C, C-10 (09:140): the living workshop from a manual start, played by one survival player with real
	 * blocks (tools/concordance_journey.py WORKSHOP, step by step). Given only vanilla materials, two Thallite ingots, two
	 * Kindled Lanterns kindled and infused full at a Lampwright's Bench (amethyst shards, ConcordanceGameTests) and a blank
	 * formula, they craft Mendvetch by the server's own recipe matcher; brew the Kiln Draught over a campfire and the
	 * Stillroot Poultice over a magma block by hand, recording the poultice's process; fill a Lesser Circle's pylons from a
	 * lantern and perform Root and Rivet; craft a Crucible Stand from its foundation and set a crafted crucible on it, fed
	 * JE by a coal generator through a cable and water by an electric pump; set the recorded formula, whose Mendvetch a
	 * Clockwork Porter (set down, keyed to a crafted Courier Post and fuelled by a lantern-filled pylon) brings by itself
	 * from a chest at the post; and lay a garden of crafted beds and seeds with a mixed bed and a Dewlit graft. Nothing is
	 * lost or made twice: every crafted Mendvetch is spent once, the post's chest gives exactly two, the courier ledger
	 * agrees with its cargo, and the powered poultice is the hand-made one; the stand pays nothing while the crucible waits
	 * for its Mendvetch.
	 */
	// The drone tests' 16 by 8 by 16 air box, reused: a structure file of its own would make tools/select_client_tests.py
	// count a shared file and run every client class.
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 900)
	public void theLivingWorkshopFromAManualStart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		ServerPlayer player = workshopper(helper);

		// Step 1, the manual start: Mendvetch crafted (four crafts, two each: two for the poultice, four for the rite, two for
		// the porter to bring); two crucibles over the heat each product stirs at.
		int crafted = 0;
		for (int i = 0; i < 4; i++) {
			crafted += craft(helper, Garden.MENDVETCH, 2, of(Items.WHEAT_SEEDS), of(Items.BONE_MEAL), of(Items.AMETHYST_SHARD), null, null, null,
					null, null, null).getCount();
		}
		ItemStack mendvetch = new ItemStack(Garden.MENDVETCH, crafted);
		helper.setBlock(KILN.below(), Blocks.CAMPFIRE);
		helper.setBlock(KILN, JugcraftConcordance.CRUCIBLE);
		helper.setBlock(POULTICE.below(), Blocks.MAGMA_BLOCK);
		helper.setBlock(POULTICE, JugcraftConcordance.CRUCIBLE);
		CrucibleBlockEntity kiln = helper.getBlockEntity(KILN, CrucibleBlockEntity.class);
		CrucibleBlockEntity poultice = helper.getBlockEntity(POULTICE, CrucibleBlockEntity.class);
		for (BlockPos at : List.of(KILN, POULTICE)) {
			helper.assertTrue(use(helper, player, at, PotionContents.createItemStack(Items.POTION, Potions.WATER)).consumesAction(),
					"A bottle of water goes into the crucible at " + at);
		}
		use(helper, player, KILN, of(Items.SUGAR));
		use(helper, player, KILN, of(Items.SUGAR));
		use(helper, player, POULTICE, mendvetch);
		use(helper, player, POULTICE, mendvetch);
		helper.assertTrue(mendvetch.getCount() == 6 && poultice.mixture().parts() == 1, "Two crafted Mendvetch went in: " + mendvetch.getCount());

		// Step 2: a Lesser Circle, its four pylons filled from one lantern, and Root and Rivet on a crafted plate.
		helper.setBlock(ANCHOR, JugcraftConcordance.CIRCLE_ANCHOR);
		List<BlockPos> pylons = new ArrayList<>();
		for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:lesser_circle").parts()) {
			BlockPos at = ANCHOR.offset(part.offset().x(), part.offset().y(), part.offset().z());
			if (part.role() == StructurePattern.Role.CHANNEL) {
				helper.setBlock(at, JugcraftConcordance.LEY_PYLON);
				pylons.add(at);
			} else if (part.role() == StructurePattern.Role.BOUNDARY) {
				helper.setBlock(at, JugcraftConcordance.WARDING_STONE);
			}
		}
		ItemStack lantern = fullLantern(level);
		for (BlockPos at : pylons) {
			use(helper, player, at, lantern);
			helper.assertValueEqual(helper.getBlockEntity(at, LeyPylonBlockEntity.class).ley(), 10L, "15 Radiance poured make 10 Ley Charge at " + at);
		}
		helper.assertValueEqual(KindledLanternItem.remaining(lantern, ConcordanceProgress.now(level)), 4, "four pours from one lantern");
		Item ingot = item("jugcraft:thallite_ingot");
		ItemStack plate = craft(helper, item("jugcraft:thallite_plate"), 1, of(ingot), null, null, of(ingot), null, null, null, null, null);
		use(helper, player, ANCHOR, plate);
		use(helper, player, ANCHOR, new ItemStack(Items.COPPER_INGOT, 4));
		use(helper, player, ANCHOR, mendvetch.split(4));
		use(helper, player, ANCHOR, ItemStack.EMPTY);
		CircleAnchorBlockEntity anchor = helper.getBlockEntity(ANCHOR, CircleAnchorBlockEntity.class);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING && "jugcraft:root_and_rivet".equals(anchor.run().ritual())
				&& ConcordanceProgress.currentFocus(player) == 20 - 6 && plate.isEmpty(), "Root and Rivet begins for six Focus: " + anchor.run());

		// Steps 3 and 4, laid out now: the factory's power and water, the hopper's bowl, the post and its porter.
		helper.setBlock(STAND_GENERATOR, JugcraftMachines.MACHINES.get(MachineKind.COAL_GENERATOR).defaultBlockState());
		helper.setBlock(CABLE, JugcraftMachines.COPPER_CABLE);
		helper.setBlock(PUMP_GENERATOR, JugcraftMachines.MACHINES.get(MachineKind.COAL_GENERATOR).defaultBlockState());
		for (BlockPos generator : List.of(STAND_GENERATOR, PUMP_GENERATOR)) {
			helper.getBlockEntity(generator, MachineBlockEntity.class).setItem(0, of(Items.COAL));
		}
		// A spring walled in stone (loose water would run into the next test), the pump on it and a pipe to the crucible's west.
		for (BlockPos wall : List.of(SPRING.west(), SPRING.east(), SPRING.north(), SPRING.south())) {
			helper.setBlock(wall, Blocks.STONE);
		}
		helper.setBlock(SPRING, Blocks.WATER);
		helper.setBlock(SPRING.above(), JugcraftFluids.ELECTRIC_PUMP);
		helper.setBlock(STAND.above().west(), JugcraftFluids.BRONZE_FLUID_PIPE);
		helper.setBlock(HOPPER, Blocks.HOPPER);
		helper.getBlockEntity(HOPPER, HopperBlockEntity.class).setItem(0, of(Items.BOWL));
		ItemStack post = craft(helper, item("jugcraft:courier_post"), 1, of(Items.OAK_PLANKS), of(Items.BARREL), of(Items.OAK_PLANKS),
				of(Items.OAK_PLANKS), of(Items.WRITABLE_BOOK), of(Items.OAK_PLANKS), of(Items.OAK_PLANKS), null, of(Items.OAK_PLANKS));
		helper.assertTrue(place(helper, player, POST.below(), post).consumesAction() && post.isEmpty()
				&& helper.getBlockEntity(POST, CourierPostBlockEntity.class).mayUse(player.getUUID()), "The crafted post is set down, its placer's");
		helper.setBlock(CHEST, Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(CHEST, ChestBlockEntity.class);
		chest.setItem(0, mendvetch.split(2));
		helper.setBlock(POST_PYLON, JugcraftConcordance.LEY_PYLON);
		use(helper, player, POST_PYLON, fullLantern(level));
		ItemStack porterItem = craft(helper, Workers.PORTER, 1, of(Items.COPPER_INGOT), of(Items.BARREL), of(Items.COPPER_INGOT),
				of(Items.COPPER_INGOT), of(Items.REDSTONE), of(Items.COPPER_INGOT), of(Items.IRON_INGOT), null, of(Items.IRON_INGOT));
		helper.assertTrue(place(helper, player, PORTER_AT.below(), porterItem).consumesAction() && porterItem.isEmpty(),
				"With the Binding Arts understood, a Clockwork Porter is set down");
		List<ClockworkPorterEntity> porters = level.getEntitiesOfClass(ClockworkPorterEntity.class, new AABB(helper.absolutePos(PORTER_AT)).inflate(1.0));
		helper.assertTrue(porters.size() == 1, "One porter stands by the post: " + porters);
		ClockworkPorterEntity porter = porters.get(0);
		ItemStack key = craft(helper, Workers.PORTER_KEY, 1, of(Items.COPPER_INGOT), of(Items.COPPER_INGOT), of(Items.IRON_NUGGET),
				null, null, null, null, null, null);
		place(helper, player, POST, key);
		key.getItem().interactLivingEntity(key, player, porter, InteractionHand.MAIN_HAND);
		helper.assertTrue(porter.post() != null && porter.post().pos().equals(helper.absolutePos(POST)), "The key binds the porter to the post");

		// Step 5: three crafted beds laid by a Greenwarden (awake), Dewmoss and Gloamcap two blocks apart, a Sunpetal grafted Dewlit.
		ItemStack beds = craft(helper, item("jugcraft:verdant_bed"), 3, of(Items.MOSS_BLOCK), of(Items.BONE_MEAL), of(Items.MOSS_BLOCK),
				of(Items.DIRT), of(Items.DIRT), of(Items.DIRT), null, null, null);
		for (BlockPos at : List.of(MOSS_BED, CAP_BED, SUN_BED)) {
			place(helper, player, at.below(), beds);
			helper.assertTrue(helper.getBlockEntity(at, VerdantBedBlockEntity.class).awake(), "A Greenwarden's bed wakes as it is laid at " + at);
		}
		ItemStack dewmoss = craft(helper, Garden.DEWMOSS, 2, of(Items.MOSS_BLOCK), of(Items.KELP), of(Items.AMETHYST_SHARD), null, null, null, null, null, null);
		ItemStack gloamcap = craft(helper, Garden.GLOAMCAP, 2, of(Items.BROWN_MUSHROOM), of(Items.GLOW_LICHEN), of(Items.AMETHYST_SHARD), null, null, null,
				null, null, null);
		ItemStack sunpetal = craft(helper, Garden.SUNPETAL, 2, of(Items.SUNFLOWER), of(Items.AMETHYST_SHARD), of(Items.BONE_MEAL), null, null, null, null,
				null, null);
		place(helper, player, MOSS_BED, dewmoss);
		place(helper, player, CAP_BED, gloamcap);
		place(helper, player, SUN_BED, sunpetal);
		helper.assertTrue(helper.getBlockState(MOSS_BED.above()).is(Garden.DEWMOSS_CROP) && helper.getBlockState(CAP_BED.above()).is(Garden.GLOAMCAP_CROP)
				&& helper.getBlockState(SUN_BED.above()).is(Garden.SUNPETAL_CROP), "Each crafted seed planted on its bed");
		VerdantBedBlockEntity sunBed = helper.getBlockEntity(SUN_BED, VerdantBedBlockEntity.class);
		helper.assertTrue(use(helper, player, SUN_BED.above(), dewmoss).consumesAction() && DEWLIT.equals(sunBed.graft()) && dewmoss.isEmpty(),
				"A Dewmoss used on the Sunpetal grafts Dewlit onto its bed and is spent: " + sunBed.graft());
		VerdantBedBlockEntity mossBed = helper.getBlockEntity(MOSS_BED, VerdantBedBlockEntity.class);
		VerdantBedBlockEntity capBed = helper.getBlockEntity(CAP_BED, VerdantBedBlockEntity.class);
		mossBed.forgetSample();
		capBed.forgetSample();

		// Step 1's stirs: the poultice warm (its magma block holds 60), the Kiln Draught hot (the campfire, 120), each a stick.
		for (int tick : new int[] {30, 41, 52, 63}) {
			helper.runAtTickTime(tick, () -> {
				helper.assertTrue(poultice.band() == Band.WARM, "The poultice's crucible stirs warm: " + poultice.temperature());
				helper.assertTrue(use(helper, player, POULTICE, of(Items.STICK)).consumesAction(), "A warm stir");
			});
		}
		for (int tick : new int[] {80, 91}) {
			helper.runAtTickTime(tick, () -> {
				helper.assertTrue(kiln.band() == Band.HOT, "The Kiln Draught's crucible stirs hot: " + kiln.temperature());
				helper.assertTrue(use(helper, player, KILN, of(Items.STICK)).consumesAction(), "A hot stir");
			});
		}
		List<Operation> process = new ArrayList<>(List.of(new Operation.Water(1), new Operation.Add("jugcraft:mendvetch", "jugcraft:raw"),
				new Operation.Add("jugcraft:mendvetch", "jugcraft:raw")));
		for (int i = 0; i < 4; i++) {
			process.add(new Operation.Stir(Band.WARM));
		}
		String written = new Formula(process).text();
		AtomicReference<ItemStack> recorded = new AtomicReference<>(ItemStack.EMPTY);
		AtomicReference<ItemStack> handMade = new AtomicReference<>(ItemStack.EMPTY);
		helper.runAtTickTime(70, () -> {
			use(helper, player, POULTICE, of(JugcraftConcordance.FORMULA_ITEM));
			recorded.set(take(player, JugcraftConcordance.FORMULA_ITEM));
			helper.assertTrue(written.equals(recorded.get().get(JugcraftConcordance.FORMULA)), "A blank formula records the poultice's process: "
					+ recorded.get().get(JugcraftConcordance.FORMULA));
			use(helper, player, POULTICE, of(Items.BOWL));
			ItemStack salve = take(player, JugcraftConcordance.SALVE);
			Product product = Alchemy.product(salve);
			helper.assertTrue(product != null && Alchemy.STILLROOT_POULTICE.equals(product.id()) && new Brew(List.of(new Brew.Dose("minecraft:regeneration", 1, 450,
					false)), "minecraft:regeneration@1").equals(salve.get(JugcraftConcordance.BREW)), "By hand, a Stillroot Poultice of Regeneration II: " + salve);
			handMade.set(salve.copy());
			ServerPlayer friend = TestPlayers.mock(helper);
			friend.setGameMode(GameType.SURVIVAL);
			friend.setHealth(10.0F);
			player.setItemInHand(InteractionHand.MAIN_HAND, salve);
			RateGate.forget(player.getUUID());
			salve.getItem().interactLivingEntity(salve, player, friend, InteractionHand.MAIN_HAND);
			MobEffectInstance mending = friend.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(mending != null && mending.getAmplifier() == 1 && mending.getDuration() == 450 && salve.isEmpty()
					&& player.getInventory().countItem(Items.BOWL) == 1, "On a friend it mends (Regeneration II, 450 ticks) and the bowl comes back: " + mending);
		});
		helper.runAtTickTime(102, () -> {
			use(helper, player, KILN, of(Items.GLASS_BOTTLE));
			ItemStack draught = take(player, JugcraftConcordance.DRAUGHT);
			Product product = Alchemy.product(draught);
			helper.assertTrue(product != null && "jugcraft:kiln_draught".equals(product.id()) && new Brew(List.of(new Brew.Dose("minecraft:fire_resistance", 0,
					1722, false)), "minecraft:fire_resistance@0").equals(draught.get(JugcraftConcordance.BREW)), "From sugar, a Kiln Draught of 1722 ticks: " + draught);
			draught.finishUsingItem(level, player);
			MobEffectInstance resistance = player.getEffect(MobEffects.FIRE_RESISTANCE);
			helper.assertTrue(resistance != null && resistance.getDuration() == 1722 && !MoltenSlagBlock.harms(player),
					"Drunk, it keeps the Cinder Kiln's slag off its drinker: " + resistance);
		});

		// Step 3: the rite done, the foundation crafted into a stand; a crafted crucible on it; the recorded formula set.
		AtomicReference<CrucibleBlockEntity> powered = new AtomicReference<>();
		helper.runAtTickTime(RITE_DONE, () -> {
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.COMPLETE && anchor.output().is(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION),
					"Root and Rivet made a foundation: " + anchor.run());
			helper.assertValueEqual(anchor.spent(), new Readiness.Spent(6, 32, 4, 0, 0), "spent in all: 6 Focus, 32 Ley Charge");
			for (BlockPos at : pylons) {
				helper.assertValueEqual(helper.getBlockEntity(at, LeyPylonBlockEntity.class).ley(), 2L, "8 of the lantern's 10 Ley Charge drawn at " + at);
			}
			use(helper, player, ANCHOR, ItemStack.EMPTY);
			ItemStack foundation = take(player, JugcraftConcordance.ROOTED_THALLITE_FOUNDATION);
			helper.assertTrue(foundation.getCount() == 1 && anchor.offerings().stream().allMatch(ItemStack::isEmpty), "The participant took it; nothing is left");
			ItemStack stand = craft(helper, JugcraftConcordance.CRUCIBLE_STAND.asItem(), 1, of(Items.IRON_INGOT), of(Items.COPPER_INGOT), of(Items.IRON_INGOT),
					of(Items.IRON_INGOT), of(Items.COPPER_INGOT), of(Items.IRON_INGOT), of(Items.IRON_INGOT), foundation, of(Items.IRON_INGOT));
			helper.assertTrue(foundation.isEmpty(), "The stand takes the foundation");
			ItemStack crucible = craft(helper, JugcraftConcordance.CRUCIBLE.asItem(), 1, of(Items.COPPER_INGOT), null, of(Items.COPPER_INGOT),
					of(Items.COPPER_INGOT), of(Items.CAULDRON), of(Items.COPPER_INGOT), of(Items.BRICKS), of(Items.BRICKS), of(Items.BRICKS));
			helper.assertTrue(place(helper, player, STAND.below(), stand).consumesAction() && place(helper, player, STAND, crucible).consumesAction()
					&& helper.getBlockState(STAND).is(JugcraftConcordance.CRUCIBLE_STAND) && helper.getBlockState(STAND.above()).is(JugcraftConcordance.CRUCIBLE),
					"The stand is set down and the crucible on it");
			CrucibleBlockEntity on = helper.getBlockEntity(STAND.above(), CrucibleBlockEntity.class);
			helper.assertTrue(use(helper, player, STAND.above(), recorded.get()).consumesAction() && on.program() != null
					&& player.getUUID().equals(on.formulaOwner()), "The recorded formula is set; its setter is the workshopper");
			powered.set(on);
		});

		// What the powered crucible, its stand and its porter do, tick by tick (Step 4): the request it files, and whether the
		// stand paid while the crucible waited for its Mendvetch.
		AtomicLong request = new AtomicLong();
		AtomicBoolean waited = new AtomicBoolean();
		AtomicBoolean paidWhileWaiting = new AtomicBoolean();
		AtomicBoolean heated = new AtomicBoolean();
		helper.onEachTick(() -> {
			CrucibleBlockEntity on = powered.get();
			if (on == null) {
				return;
			}
			boolean heats = helper.getBlockEntity(STAND, CrucibleStandBlockEntity.class).plan(level).heats();
			Diagnostic waiting = on.waiting();
			if (waiting != null && waiting.code() == Code.MISSING_INGREDIENT) {
				waited.set(true);
				paidWhileWaiting.compareAndSet(false, heats);
			}
			heated.compareAndSet(false, heats);
			if (request.get() == 0L && on.request() > 0L) {
				request.set(on.request());
			}
		});

		Organism moss = organism("jugcraft:dewmoss");
		Organism cap = organism("jugcraft:gloamcap");
		helper.succeedWhen(() -> {
			CrucibleBlockEntity on = powered.get();
			helper.assertTrue(on != null, "The stand is not set yet");
			ItemStack made = on.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 1 && ItemStack.isSameItemSameComponents(made, handMade.get()),
					"Waiting for the powered poultice, the hand-made one's twin: " + made + " (step " + on.step() + ", " + on.temperature() + ", " + on.waiting() + ")");
			CourierLedger ledger = CourierLedger.of(level.getServer());
			helper.assertTrue(request.get() > 0L && ledger.ledger().get(request.get()) == null && ledger.problems().isEmpty(),
					"The crucible's request was filed, delivered and closed, and the ledger agrees with its cargo: " + request.get() + ", " + ledger.problems());
			helper.assertTrue(chest.countItem(Garden.MENDVETCH) == 0 && mendvetch.isEmpty(),
					"The porter brought exactly the chest's two; every crafted Mendvetch is spent once");
			helper.assertTrue(waited.get() && !paidWhileWaiting.get() && heated.get(),
					"The stand heated for the stirs and paid nothing while the crucible waited for its Mendvetch");
			helper.assertTrue(helper.getBlockEntity(HOPPER, HopperBlockEntity.class).isEmpty() && on.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty(),
					"The hopper's one bowl was bottled");
			helper.assertTrue(mossBed.habitat(level) != null && mossBed.mixed(moss) && capBed.habitat(level) != null && capBed.mixed(cap),
					"Dewmoss and Gloamcap two blocks apart are a mixed bed, each way");
		});
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-09: the pact hall, whole

	/**
	 * Every recipe the pact hall's rows name (tools/acquisition.py PACT_HALL; check_acquisition holds this list to the rows).
	 */
	private static final List<String> PACT_HALL_RECIPES = List.of("verdant_bed", "sunpetal", "assayers_scale", "observatory", "astrolabe",
			"pact_perch", "spirit_anchor", "courier_post", "porter_key");

	// The pact hall in the yard (jugcraft-test:drone_depot, its floor laid first). WorkshopHands' corner (x 1 to 3, z 2 to 5)
	// keeps the workshop hand's post, chest, crucible and anchor, as the hand's own tests place them.
	/** Where Styxhexenhammer stands, the perch, the observatory, the Verdant Bed (its crop above it) and the Assayer's Scale. */
	private static final BlockPos STYX_AT = new BlockPos(13, 1, 2);
	private static final BlockPos PERCH_AT = new BlockPos(10, 1, 10);
	private static final BlockPos OBSERVATORY_AT = new BlockPos(13, 1, 10);
	private static final BlockPos BED_AT = new BlockPos(6, 1, 13);
	private static final BlockPos SCALE_AT = new BlockPos(13, 1, 13);
	/** Inventory slots past the hotbar for what the holder carries, so a use with the main hand never touches them. */
	private static final int COMPACT_SLOT = 9;
	private static final int ASTROLABE_SLOT = 20;
	private static final int OFFERING_SLOT = 21;
	/** Noon by the world's clock, for Stolas's thinking (ConcordanceWorkerGameTests' NOON is private). */
	private static final long PACT_NOON = 6000L;

	/** What Styx says to {@code listener} after his own line, as translation keys (his line first, as StyxGameTests reads it). */
	private static List<String> styxHint(GameTestHelper helper, Styxhexenhammer styx, ServerPlayer listener) {
		List<String> said = styx.says(listener).stream().map(ConcordanceWorkerGameTests::key).toList();
		helper.assertTrue(!said.isEmpty() && said.getFirst().startsWith("message.jugcraft.styx.line."), "Styx's own line comes first: " + said);
		return said.subList(1, said.size());
	}

	/** The keys of the journal's pacts section for {@code player}, as Journal.build sends it; empty when it is not shown. */
	private static List<String> pactsSection(ServerPlayer player) {
		return Journal.build(player).stream().filter(section -> section.id().equals("pacts")).findFirst()
				.map(section -> section.lines().stream().map(line -> ConcordanceWorkerGameTests.key(line.text())).toList()).orElse(List.of());
	}

	/**
	 * Styx's hint and the journal's pacts section name the same next step of Stolas's Regard for {@code player}, Styx with the
	 * sky step while {@code sky} (none recorded), the journal with the sky step apart: the route with Styx and without him.
	 */
	private static void sameStep(GameTestHelper helper, Styxhexenhammer styx, ServerPlayer player, String step, boolean sky, String when) {
		List<String> hint = sky ? List.of("message.jugcraft.styx.stolas." + step, "message.jugcraft.styx.stolas.sky")
				: List.of("message.jugcraft.styx.stolas." + step);
		helper.assertValueEqual(styxHint(helper, styx, player), hint, when + ": Styx tells the holder their own next step");
		List<String> lines = pactsSection(player);
		helper.assertTrue(lines.size() >= 2 && lines.get(0).equals("journal.jugcraft.pacts.regard." + step)
				&& lines.get(1).equals(sky ? "journal.jugcraft.pacts.sky_needed" : "journal.jugcraft.pacts.sky_recorded"),
				when + ": the journal's pacts section names the same step without him, and the sky apart: " + lines);
	}

	/** D-09: every recipe the pact hall's rows name loads (check_acquisition holds PACT_HALL_RECIPES to tools/acquisition.py PACT_HALL). */
	@GameTest(maxTicks = 20)
	public void thePactHallsRecipesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> missing = new ArrayList<>();
		for (String name : PACT_HALL_RECIPES) {
			if (level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isEmpty()) {
				missing.add(name);
			}
		}
		helper.assertTrue(missing.isEmpty(), "These pact-hall recipes did not load: " + missing);
		helper.succeed();
	}

	/**
	 * Concordance Chunk D, D-09 (E7, 09:174; tools/concordance_journey.py PACT, step by step): the whole pact path on one
	 * ordinary survival player, in order. At each stage Styx tells them their own next step and the journal's pacts section
	 * names the same one without him. They examine a feather, harvest a ripe Sunpetal by hand and dissolve iron nuggets at an
	 * Assayer's Scale (no notes, never one of Styx's flowers); record the full moon at an observatory, which asks no research;
	 * keep that observatory once they understand Celestial Attunement and let it gather eight full moons' resonance. They
	 * craft a Pact Perch and place it; opening it, Stolas arrives and names every term; accepting with an empty astrolabe is
	 * refused with its reason and spends nothing; filled at their observatory, they accept and the Open Hand Compact is spent
	 * once. Star-reckoning recalls the full moon, once a game day, under the claim a master's recall makes; an observation
	 * while the pact is active keeps its first obligation and masters Stolas's Regard once, which lets them seal a workshop
	 * hand at an anchor bound to their Courier Post: it carries the crucible's own request once, its glowstone conserved.
	 * Unpaid, the pact is warned, graced a day and suspended with nothing taken; an offering resumes it; away past a due date
	 * and a grace, they come back to a warning with the whole grace. Ended from the roster, what was paid ahead comes back
	 * once, Stolas makes his departing gesture and is gone, the departure is kept, and the Bound Wills and the courier ledger
	 * stay sound. The path takes one tick; the test then waits out Stolas's departing gesture.
	 */
	// The drone tests' 16 by 8 by 16 air box, as theLivingWorkshopFromAManualStart reuses it (no new structure file).
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 80)
	public void thePactHallFromStyxToDeparture(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		MinecraftServer server = level.getServer();
		PactDefinition offer = ConcordanceWorkerGameTests.stolasOffer();
		Pattern full = Sky.catalog().pattern(ConcordanceWorkerGameTests.FULL_MOON);
		ServerPlayer holder = TestPlayers.survivor(helper);
		UUID id = holder.getUUID();
		RateGate.forget(id);
		ConcordanceWorkerGameTests.beside(helper, holder, PERCH_AT);
		Inventory inventory = holder.getInventory();
		for (int i = 0; i < offer.compact().size(); i++) {
			PactDefinition.Cost cost = offer.compact().get(i);
			inventory.setItem(COMPACT_SLOT + i, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(cost.item())), cost.count()));
		}
		ItemStack astrolabe = new ItemStack(Sky.ASTROLABE);
		inventory.setItem(ASTROLABE_SLOT, astrolabe);
		Styxhexenhammer styx = JugcraftStyx.WIZARD.create(level, EntitySpawnReason.COMMAND);
		helper.assertTrue(styx != null, "Styxhexenhammer creates on a server");
		Vec3 styxAt = helper.absoluteVec(Vec3.atBottomCenterOf(STYX_AT));
		styx.snapTo(styxAt.x, styxAt.y, styxAt.z, 0.0F, 0.0F);
		level.addFreshEntity(styx);
		helper.assertTrue(styxHint(helper, styx, holder).isEmpty() && pactsSection(holder).isEmpty() && StolasRegard.step(holder) == null,
				"A stranger to binding: Styx says only his line, and the journal has no pacts section");

		// PACT step 1: the Binding Arts understood (granted, as the journey grants a route's research); a feather examined.
		ConcordanceProgress.grant(holder, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(holder, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		sameStep(helper, styx, holder, "examine", true, "The Binding Arts understood");
		Examination.forget(id);
		ItemStack feather = new ItemStack(Items.FEATHER);
		helper.assertTrue(Examination.examine(holder, level, feather) != null && feather.getCount() == 1
				&& ConcordanceWorkerGameTests.regard(holder) == ResearchState.ENCOUNTERED, "A feather examined begins Stolas's Regard; the feather stays");
		sameStep(helper, styx, holder, "cultivate", true, "A feather examined");

		// Step 2: Verdant Husbandry understood; a ripe Sunpetal on a Verdant Bed harvested by hand, by the server's own use.
		ConcordanceProgress.grant(holder, Garden.RESEARCH, ResearchState.UNDERSTOOD);
		helper.setBlock(BED_AT, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
		OrganismCropBlock sunpetal = (OrganismCropBlock) Garden.SUNPETAL_CROP;
		helper.setBlock(BED_AT.above(), sunpetal.getStateForAge(OrganismCropBlock.MAX_AGE));
		helper.assertTrue(use(helper, holder, BED_AT.above(), ItemStack.EMPTY).consumesAction() && !sunpetal.isMaxAge(helper.getBlockState(BED_AT.above()))
				&& ConcordanceWorkerGameTests.regard(holder) == ResearchState.OBSERVED, "A ripe Sunpetal harvested by hand: Stolas's Regard is observed");
		sameStep(helper, styx, holder, "assay", true, "A crop harvested");

		// Step 3: the Assay understood; nine iron nuggets weighed, then dissolved, at an Assayer's Scale.
		ConcordanceProgress.grant(holder, Assaying.RESEARCH, ResearchState.UNDERSTOOD);
		helper.setBlock(SCALE_AT, Assaying.SCALE);
		ItemStack sample = new ItemStack(Items.IRON_NUGGET, 9);
		use(helper, holder, SCALE_AT, sample);
		helper.assertTrue(sample.getCount() == 9, "Weighed at the scale, nothing is taken yet");
		use(helper, holder, SCALE_AT, sample);
		helper.assertTrue(sample.isEmpty() && Assaying.balance(holder) > 0L && ConcordanceWorkerGameTests.regard(holder) == ResearchState.UNDERSTOOD,
				"Dissolved there: Stolas's Regard is understood");
		Set<String> evidence = ConcordanceProgress.knowledge(holder).progress(StolasRegard.RESEARCH).evidence().keySet();
		helper.assertTrue(evidence.contains("examine:minecraft:feather")
				&& evidence.stream().anyMatch(key -> key.startsWith("practice:" + Garden.ACTIVITY + ":"))
				&& evidence.stream().anyMatch(key -> key.startsWith("practice:" + Assaying.ACTIVITY + ":"))
				&& evidence.stream().noneMatch(key -> key.startsWith("notes:")), "Its evidence is the holder's own three deeds, no notes: " + evidence);
		sameStep(helper, styx, holder, "serve", true, "A sample dissolved");

		// Step 4, the sky step: an observatory its holder places (no Starwatcher yet, so it keeps no one) records the full moon.
		ObservatoryBlockEntity observatory = ConcordanceWorkerGameTests.observatory(helper, OBSERVATORY_AT);
		Sky.OBSERVATORY.setPlacedBy(level, helper.absolutePos(OBSERVATORY_AT), helper.getBlockState(OBSERVATORY_AT), holder, ItemStack.EMPTY);
		long g = level.getGameTime();
		helper.assertTrue(observatory.keeper() == null && !StolasRegard.skySeen(holder), "Placed by one who is no Starwatcher, it keeps no one; no sky yet");
		helper.assertTrue(observatory.observe(holder, level, ConcordanceWorkerGameTests.NIGHT_ZERO, g, ConcordanceWorkerGameTests.ONLY_MOON) == 1
				&& StolasRegard.skySeen(holder), "The full moon observed there: the sky step, which asks no research");
		sameStep(helper, styx, holder, "serve", false, "An observation recorded");

		// Step 5: Celestial Attunement understood, they align that observatory by using it; it gathers eight full moons.
		ConcordanceProgress.grant(holder, Sky.RESEARCH, ResearchState.UNDERSTOOD);
		helper.assertTrue(use(helper, holder, OBSERVATORY_AT, ItemStack.EMPTY).consumesAction() && id.equals(observatory.keeper()),
				"A Starwatcher now, its holder keeps the observatory");
		for (int k = 0; k < ObservatoryBlockEntity.CAPACITY / full.resonance(); k++) {
			helper.assertValueEqual(observatory.gather(level, ConcordanceWorkerGameTests.NIGHT_ZERO + k * 8L * Pacts.DAY, g + k * Calendar.minGap(full),
					ConcordanceWorkerGameTests.ONLY_MOON), "gathering", "Full moon " + k + " pays its keeper once");
		}
		helper.assertValueEqual(observatory.resonance(), (long) ObservatoryBlockEntity.CAPACITY, "Its store is full");

		// Step 6: the perch, crafted by its recipe and placed by hand; Stolas invited; a refusal; acceptance.
		ItemStack perchItem = craft(helper, Workers.PACT_PERCH_ITEM, 1, of(Items.STICK), of(Items.AMETHYST_SHARD), of(Items.STICK), null,
				of(Items.DARK_OAK_LOG), null, null, of(Items.POLISHED_DEEPSLATE), null);
		helper.assertTrue(place(helper, holder, PERCH_AT.below(), perchItem).consumesAction() && perchItem.isEmpty()
				&& helper.getBlockState(PERCH_AT).is(Workers.PACT_PERCH), "The perch, crafted by its recipe, is placed by hand");
		PactPerchBlockEntity perch = helper.getBlockEntity(PERCH_AT, PactPerchBlockEntity.class);
		helper.assertTrue(id.equals(perch.holder().owner()), "It records its owner");
		helper.assertTrue(use(helper, holder, PERCH_AT, ItemStack.EMPTY).consumesAction() && holder.containerMenu instanceof PactMenu,
				"Its owner opens it: the pact screen");
		PactMenu menu = (PactMenu) holder.containerMenu;
		PactStatePayload invited = menu.lastSent();
		helper.assertTrue(invited != null && invited.mode() == PactStatePayload.NEGOTIATE && invited.revision() == 1 && invited.notes().isEmpty()
				&& invited.offers(PactRequestPayload.ACCEPT) && invited.terms().stream().map(PactStatePayload.Term::key).toList().equals(PactStatePayload.TERMS)
				&& ConcordanceWorkerGameTests.keys(invited.beats())
						.equals(offer.beats().stream().map(beat -> ConcordanceWorkerGameTests.PACT_KEY + "stolas.beat." + beat).toList()),
				"Invited: his three beats and every term before anything is agreed, and nothing in the way: " + invited);
		UUID visitorId = perch.visitor();
		StolasEntity visitor = visitorId != null && level.getEntity(visitorId) instanceof StolasEntity found ? found : null;
		helper.assertTrue(visitor != null && visitor.owner() == null && helper.absolutePos(PERCH_AT).equals(visitor.perch()) && visitor.listening()
				&& visitor.cues().equals(List.of("arrive")), "Stolas alights on the perch, a visitor bound to no one, and listens");
		helper.assertTrue(BoundWills.of(server).ledger().heldBy(id).isEmpty() && perch.holder().agreement() == null, "Nothing is stored yet");
		PactStatePayload refused = ConcordanceWorkerGameTests.send(holder, menu, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(refused, Pacts.Outcome.CANNOT_AFFORD.id) && !refused.result().get().done()
				&& ConcordanceWorkerGameTests.said(refused).equals(ConcordanceWorkerGameTests.PACT_KEY + "answer.cannot_afford")
				&& Integer.valueOf(offer.resonance()).equals(ConcordanceWorkerGameTests.arg(refused.result().get().message(), 0)),
				"Accepting with an empty astrolabe is refused, and says why: the compact's resonance");
		helper.assertTrue(ConcordanceWorkerGameTests.pocketed(holder, 1) && AstrolabeItem.charge(astrolabe) == 0 && perch.holder().agreement() == null
				&& BoundWills.of(server).ledger().heldBy(id).isEmpty() && visitorId.equals(perch.visitor()) && !visitor.gone(),
				"Nothing is spent or sealed, and Stolas stays");
		helper.assertValueEqual(observatory.fill(holder, level, astrolabe), (long) AstrolabeItem.CAPACITY, "Their observatory fills their astrolabe");
		long sealedAt = level.getGameTime();
		PactStatePayload sealed = ConcordanceWorkerGameTests.send(holder, menu, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(sealed, PactMenu.SEALED) && sealed.result().get().done() && sealed.mode() == PactStatePayload.KEEP,
				"Accepted: sealed, and kept here");
		int afterCompact = AstrolabeItem.CAPACITY - offer.resonance();
		helper.assertTrue(ConcordanceWorkerGameTests.pocketed(holder, 0) && AstrolabeItem.charge(astrolabe) == afterCompact,
				"The Open Hand Compact is spent once: its materials, and its resonance from the astrolabe");
		BoundWill will = ConcordanceWorkerGameTests.recordOf(level, holder);
		helper.assertTrue(will != null && PactDefinition.STOLAS.equals(will.counterpart()) && visitorId.equals(will.pact().presence())
				&& id.equals(visitor.owner()) && will.pact().upkeep().due() == sealedAt + offer.period() && ConcordanceWorkerGameTests.escrowed(level, will.id()) == 0
				&& "pact".equals(WorkerRoster.of(server).workers(id).get(visitorId).kind()),
				"One Bound Will, its presence the visitor, on the roster as a pact; the compact paid the first period: " + will);
		UUID record = will.id();
		PactStatePayload again = ConcordanceWorkerGameTests.send(holder, menu, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(again, Pacts.Outcome.TAKEN.id) && ConcordanceWorkerGameTests.pocketed(holder, 0)
				&& AstrolabeItem.charge(astrolabe) == afterCompact && BoundWills.of(server).ledger().heldBy(id).size() == 1,
				"Accepting again seals and spends nothing");
		helper.assertTrue(BoundWills.of(server).problems().isEmpty(), "Sealed, the Bound Wills are sound: " + BoundWills.of(server).problems());

		// Step 7: Star-reckoning through the screen, once a game day, on the master's claim; the first obligation.
		long clock = Sky.time(level);
		List<PactStatePayload.Choice> patterns = again.patterns();
		int moon = IntStream.range(0, patterns.size()).filter(i -> patterns.get(i).id().equals(ConcordanceWorkerGameTests.FULL_MOON)).findFirst().orElse(-1);
		helper.assertTrue(moon >= 0 && patterns.get(moon).ready() && again.offers(PactRequestPayload.RECALL), "Kept here, the screen offers the full moon to recall");
		PactStatePayload recalled = ConcordanceWorkerGameTests.send(holder, menu, PactRequestPayload.RECALL, moon);
		Attunement attuned = holder.getAttached(Sky.ATTUNEMENT);
		int afterRecall = afterCompact - full.recallCost();
		helper.assertTrue(ConcordanceWorkerGameTests.answered(recalled, Pacts.Reckoning.RECALLED.id) && AstrolabeItem.charge(astrolabe) == afterRecall
				&& attuned != null && attuned.recalled() && ConcordanceWorkerGameTests.FULL_MOON.equals(attuned.pattern())
				&& attuned.until() == sealedAt + Calendar.RECALL_TICKS && ConcordanceWorkerGameTests.done(level, perch, sealedAt) == 1,
				"Star-reckoning: the full moon recalled at its recall cost, for 2,400 ticks, the game day's one");
		helper.assertTrue(new AstralLedger.Claim(Calendar.occurrence(full, clock), sealedAt)
				.equals(AstralClaims.of(server).ledger(id).claims().get(AstrolabeItem.recallKey(full))), "claimed under the key a master's recall claims");
		PactStatePayload twice = ConcordanceWorkerGameTests.send(holder, menu, PactRequestPayload.RECALL, moon);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(twice, Pacts.Reckoning.QUOTA_SPENT.id) && AstrolabeItem.charge(astrolabe) == afterRecall,
				"A second the same game day: the quota is spent, and nothing with it");
		helper.assertTrue(StarReckoning.reckon(level, holder, perch.holder(), ConcordanceWorkerGameTests.FULL_MOON, clock, sealedAt + Pacts.DAY)
				== Pacts.Reckoning.RECALLED_ALREADY && AstrolabeItem.charge(astrolabe) == afterRecall
				&& ConcordanceWorkerGameTests.done(level, perch, sealedAt + Pacts.DAY) == 0,
				"The next game day the same occurrence is recalled already, by the one shared claim: nothing spent, no quota used");
		holder.closeContainer();
		// The workshop hand's post, chest and crucible (its request filed); an anchor bound to the post before the Regard is mastered.
		WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
		CrucibleBlockEntity crucible = WorkshopHands.crucible(helper, WorkshopHands.CRUCIBLE, holder);
		long request = crucible.request();
		List<Long> requests = List.of(request);
		SpiritAnchorBlockEntity anchor = WorkshopHands.anchor(helper, holder, WorkshopHands.ANCHOR);
		helper.assertTrue(WorkshopHands.use(helper, holder, WorkshopHands.keyFor(helper, holder, WorkshopHands.POST), WorkshopHands.ANCHOR) == InteractionResult.SUCCESS
				&& anchor.post() != null, "They bind a Spirit Anchor to their Courier Post with a Porter Key"
						+ WorkshopHands.why(helper, holder, WorkshopHands.ANCHOR, WorkshopHands.POST));
		helper.assertTrue(WorkshopHands.UNKNOWN.equals(WorkshopHands.key(WorkshopHands.seal(helper, holder, anchor))) && anchor.agreement() == null,
				"Stolas's Regard understood, not mastered: no workshop hand yet");
		WorkshopHands.conserved(helper, requests, WorkshopHands.PLAIN, 10, 0, "the crucible's request filed", chest, crucible);
		helper.assertTrue(observatory.observe(holder, level, ConcordanceWorkerGameTests.NIGHT_ZERO, sealedAt + 200, ConcordanceWorkerGameTests.ONLY_MOON) == 1
				&& ConcordanceWorkerGameTests.regard(holder) == ResearchState.MASTERED && ConcordanceWorkerGameTests.served(holder) == 1 && StarReckoning.kept(holder)
				&& ConcordanceWorkerGameTests.earned(holder, ConcordanceProgress.advancementFor(StolasRegard.RESEARCH, ResearchState.MASTERED)),
				"An observation while the pact is active keeps its first obligation: Stolas's Regard is mastered");
		helper.assertTrue(observatory.observe(holder, level, ConcordanceWorkerGameTests.NIGHT_ZERO, sealedAt + 400, ConcordanceWorkerGameTests.ONLY_MOON) == 1
				&& ConcordanceWorkerGameTests.served(holder) == 1, "Once: a second observation keeps nothing more");
		sameStep(helper, styx, holder, "kept", false, "Stolas's Regard mastered");
		styx.discard();

		// Step 8: the workshop hand, unlocked by the mastery, carries the crucible's own request once.
		RateGate.forget(id);
		helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, holder, anchor)), WorkshopHands.SEALED, "Mastered: they seal a workshop hand there");
		WorkshopHandEntity hand = level.getEntity(anchor.spirit()) instanceof WorkshopHandEntity found ? found : null;
		helper.assertTrue(hand != null && id.equals(hand.owner()) && BoundWills.of(server).ledger().heldBy(id).size() == 2
				&& BoundWills.of(server).problems().isEmpty(), "Its own Bound Will beside the pact's; the records sound");
		UUID handRecord = anchor.agreement().record();
		WorkshopHands.stand(helper, hand, 2.5, 1.0, 3.5);
		long morning = WorkshopHands.morning(level, 5_200_000L);
		long in = WorkshopHands.inHours();
		CourierLedger ledger = CourierLedger.of(server);
		helper.assertTrue(hand.think(level, in, morning) == Status.TRAVELLING && hand.task() == request, "The hand claims the crucible's own request");
		WorkshopHands.conserved(helper, requests, WorkshopHands.PLAIN, 10, 0, "claimed", chest, crucible);
		helper.assertTrue(hand.think(level, in, morning + 20) == Status.WORKING && ledger.ledger().get(request).progress().carried() == 2
				&& WorkshopHands.matching(chest, WorkshopHands.PLAIN) == 8, "It picks up the two the crucible asked for");
		WorkshopHands.conserved(helper, requests, WorkshopHands.PLAIN, 10, 0, "carried", chest, crucible);
		helper.assertTrue(hand.think(level, in, morning + 40) == Status.WORKING && ledger.ledger().get(request) == null
				&& WorkshopHands.matching(crucible, WorkshopHands.PLAIN) == 2 && anchor.agreement().done(morning + 40) == 1,
				"and delivers them once: the request closed, one load counted");
		WorkshopHands.conserved(helper, requests, WorkshopHands.PLAIN, 10, 0, "delivered", chest, crucible);
		helper.assertTrue(ConcordanceProgress.knowledge(holder).progress(Workers.RESEARCH).evidence().containsKey("practice:" + Workers.ACTIVITY + ":spirit")
				&& ConcordanceProgress.knowledge(holder).state(Workers.RESEARCH) == ResearchState.UNDERSTOOD,
				"A spirit's service, one of the Binding Arts' three kinds, not mastering them by itself");

		// Step 9, keeping: due a week after sealing with nothing paid ahead; warned, a day's grace, then suspended, nothing taken.
		ConcordanceWorkerGameTests.beside(helper, holder, PERCH_AT);
		inventory.setItem(OFFERING_SLOT, new ItemStack(Items.AMETHYST_SHARD, 1));
		long due = sealedAt + ConcordanceWorkerGameTests.PERIOD_7;
		helper.assertTrue(ConcordanceWorkerGameTests.stay(level, perch.holder(), sealedAt + 1_000L, due - 1_000L).isEmpty(), "Nothing falls due before its week");
		helper.assertValueEqual(perch.holder().look(level, due), Pacts.Event.WARNED, "Due with none paid ahead: a warning");
		helper.assertTrue(ConcordanceWorkerGameTests.pactOf(level, perch).pact().upkeep().graceUntil() == due + Pacts.DAY && perch.holder().held(level) == null
				&& ConcordanceWorkerGameTests.stolas(level, perch).think(level, PACT_NOON, due) == Status.UPKEEP_DUE,
				"A day of grace, the service still on; Stolas says an offering is due");
		helper.assertTrue(ConcordanceWorkerGameTests.stay(level, perch.holder(), due + 1_000L, due + Pacts.DAY - 1_000L).isEmpty(), "Graced: nothing more within the day");
		helper.assertValueEqual(perch.holder().look(level, due + Pacts.DAY), Pacts.Event.SUSPENDED, "Unpaid at its end: suspended for upkeep");
		helper.assertTrue(perch.holder().held(level) == Status.UPKEEP_HELD
				&& ConcordanceWorkerGameTests.stolas(level, perch).think(level, PACT_NOON, due + Pacts.DAY) == Status.UPKEEP_HELD
				&& StarReckoning.reckon(level, holder, perch.holder(), ConcordanceWorkerGameTests.FULL_MOON, clock, due + Pacts.DAY) == Pacts.Reckoning.UPKEEP_HELD
				&& AstrolabeItem.charge(astrolabe) == afterRecall, "The service stops: Stolas says so and reckons nothing");
		helper.assertTrue(ConcordanceWorkerGameTests.shards(holder) == 1 && ConcordanceWorkerGameTests.escrowed(level, record) == 0
				&& BoundWills.of(server).ledger().get(record) != null && BoundWills.of(server).problems().isEmpty(),
				"Nothing is taken: the shard they carry is still theirs, and the pact stays");
		long resumedAt = due + Pacts.DAY + 500L;
		helper.assertValueEqual(PactKeeping.deposit(level, holder, perch.holder(), Items.AMETHYST_SHARD, resumedAt), 1, "They offer the shard they carry");
		helper.assertTrue(perch.holder().held(level) == null
				&& ConcordanceWorkerGameTests.pactOf(level, perch).pact().upkeep().due() == resumedAt + ConcordanceWorkerGameTests.PERIOD_7
				&& ConcordanceWorkerGameTests.shards(holder) == 0 && ConcordanceWorkerGameTests.escrowed(level, record) == 0
				&& ConcordanceWorkerGameTests.stolas(level, perch).think(level, PACT_NOON, resumedAt) != Status.UPKEEP_HELD,
				"It pays the period held and resumes at once, the next a whole week on: one shard given, one taken, none ahead");
		// Away past the next due date and past a grace; back, the whole grace.
		TestPlayers.leave(holder);
		helper.assertTrue(ConcordanceWorkerGameTests.stay(level, perch.holder(), resumedAt + 1_000L, resumedAt + ConcordanceWorkerGameTests.PERIOD_7 + 3 * Pacts.DAY).isEmpty()
				&& perch.holder().held(level) == null && !ConcordanceWorkerGameTests.pactOf(level, perch).pact().upkeep().warned(),
				"Away past the next due date and a grace: nothing is looked at, warned or held");
		ServerPlayer back = TestPlayers.again(helper, id);
		RateGate.forget(id);
		ConcordanceWorkerGameTests.beside(helper, back, PERCH_AT);
		long returned = resumedAt + ConcordanceWorkerGameTests.PERIOD_7 + 3 * Pacts.DAY + 500L;
		helper.assertValueEqual(perch.holder().look(level, returned), Pacts.Event.WARNED, "Back, they are warned, never suspended at once");
		helper.assertTrue(ConcordanceWorkerGameTests.pactOf(level, perch).pact().upkeep().graceUntil() == returned + Pacts.DAY && perch.holder().held(level) == null
				&& ConcordanceWorkerGameTests.stolas(level, perch).think(level, PACT_NOON, returned) == Status.UPKEEP_DUE
				&& ConcordanceWorkerGameTests.regard(back) == ResearchState.MASTERED, "with the whole day of grace from their return; nothing of theirs lost");
		back.getInventory().setItem(OFFERING_SLOT, new ItemStack(Items.AMETHYST_SHARD, 3));
		helper.assertValueEqual(PactKeeping.deposit(level, back, perch.holder(), Items.AMETHYST_SHARD, returned + 500L), 3, "They pay three");
		helper.assertTrue(ConcordanceWorkerGameTests.escrowed(level, record) == 2 && ConcordanceWorkerGameTests.shards(back) == 0
				&& ConcordanceWorkerGameTests.pactOf(level, perch).pact().upkeep().due() == returned + ConcordanceWorkerGameTests.PERIOD_7
				&& !ConcordanceWorkerGameTests.pactOf(level, perch).pact().upkeep().warned() && BoundWills.of(server).problems().isEmpty(),
				"One for the period due, two kept ahead: four shards given in all, two taken by upkeep, two kept, none lost");

		// Ending, from the roster (D-07): the escrow back once, Stolas departs, the departure kept.
		RosterStatePayload listing = ConcordanceWorkerGameTests.roster(back);
		RosterStatePayload.Row row = ConcordanceWorkerGameTests.rowOf(listing, record);
		helper.assertTrue(row != null && row.offers(RosterRequestPayload.END) && ConcordanceWorkerGameTests.key(row.name()).equals("entity.jugcraft.stolas")
				&& ConcordanceWorkerGameTests.rowOf(listing, handRecord) != null, "Their roster lists the pact, end offered, and the workshop hand");
		StolasEntity presence = ConcordanceWorkerGameTests.stolas(level, perch);
		RosterStatePayload ended = ConcordanceWorkerGameTests.act(back, row, RosterRequestPayload.END);
		Component told = ConcordanceWorkerGameTests.result(ended);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(ended, "ended") && ConcordanceWorkerGameTests.key(told).equals("message.jugcraft.concordance.pact.departed")
				&& Integer.valueOf(2).equals(ConcordanceWorkerGameTests.arg(told, 2)), "Ended from the roster: told who departs, why, and that two came back");
		helper.assertTrue(ConcordanceWorkerGameTests.shards(back) == 2 && BoundWills.of(server).escrow(record).isEmpty() && BoundWills.of(server).ledger().get(record) == null
				&& perch.holder().agreement() == null && helper.getBlockState(PERCH_AT).is(Workers.PACT_PERCH),
				"What was paid ahead comes back once (four given: two taken, two returned); the record goes; the perch stays");
		helper.assertTrue(presence != null && presence.departing() && presence.gone() && !presence.isRemoved()
				&& !WorkerRoster.of(server).workers(id).containsKey(presence.getUUID()) && perch.stolas(level) == null
				&& presence.cues().equals(List.of("arrive", "refuse", "assent", "inspect", "depart")),
				"Stolas makes his departing gesture, struck off at once; his cues in order: " + (presence == null ? "none" : presence.cues()));
		List<Pacts.Departure> history = PactHistory.of(server).history(id);
		helper.assertTrue(history.size() == 1 && record.equals(history.getFirst().record()) && Pacts.ENDED.equals(history.getFirst().reason())
				&& Couriers.place(level, helper.absolutePos(PERCH_AT)).equals(history.getFirst().place()), "The departure is kept once, with its reason and place: " + history);
		helper.assertValueEqual(pactsSection(back), List.of("journal.jugcraft.pacts.regard.kept", "journal.jugcraft.pacts.sky_recorded",
				"journal.jugcraft.pacts.kept", "journal.jugcraft.pacts.departed"), "The journal: the Regard kept, the sky recorded, the hand kept, Stolas's departure");
		helper.assertTrue(ConcordanceWorkerGameTests.rowOf(ended, record) == null && ConcordanceWorkerGameTests.rowOf(ended, handRecord) != null
				&& Roster.act(back, Roster.RECORD, record, RosterRequestPayload.END, level.getGameTime()).reason().equals("nothing")
				&& ConcordanceWorkerGameTests.shards(back) == 2 && PactHistory.of(server).history(id).size() == 1, "Ending again returns nothing more");
		helper.assertTrue(!StyxState.get(level).claimed(id) && ConcordanceWorkerGameTests.count(back, "jugcraft:stolas_starflower") == 0
				&& ConcordanceWorkerGameTests.count(back, "jugcraft:ravenquill_lupine") == 0
				&& ConcordanceWorkerGameTests.count(back, "jugcraft:amethyst_mourningbell") == 0,
				"The whole way without Styx's gift: a feather examined and amethyst offered, never one of his flowers");
		helper.assertTrue(BoundWills.of(server).problems().isEmpty() && CourierLedger.of(server).problems().isEmpty(),
				"The Bound Wills and the courier ledger are sound: " + BoundWills.of(server).problems() + ", " + CourierLedger.of(server).problems());
		helper.runAfterDelay(StolasEntity.Gesture.DEPART.ticks + 2, () -> {
			helper.assertTrue(presence.isRemoved() && ConcordanceWorkerGameTests.stolases(helper).isEmpty(), "His gesture over, Stolas is gone from the world");
			helper.succeed();
		});
	}
}
