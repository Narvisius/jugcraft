package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An Enchanted Broom's charge and its sweeping: while charged it looks once every
 * {@value EnchantedBroomBlock#SWEEP_TICKS} ticks for dropped items within {@value EnchantedBroomBlock#RANGE} blocks
 * (a box one block up and down), and pushes at most {@value EnchantedBroomBlock#MAX_MOVES} of them toward the nearest
 * Dustpan in that box, which takes those that reach it; with no Dustpan it sweeps them into a heap at its own feet. It
 * moves only item entities, makes nothing, and its charge runs down by the sweep.
 */
public class EnchantedBroomBlockEntity extends BlockEntity {
	private int charge;
	private @Nullable BlockPos pan;
	private int panCheck;

	public EnchantedBroomBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.ENCHANTED_BROOM_ENTITY, pos, state);
	}

	public int charge() {
		return charge;
	}

	/** Charges it with one Flying Ointment's worth; returns false if it is (nearly) full already. */
	public boolean anoint() {
		if (charge > EnchantedBroomBlock.CHARGE_TICKS - EnchantedBroomBlock.CHARGE_TICKS / 20) {
			return false;
		}
		charge = EnchantedBroomBlock.CHARGE_TICKS;
		setChanged();
		return true;
	}

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (Math.floorMod(level.getGameTime() + pos.hashCode(), EnchantedBroomBlock.SWEEP_TICKS) != 0) {
			return;
		}
		charge = Math.max(0, charge - EnchantedBroomBlock.SWEEP_TICKS);
		setChanged();
		boolean swept = charge > 0 && sweep(level, pos);
		BlockState next = state.setValue(EnchantedBroomBlock.CHARGED, charge > 0).setValue(EnchantedBroomBlock.SWEEPING, swept);
		if (next != state) {
			level.setBlock(pos, next, Block.UPDATE_CLIENTS);
		}
	}

	/** One sweep; returns whether anything was moved. */
	public boolean sweep(ServerLevel level, BlockPos pos) {
		int range = EnchantedBroomBlock.RANGE;
		AABB area = new AABB(pos).inflate(range, 1, range);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.getItem().isEmpty());
		if (items.isEmpty()) {
			return false;
		}
		BlockPos dustpan = findPan(level, pos);
		Vec3 target = dustpan != null ? Vec3.atBottomCenterOf(dustpan) : Vec3.atBottomCenterOf(pos);
		DustpanBlockEntity container = dustpan != null && level.getBlockEntity(dustpan) instanceof DustpanBlockEntity d ? d : null;
		items.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(target)));
		int moved = 0;
		boolean any = false;
		for (ItemEntity item : items) {
			if (moved >= EnchantedBroomBlock.MAX_MOVES) {
				break;
			}
			Vec3 to = target.subtract(item.position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (container != null && flat <= EnchantedBroomBlock.PAN_REACH && Math.abs(to.y) < 1.5) {
				if (HopperBlockEntity.addItem(container, item)) {
					any = true;
				}
				moved++;
				continue;
			}
			if (container == null && flat < 0.6) {
				continue;
			}
			double speed = EnchantedBroomBlock.PUSH_SPEED;
			item.setDeltaMovement(to.x / flat * speed, Math.max(item.getDeltaMovement().y, 0.12), to.z / flat * speed);
			item.needsSync = true; // Tell clients now, not at the item's next periodic update.
			moved++;
			any = true;
		}
		if (any) {
			level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.3F);
			level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 2, 0.25, 0.05, 0.25, 0.0);
		}
		return any;
	}

	/** The nearest Dustpan within its range, looked for again every few sweeps (or when the last one is gone). */
	private @Nullable BlockPos findPan(ServerLevel level, BlockPos pos) {
		if (pan != null && !(level.getBlockState(pan).getBlock() instanceof DustpanBlock)) {
			pan = null;
		}
		if (pan == null || --panCheck <= 0) {
			panCheck = 5;
			pan = null;
			double best = Double.MAX_VALUE;
			int range = EnchantedBroomBlock.RANGE;
			for (BlockPos p : BlockPos.betweenClosed(pos.offset(-range, -1, -range), pos.offset(range, 1, range))) {
				if (level.getBlockState(p).getBlock() instanceof DustpanBlock) {
					double d = p.distSqr(pos);
					if (d < best) {
						best = d;
						pan = p.immutable();
					}
				}
			}
		}
		return pan;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		charge = input.getIntOr("charge", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("charge", charge);
	}
}
