package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Artifice;
import io.github.jimbozoomer.jugcraft.concordance.artifice.ArtificeCatalog;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Forge;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Quality;
import io.github.jimbozoomer.jugcraft.concordance.artifice.RolledAffix;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Stat;
import io.github.jimbozoomer.jugcraft.concordance.artifice.Substrate;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.smithy.ArtificerBenchBlock;
import io.github.jimbozoomer.jugcraft.concordance.smithy.Artificery;
import io.github.jimbozoomer.jugcraft.concordance.smithy.ResonantRingItem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Roadmap step 19, equipment construction and enhancement (docs/features/arcane-concordance-artifice.md), on a real
 * server: forging saves the roll on the ring before it is shown; reforging never rerolls by looking; gems, runes and
 * bonds keep their rules and the ring's modifiers follow them; salvage needs confirming and never gains; repair mends
 * wear; and every statistic the data names is a real attribute.
 */
public class ConcordanceArtificeGameTests {
	private static ServerPlayer smith(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Artificery.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(player, 20);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static void use(ServerPlayer player, ServerLevel level, ItemStack main, ItemStack other) {
		player.setItemInHand(InteractionHand.MAIN_HAND, main);
		player.setItemInHand(InteractionHand.OFF_HAND, other);
		ArtificerBenchBlock.use(player, level, player.getMainHandItem(), player.getOffhandItem());
	}

	private static ItemStack ringOf(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(Artificery.RESONANT_RING)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack ring(Artifice artifice, Substrate substrate) {
		ItemStack ring = new ItemStack(Artificery.RESONANT_RING);
		ring.set(Artificery.ARTIFICE, artifice);
		ring.set(DataComponents.MAX_DAMAGE, substrate.durability());
		ring.set(DataComponents.DAMAGE, 0);
		return ring;
	}

	private static List<String> modifiers(ItemStack ring, ServerPlayer wearer) {
		List<String> seen = new ArrayList<>();
		((ResonantRingItem) ring.getItem()).forEachTrinketModifier(ring, null, wearer, Identifier.parse("trinkets:hand/ring/0"),
				(attribute, modifier) -> seen.add(attribute.unwrapKey().map(key -> key.identifier().toString()).orElse("?") + "=" + modifier.amount()));
		return seen;
	}

	/** Four gold ingots and 8 Focus forge a ring whose quality and affixes were rolled once and saved on it. */
	@GameTest(maxTicks = 20)
	public void aRingIsForgedAndSavedBeforeItIsShown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer smith = smith(helper);
		ArtificeCatalog catalog = Artificery.catalog();
		Substrate gold = catalog.substrate("jugcraft:gold");
		use(smith, level, new ItemStack(Items.GOLD_INGOT, 5), ItemStack.EMPTY);
		ItemStack ring = ringOf(smith);
		Artifice artifice = ring.get(Artificery.ARTIFICE);
		helper.assertTrue(artifice != null && artifice.substrate().equals("jugcraft:gold"), "A gold ring is forged");
		helper.assertTrue(smith.getInventory().countItem(Items.GOLD_INGOT) == 1 && ConcordanceProgress.currentFocus(smith) == 20 - Artificery.FORGE_FOCUS,
				"It cost four ingots and the forging's Focus");
		helper.assertTrue(artifice.equals(Forge.forge(gold, catalog, artifice.seed())), "Its roll is the one its saved seed gives: nothing rolls again");
		helper.assertTrue(ring.getMaxDamage() == gold.durability() && ring.getDamageValue() == 0, "with its substrate's durability");
		helper.assertTrue(Forge.used(artifice, catalog) <= Forge.capacity(artifice, gold), "within its capacity");
		helper.succeed();
	}

	/** Inspecting changes nothing; reforging gives exactly the roll its seed and count give, keeping the rest. */
	@GameTest(maxTicks = 20)
	public void reforgingNeverRerolls(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer smith = smith(helper);
		ArtificeCatalog catalog = Artificery.catalog();
		Substrate gold = catalog.substrate("jugcraft:gold");
		Artifice start = new Artifice("jugcraft:gold", Quality.FINE, 1919L, 0, List.of(), List.of("jugcraft:radiance"), List.of("jugcraft:emerald"), null);
		ItemStack ring = ring(start, gold);
		use(smith, level, ring, ItemStack.EMPTY);
		helper.assertTrue(ring.get(Artificery.ARTIFICE).equals(start), "Inspecting it changes nothing");
		Artifice expected = Forge.reforge(start, gold, catalog).artifice();
		use(smith, level, ring, new ItemStack(Items.REDSTONE, 2));
		Artifice after = ring.get(Artificery.ARTIFICE);
		helper.assertTrue(after.equals(expected), "The reforge is the roll its seed and count give: " + after);
		helper.assertTrue(after.runes().equals(start.runes()) && after.gems().equals(start.gems()) && after.quality() == start.quality(),
				"It keeps quality, runes and gems");
		helper.assertTrue(smith.getOffhandItem().getCount() == 1 && ConcordanceProgress.currentFocus(smith) == 20 - Artificery.REFORGE_FOCUS,
				"It cost one redstone and its Focus");
		helper.succeed();
	}

	/** A gem is socketed and comes back whole; a rune is inscribed; a bond serves its player alone; a dull ring gives nothing. */
	@GameTest(maxTicks = 20)
	public void gemsRunesAndBondsKeepTheirRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer smith = smith(helper);
		ServerPlayer stranger = smith(helper);
		ArtificeCatalog catalog = Artificery.catalog();
		Substrate iron = catalog.substrate("jugcraft:iron");
		ItemStack ring = ring(new Artifice("jugcraft:iron", Quality.SOUND, 7L, 0, List.of(new RolledAffix("jugcraft:fortune", 1.0)), List.of(), List.of(), null), iron);
		use(smith, level, ring, new ItemStack(Items.DIAMOND, 1));
		helper.assertTrue(ring.get(Artificery.ARTIFICE).gems().equals(List.of("jugcraft:diamond")) && smith.getOffhandItem().isEmpty(), "A diamond is set");
		use(smith, level, ring, new ItemStack(Items.LAPIS_LAZULI, 1));
		helper.assertTrue(ring.get(Artificery.ARTIFICE).gems().size() == 1 && smith.getOffhandItem().getCount() == 1, "Iron has one socket: the lapis is not taken");
		use(smith, level, ring, new ItemStack(Items.SHEARS));
		helper.assertTrue(ring.get(Artificery.ARTIFICE).gems().isEmpty() && smith.getInventory().countItem(Items.DIAMOND) == 1, "Shears give the diamond back whole");
		use(smith, level, ring, new ItemStack(Items.GOLDEN_CARROT, 1));
		helper.assertTrue(ring.get(Artificery.ARTIFICE).runes().equals(List.of("jugcraft:vigor")), "A golden carrot inscribes the Rune of Vigor");
		use(smith, level, ring, new ItemStack(Items.LEAD));
		helper.assertTrue(smith.getUUID().equals(ring.get(Artificery.ARTIFICE).bond()), "A lead bonds it to its smith");
		helper.assertTrue(!modifiers(ring, smith).isEmpty() && modifiers(ring, stranger).isEmpty(), "It gives its modifiers to its bond alone: " + modifiers(ring, smith));
		use(stranger, level, ring, new ItemStack(Items.LEAD));
		helper.assertTrue(smith.getUUID().equals(ring.get(Artificery.ARTIFICE).bond()), "No one else can unbond it");
		helper.assertTrue(modifiers(ring, smith).size() == Forge.stats(ring.get(Artificery.ARTIFICE), catalog).size(),
				"one modifier for each of its stats");
		ring.setDamageValue(ring.getMaxDamage() - 1);
		helper.assertTrue(Artificery.dull(ring) && modifiers(ring, smith).isEmpty(), "Dull, it gives nothing, and it is not broken");
		helper.succeed();
	}

