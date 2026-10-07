package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Knitting;
import io.github.jimbozoomer.jugcraft.agriculture.KnittingNeedlesItem;
import io.github.jimbozoomer.jugcraft.agriculture.KnittingWork;
import io.github.jimbozoomer.jugcraft.agriculture.Knitwear;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for knitting: the Spinning Wheel taking a skein of wool and spinning it in four turns (by hand or by a
 * redstone pulse) into four balls of yarn in its colour set out in front, refusing a second skein, and unravelling knitwear
 * into its yarn less one; Knitting Needles changing project, knitting a row a ball of yarn, unpicking, and finishing a
 * garment in the blend of its yarns; knitwear being worn and freeze-proof, taking dye as leather does and washing clean in
 * a cauldron; and keeping cosy by a lit campfire in two pieces of knitwear (not one, not by an unlit fire), and Snug as a
 * Bug in all three.
 */
public class KnittingGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Item wool(DyeColor color) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + "_wool"));
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

	private static SpinningWheelBlockEntity wheel(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, JugcraftAgriculture.block("spinning_wheel").defaultBlockState().setValue(SpinningWheelBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(pos, SpinningWheelBlockEntity.class);
	}

	/** {@code player} uses what they hold (or an empty hand) on the wheel at {@code pos}. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
	}

	/** The yarn lying within two blocks of {@code pos}. */
	private static List<ItemStack> yarnLying(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2.0)).stream()
				.map(ItemEntity::getItem).filter(Knitting::isYarn).toList();
	}

	/**
	 * Wool goes on the distaff (a second skein doesn't); four turns of the treadle spin it into four balls of red yarn, set
	 * out in front of the wheel, and the distaff is bare again. A redstone pulse turns it too. Without wool it doesn't turn.
	 */
	@GameTest(maxTicks = 20)
	public void theWheelSpinsWoolIntoYarn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(3, 2, 3);
		SpinningWheelBlockEntity wheel = wheel(helper, pos);
		ServerPlayer spinner = player(helper, new BlockPos(3, 2, 1));
		helper.assertTrue(!wheel.turn(level), "With no wool it doesn't turn");
		ItemStack red = new ItemStack(wool(DyeColor.RED), 2);
		use(helper, spinner, pos, red);
		helper.assertTrue(wheel.wool() == DyeColor.RED && red.getCount() == 1, "Red wool goes on the distaff");
		use(helper, spinner, pos, red);
		helper.assertTrue(red.getCount() == 1, "A second skein doesn't");
		for (int turn = 1; turn < SpinningWheelBlockEntity.TURNS; turn++) {
			use(helper, spinner, pos, ItemStack.EMPTY);
			helper.assertTrue(wheel.turns() == turn && yarnLying(helper, pos.north()).isEmpty(), "Turn " + turn + " spins it on");
		}
		use(helper, spinner, pos, ItemStack.EMPTY);
		List<ItemStack> yarn = yarnLying(helper, pos.north());
		int count = yarn.stream().mapToInt(ItemStack::getCount).sum();
		helper.assertTrue(count == Knitting.YARN_PER_WOOL && yarn.stream().allMatch(ball -> Knitting.color(ball) == (DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF))
				&& wheel.wool() == null, "Four turns spin four balls of red yarn: " + count);
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0))) {
			entity.discard();
		}

		BlockPos powered = new BlockPos(6, 2, 3);
		SpinningWheelBlockEntity driven = wheel(helper, powered);
		driven.loadWool(DyeColor.BLUE);
		helper.setBlock(powered.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(driven.turns() == 1, "A redstone pulse works the treadle: " + driven.turns());
		helper.succeed();
	}

	/** A sweater used on the wheel unravels into four balls of its yarn (five rows, less one). */
	@GameTest(maxTicks = 20)
	public void knitwearUnravels(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 2, 3);
		wheel(helper, pos);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1));
		ItemStack sweater = Knitting.garment(Knitwear.PUMPKIN_SWEATER, 0x3366CC);
		use(helper, player, pos, sweater);
		int balls = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (Knitting.isYarn(stack) && Knitting.color(stack) == 0x3366CC) {
				balls += stack.getCount();
			}
		}
		helper.assertTrue(sweater.isEmpty() && balls == Knitwear.PUMPKIN_SWEATER.rows - SpinningWheelBlockEntity.UNRAVEL_LOSS,
				"It unravels into four balls of its blue: " + balls);
		helper.succeed();
	}

	/**
	 * Sneaking changes the project; each row uses a ball of yarn; a beanie of one red and one white row comes out their
	 * blend, and the needles take a point of wear; rows can be unpicked, the yarn back. Knitting earns Knit One, Purl Two.
	 */
	@GameTest(maxTicks = 20)
	public void needlesKnitGarments(GameTestHelper helper) {
		ServerPlayer knitter = player(helper, new BlockPos(3, 2, 3));
		ItemStack needles = new ItemStack(item("knitting_needles"));
		knitter.setItemInHand(InteractionHand.MAIN_HAND, needles);
		helper.assertTrue(KnittingNeedlesItem.work(needles).knitwear() == Knitwear.BEANIE, "The needles start on a beanie");
		knitter.setShiftKeyDown(true);
		needles.getItem().use(helper.getLevel(), knitter, InteractionHand.MAIN_HAND);
		helper.assertTrue(KnittingNeedlesItem.work(needles).knitwear() == Knitwear.SOCKS, "Sneaking moves on to socks");
		needles.set(JugcraftAgriculture.KNITTING, KnittingWork.NONE);
		knitter.setShiftKeyDown(false);

		ItemStack red = Knitting.yarn(0xFF0000, 3);
		ItemStack white = Knitting.yarn(0xFFFFFF, 1);
		helper.assertTrue(KnittingNeedlesItem.knitRow(knitter, needles, red).isEmpty() && red.getCount() == 2
				&& KnittingNeedlesItem.work(needles).rows() == 1, "A row uses a ball of yarn");
		ItemStack beanie = KnittingNeedlesItem.knitRow(knitter, needles, white);
		helper.assertTrue(beanie.is(item("knit_beanie")) && Knitting.color(beanie) == 0xFF7F7F && white.isEmpty(),
				"The second row finishes the beanie, in pink: " + Integer.toHexString(Knitting.color(beanie)));
		helper.assertTrue(KnittingNeedlesItem.work(needles).rows() == 0 && needles.getDamageValue() == 1, "The needles are free, a little worn");
		helper.assertTrue(earned(knitter, "knit_one_purl_two"), "Knit One, Purl Two");
		int before = knitter.getInventory().countItem(item("yarn"));
		KnittingNeedlesItem.knitRow(knitter, needles, red);
		knitter.setShiftKeyDown(true);
		needles.getItem().use(helper.getLevel(), knitter, InteractionHand.MAIN_HAND);
		helper.assertTrue(KnittingNeedlesItem.work(needles).rows() == 0 && knitter.getInventory().countItem(item("yarn")) == before + 1,
				"Unpicking gives the row's yarn back");
		helper.succeed();
	}

	/**
	 * Knitwear and yarn take dye at a crafting table as Minecraft 26.3 dyes leather armour (a dyeing recipe for each, of the
	 * same kind as vanilla's leather_helmet_dyed): a garment and red dye make the garment redder. A water cauldron washes
	 * the dye out again.
	 */
	@GameTest
	public void knitwearTakesDye(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Item red = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_dye"));
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Optional<RecipeHolder<CraftingRecipe>> leather = crafting.getRecipeFor(
				CraftingInput.of(2, 1, List.of(new ItemStack(Items.LEATHER_HELMET), new ItemStack(red))), level);
		helper.assertTrue(leather.isPresent(), "A leather helmet and red dye make a recipe");
		CraftingRecipe dyeing = leather.get().value();
		String kind = BuiltInRegistries.RECIPE_SERIALIZER.getKey(dyeing.getSerializer()) + " (" + dyeing.getClass().getSimpleName() + ")";
		List<ItemStack> knits = new java.util.ArrayList<>(List.of(Knitting.yarn(Knitting.UNDYED, 1)));
		for (Knitwear knit : Knitwear.values()) {
			knits.add(Knitting.garment(knit, Knitting.UNDYED));
		}
		for (ItemStack knit : knits) {
			CraftingInput input = CraftingInput.of(2, 1, List.of(knit, new ItemStack(red)));
			Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, level);
			helper.assertTrue(recipe.isPresent() && recipe.get().value().getClass() == dyeing.getClass(),
					knit + " takes dye as leather does, by " + kind);
			ItemStack dyed = recipe.get().value().assemble(input);
			helper.assertTrue(dyed.is(knit.getItem()) && Knitting.color(dyed) != Knitting.UNDYED, knit + " comes out dyed: " + dyed);
		}
		BlockPos cauldron = new BlockPos(1, 2, 1);
		helper.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		ServerPlayer washer = player(helper, new BlockPos(1, 2, 3));
		use(helper, washer, cauldron, Knitting.garment(Knitwear.SWEATER, 0xB02E26));
		ItemStack washed = washer.getMainHandItem();
		helper.assertTrue(washed.is(item("knit_sweater")) && Knitting.color(washed) == Knitting.UNDYED, "A water cauldron washes the dye out: "
				+ washed);
		helper.succeed();
	}

	/**
	 * Knitwear is worn in its slot and keeps out powder snow's cold. Two pieces by a lit campfire make a player
	 * cosy (Regeneration); one piece, or an unlit fire, doesn't; all three earn Snug as a Bug.
	 */
	@GameTest(maxTicks = 20)
	public void knitwearKeepsYouCosy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (Knitwear knit : Knitwear.values()) {
			ItemStack garment = Knitting.garment(knit, 0x228822);
			helper.assertTrue(garment.get(DataComponents.EQUIPPABLE).slot() == knit.slot && garment.is(ItemTags.FREEZE_IMMUNE_WEARABLES)
					&& garment.is(Knitting.KNITWEAR), knit + " is worn and warm");
		}
		helper.setBlock(new BlockPos(3, 2, 3), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5));
		player.setItemSlot(EquipmentSlot.HEAD, Knitting.garment(Knitwear.BEANIE, Knitting.UNDYED));
		helper.assertTrue(!Knitting.warm(level, player), "One piece isn't enough");
		player.setItemSlot(EquipmentSlot.CHEST, Knitting.garment(Knitwear.LEAF_SWEATER, Knitting.UNDYED));
		helper.assertTrue(!Knitting.warm(level, player), "An unlit fire doesn't warm");
		helper.setBlock(new BlockPos(3, 2, 3), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		helper.assertTrue(Knitting.warm(level, player) && player.hasEffect(MobEffects.REGENERATION) && !earned(player, "snug_as_a_bug"),
				"Two pieces by a lit fire: cosy");
		player.setItemSlot(EquipmentSlot.FEET, Knitting.garment(Knitwear.SOCKS, Knitting.UNDYED));
		Knitting.warm(level, player);
		helper.assertTrue(earned(player, "snug_as_a_bug"), "Beanie, sweater and socks: Snug as a Bug");
		for (String recipe : List.of("spinning_wheel", "knitting_needles")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.succeed();
	}
}
