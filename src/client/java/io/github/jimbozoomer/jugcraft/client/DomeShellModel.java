package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.DomeShellBlock;
import io.github.jimbozoomer.jugcraft.tower.DomeShellData;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * An atrium dome cell's look (tower/DomeShellBlock, tools/atrium_dome.py): its quads from {@link DomeShellData}, baked into
 * the chunk's mesh like any block model. The sphere's concrete is lit by its own smooth normals (the curve shades as one
 * surface, not in facets); the cell's cut faces cull against a full block beside them, as a cube's would; the light lines
 * glow (full light, no shading); the formwork joints are the concrete darkened.
 */
public final class DomeShellModel extends WrapperBlockStateModel {
	private static final int JOINT = 0xFF4E4E4C;
	private static final Direction[] DIRS = Direction.values();
	private static volatile Material.@Nullable Baked concrete, line;
	private static volatile Object bakedFor;

	private DomeShellModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, ctx) -> {
			if (!(ctx.state().getBlock() instanceof DomeShellBlock)) {
				return model;
			}
			resolve(ctx.baker());
			return new DomeShellModel(model);
		}));
	}

	private static synchronized void resolve(ModelBaker baker) {
		if (bakedFor == baker) {
			return;
		}
		concrete = baker.materials().get(new Material(Jugcraft.id("block/reinforced_concrete")), () -> "atrium dome concrete");
		line = baker.materials().get(new Material(Jugcraft.id("block/neon_amber_tube")), () -> "atrium dome light line");
		bakedFor = baker;
	}

	@Override
	public void emitQuads(QuadEmitter e, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		Material.Baked conc = concrete, glow = line;
		if (conc == null || glow == null) {
			return;
		}
		DomeShellData d = DomeShellData.get();
		int i = state.getValue(DomeShellBlock.INDEX);
		for (int q = d.quadStart(i), end = d.quadEnd(i); q < end; q++) {
			int c = d.cull(q);
			Direction face = c >= 0 && c < DIRS.length ? DIRS[c] : null;
			if (face != null && cullTest.test(face)) {
				continue;
			}
			int mat = d.material(q);
			for (int k = 0; k < 4; k++) {
				e.pos(k, d.corner(q, k, 0), d.corner(q, k, 1), d.corner(q, k, 2));
				e.uv(k, d.corner(q, k, 3), d.corner(q, k, 4));
				e.normal(k, d.corner(q, k, 5), d.corner(q, k, 6), d.corner(q, k, 7));
				e.color(k, mat == 2 ? JOINT : 0xFFFFFFFF);
			}
			e.cullFace(face);
			e.materialBake(mat == 1 ? glow : conc, MutableQuadView.BAKE_NORMALIZED);
			e.chunkLayer(ChunkSectionLayer.SOLID);
			e.emissive(mat == 1);
			e.ambientOcclusion(mat == 1 ? TriState.FALSE : TriState.DEFAULT);
			e.shadeMode(face == null ? ShadeMode.ENHANCED : ShadeMode.VANILLA);
			e.emit();
		}
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		Material.Baked m = concrete;
		return m != null ? m : wrapped.particleMaterial(level, pos, state);
	}
}
