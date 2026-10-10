package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
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
	/** Set by the client: opens the remote shutdown panel's screen for the panel at this position. */
	public static Consumer<BlockPos> openShutdownPanel = pos -> {
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
			BlockPos local = EquipmentKind.toLocal(offset, facing);
			level.setBlock(anchor.offset(offset), frame.setValue(EquipmentFrameBlock.SHAPE, kind.frameShape(local.getX(), local.getY(), local.getZ())),
					Block.UPDATE_ALL);
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
		return use(level, anchor, player, stack, hand, null);
	}

	/** Using any part of a piece of equipment, at {@code hit} (where it was clicked, for the load centre's breakers). */
	public static InteractionResult use(Level level, BlockPos anchor, Player player, ItemStack stack, InteractionHand hand,
			net.minecraft.world.phys.@org.jspecify.annotations.Nullable Vec3 hit) {
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
			case SWITCHGEAR_LINEUP -> {
				// A breaker's door opens or closes it; elsewhere, the bus's state. A block in hand is placed against it
				// (a cable to its terminals).
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				Switchgear.use(level, equipment, player, hit);
				return InteractionResult.SUCCESS;
			}
			case REMOTE_SHUTDOWN_PANEL -> {
				// It finds the nearest reactor itself; with none in range it says so.
				if (!level.isClientSide()) {
					equipment.linkShutdownPanel((net.minecraft.server.level.ServerLevel) level);
					if (equipment.linked == null) {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.panel_no_reactor", EquipmentBlockEntity.PANEL_RANGE));
					}
				} else {
					openShutdownPanel.accept(anchor);
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
			case EMERGENCY_DIESEL_GENERATOR -> {
				// Started and stopped from its panel (anywhere on it): air start, it fires and runs up to speed; stopped,
				// it coasts down.
				if (!level.isClientSide()) {
					if (!equipment.running && !DieselFuel.hasFuel((net.minecraft.server.level.ServerLevel) level, equipment)) {
						// No diesel in its day tank (or none found, or its outlet shut): it cranks on air but won't fire.
						equipment.fuelOut = true;
						equipment.changed();
						level.playSound(null, anchor, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 0.6F);
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_no_fuel", DieselFuel.REACH));
						return InteractionResult.SUCCESS;
					}
					if (!equipment.running) {
						equipment.fuelOut = false;
					}
					equipment.setDieselRunning(level.getGameTime(), !equipment.running);
					// The air start (or the shutdown solenoid) from the engine's middle; the running sound is the client's.
					BlockPos middle = anchor.offset(EquipmentKind.toWorld(new BlockPos(3, 5, 6), equipment.facing()));
					level.playSound(null, middle, equipment.running ? JugcraftNuclear.EDG_START : JugcraftNuclear.EDG_STOP, SoundSource.BLOCKS, 1.0F, 1.0F);
					player.sendOverlayMessage(Component.translatable(equipment.running ? "message.jugcraft.nuclear.edg_start"
							: "message.jugcraft.nuclear.edg_stop"));
				}
				return InteractionResult.SUCCESS;
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
