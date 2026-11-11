package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A plant door's lower half (PlantDoorBlock): who owns it (its badge reader lets in the owner and the owner's party),
 * and on the client how far its leaf has swung (client/PlantDoorRenderer.java), timed from when the client saw the
 * swing begin.
 */
public class PlantDoorBlockEntity extends BlockEntity {
	private @Nullable UUID owner;
	// Client: the swing being drawn (from and to angles, its start and length), and the angle last drawn.
	private boolean swinging, toOpen;
	private long start;
	private float from, last;

	public PlantDoorBlockEntity(BlockPos pos, BlockState state) {
		super(PlantDoors.ENTITY, pos, state);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** How far the leaf stands open now, in degrees (0 shut, 90 open): eased from where it was when the swing began. */
	public float angle(long gameTime, float partialTick) {
		BlockState state = getBlockState();
		boolean open = state.getValue(DoorBlock.OPEN);
		if (!state.hasProperty(PlantDoorBlock.MOVING) || !state.getValue(PlantDoorBlock.MOVING)) {
			swinging = false;
			last = open ? 90 : 0;
			return last;
		}
		if (!swinging || toOpen != open) {
			swinging = true;
			toOpen = open;
			start = gameTime;
			from = last == (open ? 90 : 0) ? (open ? 0 : 90) : last;
		}
		int ticks = open ? PlantDoorBlock.SWING_OPEN : PlantDoorBlock.SWING_SHUT;
		float t = Math.min(1, Math.max(0, (gameTime - start + partialTick) / ticks));
		float eased = t * t * (3 - 2 * t);
		float to = open ? 90 : 0;
		last = from + (to - from) * eased;
		return last;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
	}
}
