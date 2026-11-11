package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Maintenance Door (work list item 10, approved by Justin on 23 October 2026): the solid steel door of the
 * Tri-Pillar's pillar doorways, a pair in each (the riser rooms off the hall, the pillars' doors off the roof and the tie
 * decks). It is a plant door (nuclear/PlantDoorBlock: it swings, its closer shuts it, its badge reader lets in the owner
 * and their party from outside, anyone out), with tools/maintenance_door.py's models, and:
 * <ul>
 *   <li>{@link #HOOD}: a steel rain hood with an LED downlight over it outside (the roof's and the decks' doors); the
 *   upper half of a hooded door gives light {@link #HOOD_LIGHT};</li>
 *   <li>a pair opens and shuts together: using either leaf swings both (the reader asked once), and neither closer
 *   shuts its leaf while someone stands in the other's doorway.</li>
 * </ul>
 * It has no exit hardware (EXIT changes nothing). Its leaves swing as client/PlantDoorRenderer draws the steel door's
 * (the same frame, hinge pin and closer arm), from part models 32 to 35 (nuclear/PlantDoors leafPart).
 */
public final class MaintenanceDoorBlock extends PlantDoorBlock {
	public static final BooleanProperty HOOD = BooleanProperty.create("hood");
	/** The light the hood's downlight gives (tools/maintenance_door.py HOOD_LIGHT, tools/tripillar_lights.py). */
	public static final int HOOD_LIGHT = 9;

	public MaintenanceDoorBlock(BlockSetType type, Properties properties) {
		super(type, properties, true);
		registerDefaultState(defaultBlockState().setValue(HOOD, false));
	}

	/** The light a state gives: a hooded door's upper half, its downlight. */
	public static int light(BlockState state) {
		return state.getValue(HOOD) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? HOOD_LIGHT : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HOOD);
	}

	/** The foot of a leaf's partner: the leaf beside its latch edge, if this is a pair. */
	public static BlockPos partner(BlockState state, BlockPos foot) {
		Direction facing = state.getValue(FACING);
		Direction latch = state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
		return foot.relative(latch);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos foot = foot(state, pos);
		boolean was = state.getValue(OPEN);
		InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
		if (level.isClientSide()) {
			return result;
		}
		BlockState now = level.getBlockState(foot);
		if (now.is(this) && now.getValue(OPEN) != was && now.getValue(PAIRED)) {
			BlockPos other = partner(now, foot);
			BlockState leaf = level.getBlockState(other);
			if (leaf.is(this) && leaf.getValue(HALF) == DoubleBlockHalf.LOWER && leaf.getValue(OPEN) == was && !leaf.getValue(MOVING)) {
				swing(level, other, leaf, !was, player, false);
			}
		}
		return result;
	}

	/** The closer waits while anyone stands in either leaf's doorway. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER && !state.getValue(MOVING) && state.getValue(OPEN)
				&& !state.getValue(POWERED) && state.getValue(PAIRED)) {
			BlockPos other = partner(state, pos);
			if (!level.getEntitiesOfClass(Entity.class, new AABB(other).expandTowards(0, 1, 0), e -> !e.isSpectator()).isEmpty()) {
				level.scheduleTick(pos, this, 20);
				return;
			}
		}
		super.tick(state, level, pos, random);
	}

	/** The door's item placing it with a badge reader and / or the rain hood (the creative tab's versions). */
	public static ItemStack version(Block door, boolean reader, boolean hood) {
		ItemStack stack = new ItemStack(door);
		BlockItemStateProperties props = BlockItemStateProperties.EMPTY;
		if (reader) {
			props = props.with(READER, true);
		}
		if (hood) {
			props = props.with(HOOD, true);
		}
		stack.set(DataComponents.BLOCK_STATE, props);
		stack.set(DataComponents.ITEM_NAME, Component.translatable("block.jugcraft.maintenance_door" + (reader ? "_reader" : "") + (hood ? "_hood" : "")));
		return stack;
	}
}
