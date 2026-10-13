package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlock;
import io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireRecord;
import io.github.jimbozoomer.jugcraft.concordance.stages.StageProgress;
import io.github.jimbozoomer.jugcraft.concordance.wonder.FieldKind;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireConfiguration;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireDefinition;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireState;
import io.github.jimbozoomer.jugcraft.concordance.wonder.Spires;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Roadmap step 25, the Concord Spire (docs/features/arcane-concordance-spire.md), on a real server: founding needs the
 * Master stage and the configuration's research; the phases finish in order (structures built round the heart, the
 * configuration's practices, the Kindling rite at a circle nearby, three days of upkeep held) and the raised spire
 * records the Architect's milestone; a day's upkeep is taken only when it is all there; damage rests the field and a
 * repair wakes it with nothing lost; a broken heart drops its store and the spire answers its heart again; attendance
 * lapses and returns; realigning keeps the foundation; each configuration's field does its own work, bounded; the heart
 * asks a courier for its upkeep; strangers neither count nor change it.
 */
public class ConcordanceSpireGameTests {
	private static final BlockPos HEART = new BlockPos(3, 2, 3);

	private static SpireDefinition spire() {
		return ConcordSpire.definition();
	}

	private static SpireConfiguration configuration(String id) {
		return ConcordSpire.catalog().configuration(id);
	}

