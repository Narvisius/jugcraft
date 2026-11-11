package io.github.jimbozoomer.jugcraft.tower.crew;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The drop ships' floodlights in the world: the invisible light blocks a ship puts in the air round its landing
 * zone while it's down (only where there was air), and takes away when it leaves. Each dimension keeps a list of
 * them, so that any left behind (a crash, the ship lost with its chunk) are cleared when the server starts; a ship
 * still on the ground puts its own back.
 */
public final class CrewShipLights extends SavedData {
	static final Codec<CrewShipLights> CODEC = Codec.LONG.listOf().xmap(CrewShipLights::new, l -> new ArrayList<>(l.cells));
	static final SavedDataType<CrewShipLights> TYPE = new SavedDataType<>(Jugcraft.id("crew_ship_lights"), CrewShipLights::new, CODEC, null);
	/** The light the floodlights give. */
	public static final int LEVEL = 15;

	private final Set<Long> cells = new LinkedHashSet<>();

	public CrewShipLights() {
	}

	private CrewShipLights(List<Long> list) {
		cells.addAll(list);
	}

	public static CrewShipLights get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				get(level).clearAll(level);
			}
		});
	}

	private static BlockState light() {
		return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LEVEL);
	}

	/** Puts a light at each of {@code at} that is air now; returns the cells it lit. */
	public List<BlockPos> place(ServerLevel level, List<BlockPos> at) {
		List<BlockPos> lit = new ArrayList<>();
		for (BlockPos p : at) {
			if (level.isLoaded(p) && level.getBlockState(p).isAir()) {
				level.setBlock(p, light(), 3);
				cells.add(p.asLong());
				lit.add(p.immutable());
			}
		}
		if (!lit.isEmpty()) {
			setDirty();
		}
		return lit;
	}

	/** Takes away the lights at {@code at} (those still the ship's). */
	public void remove(ServerLevel level, List<BlockPos> at) {
		for (BlockPos p : at) {
			if (level.isLoaded(p) && level.getBlockState(p).is(Blocks.LIGHT)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			}
			cells.remove(p.asLong());
		}
		setDirty();
	}

	/** Takes away every light on the list (the server starting). */
	public void clearAll(ServerLevel level) {
		if (cells.isEmpty()) {
			return;
		}
		for (long c : cells) {
			BlockPos p = BlockPos.of(c);
			level.getChunkAt(p);                      // load it: a few chunks round one landing zone
			if (level.getBlockState(p).is(Blocks.LIGHT)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		cells.clear();
		setDirty();
	}

	public boolean has(BlockPos p) {
		return cells.contains(p.asLong());
	}
}
