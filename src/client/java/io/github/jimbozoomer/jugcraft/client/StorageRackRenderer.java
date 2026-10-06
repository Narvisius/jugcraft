package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.StorageRackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what is in a storage rack on its pallets. Each of the nine bays shows its six slots: nothing when they are all
 * empty; with anything in it, a pallet, and on it a load for each slot in use, four on the deck (two along, two deep)
 * and two more on top. What a load is comes from the item in that slot (see {@link StorageRackBlockEntity#lookOf}): a
 * carton (kraft, labelled, fragile, white), a blue tote with its lid or a wooden crate for dry goods, and a drum (by
 * what is in it) or a pair of jerrycans only for things holding a liquid; sizes vary a little from load to load.
 */
public class StorageRackRenderer implements BlockEntityRenderer<StorageRackBlockEntity, StorageRackRenderer.State> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/storage_rack.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float PU = 1 / 512F, PV = 1 / 256F;
	static final float[] KRAFT = region(0, 0), KRAFT_TOP = region(1, 0), KRAFT_LABEL = region(2, 0), WHITE = region(3, 0);
	static final float[] WHITE_TOP = region(0, 1), TOTE = region(1, 1), TOTE_LID = region(2, 1), DRUM = region(3, 1);
	static final float[] DRUM_TOP = region(0, 2), CRATE = region(1, 2), PALLET = region(2, 2), PALLET_DARK = region(3, 2);
	static final float[] DRUM_RED = region(0, 3), KRAFT_DARK = region(1, 3), WHITE_LABEL = region(2, 3), CRATE_TOP = region(3, 3);
	static final float[] DRUM_BLACK = region(4, 0), DRUM_GREEN = region(5, 0), DRUM_YELLOW = region(6, 0), DRUM_WHITE = region(7, 0);
	static final float[] JERRYCAN = region(4, 1), JERRYCAN_TOP = region(5, 1), KRAFT_FRAGILE = region(6, 1), KRAFT_LABEL_C = region(7, 1);

	/** A plain red corner of the red drum's tile, for the jerrycans' caps. */
	static final float[] CAP = {(2 + 0.5F) * PU, (3 * 64 + 2 + 0.5F) * PV, (12 - 0.5F) * PU, (3 * 64 + 12 - 0.5F) * PV};

	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PU, (row * 64 + 0.5F) * PV, (col * 64 + 63.5F) * PU, (row * 64 + 63.5F) * PV};
	}

	public static final class State extends BlockEntityRenderState {
		int[] looks = new int[StorageRackBlockEntity.SIZE];
		Direction facing = Direction.NORTH;
		long seed;
	}

	public StorageRackRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StorageRackBlockEntity rack, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(rack, state, crumbling);
		state.facing = rack.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
		state.looks = rack.looks().clone();
		BlockPos pos = rack.getBlockPos();
		state.seed = pos.asLong();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		boolean any = false;
		for (int look : state.looks) {
			any |= look != 0;
		}
		if (!any) {
			return;
		}
		pose.pushPose();
		// The rack's model faces north, front to the south; the bays run to its right (east) from the left one, and this
		// block is the bottom middle bay.
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		int light = state.lightCoords;
		int[] looks = state.looks;
		long seed = state.seed;
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			for (int bay = 0; bay < 9; bay++) {
				int c = bay % 3, r = bay / 3;
				int first = bay * StorageRackBlockEntity.PER_BAY;
				boolean used = false;
				for (int k = 0; k < StorageRackBlockEntity.PER_BAY; k++) {
					used |= looks[first + k] != 0;
				}
				if (!used) {
					continue;
				}
				g.push();
				g.translate((c - 1) * 16, r * 16, 0);
				bay(g, looks, first, r == 0 ? 0 : 1.4, seed * 31 + bay);
				g.pop();
			}
		});
		pose.popPose();
	}

	/** One bay's pallet and loads, the deck at height y (the floor, or the wire deck on the beams). */
	private static void bay(Geometry g, int[] looks, int first, double y, long seed) {
		// The pallet: three bearers, the top boards across them.
		for (double z : new double[] {2.2, 7.4, 12.6}) {
			LineRenderer.box(g, 1.6, y, z, 14.4, y + 0.9, z + 1.2, PALLET_DARK);
		}
		for (int k = 0; k < 5; k++) {
			double x = 1.6 + k * 2.75;
			LineRenderer.box(g, x, y + 0.9, 1.8, x + 2.2, y + 1.3, 14.2, PALLET, PALLET, PALLET_DARK);
		}
		double deck = y + 1.3;
		// The loads: slots 0..3 on the deck (back left, back right, front left, front right), 4 and 5 stacked on the
		// front pair.
		double[] tops = new double[4];
		for (int k = 0; k < 6; k++) {
			int look = looks[first + k];
			if (look == 0) {
				continue;
			}
			long h = mix(seed * 7 + k);
			int spot = k < 4 ? k : k - 2;
			double x0 = spot % 2 == 0 ? 2.0 : 8.2, z0 = spot < 2 ? 2.2 : 8.4;
			double w = 5.4 + (h & 3) * 0.15, d = 5.2 + ((h >> 2) & 3) * 0.15;
			double hgt = 4.4 + ((h >> 4) & 7) * 0.25;
			double base = k < 4 ? deck : (tops[spot] > 0 ? tops[spot] : deck);
			double top = load(g, look - 1, x0 + ((h >> 8) & 1) * 0.2, base, z0, w, Math.min(hgt, 14.4 - base), d, h);
			if (k < 4) {
				tops[k] = top;
			}
		}
	}

	/** One load; returns the height of its top. */
	private static double load(Geometry g, int kind, double x, double y, double z, double w, double h, double d, long hash) {
		if (h < 1.5) {
			return y;
		}
		switch (kind) {
			case StorageRackBlockEntity.KRAFT, StorageRackBlockEntity.KRAFT_LABEL, StorageRackBlockEntity.KRAFT_DARK,
					StorageRackBlockEntity.KRAFT_FRAGILE, StorageRackBlockEntity.KRAFT_LABEL_C -> {
				float[] side = switch (kind) {
					case StorageRackBlockEntity.KRAFT_LABEL -> KRAFT_LABEL;
					case StorageRackBlockEntity.KRAFT_DARK -> KRAFT_DARK;
					case StorageRackBlockEntity.KRAFT_FRAGILE -> KRAFT_FRAGILE;
					case StorageRackBlockEntity.KRAFT_LABEL_C -> KRAFT_LABEL_C;
					default -> KRAFT;
				};
				LineRenderer.box(g, x, y, z, x + w, y + h, z + d, side, KRAFT_TOP, KRAFT);
				return y + h;
			}
			case StorageRackBlockEntity.WHITE, StorageRackBlockEntity.WHITE_LABEL -> {
				LineRenderer.box(g, x, y, z, x + w, y + h, z + d, kind == StorageRackBlockEntity.WHITE_LABEL ? WHITE_LABEL : WHITE, WHITE_TOP, WHITE);
				return y + h;
			}
			case StorageRackBlockEntity.TOTE -> {
				// A tote crate with its lid, a lip round the top.
				double th = Math.min(h, 4.2);
				LineRenderer.box(g, x + 0.2, y, z + 0.2, x + w - 0.2, y + th - 0.4, z + d - 0.2, TOTE, TOTE_LID, TOTE);
				LineRenderer.box(g, x, y + th - 0.4, z, x + w, y + th, z + d, TOTE, TOTE_LID, TOTE);
				return y + th;
			}
			case StorageRackBlockEntity.DRUM_BLUE, StorageRackBlockEntity.DRUM_RED, StorageRackBlockEntity.DRUM_BLACK,
					StorageRackBlockEntity.DRUM_GREEN, StorageRackBlockEntity.DRUM_YELLOW, StorageRackBlockEntity.DRUM_WHITE -> {
				// A drum: round, its hoops proud of it.
				double dh = Math.min(h + 1.5, 8.6);
				double cx = x + w / 2, cz = z + d / 2;
				float[] body = switch (kind) {
					case StorageRackBlockEntity.DRUM_RED -> DRUM_RED;
					case StorageRackBlockEntity.DRUM_BLACK -> DRUM_BLACK;
					case StorageRackBlockEntity.DRUM_GREEN -> DRUM_GREEN;
					case StorageRackBlockEntity.DRUM_YELLOW -> DRUM_YELLOW;
					case StorageRackBlockEntity.DRUM_WHITE -> DRUM_WHITE;
					default -> DRUM;
				};
				LineRenderer.lathe(g, new Vec3(cx, y, cz), new Vec3(cx, y + dh, cz),
						new double[][] {{0, 2.6}, {dh * 0.32, 2.6}, {dh * 0.32, 2.75}, {dh * 0.36, 2.75}, {dh * 0.36, 2.6}, {dh * 0.64, 2.6},
								{dh * 0.64, 2.75}, {dh * 0.68, 2.75}, {dh * 0.68, 2.6}, {dh, 2.6}}, 14, body);
				LineRenderer.lathe(g, new Vec3(cx, y + dh - 0.01, cz), new Vec3(cx, y + dh + 0.02, cz), new double[][] {{0, 2.55}, {0.03, 2.55}}, 14,
						DRUM_TOP);
				return y + dh;
			}
			case StorageRackBlockEntity.JERRYCANS -> {
				// Two plastic jerrycans side by side, each with its carry handle and a red cap.
				double ch = Math.min(4.6, h + 0.4), cw = Math.min(4.4, w - 0.4), cd = (d - 0.5) / 2;
				for (int k = 0; k < 2; k++) {
					double z0 = z + k * (cd + 0.5);
					LineRenderer.box(g, x, y, z0, x + cw, y + ch, z0 + cd, JERRYCAN, JERRYCAN_TOP, JERRYCAN);
					// The handle: two posts and the grip between them, along the can.
					double hz0 = z0 + cd / 2 - 0.3, hz1 = z0 + cd / 2 + 0.3;
					LineRenderer.box(g, x + 0.9, y + ch, hz0, x + 1.3, y + ch + 0.9, hz1, JERRYCAN_TOP);
					LineRenderer.box(g, x + 2.5, y + ch, hz0, x + 2.9, y + ch + 0.9, hz1, JERRYCAN_TOP);
					LineRenderer.box(g, x + 0.9, y + ch + 0.9, hz0, x + 2.9, y + ch + 1.3, hz1, JERRYCAN_TOP);
					// The spout's cap at the front corner.
					LineRenderer.box(g, x + cw - 1.2, y + ch, z0 + cd / 2 - 0.45, x + cw - 0.3, y + ch + 0.7, z0 + cd / 2 + 0.45, CAP);
				}
				return y + ch + 1.3;
			}
			default -> {
				LineRenderer.box(g, x, y, z, x + w, y + h, z + d, CRATE, CRATE_TOP, CRATE);
				return y + h;
			}
		}
	}

	private static long mix(long v) {
		v ^= v >>> 33;
		v *= 0xff51afd7ed558ccdL;
		v ^= v >>> 33;
		v *= 0xc4ceb9fe1a85ec53L;
		v ^= v >>> 33;
		return v & Long.MAX_VALUE;
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The bays reach a block either side and two up.
	}
}
