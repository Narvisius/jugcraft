package io.github.jimbozoomer.jugcraft.concordance.dreaming;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

/**
 * The Oneiric Censer (roadmap step 22): it knows only who dreams by it, for its animation (idle, or smoking while anyone
 * does). Nothing is saved here: the dream itself lives on the dreamer ({@link DreamExpedition}), and every dream ends
 * when its dreamer leaves, so after a restart no one dreams by any censer.
 */
public class OneiricCenserBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.oneiric_censer.idle");
	private static final RawAnimation SMOKING = RawAnimation.begin().thenLoop("animation.oneiric_censer.smoking");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	/** Who dreams by this censer (the server's). */
	private final Set<UUID> dreamers = new HashSet<>();
	/** Whether anyone does (sent to clients for the animation). */
	private boolean dreaming;

	public OneiricCenserBlockEntity(BlockPos pos, BlockState state) {
		super(Dreaming.CENSER_ENTITY, pos, state);
	}

	public boolean dreaming() {
		return dreaming;
	}

	/** Notes that {@code dreamer} dreams by this censer, or no longer does. */
	public void setDreamer(UUID dreamer, boolean dreams) {
		if (dreams ? dreamers.add(dreamer) : dreamers.remove(dreamer)) {
			boolean anyone = !dreamers.isEmpty();
			if (anyone != dreaming) {
				dreaming = anyone;
				if (level instanceof ServerLevel server) {
					server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		// Only ever in the update a client receives: the server saves nothing here.
		dreaming = input.getBooleanOr("dreaming", false);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("dreaming", dreaming);
		return tag;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<OneiricCenserBlockEntity>("main", 10,
				test -> test.setAndContinue(test.animatable().dreaming() ? SMOKING : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
