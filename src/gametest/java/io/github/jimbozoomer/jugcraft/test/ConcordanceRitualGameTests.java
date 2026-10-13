package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.CircleAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualMachine;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructurePattern;
import io.github.jimbozoomer.jugcraft.concordance.ritual.StructureValidator;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
		ServerPlayer vandal = helper.makeMockServerPlayerInLevel();
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
		ServerPlayer novice = helper.makeMockServerPlayerInLevel();
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
}
