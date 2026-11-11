package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nuclear.SiteBlock;
import io.github.jimbozoomer.jugcraft.nuclear.SiteData;
import io.github.jimbozoomer.jugcraft.nuclear.SiteRegistry;
import io.github.jimbozoomer.jugcraft.nuclear.TowerShell;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A Plant Structure block's look (towers and pump houses build): the quads of its site in its cell (SiteData), and
 * for a cooling tower the shell's concrete in it (TowerShell), turned with the site, baked into the chunk's mesh like
 * any block model. Glowing quads (a lamp, a lit screen) at full light; glass, water and spray in the translucent layer,
 * grating in the cutout layer. A block whose site is not loaded yet draws nothing; its chunk is built again when its
 * anchor arrives (SiteClient).
 */
public final class SiteModel extends WrapperBlockStateModel {
	/** The sites the game has (their textures are resolved when models are baked). */
	public static final List<String> SITES = List.of("ct_medium", "lake_intake", "cw_pump_house", "transformer_yard", "switchyard",
			"reactor_building", "reactor_building_refuel");
	private static volatile Map<String, Material.Baked> sprites = Map.of();
	private static volatile Object bakedFor;

	private SiteModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, ctx) -> {
			if (!(ctx.state().getBlock() instanceof SiteBlock)) {
				return model;
			}
			resolve(ctx.baker());
			return new SiteModel(model);
		}));
	}

	/** Every site texture as a baked material (once per bake). */
	private static synchronized void resolve(ModelBaker baker) {
		if (bakedFor == baker) {
			return;
		}
		Map<String, Material.Baked> out = new HashMap<>();
		java.util.Set<String> names = new java.util.TreeSet<>(TowerShell.textures());
		for (String site : SITES) {
			SiteData d = SiteData.get(site);
			if (d != null) {
				names.addAll(List.of(d.sprites));
			}
		}
		for (String name : names) {
			Material m = new Material(Identifier.fromNamespaceAndPath(Jugcraft.MOD_ID, "block/site/" + name));
			out.put(name, baker.materials().get(m, () -> "jugcraft site texture " + name));
		}
		sprites = out;
		bakedFor = baker;
	}

	/** A site texture's baked material (for the renderer of the live parts), or null. */
	public static Material.@Nullable Baked sprite(String name) {
		return sprites.get(name);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		SiteRegistry.Hit hit = SiteRegistry.find(null, pos);
		if (hit == null) {
			return;
		}
		SiteData d = hit.placement().data();
		Direction facing = hit.placement().facing();
		Map<String, Material.Baked> mats = sprites;
		int cell = d.cell(hit.x(), hit.y(), hit.z());
		int n = d.quadCount(cell);
		float[] p = new float[12], uv = new float[8], nn = new float[3];
		for (int q = 0; n > 0 && q < n; q++) {
			int at = d.quadStart(cell) + q;
			Material.Baked mat = mats.get(d.sprites[d.sprite(at)]);
			if (mat == null) continue;
			for (int c = 0; c < 4; c++) {
				p[c * 3] = d.vert(at, c, 0);
				p[c * 3 + 1] = d.vert(at, c, 1);
				p[c * 3 + 2] = d.vert(at, c, 2);
				uv[c * 2] = d.vert(at, c, 3);
				uv[c * 2 + 1] = d.vert(at, c, 4);
			}
			nn[0] = d.normal(at, 0);
			nn[1] = d.normal(at, 1);
			nn[2] = d.normal(at, 2);
			emit(emitter, facing, p, uv, nn, null, mat, layer(d.layers[d.sprite(at)]), d.lit(at));
		}
		if (d.shell != null) {
			for (TowerShell.Quad sq : d.shell.quads(hit.x(), hit.y(), hit.z())) {
				Material.Baked mat = mats.get(sq.texture());
				if (mat == null) continue;
				float[] v = sq.v();
				for (int c = 0; c < 4; c++) {
					p[c * 3] = v[c * 5];
					p[c * 3 + 1] = v[c * 5 + 1];
					p[c * 3 + 2] = v[c * 5 + 2];
					uv[c * 2] = v[c * 5 + 3];
					uv[c * 2 + 1] = v[c * 5 + 4];
				}
				emit(emitter, facing, p, uv, null, sq.n(), mat, ChunkSectionLayer.SOLID, false);
			}
		}
	}

	private static ChunkSectionLayer layer(byte l) {
		return l == 2 ? ChunkSectionLayer.TRANSLUCENT : l == 1 ? ChunkSectionLayer.CUTOUT : ChunkSectionLayer.SOLID;
	}

	/** One quad, turned with the site about the block's middle; normals per quad or per corner. */
	private static void emit(QuadEmitter e, Direction facing, float[] p, float[] uv, float @Nullable [] n, float @Nullable [] corner,
			Material.Baked mat, ChunkSectionLayer layer, boolean lit) {
		for (int c = 0; c < 4; c++) {
			float x = p[c * 3] - 0.5F, z = p[c * 3 + 2] - 0.5F;
			float nx = corner != null ? corner[c * 3] : n[0], ny = corner != null ? corner[c * 3 + 1] : n[1],
					nz = corner != null ? corner[c * 3 + 2] : n[2];
			float tx, tz, tnx, tnz;
			switch (facing) {
				case WEST -> {
					tx = -z;
					tz = x;
					tnx = -nz;
					tnz = nx;
				}
				case NORTH -> {
					tx = -x;
					tz = -z;
					tnx = -nx;
					tnz = -nz;
				}
				case EAST -> {
					tx = z;
					tz = -x;
					tnx = nz;
					tnz = -nx;
				}
				default -> {
					tx = x;
					tz = z;
					tnx = nx;
					tnz = nz;
				}
			}
			e.pos(c, tx + 0.5F, p[c * 3 + 1], tz + 0.5F);
			e.uv(c, uv[c * 2], uv[c * 2 + 1]);
			e.normal(c, tnx, ny, tnz);
		}
		e.materialBake(mat, MutableQuadView.BAKE_NORMALIZED);
		e.chunkLayer(layer);
		e.emissive(lit);
		e.ambientOcclusion(TriState.FALSE);
		e.emit();
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		Material.Baked m = sprites.get("ct_precast");
		return m != null ? m : wrapped.particleMaterial(level, pos, state);
	}
}
