package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BoneThroneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BubblingCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ChimeraFinialBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinWardrobeBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalRibBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalSkullBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ColossalSkullBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaphs;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleRainspoutBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleSentinelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GargoyleSentinelBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.IronBoundCoffinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.IronBoundCoffinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.KeyCopyingRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SkeletonKeyItem;
import io.github.jimbozoomer.jugcraft.agriculture.VertebraFloorLampBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween decorations batch 18, the Crypt and the Ossuary: the Iron-Bound Coffin locks to the key a
 * blank is cut to, opens only for someone holding it and shuts out hoppers while locked, and a cut key copies onto
 * blanks; the Coffin Wardrobe swaps armour, leaving cursed pieces on; the sarcophagi store, are recarved and knock; the
 * Bone Throne and Skull Footstool seat, the Vertebra Floor Lamp switches, and the Ribcage Bookcase holds books and powers
 * an enchanting table; the Colossal Skull stands whole, drops its jaw on redstone and breaks as one; Colossal Ribs meet
 * as an arch and part again; vertebrae turn and the femur lies two blocks long; the Gargoyle Sentinel signals monsters,
 * not cattle; the Gargoyle Rainspout fills cauldrons below it; the Chimera Finial reads the weather; and the data loads.
 */
public class CryptAndOssuaryGameTests {
	private static final String COFFIN = JugcraftAgriculture.IRON_BOUND_COFFIN;

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos on, Direction face) {
		BlockPos absolute = helper.absolutePos(on);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** Places the item {@code id} on the block under {@code at}, by {@code player} turned to {@code yaw} (0 looks south, 180 north). */
	private static void place(GameTestHelper helper, ServerPlayer player, String id, BlockPos at, float yaw) {
		player.setYRot(yaw);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(id)));
		BlockPos below = helper.absolutePos(at.below());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(below).relative(Direction.UP, 0.5), Direction.UP, below, false);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 9; x++) {
			for (int z = 0; z < 9; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static int dropped(GameTestHelper helper, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 2, 4))).inflate(8.0)).stream()
				.filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static ItemStack key(int wards) {
		ItemStack key = new ItemStack(item(JugcraftAgriculture.SKELETON_KEY));
		key.set(JugcraftAgriculture.KEY_WARDS, wards);
		return key;
	}

	// ---------------------------------------------------------------- 6. the Iron-Bound Coffin and the Coffin Wardrobe

	/**
	 * A blank sneak-used on the coffin is cut to it and locks it, padlock on both halves. Locked, nobody opens it with an
	 * empty hand or another key, another key or a blank can't unlock it, and hoppers can't reach in; its key in either
	 * hand opens it as a 54-slot chest. Sneak-used again, its key unlocks it for anyone. The lock is saved.
	 */
	@GameTest
	public void coffinLocksToItsKey(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos foot = new BlockPos(3, 2, 4);
		ServerPlayer owner = player(helper, new BlockPos(7, 2, 7), ItemStack.EMPTY);
		place(helper, owner, COFFIN, foot, 180.0F);
		BlockState footState = helper.getBlockState(foot);
		helper.assertTrue(footState.is(block(COFFIN)) && footState.getValue(CoffinBlock.PART) == BedPart.FOOT, "The coffin is placed");
		BlockPos head = foot.relative(footState.getValue(CoffinBlock.FACING));
		IronBoundCoffinBlockEntity coffin = (IronBoundCoffinBlockEntity) level.getBlockEntity(helper.absolutePos(head));
		helper.assertTrue(coffin != null && coffin.lock() == 0 && coffin.getContainerSize() == IronBoundCoffinBlockEntity.SLOTS,
				"It starts unlocked, with " + IronBoundCoffinBlockEntity.SLOTS + " slots");

		owner.setShiftKeyDown(true);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(JugcraftAgriculture.KEY_BLANK)));
		helper.assertTrue(use(helper, owner, foot, Direction.UP).consumesAction(), "Sneak-using a blank on it cuts the blank");
		ItemStack cut = owner.getMainHandItem();
		int wards = SkeletonKeyItem.wards(cut);
		helper.assertTrue(cut.is(item(JugcraftAgriculture.SKELETON_KEY)) && wards > 0 && wards < SkeletonKeyItem.PATTERNS && coffin.lock() == wards,
				"The blank becomes the key it is locked to: " + cut + " for " + coffin.lock());
		helper.assertTrue(helper.getBlockState(head).getValue(IronBoundCoffinBlock.LOCKED) && helper.getBlockState(foot).getValue(IronBoundCoffinBlock.LOCKED),
				"The padlock hangs on both halves");
		owner.setShiftKeyDown(false);

		ServerPlayer thief = player(helper, new BlockPos(1, 2, 7), ItemStack.EMPTY);
		use(helper, thief, foot, Direction.UP);
		helper.assertTrue(thief.containerMenu == thief.inventoryMenu, "Locked, an empty hand doesn't open it");
		thief.setItemInHand(InteractionHand.MAIN_HAND, key(wards % (SkeletonKeyItem.PATTERNS - 1) + 1));
		use(helper, thief, foot, Direction.UP);
		helper.assertTrue(thief.containerMenu == thief.inventoryMenu, "nor does another key");
		thief.setShiftKeyDown(true);
		helper.assertFalse(use(helper, thief, foot, Direction.UP).consumesAction(), "Another key can't unlock it");
		thief.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(JugcraftAgriculture.KEY_BLANK)));
		helper.assertFalse(use(helper, thief, foot, Direction.UP).consumesAction(), "nor can a blank be cut to a locked lock");
		helper.assertTrue(coffin.lock() == wards && thief.getMainHandItem().is(item(JugcraftAgriculture.KEY_BLANK)), "It stays locked, the blank uncut");
		thief.setShiftKeyDown(false);
		helper.assertTrue(coffin.getSlotsForFace(Direction.UP).length == 0 && !coffin.canPlaceItemThroughFace(0, new ItemStack(Items.BONE), Direction.UP)
				&& !coffin.canTakeItemThroughFace(0, new ItemStack(Items.BONE), Direction.DOWN), "Locked, hoppers and pipes can't reach in");

		use(helper, owner, foot, Direction.UP);
		helper.assertTrue(owner.containerMenu instanceof ChestMenu menu && menu.getContainer().getContainerSize() == IronBoundCoffinBlockEntity.SLOTS,
				"Its key opens it as a " + IronBoundCoffinBlockEntity.SLOTS + "-slot chest");
		owner.closeContainer();
		thief.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		thief.setItemInHand(InteractionHand.OFF_HAND, key(wards));
		use(helper, thief, foot, Direction.UP);
		helper.assertTrue(thief.containerMenu instanceof ChestMenu, "A copy of its key in the other hand opens it too");
		thief.closeContainer();
		thief.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

		CompoundTag saved = coffin.saveWithoutMetadata(level.registryAccess());
		IronBoundCoffinBlockEntity copy = new IronBoundCoffinBlockEntity(coffin.getBlockPos(), coffin.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.lock() == wards, "The lock is saved");

		owner.setShiftKeyDown(true);
		helper.assertTrue(use(helper, owner, foot, Direction.UP).consumesAction(), "Sneak-using its key unlocks it");
		owner.setShiftKeyDown(false);
		helper.assertTrue(coffin.lock() == 0 && !helper.getBlockState(head).getValue(IronBoundCoffinBlock.LOCKED)
				&& !helper.getBlockState(foot).getValue(IronBoundCoffinBlock.LOCKED) && coffin.getSlotsForFace(Direction.UP).length == IronBoundCoffinBlockEntity.SLOTS,
				"Unlocked, the padlock comes off and hoppers reach in again");
		use(helper, thief, foot, Direction.UP);
		helper.assertTrue(thief.containerMenu instanceof ChestMenu, "and anyone opens it");
		thief.closeContainer();
		helper.succeed();
	}

	/** A hopper over a locked coffin keeps its item; unlocked, the coffin takes it. */
	@GameTest(maxTicks = 100)
	public void lockedCoffinShutsOutHoppers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos foot = new BlockPos(3, 2, 4);
		ServerPlayer owner = player(helper, new BlockPos(7, 2, 7), ItemStack.EMPTY);
		place(helper, owner, COFFIN, foot, 180.0F);
		BlockPos head = foot.relative(helper.getBlockState(foot).getValue(CoffinBlock.FACING));
		IronBoundCoffinBlockEntity coffin = (IronBoundCoffinBlockEntity) level.getBlockEntity(helper.absolutePos(head));
		IronBoundCoffinBlock.setLock(level, helper.absolutePos(head), coffin, 1234);
		helper.setBlock(head.above(), Blocks.HOPPER);
		net.minecraft.world.level.block.entity.HopperBlockEntity hopper =
				(net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(helper.absolutePos(head.above()));
		hopper.setItem(0, new ItemStack(Items.BONE, 1));
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(coffin.isEmpty() && hopper.getItem(0).is(Items.BONE), "A locked coffin takes nothing from a hopper");
			IronBoundCoffinBlock.setLock(level, helper.absolutePos(head), coffin, 0);
		});
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(!coffin.isEmpty() && hopper.isEmpty(), "Unlocked, it takes the hopper's bone");
			helper.succeed();
		});
	}

	/** A cut key and one to eight blanks make that many copies, the key staying in the grid; anything else makes none. */
	@GameTest
	public void keysCopyOntoBlanks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(JugcraftAgriculture.KEY_COPYING))).isPresent(),
				"The key-copying recipe loads");
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack blank = new ItemStack(item(JugcraftAgriculture.KEY_BLANK));
		List<ItemStack> grid = new ArrayList<>(List.of(key(777), blank.copy(), blank.copy(), blank.copy(), ItemStack.EMPTY, ItemStack.EMPTY,
				ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
		CraftingInput input = CraftingInput.of(3, 3, grid);
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, level);
		helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof KeyCopyingRecipe, "A key and three blanks copy the key");
		ItemStack copies = recipe.get().value().assemble(input);
		helper.assertTrue(copies.is(item(JugcraftAgriculture.SKELETON_KEY)) && copies.getCount() == 3 && SkeletonKeyItem.wards(copies) == 777,
				"Three keys with the same wards: " + copies);
		NonNullList<ItemStack> left = recipe.get().value().getRemainingItems(input);
		long kept = left.stream().filter(stack -> SkeletonKeyItem.wards(stack) == 777).count();
		helper.assertTrue(kept == 1, "The key that was copied stays in the grid");

		List<ItemStack> full = new ArrayList<>(List.of(key(5)));
		for (int i = 0; i < KeyCopyingRecipe.MAX_COPIES; i++) {
			full.add(blank.copy());
		}
		CraftingInput eight = CraftingInput.of(3, 3, full);
		helper.assertTrue(KeyCopyingRecipe.INSTANCE.matches(eight, level) && KeyCopyingRecipe.INSTANCE.assemble(eight).getCount() == KeyCopyingRecipe.MAX_COPIES,
				"Eight blanks make eight copies");
		helper.assertFalse(KeyCopyingRecipe.INSTANCE.matches(CraftingInput.of(2, 1, List.of(key(5), ItemStack.EMPTY)), level), "A key alone copies nothing");
		helper.assertFalse(KeyCopyingRecipe.INSTANCE.matches(CraftingInput.of(2, 1, List.of(blank.copy(), blank.copy())), level), "Blanks alone copy nothing");
		helper.assertFalse(KeyCopyingRecipe.INSTANCE.matches(CraftingInput.of(3, 1, List.of(key(5), key(6), blank.copy())), level),
				"Two keys at once copy nothing");
		helper.assertFalse(KeyCopyingRecipe.INSTANCE.matches(CraftingInput.of(3, 1, List.of(key(5), blank.copy(), new ItemStack(Items.STICK))), level),
				"Nor does a grid with something else in it");
		helper.succeed();
	}

	/**
	 * Armour used on the wardrobe hangs in it, swapping for what hung there; an empty hand swaps everything you wear for
	 * what it holds, piece by piece, except a piece cursed with Binding; something else in hand does nothing; broken, it
	 * drops what it held.
	 */
	@GameTest(maxTicks = 60)
	public void wardrobeSwapsArmour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		ServerPlayer dresser = player(helper, new BlockPos(4, 2, 7), ItemStack.EMPTY);
		place(helper, dresser, JugcraftAgriculture.COFFIN_WARDROBE, at, 180.0F);
		helper.assertTrue(helper.getBlockState(at.above()).is(block(JugcraftAgriculture.COFFIN_WARDROBE)), "The wardrobe stands two blocks tall");
		CoffinWardrobeBlockEntity wardrobe = (CoffinWardrobeBlockEntity) level.getBlockEntity(helper.absolutePos(at));

		dresser.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HELMET));
		use(helper, dresser, at.above(), Direction.SOUTH);
		dresser.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_BOOTS));
		use(helper, dresser, at, Direction.SOUTH);
		helper.assertTrue(wardrobe.get(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && wardrobe.get(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS)
				&& dresser.getMainHandItem().isEmpty(), "Armour used on it hangs in it");
		dresser.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_HELMET));
		use(helper, dresser, at, Direction.SOUTH);
		helper.assertTrue(wardrobe.get(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET) && dresser.getMainHandItem().is(Items.IRON_HELMET),
				"Another helmet swaps for the one that hung there");

		dresser.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		dresser.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
		dresser.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
		use(helper, dresser, at, Direction.SOUTH);
		helper.assertTrue(dresser.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET) && dresser.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				&& dresser.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS), "An empty hand puts on what it holds");
		helper.assertTrue(wardrobe.get(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && wardrobe.get(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE)
				&& wardrobe.get(EquipmentSlot.FEET).isEmpty(), "and hangs up what was worn");

		ItemStack cursed = new ItemStack(Items.IRON_LEGGINGS);
		cursed.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
		dresser.setItemSlot(EquipmentSlot.LEGS, cursed);
		wardrobe.set(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
		use(helper, dresser, at.above(), Direction.SOUTH);
		helper.assertTrue(dresser.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS) && wardrobe.get(EquipmentSlot.LEGS).is(Items.CHAINMAIL_LEGGINGS),
				"Cursed leggings stay on");
		helper.assertTrue(dresser.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && dresser.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE),
				"while the rest swaps back");

		dresser.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, dresser, at, Direction.SOUTH);
		helper.assertTrue(dresser.getMainHandItem().is(Items.STICK) && wardrobe.get(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET),
				"A stick does nothing");

		level.destroyBlock(helper.absolutePos(at), true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, Items.GOLDEN_HELMET) == 1 && dropped(helper, Items.CHAINMAIL_LEGGINGS) == 1
				&& dropped(helper, Items.DIAMOND_BOOTS) == 1 && dropped(helper, item(JugcraftAgriculture.COFFIN_WARDROBE)) == 1,
				"Broken, it drops what it held and itself"));
	}

	// ---------------------------------------------------------------- 7. the sarcophagi

	/**
	 * Placed, a sarcophagus lies two blocks long; opened, it is a 27-slot chest with its lid off both halves, and a
	 * comparator reads it; the chisel recarves the lid through its four effigies, on both halves; it knocks three times,
	 * only when shut, at night, with a player near and none for a minute; broken, it spills and drops itself.
	 */
	@GameTest(maxTicks = 100)
	public void sarcophagusStoresAndKnocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		for (String stone : JugcraftAgriculture.SARCOPHAGUS_STONES) {
			Block tomb = block(stone + "_sarcophagus");
			helper.assertTrue(JugcraftAgriculture.SARCOPHAGUS_TOMB_ENTITY.isValid(tomb.defaultBlockState().setValue(LongDecorationBlock.PART, BedPart.HEAD)),
					stone + " sarcophagus is a tomb-chest");
		}
		BlockPos foot = new BlockPos(3, 2, 5);
		ServerPlayer mason = player(helper, new BlockPos(7, 2, 7), ItemStack.EMPTY);
		place(helper, mason, "stone_brick_sarcophagus", foot, 180.0F);
		BlockState footState = helper.getBlockState(foot);
		helper.assertTrue(footState.is(block("stone_brick_sarcophagus")) && footState.getValue(LongDecorationBlock.PART) == BedPart.FOOT,
				"The sarcophagus is placed");
		BlockPos head = foot.relative(footState.getValue(LongDecorationBlock.FACING));
		helper.assertTrue(helper.getBlockState(head).is(block("stone_brick_sarcophagus")), "and lies two blocks long");
		SarcophagusBlockEntity tomb = (SarcophagusBlockEntity) level.getBlockEntity(helper.absolutePos(head));

		mason.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, mason, foot, Direction.UP);
		helper.assertTrue(mason.containerMenu instanceof ChestMenu menu && menu.getContainer().getContainerSize() == SarcophagusBlockEntity.SLOTS,
				"Opened, it is a " + SarcophagusBlockEntity.SLOTS + "-slot chest");
		helper.assertTrue(helper.getBlockState(head).getValue(SarcophagusBlock.OPEN) && helper.getBlockState(foot).getValue(SarcophagusBlock.OPEN),
				"with its lid off both halves");
		tomb.setItem(0, new ItemStack(Items.BONE, 5));
		mason.closeContainer();
		helper.assertFalse(helper.getBlockState(head).getValue(SarcophagusBlock.OPEN), "Closed, the lid goes back");
		helper.assertTrue(helper.getBlockState(foot).getAnalogOutputSignal(level, helper.absolutePos(foot), Direction.NORTH) > 0,
				"A comparator reads what it holds");

		ItemStack chisel = new ItemStack(item(Epitaphs.CHISEL));
		mason.setItemInHand(InteractionHand.MAIN_HAND, chisel);
		SarcophagusBlock.Effigy lid = helper.getBlockState(head).getValue(SarcophagusBlock.LID);
		helper.assertTrue(lid == SarcophagusBlock.Effigy.PLAIN, "Its lid starts plain");
		for (int i = 0; i < SarcophagusBlock.Effigy.values().length; i++) {
			use(helper, mason, head, Direction.UP);
			lid = lid.next();
			helper.assertTrue(helper.getBlockState(head).getValue(SarcophagusBlock.LID) == lid && helper.getBlockState(foot).getValue(SarcophagusBlock.LID) == lid,
					"The chisel carves " + lid + " on both halves");
		}
		helper.assertTrue(lid == SarcophagusBlock.Effigy.PLAIN && mason.getMainHandItem().getDamageValue() > 0, "and round again, wearing the chisel");

		long cooled = SarcophagusBlockEntity.COOLDOWN_TICKS;
		helper.assertTrue(SarcophagusBlockEntity.mayKnock(true, true, true, cooled, 0), "Shut, at night, a player near: it may knock");
		helper.assertFalse(SarcophagusBlockEntity.mayKnock(false, true, true, cooled, 0), "Not open");
		helper.assertFalse(SarcophagusBlockEntity.mayKnock(true, false, true, cooled, 0), "Not by day");
		helper.assertFalse(SarcophagusBlockEntity.mayKnock(true, true, false, cooled, 0), "Not with nobody near");
		helper.assertFalse(SarcophagusBlockEntity.mayKnock(true, true, true, cooled - 1, 0), "Not twice in a minute");
		tomb.startKnocking(level, helper.absolutePos(head));
		helper.assertTrue(tomb.knocksLeft() == SarcophagusBlockEntity.KNOCKS - 1, "It knocks once at once");
		helper.runAfterDelay(SarcophagusBlockEntity.GAP_TICKS * (SarcophagusBlockEntity.KNOCKS - 1) + 2, () -> {
			helper.assertTrue(tomb.knocksLeft() == 0, "and twice more, " + SarcophagusBlockEntity.GAP_TICKS + " ticks apart");
			level.destroyBlock(helper.absolutePos(foot), true);
		});
		helper.runAfterDelay(SarcophagusBlockEntity.GAP_TICKS * (SarcophagusBlockEntity.KNOCKS - 1) + 10, () -> {
			helper.assertBlockNotPresent(block("stone_brick_sarcophagus"), head);
			helper.assertTrue(dropped(helper, Items.BONE) == 5 && dropped(helper, item("stone_brick_sarcophagus")) == 1,
					"Broken, it spills the bones and drops one sarcophagus");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- 8. the ossuary parlour

	/**
	 * The Bone Throne seats one (not someone sneaking) and glows only while sat in at night; the Skull Footstool seats
	 * one; the Vertebra Floor Lamp is lit, put out by hand and switched by redstone, on both halves.
	 */
	@GameTest(maxTicks = 80)
	public void parlourSeatsAndLamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos throne = new BlockPos(2, 2, 2);
		ServerPlayer sitter = player(helper, new BlockPos(7, 2, 7), ItemStack.EMPTY);
		place(helper, sitter, JugcraftAgriculture.BONE_THRONE, throne, 180.0F);
		helper.assertTrue(helper.getBlockState(throne.above()).is(block(JugcraftAgriculture.BONE_THRONE)), "The throne stands two blocks tall");
		ServerPlayer sneaker = player(helper, new BlockPos(6, 2, 2), ItemStack.EMPTY);
		sneaker.setShiftKeyDown(true);
		use(helper, sneaker, throne, Direction.UP);
		helper.assertFalse(sneaker.isPassenger(), "Someone sneaking doesn't sit");
		sitter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(use(helper, sitter, throne.above(), Direction.NORTH).consumesAction() && sitter.isPassenger(), "Used, the throne seats you");
		BlockState lit = helper.getBlockState(throne.above()).setValue(BoneThroneBlock.LIT, true);
		helper.assertTrue(BoneThroneBlock.light(lit) == BoneThroneBlock.GLOW_LIGHT && BoneThroneBlock.light(lit.setValue(BoneThroneBlock.LIT, false)) == 0,
				"Its eyes glow when lit");

		BlockPos stool = new BlockPos(5, 2, 2);
		helper.setBlock(stool, block(JugcraftAgriculture.SKULL_FOOTSTOOL));
		sneaker.setShiftKeyDown(false);
		helper.assertTrue(use(helper, sneaker, stool, Direction.UP).consumesAction() && sneaker.isPassenger(), "The footstool seats someone too");

		BlockPos lamp = new BlockPos(5, 2, 5);
		ServerPlayer lighter = player(helper, new BlockPos(1, 2, 7), ItemStack.EMPTY);
		place(helper, lighter, JugcraftAgriculture.VERTEBRA_LAMP, lamp, 180.0F);
		helper.assertTrue(helper.getBlockState(lamp.above()).getLightEmission() == VertebraFloorLampBlock.LIGHT, "The lamp is lit when placed");
		lighter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, lighter, lamp, Direction.UP);
		helper.assertTrue(!helper.getBlockState(lamp).getValue(VertebraFloorLampBlock.LIT) && helper.getBlockState(lamp.above()).getLightEmission() == 0,
				"Used, it goes out, top and bottom");
		helper.setBlock(lamp.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(lamp.above()).getValue(VertebraFloorLampBlock.LIT) && helper.getBlockState(lamp).getValue(VertebraFloorLampBlock.POWERED),
				"Redstone lights it");
		helper.setBlock(lamp.east(), Blocks.AIR);
		helper.assertFalse(helper.getBlockState(lamp.above()).getValue(VertebraFloorLampBlock.LIT), "and its loss puts it out");

		helper.runAfterDelay(3, () -> {
			boolean night = MourningAngelBlock.night(level);
			helper.assertTrue(helper.getBlockState(throne).getValue(BoneThroneBlock.LIT) == night
					&& helper.getBlockState(throne.above()).getValue(BoneThroneBlock.LIT) == night, "Sat in, it glows if and only if it is night");
			sitter.stopRiding();
		});
		helper.runAfterDelay(3 + BoneThroneBlock.CHECK_TICKS + 3, () -> {
			helper.assertFalse(helper.getBlockState(throne).getValue(BoneThroneBlock.LIT), "Left, it goes dark");
			helper.succeed();
		});
	}

	/**
	 * The Ribcage Bookcase is a chiseled bookshelf (a book goes into the slot aimed at, and comes back out) that powers an
	 * enchanting table like a bookshelf.
	 */
	@GameTest
	public void ribcageBookcaseHoldsBooksAndPowersEnchanting(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos shelf = new BlockPos(4, 2, 4);
		helper.setBlock(shelf, block(JugcraftAgriculture.RIBCAGE_BOOKCASE));
		BlockPos absolute = helper.absolutePos(shelf);
		helper.assertTrue(level.getBlockEntity(absolute) instanceof ChiseledBookShelfBlockEntity, "It keeps its books as a chiseled bookshelf does");
		helper.assertTrue(helper.getBlockState(shelf).is(BlockTags.ENCHANTMENT_POWER_PROVIDER), "It is an enchanting power provider");
		BlockPos table = new BlockPos(4, 2, 6);
		helper.setBlock(table, Blocks.ENCHANTING_TABLE);
		helper.assertTrue(EnchantingTableBlock.isValidBookShelf(level, helper.absolutePos(table), absolute.subtract(helper.absolutePos(table))),
				"Two blocks from an enchanting table, it powers it");

		ServerPlayer reader = player(helper, new BlockPos(4, 2, 1), new ItemStack(Items.BOOK));
		Direction front = helper.getBlockState(shelf).getValue(HorizontalDirectionalBlock.FACING);
		Vec3 at = Vec3.atCenterOf(absolute).relative(front, 0.5).add(0.0, 0.25, 0.0);
		BlockHitResult hit = new BlockHitResult(at, front, absolute, false);
		reader.gameMode.useItemOn(reader, level, reader.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		BlockState filled = helper.getBlockState(shelf);
		long books = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.stream().filter(filled::getValue).count();
		helper.assertTrue(books == 1 && reader.getMainHandItem().isEmpty(), "A book goes onto the shelf aimed at");
		reader.gameMode.useItemOn(reader, level, reader.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		BlockState emptied = helper.getBlockState(shelf);
		helper.assertTrue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.stream().noneMatch(emptied::getValue) && reader.getMainHandItem().is(Items.BOOK),
				"and an empty hand takes it back");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 9. the Buried Colossus

	/**
	 * The Colossal Skull stands 2 x 2 x 2, every block part of it; redstone on any block drops the jaw on all of them; used,
	 * it snaps without breaking; broken anywhere, it goes whole and drops once; with a block in the way it is not placed.
	 */
	@GameTest(maxTicks = 40)
	public void colossalSkullStandsWhole(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ColossalSkullBlock skull = (ColossalSkullBlock) block(JugcraftAgriculture.COLOSSAL_SKULL);
		BlockPos master = new BlockPos(5, 2, 2);
		ServerPlayer builder = player(helper, new BlockPos(1, 2, 7), ItemStack.EMPTY);
		place(helper, builder, JugcraftAgriculture.COLOSSAL_SKULL, master, 0.0F);
		BlockPos absolute = helper.absolutePos(master);
		for (int part = 0; part < skull.cells().length; part++) {
			BlockState state = level.getBlockState(skull.partPos(absolute, Direction.NORTH, part));
			helper.assertTrue(state.is(skull) && skull.part(state) == part && state.getValue(ColossalSkullBlock.FACING) == Direction.NORTH,
					"Part " + part + " of the skull is in place");
		}
		helper.assertTrue(skull.cells().length == 8 && level.getBlockEntity(absolute) instanceof ColossalSkullBlockEntity, "Eight blocks, its jaw on the first");

		BlockPos top = skull.partPos(absolute, Direction.NORTH, 7);
		level.setBlock(top.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		for (int part = 0; part < skull.cells().length; part++) {
			helper.assertTrue(level.getBlockState(skull.partPos(absolute, Direction.NORTH, part)).getValue(ColossalSkullBlock.POWERED),
					"Redstone at its back corner drops the jaw on part " + part);
		}
		level.setBlock(top.west(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertFalse(level.getBlockState(absolute).getValue(ColossalSkullBlock.POWERED), "Without it, the jaw shuts");
		builder.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(use(helper, builder, master, Direction.SOUTH).consumesAction() && level.getBlockState(absolute).is(skull), "Used, it snaps");

		level.destroyBlock(skull.partPos(absolute, Direction.NORTH, 5), true);
		for (int part = 0; part < skull.cells().length; part++) {
			helper.assertFalse(level.getBlockState(skull.partPos(absolute, Direction.NORTH, part)).is(skull), "Broken anywhere, part " + part + " goes too");
		}
		BlockPos blocked = skull.partPos(absolute, Direction.NORTH, 6);
		level.setBlock(blocked, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		place(helper, builder, JugcraftAgriculture.COLOSSAL_SKULL, master, 0.0F);
		helper.assertBlockNotPresent(skull, master);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item(JugcraftAgriculture.COLOSSAL_SKULL)) == 1, "It drops once"));
	}

	/**
	 * A Colossal Rib two blocks from one facing it joins it as an arch over the block between, which stays open; breaking
	 * either parts the other. Vertebrae lie along the face they were set on; the femur lies two blocks long.
	 */
	@GameTest
	public void colossalBonesJoinAndTurn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Block rib = block(JugcraftAgriculture.COLOSSAL_RIB);
		BlockPos south = new BlockPos(4, 2, 4);
		BlockPos north = south.north(ColossalRibBlock.SPAN);
		ServerPlayer builder = player(helper, new BlockPos(8, 2, 8), ItemStack.EMPTY);
		place(helper, builder, JugcraftAgriculture.COLOSSAL_RIB, south, 0.0F);
		helper.assertTrue(helper.getBlockState(south).getValue(ColossalRibBlock.FACING) == Direction.NORTH
				&& helper.getBlockState(south.above()).is(rib) && !helper.getBlockState(south).getValue(ColossalRibBlock.JOINED),
				"A rib alone stands two blocks tall, unjoined");
		place(helper, builder, JugcraftAgriculture.COLOSSAL_RIB, north, 180.0F);
		for (BlockPos pos : List.of(south, south.above(), north, north.above())) {
			helper.assertTrue(helper.getBlockState(pos).is(rib) && helper.getBlockState(pos).getValue(ColossalRibBlock.JOINED), "Facing, they join: " + pos);
		}
		helper.assertTrue(helper.getBlockState(south.north()).isAir() && helper.getBlockState(south.north().above()).isAir(), "The arch is open beneath");
		helper.assertTrue(ColossalRibBlock.partner(helper.absolutePos(south), Direction.NORTH).equals(helper.absolutePos(north)), "Each is the other's partner");
		level.destroyBlock(helper.absolutePos(north), false);
		helper.assertTrue(!helper.getBlockState(south).getValue(ColossalRibBlock.JOINED) && !helper.getBlockState(south.above()).getValue(ColossalRibBlock.JOINED),
				"Breaking one parts the other");
		helper.assertTrue(helper.getBlockState(south).getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER, "which still stands");

		BlockPos side = new BlockPos(1, 2, 6);
		helper.setBlock(side, Blocks.STONE);
		BlockPos sideAbs = helper.absolutePos(side);
		builder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(JugcraftAgriculture.COLOSSAL_VERTEBRA)));
		builder.getMainHandItem().useOn(new UseOnContext(builder, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(sideAbs).relative(Direction.EAST, 0.5), Direction.EAST, sideAbs, false)));
		helper.assertTrue(helper.getBlockState(side.east()).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "Set on a side, a vertebra lies along it");
		place(helper, builder, JugcraftAgriculture.COLOSSAL_VERTEBRA, new BlockPos(2, 2, 2), 0.0F);
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y, "Set on the floor, it stands up");

		BlockPos femur = new BlockPos(6, 2, 7);
		place(helper, builder, JugcraftAgriculture.COLOSSAL_FEMUR, femur, 90.0F);
		BlockState femurFoot = helper.getBlockState(femur);
		helper.assertTrue(femurFoot.is(block(JugcraftAgriculture.COLOSSAL_FEMUR)) && femurFoot.getValue(LongDecorationBlock.PART) == BedPart.FOOT
				&& helper.getBlockState(femur.relative(femurFoot.getValue(LongDecorationBlock.FACING))).getValue(LongDecorationBlock.PART) == BedPart.HEAD,
				"The femur lies two blocks long");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 10. the gargoyles

	/**
	 * The Gargoyle Sentinel watches for the nearest monster, never a cow even nearer, and gives a signal by its distance
	 * (15 within two blocks, falling to 1 at sixteen, none beyond) that powers a lamp beside it. (Monsters from tests run
	 * alongside may come within its range, so each check is against whatever monster is nearest.)
	 */
	@GameTest
	public void sentinelSignalsMonsters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(GargoyleSentinelBlock.signal(0.0) == 15 && GargoyleSentinelBlock.signal(GargoyleSentinelBlock.NEAR) == 15
				&& GargoyleSentinelBlock.signal(GargoyleSentinelBlock.RANGE) == 1 && GargoyleSentinelBlock.signal(GargoyleSentinelBlock.RANGE + 0.1) == 0,
				"15 near, 1 at the edge of its range, none beyond");
		for (double d = 0.0; d < GargoyleSentinelBlock.RANGE; d += 0.5) {
			helper.assertTrue(GargoyleSentinelBlock.signal(d) >= GargoyleSentinelBlock.signal(d + 0.5), "The signal falls with distance at " + d);
		}
		BlockPos at = new BlockPos(4, 2, 2);
		helper.setBlock(at, block(JugcraftAgriculture.GARGOYLE_SENTINEL));
		BlockPos absolute = helper.absolutePos(at);
		Vec3 centre = Vec3.atCenterOf(absolute);
		GargoyleSentinelBlockEntity sentinel = (GargoyleSentinelBlockEntity) level.getBlockEntity(absolute);
		BlockPos lamp = at.west();
		helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
		Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(4, 2, 4));
		Mob spider = helper.spawnWithNoFreeWill(EntityTypes.SPIDER, new BlockPos(4, 2, 6));
		sentinel.look(level, absolute, level.getBlockState(absolute));
		Mob nearest = GargoyleSentinelBlockEntity.nearestEnemy(level, centre);
		helper.assertTrue(nearest != null && nearest != cow, "It watches a monster, not the nearer cow");
		helper.assertTrue(nearest == spider || nearest.position().distanceTo(centre) <= spider.position().distanceTo(centre), "the nearest monster");
		int power = level.getBlockState(absolute).getValue(GargoyleSentinelBlock.POWER);
		helper.assertTrue(power == GargoyleSentinelBlock.signal(nearest.position().distanceTo(centre)) && power > 0 && sentinel.target() == nearest.getId(),
				"A monster " + nearest.position().distanceTo(centre) + " away gives " + power);
		helper.assertTrue(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "and lights the lamp beside it");

		spider.discard();
		sentinel.look(level, absolute, level.getBlockState(absolute));
		Mob other = GargoyleSentinelBlockEntity.nearestEnemy(level, centre);
		int after = level.getBlockState(absolute).getValue(GargoyleSentinelBlock.POWER);
		helper.assertTrue(other != cow && sentinel.target() != spider.getId()
				&& after == (other == null ? 0 : GargoyleSentinelBlock.signal(other.position().distanceTo(centre))), "The spider gone, it looks past it: " + after);
		helper.succeed();
	}

	/**
	 * The Gargoyle Rainspout hangs on a wall (and falls without it), and pours into the first thing under its mouth within
	 * reach: a cauldron fills a level at a time to full, as do the Bubbling and Horned Skull Cauldrons; a block in the way
	 * or a cauldron out of reach gets nothing.
	 */
	@GameTest
	public void rainspoutFillsCauldronsBelow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wall = new BlockPos(4, 3, 6);
		BlockPos spout = wall.north();
		helper.setBlock(wall, Blocks.STONE);
		helper.setBlock(spout, block(JugcraftAgriculture.GARGOYLE_RAINSPOUT).defaultBlockState().setValue(GargoyleRainspoutBlock.FACING, Direction.NORTH));
		BlockPos spoutAbs = helper.absolutePos(spout);
		BlockPos below = spout.north().below();
		helper.setBlock(below, Blocks.CAULDRON);
		helper.assertTrue(helper.absolutePos(below).equals(GargoyleRainspoutBlock.landing(level, spoutAbs, Direction.NORTH, GargoyleRainspoutBlock.REACH)),
				"It pours onto the cauldron under its mouth");
		for (int i = 1; i <= LayeredCauldronBlock.MAX_FILL_LEVEL; i++) {
			helper.assertTrue(GargoyleRainspoutBlock.pour(level, spoutAbs, Direction.NORTH), "Pour " + i + " goes in");
			helper.assertTrue(helper.getBlockState(below).is(Blocks.WATER_CAULDRON) && helper.getBlockState(below).getValue(LayeredCauldronBlock.LEVEL) == i,
					"a level at a time");
		}
		helper.assertFalse(GargoyleRainspoutBlock.pour(level, spoutAbs, Direction.NORTH), "A full cauldron takes no more");

		helper.setBlock(below, block("bubbling_cauldron"));
		helper.assertTrue(GargoyleRainspoutBlock.pour(level, spoutAbs, Direction.NORTH)
				&& helper.getBlockState(below).getValue(BubblingCauldronBlock.CONTENTS) == BubblingCauldronBlock.Brew.WATER, "It fills a Bubbling Cauldron");
		helper.assertFalse(GargoyleRainspoutBlock.pour(level, spoutAbs, Direction.NORTH), "but not one with something in it");
		helper.setBlock(below, block(JugcraftAgriculture.HORNED_SKULL_CAULDRON));
		HornedSkullCauldronBlockEntity pot = (HornedSkullCauldronBlockEntity) level.getBlockEntity(helper.absolutePos(below));
		helper.assertTrue(GargoyleRainspoutBlock.pour(level, spoutAbs, Direction.NORTH)
				&& helper.getBlockState(below).getValue(HornedSkullCauldronBlock.LEVEL) == 1 && pot.isWater(), "and a Horned Skull Cauldron");

		helper.setBlock(below, Blocks.AIR);
		helper.setBlock(below.below(), Blocks.CAULDRON);
		helper.assertTrue(GargoyleRainspoutBlock.landing(level, spoutAbs, Direction.NORTH, 1) == null, "Out of reach, it lands on nothing");
		helper.setBlock(below, Blocks.STONE);
		helper.assertFalse(GargoyleRainspoutBlock.pour(level, spoutAbs, Direction.NORTH), "A block in the way gets nothing");
		helper.assertTrue(helper.getBlockState(below.below()).is(Blocks.CAULDRON), "and the cauldron under it stays empty");

		helper.setBlock(wall, Blocks.AIR);
		helper.assertBlockNotPresent(block(JugcraftAgriculture.GARGOYLE_RAINSPOUT), spout);
		helper.succeed();
	}

	/**
	 * The Chimera Finial reads rain as 7 and a storm as 15, but nothing from a dry sky, a desert's or under a roof (snow
	 * counts as rain); placed, it shows the weather where it is and a roofed one shows clear.
	 */
	@GameTest(maxTicks = 40)
	public void finialReadsTheWeather(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(ChimeraFinialBlock.weather(true, false, true, Biome.Precipitation.RAIN) == ChimeraFinialBlock.Weather.RAIN, "Rain on it is rain");
		helper.assertTrue(ChimeraFinialBlock.weather(true, true, true, Biome.Precipitation.RAIN) == ChimeraFinialBlock.Weather.STORM, "Thunder makes a storm");
		helper.assertTrue(ChimeraFinialBlock.weather(true, false, true, Biome.Precipitation.SNOW) == ChimeraFinialBlock.Weather.RAIN, "Snow counts as rain");
		helper.assertTrue(ChimeraFinialBlock.weather(false, false, true, Biome.Precipitation.RAIN) == ChimeraFinialBlock.Weather.CLEAR, "A dry sky is clear");
		helper.assertTrue(ChimeraFinialBlock.weather(true, true, false, Biome.Precipitation.RAIN) == ChimeraFinialBlock.Weather.CLEAR, "Under a roof it is clear");
		helper.assertTrue(ChimeraFinialBlock.weather(true, true, true, Biome.Precipitation.NONE) == ChimeraFinialBlock.Weather.CLEAR, "A desert stays clear");
		BlockState finial = block(JugcraftAgriculture.CHIMERA_FINIAL).defaultBlockState();
		BlockPos open = new BlockPos(2, 2, 2);
		BlockPos openAbs = helper.absolutePos(open);
		for (ChimeraFinialBlock.Weather weather : ChimeraFinialBlock.Weather.values()) {
			int signal = finial.setValue(ChimeraFinialBlock.WEATHER, weather).getSignal(level, openAbs, Direction.NORTH);
			helper.assertTrue(signal == weather.signal(), weather + " gives " + signal);
		}
		helper.assertTrue(ChimeraFinialBlock.Weather.RAIN.signal() == ChimeraFinialBlock.RAIN && ChimeraFinialBlock.Weather.STORM.signal() == ChimeraFinialBlock.STORM
				&& ChimeraFinialBlock.Weather.CLEAR.signal() == 0, "Rain 7, a storm 15, clear nothing");

		helper.setBlock(open, finial);
		BlockPos roofed = new BlockPos(6, 2, 2);
		helper.setBlock(roofed.above(), Blocks.STONE);
		helper.setBlock(roofed, finial);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(helper.getBlockState(open).getValue(ChimeraFinialBlock.WEATHER) == ChimeraFinialBlock.weather(level, openAbs),
					"Placed, it shows the weather where it is");
			helper.assertTrue(helper.getBlockState(roofed).getValue(ChimeraFinialBlock.WEATHER) == ChimeraFinialBlock.Weather.CLEAR, "Roofed, it shows clear");
			helper.succeed();
		});
	}

	/** Recipes and loot tables load. */
	@GameTest
	public void cryptDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> blocks = new ArrayList<>(List.of(COFFIN, JugcraftAgriculture.COFFIN_WARDROBE, JugcraftAgriculture.BONE_THRONE,
				JugcraftAgriculture.RIBCAGE_BOOKCASE, JugcraftAgriculture.SKULL_FOOTSTOOL, JugcraftAgriculture.VERTEBRA_LAMP, JugcraftAgriculture.COLOSSAL_SKULL,
				JugcraftAgriculture.COLOSSAL_RIB, JugcraftAgriculture.COLOSSAL_VERTEBRA, JugcraftAgriculture.COLOSSAL_FEMUR, JugcraftAgriculture.GARGOYLE_SENTINEL,
				JugcraftAgriculture.GARGOYLE_RAINSPOUT, JugcraftAgriculture.CHIMERA_FINIAL));
		for (String stone : JugcraftAgriculture.SARCOPHAGUS_STONES) {
			blocks.add(stone + "_sarcophagus");
		}
		List<String> recipes = new ArrayList<>(blocks);
		recipes.add(JugcraftAgriculture.KEY_BLANK);
		recipes.add(JugcraftAgriculture.KEY_COPYING);
		for (String id : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String id : blocks) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
