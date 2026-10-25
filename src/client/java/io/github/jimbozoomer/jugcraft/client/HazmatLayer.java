package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.HazmatItem;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the hazmat suit in 3D on whoever wears it (players, armor stands, zombies...): a hood with a visor and a
 * respirator whose two filters stand out from the face, a coverall with a harness and the air tank on the back (its
 * breathing hose runs into the hood), gloves taped at the wrist, trousers, and boots with a toe cap. Each piece is
 * fixed to its body part, so it bends, turns and sneaks with the wearer. Coordinates here are model pixels with y
 * up and the front of the body at +z, measured from each part's pivot.
 */
public class HazmatLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/hazmat.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float PX = 1 / 256F;

	static final float[] FABRIC = region(0, 0);
	static final float[] FABRIC_DARK = region(1, 0);
	static final float[] RUBBER = region(2, 0);
	static final float[] TAPE = region(3, 0);
	static final float[] VISOR = region(0, 1);
	static final float[] TANK = region(1, 1);
	static final float[] STEEL = region(2, 1);
	static final float[] FILTER = region(3, 1);
	static final float[] TREFOIL = region(0, 2);
	static final float[] BADGE = region(1, 2);
	static final float[] SOLE = region(2, 2);
	static final float[] ZIP = region(3, 2);
	static final float[] TANK_BAND = region(0, 3);

	/** A 64x64 region of the 256x256 sheet (tools/nuclear.py HAZMAT_MATERIALS), inset half a pixel. */
	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PX, (row * 64 + 0.5F) * PX, (col * 64 + 63.5F) * PX, (row * 64 + 63.5F) * PX};
	}

	public HazmatLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new HazmatLayer((RenderLayerParent) renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		M model = getParentModel();
		if (worn(state.headEquipment)) {
			part(pose, collector, light, model.head, HazmatLayer::hood);
		}
		if (worn(state.chestEquipment)) {
			part(pose, collector, light, model.body, HazmatLayer::coverall);
			part(pose, collector, light, model.rightArm, g -> sleeve(g, -1));
			part(pose, collector, light, model.leftArm, g -> sleeve(g, 1));
		}
		if (worn(state.legsEquipment)) {
			part(pose, collector, light, model.rightLeg, HazmatLayer::trouser);
			part(pose, collector, light, model.leftLeg, HazmatLayer::trouser);
		}
		if (worn(state.feetEquipment)) {
			part(pose, collector, light, model.rightLeg, HazmatLayer::boot);
			part(pose, collector, light, model.leftLeg, HazmatLayer::boot);
		}
	}

	private static boolean worn(ItemStack stack) {
		return stack.getItem() instanceof HazmatItem
                && !(stack.getItem() instanceof io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGear.SourceHazmat);
	}

	private void part(PoseStack pose, SubmitNodeCollector collector, int light, ModelPart part, Consumer<Geometry> draw) {
		if (!part.visible) {
			return;
		}
		pose.pushPose();
		part.translateAndRotate(pose);
		// Part space has y down and the front at -z: half a turn about x gives y up and the front at +z.
		pose.rotateDegrees(Axis.XP, 180);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			Geometry g = new Geometry(p, consumer);
			g.light = light;
			draw.accept(g);
		});
		pose.popPose();
	}

	// ------------------------------------------------------------------ the pieces (head pivot at the neck; others at their joint)

	static void hood(Geometry g) {
		LineRenderer.box(g, -5, -1, -5, 5, 9.2, 5, FABRIC, FABRIC_DARK, RUBBER);
		LineRenderer.box(g, -5.4, -1.4, -5.4, 5.4, -0.4, 5.4, RUBBER);                         // neck seal
		LineRenderer.box(g, -3.9, 2.6, 5, 3.9, 7.6, 5.5, RUBBER);                               // visor rim
		LineRenderer.panel(g, -3.4, 3.0, 5.2, 3.4, 7.2, 5.8, LineRenderer.SOUTH, VISOR, RUBBER); // visor, standing proud
		LineRenderer.box(g, -1.6, -0.2, 5, 1.6, 2.6, 7.2, RUBBER);                              // respirator
		LineRenderer.box(g, -0.9, 0.2, 7.2, 0.9, 1.6, 7.6, STEEL);                              // exhale valve
		for (int side = -1; side <= 1; side += 2) {
			Vec3 a = new Vec3(side * 1.4, 1.0, 6.2);
			Vec3 dir = new Vec3(side * 1.6, -0.6, 1.4).normalize();
			LineRenderer.lathe(g, a, a.add(dir.scale(2.1)), new double[][] {{0, 1.15}, {0.3, 1.3}, {1.9, 1.3}, {2.1, 1.05}}, 12, STEEL);
			LineRenderer.lathe(g, a.add(dir.scale(2.1)), a.add(dir.scale(2.25)), new double[][] {{0, 1.0}, {0.15, 1.0}}, 12, FILTER);
		}
		LineRenderer.box(g, -1.0, 0.0, -5.4, 1.0, 1.6, -5.0, STEEL);                            // breathing hose inlet
	}

	static void coverall(Geometry g) {
		LineRenderer.box(g, -4.8, -12.4, -2.8, 4.8, -0.8, 2.8, FABRIC);
		LineRenderer.box(g, -0.25, -11.5, 2.8, 0.25, -1, 3.0, ZIP);
		LineRenderer.box(g, -5.1, -7.6, -3.1, 5.1, -6.4, 3.1, RUBBER);                         // belt
		LineRenderer.box(g, -0.8, -7.5, 3.1, 0.8, -6.5, 3.3, STEEL);                           // buckle
		for (int side = -1; side <= 1; side += 2) {                                            // harness over the shoulders
			LineRenderer.box(g, side * 2.0 - 0.6, -6.4, 2.8, side * 2.0 + 0.6, -0.8, 3.15, RUBBER);
			LineRenderer.box(g, side * 2.0 - 0.6, -6.4, -3.15, side * 2.0 + 0.6, -0.8, -2.8, RUBBER);
			LineRenderer.box(g, side * 2.0 - 0.6, -0.8, -3.15, side * 2.0 + 0.6, -0.45, 3.15, RUBBER);
		}
		LineRenderer.box(g, 1.4, -5.0, 3.0, 3.6, -2.4, 3.45, BADGE);                           // dosimeter badge
		LineRenderer.panel(g, -3.8, -4.8, 2.8, -1.4, -2.4, 2.86, LineRenderer.SOUTH, TREFOIL, FABRIC);
		// The air tank on the harness, its valve, and the hose into the back of the hood.
		double tz = -3.15 - 2.25;
		LineRenderer.lathe(g, new Vec3(0, -11.4, tz), new Vec3(0, -0.6, tz),
				new double[][] {{0, 0.8}, {0.4, 1.8}, {1.2, 2.2}, {9.4, 2.2}, {10.2, 1.6}, {10.8, 0.7}}, 16, TANK);
		for (double y : new double[] {-9.5, -3.5}) {
			LineRenderer.lathe(g, new Vec3(0, y, tz), new Vec3(0, y + 0.7, tz), new double[][] {{0, 2.32}, {0.7, 2.32}}, 16, RUBBER);
		}
		LineRenderer.lathe(g, new Vec3(0, -6.0, tz), new Vec3(0, -4.2, tz), new double[][] {{0, 2.26}, {1.8, 2.26}}, 16, TANK_BAND);
		LineRenderer.panel(g, -1.0, -8.6, tz - 2.27, 1.0, -6.6, tz - 2.2, LineRenderer.NORTH, TREFOIL, TANK);
		LineRenderer.lathe(g, new Vec3(0, -0.6, tz), new Vec3(0, 0.5, tz), new double[][] {{0, 0.55}, {1.1, 0.55}}, 10, STEEL);
		LineRenderer.box(g, -0.9, 0.1, tz - 0.35, 0.9, 0.55, tz + 0.35, STEEL);
		LineRenderer.tube(g, List.of(new Vec3(0, 0.5, tz + 0.2), new Vec3(0, 1.1, tz + 0.3), new Vec3(0, 1.2, -5.0)), 0.45, 8, RUBBER);
	}

	/** An arm (pivot at the shoulder): sleeve, wrist tape, glove. side -1 is the right arm. */
	static void sleeve(Geometry g, int side) {
		double x0 = side < 0 ? -3 : -1, x1 = side < 0 ? 1 : 3;
		LineRenderer.box(g, x0 - 0.8, -6.5, -2.8, x1 + 0.8, 2.2, 2.8, FABRIC_DARK);
		LineRenderer.box(g, x0 - 1.0, -7.4, -3.0, x1 + 1.0, -6.4, 3.0, TAPE);
		LineRenderer.box(g, x0 - 1.15, -10.8, -3.15, x1 + 1.15, -7.4, 3.15, RUBBER);              // glove cuff
		LineRenderer.box(g, x0 - 0.9, -11.4, -2.9, x1 + 0.9, -10.8, 2.9, RUBBER);
	}

	/** A trouser leg (pivot at the hip). */
	static void trouser(Geometry g) {
		LineRenderer.box(g, -2.6, -7.4, -2.8, 2.6, -0.2, 2.8, FABRIC);
		LineRenderer.box(g, -2.8, -8.0, -3.0, 2.8, -7.0, 3.0, TAPE);
	}

	/** A boot (pivot at the hip): shaft, toe cap standing forward, sole. */
	static void boot(Geometry g) {
		LineRenderer.box(g, -2.75, -11.4, -3.2, 2.75, -8.0, 3.2, RUBBER);
		LineRenderer.box(g, -2.75, -11.4, 3.2, 2.75, -9.8, 4.6, RUBBER);
		LineRenderer.box(g, -2.85, -12.0, -3.3, 2.85, -11.4, 4.7, SOLE);
	}
}
