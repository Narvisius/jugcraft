package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Brew;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleStandBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Reagent;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.AlchemyCatalog;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Assay;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Axis;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Heat;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Mixture;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Operation;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Outcome;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Product;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.rime.Frostward;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.lair.MoltenSlagBlock;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import local.peepo.CompanionStatus;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 13, experimental alchemy: a crucible takes its heat from the block beneath; the same process in two
 * crucibles makes the same mixture and the same draught, whose effect follows from what was put in; only someone who
 * understands the Alembic Arts may work one; the mortar prepares ingredients; a bowl makes a salve for a creature; a
 * recorded formula is followed by another crucible fed by pipe and hopper, making the same draught; real machinery (a
 * hopper, a campfire and a Pneumatic Extractor) runs a formula twice; a broken crucible drops its items; and a crucible
 * saves its mixture and formula. Since Concordance Chunk B (B-08), Winter Brine: Rime brews Frostward only stirred cold,
 * bottles made before keep their Resistance, a Rime formula written before brews Frostward, and nothing makes Frostward
 * cheaper than its snow. Since Concordance Chunk C (C-01), the crucible says why it waits: a formula the rules in force
 * refuse, or a saved one that no longer reads, is kept, names its step and drives nothing until the rules can follow it
 * again; a full output pauses and voids nothing; the Peepo port reads the crucible's own reason; only someone who
 * could change the block by hand works it; and Jade is told the tank and that reason, encoded, never the makeup. Since
 * C-02, the workshop's products: contaminant fouls Verdance first, so a dirtier Stillroot Poultice heals less; the Kiln
 * Draught carries its drinker across the Cinder Kiln's slag; a poultice gives back its bowl; products feed no crucible
 * and never add up; dilution never heals more; and an old unnamed brew in the output pauses the crucible. Since C-05, the
 * Crucible Stand: JE heat for the next stir, idle for any other wait or a hold, set only by those who may change it, fed
 * through a cable and a piped tank; a campfire crucible as before.
 */
public class ConcordanceAlchemyGameTests {
	private static final String GLOW = "minecraft:glowstone_dust";
	private static final String SNOW = "minecraft:snowball";
	/** A stir may follow another only after this many ticks (CrucibleBlockEntity.STIR_TICKS, plus one). */
	private static final int NEXT_STIR = CrucibleBlockEntity.STIR_TICKS + 1;

	private static CrucibleBlockEntity crucible(GameTestHelper helper, BlockPos at, boolean campfire) {
		helper.setBlock(at.below(), campfire ? Blocks.CAMPFIRE : Blocks.STONE);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		return (CrucibleBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
	}

	private static ServerPlayer alembist(GameTestHelper helper, ResearchState state) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		if (state != ResearchState.NONE) {
			ConcordanceProgress.grant(player, Alchemy.RESEARCH, state);
		}
		return player;
	}

	private static InteractionResult use(CrucibleBlockEntity crucible, ServerPlayer player, ItemStack held) {
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return crucible.use(player, (ServerLevel) player.level(), held, InteractionHand.MAIN_HAND);
	}

