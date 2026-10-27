package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LanternCharge;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlock;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.ResearchNotes;
import io.github.jimbozoomer.jugcraft.concordance.ResearchNotesItem;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactHistory;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactKeeping;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StarReckoning;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap steps 6 and 7 in the game (docs/features/arcane-concordance-sharing.md): Research Notes carry one player's
 * instructions to another without carrying their observation or mastery, and the Lumen Sconce takes Radiance only
 * through the typed transfer rules (what fits, at the rate allowed, drawn back only by its owner); and (Concordance Chunk
 * D, D-02) a copied Spirit Anchor gives nothing and releases nothing, an anchor from before Chunk D claims its record only
 * at its own place, and ending a pact twice returns its escrow once; and (D-04) a copied pact perch is inert; and (D-05)
 * it recalls nothing, and nobody but its holder (their party, a stranger, a stand-in) uses, pays, suspends or ends their
 * pact or keeps its obligation.
 */
public class ConcordanceSharingGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	/** Concordance Chunk D: Gentle's upkeep numbers on the vanilla #minecraft:wool tag. */
	private static final BoundWill.Upkeep UPKEEP = Pacts.terms("minecraft:wool", 1, 7 * Agreement.DAY, Agreement.DAY, 4);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static boolean earned(ServerPlayer player, Identifier id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static ResearchState firstLight(ServerPlayer player) {
		return ConcordanceProgress.knowledge(player).state(FIRST_LIGHT);
	}

	/** The written sheet in a player's inventory, or empty. */
	private static ItemStack writtenNotes(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.has(JugcraftConcordance.RESEARCH_NOTES)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/**
	 * Two players: one who understood First Light writes notes; the other reads them. The reader gains the evidence
	 * but no state (and no codex page) until they observe it themselves; then they understand it at once. Reading again
	 * adds nothing, the author cannot learn from their own notes, notes never carry mastery, and a sheet is written
	 * only by someone who knows something.
	 */
	@GameTest(maxTicks = 20)
	public void notesShareInstructionsNotExperience(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer author = player(helper, new BlockPos(1, 2, 1));
		ServerPlayer reader = player(helper, new BlockPos(3, 2, 1));
		ServerPlayer novice = player(helper, new BlockPos(5, 2, 1));
		Identifier understood = ConcordanceProgress.advancementFor(FIRST_LIGHT, ResearchState.UNDERSTOOD);

		ItemStack blanks = new ItemStack(JugcraftConcordance.RESEARCH_NOTES_ITEM, 2);
		novice.setItemInHand(InteractionHand.MAIN_HAND, blanks.copy());
		helper.assertTrue(ResearchNotesItem.write(novice, InteractionHand.MAIN_HAND, novice.getMainHandItem()) == InteractionResult.FAIL
				&& novice.getMainHandItem().getCount() == 2, "Someone who knows nothing writes nothing and keeps their paper");

		ConcordanceProgress.grant(author, FIRST_LIGHT, ResearchState.MASTERED);
		author.setItemInHand(InteractionHand.MAIN_HAND, blanks);
		ResearchNotesItem.write(author, InteractionHand.MAIN_HAND, author.getMainHandItem());
		ItemStack sheet = writtenNotes(author);
		ResearchNotes notes = sheet.get(JugcraftConcordance.RESEARCH_NOTES);
		helper.assertTrue(notes != null && notes.author().equals(author.getUUID()) && author.getMainHandItem().getCount() == 1
				&& notes.entries().get(FIRST_LIGHT) == ResearchState.UNDERSTOOD,
				"A mastered author's notes record First Light as understood (mastery is not written down): " + notes);
		helper.assertTrue(ResearchNotes.CODEC.parse(JsonOps.INSTANCE, ResearchNotes.CODEC.encodeStart(JsonOps.INSTANCE, notes).getOrThrow())
				.getOrThrow().equals(notes), "Notes round-trip through their codec");

		Knowledge before = ConcordanceProgress.knowledge(author);
		helper.assertTrue(ResearchNotesItem.read(author, notes) == InteractionResult.FAIL
				&& ConcordanceProgress.knowledge(author).equals(before), "An author learns nothing from their own notes");

		ResearchNotesItem.read(reader, notes);
		helper.assertTrue(firstLight(reader) == ResearchState.NONE && !earned(reader, understood)
				&& ConcordanceProgress.knowledge(reader).progress(FIRST_LIGHT).evidence().containsKey("notes:" + author.getUUID()),
				"Read first, the notes are kept as evidence, but the reader has no state and no codex page yet");
		Knowledge kept = ConcordanceProgress.knowledge(reader);
		ResearchNotesItem.read(reader, notes);
		helper.assertTrue(ConcordanceProgress.knowledge(reader).equals(kept), "Reading the same notes again adds nothing");

		ConcordanceProgress.record(reader, new Evidence.Examined("minecraft:amethyst_shard", 0));
		helper.assertTrue(firstLight(reader) == ResearchState.UNDERSTOOD && earned(reader, understood)
				&& ConcordanceProgress.knowledge(reader).invocationCost("jugcraft:kindle") == 4,
				"Observing it themselves, the reader understands at once through the notes, and the codex opens");
		helper.assertTrue(!earned(reader, ConcordanceProgress.advancementFor(FIRST_LIGHT, ResearchState.MASTERED)),
				"The reader has not mastered it: that takes their own practice");

		// Through the item's use: written sheets are read, at most once per half second.
		reader.setItemInHand(InteractionHand.MAIN_HAND, sheet.copy());
		RateGate.forget(reader.getUUID());
		helper.assertTrue(reader.getMainHandItem().getItem().use(level, reader, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS,
				"Using a written sheet reads it");
		helper.assertTrue(reader.getMainHandItem().getItem().use(level, reader, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
				"A second request at once is ignored");
		helper.assertTrue(reader.getMainHandItem().has(JugcraftConcordance.RESEARCH_NOTES), "Reading does not use the sheet up");
		helper.succeed();
	}

	private static ResearchState stolas(ServerPlayer player) {
		return ConcordanceProgress.knowledge(player).state(StolasRegard.RESEARCH);
	}

	/**
	 * Concordance Chunk D, D-03 (E1, E3): Stolas's Regard takes no notes. An author who has mastered it writes it down; a
	 * reader with the Binding Arts understood reads the sheet and gains nothing toward it, not even kept evidence. The reader
	 * earns each state only by their own examination, harvest and assay, and reading again after that adds nothing.
	 */
	@GameTest(maxTicks = 20)
	public void notesNeverStandInForStolasEvidence(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Definitions.Research regard = ConcordanceData.rules().research(StolasRegard.RESEARCH);
		helper.assertTrue(regard != null && regard.states().values().stream().flatMap(List::stream)
				.noneMatch(rule -> rule.kind() == EvidenceRule.Kind.NOTES), "No state of Stolas's Regard takes notes: " + regard);
		ServerPlayer author = player(helper, new BlockPos(1, 2, 1));
		ServerPlayer reader = player(helper, new BlockPos(3, 2, 1));
		for (ServerPlayer each : List.of(author, reader)) {
			ConcordanceProgress.grant(each, FIRST_LIGHT, ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(each, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		}
		ConcordanceProgress.grant(author, StolasRegard.RESEARCH, ResearchState.MASTERED);
		author.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.RESEARCH_NOTES_ITEM));
		ResearchNotesItem.write(author, InteractionHand.MAIN_HAND, author.getMainHandItem());
		ResearchNotes notes = writtenNotes(author).get(JugcraftConcordance.RESEARCH_NOTES);
		helper.assertTrue(notes != null && notes.entries().get(StolasRegard.RESEARCH) == ResearchState.UNDERSTOOD,
				"The author's notes name Stolas's Regard, understood: " + notes);
		ResearchNotesItem.read(reader, notes);
		Knowledge.Progress read = ConcordanceProgress.knowledge(reader).progress(StolasRegard.RESEARCH);
		helper.assertTrue(read.state() == ResearchState.NONE && read.evidence().isEmpty(),
				"Read, the notes give Stolas's Regard nothing, not even kept evidence: " + read);
		Examination.forget(reader.getUUID());
		reader.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FEATHER));
		Examination.examine(reader, level, reader.getMainHandItem());
		helper.assertValueEqual(stolas(reader), ResearchState.ENCOUNTERED, "Their own examination of a feather begins it");
		ConcordanceProgress.record(reader, new Evidence.Practiced(Garden.ACTIVITY, "jugcraft:sunpetal"));
		ConcordanceProgress.record(reader, new Evidence.Practiced(Assaying.ACTIVITY, "minecraft:raw_iron"));
		helper.assertValueEqual(stolas(reader), ResearchState.UNDERSTOOD, "Their own harvest and assay understand it");
		Knowledge.Progress earned = ConcordanceProgress.knowledge(reader).progress(StolasRegard.RESEARCH);
		ResearchNotesItem.read(reader, notes);
		helper.assertTrue(ConcordanceProgress.knowledge(reader).progress(StolasRegard.RESEARCH).equals(earned)
				&& earned.evidence().keySet().stream().noneMatch(key -> key.startsWith("notes:")), "Reading again adds nothing: " + earned);
		helper.succeed();
	}

	/**
	 * The sconce: anyone may pour Radiance in (what fits, from a lantern), only its owner may draw it out, at most 32
	 * moves through it per second, it lights while it holds any, it burns one measure a minute, and broken it keeps
	 * its Radiance on the item.
	 */
	@GameTest(maxTicks = 20)
	public void sconceTakesRadianceByTheRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		ServerPlayer owner = player(helper, new BlockPos(2, 2, 1));
		ServerPlayer stranger = player(helper, new BlockPos(4, 2, 1));
		BlockPos relative = new BlockPos(3, 2, 3);
		helper.setBlock(relative, JugcraftConcordance.LUMEN_SCONCE);
		BlockPos pos = helper.absolutePos(relative);
		JugcraftConcordance.LUMEN_SCONCE.setPlacedBy(level, pos, level.getBlockState(pos), owner, ItemStack.EMPTY);
		LumenSconceBlockEntity sconce = helper.getBlockEntity(relative, LumenSconceBlockEntity.class);
		long now = ConcordanceProgress.now(level);
		helper.assertTrue(sconce.owner() != null && sconce.owner().equals(owner.getUUID()) && sconce.remaining(now) == 0
				&& !level.getBlockState(pos).getValue(LumenSconceBlock.LIT), "Placed, it belongs to its placer and is dark");

		ItemStack strangerLantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		KindledLanternItem.set(strangerLantern, 40, now, false);
		LumenSconceBlock.exchange(level, pos, stranger, strangerLantern, false);
		helper.assertTrue(sconce.remaining(now) == LumenSconceBlockEntity.POUR && KindledLanternItem.remaining(strangerLantern, now) == 24
				&& level.getBlockState(pos).getValue(LumenSconceBlock.LIT) && level.getBlockState(pos).getLightEmission() == 15,
				"Anyone may pour: 16 moves from the lantern and the sconce lights at 15");
		helper.assertTrue(LumenSconceBlock.exchange(level, pos, stranger, strangerLantern, false) == InteractionResult.FAIL
				&& sconce.remaining(now) == LumenSconceBlockEntity.POUR, "A second pour at once is ignored");

		Transfers.Transfer theft = sconce.draw(level, stranger.getUUID(), strangerLantern);
		helper.assertTrue(theft.outcome() == Transfers.Outcome.NOT_PERMITTED && sconce.remaining(now) == LumenSconceBlockEntity.POUR,
				"A stranger may not draw from it");
		ItemStack ownerLantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		KindledLanternItem.set(ownerLantern, 60, now, false);
		Transfers.Transfer drawn = sconce.draw(level, owner.getUUID(), ownerLantern);
		helper.assertTrue(drawn.outcome() == Transfers.Outcome.PARTIAL && drawn.received() == 4
				&& KindledLanternItem.remaining(ownerLantern, now) == KindledLanternItem.CAPACITY && sconce.remaining(now) == 12,
				"The owner draws what their lantern has room for; the rest stays: " + drawn);
		Transfers.Transfer pour = sconce.pour(level, stranger.getUUID(), strangerLantern);
		helper.assertTrue(pour.received() == 12 && sconce.remaining(now) == 24, "The window's budget (32) allows 12 more: " + pour);
		helper.assertTrue(sconce.pour(level, stranger.getUUID(), strangerLantern).outcome() == Transfers.Outcome.RATE_LIMITED,
				"and then nothing until the next window");
		helper.assertTrue(sconce.remaining(now + 3L * LumenSconceBlockEntity.BURN_TICKS) == 21, "It burns one measure a minute");
		long total = sconce.remaining(now) + KindledLanternItem.remaining(strangerLantern, now) + KindledLanternItem.remaining(ownerLantern, now);
		helper.assertTrue(total == 40 + 60, "Every transfer conserved the Radiance: " + total);

		if (FabricLoader.getInstance().isModLoaded("jade")) {
			helper.assertTrue(ConcordanceDataProvider.snapshot(sconce, now).getIntOr("radiance", -1) == 24,
					"Jade sees the sconce's Radiance");
		}

		// Broken and placed again, it keeps what it held.
		LanternCharge kept = sconce.collectComponents().get(JugcraftConcordance.RADIANCE);
		helper.assertTrue(kept != null && kept.stored() == 24, "Broken, the sconce keeps its Radiance on the item: " + kept);
		ItemStack item = new ItemStack(JugcraftConcordance.LUMEN_SCONCE);
		item.set(JugcraftConcordance.RADIANCE, kept);
		BlockPos second = new BlockPos(5, 2, 5);
		helper.setBlock(second, JugcraftConcordance.LUMEN_SCONCE);
		LumenSconceBlockEntity again = helper.getBlockEntity(second, LumenSconceBlockEntity.class);
		again.applyComponentsFromItemStack(item);
		again.refresh(level, now);
		helper.assertTrue(again.remaining(now) == 24 && level.getBlockState(helper.absolutePos(second)).getValue(LumenSconceBlock.LIT),
				"Placed again, it burns what it kept");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-02: a copied anchor

	private static WorkerDefinition.Spirit terms() {
		return Workers.catalog().get(GatheringShadeEntity.DEFINITION, WorkerDefinition.Spirit.class);
	}

	private static String key(@Nullable Component line) {
		return line != null && line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}

	private static int wool(GameTestHelper helper, AABB box) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.isAlive() && item.getItem().is(Items.WOOL.white()))
				.stream().mapToInt(item -> item.getItem().getCount()).sum();
	}

	private static int escrowed(ServerLevel level, UUID record) {
		return BoundWills.of(level.getServer()).escrow(record).stream().mapToInt(ItemStack::getCount).sum();
	}

	private static SpiritAnchorBlockEntity loaded(GameTestHelper helper, BlockPos at, CompoundTag saved) {
		helper.setBlock(at, Workers.SPIRIT_ANCHOR);
		SpiritAnchorBlockEntity anchor = helper.getBlockEntity(at, SpiritAnchorBlockEntity.class);
		anchor.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
		return anchor;
	}

	/**
	 * D-02 (E3, 10:52, D-S9): an anchor's data copied to a second place is inert: it says another agreement covers it and
	 * suspends, ends, takes and pays nothing; broken, it releases nothing; the original broken releases once, its escrow
	 * dropped where it stood, the departure kept.
	 */
	@GameTest(maxTicks = 20)
	public void aCopiedAnchorGivesNothingAndReleasesNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = player(helper, new BlockPos(4, 1, 4));
		BlockPos originalAt = new BlockPos(2, 1, 2);
		BlockPos copyAt = new BlockPos(6, 1, 2);
		helper.setBlock(originalAt, Workers.SPIRIT_ANCHOR);
		helper.setBlock(copyAt, Workers.SPIRIT_ANCHOR);
		SpiritAnchorBlockEntity original = helper.getBlockEntity(originalAt, SpiritAnchorBlockEntity.class);
		SpiritAnchorBlockEntity copy = helper.getBlockEntity(copyAt, SpiritAnchorBlockEntity.class);
		long now = level.getGameTime() + 1_000_000L;
		helper.assertValueEqual(original.holder().seal(level, holder, original.sealing(terms(), UPKEEP), now).outcome(), Pacts.Outcome.DONE, "Sealed with upkeep");
		BoundWills wills = BoundWills.of(level.getServer());
		UUID record = original.agreement().record();
		helper.assertValueEqual(original.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 2), now), 2, "two periods in its escrow");
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(original.spirit());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), original.saveWithoutMetadata(level.registryAccess())));
		BoundWill before = wills.ledger().get(record);
		helper.assertTrue(copy.holder().verify(level, now) == AgreementHolder.Standing.ELSEWHERE && original.holder().verify(level, now) == AgreementHolder.Standing.VALID,
				"Copied to a second place, the copy is not the record's; the original is");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, copy, false)), "compose.jugcraft.diagnostic.conflicting_agreement",
				"The copy says another agreement covers it");
		SpiritAnchorBlock.use(holder, level, copy, true);
		helper.assertTrue(copy.holder().end(level, holder, now) == AgreementHolder.Ended.CONFLICTING
				&& copy.holder().toggle(level, holder, now) == AgreementHolder.Toggle.CONFLICTING
				&& copy.holder().look(level, now + 7 * Agreement.DAY) == Pacts.Event.NONE
				&& copy.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 1), now) == 0
				&& wills.ledger().get(record).equals(before) && escrowed(level, record) == 2, "It suspends, ends, takes and pays nothing");
		helper.setBlock(copyAt, Blocks.AIR);
		helper.assertTrue(wills.ledger().get(record) != null && !shade.isRemoved()
				&& WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(shade.getUUID()) && escrowed(level, record) == 2,
				"Broken, the copy releases nothing: the record, its escrow, the shade and its roster place stay");
		helper.assertTrue(original.holder().verify(level, now) == AgreementHolder.Standing.VALID && shade.think(level, 18000L, now) != Status.FINISHED
				&& wills.problems().isEmpty(), "The original keeps its agreement and its shade");
		helper.setBlock(originalAt, Blocks.AIR);
		List<Pacts.Departure> history = PactHistory.of(level.getServer()).history(holder.getUUID());
		helper.assertTrue(wills.ledger().get(record) == null && shade.isRemoved() && wool(helper, new AABB(helper.absolutePos(originalAt)).inflate(1.5)) == 2
				&& !history.isEmpty() && record.equals(history.getLast().record()) && Pacts.BROKEN.equals(history.getLast().reason()),
				"The original broken releases it once: its escrow drops where it stood, and the departure is kept");
		helper.succeed();
	}

	/**
	 * D-02 (E3, D-S9, D-P3): a copy of a pre-Chunk-D anchor, though it loads first, never claims its record; the anchor its
	 * terms were sealed at claims it once; one whose spirit is seated elsewhere does not.
	 */
	@GameTest(maxTicks = 20)
	public void aLegacyAnchorClaimsItsPlaceOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BoundWills wills = BoundWills.of(level.getServer());
		UUID holder = UUID.randomUUID();
		UUID record = UUID.randomUUID();
		UUID spirit = UUID.randomUUID();
		wills.seal(new BoundWill(record, GatheringShadeEntity.DEFINITION, "gathering_shade", holder, 5L, false));
		BlockPos originalAt = new BlockPos(1, 1, 1);
		BlockPos copyAt = new BlockPos(5, 1, 1);
		CompoundTag older = ConcordancePersistenceGameTests.olderAnchor(level, record, holder, helper.absolutePos(originalAt), spirit, false);
		SpiritAnchorBlockEntity copy = loaded(helper, copyAt, older);
		SpiritAnchorBlockEntity original = loaded(helper, originalAt, older);
		long now = level.getGameTime();
		helper.assertTrue(copy.holder().verify(level, now) == AgreementHolder.Standing.ELSEWHERE && Pacts.unclaimed(wills.ledger().get(record)),
				"A copy from before Chunk D, though it loads first, never claims: its terms were sealed elsewhere");
		helper.assertValueEqual(original.holder().verify(level, now), AgreementHolder.Standing.VALID, "The anchor its terms were sealed at claims it");
		BoundWill claimed = wills.ledger().get(record);
		helper.assertTrue(claimed.pact() != null && Couriers.place(level, helper.absolutePos(originalAt)).equals(claimed.pact().place())
				&& spirit.equals(claimed.pact().presence()) && claimed.pact().hold() == BoundWill.Hold.NONE && "gathering_shade".equals(claimed.counterpart()),
				"once, writing its place and presence; its counterpart as written");
		helper.assertTrue(original.holder().verify(level, now + 20) == AgreementHolder.Standing.VALID && wills.ledger().get(record).equals(claimed)
				&& copy.holder().verify(level, now + 20) == AgreementHolder.Standing.ELSEWHERE, "Again, nothing changes; the copy stays inert");
		helper.setBlock(copyAt, Blocks.AIR);
		helper.assertTrue(wills.ledger().get(record) != null, "Broken, the copy releases nothing");
		UUID second = UUID.randomUUID();
		UUID seated = UUID.randomUUID();
		wills.seal(new BoundWill(second, GatheringShadeEntity.DEFINITION, "gathering_shade", holder, 6L, false));
		BlockPos thirdAt = new BlockPos(1, 1, 5);
		SpiritAnchorBlockEntity third = loaded(helper, thirdAt,
				ConcordancePersistenceGameTests.olderAnchor(level, second, holder, helper.absolutePos(thirdAt), seated, false));
		GatheringShadeEntity elsewhere = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.LOAD);
		elsewhere.setUUID(seated);
		elsewhere.setOwner(holder);
		elsewhere.setAnchor(helper.absolutePos(new BlockPos(5, 1, 5)));
		Vec3 standing = helper.absoluteVec(new Vec3(5.5, 2.0, 5.5));
		elsewhere.snapTo(standing.x, standing.y, standing.z, 0.0F, 0.0F);
		level.addFreshEntity(elsewhere);
		helper.assertTrue(third.holder().verify(level, now) == AgreementHolder.Standing.ELSEWHERE && Pacts.unclaimed(wills.ledger().get(second)),
				"An anchor whose spirit is seated at another place does not claim");
		elsewhere.discard();
		wills.release(record, holder);
		wills.release(second, holder);
		helper.succeed();
	}

	/**
	 * D-02 (E4, 10:52, D-S6): ended, the escrow comes back once (what fits into the inventory, the rest at the holder's feet);
	 * the record, its escrow and the shade go; one departure kept; a copy forgets its terms; ending again or breaking the copy
	 * returns nothing more.
	 */
	@GameTest(maxTicks = 20)
	public void endingTwiceReturnsOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = player(helper, new BlockPos(4, 1, 4));
		BlockPos at = new BlockPos(2, 1, 2);
		BlockPos copyAt = new BlockPos(6, 1, 2);
		helper.setBlock(at, Workers.SPIRIT_ANCHOR);
		helper.setBlock(copyAt, Workers.SPIRIT_ANCHOR);
		SpiritAnchorBlockEntity anchor = helper.getBlockEntity(at, SpiritAnchorBlockEntity.class);
		SpiritAnchorBlockEntity copy = helper.getBlockEntity(copyAt, SpiritAnchorBlockEntity.class);
		long now = level.getGameTime() + 1_000_000L;
		helper.assertValueEqual(anchor.holder().seal(level, holder, anchor.sealing(terms(), UPKEEP), now).outcome(), Pacts.Outcome.DONE, "Sealed with upkeep");
		UUID record = anchor.agreement().record();
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 3), now), 3, "three periods in its escrow");
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), anchor.saveWithoutMetadata(level.registryAccess())));
		Inventory inventory = holder.getInventory();
		for (int slot = 0; slot < 36; slot++) {
			inventory.setItem(slot, slot == 0 ? new ItemStack(Items.WOOL.white(), 63) : new ItemStack(Items.STONE, 64));
		}
		helper.assertValueEqual(anchor.holder().end(level, holder, now), AgreementHolder.Ended.ENDED, "Its holder ends it");
		AABB feet = holder.getBoundingBox().inflate(3.0);
		helper.assertTrue(inventory.countItem(Items.WOOL.white()) == 64 && wool(helper, feet) == 2,
				"the escrow comes back once: what fits into the inventory, the rest at their feet");
		BoundWills wills = BoundWills.of(level.getServer());
		helper.assertTrue(wills.ledger().get(record) == null && wills.escrow(record).isEmpty() && shade.isRemoved()
				&& !WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(shade.getUUID()) && anchor.agreement() == null,
				"the record and its escrow go, the shade departs and is struck off");
		List<Pacts.Departure> history = PactHistory.of(level.getServer()).history(holder.getUUID());
		helper.assertTrue(history.size() == 1 && history.getFirst().record().equals(record) && Pacts.ENDED.equals(history.getFirst().reason()),
				"and the departure is kept once: " + history);
		helper.assertTrue(copy.holder().verify(level, now) == AgreementHolder.Standing.NONE && copy.agreement() == null, "The copy, its record gone, forgets its terms");
		helper.setBlock(copyAt, Blocks.AIR);
		helper.assertValueEqual(AgreementHolder.end(level, null, holder, record, Pacts.ENDED, now + 20), AgreementHolder.Ended.NOTHING, "Ending again finds nothing");
		helper.assertTrue(inventory.countItem(Items.WOOL.white()) == 64 && wool(helper, feet) == 2 && PactHistory.of(level.getServer()).history(holder.getUUID()).size() == 1,
				"and nothing more comes back, by the copy's breaking or the second end");
		helper.succeed();
	}

	/** D-04 (E3, 10:52, D-S9): a perch's data copied to a second place is inert: its screen reads CONFLICTING_AGREEMENT; accepting, depositing, suspending, ending, seating and recalling there change nothing; its upkeep looks at nothing; no visitor comes; breaking it releases and unseats nothing; the original keeps its pact. */
	@GameTest(maxTicks = 20)
	public void aCopiedPerchIsInert(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = ConcordanceWorkerGameTests.ready(level, player(helper, new BlockPos(4, 1, 4)));
		ConcordanceWorkerGameTests.pocket(holder, 1, AstrolabeItem.CAPACITY); // a survival player pays the Open Hand Compact
		BlockPos originalAt = new BlockPos(2, 1, 2);
		BlockPos copyAt = new BlockPos(6, 1, 2);
		PactPerchBlockEntity original = ConcordanceWorkerGameTests.perch(helper, originalAt, holder);
		PactMenu sealing = ConcordanceWorkerGameTests.open(helper, holder, original);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(ConcordanceWorkerGameTests.send(holder, sealing, PactRequestPayload.ACCEPT, 0), PactMenu.SEALED),
				"Sealed at the original");
		holder.closeContainer();
		long now = level.getGameTime();
		BoundWill sealed = ConcordanceWorkerGameTests.recordOf(level, holder);
		helper.assertValueEqual(original.holder().deposit(level, holder, new ItemStack(Items.AMETHYST_SHARD, 2), now), 2, "two offerings kept");
		BoundWill record = BoundWills.of(level.getServer()).ledger().get(sealed.id());
		StolasEntity presence = (StolasEntity) level.getEntity(record.pact().presence());
		helper.setBlock(copyAt, Workers.PACT_PERCH);
		PactPerchBlockEntity copy = helper.getBlockEntity(copyAt, PactPerchBlockEntity.class);
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), original.saveWithoutMetadata(level.registryAccess())));
		helper.assertTrue(copy.holder().verify(level, now) == AgreementHolder.Standing.ELSEWHERE && original.holder().verify(level, now) == AgreementHolder.Standing.VALID,
				"The copy is not the record's; the original is");
		PactMenu menu = ConcordanceWorkerGameTests.open(helper, holder, copy);
		helper.assertTrue(menu.lastSent().mode() == PactStatePayload.CLOSED
				&& ConcordanceWorkerGameTests.keys(menu.lastSent().notes()).contains("compose.jugcraft.diagnostic.conflicting_agreement")
				&& copy.visitor() == null && ConcordanceWorkerGameTests.stolases(helper).size() == 1, "Its screen says another agreement covers it; no visitor");
		for (int action : List.of(PactRequestPayload.ACCEPT, PactRequestPayload.DEPOSIT, PactRequestPayload.SUSPEND, PactRequestPayload.RESUME,
				PactRequestPayload.END, PactRequestPayload.SEAT, PactRequestPayload.RECALL)) {
			PactStatePayload answer = ConcordanceWorkerGameTests.send(holder, menu, action, 0);
			helper.assertTrue(answer != null && answer.result().isPresent() && !answer.result().get().done()
					&& record.equals(BoundWills.of(level.getServer()).ledger().get(record.id()))
					&& ConcordanceWorkerGameTests.escrowed(level, record.id()) == 2, "At the copy, action " + action + " changes nothing: " + answer);
		}
		helper.assertTrue(copy.holder().look(level, now + 7 * Agreement.DAY) == Pacts.Event.NONE, "Its upkeep looks at nothing");
		holder.closeContainer();
		helper.setBlock(copyAt, Blocks.AIR);
		helper.assertTrue(record.equals(BoundWills.of(level.getServer()).ledger().get(record.id())) && !presence.gone()
				&& WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(presence.getUUID())
				&& ConcordanceWorkerGameTests.escrowed(level, record.id()) == 2, "Broken, the copy releases and unseats nothing");
		helper.assertTrue(original.holder().verify(level, now) == AgreementHolder.Standing.VALID && presence.think(level, 0L, now) != Status.FINISHED
				&& BoundWills.of(level.getServer()).problems().isEmpty(), "The original keeps its pact and its Stolas");
		helper.succeed();
	}

	/** D-05 (E3, 10:52): a perch's data copied to a second place recalls nothing: it spends, claims, pays, looks, suspends, ends, seats and calls nothing; broken, it releases nothing; the original still serves. */
	@GameTest(maxTicks = 20)
	public void aCopiedPerchRecallsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = ConcordanceWorkerGameTests.regarded(helper);
		long g0 = ConcordanceWorkerGameTests.dayStart(level, 100);
		BlockPos originalAt = new BlockPos(2, 1, 2);
		BlockPos copyAt = new BlockPos(6, 1, 2);
		PactPerchBlockEntity original = ConcordanceWorkerGameTests.stolasPact(helper, keeper, originalAt, g0);
		UUID record = original.holder().agreement().record();
		ConcordanceWorkerGameTests.observed(level, keeper, ConcordanceWorkerGameTests.FULL_MOON);
		ItemStack held = ConcordanceWorkerGameTests.holding(keeper, AstrolabeItem.CAPACITY);
		ConcordanceWorkerGameTests.give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, original.holder(), Items.AMETHYST_SHARD, g0), 2, "Two paid ahead");
		helper.setBlock(copyAt, Workers.PACT_PERCH);
		PactPerchBlockEntity copy = helper.getBlockEntity(copyAt, PactPerchBlockEntity.class);
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), original.saveWithoutMetadata(level.registryAccess())));
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill before = wills.ledger().get(record);
		ConcordanceWorkerGameTests.beside(helper, keeper, copyAt);
		helper.assertTrue(copy.holder().verify(level, g0) == AgreementHolder.Standing.ELSEWHERE, "The copy is not the record's");
		helper.assertValueEqual(StarReckoning.reckon(level, keeper, copy.holder(), ConcordanceWorkerGameTests.FULL_MOON, 24006L, g0), Pacts.Reckoning.CONFLICTING,
				"At the copy, Stolas reckons nothing");
		ConcordanceWorkerGameTests.give(keeper, new ItemStack(Items.AMETHYST_SHARD, 1));
		helper.assertTrue(AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY && !ConcordanceWorkerGameTests.recalledEver(level, keeper, ConcordanceWorkerGameTests.FULL_MOON)
				&& PactKeeping.deposit(level, keeper, copy.holder(), Items.AMETHYST_SHARD, g0) == 0 && ConcordanceWorkerGameTests.shards(keeper) == 1
				&& copy.holder().look(level, g0 + 7 * 24000L) == Pacts.Event.NONE && copy.holder().end(level, keeper, g0) == AgreementHolder.Ended.CONFLICTING
				&& PactKeeping.suspend(level, keeper, copy.holder(), g0) == AgreementHolder.Toggle.CONFLICTING
				&& copy.holder().seat(level, keeper, StolasEntity.COUNTERPART, g0) == AgreementHolder.Seat.NOTHING
				&& PactKeeping.page(level, keeper, copy.holder(), g0) == PactKeeping.Page.NONE
				&& wills.ledger().get(record).equals(before) && ConcordanceWorkerGameTests.escrowed(level, record) == 2,
				"nothing is spent, claimed, paid, held, ended, seated or shown through it");
		ConcordanceWorkerGameTests.stolas(level, original).remove(Entity.RemovalReason.KILLED);
		PactKeeping.tick(level, copy.holder(), copy.getBlockPos(), g0 + 20);
		helper.assertTrue(ConcordanceWorkerGameTests.stolas(level, original) == null && original.holder().spiritLost(level), "The copy's ticker calls nobody");
		PactKeeping.tick(level, original.holder(), original.getBlockPos(), g0 + 40);
		helper.assertTrue(ConcordanceWorkerGameTests.stolas(level, original) != null, "the original's calls Stolas back");
		helper.setBlock(copyAt, Blocks.AIR);
		helper.assertTrue(Pacts.seated(wills.ledger().get(record)) && ConcordanceWorkerGameTests.escrowed(level, record) == 2, "Broken, the copy releases nothing");
		ConcordanceWorkerGameTests.beside(helper, keeper, originalAt);
		helper.assertTrue(StarReckoning.reckon(level, keeper, original.holder(), ConcordanceWorkerGameTests.FULL_MOON, 24006L, g0) == Pacts.Reckoning.RECALLED
				&& wills.problems().isEmpty(), "The original still serves");
		helper.succeed();
	}

	/** D-05 (E1, D-S8): the holder's party, a stranger and a stand-in with the holder's identity cannot use, pay, suspend or end the holder's pact, and another's observation keeps no obligation of it. */
	@GameTest(maxTicks = 20)
	public void anotherPlayerCannotUseTheHoldersService(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = ConcordanceWorkerGameTests.regarded(helper);
		ServerPlayer friend = ConcordanceWorkerGameTests.regarded(helper);
		ServerPlayer stranger = ConcordanceWorkerGameTests.regarded(helper);
		BlockPos at = new BlockPos(2, 1, 2);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(keeper.getUUID(), friend.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(friend.getUUID(), ms) == PartyManager.Result.OK, "The holder and a friend form a party");
			long g0 = ConcordanceWorkerGameTests.dayStart(level, 100);
			PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, keeper, at, g0);
			String full = ConcordanceWorkerGameTests.FULL_MOON;
			for (ServerPlayer player : List.of(keeper, friend, stranger)) {
				ConcordanceWorkerGameTests.observed(level, player, full);
				ConcordanceWorkerGameTests.holding(player, AstrolabeItem.CAPACITY);
			}
			ConcordanceWorkerGameTests.beside(helper, friend, at);
			ConcordanceWorkerGameTests.beside(helper, stranger, at);
			FakePlayer standIn = FakePlayer.get(level, new GameProfile(keeper.getUUID(), "[Concordance]"));
			helper.assertTrue(StarReckoning.reckon(level, friend, perch.holder(), full, 24006L, g0) == Pacts.Reckoning.NOT_HOLDER
					&& StarReckoning.reckon(level, stranger, perch.holder(), full, 24006L, g0) == Pacts.Reckoning.NOT_HOLDER
					&& StarReckoning.reckon(level, standIn, perch.holder(), full, 24006L, g0) == Pacts.Reckoning.NOT_HOLDER,
					"Its holder's party, a stranger and a stand-in are refused");
			helper.assertTrue(AstrolabeItem.charge(friend.getMainHandItem()) == AstrolabeItem.CAPACITY && !ConcordanceWorkerGameTests.recalledEver(level, friend, full)
					&& ConcordanceWorkerGameTests.done(level, perch, g0) == 0, "spending, claiming and counting nothing");
			ConcordanceWorkerGameTests.give(friend, new ItemStack(Items.AMETHYST_SHARD, 2));
			UUID record = perch.holder().agreement().record();
			helper.assertTrue(PactKeeping.deposit(level, friend, perch.holder(), Items.AMETHYST_SHARD, g0) == 0 && ConcordanceWorkerGameTests.shards(friend) == 2
					&& PactKeeping.suspend(level, friend, perch.holder(), g0) == AgreementHolder.Toggle.NOT_HOLDER
					&& perch.holder().end(level, friend, g0) == AgreementHolder.Ended.NOT_HOLDER
					&& ConcordanceWorkerGameTests.key(PactKeeping.end(level, friend, perch.holder(), StolasEntity.COUNTERPART, g0).said())
							.equals("message.jugcraft.concordance.workers.not_holder")
					&& PactKeeping.page(level, friend, perch.holder(), g0) == PactKeeping.Page.NONE
					&& !Pacts.benefits(BoundWills.of(level.getServer()).ledger().get(record), friend.getUUID(), true), "nor pay, suspend, end or read it; it benefits its holder alone");
			ObservatoryBlockEntity observatory = ConcordanceWorkerGameTests.observatory(helper, new BlockPos(0, 1, 6));
			helper.assertTrue(observatory.observe(friend, level, ConcordanceWorkerGameTests.NIGHT_ZERO, g0 + 20, ConcordanceWorkerGameTests.ONLY_MOON) == 1
					&& ConcordanceWorkerGameTests.regard(friend) == ResearchState.UNDERSTOOD && ConcordanceWorkerGameTests.regard(keeper) == ResearchState.UNDERSTOOD,
					"Another's observation keeps no one's obligation");
			ConcordanceWorkerGameTests.beside(helper, keeper, at);
			helper.assertValueEqual(StarReckoning.reckon(level, keeper, perch.holder(), full, 24006L, g0), Pacts.Reckoning.RECALLED, "Its holder is served");
		} finally {
			parties.adminDisband(keeper.getUUID());
		}
		helper.succeed();
	}
}
