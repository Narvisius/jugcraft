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
		return place(level, anchor, kind, facing, player, 0);
	}

	/** As above, the anchor its labelled version {@code variant} (EquipmentAnchorBlock.VARIANT; a blueprint's). */
	public static boolean place(ServerLevel level, BlockPos anchor, EquipmentKind kind, Direction facing, @Nullable Player player, int variant) {
		if (kind.hideable() && variant == EquipmentAnchorBlock.HIDDEN) {
			// Standing hidden (a building's site draws it): its anchor alone.
			level.setBlock(anchor, JugcraftNuclear.EQUIPMENT.get(kind).defaultBlockState().setValue(EquipmentAnchorBlock.FACING, facing)
					.setValue(EquipmentAnchorBlock.VARIANT, variant), Block.UPDATE_ALL);
			refreshPipes(level, anchor, kind, facing);
			EquipmentRegistry.of(level).touch();
			return true;
		}
		if (!fits(level, anchor, kind, facing)) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.no_room", kind.wide, kind.deep, kind.tall));
			}
			return false;
		}
		level.setBlock(anchor, JugcraftNuclear.EQUIPMENT.get(kind).defaultBlockState().setValue(EquipmentAnchorBlock.FACING, facing)
				.setValue(EquipmentAnchorBlock.VARIANT, variant), Block.UPDATE_ALL);
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
				// Round 30: a working control on its desk (where the player looks) works it; anywhere else opens its screen.
				if (ControlBoard.control(equipment, player) != null) {
					ControlBoard.use(level, equipment, player, hit);
					return InteractionResult.SUCCESS;
				}
				if (level.isClientSide()) {
					openConsole.accept(anchor);
				}
				return InteractionResult.SUCCESS;
			}
			case SWITCHGEAR_LINEUP -> {
				// Its controls (a breaker's control switch, racking port, trip unit reset; the transformer's fan switch: the
				// rebuilt load centre, nuclear/LoadCentre.java); elsewhere, the bus's state. A block in hand is placed against
				// it (a cable to its terminals).
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				LoadCentre.use(level, equipment, player, hit);
				return InteractionResult.SUCCESS;
			}
			case MCC_LINEUP -> {
				// A unit's door switches its breaker; elsewhere, the bus's state (round 26). A block in hand is placed.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				MotorControlCenter.use(level, equipment, player, hit);
				return InteractionResult.SUCCESS;
			}
			case RCS_BOARD, TURBINE_BOARD, ELECTRICAL_BOARD, ESF_BOARD, SECONDARY_BOARD, RADIATION_BOARD -> {
				// A live board (rounds 27 and 28): its working controls, or what it shows of the plant. A block in hand is placed.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				ControlBoard.use(level, equipment, player, hit);
				return InteractionResult.SUCCESS;
			}
			case PLANT_DISPLAY_WALL -> {
				// Round 32: the display wall has no controls; a click says what it shows. A block in hand is placed.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					ControlBoard.State s = ControlBoard.refresh((net.minecraft.server.level.ServerLevel) level, equipment);
					player.sendOverlayMessage(DisplayWall.status(s));
				}
				return InteractionResult.SUCCESS;
			}
			case REMOTE_SHUTDOWN_PANEL -> {
				// Round 29: a working switch on its face (where the player looks) works it; anywhere else opens its screen.
				if (ControlBoard.control(equipment, player) != null) {
					ControlBoard.use(level, equipment, player, hit);
					return InteractionResult.SUCCESS;
				}
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
				if (kind == EquipmentKind.CONDENSER && player.isShiftKeyDown() && stack.isEmpty()) {
					// Turbine hall part 2: its label (condenser 1A, 1B, 1C).
					TurbineHall.usePiece(level, equipment, player, hit, true);
					return InteractionResult.SUCCESS;
				}
				if (!level.isClientSide() && equipment.water != null) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.condenser_water",
							String.format("%,d", equipment.water.millibuckets()), String.format("%,d", EquipmentBlockEntity.CONDENSER_TANK)));
				}
				return stack.isEmpty() ? InteractionResult.SUCCESS : InteractionResult.PASS;
			}
			case TURBINE_GENERATOR -> {
				// Turbine hall part 1: the manual trip on the front standard, the turning gear's lever; elsewhere, how it
				// stands. A block in hand is placed against it (a pipe to its steam chests).
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				TurbineHall.useTurbine(level, equipment, player, hit);
				return InteractionResult.SUCCESS;
			}
			case CONDENSER_SHELL, CONDENSER_HOTWELL, CONDENSATE_PUMP, MAIN_FEED_PUMP, LUBE_OIL_RESERVOIR, EHC_UNIT, SEAL_OIL_UNIT, H2_GAS_PANEL,
					LO_CONDITIONER, LO_STORAGE_TANK, LO_TRANSFER_PUMPS -> {
				// Turbine hall part 2: a condensate pump's START and STOP, a main feed pump's TRIP and RESET; elsewhere, how it
				// stands; sneaking with an empty hand, its next label. A block in hand is placed against it.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				TurbineHall.usePiece(level, equipment, player, hit, player.isShiftKeyDown() && stack.isEmpty());
				return InteractionResult.SUCCESS;
			}
			case OVERHEAD_CRANE -> {
				// Turbine hall part 3: it runs from its pendant stations (HallCrane); a click on it says how it stands.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					player.sendOverlayMessage(HallCrane.status(equipment, level.getGameTime()));
				}
				return InteractionResult.SUCCESS;
			}
			case MOISTURE_SEPARATOR_REHEATER -> {
				// Its label and its turbine's steam; sneaking with an empty hand, the other reheater's label.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				TurbineHall.useReheater(level, equipment, player, player.isShiftKeyDown() && stack.isEmpty());
				return InteractionResult.SUCCESS;
			}
			case EMERGENCY_DIESEL_GENERATOR -> {
				// Round 31 part 2 (Justin: "Show its status"): it is started and stopped from its engine panel or the
				// control room; a click on the set says how it stands.
				if (!level.isClientSide()) {
					player.sendOverlayMessage(DieselPanels.status(equipment, level.getGameTime()));
				}
				return InteractionResult.SUCCESS;
			}
			case EDG_AIR_RECEIVER, EDG_AIR_COMPRESSOR, EDG_AIR_DRYER -> {
				// The diesel generator's starting air (round 31, StartingAir): its label and state; sneaking with an empty
				// hand, its next label (receivers and compressors 1A-1 / 1A-A ... 1B-2 / 1B-B, the dryer 1A or 1B).
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					net.minecraft.world.level.block.state.BlockState state = level.getBlockState(anchor);
					int variant = state.getValue(EquipmentAnchorBlock.VARIANT);
					int labels = kind == EquipmentKind.EDG_AIR_DRYER ? 2 : 4;
					if (player.isShiftKeyDown() && stack.isEmpty()) {
						variant = (variant + 1) % labels;
						level.setBlock(anchor, state.setValue(EquipmentAnchorBlock.VARIANT, variant), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", airTag(kind, variant)));
					} else {
						player.sendOverlayMessage(airStatus(equipment, kind, variant));
					}
				}
				return InteractionResult.SUCCESS;
			}
			case EDG_JW_KEEPWARM, EDG_LO_KEEPWARM, EDG_EXPANSION_TANK, EDG_JW_COOLER, EDG_NGR, EDG_GEN_CUBICLE, EDG_CO2_BANK, EDG_MONORAIL -> {
				// A diesel room's own plant (round 31 part 3, DieselRoom): its label and generator (the CO2 bank its system's
				// state, the monorail where its hoist is); sneaking with an empty hand, its other train's label.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					net.minecraft.world.level.block.state.BlockState state = level.getBlockState(anchor);
					int variant = state.getValue(EquipmentAnchorBlock.VARIANT);
					if (player.isShiftKeyDown() && stack.isEmpty()) {
						variant = 1 - Math.min(1, variant);
						level.setBlock(anchor, state.setValue(EquipmentAnchorBlock.VARIANT, variant), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", variant == 1 ? "1B" : "1A"));
					} else {
						player.sendOverlayMessage(roomStatus(level, equipment, kind));
					}
				}
				return InteractionResult.SUCCESS;
			}
			case CR_FILTRATION_UNIT, CR_AIR_HANDLER, CR_CHILLER, CHW_PUMP -> {
				// A control room HVAC machine (round 26, #11): its tag and state; sneaking with an empty hand, its other
				// train's tag. A block in hand is placed against it.
				if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					net.minecraft.world.level.block.state.BlockState state = level.getBlockState(anchor);
					int train = HvacRoom.train(state);
					if (player.isShiftKeyDown() && stack.isEmpty()) {
						train = 1 - train;
						level.setBlock(anchor, state.setValue(EquipmentAnchorBlock.VARIANT, train), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
						player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", HvacRoom.tag(HvacRoom.part(kind), train)));
					} else {
						player.sendOverlayMessage(Component.translatable(equipment.running ? "message.jugcraft.nuclear.hvac_running"
								: "message.jugcraft.nuclear.hvac_standby", HvacRoom.tag(HvacRoom.part(kind), train)));
					}
				}
				return InteractionResult.SUCCESS;
			}
			default -> {
				return InteractionResult.PASS;
			}
		}
	}

	/** What a diesel room's piece tells whoever uses it: its name and label, its generator; the bank's CO2, the hoist. */
	static Component roomStatus(Level level, EquipmentBlockEntity piece, EquipmentKind kind) {
		Component name = Component.translatable("block.jugcraft." + kind.id);
		String label = DieselRoom.label(piece);
		if (piece.linked == null || !(level.getBlockEntity(piece.linked) instanceof EquipmentBlockEntity edg)) {
			return Component.translatable("message.jugcraft.nuclear.room_piece", name, label,
					Component.translatable("message.jugcraft.nuclear.air_unlinked", DieselRoom.REACH));
		}
		Component joined = Component.translatable("message.jugcraft.nuclear.air_linked", StartingAir.distance(piece));
		if (kind == EquipmentKind.EDG_CO2_BANK) {
			return Component.translatable("message.jugcraft.nuclear.room_co2", name, label, Component.translatable("message.jugcraft.nuclear.co2_state_" + edg.co2Phase),
					joined);
		}
		if (kind == EquipmentKind.EDG_MONORAIL) {
			long now = level.getGameTime();
			return Component.translatable("message.jugcraft.nuclear.room_hoist", name, label, Math.round(Monorail.trolley(piece, now)),
					Math.round(Monorail.MAX_X), Math.round(Monorail.hook(piece, now)));
		}
		return Component.translatable("message.jugcraft.nuclear.room_piece", name, label, joined);
	}

	/** A starting air piece's label: 1A-1, 1A-2, 1B-1, 1B-2 (receivers), 1A-A ... 1B-B (compressors), 1A or 1B (dryer). */
	static String airTag(EquipmentKind kind, int variant) {
		String train = variant < 2 ? "1A" : "1B";
		return switch (kind) {
			case EDG_AIR_RECEIVER -> train + "-" + (variant % 2 + 1);
			case EDG_AIR_COMPRESSOR -> train + "-" + (variant % 2 == 0 ? "A" : "B");
			default -> variant % 2 == 0 ? "1A" : "1B";
		};
	}

	/** What a starting air piece tells whoever uses it: its label, its pressure or state, and its generator. */
	static Component airStatus(EquipmentBlockEntity piece, EquipmentKind kind, int variant) {
		Component joined = StartingAir.joined(piece) ? Component.translatable("message.jugcraft.nuclear.air_linked", StartingAir.distance(piece))
				: Component.translatable("message.jugcraft.nuclear.air_unlinked", StartingAir.REACH);
		return switch (kind) {
			case EDG_AIR_RECEIVER -> Component.translatable("message.jugcraft.nuclear.air_receiver", airTag(kind, variant), Math.round(piece.air), joined);
			case EDG_AIR_COMPRESSOR -> Component.translatable("message.jugcraft.nuclear.air_compressor", airTag(kind, variant),
					Component.translatable(piece.running ? "message.jugcraft.nuclear.air_running" : "message.jugcraft.nuclear.air_standby"),
					Component.translatable(piece.airLead ? "message.jugcraft.nuclear.air_lead" : "message.jugcraft.nuclear.air_lag"),
					Math.round(piece.airLead ? StartingAir.LEAD_ON : StartingAir.LAG_ON), Math.round(StartingAir.FULL));
			default -> Component.translatable("message.jugcraft.nuclear.air_dryer", airTag(kind, variant), joined);
		};
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
			// Its board looks for the reactor again at once (round 30).
			equipment.board().searchVersion = -1;
			equipment.changed();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.console_linked"));
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}

	/**
	 * Starts or stops an emergency diesel generator from the control room (the electrical board's buttons, safety
	 * injection): only with its MODE at AUTO (DieselPanels).
	 */
	public static boolean dieselSwitch(net.minecraft.server.level.ServerLevel level, EquipmentBlockEntity equipment, @org.jspecify.annotations.Nullable Player player,
			boolean run) {
		return dieselSwitch(level, equipment, player, run, DieselPanels.REMOTE);
	}

	/**
	 * Starts or stops an emergency diesel generator: {@code from} the control room ({@link DieselPanels#REMOTE}: its MODE
	 * at AUTO), its engine panel ({@link DieselPanels#PANEL}: at LOCAL) or a trip ({@link DieselPanels#FORCE}: the
	 * emergency stop, the 86G, MODE turned to OFF; stops only). Nothing starts it with its 86G tripped, its emergency stop
	 * in or its MODE at OFF. A start takes starting air from its receivers (StartingAir): with too little it barely turns
	 * over and won't start; with no diesel in its day tank (or none found, or its outlet shut) it cranks on air but won't
	 * fire. Either way FAIL TO START comes in until it next starts. Whether it did what was asked.
	 */
	public static boolean dieselSwitch(net.minecraft.server.level.ServerLevel level, EquipmentBlockEntity equipment, @org.jspecify.annotations.Nullable Player player,
			boolean run, int from) {
		// (No player: started by safety injection from the ESF board, round 28.)
		BlockPos anchor = equipment.getBlockPos();
		Component refused = DieselPanels.refuse(equipment, run, from);
		if (refused != null) {
			if (player != null) {
				player.sendOverlayMessage(refused);
			}
			return false;
		}
		if (run && !equipment.running && !StartingAir.start(equipment)) {
			equipment.airFail = true;
			equipment.changed();
			level.playSound(null, anchor, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 0.5F);
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_no_air", Math.round(equipment.air),
						Math.round(StartingAir.MIN + StartingAir.perStart(equipment))));
			}
			return false;
		}
		if (run && !equipment.running && !DieselFuel.hasFuel(level, equipment)) {
			equipment.fuelOut = true;
			equipment.airFail = false;
			equipment.changed();
			level.playSound(null, anchor, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 0.6F);
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.edg_no_fuel", DieselFuel.REACH));
			}
			return false;
		}
		if (run == equipment.running) {
			return true;
		}
		if (run) {
			equipment.fuelOut = false;
			equipment.airFail = false;
		}
		equipment.setDieselRunning(level.getGameTime(), run);
		// The air start (or the shutdown solenoid) from the engine's middle; the running sound is the client's.
		BlockPos middle = anchor.offset(EquipmentKind.toWorld(new BlockPos(5, 6, 7), equipment.facing()));
		level.playSound(null, middle, equipment.running ? JugcraftNuclear.EDG_START : JugcraftNuclear.EDG_STOP, SoundSource.BLOCKS, 1.0F, 1.0F);
		if (player != null) {
			player.sendOverlayMessage(Component.translatable(equipment.running ? "message.jugcraft.nuclear.edg_start" : "message.jugcraft.nuclear.edg_stop"));
		}
		return true;
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
