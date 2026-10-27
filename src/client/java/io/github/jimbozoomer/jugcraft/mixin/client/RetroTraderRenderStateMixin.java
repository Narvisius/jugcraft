package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.RetroTraderPose;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the Retro Trader's frame ({@link RetroTraderPose}) from extraction to his model and layer; null for others. */
@Mixin(VillagerRenderState.class)
public abstract class RetroTraderRenderStateMixin implements RetroTraderPose.Holder {
	@Unique
	private @Nullable RetroTraderPose jugcraft$retroTraderPose;

	@Override
	public @Nullable RetroTraderPose jugcraft$retroTrader() {
		return jugcraft$retroTraderPose;
	}

	@Override
	public void jugcraft$setRetroTrader(@Nullable RetroTraderPose pose) {
		jugcraft$retroTraderPose = pose;
	}
}
