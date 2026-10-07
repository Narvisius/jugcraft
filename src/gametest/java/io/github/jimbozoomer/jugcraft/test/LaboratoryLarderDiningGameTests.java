package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BrainVatConsoleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CrawlingHandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingTableSettingBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoonLampBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedDiningChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedDiningChairBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LabTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.LightningHarnessBlock;
import io.github.jimbozoomer.jugcraft.agriculture.LightningHarnessBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilkCocoonBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SilkSpoolStackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilkSpoolStackBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.WitchlightBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.Witchlights;
import io.github.jimbozoomer.jugcraft.agriculture.YardSilhouetteBlock;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween decorations batch 19, the Laboratory, the Larder and the Dining Room: the Lightning
 * Harness fires on a strong pulse (not a weak one), keeps its cooldown and wakes the Lab Table patient below; the
 * Brain-Vat Console remembers the strongest signal at its back until a side clears it, and gives it out of its front;
 * the Crawling Hand scuttles while powered; the Silk Cocoon hangs, stores and falls with its ceiling; Egg Sac Clusters
 * cover faces and drop one a face; the Web Drape is placed and broken whole; the Silk Spool Stack takes a dye a spool
 * and keeps them; the Haunted Dining Chair seats you and slides out and back; the Floating Table Setting is laid three
 * ways and its candle lit and snuffed; the Grandfather Clock keeps the hour, the minutes and the moon, pulses and
 * strikes the hour and remembers it; the witchlights wake for a player and sleep after, and take dyes; the Yard
 * Silhouette faces whoever places it and changes figure; the Harvest Moon Lamp stands two by two, follows the moon and
 * lights as one; and the data loads.
 */
