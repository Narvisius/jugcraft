package io.github.jimbozoomer.jugcraft.rocketry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A zipline anchor (batch 40, docs/features/zipline.md): it holds at most one line, to another anchor at most
 * {@value #RANGE} blocks away ({@link #link}), strung by a line-throwing rocket ({@link #connect}). The anchor the
 * rocket was fired from draws the line ({@link #draws}). Breaking either anchor takes the line down at both ends.
 */
public class ZiplineAnchorBlockEntity extends BlockEntity {
	public static final int RANGE = 96;
	/** How far the player may stand from the anchor the rocket is fired from. */
	public static final int REACH = 4;
	/** Where the line is tied on, above the anchor block's bottom. */
	public static final double ATTACH = 0.875;

	/** What stringing a line between two anchors came to. */
	public enum Result {
		OK, NOT_ANCHOR, SAME_ANCHOR, IN_USE, TOO_FAR, BLOCKED
	}

	private @Nullable BlockPos link;
	private boolean draws;

	public ZiplineAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftRocketry.ZIPLINE_ANCHOR_ENTITY, pos, state);
	}

	/** The anchor this one's line runs to, or null. */
	public @Nullable BlockPos link() {
		return link;
	}

	/** Whether this end draws the line (the other end does not, so it is drawn once). */
	public boolean draws() {
		return draws;
	}

	/** Where the line is tied on at the anchor at {@code pos}. */
	public static Vec3 attachPoint(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + ATTACH, pos.getZ() + 0.5);
	}

	/**
	 * Strings a line from the anchor at {@code from} to the anchor at {@code to}, if both are free anchors within
	 * {@link #RANGE} blocks and nothing solid is in the way. {@code from} draws it.
	 */
	public static Result connect(Level level, BlockPos from, BlockPos to, @Nullable Player player) {
		if (!(level.getBlockEntity(from) instanceof ZiplineAnchorBlockEntity a) || !(level.getBlockEntity(to) instanceof ZiplineAnchorBlockEntity b)) {
			return Result.NOT_ANCHOR;
		}
		if (from.equals(to)) {
			return Result.SAME_ANCHOR;
		}
		if (a.link != null || b.link != null) {
			return Result.IN_USE;
		}
		Vec3 start = attachPoint(from);
		Vec3 end = attachPoint(to);
		if (start.distanceToSqr(end) > (double) RANGE * RANGE) {
			return Result.TOO_FAR;
		}
		if (!clear(level, start, end, to)) {
			return Result.BLOCKED;
		}
		a.set(to, true);
		b.set(from, false);
		return Result.OK;
	}

	/** Whether the straight line from {@code start} reaches the anchor at {@code to} without passing through a block. */
	private static boolean clear(Level level, Vec3 start, Vec3 end, BlockPos to) {
		// Start just clear of the first anchor's post, so the ray does not report it.
		Vec3 from = start.add(end.subtract(start).normalize().scale(0.75));
		BlockHitResult hit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
				net.minecraft.world.phys.shapes.CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(to);
	}

	private void set(@Nullable BlockPos other, boolean draws) {
		this.link = other == null ? null : other.immutable();
		this.draws = draws;
		changed();
	}

	/** Takes this end of the line down (the other end is told by the caller). */
	public void unlink() {
		if (link != null) {
			set(null, false);
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Breaking an anchor takes its line down at the far end too. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (link != null && level != null && level.getBlockEntity(link) instanceof ZiplineAnchorBlockEntity other
				&& pos.equals(other.link)) {
			other.unlink();
		}
		link = null;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		link = input.getLong("link").map(BlockPos::of).orElse(null);
		draws = input.getIntOr("draws", 0) != 0;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (link != null) {
			output.putLong("link", link.asLong());
			output.putInt("draws", draws ? 1 : 0);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
