package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OfrendaBlock;
import io.github.jimbozoomer.jugcraft.agriculture.OfrendaBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the ofrenda: offerings are set out one at a time (not a stick, not a seventh) and taken back from the
 * last; one of each of the five kinds makes it complete, and it glows; complete, it welcomes a restless spirit near (who
 * makes it its home, shows itself and stays calm), and a spirit arrived at it earns Remembered for the players near;
 * incomplete, it stays dark; and the data loads.
 */
public class OfrendaGameTests {
	private static final BlockPos OFRENDA = new BlockPos(3, 2, 3);

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Item vanilla(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static OfrendaBlockEntity ofrenda(GameTestHelper helper) {
		helper.setBlock(OFRENDA, JugcraftAgriculture.block("ofrenda").defaultBlockState().setValue(OfrendaBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(OFRENDA, OfrendaBlockEntity.class);
	}

	/** {@code player} uses what they hold (or an empty hand) on the ofrenda. */
	private static void use(GameTestHelper helper, ServerPlayer player, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos absolute = helper.absolutePos(OFRENDA);
		player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
	}

	/** The five kinds: a marigold, a candle, pan de muerto, a sugar skull and a honey bottle. */
	private static List<ItemStack> complete() {
		return List.of(new ItemStack(item("marigold")), new ItemStack(vanilla("candle")), new ItemStack(item("pan_de_muerto")),
				new ItemStack(item("sugar_skull")), new ItemStack(vanilla("honey_bottle")));
	}

	/** A stick isn't an offering; a marigold is, one at a time; six fit, not seven; an empty hand takes back the last. */
	@GameTest
	public void offeringsAreSetOutAndTakenBack(GameTestHelper helper) {
		OfrendaBlockEntity ofrenda = ofrenda(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1));
		ItemStack sticks = new ItemStack(vanilla("stick"), 4);
		use(helper, player, sticks);
		helper.assertTrue(ofrenda.offerings().isEmpty() && sticks.getCount() == 4, "A stick isn't an offering");
		ItemStack marigolds = new ItemStack(item("marigold"), 8);
		for (int i = 0; i < OfrendaBlockEntity.SLOTS + 1; i++) {
			use(helper, player, marigolds);
		}
		helper.assertTrue(ofrenda.offerings().size() == OfrendaBlockEntity.SLOTS && marigolds.getCount() == 8 - OfrendaBlockEntity.SLOTS,
				"Six marigolds set out one at a time, and no seventh");
		use(helper, player, ItemStack.EMPTY);
		helper.assertTrue(ofrenda.offerings().size() == OfrendaBlockEntity.SLOTS - 1 && player.getInventory().countItem(item("marigold")) >= 1,
				"An empty hand takes back the last");
		helper.succeed();
	}

	/**
	 * One of each kind makes it complete, and it glows; it welcomes a spirit near, which makes it its home, shows itself and
	 * stays calm; once a spirit has come to it, a player near earns Remembered.
	 */
	@GameTest(maxTicks = 100)
	public void aCompleteOfrendaWelcomesSpirits(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		OfrendaBlockEntity ofrenda = ofrenda(helper);
		for (ItemStack offering : complete()) {
			helper.assertTrue(ofrenda.offer(offering), "Offered " + offering);
		}
		helper.assertTrue(ofrenda.complete(), "One of each kind: complete");
		ServerPlayer witness = player(helper, new BlockPos(3, 2, 0));
		RestlessSpirit spirit = helper.spawnWithNoFreeWill(JugcraftAgriculture.RESTLESS_SPIRIT, new BlockPos(3, 3, 7));
		spirit.setNoAi(true);
		spirit.setPersistenceRequired();
		spirit.setHome(helper.absolutePos(new BlockPos(3, 2, 7)));
		helper.assertTrue(ofrenda.welcome(level) >= 1 && spirit.welcomed() && spirit.home().equals(helper.absolutePos(OFRENDA)) && spirit.revealed(),
				"The spirit is welcomed: the ofrenda is its home, and it shows");
		helper.assertTrue(!earned(witness, "remembered"), "Not yet arrived");
		BlockPos at = helper.absolutePos(OFRENDA.above());
		spirit.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
		ofrenda.welcome(level);
		helper.assertTrue(earned(witness, "remembered"), "Arrived at the ofrenda: Remembered for the player near");
		helper.runAfterDelay(OfrendaBlockEntity.CHECK_TICKS + 2, () -> {
			helper.assertTrue(helper.getBlockState(OFRENDA).getValue(OfrendaBlock.COMPLETE), "A complete ofrenda glows");
			spirit.discard();
			helper.succeed();
		});
	}

	/** Missing a kind (no drink), it isn't complete and stays dark. */
	@GameTest(maxTicks = 100)
	public void anIncompleteOfrendaStaysDark(GameTestHelper helper) {
		OfrendaBlockEntity ofrenda = ofrenda(helper);
		for (ItemStack offering : complete().subList(0, 4)) {
			ofrenda.offer(offering);
		}
		helper.assertTrue(!ofrenda.complete(), "Four kinds of five isn't complete");
		helper.runAfterDelay(OfrendaBlockEntity.CHECK_TICKS + 2, () -> {
			helper.assertTrue(!helper.getBlockState(OFRENDA).getValue(OfrendaBlock.COMPLETE), "Incomplete, it doesn't glow");
			helper.succeed();
		});
	}

	/** Each kind's tag holds its offerings; keepsakes may be offered; the recipes, cooking and advancement load. */
	@GameTest
	public void ofrendaDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ItemStack> kinds = complete();
		OfrendaBlockEntity.Kind[] order = OfrendaBlockEntity.Kind.values();
		for (int i = 0; i < order.length; i++) {
			helper.assertTrue(kinds.get(i).is(order[i].items), kinds.get(i) + " is a " + order[i]);
		}
		helper.assertTrue(OfrendaBlockEntity.offerable(new ItemStack(vanilla("poppy"))) && OfrendaBlockEntity.offerable(new ItemStack(item("papel_picado")))
				&& OfrendaBlockEntity.offerable(new ItemStack(vanilla("apple"))) && !OfrendaBlockEntity.offerable(new ItemStack(vanilla("stone"))),
				"Keepsakes and favourite foods may be offered; a stone may not");
		for (String recipe : List.of("ofrenda", "papel_picado", "sugar_skull", "marigold_petals", "pan_de_muerto_dough", "pan_de_muerto",
				"orange_dye_from_marigold")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id("remembered")) != null, "Remembered loads");
		helper.succeed();
	}
}
