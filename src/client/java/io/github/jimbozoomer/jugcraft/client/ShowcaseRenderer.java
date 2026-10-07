package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.BellJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BroomRackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CuriosityCabinetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowcaseBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what is on show in the Witch's Workshop's displays: the Curiosity Cabinet's nine things on its three shelves,
 * behind glazed doors that swing open while someone reaches in; the Bell Jar's one thing, turning slowly on its plinth;
 * and the brooms hung from the Broom Rack's pegs.
 */
public class ShowcaseRenderer implements BlockEntityRenderer<ShowcaseBlockEntity, ShowcaseRenderer.State> {
	/** The cabinet's nine places, {x, y, z} pixels (the thing's bottom middle), row by row from the bottom (tools/decor17_data.py). */
	private static final float[][] CABINET_PLACES = {{4.5F, 2.5F, 8.5F}, {8.0F, 2.5F, 8.5F}, {11.5F, 2.5F, 8.5F}, {4.5F, 12.4F, 8.5F},
			{8.0F, 12.4F, 8.5F}, {11.5F, 12.4F, 8.5F}, {4.5F, 22.4F, 8.5F}, {8.0F, 22.4F, 8.5F}, {11.5F, 22.4F, 8.5F}};
	private static final float CABINET_SCALE = 0.34F;
	private static final float JAR_SCALE = 0.5F;
	private static final float RACK_SCALE = 0.62F;
	/** The doors' hinges, {x, z} pixels, and how far they swing open. */
	private static final float[][] HINGES = {{1.5F, 2.8F}, {14.5F, 2.8F}};
	private static final float DOOR_SWING = 105.0F;
	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] things = new ItemStackRenderState[CuriosityCabinetBlock.PLACES];
		int places;
		Block block;
		Direction facing = Direction.NORTH;
		float time;
		float open;

		State() {
			for (int i = 0; i < things.length; i++) {
				things[i] = new ItemStackRenderState();
			}
		}
	}

	public ShowcaseRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ShowcaseBlockEntity showcase, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(showcase, state, crumbling);
		BlockState block = showcase.getBlockState();
		state.block = block.getBlock();
		state.facing = block.hasProperty(CuriosityCabinetBlock.FACING) ? block.getValue(CuriosityCabinetBlock.FACING)
				: block.hasProperty(BroomRackBlock.FACING) ? block.getValue(BroomRackBlock.FACING) : Direction.NORTH;
		long now = showcase.getLevel() == null ? 0 : showcase.getLevel().getGameTime();
		state.time = (now % 24000) + partialTick;
		state.places = Math.min(showcase.places(), state.things.length);
		for (int i = 0; i < state.places; i++) {
			itemModels.updateForTopItem(state.things[i], showcase.get(i), ItemDisplayContext.FIXED, showcase.getLevel(), null,
					(int) showcase.getBlockPos().asLong() + i);
		}
		float since = now - showcase.opened() + partialTick;
		int ticks = CuriosityCabinetBlock.DOOR_TICKS;
		state.open = since < 0 || since >= ticks ? 0.0F : since < 8 ? since / 8 : since > ticks - 10 ? (ticks - since) / 10 : 1.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (state.block instanceof CuriosityCabinetBlock) {
			for (int i = 0; i < state.places; i++) {
				thing(state.things[i], CABINET_PLACES[i], CABINET_SCALE, 180.0F + (i % 3 - 1) * 12.0F, pose, collector, state.lightCoords);
			}
			doors(state, pose, collector);
		} else if (state.block instanceof BellJarBlock) {
			float turn = state.time * 360.0F / BellJarBlock.TURN_TICKS % 360.0F;
			thing(state.things[0], new float[] {8.0F, 2.8F, 8.0F}, JAR_SCALE, turn, pose, collector, state.lightCoords);
		} else if (state.block instanceof BroomRackBlock) {
			for (int i = 0; i < state.places; i++) {
				if (state.things[i].isEmpty()) {
					continue;
				}
				// Each broom hangs by its handle from its peg, straight down, against the wall.
				pose.pushPose();
				pose.translate(BroomRackBlock.PEG_X[i] / 16, 2.4F / 16, 12.2F / 16);
				pose.rotateDegrees(Axis.YP, 180.0F);
				pose.scale(RACK_SCALE, RACK_SCALE, RACK_SCALE);
				pose.translate(0.0F, 0.5F, 0.0F);
				state.things[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				pose.popPose();
			}
		}
		pose.popPose();
	}

	/** One thing standing at {@code at} (pixels: its bottom middle), turned {@code turn} degrees about y. */
	private static void thing(ItemStackRenderState thing, float[] at, float scale, float turn, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (thing.isEmpty()) {
			return;
		}
		pose.pushPose();
		pose.translate(at[0] / 16, at[1] / 16 + scale * 0.25F, at[2] / 16);
		pose.rotateDegrees(Axis.YP, turn);
		pose.scale(scale, scale, scale);
		thing.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}

	/** The cabinet's two glazed doors, swung open about their hinges by how far the doors are open. */
	private static void doors(State state, PoseStack pose, SubmitNodeCollector collector) {
		float swing = DOOR_SWING * Mth.sin(state.open * Mth.HALF_PI);
		String[] names = {"curiosity_cabinet_door_left", "curiosity_cabinet_door_right"};
		for (int d = 0; d < 2; d++) {
			QuadModel frame = DecorQuads.get(names[d]);
			QuadModel pane = DecorQuads.get(names[d] + "_pane");
			pose.pushPose();
			pose.translate(HINGES[d][0] / 16, 0.0F, HINGES[d][1] / 16);
			pose.rotateDegrees(Axis.YP, d == 0 ? swing : -swing);
			pose.translate(-HINGES[d][0] / 16, 0.0F, -HINGES[d][1] / 16);
			if (frame != null) {
				frame.submit(pose, collector, state.lightCoords);
			}
			if (pane != null) {
				pane.submit(pose, collector, state.lightCoords);
			}
			pose.popPose();
		}
	}
}
