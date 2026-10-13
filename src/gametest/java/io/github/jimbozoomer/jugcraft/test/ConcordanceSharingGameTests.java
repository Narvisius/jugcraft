package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LanternCharge;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlock;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.ResearchNotes;
import io.github.jimbozoomer.jugcraft.concordance.ResearchNotesItem;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Roadmap steps 6 and 7 in the game (docs/features/arcane-concordance-sharing.md): Research Notes carry one player's
 * instructions to another without carrying their observation or mastery, and the Lumen Sconce takes Radiance only
 * through the typed transfer rules (what fits, at the rate allowed, drawn back only by its owner).
 */
public class ConcordanceSharingGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
}