public class LaboratoryLarderDiningGameTests {
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

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos on, Vec3 at, Direction face) {
		BlockPos absolute = helper.absolutePos(on);
		BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(at), face, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos on, Direction face) {
		return use(helper, player, on, new Vec3(0.5, 0.5, 0.5).relative(face, 0.5), face);
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

	// ---------------------------------------------------------------- 11. the reanimation rig

	/** A strong pulse fires the harness, which wakes the patient of the table three blocks below; then it rests. */
	@GameTest(maxTicks = 80)
	public void harnessWakesThePatient(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos foot = new BlockPos(2, 2, 3);
		BlockState table = block("lab_table").defaultBlockState().setValue(LabTableBlock.FACING, Direction.NORTH);
		helper.setBlock(foot, table.setValue(LabTableBlock.PART, BedPart.FOOT));
		helper.setBlock(foot.north(), table.setValue(LabTableBlock.PART, BedPart.HEAD));
		BlockPos harness = new BlockPos(2, 5, 2);
		helper.setBlock(harness.above(), Blocks.STONE);
		helper.setBlock(harness, block(JugcraftAgriculture.LIGHTNING_HARNESS));
		helper.assertTrue(LightningHarnessBlock.tableBelow(level, helper.absolutePos(harness)) == 3, "It finds the table three blocks below");
		BlockPos lonely = new BlockPos(6, 5, 6);
		helper.setBlock(lonely.above(), Blocks.STONE);
		helper.setBlock(lonely, block(JugcraftAgriculture.LIGHTNING_HARNESS));
		helper.assertTrue(LightningHarnessBlock.tableBelow(level, helper.absolutePos(lonely)) == 0, "Over bare floor there is no table");

		helper.assertFalse(helper.getBlockState(lonely).getValue(LightningHarnessBlock.POWERED), "Unpowered, it waits");
		LightningHarnessBlockEntity rested = (LightningHarnessBlockEntity) level.getBlockEntity(helper.absolutePos(lonely));
		helper.assertTrue(rested != null && rested.lastFired() < 0 && LightningHarnessBlock.PULSE == 13, "It takes a strong pulse, 13 or more");
		helper.setBlock(harness.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(harness).getValue(LightningHarnessBlock.POWERED), "A strong signal powers it");
		LightningHarnessBlockEntity fired = (LightningHarnessBlockEntity) level.getBlockEntity(helper.absolutePos(harness));
		long when = fired.lastFired();
		helper.assertTrue(when == level.getGameTime(), "and fires it");
		helper.assertFalse(fired.fire(level, helper.absolutePos(harness)), "Just fired, it rests");
		helper.runAfterDelay(2, () -> {
			DecorationBlockEntity patient = (DecorationBlockEntity) level.getBlockEntity(helper.absolutePos(foot));
			helper.assertTrue(patient != null && LabTableBlock.awake(patient.marked(), level.getGameTime()), "The patient sits up");
		});
		helper.runAfterDelay(LightningHarnessBlock.COOLDOWN_TICKS + 2, () -> {
			helper.assertTrue(fired.fire(level, helper.absolutePos(harness)) && fired.lastFired() > when, "Rested, it fires again");
			helper.setBlock(harness.above(), Blocks.AIR);
			helper.assertTrue(helper.getBlockState(harness).isAir(), "Its ceiling gone, it falls");
			helper.succeed();
		});
	}

	/** The console remembers the strongest signal at its back, gives it out of its front only, and a side clears it. */
	@GameTest
	public void brainVatRemembers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(BrainVatConsoleBlock.remember(5, 9, false) == 9 && BrainVatConsoleBlock.remember(9, 5, false) == 9,
				"It keeps the strongest");
		helper.assertTrue(BrainVatConsoleBlock.remember(9, 5, true) == 5 && BrainVatConsoleBlock.remember(9, 0, true) == 0, "Cleared, it holds its back");
		BlockPos vat = new BlockPos(4, 2, 4);
		helper.setBlock(vat, block(JugcraftAgriculture.BRAIN_VAT_CONSOLE).defaultBlockState().setValue(BrainVatConsoleBlock.FACING, Direction.NORTH));
		BlockPos back = vat.south();
		helper.setBlock(back, Blocks.REDSTONE_BLOCK);
		BlockState memory = helper.getBlockState(vat);
		helper.assertTrue(memory.getValue(BrainVatConsoleBlock.MEMORY) == 15 && memory.getLightEmission() == 7, "A full signal at its back is remembered");
		helper.setBlock(back, Blocks.AIR);
		helper.assertTrue(helper.getBlockState(vat).getValue(BrainVatConsoleBlock.MEMORY) == 15, "It remembers when the signal goes");
		BlockPos vatAbs = helper.absolutePos(vat);
		helper.assertTrue(level.getSignal(vatAbs, Direction.SOUTH) == 15, "It gives it out of its front");
		helper.assertTrue(level.getSignal(vatAbs, Direction.NORTH) == 0 && level.getSignal(vatAbs, Direction.WEST) == 0, "and nowhere else");
		helper.assertTrue(helper.getBlockState(vat).getAnalogOutputSignal(level, vatAbs, Direction.NORTH) == 15, "A comparator reads it");
		helper.setBlock(vat.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(vat).getValue(BrainVatConsoleBlock.MEMORY) == 0 && helper.getBlockState(vat).getValue(BrainVatConsoleBlock.CLEARING),
				"A signal at its side clears it");
		helper.setBlock(vat.east(), Blocks.AIR);
		helper.assertFalse(helper.getBlockState(vat).getValue(BrainVatConsoleBlock.CLEARING), "and lets go");
		helper.succeed();
	}

	/** Powered, the hand scuttles; unpowered it lies still. */
	@GameTest
	public void handScuttlesWhenPowered(GameTestHelper helper) {
		floor(helper);
		BlockPos hand = new BlockPos(4, 2, 4);
		helper.setBlock(hand, block(JugcraftAgriculture.CRAWLING_HAND));
		helper.assertFalse(helper.getBlockState(hand).getValue(CrawlingHandBlock.POWERED), "It lies still");
		helper.setBlock(hand.west(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(hand).getValue(CrawlingHandBlock.POWERED), "Powered, it runs");
		helper.setBlock(hand.west(), Blocks.AIR);
		helper.assertFalse(helper.getBlockState(hand).getValue(CrawlingHandBlock.POWERED), "and stops");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 12. the spider's larder

	/** The cocoon hangs from a ceiling, holds nine stacks, wriggles when opened, and falls with its ceiling. */
	@GameTest(maxTicks = 40)
	public void cocoonHangsAndStores(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos cocoon = new BlockPos(4, 3, 4);
		helper.setBlock(cocoon.above(), Blocks.OAK_PLANKS);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(item(JugcraftAgriculture.SILK_COCOON)));
		BlockPos ceiling = helper.absolutePos(cocoon.above());
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(ceiling).relative(Direction.DOWN, 0.5), Direction.DOWN, ceiling, false)));
		helper.assertBlockPresent(block(JugcraftAgriculture.SILK_COCOON), cocoon);
		helper.assertFalse(block(JugcraftAgriculture.SILK_COCOON).defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(6, 2, 6))),
				"It needs something to hang from");
		SilkCocoonBlockEntity larder = (SilkCocoonBlockEntity) level.getBlockEntity(helper.absolutePos(cocoon));
		helper.assertTrue(larder.getContainerSize() == SilkCocoonBlockEntity.SLOTS, "It holds nine stacks");
		larder.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 64));
		helper.assertTrue(helper.getBlockState(cocoon).getAnalogOutputSignal(level, helper.absolutePos(cocoon), Direction.NORTH) > 0, "A comparator reads it");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(use(helper, player, cocoon, Direction.NORTH).consumesAction(), "Used, it opens");
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(larder.wriggled() >= level.getGameTime() - 2, "and wriggles");
			helper.setBlock(cocoon.above(), Blocks.AIR);
			helper.assertTrue(helper.getBlockState(cocoon).isAir(), "Its ceiling gone, it falls");
		});
		helper.runAfterDelay(5, () -> helper.succeedWhen(() -> helper.assertTrue(dropped(helper, Items.ROTTEN_FLESH) == 64, "and spills what it held")));
	}

	/** Egg sacs cover any faces of their block, like glow lichen, and give one cluster a face. */
	@GameTest(maxTicks = 40)
	public void eggSacsCoverFaces(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		helper.setBlock(at.north(), Blocks.STONE);
		BlockState sacs = block(JugcraftAgriculture.EGG_SAC_CLUSTER).defaultBlockState()
				.setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true).setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true);
		helper.setBlock(at, sacs);
		helper.assertTrue(helper.getBlockState(at).getValue(MultifaceBlock.getFaceProperty(Direction.NORTH))
				&& helper.getBlockState(at).getValue(MultifaceBlock.getFaceProperty(Direction.DOWN)), "On the floor and the wall at once");
		helper.assertTrue(level.getBlockEntity(helper.absolutePos(at)) instanceof DecorationBlockEntity, "with a block entity for the spiderlings");
		level.destroyBlock(helper.absolutePos(at), true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item(JugcraftAgriculture.EGG_SAC_CLUSTER)) == 2, "Two faces give two clusters"));
	}

	/** The drape is placed two by two in front of the player, lets you walk through, and breaks whole for one drape. */
	@GameTest(maxTicks = 40)
	public void webDrapeHangsWhole(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = new BlockPos(4, 2, 4);
		place(helper, player, JugcraftAgriculture.WEB_DRAPE, master, 0.0F);
		MultiDecorationBlock drape = (MultiDecorationBlock) block(JugcraftAgriculture.WEB_DRAPE);
		BlockPos masterAbs = helper.absolutePos(master);
		for (int part = 0; part < 4; part++) {
			BlockPos at = drape.partPos(masterAbs, Direction.NORTH, part);
			helper.assertTrue(level.getBlockState(at).is(drape) && drape.part(level.getBlockState(at)) == part, "Part " + part + " is in place");
			helper.assertTrue(level.getBlockState(at).getCollisionShape(level, at).isEmpty(), "You walk through it");
		}
		level.destroyBlock(drape.partPos(masterAbs, Direction.NORTH, 3), true);
		helper.succeedWhen(() -> {
			for (int part = 0; part < 4; part++) {
				helper.assertTrue(level.getBlockState(drape.partPos(masterAbs, Direction.NORTH, part)).isAir(), "It all comes down");
			}
			helper.assertTrue(dropped(helper, item(JugcraftAgriculture.WEB_DRAPE)) == 1, "for one drape");
		});
	}

	/** Each spool takes the dye used on it, and the stack keeps its colours. */
	@GameTest
	public void spoolsTakeDyes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		helper.setBlock(at, block(JugcraftAgriculture.SILK_SPOOL_STACK).defaultBlockState().setValue(SilkSpoolStackBlock.FACING, Direction.NORTH));
		BlockPos abs = helper.absolutePos(at);
		helper.assertTrue(SilkSpoolStackBlock.spoolAt(abs, Direction.NORTH, Vec3.atLowerCornerOf(abs).add(0.85, 0.5, 0.0)) == 0
				&& SilkSpoolStackBlock.spoolAt(abs, Direction.NORTH, Vec3.atLowerCornerOf(abs).add(0.5, 0.5, 0.0)) == 1
				&& SilkSpoolStackBlock.spoolAt(abs, Direction.NORTH, Vec3.atLowerCornerOf(abs).add(0.15, 0.5, 0.0)) == 2,
				"Seen from the front, the spools run left to right");
		SilkSpoolStackBlockEntity spools = (SilkSpoolStackBlockEntity) level.getBlockEntity(abs);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(dye("blue"), 2));
		helper.assertTrue(use(helper, player, at, new Vec3(0.15, 0.5, 0.0), Direction.NORTH).consumesAction(), "Dye takes");
		helper.assertTrue(spools.colour(2) == DyeColor.BLUE && player.getMainHandItem().getCount() == 1, "on the spool it touched, for one dye");
		helper.assertTrue(spools.colour(0) == DyeColor.WHITE && spools.colour(1) == DyeColor.PURPLE, "The others keep theirs");
		CompoundTag saved = spools.saveWithoutMetadata(level.registryAccess());
		SilkSpoolStackBlockEntity copy = new SilkSpoolStackBlockEntity(abs, helper.getBlockState(at));
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.colour(2) == DyeColor.BLUE && copy.colour(1) == DyeColor.PURPLE, "The colours are saved");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 13. the dinner party

	/** It seats you; at night it may slide out for a player near, and slides back in after a while. */
	@GameTest(maxTicks = 200)
	public void chairSlidesOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(HauntedDiningChairBlockEntity.maySlide(true, false, true, false), "At night, empty, someone near: it slides");
		helper.assertFalse(HauntedDiningChairBlockEntity.maySlide(false, false, true, false), "Not by day");
		helper.assertFalse(HauntedDiningChairBlockEntity.maySlide(true, true, true, false), "Not with someone in it");
		helper.assertFalse(HauntedDiningChairBlockEntity.maySlide(true, false, false, false), "Not with nobody near");
		helper.assertFalse(HauntedDiningChairBlockEntity.maySlide(true, false, true, true), "Not when out already");
		BlockPos chair = new BlockPos(1, 2, 1);
		BlockPos other = new BlockPos(7, 2, 7);
		helper.setBlock(chair, block(JugcraftAgriculture.DINING_CHAIR));
		helper.setBlock(other, block(JugcraftAgriculture.DINING_CHAIR));
		ServerPlayer sitter = player(helper, new BlockPos(7, 2, 5), ItemStack.EMPTY);
		helper.assertTrue(use(helper, sitter, other, Direction.UP).consumesAction() && sitter.isPassenger(), "Used, it seats you");
		HauntedDiningChairBlockEntity haunted = (HauntedDiningChairBlockEntity) level.getBlockEntity(helper.absolutePos(chair));
		haunted.slideOut(level, helper.absolutePos(chair), helper.getBlockState(chair));
		helper.assertTrue(helper.getBlockState(chair).getValue(HauntedDiningChairBlock.OUT), "It slides out");
		helper.runAfterDelay(HauntedDiningChairBlock.OUT_TICKS + 5, () -> {
			helper.assertFalse(helper.getBlockState(chair).getValue(HauntedDiningChairBlock.OUT), "and back in after a while");
			helper.succeed();
		});
	}

	/** Used, the table is laid for dinner, tea or a feast in turn; flint lights its candle and a sneaking hand snuffs it. */
	@GameTest
	public void tableSettingIsLaid(GameTestHelper helper) {
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		helper.setBlock(at.below(), Blocks.OAK_PLANKS);
		helper.setBlock(at, block(JugcraftAgriculture.TABLE_SETTING));
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		helper.assertTrue(helper.getBlockState(at).getValue(FloatingTableSettingBlock.SETTING) == FloatingTableSettingBlock.Setting.DINNER, "Laid for dinner");
		for (FloatingTableSettingBlock.Setting next : List.of(FloatingTableSettingBlock.Setting.TEA, FloatingTableSettingBlock.Setting.FEAST,
				FloatingTableSettingBlock.Setting.DINNER)) {
			use(helper, player, at, Direction.UP);
			helper.assertTrue(helper.getBlockState(at).getValue(FloatingTableSettingBlock.SETTING) == next, "then for " + next);
		}
		helper.assertTrue(helper.getBlockState(at).getCollisionShape(helper.getLevel(), helper.absolutePos(at)).isEmpty(), "Nothing to bump into");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		helper.assertTrue(use(helper, player, at, Direction.UP).consumesAction(), "Flint and steel");
		helper.assertTrue(helper.getBlockState(at).getValue(FloatingTableSettingBlock.LIT)
				&& helper.getBlockState(at).getLightEmission() == FloatingTableSettingBlock.LIGHT, "lights the candle");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		use(helper, player, at, Direction.UP);
		helper.assertFalse(helper.getBlockState(at).getValue(FloatingTableSettingBlock.LIT), "A sneaking hand snuffs it");
		helper.assertTrue(helper.getBlockState(at).getValue(FloatingTableSettingBlock.SETTING) == FloatingTableSettingBlock.Setting.DINNER,
				"without changing the setting");
		helper.succeed();
	}

	/** The clock reads the hour, the minutes and the moon; a new hour pulses and strikes; it remembers the hour it saw. */
	@GameTest(maxTicks = 40)
	public void clockKeepsTheHour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(GrandfatherClockBlock.clockHour(0) == 6 && GrandfatherClockBlock.clockHour(6000) == 12
				&& GrandfatherClockBlock.clockHour(13000) == 7 && GrandfatherClockBlock.clockHour(18000) == 12 && GrandfatherClockBlock.clockHour(23999) == 5,
				"Dawn is six, noon and midnight twelve");
		helper.assertTrue(GrandfatherClockBlock.minutes(6500) == 30 && GrandfatherClockBlock.minutes(6000) == 0, "Half an hour is thirty minutes");
		helper.assertTrue(GrandfatherClockBlock.moonPhase(1000) == 0 && GrandfatherClockBlock.moonPhase(24000 * 4 + 5) == 4
				&& GrandfatherClockBlock.moonPhase(24000 * 9) == 1, "The moon turns nightly through eight phases");
		helper.assertTrue(GrandfatherClockBlock.midnight(18000) && GrandfatherClockBlock.midnight(18000 + 24000 * 3 + 199)
				&& !GrandfatherClockBlock.midnight(18200) && !GrandfatherClockBlock.midnight(17999), "The face shows for the first ticks after midnight");

		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos clock = new BlockPos(4, 2, 4);
		place(helper, player, JugcraftAgriculture.GRANDFATHER_CLOCK, clock, 0.0F);
		helper.assertTrue(helper.getBlockState(clock.above()).is(block(JugcraftAgriculture.GRANDFATHER_CLOCK))
				&& helper.getBlockState(clock.above()).getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER, "It stands two blocks tall");
		BlockPos abs = helper.absolutePos(clock);
		GrandfatherClockBlockEntity works = (GrandfatherClockBlockEntity) level.getBlockEntity(abs);
		long now = level.getOverworldClockTime();
		works.advance(level, abs, now);
		helper.assertTrue(works.lastHour() == GrandfatherClockBlock.hourIndex(now) && !helper.getBlockState(clock).getValue(GrandfatherClockBlock.POWERED),
				"Started, it notes the hour without striking");
		long next = (GrandfatherClockBlock.hourIndex(now) + 1) * GrandfatherClockBlock.HOUR_TICKS;
		works.advance(level, abs, next);
		helper.assertTrue(helper.getBlockState(clock).getValue(GrandfatherClockBlock.POWERED) && helper.getBlockState(clock.above()).getValue(GrandfatherClockBlock.POWERED),
				"A new hour pulses");
		helper.assertTrue(works.strikesLeft() == GrandfatherClockBlock.clockHour(next) - 1, "and strikes the hour, once already");
		helper.assertTrue(helper.getBlockState(clock).getSignal(level, abs, Direction.NORTH) == 15, "The pulse is a full signal");
		helper.assertTrue(helper.getBlockState(clock).getAnalogOutputSignal(level, abs, Direction.NORTH) == GrandfatherClockBlock.clockHour(now),
				"A comparator reads the hour");
		CompoundTag saved = works.saveWithoutMetadata(level.registryAccess());
		GrandfatherClockBlockEntity copy = new GrandfatherClockBlockEntity(abs, helper.getBlockState(clock));
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.lastHour() == GrandfatherClockBlock.hourIndex(next), "It remembers the hour it last saw");
		// (Its own ticking then sees the world's hour again and pulses once more, so the pulse is checked well after.)
		helper.runAfterDelay(GrandfatherClockBlock.PULSE_TICKS * 3, () -> {
			helper.assertFalse(helper.getBlockState(clock).getValue(GrandfatherClockBlock.POWERED), "The pulse ends");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- 14. the witchlights

	/** A witchlight wakes for a player near and sleeps once they have long gone; a dye recolours its glass. */
	@GameTest(maxTicks = 40)
	public void witchlightsWakeAndSleep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(Witchlights.awake(true, false, 100, 0) && Witchlights.awake(false, true, 100, 0), "A player near or power wakes it");
		helper.assertTrue(Witchlights.awake(false, false, 100 + Witchlights.LINGER_TICKS - 1, 100), "It lingers after they go");
		helper.assertFalse(Witchlights.awake(false, false, 100 + Witchlights.LINGER_TICKS, 100), "then sleeps");
		BlockPos stake = new BlockPos(1, 2, 1);
		helper.setBlock(stake, block(JugcraftAgriculture.WITCHLIGHT_STAKE));
		BlockState asleep = helper.getBlockState(stake);
		helper.assertTrue(!asleep.getValue(Witchlights.LIT) && asleep.getLightEmission() == Witchlights.ASLEEP, "Asleep it barely glows");
		BlockPos abs = helper.absolutePos(stake);
		WitchlightBlockEntity lamp = (WitchlightBlockEntity) level.getBlockEntity(abs);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), new ItemStack(dye("lime")));
		long now = level.getGameTime();
		lamp.look(level, abs, helper.getBlockState(stake), now);
		helper.assertTrue(helper.getBlockState(stake).getValue(Witchlights.LIT) && helper.getBlockState(stake).getLightEmission() == Witchlights.AWAKE,
				"A player near wakes it");
		helper.assertTrue(use(helper, player, stake, Direction.NORTH).consumesAction()
				&& helper.getBlockState(stake).getValue(Witchlights.COLOUR) == Witchlights.Colour.GREEN, "Lime dye turns its glass green");
		player.discard();
		lamp.look(level, abs, helper.getBlockState(stake), now + Witchlights.LINGER_TICKS + 1);
		helper.assertFalse(helper.getBlockState(stake).getValue(Witchlights.LIT), "Long after the player goes, it sleeps");

		// The lamp-post wakes and is dyed whole, its light in its lantern.
		ServerPlayer placer = player(helper, new BlockPos(6, 2, 2), ItemStack.EMPTY);
		BlockPos post = new BlockPos(6, 2, 6);
		place(helper, placer, JugcraftAgriculture.WITCHLIGHT_POST, post, 0.0F);
		placer.discard();
		helper.assertTrue(helper.getBlockState(post.above()).is(block(JugcraftAgriculture.WITCHLIGHT_POST)), "The lamp-post stands two blocks tall");
		helper.setBlock(post.west(), Blocks.REDSTONE_BLOCK);
		WitchlightBlockEntity lantern = (WitchlightBlockEntity) level.getBlockEntity(helper.absolutePos(post.above()));
		lantern.look(level, helper.absolutePos(post.above()), helper.getBlockState(post.above()), level.getGameTime());
		helper.assertTrue(helper.getBlockState(post).getValue(Witchlights.LIT) && helper.getBlockState(post.above()).getLightEmission() == Witchlights.AWAKE
				&& helper.getBlockState(post).getLightEmission() == 0, "Powered at its foot, it wakes; the light is in its lantern");

		// The hanging lantern needs something above it.
		helper.assertFalse(block(JugcraftAgriculture.HANGING_WITCHLIGHT).defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(4, 4, 4))),
				"A hanging lantern needs something to hang from");
		helper.setBlock(new BlockPos(4, 5, 4), Blocks.OAK_PLANKS);
		helper.assertTrue(block(JugcraftAgriculture.HANGING_WITCHLIGHT).defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(4, 4, 4))),
				"and hangs under a block");
		helper.succeed();
	}

	// ---------------------------------------------------------------- 15. the silhouettes and the harvest moon

	/** A silhouette turns to face whoever places it and changes figure when used; it needs ground under it. */
	@GameTest(maxTicks = 20)
	public void silhouetteFacesItsMaker(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos at = new BlockPos(4, 2, 4);
		place(helper, player, JugcraftAgriculture.YARD_SILHOUETTE, at, 0.0F);
		helper.assertTrue(helper.getBlockState(at).getValue(YardSilhouetteBlock.ROTATION) == 8, "Placed by someone looking south, it faces north to them");
		helper.assertTrue(helper.getBlockState(at).getValue(YardSilhouetteBlock.FIGURE) == YardSilhouetteBlock.Figure.ARCHED_CAT, "An arched cat first");
		YardSilhouetteBlock.Figure figure = YardSilhouetteBlock.Figure.ARCHED_CAT;
		for (int i = 0; i < YardSilhouetteBlock.Figure.values().length; i++) {
			use(helper, player, at, Direction.NORTH);
			figure = figure.next();
			helper.assertTrue(helper.getBlockState(at).getValue(YardSilhouetteBlock.FIGURE) == figure, "Used, it becomes " + figure);
		}
		helper.assertTrue(figure == YardSilhouetteBlock.Figure.ARCHED_CAT, "and comes round again");
		helper.setBlock(at.below(), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(at).isAir(), "With no ground it falls");
		helper.succeed();
	}

	/** The lamp stands two by two, reads the moon's phase to a comparator, and lights or darkens as one. */
	@GameTest(maxTicks = 40)
	public void moonLampFollowsTheMoon(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		int[] expected = {15, 11, 8, 4, 0, 4, 8, 11};
		for (int phase = 0; phase < 8; phase++) {
			helper.assertTrue(HarvestMoonLampBlock.brightness(phase) == expected[phase], "Phase " + phase + " gives " + expected[phase]);
		}
		ServerPlayer player = player(helper, new BlockPos(4, 2, 1), ItemStack.EMPTY);
		BlockPos master = new BlockPos(4, 2, 4);
		place(helper, player, JugcraftAgriculture.MOON_LAMP, master, 0.0F);
		HarvestMoonLampBlock lamp = (HarvestMoonLampBlock) block(JugcraftAgriculture.MOON_LAMP);
		BlockPos masterAbs = helper.absolutePos(master);
		for (int part = 0; part < 4; part++) {
			BlockState state = level.getBlockState(lamp.partPos(masterAbs, Direction.NORTH, part));
			helper.assertTrue(state.is(lamp) && lamp.part(state) == part && state.getLightEmission() == HarvestMoonLampBlock.LIGHT, "Part " + part + " stands lit");
		}
		helper.assertTrue(helper.getBlockState(master).getAnalogOutputSignal(level, masterAbs, Direction.NORTH)
				== HarvestMoonLampBlock.brightness(HarvestMoonLampBlock.phase(level)), "A comparator reads tonight's moon");
		BlockPos top = lamp.partPos(masterAbs, Direction.NORTH, 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(top).relative(Direction.NORTH, 0.5), Direction.NORTH, top, false));
		for (int part = 0; part < 4; part++) {
			helper.assertFalse(level.getBlockState(lamp.partPos(masterAbs, Direction.NORTH, part)).getValue(MultiDecorationBlock.LIT), "Used, it all goes dark");
		}
		level.destroyBlock(top, true);
		helper.succeedWhen(() -> {
			helper.assertTrue(level.getBlockState(masterAbs).isAir(), "Broken anywhere, it comes down whole");
			helper.assertTrue(dropped(helper, item(JugcraftAgriculture.MOON_LAMP)) == 1, "for one lamp");
		});
	}

	@GameTest
	public void laboratoryDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> blocks = List.of(JugcraftAgriculture.LIGHTNING_HARNESS, JugcraftAgriculture.BRAIN_VAT_CONSOLE, JugcraftAgriculture.CRAWLING_HAND,
				JugcraftAgriculture.SILK_COCOON, JugcraftAgriculture.EGG_SAC_CLUSTER, JugcraftAgriculture.WEB_DRAPE, JugcraftAgriculture.SILK_SPOOL_STACK,
				JugcraftAgriculture.DINING_CHAIR, JugcraftAgriculture.TABLE_SETTING, JugcraftAgriculture.GRANDFATHER_CLOCK, JugcraftAgriculture.WITCHLIGHT_POST,
				JugcraftAgriculture.WITCHLIGHT_STAKE, JugcraftAgriculture.HANGING_WITCHLIGHT, JugcraftAgriculture.YARD_SILHOUETTE, JugcraftAgriculture.MOON_LAMP);
		for (String id : blocks) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
