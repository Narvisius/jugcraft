package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.dream.DreamRules;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.DreamExpedition;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.DreamWispEntity;
import io.github.jimbozoomer.jugcraft.concordance.dreaming.Dreaming;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.hex.Curse;
import io.github.jimbozoomer.jugcraft.concordance.hex.Link;
import io.github.jimbozoomer.jugcraft.concordance.hex.WardCategory;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.ScryingGlassItem;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.Sympathy;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.TaglockItem;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.WardSigilItem;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Roadmap step 22, sympathetic links, curses, wards and dream travel (docs/features/arcane-concordance-hexes.md), on a
 * real server: links are taken by touch, fade, and respect the multiplayer rules and wards; curses are bounded, pulse
 * through the shared boundary, are revalidated at every pulse, investigated and remedied; a moving ward stops pushes;
 * and a dream holds everything in one escrow and gives back exactly that, however it ends, never twice.
 */
public class ConcordanceHexGameTests {
	private static final BlockPos STAND = new BlockPos(2, 2, 2);

	private static ServerPlayer hexer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(STAND);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Sympathy.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Dreaming.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(player, 20);
		RateGate.forget(player.getUUID());
		return player;
	}

	/** A taglock takes a link by touch; never to oneself, through a linking ward, or against the multiplayer rules. */
	@GameTest(maxTicks = 20)
	public void linksAreTakenByTouchAndRespectTheRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer hexer = hexer(helper);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		ItemStack taglock = new ItemStack(Sympathy.TAGLOCK);
		helper.assertTrue(TaglockItem.bind(hexer, level, taglock, zombie).isEmpty() && taglock.get(Sympathy.LINK) != null
				&& taglock.get(Sympathy.LINK).target().equals(zombie.getUUID()) && taglock.get(Sympathy.LINK).strength() == Link.FRESH,
				"A taglock touched to a zombie holds a fresh link to it");
		helper.assertTrue(TaglockItem.bind(hexer, level, new ItemStack(Sympathy.TAGLOCK), hexer).equals("self"), "Never to oneself");
		ServerPlayer other = hexer(helper);
		Sympathy.ward(other, WardCategory.LINKING);
		String warded = TaglockItem.bind(hexer, level, new ItemStack(Sympathy.TAGLOCK), other);
		helper.assertTrue(warded.equals("warded") || warded.equals("not_allowed"), "A linking ward (or the rules) refuses a link: " + warded);
		ServerPlayer novice = helper.makeMockServerPlayerInLevel();
		RateGate.forget(novice.getUUID());
		helper.assertTrue(TaglockItem.bind(novice, level, new ItemStack(Sympathy.TAGLOCK), zombie).equals("unknown"), "Sympathy must be understood");
		helper.succeed();
	}

	/**
	 * A curse cast through a link takes its reagent and Focus, pulses its status through the shared boundary, is
	 * revalidated (a cursing ward skips it), is investigated (named, then traced) and lifted by its remedy.
	 */
	@GameTest(maxTicks = 20)
	public void cursesAreBoundedInvestigatedAndRemedied(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer hexer = hexer(helper);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		ItemStack taglock = new ItemStack(Sympathy.TAGLOCK);
		TaglockItem.bind(hexer, level, taglock, zombie);
		ItemStack cobweb = new ItemStack(Items.COBWEB, 2);
		helper.assertTrue(TaglockItem.curse(hexer, level, taglock, cobweb).isEmpty(), "The Curse of Lethargy is cast");
		helper.assertTrue(cobweb.getCount() == 1 && ConcordanceProgress.currentFocus(hexer) == 20 - Sympathy.catalog().curse("jugcraft:lethargy").focus(),
				"It took one cobweb and its Focus");
		helper.assertTrue(TaglockItem.curse(hexer, level, taglock, cobweb).equals("already"), "The same curse cannot lie twice");
		Sympathy.pulse(zombie, level);
		helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "Its first pulse slows the zombie, through the shared boundary");
		helper.assertTrue(TaglockItem.curse(hexer, level, taglock, new ItemStack(Items.DIRT)).equals("no_reagent"), "Dirt is no reagent");
		// A player cursed by another (where the rules allow it) investigates and remedies it.
		ServerPlayer victim = hexer(helper);
		Curse curse = new Curse("jugcraft:frailty", hexer.getUUID(), hexer.getName().getString(), level.getGameTime(), level.getGameTime() + 6000,
				level.getGameTime() - 400, 0);
		victim.setAttached(Sympathy.CURSES, List.of(curse));
		ScryingGlassItem.look(victim, level, ItemStack.EMPTY);
		helper.assertTrue(Sympathy.curses(victim).get(0).insight() == Curse.NAMED, "One look names the curse and its remedy");
		Sympathy.ward(hexer, WardCategory.SCRYING);
		ScryingGlassItem.look(victim, level, ItemStack.EMPTY);
		helper.assertTrue(Sympathy.curses(victim).get(0).insight() == Curse.NAMED, "The caster's scrying ward hides them");
		ItemStack remedy = new ItemStack(Items.BLAZE_POWDER, 2);
		ScryingGlassItem.look(victim, level, remedy);
		helper.assertTrue(Sympathy.curses(victim).isEmpty() && remedy.getCount() == 1, "Blaze powder lifts the Curse of Frailty, and is spent");
		helper.succeed();
	}

	/** A cursing ward skips a curse's pulses; a curse on a player is lifted when the rules no longer allow it. */
	@GameTest(maxTicks = 20)
	public void cursesAreRevalidatedAtEveryPulse(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer hexer = hexer(helper);
		ServerPlayer victim = hexer(helper);
		long now = level.getGameTime();
		victim.setAttached(Sympathy.CURSES, List.of(new Curse("jugcraft:lethargy", hexer.getUUID(), "Hexer", now, now + 6000, now - 400, 0)));
		Sympathy.ward(victim, WardCategory.CURSING);
		Sympathy.pulse(victim, level);
		helper.assertTrue(!victim.hasEffect(MobEffects.SLOWNESS), "A cursing ward skips the pulse");
		victim.removeAttached(Sympathy.WARDS);
		Sympathy.pulse(victim, level);
		boolean allowed = Sympathy.allowed(hexer, victim);
		helper.assertTrue(allowed ? victim.hasEffect(MobEffects.SLOWNESS) && !Sympathy.curses(victim).isEmpty()
				: !victim.hasEffect(MobEffects.SLOWNESS) && Sympathy.curses(victim).isEmpty(),
				"Unwarded, it pulses where the rules allow harm, and lifts where they do not (allowed: " + allowed + ")");
		helper.succeed();
	}

	/** A ward sigil wards its user for twenty minutes, and a moving ward stops the shared boundary's pushes. */
	@GameTest(maxTicks = 20)
	public void wardSigilsWardTheirCategory(GameTestHelper helper) {
		ServerPlayer player = hexer(helper);
		ItemStack sigil = new ItemStack(Sympathy.WARD_SIGIL, 2);
		sigil.set(Sympathy.WARD, "moving");
		helper.assertTrue(WardSigilItem.apply(player, sigil) && sigil.getCount() == 1 && Sympathy.warded(player, WardCategory.MOVING)
				&& !Sympathy.warded(player, WardCategory.CURSING), "The sigil wards against moving only, and is spent");
		helper.assertTrue(!WardSigilItem.apply(player, new ItemStack(Sympathy.WARD_SIGIL)), "A blank sigil wards against nothing");
		ServerPlayer pusher = hexer(helper);
		ServerPlayer bystander = hexer(helper);
		EffectSpec push = EffectSpec.of(EffectKind.MOVEMENT, Intent.HELPFUL, 10, 0);
		helper.assertTrue(ConcordanceEffects.apply(pushContext(helper, pusher), push, player) == ConcordanceEffects.Result.IMMUNE,
				"Another's push stops at the moving ward");
		helper.assertTrue(ConcordanceEffects.apply(pushContext(helper, pusher), push, bystander) == ConcordanceEffects.Result.APPLIED,
				"while the same push moves someone unwarded");
		helper.assertTrue(ConcordanceEffects.apply(pushContext(helper, player), push, player) == ConcordanceEffects.Result.APPLIED,
				"and the warded player's own push still moves them");
		helper.succeed();
	}

	private static ConcordanceEffects.Context pushContext(GameTestHelper helper, ServerPlayer actor) {
		Cause cause = Cause.of(actor.getUUID(), Cause.Origin.ITEM, "jugcraft:test", ConcordanceEffects.nextSerial());
		return new ConcordanceEffects.Context(helper.getLevel(), cause, actor, new Ledger(new Ledger.Limits(1, EffectKind.MOVEMENT.work, 0)), "push",
				actor.position().add(1.0, 0.0, 0.0));
	}

	/**
	 * A dream holds everything in one escrow and gives back exactly that: the same items in the same slots and the same
	 * experience; what was picked up in the dream falls at the dreamer's feet; caught wisps become dreamglass; ending
	 * twice does nothing; and a dream saved mid-way and recovered on joining gives everything back once.
	 */
	@GameTest(maxTicks = 40)
	public void dreamsNeverDuplicatePossessions(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer dreamer = hexer(helper);
		BlockPos censer = helper.absolutePos(new BlockPos(2, 1, 4));
		helper.setBlock(new BlockPos(2, 1, 4), Dreaming.CENSER);
		dreamer.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
		dreamer.getInventory().setItem(7, new ItemStack(Items.IRON_INGOT, 12));
		dreamer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		dreamer.experienceLevel = 5;
		helper.assertTrue(Dreaming.begin(dreamer, level, censer, false).equals("needs_night") && Dreaming.expedition(dreamer) == null
				&& dreamer.getInventory().getItem(0).getCount() == 3, "By day no dream begins and nothing is held");
		String reason = Dreaming.begin(dreamer, level, censer, true);
		helper.assertTrue(reason.isEmpty(), "At night the dream begins: " + reason);
		DreamExpedition expedition = Dreaming.expedition(dreamer);
		helper.assertTrue(expedition != null && expedition.items() == 3 + 12 + 1 && dreamer.getInventory().isEmpty()
				&& Dreaming.currentMode(dreamer) == GameType.ADVENTURE, "Everything is held in the escrow; the dreamer carries nothing, in adventure mode");
		// In the dream: pick something up, gain experience, catch a wisp.
		dreamer.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 5));
		dreamer.experienceLevel = 30;
		List<DreamWispEntity> wisps = level.getEntitiesOfClass(DreamWispEntity.class, dreamer.getBoundingBox().inflate(24),
				wisp -> dreamer.getUUID().equals(wisp.dreamer()));
		helper.assertTrue(wisps.size() == DreamRules.WISPS && Dreaming.catchWisp(dreamer, wisps.get(0)), "Three wisps gather; one is caught");
		// Saved mid-dream and read back: the one transaction is whole (what a crash would leave for the next join).
		DreamExpedition held = Dreaming.expedition(dreamer);
		DreamExpedition reread = DreamExpedition.CODEC.parse(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
				DreamExpedition.CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), held)
						.getOrThrow()).getOrThrow();
		helper.assertTrue(reread.items() == held.items() && reread.caught() == 1 && reread.escrow().size() == held.escrow().size(),
				"The expedition saves and reads back whole: " + reread.items() + " items, " + reread.caught() + " caught");
		Dreaming.end(dreamer, DreamRules.End.WOKE);
		helper.assertTrue(dreamer.getInventory().getItem(0).getCount() == 3 && dreamer.getInventory().getItem(0).is(Items.DIAMOND)
				&& dreamer.getInventory().getItem(7).getCount() == 12 && dreamer.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET),
				"Every item is back in its own slot");
		helper.assertTrue(dreamer.experienceLevel == 5 && Dreaming.currentMode(dreamer) == GameType.SURVIVAL && Dreaming.expedition(dreamer) == null,
				"The experience and game mode are as before; no expedition remains");
		helper.assertTrue(dreamer.getInventory().countItem(Items.COBBLESTONE) == 0 && dreamer.getInventory().countItem(Dreaming.DREAMGLASS) == 1,
				"What was picked up in the dream fell at the dreamer's feet; one dreamglass came back");
		Dreaming.end(dreamer, DreamRules.End.WOKE);
		helper.assertTrue(dreamer.getInventory().getItem(0).getCount() == 3 && dreamer.getInventory().countItem(Dreaming.DREAMGLASS) == 1,
				"Ending again does nothing: nothing is given twice");
		// Experience spent in a dream (an enchantment, a repair) stays spent: a dream never gives back what was used.
		ConcordanceProgress.setFocus(dreamer, 20);
		helper.assertTrue(Dreaming.begin(dreamer, level, censer, true).isEmpty(), "A second dream begins");
		dreamer.experienceLevel = 2;
		Dreaming.end(dreamer, DreamRules.End.WOKE);
		helper.assertTrue(dreamer.experienceLevel == 2 && dreamer.getInventory().getItem(0).getCount() == 3,
				"Spent experience stays spent: level " + dreamer.experienceLevel);
		helper.succeed();
	}

	/**
	 * More wisps gather as a dream goes on (eight at most), and the dream ends when the dreamer leaves the body's
	 * dimension's reach or its time is over.
	 */
	@GameTest(maxTicks = 20)
	public void wispsGatherAndDreamsEnd(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer dreamer = hexer(helper);
		BlockPos censer = helper.absolutePos(new BlockPos(2, 1, 4));
		helper.setBlock(new BlockPos(2, 1, 4), Dreaming.CENSER);
		dreamer.getInventory().setItem(4, new ItemStack(Items.BREAD, 6));
		helper.assertTrue(Dreaming.begin(dreamer, level, censer, true).isEmpty(), "The dream begins");
		DreamExpedition opened = Dreaming.expedition(dreamer);
		// Two wisp-intervals later, as the server's check sees it.
		DreamExpedition later = new DreamExpedition(opened.id(), opened.started() - 2L * DreamRules.WISP_TICKS, opened.until(), opened.dimension(),
				opened.body(), opened.yRot(), opened.xRot(), opened.gameMode(), opened.xpLevel(), opened.xpProgress(), opened.xpTotal(),
				opened.gathered(), opened.caught(), opened.escrow(), opened.censer());
		dreamer.setAttached(Dreaming.EXPEDITION, later);
		Dreaming.check(dreamer, later);
		int wisps = level.getEntitiesOfClass(DreamWispEntity.class, dreamer.getBoundingBox().inflate(24), wisp -> dreamer.getUUID().equals(wisp.dreamer()))
				.size();
		helper.assertTrue(Dreaming.expedition(dreamer).gathered() == DreamRules.WISPS + 2 && wisps == DreamRules.WISPS + 2,
				"Two more wisps have gathered: " + wisps);
		DreamExpedition over = new DreamExpedition(later.id(), later.started(), level.getGameTime(), later.dimension(), later.body(), later.yRot(),
				later.xRot(), later.gameMode(), later.xpLevel(), later.xpProgress(), later.xpTotal(), later.gathered(), later.caught(), later.escrow(),
				later.censer());
		dreamer.setAttached(Dreaming.EXPEDITION, over);
		Dreaming.check(dreamer, over);
		helper.assertTrue(!Dreaming.dreaming(dreamer) && dreamer.getInventory().getItem(4).getCount() == 6, "Its time over, the dream ends; the bread is back");
		helper.succeed();
	}

	/**
	 * A dreamer killed some way the death event did not see keeps their dream on the dead body (ending it there does
	 * nothing), and the respawned body gets the escrow back, once.
	 */
	@GameTest(maxTicks = 20)
	public void anUnseenDeathRecoversTheEscrowOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer dreamer = hexer(helper);
		BlockPos censer = helper.absolutePos(new BlockPos(2, 1, 4));
		dreamer.getInventory().setItem(2, new ItemStack(Items.GOLD_INGOT, 9));
		helper.assertTrue(Dreaming.begin(dreamer, level, censer, true).isEmpty(), "The dream begins");
		dreamer.setHealth(0.0F);
		Dreaming.end(dreamer, DreamRules.End.DISCONNECTED);
		helper.assertTrue(Dreaming.dreaming(dreamer) && dreamer.getInventory().isEmpty(), "Ending it on the dead body does nothing");
		ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
		respawned.setGameMode(GameType.ADVENTURE);
		helper.assertTrue(Dreaming.recover(dreamer, respawned) && respawned.getInventory().getItem(2).getCount() == 9
				&& Dreaming.currentMode(respawned) == GameType.SURVIVAL && !Dreaming.dreaming(dreamer), "The new body has the gold back, in survival");
		helper.assertTrue(!Dreaming.recover(dreamer, respawned) && respawned.getInventory().countItem(Items.GOLD_INGOT) == 9,
				"and recovering again gives nothing");
		dreamer.setHealth(dreamer.getMaxHealth());
		helper.succeed();
	}

	/** A dream cannot begin with something worn in an accessory slot, in creative mode, or without Focus. */
	@GameTest(maxTicks = 20)
	public void dreamsHaveExplicitEntryRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer dreamer = hexer(helper);
		BlockPos censer = helper.absolutePos(new BlockPos(2, 1, 4));
		dreamer.setGameMode(GameType.CREATIVE);
		String creative = Dreaming.begin(dreamer, level, censer, true);
		helper.assertTrue(creative.equals("mode"), "Creative players do not dream: " + creative);
		dreamer.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.setFocus(dreamer, 2);
		String tired = Dreaming.begin(dreamer, level, censer, true);
		helper.assertTrue(tired.equals("no_focus"), "Without Focus nothing begins: " + tired);
		helper.assertTrue(Dreaming.expedition(dreamer) == null, "and nothing is held");
		ConcordanceProgress.setFocus(dreamer, 20);
		var necklace = TrinketsApi.getAttachment(dreamer).getInventory("chest/necklace");
		helper.assertTrue(necklace != null, "Players have the necklace slot");
		necklace.setItem(0, new ItemStack(Items.STRING));
		String adorned = Dreaming.begin(dreamer, level, censer, true);
		helper.assertTrue(adorned.equals("accessories") && Dreaming.expedition(dreamer) == null,
				"Nothing worn in an accessory slot enters a dream (it is not held in the escrow): " + adorned);
		helper.succeed();
	}
}
