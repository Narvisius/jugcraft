package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Operator-only testing aids, available in development and packaged builds:
 * {@code /dronetest fill <from> <to> <block> [personal|party]} asks drones to build a box of blocks, and
 * {@code /dronetest clear} cancels those jobs; {@code /dronetest supplies [off]} gives nearby depots endless power and
 * building blocks; {@code /dronetest stock} fills nearby Tower Cores with every block their towers still need.
 * {@code /dronetest build [tier]} builds the nearest tower (the Tri-Pillar, the only design) straight up to that tier, or
 * places a core first if there is none nearby.
 */
final class DroneDevCommands {
	private static final List<SimpleBuildJobs> JOBS = new ArrayList<>();
	private static final int MAX_BLOCKS = 4096;

	private DroneDevCommands() {
	}

	static void register() {
		// Available in every build, for operators only (cheats on in single player, or op level 2 on a server).
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("dronetest").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("fill")
								.then(Commands.argument("from", BlockPosArgument.blockPos())
										.then(Commands.argument("to", BlockPosArgument.blockPos())
												.then(Commands.argument("block", BlockStateArgument.block(registryAccess))
														.executes(context -> fill(context.getSource().getPlayerOrException(),
																BlockPosArgument.getLoadedBlockPos(context, "from"),
																BlockPosArgument.getLoadedBlockPos(context, "to"),
																BlockStateArgument.getBlock(context, "block").getState(), UseMode.PERSONAL))
														.then(Commands.literal("party")
																.executes(context -> fill(context.getSource().getPlayerOrException(),
																		BlockPosArgument.getLoadedBlockPos(context, "from"),
																		BlockPosArgument.getLoadedBlockPos(context, "to"),
																		BlockStateArgument.getBlock(context, "block").getState(), UseMode.PARTY)))))))
						.then(Commands.literal("supplies").executes(context -> supplies(context.getSource().getPlayerOrException(), true))
								.then(Commands.literal("off").executes(context -> supplies(context.getSource().getPlayerOrException(), false))))
						.then(Commands.literal("stock").executes(context -> stock(context.getSource().getPlayerOrException())))
						.then(Commands.literal("repair").executes(context -> repair(context.getSource().getPlayerOrException())))
						.then(Commands.literal("claim").executes(context -> claim(context.getSource().getPlayerOrException())))
						.then(Commands.literal("slimes")
								.then(Commands.literal("off").executes(context -> slimes(context.getSource().getPlayerOrException(), false)))
								.then(Commands.literal("on").executes(context -> slimes(context.getSource().getPlayerOrException(), true))))
						.then(Commands.literal("build").executes(context -> build(context.getSource().getPlayerOrException(), 9))
								.then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
										.executes(context -> build(context.getSource().getPlayerOrException(),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "tier")))))
						.then(Commands.literal("clear").executes(context -> {
							JOBS.forEach(BuildJobs::unregister);
							JOBS.clear();
							context.getSource().sendSuccess(() -> Component.literal("Drone test jobs cleared"), false);
							return 1;
						}))));
	}

	/**
	 * Makes the caller the owner of every depot (and its Drone Tower) within 96 blocks. Testing only: a
	 * development client gets a new random name, and so a new player ID, each launch.
	 */
	private static int slimes(ServerPlayer player, boolean allow) {
		var level = (net.minecraft.server.level.ServerLevel) player.level();
		if (!io.github.jimbozoomer.jugcraft.world.ReviewWorldSettings.isFlat(level)) {
			player.sendSystemMessage(Component.literal("Use this command in a flat review world"));
			return 0;
		}
		int removed = io.github.jimbozoomer.jugcraft.world.ReviewWorldSettings.setSlimesSuppressed(level, !allow);
		player.sendSystemMessage(Component.literal(allow ? "Slimes enabled in this flat world"
				: "Slimes disabled in this flat world; removed " + removed + " loaded slimes"));
		return 1;
	}

	private static int claim(ServerPlayer player) {
		net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
		int count = 0;
		for (BlockPos pos : List.copyOf(DroneDepots.terminals(level))) {
			if (pos.closerThan(player.blockPosition(), 96) && level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) {
				terminal.setOwner(player.getUUID());
				if (terminal.tower() != null) {
					terminal.tower().setOwner(player.getUUID());
				}
				count++;
			}
		}
		int total = count;
		player.sendSystemMessage(Component.literal(total == 0 ? "No drone depot within 96 blocks" : "You now own " + total + " depot(s) and their towers"));
		return total;
	}

	/**
	 * Builds the nearest Drone Tower (Tower Core within 3 chunks) straight up to {@code tier}, every block at once,
	 * with no drones or blocks needed (testing only). If there is no core nearby, one is placed at the
	 * player's feet first, owned by the player, and the player is put on the control building's roof.
	 */
	private static int build(ServerPlayer player, int tier) {
		net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity found = null;
		double best = Double.MAX_VALUE;
		int cx = player.blockPosition().getX() >> 4, cz = player.blockPosition().getZ() >> 4;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) {
					continue;
				}
				for (var entity : List.copyOf(level.getChunk(cx + dx, cz + dz).getBlockEntities().values())) {
					if (entity instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity core
							&& core.getBlockPos().distSqr(player.blockPosition()) < best) {
						best = core.getBlockPos().distSqr(player.blockPosition());
						found = core;
					}
				}
			}
		}
		if (found != null && (found.converting() || found.layoutVersion() != io.github.jimbozoomer.jugcraft.tower.TowerData.CURRENT_LAYOUT)) {
			player.sendSystemMessage(Component.literal("The tower at " + found.getBlockPos().toShortString()
					+ " is being rebuilt as the Tri-Pillar (it was an older design). Try again when it has finished."));
			return 0;
		}
		if (found == null) {
			BlockPos at = player.blockPosition();
			if (!io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.fits(level, at)) {
				player.sendSystemMessage(Component.literal("The tower wouldn't fit here: its core must stand between y "
						+ io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.lowest(level) + " and y "
						+ io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.highest(level) + " (you're at y " + at.getY() + ")."));
				return 0;
			}
			level.setBlockAndUpdate(at, io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CORE.defaultBlockState());
			if (level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity placed) {
				placed.setOwner(player.getUUID());
				found = placed;
			}
			// Stand on the control building's roof, not inside it.
			player.teleportTo(at.getX() + 0.5, at.getY() + 21, at.getZ() - 24.5);
		}
		if (found == null) {
			player.sendSystemMessage(Component.literal("Could not place a Tower Core here"));
			return 0;
		}
		if (found.tier() >= tier) {
			player.sendSystemMessage(Component.literal("That Drone Tower is already tier " + found.tier() + "."));
			return 0;
		}
		long start = System.currentTimeMillis();
		found.buildInstantly(level, tier);
		long ms = System.currentTimeMillis() - start;
		player.sendSystemMessage(Component.literal("Built the Tri-Pillar Drone Tower to tier " + found.tier() + " (" + ms + " ms)"));
		return 1;
	}

	/** Endless power and building blocks for every depot within 96 blocks (testing only). */
	private static int supplies(ServerPlayer player, boolean on) {
		net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
		int count = 0;
		for (BlockPos pos : List.copyOf(DroneDepots.terminals(level))) {
			if (pos.closerThan(player.blockPosition(), 96) && level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) {
				terminal.setTestSupplies(on);
				count++;
			}
		}
		int total = count;
		player.sendSystemMessage(Component.literal(total == 0 ? "No drone depot within 96 blocks (build tier 1 of a Drone Tower first)"
				: (on ? "Endless test power and building blocks ON for " : "Endless test supplies OFF for ") + total + " depot(s)"));
		return total;
	}

	/** Fills every Drone Tower Core within 48 blocks with every block its tower still needs (testing only). */
	/**
	 * Puts back the blocks of the nearest Drone Tower (its core within 5 chunks) that are gone, a block someone put in
	 * their place staying, and binds its Cargo Exchanges again (TowerCoreBlockEntity.repair).
	 */
	private static int repair(ServerPlayer player) {
		net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity found = null;
		double best = Double.MAX_VALUE;
		int cx = player.blockPosition().getX() >> 4, cz = player.blockPosition().getZ() >> 4;
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) {
					continue;
				}
				for (var entity : List.copyOf(level.getChunk(cx + dx, cz + dz).getBlockEntities().values())) {
					if (entity instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity core
							&& core.getBlockPos().distSqr(player.blockPosition()) < best) {
						best = core.getBlockPos().distSqr(player.blockPosition());
						found = core;
					}
				}
			}
		}
		if (found == null) {
			player.sendSystemMessage(Component.literal("No Drone Tower Core nearby"));
			return 0;
		}
		var done = found.repair(level);
		if (done == null) {
			player.sendSystemMessage(Component.literal("The tower at " + found.getBlockPos().toShortString()
					+ " is building or being rebuilt (or has built nothing yet). Try again when it has finished."));
			return 0;
		}
		player.sendSystemMessage(Component.literal("Repaired the Drone Tower at " + found.getBlockPos().toShortString() + ": "
				+ done.placed() + " block" + (done.placed() == 1 ? "" : "s") + " put back"
				+ (done.blocked() > 0 ? ", " + done.blocked() + " left where something else is in the way" : "")
				+ (done.unloaded() > 0 ? ", " + done.unloaded() + " not loaded (come closer and run it again)" : "")));
		return Math.max(1, done.placed());
	}

	private static int stock(ServerPlayer player) {
		net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
		int count = 0;
		int cx = player.blockPosition().getX() >> 4, cz = player.blockPosition().getZ() >> 4;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) {
					continue;
				}
				for (var entity : List.copyOf(level.getChunk(cx + dx, cz + dz).getBlockEntities().values())) {
					if (entity instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity core) {
						core.stockAll();
						count++;
					}
				}
			}
		}
		int total = count;
		player.sendSystemMessage(Component.literal(total == 0 ? "No Drone Tower Core nearby" : "Stocked " + total + " Tower Core(s) with every block their towers still need"));
		return total;
	}

	private static int fill(ServerPlayer player, BlockPos from, BlockPos to, BlockState state, UseMode mode) {
		SimpleBuildJobs jobs = new SimpleBuildJobs(player.getUUID(), mode);
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (count >= MAX_BLOCKS) {
				break;
			}
			jobs.want(pos, state);
			count++;
		}
		BuildJobs.register(jobs);
		JOBS.add(jobs);
		int total = count;
		player.sendSystemMessage(Component.literal("Drone test job: " + total + " blocks of " + state.getBlock().getName().getString()
				+ " (" + mode.id() + "). Put the blocks in a cargo packager next to your depot."));
		return 1;
	}
}
