package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.CycleAudit;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.EquivalenceCatalog;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Material;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Transmutation;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicState;
import io.github.jimbozoomer.jugcraft.concordance.reliquary.Reliquary;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.smithy.Artificery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;

/**
 * Roadmap step 21, bounded material equivalence (docs/features/arcane-concordance-equivalence.md), on a real server: a
 * stack is weighed, then dissolved on a second use at its exact worth rounded down; forming costs more than dissolving
 * pays, so a round trip loses; only plain catalogued matter is weighed (named, filled, bound, enchanted, magical and
 * excluded things are refused with their reason, and nothing changes); the ledger never passes its cap; the declared
 * conversion graph is the game's own recipes; and the catalogue is balanced.
 */
public class ConcordanceAssayGameTests {
	private static ServerPlayer assayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Assaying.RESEARCH, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		Assaying.setBalance(player, 0L);
		return player;
	}

	private static void use(ServerPlayer player, ServerLevel level, ItemStack main, ItemStack other) {
		player.setItemInHand(InteractionHand.MAIN_HAND, main);
		player.setItemInHand(InteractionHand.OFF_HAND, other);
		Assaying.use(player, level, player.getMainHandItem(), player.getOffhandItem());
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	/** Nine iron nuggets are weighed (nothing changes), then dissolved on the second use into exactly one ingot's worth. */
	@GameTest(maxTicks = 20)
	public void aStackIsWeighedThenDissolved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = assayer(helper);
		ItemStack nuggets = new ItemStack(Items.IRON_NUGGET, 9);
		use(player, level, nuggets, ItemStack.EMPTY);
		helper.assertTrue(player.getMainHandItem().getCount() == 9 && Assaying.balance(player) == 0, "Weighing changes nothing");
		Assaying.use(player, level, player.getMainHandItem(), player.getOffhandItem());
		helper.assertTrue(player.getMainHandItem().isEmpty() && Assaying.balance(player) == 256,
				"A second use dissolves the nine at their exact worth: " + Assaying.balance(player));
		ItemStack one = new ItemStack(Items.IRON_NUGGET, 1);
		use(player, level, one, ItemStack.EMPTY);
		Assaying.use(player, level, player.getMainHandItem(), player.getOffhandItem());
		helper.assertTrue(Assaying.balance(player) == 256 + 28, "One nugget is worth 256/9 grains, rounded down to 28");
		helper.succeed();
	}

	/** Forming an ingot costs five quarters of its worth, rounded up; the pattern in the other hand is kept; a round trip loses. */
	@GameTest(maxTicks = 20)
	public void formingCostsMoreThanDissolvingPays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = assayer(helper);
		Assaying.setBalance(player, 1000L);
		ItemStack pattern = new ItemStack(Items.IRON_INGOT);
		use(player, level, ItemStack.EMPTY, pattern);
		// The formed ingot goes where the inventory puts it back: here it joins the pattern's stack in the other hand.
		helper.assertTrue(Assaying.balance(player) == 1000 - 320 && player.getOffhandItem() == pattern
				&& player.getInventory().countItem(Items.IRON_INGOT) == 2, "One ingot formed for 320 grains; the pattern kept: "
						+ Assaying.balance(player) + " grains, " + player.getInventory().countItem(Items.IRON_INGOT) + " ingots");
		use(player, level, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY);
		Assaying.use(player, level, player.getMainHandItem(), player.getOffhandItem());
		helper.assertTrue(Assaying.balance(player) == 1000 - 320 + 256, "Dissolved again it pays 256: the round trip lost 64");
		Assaying.setBalance(player, 100L);
		use(player, level, ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT));
		helper.assertTrue(Assaying.balance(player) == 100 && player.getInventory().countItem(Items.IRON_INGOT) == 1,
				"Too few grains: nothing is formed and nothing is taken");
		helper.succeed();
	}

	/** Only plain catalogued matter is weighed; everything else is refused with its reason, and nothing changes. */
	@GameTest(maxTicks = 20)
	public void onlyPlainCataloguedMatterIsWeighed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = assayer(helper);
		ItemStack named = new ItemStack(Items.IRON_INGOT, 4);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Heirloom"));
		ItemStack relicCharged = new ItemStack(Items.IRON_INGOT);
		relicCharged.set(Reliquary.RELIC, new RelicState(3, null, 0L));
		ItemStack enchanted = new ItemStack(Items.IRON_INGOT);
		enchanted.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		ItemStack rarer = new ItemStack(Items.IRON_INGOT);
		rarer.set(DataComponents.MAX_STACK_SIZE, 16);
		List<Object[]> cases = List.of(new Object[] {named, "unique"}, new Object[] {relicCharged, "magical"},
				new Object[] {enchanted, "magical"}, new Object[] {rarer, "metadata"}, new Object[] {new ItemStack(Items.DIAMOND, 3), "uncatalogued"},
				new Object[] {new ItemStack(Artificery.RESONANT_RING), "excluded"}, new Object[] {new ItemStack(item("jugcraft:research_notes")), "excluded"},
				new Object[] {new ItemStack(Items.ENCHANTED_BOOK), "excluded"});
		for (Object[] row : cases) {
			ItemStack stack = (ItemStack) row[0];
			String reason = Assaying.eligibility(stack);
			helper.assertTrue(reason.equals(row[1]), stack + " is refused as " + row[1] + ", not " + reason);
			int count = stack.getCount();
			use(player, level, stack, ItemStack.EMPTY);
			Assaying.use(player, level, player.getMainHandItem(), player.getOffhandItem());
			helper.assertTrue(player.getMainHandItem().getCount() == count && Assaying.balance(player) == 0, stack + ": nothing dissolved");
			Assaying.setBalance(player, 10_000L);
			use(player, level, ItemStack.EMPTY, stack.copy());
			helper.assertTrue(Assaying.balance(player) == 10_000L, stack + ": nothing is formed from it as a pattern");
			Assaying.setBalance(player, 0L);
		}
		helper.assertTrue(Assaying.eligibility(new ItemStack(Items.IRON_INGOT)).isEmpty(), "A plain ingot is weighed");
		helper.succeed();
	}

	/** The ledger never passes its cap: a stack too big for the room left is dissolved only in part. */
	@GameTest(maxTicks = 20)
	public void theLedgerNeverPassesItsCap(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = assayer(helper);
		Assaying.setBalance(player, Assay.MAX_BALANCE - 10);
		use(player, level, new ItemStack(Items.COBBLESTONE, 64), ItemStack.EMPTY);
		Assaying.use(player, level, player.getMainHandItem(), player.getOffhandItem());
		helper.assertTrue(Assaying.balance(player) == Assay.MAX_BALANCE && player.getMainHandItem().getCount() == 54,
				"Ten cobblestone fill it; fifty-four stay in hand: " + player.getMainHandItem().getCount());
		helper.succeed();
	}

	/** Every declared crafting and smelting conversion is the game's own recipe, with the same output and count. */
	@GameTest(maxTicks = 20)
	public void theDeclaredGraphIsTheGamesOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EquivalenceCatalog catalog = Assaying.catalog();
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		int crafted = 0;
		int smelted = 0;
		for (Transmutation conversion : catalog.transmutations().values()) {
			Map.Entry<String, Integer> output = conversion.outputs().entrySet().iterator().next();
			if (conversion.via().equals("crafting") && !conversion.pattern().isEmpty()) {
				int height = conversion.pattern().size();
				int width = conversion.pattern().stream().mapToInt(String::length).max().orElse(1);
				List<ItemStack> grid = new ArrayList<>();
				for (String row : conversion.pattern()) {
					for (int x = 0; x < width; x++) {
						String symbol = x < row.length() ? String.valueOf(row.charAt(x)) : " ";
						grid.add(symbol.equals(" ") ? ItemStack.EMPTY : new ItemStack(item(conversion.key().get(symbol))));
					}
				}
				CraftingInput input = CraftingInput.of(width, height, grid);
				Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, level);
				helper.assertTrue(recipe.isPresent(), conversion.id() + ": the game has no such crafting recipe");
				ItemStack made = recipe.get().value().assemble(input);
				helper.assertTrue(made.is(item(output.getKey())) && made.getCount() == output.getValue(),
						conversion.id() + ": the game makes " + made + ", not " + output.getValue() + " " + output.getKey());
				crafted++;
			} else if (conversion.via().equals("smelting")) {
				String smeltable = conversion.inputs().keySet().stream().filter(id -> !id.equals("minecraft:coal")).findFirst().orElseThrow();
				SingleRecipeInput single = new SingleRecipeInput(new ItemStack(item(smeltable)));
				ItemStack made = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, single, level)
						.orElseThrow(() -> helper.assertionException(conversion.id() + ": a furnace does not smelt " + smeltable)).value().assemble(single);
				helper.assertTrue(made.is(item(output.getKey())) && made.getCount() * conversion.inputs().get(smeltable) == output.getValue(),
						conversion.id() + ": a furnace makes " + made);
				smelted++;
			}
		}
		helper.assertTrue(crafted >= 20 && smelted == 2, "Checked " + crafted + " crafting and " + smelted + " smelting conversions");
		helper.succeed();
	}

	/** The catalogue is balanced: no problems, no finding, no catalogued item that is excluded or by nature not matter. */
	@GameTest(maxTicks = 20)
	public void theCatalogueIsBalanced(GameTestHelper helper) {
		EquivalenceCatalog catalog = Assaying.catalog();
		helper.assertTrue(catalog.materials().size() >= 40, "The catalogue loaded: " + catalog.materials().size());
		helper.assertTrue(Assaying.problems(helper.getLevel()).isEmpty(), "The scale is in balance, the live recipes included: "
				+ Assaying.problems(helper.getLevel()));
		helper.assertTrue(Assaying.probe(helper.getLevel(), catalog).isEmpty(), "No live recipe gains: " + Assaying.probe(helper.getLevel(), catalog));
		helper.assertTrue(CycleAudit.audit(catalog).isEmpty(), "The audit finds nothing: " + CycleAudit.audit(catalog));
		for (Material material : catalog.materials().values()) {
			ItemStack plain = new ItemStack(item(material.item()));
			helper.assertTrue(!plain.isEmpty() && Assaying.eligibility(plain).isEmpty(), material.item() + " is a real, plain, weighable item");
		}
		helper.succeed();
	}
}
