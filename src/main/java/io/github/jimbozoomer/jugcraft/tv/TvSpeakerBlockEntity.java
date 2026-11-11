package io.github.jimbozoomer.jugcraft.tv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A surround speaker's link (tv/TvSpeakerBlock, docs/features/flat-screen-tv.md): the screen it plays (that screen's
 * bottom-left block, its controller, when it was linked; any block of the screen finds it again if the screen has
 * grown) and when it was last linked (its light blinks). Kept on the lower half; nothing runs on the server: each
 * player's game plays the screen's sound through the speakers linked to it (client/tv/TvPlayers).
 */
public class TvSpeakerBlockEntity extends BlockEntity {
	/** How far a speaker just placed looks for a screen to link itself to, and how far a Sync Key links one (blocks). */
	public static final int AUTO_LINK = 24, MAX_LINK = 32;
	/** The speakers this game has loaded (on the client: the screens' players look their linked speakers up here). */
	public static final Set<TvSpeakerBlockEntity> LOADED = ConcurrentHashMap.newKeySet();

	private @Nullable BlockPos screen;
	private long linkedAt = Long.MIN_VALUE;

	public TvSpeakerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTv.SPEAKER_ENTITY, pos, state);
	}

	/** The screen it plays (a block of it: its controller when linked), or null. */
	public @Nullable BlockPos screen() {
		return screen;
	}

	/** The game time it was last linked (Long.MIN_VALUE never). */
	public long linkedAt() {
		return linkedAt;
	}

	/** Links it to the screen whose controller is {@code controller} (null: unlinks it). */
	public void link(@Nullable BlockPos controller) {
		screen = controller == null ? null : controller.immutable();
		linkedAt = level == null ? 0 : level.getGameTime();
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** The middle of the speaker's cabinet in the world (where its sound comes from). */
	public Vec3 cabinet() {
		return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.77, worldPosition.getZ() + 0.5);
	}

	/**
	 * The controller of the screen nearest {@code at} (by its nearest block) within {@code reach} blocks, or null: the
	 * block entities of the loaded chunks round it.
	 */
	public static @Nullable BlockPos nearestScreen(ServerLevel level, BlockPos at, int reach) {
		Vec3 from = Vec3.atCenterOf(at).add(0, 1.27, 0);
		TvBlockEntity best = null;
		double bestD = reach + 1e-6;
		int r = (reach >> 4) + 1;
		for (int cx = (at.getX() >> 4) - r; cx <= (at.getX() >> 4) + r; cx++) {
			for (int cz = (at.getZ() >> 4) - r; cz <= (at.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof TvBlockEntity tv && tv.isController() && !tv.isRemoved()) {
						double d = tv.screenDistance(from);
						if (d < bestD) {
							bestD = d;
							best = tv;
						}
					}
				}
			}
		}
		return best == null ? null : best.getBlockPos().immutable();
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if (level.isClientSide()) {
			LOADED.add(this);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		LOADED.remove(this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		screen = input.read("screen", BlockPos.CODEC).orElse(null);
		linkedAt = input.getLongOr("linked_at", Long.MIN_VALUE);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (screen != null) {
			output.store("screen", BlockPos.CODEC, screen);
		}
		output.putLong("linked_at", linkedAt);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
