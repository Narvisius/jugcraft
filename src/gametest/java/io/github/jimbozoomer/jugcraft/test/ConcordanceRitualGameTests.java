package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.encyclopedia.InspectPage;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Choice;
import io.github.jimbozoomer.jugcraft.concordance.ritual.Readiness;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructureValidator;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.strata.Strata;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Encyclopedia;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 12, rituals: a Lesser Circle built round a Circle Anchor, checked part by part; the Adept's Attunement
 * carried through to completion, consuming exactly its offerings once and making one Adept's Wand that keeps its
 * inscription; and every way a running ritual can stop (a broken channel, a lost boundary with its backlash, a dry
 * pylon, a participant gone, the leader calling it off, the anchor broken, the ritual saved mid-run, too much light),
 * each releasing the offerings and committing nothing. Also the two-person Lumen Vigil, pylon charging and refusals.
 * Concordance Chunk C, C-03: an idle circle says everything a rite still needs at once, a run-level shortfall only warns,
 * a refused start and a chat reading mark every faulted part (the Inspect page none), what a run has spent is shown apart
 * from what it holds, an interruption is told in one line kept for the Inspect page, and a rite the reader has not
 * learned is never named. Concordance Chunk C, C-04: Root and Rivet, a Thallite plate rooted into a foundation, completed
 * once, stopped by a dry pylon, lapsed when saved mid-run, never chosen for the attunement's offerings, and a
 * Stratawright's foundation laid.
 */
public class ConcordanceRitualGameTests {
	private static final String CIRCLE_LORE = "jugcraft:circle_lore";
	private static final BlockPos ANCHOR = new BlockPos(3, 2, 3);
	/** Where participants stand inside the circle (no part there). */
	private static final BlockPos STAND = new BlockPos(4, 2, 4);
	private static final BlockPos STAND_2 = new BlockPos(2, 2, 2);
	/** Long enough for the first step to have passed after the ritual starts (STEP_TICKS 40) plus margin. */
	private static final int AFTER_FIRST_STEP = 50;
	/** Past a mock player's spawn invulnerability, so backlash can land. */
	private static final int PAST_SPAWN_GRACE = 70;

	private static StructurePattern circle() {
		return ConcordanceData.rules().structure("jugcraft:lesser_circle");
	}

	private static BlockPos at(StructurePattern.Offset offset) {
		return ANCHOR.offset(offset.x(), offset.y(), offset.z());
	}

	private static BlockPos first(StructurePattern.Role role) {
		for (StructurePattern.Part part : circle().parts()) {
			if (part.role() == role) {
				return at(part.offset());
			}
		}
		throw new IllegalStateException("no " + role);
	}

