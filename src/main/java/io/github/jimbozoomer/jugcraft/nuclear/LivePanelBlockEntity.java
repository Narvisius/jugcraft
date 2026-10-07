package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A diesel room panel's link to its generator (round 14, Justin: "link the equipment to the nearest diesel
 * generator... a generator connects to at most one"). Each panel links to the nearest emergency diesel generator within
 * {@link #REACH} blocks that has no other panel of its kind yet, so a generator has at most one engine panel, one
 * generator panel, one regulator and so on; a second set of panels put down by a second generator takes that one. The
 * link is kept while the generator stands (checked every two seconds), saved, and sent to the players, whose
 * client/LivePanelRenderer.java draws the panel from that generator's state.
 */
public class LivePanelBlockEntity extends BlockEntity {
	public static final int REACH = 24;
	/** The linked generator's anchor, or null. */
	public @Nullable BlockPos generator;
	/**
	 * The generator's run time (game ticks) when this panel was linked to it: the engine panel's hour meter counts from
	 * there, so it reads zero when it is linked (Justin, round 14). -1: not linked.
	 */
	public long hoursBase = -1;
	private int timer;

	public LivePanelBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.LIVE_PANEL_ENTITY, pos, state);
	}

	public void serverTick(ServerLevel level) {
		if (timer-- > 0) {
			return;
		}
		timer = 40;
		if (generator != null && isGenerator(level, generator) && generator.distSqr(getBlockPos()) <= (double) REACH * REACH) {
			if (hoursBase < 0 && level.getBlockEntity(generator) instanceof EquipmentBlockEntity edg) {
				// Linked before the hour meter counted from the link: it starts from zero now.
				hoursBase = edg.runTicksAt(level.getGameTime());
				sync(level);
			}
			return;
		}
		BlockPos found = choose(level);
		if (found == null ? generator != null : !found.equals(generator)) {
			generator = found;
			hoursBase = found != null && level.getBlockEntity(found) instanceof EquipmentBlockEntity edg ? edg.runTicksAt(level.getGameTime()) : -1;
			sync(level);
		}
	}

	private void sync(ServerLevel level) {
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** The hours the linked generator has run since this panel was linked to it, at game time {@code now}. */
	public double hours(EquipmentBlockEntity edg, double now) {
		long base = Math.max(0, hoursBase);
		double total = edg.runTicks + (edg.running ? Math.max(0, now - edg.runningSince) : 0);
		return Math.max(0, total - base) / DieselFuel.TICKS_PER_HOUR;
	}

	private static boolean isGenerator(ServerLevel level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EquipmentBlockEntity e && e.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR;
	}

	/** The nearest generator in reach with no other panel of this kind linked to it. */
	private @Nullable BlockPos choose(ServerLevel level) {
		List<BlockPos> candidates = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR && e.getKey().distSqr(getBlockPos()) <= (double) REACH * REACH
					&& isGenerator(level, e.getKey())) {
				candidates.add(e.getKey().immutable());
			}
		}
		candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(getBlockPos())));
		for (BlockPos candidate : candidates) {
			if (!taken(level, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/** Whether another panel of this kind is already linked to the generator. */
	private boolean taken(ServerLevel level, BlockPos generator) {
		Block kind = getBlockState().getBlock();
		int r = (REACH >> 4) + 1;
		for (int cx = (generator.getX() >> 4) - r; cx <= (generator.getX() >> 4) + r; cx++) {
			for (int cz = (generator.getZ() >> 4) - r; cz <= (generator.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be != this && be instanceof LivePanelBlockEntity other && generator.equals(other.generator)
							&& other.getBlockState().getBlock() == kind) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (generator != null) {
			out.store("generator", BlockPos.CODEC, generator);
			out.putLong("hours_base", hoursBase);
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		generator = in.read("generator", BlockPos.CODEC).orElse(null);
		hoursBase = in.getLongOr("hours_base", -1L);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		if (generator != null) {
			out.store("generator", BlockPos.CODEC, generator);
			out.putLong("hours_base", hoursBase);
		}
		return out.buildResult();
	}
}
