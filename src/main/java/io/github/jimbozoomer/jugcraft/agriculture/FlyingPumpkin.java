package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pumpkin thrown by a {@link TrebuchetBlock}. Where it first comes down (on a block or on someone, whom it only
 * bumps), it bursts. The server measures the distance across the ground from where it left the sling, puts it on
 * the trebuchet's board for the player who fired it, and leaves a {@link ThrowMarker} showing the distance. A
 * pumpkin still in the air after {@link #MAX_FLIGHT} ticks is lost, and so is one whose trebuchet is gone.
 */
public class FlyingPumpkin extends ThrowableItemProjectile {
	public static final int MAX_FLIGHT = 200;

	private Vec3 origin = Vec3.ZERO;
	private BlockPos trebuchet = BlockPos.ZERO;
	private int flight;

	public FlyingPumpkin(EntityType<? extends FlyingPumpkin> type, Level level) {
		super(type, level);
	}

	public FlyingPumpkin(ServerLevel level, Vec3 start, ItemStack pumpkin, BlockPos trebuchet) {
		super(JugcraftAgriculture.FLYING_PUMPKIN, start.x, start.y, start.z, level, pumpkin.copyWithCount(1));
		this.origin = start;
		this.trebuchet = trebuchet.immutable();
	}

	@Override
	protected Item getDefaultItem() {
		return Items.PUMPKIN;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && ++flight > MAX_FLIGHT) {
			discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		land(hit.getLocation());
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		land(hit.getLocation());
	}

	/** The distance across the ground from the sling to {@code at}. */
	public double distanceTo(Vec3 at) {
		double dx = at.x - origin.x;
		double dz = at.z - origin.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Bursts at {@code at}: records the throw and marks the spot (server side). */
	public void land(Vec3 at) {
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		double distance = distanceTo(at);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PUMPKIN.defaultBlockState()), at.x, at.y + 0.2, at.z, 30,
				0.3, 0.2, 0.3, 0.1);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
		if (getOwner() instanceof ServerPlayer player && level.isLoaded(trebuchet)
				&& level.getBlockEntity(trebuchet) instanceof TrebuchetBlockEntity board) {
			String blocks = TrebuchetBlockEntity.blocks((int) Math.round(distance * 10.0));
			int place = board.record(player, distance);
			player.sendSystemMessage(place >= 0 ? Component.translatable("message.jugcraft.trebuchet.landed_place", blocks, place + 1)
					: Component.translatable("message.jugcraft.trebuchet.landed", blocks));
			if (distance >= TrebuchetBlockEntity.ADVANCEMENT_DISTANCE) {
				TrickOrTreat.award(player, "pumpkin_chunkin");
			}
			ThrowMarker marker = JugcraftAgriculture.THROW_MARKER.create(level, EntitySpawnReason.TRIGGERED);
			if (marker != null) {
				marker.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
				marker.setCustomName(Component.translatable("entity.jugcraft.throw_marker.distance", blocks));
				marker.setCustomNameVisible(true);
				level.addFreshEntity(marker);
			}
		}
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("origin", Vec3.CODEC, origin);
		output.store("trebuchet", BlockPos.CODEC, trebuchet);
		output.putInt("flight", flight);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		origin = input.read("origin", Vec3.CODEC).orElse(position());
		trebuchet = input.read("trebuchet", BlockPos.CODEC).orElse(BlockPos.ZERO);
		flight = input.getIntOr("flight", 0);
	}
}
