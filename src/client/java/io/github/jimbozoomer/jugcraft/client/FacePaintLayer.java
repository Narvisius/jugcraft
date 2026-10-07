package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FacePaint;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Draws a player's face paint ({@link FacePaint}) on their face: the design's texture (entity/face_paint/&lt;design&gt;,
 * 16x16, see-through where the skin shows) a hair in front of the head's front, turning and nodding with the head. A
 * helmet or a pumpkin on the head covers it as it would the face. Added to player renderers only. The design comes from
 * the player's face paint attachment, which the server sends to every client that sees them; the render state carries
 * the player's position, so the layer finds the player there (each frame, among the few players in the level).
 */
public class FacePaintLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private static final Map<FacePaint.Design, RenderType> PAINTS = new EnumMap<>(FacePaint.Design.class);
	/** How close (blocks) a player must stand to the render state's position to be the one drawn. */
	private static final double MATCH = 2.0;

	static {
		for (FacePaint.Design design : FacePaint.Design.values()) {
			PAINTS.put(design, RenderTypes.entityTranslucent(Jugcraft.id("textures/entity/face_paint/" + design.getSerializedName() + ".png")));
		}
	}

	public FacePaintLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	/** Adds the layer to the player renderers. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (type == EntityTypes.PLAYER && renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new FacePaintLayer((RenderLayerParent) renderer));
			}
		});
	}

	/** The paint on the player drawn at ({@code x}, {@code y}, {@code z}): the nearest player there, if painted. */
	private static FacePaint.@Nullable Design paintAt(double x, double y, double z) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}
		Player nearest = null;
		double best = MATCH * MATCH;
		for (Player player : level.players()) {
			double distance = player.distanceToSqr(x, y, z);
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		return nearest == null ? null : FacePaint.design(nearest);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		M model = getParentModel();
		FacePaint.Design design = model.head.visible ? paintAt(state.x, state.y, state.z) : null;
		if (design == null) {
			return;
		}
		pose.pushPose();
		model.head.translateAndRotate(pose);
		collector.submitCustomGeometry(pose, PAINTS.get(design), (matrix, buffer) -> {
			// The head's front, in its own space (pixels; y down): from (-4, -8) to (4, 0) at z = -4, the paint just before it.
			float z = -4.04F / 16;
			DecorDraw.quad(buffer, matrix, new float[][] {{-4.0F / 16, -8.0F / 16, z, 0, 0}, {4.0F / 16, -8.0F / 16, z, 1, 0},
					{4.0F / 16, 0.0F, z, 1, 1}, {-4.0F / 16, 0.0F, z, 0, 1}}, 0, 0, -1, 0xFFFFFFFF, light);
		});
		pose.popPose();
	}
}
