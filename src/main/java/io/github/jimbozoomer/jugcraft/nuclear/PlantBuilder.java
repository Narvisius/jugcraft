package io.github.jimbozoomer.jugcraft.nuclear;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The operators' build commands (docs/features/nuclear.md, "Pre-made plants"), for creative play and testing:
 * <ul>
 *   <li>{@code /nuclear build <blueprint> [fuelled]} builds any blueprint at once where the player stands, turned the
 *   way they face, exactly as drones would finish it: every block, the equipment placed whole, pipes joined, consoles
 *   linked to the nearest reactor. With {@code fuelled} the reactors are also loaded (rods fully in: start them from
 *   the console).</li>
 *   <li>{@code /nuclear showcase} builds the stage-1 running unit ({@link ReactorShowcase}).</li>
 *   <li>{@code /nuclear plant <small|medium|large> [fuelled]} (Justin, 2026-10-13: "make a command so someone can load
 *   in the current medium nuclear plant in game in front of them") builds that pre-made plant as its blueprint has it
 *   now, just in front of the player: turned the way they face, its near side {@link #AHEAD} blocks ahead of them and
 *   centred on them, its ground at their feet.</li>
 * </ul>
 * A whole plant is a third of a million blocks: the command takes a few seconds and loads the chunks it builds in.
 */
public final class PlantBuilder {
	/** The pre-made plants by the name the plant command takes. */
	public static final java.util.Map<String, String> PLANTS = java.util.Map.of("small", "nuclear_small_modular", "medium", "nuclear_medium_pwr",
			"large", "nuclear_large_epr");
	/** How far in front of the player a plant built by the plant command starts (blocks). */
	public static final int AHEAD = 4;

	private PlantBuilder() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("nuclear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("build").then(Commands.argument("blueprint", StringArgumentType.string())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Blueprint.ids(false).stream().map(i -> "\"" + i + "\"").toList(), builder))
								.executes(context -> build(context.getSource(), StringArgumentType.getString(context, "blueprint"), false))
								.then(Commands.literal("fuelled")
										.executes(context -> build(context.getSource(), StringArgumentType.getString(context, "blueprint"), true)))))
						.then(Commands.literal("plant").then(Commands.argument("size", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("small", "medium", "large"), builder))
								.executes(context -> plant(context.getSource(), StringArgumentType.getString(context, "size"), false))
								.then(Commands.literal("fuelled")
										.executes(context -> plant(context.getSource(), StringArgumentType.getString(context, "size"), true)))))
						.then(Commands.literal("showcase").then(Commands.literal("plants").executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							PlantShowcase.build(player.level(), player, player.blockPosition());
							return 1;
						})).then(Commands.literal("refresh").executes(context -> {
							// Rebuild the medium plant near you in place, from the current blueprint.
							ServerPlayer player = context.getSource().getPlayerOrException();
							Blueprint plant = Blueprint.get("nuclear_medium_pwr", false);
							BlockPos stake = plant == null ? null : PlantShowcase.locate(player.level(), plant, player.blockPosition());
							if (stake == null) {
								context.getSource().sendFailure(Component.literal("No medium plant reactor loaded near you."));
								return 0;
							}
							PlantShowcase.rebuild(player.level(), plant, stake, player);
							context.getSource().sendSuccess(() -> Component.literal("Rebuilt the plant in place."), true);
							return 1;
						})).executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							BlockPos vessel = ReactorShowcase.build(player.level(), player.blockPosition());
							context.getSource().sendSuccess(() -> Component.translatable("message.jugcraft.nuclear.showcase", vessel.toShortString()), true);
							return 1;
						}))));
	}

	private static int build(CommandSourceStack source, String id, boolean fuelled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Blueprint blueprint = Blueprint.get(id, false);
		if (blueprint == null) {
			source.sendFailure(Component.translatable("message.jugcraft.blueprint.unknown"));
			return 0;
		}
		Direction facing = player.getDirection();
		Rotation rotation = BlueprintItem.rotationFor(facing);
		long started = System.currentTimeMillis();
		Result result = build(player.level(), blueprint, player.blockPosition(), rotation, player.getUUID(), fuelled);
		long ms = System.currentTimeMillis() - started;
		source.sendSuccess(() -> Component.translatable("message.jugcraft.nuclear.built", blueprint.name, String.format("%,d", result.blocks()),
				result.equipment(), result.reactors(), ms / 1000.0), true);
		return 1;
	}

	/** The plant command: a pre-made plant built just in front of the player. */
	private static int plant(CommandSourceStack source, String size, boolean fuelled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String id = PLANTS.get(size.toLowerCase(java.util.Locale.ROOT));
		Blueprint blueprint = id == null ? null : Blueprint.get(id, false);
		if (blueprint == null) {
			source.sendFailure(Component.translatable("message.jugcraft.nuclear.plant_unknown", size));
			return 0;
		}
		Direction facing = player.getDirection();
		Rotation rotation = BlueprintItem.rotationFor(facing);
		BlockPos stake = inFront(blueprint, rotation, player.blockPosition(), facing);
		long started = System.currentTimeMillis();
		Result result = build(player.level(), blueprint, stake, rotation, player.getUUID(), fuelled);
		long ms = System.currentTimeMillis() - started;
		source.sendSuccess(() -> Component.translatable("message.jugcraft.nuclear.built", blueprint.name, String.format("%,d", result.blocks()),
				result.equipment(), result.reactors(), ms / 1000.0), true);
		return 1;
	}

	/**
	 * Where to put a blueprint's stake, turned by {@code rotation}, so it stands in front of a player at {@code at} facing
	 * {@code facing}: its near side {@link #AHEAD} blocks ahead, centred across their view, its ground at their feet.
	 */
	public static BlockPos inFront(Blueprint blueprint, Rotation rotation, BlockPos at, Direction facing) {
		int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos o = blueprint.offset(i, rotation);
			minX = Math.min(minX, o.getX());
			maxX = Math.max(maxX, o.getX());
			minZ = Math.min(minZ, o.getZ());
			maxZ = Math.max(maxZ, o.getZ());
		}
		if (minX > maxX) {
			return at.relative(facing, AHEAD);
		}
		int midX = Math.floorDiv(minX + maxX, 2), midZ = Math.floorDiv(minZ + maxZ, 2);
		return switch (facing) {
			case NORTH -> new BlockPos(at.getX() - midX, at.getY(), at.getZ() - AHEAD - maxZ);
			case SOUTH -> new BlockPos(at.getX() - midX, at.getY(), at.getZ() + AHEAD - minZ);
			case EAST -> new BlockPos(at.getX() + AHEAD - minX, at.getY(), at.getZ() - midZ);
			default -> new BlockPos(at.getX() - AHEAD - maxX, at.getY(), at.getZ() - midZ);
		};
	}

	/** What a build placed. */
	public record Result(int blocks, int equipment, int reactors, List<BlockPos> vessels) {
	}

	/**
	 * Builds {@code blueprint} with its stake at {@code stake}, turned by {@code rotation}: blocks first (no neighbour
	 * updates, for speed), then the equipment whole, then a pass that joins panes, bars and pipes to their neighbours.
	 */
	public static Result build(ServerLevel level, Blueprint blueprint, BlockPos stake, Rotation rotation, @Nullable UUID owner, boolean fuelled) {
		List<Integer> anchors = new ArrayList<>();
		int blocks = 0;
		int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		for (int i = 0; i < blueprint.size(); i++) {
			BlockState state = blueprint.state(i, rotation);
			if (state.getBlock() instanceof EquipmentAnchorBlock) {
				anchors.add(i);
				continue;
			}
			BlockPos pos = stake.offset(blueprint.offset(i, rotation));
			if (level.isOutsideBuildHeight(pos)) {
				continue;
			}
			level.setBlock(pos, state, flags);
			blocks++;
		}
		List<BlockPos> vessels = new ArrayList<>();
		List<BlockPos> consoles = new ArrayList<>();
		List<BlockPos> loadCentres = new ArrayList<>();
		int equipment = 0;
		for (int i : anchors) {
			BlockState state = blueprint.state(i, rotation);
			EquipmentKind kind = ((EquipmentAnchorBlock) state.getBlock()).kind;
			Direction face = state.getValue(EquipmentAnchorBlock.FACING);
			BlockPos pos = stake.offset(blueprint.offset(i, rotation));
			// Whatever ground is in the equipment's way goes (the blueprint leaves its body empty).
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
			for (BlockPos offset : kind.frames(face)) {
				BlockPos cell = pos.offset(offset);
				if (!level.getBlockState(cell).canBeReplaced() && level.getBlockEntity(cell) == null) {
					level.setBlock(cell, Blocks.AIR.defaultBlockState(), flags);
				}
			}
			if (!EquipmentPlacement.place(level, pos, kind, face, null, state.getValue(EquipmentAnchorBlock.VARIANT))) {
				continue;
			}
			equipment++;
			if (level.getBlockEntity(pos) instanceof EquipmentBlockEntity placed) {
				placed.setOwner(owner);
			}
			if (kind.reactor()) {
				vessels.add(pos);
			} else if (kind == EquipmentKind.CONTROL_CONSOLE) {
				consoles.add(pos);
			} else if (kind.board()) {
				loadCentres.add(pos);
			}
		}
		// Panes, bars, fences and pipes join their neighbours now that everything is in.
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos pos = stake.offset(blueprint.offset(i, rotation));
			BlockState now = level.getBlockState(pos);
			if (now.getBlock() instanceof EquipmentAnchorBlock || level.isOutsideBuildHeight(pos)) {
				continue;
			}
			BlockState joined = Block.updateFromNeighbourShapes(now, level, pos);
			if (joined != now) {
				level.setBlock(pos, joined, flags);
			}
			// A pipe or cable in a wall takes the look of the wall round it.
			if (now.getBlock() instanceof io.github.jimbozoomer.jugcraft.wallrun.WallRun
					&& level.getBlockEntity(pos) instanceof io.github.jimbozoomer.jugcraft.wallrun.WallRunBlockEntity run && !run.hasDisguise()) {
				run.setDisguise(io.github.jimbozoomer.jugcraft.wallrun.WallRuns.lookAround(level, pos));
			}
		}
		// Each console runs the reactor nearest to it that no other console has taken (a plant's control room).
		List<BlockPos> free = new ArrayList<>(vessels);
		for (BlockPos console : consoles) {
			BlockPos best = null;
			for (BlockPos vessel : free) {
				if (best == null || vessel.distSqr(console) < best.distSqr(console)) {
					best = vessel;
				}
			}
			if (best == null) {
				best = vessels.isEmpty() ? null : vessels.get(0);
			} else {
				free.remove(best);
			}
			if (best != null && level.getBlockEntity(console) instanceof EquipmentBlockEntity c) {
				c.linked = best.immutable();
				c.changed();
			}
		}
		if (fuelled) {
			for (BlockPos vessel : vessels) {
				if (level.getBlockEntity(vessel) instanceof EquipmentBlockEntity v) {
					v.core.refuelOpen = true;
					v.loadAssemblies(v.core.design.size());
					v.core.refuelOpen = false;
					v.changed();
				}
			}
		}
		// Storage tanks form at once (their outlets would within two seconds anyway); a fuelled build fills them with
		// what they are for.
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, rotation).getBlock() instanceof StorageTankOutletBlock) {
				BlockPos pos = stake.offset(blueprint.offset(i, rotation));
				if (level.getBlockEntity(pos) instanceof StorageTankPortBlockEntity outlet) {
					if (fuelled) {
						outlet.prefill();
					}
					outlet.check(level);
				}
			}
		}
		// A fuelled plant's electrical system is live: its load centres' and motor control centres' buses and its station
		// batteries start full (round 25, 26).
		if (fuelled) {
			for (BlockPos pos : loadCentres) {
				if (level.getBlockEntity(pos) instanceof EquipmentBlockEntity gear && gear.bus != null) {
					gear.bus.setAmount(gear.busCapacity());
					gear.changed();
				}
			}
			for (int i = 0; i < blueprint.size(); i++) {
				if (blueprint.state(i, rotation).getBlock() instanceof StationBatteryBlock) {
					StationBatteryBlockEntity bank = StationBatteryBlock.battery(level, stake.offset(blueprint.offset(i, rotation)));
					if (bank != null) {
						bank.fill();
					}
				}
			}
		}
		EquipmentRegistry.of(level).touch();
		return new Result(blocks, equipment, vessels.size(), List.copyOf(vessels));
	}
}
