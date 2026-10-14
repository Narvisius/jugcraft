package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.wallrun.WallRun;
import io.github.jimbozoomer.jugcraft.wallrun.WallRuns;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A pipe or cable in a wall (wallrun/WallRuns): the wall's own block model, whatever block it is, and over it the
 * run's block model, which is only the wall sleeves where the run comes out into the open.
 */
public final class WallRunModel extends WrapperBlockStateModel {
	private WallRunModel(BlockStateModel sleeves) {
		super(sleeves);
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, ctx) ->
				ctx.state().getBlock() instanceof WallRun ? new WallRunModel(model) : model));
	}

	private static BlockState look(BlockAndTintGetter level, BlockPos pos) {
		Object data = level instanceof FabricBlockGetter getter ? getter.getBlockEntityRenderData(pos) : null;
		return data instanceof BlockState state ? state : WallRuns.FALLBACK;
	}

	private static BlockStateModel modelOf(BlockState look) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(look);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		BlockState look = look(level, pos);
		modelOf(look).emitQuads(emitter, level, pos, look, random, cullTest);
		wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		// It looks like whatever wall it is in: never shared with another position's geometry.
		return null;
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		BlockState look = look(level, pos);
		return modelOf(look).particleMaterial(level, pos, look);
	}
}
