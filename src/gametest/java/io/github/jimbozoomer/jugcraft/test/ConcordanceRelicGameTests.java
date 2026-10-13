package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.relic.Context;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicState;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.ReliquaryShrineBlock;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.ReliquaryShrineBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Roadmap step 20, relics and shrines (docs/features/arcane-concordance-relics.md), on a real server: a relic works only
 * in its supported contexts and says why it cannot elsewhere (held in the wrong hand, carried loose, worn for show, out
 * of charge, under a roof, unhurt or lately hurt); relics are found in real Trinkets slots; a Reliquary Shrine installs
 * only what can be installed, works it, charges it from a Ley Pylon, serves its owner's party alone and keeps its relic
 * through a save; one player's relics share a budget and two relics giving the same effect never add up; and a bound
 * relic serves only its player.
 */
public class ConcordanceRelicGameTests {
	private static final BlockPos STAND = new BlockPos(2, 2, 2);
	private static final BlockPos FOE = new BlockPos(5, 2, 5);

	private static ServerPlayer keeper(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(STAND);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Reliquary.RESEARCH, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static ItemStack relic(String id, int charge) {
		ItemStack stack = new ItemStack(Reliquary.ITEMS.get(id));
		stack.set(Reliquary.RELIC, new RelicState(charge, null, RelicState.FRESH.lastPulse()));
		return stack;
	}

	private static Reliquary.Outcome work(ServerPlayer player, Context context, ItemStack stack) {
		return Reliquary.work(player, new Reliquary.Found(context, context.id, stack), true);
	}

	private static Mob foe(GameTestHelper helper) {
		return helper.spawnWithNoFreeWill(EntityTypes.SPIDER, FOE);
	}

	private static ReliquaryShrineBlockEntity shrine(GameTestHelper helper, BlockPos pos, ServerPlayer owner) {
		helper.setBlock(pos, Reliquary.SHRINE);
		ReliquaryShrineBlockEntity shrine = (ReliquaryShrineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
		shrine.setOwner(owner.getUUID());
		return shrine;
	}

	private static LeyPylonBlockEntity pylon(GameTestHelper helper, BlockPos pos, ServerPlayer owner, int ley) {
		helper.setBlock(pos, JugcraftConcordance.LEY_PYLON);
		LeyPylonBlockEntity pylon = (LeyPylonBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
		pylon.setOwner(owner.getUUID());
		pylon.setLey(helper.getLevel(), ley);
		return pylon;
	}

	/** The Wardlight works held in the off hand only: in the main hand or carried loose it does nothing and says so. */
	@GameTest(maxTicks = 20)
	public void aRelicWorksOnlyInItsContexts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = keeper(helper);
		Mob spider = foe(helper);
		ItemStack wardlight = relic("wardlight", 10);
		Reliquary.Outcome main = work(player, Context.MAIN_HAND, wardlight);
		helper.assertTrue(!main.pulsed() && main.reason().equals("wrong_context") && !spider.hasEffect(MobEffects.GLOWING)
				&& Reliquary.state(wardlight).charge() == 10, "In the main hand it does nothing, spends nothing, and says it is the wrong place");
		helper.assertTrue(main.relic() != null && main.relic().contexts().equals(List.of(Context.OFF_HAND, Context.INSTALLED)),
				"and where it does work: the off hand or installed");
		Reliquary.Outcome off = work(player, Context.OFF_HAND, wardlight);
		helper.assertTrue(off.pulsed() && spider.hasEffect(MobEffects.GLOWING) && Reliquary.state(wardlight).charge() == 9
				&& Reliquary.state(wardlight).lastPulse() == level.getGameTime(), "In the off hand it reveals the spider and spends one charge: " + off.reason());
		helper.assertTrue(work(player, Context.OFF_HAND, wardlight).reason().equals("resting"), "Straight after, it rests until its interval");
		helper.assertTrue(work(player, Context.INVENTORY, relic("wardlight", 10)).reason().equals("wrong_context"), "Carried loose it does nothing");
		helper.assertTrue(work(player, Context.COSMETIC, relic("hearthstone", 10)).reason().equals("wrong_context"),
				"Worn for show in a cosmetic slot, a hearthstone does nothing");
		// Where each relic is: the hands and armour by their slots, the rest of the inventory as loose.
		player.setItemInHand(InteractionHand.OFF_HAND, wardlight);
		ItemStack storm = relic("stormglass", 5);
		player.getInventory().setItem(9, storm);
		ItemStack circlet = relic("owlsight_circlet", 5);
		player.setItemSlot(EquipmentSlot.HEAD, circlet);
		List<Reliquary.Found> found = Reliquary.find(player);
		helper.assertTrue(found.size() == 3 && found.get(0).context() == Context.OFF_HAND && found.get(0).stack() == wardlight
				&& found.get(1).context() == Context.HEAD && found.get(1).stack() == circlet
				&& found.get(2).context() == Context.INVENTORY && found.get(2).stack() == storm, "Each relic is found where it is, once: " + found);
		// The command's report never pulses.
		List<Reliquary.Outcome> report = Reliquary.check(player, false);
		helper.assertTrue(report.size() == 3 && report.get(2).reason().equals("wrong_context") && Reliquary.state(storm).charge() == 5
				&& Reliquary.state(wardlight).charge() == 9, "The report says why the loose orb does nothing, and spends nothing");
		helper.succeed();
	}

	/** A relic says exactly why it cannot activate: no charge, a roof, unhurt, lately hurt; and works once it can. */
	@GameTest(maxTicks = 20)
	public void aRelicSaysWhyItCannotActivate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = keeper(helper);
		foe(helper);
		helper.assertTrue(work(player, Context.OFF_HAND, relic("wardlight", 0)).reason().equals("no_charge"), "Out of charge it says so");
		BlockPos roof = STAND.above(3);
		helper.setBlock(roof, Blocks.STONE);
		ItemStack storm = relic("stormglass", 5);
		helper.assertTrue(work(player, Context.MAIN_HAND, storm).reason().equals("needs_sky"), "Under a roof the Stormglass needs the open sky");
		helper.setBlock(roof, Blocks.AIR);
		Reliquary.Outcome open = work(player, Context.MAIN_HAND, storm);
		boolean sky = Reliquary.openSky(level, helper.absolutePos(STAND).above());
		helper.assertTrue(sky ? open.pulsed() && player.hasEffect(MobEffects.SPEED) : open.reason().equals("needs_sky"),
				"Under the open sky it quickens its holder: " + open.reason());
		ItemStack circlet = relic("owlsight_circlet", 5);
		Reliquary.Outcome night = work(player, Context.HEAD, circlet);
		helper.assertTrue(Reliquary.night(level) ? night.pulsed() : night.reason().equals("needs_night"), "The circlet works only in the dark: " + night.reason());
		// Another player (the pulses above count against this one's budget).
		ServerPlayer healer = keeper(helper);
		ItemStack hearthstone = relic("hearthstone", 10);
		helper.assertTrue(work(healer, Context.TRINKET, hearthstone).reason().equals("not_wounded"), "Unhurt, the Hearthstone has nothing to mend");
		healer.setHealth(10.0F);
		Reliquary.hurt(healer.getUUID(), level.getGameTime());
		helper.assertTrue(work(healer, Context.TRINKET, hearthstone).reason().equals("not_calm"), "Just hurt, it waits for ten calm seconds");
		Reliquary.hurt(healer.getUUID(), level.getGameTime() - 400);
		Reliquary.Outcome mend = work(healer, Context.TRINKET, hearthstone);
		helper.assertTrue(mend.pulsed() && healer.hasEffect(MobEffects.REGENERATION) && Reliquary.state(hearthstone).charge() == 8,
				"Calm and hurt, it mends for two charge: " + mend.reason());
		helper.assertTrue(healer.getUUID().equals(Reliquary.state(hearthstone).owner()), "and binds to the first player it serves");
		helper.succeed();
	}

	/** Relics are found in real Trinkets slots: the necklace slot Jugcraft gives players holds a working Hearthstone. */
	@GameTest(maxTicks = 20)
	public void relicsAreFoundInTrinketSlots(GameTestHelper helper) {
		ServerPlayer player = keeper(helper);
		var necklace = TrinketsApi.getAttachment(player).getInventory("chest/necklace");
		helper.assertTrue(necklace != null, "Players have the necklace slot");
		ItemStack hearthstone = relic("hearthstone", 10);
		necklace.setItem(0, hearthstone);
		List<Reliquary.Found> found = Reliquary.find(player);
		helper.assertTrue(found.size() == 1 && found.get(0).context() == Context.TRINKET && found.get(0).slot().equals("chest/necklace"),
				"Worn as a necklace, it is in its trinket context: " + found);
		player.setHealth(10.0F);
		Reliquary.hurt(player.getUUID(), helper.getLevel().getGameTime() - 400);
		List<Reliquary.Outcome> outcomes = Reliquary.check(player, true);
		helper.assertTrue(outcomes.size() == 1 && outcomes.get(0).pulsed() && player.hasEffect(MobEffects.REGENERATION),
				"and the server's check works it there: " + outcomes);
		helper.succeed();
	}

	/**
	 * A Reliquary Shrine installs a Wardlight, charges it from the Ley Pylon beside it and works it; recharges a relic it
	 * cannot install; lets only its owner's party take it back; and keeps its relic and owner through a save.
	 */
	@GameTest(maxTicks = 20)
	public void aShrineWorksItsInstalledRelic(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = keeper(helper);
		ServerPlayer stranger = keeper(helper);
		Mob spider = foe(helper);
		BlockPos at = new BlockPos(2, 1, 4);
		ReliquaryShrineBlockEntity shrine = shrine(helper, at, owner);
		LeyPylonBlockEntity pylon = pylon(helper, at.east(), owner, 10);
		owner.setItemInHand(InteractionHand.MAIN_HAND, relic("wardlight", 0));
		shrine.useWith(owner, level, owner.getMainHandItem());
		helper.assertTrue(owner.getMainHandItem().isEmpty() && !shrine.relic().isEmpty()
				&& level.getBlockState(helper.absolutePos(at)).getValue(ReliquaryShrineBlock.LIT), "The Wardlight is installed and the shrine lights");
		helper.assertTrue(shrine.status().equals("working") && spider.hasEffect(MobEffects.GLOWING), "It works at once: the spider glows");
		helper.assertTrue(pylon.ley() == 8 && Reliquary.state(shrine.relic()).charge() == 2 * Reliquary.CHARGE_PER_LEY - 1,
				"It drew two Ley Charge (eight charge) and spent one: " + Reliquary.state(shrine.relic()).charge());
		// The Stormglass cannot be installed: used on the full shrine it is recharged from the pylon instead.
		ItemStack storm = relic("stormglass", 0);
		owner.setItemInHand(InteractionHand.MAIN_HAND, storm);
		shrine.useWith(owner, level, storm);
		helper.assertTrue(owner.getMainHandItem() == storm && Reliquary.state(storm).charge() == 32 && pylon.ley() == 0,
				"The Stormglass stays in hand, recharged with all eight Ley Charge left: " + Reliquary.state(storm).charge());
		// Saved and loaded, it keeps its relic and owner.
		CompoundTag saved = shrine.saveWithoutMetadata(level.registryAccess());
		ReliquaryShrineBlockEntity copy = new ReliquaryShrineBlockEntity(shrine.getBlockPos(), shrine.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.relic().is(Reliquary.ITEMS.get("wardlight")) && Reliquary.state(copy.relic()).equals(Reliquary.state(shrine.relic()))
				&& owner.getUUID().equals(copy.owner()), "A save keeps its relic, its charge and its owner");
		// A stranger cannot take it; its owner can.
		shrine.useEmpty(stranger, level);
		helper.assertTrue(!shrine.relic().isEmpty() && stranger.getInventory().countItem(Reliquary.ITEMS.get("wardlight")) == 0,
				"A stranger is only told what it does");
		owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		shrine.useEmpty(owner, level);
		helper.assertTrue(shrine.relic().isEmpty() && owner.getInventory().countItem(Reliquary.ITEMS.get("wardlight")) == 1
				&& !level.getBlockState(helper.absolutePos(at)).getValue(ReliquaryShrineBlock.LIT) && shrine.status().equals("empty"),
				"Its owner takes it back and the shrine goes dark");
		helper.succeed();
	}

	/** A shrine refuses to install what cannot be installed, and anything from someone who has not understood Relic Lore. */
	@GameTest(maxTicks = 20)
	public void aShrineInstallsOnlyWhatCanBeInstalled(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = keeper(helper);
		BlockPos at = new BlockPos(2, 1, 4);
		ReliquaryShrineBlockEntity shrine = shrine(helper, at, owner);
		ItemStack storm = relic("stormglass", 3);
		owner.setItemInHand(InteractionHand.MAIN_HAND, storm);
		shrine.useWith(owner, level, storm);
		helper.assertTrue(shrine.relic().isEmpty() && owner.getMainHandItem() == storm && Reliquary.state(storm).charge() == 3,
				"The Stormglass is not installed (and with no pylon, not recharged)");
		helper.assertTrue(Reliquary.work(owner, new Reliquary.Found(Context.INSTALLED, "shrine", storm), false).reason().equals("cannot_install"),
				"Installed is not one of its contexts, and it says so");
		ServerPlayer novice = helper.makeMockServerPlayerInLevel();
		RateGate.forget(novice.getUUID());
		ItemStack wardlight = relic("wardlight", 5);
		novice.setItemInHand(InteractionHand.MAIN_HAND, wardlight);
		shrine.useWith(novice, level, wardlight);
		helper.assertTrue(shrine.relic().isEmpty() && novice.getMainHandItem() == wardlight, "Without Relic Lore nothing is installed");
		helper.succeed();
	}

	/**
	 * One player's relics pulse at most twice a second together, and a relic held back is not spent; two relics giving
	 * the same effect never add up: the second keeps its charge.
	 */
	@GameTest(maxTicks = 20)
	public void relicsShareABudgetAndNeverAddUp(GameTestHelper helper) {
		ServerPlayer player = keeper(helper);
		Mob spider = foe(helper);
		ItemStack first = relic("wardlight", 10);
		ItemStack second = relic("wardlight", 10);
		ItemStack third = relic("wardlight", 10);
		helper.assertTrue(work(player, Context.OFF_HAND, first).pulsed(), "The first pulses");
		spider.removeAllEffects();
		helper.assertTrue(work(player, Context.OFF_HAND, second).pulsed(), "The second pulses");
		spider.removeAllEffects();
		Reliquary.Outcome held = work(player, Context.OFF_HAND, third);
		helper.assertTrue(held.reason().equals("budget") && Reliquary.state(third).charge() == 10 && !spider.hasEffect(MobEffects.GLOWING),
				"The third waits for the next second and is not spent: " + held.reason());
		ServerPlayer other = keeper(helper);
		ItemStack a = relic("wardlight", 10);
		ItemStack b = relic("wardlight", 10);
		helper.assertTrue(work(other, Context.OFF_HAND, a).pulsed(), "Another player has their own budget");
		Reliquary.Outcome kept = work(other, Context.OFF_HAND, b);
		helper.assertTrue(kept.reason().equals("kept") && Reliquary.state(b).charge() == 10,
				"A second Wardlight changes nothing the first gave, so it spends nothing: " + kept.reason());
		helper.succeed();
	}

	/** A Hearthstone bound to one player does nothing for another, worn or installed in their shrine. */
	@GameTest(maxTicks = 20)
	public void aBoundRelicServesOnlyItsPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer first = keeper(helper);
		ServerPlayer second = keeper(helper);
		ItemStack hearthstone = relic("hearthstone", 20);
		first.setHealth(10.0F);
		second.setHealth(10.0F);
		Reliquary.hurt(first.getUUID(), level.getGameTime() - 400);
		Reliquary.hurt(second.getUUID(), level.getGameTime() - 400);
		helper.assertTrue(work(first, Context.TRINKET, hearthstone).pulsed() && first.getUUID().equals(Reliquary.state(hearthstone).owner()),
				"It binds to the first who wears it");
		Reliquary.Outcome refused = work(second, Context.TRINKET, hearthstone);
		helper.assertTrue(refused.reason().equals("not_owner") && !second.hasEffect(MobEffects.REGENERATION), "It refuses anyone else");
		ReliquaryShrineBlockEntity theirs = shrine(helper, new BlockPos(2, 1, 4), second);
		theirs.setRelic(level, hearthstone.copy());
		helper.assertTrue(theirs.status().equals("not_owner"), "Installed in someone else's shrine it says it is bound to another: " + theirs.status());
		helper.succeed();
	}
}
