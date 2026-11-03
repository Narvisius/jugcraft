package io.github.jimbozoomer.jugcraft.test.mixin;

import io.github.jimbozoomer.jugcraft.test.TestPlayers;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raw vanilla helper calls have the same per-test lifetime as TestPlayers.mock. Never shipped with the mod. */
@Mixin(GameTestHelper.class)
public abstract class GameTestHelperPlayersMixin {
	@Inject(method = "makeMockServerPlayerInLevel", at = @At("RETURN"))
	private void jugcraftTest$trackPlayer(CallbackInfoReturnable<ServerPlayer> result) {
		TestPlayers.track((GameTestHelper) (Object) this, result.getReturnValue());
	}
}
