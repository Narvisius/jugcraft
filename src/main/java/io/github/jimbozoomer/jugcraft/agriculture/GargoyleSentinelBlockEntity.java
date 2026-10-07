package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Gargoyle Sentinel's watch: every {@value GargoyleSentinelBlock#CHECK_TICKS} ticks (offset by its position, so a
 * row of them doesn't all look on the same tick) it finds the nearest living hostile mob within range, sets its signal,
 * and tells clients which mob to turn its head to (only when that changes). The client turns the head smoothly
 * ({@link #headYaw}, not saved).
 */
public class GargoyleSentinelBlockEntity extends BlockEntity {
	private int target = -1;
	/** The head's turn on the client, degrees clockwise from straight ahead, and its last frame's. */
	public float headYaw;
	public float headYawO;

	public GargoyleSentinelBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.GARGOYLE_SENTINEL_ENTITY, pos, state);
	}

	public int target() {
		return target;
	}

	/** The nearest living hostile mob within range of {@code centre}, or null. */
	public static Mob nearestEnemy(ServerLevel level, Vec3 centre) {
		double r = GargoyleSentinelBlock.RANGE;
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(centre, centre).inflate(r),
				mob -> mob instanceof Enemy && mob.isAlive() && mob.position().distanceTo(centre) <= r);
		Mob nearest = null;
		double best = Double.MAX_VALUE;
		for (Mob mob : mobs) {
			double d = mob.position().distanceToSqr(centre);
			if (d < best) {
				best = d;
				nearest = mob;
			}
		}
		return nearest;
	}

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (Math.floorMod(level.getGameTime() + pos.asLong(), GargoyleSentinelBlock.CHECK_TICKS) == 0) {
			look(level, pos, state);
		}
	}

	/** Looks for the nearest monster now: sets the signal for its distance and turns the head to it. */
	public void look(ServerLevel level, BlockPos pos, BlockState state) {
		Vec3 centre = Vec3.atCenterOf(pos);
		Mob nearest = nearestEnemy(level, centre);
		int power = nearest == null ? 0 : GargoyleSentinelBlock.signal(nearest.position().distanceTo(centre));
		if (power != state.getValue(GargoyleSentinelBlock.POWER)) {
			level.setBlock(pos, state.setValue(GargoyleSentinelBlock.POWER, power), Block.UPDATE_ALL);
		}
		int id = nearest == null ? -1 : nearest.getId();
		if (id != target) {
			target = id;
			setChanged();
			level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		target = input.getIntOr("target", -1);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Only clients are told the target: it is an entity ID, meaningless after a reload. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("target", target);
		return tag;
	}
}
