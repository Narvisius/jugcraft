package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandMenu;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestCheer;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestEffigyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestEffigyBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SingingPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightsItem;
import io.github.jimbozoomer.jugcraft.agriculture.TrebuchetBlockEntity;
import io.github.jimbozoomer.jugcraft.town.Jugs;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween decorations batch 20, Pumpkin Night: the Red Kuri and Kabocha carve into their own
 * hand-carved pumpkins and fly from the trebuchet as listed; the Singing Pumpkins tune by use (down while sneaking,
 * wrapping round), name their notes, and sing on a rising redstone edge; the garlands string between hooks, come down
 * as their own items and are remembered; the Harvest Effigy stands three blocks tall, wears a head and a dyed cloak,
 * lights only at night and in the dry, cheers players once a night, goes out, and burns through to Effigy Ashes,
 * dropping its head; the ashes give Hearth Ash, which grows crops; the Farm Stand belongs to whoever places it, sells
 * one item at a time with the Jugs moving buyer to owner exactly, refuses every bad sale without moving anything, keeps
 * prices in bounds, takes price changes only from its owner, is reached only near, is taken down only by its owner and
 * remembers everything; and the data loads.
 */
public class PumpkinNightGameTests {
	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Item dye(String colour) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(colour + "_dye"));
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

	/** Places the item {@code id} on the block under {@code at}, by {@code player} turned to {@code yaw} (0 looks south). */
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

	// ---------------------------------------------------------------- 16. the heirlooms

	/** Each new heirloom carves into its own hand-carved pumpkin, which carves back from it, and flies as listed. */
	@GameTest
	public void heirloomsCarveAndFly(GameTestHelper helper) {
		String[][] heirlooms = {{"red_kuri_pumpkin", "0.99", "1.05"}, {"kabocha_pumpkin", "0.96", "1.02"}};
		for (String[] heirloom : heirlooms) {
			Block pumpkin = block(heirloom[0]);
			Block carved = block("hand_carved_" + heirloom[0]);
			helper.assertTrue(JugcraftAgriculture.carvedFrom(pumpkin) == carved, heirloom[0] + " carves into its own hand-carved pumpkin");
			helper.assertTrue(JugcraftAgriculture.plainPumpkin(carved) == pumpkin, "which is drawn on " + heirloom[0]);
			helper.assertTrue(Math.abs(TrebuchetBlockEntity.factor(new ItemStack(pumpkin)) - Double.parseDouble(heirloom[1])) < 1e-9
					&& Math.abs(TrebuchetBlockEntity.factor(new ItemStack(carved)) - Double.parseDouble(heirloom[2])) < 1e-9,
					heirloom[0] + " flies from the trebuchet at " + heirloom[1] + ", carved at " + heirloom[2]);
			Item seeds = item(heirloom[0] + "_seeds");
			helper.assertTrue(seeds instanceof BlockItem planted && BuiltInRegistries.BLOCK.getKey(planted.getBlock()).getPath().equals(heirloom[0] + "_stem"),
					heirloom[0] + "'s seeds plant its stem");
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- 20. the singing pumpkins

	/** Use tunes a singing pumpkin up a note, sneaking down, wrapping round; it names its notes as a note block does. */
	@GameTest
	public void singingPumpkinTunes(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(4, 2, 4);
		helper.setBlock(pos, block("singing_pumpkin_tenor"));
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		helper.assertTrue(helper.getBlockState(pos).getValue(SingingPumpkinBlock.NOTE) == 12, "It starts on its middle note");
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SingingPumpkinBlock.NOTE) == 13, "Use tunes it up a note");
		player.setShiftKeyDown(true);
		use(helper, player, pos, Direction.NORTH);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SingingPumpkinBlock.NOTE) == 11, "Sneaking tunes it down");
		helper.setBlock(pos, helper.getBlockState(pos).setValue(SingingPumpkinBlock.NOTE, 0));
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SingingPumpkinBlock.NOTE) == SingingPumpkinBlock.NOTES - 1, "Down from the bottom wraps to the top");
		player.setShiftKeyDown(false);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SingingPumpkinBlock.NOTE) == 0, "and up from the top to the bottom");
		helper.assertTrue(SingingPumpkinBlock.pitch(12) == 1.0F && Math.abs(SingingPumpkinBlock.pitch(0) - 0.5F) < 1e-6
				&& Math.abs(SingingPumpkinBlock.pitch(24) - 2.0F) < 1e-6, "Two octaves round the middle note");
		helper.assertTrue(SingingPumpkinBlock.noteName(0).equals("F#0") && SingingPumpkinBlock.noteName(6).equals("C1")
				&& SingingPumpkinBlock.noteName(12).equals("F#1") && SingingPumpkinBlock.noteName(24).equals("F#2"), "The notes are named as a note block's");
		helper.assertTrue(((SingingPumpkinBlock) block("singing_pumpkin_bass")).voice() == SingingPumpkinBlock.Voice.BASS
				&& ((SingingPumpkinBlock) block("singing_pumpkin_soprano")).voice() == SingingPumpkinBlock.Voice.SOPRANO, "Each block sings its own voice");
		helper.assertTrue(helper.getBlockState(pos).getLightEmission() == SingingPumpkinBlock.LIGHT, "It glows");
		helper.succeed();
	}

	/** A rising redstone edge makes it sing (the block event marks the time); staying powered doesn't sing again. */
	@GameTest(maxTicks = 40)
	public void singingPumpkinSingsOnAPulse(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(4, 2, 4);
		helper.setBlock(pos, block("singing_pumpkin_alto"));
		DecorationBlockEntity pumpkin = helper.getBlockEntity(pos, DecorationBlockEntity.class);
		helper.assertTrue(pumpkin.marked() == Long.MIN_VALUE, "Unpowered, it hasn't sung");
		helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(helper.getBlockState(pos).getValue(SingingPumpkinBlock.POWERED), "Powered");
			long sang = pumpkin.marked();
			helper.assertTrue(sang != Long.MIN_VALUE, "it sings");
			helper.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(pumpkin.marked() == sang, "Already powered, a second signal doesn't sing again");
				helper.setBlock(pos.east(), Blocks.AIR);
				helper.setBlock(pos.west(), Blocks.AIR);
				helper.runAfterDelay(2, () -> {
					helper.assertFalse(helper.getBlockState(pos).getValue(SingingPumpkinBlock.POWERED), "The signal gone, it waits for the next");
					helper.succeed();
				});
			});
		});
	}

	// ---------------------------------------------------------------- 18. the garlands

	private static BlockState hook() {
		return block("string_light_hook").defaultBlockState().setValue(StringLightHookBlock.FACE, AttachFace.FLOOR).setValue(StringLightHookBlock.FACING, Direction.NORTH);
	}

	/** Each garland strings between two hooks with its own item, keeps its kind through a save, and comes down as itself. */
	@GameTest(maxTicks = 40)
	public void garlandsHangBetweenHooks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos a = new BlockPos(1, 2, 1);
		BlockPos b = new BlockPos(6, 2, 1);
		BlockPos c = new BlockPos(1, 2, 6);
		BlockPos d = new BlockPos(6, 2, 6);
		for (BlockPos pos : List.of(a, b, c, d)) {
			helper.setBlock(pos, hook());
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), new ItemStack(item(JugcraftAgriculture.PUMPKIN_VINE_GARLAND)));
		use(helper, player, a, Direction.UP);
		use(helper, player, b, Direction.UP);
		StringLightHookBlockEntity first = helper.getBlockEntity(a, StringLightHookBlockEntity.class);
		helper.assertTrue(helper.absolutePos(b).equals(first.link()) && first.strand() == StringLightHookBlockEntity.Strand.PUMPKIN_VINE
				&& player.getMainHandItem().isEmpty(), "The vine garland strings the hooks, its item used");
		helper.assertTrue(StringLightsItem.string(level, helper.absolutePos(c), helper.absolutePos(d), StringLightHookBlockEntity.Strand.AUTUMN_LEAVES)
				== StringLightsItem.Result.STRUNG, "and so does the autumn leaf garland");
		StringLightHookBlockEntity leaves = helper.getBlockEntity(c, StringLightHookBlockEntity.class);
		CompoundTag saved = leaves.saveWithoutMetadata(level.registryAccess());
		StringLightHookBlockEntity copy = new StringLightHookBlockEntity(helper.absolutePos(c), helper.getBlockState(c));
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.strand() == StringLightHookBlockEntity.Strand.AUTUMN_LEAVES, "A garland is remembered as itself");
		first.unstring(true);
		leaves.unstring(true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item(JugcraftAgriculture.PUMPKIN_VINE_GARLAND)) == 1
				&& dropped(helper, item(JugcraftAgriculture.AUTUMN_LEAF_GARLAND)) == 1, "Taken down, each comes down as its own item"));
	}

	// ---------------------------------------------------------------- 19. the harvest effigy

	private static BlockPos placeEffigy(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		place(helper, player, JugcraftAgriculture.HARVEST_EFFIGY, at, 180.0F);
		return at;
	}

	/** He stands three blocks tall, wears a head, takes a dye on all of him, and lights only at night in the dry. */
	@GameTest(maxTicks = 40)
	public void effigyStandsAndDresses(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = placeEffigy(helper, player, new BlockPos(4, 2, 4));
		Block effigy = block(JugcraftAgriculture.HARVEST_EFFIGY);
		for (int part = 0; part < 3; part++) {
			BlockState state = helper.getBlockState(master.above(part));
			helper.assertTrue(state.is(effigy) && state.getValue(HarvestEffigyBlock.PART) == part && !state.getValue(MultiDecorationBlock.LIT),
					"Part " + part + " stands, unlit");
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARVED_PUMPKIN));
		use(helper, player, master.above(2), Direction.SOUTH);
		HarvestEffigyBlockEntity works = helper.getBlockEntity(master, HarvestEffigyBlockEntity.class);
		helper.assertTrue(works.head().is(Items.CARVED_PUMPKIN) && player.getMainHandItem().isEmpty(), "A carved pumpkin goes on as his head");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(dye("orange")));
		use(helper, player, master.above(1), Direction.SOUTH);
		for (int part = 0; part < 3; part++) {
			helper.assertTrue(helper.getBlockState(master.above(part)).getValue(HarvestEffigyBlock.CLOAK) == DyeColor.ORANGE, "The dye colours all of his cloak");
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, master, Direction.SOUTH);
		helper.assertTrue(works.head().isEmpty() && player.getInventory().countItem(Items.CARVED_PUMPKIN) == 1, "An empty hand takes his head back");
		helper.assertTrue("message.jugcraft.effigy.day".equals(HarvestEffigyBlock.refusal(false, false))
				&& "message.jugcraft.effigy.wet".equals(HarvestEffigyBlock.refusal(true, true)) && HarvestEffigyBlock.refusal(true, false) == null,
				"He catches only at night, and not in the rain");
		helper.succeed();
	}

	/** Lit, he burns bright and cheers the players near, once a night each; put out, he can be lit again. */
	@GameTest(maxTicks = 80)
	public void effigyCheersOnceANight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = placeEffigy(helper, player, new BlockPos(4, 2, 4));
		helper.setBlock(master.above(4), Blocks.STONE); // A roof: no rain puts him out mid-test.
		HarvestEffigyBlock.ignite(level, helper.absolutePos(master));
		for (int part = 0; part < 3; part++) {
			BlockState state = helper.getBlockState(master.above(part));
			helper.assertTrue(state.getValue(MultiDecorationBlock.LIT) && state.getLightEmission() == HarvestEffigyBlock.LIGHT, "Every part burns bright");
		}
		helper.runAfterDelay(HarvestEffigyBlock.CHECK_TICKS + 2, () -> {
			helper.assertTrue(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.LUCK)
					&& HarvestCheer.cheered(level, player.getUUID()), "A player near is cheered: Regeneration and Luck");
			player.removeAllEffects();
			helper.assertFalse(HarvestCheer.give(level, player), "Only once a night");
			helper.assertTrue(HarvestCheer.night(0) == 0 && HarvestCheer.night(23999) == 0 && HarvestCheer.night(24000) == 1, "A night is a day's turn");
			HarvestEffigyBlock.extinguish(level, helper.absolutePos(master));
			helper.assertFalse(helper.getBlockState(master.above(2)).getValue(MultiDecorationBlock.LIT), "Put out, he goes dark all over");
			HarvestEffigyBlock.ignite(level, helper.absolutePos(master));
			helper.assertTrue(helper.getBlockState(master).getValue(MultiDecorationBlock.LIT), "and lights again");
			helper.succeed();
		});
	}

	/** Burnt through, he falls into Effigy Ashes and drops his head; the ashes, shovelled, give Hearth Ash. */
	@GameTest(maxTicks = 700)
	public void effigyBurnsToAshes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = placeEffigy(helper, player, new BlockPos(4, 2, 4));
		helper.setBlock(master.above(4), Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARVED_PUMPKIN));
		use(helper, player, master.above(2), Direction.SOUTH);
		HarvestEffigyBlock.ignite(level, helper.absolutePos(master));
		helper.runAfterDelay(HarvestEffigyBlock.BURN_TICKS + 10, () -> {
			helper.assertTrue(helper.getBlockState(master).is(block(JugcraftAgriculture.EFFIGY_ASHES)), "He falls into Effigy Ashes");
			helper.assertTrue(helper.getBlockState(master.above()).isAir() && helper.getBlockState(master.above(2)).isAir(), "and nothing of him stands");
			helper.assertTrue(dropped(helper, Items.CARVED_PUMPKIN) == 1, "His head drops");
			helper.assertTrue(dropped(helper, item(JugcraftAgriculture.HARVEST_EFFIGY)) == 0, "He is gone, not dropped");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
			use(helper, player, master, Direction.UP);
			helper.assertTrue(helper.getBlockState(master).isAir() && player.getMainHandItem().getDamageValue() == 1, "A shovel scoops the ashes up");
			helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item(JugcraftAgriculture.HEARTH_ASH)) == 2, "They give 2 Hearth Ash"));
		});
	}

	/** Hearth Ash grows the crops round where it is used, and is used up. */
	@GameTest
	public void hearthAshGrowsCrops(GameTestHelper helper) {
		floor(helper);
		BlockPos crop = new BlockPos(4, 3, 4);
		for (int dx = -1; dx <= 1; dx++) {
			helper.setBlock(crop.below().offset(dx, 0, 0), Blocks.FARMLAND);
			helper.setBlock(crop.offset(dx, 0, 0), Blocks.WHEAT);
		}
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), new ItemStack(item(JugcraftAgriculture.HEARTH_ASH), 2));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(crop)), Direction.UP, helper.absolutePos(crop), false)));
		int grown = 0;
		for (int dx = -1; dx <= 1; dx++) {
			grown += helper.getBlockState(crop.offset(dx, 0, 0)).getValue(CropBlock.AGE) > 0 ? 1 : 0;
		}
		helper.assertTrue(grown == 3 && player.getMainHandItem().getCount() == 1, "All three crops grew from one Hearth Ash: " + grown);
		helper.succeed();
	}

	// ---------------------------------------------------------------- 17. the farm stand

	private static FarmStandBlockEntity placeStand(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
		place(helper, owner, JugcraftAgriculture.FARM_STAND, at, 0.0F);
		return helper.getBlockEntity(at, FarmStandBlockEntity.class);
	}

	/** Whoever places a stand owns it; it fills two blocks with its records on the first. */
	@GameTest
	public void farmStandBelongsToItsMaker(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = new BlockPos(4, 2, 4);
		FarmStandBlockEntity stand = placeStand(helper, owner, master);
		BlockState part = helper.getBlockState(master.west());
		helper.assertTrue(part.is(block(JugcraftAgriculture.FARM_STAND)) && part.getValue(FarmStandBlock.PART) == 1
				&& helper.getBlockState(master).getValue(FarmStandBlock.FACING) == Direction.NORTH, "Facing its maker, its second block to their right");
		helper.assertTrue(stand.isOwner(owner) && stand.ownerName().equals(owner.getName().getString()), "Its maker owns it");
		helper.assertTrue(level.getBlockEntity(helper.absolutePos(master.west())) == null, "Its records are on its first block alone");
		stand.setPrice(1, 0);
		helper.assertTrue(stand.price(1) == 1, "No price is under 1 Jug");
		stand.setPrice(1, Long.MAX_VALUE);
		helper.assertTrue(stand.price(1) == FarmStandBlockEntity.maxPrice() && FarmStandBlockEntity.maxPrice() == TownShops.get().maxBalance,
				"nor over the town's maximum");
		stand.setPrice(2, 42);
		stand.crates().setItem(2, new ItemStack(Items.APPLE, 9));
		CompoundTag saved = stand.saveWithoutMetadata(level.registryAccess());
		FarmStandBlockEntity copy = new FarmStandBlockEntity(helper.absolutePos(master), helper.getBlockState(master));
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.isOwner(owner) && copy.ownerName().equals(stand.ownerName()) && copy.price(2) == 42 && copy.crate(2).is(Items.APPLE)
				&& copy.crate(2).getCount() == 9, "It remembers its owner, its prices and its goods");
		helper.succeed();
	}

	/** A sale moves exactly the price from buyer to owner and one item from crate to buyer; every bad sale moves nothing. */
	@GameTest
	public void farmStandSellsForJugs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = new BlockPos(4, 2, 4);
		FarmStandBlockEntity stand = placeStand(helper, owner, master);
		stand.crates().setItem(0, new ItemStack(Items.PUMPKIN, 5));
		stand.setPrice(0, 7);
		ServerPlayer buyer = player(helper, new BlockPos(3, 2, 2), ItemStack.EMPTY);
		Jugs.add(server, buyer.getUUID(), 10);
		helper.assertTrue(FarmStandMenu.buy(buyer, stand, 0), "A buyer with the Jugs buys");
		helper.assertTrue(Jugs.balance(server, buyer.getUUID()) == 3 && Jugs.balance(server, owner.getUUID()) == 7,
				"Exactly 7 Jugs move from buyer to owner: " + Jugs.balance(server, buyer.getUUID()) + ", " + Jugs.balance(server, owner.getUUID()));
		helper.assertTrue(stand.crate(0).getCount() == 4 && buyer.getInventory().countItem(Items.PUMPKIN) == 1, "and one pumpkin moves from crate to buyer");

		helper.assertFalse(FarmStandMenu.buy(buyer, stand, 0), "Short of the price, no sale");
		helper.assertFalse(FarmStandMenu.buy(buyer, stand, 3), "An empty crate sells nothing");
		helper.assertFalse(FarmStandMenu.buy(owner, stand, 0), "An owner can't buy from their own stand");
		helper.assertFalse(FarmStandMenu.buy(buyer, stand, FarmStandBlockEntity.CRATES), "There is no seventh crate");
		long max = TownShops.get().maxBalance;
		Jugs.add(server, owner.getUUID(), max - 7 - 3);
		Jugs.add(server, buyer.getUUID(), 7);
		helper.assertFalse(FarmStandMenu.buy(buyer, stand, 0), "A sale that would pass the owner's maximum is refused");
		helper.assertTrue(Jugs.balance(server, buyer.getUUID()) == 10 && Jugs.balance(server, owner.getUUID()) == max - 3 && stand.crate(0).getCount() == 4
				&& buyer.getInventory().countItem(Items.PUMPKIN) == 1, "Every refused sale moved nothing");

		helper.assertTrue(FarmStandMenu.inReach(buyer, stand), "A buyer at the stand is in reach");
		buyer.setPos(buyer.getX() + 20.0, buyer.getY(), buyer.getZ());
		helper.assertFalse(FarmStandMenu.inReach(buyer, stand), "one twenty blocks off is not");
		helper.assertTrue(FarmStandMenu.priceButton(0, 0, false) == FarmStandMenu.PRICE_BASE
				&& FarmStandMenu.priceButton(FarmStandBlockEntity.CRATES - 1, FarmStandMenu.PRICE_STEPS.length - 1, true)
				== FarmStandMenu.PRICE_BASE + FarmStandBlockEntity.CRATES * FarmStandMenu.PRICE_STEPS.length * 2 - 1, "Each price button has its own ID");
		helper.succeed();
	}

	/** Only its owner changes its prices or stocks its crates from its screen; buyers buy from theirs. */
	@GameTest
	public void farmStandScreenObeysItsOwner(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		BlockPos master = new BlockPos(4, 2, 4);
		FarmStandBlockEntity stand = placeStand(helper, owner, master);
		stand.crates().setItem(0, new ItemStack(Items.APPLE, 3));
		stand.setPrice(0, 5);
		FarmStandMenu.open(owner, stand);
		helper.assertTrue(owner.containerMenu instanceof FarmStandMenu, "Using it opens its screen");
		FarmStandMenu mine = (FarmStandMenu) owner.containerMenu;
		helper.assertTrue(mine.clickMenuButton(owner, FarmStandMenu.priceButton(0, 1, true)) && stand.price(0) == 15, "The owner chalks 10 more: 15");
		helper.assertTrue(mine.clickMenuButton(owner, FarmStandMenu.priceButton(0, 0, false)) && stand.price(0) == 14, "and 1 less: 14");
		helper.assertTrue(mine.getSlot(0).mayPickup(owner) && mine.getSlot(0).mayPlace(new ItemStack(Items.APPLE)), "The owner works the crates");

		ServerPlayer buyer = player(helper, new BlockPos(5, 2, 2), ItemStack.EMPTY);
		Jugs.add(server, buyer.getUUID(), 20);
		FarmStandMenu.open(buyer, stand);
		FarmStandMenu theirs = (FarmStandMenu) buyer.containerMenu;
		helper.assertFalse(theirs.clickMenuButton(buyer, FarmStandMenu.priceButton(0, 3, false)), "A buyer can't change a price");
		helper.assertTrue(stand.price(0) == 14, "which stays 14");
		helper.assertFalse(theirs.getSlot(0).mayPickup(buyer) || theirs.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "nor take or put goods");
		helper.assertTrue(theirs.clickMenuButton(buyer, 0) && Jugs.balance(server, buyer.getUUID()) == 6 && stand.crate(0).getCount() == 2,
				"but buys with the crate's button");
		helper.assertFalse(theirs.clickMenuButton(buyer, 0), "and not again within " + FarmStandMenu.RATE_TICKS + " ticks");
		helper.succeed();
	}

	/** Only its owner takes it down, whole, its goods spilling; anyone else is refused and it stands. */
	@GameTest(maxTicks = 40)
	public void farmStandIsTakenDownByItsOwner(GameTestHelper helper) {
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		BlockPos master = new BlockPos(4, 2, 4);
		FarmStandBlockEntity stand = placeStand(helper, owner, master);
		stand.crates().setItem(4, new ItemStack(Items.CARROT, 6));
		ServerPlayer thief = player(helper, new BlockPos(3, 2, 2), ItemStack.EMPTY);
		helper.assertFalse(FarmStandBlock.mayBreak(thief, stand), "Someone else may not take it down");
		helper.assertFalse(thief.gameMode.destroyBlock(helper.absolutePos(master.west())), "Breaking it is refused");
		helper.assertTrue(helper.getBlockState(master).is(block(JugcraftAgriculture.FARM_STAND))
				&& helper.getBlockState(master.west()).is(block(JugcraftAgriculture.FARM_STAND)), "and it stands whole");
		helper.assertTrue(owner.gameMode.destroyBlock(helper.absolutePos(master.west())), "Its owner takes it down");
		helper.assertTrue(helper.getBlockState(master).isAir() && helper.getBlockState(master.west()).isAir(), "whole");
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, Items.CARROT) == 6 && dropped(helper, item(JugcraftAgriculture.FARM_STAND)) == 1,
				"Its goods spill and it drops once: " + dropped(helper, Items.CARROT) + " carrots"));
	}

	@GameTest
	public void pumpkinNightDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> made = List.of(JugcraftAgriculture.FARM_STAND, JugcraftAgriculture.HARVEST_EFFIGY, JugcraftAgriculture.PUMPKIN_VINE_GARLAND,
				JugcraftAgriculture.AUTUMN_LEAF_GARLAND, "singing_pumpkin_bass", "singing_pumpkin_tenor", "singing_pumpkin_alto", "singing_pumpkin_soprano",
				"red_kuri_pumpkin_seeds", "kabocha_pumpkin_seeds");
		for (String id : made) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		List<String> blocks = List.of(JugcraftAgriculture.FARM_STAND, JugcraftAgriculture.HARVEST_EFFIGY, JugcraftAgriculture.EFFIGY_ASHES,
				"singing_pumpkin_bass", "singing_pumpkin_tenor", "singing_pumpkin_alto", "singing_pumpkin_soprano", "red_kuri_pumpkin", "kabocha_pumpkin",
				"hand_carved_red_kuri_pumpkin", "hand_carved_kabocha_pumpkin");
		for (String id : blocks) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
