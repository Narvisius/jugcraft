package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FogMachineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FogMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedPortraitBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightsItem;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the first five Halloween decorations: String Light Hooks and their strands (stringing rules,
 * taking down, dropping when a hook goes, lighting from redstone or electricity), the Candy Bowl (only treats, one
 * a night per visitor, any time for its owner, saved), the Coffin (a chest whose lid lifts on both halves, a bed that
 * sets your spawn, dropping once), the Haunted Portrait (hangs on walls only, falls with its wall, four sitters) and
 * the Fog Machine (on by hand or redstone, only with electricity, its radius), and that their data loads.
 */
public class DecorGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static BlockState hook(AttachFace face) {
		return block("string_light_hook").defaultBlockState().setValue(StringLightHookBlock.FACE, face).setValue(StringLightHookBlock.FACING, Direction.NORTH);
	}

	private static StringLightHookBlockEntity hookAt(GameTestHelper helper, BlockPos pos) {
		return (StringLightHookBlockEntity) helper.getBlockEntity(pos, StringLightHookBlockEntity.class);
	}

	// ---------------------------------------------------------------- string lights

	/**
	 * The strand is strung by using it on two hooks (one strand used); hooks too far apart, the same hook twice, or two
	 * already strung are refused; the strand belongs to whichever hook has none yet, so hooks chain; sneaking with an
	 * empty hand takes a strand down (it drops); breaking a hook drops its strand.
	 */
	@GameTest
	public void stringLightsAreStrungBetweenHooks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos a = new BlockPos(1, 2, 1);
		BlockPos b = new BlockPos(6, 2, 1);
		BlockPos c = new BlockPos(6, 2, 6);
		for (BlockPos pos : List.of(a, b, c)) {
			helper.setBlock(pos, hook(AttachFace.FLOOR));
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), new ItemStack(item("jack_o_lantern_string_lights"), 2));
		helper.assertTrue(use(helper, player, a, Direction.UP).consumesAction(), "Using the strand on a hook starts it");
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "and uses nothing yet");
		helper.assertTrue(use(helper, player, b, Direction.UP).consumesAction(), "Using it on a second hook strings it");
		helper.assertTrue(player.getMainHandItem().getCount() == 1 && helper.absolutePos(b).equals(hookAt(helper, a).link()),
				"One strand runs from the first hook to the second");

		BlockPos absA = helper.absolutePos(a);
		BlockPos absB = helper.absolutePos(b);
		BlockPos absC = helper.absolutePos(c);
		helper.assertTrue(StringLightsItem.string(level, absA, absA) == StringLightsItem.Result.SAME_HOOK, "Not a hook to itself");
		helper.assertTrue(StringLightsItem.string(level, absA, absA.east(StringLightHookBlockEntity.MAX_LENGTH + 1)) == StringLightsItem.Result.TOO_FAR,
				"Not beyond " + StringLightHookBlockEntity.MAX_LENGTH + " blocks");
		helper.assertTrue(StringLightsItem.string(level, absA, absB) == StringLightsItem.Result.ALREADY_STRUNG
				&& StringLightsItem.string(level, absB, absA) == StringLightsItem.Result.ALREADY_STRUNG, "Two hooks are strung together once");
		helper.assertTrue(StringLightsItem.string(level, absA, absC) == StringLightsItem.Result.STRUNG && absA.equals(hookAt(helper, c).link()),
				"The first hook already holds a strand, so the second one takes it: hooks chain");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		use(helper, player, a, Direction.UP);
		helper.assertTrue(hookAt(helper, a).link() == null, "Sneaking with an empty hand takes the strand down");
		level.destroyBlock(absC, false);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item("jack_o_lantern_string_lights")) == 2,
				"The taken-down strand and the broken hook's strand drop: " + dropped(helper, item("jack_o_lantern_string_lights"))));
	}

	/** A strand whose far hook is gone comes down within a check, and drops. */
	@GameTest(maxTicks = 160)
	public void aStrandToAMissingHookComesDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos a = new BlockPos(1, 2, 1);
		BlockPos b = new BlockPos(5, 2, 5);
		helper.setBlock(a, hook(AttachFace.FLOOR));
		helper.setBlock(b, hook(AttachFace.FLOOR));
		helper.assertTrue(StringLightsItem.string(level, helper.absolutePos(a), helper.absolutePos(b)) == StringLightsItem.Result.STRUNG, "Strung");
		helper.setBlock(b, Blocks.AIR);
		helper.succeedWhen(() -> {
			helper.assertTrue(hookAt(helper, a).link() == null, "The strand to the missing hook comes down");
			helper.assertTrue(dropped(helper, item("jack_o_lantern_string_lights")) == 1, "and drops");
		});
	}

	/**
	 * A hook lights (light 10) from electricity, using it up, or from a redstone signal, using none; a strand glows if
	 * either hook is lit. The energy interface finds the hook's buffer.
	 */
	@GameTest
	public void hooksLightFromRedstoneOrElectricity(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos a = new BlockPos(2, 2, 2);
		BlockPos b = new BlockPos(5, 2, 2);
		helper.setBlock(a, hook(AttachFace.FLOOR));
		helper.setBlock(b, hook(AttachFace.FLOOR));
		StringLightHookBlockEntity hook = hookAt(helper, a);
		helper.assertTrue(EnergyStorage.SIDED.find(level, helper.absolutePos(a), null) == hook.energy(), "Cables reach the hook's buffer");
		helper.assertFalse(hook.update(level), "Unpowered, it is dark");
		hook.energy().setAmount(StringLightHookBlockEntity.CAPACITY);
		helper.assertTrue(hook.update(level) && helper.getBlockState(a).getLightEmission() == StringLightHookBlock.LIGHT, "Electricity lights it");
		helper.assertTrue(hook.energy().getAmount() == StringLightHookBlockEntity.CAPACITY - StringLightHookBlockEntity.USE * StringLightHookBlockEntity.PERIOD,
				"using " + StringLightHookBlockEntity.USE + " JE a tick");
		helper.assertTrue(StringLightHookBlockEntity.glows(level, helper.absolutePos(a), helper.absolutePos(b))
				&& StringLightHookBlockEntity.glows(level, helper.absolutePos(b), helper.absolutePos(a)), "A strand glows if either end is lit");
		hook.energy().setAmount(0);
		helper.assertFalse(hook.update(level), "Without power it goes dark");
		helper.setBlock(a.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(a).getValue(StringLightHookBlock.LIT) && hook.energy().getAmount() == 0,
				"A redstone signal lights it at once, using no electricity");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the candy bowl

	/**
	 * Treats go in (up to 64, nothing else); each visitor takes one a night and no more, the next night another; its
	 * owner takes any time; it shows how full it is; the record survives a save and load; breaking it spills the rest.
	 */
	@GameTest
	public void theCandyBowlGivesEachVisitorOneTreatANight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		ServerPlayer owner = player(helper, pos.south(), new ItemStack(item("candy_bowl")));
		owner.getMainHandItem().useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, hit(helper, pos.below(), Direction.UP)));
		CandyBowlBlockEntity bowl = (CandyBowlBlockEntity) helper.getBlockEntity(pos, CandyBowlBlockEntity.class);
		helper.assertTrue(owner.getUUID().equals(bowl.owner()), "Whoever places it owns it");

		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
		helper.assertTrue(bowl.add(owner.getMainHandItem()) == 0 && owner.getMainHandItem().getCount() == 4, "Dirt is no treat");
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("candy_corn"), 10));
		helper.assertTrue(use(helper, owner, pos, Direction.UP).consumesAction() && bowl.count() == 10 && owner.getMainHandItem().isEmpty(),
				"Using treats on it fills it");
		helper.assertTrue(helper.getBlockState(pos).getValue(CandyBowlBlock.FILL) == 1, "It looks a little full");
		ItemStack lots = new ItemStack(Items.COOKIE, 64);
		helper.assertTrue(bowl.add(lots) == CandyBowlBlockEntity.CAPACITY - 10 && lots.getCount() == 10, "It holds " + CandyBowlBlockEntity.CAPACITY);
		helper.assertTrue(helper.getBlockState(pos).getValue(CandyBowlBlock.FILL) == 3, "and looks heaped full");

		long tonight = TrickOrTreat.night(TrickOrTreat.DUSK + TrickOrTreat.DAY * 4000);
		ServerPlayer visitor = player(helper, pos.north(), ItemStack.EMPTY);
		helper.assertTrue(bowl.take(visitor, tonight) == CandyBowlBlockEntity.Taken.TAKEN, "A visitor takes a treat");
		helper.assertTrue(bowl.take(visitor, tonight) == CandyBowlBlockEntity.Taken.HAD_ONE, "but only one a night");
		helper.assertTrue(bowl.take(owner, tonight) == CandyBowlBlockEntity.Taken.TAKEN && bowl.take(owner, tonight) == CandyBowlBlockEntity.Taken.TAKEN,
				"Its owner takes as they like");
		helper.assertTrue(bowl.count() == CandyBowlBlockEntity.CAPACITY - 3, "Three treats are gone: " + bowl.count());
		int inPockets = count(visitor, item("candy_corn")) + count(visitor, Items.COOKIE);
		helper.assertTrue(inPockets == 1, "The visitor has one treat: " + inPockets);

		var saved = bowl.saveWithoutMetadata(level.registryAccess());
		CandyBowlBlockEntity loaded = new CandyBowlBlockEntity(helper.absolutePos(pos), helper.getBlockState(pos));
		loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(loaded.count() == bowl.count() && owner.getUUID().equals(loaded.owner()), "Treats and owner survive a save and load");
		helper.assertTrue(loaded.take(visitor, tonight) == CandyBowlBlockEntity.Taken.HAD_ONE, "and so does who had one tonight");
		helper.assertTrue(bowl.take(visitor, tonight + 1) == CandyBowlBlockEntity.Taken.TAKEN, "Tomorrow night the visitor may take another");

		int left = bowl.count();
		level.destroyBlock(helper.absolutePos(pos), true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item("candy_corn")) + dropped(helper, Items.COOKIE) == left
				&& dropped(helper, item("candy_bowl")) == 1, "Breaking it spills the " + left + " treats and drops the bowl"));
	}

	// ---------------------------------------------------------------- the coffin

	/**
	 * Placed, it lies two blocks long; opening it shows a 27-slot chest and lifts the lid on both halves, closing lowers
	 * it; sneak-using it sets your spawn like a bed; breaking it spills what is inside and drops one coffin.
	 */
	@GameTest(maxTicks = 60)
	public void theCoffinIsAChestAndABed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos foot = new BlockPos(3, 2, 4);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("coffin")));
		player.setYRot(180.0F);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, foot.below(), Direction.UP)));
		BlockState footState = helper.getBlockState(foot);
		helper.assertTrue(footState.is(block("coffin")) && footState.getValue(CoffinBlock.PART) == BedPart.FOOT, "The foot is where it was placed");
		BlockPos head = foot.relative(footState.getValue(CoffinBlock.FACING));
		helper.assertTrue(helper.getBlockState(head).is(block("coffin")) && helper.getBlockState(head).getValue(CoffinBlock.PART) == BedPart.HEAD,
				"and the head beside it");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(use(helper, player, foot, Direction.UP).consumesAction(), "Using it opens it");
		helper.assertTrue(player.containerMenu instanceof ChestMenu menu && menu.getContainer().getContainerSize() == CoffinBlockEntity.SLOTS,
				"as a " + CoffinBlockEntity.SLOTS + "-slot chest");
		helper.assertTrue(helper.getBlockState(head).getValue(CoffinBlock.OPEN) && helper.getBlockState(foot).getValue(CoffinBlock.OPEN),
				"The lid lifts on both halves");
		CoffinBlockEntity coffin = (CoffinBlockEntity) helper.getBlockEntity(head, CoffinBlockEntity.class);
		coffin.setItem(0, new ItemStack(Items.BONE, 7));
		player.closeContainer();
		helper.assertFalse(helper.getBlockState(head).getValue(CoffinBlock.OPEN) || helper.getBlockState(foot).getValue(CoffinBlock.OPEN),
				"Closing lowers it");

		var before = player.getRespawnConfig();
		player.setShiftKeyDown(true);
		use(helper, player, foot, Direction.UP);
		helper.assertTrue(player.getRespawnConfig() != null && !player.getRespawnConfig().equals(before),
				"Lying down in it sets the player's spawn: " + player.getRespawnConfig());
		player.setShiftKeyDown(false);
		if (player.isSleeping()) {
			player.stopSleepInBed(true, true);
		}

		level.destroyBlock(helper.absolutePos(foot), true);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("coffin"), head);
			helper.assertTrue(dropped(helper, Items.BONE) == 7 && dropped(helper, item("coffin")) == 1, "Breaking it spills the bones and drops one coffin");
		});
	}

	// ---------------------------------------------------------------- the haunted portrait

	/** It hangs only on the side of a block, cycles through four sitters, and falls (dropping) when its wall goes. */
	@GameTest
	public void theHauntedPortraitHangsOnAWall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wall = new BlockPos(3, 2, 4);
		helper.setBlock(wall, Blocks.OAK_PLANKS);
		BlockPos pos = wall.north();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("haunted_portrait"), 2));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, new BlockPos(5, 1, 5), Direction.UP)));
		helper.assertBlockNotPresent(block("haunted_portrait"), new BlockPos(5, 2, 5));
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "It does not stand on the floor");
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, wall, Direction.NORTH)));
		helper.assertTrue(helper.getBlockState(pos).is(block("haunted_portrait"))
				&& helper.getBlockState(pos).getValue(HauntedPortraitBlock.FACING) == Direction.NORTH, "It hangs on the wall, facing out");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		HauntedPortraitBlock.Portrait first = helper.getBlockState(pos).getValue(HauntedPortraitBlock.PORTRAIT);
		for (int i = 0; i < HauntedPortraitBlock.Portrait.values().length; i++) {
			use(helper, player, pos, Direction.NORTH);
		}
		helper.assertTrue(helper.getBlockState(pos).getValue(HauntedPortraitBlock.PORTRAIT) == first, "Four sitters, round and back");
		for (HauntedPortraitBlock.Portrait portrait : HauntedPortraitBlock.Portrait.values()) {
			for (int[] eye : portrait.eyes) {
				helper.assertTrue(eye[0] >= 1 && eye[1] >= 1 && eye[0] + eye[2] <= 15 && eye[1] + eye[3] <= 15 && eye[2] >= 1 && eye[3] >= 1,
						portrait + "'s eyes lie inside the frame");
			}
		}
		level.destroyBlock(helper.absolutePos(wall), false);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("haunted_portrait"), pos);
			helper.assertTrue(dropped(helper, item("haunted_portrait")) == 1, "It falls with its wall");
		});
	}

	// ---------------------------------------------------------------- the fog machine

	/**
	 * Switched on by hand or by redstone, it runs only with electricity (using 16 JE a tick) and stops when the
	 * energy runs out; sneak-use steps its radius; the energy interface finds its buffer.
	 */
	@GameTest
	public void theFogMachineRunsOnElectricity(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("fog_machine").defaultBlockState().setValue(FogMachineBlock.FACING, Direction.NORTH));
		FogMachineBlockEntity machine = (FogMachineBlockEntity) helper.getBlockEntity(pos, FogMachineBlockEntity.class);
		helper.assertTrue(EnergyStorage.SIDED.find(level, helper.absolutePos(pos), Direction.SOUTH) == machine.energy(), "Cables reach its buffer");
		ServerPlayer player = player(helper, pos.north(), ItemStack.EMPTY);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(FogMachineBlock.ENABLED) && !helper.getBlockState(pos).getValue(FogMachineBlock.RUNNING),
				"Switched on without power, it does not run");
		machine.energy().setAmount(FogMachineBlockEntity.CAPACITY);
		helper.assertTrue(machine.update(level) && helper.getBlockState(pos).getLightEmission() == FogMachineBlock.LIGHT, "With power it runs");
		helper.assertTrue(machine.energy().getAmount() == FogMachineBlockEntity.CAPACITY - FogMachineBlockEntity.USE, "using " + FogMachineBlockEntity.USE + " JE");
		use(helper, player, pos, Direction.NORTH);
		helper.assertFalse(helper.getBlockState(pos).getValue(FogMachineBlock.RUNNING), "Switched off, it stops");
		helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(pos).getValue(FogMachineBlock.RUNNING), "A redstone signal switches it on");
		machine.energy().setAmount(FogMachineBlockEntity.USE);
		machine.update(level);
		helper.assertFalse(machine.update(level), "When the energy runs out it stops");

		int radius = FogMachineBlock.radius(helper.getBlockState(pos));
		player.setShiftKeyDown(true);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(FogMachineBlock.radius(helper.getBlockState(pos)) == radius + 4, "Sneak-use widens the fog by 4 blocks");
		for (int i = 0; i < 3; i++) {
			use(helper, player, pos, Direction.NORTH);
		}
		helper.assertTrue(FogMachineBlock.radius(helper.getBlockState(pos)) == radius, "and comes round again");
		helper.succeed();
	}

	// ---------------------------------------------------------------- data

	/** The recipes, loot tables, tool tags and the fog particle load. */
	@GameTest
	public void decorationDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("string_light_hook", "jack_o_lantern_string_lights", "candy_bowl", "coffin", "haunted_portrait", "fog_machine")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String id : List.of("string_light_hook", "candy_bowl", "coffin", "haunted_portrait", "fog_machine")) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(block("fog_machine").defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
				&& block("coffin").defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), "Tools fit");
		helper.assertTrue(Jugcraft.id("fog").equals(BuiltInRegistries.PARTICLE_TYPE.getKey(JugcraftAgriculture.FOG)), "The fog particle is registered");
		helper.succeed();
	}
}
