package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.nuclear.ServerRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A fire (or soul fire) put down tells the plant computer rooms in reach (nuclear/ServerRoom.fire): their smoke detectors
 * look then, so no room has to watch for fire all the time.
 */
@Mixin(BaseFireBlock.class)
public class ServerRoomFireMixin {
	@Inject(method = "onPlace", at = @At("HEAD"))
	private void jugcraft$serverRooms(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
		if (level instanceof ServerLevel server && !oldState.is(state.getBlock())) {
			ServerRoom.fire(server, pos);
		}
	}
}