	private static ItemStack waterBottle() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}

	private static CrucibleBlockEntity crucible(GameTestHelper helper, BlockPos at, Block beneath) {
		helper.setBlock(at.below(), beneath);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		return (CrucibleBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
	}

	/**
	 * Winter Brine's process (tools/concordance_alchemy.py WINTER_BRINE): three snowballs as they come in {@code water}
	 * parts, stirred three times at {@code band}.
	 */
	private static Mixture threeSnowballs(int water, Band band) {
		List<Operation> process = new ArrayList<>(List.of(new Operation.Water(water)));
		for (int i = 0; i < 3; i++) {
			process.add(new Operation.Add(SNOW, "jugcraft:raw"));
		}
		for (int i = 0; i < 3; i++) {
			process.add(new Operation.Stir(band));
		}
		return ((Mixture.Applied) new Formula(process).replay(Alchemy.catalog())).mixture();
	}

	/** What the first bottle drawn from {@code mixture} would do, as the crucible bottles it. */
	private static Outcome bottled(Mixture mixture) {
		Mixture.Bottled bottled = mixture.bottle();
		return Outcome.of(bottled.dose(), bottled.contaminant(), Alchemy.catalog(), mixture.history());
	}

	/** Three snowballs in a bottle of water, then a stir: by hand, as an alembist does it. */
	private static void startWinterBrine(CrucibleBlockEntity crucible, ServerPlayer player) {
		use(crucible, player, waterBottle());
		for (int i = 0; i < 3; i++) {
			use(crucible, player, new ItemStack(Items.SNOWBALL));
		}
		use(crucible, player, new ItemStack(Items.STICK));
	}

	/** Water, one glowstone dust as it comes, and the crucible held hot: then two stirs, a little apart. */
	private static void startNightEye(GameTestHelper helper, CrucibleBlockEntity crucible, ServerPlayer player) {
		crucible.setTemperature(120);
		helper.assertTrue(use(crucible, player, waterBottle()).consumesAction() && crucible.mixture().parts() == 1, "A bottle of water is one part");
		helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "The empty bottle is handed back");
		use(crucible, player, new ItemStack(Items.GLOWSTONE_DUST));
		helper.assertTrue(crucible.mixture().pending().get(Axis.RADIANCE) == 1500, "Glowstone goes in undissolved");
		use(crucible, player, new ItemStack(Items.STICK));
	}

	/** The worked example as a formula: Night Vision I for 1435 ticks (1.196 Radiance a part). */
	private static final String NIGHT_EYE = "water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot";
	/** The same in two parts with two glowstone dust: two doses, each the worked example's. */
	private static final String TWO_NIGHT_EYES = "water 2; add minecraft:glowstone_dust jugcraft:raw; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot";
	/** A spider eye stirred hot twice: Poison I (0.957 Hollow a part), 0.30 contaminant, under Nausea's 0.50. */
	private static final String SPIDER = "water 1; add minecraft:spider_eye jugcraft:raw; stir hot; stir hot";

	private static ItemStack written(String text) {
		ItemStack formula = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		formula.set(JugcraftConcordance.FORMULA, text);
		return formula;
	}

	private static void pour(CrucibleBlockEntity crucible, long droplets) {
		try (Transaction transaction = Transaction.openOuter()) {
			crucible.water.insert(FluidVariant.of(Fluids.WATER), droplets, transaction);
			transaction.commit();
		}
	}

	/** The draught a formula's first dose makes, worked out on paper. */
	private static ItemStack draught(String formula) {
		return Alchemy.brew(JugcraftConcordance.DRAUGHT, bottled(((Mixture.Applied) Formula.parse(formula).replay(Alchemy.catalog())).mixture()));
	}

	/** {@code times} pulses of its formula, at once; the last one's reason. */
	private static Diagnostic pulse(CrucibleBlockEntity crucible, ServerLevel level, int times) {
		Diagnostic last = null;
		for (int i = 0; i < times; i++) {
			last = crucible.automate(level);
		}
		return last;
	}

	/** A Fire Resistance draught: another brew than any formula here makes. */
	private static ItemStack otherBrew(int count) {
		ItemStack other = new ItemStack(JugcraftConcordance.DRAUGHT, count);
		other.set(JugcraftConcordance.BREW, new Brew(List.of(new Brew.Dose("minecraft:fire_resistance", 0, 1200, false)), "minecraft:fire_resistance@0"));
		return other;
	}

	/** The reason a status line gives (its second argument's key), or "" if none. */
	private static String reasonKey(Component line) {
		return line.getContents() instanceof TranslatableContents contents && contents.getArgs().length == 2
				&& contents.getArgs()[1] instanceof Component reason && reason.getContents() instanceof TranslatableContents why ? why.getKey() : "";
	}

	/** Step 2 of NIGHT_EYE refused under rules that know no glowstone (the empty rules stand for that reload). */
	private static Diagnostic glowstoneRefused() {
		return Diagnostic.incompatible(2, new Operation.Add(GLOW, "jugcraft:raw").describe(), Text.of("alchemy.not_ingredient", new Text.Ref("item", GLOW)));
	}

	/**
	 * {@code during} under {@code rules}, at once, the rules put back whatever happens (C-P28): ConcordanceData's rules are
	 * every test's, and the server runs other tests, so they are never left swapped past this call (check_diagnostics).
	 */
	private static <T> T under(ConcordanceRules rules, Supplier<T> during) {
		ConcordanceRules was = ConcordanceData.rules();
		ConcordanceData.replace(rules);
		try {
			return during.get();
		} finally {
			ConcordanceData.replace(was);
		}
	}

	/** A crucible over a campfire, held hot (120), following {@code formula} set by {@code player}. */
	private static CrucibleBlockEntity following(GameTestHelper helper, BlockPos at, String formula, ServerPlayer player) {
		CrucibleBlockEntity crucible = crucible(helper, at, true);
		crucible.setTemperature(120);
		helper.assertTrue(use(crucible, player, written(formula)).consumesAction() && crucible.program() != null, "the formula is set: " + formula);
		return crucible;
	}

	/** NIGHT_EYE followed with a bottle of water, its glowstone and a glass bottle at hand, its water already poured (step 1). */
	private static CrucibleBlockEntity pouredNightEye(GameTestHelper helper, BlockPos at) {
		CrucibleBlockEntity crucible = following(helper, at, NIGHT_EYE, alembist(helper, ResearchState.UNDERSTOOD));
		pour(crucible, FluidConstants.BOTTLE);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(crucible.automate(helper.getLevel()) == null && crucible.step() == 1, "The first pulse pours the water: step " + crucible.step());
		return crucible;
	}

	/** TWO_NIGHT_EYES followed to its end with all it needs: two parts made, nothing bottled yet (step 5). */
	private static CrucibleBlockEntity twoNightEyesMade(GameTestHelper helper, BlockPos at) {
		CrucibleBlockEntity crucible = following(helper, at, TWO_NIGHT_EYES, alembist(helper, ResearchState.UNDERSTOOD));
		pour(crucible, FluidConstants.BOTTLE * 2);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE, 2));
		helper.assertTrue(pulse(crucible, helper.getLevel(), 5) == null && crucible.step() == 5 && crucible.mixture().parts() == 2,
				"Five pulses make both parts: step " + crucible.step() + ", " + crucible.mixture());
		return crucible;
	}

	/** C-05: a mixed-band formula: one glowstone dust stirred warm, then hot. */
	private static final String WARM_THEN_HOT = "water 1; add minecraft:glowstone_dust jugcraft:raw; stir warm; stir hot";

	/** A crucible at {@code at} on a Crucible Stand beneath it; the stand. */
	private static CrucibleStandBlockEntity onStand(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at.below(), JugcraftConcordance.CRUCIBLE_STAND);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		return helper.getBlockEntity(at.below(), CrucibleStandBlockEntity.class);
	}

	private static boolean lit(GameTestHelper helper, BlockPos relative) {
		return helper.getBlockState(relative).getValue(BlockStateProperties.LIT);
	}

	/** A stand that would heat but holds {@code held} JE, short of {@code rate} a tick. */
	private static Diagnostic unpowered(int held, long rate) {
		return Diagnostic.of(Code.UNAVAILABLE_POWER, Text.of("alchemy.stand.it"), Text.of("alchemy.stand.je", held), Text.of("alchemy.stand.je_tick", (int) rate));
	}

	/** Offers {@code amount} JE to a stand's west face as a cable would; what it took. */
	private static long offer(ServerLevel level, BlockPos absolute, long amount) {
		EnergyStorage storage = EnergyStorage.SIDED.find(level, absolute, Direction.WEST);
		try (Transaction transaction = Transaction.openOuter()) {
			long taken = storage == null ? 0L : storage.insert(amount, transaction);
			transaction.commit();
			return taken;
		}
	}

	/** Each cable round {@code at} reaches it exactly where it takes JE: every face but its seat while it stands, none once gone. */
	private static void armsMeet(GameTestHelper helper, BlockPos at, boolean standing) {
		for (Direction side : Direction.values()) {
			boolean takes = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(at), side) != null;
			boolean arm = helper.getBlockState(at.relative(side)).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()));
			helper.assertTrue(takes == (standing && side != Direction.UP) && arm == takes,
					"The cable on its " + side + " face: arm " + arm + ", takes JE " + takes + " (stand " + (standing ? "placed" : "gone") + ")");
		}
	}

	// ---------------------------------------------------------------- heat

	/** A lit campfire heats a crucible one degree a tick to 120 (hot); with nothing beneath it cools to the air's 20. */
	@GameTest(maxTicks = 160)
	public void heatComesFromBeneath(GameTestHelper helper) {
		CrucibleBlockEntity heated = crucible(helper, new BlockPos(1, 2, 1), true);
		CrucibleBlockEntity cold = crucible(helper, new BlockPos(4, 2, 1), false);
		cold.setTemperature(100);
		helper.runAfterDelay(40, () -> helper.assertTrue(heated.temperature() >= 55 && heated.temperature() <= 65,
				"Forty ticks over a campfire: about 60 degrees, not jumping: " + heated.temperature()));
		helper.succeedWhen(() -> {
			helper.assertTrue(heated.temperature() == 120 && heated.band() == Band.HOT, "Over a campfire it settles hot: " + heated.temperature());
			helper.assertTrue(cold.temperature() == Heat.AMBIENT, "With nothing beneath it cools to 20: " + cold.temperature());
		});
	}

	// ---------------------------------------------------------------- the same process, the same result

	/**
	 * Two crucibles worked the same way hold exactly the same mixture; the draughts they make are the same; drunk, one
	 * gives Night Vision I for 71.75 seconds, as a Radiance of 1.20 a part (two hot stirs of one glowstone dust) says it
	 * should; and the outcome is recorded as practice.
	 */
	@GameTest(maxTicks = 100)
	public void theSameProcessMakesTheSameDraught(GameTestHelper helper) {
		CrucibleBlockEntity first = crucible(helper, new BlockPos(1, 2, 1), true);
		CrucibleBlockEntity second = crucible(helper, new BlockPos(4, 2, 1), true);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		startNightEye(helper, first, player);
		startNightEye(helper, second, player);
		helper.runAfterDelay(NEXT_STIR, () -> {
			use(first, player, new ItemStack(Items.STICK));
			use(second, player, new ItemStack(Items.STICK));
			helper.assertTrue(first.mixture().equals(second.mixture()), "Both crucibles hold the same mixture: " + first.mixture());
			helper.assertTrue(first.mixture().dissolved().get(Axis.RADIANCE) == 1196, "Two hot stirs dissolve 1.196: " + first.mixture());
			use(first, player, new ItemStack(Items.GLASS_BOTTLE));
			ItemStack one = take(player, JugcraftConcordance.DRAUGHT);
			use(second, player, new ItemStack(Items.GLASS_BOTTLE));
			ItemStack two = take(player, JugcraftConcordance.DRAUGHT);
			Brew brew = one.get(JugcraftConcordance.BREW);
			helper.assertTrue(brew != null && brew.equals(two.get(JugcraftConcordance.BREW)), "The two draughts are the same: " + brew);
			helper.assertTrue(brew.effects().size() == 1 && brew.effects().get(0).status().equals("minecraft:night_vision")
					&& brew.effects().get(0).amplifier() == 0 && brew.effects().get(0).ticks() == 1196 * 1200 / 1000,
					"Night Vision I for 1435 ticks: " + brew);
			helper.assertTrue(first.mixture().isEmpty(), "One part, one bottle: the crucible is empty");
			helper.assertTrue(ConcordanceProgress.knowledge(player).progress(Alchemy.RESEARCH).evidence()
					.containsKey("practice:jugcraft:alchemy:minecraft:night_vision@0"), "The outcome is recorded as practice");
			one.getItem().finishUsingItem(one, helper.getLevel(), player);
			helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "Drunk, it gives night vision");
			helper.succeed();
		});
	}

	private static ItemStack take(ServerPlayer player, net.minecraft.world.item.Item item) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				player.getInventory().setItem(i, ItemStack.EMPTY);
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	// ---------------------------------------------------------------- who may, and with what

	/**
	 * Without the Alembic Arts a player cannot put anything in (nothing changes) but can still taste it with a spoon;
	 * the mortar grinds an ingredient into a reagent that dissolves at once in part; a bowl bottles a salve, which used
	 * on a pig gives it the effect.
	 */
	@GameTest(maxTicks = 60)
	public void onlyAnAlembistWorksTheCrucible(GameTestHelper helper) {
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), true);
		crucible.setTemperature(120);
		ServerPlayer novice = alembist(helper, ResearchState.NONE);
		helper.assertTrue(use(crucible, novice, waterBottle()) == InteractionResult.FAIL && crucible.mixture().isEmpty(),
				"Without the Alembic Arts nothing goes in");
		helper.assertTrue(use(crucible, novice, new ItemStack(JugcraftConcordance.SAMPLING_SPOON)).consumesAction(), "Anyone may taste it");
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		use(crucible, player, waterBottle());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.MORTAR));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOWSTONE_DUST, 2));
		RateGate.forget(player.getUUID());
		JugcraftConcordance.MORTAR.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getOffhandItem().getCount() == 1, "The mortar grinds one");
		ItemStack reagent = take(player, JugcraftConcordance.REAGENT_ITEM);
		Reagent ground = reagent.get(JugcraftConcordance.REAGENT);
		helper.assertTrue(ground != null && ground.item().equals(GLOW) && ground.preparation().equals("jugcraft:ground"), "A ground glowstone reagent: " + ground);
		use(crucible, player, reagent);
		helper.assertTrue(crucible.mixture().dissolved().get(Axis.RADIANCE) == 810 && crucible.mixture().pending().get(Axis.RADIANCE) == 540,
				"Ground, 0.81 of its 1.35 dissolves at once: " + crucible.mixture());
		use(crucible, player, new ItemStack(Items.BOWL));
		ItemStack salve = take(player, JugcraftConcordance.SALVE);
		helper.assertTrue(salve.has(JugcraftConcordance.BREW), "A bowl bottles a salve");
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 4));
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, salve);
		salve.getItem().interactLivingEntity(salve, player, pig, InteractionHand.MAIN_HAND);
		helper.assertTrue(pig.hasEffect(MobEffects.NIGHT_VISION) && player.getInventory().countItem(Items.BOWL) == 1,
				"The salve takes on the pig, and the bowl comes back");
		helper.succeed();
	}

	// ---------------------------------------------------------------- recorded, and repeated

	/**
	 * A formula recorded from a fresh mixture is followed by another crucible: water by pipe, the glowstone from its
	 * buffer, two stirs once it is hot, and a draught into its output from a supplied bottle, the same draught as the
	 * one made by hand. Hoppers reach only the buffer and bottle slot from above or a side; the output comes out from a side
	 * or below, never from above.
	 */
	@GameTest(maxTicks = 400)
	public void aFormulaIsRepeatedByAnotherCrucible(GameTestHelper helper) {
		CrucibleBlockEntity byHand = crucible(helper, new BlockPos(1, 2, 1), true);
		CrucibleBlockEntity automated = crucible(helper, new BlockPos(4, 2, 4), true);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		startNightEye(helper, byHand, player);
		helper.runAfterDelay(NEXT_STIR, () -> {
			use(byHand, player, new ItemStack(Items.STICK));
			use(byHand, player, new ItemStack(JugcraftConcordance.FORMULA_ITEM));
			ItemStack written = take(player, JugcraftConcordance.FORMULA_ITEM);
			String text = written.get(JugcraftConcordance.FORMULA);
			helper.assertTrue("water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot".equals(text), "The formula records the process: " + text);
			use(byHand, player, new ItemStack(Items.GLASS_BOTTLE));
			Brew byHandBrew = take(player, JugcraftConcordance.DRAUGHT).get(JugcraftConcordance.BREW);
			helper.assertTrue(use(automated, player, written).consumesAction() && automated.program() != null, "The written formula is set");
			automated.setTemperature(120);
			try (Transaction transaction = Transaction.openOuter()) {
				automated.water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BOTTLE, transaction);
				transaction.commit();
			}
			helper.assertTrue(automated.canPlaceItemThroughFace(0, new ItemStack(Items.GLOWSTONE_DUST), Direction.UP)
					&& !automated.canPlaceItemThroughFace(0, new ItemStack(Items.DIRT), Direction.UP)
					&& !automated.canPlaceItemThroughFace(0, new ItemStack(Items.GLOWSTONE_DUST), Direction.DOWN)
					&& automated.canPlaceItemThroughFace(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE), Direction.NORTH)
					&& !automated.canTakeItemThroughFace(CrucibleBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY, Direction.UP)
					&& automated.canTakeItemThroughFace(CrucibleBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY, Direction.EAST)
					&& automated.canTakeItemThroughFace(CrucibleBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY, Direction.DOWN)
					&& !automated.canTakeItemThroughFace(0, ItemStack.EMPTY, Direction.EAST),
					"Hoppers reach the buffer and bottles from above and the sides; the output comes out of a side or below");
			automated.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
			automated.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
			helper.succeedWhen(() -> {
				ItemStack made = automated.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
				helper.assertTrue(made.is(JugcraftConcordance.DRAUGHT), "Waiting for the repeat: step " + automated.step() + ", " + automated.mixture());
				helper.assertTrue(byHandBrew.equals(made.get(JugcraftConcordance.BREW)), "The repeat makes the same draught: " + made.get(JugcraftConcordance.BREW));
				helper.assertTrue(automated.getItem(0).isEmpty() && automated.water.amount == 0 && automated.mixture().isEmpty(),
						"It used exactly the glowstone and the water");
			});
		});
	}

	/**
	 * A crucible run entirely by machinery: a campfire beneath, a hopper above dropping in glowstone and bottles, water
	 * in its tank through the Transfer API, and a Pneumatic Extractor at its side moving each draught into a chest. The
	 * formula runs twice over and makes two draughts, both the one the worked example predicts.
	 */
	@GameTest(maxTicks = 600)
	public void machineryRunsAFormulaTwice(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = crucible(helper, at, true);
		crucible.setTemperature(120);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		ItemStack written = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		written.set(JugcraftConcordance.FORMULA, "water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot");
		helper.assertTrue(use(crucible, player, written).consumesAction() && crucible.program() != null, "The formula is set");
		try (Transaction transaction = Transaction.openOuter()) {
			crucible.water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BOTTLE * 2, transaction);
			transaction.commit();
		}
		helper.setBlock(at.above(), Blocks.HOPPER);
		HopperBlockEntity hopper = helper.getBlockEntity(at.above(), HopperBlockEntity.class);
		hopper.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
		hopper.setItem(1, new ItemStack(Items.GLASS_BOTTLE, 2));
		BlockPos side = at.east();
		helper.setBlock(side, JugcraftLogistics.PNEUMATIC_EXTRACTOR.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, Direction.WEST));
		helper.setBlock(side.east(), Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(side.east(), ChestBlockEntity.class);
		helper.succeedWhen(() -> {
			int made = 0;
			Brew first = null;
			boolean same = true;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				ItemStack stack = chest.getItem(i);
				if (stack.is(JugcraftConcordance.DRAUGHT)) {
					made += stack.getCount();
					Brew brew = stack.get(JugcraftConcordance.BREW);
					same &= first == null || first.equals(brew);
					first = first == null ? brew : first;
				}
			}
			helper.assertTrue(made == 2, "Waiting for two draughts in the chest: " + made + " (step " + crucible.step() + ", "
					+ crucible.mixture() + ", output " + crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT) + ")");
			helper.assertTrue(same && first != null && first.effects().size() == 1
					&& first.effects().get(0).status().equals("minecraft:night_vision") && first.effects().get(0).ticks() == 1435,
					"Both are Night Vision I for 1435 ticks: " + first);
			helper.assertTrue(hopper.isEmpty() && crucible.water.amount == 0 && crucible.mixture().isEmpty()
					&& crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty(), "It used exactly the glowstone, bottles and water");
		});
	}

	/** Broken, a crucible drops what its buffer, bottle slot and output hold; its mixture spills. */
	@GameTest(maxTicks = 20)
	public void aBrokenCrucibleDropsItsItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = crucible(helper, at, false);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 3));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE, 2));
		helper.destroyBlock(at);
		int glowstone = 0;
		int bottles = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(3.0))) {
			glowstone += item.getItem().is(Items.GLOWSTONE_DUST) ? item.getItem().getCount() : 0;
			bottles += item.getItem().is(Items.GLASS_BOTTLE) ? item.getItem().getCount() : 0;
		}
		helper.assertTrue(glowstone == 3 && bottles == 2, "The crucible's items drop once: " + glowstone + " glowstone, " + bottles + " bottles");
		helper.succeed();
	}

	/** A crucible saves its mixture (to the milli-unit), its history and its formula. */
	@GameTest(maxTicks = 40)
	public void aCrucibleKeepsItsMixtureWhenSaved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), true);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		startNightEye(helper, crucible, player);
		use(crucible, player, new ItemStack(Items.SWEET_BERRIES));
		Mixture before = crucible.mixture();
		CompoundTag saved = crucible.saveWithoutMetadata(level.registryAccess());
		CrucibleBlockEntity copy = new CrucibleBlockEntity(crucible.getBlockPos(), crucible.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.mixture().equals(before), "The mixture is saved exactly: " + copy.mixture() + " vs " + before);
		helper.assertTrue(Formula.of(copy.mixture()) != null && Formula.of(copy.mixture()).text().equals(Formula.of(before).text()),
				"Its history still records as the same formula");
		helper.succeed();
	}

	// ---------------------------------------------------------------- what it shows (roadmap step 27)

	/**
	 * The vessel shows what is in it and no more: what a client is sent is the volume, the heat and the spoon's reading
	 * (the strongest property, murky or not), never the mixture's makeup. Each step shows as work, a searing stir (which
	 * damages the mixture) as danger, and a bottle filled as success; nothing shows a shortage while nothing is lacking.
	 */
	@GameTest(maxTicks = 100)
	public void theVesselShowsWhatASpoonFinds(GameTestHelper helper) {
		SignWatch signs = new SignWatch(helper);
		BlockPos at = new BlockPos(1, 2, 1);
		CrucibleBlockEntity crucible = crucible(helper, at, false);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		CompoundTag empty = crucible.getUpdateTag(helper.getLevel().registryAccess());
		helper.assertTrue(empty.getIntOr("parts", -1) == 0 && empty.getStringOr("taste", "?").isEmpty() && !empty.getBooleanOr("murky", true),
				"An empty crucible shows nothing in it: " + empty);
		startNightEye(helper, crucible, player);
		CompoundTag shown = crucible.getUpdateTag(helper.getLevel().registryAccess());
		helper.assertTrue(shown.keySet().equals(Set.of("temperature", "parts", "taste", "murky")),
				"A client is sent the heat, the volume and the spoon's reading, nothing of the makeup: " + shown.keySet());
		helper.assertTrue(shown.getIntOr("parts", 0) == 1 && shown.getStringOr("taste", "").equals(Axis.RADIANCE.id),
				"One part, tasting of Radiance, as a spoon would find: " + shown);
		helper.assertValueEqual(signs.at(Sign.WORK, at), 3L, "water, glowstone and a stir: three steps of work");
		helper.runAfterDelay(NEXT_STIR, () -> {
			crucible.setTemperature(200);
			use(crucible, player, new ItemStack(Items.STICK));
			helper.assertTrue(crucible.band() == Band.SEARING, "Searing: " + crucible.temperature());
			helper.assertValueEqual(signs.at(Sign.PERIL, at), 1L, "a searing stir shows danger");
			helper.assertValueEqual(signs.at(Sign.WORK, at), 3L, "and not work");
			use(crucible, player, new ItemStack(Items.GLASS_BOTTLE));
			helper.assertValueEqual(signs.at(Sign.DONE, at), 1L, "a bottle filled shows success");
			helper.assertTrue(signs.of(Sign.WANT).isEmpty(), "No shortage while nothing is lacking: " + signs.all());
			signs.close();
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- Winter Brine (Concordance Chunk B, B-08)

	/**
	 * Rime brews Frostward, and Rime is cold's own: each stir above the cold band keeps only part of the Rime already
	 * dissolved. On paper, three snowballs as they come in one part of water, stirred three times, keep what
	 * tools/concordance_alchemy.py winter_brine says at each band (check_mod_data holds the two to each other): cold, a
	 * draught of Frostward I for 48.75 s; warm, too weak, naming the stirs that thawed it; searing, only the fouling's
	 * Nausea. A warm stir before any snowball went in thaws nothing and is not named. By hand, one crucible over stone and
	 * one over a magma block: the cold one's draught drunk gives Frostward and no Resistance; the warm one's does nothing.
	 */
	@GameTest(maxTicks = 60)
	public void winterBrineNeedsTheColdBand(GameTestHelper helper) {
		AlchemyCatalog catalog = Alchemy.catalog();
		helper.assertTrue(catalog.property(Axis.RIME.id) != null && Frostward.STATUS.equals(catalog.property(Axis.RIME.id).status()),
				"Rime brews Frostward: " + catalog.property(Axis.RIME.id));
		long[] kept = {813, 685, 211, 0};
		for (int i = 0; i < kept.length; i++) {
			Band band = Band.values()[i];
			long rime = threeSnowballs(1, band).dissolved().get(Axis.RIME);
			helper.assertTrue(rime == kept[i], "Three " + band.id + " stirs keep " + kept[i] + " Rime, not " + rime);
		}
		Outcome cold = bottled(threeSnowballs(1, Band.COLD));
		helper.assertTrue(cold.key().equals("jugcraft:frostward@0") && cold.effects().size() == 1 && cold.effects().get(0).ticks() == 975,
				"Stirred cold: Frostward I for 975 ticks: " + cold);
		Outcome warm = bottled(threeSnowballs(1, Band.WARM));
		helper.assertTrue(warm.effects().isEmpty() && warm.reasons().stream().anyMatch(reason -> reason.key().equals("alchemy.too_weak")
				&& reason.args().stream().anyMatch(arg -> arg.toString().contains("thawed"))),
				"Stirred warm: too weak, and the warm stirs named as what thawed it: " + warm.reasons());
		List<Operation> stirredFirst = new ArrayList<>(List.of(new Operation.Water(1), new Operation.Stir(Band.WARM)));
		for (int i = 0; i < 3; i++) {
			stirredFirst.add(new Operation.Add(SNOW, "jugcraft:raw"));
		}
		for (int i = 0; i < 3; i++) {
			stirredFirst.add(new Operation.Stir(Band.COLD));
		}
		Outcome early = bottled(((Mixture.Applied) new Formula(stirredFirst).replay(catalog)).mixture());
		helper.assertTrue(early.key().equals("jugcraft:frostward@0") && early.reasons().stream()
						.noneMatch(reason -> reason.args().stream().anyMatch(arg -> arg.toString().contains("thawed"))),
				"A warm stir before any Rime went in thaws nothing and is not named: " + early.reasons());
		Outcome searing = bottled(threeSnowballs(1, Band.SEARING));
		helper.assertTrue(searing.key().equals("minecraft:nausea@0"), "Stirred searing: no Rime left, only the fouling: " + searing.key());

		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		CrucibleBlockEntity cool = crucible(helper, new BlockPos(1, 2, 1), Blocks.STONE);
		CrucibleBlockEntity warmed = crucible(helper, new BlockPos(4, 2, 1), Blocks.MAGMA_BLOCK);
		warmed.setTemperature(60);
		startWinterBrine(cool, player);
		startWinterBrine(warmed, player);
		helper.runAfterDelay(NEXT_STIR, () -> {
			use(cool, player, new ItemStack(Items.STICK));
			use(warmed, player, new ItemStack(Items.STICK));
			helper.runAfterDelay(NEXT_STIR, () -> {
				use(cool, player, new ItemStack(Items.STICK));
				use(warmed, player, new ItemStack(Items.STICK));
				helper.assertTrue(cool.band() == Band.COLD && warmed.band() == Band.WARM, "Over stone it stays cold, over magma warm: "
						+ cool.temperature() + ", " + warmed.temperature());
				helper.assertTrue(cool.mixture().dissolved().get(Axis.RIME) == 813 && warmed.mixture().dissolved().get(Axis.RIME) == 685,
						"By hand, the same Rime as on paper: " + cool.mixture() + " / " + warmed.mixture());
				use(cool, player, new ItemStack(Items.GLASS_BOTTLE));
				ItemStack brine = take(player, JugcraftConcordance.DRAUGHT);
				use(warmed, player, new ItemStack(Items.GLASS_BOTTLE));
				Brew thawed = take(player, JugcraftConcordance.DRAUGHT).get(JugcraftConcordance.BREW);
				helper.assertTrue(thawed != null && thawed.effects().isEmpty(), "Stirred warm, the draught does nothing: " + thawed);
				brine.getItem().finishUsingItem(brine, helper.getLevel(), player);
				MobEffectInstance ward = player.getEffect(Frostward.FROSTWARD);
				helper.assertTrue(ward != null && ward.getDuration() == 975 && ward.getAmplifier() == 0 && !player.hasEffect(MobEffects.RESISTANCE),
						"Drunk, Winter Brine gives Frostward I for 975 ticks and no Resistance: " + ward);
				helper.succeed();
			});
		});
	}

	/**
	 * A save test: a Rime draught bottled before Winter Brine (Resistance I for 487 ticks: 0.813 a part at 600 ticks a
	 * unit) reads, writes and reads again as it was, and drunk still gives Resistance, never Frostward: a bottle keeps
	 * what it was bottled with.
	 */
	@GameTest(maxTicks = 20)
	public void oldRimeBottlesStillGiveResistance(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		DynamicOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
		JsonElement saved = JsonParser.parseString("{\"id\":\"jugcraft:draught\",\"count\":1,\"components\":{\"jugcraft:brew\":"
				+ "{\"effects\":[{\"status\":\"minecraft:resistance\",\"amplifier\":0,\"ticks\":487}],\"outcome\":\"minecraft:resistance@0\"}}}");
		ItemStack old = ItemStack.CODEC.parse(ops, saved).getOrThrow();
		Brew brew = old.get(JugcraftConcordance.BREW);
		helper.assertTrue(old.is(JugcraftConcordance.DRAUGHT) && brew != null && brew.effects().size() == 1
				&& brew.effects().get(0).status().equals("minecraft:resistance"), "The old draught reads as it was: " + brew);
		ItemStack again = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, old).getOrThrow()).getOrThrow();
		helper.assertTrue(brew.equals(again.get(JugcraftConcordance.BREW)), "Written and read again it is the same: " + again.get(JugcraftConcordance.BREW));
		ServerPlayer player = alembist(helper, ResearchState.NONE);
		old.getItem().finishUsingItem(old, level, player);
		MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
		helper.assertTrue(resistance != null && resistance.getDuration() == 487 && resistance.getAmplifier() == 0,
				"Drunk, it still gives Resistance I for 487 ticks: " + resistance);
		helper.assertTrue(!Frostward.warded(player), "and no Frostward");
		helper.succeed();
	}

	/**
	 * A Rime formula written before Winter Brine (three snowballs, three cold stirs) still reads, is set on a crucible,
	 * and run from its tank and buffer now bottles Frostward I for 975 ticks, using exactly its snow and water.
	 */
	@GameTest(maxTicks = 300)
	public void aRimeFormulaWrittenBeforeBrewsFrostward(GameTestHelper helper) {
		String text = "water 1; add minecraft:snowball jugcraft:raw; add minecraft:snowball jugcraft:raw; "
				+ "add minecraft:snowball jugcraft:raw; stir cold; stir cold; stir cold";
		helper.assertTrue(Formula.parse(text) != null, "The formula still reads");
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), Blocks.STONE);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		ItemStack written = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		written.set(JugcraftConcordance.FORMULA, text);
		helper.assertTrue(use(crucible, player, written).consumesAction() && crucible.program() != null, "The formula is set");
		try (Transaction transaction = Transaction.openOuter()) {
			crucible.water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BOTTLE, transaction);
			transaction.commit();
		}
		crucible.setItem(0, new ItemStack(Items.SNOWBALL, 3));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		Brew expected = new Brew(List.of(new Brew.Dose(Frostward.STATUS, 0, 975, false)), "jugcraft:frostward@0");
		helper.succeedWhen(() -> {
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(expected.equals(made.get(JugcraftConcordance.BREW)), "Waiting for Frostward: step " + crucible.step() + ", "
					+ crucible.mixture() + ", output " + made.get(JugcraftConcordance.BREW));
			helper.assertTrue(crucible.getItem(0).isEmpty() && crucible.water.amount == 0, "It used exactly its three snowballs and its water");
		});
	}

	/**
	 * Economy (06:146-158): Frostward is never cheaper than its snow. Yeti Fur, the Frost Horn, wool and brews are no
	 * ingredients: the crucible will not take fur by hand or by hopper, and the mortar will not grind it. Two parts of
	 * water make more bottles but no more Frostward (each too weak); and two draughts drunk one after the other last as
	 * long as one.
	 */
	@GameTest(maxTicks = 20)
	public void frostwardIsNeverCheaperThanItsSnow(GameTestHelper helper) {
		AlchemyCatalog catalog = Alchemy.catalog();
		for (String thing : List.of("jugcraft:yeti_fur", "jugcraft:frost_horn", "minecraft:white_wool", "jugcraft:draught", "jugcraft:salve")) {
			helper.assertTrue(catalog.ingredient(thing) == null, thing + " is no alchemical ingredient");
		}
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), Blocks.STONE);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		use(crucible, player, waterBottle());
		Mixture before = crucible.mixture();
		ItemStack fur = new ItemStack(JugcraftYeti.YETI_FUR, 2);
		helper.assertTrue(use(crucible, player, fur) == InteractionResult.TRY_WITH_EMPTY_HAND && fur.getCount() == 2
				&& crucible.mixture().equals(before), "The crucible takes no Yeti Fur: " + crucible.mixture());
		helper.assertTrue(!crucible.canPlaceItem(0, fur), "nor from a hopper");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.MORTAR));
		player.setItemInHand(InteractionHand.OFF_HAND, fur);
		RateGate.forget(player.getUUID());
		helper.assertTrue(JugcraftConcordance.MORTAR.use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
				&& player.getOffhandItem().getCount() == 2 && take(player, JugcraftConcordance.REAGENT_ITEM).isEmpty(),
				"The mortar grinds no Yeti Fur");
		Mixture diluted = threeSnowballs(2, Band.COLD);
		Mixture.Bottled first = diluted.bottle();
		Outcome weak = Outcome.of(first.dose(), first.contaminant(), catalog, diluted.history());
		helper.assertTrue(first.dose().get(Axis.RIME) == 406 && weak.effects().isEmpty(),
				"Three snowballs in two parts: 0.406 a part, too weak for Frostward: " + first.dose());
		Outcome cold = bottled(threeSnowballs(1, Band.COLD));
		ItemStack one = Alchemy.brew(JugcraftConcordance.DRAUGHT, cold);
		ItemStack two = Alchemy.brew(JugcraftConcordance.DRAUGHT, cold);
		ServerPlayer drinker = alembist(helper, ResearchState.NONE);
		one.getItem().finishUsingItem(one, helper.getLevel(), drinker);
		two.getItem().finishUsingItem(two, helper.getLevel(), drinker);
		MobEffectInstance ward = drinker.getEffect(Frostward.FROSTWARD);
		helper.assertTrue(ward != null && ward.getDuration() == 975, "Two draughts last as long as one, not their sum: " + ward);
		helper.succeed();
	}

	/** Roadmap step 28: only an alembist may stop a crucible's formula, as only one may set it. */
	@GameTest(maxTicks = 20)
	public void onlyAnAlembistStopsAFormula(GameTestHelper helper) {
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), true);
		ServerPlayer alembist = alembist(helper, ResearchState.UNDERSTOOD);
		ItemStack written = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		written.set(JugcraftConcordance.FORMULA, "water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot");
		helper.assertTrue(use(crucible, alembist, written).consumesAction() && crucible.program() != null, "The formula is set");
		ServerPlayer stranger = alembist(helper, ResearchState.NONE);
		crucible.useEmpty(stranger, true);
		helper.assertTrue(crucible.program() != null, "Someone who knows no alchemy cannot stop it");
		crucible.useEmpty(alembist, true);
		helper.assertTrue(crucible.program() == null, "An alembist can");
		helper.succeed();
	}

	// ---------------------------------------------------------------- why it waits (Concordance Chunk C, C-01)

	/**
	 * C-01 (C-P1): rules that can no longer make a formula's next step (a reload that drops glowstone; the empty rules
	 * stand for it) keep the formula, its step and all it holds. Each pulse names the step and why, the danger shows
	 * once, the Peepo port asks for nothing and calls the recipe unavailable, and the status line gives the reason.
	 */
	@GameTest(maxTicks = 20)
	public void aChangedRuleKeepsTheFormulaAndSaysWhy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		SignWatch signs = new SignWatch(helper);
		CrucibleBlockEntity crucible = pouredNightEye(helper, at);
		Mixture before = crucible.mixture();
		record Seen(Diagnostic first, Diagnostic second, Diagnostic shown, int dust, int bottles, CompanionStatus status, Component line) {
		}
		Seen seen = under(ConcordanceRules.EMPTY, () -> new Seen(crucible.automate(level), crucible.automate(level), crucible.waiting(),
				crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)), crucible.companion.needed(new ItemStack(Items.GLASS_BOTTLE)),
				crucible.companion.status(), crucible.statusLine(ConcordanceProgress.now(level))));
		signs.close();
		helper.assertTrue(glowstoneRefused().equals(seen.first()) && seen.first().equals(seen.second()) && seen.first().equals(seen.shown()),
				"Each pulse names step 2 and why: " + seen.first() + " / " + seen.second() + " / " + seen.shown());
		helper.assertTrue(crucible.program() != null && NIGHT_EYE.equals(crucible.program().text()) && crucible.step() == 1,
				"The formula is kept at its step: " + crucible.program() + ", step " + crucible.step());
		helper.assertTrue(crucible.mixture().equals(before) && crucible.getItem(0).getCount() == 1
				&& crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount() == 1, "Nothing is taken or bottled: " + crucible.mixture());
		helper.assertTrue(seen.dust() == 0 && seen.bottles() == 0 && seen.status() == CompanionStatus.RECIPE_MISSING,
				"The Peepo port asks for nothing: " + seen.dust() + " dust, " + seen.bottles() + " bottles, " + seen.status());
		helper.assertValueEqual(signs.at(Sign.PERIL, at), 1L, "the danger shown once, when it began");
		helper.assertTrue(reasonKey(seen.line()).equals("compose.jugcraft.diagnostic.incompatible"), "The status line says why: " + seen.line());
		helper.succeed();
	}

	/**
	 * C-01 (C-P1): rules that can make the step again are noticed before any pulse (the reason clears at once), and the
	 * formula carries on from its step to the same draught it would have made.
	 */
	@GameTest(maxTicks = 20)
	public void aRestoredRuleResumesTheFormula(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		SignWatch signs = new SignWatch(helper);
		CrucibleBlockEntity crucible = pouredNightEye(helper, at);
		Diagnostic during = under(ConcordanceRules.EMPTY, () -> crucible.automate(level));
		helper.assertTrue(glowstoneRefused().equals(during), "Under the changed rules it names step 2: " + during);
		helper.assertTrue(crucible.waiting() == null && crucible.incompatibility() == null,
				"The rules restored, it no longer waits, before any pulse: " + crucible.waiting());
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 2 && crucible.getItem(0).isEmpty(),
				"It carries on from its step: step " + crucible.step());
		helper.assertTrue(pulse(crucible, level, 2) == null && crucible.step() == 4, "Two stirs: step " + crucible.step());
		helper.assertTrue(crucible.automate(level) == null
				&& ItemStack.isSameItemSameComponents(crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT), draught(NIGHT_EYE))
				&& crucible.mixture().isEmpty() && crucible.step() == 0, "The same draught as ever: " + crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT));
		helper.assertValueEqual(signs.at(Sign.PERIL, at), 1L, "the danger shown once");
		signs.close();
		helper.succeed();
	}

	/**
	 * C-01 (plan 03:54): a written formula the rules in force cannot replay is not set, and the formula item's use says
	 * which step and why (the message it sends, CrucibleBlockEntity.refusal); one that does not read is not set either.
	 * Setting one names what is never hidden: the harmful statuses a dose carries and the first searing stir, judged by
	 * the server as well as the property's own word; and, when the mixture in the crucible is not the formula's own
	 * batch (there before it was set, or under a formula the rules refuse), that mixture's harm and searing too.
	 */
	@GameTest(maxTicks = 20)
	public void aWrittenFormulaThatNoLongerReplaysSaysWhichStep(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = crucible(helper, at, true);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		record Tried(InteractionResult result, Component told) {
		}
		Tried tried = under(ConcordanceRules.EMPTY, () -> new Tried(use(crucible, player, written(NIGHT_EYE)),
				CrucibleBlockEntity.refusal(Formula.parse(NIGHT_EYE), Alchemy.catalog())));
		helper.assertTrue(tried.result() == InteractionResult.FAIL && crucible.program() == null && tried.told() != null
				&& tried.told().getContents() instanceof TranslatableContents told && told.getKey().equals("message.jugcraft.concordance.alchemy.formula_refused")
				&& told.getArgs().length == 1 && ComposeText.show(glowstoneRefused().text()).equals(told.getArgs()[0]),
				"Rules without glowstone refuse it, and the use says step 2 and why: " + tried);
		helper.assertTrue(CrucibleBlockEntity.refusal(Formula.parse(NIGHT_EYE), Alchemy.catalog()) == null, "The rules in force replay it");
		AlchemyCatalog catalog = Alchemy.catalog();
		Diagnostic overflow = Formula.parse("water 4; water 3").incompatibility(catalog);
		helper.assertTrue(Diagnostic.incompatible(2, new Operation.Water(3).describe(), Text.of("alchemy.overflowing", Mixture.MAX_PARTS)).equals(overflow),
				"Seven parts overflow at step 2: " + overflow);
		helper.assertTrue(use(crucible, player, written("water 4; water 3")) == InteractionResult.FAIL && crucible.program() == null,
				"and it is not set");
		helper.assertTrue(Formula.parse("water 1; stir boiling") == null && use(crucible, player, written("water 1; stir boiling")) == InteractionResult.FAIL,
				"A formula that does not read is not set");
		List<Text> poison = List.of(Text.of("alchemy.risk.harm", List.of(new Text.Ref("status", "minecraft:poison"))));
		List<Text> spider = Assay.risks(Formula.parse(SPIDER), Mixture.EMPTY, false, Alchemy::harmfulStatus, catalog);
		helper.assertTrue(poison.equals(spider), "A spider eye's dose brings Poison, and its fouling is under Nausea's threshold: " + spider);
		List<Text> seared = Assay.risks(Formula.parse("water 2; stir searing"), Mixture.EMPTY, false, Alchemy::harmfulStatus, catalog);
		helper.assertTrue(List.of(Text.of("alchemy.risk.searing", 2)).equals(seared), "Step 2 stirs it searing: " + seared);
		helper.assertTrue(Assay.risks(Formula.parse(NIGHT_EYE), Mixture.EMPTY, false, Alchemy::harmfulStatus, catalog).isEmpty(), "The worked example hides nothing");
		// The server judges harm too (C-P29): a status it holds harmful is named whatever the property says.
		helper.assertTrue(Alchemy.harmfulStatus("minecraft:poison") && !Alchemy.harmfulStatus("minecraft:night_vision") && Alchemy.harmfulStatus("jugcraft:no_such_status"),
				"Poison and an unknown status harm; Night Vision does not");
		helper.assertTrue(List.of(Text.of("alchemy.risk.harm", List.of(new Text.Ref("status", "minecraft:night_vision"))))
				.equals(Assay.risks(Formula.parse(NIGHT_EYE), Mixture.EMPTY, false, status -> true, catalog)), "A status the server holds harmful is named");
		// A mixture that is not the formula's own batch: its harm is named too, as a dose of it can be bottled by hand.
		Mixture spiderBatch = ((Mixture.Applied) Formula.parse(SPIDER + "; stir searing").replay(catalog)).mixture();
		List<Text> over = Assay.risks(Formula.parse(NIGHT_EYE), spiderBatch, true, Alchemy::harmfulStatus, catalog);
		helper.assertTrue(over.size() == 2 && over.get(0).key().equals("alchemy.risk.harm") && over.get(0).args().toString().contains("minecraft:poison")
				&& over.get(1).equals(Text.of("alchemy.risk.seared")), "A clean formula over a searing spider-eye batch names its Poison and its searing: " + over);
		helper.assertTrue(Assay.risks(Formula.parse(NIGHT_EYE), spiderBatch, false, Alchemy::harmfulStatus, catalog).isEmpty(),
				"its own batch: the formula's risk alone");
		List<Text> refused = Assay.risks(Formula.parse("water 4; water 3"), replay(Formula.parse(SPIDER).operations()), true, Alchemy::harmfulStatus, catalog);
		helper.assertTrue(poison.equals(refused), "A finished Poison batch under a formula the rules refuse: " + refused);
		helper.assertTrue(use(crucible, player, written(SPIDER)).consumesAction() && SPIDER.equals(crucible.program().text()),
				"A formula with a risk is still set, by a plain use");
		crucible.useEmpty(player, true);
		use(crucible, player, waterBottle());
		use(crucible, player, new ItemStack(Items.SPIDER_EYE));
		helper.assertTrue(use(crucible, player, written(NIGHT_EYE)).consumesAction() && NIGHT_EYE.equals(crucible.program().text())
				&& crucible.step() == 0 && !crucible.mixture().isEmpty(), "A clean formula is still set over a spider eye in water");
		Diagnostic emptying = crucible.automate(helper.getLevel());
		helper.assertTrue(Diagnostic.of(Code.NEEDS_EMPTYING).equals(emptying) && crucible.companion.status() == CompanionStatus.OCCUPIED,
				"It waits for that mixture to be bottled, and the Peepo port says OCCUPIED: " + emptying + ", " + crucible.companion.status());
		helper.succeed();
	}

	/**
	 * C-01 (C-P1): a saved formula this build cannot read is kept as written and saved back, never followed: it says so,
	 * shows danger once, asks the Peepo port for nothing and draws no water; sneaking with an empty hand clears it.
	 */
	@GameTest(maxTicks = 20)
	public void aSavedFormulaThatNoLongerReadsIsKeptAndReported(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		SignWatch signs = new SignWatch(helper);
		CrucibleBlockEntity crucible = crucible(helper, at, false);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		helper.assertTrue(use(crucible, player, written(NIGHT_EYE)).consumesAction(), "The formula is set");
		pour(crucible, FluidConstants.BOTTLE);
		CompoundTag saved = crucible.saveWithoutMetadata(level.registryAccess());
		String unknown = "water 1; stir boiling";
		saved.putString("program", unknown);
		saved.putInt("step", 1);
		crucible.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(crucible.program() == null && unknown.equals(crucible.unreadableProgram()) && crucible.step() == 1,
				"Read back, the formula is kept as written at its step: '" + crucible.unreadableProgram() + "', step " + crucible.step());
		Diagnostic unreadable = Diagnostic.of(Code.UNREADABLE);
		helper.assertTrue(unreadable.equals(crucible.waiting()) && unreadable.equals(crucible.incompatibility()),
				"It says it cannot be read: " + crucible.waiting());
		helper.assertTrue(crucible.companion.needed(new ItemStack(Items.GLASS_BOTTLE)) == 0 && crucible.companion.status() == CompanionStatus.RECIPE_MISSING,
				"The Peepo port asks for nothing: " + crucible.companion.status());
		helper.assertTrue(unreadable.equals(crucible.automate(level)) && crucible.water.amount == FluidConstants.BOTTLE && crucible.mixture().isEmpty(),
				"A pulse does nothing: " + crucible.water.amount + " droplets, " + crucible.mixture());
		helper.assertValueEqual(signs.at(Sign.PERIL, at), 1L, "the danger shown once");
		Component line = crucible.statusLine(ConcordanceProgress.now(level));
		helper.assertTrue(reasonKey(line).equals("compose.jugcraft.diagnostic.unreadable"), "The status line says why: " + line);
		CompoundTag again = crucible.saveWithoutMetadata(level.registryAccess());
		helper.assertTrue(unknown.equals(again.getStringOr("program", "")) && again.getIntOr("step", -1) == 1 && again.getIntOr(Saved.VERSION, 0) == 1,
				"Saved again, it is written back unchanged: " + again);
		crucible.useEmpty(player, true);
		helper.assertTrue(crucible.unreadableProgram().isEmpty() && crucible.waiting() == null
				&& !crucible.saveWithoutMetadata(level.registryAccess()).keySet().contains("program"), "Sneaking with an empty hand clears it");
		signs.close();
		helper.succeed();
	}

	/**
	 * C-01 (C-P1, C-P23): while the rules cannot follow its formula, the crucible asks the Peepo port for nothing (seen at
	 * once, before any pulse), draws nothing, and does not bottle even a finished batch; the rules restored, it asks again
	 * and bottles. C-05's stand and C-06's filing extend this test.
	 */
	@GameTest(maxTicks = 20)
	public void anIncompatibleFormulaAsksForNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = following(helper, at, NIGHT_EYE, alembist(helper, ResearchState.UNDERSTOOD));
		ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST);
		ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
		helper.assertTrue(crucible.companion.needed(dust) == 1 && crucible.companion.needed(bottle) == 1 && crucible.incompatibility() == null,
				"Followable, it asks for its glowstone and a bottle");
		pour(crucible, FluidConstants.BOTTLE);
		record Asked(int dust, int bottles, CompanionStatus status, Diagnostic hook, Diagnostic first, Diagnostic second, Diagnostic third, Band heat) {
		}
		Asked asked = under(ConcordanceRules.EMPTY, () -> new Asked(crucible.companion.needed(dust), crucible.companion.needed(bottle),
				crucible.companion.status(), crucible.incompatibility(), crucible.automate(level), crucible.automate(level), crucible.automate(level),
				Heat.standBand(crucible.program(), crucible.step(), crucible.waiting(), false, Band.SEARING)));
		helper.assertTrue(asked.dust() == 0 && asked.bottles() == 0 && asked.status() == CompanionStatus.RECIPE_MISSING,
				"It asks for nothing at once: " + asked);
		helper.assertTrue(asked.heat() == null, "and a stand beneath would give it no heat: " + asked);
		helper.assertTrue(asked.hook() != null && asked.hook().code() == Code.INCOMPATIBLE && asked.hook().equals(asked.first())
				&& asked.hook().equals(asked.second()) && asked.hook().equals(asked.third()), "Every pulse says why: " + asked);
		helper.assertTrue(crucible.water.amount == FluidConstants.BOTTLE && crucible.mixture().isEmpty() && crucible.step() == 0,
				"Nothing is drawn: " + crucible.water.amount + " droplets, " + crucible.mixture());
		helper.assertTrue(crucible.companion.needed(dust) == 1 && crucible.incompatibility() == null, "The rules restored, it asks again");
		helper.assertTrue(Heat.standBand(crucible.program(), crucible.step(), crucible.waiting(), false, Band.SEARING) == Band.HOT,
				"Restored, a stand would heat for its hot stir");
		// C-P23: a batch made to its end is not bottled while the rules refuse its formula.
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(pulse(crucible, level, 4) == null && crucible.step() == 4 && crucible.mixture().parts() == 1,
				"Made to its end, one part to bottle: step " + crucible.step());
		Mixture done = crucible.mixture();
		record Finished(Diagnostic pulse, Diagnostic hook, boolean outputEmpty, int bottles, Mixture mixture) {
		}
		Finished finished = under(ConcordanceRules.EMPTY, () -> new Finished(crucible.automate(level), crucible.incompatibility(),
				crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).isEmpty(), crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount(), crucible.mixture()));
		helper.assertTrue(glowstoneRefused().equals(finished.pulse()) && finished.pulse().equals(finished.hook()) && finished.outputEmpty()
				&& finished.bottles() == 1 && done.equals(finished.mixture()), "Even a finished batch waits: " + finished);
		helper.assertTrue(crucible.automate(level) == null && ItemStack.isSameItemSameComponents(crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT), draught(NIGHT_EYE))
				&& crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).getCount() == 1 && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty()
				&& crucible.mixture().isEmpty(), "The rules restored, one pulse bottles it: " + crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT));
		// C-06: its courier request goes the same way: a formula the rules cannot follow turns it back and files no other;
		// restored, the next ingredient wait asks again; broken, the request goes with it.
		BlockPos postAt = new BlockPos(4, 2, 2);
		helper.setBlock(postAt, Couriers.COURIER_POST);
		helper.getBlockEntity(postAt, CourierPostBlockEntity.class).setOwner(crucible.formulaOwner());
		CourierLedger ledger = CourierLedger.of(level.getServer());
		pour(crucible, FluidConstants.BOTTLE);
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1, "The next repeat pours its water");
		Diagnostic waits = crucible.automate(level);
		long firstAsk = crucible.request();
		helper.assertTrue(waits != null && waits.code() == Code.MISSING_INGREDIENT && firstAsk > 0L && ledger.ledger().get(firstAsk) != null,
				"Waiting for its glowstone, it asks a courier: " + waits + ", request " + firstAsk);
		long filed = ledger.ledger().nextId();
		record Turned(Diagnostic first, Diagnostic second, long request, boolean open, long next) {
		}
		Turned turned = under(ConcordanceRules.EMPTY, () -> new Turned(crucible.automate(level), crucible.automate(level), crucible.request(),
				ledger.ledger().get(firstAsk) != null, ledger.ledger().nextId()));
		helper.assertTrue(turned.first().code() == Code.INCOMPATIBLE && turned.request() == -1L && !turned.open() && turned.next() == filed,
				"A formula the rules refuse turns its request back and asks for nothing more: " + turned);
		Diagnostic again = crucible.automate(level);
		long reasked = crucible.request();
		helper.assertTrue(again != null && again.code() == Code.MISSING_INGREDIENT && reasked > 0L && reasked != firstAsk,
				"The rules restored, it asks again: " + again + ", request " + reasked);
		helper.setBlock(at, Blocks.AIR);
		helper.assertTrue(ledger.ledger().get(reasked) == null, "Broken, its request goes with it");
		helper.succeed();
	}

	/**
	 * C-01 (plan 03:64): a finished batch waits for a bottle, then for room: a full output and an output holding another
	 * brew pause it, nothing voided or taken, each shown once; emptied, it bottles into it.
	 */
	@GameTest(maxTicks = 20)
	public void aFullOutputPausesAndVoidsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		SignWatch signs = new SignWatch(helper);
		CrucibleBlockEntity crucible = following(helper, at, NIGHT_EYE, alembist(helper, ResearchState.UNDERSTOOD));
		pour(crucible, FluidConstants.BOTTLE);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		helper.assertTrue(pulse(crucible, level, 4) == null && crucible.step() == 4, "Made to its end: step " + crucible.step());
		Mixture made = crucible.mixture();
		Diagnostic noBottle = crucible.automate(level);
		helper.assertTrue(Diagnostic.of(Code.NEEDS_CONTAINER).equals(noBottle) && crucible.companion.status() == CompanionStatus.NEEDS_BOTTLES,
				"It waits for a bottle: " + noBottle + ", " + crucible.companion.status());
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, draught(NIGHT_EYE).copyWithCount(16));
		Diagnostic full = crucible.automate(level);
		Diagnostic still = crucible.automate(level);
		Diagnostic noRoom = Diagnostic.of(Code.INSUFFICIENT_CAPACITY);
		helper.assertTrue(noRoom.equals(full) && noRoom.equals(still) && noRoom.equals(crucible.waiting()), "A full output: " + full + " / " + still);
		helper.assertTrue(crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).getCount() == 16 && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount() == 1
				&& crucible.mixture().equals(made) && crucible.step() == 4, "Nothing voided or taken: " + crucible.mixture());
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, otherBrew(3));
		Diagnostic differs = crucible.automate(level);
		ItemStack other = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(Diagnostic.of(Code.OUTPUT_DIFFERS).equals(differs), "Another brew in its output: " + differs);
		helper.assertTrue(other.getCount() == 3 && otherBrew(1).get(JugcraftConcordance.BREW).equals(other.get(JugcraftConcordance.BREW))
				&& crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount() == 1 && crucible.mixture().equals(made), "The other brew is kept as it was: " + other);
		helper.assertValueEqual(signs.at(Sign.WANT, at), 3L, "one want for no bottle, one for no room, one for another brew");
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		helper.assertTrue(crucible.automate(level) == null, "Emptied, it bottles");
		ItemStack out = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(out.getCount() == 1 && ItemStack.isSameItemSameComponents(out, draught(NIGHT_EYE)) && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty()
				&& crucible.mixture().isEmpty() && crucible.step() == 0 && crucible.waiting() == null, "One draught, and nothing waits: " + out);
		helper.assertValueEqual(signs.at(Sign.DONE, at), 1L, "a bottle filled");
		signs.close();
		helper.succeed();
	}

	/**
	 * C-01: two hoppers, one above and one at a side, race to fill the buffer and bottle slot. The formula runs twice on
	 * its two parts of water and takes exactly what it needs; the glowstone it does not need stays, and it then says it
	 * needs water.
	 */
	@GameTest(maxTicks = 600)
	public void twoHoppersRaceForTheBuffer(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = following(helper, at, NIGHT_EYE, alembist(helper, ResearchState.UNDERSTOOD));
		pour(crucible, FluidConstants.BOTTLE * 2);
		helper.setBlock(at.above(), Blocks.HOPPER);
		helper.setBlock(at.north(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
		List<HopperBlockEntity> hoppers = List.of(helper.getBlockEntity(at.above(), HopperBlockEntity.class),
				helper.getBlockEntity(at.north(), HopperBlockEntity.class));
		for (HopperBlockEntity hopper : hoppers) {
			hopper.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
			hopper.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
		}
		ItemStack expected = draught(NIGHT_EYE);
		helper.succeedWhen(() -> {
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 2 && ItemStack.isSameItemSameComponents(made, expected),
					"Waiting for two draughts: " + made + " (step " + crucible.step() + ", " + crucible.waiting() + ")");
			helper.assertTrue(hoppers.stream().allMatch(HopperBlockEntity::isEmpty), "Both hoppers are emptied");
			int dust = 0;
			for (int i = 0; i < CrucibleBlockEntity.BUFFER_SLOTS; i++) {
				dust += crucible.getItem(i).is(Items.GLOWSTONE_DUST) ? crucible.getItem(i).getCount() : 0;
			}
			helper.assertTrue(dust == 2, "Four in, two used: " + dust + " left in the buffer");
			helper.assertTrue(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty(), "Both bottles are used");
			helper.assertTrue(crucible.water.amount == 0 && crucible.mixture().isEmpty(), "Both parts of water are used");
			helper.assertTrue(Diagnostic.needsWater(1, 0).equals(crucible.waiting()), "It says it needs water: " + crucible.waiting());
		});
	}

	/** C-01: a tank short of a step's water waits (shown once) and draws nothing; topped up, it draws exactly the step's parts. */
	@GameTest(maxTicks = 20)
	public void partialWaterWaitsThenDrawsExactly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		SignWatch signs = new SignWatch(helper);
		CrucibleBlockEntity crucible = following(helper, at, TWO_NIGHT_EYES, alembist(helper, ResearchState.UNDERSTOOD));
		pour(crucible, FluidConstants.BOTTLE + FluidConstants.BOTTLE / 2);
		Diagnostic lacking = Diagnostic.needsWater(2, 1);
		Diagnostic first = crucible.automate(level);
		Diagnostic second = crucible.automate(level);
		helper.assertTrue(lacking.equals(first) && lacking.equals(second) && crucible.companion.status() == CompanionStatus.NEEDS_WATER,
				"Two parts needed, one whole part held: " + first + " / " + second + ", " + crucible.companion.status());
		helper.assertTrue(crucible.water.amount == FluidConstants.BOTTLE * 3 / 2 && crucible.mixture().isEmpty() && crucible.step() == 0,
				"Nothing is drawn: " + crucible.water.amount);
		helper.assertValueEqual(signs.at(Sign.WANT, at), 1L, "the want shown once");
		pour(crucible, FluidConstants.BOTTLE);
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1 && crucible.mixture().parts() == 2, "Topped up, it pours two parts");
		helper.assertTrue(crucible.water.amount == FluidConstants.BOTTLE / 2 && crucible.water.variant.isOf(Fluids.WATER),
				"Exactly two parts drawn: " + crucible.water.amount + " left");
		signs.close();
		helper.succeed();
	}

	/**
	 * C-01: saved and read back between its two bottles, a two-part batch bottles its second dose once (the same as the
	 * first) and then waits for water for the next repeat.
	 */
	@GameTest(maxTicks = 20)
	public void aRestartJustBeforeOutputBottlesOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CrucibleBlockEntity crucible = twoNightEyesMade(helper, new BlockPos(2, 2, 2));
		helper.assertTrue(crucible.automate(level) == null && crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).getCount() == 1
				&& crucible.mixture().drawn() == 1, "One bottled: " + crucible.mixture());
		CompoundTag saved = crucible.saveWithoutMetadata(level.registryAccess());
		helper.assertTrue(saved.getIntOr("step", -1) == 5 && saved.getIntOr("drawn", -1) == 1 && saved.getIntOr("parts", -1) == 1,
				"Saved at its end with one part left: " + saved);
		crucible.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(crucible.waiting() == null && crucible.step() == 5 && crucible.mixture().drawn() == 1
				&& crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).getCount() == 1, "Read back as it was: step " + crucible.step());
		helper.assertTrue(crucible.automate(level) == null, "It bottles the second dose");
		Diagnostic next = crucible.automate(level);
		helper.assertTrue(Diagnostic.needsWater(2, 0).equals(next), "Then waits for water to repeat: " + next);
		ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(made.getCount() == 2 && ItemStack.isSameItemSameComponents(made, draught(TWO_NIGHT_EYES)), "Two draughts, the same: " + made);
		helper.assertTrue(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty() && crucible.getItem(0).isEmpty() && crucible.mixture().isEmpty(),
				"Exactly its bottles and glowstone were used");
		helper.succeed();
	}

	/**
	 * C-01: the Peepo port says FULL only when the crucible itself is stopped by its output (full, or another brew), not
	 * merely because the output holds its own draughts.
	 */
	@GameTest(maxTicks = 20)
	public void theCompanionAndTheCrucibleAgreeOnFull(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CrucibleBlockEntity crucible = twoNightEyesMade(helper, new BlockPos(2, 2, 2));
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, draught(TWO_NIGHT_EYES).copyWithCount(1));
		helper.assertTrue(crucible.automate(level) == null && crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).getCount() == 2
				&& crucible.companion.status() != CompanionStatus.FULL && crucible.waiting() == null,
				"Its own draughts in the output are no reason to say FULL: " + crucible.companion.status());
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, draught(TWO_NIGHT_EYES).copyWithCount(16));
		helper.assertTrue(Diagnostic.of(Code.INSUFFICIENT_CAPACITY).equals(crucible.automate(level)) && crucible.companion.status() == CompanionStatus.FULL,
				"A full output is FULL: " + crucible.companion.status());
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, otherBrew(1));
		helper.assertTrue(Diagnostic.of(Code.OUTPUT_DIFFERS).equals(crucible.automate(level)) && crucible.companion.status() == CompanionStatus.FULL,
				"Another brew in the output is FULL: " + crucible.companion.status());
		helper.assertTrue(crucible.mixture().parts() == 1 && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount() == 1, "Nothing more was bottled");
		helper.succeed();
	}

	/**
	 * C-01: a stir waits for its band, and Preserve's hold is named as the reason only while it is what keeps the heat
	 * from that band. Over a campfire (heading hot) held warm, a hot stir waits on the hold; over a magma block (warm at
	 * most), held or not, it waits for hot heat, which will not come from there. Either way the Peepo port says
	 * WRONG_TEMPERATURE.
	 */
	@GameTest(maxTicks = 20)
	public void aHoldIsTheReasonOnlyWhileItKeepsTheHeat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long now = ConcordanceProgress.now(level);
		CrucibleBlockEntity campfire = pouredNightEye(helper, new BlockPos(1, 2, 2));
		helper.assertTrue(campfire.automate(level) == null && campfire.step() == 2, "The glowstone goes in: step " + campfire.step());
		campfire.setTemperature(60);
		campfire.hold(now, 200);
		Diagnostic held = campfire.automate(level);
		helper.assertTrue(Diagnostic.held(Band.WARM.id, 10).equals(held) && campfire.step() == 2 && campfire.companion.status() == CompanionStatus.WRONG_TEMPERATURE,
				"Over a campfire held warm, the hot stir waits on the hold: " + held + ", " + campfire.companion.status());
		CrucibleBlockEntity magma = crucible(helper, new BlockPos(4, 2, 2), Blocks.MAGMA_BLOCK);
		magma.setTemperature(60);
		helper.assertTrue(use(magma, alembist(helper, ResearchState.UNDERSTOOD), written(NIGHT_EYE)).consumesAction(), "The formula is set over magma");
		pour(magma, FluidConstants.BOTTLE);
		magma.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		helper.assertTrue(pulse(magma, level, 2) == null && magma.step() == 2, "Water and glowstone: step " + magma.step());
		Diagnostic unheld = magma.automate(level);
		magma.hold(now, 200);
		Diagnostic heldAnyway = magma.automate(level);
		Diagnostic hot = Diagnostic.wrongHeat(Band.HOT.id, Band.WARM.id);
		helper.assertTrue(hot.equals(unheld) && hot.equals(heldAnyway) && magma.step() == 2 && magma.companion.status() == CompanionStatus.WRONG_TEMPERATURE,
				"Over magma the hot stir waits for hot heat, held or not: " + unheld + " / " + heldAnyway + ", " + magma.companion.status());
		helper.succeed();
	}

	/**
	 * C-01 (C-P5, C-P24): only someone who could change the block by hand works a crucible. In someone else's claim a
	 * player who knows more alchemy than its owner can pour, add, stir, bottle, set or clear nothing, but may still read
	 * it with a spoon or glass and record its process on a blank formula. Outside any claim, adventure mode refuses the
	 * same way (a protected town cannot be built in a game test).
	 */
	@GameTest(maxTicks = 20)
	public void onlyThoseWhoMayChangeACrucibleWorkIt(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = crucible(helper, at, true);
		crucible.setTemperature(120);
		ServerPlayer owner = alembist(helper, ResearchState.UNDERSTOOD);
		ServerPlayer stranger = alembist(helper, ResearchState.MASTERED);
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(at), owner.getUUID());
			helper.assertTrue(use(crucible, owner, waterBottle()).consumesAction() && use(crucible, owner, new ItemStack(Items.GLOWSTONE_DUST)).consumesAction(),
					"The claim's owner pours water and adds glowstone");
			Mixture before = crucible.mixture();
			// The stranger's stick comes before any stir, so only the claim can refuse it (not the settling time).
			helper.assertTrue(use(crucible, stranger, waterBottle()) == InteractionResult.FAIL && stranger.getMainHandItem().is(Items.POTION),
					"A stranger pours no water");
			ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST, 2);
			helper.assertTrue(use(crucible, stranger, dust) == InteractionResult.FAIL && dust.getCount() == 2, "adds nothing");
			helper.assertTrue(use(crucible, stranger, new ItemStack(Items.STICK)) == InteractionResult.FAIL, "stirs nothing");
			ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
			helper.assertTrue(use(crucible, stranger, glass) == InteractionResult.FAIL && glass.getCount() == 1
					&& take(stranger, JugcraftConcordance.DRAUGHT).isEmpty(), "bottles nothing");
			helper.assertTrue(use(crucible, stranger, written(NIGHT_EYE)) == InteractionResult.FAIL && crucible.program() == null, "and sets no formula");
			helper.assertTrue(crucible.mixture().equals(before), "Nothing in the crucible changed: " + crucible.mixture());
			helper.assertTrue(use(crucible, owner, new ItemStack(Items.STICK)).consumesAction() && crucible.mixture().history().size() == 3, "Its owner stirs");
			Mixture stirred = crucible.mixture();
			helper.assertTrue(use(crucible, stranger, new ItemStack(JugcraftConcordance.SAMPLING_SPOON)).consumesAction()
					&& use(crucible, stranger, new ItemStack(JugcraftConcordance.ASSAY_GLASS)).consumesAction(), "Anyone may read it");
			helper.assertTrue(use(crucible, stranger, new ItemStack(JugcraftConcordance.FORMULA_ITEM)).consumesAction()
					&& take(stranger, JugcraftConcordance.FORMULA_ITEM).get(JugcraftConcordance.FORMULA) != null && crucible.mixture().equals(stirred),
					"and record its process on a blank formula, changing nothing");
			helper.assertTrue(use(crucible, owner, written(NIGHT_EYE)).consumesAction() && crucible.program() != null, "Its owner sets a formula");
			crucible.useEmpty(stranger, true);
			helper.assertTrue(crucible.program() != null, "A stranger cannot clear it");
			crucible.useEmpty(owner, true);
			helper.assertTrue(crucible.program() == null, "Its owner can");
		}
		Mixture kept = crucible.mixture();
		stranger.setGameMode(GameType.ADVENTURE);
		helper.assertTrue(use(crucible, stranger, waterBottle()) == InteractionResult.FAIL && stranger.getMainHandItem().is(Items.POTION)
				&& crucible.mixture().equals(kept), "In adventure mode, outside any claim, nothing goes in");
		stranger.setGameMode(GameType.SURVIVAL);
		helper.assertTrue(use(crucible, stranger, waterBottle()).consumesAction() && crucible.mixture().parts() == 2,
				"Back in survival, outside the claim, the same player may pour");
		helper.succeed();
	}

	/**
	 * C-01 (C-P4): Jade is told the tank's whole parts and the crucible's own reason, encoded (its code and parameters, a
	 * line inside it closed by "."), which reads back the same; anything malformed reads as nothing. Never the makeup:
	 * the snapshot holds only the heat, hold, volume, tank, step and reason, and the update every client gets is still the
	 * heat, the volume and the spoon's reading. (The snapshot is checked only where Jade is loaded.)
	 */
	@GameTest(maxTicks = 20)
	public void jadeIsToldTheTankAndWhyNeverTheMakeup(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Diagnostic refused = glowstoneRefused();
		helper.assertValueEqual(refused.encode(), "incompatible|#2|!alchemy.op.add|@item=minecraft:glowstone_dust|@preparation=jugcraft:raw|."
				+ "|!alchemy.not_ingredient|@item=minecraft:glowstone_dust|.", "step 2 refused, encoded");
		helper.assertTrue(refused.equals(Diagnostic.decode(refused.encode())), "and read back the same: " + Diagnostic.decode(refused.encode()));
		for (String bad : List.of("", "no_such_code|#1", "needs_water|x", "needs_water|!alchemy.op.water|#1", "needs_water|.")) {
			helper.assertTrue(Diagnostic.decode(bad) == null, "Malformed, it reads as nothing: '" + bad + "'");
		}
		CrucibleBlockEntity crucible = following(helper, new BlockPos(2, 2, 2), NIGHT_EYE, alembist(helper, ResearchState.UNDERSTOOD));
		pour(crucible, FluidConstants.BOTTLE * 2);
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1 && crucible.tankParts() == 1,
				"The first pulse pours one part of the tank's two: step " + crucible.step() + ", tank " + crucible.tankParts());
		Diagnostic waiting = crucible.automate(level);
		Diagnostic missing = Diagnostic.missingIngredient(1, new Operation.Add(GLOW, "jugcraft:raw").describe());
		helper.assertTrue(missing.equals(waiting) && missing.equals(crucible.waiting()) && crucible.companion.status() == CompanionStatus.NO_INPUT,
				"It waits for its glowstone: " + waiting + ", " + crucible.companion.status());
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			CompoundTag shown = ConcordanceDataProvider.snapshot(crucible, level.getGameTime());
			helper.assertTrue(Set.of("temperature", "band", "parts", "tank", "step", "steps", "next", "reason").containsAll(shown.keySet()),
					"Jade is told no more than the heat, volume, tank, step and reason: " + shown.keySet());
			helper.assertTrue(shown.getIntOr("tank", -1) == 1, "Jade is told the tank's one whole part: " + shown);
			helper.assertTrue(waiting.encode().equals(shown.getStringOr("reason", "")), "and the crucible's own reason: " + shown);
		}
		Set<String> sent = crucible.getUpdateTag(level.registryAccess()).keySet();
		helper.assertTrue(Set.of("temperature", "parts", "taste", "murky").equals(sent), "Every client is told only the heat, volume and look: " + sent);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the workshop's products (Concordance Chunk C, C-02)

	private static final String MENDVETCH = "jugcraft:mendvetch";
	private static final String RAW = "jugcraft:raw";

	/** A process replayed under the rules in force, from an empty crucible. */
	private static Mixture replay(List<Operation> process) {
		return ((Mixture.Applied) new Formula(process).replay(Alchemy.catalog())).mixture();
	}

	/**
	 * The Stillroot Poultice's process (tools/concordance_alchemy.py stillroot_poultice): two Mendvetch as they come in
	 * {@code water} parts, {@code boneMeal} bone meal stirred in with them (a dirty batch), then four warm stirs.
	 */
	private static List<Operation> poultice(int water, int boneMeal) {
		List<Operation> process = new ArrayList<>(List.of(new Operation.Water(water)));
		for (int i = 0; i < 2; i++) {
			process.add(new Operation.Add(MENDVETCH, RAW));
		}
		for (int i = 0; i < boneMeal; i++) {
			process.add(new Operation.Add("minecraft:bone_meal", RAW));
		}
		for (int i = 0; i < 4; i++) {
			process.add(new Operation.Stir(Band.WARM));
		}
		return process;
	}

	/**
	 * The Kiln Draught's process (tools/concordance_alchemy.py KILN_DRAUGHT and KILN_DRAUGHT_SUGAR): one magma cream, or
	 * two sugar, as they come in {@code water} parts, stirred hot twice.
	 */
	private static List<Operation> kilnDraught(int water, boolean sugar) {
		List<Operation> process = new ArrayList<>(List.of(new Operation.Water(water)));
		for (int i = 0; i < (sugar ? 2 : 1); i++) {
			process.add(new Operation.Add(sugar ? "minecraft:sugar" : "minecraft:magma_cream", RAW));
		}
		process.add(new Operation.Stir(Band.HOT));
		process.add(new Operation.Stir(Band.HOT));
		return process;
	}

	/** A salve of a process's first dose, bottled as the crucible bottles it. */
	private static ItemStack salve(List<Operation> process) {
		return Alchemy.brew(JugcraftConcordance.SALVE, bottled(replay(process)));
	}

	/** The translation key of the name a stack carries: vanilla's item name, its item's own or a product's. */
	private static String nameKey(ItemStack stack) {
		Component name = stack.get(DataComponents.ITEM_NAME);
		return name != null && name.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}

	/** Stirs each crucible with a stick {@code times} times, a little apart, then runs {@code then}. */
	private static void stirs(GameTestHelper helper, ServerPlayer player, List<CrucibleBlockEntity> crucibles, int times, Runnable then) {
		for (CrucibleBlockEntity crucible : crucibles) {
			helper.assertTrue(use(crucible, player, new ItemStack(Items.STICK)).consumesAction(), "A stir at " + crucible.getBlockPos());
		}
		if (times > 1) {
			helper.runAfterDelay(NEXT_STIR, () -> stirs(helper, player, crucibles, times - 1, then));
		} else {
			then.run();
		}
	}

	/** A mock player who knows no alchemy, standing in this test's own space at {@code at}, not at the world's spawn. */
	private static ServerPlayer standing(GameTestHelper helper, BlockPos at) {
		ServerPlayer player = alembist(helper, ResearchState.NONE);
		BlockPos feet = helper.absolutePos(at);
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
		return player;
	}

	/** The health {@code player} gains over {@code ticks} of its own ticking (a mock player has no client to tick it). */
	private static float healed(ServerPlayer player, int ticks) {
		float before = player.getHealth();
		for (int i = 0; i < ticks; i++) {
			player.baseTick();
		}
		return player.getHealth() - before;
	}

	/** Bowls lying in this test's own space. */
	private static int droppedBowls(GameTestHelper helper) {
		int bowls = 0;
		for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
			bowls += item.getItem().is(Items.BOWL) ? item.getItem().getCount() : 0;
		}
		return bowls;
	}

	/**
	 * C-02 (C-D1, C-P2, C-P7): on paper, two Mendvetch stirred warm four times hold 2.127 Verdance a part, a Stillroot
	 * Poultice of Regeneration II; each bone meal stirred in with them brings contaminant that fouls Verdance first
	 * (4.00 for each unit), so a dirtier batch holds less (the generator's poultice_verdance, held to this array by
	 * check_alchemy), with a reason naming the bone meal; the glass says what the contaminant takes. By hand and by a
	 * formula over a magma block the same batches come out, named Stillroot Poultice by what they carry, and the
	 * automated one is the same salve as the hand-made one. Over 100 ticks the clean poultice heals 4 and the dirty one 2
	 * (vanilla's 50 >> amplifier interval).
	 */
	@GameTest(maxTicks = 400)
	public void aStillrootPoulticeRepeatsAndADirtyBatchHealsLess(GameTestHelper helper) {
		AlchemyCatalog catalog = Alchemy.catalog();
		long[] verdance = {2127, 1935, 1743, 1551};
		String[] keys = {"minecraft:regeneration@1", "minecraft:regeneration@0", "minecraft:poison@0+minecraft:regeneration@0",
				"minecraft:nausea@0+minecraft:poison@0+minecraft:regeneration@0"};
		for (int n = 0; n < verdance.length; n++) {
			Mixture mixture = replay(poultice(1, n));
			Outcome outcome = bottled(mixture);
			Outcome.Effect regeneration = outcome.effects().stream().filter(effect -> effect.property().id().equals(Axis.VERDANCE.id)).findFirst().orElse(null);
			helper.assertTrue(regeneration != null && regeneration.concentration() == verdance[n] && regeneration.ticks() == 450 && outcome.key().equals(keys[n]),
					n + " bone meal: " + verdance[n] + " Verdance a part after fouling, " + keys[n] + ": " + outcome.effects() + ", " + outcome.key());
			boolean fouled = outcome.reasons().stream().anyMatch(reason -> reason.key().equals("alchemy.fouled")
					&& reason.args().toString().contains("minecraft:bone_meal"));
			helper.assertTrue(fouled == (n > 0), n + " bone meal: a reason names the bone meal as what fouled it exactly when there is some: " + outcome.reasons());
			helper.assertTrue(mixture.outcome(catalog).equals(outcome), "The preview is the bottled outcome: " + mixture.outcome(catalog));
		}
		List<Text> glass = Assay.read(replay(poultice(1, 1)), 60, Assay.GLASS, catalog);
		helper.assertTrue(glass.contains(Text.of("alchemy.assay.fouls", new Text.Ref("principle", Axis.VERDANCE.id), "0.80")),
				"The glass says the contaminant takes 0.80 Verdance a part: " + glass);

		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		CrucibleBlockEntity clean = crucible(helper, new BlockPos(1, 2, 1), Blocks.MAGMA_BLOCK);
		CrucibleBlockEntity dirty = crucible(helper, new BlockPos(4, 2, 1), Blocks.MAGMA_BLOCK);
		CrucibleBlockEntity automated = crucible(helper, new BlockPos(1, 2, 4), Blocks.MAGMA_BLOCK);
		for (CrucibleBlockEntity crucible : List.of(clean, dirty, automated)) {
			crucible.setTemperature(60);
		}
		helper.assertTrue(use(automated, player, written(new Formula(poultice(1, 0)).text())).consumesAction() && automated.program() != null,
				"The poultice's formula is set");
		pour(automated, FluidConstants.BOTTLE);
		automated.setItem(0, new ItemStack(Garden.MENDVETCH, 2));
		automated.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.BOWL));
		for (CrucibleBlockEntity crucible : List.of(clean, dirty)) {
			use(crucible, player, waterBottle());
			use(crucible, player, new ItemStack(Garden.MENDVETCH));
			use(crucible, player, new ItemStack(Garden.MENDVETCH));
		}
		use(dirty, player, new ItemStack(Items.BONE_MEAL));
		stirs(helper, player, List.of(clean, dirty), 4, () -> {
			helper.assertTrue(clean.mixture().equals(replay(poultice(1, 0))) && dirty.mixture().equals(replay(poultice(1, 1))),
					"By hand, the same batches as on paper: " + clean.mixture() + " / " + dirty.mixture());
			helper.assertTrue(Assay.read(dirty.mixture(), dirty.temperature(), Assay.MASTERED, catalog).stream().anyMatch(line -> line.key().equals("alchemy.fouled")),
					"A master's glass names what fouled the dirty batch");
			use(clean, player, new ItemStack(Items.BOWL));
			ItemStack strong = take(player, JugcraftConcordance.SALVE);
			use(dirty, player, new ItemStack(Items.BOWL));
			ItemStack weak = take(player, JugcraftConcordance.SALVE);
			helper.assertValueEqual(strong.get(JugcraftConcordance.BREW), new Brew(List.of(new Brew.Dose("minecraft:regeneration", 1, 450, false)),
					"minecraft:regeneration@1"), "the clean poultice: Regeneration II for 450 ticks");
			helper.assertValueEqual(weak.get(JugcraftConcordance.BREW), new Brew(List.of(new Brew.Dose("minecraft:regeneration", 0, 450, false)),
					"minecraft:regeneration@0"), "the dirty one: Regeneration I for 450 ticks");
			for (ItemStack made : List.of(strong, weak)) {
				Product product = Alchemy.product(made);
				helper.assertTrue(product != null && product.id().equals(Alchemy.STILLROOT_POULTICE)
						&& nameKey(made).equals("item.jugcraft.salve.stillroot_poultice"), "Each is named a Stillroot Poultice: " + product + ", " + nameKey(made));
			}
			ServerPlayer a = standing(helper, new BlockPos(6, 2, 6));
			ServerPlayer b = standing(helper, new BlockPos(6, 2, 4));
			a.setHealth(2.0F);
			b.setHealth(2.0F);
			Alchemy.apply(helper.getLevel(), strong.get(JugcraftConcordance.BREW), a, null);
			Alchemy.apply(helper.getLevel(), weak.get(JugcraftConcordance.BREW), b, null);
			float gainedA = healed(a, 100);
			float gainedB = healed(b, 100);
			helper.assertTrue(gainedA == 4.0F && gainedB == 2.0F,
					"Over 100 ticks the clean poultice heals 4 and the dirty one 2 (one a 25 and a 50 ticks): " + gainedA + ", " + gainedB);
			helper.succeedWhen(() -> {
				ItemStack output = automated.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
				helper.assertTrue(output.getCount() == 1 && ItemStack.isSameItemSameComponents(output, strong),
						"Waiting for the formula's poultice, the same as the hand-made one: " + output + " (step " + automated.step() + ", " + automated.waiting() + ")");
				helper.assertTrue(automated.getItem(0).isEmpty() && automated.water.amount == 0 && automated.mixture().isEmpty()
						&& automated.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty(), "It used exactly its Mendvetch, its water and its bowl");
			});
		});
	}

	/**
	 * C-02 (C-D1, C-P9): one magma cream stirred hot twice holds 1.276 Ember a part (and 0.20 contaminant, under Nausea's
	 * 0.50), a Kiln Draught of Fire Resistance for 1531 ticks; two sugar hold 1.435, 1722 ticks. Either is named the Kiln
	 * Draught in a bottle, never in a bowl. Drunk, it hands back its bottle, and under it the Cinder Kiln's slag spares its
	 * drinker; a pig carrying the same brew takes no burn while a bare one does.
	 */
	@GameTest(maxTicks = 20)
	public void aKilnDraughtCarriesItsDrinkerAcrossTheSlag(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mixture cream = replay(kilnDraught(1, false));
		Mixture sugar = replay(kilnDraught(1, true));
		helper.assertTrue(cream.dissolved().get(Axis.EMBER) == 1276 && cream.contaminant() == 200 && sugar.dissolved().get(Axis.EMBER) == 1435,
				"Magma cream holds 1.276 Ember with 0.200 contaminant, two sugar 1.435: " + cream + " / " + sugar);
		Outcome fromCream = bottled(cream);
		Outcome fromSugar = bottled(sugar);
		helper.assertTrue(fromCream.key().equals("minecraft:fire_resistance@0") && fromCream.effects().size() == 1 && fromCream.effects().get(0).ticks() == 1531,
				"Fire Resistance I for 1531 ticks: " + fromCream.effects());
		helper.assertTrue(fromSugar.key().equals("minecraft:fire_resistance@0") && fromSugar.effects().size() == 1 && fromSugar.effects().get(0).ticks() == 1722,
				"From sugar, 1722 ticks: " + fromSugar.effects());
		ItemStack draught = Alchemy.brew(JugcraftConcordance.DRAUGHT, fromCream);
		for (ItemStack made : List.of(draught, Alchemy.brew(JugcraftConcordance.DRAUGHT, fromSugar))) {
			Product product = Alchemy.product(made);
			helper.assertTrue(product != null && product.id().equals("jugcraft:kiln_draught") && nameKey(made).equals("item.jugcraft.draught.kiln_draught"),
					"Bottled, it is named the Kiln Draught: " + product + ", " + nameKey(made));
		}
		helper.assertTrue(Alchemy.product(Alchemy.brew(JugcraftConcordance.SALVE, fromCream)) == null, "In a bowl it is no Kiln Draught: the form counts");
		Brew brew = draught.get(JugcraftConcordance.BREW);
		ServerPlayer drinker = alembist(helper, ResearchState.NONE);
		ServerPlayer bare = alembist(helper, ResearchState.NONE);
		ItemStack left = draught.finishUsingItem(level, drinker);
		helper.assertTrue(left.is(Items.GLASS_BOTTLE) && left.getCount() == 1, "Drunk, it hands back its bottle: " + left);
		MobEffectInstance resistance = drinker.getEffect(MobEffects.FIRE_RESISTANCE);
		helper.assertTrue(resistance != null && resistance.getDuration() == 1531 && resistance.getAmplifier() == 0,
				"Fire Resistance I for 1531 ticks on its drinker: " + resistance);
		helper.assertTrue(!MoltenSlagBlock.harms(drinker) && MoltenSlagBlock.harms(bare), "The slag spares its drinker and not a bare player");
		Mob covered = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 2, 1));
		Mob exposed = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 1));
		Alchemy.apply(level, brew, covered, null);
		float coveredHealth = covered.getHealth();
		float exposedHealth = exposed.getHealth();
		helper.assertTrue(!MoltenSlagBlock.burn(level, covered) && covered.getHealth() == coveredHealth && covered.getRemainingFireTicks() <= 0,
				"Under the draught's Fire Resistance a pig takes no burn from the slag");
		helper.assertTrue(MoltenSlagBlock.burn(level, exposed) && exposed.getHealth() < exposedHealth && exposed.getRemainingFireTicks() > 0,
				"A bare pig is burned");
		helper.succeed();
	}

	/**
	 * C-02: a poultice used on a creature gives back its bowl; with no room for it the bowl drops at the user's feet, once;
	 * the last poultice leaves its bowl in the hand it was used from.
	 */
	@GameTest(maxTicks = 20)
	public void aPoulticeReturnsItsBowlToAFullInventory(GameTestHelper helper) {
		ServerPlayer user = standing(helper, new BlockPos(1, 2, 1));
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			user.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
		user.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 64));
		ItemStack poultices = salve(poultice(1, 0)).copyWithCount(2);
		user.setItemInHand(InteractionHand.MAIN_HAND, poultices);
		Mob first = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 1));
		Mob second = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 4));
		RateGate.forget(user.getUUID());
		poultices.getItem().interactLivingEntity(poultices, user, first, InteractionHand.MAIN_HAND);
		helper.assertTrue(first.hasEffect(MobEffects.REGENERATION) && user.getMainHandItem().getCount() == 1, "The first poultice takes on the pig");
		helper.assertTrue(user.getInventory().countItem(Items.BOWL) == 0 && droppedBowls(helper) == 1,
				"With no room, its bowl drops once: " + droppedBowls(helper) + " dropped");
		RateGate.forget(user.getUUID());
		ItemStack last = user.getMainHandItem();
		last.getItem().interactLivingEntity(last, user, second, InteractionHand.MAIN_HAND);
		helper.assertTrue(second.hasEffect(MobEffects.REGENERATION) && user.getMainHandItem().is(Items.BOWL) && user.getMainHandItem().getCount() == 1,
				"The last poultice leaves its bowl in the hand: " + user.getMainHandItem());
		helper.assertTrue(droppedBowls(helper) == 1, "and no other bowl drops: " + droppedBowls(helper));
		helper.succeed();
	}

	/**
	 * C-02 (05:117): brews feed no brew. The Stillroot Poultice, the Kiln Draught and Winter Brine are no ingredient: the
	 * crucible takes none of them by hand or by hopper, and the mortar grinds none.
	 */
	@GameTest(maxTicks = 20)
	public void productsNeverFeedTheCrucible(GameTestHelper helper) {
		AlchemyCatalog catalog = Alchemy.catalog();
		List<ItemStack> products = List.of(salve(poultice(1, 0)).copyWithCount(2),
				Alchemy.brew(JugcraftConcordance.DRAUGHT, bottled(replay(kilnDraught(1, false)))).copyWithCount(2),
				Alchemy.brew(JugcraftConcordance.DRAUGHT, bottled(threeSnowballs(1, Band.COLD))).copyWithCount(2));
		List<String> ids = List.of(Alchemy.STILLROOT_POULTICE, "jugcraft:kiln_draught", "jugcraft:winter_brine");
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), Blocks.STONE);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		use(crucible, player, waterBottle());
		Mixture before = crucible.mixture();
		for (int i = 0; i < products.size(); i++) {
			ItemStack product = products.get(i);
			Product named = Alchemy.product(product);
			helper.assertTrue(named != null && named.id().equals(ids.get(i)), "It is " + ids.get(i) + ": " + named);
			helper.assertTrue(catalog.ingredient(ConcordanceProgress.itemId(product)) == null, ids.get(i) + " is no ingredient");
			helper.assertTrue(use(crucible, player, product) == InteractionResult.TRY_WITH_EMPTY_HAND && product.getCount() == 2
					&& crucible.mixture().equals(before), "The crucible takes no " + ids.get(i) + " by hand: " + crucible.mixture());
			for (int slot = 0; slot <= CrucibleBlockEntity.BOTTLE_SLOT; slot++) {
				helper.assertTrue(!crucible.canPlaceItem(slot, product) && !crucible.canPlaceItemThroughFace(slot, product, Direction.UP),
						"nor from a hopper into slot " + slot + ": " + ids.get(i));
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.MORTAR));
			player.setItemInHand(InteractionHand.OFF_HAND, product);
			RateGate.forget(player.getUUID());
			helper.assertTrue(JugcraftConcordance.MORTAR.use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
					&& player.getOffhandItem().getCount() == 2 && take(player, JugcraftConcordance.REAGENT_ITEM).isEmpty(), "The mortar grinds no " + ids.get(i));
		}
		helper.succeed();
	}

	/**
	 * C-02 (C-P7): poultices never add up. Two strong ones at once leave Regeneration II for 450 ticks; a strong one
	 * replaces a weak one; later, a weak one changes nothing and a strong one only renews the time (Stacking.STRONGEST).
	 */
	@GameTest(maxTicks = 200)
	public void twoPoulticesDoNotAdd(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Brew strong = salve(poultice(1, 0)).get(JugcraftConcordance.BREW);
		Brew weak = salve(poultice(1, 1)).get(JugcraftConcordance.BREW);
		Mob twice = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 2, 1));
		Mob replaced = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 1));
		Alchemy.apply(level, strong, twice, null);
		Alchemy.apply(level, strong, twice, null);
		MobEffectInstance both = twice.getEffect(MobEffects.REGENERATION);
		helper.assertTrue(both != null && both.getAmplifier() == 1 && both.getDuration() == 450, "Two strong poultices at once: Regeneration II for 450, no more: " + both);
		Alchemy.apply(level, weak, replaced, null);
		Alchemy.apply(level, strong, replaced, null);
		MobEffectInstance stronger = replaced.getEffect(MobEffects.REGENERATION);
		helper.assertTrue(stronger != null && stronger.getAmplifier() == 1 && stronger.getDuration() == 450, "A strong one replaces a weak one: " + stronger);
		helper.runAfterDelay(100, () -> {
			int left = twice.getEffect(MobEffects.REGENERATION).getDuration();
			helper.assertTrue(left < 450, "The time runs down: " + left);
			Alchemy.apply(level, weak, twice, null);
			MobEffectInstance kept = twice.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(kept.getAmplifier() == 1 && kept.getDuration() == left, "A weak one later changes nothing: " + kept);
			Alchemy.apply(level, strong, twice, null);
			MobEffectInstance renewed = twice.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(renewed.getAmplifier() == 1 && renewed.getDuration() == 450, "A strong one renews the time, at the same level: " + renewed);
			helper.succeed();
		});
	}

	/**
	 * C-02 (C-P7): dilution never heals more. The poultice's batch in two parts of water bottles 1.063 Verdance a part
	 * twice (every dose of a batch the same share; what does not divide evenly is lost): Regeneration I for 318 ticks
	 * each, against the one part's Regeneration II for 450. Worked on players, the whole batch heals 18 and the two halves
	 * 6 and 6. The Kiln Draught's magma cream in two parts lasts no longer in all than in one. Followed as a formula, the
	 * two-part batch bottles two poultices that stack in its output.
	 */
	@GameTest(maxTicks = 20)
	public void dilutionNeverHealsMore(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		AlchemyCatalog catalog = Alchemy.catalog();
		Mixture diluted = replay(poultice(2, 0));
		Mixture.Bottled first = diluted.bottle();
		Mixture.Bottled second = first.rest().bottle();
		helper.assertTrue(first.dose().get(Axis.VERDANCE) == 1063 && second.dose().equals(first.dose()) && second.contaminant() == first.contaminant()
				&& second.rest().isEmpty(), "Two parts: 1.063 Verdance a part, twice: " + first.dose() + " / " + second.dose());
		Outcome weakFirst = Outcome.of(first.dose(), first.contaminant(), catalog, diluted.history());
		Outcome weakSecond = Outcome.of(second.dose(), second.contaminant(), catalog, first.rest().history());
		Outcome whole = bottled(replay(poultice(1, 0)));
		helper.assertTrue(weakFirst.key().equals("minecraft:regeneration@0") && weakFirst.effects().get(0).ticks() == 318
				&& weakSecond.equals(weakFirst), "Each half is Regeneration I for 318 ticks: " + weakFirst.effects() + " / " + weakSecond.effects());
		helper.assertTrue(whole.key().equals("minecraft:regeneration@1") && whole.effects().get(0).ticks() == 450, "The whole is Regeneration II for 450: " + whole.effects());
		ServerPlayer one = standing(helper, new BlockPos(1, 2, 1));
		ServerPlayer two = standing(helper, new BlockPos(4, 2, 4));
		one.setHealth(1.0F);
		two.setHealth(1.0F);
		Alchemy.apply(level, Alchemy.brew(JugcraftConcordance.SALVE, whole).get(JugcraftConcordance.BREW), one, null);
		float all = healed(one, 460);
		Alchemy.apply(level, Alchemy.brew(JugcraftConcordance.SALVE, weakFirst).get(JugcraftConcordance.BREW), two, null);
		float split = healed(two, 330);
		Alchemy.apply(level, Alchemy.brew(JugcraftConcordance.SALVE, weakSecond).get(JugcraftConcordance.BREW), two, null);
		split += healed(two, 330);
		helper.assertTrue(!one.hasEffect(MobEffects.REGENERATION) && !two.hasEffect(MobEffects.REGENERATION), "Every dose has run out");
		helper.assertTrue(all == 18.0F && split == 12.0F, "The whole heals 18, the two halves 6 and 6: " + all + " against " + split);
		Mixture creamTwice = replay(kilnDraught(2, false));
		Mixture.Bottled creamFirst = creamTwice.bottle();
		Mixture.Bottled creamSecond = creamFirst.rest().bottle();
		int halves = Outcome.of(creamFirst.dose(), creamFirst.contaminant(), catalog, creamTwice.history()).effects().get(0).ticks()
				+ Outcome.of(creamSecond.dose(), creamSecond.contaminant(), catalog, creamFirst.rest().history()).effects().get(0).ticks();
		helper.assertTrue(halves == 765 + 765 && halves <= 1531, "Two Kiln Draughts from one magma cream last no longer than one: " + halves);
		// Followed as a formula over a magma block (warm), the two-part batch bottles two poultices that stack (C-01: no
		// OUTPUT_DIFFERS pause between doses of one batch).
		CrucibleBlockEntity automated = crucible(helper, new BlockPos(1, 2, 6), Blocks.MAGMA_BLOCK);
		automated.setTemperature(60);
		ServerPlayer maker = alembist(helper, ResearchState.UNDERSTOOD);
		helper.assertTrue(use(automated, maker, written(new Formula(poultice(2, 0)).text())).consumesAction() && automated.program() != null,
				"The two-part poultice's formula is set");
		pour(automated, FluidConstants.BOTTLE * 2);
		automated.setItem(0, new ItemStack(Garden.MENDVETCH, 2));
		automated.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.BOWL, 2));
		int steps = poultice(2, 0).size();
		Diagnostic last = pulse(automated, level, steps + 2);
		ItemStack output = automated.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(last == null && output.getCount() == 2 && ItemStack.isSameItemSameComponents(output, Alchemy.brew(JugcraftConcordance.SALVE, weakFirst))
				&& automated.mixture().isEmpty() && automated.step() == 0 && automated.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty(),
				"Both doses bottled, one stack of two like poultices: " + output + " (" + last + ", step " + automated.step() + ", " + automated.mixture() + ")");
		helper.succeed();
	}

	/**
	 * C-02 (C-P2): a stack is a product by what it carries, never by its name. A poultice bottled before products were
	 * named reads as the Stillroot Poultice yet keeps the salve's own name, so it is not the same stack as a fresh one; a
	 * Night Vision salve wearing the poultice's name is none. An automated crucible whose output holds such an old salve
	 * makes its batch, then pauses (another brew in the output, C-01), voiding and taking nothing; emptied, it bottles a
	 * named poultice.
	 */
	@GameTest(maxTicks = 20)
	public void anOldUnnamedBrewInTheOutputPausesAndSaysSo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		DynamicOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
		ItemStack old = ItemStack.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"jugcraft:salve\",\"count\":1,\"components\":{\"jugcraft:brew\":"
				+ "{\"effects\":[{\"status\":\"minecraft:regeneration\",\"amplifier\":1,\"ticks\":450}],\"outcome\":\"minecraft:regeneration@1\"}}}")).getOrThrow();
		ItemStack fresh = salve(poultice(1, 0));
		Product named = Alchemy.product(old);
		helper.assertTrue(named != null && named.id().equals(Alchemy.STILLROOT_POULTICE), "By what it carries, the old salve is a Stillroot Poultice: " + named);
		helper.assertTrue(nameKey(old).equals("item.jugcraft.salve") && !ItemStack.isSameItemSameComponents(old, fresh),
				"but it keeps the salve's own name, so it is not the same stack as a fresh one: " + nameKey(old));
		ItemStack night = salve(List.of(new Operation.Water(1), new Operation.Add(GLOW, RAW), new Operation.Stir(Band.HOT), new Operation.Stir(Band.HOT)));
		night.set(DataComponents.ITEM_NAME, Component.translatable("item.jugcraft.salve.stillroot_poultice"));
		helper.assertTrue(Alchemy.product(night) == null, "A Night Vision salve wearing the poultice's name is none");

		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(2, 2, 2), Blocks.MAGMA_BLOCK);
		crucible.setTemperature(60);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		helper.assertTrue(use(crucible, player, written(new Formula(poultice(1, 0)).text())).consumesAction(), "The poultice's formula is set");
		pour(crucible, FluidConstants.BOTTLE);
		crucible.setItem(0, new ItemStack(Garden.MENDVETCH, 2));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.BOWL));
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, old.copy());
		helper.assertTrue(pulse(crucible, level, 7) == null && crucible.step() == 7, "Seven pulses make the batch: step " + crucible.step());
		Diagnostic differs = crucible.automate(level);
		ItemStack kept = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(Diagnostic.of(Code.OUTPUT_DIFFERS).equals(differs), "The old salve in its output is another brew: " + differs);
		helper.assertTrue(kept.getCount() == 1 && nameKey(kept).equals("item.jugcraft.salve") && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount() == 1
				&& crucible.mixture().parts() == 1, "Nothing voided or taken: " + kept + ", " + crucible.mixture());
		crucible.setItem(CrucibleBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
		helper.assertTrue(crucible.automate(level) == null, "Emptied, it bottles");
		ItemStack out = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
		helper.assertTrue(out.getCount() == 1 && ItemStack.isSameItemSameComponents(out, fresh) && nameKey(out).equals("item.jugcraft.salve.stillroot_poultice")
				&& crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty() && crucible.mixture().isEmpty(), "A named poultice, its bowl used: " + out);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the Crucible Stand (Concordance Chunk C, C-05)

	/**
	 * C-05 (C-D3): a mixed-band formula repeats unattended over a stand fed by a Creative Energy Cell. It heats for the next
	 * stir (warm, then hot), lets the crucible cool to warm for the repeat, and gives no heat while the crucible bottles or
	 * waits for water. Both repeats make the draught the formula makes on paper.
	 */
	@GameTest(maxTicks = 900)
	public void aStandFollowsTheFormulasBands(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleStandBlockEntity stand = onStand(helper, at);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		helper.setBlock(at.below().west(), CreativeEnergyCellBlock.BLOCK);
		helper.assertTrue(use(crucible, alembist(helper, ResearchState.UNDERSTOOD), written(WARM_THEN_HOT)).consumesAction(), "The formula is set");
		pour(crucible, FluidConstants.BOTTLE * 2);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE, 2));
		helper.assertTrue(crucible.temperature() == Heat.AMBIENT, "It starts at the air's heat");
		ItemStack expected = draught(WARM_THEN_HOT);
		int[] hottest = {Heat.AMBIENT};
		helper.onEachTick(() -> hottest[0] = Math.max(hottest[0], crucible.temperature()));
		helper.succeedWhen(() -> {
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 2 && ItemStack.isSameItemSameComponents(made, expected), "Waiting for two draughts: " + made
					+ " (step " + crucible.step() + ", " + crucible.temperature() + " degrees, " + crucible.waiting() + ")");
			helper.assertTrue(crucible.getItem(0).isEmpty() && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty()
					&& crucible.water.amount == 0 && crucible.mixture().isEmpty(), "It used exactly its glowstone, bottles and water");
			helper.assertTrue(Diagnostic.needsWater(1, 0).equals(crucible.waiting()), "Then it waits for water: " + crucible.waiting());
			helper.assertTrue(!lit(helper, at.below()) && stand.given() == Heat.AMBIENT, "and the stand gives it no heat");
			helper.assertTrue(hottest[0] >= Band.HOT.from && hottest[0] <= Heat.STAND_HOT, "Heated into hot, never past its target: " + hottest[0]);
		});
	}

	/**
	 * C-05: with no JE, a stand gives no heat. The crucible cools by the usual rule and waits for its stir's band; the Peepo
	 * port says WRONG_TEMPERATURE, as for any stir out of band (CrucibleCompanion maps WRONG_HEAT). The stand says what it
	 * lacks. Given power, it heats and the formula goes on.
	 */
	@GameTest(maxTicks = 400)
	public void anUnpoweredStandLetsTheCrucibleCool(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleStandBlockEntity stand = onStand(helper, at);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		helper.assertTrue(use(crucible, alembist(helper, ResearchState.UNDERSTOOD), written(NIGHT_EYE)).consumesAction(), "The formula is set");
		pour(crucible, FluidConstants.BOTTLE);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(pulse(crucible, level, 2) == null && crucible.step() == 2, "Water and glowstone in: step " + crucible.step());
		crucible.setTemperature(Band.HOT.from - 1);
		helper.assertTrue(!stand.heat(level) && unpowered(0, CrucibleStandBlockEntity.JE_HOT).equals(stand.plan(level).reason()),
				"With no JE it pays nothing and says so: " + stand.plan(level));
		Heat.Below below = crucible.below(level);
		helper.assertTrue(below.unlit() && below.heat() != null && below.target() == Heat.AMBIENT && !lit(helper, at.below()), "Dark, it heats nothing: " + below);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(crucible.band() == Band.COLD && crucible.temperature() >= Heat.AMBIENT && crucible.step() == 2,
					"Unpowered, it cooled into the cold band and stirred nothing: " + crucible.temperature() + ", step " + crucible.step());
			Diagnostic waiting = crucible.waiting();
			helper.assertTrue(waiting != null && waiting.code() == Code.WRONG_HEAT && crucible.companion.status() == CompanionStatus.WRONG_TEMPERATURE,
					"It waits for the hot band; the Peepo port says the temperature is wrong: " + waiting + ", " + crucible.companion.status());
			helper.assertTrue(stand.energy.getAmount() == 0 && !lit(helper, at.below()), "The stand still holds nothing and is dark");
			helper.setBlock(at.below().west(), CreativeEnergyCellBlock.BLOCK);
		});
		ItemStack expected = draught(NIGHT_EYE);
		helper.succeedWhen(() -> {
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 1 && ItemStack.isSameItemSameComponents(made, expected),
					"Powered, it heats for the stirs and the formula goes on: " + made + " (step " + crucible.step() + ", " + crucible.temperature() + ")");
		});
	}

	/**
	 * C-05 (C-P43, the unit review): a stand fed 8 JE a tick, half the hot band's rate, still brings its crucible into the
	 * hot band. A tick it cannot pay holds the crucible below the band instead of letting it cool, so each degree costs what
	 * it costs at full power, and its light stays on through those ticks. Set to hot with no formula, its crucible reaches
	 * the band; following a formula, the hot stirs are taken (it also holds the band's lowest degree, so a pulse finds the
	 * crucible in the band) and the draught is made.
	 */
	@GameTest(maxTicks = 900)
	public void aTrickleFedStandStillReachesItsBand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos set = new BlockPos(1, 2, 1);
		BlockPos following = new BlockPos(4, 2, 4);
		CrucibleStandBlockEntity setStand = onStand(helper, set);
		CrucibleBlockEntity setCrucible = helper.getBlockEntity(set, CrucibleBlockEntity.class);
		onStand(helper, following);
		CrucibleBlockEntity crucible = helper.getBlockEntity(following, CrucibleBlockEntity.class);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		for (int i = 0; i < 2; i++) {
			RateGate.forget(player.getUUID());
			setStand.useEmpty(player, false);
		}
		helper.assertTrue(setStand.setting() == Band.HOT, "Set to hot: " + setStand.setting());
		helper.assertTrue(use(crucible, player, written(NIGHT_EYE)).consumesAction(), "The formula is set");
		pour(crucible, FluidConstants.BOTTLE);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(pulse(crucible, level, 2) == null && crucible.step() == 2, "Water and glowstone in: step " + crucible.step());
		long half = CrucibleStandBlockEntity.JE_HOT / 2;
		int[] hottest = {Heat.AMBIENT};
		boolean[] wasLit = {false};
		boolean[] darkAgain = {false};
		helper.onEachTick(() -> {
			offer(level, helper.absolutePos(set.below()), half);
			offer(level, helper.absolutePos(following.below()), half);
			hottest[0] = Math.max(hottest[0], setCrucible.temperature());
			boolean on = lit(helper, set.below());
			darkAgain[0] |= wasLit[0] && !on;
			wasLit[0] |= on;
		});
		ItemStack expected = draught(NIGHT_EYE);
		helper.succeedWhen(() -> {
			helper.assertTrue(hottest[0] >= Band.HOT.from, "Set to hot and fed half its rate, its crucible reaches the hot band: " + hottest[0]
					+ " (now " + setCrucible.temperature() + ", " + setStand.energy.getAmount() + " JE)");
			helper.assertTrue(wasLit[0] && !darkAgain[0], "Its light stays on through the ticks it cannot pay");
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 1 && ItemStack.isSameItemSameComponents(made, expected),
					"Following a formula on half the rate, the hot stirs are taken: " + made + " (step " + crucible.step() + ", "
							+ crucible.temperature() + " degrees, " + crucible.waiting() + ")");
		});
	}

	/**
	 * C-05 (C-P12, C-P43): a stand pays only while its crucible can go on or waits only for heat. It idles, paying nothing
	 * and dark, while the crucible waits for water, an ingredient or a bottle, while Preserve holds it (the crucible then
	 * names the hold, as over a campfire), and while the rules in force cannot follow the formula, each time giving the
	 * crucible's own reason; with nothing on it, nothing to heat. Jade is told the same.
	 */
	@GameTest(maxTicks = 20)
	public void aStandIdlesForAWaitingOrIncompatibleFormula(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleStandBlockEntity stand = onStand(helper, at);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		helper.assertTrue(use(crucible, alembist(helper, ResearchState.UNDERSTOOD), written(NIGHT_EYE)).consumesAction(), "The formula is set");
		long full = CrucibleStandBlockEntity.CAPACITY;
		long afterOne = full - CrucibleStandBlockEntity.JE_HOT;
		stand.energy.setAmount(full);
		Diagnostic noWater = crucible.automate(level);
		helper.assertTrue(Diagnostic.needsWater(1, 0).equals(noWater) && !stand.heat(level) && stand.energy.getAmount() == full
				&& !lit(helper, at.below()) && noWater.equals(stand.plan(level).reason()) && stand.plan(level).waits()
				&& crucible.below(level).target() == Heat.AMBIENT, "Waiting for water, it pays nothing and says why: " + stand.plan(level));
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			CompoundTag shown = ConcordanceDataProvider.standSnapshot(stand, level);
			helper.assertTrue(Set.of("energy", "capacity", "following", "aim", "target", "draw", "reason", "waits", "disabled").containsAll(shown.keySet())
					&& noWater.encode().equals(shown.getStringOr("reason", "")) && shown.getBooleanOr("waits", false)
					&& shown.getBooleanOr("following", false) && shown.getLongOr("energy", -1L) == full && shown.getIntOr("draw", 0) == 0,
					"Jade is told its JE, that the formula decides, and the crucible's reason: " + shown);
		}
		pour(crucible, FluidConstants.BOTTLE);
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1, "The water poured");
		helper.assertTrue(stand.heat(level) && stand.energy.getAmount() == afterOne && lit(helper, at.below())
				&& crucible.below(level).target() == Heat.STAND_HOT && stand.plan(level).band() == Band.HOT,
				"With nothing to wait for, it pays a hot tick ahead of the hot stirs: " + stand.plan(level));
		Diagnostic noDust = crucible.automate(level);
		helper.assertTrue(noDust != null && noDust.code() == Code.MISSING_INGREDIENT && !stand.heat(level) && stand.energy.getAmount() == afterOne
				&& !lit(helper, at.below()) && crucible.below(level).target() == Heat.AMBIENT && noDust.equals(stand.plan(level).reason()),
				"Waiting for its glowstone, it pays nothing: " + stand.plan(level));
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 2 && crucible.waiting() == null, "The glowstone went in");
		long now = ConcordanceProgress.now(level);
		crucible.hold(now, Rime.HOLD_TICKS);
		Diagnostic held = Diagnostic.held(crucible.band().id, (crucible.held(now) + 19) / 20);
		helper.assertTrue(!stand.heat(level) && held.equals(stand.plan(level).reason()) && stand.plan(level).band() == null
				&& stand.energy.getAmount() == afterOne, "Held by Preserve, it gives nothing: " + stand.plan(level));
		// The unit review: the crucible names the hold too, as over a campfire; unheld, the stand would aim inside the hot band.
		Diagnostic heldPulse = crucible.automate(level);
		helper.assertTrue(held.equals(heldPulse) && crucible.step() == 2, "Its pulse says the hold keeps it from the hot stir: " + heldPulse);
		crucible.hold(now, 0);
		record Refused(boolean paid, Diagnostic reason, long energy) {
		}
		Refused refused = under(ConcordanceRules.EMPTY, () -> new Refused(stand.heat(level), stand.plan(level).reason(), stand.energy.getAmount()));
		helper.assertTrue(!refused.paid() && glowstoneRefused().equals(refused.reason()) && refused.energy() == afterOne,
				"A formula the rules refuse drives no heat: " + refused);
		helper.assertTrue(stand.heat(level), "The rules restored, it heats for the stir again");
		crucible.setTemperature(Heat.STAND_HOT);
		helper.assertTrue(pulse(crucible, level, 2) == null && crucible.step() == 4, "Both hot stirs taken");
		Diagnostic noBottle = crucible.automate(level);
		helper.assertTrue(Diagnostic.of(Code.NEEDS_CONTAINER).equals(noBottle) && !stand.heat(level) && noBottle.equals(stand.plan(level).reason()),
				"Waiting for a bottle, it gives nothing: " + stand.plan(level));
		helper.setBlock(at, Blocks.AIR);
		helper.assertTrue(!stand.heat(level) && Diagnostic.of(Code.NO_CRUCIBLE).equals(stand.plan(level).reason()), "Nothing on it: nothing to heat");
		helper.succeed();
	}

	/**
	 * C-05 (C-P12, C-P46): with no formula, an empty-hand use cycles a stand off, warm, hot, searing and off, at most once
	 * every SET_TICKS ticks a player. In someone else's claim, or in adventure mode, it changes nothing, and sneaking only
	 * reads it. Following a formula, the formula decides. Holding something, a use is the item's own: a crucible is set on
	 * the stand by a plain use, never a change of its heat.
	 */
	@GameTest(maxTicks = 20)
	public void aStandIsSetOnlyByThoseWhoMayChangeIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleStandBlockEntity stand = onStand(helper, at);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		ServerPlayer owner = alembist(helper, ResearchState.UNDERSTOOD);
		ServerPlayer stranger = alembist(helper, ResearchState.MASTERED);
		helper.assertTrue(stand.setting() == Band.COLD, "A new stand is off");
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(at.below()), owner.getUUID());
			List<Band> seen = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				RateGate.forget(owner.getUUID());
				stand.useEmpty(owner, false);
				seen.add(stand.setting());
			}
			helper.assertValueEqual(seen, List.of(Band.WARM, Band.HOT, Band.SEARING, Band.COLD, Band.WARM), "its owner cycles it");
			stand.useEmpty(owner, false);
			helper.assertTrue(stand.setting() == Band.WARM, "A second use in the same tick is refused (rate-gated)");
			RateGate.forget(stranger.getUUID());
			stand.useEmpty(stranger, false);
			stand.useEmpty(stranger, true);
			helper.assertTrue(stand.setting() == Band.WARM, "In the owner's claim a stranger changes nothing; reading changes nothing");
			stand.energy.setAmount(CrucibleStandBlockEntity.CAPACITY);
			helper.assertTrue(stand.heat(level) && crucible.below(level).target() == Heat.STAND_WARM
					&& stand.energy.getAmount() == CrucibleStandBlockEntity.CAPACITY - CrucibleStandBlockEntity.JE_WARM,
					"Set warm with no formula, it heats the crucible towards 65 for 8 JE a tick");
			helper.assertTrue(use(crucible, owner, written(NIGHT_EYE)).consumesAction(), "The owner sets a formula");
			RateGate.forget(owner.getUUID());
			stand.useEmpty(owner, false);
			helper.assertTrue(stand.setting() == Band.WARM && stand.plan(level).following() && stand.plan(level).band() == Band.HOT,
					"Following a formula, a use changes nothing: its hot stir decides: " + stand.plan(level));
			// C-P46: holding something, the stand's use is the item's own (here a crucible set on it), never a setting.
			BlockPos standAt = helper.absolutePos(at.below());
			ItemStack pot = new ItemStack(JugcraftConcordance.CRUCIBLE);
			owner.setItemInHand(InteractionHand.MAIN_HAND, pot);
			RateGate.forget(owner.getUUID());
			Band kept = stand.setting();
			helper.assertTrue(helper.getBlockState(at.below()).useItemOn(pot, level, owner, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(standAt), Direction.UP, standAt, false)) == InteractionResult.PASS && stand.setting() == kept,
					"Holding something, a use is the item's own, never a setting");
			owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		crucible.useEmpty(owner, true);
		helper.assertTrue(crucible.program() == null, "The formula is cleared");
		stranger.setGameMode(GameType.ADVENTURE);
		RateGate.forget(stranger.getUUID());
		stand.useEmpty(stranger, false);
		helper.assertTrue(stand.setting() == Band.WARM, "In adventure mode, outside any claim, nothing changes");
		stranger.setGameMode(GameType.SURVIVAL);
		stand.useEmpty(stranger, false);
		helper.assertTrue(stand.setting() == Band.HOT, "Back in survival, outside the claim, anyone may set it");
		helper.succeed();
	}

	/**
	 * C-05 (C-D3): the workshop as built. A Creative Energy Cell feeds the stand through a copper cable. An Electric Pump on
	 * a spring pipes water through a bronze pipe into the crucible's tank. The pump is cut off once the tank holds its
	 * bucket, and three water steps then draw exactly that bucket, a part each. Two draughts are made, and the stand idles
	 * while the third repeat waits for glowstone.
	 */
	@GameTest(maxTicks = 900)
	public void theFullChainRunsFromAPumpAndACable(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 2, 3);
		BlockPos cable = new BlockPos(2, 1, 3);
		helper.setBlock(new BlockPos(1, 1, 3), CreativeEnergyCellBlock.BLOCK);
		helper.setBlock(cable, JugcraftMachines.COPPER_CABLE);
		// The cable resolves its arms a tick after anything is placed beside it (CableBlock.onPlace), so the order is free.
		CrucibleStandBlockEntity stand = onStand(helper, at);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		BlockPos spring = new BlockPos(5, 1, 3);
		// A floor and four walls: the test's own floor is air, and loose water would run into the next test.
		helper.setBlock(spring.below(), Blocks.STONE);
		for (BlockPos wall : List.of(spring.west(), spring.east(), spring.north(), spring.south())) {
			helper.setBlock(wall, Blocks.STONE);
		}
		helper.setBlock(spring, Blocks.WATER);
		BlockPos pump = spring.above();
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		ElectricPumpBlockEntity pumping = helper.getBlockEntity(pump, ElectricPumpBlockEntity.class);
		pumping.energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		helper.setBlock(pump.west(), JugcraftFluids.BRONZE_FLUID_PIPE);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		ItemStack expected = draught(NIGHT_EYE);
		AtomicBoolean filled = new AtomicBoolean();
		helper.onEachTick(() -> {
			if (filled.get() || crucible.water.amount != FluidConstants.BUCKET) {
				return;
			}
			filled.set(true);
			pumping.energy().setAmount(0); // from here the tank holds exactly the bucket it was piped
			helper.assertTrue(helper.getBlockState(cable).getValue(PipeBlock.EAST) && stand.energy.getAmount() > 0,
					"The cable meets the stand's side and feeds it: " + stand.energy.getAmount());
			helper.assertTrue(use(crucible, player, written(NIGHT_EYE)).consumesAction(), "The formula is set");
			crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
			crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE, 2));
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(filled.get(), "The pump fills the tank through the pipe: " + crucible.water.amount);
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 2 && ItemStack.isSameItemSameComponents(made, expected),
					"Waiting for two draughts: " + made + " (step " + crucible.step() + ", " + crucible.temperature() + ", " + crucible.waiting() + ")");
			Diagnostic waiting = crucible.waiting();
			helper.assertTrue(waiting != null && waiting.code() == Code.MISSING_INGREDIENT, "then waits for a third glowstone: " + waiting);
			helper.assertTrue(crucible.water.amount == 0 && crucible.mixture().parts() == 1,
					"Three water steps drew exactly the piped bucket, a part each: " + crucible.water.amount + ", " + crucible.mixture());
			helper.assertTrue(crucible.getItem(0).isEmpty() && crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty(), "and both glowstone and bottles");
			helper.assertTrue(!lit(helper, at.below()), "The stand gives no heat while the crucible waits for glowstone");
		});
	}

	/**
	 * C-05: every Heat.SOURCES block heats as before (a campfire only while lit); stone and a stand's id are no source (a
	 * stand heats only when it pays). Over a lit campfire, a crucible follows its formula to its draught.
	 */
	@GameTest(maxTicks = 400)
	public void aCampfireCrucibleStillWorks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (Map.Entry<String, Integer> source : Heat.SOURCES.entrySet()) {
			Heat.Below below = new Heat.Below(source.getKey(), false);
			helper.assertTrue(source.getValue().equals(below.heat()) && below.target() == source.getValue(), "Heat for " + source.getKey() + ": " + below);
		}
		helper.assertTrue(new Heat.Below("minecraft:stone", false).heat() == null && new Heat.Below("jugcraft:crucible_stand", false).heat() == null
				&& new Heat.Below("minecraft:campfire", true).target() == Heat.AMBIENT, "Stone, a stand's id and an unlit campfire heat nothing by themselves");
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleBlockEntity crucible = crucible(helper, at, true);
		helper.assertValueEqual(crucible.below(level), new Heat.Below("minecraft:campfire", false, 120), "a lit campfire beneath");
		helper.assertTrue(use(crucible, alembist(helper, ResearchState.UNDERSTOOD), written(NIGHT_EYE)).consumesAction(), "The formula is set");
		pour(crucible, FluidConstants.BOTTLE);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
		ItemStack expected = draught(NIGHT_EYE);
		helper.succeedWhen(() -> {
			ItemStack made = crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT);
			helper.assertTrue(made.getCount() == 1 && ItemStack.isSameItemSameComponents(made, expected), "Over a campfire, one draught: " + made);
			helper.assertTrue(crucible.temperature() == 120, "settled at the campfire's 120: " + crucible.temperature());
		});
	}

	/**
	 * C-05: with the Concordance off, a stand takes no JE from a cable, pays none, gives no heat, cannot be set and shows
	 * Jade only that it is off, as the crucible stops. Switched back on (in the same tick: the server runs other tests), it
	 * does all of these again.
	 */
	@GameTest(maxTicks = 20)
	public void aSwitchedOffConcordanceStandDrawsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		CrucibleStandBlockEntity stand = onStand(helper, at);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		ServerPlayer player = alembist(helper, ResearchState.UNDERSTOOD);
		helper.assertTrue(use(crucible, player, written(NIGHT_EYE)).consumesAction(), "The formula is set");
		stand.energy.setAmount(100);
		helper.setBlock(new BlockPos(5, 1, 2), JugcraftConcordance.CRUCIBLE_STAND);
		CrucibleStandBlockEntity lone = helper.getBlockEntity(new BlockPos(5, 1, 2), CrucibleStandBlockEntity.class);
		BlockPos standAt = helper.absolutePos(at.below());
		boolean jade = FabricLoader.getInstance().isModLoaded("jade");
		record Off(boolean paid, long taken, long energy, int target, Band setting, boolean shownOff) {
		}
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		Off off;
		try {
			boolean paid = stand.heat(level);
			long taken = offer(level, standAt, 64);
			RateGate.forget(player.getUUID());
			lone.useEmpty(player, false);
			boolean shownOff = !jade || ConcordanceDataProvider.standSnapshot(stand, level).getBooleanOr("disabled", false);
			off = new Off(paid, taken, stand.energy.getAmount(), crucible.below(level).target(), lone.setting(), shownOff);
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		helper.assertValueEqual(off, new Off(false, 0L, 100L, Heat.AMBIENT, Band.COLD, true), "switched off, nothing is taken, paid, given or set");
		helper.assertTrue(!lit(helper, at.below()), "and the stand is dark");
		helper.assertTrue(stand.heat(level) && stand.energy.getAmount() == 100 - CrucibleStandBlockEntity.JE_HOT
				&& crucible.below(level).target() == Heat.STAND_HOT, "Switched back on, it pays a hot tick: " + stand.energy.getAmount());
		helper.assertValueEqual(offer(level, standAt, 64), 64L, "and takes JE from a cable again");
		RateGate.forget(player.getUUID());
		lone.useEmpty(player, false);
		helper.assertTrue(lone.setting() == Band.WARM, "and can be set");
		helper.succeed();
	}

	/**
	 * C-05 (C-P44): a cable meets a stand on each face that takes JE (four sides and underneath), never its seat. Removed,
	 * the arms retract; placed again, they reach it. The contract's cable check, beside MachineConnectionGameTests, which
	 * covers only machines.
	 */
	@GameTest(maxTicks = 40)
	public void aCableMeetsTheStandWhereItTakesPower(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 2, 3);
		for (Direction side : Direction.values()) {
			helper.setBlock(at.relative(side), JugcraftMachines.COPPER_CABLE);
		}
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE_STAND);
		helper.runAfterDelay(3, () -> {
			armsMeet(helper, at, true);
			helper.setBlock(at, Blocks.AIR);
			helper.runAfterDelay(3, () -> {
				armsMeet(helper, at, false);
				helper.setBlock(at, JugcraftConcordance.CRUCIBLE_STAND);
				helper.runAfterDelay(3, () -> {
					armsMeet(helper, at, true);
					helper.succeed();
				});
			});
		});
	}
}
