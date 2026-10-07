package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what is in a Horned Skull Cauldron: the brew's surface in its potion's colour, heaving gently; up to three things
 * floating in it, half-sunk, bobbing and turning; over heat, three fume ribbons twisting up out of it in the brew's colour
 * and fading; and the skull's square eye sockets glowing flat in that colour while it holds a potion.
 */
public class HornedSkullCauldronRenderer implements BlockEntityRenderer<HornedSkullCauldronBlockEntity, HornedSkullCauldronRenderer.State> {
	private static final RenderType BREW = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/witchs_workshop_brew.png"));
	private static final RenderType FUME = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/witchs_workshop_fume.png"));
	private static final RenderType GLOW = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/witchs_workshop_glow.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** The inside's edges, in pixels. */
	private static final float INSIDE_LO = 3.3F;
	private static final float INSIDE_HI = 12.7F;
	/** Where the floating things sit about the middle, in pixels {x, z}, and how big they are. */
	private static final float[][] FLOAT_SPOTS = {{-2.0F, -1.4F}, {2.1F, 0.6F}, {-0.4F, 2.4F}};
	private static final float FLOAT_SCALE = 0.42F;
	/**
	 * The skull's square eye sockets on its face, {x0, y0, x1, y1} in pixels (tools/decor17.py CAULDRON skull_eyes, which
	 * the audit compares), and where their glow is drawn: a flat square exactly over each socket, 0.1 pixel in front of
	 * the face (at z 0.7), never a soft halo round it.
	 */
	private static final float[][] EYES = {{5.8F, 8.6F, 7.2F, 9.8F}, {8.8F, 8.6F, 10.2F, 9.8F}};
	private static final float EYE_Z = 0.6F;
	private static final float FUME_HEIGHT = 22.0F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
		int level;
		boolean heated;
		boolean potion;
		int color;
		float time;
		final ItemStackRenderState[] floating = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};
		int floaters;
	}

	public HornedSkullCauldronRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HornedSkullCauldronBlockEntity pot, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pot, state, crumbling);
		BlockState block = pot.getBlockState();
		state.facing = block.hasProperty(HornedSkullCauldronBlock.FACING) ? block.getValue(HornedSkullCauldronBlock.FACING)
				: net.minecraft.core.Direction.NORTH;
		state.level = block.hasProperty(HornedSkullCauldronBlock.LEVEL) ? block.getValue(HornedSkullCauldronBlock.LEVEL) : 0;
		state.heated = block.hasProperty(HornedSkullCauldronBlock.HEATED) && block.getValue(HornedSkullCauldronBlock.HEATED);
		state.potion = block.hasProperty(HornedSkullCauldronBlock.POTION) && block.getValue(HornedSkullCauldronBlock.POTION);
		state.color = pot.color();
		state.time = pot.getLevel() == null ? 0 : (pot.getLevel().getGameTime() % 24000) + partialTick;
		List<ItemStack> floating = pot.floating();
		state.floaters = state.level == 0 ? 0 : Math.min(floating.size(), state.floating.length);
		for (int i = 0; i < state.floaters; i++) {
			itemModels.updateForTopItem(state.floating[i], floating.get(i), ItemDisplayContext.FIXED, pot.getLevel(), null, (int) pot.getBlockPos().asLong() + i);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		submitTurned(state, pose, collector);
		pose.popPose();
	}

	/** Everything, for a cauldron whose skull faces north. */
	private void submitTurned(State state, PoseStack pose, SubmitNodeCollector collector) {
		if (state.potion) {
			int glow = 0xE0000000 | state.color;
			collector.submitCustomGeometry(pose, GLOW, (matrix, buffer) -> {
				for (float[] eye : EYES) {
					DecorDraw.quad(buffer, matrix, new float[][] {{eye[0] / 16, eye[1] / 16, EYE_Z / 16, 0, 1},
							{eye[0] / 16, eye[3] / 16, EYE_Z / 16, 0, 0}, {eye[2] / 16, eye[3] / 16, EYE_Z / 16, 1, 0},
							{eye[2] / 16, eye[1] / 16, EYE_Z / 16, 1, 1}}, 0, 0, -1, glow, FULL_BRIGHT);
				}
			});
		}
		if (state.level == 0) {
			return;
		}
		float surface = HornedSkullCauldronBlock.SURFACE[state.level] + (state.heated ? 0.15F * Mth.sin(state.time * 0.4F) : 0.0F);
		int brew = 0xD8000000 | state.color;
		int brewLight = state.potion ? FULL_BRIGHT : state.lightCoords;
		collector.submitCustomGeometry(pose, BREW, (matrix, buffer) -> DecorDraw.box(buffer, matrix, INSIDE_LO / 16, (surface - 0.05F) / 16, INSIDE_LO / 16,
				INSIDE_HI / 16, surface / 16, INSIDE_HI / 16, 0, 0, 1, 1, brew, brewLight, DecorDraw.UP));
		for (int i = 0; i < state.floaters; i++) {
			if (state.floating[i].isEmpty()) {
				continue;
			}
			pose.pushPose();
			float bob = 0.35F * Mth.sin(state.time * 0.07F + i * 2.1F);
			pose.translate(0.5F + FLOAT_SPOTS[i][0] / 16, (surface + 0.6F + bob) / 16, 0.5F + FLOAT_SPOTS[i][1] / 16);
			pose.rotateDegrees(Axis.YP, (state.time * 1.2F + i * 120.0F) % 360.0F);
			pose.rotateDegrees(Axis.XP, 12.0F * Mth.sin(state.time * 0.05F + i));
			pose.scale(FLOAT_SCALE, FLOAT_SCALE, FLOAT_SCALE);
			state.floating[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		if (state.heated) {
			submitFumes(state, pose, collector, surface);
		}
	}

	/** Three ribbons of fume twisting up out of the brew, each a column of crossed cards that sway and fade as they rise. */
	private static void submitFumes(State state, PoseStack pose, SubmitNodeCollector collector, float surface) {
		float time = state.time;
		int color = 0xB0000000 | state.color;
		collector.submitCustomGeometry(pose, FUME, (matrix, buffer) -> {
			for (int ribbon = 0; ribbon < 3; ribbon++) {
				float phase = ribbon * 2.09F;
				float baseX = 8.0F + 2.4F * Mth.cos(phase);
				float baseZ = 8.0F + 2.4F * Mth.sin(phase);
				int segments = 6;
				for (int s = 0; s < segments; s++) {
					float v0 = (float) s / segments;
					float v1 = (float) (s + 1) / segments;
					float y0 = surface + v0 * FUME_HEIGHT;
					float y1 = surface + v1 * FUME_HEIGHT;
					float sway0 = 2.2F * v0 * Mth.sin(time * 0.06F + phase + v0 * 4.0F);
					float sway1 = 2.2F * v1 * Mth.sin(time * 0.06F + phase + v1 * 4.0F);
					float w0 = 1.6F * (1.0F - v0 * 0.4F);
					float w1 = 1.6F * (1.0F - v1 * 0.4F);
					float x0 = (baseX + sway0) / 16;
					float x1 = (baseX + sway1) / 16;
					float z0 = (baseZ + sway0 * 0.6F) / 16;
					float z1 = (baseZ + sway1 * 0.6F) / 16;
					// v runs from the texture's top (faint, the ribbon's tip) to its bottom (dense, at the brew).
					float tv0 = 1.0F - v0;
					float tv1 = 1.0F - v1;
					DecorDraw.quad(buffer, matrix, new float[][] {{x0 - w0 / 16, y0 / 16, z0, 0, tv0}, {x1 - w1 / 16, y1 / 16, z1, 0, tv1},
							{x1 + w1 / 16, y1 / 16, z1, 1, tv1}, {x0 + w0 / 16, y0 / 16, z0, 1, tv0}}, 0, 0, -1, color, FULL_BRIGHT);
					DecorDraw.quad(buffer, matrix, new float[][] {{x0, y0 / 16, z0 + w0 / 16, 0, tv0}, {x1, y1 / 16, z1 + w1 / 16, 0, tv1},
							{x1, y1 / 16, z1 - w1 / 16, 1, tv1}, {x0, y0 / 16, z0 - w0 / 16, 1, tv0}}, -1, 0, 0, color, FULL_BRIGHT);
				}
			}
		});
	}
}
