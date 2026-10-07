package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for pie baking: the Hearth Oven taking fuel (coal as long as a generator burns it, logs as long as a
 * furnace, up to its bank), lighting while
 * it burns, baking a pie only when hot enough, faster at full heat, giving back a raw pie taken out too soon, a baked pie
 * (As Easy as Pie) on time and a burnt pie too late, and reading the pie on a comparator; pies eaten a slice at a time,
 * cut with a carving knife into slices to take away, gone with the last slice, dropping themselves only while whole; a
 * burnt pie's poor slices; and the recipes.
 */
public class PieGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
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

	private static HearthOvenBlockEntity oven(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(pos, HearthOvenBlockEntity.class);
	}

	/** {@code player} uses {@code held} (or an empty hand) on the block at {@code pos}. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
	}

	/**
	 * Coal banks a generator's 1,600 ticks of fire, twice, but not a third time past the bank; a log burns 300 ticks and dirt
	 * not at all; the oven lights while it
	 * burns. A raw pie goes in (not a second); taken out at once it comes back raw. Too cool (below 50) a pie doesn't bake.
	 */
	@GameTest(maxTicks = 40)
	public void theOvenTakesFuelAndPies(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 2, 3);
		HearthOvenBlockEntity oven = oven(helper, pos);
		ServerPlayer baker = player(helper, new BlockPos(3, 2, 1));
		Item coal = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("coal"));
		ItemStack fuel = new ItemStack(coal, 3);
		use(helper, baker, pos, fuel);
		use(helper, baker, pos, fuel);
		int banked = oven.burn();
		use(helper, baker, pos, fuel);
		helper.assertTrue(banked == 3200 && oven.burn() == 3200 && fuel.getCount() == 1, "Two coal bank 3,200 ticks; a third doesn't fit: " + banked);
		ItemStack log = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("oak_log")));
		ItemStack dirt = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("dirt")));
		helper.assertTrue(HearthOvenBlockEntity.burnTicks(log) == HearthOvenBlockEntity.WOOD_BURN && !HearthOvenBlockEntity.isFuel(dirt),
				"A log burns 300 ticks; dirt doesn't burn");
		ItemStack raw = new ItemStack(item(PieFilling.APPLE.rawPie()), 2);
		use(helper, baker, pos, raw);
		use(helper, baker, pos, raw);
		helper.assertTrue(oven.pie() == PieFilling.APPLE && raw.getCount() == 1, "One raw pie goes in, not two");
		use(helper, baker, pos, ItemStack.EMPTY);
		helper.assertTrue(oven.pie() == null && baker.getInventory().countItem(item(PieFilling.APPLE.rawPie())) == 1, "Taken out at once it is raw");
		oven.set(0, 40, PieFilling.CHESTNUT, 100);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(helper.getBlockState(pos).getValue(HearthOvenBlock.LIT) == false && oven.baked() == 100,
					"Out and too cool, nothing bakes: " + oven.baked());
			oven.set(400, 60, null, 0);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(helper.getBlockState(pos).getValue(HearthOvenBlock.LIT), "Burning, it is lit");
				helper.succeed();
			});
		});
	}

	/**
	 * At full heat a pie bakes two points a tick: in time it is baked (a comparator reads 15) and comes out as the pie,
	 * earning As Easy as Pie; left in too long it burns and comes out burnt.
	 */
	@GameTest(maxTicks = 40)
	public void piesBakeAndBurn(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 2, 3);
		HearthOvenBlockEntity oven = oven(helper, pos);
		ServerPlayer baker = player(helper, new BlockPos(3, 2, 1));
		oven.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.SWEET_POTATO, HearthOvenBlockEntity.BAKED - 6);
		BlockPos burnPos = new BlockPos(6, 2, 3);
		HearthOvenBlockEntity burning = oven(helper, burnPos);
		burning.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.CRANBERRY, HearthOvenBlockEntity.BURNT - 6);
		helper.runAfterDelay(5, () -> {
			ServerLevel level = helper.getLevel();
			BlockPos absolute = helper.absolutePos(pos);
			helper.assertTrue(oven.baked() >= HearthOvenBlockEntity.BAKED && oven.baked() < HearthOvenBlockEntity.BURNT
					&& level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 15, "Baked: " + oven.baked());
			use(helper, baker, pos, ItemStack.EMPTY);
			helper.assertTrue(baker.getInventory().countItem(item(PieFilling.SWEET_POTATO.pie())) == 1 && earned(baker, "as_easy_as_pie"),
					"Out comes a sweet potato pie: As Easy as Pie");
			helper.assertTrue(burning.baked() >= HearthOvenBlockEntity.BURNT, "Left in, the cranberry pie burns");
			use(helper, baker, burnPos, ItemStack.EMPTY);
			helper.assertTrue(baker.getInventory().countItem(item("burnt_pie")) == 1, "Out comes a burnt pie");
			helper.succeed();
		});
	}

	/**
	 * A hungry player eats a slice (four food for apple), a carving knife cuts one to take away, and the last slice takes
	 * the pie; comparators read the slices left. A whole pie drops itself; a cut one doesn't. A burnt pie's slice is one
	 * food.
	 */
	@GameTest(maxTicks = 20)
	public void piesAreEatenAndCut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, item(PieFilling.APPLE.pie()) instanceof net.minecraft.world.item.BlockItem block ? block.getBlock() : Blocks.AIR);
		ServerPlayer eater = player(helper, new BlockPos(3, 2, 1));
		eater.getFoodData().setFoodLevel(4);
		BlockPos absolute = helper.absolutePos(pos);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 15, "A whole pie reads 15");
		use(helper, eater, pos, ItemStack.EMPTY);
		helper.assertTrue(eater.getFoodData().getFoodLevel() == 4 + PieFilling.APPLE.nutrition && helper.getBlockState(pos).getValue(PieBlock.BITES) == 1,
				"A slice eaten: " + eater.getFoodData().getFoodLevel());
		use(helper, eater, pos, new ItemStack(item("carving_knife")));
		helper.assertTrue(eater.getInventory().countItem(item(PieFilling.APPLE.slice())) == 1 && helper.getBlockState(pos).getValue(PieBlock.BITES) == 2,
				"A knife cuts a slice to take away");
		use(helper, eater, pos, ItemStack.EMPTY);
		use(helper, eater, pos, ItemStack.EMPTY);
		helper.assertTrue(helper.getBlockState(pos).isAir(), "The last slice takes the pie");

		BlockPos whole = new BlockPos(5, 2, 3);
		BlockPos cut = new BlockPos(6, 2, 3);
		for (BlockPos at : List.of(whole, cut)) {
			helper.setBlock(at.below(), Blocks.STONE);
			helper.setBlock(at, JugcraftAgriculture.block(PieFilling.CHESTNUT.pie()));
		}
		helper.setBlock(cut, helper.getBlockState(cut).setValue(PieBlock.BITES, 1));
		level.destroyBlock(helper.absolutePos(whole), true);
		level.destroyBlock(helper.absolutePos(cut), true);
		int dropped = 0;
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(whole)).inflate(3.0))) {
			dropped += entity.getItem().is(item(PieFilling.CHESTNUT.pie())) ? entity.getItem().getCount() : 0;
			entity.discard();
		}
		helper.assertTrue(dropped == 1, "Only the whole pie drops itself: " + dropped);

		BlockPos burnt = new BlockPos(3, 2, 6);
		helper.setBlock(burnt.below(), Blocks.STONE);
		helper.setBlock(burnt, JugcraftAgriculture.block("burnt_pie"));
		eater.getFoodData().setFoodLevel(4);
		use(helper, eater, burnt, ItemStack.EMPTY);
		helper.assertTrue(eater.getFoodData().getFoodLevel() == 4 + PieBlock.BURNT_NUTRITION, "A burnt slice is one food");
		helper.succeed();
	}

	/** The oven's, pastry's and raw pies' recipes load; the slices are foods. */
	@GameTest(maxTicks = 20)
	public void pieDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> recipes = new java.util.ArrayList<>(List.of("hearth_oven", "pastry_dough"));
		TagKey<Item> foods = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods"));
		for (PieFilling filling : PieFilling.values()) {
			recipes.add(filling.rawPie());
			helper.assertTrue(new ItemStack(item(filling.slice())).is(foods), filling.slice() + " is a food");
		}
		for (String recipe : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.succeed();
	}
}
