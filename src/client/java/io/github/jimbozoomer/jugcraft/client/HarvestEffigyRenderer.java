package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestEffigyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestEffigyBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws what the Harvest Effigy's block models can't, from its first block: his tattered cloak in its dye
 * (decor20_quads.json {@code harvest_effigy_cloak}, a pale cloth tinted), the pumpkin he wears as his head
 * ({@link CarvedHead}, {@value #HEAD_PIXELS} pixels a side on his neck), and, while he burns, the blaze: sheets of flame
 * crossed at his middle and ringed round him that climb from the corn at his feet to over his head in the first {@value #CLIMB} of the burn, flare at his
 * hands, and sink as he burns through. The flames and his head are at full brightness while he burns.
 */
public class HarvestEffigyRenderer implements BlockEntityRenderer<HarvestEffigyBlockEntity, HarvestEffigyRenderer.State> {
	private static final RenderType FLAME = RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/bonfire_flame.png"));
	/** The head's size and where its bottom sits, in pixels up from his feet (tools/decor20_data.py EFFIGY_NECK). */
	static final float HEAD_PIXELS = 13.0F;
	static final float NECK_TOP = 36.0F;
	/** His hands, the ends of the cross-pole: pixels out from his middle and up. */
	static final float HAND_X = 13.0F;
	static final float HAND_Y = 31.0F;
	/** How much of the burn the blaze takes to climb to his head, and when it begins to sink. */
	static final float CLIMB = 0.4F;
	static final float SINK = 0.85F;
	/** The blaze's tallest reach, in blocks. */
	private static final float TOP = 3.1F;
	/** The ring of flame round him: how many sheets a layer, and how far out from his middle (blocks). */
	private static final int RING = 5;
	private static final float RING_RADIUS = 0.36F;

	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final CarvedHead head = new CarvedHead();
		Direction facing = Direction.NORTH;
		boolean lit;
		float burnt;
		float time;
		int cloak = 0xFFFFFFFF;
	}

	public HarvestEffigyRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** How far up the blaze reaches, in blocks, {@code burnt} of the way through the burn. */
	static float blaze(float burnt) {
		float climb = Mth.clamp(burnt / CLIMB, 0.0F, 1.0F);
		float height = 0.45F + (TOP - 0.45F) * climb * climb * (3.0F - 2.0F * climb);
		return burnt > SINK ? height * (1.0F - 0.65F * (burnt - SINK) / (1.0F - SINK)) : height;
	}

	@Override
	public void extractRenderState(HarvestEffigyBlockEntity effigy, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(effigy, state, crumbling);
		BlockState block = effigy.getBlockState();
		Level level = effigy.getLevel();
		if (!(block.getBlock() instanceof HarvestEffigyBlock)) {
			return;
		}
		state.facing = block.getValue(HarvestEffigyBlock.FACING);
		state.lit = block.getValue(HarvestEffigyBlock.LIT);
		DyeColor cloak = block.getValue(HarvestEffigyBlock.CLOAK);
		state.cloak = 0xFF000000 | cloak.getTextureDiffuseColor();
		double now = level == null ? 0.0 : level.getGameTime() + (double) partialTick;
		state.burnt = state.lit ? Mth.clamp((float) ((now - effigy.burnStart()) / HarvestEffigyBlock.BURN_TICKS), 0.0F, 1.0F) : 0.0F;
		state.time = (float) (now % 24000.0) + (effigy.getBlockPos().hashCode() & 0xFF);
		state.head.extract(itemModels, effigy.head(), level, (int) effigy.getBlockPos().asLong(), state.lit ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lit ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
		QuadModel cloak = DecorQuads.get("harvest_effigy_cloak");
		if (cloak != null) {
			pose.pushPose();
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
			pose.translate(-0.5F, 0.0F, -0.5F);
			cloak.submit(pose, collector, state.lit ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords, state.cloak);
			pose.popPose();
		}
		pose.pushPose();
		pose.translate(0.5F, (NECK_TOP + HEAD_PIXELS / 2.0F) / 16.0F, 0.5F);
		state.head.submit(pose, collector, state.facing, HEAD_PIXELS / 16.0F, light);
		pose.popPose();
		if (!state.lit) {
			return;
		}
		float time = state.time;
		float top = blaze(state.burnt);
		boolean hands = top > HAND_Y / 16.0F;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		collector.submitCustomGeometry(pose, FLAME, (matrix, buffer) -> {
			// The column: overlapping layers a block tall, each a crossed pair at his middle and a ring of sheets round
			// him (so the fire shows past his body and cloak), narrower as they climb, each flickering on its own.
			int layers = Mth.ceil(top / 0.8F);
			for (int i = 0; i < layers; i++) {
				float bottom = i * 0.8F;
				float height = Math.min(1.1F, top - bottom + 0.15F);
				float flicker = 1.0F + 0.14F * Mth.sin(time * (0.7F + i * 0.23F) + i * 1.3F);
				float sway = 0.05F * Mth.sin(time * 0.31F + i * 2.1F);
				float width = Math.max(0.45F, 1.2F - i * 0.17F);
				crossed(buffer, matrix, 0.0F, bottom, 0.0F, width, height * flicker, sway, i * 30.0F);
				for (int k = 0; k < RING; k++) {
					double angle = Math.toRadians(k * 360.0 / RING + i * 37.0);
					float out = RING_RADIUS * (1.0F - i * 0.12F);
					float lick = 1.0F + 0.18F * Mth.sin(time * (0.9F + k * 0.11F) + k * 1.7F + i);
					sheet(buffer, matrix, (float) Math.cos(angle) * out, bottom, (float) Math.sin(angle) * out, width * 0.6F, height * lick * 0.95F,
							sway * 1.4F, (float) Math.toDegrees(angle) + 90.0F);
				}
			}
			if (hands) {
				for (int side = -1; side <= 1; side += 2) {
					float flicker = 1.0F + 0.2F * Mth.sin(time * 0.9F + side);
					crossed(buffer, matrix, side * HAND_X / 16.0F, (HAND_Y - 3.0F) / 16.0F, 0.0F, 0.45F, 0.65F * flicker,
							0.04F * Mth.sin(time * 0.4F + side), 15.0F);
				}
			}
		});
		pose.popPose();
	}

	/**
	 * One sheet of flame standing at ({@code x}, {@code y}, {@code z}), across {@code turn} degrees, seen from both sides:
	 * entityTranslucent does not cull in 26.3, so it is one quad.
	 */
	private static void sheet(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z, float width, float height, float sway,
			float turn) {
		double angle = Math.toRadians(turn);
		float dx = (float) (Math.cos(angle) * width / 2);
		float dz = (float) (Math.sin(angle) * width / 2);
		float[][] front = {{x - dx, y, z - dz, 0, 1}, {x - dx + sway, y + height, z - dz, 0, 0}, {x + dx + sway, y + height, z + dz, 1, 0},
				{x + dx, y, z + dz, 1, 1}};
		float nx = (float) -Math.sin(angle);
		float nz = (float) Math.cos(angle);
		// One quad: the translucent type draws it from both sides, and a reversed twin on the same plane would flicker.
		DecorDraw.quad(buffer, matrix, front, nx, 0, nz, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
	}

	/**
	 * Two crossed sheets of flame standing at ({@code x}, {@code y}, {@code z}) (blocks, about his middle), turned
	 * {@code turn} degrees, each one quad seen from both sides, the tips leaning {@code sway} blocks.
	 */
	private static void crossed(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z, float width, float height, float sway,
			float turn) {
		for (int pair = 0; pair < 2; pair++) {
			double angle = Math.toRadians(turn + pair * 90.0 + 45.0);
			float dx = (float) (Math.cos(angle) * width / 2);
			float dz = (float) (Math.sin(angle) * width / 2);
			float[][] front = {{x - dx, y, z - dz, 0, 1}, {x - dx + sway, y + height, z - dz, 0, 0}, {x + dx + sway, y + height, z + dz, 1, 0},
					{x + dx, y, z + dz, 1, 1}};
			float nx = (float) -Math.sin(angle);
			float nz = (float) Math.cos(angle);
			DecorDraw.quad(buffer, matrix, front, nx, 0, nz, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // He stands three blocks tall and his blaze climbs over his head.
	}
}
