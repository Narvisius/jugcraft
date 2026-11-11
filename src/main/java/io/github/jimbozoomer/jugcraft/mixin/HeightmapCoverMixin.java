package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.building.RainCover;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The motion-blocking heightmaps (where rain stops, where it is raining at all, the surface) count this mod's solid
 * blocks as well as the tag {@code #minecraft:blocks_motion}, which holds vanilla's only. Every heightmap, on the server
 * and on a client, takes its test from here when it is made. See {@link RainCover}.
 */
@Mixin(Heightmap.Types.class)
public abstract class HeightmapCoverMixin {
	@ModifyReturnValue(method = "isOpaque", at = @At("RETURN"))
	private Predicate<BlockState> jugcraft$rainCover(Predicate<BlockState> original) {
		return RainCover.wrap((Heightmap.Types) (Object) this, original);
	}
}
