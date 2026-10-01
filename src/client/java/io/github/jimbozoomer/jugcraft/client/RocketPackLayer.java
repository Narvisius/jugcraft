package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.RocketPackItem;
import java.io.Reader;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

/**
 * Draws a worn rocket pack in 3D on the wearer's back: the same boxes as its item model
 * (assets/jugcraft/worn_models.json), fixed to the body so it follows bending and sneaking. The armor
 * layer adds the harness straps.
 */
public class RocketPackLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private static final Identifier FILE = Jugcraft.id("worn_models.json");
	/** Pack size on the back: its 14.75-pixel item model scaled to about a body's height. */
	private static final float SCALE = 0.8F;
	private static @Nullable QuadModel pack;
	private static boolean loaded;

	public RocketPackLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	/** Adds the layer to every humanoid renderer (players, zombies, armor stands...). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new RocketPackLayer((RenderLayerParent) renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		if (!(state.chestEquipment.getItem() instanceof RocketPackItem)) {
			return;
		}
		QuadModel model = pack();
		if (model == null) {
			return;
		}
		pose.pushPose();
		getParentModel().body.translateAndRotate(pose);
		// Body space: y runs down from the neck and the back faces +z (2 pixels from the middle). The pack model stands
		// upright with its straps at z = 5 pixels: turn it over (about z, so nothing is mirrored), hang it from the
		// shoulders and put the strap side against the back.
		pose.translate(0.0F, -0.5F / 16, 2.0F / 16);
		pose.rotateDegrees(Axis.ZP, 180);
		pose.scale(SCALE, SCALE, SCALE);
		pose.translate(-8.0F / 16, -14.75F / 16, -5.0F / 16);
		model.submit(pose, collector, light);
		pose.popPose();
	}

	private static @Nullable QuadModel pack() {
		if (!loaded) {
			loaded = true;
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.warn("Missing {}: worn rocket packs show only their straps", FILE);
				return null;
			}
			try (Reader reader = resource.get().openAsReader()) {
				pack = QuadModel.parse(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("rocket_pack"));
			} catch (Exception e) {
				Jugcraft.LOGGER.warn("Could not read {}", FILE, e);
			}
		}
		return pack;
	}
}
