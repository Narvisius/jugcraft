package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DustSheetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DustSheetBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Dust Sheet draped over the block under it: a little wider than the block's shape, sagging a touch in the
 * middle of its top, its sides falling to the floor and flaring out at a ragged hem. At night one sheet in
 * {@link DustSheetBlock#BREATHE_CHANCE} rises and falls as if something under it were breathing.
 */
public class DustSheetRenderer implements BlockEntityRenderer<DustSheetBlockEntity, DustSheetRenderer.State> {
	private static final RenderType TOP = RenderTypes.entityCutout(Jugcraft.id("textures/block/dust_sheet.png"));
	private static final RenderType SIDE = RenderTypes.entityCutout(Jugcraft.id("textures/entity/dust_sheet_side.png"));
	/** How far the sheet stands off the shape, and how far its hem flares beyond that, in blocks. */
	private static final float OFF = 0.5F / 16;
	private static final float FLARE = 0.9F / 16;
	private static final float SAG = 0.35F / 16;
	private static final float BREATH = 0.7F / 16;

	public static final class State extends BlockEntityRenderState {
		boolean covered;
		float minX;
		float minZ;
		float maxX;
		float maxY;
		float maxZ;
		float breath;
	}

	public DustSheetRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DustSheetBlockEntity sheet, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sheet, state, crumbling);
		BlockState covered = sheet.covered();
		state.covered = false;
		if (covered.isAir() || sheet.getLevel() == null) {
			return;
		}
		VoxelShape shape = covered.getShape(sheet.getLevel(), sheet.getBlockPos());
		if (shape.isEmpty()) {
			return;
		}
		AABB box = shape.bounds();
		state.covered = true;
		state.minX = (float) Math.max(box.minX - OFF, -OFF);
		state.minZ = (float) Math.max(box.minZ - OFF, -OFF);
		state.maxX = (float) Math.min(box.maxX + OFF, 1 + OFF);
		state.maxZ = (float) Math.min(box.maxZ + OFF, 1 + OFF);
		state.maxY = (float) Math.min(box.maxY, 1.5) + OFF;
		state.breath = 0.0F;
		if (DustSheetBlock.breathes(sheet.getBlockPos()) && MourningAngelBlock.night(sheet.getLevel())) {
			float time = sheet.getLevel().getGameTime() % 24000 + partialTick;
			state.breath = BREATH * (0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / 60.0F));
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.covered) {
			return;
		}
		int light = state.lightCoords;
		float x0 = state.minX;
		float z0 = state.minZ;
		float x1 = state.maxX;
		float z1 = state.maxZ;
		float top = state.maxY + state.breath;
		float xm = (x0 + x1) / 2;
		float zm = (z0 + z1) / 2;
		// The top as four quads round a middle that sags (or, breathing, rises).
		float middle = top - SAG + state.breath * 0.6F;
		float edge = top - SAG / 2;
		float[][] grid = {
				{x0, top, z0}, {xm, edge, z0}, {x1, top, z0},
				{x0, edge, zm}, {xm, middle, zm}, {x1, edge, zm},
				{x0, top, z1}, {xm, edge, z1}, {x1, top, z1}};
		collector.submitCustomGeometry(pose, TOP, (matrix, buffer) -> {
			for (int row = 0; row < 2; row++) {
				for (int col = 0; col < 2; col++) {
					float[] a = grid[row * 3 + col];
					float[] b = grid[(row + 1) * 3 + col];
					float[] c = grid[(row + 1) * 3 + col + 1];
					float[] d = grid[row * 3 + col + 1];
					DecorDraw.quad(buffer, matrix, new float[][] {
							{a[0], a[1], a[2], col * 0.5F, row * 0.5F}, {b[0], b[1], b[2], col * 0.5F, row * 0.5F + 0.5F},
							{c[0], c[1], c[2], col * 0.5F + 0.5F, row * 0.5F + 0.5F}, {d[0], d[1], d[2], col * 0.5F + 0.5F, row * 0.5F}},
							0, 1, 0, 0xFFFFFFFF, light);
				}
			}
		});
		// The sides, from the top's edges down to a flared hem on the floor.
		float bottom = 0.0F;
		collector.submitCustomGeometry(pose, SIDE, (matrix, buffer) -> {
			side(buffer, matrix, light, x0, z0, x1, z0, top, bottom, 0, -1);  // north
			side(buffer, matrix, light, x1, z1, x0, z1, top, bottom, 0, 1);   // south
			side(buffer, matrix, light, x0, z1, x0, z0, top, bottom, -1, 0);  // west
			side(buffer, matrix, light, x1, z0, x1, z1, top, bottom, 1, 0);   // east
		});
	}

	/**
	 * One side from (ax, az) to (bx, bz) along the top, falling to the hem pushed out by FLARE toward its normal
	 * (nx, nz); a runs to b so that the side faces outward.
	 */
	private static void side(VertexConsumer buffer, PoseStack.Pose matrix, int light, float ax, float az, float bx, float bz, float top,
			float bottom, float nx, float nz) {
		float fx = nx * FLARE;
		float fz = nz * FLARE;
		// Along the side the hem also reaches past the corners a little, so the corners of the sheet stand out.
		float ex = Math.signum(bx - ax) * FLARE * 0.6F;
		float ez = Math.signum(bz - az) * FLARE * 0.6F;
		DecorDraw.quad(buffer, matrix, new float[][] {
				{ax + fx - ex, bottom, az + fz - ez, 0, 1}, {ax, top, az, 0, 0},
				{bx, top, bz, 1, 0}, {bx + fx + ex, bottom, bz + fz + ez, 1, 1}}, nx, 0, nz, 0xFFFFFFFF, light);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // A sheet over a tall block (a rocking chair's back) reaches above its block.
	}
}
