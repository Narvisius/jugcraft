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
 * building blocks; {@code /dronetest modules} fills nearby Tower Cores with modules.
 * {@code /dronetest build industrial [tier]} explicitly requests the current industrial design,
 * refusing nearby legacy cores instead of changing their saved layout.
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
						.then(Commands.literal("modules").executes(context -> modules(context.getSource().getPlayerOrException())))
						.then(Commands.literal("claim").executes(context -> claim(context.getSource().getPlayerOrException())))
						.then(Commands.literal("slimes")
								.then(Commands.literal("off").executes(context -> slimes(context.getSource().getPlayerOrException(), false)))
								.then(Commands.literal("on").executes(context -> slimes(context.getSource().getPlayerOrException(), true))))
						.then(Commands.literal("build").executes(context -> build(context.getSource().getPlayerOrException(), 9))
								.then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
										.executes(context -> build(context.getSource().getPlayerOrException(),
												com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "tier"))))
								.then(Commands.literal("industrial").executes(context -> build(context.getSource().getPlayerOrException(), 9, true))
										.then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
												.executes(context -> build(context.getSource().getPlayerOrException(),
														com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "tier"), true)))))
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
	 * with no drones, modules or plinth needed (testing only). If there is no core nearby, one is placed at the
	 * player's feet first, owned by the player.
	 */
	private static int build(ServerPlayer player, int tier) {
		return build(player, tier, false);
	}

	private static int build(ServerPlayer player, int tier, boolean industrialOnly) {
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
		if (industrialOnly && found != null
				&& found.layoutVersion() != io.github.jimbozoomer.jugcraft.tower.TowerData.INDUSTRIAL_LAYOUT) {
			player.sendSystemMessage(Component.literal("The nearby tower at " + found.getBlockPos().toShortString()
					+ " uses an earlier design (layout " + found.layoutVersion() + "). It has been left unchanged. Move at least 100 blocks away to an open site,"
					+ " then run /dronetest build industrial " + tier + "."));
			return 0;
		}
		if (found == null) {
			BlockPos at = player.blockPosition();
			level.setBlockAndUpdate(at, io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CORE.defaultBlockState());
			if (level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity placed) {
				placed.setOwner(player.getUUID());
				found = placed;
			}
			// Stand on top of where the tower will be, not inside it.
			player.teleportTo(at.getX() + 0.5, at.getY() + 20, at.getZ() - 45.5);
		}
		if (found == null) {
			player.sendSystemMessage(Component.literal("Could not place a Tower Core here"));
			return 0;
		}
		if (found.tier() >= tier) {
			player.sendSystemMessage(Component.literal("That " + towerDesign(found) + " tower is already tier " + found.tier()
					+ (found.layoutVersion() == io.github.jimbozoomer.jugcraft.tower.TowerData.LEGACY_LAYOUT
							? ". For the redesign, move at least 100 blocks away to an open site and run /dronetest build industrial."
							: ".")));
			return 0;
		}
		long start = System.currentTimeMillis();
		found.buildInstantly(level, tier);
		long ms = System.currentTimeMillis() - start;
		player.sendSystemMessage(Component.literal("Built the " + towerDesign(found) + " Drone Tower to tier " + found.tier() + " (" + ms + " ms)"));
		return 1;
	}

	private static String towerDesign(io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity core) {
		return core.layoutVersion() >= io.github.jimbozoomer.jugcraft.tower.TowerData.FIRST_INDUSTRIAL_LAYOUT
				? "industrial (layout " + core.layoutVersion() + ")" : "legacy (layout 1)";
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

	/** Fills every Drone Tower Core within 48 blocks with 1024 of each tower module (testing only). */
	private static int modules(ServerPlayer player) {
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
						for (String module : io.github.jimbozoomer.jugcraft.tower.JugcraftTower.MODULES) {
							core.deposit(new net.minecraft.world.item.ItemStack(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.MODULE_ITEMS.get(module),
									io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity.MODULE_CAPACITY));
						}
						count++;
					}
				}
			}
		}
		int total = count;
		player.sendSystemMessage(Component.literal(total == 0 ? "No Drone Tower Core nearby" : "Filled " + total + " Tower Core(s) with modules"));
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
