package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloon;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a hot-air balloon (fall addition 29) from the quads tools/hot_air_balloon_data.py writes
 * (assets/jugcraft/balloon_quads.json): the basket, burner and rigging, turned to the balloon's facing; the envelope in
 * its design over them, swaying a little on its cables; the burner's flames while it fires, flickering; the
 * Jack-o'-Lantern's carved faces lit while it fires; and, moored, the rope down to its post.
 *
 * <p>The basket takes its light from where it stands and the envelope from its middle. While the burner fires the
 * envelope glows from inside, as balloons do at a night glow: the glow comes up fast and dies away over a second or so
 * after the burner stops.
 */
public class HotAirBalloonRenderer extends EntityRenderer<HotAirBalloon, HotAirBalloonRenderer.State> {
	/** How brightly each balloon's envelope glows (0 to 1), eased from tick to tick. */
	private static final Map<HotAirBalloon, float[]> GLOW = new WeakHashMap<>();

	public static final class State extends EntityRenderState {
		HotAirBalloon.Kind kind = HotAirBalloon.Kind.HARVEST;
		float yaw;
		boolean burning;
		float glow;
		float flicker;
		float swayX;
		float swayZ;
		int envelopeLight;
		Vec3 rope;
	}

	public HotAirBalloonRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.8F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	protected AABB getBoundingBoxForCulling(HotAirBalloon balloon, float partialTick) {
		return balloon.extent();
	}

	@Override
	public void extractRenderState(HotAirBalloon balloon, State state, float partialTick) {
		super.extractRenderState(balloon, state, partialTick);
		state.kind = balloon.kind();
		state.yaw = balloon.getYRot();
		state.burning = balloon.burning();
		float now = balloon.tickCount + partialTick;
		float[] glow = GLOW.computeIfAbsent(balloon, b -> new float[] {0.0F, now});
		float dt = Math.max(0.0F, now - glow[1]);
		glow[1] = now;
		glow[0] = state.burning ? Math.min(1.0F, glow[0] + dt * 0.25F) : Math.max(0.0F, glow[0] - dt * 0.05F);
		state.glow = glow[0];
		state.flicker = 0.85F + 0.15F * Mth.sin(now * 2.3F) * Mth.sin(now * 0.7F + 1.0F);
		// The envelope sways gently on its cables, more as it moves.
		Vec3 motion = balloon.getDeltaMovement();
		state.swayX = 0.8F * Mth.sin(now * 0.031F) + (float) Mth.clamp(motion.z * 20.0, -3.0, 3.0);
		state.swayZ = 0.8F * Mth.sin(now * 0.023F + 1.7F) - (float) Mth.clamp(motion.x * 20.0, -3.0, 3.0);
		Level level = balloon.level();
		int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(balloon.getX(),
				balloon.getY() + HotAirBalloon.THROAT + HotAirBalloon.ENVELOPE_HEIGHT / 2, balloon.getZ()));
		int block = Math.max((light >> 4) & 15, Math.round(15 * state.glow));
		int sky = (light >> 20) & 15;
		state.envelopeLight = (sky << 20) | (block << 4);
		BlockPos post = balloon.mooring();
		Vec3 at = balloon.getPosition(partialTick);
		state.rope = post == null ? null : new Vec3(post.getX() + 0.5, post.getY() + 0.85, post.getZ() + 0.5).subtract(at);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel basket = DecorQuads.get("balloon_basket");
		QuadModel envelope = DecorQuads.get("balloon_envelope_" + state.kind.id);
		QuadModel flame = DecorQuads.get("balloon_flame");
		QuadModel glow = DecorQuads.get("balloon_glow_pumpkin");
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		if (basket != null) {
			basket.submit(pose, collector, state.lightCoords);
		}
		if (state.burning && flame != null) {
			pose.pushPose();
			pose.translate(0.0F, (float) HotAirBalloon.BURNER, 0.0F);
			pose.scale(1.0F, state.flicker, 1.0F);
			pose.translate(0.0F, (float) -HotAirBalloon.BURNER, 0.0F);
			flame.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT);
			pose.popPose();
		}
		// The envelope, swaying about its throat.
		pose.pushPose();
		pose.translate(0.0F, (float) HotAirBalloon.THROAT, 0.0F);
		pose.rotateDegrees(Axis.XP, state.swayX);
		pose.rotateDegrees(Axis.ZP, state.swayZ);
		pose.translate(0.0F, (float) -HotAirBalloon.THROAT, 0.0F);
		if (envelope != null) {
			envelope.submit(pose, collector, state.envelopeLight);
		}
		if (state.kind == HotAirBalloon.Kind.PUMPKIN && state.glow > 0.05F && glow != null) {
			glow.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT);
		}
		pose.popPose();
		pose.popPose();
		if (state.rope != null) {
			drawRope(state, pose, collector);
		}
		super.submit(state, pose, collector, camera);
	}

	/** The mooring rope: from the basket's rim down to the post, sagging a little. */
	private static void drawRope(State state, PoseStack pose, SubmitNodeCollector collector) {
		QuadModel rope = DecorQuads.get("balloon_mooring_rope");
		if (rope == null) {
			return;
		}
		Vec3 from = new Vec3(0.0, HotAirBalloon.BASKET_HEIGHT * 0.6, 0.0);
		Vec3 span = state.rope.subtract(from);
		double length = span.length();
		if (length < 0.1) {
			return;
		}
		pose.pushPose();
		pose.translate(from.x, from.y, from.z);
		// The rope's quads run one block up the y axis: turn them along the span and stretch them to its length.
		float yaw = (float) Math.toDegrees(Math.atan2(span.x, span.z));
		float pitch = (float) Math.toDegrees(Math.acos(Mth.clamp(span.y / length, -1.0, 1.0)));
		pose.rotateDegrees(Axis.YP, yaw);
		pose.rotateDegrees(Axis.XP, pitch);
		pose.scale(1.0F, (float) length, 1.0F);
		rope.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}
}
