package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.Geometry;
import io.github.jimbozoomer.jugcraft.nuclear.UniformItem;
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

/**
 * Draws the work clothes in 3D: worn (on players, armor stands and the like, fixed to the body and arms so they move
 * with the wearer) and hung up in a locker ({@link #hanging}, used by LockerRenderer).
 *
 * The lab coat is a modern one: crisp white, cut to below the knee, a notched lapel collar open over a blue shirt, a
 * hidden snap front (just the placket seam shows), a chest pocket with a pen and the plant's embroidered logo, the
 * photo ID clipped on the other side with a dosimeter under it, slanted hip pockets, turned-back cuffs, a half belt
 * and a vent at the back. The security jacket is navy, to the hip: a stand collar, a zip, epaulettes, flap pockets
 * with snaps, a radio clipped to the chest, SECURITY across the back and on the sleeve.
 *
 * The cloth is plain (the shape comes from the geometry; a woven texture across a face would read as stripes).
 * Coordinates are model pixels, y up, the front at +z, from each part's pivot (the body's at the neck, the arms' at
 * the shoulder).
 */
public class UniformLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/uniform.png");
	static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);
	private static final float PX = 1 / 256F;

	static final float[] WHITE = region(0, 0), WHITE_SHADE = region(1, 0), NAVY = region(2, 0), NAVY_SHADE = region(3, 0);
	static final float[] BLACK = region(0, 1), PEN = region(1, 1), SHIRT_BLUE = region(2, 1), SHIRT_WHITE = region(3, 1);
	static final float[] DOSIMETER = region(0, 2), PATCH = region(1, 2), CHROME = region(2, 2), ZIP = region(3, 2);
	static final float[] WHITE_LINING = region(0, 3), NAVY_LINING = region(1, 3), LOGO = region(2, 3), ID_BADGE = region(3, 3);

	/** A 64x64 region of the 256x256 sheet (tools/plant_office6.py UNIFORM_MATERIALS), inset half a pixel. */
	private static float[] region(int col, int row) {
		return new float[] {(col * 64 + 0.5F) * PX, (row * 64 + 0.5F) * PX, (col * 64 + 63.5F) * PX, (row * 64 + 63.5F) * PX};
	}

	public UniformLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new UniformLayer((RenderLayerParent) renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		ItemStack chest = state.chestEquipment;
		if (!(chest.getItem() instanceof UniformItem uniform)) {
			return;
		}
		M model = getParentModel();
		UniformItem.Kind kind = uniform.kind;
		part(pose, collector, light, model.body, g -> torso(g, kind, false));
		part(pose, collector, light, model.rightArm, g -> sleeve(g, kind, -1));
		part(pose, collector, light, model.leftArm, g -> sleeve(g, kind, 1));
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

	// ------------------------------------------------------------------ drawing helpers

	/** A flat panel of cloth with its corners anywhere (a lapel, a collar point, the shirt in the opening), seen from both sides. */
	private static void cloth(Geometry g, double[] a, double[] b, double[] c, double[] d, float[] uv) {
		// The normal: the quad's two edges crossed.
		double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		double vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
		double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
		double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len < 1e-9) {
			nx = 0;
			ny = 0;
			nz = 1;
		} else {
			nx /= len;
			ny /= len;
			nz /= len;
		}
		g.face((float) a[0], (float) a[1], (float) a[2], (float) b[0], (float) b[1], (float) b[2], (float) c[0], (float) c[1], (float) c[2],
				(float) d[0], (float) d[1], (float) d[2], (float) nx, (float) ny, (float) nz,
				uv[0], uv[1], uv[0], uv[3], uv[2], uv[3], uv[2], uv[1]);
	}

	private static double[] p(double x, double y, double z) {
		return new double[] {x, y, z};
	}

	// ------------------------------------------------------------------ the lab coat

	/**
	 * The coat's body (pivot at the neck). hanging: on a hanger, so the top is shaped by the hanger (sloping
	 * shoulders) instead of by a wearer's.
	 */
	static void torso(Geometry g, UniformItem.Kind kind, boolean hanging) {
		if (kind == UniformItem.Kind.LAB_COAT) {
			labCoat(g, hanging);
		} else {
			securityJacket(g, hanging);
		}
	}

	private static void labCoat(Geometry g, boolean hanging) {
		// On a hanger the body's top is low at the shoulders (the sloping caps make the top); the collar stays at the neck.
		double top = hanging ? -2.2 : 0.3, neck = hanging ? -0.4 : 0.3;
		// The body: the chest, a hair narrower through the waist, so it reads as cut rather than a box.
		LineRenderer.box(g, -4.7, -5.6, -2.7, 4.7, top, 2.7, WHITE, WHITE, WHITE_LINING);
		LineRenderer.box(g, -4.5, -12.4, -2.6, 4.5, -5.6, 2.6, WHITE, WHITE, WHITE_LINING);
		if (hanging) {
			shoulders(g, WHITE);
		}
		// The skirt below the waist seam, in two flaring tiers, open down the front.
		for (int side = -1; side <= 1; side += 2) {
			double in = side * 0.06;
			LineRenderer.box(g, side < 0 ? -4.75 : in, -17.0, -2.8, side < 0 ? in : 4.75, -12.4, 2.8, WHITE, WHITE_LINING, WHITE_LINING);
			LineRenderer.box(g, side < 0 ? -5.05 : in, -21.0, -3.0, side < 0 ? in : 5.05, -17.0, 3.0, WHITE, WHITE_LINING, WHITE_LINING);
		}
		// The seams: the waist seam round the body, the shoulder yoke across the back, the back vent, the placket.
		LineRenderer.box(g, -4.72, -12.5, -2.82, 4.72, -12.3, 2.82, WHITE_SHADE);
		LineRenderer.box(g, -4.6, -3.3, -2.78, 4.6, -3.1, -2.6, WHITE_SHADE);
		LineRenderer.box(g, -0.1, -21.0, -3.06, 0.1, -16.2, -2.9, WHITE_SHADE);
		LineRenderer.box(g, -0.1, -21.0, 2.66, 0.1, -5.9, 2.9, WHITE_SHADE);              // the front edge, inside the opening
		LineRenderer.box(g, 0.95, -12.4, 2.7, 1.1, -5.9, 2.75, WHITE_SHADE);               // the placket's edge (snaps hidden behind it)
		LineRenderer.box(g, 0.95, -17.0, 2.8, 1.1, -12.4, 2.85, WHITE_SHADE);
		LineRenderer.box(g, 0.95, -21.0, 3.0, 1.1, -17.0, 3.05, WHITE_SHADE);
		// The half belt across the back at the waist.
		LineRenderer.box(g, -4.55, -11.6, -2.8, 4.55, -10.3, -2.55, WHITE, WHITE_SHADE, WHITE_SHADE);
		// The shirt showing in the V of the opening (a blue oxford), its collar points either side of the neck.
		cloth(g, p(-1.8, neck, 2.74), p(1.8, neck, 2.74), p(0.02, -6.2, 2.72), p(-0.02, -6.2, 2.72), SHIRT_BLUE);
		for (int side = -1; side <= 1; side += 2) {
			cloth(g, p(side * 1.8, neck, 2.78), p(side * 0.3, neck, 2.78), p(side * 1.1, neck - 1.9, 2.78), p(side * 1.15, neck - 1.85, 2.78), SHIRT_WHITE);
		}
		// The collar: a stand round the back and sides of the neck, its points folded down over the chest.
		LineRenderer.box(g, -3.3, neck - 0.1, -3.0, 3.3, neck + 1.5, -2.1, WHITE, WHITE_SHADE, WHITE);
		for (int side = -1; side <= 1; side += 2) {
			double sx0 = side < 0 ? -3.3 : 2.4, sx1 = side < 0 ? -2.4 : 3.3;
			LineRenderer.box(g, sx0, neck - 0.1, -2.1, sx1, neck + 1.4, 1.2, WHITE, WHITE_SHADE, WHITE);
			// The collar point: from the top of the side stand out over the shoulder and down to its tip over the chest.
			double[] a = p(side * 2.4, neck + 1.3, 1.2), b = p(side * 3.7, neck + 0.7, 0.4), c = p(side * 4.0, neck - 1.3, 2.95),
					d = p(side * 2.45, neck - 0.8, 2.9);
			cloth(g, a, b, c, d, WHITE);
			cloth(g, p(a[0], a[1] - 0.12, a[2] - 0.04), p(b[0] + side * 0.14, b[1] - 0.1, b[2] - 0.04), p(c[0] + side * 0.14, c[1] - 0.16, c[2] - 0.04),
					p(d[0], d[1] - 0.16, d[2] - 0.04), WHITE_SHADE);
			// The lapel: the facing turned back from the notch down to where the front closes, lifted off the chest at the top.
			double[] la = p(side * 3.05, neck - 1.6, 3.12), lb = p(side * 1.15, neck - 0.05, 2.92), lc = p(side * 0.12, -6.2, 2.76),
					ld = p(side * 0.42, -5.75, 2.8);
			cloth(g, la, lb, lc, ld, WHITE);
			cloth(g, p(la[0] + side * 0.16, la[1] - 0.12, la[2] - 0.05), p(lb[0], lb[1], lb[2] - 0.05), p(lc[0], lc[1] - 0.1, lc[2] - 0.05),
					p(ld[0] + side * 0.16, ld[1] - 0.1, ld[2] - 0.05), WHITE_SHADE);
		}
		// The chest pocket (the wearer's left, +x) with the plant's embroidered logo and a pen clipped in it.
		LineRenderer.box(g, 1.6, -6.4, 2.7, 4.1, -3.6, 2.84, WHITE, WHITE_SHADE, WHITE_SHADE);
		LineRenderer.panel(g, 2.1, -5.9, 2.84, 3.3, -4.7, 2.9, LineRenderer.SOUTH, LOGO, WHITE);
		LineRenderer.box(g, 3.35, -5.2, 2.78, 3.7, -2.9, 3.02, PEN);
		LineRenderer.box(g, 3.4, -3.3, 2.92, 3.65, -3.0, 3.08, CHROME);
		// The photo ID on its clip on the other side, the dosimeter below it.
		LineRenderer.box(g, -2.65, -3.05, 2.72, -2.25, -2.55, 2.96, CHROME);
		LineRenderer.panel(g, -3.6, -5.6, 2.7, -1.5, -3.05, 2.94, LineRenderer.SOUTH, ID_BADGE, WHITE_SHADE);
		LineRenderer.panel(g, -3.5, -7.4, 2.7, -2.3, -6.0, 2.9, LineRenderer.SOUTH, DOSIMETER, BLACK);
		// Slanted hip pockets: a patch pocket each side with its top edge cut on the slant.
		for (int side = -1; side <= 1; side += 2) {
			double x0 = side < 0 ? -4.3 : 1.5, x1 = side < 0 ? -1.5 : 4.3;
			LineRenderer.box(g, x0, -17.4, 2.8, x1, -13.8, 2.94, WHITE, WHITE, WHITE_SHADE);
			g.push();
			g.translate((x0 + x1) / 2, -13.8, 2.94);
			g.roll(-side * 0.22);
			LineRenderer.box(g, -1.45, -0.1, 0, 1.45, 0.14, 0.05, WHITE_SHADE);
			g.pop();
		}
	}

	/** On a hanger the shoulders slope from the collar down to the ends of the hanger's arms. */
	private static void shoulders(Geometry g, float[] cloth) {
		for (int side = -1; side <= 1; side += 2) {
			g.push();
			g.translate(0, -0.3, 0);
			g.roll(-side * 0.36);
			LineRenderer.box(g, side < 0 ? -5.3 : 0, -1.9, -2.55, side < 0 ? 0 : 5.3, 0, 2.55, cloth);
			g.pop();
		}
	}

	// ------------------------------------------------------------------ the security jacket

	private static void securityJacket(Geometry g, boolean hanging) {
		double top = hanging ? -2.2 : 0.3, neck = hanging ? -0.4 : 0.3;
		LineRenderer.box(g, -4.7, -12.4, -2.7, 4.7, top, 2.7, NAVY, NAVY, NAVY_LINING);
		if (hanging) {
			shoulders(g, NAVY);
		}
		LineRenderer.box(g, -4.85, -14.8, -2.85, 4.85, -12.4, 2.85, NAVY_SHADE, NAVY_SHADE, NAVY_LINING);    // the waistband
		LineRenderer.box(g, -0.2, -14.6, 2.7, 0.2, neck, 2.8, ZIP);                                            // the zip
		LineRenderer.box(g, -0.32, -14.3, 2.8, 0.32, -13.6, 2.95, CHROME);                                      // its pull
		// The stand collar, open at the front.
		LineRenderer.box(g, -3.4, neck - 0.1, -3.0, 3.4, neck + 1.8, -2.1, NAVY, NAVY_SHADE, NAVY);
		for (int side = -1; side <= 1; side += 2) {
			double sx0 = side < 0 ? -3.4 : 2.5, sx1 = side < 0 ? -2.5 : 3.4;
			LineRenderer.box(g, sx0, neck - 0.1, -2.1, sx1, neck + 1.8, 2.0, NAVY, NAVY_SHADE, NAVY);
			cloth(g, p(side * 2.5, neck + 1.7, 2.0), p(side * 2.5, neck - 0.1, 2.0), p(side * 0.9, neck - 0.1, 2.85), p(side * 1.2, neck + 1.3, 2.85), NAVY);
			// Epaulettes with a snap.
			if (!hanging) {
				LineRenderer.box(g, side * 2.7 - 0.9, top, -1.6, side * 2.7 + 0.9, top + 0.3, 1.6, NAVY_SHADE);
				LineRenderer.box(g, side * 2.7 - 0.25, top + 0.3, 0.9, side * 2.7 + 0.25, top + 0.42, 1.4, CHROME);
			}
			// Chest pockets with flaps and snaps; the radio clipped on the right one, the patch on the left.
			double x0 = side < 0 ? -4.1 : 1.3, x1 = side < 0 ? -1.3 : 4.1;
			LineRenderer.box(g, x0, -7.6, 2.7, x1, -4.4, 2.82, NAVY, NAVY_SHADE, NAVY_SHADE);
			LineRenderer.box(g, x0 - 0.1, -4.6, 2.7, x1 + 0.1, -3.5, 2.92, NAVY_SHADE);
			LineRenderer.box(g, (x0 + x1) / 2 - 0.25, -4.45, 2.92, (x0 + x1) / 2 + 0.25, -3.95, 3.02, CHROME);
		}
		LineRenderer.panel(g, 1.6, -7.2, 2.82, 3.9, -5.8, 2.9, LineRenderer.SOUTH, PATCH, NAVY);
		LineRenderer.box(g, -3.9, -6.8, 2.82, -2.5, -4.3, 3.5, BLACK);                                           // the radio
		LineRenderer.box(g, -3.7, -6.2, 3.5, -2.7, -4.9, 3.56, ZIP);                                              // its speaker grille
		LineRenderer.box(g, -3.35, -4.3, 3.05, -3.05, -1.7, 3.3, BLACK);                                          // its antenna
		LineRenderer.box(g, -3.0, -5.1, 2.72, -2.8, -3.0, 2.82, CHROME);                                          // its belt clip
		// SECURITY across the shoulders at the back.
		LineRenderer.panel(g, -3.7, -8.2, -2.92, 3.7, -4.6, -2.7, LineRenderer.NORTH, PATCH, NAVY);
		// The back yoke seam and the cuffs' colour on the shoulders.
		LineRenderer.box(g, -4.6, -3.0, -2.78, 4.6, -2.8, -2.6, NAVY_SHADE);
	}

	// ------------------------------------------------------------------ sleeves

	/** A sleeve (pivot at the shoulder): to the wrist, with a turned-back cuff. side -1 is the right arm. */
	static void sleeve(Geometry g, UniformItem.Kind kind, int side) {
		boolean lab = kind == UniformItem.Kind.LAB_COAT;
		float[] cloth = lab ? WHITE : NAVY, shade = lab ? WHITE_SHADE : NAVY_SHADE;
		double x0 = side < 0 ? -3 : -1, x1 = side < 0 ? 1 : 3;
		LineRenderer.box(g, x0 - 0.6, -9.4, -2.6, x1 + 0.6, 2.4, 2.6, cloth);
		LineRenderer.box(g, x0 - 0.78, -10.3, -2.78, x1 + 0.78, -9.0, 2.78, cloth, shade, shade);     // the turned-back cuff
		LineRenderer.box(g, x0 - 0.8, -9.05, -2.8, x1 + 0.8, -8.9, 2.8, shade);                        // its fold
		if (!lab) {
			LineRenderer.box(g, x0 - 0.7, 0.6, -2.7, x1 + 0.7, 2.6, 2.7, shade);                        // the shoulder band
			// The SECURITY patch on the outside of the upper arm.
			double ox = side < 0 ? x0 - 0.62 : x1 + 0.62;
			LineRenderer.panel(g, Math.min(ox, ox - side * 0.1), -3.6, -1.5, Math.max(ox, ox - side * 0.1), -1.2, 1.5,
					side < 0 ? LineRenderer.WEST : LineRenderer.EAST, PATCH, NAVY);
		}
	}

	/**
	 * The coat on its hanger as it hangs in a locker: the hook at the origin (on the rail), the hanger's arms, the
	 * coat below shaped by them, its sleeves hanging close in at its sides.
	 */
	static void hanging(Geometry g, UniformItem.Kind kind) {
		LineRenderer.box(g, -0.2, -1.4, -0.2, 0.2, 1.0, 0.2, CHROME);
		for (int side = -1; side <= 1; side += 2) {
			g.push();
			g.translate(0, -1.4, 0);
			g.roll(-side * 0.36);
			LineRenderer.box(g, side < 0 ? -5.2 : 0, -0.3, -0.25, side < 0 ? 0 : 5.2, 0, 0.25, CHROME);
			g.pop();
		}
		g.push();
		g.translate(0, -1.0, 0);
		torso(g, kind, true);
		for (int side = -1; side <= 1; side += 2) {
			g.push();
			g.translate(side * 4.95, -4.6, -0.1);
			g.roll(-side * 0.09);
			sleeve(g, kind, side);
			g.pop();
		}
		g.pop();
	}
}