	/** Salvage waits for a second use; then the ring goes and its substrate's share and its gems come back, never more. */
	@GameTest(maxTicks = 20)
	public void salvageNeedsConfirmingAndNeverGains(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer smith = smith(helper);
		Substrate gold = Artificery.catalog().substrate("jugcraft:gold");
		ItemStack ring = ring(new Artifice("jugcraft:gold", Quality.SOUND, 3L, 0, List.of(), List.of(), List.of("jugcraft:emerald"), null), gold);
		use(smith, level, ring, new ItemStack(Items.FLINT));
		helper.assertTrue(!ring.isEmpty() && smith.getInventory().countItem(Items.GOLD_INGOT) == 0, "The first use only says what salvage would do");
		use(smith, level, ring, new ItemStack(Items.FLINT));
		helper.assertTrue(ring.isEmpty() && smith.getInventory().countItem(Items.GOLD_INGOT) == gold.salvage()
				&& smith.getInventory().countItem(Items.EMERALD) == 1, "The second salvages: " + gold.salvage() + " gold and the emerald back");
		helper.assertTrue(gold.salvage() < gold.cost(), "never as much as forging cost");
		helper.succeed();
	}

	/** Repair takes as few of its substrate's ingots as mend its wear. */
	@GameTest(maxTicks = 20)
	public void repairMendsWear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer smith = smith(helper);
		Substrate copper = Artificery.catalog().substrate("jugcraft:copper");
		ItemStack ring = ring(new Artifice("jugcraft:copper", Quality.SOUND, 5L, 0, List.of(), List.of(), List.of(), null), copper);
		ring.setDamageValue(copper.repair() + 1);
		use(smith, level, ring, new ItemStack(Items.COPPER_INGOT, 5));
		helper.assertTrue(ring.getDamageValue() == 0 && smith.getOffhandItem().getCount() == 3, "Two ingots mend it: " + ring.getDamageValue());
		use(smith, level, ring, new ItemStack(Items.COPPER_INGOT, 5));
		helper.assertTrue(smith.getOffhandItem().getCount() == 5, "Unworn, nothing is taken");
		helper.succeed();
	}

	/** Every statistic the substrates, gems, runes and affixes name is a registered attribute (Spell Power's included). */
	@GameTest(maxTicks = 20)
	public void everyStatisticIsARealAttribute(GameTestHelper helper) {
		ArtificeCatalog catalog = Artificery.catalog();
		List<String> named = new ArrayList<>();
		catalog.affixes().values().forEach(affix -> named.add(affix.attribute()));
		catalog.gems().values().forEach(gem -> named.add(gem.stat().attribute()));
		catalog.runes().values().forEach(rune -> named.add(rune.stat().attribute()));
		for (String attribute : named) {
			helper.assertTrue(BuiltInRegistries.ATTRIBUTE.containsKey(Identifier.parse(attribute)), "Unknown attribute " + attribute);
		}
		for (Substrate substrate : catalog.substrates().values()) {
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(substrate.item()));
			helper.assertTrue(item != Items.AIR, "Unknown substrate item " + substrate.item());
		}
		helper.assertTrue(!named.isEmpty() && named.contains("spell_power:arcane"), "Spell Power's arcane power is among them");
		helper.succeed();
	}
}
