package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Placing and taking down nuclear equipment, and using it: linking a console to a vessel with the link card, loading
 * and unloading fuel while the vessel is open for refuelling, filling a condenser with buckets and opening the console.
 */
public final class EquipmentPlacement {
	/** Set while equipment removes its own frame, so the frame blocks don't try to break it again. */
	public static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);
	/** Set by the client: opens the console screen for the console at this position. */
	public static Consumer<BlockPos> openConsole = pos -> {
	};

	private EquipmentPlacement() {
	}

	/** Whether every cell of the equipment's body (and its anchor) is free. */
	public static boolean fits(Level level, BlockPos anchor, EquipmentKind kind, Direction facing) {
		if (!level.getBlockState(anchor).canBeReplaced() || level.isOutsideBuildHeight(anchor)) {
			return false;
		}
		for (BlockPos offset : kind.frames(facing)) {
			BlockPos p = anchor.offset(offset);
			if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	/** Places equipment from its item. Returns false (telling the player why) if there is no room. */
	public static boolean place(ServerLevel level, BlockPos anchor, EquipmentKind kind, Direction facing, @Nullable Player player) {
		if (!fits(level, anchor, kind, facing)) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.no_room", kind.wide, kind.deep, kind.tall));
			}
			return false;
		}
		level.setBlock(anchor, JugcraftNuclear.EQUIPMENT.get(kind).defaultBlockState().setValue(EquipmentAnchorBlock.FACING, facing), Block.UPDATE_ALL);
		BlockState frame = JugcraftNuclear.FRAME.defaultBlockState();
		for (BlockPos offset : kind.frames(facing)) {
			level.setBlock(anchor.offset(offset), frame, Block.UPDATE_ALL);
		}
		if (player != null && level.getBlockEntity(anchor) instanceof EquipmentBlockEntity equipment) {
			equipment.setOwner(player.getUUID());
		}
		refreshPipes(level, anchor, kind, facing);
		EquipmentRegistry.of(level).touch();
		return true;
	}

	/** Pipes next to new equipment turn to join it. */
	private static void refreshPipes(ServerLevel level, BlockPos anchor, EquipmentKind kind, Direction facing) {
		for (BlockPos cell : ReactorUnit.outside(EquipmentRegistry.of(level), anchor)) {
			BlockState state = level.getBlockState(cell);
			if (state.getBlock() instanceof NuclearPipeBlock pipe) {
				BlockState updated = pipe.refresh(level, cell, state);
				if (updated != state) {
					level.setBlock(cell, updated, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Removes equipment's frame quietly (no drops; the anchor drops the item). */
	static void clearFrames(ServerLevel level, BlockPos anchor, EquipmentKind kind, Direction facing) {
		boolean was = CLEARING.get();
		CLEARING.set(true);
		try {
			for (BlockPos offset : kind.frames(facing)) {
				BlockPos p = anchor.offset(offset);
				if (level.getBlockState(p).is(JugcraftNuclear.FRAME)) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		} finally {
			CLEARING.set(was);
		}
		EquipmentRegistry.of(level).touch();
	}

	/** Using any part of a piece of equipment. */
	public static InteractionResult use(Level level, BlockPos anchor, Player player, ItemStack stack, InteractionHand hand) {
		if (!(level.getBlockEntity(anchor) instanceof EquipmentBlockEntity equipment)) {
			return InteractionResult.PASS;
		}
		EquipmentKind kind = equipment.kind();
		if (stack.is(JugcraftNuclear.LINK_CARD)) {
			if (level.isClientSide()) {
				// Linking a console opens it straight away (the server links it; the screen follows the synced link).
				if (kind == EquipmentKind.CONTROL_CONSOLE && stack.get(JugcraftNuclear.LINKED_VESSEL) != null) {
					openConsole.accept(anchor);
				}
				return InteractionResult.SUCCESS;
			}
			return linkCard(level, equipment, player, stack);
		}
		switch (kind) {
			case CONTROL_CONSOLE -> {
				// Opens with anything in hand (a block in hand is not placed against the console).
				if (equipment.linked == null) {
					if (!level.isClientSide()) {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.console_unlinked"));
					}
					return InteractionResult.SUCCESS;
				}
				if (level.isClientSide()) {
					openConsole.accept(anchor);
				}
				return InteractionResult.SUCCESS;
			}
			case REACTOR_VESSEL, EPR_VESSEL, SMALL_MODULAR_REACTOR -> {
				if (level.isClientSide()) {
					return InteractionResult.SUCCESS;
				}
				return useVessel(equipment, player, stack, hand);
			}
			case CONDENSER, SMALL_TURBINE -> {
				if (equipment.water != null && FluidStorageUtil.interactWithFluidStorage(equipment.water, player, hand)) {
					return InteractionResult.SUCCESS;
				}
				if (!level.isClientSide() && equipment.water != null) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.condenser_water",
							String.format("%,d", equipment.water.millibuckets()), String.format("%,d", EquipmentBlockEntity.CONDENSER_TANK)));
				}
				return stack.isEmpty() ? InteractionResult.SUCCESS : InteractionResult.PASS;
			}
			default -> {
				return InteractionResult.PASS;
			}
		}
	}

	private static InteractionResult linkCard(Level level, EquipmentBlockEntity equipment, Player player, ItemStack card) {
		if (equipment.kind().reactor()) {
			card.set(JugcraftNuclear.LINKED_VESSEL, equipment.getBlockPos());
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.card_set"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (equipment.kind() == EquipmentKind.CONTROL_CONSOLE) {
			BlockPos vessel = card.get(JugcraftNuclear.LINKED_VESSEL);
			if (vessel == null || !(level.getBlockEntity(vessel) instanceof EquipmentBlockEntity v) || !v.kind().reactor()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.card_empty"));
				return InteractionResult.FAIL;
			}
			if (!equipment.mayUse(player)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.not_owner"));
				return InteractionResult.FAIL;
			}
			equipment.linked = vessel.immutable();
			equipment.changed();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.console_linked"));
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}

	private static InteractionResult useVessel(EquipmentBlockEntity vessel, Player player, ItemStack stack, InteractionHand hand) {
		ReactorCore core = vessel.core;
		if (!core.refuelOpen) {
			if (stack.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.vessel_status",
						Math.round(core.fission * 100), Math.round(core.temperature), core.fuel.size(), core.design.size()));
				return InteractionResult.SUCCESS_SERVER;
			}
			if (stack.is(JugcraftNuclear.FUEL_ASSEMBLY) || stack.is(JugcraftNuclear.SPENT_FUEL)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.vessel_closed"));
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		}
		if (!vessel.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.not_owner"));
			return InteractionResult.FAIL;
		}
		if (stack.is(JugcraftNuclear.SPENT_FUEL)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.spent_no"));
			return InteractionResult.FAIL;
		}
		if (stack.is(JugcraftNuclear.FUEL_ASSEMBLY)) {
			int n = vessel.loadAssemblies(stack.getCount());
			if (n > 0) {
				if (!player.getAbilities().instabuild) {
					stack.shrink(n);
				}
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.loaded", core.fuel.size(), core.design.size()));
				return InteractionResult.SUCCESS_SERVER;
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.core_full"));
			return InteractionResult.FAIL;
		}
		if (stack.isEmpty()) {
			ItemStack taken = vessel.takeAssemblies(16);
			if (taken.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.core_empty"));
				return InteractionResult.FAIL;
			}
			player.setItemInHand(hand, taken);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.unloaded", core.fuel.size(), core.design.size()));
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}
}