	/** A stone floor and a complete Lesser Circle whose pylons each hold {@code ley}. */
	private static CircleAnchorBlockEntity build(GameTestHelper helper, long ley) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		helper.setBlock(ANCHOR, JugcraftConcordance.CIRCLE_ANCHOR);
		for (StructurePattern.Part part : circle().parts()) {
			if (part.role() == StructurePattern.Role.CHANNEL) {
				helper.setBlock(at(part.offset()), JugcraftConcordance.LEY_PYLON);
				pylon(helper, at(part.offset())).setLey(helper.getLevel(), ley);
			} else if (part.role() == StructurePattern.Role.BOUNDARY) {
				helper.setBlock(at(part.offset()), JugcraftConcordance.WARDING_STONE);
			}
		}
		return anchor(helper);
	}

	/** Stone walls and a roof round the circle, so no daylight reaches it (the vigil needs the dark). */
	private static void enclose(GameTestHelper helper) {
		for (int y = 2; y <= 6; y++) {
			for (int i = 0; i <= 7; i++) {
				for (BlockPos wall : List.of(new BlockPos(i, y, 0), new BlockPos(i, y, 7), new BlockPos(0, y, i), new BlockPos(7, y, i))) {
					if (helper.getBlockState(wall).isAir()) {
						helper.setBlock(wall, Blocks.STONE);
					}
				}
			}
		}
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 7, z), Blocks.STONE);
			}
		}
	}

	private static CircleAnchorBlockEntity anchor(GameTestHelper helper) {
		return (CircleAnchorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
	}

	private static LeyPylonBlockEntity pylon(GameTestHelper helper, BlockPos pos) {
		return (LeyPylonBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
	}

	/** A survival player standing inside the circle who understands Circle Lore and has full Focus. */
	private static ServerPlayer practitioner(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, CIRCLE_LORE, ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(player, 20);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static ItemStack inscribedWand() {
		ItemStack wand = new ItemStack(JugcraftConcordance.INITIATE_WAND);
		wand.set(JugcraftConcordance.INSCRIPTION, new Inscription("touch struck light", 2, 10));
		return wand;
	}

	/** Offers the Adept's Attunement's offerings, with one gold ingot more than it needs. */
	private static void offerAttunement(CircleAnchorBlockEntity anchor, ServerPlayer player) {
		for (ItemStack stack : List.of(inscribedWand(), new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(Items.GOLD_INGOT, 3),
				new ItemStack(Items.GLOWSTONE_DUST, 4))) {
			anchor.offer(player, stack);
		}
	}

	private static void use(CircleAnchorBlockEntity anchor, ServerPlayer player, boolean sneaking) {
		RateGate.forget(player.getUUID());
		anchor.use(player, (ServerLevel) player.level(), sneaking);
	}

	private static int count(List<ItemStack> stacks, Item item) {
		int total = 0;
		for (ItemStack stack : stacks) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** Every offering is still in the anchor, unlocked, and nothing was made. */
	private static void offeringsKept(GameTestHelper helper, CircleAnchorBlockEntity anchor, RitualMachine.Interruption reason) {
		List<ItemStack> held = anchor.offerings();
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().last() == reason && !anchor.run().completed(),
				"The ritual broke off for " + reason + ": " + anchor.run());
		helper.assertTrue(count(held, JugcraftConcordance.INITIATE_WAND) == 1 && count(held, Items.AMETHYST_SHARD) == 4
				&& count(held, Items.GOLD_INGOT) == 3 && count(held, Items.GLOWSTONE_DUST) == 4,
				"Every offering is still in the anchor after " + reason + ": " + held);
		helper.assertTrue(anchor.output().isEmpty(), "Nothing was made after " + reason);
	}

	/** Starts the Adept's Attunement and checks it channels with the Focus paid. */
	private static ServerPlayer startAttunement(GameTestHelper helper, CircleAnchorBlockEntity anchor) {
		ServerPlayer player = practitioner(helper, STAND);
		offerAttunement(anchor, player);
		use(anchor, player, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING, "The attunement starts: " + anchor.run());
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == 20 - 6, "It took six Focus");
		return player;
	}

	// ---------------------------------------------------------------- the circle

	/**
	 * The validator names every fault at its place (missing, wrong block, obstructed, unpowered, someone else's pylon),
	 * an idle anchor answers from a cached report, and a change to a circle part voids the cache at once.
	 */
	@GameTest(maxTicks = 40)
	public void theCircleIsCheckedPartByPart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		helper.assertTrue(Rituals.loaded(level) >= 1, "The anchor joined the index of loaded anchors");
		StructureValidator.Report report = anchor.report(level);
		helper.assertTrue(report != null && report.complete() && report.linked() == 0b1111, "A complete circle: " + report);
		BlockPos pylon = first(StructurePattern.Role.CHANNEL);
		helper.setBlock(pylon, Blocks.AIR);
		report = anchor.report(level);
		helper.assertTrue(report.faults().size() == 1 && report.faults().get(0).problem() == StructureValidator.Problem.MISSING
				&& report.linked() == 0b1110, "Removing a pylon voids the cached report and shows it missing: " + report.faults());
		helper.setBlock(pylon, Blocks.COBBLESTONE);
		Rituals.changed(level, helper.absolutePos(pylon));
		report = anchor.report(level);
		helper.assertTrue(report.faults().get(0).problem() == StructureValidator.Problem.INCOMPATIBLE, "Cobblestone is the wrong block");
		helper.setBlock(pylon, JugcraftConcordance.LEY_PYLON);
		BlockPos clearance = first(StructurePattern.Role.CLEARANCE);
		helper.setBlock(clearance, Blocks.DIRT);
		Rituals.changed(level, helper.absolutePos(clearance));
		report = anchor.report(level);
		helper.assertTrue(report.faults().size() == 1 && report.faults().get(0).problem() == StructureValidator.Problem.OBSTRUCTED,
				"Dirt above the anchor obstructs it (the new pylon counts again): " + report.faults());
		helper.setBlock(clearance, Blocks.AIR);
		BlockPos stone = first(StructurePattern.Role.BOUNDARY);
		helper.setBlock(stone, Blocks.AIR);
		report = anchor.report(level);
		helper.assertTrue(report.faults().size() == 1 && report.faults().get(0).part().role() == StructurePattern.Role.BOUNDARY,
				"A Warding Stone removed voids the report by itself: " + report.faults());
		helper.setBlock(stone, JugcraftConcordance.WARDING_STONE);
		// Charge and ownership count only when a ritual asks: a participant's start fails on an empty, foreign pylon.
		ServerPlayer player = practitioner(helper, STAND);
		pylon(helper, pylon).setOwner(java.util.UUID.randomUUID());
		offerAttunement(anchor, player);
		use(anchor, player, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(player) == 20,
				"Someone else's pylon refuses the start, and no Focus is taken");
		pylon(helper, pylon).setOwner(player.getUUID());
		pylon(helper, pylon).setLey(level, 1);
		use(anchor, player, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(player) == 20,
				"A pylon short of a step's charge refuses the start");
		helper.succeed();
	}

	// ---------------------------------------------------------------- completion

	/**
	 * The Adept's Attunement completes after five steps: each pylon gave 2 Ley Charge a step, the offerings it needs were
	 * consumed exactly once (the spare gold ingot stays), one Adept's Wand waits in the anchor keeping the wand's
	 * inscription, the practice is recorded for the chunk, and only a participant can take the wand.
	 */
	@GameTest(maxTicks = 400)
	public void attunementCompletesOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		anchor.offer(player, new ItemStack(Items.STICK));
		helper.assertTrue(count(anchor.offerings(), Items.STICK) == 0, "Nothing can be added while the ritual holds the slots");
		// Five steps of STEP_TICKS, then a little time; checked once, so nothing below repeats.
		helper.runAfterDelay(RitualMachine.STEP_TICKS * 5L + 20, () -> {
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.COMPLETE, "Complete after five steps: " + anchor.run());
			helper.assertTrue(anchor.output().is(JugcraftConcordance.ADEPT_WAND) && anchor.output().getCount() == 1,
					"One Adept's Wand was made: " + anchor.output());
			Inscription kept = anchor.output().get(JugcraftConcordance.INSCRIPTION);
			helper.assertTrue(kept != null && kept.text().equals("touch struck light"), "The wand keeps its inscription");
			List<ItemStack> left = anchor.offerings();
			helper.assertTrue(count(left, Items.GOLD_INGOT) == 1 && count(left, Items.AMETHYST_SHARD) == 0
					&& count(left, Items.GLOWSTONE_DUST) == 0 && count(left, JugcraftConcordance.INITIATE_WAND) == 0,
					"Exactly the offerings were consumed: " + left);
			for (StructurePattern.Part part : circle().channels()) {
				helper.assertTrue(pylon(helper, at(part.offset())).ley() == 64 - 2 * 5, "Each pylon gave 2 Ley Charge a step for five steps");
			}
			String chunk = Long.toString(ChunkPos.containing(helper.absolutePos(ANCHOR)).pack());
			helper.assertTrue(ConcordanceProgress.knowledge(player).progress(CIRCLE_LORE).evidence()
					.containsKey("practice:jugcraft:ritual:" + chunk), "The practice is recorded for this chunk");
			ServerPlayer stranger = practitioner(helper, STAND_2);
			use(anchor, stranger, false);
			helper.assertTrue(!anchor.output().isEmpty(), "Someone who did not take part cannot take the result");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			use(anchor, player, false);
			helper.assertTrue(anchor.output().isEmpty() && anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().completed(),
					"The participant took the wand and the anchor is free");
			helper.assertTrue(player.getInventory().countItem(JugcraftConcordance.ADEPT_WAND) == 1, "The wand is in the participant's inventory");
			use(anchor, player, true);
			helper.assertTrue(count(anchor.offerings(), Items.GOLD_INGOT) == 0 && player.getInventory().countItem(Items.GOLD_INGOT) == 1,
					"Sneaking takes back the spare gold");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- every way it can stop

	/** A channel broken mid-ritual: STRUCTURE at the next step. */
	@GameTest(maxTicks = 200)
	public void aBrokenChannelReleasesTheOfferings(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> helper.setBlock(first(StructurePattern.Role.CHANNEL), Blocks.STONE));
		helper.succeedWhen(() -> offeringsKept(helper, anchor, RitualMachine.Interruption.STRUCTURE));
	}

	/** A Warding Stone lost mid-ritual: CONTAINMENT, with backlash to the participant, offerings still kept. */
	@GameTest(maxTicks = 240)
	public void aLostBoundaryLashesOut(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		helper.runAfterDelay(PAST_SPAWN_GRACE, () -> helper.setBlock(first(StructurePattern.Role.BOUNDARY), Blocks.AIR));
		helper.succeedWhen(() -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.CONTAINMENT);
			helper.assertTrue(player.getHealth() < player.getMaxHealth(), "The backlash hurt the participant: " + player.getHealth());
		});
	}

	/**
	 * Roadmap step 28: a Warding Stone broken mid-ritual by another player is their doing. The backlash is theirs: it
	 * reaches the participant only where the PvP rules let that player harm them, and is credited to them, so breaking a
	 * stranger's boundary is no way round the multiplayer rules.
	 */
	@GameTest(maxTicks = 240)
	public void aBreakerAnswersForTheBacklash(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		ServerPlayer vandal = TestPlayers.mock(helper);
		vandal.setGameMode(GameType.SURVIVAL);
		boolean mayHarm = vandal.canHarmPlayer(player);
		helper.runAfterDelay(PAST_SPAWN_GRACE, () -> vandal.gameMode.destroyBlock(helper.absolutePos(first(StructurePattern.Role.BOUNDARY))));
		helper.succeedWhen(() -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.CONTAINMENT);
			if (mayHarm) {
				helper.assertTrue(player.getHealth() < player.getMaxHealth() && player.getLastHurtByMob() == vandal,
						"Where PvP allows it, the backlash hurt the participant as the vandal's doing: " + player.getHealth());
			} else {
				helper.assertTrue(player.getHealth() == player.getMaxHealth(),
						"Where PvP forbids it, the vandal's backlash spares the participant: " + player.getHealth());
			}
		});
	}

	/**
	 * Roadmap step 29, remote storage with an interrupted ritual: a Clockwork Porter whose source chest stands by the
	 * circle fuels itself from a channel's pylon mid-ritual. The Ley Charge it took is exactly what the pylons lost;
	 * the ritual then stops for want of power, keeps every offering and makes nothing; and neither the Focus it took nor
	 * the Ley Charge the porter took comes back. Nothing is counted twice or returned.
	 */
	@GameTest(maxTicks = 200)
	public void aPorterFuellingFromACircleStopsItCleanly(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		ServerLevel level = helper.getLevel();
		List<BlockPos> channels = new ArrayList<>();
		for (StructurePattern.Part part : circle().parts()) {
			if (part.role() == StructurePattern.Role.CHANNEL) {
				channels.add(at(part.offset()));
			}
		}
		BlockPos chest = new BlockPos(6, 2, 5);
		helper.setBlock(chest, Blocks.CHEST);
		WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		porter.setOwner(player.getUUID());
		porter.setBody(new Body(terms.integrity(), 0L));
		Vec3 stand = helper.absoluteVec(new Vec3(6.5, 2.0, 6.5));
		porter.snapTo(stand.x, stand.y, stand.z, 0.0F, 0.0F);
		level.addFreshEntity(porter);
		porter.setRoute(new ClockworkPorterEntity.Route(helper.absolutePos(chest), helper.absolutePos(new BlockPos(6, 2, 6)),
				level.dimension().identifier().toString()));
		long before = 0;
		for (BlockPos channel : channels) {
			before += pylon(helper, channel).ley();
		}
		porter.think(level, 18000L, level.getGameTime());
		long after = 0;
		for (BlockPos channel : channels) {
			after += pylon(helper, channel).ley();
		}
		long taken = porter.body().energy();
		helper.assertTrue(taken == terms.energy() && before - after == taken,
				"The porter took exactly what the pylons lost: " + taken + " of " + (before - after));
		helper.assertTrue(channels.stream().anyMatch(channel -> pylon(helper, channel).ley() == 0), "and left a channel dry");
		int focusAtStart = ConcordanceProgress.currentFocus(player);
		long started = level.getGameTime();
		helper.succeedWhen(() -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.POWER);
			helper.assertTrue(porter.body().energy() == taken, "The porter's charge is its own; nothing was returned to the pylons");
			// Focus comes back with time, so the stop must not have added a refund on top: no more than time alone gives.
			int regained = ConcordanceProgress.currentFocus(player) - focusAtStart;
			long elapsed = level.getGameTime() - started;
			helper.assertTrue(regained <= elapsed / FocusPool.REGEN_TICKS + 1,
					"The ritual's Focus is not refunded: " + regained + " regained in " + elapsed + " ticks");
		});
	}

	/** A pylon drained mid-ritual: POWER, and the step draws from no pylon. */
	@GameTest(maxTicks = 200)
	public void aDryPylonStopsTheRitual(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> pylon(helper, first(StructurePattern.Role.CHANNEL)).setLey(helper.getLevel(), 0));
		helper.succeedWhen(() -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.POWER);
			for (StructurePattern.Part part : circle().channels()) {
				long ley = pylon(helper, at(part.offset())).ley();
				helper.assertTrue(ley == 0 || ley == 62, "The failed step drew from no pylon (one step's 2 were drawn): " + ley);
			}
		});
	}

	/** The participant walks away: PARTICIPANTS. */
	@GameTest(maxTicks = 200)
	public void aParticipantWhoLeavesStopsTheRitual(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
			BlockPos far = helper.absolutePos(ANCHOR).offset(24, 0, 0);
			player.snapTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.succeedWhen(() -> offeringsKept(helper, anchor, RitualMachine.Interruption.PARTICIPANTS));
	}

	/** The leader sneaks and uses the anchor: CANCELLED at once. Focus is not returned. */
	@GameTest(maxTicks = 100)
	public void theLeaderCanCallItOff(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
			use(anchor, player, true);
			offeringsKept(helper, anchor, RitualMachine.Interruption.CANCELLED);
			helper.assertTrue(ConcordanceProgress.currentFocus(player) < 20, "The Focus spent stays spent");
			helper.succeed();
		});
	}

	/** The anchor broken mid-ritual: REMOVED, and every offering drops once where it stood. */
	@GameTest(maxTicks = 100)
	public void aBrokenAnchorDropsItsOfferingsOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
			helper.destroyBlock(ANCHOR);
			List<ItemStack> dropped = new ArrayList<>();
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(ANCHOR)).inflate(3.0))) {
				dropped.add(item.getItem());
			}
			helper.assertTrue(count(dropped, JugcraftConcordance.INITIATE_WAND) == 1 && count(dropped, Items.AMETHYST_SHARD) == 4
					&& count(dropped, Items.GOLD_INGOT) == 3 && count(dropped, Items.GLOWSTONE_DUST) == 4
					&& count(dropped, JugcraftConcordance.ADEPT_WAND) == 0, "Each offering dropped once, nothing made: " + dropped);
			helper.succeed();
		});
	}

	/** Saved mid-ritual and loaded again (an unloaded chunk, a restart): LAPSED on its next tick. */
	@GameTest(maxTicks = 100)
	public void aRitualSavedMidRunLapses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
			CompoundTag saved = anchor.saveWithoutMetadata(level.registryAccess());
			anchor.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.runAfterDelay(2, () -> {
				offeringsKept(helper, anchor, RitualMachine.Interruption.LAPSED);
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- two practitioners, in the dark

	/**
	 * The Lumen Vigil: the leader starts it and it gathers; the second participant joins and it channels; after three
	 * steps both carry 8 points of absorption and the pig nearby glows, through the shared effect executor.
	 */
	@GameTest(maxTicks = 300)
	public void theVigilGathersTwoAndShieldsThem(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		enclose(helper);
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 2));
		helper.runAfterDelay(5, () -> {
			ServerPlayer leader = practitioner(helper, STAND);
			ServerPlayer friend = practitioner(helper, STAND_2);
			anchor.offer(leader, new ItemStack(Items.GLOW_BERRIES, 4));
			use(anchor, leader, false);
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.GATHERING, "The vigil gathers: " + anchor.run());
			use(anchor, friend, false);
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING && anchor.run().joined().size() == 2,
					"The second participant joins and it channels");
			helper.assertTrue(ConcordanceProgress.currentFocus(friend) == 20 - 4, "The joiner paid four Focus");
			helper.succeedWhen(() -> {
				helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().completed(), "Waiting: " + anchor.run());
				helper.assertTrue(leader.getAbsorptionAmount() >= 8.0F && friend.getAbsorptionAmount() >= 8.0F,
						"Both participants are shielded: " + leader.getAbsorptionAmount() + ", " + friend.getAbsorptionAmount());
				helper.assertTrue(pig.hasEffect(MobEffects.GLOWING), "The pig nearby glows");
				helper.assertTrue(count(anchor.offerings(), Items.GLOW_BERRIES) == 0, "The berries were used once");
			});
		});
	}

	/** Too bright: the vigil will not start under a glowstone, and one already running stops when light arrives. */
	@GameTest(maxTicks = 200)
	public void theVigilNeedsTheDark(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		enclose(helper);
		BlockPos lamp = new BlockPos(5, 3, 4);
		helper.setBlock(lamp, Blocks.GLOWSTONE);
		helper.runAfterDelay(5, () -> {
			ServerPlayer leader = practitioner(helper, STAND);
			anchor.offer(leader, new ItemStack(Items.GLOW_BERRIES, 4));
			use(anchor, leader, false);
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(leader) == 20,
					"Under a glowstone the vigil does not start");
			helper.setBlock(lamp, Blocks.STONE);
			helper.runAfterDelay(5, () -> {
				ServerPlayer friend = practitioner(helper, STAND_2);
				use(anchor, leader, false);
				use(anchor, friend, false);
				helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING, "In the dark it starts: " + anchor.run());
				helper.setBlock(lamp, Blocks.GLOWSTONE);
				helper.succeedWhen(() -> {
					helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().last() == RitualMachine.Interruption.CONDITIONS,
							"Light arriving stops it: " + anchor.run());
					helper.assertTrue(count(anchor.offerings(), Items.GLOW_BERRIES) == 4, "The berries are kept");
				});
			});
		});
	}

	// ---------------------------------------------------------------- power and refusals

	/**
	 * A pylon fills from a Kindled Lantern through radiance_to_ley (15 Radiance make 10 Ley Charge; a third is lost) and
	 * from electricity (1000 JE make 1), never gives either back, and broken keeps its charge on the item.
	 */
	@GameTest(maxTicks = 60)
	public void aPylonFillsFromLightAndElectricity(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
		helper.setBlock(at, JugcraftConcordance.LEY_PYLON);
		LeyPylonBlockEntity pylon = pylon(helper, at);
		ItemStack lantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		KindledLanternItem.set(lantern, KindledLanternItem.CAPACITY, ConcordanceProgress.now(level), false);
		LeyPylonBlockEntity.Pour pour = pylon.pour(level, lantern, ConcordanceData.rules().conversions().get(LeyPylonBlockEntity.CONVERSION));
		helper.assertTrue(pour.refusal() == null && pour.radiance() == 15 && pour.ley() == 10 && pylon.ley() == 10
				&& KindledLanternItem.remaining(lantern, ConcordanceProgress.now(level)) == KindledLanternItem.CAPACITY - 15,
				"A pour turns 15 Radiance into 10 Ley Charge: " + pour);
		EnergyStorage energy = EnergyStorage.SIDED.find(level, helper.absolutePos(at), null);
		helper.assertTrue(energy != null && energy.supportsInsertion() && !energy.supportsExtraction(), "It takes electricity and gives none back");
		long accepted;
		try (Transaction transaction = Transaction.openOuter()) {
			accepted = energy.insert(5000, transaction);
			transaction.commit();
		}
		helper.assertTrue(accepted == LeyPylonBlockEntity.JE_RATE, "It accepts at most " + LeyPylonBlockEntity.JE_RATE + " JE a call: " + accepted);
		pylon.energy.setAmount(LeyPylonBlockEntity.JE_PER_LEY);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(pylon.ley() == 11 && pylon.energy.getAmount() == 0, "1000 JE became 1 Ley Charge: " + pylon.ley());
			List<ItemStack> drops = Block.getDrops(helper.getBlockState(at), level, helper.absolutePos(at), pylon);
			helper.assertTrue(drops.size() == 1 && drops.get(0).getOrDefault(JugcraftConcordance.LEY_CHARGE, 0) == 11,
					"Broken, it keeps its charge on the item: " + drops + " " + (drops.isEmpty() ? "" : drops.get(0).getComponents()));
			helper.succeed();
		});
	}

	/**
	 * Refusals take nothing: someone who has not learned the ritual, offerings that are short, and someone standing too
	 * far away cannot start it, and their Focus and the offerings stay as they were.
	 */
	@GameTest(maxTicks = 40)
	public void refusalsTakeNothing(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer novice = TestPlayers.mock(helper);
		novice.setGameMode(GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(STAND);
		novice.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
		ConcordanceProgress.setFocus(novice, 20);
		offerAttunement(anchor, novice);
		use(anchor, novice, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(novice) == 20,
				"Without Circle Lore it does not start");
		ServerPlayer practitioner = practitioner(helper, STAND_2);
		use(anchor, practitioner, true);
		helper.assertTrue(practitioner.getInventory().countItem(Items.GLOWSTONE_DUST) == 4, "Anyone may take idle offerings back");
		anchor.offer(practitioner, inscribedWand());
		anchor.offer(practitioner, new ItemStack(Items.AMETHYST_SHARD, 4));
		anchor.offer(practitioner, new ItemStack(Items.GOLD_INGOT, 2));
		anchor.offer(practitioner, new ItemStack(Items.GLOWSTONE_DUST, 3));
		use(anchor, practitioner, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(practitioner) == 20
				&& count(anchor.offerings(), Items.GLOWSTONE_DUST) == 3, "One glowstone dust short, it does not start");
		anchor.offer(practitioner, new ItemStack(Items.GLOWSTONE_DUST, 1));
		BlockPos far = helper.absolutePos(ANCHOR).offset(12, 0, 0);
		practitioner.snapTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 0.0F, 0.0F);
		use(anchor, practitioner, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE, "Too far from the circle, it does not start");
		helper.succeed();
	}

	// ---------------------------------------------------------------- what it shows (roadmap step 27)

	/**
	 * A clean working shows what happened and nothing else: one sign of preparation as it starts, the Ley Charge of each
	 * of its five steps travelling from each of the four channels to the anchor (twenty flows, one for each draw), one
	 * success when it completes, and no warning.
	 */
	@GameTest(maxTicks = 400)
	public void aWorkingShowsWhatHappened(GameTestHelper helper) {
		SignWatch signs = new SignWatch(helper);
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		helper.runAfterDelay(RitualMachine.STEP_TICKS * 5L + 20, () -> {
			signs.close();
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.COMPLETE, "Complete after five steps: " + anchor.run());
			helper.assertValueEqual(signs.at(Sign.GATHER, ANCHOR), 1L, "one sign of preparation, at the anchor");
			for (StructurePattern.Part part : circle().channels()) {
				helper.assertValueEqual(signs.flowed(Sign.FLOW, at(part.offset()), ANCHOR), 5L, "five draws from the channel at " + part.offset());
			}
			helper.assertValueEqual((long) signs.of(Sign.FLOW).size(), 20L, "no flow that was not a draw");
			helper.assertValueEqual(signs.at(Sign.DONE, ANCHOR.above()), 1L, "one success, when it completed");
			helper.assertTrue(signs.of(Sign.WANT).isEmpty() && signs.of(Sign.PERIL).isEmpty(), "no warning: " + signs.all());
			helper.succeed();
		});
	}

	/** A pylon drained mid-ritual: a shortage shows at that pylon and at the anchor, and nothing claims success. */
	@GameTest(maxTicks = 200)
	public void aDryPylonShowsWhereItFellShort(GameTestHelper helper) {
		SignWatch signs = new SignWatch(helper);
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		BlockPos drained = first(StructurePattern.Role.CHANNEL);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> pylon(helper, drained).setLey(helper.getLevel(), 0));
		helper.succeedWhen(() -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.POWER);
			helper.assertTrue(signs.at(Sign.WANT, drained) == 1 && signs.at(Sign.WANT, ANCHOR) == 1,
					"A shortage at the dry pylon and at the anchor: " + signs.all());
			helper.assertTrue(signs.of(Sign.DONE).isEmpty() && signs.of(Sign.PERIL).isEmpty(), "No success and no danger: " + signs.all());
			signs.close();
		});
	}

	/** A Warding Stone lost mid-ritual: danger at the anchor (with the backlash's own marks), never a mere shortage. */
	@GameTest(maxTicks = 240)
	public void aLostBoundaryShowsDanger(GameTestHelper helper) {
		SignWatch signs = new SignWatch(helper);
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startAttunement(helper, anchor);
		helper.runAfterDelay(PAST_SPAWN_GRACE, () -> helper.setBlock(first(StructurePattern.Role.BOUNDARY), Blocks.AIR));
		helper.succeedWhen(() -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.CONTAINMENT);
			helper.assertTrue(signs.at(Sign.PERIL, ANCHOR) == 1, "Danger at the anchor: " + signs.all());
			helper.assertTrue(!signs.of(Sign.HARM).isEmpty(), "The backlash's harm shows on the participant: " + signs.all());
			helper.assertTrue(signs.of(Sign.DONE).isEmpty() && signs.at(Sign.WANT, ANCHOR) == 0, "No success and no shortage: " + signs.all());
			signs.close();
		});
	}

	// ---------------------------------------------------------------- readiness (Concordance Chunk C, C-03)

	private static final Text.Ref CIRCLE = new Text.Ref("structure", "jugcraft:lesser_circle");

	private static String key(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
	}

	private static Object[] args(Component component) {
		return component.getContents() instanceof TranslatableContents translatable ? translatable.getArgs() : new Object[0];
	}

	/** Every translation key in a component, its arguments and its siblings. */
	private static void keys(Component component, List<String> out) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			out.add(translatable.getKey());
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof Component inner) {
					keys(inner, out);
				}
			}
		}
		component.getSiblings().forEach(sibling -> keys(sibling, out));
	}

	private static List<String> keys(List<EncyclopediaPayload.Row> rows) {
		List<String> out = new ArrayList<>();
		rows.forEach(row -> keys(row.text(), out));
		return out;
	}

	private static EncyclopediaPayload.Row row(List<EncyclopediaPayload.Row> rows, String key) {
		return rows.stream().filter(row -> key(row.text()).equals(key)).findFirst().orElse(null);
	}

	/** The Inspect page {@code reader} is given looking at the anchor; empty if none. */
	private static List<EncyclopediaPayload.Row> inspect(GameTestHelper helper, ServerPlayer reader) {
		return Encyclopedia.build(reader, new EncyclopediaRequestPayload(1, Optional.of(helper.absolutePos(ANCHOR))), PageWriter::of)
				.pages().stream().filter(page -> page.id().equals(InspectPage.PAGE)).findFirst()
				.map(EncyclopediaPayload.Page::rows).orElse(List.of());
	}

	/** A channel in readiness's words (Readiness.Channel.words): where its pylon stands, and where that is from the anchor. */
	private static Text pylonAt(GameTestHelper helper, StructurePattern.Part part) {
		BlockPos at = helper.absolutePos(at(part.offset()));
		StructurePattern.Offset offset = part.offset();
		return Text.of("ritual.pylon_at", at.getX() + ", " + at.getY() + ", " + at.getZ(), offset.x() + ", " + offset.y() + ", " + offset.z());
	}

	private static List<Diagnostic> diagnostics(Readiness readiness) {
		return readiness.lines().stream().map(Readiness.Line::diagnostic).toList();
	}

	private static List<Boolean> blocking(Readiness readiness) {
		return readiness.lines().stream().map(Readiness.Line::blocks).toList();
	}

	private static void snapTo(ServerPlayer player, BlockPos absolute) {
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
	}

	private static List<BlockPos> pylons() {
		return circle().channels().stream().map(part -> at(part.offset())).toList();
	}

	/** With Jade loaded, the anchor's snapshot names no rite (C-P38). */
	private static void jadeNamesNoRite(GameTestHelper helper, CircleAnchorBlockEntity anchor) {
		if (!FabricLoader.getInstance().isModLoaded("jade")) {
			return;
		}
		CompoundTag shown = ConcordanceDataProvider.snapshot(anchor, helper.getLevel());
		for (String name : shown.keySet()) {
			helper.assertTrue(!name.contains("ritual") && !shown.getStringOr(name, "").contains("adept_attunement"),
					"Jade's anchor names no rite: " + shown);
		}
	}

	/**
	 * An idle circle says everything a rite still needs at once, for its reader as the leader, in the shared vocabulary:
	 * offerings short, a pylon short of the whole run (a warning only) or of a step, a pylon not lent, the reader too far,
	 * short of Focus, too much light, the shape's faults. The Inspect page shows the same lines, names the rite and states
	 * its stages, and a fault says where it is from the anchor; nothing is marked. Mended but for one pylon short of the
	 * whole run, the rite begins (C-P10) and breaks off at the step the warning named.
	 */
	@GameTest(maxTicks = 300)
	public void anIdleCircleSaysEverythingARiteStillNeeds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		List<StructurePattern.Part> channels = circle().channels();
		BlockPos stone = first(StructurePattern.Role.BOUNDARY);
		pylon(helper, at(channels.get(0).offset())).setLey(level, 1);
		pylon(helper, at(channels.get(1).offset())).setLey(level, 9);
		pylon(helper, at(channels.get(2).offset())).setOwner(UUID.randomUUID());
		helper.setBlock(stone, Blocks.AIR);
		// A lamp beside the clearance, not a part: too bright above the anchor for the vigil.
		helper.setBlock(new BlockPos(4, 3, 3), Blocks.GLOWSTONE);
		helper.runAfterDelay(5, () -> { // the lamp's light has spread, as theVigilNeedsTheDark waits
			SignWatch signs = new SignWatch(helper);
			ServerPlayer player = practitioner(helper, STAND);
			ConcordanceProgress.setFocus(player, 3);
			snapTo(player, helper.absolutePos(ANCHOR).offset(10, 0, 0));
			// A. The Lumen Vigil, half offered.
			anchor.offer(player, new ItemStack(Items.GLOW_BERRIES, 2));
			int light = level.getMaxLocalRawBrightness(helper.absolutePos(ANCHOR).above());
			Readiness vigil = anchor.readiness(level, player);
			helper.assertTrue(light > 7 && vigil != null && vigil.rite() != null && vigil.rite().id().equals("jugcraft:lumen_vigil")
					&& !vigil.ready(), "The berries answer the vigil, which is not ready: " + vigil + " at light " + light);
			helper.assertValueEqual(diagnostics(vigil), List.of(
					Diagnostic.of(Code.MISSING_INGREDIENT, 2, new Text.Ref("item", "minecraft:glow_berries")),
					Diagnostic.of(Code.UNAVAILABLE_POWER, pylonAt(helper, channels.get(0)), Text.of("ritual.ley", 1), Text.of("ritual.ley_run", 3, 2)),
					Diagnostic.of(Code.NOT_LENT, pylonAt(helper, channels.get(2))),
					Diagnostic.of(Code.TOO_FAR, 7, 3),
					Diagnostic.of(Code.NEEDS_FOCUS, 3, 4),
					Diagnostic.of(Code.TOO_BRIGHT, light, 7),
					Diagnostic.of(Code.INCOMPLETE_PATTERN, CIRCLE, 1)), "every shortfall of the vigil, in readiness's order");
			helper.assertValueEqual(blocking(vigil), List.of(true, false, true, true, true, true, true), "only the whole run's charge warns");
			// B. The Adept's Attunement, two glowstone dust short.
			use(anchor, player, true);
			for (ItemStack stack : List.of(inscribedWand(), new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(Items.GOLD_INGOT, 2),
					new ItemStack(Items.GLOWSTONE_DUST, 2))) {
				anchor.offer(player, stack);
			}
			List<Diagnostic> wanting = List.of(
					Diagnostic.of(Code.MISSING_INGREDIENT, 2, new Text.Ref("item", "minecraft:glowstone_dust")),
					Diagnostic.of(Code.UNAVAILABLE_POWER, pylonAt(helper, channels.get(0)), Text.of("ritual.ley", 1), Text.of("ritual.ley_step", 2)),
					Diagnostic.of(Code.UNAVAILABLE_POWER, pylonAt(helper, channels.get(1)), Text.of("ritual.ley", 9), Text.of("ritual.ley_run", 10, 5)),
					Diagnostic.of(Code.NOT_LENT, pylonAt(helper, channels.get(2))),
					Diagnostic.of(Code.TOO_FAR, 7, 3),
					Diagnostic.of(Code.NEEDS_FOCUS, 3, 6),
					Diagnostic.of(Code.INCOMPLETE_PATTERN, CIRCLE, 1));
			Readiness attunement = anchor.readiness(level, player);
			helper.assertTrue(attunement != null, "the attunement is the closest rite");
			helper.assertValueEqual(diagnostics(attunement), wanting, "every shortfall of the attunement");
			helper.assertValueEqual(blocking(attunement), List.of(true, true, false, true, true, true, true), "a run's shortfall only warns");
			List<EncyclopediaPayload.Row> rows = inspect(helper, player);
			List<String> shown = rows.stream().map(row -> row.text().getString()).toList();
			for (Diagnostic line : wanting) {
				helper.assertTrue(shown.contains(ComposeText.show(line.text()).getString()), "The Inspect page says " + line + ": " + shown);
			}
			EncyclopediaPayload.Row heading = row(rows, "encyclopedia.jugcraft.inspect.readiness");
			helper.assertTrue(heading != null && key((Component) args(heading.text())[0]).equals("ritual.jugcraft.adept_attunement")
					&& keys(rows).contains("compose.jugcraft.ritual.stage.backlash"), "It names the rite and states its stages: " + shown);
			BlockPos absolute = helper.absolutePos(stone);
			EncyclopediaPayload.Row missing = row(rows, "message.jugcraft.concordance.circle.fault.missing");
			Component where = missing == null ? Component.empty() : (Component) args(missing.text())[1];
			helper.assertTrue(key(where).equals("message.jugcraft.concordance.circle.where")
					&& List.of(args(where)).equals(List.of(absolute.getX() + ", " + absolute.getY() + ", " + absolute.getZ(), "3, 0, 0")),
					"A fault says where it is, and where that is from the anchor: " + where);
			signs.close();
			helper.assertTrue(signs.all().isEmpty(), "Readiness and the Inspect page show no sign: " + signs.all());
			// C. Everything mended but the second pylon's charge for the whole run: a warning, and the start agrees (C-P10).
			ConcordanceProgress.setFocus(player, 20);
			snapTo(player, helper.absolutePos(STAND));
			helper.setBlock(stone, JugcraftConcordance.WARDING_STONE);
			Rituals.changed(level, helper.absolutePos(stone));
			pylon(helper, at(channels.get(0).offset())).setLey(level, 64);
			pylon(helper, at(channels.get(2).offset())).setOwner(player.getUUID());
			anchor.offer(player, new ItemStack(Items.GLOWSTONE_DUST, 2));
			Readiness ready = anchor.readiness(level, player);
			helper.assertTrue(ready != null && ready.ready() && blocking(ready).equals(List.of(false)), "only a warning is left: " + ready);
			helper.assertValueEqual(diagnostics(ready), List.of(Diagnostic.of(Code.UNAVAILABLE_POWER, pylonAt(helper, channels.get(1)),
					Text.of("ritual.ley", 9), Text.of("ritual.ley_run", 10, 5))), "the run's warning");
			use(anchor, player, false);
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING && ConcordanceProgress.currentFocus(player) == 14,
					"A run-level shortfall does not refuse the start: " + anchor.run());
			BlockPos dry = at(channels.get(1).offset());
			helper.succeedWhen(() -> {
				helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().last() == RitualMachine.Interruption.POWER,
						"It breaks off for power: " + anchor.run());
				helper.assertValueEqual(anchor.ending(), new Readiness.Ending(RitualMachine.Interruption.POWER, new Readiness.Spent(6, 32, 4, 1, 8)),
						"at step 5, as the warning said, having spent 6 Focus and 32 Ley Charge");
				List<ItemStack> held = anchor.offerings();
				helper.assertTrue(pylon(helper, dry).ley() == 1 && count(held, JugcraftConcordance.INITIATE_WAND) == 1
						&& count(held, Items.AMETHYST_SHARD) == 4 && count(held, Items.GOLD_INGOT) == 2 && count(held, Items.GLOWSTONE_DUST) == 4,
						"The dry pylon kept 1; every offering is kept: " + held);
			});
		});
	}

	/**
	 * A refused start marks every part that refused it where it stands, with the sign its interruption would show: danger
	 * for a broken boundary (and every other fault the check found), then, the boundary mended, a shortage at each channel
	 * missing, short or not lent. Nothing else is marked, and nothing is taken.
	 */
	@GameTest(maxTicks = 40)
	public void aRefusedStartMarksEveryFault(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		List<BlockPos> pylons = pylons();
		BlockPos stone = first(StructurePattern.Role.BOUNDARY);
		helper.setBlock(pylons.get(0), Blocks.AIR);
		pylon(helper, pylons.get(1)).setLey(level, 1);
		pylon(helper, pylons.get(2)).setOwner(UUID.randomUUID());
		helper.setBlock(stone, Blocks.AIR);
		ServerPlayer player = practitioner(helper, STAND);
		offerAttunement(anchor, player);
		SignWatch signs = new SignWatch(helper);
		use(anchor, player, false); // containment broken: danger at every part that refused it
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(player) == 20
				&& count(anchor.offerings(), Items.GLOWSTONE_DUST) == 4, "Refused, it took nothing");
		for (BlockPos part : List.of(pylons.get(0), pylons.get(1), pylons.get(2), stone)) {
			helper.assertValueEqual(signs.at(Sign.PERIL, part), 1L, "danger where it refused: " + part);
		}
		helper.assertTrue(signs.of(Sign.PERIL).size() == 4 && signs.of(Sign.WANT).isEmpty() && signs.of(Sign.GATHER).isEmpty()
				&& signs.at(Sign.PERIL, ANCHOR) == 0, "Only the faulted parts are marked: " + signs.all());
		helper.setBlock(stone, JugcraftConcordance.WARDING_STONE);
		use(anchor, player, false); // the channels alone: a shortage at each
		for (BlockPos part : pylons.subList(0, 3)) {
			helper.assertValueEqual(signs.at(Sign.WANT, part), 1L, "a shortage where it refused: " + part);
		}
		helper.assertTrue(signs.of(Sign.WANT).size() == 3 && signs.of(Sign.PERIL).size() == 4 && anchor.phase() == RitualMachine.Phase.IDLE,
				"Nothing else is marked: " + signs.all());
		signs.close();
		helper.succeed();
	}

	/**
	 * A reading in chat (an empty-hand use with nothing offered, the command) marks each faulted part, at most once a
	 * second a player, and with offerings also a pylon short of a step for its reader; the Inspect page, the shared status
	 * lines and readiness mark nothing.
	 */
	@GameTest(maxTicks = 40)
	public void aStatusReadingMarksFaultsButTheInspectPageDoesNot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		List<BlockPos> pylons = pylons();
		BlockPos clearance = first(StructurePattern.Role.CLEARANCE);
		helper.setBlock(pylons.get(0), Blocks.AIR);
		helper.setBlock(clearance, Blocks.DIRT);
		Rituals.changed(level, helper.absolutePos(clearance));
		ServerPlayer player = practitioner(helper, STAND);
		SignWatch signs = new SignWatch(helper);
		helper.assertTrue(anchor.readiness(level, player) == null, "Nothing offered: no rite to speak of");
		use(anchor, player, false); // nothing offered: the reading in chat
		helper.assertTrue(signs.at(Sign.WANT, pylons.get(0)) == 1 && signs.at(Sign.WANT, clearance) == 1 && signs.of(Sign.WANT).size() == 2
				&& signs.of(Sign.PERIL).isEmpty() && signs.at(Sign.WANT, ANCHOR) == 0, "The reading marks each faulted part: " + signs.all());
		anchor.status(player, level);
		helper.assertTrue(signs.all().size() == 2, "Once a second a player: " + signs.all());
		helper.assertFalse(inspect(helper, player).isEmpty(), "The Inspect page reads the anchor");
		anchor.status(level, line -> {
		});
		anchor.readiness(level, player);
		helper.assertTrue(signs.all().size() == 2, "The Inspect page, the shared status and readiness mark nothing: " + signs.all());
		offerAttunement(anchor, player);
		pylon(helper, pylons.get(1)).setLey(level, 1);
		RateGate.forget(player.getUUID());
		anchor.status(player, level); // the command's path
		helper.assertTrue(signs.at(Sign.WANT, pylons.get(1)) == 1 && signs.of(Sign.WANT).size() == 5,
				"The shape again, and the pylon short of a step for this reader: " + signs.all());
		inspect(helper, player);
		helper.assertTrue(signs.all().size() == 5, "Still nothing from the Inspect page: " + signs.all());
		signs.close();
		helper.succeed();
	}

	/**
	 * While a rite runs, what it has spent (six Focus, then 2 Ley Charge from each of four pylons a step) is shown apart
	 * from what it still holds (its plan, the spare gold not), on the Inspect page and in Jade, and a pylon gone since the
	 * last step is a blocking line (the unit review); called off, its ending is
	 * kept with what it spent, and the one line for every interruption says why, where the offerings are and what was
	 * lost. A new attempt clears the last; broken, the anchor drops everything once.
	 */
	@GameTest(maxTicks = 200)
	public void spentAndReservedAreShownApart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		List<Readiness.Held> held = List.of(new Readiness.Held("jugcraft:initiate_wand", 1), new Readiness.Held("minecraft:amethyst_shard", 4),
				new Readiness.Held("minecraft:gold_ingot", 2), new Readiness.Held("minecraft:glowstone_dust", 4));
		helper.assertValueEqual(anchor.spent(), new Readiness.Spent(6, 0, 0, 5, 40), "begun: six Focus, no Ley Charge yet");
		helper.assertValueEqual(Readiness.holding(anchor.run().plan()), held, "held: the plan, the spare gold not");
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
			Readiness.Spent oneStep = new Readiness.Spent(6, 8, 1, 4, 32);
			long drawn = 0;
			for (BlockPos pylon : pylons()) {
				drawn += 64 - pylon(helper, pylon).ley();
			}
			helper.assertTrue(oneStep.equals(anchor.spent()) && drawn == oneStep.ley(), "one step spent: " + anchor.spent() + ", " + drawn);
			Readiness running = anchor.readiness(level, player);
			helper.assertTrue(running != null && oneStep.equals(running.spent()) && held.equals(running.held()) && running.lines().isEmpty(),
					"running: spent apart from held, nothing short: " + running);
			// A pylon gone or unloaded since the last step: the shape shown is still that step's, so readiness says it,
			// blocking, as the next step will (the unit review).
			var rite = ConcordanceData.rules().ritual(anchor.run().ritual());
			List<Readiness.Channel> oneGone = new ArrayList<>();
			for (StructurePattern.Part part : ConcordanceData.rules().structure(rite.structure()).channels()) {
				oneGone.add(new Readiness.Channel(part.offset(), "", !oneGone.isEmpty(), 64, true));
			}
			Readiness gone = Readiness.running(rite, anchor.run(), oneGone, 0L, 0);
			helper.assertTrue(gone.lines().equals(List.of(new Readiness.Line(Diagnostic.of(Code.INCOMPLETE_PATTERN, CIRCLE, 1), true, null))),
					"A pylon gone since the last step is said, and blocks: " + gone.lines());
			List<EncyclopediaPayload.Row> rows = inspect(helper, player);
			EncyclopediaPayload.Row heading = row(rows, "encyclopedia.jugcraft.inspect.under_way");
			EncyclopediaPayload.Row spent = row(rows, "compose.jugcraft.ritual.spent");
			EncyclopediaPayload.Row holding = row(rows, "encyclopedia.jugcraft.inspect.anchor.held");
			helper.assertTrue(heading != null && key((Component) args(heading.text())[0]).equals("ritual.jugcraft.adept_attunement")
					&& spent != null && List.of(args(spent.text())).equals(List.of(6, 8, 1, 5)) && holding != null
					&& holding.icons().equals(List.of(JugcraftConcordance.INITIATE_WAND, Items.AMETHYST_SHARD, Items.GOLD_INGOT, Items.GLOWSTONE_DUST)),
					"The Inspect page shows spent apart from held: " + rows.stream().map(row -> row.text().getString()).toList());
			if (FabricLoader.getInstance().isModLoaded("jade")) {
				CompoundTag jade = ConcordanceDataProvider.snapshot(anchor, level);
				helper.assertTrue(jade.getIntOr("needed", -1) == 1 && jade.getIntOr("spent_focus", -1) == 6 && jade.getIntOr("spent_ley", -1) == 8
						&& jade.getIntOr("reserved", -1) == 11 && jade.getIntOr("offerings", -1) == 4
						&& jade.getStringOr("offering2", "").equals("minecraft:gold_ingot|3"), "Jade: " + jade);
			}
			use(anchor, player, true); // the leader calls it off
			helper.assertValueEqual(anchor.ending(), new Readiness.Ending(RitualMachine.Interruption.CANCELLED, oneStep), "how it ended");
			helper.assertTrue(anchor.spent() == null && keys(inspect(helper, player)).contains("encyclopedia.jugcraft.inspect.last"),
					"Idle: nothing spent now; the last attempt is shown");
			if (FabricLoader.getInstance().isModLoaded("jade")) {
				helper.assertFalse(ConcordanceDataProvider.snapshot(anchor, level).keySet().contains("spent_focus"), "Jade: nothing spent while idle");
			}
			Readiness again = anchor.readiness(level, player);
			helper.assertTrue(again != null && again.ready(), "The kept offerings stand ready again: " + again);
			for (RitualMachine.Interruption reason : RitualMachine.Interruption.values()) {
				Component line = CircleAnchorBlockEntity.endingLine(Component.literal("x"), new Readiness.Ending(reason, oneStep));
				Object[] parts = args(line);
				helper.assertTrue(key(line).equals("message.jugcraft.concordance.circle.ended.spent") && parts.length == 4
						&& key((Component) parts[0]).equals("message.jugcraft.concordance.circle.interrupted." + reason.id)
						&& key((Component) parts[1]).equals(reason == RitualMachine.Interruption.REMOVED
								? "message.jugcraft.concordance.circle.release.dropped" : "message.jugcraft.concordance.circle.release.kept")
						&& parts[2].equals(6) && parts[3].equals(8), "the one line for " + reason + ": " + line);
			}
			Component bare = CircleAnchorBlockEntity.endingLine(Component.literal("x"), new Readiness.Ending(RitualMachine.Interruption.FORGOTTEN, null));
			helper.assertTrue(key(bare).equals("message.jugcraft.concordance.circle.ended") && args(bare).length == 2, "no loss when the rite is gone");
			use(anchor, player, false);
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING && anchor.ending() == null, "A new attempt clears the last");
			helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
				helper.destroyBlock(ANCHOR);
				helper.assertValueEqual(anchor.ending(), new Readiness.Ending(RitualMachine.Interruption.REMOVED, oneStep), "broken");
				List<ItemStack> dropped = new ArrayList<>();
				for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(ANCHOR)).inflate(3.0))) {
					dropped.add(item.getItem());
				}
				helper.assertTrue(count(dropped, JugcraftConcordance.INITIATE_WAND) == 1 && count(dropped, Items.AMETHYST_SHARD) == 4
						&& count(dropped, Items.GOLD_INGOT) == 3 && count(dropped, Items.GLOWSTONE_DUST) == 4, "dropped once: " + dropped);
				helper.succeed();
			});
		});
	}

	/**
	 * A dry pylon breaks the attunement off: the one line says why (power), that the offerings are kept and what the
	 * attempt lost; the ending is kept in memory for the Inspect page, never saved, and forgotten on a load.
	 */
	@GameTest(maxTicks = 200)
	public void theInterruptionReasonIsKept(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startAttunement(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> pylon(helper, first(StructurePattern.Role.CHANNEL)).setLey(level, 0));
		helper.runAfterDelay(AFTER_FIRST_STEP + RitualMachine.STEP_TICKS + 10, () -> {
			offeringsKept(helper, anchor, RitualMachine.Interruption.POWER);
			helper.assertValueEqual(anchor.ending(), new Readiness.Ending(RitualMachine.Interruption.POWER, new Readiness.Spent(6, 8, 1, 4, 32)),
					"how it ended");
			Component line = anchor.endingLine();
			Object[] parts = line == null ? new Object[0] : args(line);
			helper.assertTrue(line != null && key(line).equals("message.jugcraft.concordance.circle.ended.spent") && parts.length == 4
					&& key((Component) parts[0]).equals("message.jugcraft.concordance.circle.interrupted.power")
					&& key((Component) args((Component) parts[0])[0]).equals("ritual.jugcraft.adept_attunement")
					&& key((Component) parts[1]).equals("message.jugcraft.concordance.circle.release.kept") && parts[2].equals(6) && parts[3].equals(8),
					"One line: the reason, the offerings kept, what was lost: " + line);
			CompoundTag saved = anchor.saveWithoutMetadata(level.registryAccess());
			helper.assertTrue(saved.keySet().stream().noneMatch(name -> name.contains("ending") || name.contains("spent")), "never saved: " + saved.keySet());
			helper.assertTrue(keys(inspect(helper, player)).contains("encyclopedia.jugcraft.inspect.last"), "the Inspect page tells it");
			anchor.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(anchor.ending() == null, "forgotten on a load");
			helper.succeed();
		});
	}

	/**
	 * Offerings that answer only a rite the reader has not learned: readiness says nothing and names nothing, the Inspect
	 * page says only that it is unlearned (no name, no stage table), and Jade names no rite. Once a practitioner begins it,
	 * the novice sees it run, unnamed, with what it has spent; the practitioner sees it named, with its table.
	 */
	@GameTest(maxTicks = 100)
	public void unknownRitesStayUnnamed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer novice = TestPlayers.mock(helper);
		novice.setGameMode(GameType.SURVIVAL);
		snapTo(novice, helper.absolutePos(STAND));
		ConcordanceProgress.grant(novice, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(novice, 20);
		RateGate.forget(novice.getUUID());
		offerAttunement(anchor, novice);
		helper.assertValueEqual(anchor.readiness(level, novice), Readiness.UNKNOWN, "an unknown rite: nothing said, nothing named");
		List<EncyclopediaPayload.Row> before = inspect(helper, novice);
		EncyclopediaPayload.Row unknown = row(before, "encyclopedia.jugcraft.inspect.rituals_unknown");
		helper.assertTrue(keys(before).contains("encyclopedia.jugcraft.inspect.readiness.unknown")
				&& keys(before).stream().noneMatch(name -> name.startsWith("ritual.jugcraft.") || name.startsWith("compose.jugcraft.ritual.stage."))
				&& unknown != null && args(unknown.text())[0].equals(ConcordanceData.rules().ritualsAt(CircleAnchorBlockEntity.ANCHOR).size()),
				"Unnamed: " + keys(before));
		jadeNamesNoRite(helper, anchor);
		ServerPlayer practitioner = practitioner(helper, STAND_2);
		use(anchor, practitioner, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING, "The practitioner begins it: " + anchor.run());
		List<EncyclopediaPayload.Row> theirs = inspect(helper, novice);
		EncyclopediaPayload.Row heading = row(theirs, "encyclopedia.jugcraft.inspect.under_way");
		EncyclopediaPayload.Row spent = row(theirs, "compose.jugcraft.ritual.spent");
		helper.assertTrue(heading != null && key((Component) args(heading.text())[0]).equals("encyclopedia.jugcraft.inspect.unknown_rite")
				&& keys(theirs).stream().noneMatch(name -> name.startsWith("ritual.jugcraft.") || name.startsWith("compose.jugcraft.ritual.stage."))
				&& spent != null && List.of(args(spent.text())).equals(List.of(6, 0, 0, 5)), "The novice sees it run, unnamed: " + keys(theirs));
		List<EncyclopediaPayload.Row> mine = inspect(helper, practitioner);
		EncyclopediaPayload.Row named = row(mine, "encyclopedia.jugcraft.inspect.under_way");
		helper.assertTrue(named != null && key((Component) args(named.text())[0]).equals("ritual.jugcraft.adept_attunement")
				&& keys(mine).contains("compose.jugcraft.ritual.stage.backlash"), "The practitioner sees it named, with its table");
		jadeNamesNoRite(helper, anchor);
		helper.succeed();
	}

	// ---------------------------------------------------------------- Root and Rivet (Concordance Chunk C, C-04)

	private static final String ROOT_AND_RIVET = "jugcraft:root_and_rivet";

	/** The Thallite plate (registered by the materials, so it has no Java constant). */
	private static Item thallitePlate() {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse("jugcraft:thallite_plate"));
	}

	private static List<ItemStack> plus(List<ItemStack> base, ItemStack... more) {
		List<ItemStack> out = new ArrayList<>(base);
		out.addAll(List.of(more));
		return out;
	}

	/** Offers Root and Rivet's offerings, with one Mendvetch more than it needs. */
	private static void offerRootAndRivet(CircleAnchorBlockEntity anchor, ServerPlayer player) {
		for (ItemStack stack : List.of(new ItemStack(thallitePlate()), new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Garden.MENDVETCH, 5))) {
			anchor.offer(player, stack);
		}
	}

	/** Every Root and Rivet offering is still in the anchor, unlocked, and nothing was made. */
	private static void rootAndRivetKept(GameTestHelper helper, CircleAnchorBlockEntity anchor, RitualMachine.Interruption reason) {
		List<ItemStack> held = anchor.offerings();
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().last() == reason && !anchor.run().completed()
				&& ROOT_AND_RIVET.equals(anchor.run().ritual()), "Root and Rivet broke off for " + reason + ": " + anchor.run());
		helper.assertTrue(count(held, thallitePlate()) == 1 && count(held, Items.COPPER_INGOT) == 4 && count(held, Garden.MENDVETCH) == 5,
				"Every offering is still in the anchor after " + reason + ": " + held);
		helper.assertTrue(anchor.output().isEmpty(), "Nothing was made after " + reason);
	}

	/** Starts Root and Rivet and checks it channels with six Focus paid. */
	private static ServerPlayer startRootAndRivet(GameTestHelper helper, CircleAnchorBlockEntity anchor) {
		helper.assertTrue(thallitePlate() != Items.AIR, "The Thallite plate is registered");
		ServerPlayer player = practitioner(helper, STAND);
		offerRootAndRivet(anchor, player);
		use(anchor, player, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.CHANNELING && ROOT_AND_RIVET.equals(anchor.run().ritual()),
				"Root and Rivet starts: " + anchor.run());
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == 20 - 6, "It took six Focus");
		return player;
	}

	/** Four steps of 40 ticks: one foundation, its offerings consumed once, 2 Ley Charge a step from each pylon; the spare Mendvetch makes nothing. */
	@GameTest(maxTicks = 300)
	public void rootAndRivetAttunesOnePlateOnce(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = startRootAndRivet(helper, anchor);
		helper.runAfterDelay(RitualMachine.STEP_TICKS * 4L + 20, () -> {
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.COMPLETE && anchor.output().is(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION)
					&& anchor.output().getCount() == 1, "One Rooted Thallite Foundation: " + anchor.run() + " " + anchor.output());
			List<ItemStack> left = anchor.offerings();
			helper.assertTrue(count(left, thallitePlate()) == 0 && count(left, Items.COPPER_INGOT) == 0 && count(left, Garden.MENDVETCH) == 1,
					"Exactly its offerings were consumed; the spare Mendvetch stays: " + left);
			for (BlockPos at : pylons()) {
				helper.assertValueEqual(pylon(helper, at).ley(), 64L - 2 * 4, "2 Ley Charge a step for four steps at " + at);
			}
			helper.assertValueEqual(anchor.spent(), new Readiness.Spent(6, 32, 4, 0, 0), "spent in all: 6 Focus, 32 Ley Charge");
			use(anchor, player, false);
			helper.assertTrue(anchor.output().isEmpty() && anchor.phase() == RitualMachine.Phase.IDLE && anchor.run().completed()
					&& player.getInventory().countItem(JugcraftConcordance.ROOTED_THALLITE_FOUNDATION) == 1, "The participant took it");
			int focus = ConcordanceProgress.currentFocus(player);
			use(anchor, player, false); // one Mendvetch: the vigil is closest (4 lacking), so nothing starts
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(player) == focus
					&& count(anchor.offerings(), Garden.MENDVETCH) == 1 && anchor.output().isEmpty(), "Nothing more is made of what is left");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void aDryPylonStopsRootAndRivetAndKeepsTheOfferings(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startRootAndRivet(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> pylon(helper, first(StructurePattern.Role.CHANNEL)).setLey(helper.getLevel(), 0));
		helper.runAfterDelay(AFTER_FIRST_STEP + RitualMachine.STEP_TICKS + 10, () -> {
			rootAndRivetKept(helper, anchor, RitualMachine.Interruption.POWER);
			helper.assertValueEqual(anchor.ending(), new Readiness.Ending(RitualMachine.Interruption.POWER, new Readiness.Spent(6, 8, 1, 3, 24)),
					"one step drawn, then a dry pylon");
			for (BlockPos at : pylons()) {
				long ley = pylon(helper, at).ley();
				helper.assertTrue(ley == 0 || ley == 62, "The failed step drew from no pylon: " + ley);
			}
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void rootAndRivetSavedMidRunLapses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CircleAnchorBlockEntity anchor = build(helper, 64);
		startRootAndRivet(helper, anchor);
		helper.runAfterDelay(AFTER_FIRST_STEP, () -> {
			CompoundTag saved = anchor.saveWithoutMetadata(level.registryAccess());
			anchor.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.runAfterDelay(2, () -> {
				rootAndRivetKept(helper, anchor, RitualMachine.Interruption.LAPSED);
				helper.assertValueEqual(anchor.ending(), new Readiness.Ending(RitualMachine.Interruption.LAPSED, new Readiness.Spent(6, 8, 1, 3, 24)),
						"the loaded run lapses, its ending worked out after the load");
				helper.succeed();
			});
		});
	}

	@GameTest(maxTicks = 40)
	public void theAttunementsOfferingsNeverChooseRootAndRivet(GameTestHelper helper) {
		record Case(String name, List<ItemStack> offered, String chosen) {
		}
		CircleAnchorBlockEntity anchor = build(helper, 64);
		ServerPlayer player = practitioner(helper, STAND);
		Item plate = thallitePlate();
		List<ItemStack> attunement = List.of(inscribedWand(), new ItemStack(Items.AMETHYST_SHARD, 4), new ItemStack(Items.GOLD_INGOT, 2),
				new ItemStack(Items.GLOWSTONE_DUST, 4));
		List<ItemStack> rootAndRivet = List.of(new ItemStack(plate), new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Garden.MENDVETCH, 4));
		List<Case> cases = List.of(
				new Case("the attunement's", attunement, "jugcraft:adept_attunement"),
				new Case("the attunement's and copper", plus(attunement, new ItemStack(Items.COPPER_INGOT, 4)), "jugcraft:adept_attunement"),
				new Case("the attunement's, a plate and Mendvetch (six slots)", plus(attunement, new ItemStack(plate), new ItemStack(Garden.MENDVETCH, 4)),
						"jugcraft:adept_attunement"),
				new Case("Root and Rivet's", rootAndRivet, ROOT_AND_RIVET),
				new Case("Root and Rivet's and the vigil's (9 to 4)", plus(rootAndRivet, new ItemStack(Items.GLOW_BERRIES, 4)), ROOT_AND_RIVET),
				new Case("Root and Rivet's and the Kindling's (11 to 9)", plus(rootAndRivet, new ItemStack(Items.GLOWSTONE_DUST, 8),
						new ItemStack(Items.GOLD_INGOT, 2), new ItemStack(Items.ENDER_PEARL)), "jugcraft:spire_kindling"),
				new Case("one Mendvetch short", List.of(new ItemStack(plate), new ItemStack(Items.COPPER_INGOT, 4), new ItemStack(Garden.MENDVETCH, 3)), ""));
		for (Case each : cases) {
			use(anchor, player, true); // the last case's offerings back
			for (ItemStack stack : each.offered()) {
				anchor.offer(player, stack.copy()); // offer() shrinks what it is given
			}
			Choice choice = anchor.choose(player);
			helper.assertValueEqual(choice.chosen() == null ? "" : choice.chosen().id(), each.chosen(), each.name());
		}
		Choice wanting = anchor.choose(player);
		helper.assertTrue(wanting.closest() != null && ROOT_AND_RIVET.equals(wanting.closest().id()) && wanting.missing().size() == 1
				&& wanting.missing().getFirst().have() == 3, "Root and Rivet is closest, one Mendvetch short: " + wanting);
		use(anchor, player, false);
		helper.assertTrue(anchor.phase() == RitualMachine.Phase.IDLE && ConcordanceProgress.currentFocus(player) == 20
				&& count(anchor.offerings(), Garden.MENDVETCH) == 3, "Short, nothing starts and nothing is taken");
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void rootAndRivetIsStrataFoundationWork(GameTestHelper helper) {
		CircleAnchorBlockEntity anchor = build(helper, 64);
		String foundation = "practice:" + Strata.ACTIVITY + ":" + Strata.KEY_FOUNDATION;
		ServerPlayer bystander = practitioner(helper, STAND_2);
		ConcordanceProgress.grant(bystander, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		ServerPlayer player = startRootAndRivet(helper, anchor);
		ConcordanceProgress.grant(player, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		helper.assertFalse(ConcordanceProgress.knowledge(player).progress(Strata.RESEARCH).evidence().containsKey(foundation), "not before it completes");
		helper.runAfterDelay(RitualMachine.STEP_TICKS * 4L + 20, () -> {
			helper.assertTrue(anchor.phase() == RitualMachine.Phase.COMPLETE, "Complete: " + anchor.run());
			helper.assertTrue(ConcordanceProgress.knowledge(player).progress(Strata.RESEARCH).evidence().containsKey(foundation),
					"The participant laid a Stratawright's foundation");
			helper.assertFalse(ConcordanceProgress.knowledge(bystander).progress(Strata.RESEARCH).evidence().containsKey(foundation),
					"A bystander in the circle who took no part laid none");
			helper.succeed();
		});
	}
}