	/** A Master who knows {@code configuration}'s research (and Circle Lore). */
	private static ServerPlayer master(GameTestHelper helper, String configuration) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		RateGate.forget(player.getUUID());
		player.setAttached(StageProgress.STAGE, "master");
		for (var requirement : configuration(configuration).requires()) {
			ConcordanceProgress.grant(player, requirement.research(), requirement.state());
		}
		return player;
	}

	private static BlockPos at(StructurePattern.Offset offset) {
		return HEART.offset(offset.x(), offset.y(), offset.z());
	}

	/** A stone floor and the heart. */
	private static SpireHeartBlockEntity heart(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		helper.setBlock(HEART, ConcordSpire.HEART);
		return (SpireHeartBlockEntity) helper.getBlockEntity(HEART, SpireHeartBlockEntity.class);
	}

	/** Builds the structure {@code id} round the heart, as its parts ask (a tag's first block; pylons holding {@code ley}). */
	private static void build(GameTestHelper helper, String id, long ley) {
		StructurePattern pattern = ConcordanceData.rules().structure(id);
		for (StructurePattern.Part part : pattern.parts()) {
			if (part.block() == null) {
				continue;
			}
			Block block = part.block().startsWith("#")
					? (part.block().endsWith("ritual_boundary") ? JugcraftConcordance.WARDING_STONE : Blocks.POLISHED_DEEPSLATE)
					: BuiltInRegistries.BLOCK.getValue(Identifier.parse(part.block()));
			helper.setBlock(at(part.offset()), block);
			if (part.role() == StructurePattern.Role.CHANNEL && helper.getBlockEntity(at(part.offset()), LeyPylonBlockEntity.class) instanceof LeyPylonBlockEntity pylon) {
				pylon.setLey(helper.getLevel(), ley);
			}
		}
	}

	/** Forgets this test's spire, so no field of it outlives the test (other tests stand within its reach). */
	private static void forget(GameTestHelper helper) {
		SpireRecord.of(helper.getLevel().getServer()).remove(ConcordSpire.id(helper.getLevel(), helper.absolutePos(HEART)));
	}

	private static SpireState state(GameTestHelper helper) {
		return SpireRecord.of(helper.getLevel().getServer()).spire(ConcordSpire.id(helper.getLevel(), helper.absolutePos(HEART)));
	}

	private static long ley(GameTestHelper helper) {
		long total = 0;
		for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:spire_foundation").channels()) {
			total += ((LeyPylonBlockEntity) helper.getBlockEntity(at(part.offset()), LeyPylonBlockEntity.class)).ley();
		}
		return total;
	}

	private static boolean done(GameTestHelper helper, ServerPlayer player, String advancement) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Jugcraft.id(advancement));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	/**
	 * Raises a Lantern Spire for {@code keeper} through all four phases; returns the game time its last day fell on.
	 * Each phase is checked on the way.
	 */
	private static long raise(GameTestHelper helper, ServerPlayer keeper, SpireHeartBlockEntity heart) {
		ServerLevel level = helper.getLevel();
		long now = level.getGameTime();
		helper.assertValueEqual(ConcordSpire.found(keeper, level, helper.absolutePos(HEART), "jugcraft:lantern_spire"), "", "the spire is founded");
		helper.assertValueEqual(heart.work(level, now), "raising", "a founded spire is being raised");
		helper.assertValueEqual(state(helper).phase(), 0, "nothing stands yet: the foundation");
		build(helper, "jugcraft:spire_foundation", 40);
		heart.work(level, now += 20);
		helper.assertValueEqual(state(helper).phase(), 1, "the foundation stands: the shaft");
		build(helper, "jugcraft:spire_shaft", 40);
		heart.work(level, now += 20);
		helper.assertValueEqual(state(helper).phase(), 1, "the shaft waits for its practices");
		for (int i = 0; i < 2; i++) {
			ConcordanceProgress.record(keeper, new Evidence.Practiced("jugcraft:ritual", "test_" + i));
		}
		heart.work(level, now += 20);
		helper.assertValueEqual(state(helper).phase(), 2, "two rituals finish the shaft: the crown");
		build(helper, "jugcraft:spire_crown_lantern", 40);
		heart.work(level, now += 20);
		helper.assertValueEqual(state(helper).phase(), 2, "the crown waits for its rite");
		Rituals.completed(level, helper.absolutePos(HEART).offset(6, 0, 0), "jugcraft:spire_kindling", List.of(keeper));
		heart.work(level, now += 20);
		helper.assertValueEqual(state(helper).phase(), 3, "the Kindling finishes the crown: the kindling");
		long day = now;
		heart.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 32));
		for (int i = 1; i <= 3; i++) {
			day += Spires.DAY;
			ConcordanceProgress.record(keeper, new Evidence.Practiced("jugcraft:ritual", "attend_" + i));
			heart.work(level, day);
		}
		return day;
	}

	/** Founding needs the Master stage and the configuration's research; a place holds one spire. */
	@GameTest(maxTicks = 20)
	public void foundingNeedsTheMasterStageAndTheResearch(GameTestHelper helper) {
		heart(helper);
		ServerLevel level = helper.getLevel();
		BlockPos heart = helper.absolutePos(HEART);
		ServerPlayer novice = helper.makeMockServerPlayerInLevel();
		helper.assertValueEqual(ConcordSpire.found(novice, level, heart, "jugcraft:lantern_spire"), "stage", "an Adept cannot found one");
		novice.setAttached(StageProgress.STAGE, "master");
		helper.assertValueEqual(ConcordSpire.found(novice, level, heart, "jugcraft:lantern_spire"), "research", "nor a Master without its research");
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		helper.assertValueEqual(ConcordSpire.found(keeper, level, heart, "jugcraft:verdant_spire"), "research", "each configuration its own research");
		helper.assertValueEqual(ConcordSpire.found(keeper, level, heart, "jugcraft:lantern_spire"), "", "a Master who knows it founds it");
		helper.assertTrue(done(helper, keeper, "concord_spire_founded"), "the founding's advancement");
		helper.assertValueEqual(ConcordSpire.found(keeper, level, heart, "jugcraft:lantern_spire"), "founded", "a place holds one spire");
		SpireState state = state(helper);
		helper.assertTrue(state != null && state.keeper().equals(keeper.getUUID()) && state.phase() == 0, "recorded by the world, phase one");
		forget(helper);
		helper.succeed();
	}

	/**
	 * The phases finish in order, each when what it asks for is there; three days of upkeep held raise the spire, which
	 * then works, records the Architect's milestone and took exactly three days' Ley and glowstone.
	 */
	@GameTest(maxTicks = 40)
	public void phasesFinishInOrderAndTheSpireIsRaised(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		long day = raise(helper, keeper, heart);
		SpireState state = state(helper);
		helper.assertTrue(state.raised(spire()), "three days held raise it");
		helper.assertValueEqual(heart.status(), "active", "a raised spire works");
		helper.assertValueEqual(heart.count("minecraft:glowstone_dust"), 32 - 3 * 4, "three days' glowstone taken, no more");
		helper.assertValueEqual(ley(helper), 4 * 40L - 3 * 8, "three days' Ley Charge drawn from its pylons, no more");
		helper.assertTrue(keeper.getAttached(StageProgress.MILESTONES) != null && keeper.getAttached(StageProgress.MILESTONES).contains(ConcordSpire.MILESTONE),
				"raising it records the milestone");
		helper.assertTrue(done(helper, keeper, "concord_spire_raised"), "and its advancement");
		helper.assertValueEqual(heart.work(helper.getLevel(), day + 20), "active", "it goes on working");
		forget(helper);
		helper.succeed();
	}

	/**
	 * A day's upkeep is taken only when all of it is there; a day short takes nothing and the count of days starts again.
	 * What shows is what happened (roadmap step 27): a day short shows a shortage at the heart and nothing travelling; a
	 * day held shows the Ley Charge travelling to the heart from each pylon it was drawn from, and from no other.
	 */
	@GameTest(maxTicks = 40)
	public void upkeepIsTakenOnlyWhenItIsAllThere(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		ServerLevel level = helper.getLevel();
		long now = level.getGameTime();
		ConcordSpire.found(keeper, level, helper.absolutePos(HEART), "jugcraft:lantern_spire");
		for (String structure : List.of("jugcraft:spire_foundation", "jugcraft:spire_shaft", "jugcraft:spire_crown_lantern")) {
			build(helper, structure, 40);
		}
		SpireRecord record = SpireRecord.of(level.getServer());
		SpireState kindling = state(helper);
		for (int i = 0; i < 3; i++) {
			kindling = Spires.advance(spire(), kindling, now);
		}
		record.put(kindling);
		heart.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
		SignWatch signs = new SignWatch(helper);
		heart.work(level, now + Spires.DAY);
		helper.assertValueEqual(state(helper).sustained(), 0, "two glowstone dust are not a day's four");
		helper.assertValueEqual(heart.count("minecraft:glowstone_dust"), 2, "and nothing was taken");
		helper.assertValueEqual(ley(helper), 160L, "not even the Ley");
		helper.assertValueEqual(signs.at(Sign.WANT, HEART), 1L, "a day short shows its shortage at the heart");
		helper.assertTrue(signs.of(Sign.FLOW).isEmpty(), "and nothing travels: " + signs.all());
		heart.setItem(1, new ItemStack(Items.GLOWSTONE_DUST, 4));
		heart.work(level, now + 2 * Spires.DAY);
		helper.assertValueEqual(state(helper).sustained(), 1, "a day with all of it is held");
		helper.assertValueEqual(heart.count("minecraft:glowstone_dust"), 2, "and four were taken");
		long fromPylons = 0;
		for (StructurePattern.Part part : ConcordanceData.rules().structure("jugcraft:spire_foundation").channels()) {
			long ley = ((LeyPylonBlockEntity) helper.getBlockEntity(at(part.offset()), LeyPylonBlockEntity.class)).ley();
			long flows = signs.flowed(Sign.FLOW, at(part.offset()), HEART);
			helper.assertValueEqual(flows, ley < 40 ? 1L : 0L, "Ley travels from the pylon at " + part.offset() + " only if it was drawn from");
			fromPylons += flows;
		}
		helper.assertTrue(ley(helper) < 160L && fromPylons > 0 && fromPylons == signs.of(Sign.FLOW).size(),
				"the day's Ley travels to the heart from where it was drawn, and from nowhere else: " + signs.all());
		signs.close();
		forget(helper);
		helper.succeed();
	}

	/**
	 * Damage rests the field and says where; putting the part back wakes it; nothing is lost. It shows (roadmap step 27):
	 * danger at the heart when it is damaged, success when it works again, each once.
	 */
	@GameTest(maxTicks = 40)
	public void aDamagedSpireRestsAndIsRepaired(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		long day = raise(helper, keeper, heart);
		SignWatch signs = new SignWatch(helper);
		BlockPos shaft = HEART.above(2);
		helper.setBlock(shaft, Blocks.AIR);
		helper.assertValueEqual(heart.work(helper.getLevel(), day + 40), "damaged", "a missing shaft block rests the spire");
		helper.assertTrue(state(helper).raised(spire()), "it keeps its phase");
		heart.work(helper.getLevel(), day + 50);
		helper.assertTrue(signs.at(Sign.PERIL, HEART) == 1 && signs.of(Sign.DONE).isEmpty(), "danger shows once, when it is damaged: " + signs.all());
		helper.setBlock(shaft, Blocks.DEEPSLATE_BRICKS);
		helper.assertValueEqual(heart.work(helper.getLevel(), day + 60), "active", "any spire stone put back wakes it");
		helper.assertValueEqual(signs.at(Sign.DONE, HEART), 1L, "and its waking shows success");
		signs.close();
		forget(helper);
		helper.succeed();
	}

	/** A broken heart drops its store and the spire keeps everything; a heart put back answers as before. */
	@GameTest(maxTicks = 40)
	public void aBrokenHeartLosesNothing(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		long day = raise(helper, keeper, heart);
		ServerLevel level = helper.getLevel();
		BlockPos where = helper.absolutePos(HEART);
		level.destroyBlock(where, false);
		List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(where).inflate(2), item -> item.getItem().is(Items.GLOWSTONE_DUST));
		helper.assertValueEqual(dropped.stream().mapToInt(item -> item.getItem().getCount()).sum(), 20, "its store drops, all of it");
		helper.assertTrue(state(helper) != null && state(helper).raised(spire()), "the world keeps the spire");
		helper.setBlock(HEART, ConcordSpire.HEART);
		SpireHeartBlockEntity again = (SpireHeartBlockEntity) helper.getBlockEntity(HEART, SpireHeartBlockEntity.class);
		helper.assertValueEqual(again.work(level, day + 40), "active", "a heart put back answers as before");
		forget(helper);
		helper.succeed();
	}

	/** A spire nobody keeps practising rests, which shows as a shortage at its heart; the next practice wakes it. */
	@GameTest(maxTicks = 40)
	public void attendanceLapsesAndReturns(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		raise(helper, keeper, heart);
		long now = helper.getLevel().getGameTime();
		SpireState state = state(helper);
		SpireRecord.of(helper.getLevel().getServer()).put(new SpireState(state.id(), state.wonder(), state.configuration(), state.keeper(),
				state.communal(), state.phase(), state.phaseBegan(), state.practiced(), state.rite(), state.sustained(), now + Spires.DAY,
				now - (spire().attendanceDays() + 1) * Spires.DAY, state.supplied(), state.founded(), state.contributors()));
		SignWatch signs = new SignWatch(helper);
		helper.assertValueEqual(heart.work(helper.getLevel(), now), "unattended", "a week and a day without a ritual rests it");
		helper.assertValueEqual(signs.at(Sign.WANT, HEART), 1L, "its lapse shows as a shortage at the heart");
		signs.close();
		ConcordanceProgress.record(keeper, new Evidence.Practiced("jugcraft:ritual", "back"));
		helper.assertValueEqual(state(helper).lastAttended(), now, "a ritual attends it");
		helper.assertValueEqual(heart.work(helper.getLevel(), now), "active", "and it works again");
		forget(helper);
		helper.succeed();
	}

	/** Realigning a raised spire keeps its foundation and shaft; the new crown, rite and days are done again. */
	@GameTest(maxTicks = 40)
	public void realigningKeepsTheFoundation(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		long day = raise(helper, keeper, heart);
		keeper.teleportTo(helper.absolutePos(HEART).getX() + 0.5, helper.absolutePos(HEART).getY() + 1, helper.absolutePos(HEART).getZ() + 2.5);
		helper.assertValueEqual(ConcordSpire.realign(keeper, "jugcraft:verdant_spire"), "research", "the new configuration's research first");
		ConcordanceProgress.grant(keeper, "jugcraft:verdant_husbandry", ResearchState.MASTERED);
		helper.assertValueEqual(ConcordSpire.realign(keeper, "jugcraft:verdant_spire"), "", "realigned");
		SpireState state = state(helper);
		helper.assertValueEqual(state.phase(), Spires.crownPhase(spire()), "back to the crown");
		helper.assertValueEqual(state.configuration(), "jugcraft:verdant_spire", "as a Verdant Spire");
		helper.assertValueEqual(heart.work(helper.getLevel(), day + 40), "raising", "the lantern's crown is not the verdant's");
		helper.assertValueEqual(state(helper).phase(), Spires.crownPhase(spire()), "the foundation and shaft still stand for it");
		forget(helper);
		helper.succeed();
	}

	/** Each configuration's field does its own work, bounded: light in the dark, crops a step older, Focus and a reveal. */
	@GameTest(maxTicks = 40)
	public void eachConfigurationsFieldDoesItsWork(GameTestHelper helper) {
		heart(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = master(helper, "jugcraft:star_spire");
		BlockPos heart = helper.absolutePos(HEART);
		SpireState state = Spires.found(ConcordSpire.id(level, heart), spire(), configuration("jugcraft:star_spire"), keeper.getUUID(), false, 0L);
		// A field three blocks across, so the test keeps to its own ground.
		SpireConfiguration lantern = configuration("jugcraft:lantern_spire");
		SpireConfiguration small = new SpireConfiguration(lantern.id(), lantern.wonder(), lantern.tradition(), lantern.requires(), lantern.practice(),
				lantern.crown(), lantern.upkeepItem(), lantern.upkeepCount(), lantern.upkeepLey(), FieldKind.ILLUMINATION, 3, 4);
		// Roadmap step 28: a spire whose keeper is away changes no block (unless the server lets a stand-in answer).
		SpireState away = Spires.found(ConcordSpire.id(level, heart), spire(), configuration("jugcraft:star_spire"), new UUID(28L, 9L), false, 0L);
		helper.assertValueEqual(ConcordSpire.field(level, heart, away, small), 0, "its keeper away, it lights nothing");
		int lit = ConcordSpire.field(level, heart, state, small);
		helper.assertTrue(lit > 0 && lit <= 4, "light in the dark, at most four: " + lit);
		int motes = 0;
		for (BlockPos pos : BlockPos.betweenClosed(heart.offset(-3, -6, -3), heart.offset(3, 4, 3))) {
			motes += level.getBlockState(pos).is(JugcraftConcordance.LUMEN_MOTE) ? 1 : 0;
		}
		helper.assertValueEqual(motes, lit, "each light is a Kindled mote in open air");
		helper.setBlock(new BlockPos(5, 1, 3), Blocks.FARMLAND);
		helper.setBlock(new BlockPos(5, 2, 3), Blocks.WHEAT);
		SpireConfiguration verdant = configuration("jugcraft:verdant_spire");
		SpireConfiguration garden = new SpireConfiguration(verdant.id(), verdant.wonder(), verdant.tradition(), verdant.requires(), verdant.practice(),
				verdant.crown(), verdant.upkeepItem(), verdant.upkeepCount(), verdant.upkeepLey(), FieldKind.GROWTH, 2, 6);
		for (int pulse = 0; pulse < 8; pulse++) {
			helper.assertValueEqual(ConcordSpire.field(level, heart, away, garden), 0, "its keeper away, it grows nothing");
		}
		helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 3)).equals(Blocks.WHEAT.defaultBlockState()), "the wheat is as planted");
		for (int pulse = 0; pulse < 8; pulse++) {
			ConcordSpire.field(level, heart, state, garden);
		}
		BlockState wheat = helper.getBlockState(new BlockPos(5, 2, 3));
		helper.assertTrue(wheat.getBlock() instanceof CropBlock && !wheat.equals(Blocks.WHEAT.defaultBlockState()), "the wheat grew: " + wheat);
		keeper.teleportTo(heart.getX() + 0.5, heart.getY() + 1, heart.getZ() + 1.5);
		ConcordanceProgress.setFocus(keeper, 0);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
		SpireConfiguration star = configuration("jugcraft:star_spire");
		ConcordSpire.field(level, heart, state, new SpireConfiguration(star.id(), star.wonder(), star.tradition(), star.requires(), star.practice(),
				star.crown(), star.upkeepItem(), star.upkeepCount(), star.upkeepLey(), FieldKind.FOCUS, 4, 6));
		helper.assertValueEqual(ConcordanceProgress.currentFocus(keeper), 1, "its keeper regains a Focus");
		helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING), "a hostile creature is revealed");
		zombie.discard();
		helper.succeed();
	}

	/**
	 * Roadmap step 29, growth acceleration with energy generation: a Verdant field hastens Concordance crops only as
	 * their beds pay (a nutrient a step at least) and fixes nothing, so over many pulses a Mendvetch (free and fixing
	 * when time grows it) and a Sunpetal each grow exactly as far as their beds' nutrients go, and the beds hold no
	 * more than they were given. Nothing the field does can feed a Verdant Heart for free.
	 */
	@GameTest(maxTicks = 80)
	public void aVerdantFieldHastensOnlyWhatItsBedsPay(GameTestHelper helper) {
		heart(helper);
		BlockPos vetch = new BlockPos(1, 1, 3);
		BlockPos petal = new BlockPos(5, 1, 3);
		for (BlockPos at : List.of(vetch, petal)) {
			helper.setBlock(at, Garden.VERDANT_BED.defaultBlockState().setValue(VerdantBedBlock.MOISTURE, VerdantBedBlock.WET));
			helper.setBlock(at.above().north(), Blocks.GLOWSTONE);
		}
		helper.setBlock(vetch.above(), Garden.MENDVETCH_CROP);
		helper.setBlock(petal.above(), Garden.SUNPETAL_CROP);
		ServerPlayer keeper = master(helper, "jugcraft:verdant_spire");
		// Awake from the start, so the crops' own ticks have read the area round them by the time the field pulses.
		VerdantBedBlockEntity vetchBed = helper.getBlockEntity(vetch, VerdantBedBlockEntity.class);
		VerdantBedBlockEntity petalBed = helper.getBlockEntity(petal, VerdantBedBlockEntity.class);
		for (VerdantBedBlockEntity bed : List.of(vetchBed, petalBed)) {
			bed.awaken(keeper.getUUID());
			bed.setNutrients(4);
		}
		// Read the area round each crop early (a level takes only so many readings a tick): any one reading serves.
		for (int tick : new int[] {10, 20, 30}) {
			helper.runAtTickTime(tick, () -> {
				vetchBed.habitat(helper.getLevel());
				petalBed.habitat(helper.getLevel());
			});
		}
		helper.runAtTickTime(40, () -> {
			ServerLevel level = helper.getLevel();
			BlockPos heart = helper.absolutePos(HEART);
			SpireState state = Spires.found(ConcordSpire.id(level, heart), spire(), configuration("jugcraft:verdant_spire"), keeper.getUUID(), false, 0L);
			SpireConfiguration verdant = configuration("jugcraft:verdant_spire");
			SpireConfiguration field = new SpireConfiguration(verdant.id(), verdant.wonder(), verdant.tradition(), verdant.requires(), verdant.practice(),
					verdant.crown(), verdant.upkeepItem(), verdant.upkeepCount(), verdant.upkeepLey(), FieldKind.GROWTH, 2, 6);
			// Set again: a random tick may have grown a step meanwhile.
			helper.setBlock(vetch.above(), Garden.MENDVETCH_CROP);
			helper.setBlock(petal.above(), Garden.SUNPETAL_CROP);
			vetchBed.setNutrients(1);
			petalBed.setNutrients(1);
			int grown = 0;
			for (int pulse = 0; pulse < 12; pulse++) {
				grown += ConcordSpire.field(level, heart, state, field);
				helper.assertTrue(vetchBed.nutrients() + petalBed.nutrients() <= 2, "the beds never gain: " + vetchBed.nutrients() + "/" + petalBed.nutrients());
			}
			int vetchAge = helper.getBlockState(vetch.above()).getValue(OrganismCropBlock.AGE);
			int petalAge = helper.getBlockState(petal.above()).getValue(OrganismCropBlock.AGE);
			helper.assertTrue(vetchAge == 1 && petalAge == 1 && grown == 2,
					"each grew one step for its bed's one nutrient, and no further: " + vetchAge + ", " + petalAge + ", " + grown);
			helper.assertTrue(vetchBed.nutrients() == 0 && petalBed.nutrients() == 0, "both beds paid; the Mendvetch fixed nothing");
			helper.succeed();
		});
	}

	/** While its store holds less than two days' upkeep, the heart asks the keeper's Courier Post for the rest, once. */
	@GameTest(maxTicks = 40)
	public void theHeartAsksACourierForItsUpkeep(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(6, 2, 6), Couriers.COURIER_POST);
		((CourierPostBlockEntity) helper.getBlockEntity(new BlockPos(6, 2, 6), CourierPostBlockEntity.class)).setOwner(keeper.getUUID());
		ConcordSpire.found(keeper, level, helper.absolutePos(HEART), "jugcraft:lantern_spire");
		SpireState state = state(helper);
		for (int i = 0; i < 3; i++) {
			state = Spires.advance(spire(), state, level.getGameTime());
		}
		SpireRecord.of(level.getServer()).put(state);
		heart.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 3));
		heart.work(level, level.getGameTime());
		Request request = CourierLedger.of(level.getServer()).ledger().get(heart.request());
		helper.assertTrue(request != null, "a request was filed");
		helper.assertTrue(request.ticket().item().contains("glowstone_dust") && request.ticket().wanted() == 5
				&& request.ticket().destination().equals(Couriers.place(level, helper.absolutePos(HEART)))
				&& request.ticket().requester().equals(keeper.getUUID()), "five glowstone dust to the heart, as its keeper's: " + request.ticket());
		long first = heart.request();
		heart.work(level, level.getGameTime() + 20);
		helper.assertValueEqual(heart.request(), first, "no second request while the first is open");
		CourierLedger.of(level.getServer()).destinationGone(Couriers.place(level, helper.absolutePos(HEART)), level.getGameTime());
		forget(helper);
		helper.succeed();
	}

	/** A stranger's practices count for nothing and a stranger cannot fill or share the spire; a shared party's can. */
	@GameTest(maxTicks = 40)
	public void strangersNeitherCountNorChangeIt(GameTestHelper helper) {
		SpireHeartBlockEntity heart = heart(helper);
		ServerPlayer keeper = master(helper, "jugcraft:lantern_spire");
		ServerLevel level = helper.getLevel();
		ConcordSpire.found(keeper, level, helper.absolutePos(HEART), "jugcraft:lantern_spire");
		SpireState state = Spires.advance(spire(), state(helper), level.getGameTime());
		SpireRecord.of(level.getServer()).put(state);
		ServerPlayer stranger = master(helper, "jugcraft:lantern_spire");
		ConcordanceProgress.record(stranger, new Evidence.Practiced("jugcraft:ritual", "stranger"));
		helper.assertValueEqual(state(helper).practiced(), 0, "a stranger's ritual counts for nothing");
		ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST, 8);
		heart.offer(stranger, level, dust);
		helper.assertValueEqual(heart.count("minecraft:glowstone_dust"), 0, "a stranger cannot fill its store");
		heart.use(stranger, level, true);
		helper.assertTrue(!state(helper).communal(), "nor share it");
		heart.use(keeper, level, true);
		helper.assertTrue(state(helper).communal(), "its keeper shares it");
		ConcordanceProgress.record(keeper, new Evidence.Practiced("jugcraft:ritual", "keeper"));
		helper.assertValueEqual(state(helper).practiced(), 1, "its keeper's ritual counts");
		forget(helper);
		helper.succeed();
	}
}
