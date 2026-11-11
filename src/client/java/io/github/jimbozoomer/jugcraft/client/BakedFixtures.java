package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock;
import io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.OpsTreeBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The tower's fixtures that never move, baked into the world's cached mesh (Justin's frame rate, 7 November 2026, his pick
 * "Static parts baked in"): their meshes (client/ConsoleMesh) were put together by their block entity renderers every
 * frame, some 23,000 quads for a parts shelving alone; now the master block's model carries them, so they are built
 * once when the chunk is and drawn from the chunk's buffers like any block. What moves or is live stays with the
 * renderers: the shelving's LEDs, charger bars, labels and glass; the console's screens, lamps and floor edges; the
 * trees' pulses, crystals, halos and the drift isle's floating island; the map's picture. Baked here:
 *
 * <ul>
 * <li>the Drone Parts Shelving (both stocks), the Tower Control Console's body, the Cradle Struts;
 * <li>the Crystal Tree's planter and its bark that never moves (r186), the Ember Oak's bed and tree (its bark; the cracks' light still pulses), the Drift Isle's
 * base; the Big Map Table's table.
 * </ul>
 *
 * Their textures are added to the block atlas ({@code assets/minecraft/atlases/blocks.json}; the shelving's and the console's
 * at half size, {@code *_baked.png}, still 680 and 100 pixels a block). Each quad is lit as the
 * renderers lit it: by the master block's light (flat), its own baked shade as its colour, no ambient occlusion; the
 * full-bright ones emissive. {@code -Djugcraft.bakedFixtures=false} puts everything back on the renderers.
 */
public final class BakedFixtures extends WrapperBlockStateModel {
	public static final boolean ON = !"false".equals(System.getProperty("jugcraft.bakedFixtures"));

	/** One mesh of a fixture: its name, where it goes in the master's frame, and whether its full-bright kind is baked too. */
	record Piece(String mesh, Matrix4f place, boolean lit) {
	}

	interface Layout {
		List<Piece> pieces(BlockState state, BlockPos pos);
	}

	/** Quads worked out in code rather than read from a mesh (the crystal tree's still bark), in the master's frame. */
	interface Extra {
		void emit(QuadEmitter e, Material.Baked sprite, BlockState state, BlockPos pos);
	}

	record Spec(Identifier sprite, Layout layout, List<String> meshes, @Nullable Extra extra) {
		Spec(Identifier sprite, Layout layout, List<String> meshes) {
			this(sprite, layout, meshes, null);
		}
	}

	private static final Map<Block, Spec> SPECS = new IdentityHashMap<>();
	private static final Map<Identifier, Material.Baked> SPRITES = new ConcurrentHashMap<>();
	private static volatile Object bakedFor;
	private final Spec spec;

	private BakedFixtures(BlockStateModel wrapped, Spec spec) {
		super(wrapped);
		this.spec = spec;
	}

	/** Whether {@code block}'s fixture meshes are baked (its renderer then leaves them out). */
	public static boolean baked(Block block) {
		return ON && SPECS.containsKey(block);
	}

	private static Matrix4f turnedAboutMiddle(double yaw) {
		return new Matrix4f().translate(0.5f, 0, 0.5f).rotateY((float) yaw).translate(-0.5f, 0, -0.5f);
	}

	public static void register() {
		if (!ON) {
			return;
		}
		SPECS.put(JugcraftTower.DRONE_PARTS_SHELVING, new Spec(Jugcraft.id("entity/drone_parts_shelving_baked"),
				(state, pos) -> List.of(new Piece(DronePartsShelvingRenderer.meshName(state.getValue(DronePartsShelvingBlock.STOCK).getSerializedName()),
						new Matrix4f().translate(0.5f, 0, 0.5f).rotateY(DronePartsShelvingRenderer.facingTurn(state.getValue(DronePartsShelvingBlock.FACING))),
						true)),
				List.of(DronePartsShelvingRenderer.meshName("a"), DronePartsShelvingRenderer.meshName("b"))));
		SPECS.put(JugcraftTower.TOWER_CONTROL_CONSOLE, new Spec(Jugcraft.id("entity/tower_control_console_baked"), (state, pos) -> {
			if (state.getValue(TowerControlConsoleBlock.PART) != 0) {
				return List.of();
			}
			float rotation = switch (state.getValue(TowerControlConsoleBlock.FACING)) {
				case EAST -> -(float) Math.PI / 2;
				case SOUTH -> (float) Math.PI;
				case WEST -> (float) Math.PI / 2;
				default -> 0;
			};
			return List.of(new Piece(TowerControlConsoleRenderer.MESH_NAME, turnedAboutMiddle(rotation), true));
		}, List.of(TowerControlConsoleRenderer.MESH_NAME)));
		SPECS.put(JugcraftTower.CRADLE_STRUTS, new Spec(Jugcraft.id("entity/cradle_struts"),
				(state, pos) -> List.of(new Piece(CradleStrutsRenderer.MESH_NAME,
						turnedAboutMiddle(-Math.PI / 2 * CradleStrutsBlock.turns(state.getValue(CradleStrutsBlock.FACING))), false)),
				List.of(CradleStrutsRenderer.MESH_NAME)));
		// the planter, and the tree's bark that never moves (r186: what sways on the threads stays with the renderer)
		SPECS.put(JugcraftTower.CRYSTAL_TREE, new Spec(Jugcraft.id("entity/crystal_tree"),
				(state, pos) -> List.of(new Piece(CrystalTreeRenderer.MESH_NAME, new Matrix4f(), true)), List.of(CrystalTreeRenderer.MESH_NAME),
				(e, sprite, state, pos) -> CrystalTreeRenderer.bakeBark(e, sprite, pos)));
		SPECS.put(JugcraftTower.BIG_MAP_TABLE, new Spec(Jugcraft.id("entity/big_map_table"),
				(state, pos) -> List.of(new Piece(BigMapTableRenderer.MESH_NAME, new Matrix4f(), true)), List.of(BigMapTableRenderer.MESH_NAME)));
		List<String> oaks = new ArrayList<>(List.of("ops_ember_bed"));
		for (int k = 0; k < OpsTreeRenderer.VARIANTS; k++) {
			oaks.add("ops_ember_oak_" + k);
			oaks.add("ops_ember_oak_" + k + "_bright");
		}
		SPECS.put(JugcraftTower.EMBER_OAK, new Spec(Jugcraft.id("entity/ops_trees"), (state, pos) -> {
			long seed = OpsTreeBlock.seed(pos);
			int k = OpsTreeRenderer.variant(seed);
			Matrix4f turned = turnedAboutMiddle(OpsTreeRenderer.turn(seed));
			// the full-bright parts baked in too, as light: the sides of the bark's plates (r183) and the leaves (r184)
			return List.of(new Piece("ops_ember_bed", new Matrix4f(), false), new Piece("ops_ember_oak_" + k, turned, false),
					new Piece("ops_ember_oak_" + k + "_bright", turned, true));
		}, oaks));
		SPECS.put(JugcraftTower.DRIFT_ISLE, new Spec(Jugcraft.id("entity/ops_trees"),
				(state, pos) -> List.of(new Piece("ops_drift_isle_base", new Matrix4f(), false)), List.of("ops_drift_isle_base")));
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, ctx) -> {
			Spec spec = SPECS.get(ctx.state().getBlock());
			if (spec == null) {
				return model;
			}
			resolve(ctx.baker(), spec);
			return new BakedFixtures(model, spec);
		}));
	}

	/** The fixture's sprite from the block atlas, and its meshes read now (not on a chunk builder's thread). */
	private static synchronized void resolve(ModelBaker baker, Spec spec) {
		if (bakedFor != baker) {
			SPRITES.clear();
			bakedFor = baker;
		}
		SPRITES.computeIfAbsent(spec.sprite(), id -> baker.materials().get(new Material(id), () -> "tower fixture " + id));
		for (String mesh : spec.meshes()) {
			ConsoleMesh.get(mesh);
		}
	}

	@Override
	public void emitQuads(QuadEmitter e, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
			Predicate<@Nullable Direction> cullTest) {
		super.emitQuads(e, level, pos, state, random, cullTest);
		Material.Baked sprite = SPRITES.get(spec.sprite());
		if (sprite == null) {
			return;
		}
		for (Piece piece : spec.layout().pieces(state, pos)) {
			ConsoleMesh mesh = ConsoleMesh.get(piece.mesh());
			if (mesh == null) {
				continue;
			}
			emit(e, sprite, mesh.quads[ConsoleMesh.SOLID], piece.place(), false);
			if (piece.lit()) {
				emit(e, sprite, mesh.quads[ConsoleMesh.LIT], piece.place(), true);
			}
		}
		if (spec.extra() != null) {
			spec.extra().emit(e, sprite, state, pos);
		}
	}

	/** A kind's quads into the chunk: placed, textured from the sprite, shaded by their own shade (or full bright). */
	static void emit(QuadEmitter e, Material.Baked sprite, float[] corners, Matrix4f place, boolean lit) {
		Matrix3f turn = place.get3x3(new Matrix3f());
		Vector3f p = new Vector3f(), n = new Vector3f();
		final int C = ConsoleMesh.CORNER;
		for (int o = 0; o + 4 * C <= corners.length; o += 4 * C) {
			for (int k = 0; k < 4; k++) {
				int c = o + k * C;
				place.transformPosition(corners[c], corners[c + 1], corners[c + 2], p);
				e.pos(k, p.x, p.y, p.z);
				e.uv(k, corners[c + 3], corners[c + 4]);
				if (lit) {
					e.normal(k, 0, 1, 0);
					e.color(k, 0xFFFFFFFF);
				} else {
					turn.transform(corners[c + 5], corners[c + 6], corners[c + 7], n);
					float l = n.length();
					if (l > 1e-6f) {
						n.div(l);
					}
					e.normal(k, n.x, n.y, n.z);
					int s = Math.round(corners[c + 8] * 255);
					e.color(k, 0xFF000000 | s << 16 | s << 8 | s);
				}
			}
			e.cullFace(null);
			e.materialBake(sprite, MutableQuadView.BAKE_NORMALIZED);
			e.chunkLayer(ChunkSectionLayer.CUTOUT);
			e.emissive(lit);
			e.ambientOcclusion(TriState.FALSE);
			e.shadeMode(ShadeMode.ENHANCED);
			e.emit();
		}
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}
}
