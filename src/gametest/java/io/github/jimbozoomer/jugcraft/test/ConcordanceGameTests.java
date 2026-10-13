package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import com.klikli_dev.modonomicon.book.error.BookErrorManager;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.BenchStatus;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceCodecs;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.Illumination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LampwrightBenchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LanternCharge;
import io.github.jimbozoomer.jugcraft.concordance.LumenMoteBlock;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;

/**
 * The Arcane Concordance's first loop (docs/features/arcane-concordance-first-light.md), on a real server with mock
 * players: examining and studying specimens advances First Light once per piece of evidence; Kindle casts through
 * Spell Engine only for a player who understands it, holding an instrument, with the Focus for it, and lights open air
 * only; a Kindled mote ends by itself; the bench's study and workings check everything again on the server; the
 * lantern's charge burns by the clock and survives its codec. Mock players never tick, so these tests call the tick
 * work (a block's scheduled tick, the bench's tick) themselves.
 */
public class ConcordanceGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	private static final String KINDLE = "jugcraft:kindle";
	private static final String AMETHYST = "minecraft:amethyst_shard";

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player standing on {@code standAt} (relative), facing south (+Z), level. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		return player;
	}

	private static boolean earned(ServerPlayer player, Identifier id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static ResearchState firstLight(ServerPlayer player) {
		return ConcordanceProgress.knowledge(player).state(FIRST_LIGHT);
	}

	/** Spell Engine rebuilds a player's spell containers in their tick; mock players do not tick. */
	private static void refreshSpells(ServerPlayer player) {
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	private static Holder<Spell> kindle(ServerLevel level) {
		return SpellRegistry.from(level).get(Identifier.parse(KINDLE)).orElseThrow();
	}

	private static void cast(ServerLevel level, ServerPlayer player, Holder<Spell> spell) {
		SpellExecution.performSpell(level, player, spell, SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	/** Jugcraft's own rules load whole: one entry with no prerequisite, its invocation and three workings. */
	@GameTest(maxTicks = 20)
	public void rulesLoadClean(GameTestHelper helper) {
		ConcordanceRules rules = ConcordanceData.rules();
		helper.assertTrue(rules.problems().isEmpty(), "Jugcraft's own Concordance data has no problems: " + rules.problems());
		helper.assertTrue(rules.research(FIRST_LIGHT) != null && rules.entryPoints().contains(FIRST_LIGHT),
				"First Light loads and needs nothing before it");
		helper.assertTrue(rules.invocationForSpell(KINDLE) != null, "Kindle is the invocation for the kindle spell");
		helper.assertTrue(rules.workings().size() == 3, "Three bench workings load: " + rules.workings().keySet());
		// A broken file from some data pack is left out with a reason; the good one beside it still loads.
		ConcordanceRules mixed = ConcordanceRules.build(List.of(
				new ConcordanceRules.Source("research", "test:broken", JsonParser.parseString("{\"schema\": 1, \"colour\": \"red\"}")),
				new ConcordanceRules.Source("research", "test:fine", JsonParser.parseString(
						"{\"schema\": 1, \"principle\": \"radiance\", \"tradition\": \"lampwrights\", \"stage\": \"initiate\","
								+ " \"icon\": \"minecraft:glowstone_dust\", \"states\": {\"encountered\": {\"any\": [{\"type\":"
								+ " \"examine\", \"specimens\": \"minecraft:glowstone_dust\"}]}}}"))));
		helper.assertTrue(mixed.research("test:broken") == null && mixed.research("test:fine") != null && !mixed.problems().isEmpty(),
				"A malformed entry is reported and dropped, the rest load: " + mixed.problems());
		helper.succeed();
	}

	/** The codex loads whole: Modonomicon will not open a book with a broken entry (an empty icon, say). */
	@GameTest(maxTicks = 20)
	public void codexLoadsWithoutErrors(GameTestHelper helper) {
		Identifier book = Jugcraft.id("arcane_concordance");
		helper.assertFalse(BookErrorManager.get().hasErrors(book), "The Arcane Concordance codex has no load errors: "
				+ (BookErrorManager.get().hasErrors(book) ? BookErrorManager.get().getErrors(book).getErrors() : ""));
		helper.succeed();
	}

	/**
	 * Examining in darkness encounters and observes First Light at once; repeating the same examination teaches nothing;
	 * a third distinct specimen in the dark understands it in the field, which teaches Kindle and marks the codex.
	 */
	@GameTest(maxTicks = 20)
	public void fieldEvidenceUnderstandsFirstLight(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ResearchEngine.Result first = ConcordanceProgress.record(player, new Evidence.Examined(AMETHYST, 0));
		helper.assertTrue(first.transitions().size() == 2 && firstLight(player) == ResearchState.OBSERVED,
				"Amethyst examined in the dark encounters and observes First Light: " + first.transitions());
		ResearchEngine.Result again = ConcordanceProgress.record(player, new Evidence.Examined(AMETHYST, 0));
		helper.assertFalse(again.recorded(), "The same examination twice counts once");
		helper.assertFalse(ConcordanceProgress.record(player, new Evidence.Examined("minecraft:stick", 0)).recorded(),
				"A stick is no luminous specimen");
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glowstone_dust", 3));
		helper.assertTrue(firstLight(player) == ResearchState.OBSERVED, "Two specimens are not yet three");
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glow_lichen", 9));
		helper.assertTrue(firstLight(player) == ResearchState.OBSERVED, "A specimen seen in bright light does not count");
		ConcordanceProgress.record(player, new Evidence.Examined("minecraft:glow_berries", 4));
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		helper.assertTrue(knowledge.state(FIRST_LIGHT) == ResearchState.UNDERSTOOD && knowledge.invocationCost(KINDLE) == 4,
				"Three dark specimens understand First Light and teach Kindle at 4 Focus: " + knowledge.invocations());
		helper.assertTrue(earned(player, ConcordanceProgress.advancementFor(FIRST_LIGHT, ResearchState.UNDERSTOOD)),
				"Understanding marks the codex's advancement");
		// What a player has learned survives saving.
		Knowledge loaded = ConcordanceCodecs.KNOWLEDGE.parse(JsonOps.INSTANCE,
				ConcordanceCodecs.KNOWLEDGE.encodeStart(JsonOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
		helper.assertTrue(loaded.equals(knowledge), "Knowledge round-trips through its codec");
		helper.succeed();
	}

	/** Sneak-using a specimen examines it on the server, once per half second however fast the requests come. */
	@GameTest(maxTicks = 20)
	public void examiningBySneakUse(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ItemStack amethyst = new ItemStack(Items.AMETHYST_SHARD);
		player.setItemInHand(InteractionHand.MAIN_HAND, amethyst);
		ResearchEngine.Result result = Examination.examine(player, helper.getLevel(), amethyst);
		helper.assertTrue(result != null && firstLight(player).atLeast(ResearchState.ENCOUNTERED),
				"Examining amethyst encounters First Light");
		helper.assertTrue(Examination.examine(player, helper.getLevel(), amethyst) == null, "A second request at once is ignored");
		helper.assertTrue(amethyst.getCount() == 1, "Examining uses nothing up");
		Examination.forget(player.getUUID());
		helper.succeed();
	}

	/**
	 * Kindle through Spell Engine: refused before First Light is understood; once understood, with the wand in hand, it
	 * lights the open block in front of the wall the player faces, takes 4 Focus once, starts its cooldown and counts as
	 * evidence towards mastery. During the cooldown nothing is spent.
	 */
	@GameTest(maxTicks = 20)
	public void kindleCastsThroughSpellEngine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(1, 2, 5), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 3, 5), Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		Holder<Spell> kindle = kindle(level);
		BlockPos lit = helper.absolutePos(new BlockPos(1, 3, 4));
		// Kindle is the composition "ray struck kindle_light": its ray, traced on the server, meets the wall.
		Plan plan = ConcordanceData.rules().authored(KINDLE, "jugcraft:initiate_wand").plan();
		int steps = plan.root().steps().getFirst().effect().duration() / LumenMoteBlock.STEP_TICKS;
		BlockPos aim = ComposedSpells.deliver(level, player, plan.root(), null).open();
		helper.assertTrue(aim.equals(lit) && plan.root().range() == 16 && steps == 15,
				"Kindle reaches 16 blocks and aims at the open side of the wall: " + aim + " vs " + lit + ", " + steps + " steps");

		refreshSpells(player);
		cast(level, player, kindle);
		helper.assertTrue(level.getBlockState(lit).isAir() && ConcordanceProgress.currentFocus(player) == FocusPool.MAX,
				"Without First Light understood, Kindle does nothing and costs nothing");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		refreshSpells(player);
		cast(level, player, kindle);
		BlockState mote = level.getBlockState(lit);
		helper.assertTrue(mote.is(JugcraftConcordance.LUMEN_MOTE) && mote.getValue(LumenMoteBlock.AGE) == steps
				&& mote.getLightEmission() == LumenMoteBlock.LIGHT, "Kindle sets a full-length mote of light 14: " + mote);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 4, "Kindle takes 4 Focus once: "
				+ ConcordanceProgress.currentFocus(player));
		SpellCaster.Player caster = (SpellCaster.Player) player;
		helper.assertTrue(caster.getCooldownManager().isCoolingDown(kindle), "Kindle starts its cooldown");
		helper.assertTrue(ConcordanceProgress.knowledge(player).progress(FIRST_LIGHT).evidence().keySet().stream()
				.anyMatch(key -> key.startsWith("invoke:")), "The cast counts towards mastering First Light");
		helper.assertTrue(earned(player, Jugcraft.id("concordance_first_kindle")), "The first cast marks the codex");

		cast(level, player, kindle);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 4, "During the cooldown nothing is spent");

		// Without the instrument in hand the spell is not on offer at all.
		caster.getCooldownManager().reset(null);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		refreshSpells(player);
		cast(level, player, kindle);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 4, "No instrument, no cast");

		// Out of Focus: refused, nothing spent, no cooldown.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		refreshSpells(player);
		ConcordanceProgress.setFocus(player, 3);
		cast(level, player, kindle);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == 3 && !caster.getCooldownManager().isCoolingDown(kindle),
				"With 3 Focus Kindle is refused and starts no cooldown");
		helper.succeed();
	}

	/** Light goes only into open air and ends by itself; a lantern's trail light goes out without its lantern. */
	@GameTest(maxTicks = 20)
	public void kindledLightEndsByItself(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		BlockPos stone = helper.absolutePos(new BlockPos(3, 1, 3));
		helper.assertTrue(Illumination.kindle(level, player, stone, 2) == Illumination.Result.NO_SPACE
				&& level.getBlockState(stone).is(Blocks.STONE), "Kindle never replaces a block");
		BlockPos air = helper.absolutePos(new BlockPos(3, 3, 3));
		helper.assertTrue(Illumination.kindle(level, player, air, 1) == Illumination.Result.PLACED, "Kindle lights open air");
		helper.assertTrue(Illumination.kindle(level, player, air, 1) == Illumination.Result.REFRESHED, "and refreshes its own light");
		level.getBlockState(air).tick(level, air, level.getRandom());
		helper.assertTrue(level.getBlockState(air).getValue(LumenMoteBlock.AGE) == 0, "A step passes");
		level.getBlockState(air).tick(level, air, level.getRandom());
		helper.assertTrue(level.getBlockState(air).isAir(), "and at the end the light is gone");

		BlockPos trail = helper.absolutePos(new BlockPos(5, 3, 5));
		level.setBlockAndUpdate(trail, JugcraftConcordance.LUMEN_MOTE.defaultBlockState().setValue(LumenMoteBlock.TRAIL, true));
		helper.assertTrue(level.getBlockState(trail).getLightEmission() == LumenMoteBlock.TRAIL_LIGHT, "A trail light gives 12");
		level.getBlockState(trail).tick(level, trail, level.getRandom());
		helper.assertTrue(level.getBlockState(trail).isAir(), "A trail light with no lit lantern near goes out");
		helper.succeed();
	}

	/** The lantern burns one Radiance per 400 ticks while lit, none while out, and its charge survives its codec. */
	@GameTest(maxTicks = 20)
	public void lanternBurnsByTheClock(GameTestHelper helper) {
		ItemStack lantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		KindledLanternItem.set(lantern, 10, 1000L, true);
		helper.assertTrue(KindledLanternItem.lit(lantern) && KindledLanternItem.remaining(lantern, 1000L + 3 * KindledLanternItem.BURN_TICKS) == 7,
				"Lit, three burn periods take three Radiance");
		helper.assertTrue(KindledLanternItem.remaining(lantern, 1000L + 40 * KindledLanternItem.BURN_TICKS) == 0, "and it runs out");
		KindledLanternItem.set(lantern, 10, 1000L, false);
		helper.assertTrue(KindledLanternItem.remaining(lantern, 1000L + 40 * KindledLanternItem.BURN_TICKS) == 10, "Put out, it keeps its charge");
		KindledLanternItem.set(lantern, 0, 1000L, true);
		helper.assertFalse(KindledLanternItem.lit(lantern), "An empty lantern cannot be lit");
		LanternCharge charge = new LanternCharge(12, 4321L);
		helper.assertTrue(LanternCharge.CODEC.parse(JsonOps.INSTANCE, LanternCharge.CODEC.encodeStart(JsonOps.INSTANCE, charge).getOrThrow())
				.getOrThrow().equals(charge), "The charge round-trips through its codec");
		helper.assertTrue(new LanternCharge(500, 0L).stored() == KindledLanternItem.CAPACITY, "A charge is capped at the capacity");
		helper.succeed();
	}

	/**
	 * The bench: a player who has observed First Light studies amethyst; taking it out cancels with nothing used; left
	 * to finish, the study uses one shard and understands First Light. The bench names no player to Jade and inspection
	 * changes nothing.
	 */
	@GameTest(maxTicks = 20)
	public void benchStudyUnderstandsFirstLight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos benchPos = new BlockPos(3, 2, 3);
		helper.setBlock(benchPos, JugcraftConcordance.LAMPWRIGHT_BENCH);
		LampwrightBenchBlockEntity bench = helper.getBlockEntity(benchPos, LampwrightBenchBlockEntity.class);
		BlockPos absolute = helper.absolutePos(benchPos);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 2));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.OBSERVED);

		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_STUDY) == BenchStatus.NO_SPECIMEN, "Nothing to study yet");
		bench.setItem(LampwrightBenchBlockEntity.SPECIMEN, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertTrue(bench.press(player, LampwrightBenchBlockEntity.BUTTON_STUDY) && bench.studying(), "The study starts");
		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_STUDY) == BenchStatus.BUSY, "One study at a time");
		ItemStack taken = bench.removeItemNoUpdate(LampwrightBenchBlockEntity.SPECIMEN);
		bench.serverTick(level, absolute, level.getBlockState(absolute));
		helper.assertTrue(!bench.studying() && taken.getCount() == 2, "Taking the specimen out cancels the study; nothing is used");

		bench.setItem(LampwrightBenchBlockEntity.SPECIMEN, taken);
		bench.press(player, LampwrightBenchBlockEntity.BUTTON_STUDY);
		for (int tick = 0; tick < LampwrightBenchBlockEntity.STUDY_TICKS / 2; tick++) {
			bench.serverTick(level, absolute, level.getBlockState(absolute));
		}
		if (FabricLoader.getInstance().isModLoaded("jade")) {
			int percent = ConcordanceDataProvider.snapshot(bench, level.getGameTime()).getIntOr("study", -1);
			helper.assertTrue(percent == 50 && bench.progress() == LampwrightBenchBlockEntity.STUDY_TICKS / 2,
					"Jade sees the study half done and looking changes nothing: " + percent);
		}
		for (int tick = LampwrightBenchBlockEntity.STUDY_TICKS / 2; tick < LampwrightBenchBlockEntity.STUDY_TICKS; tick++) {
			bench.serverTick(level, absolute, level.getBlockState(absolute));
		}
		helper.assertTrue(!bench.studying() && bench.getItem(LampwrightBenchBlockEntity.SPECIMEN).getCount() == 1,
				"The finished study uses one shard");
		helper.assertTrue(firstLight(player) == ResearchState.UNDERSTOOD, "and the notes understand First Light");
		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_STUDY) == BenchStatus.NOTHING_TO_LEARN,
				"Studying amethyst again would teach nothing");
		helper.succeed();
	}

	/**
	 * The workings: a stranger to First Light can run none; once it is understood a lantern and a shard kindle a
	 * Kindled Lantern holding 8, another shard infuses 8 more, and 6 Focus channel 2. Overfilling is refused with nothing
	 * used.
	 */
	@GameTest(maxTicks = 20)
	public void benchWorkingsMakeAndChargeALantern(GameTestHelper helper) {
		floor(helper);
		BlockPos benchPos = new BlockPos(3, 2, 3);
		helper.setBlock(benchPos, JugcraftConcordance.LAMPWRIGHT_BENCH);
		LampwrightBenchBlockEntity bench = helper.getBlockEntity(benchPos, LampwrightBenchBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 2));
		bench.setItem(LampwrightBenchBlockEntity.SPECIMEN, new ItemStack(Items.AMETHYST_SHARD, 3));
		bench.setItem(LampwrightBenchBlockEntity.WORK, new ItemStack(Items.LANTERN));

		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_KINDLE) == BenchStatus.UNKNOWN_RESEARCH
				&& !bench.press(player, LampwrightBenchBlockEntity.BUTTON_KINDLE)
				&& bench.getItem(LampwrightBenchBlockEntity.WORK).is(Items.LANTERN), "A stranger to First Light kindles nothing");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		long now = helper.getLevel().getGameTime();
		helper.assertTrue(bench.press(player, LampwrightBenchBlockEntity.BUTTON_KINDLE), "Kindling a lantern");
		ItemStack lantern = bench.getItem(LampwrightBenchBlockEntity.WORK);
		helper.assertTrue(lantern.is(JugcraftConcordance.KINDLED_LANTERN) && KindledLanternItem.remaining(lantern, now) == 8
				&& bench.getItem(LampwrightBenchBlockEntity.SPECIMEN).getCount() == 2, "makes a Kindled Lantern holding 8 from one shard");
		helper.assertTrue(bench.press(player, LampwrightBenchBlockEntity.BUTTON_INFUSE)
				&& KindledLanternItem.remaining(lantern, now) == 16, "Infusing a shard adds 8");
		helper.assertTrue(bench.press(player, LampwrightBenchBlockEntity.BUTTON_CHANNEL)
				&& KindledLanternItem.remaining(lantern, now) == 18 && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 6,
				"Channelling 6 Focus adds 2");

		KindledLanternItem.set(lantern, 60, now, false);
		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_INFUSE) == BenchStatus.OVERFULL
				&& !bench.press(player, LampwrightBenchBlockEntity.BUTTON_INFUSE)
				&& bench.getItem(LampwrightBenchBlockEntity.SPECIMEN).getCount() == 1, "Overfilling is refused and uses nothing");
		ConcordanceProgress.setFocus(player, 2);
		KindledLanternItem.set(lantern, 10, now, false);
		helper.assertTrue(bench.status(player, LampwrightBenchBlockEntity.BUTTON_CHANNEL) == BenchStatus.NO_FOCUS
				&& !bench.press(player, LampwrightBenchBlockEntity.BUTTON_CHANNEL), "Channelling needs the Focus");
		helper.succeed();
	}

	/** The wand resolves only Concordance spells; Jugcraft's weapons are opted out of Spell Engine's weapon fallback. */
	@GameTest(maxTicks = 20)
	public void instrumentsAndWeaponsResolveAsDesigned(GameTestHelper helper) {
		SpellContainer wand = SpellContainerHelper.containerFromItemStack(new ItemStack(JugcraftConcordance.INITIATE_WAND));
		helper.assertTrue(wand != null && wand.access() == SpellContainer.ContentType.TAG && wand.isResolver()
				&& "jugcraft:concordance".equals(wand.access_param()), "The wand resolves the Concordance spell tag: " + wand);
		SpellContainer sword = SpellContainerHelper.containerFromItemStack(
				new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("steel_longsword"))));
		helper.assertTrue(sword == null || !sword.isResolver(), "A Jugcraft weapon is no spell caster: " + sword);
		helper.succeed();
	}
}
