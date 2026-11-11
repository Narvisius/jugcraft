package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.warehouse.Drone;
import io.github.jimbozoomer.jugcraft.warehouse.DroneKind;
import io.github.jimbozoomer.jugcraft.warehouse.Kinematics;
import io.github.jimbozoomer.jugcraft.warehouse.Leg;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the warehouses' drones (no entities: each terminal's fleet, client/warehouse/WarehouseClient.java): each at its
 * leg's pose for this frame, part by part (tools/warehouse.py: the pallet drone's chassis, triplex mast stages,
 * carriage and turret head with what it carries; the stacker's base, arm joints, gripper with its case, film
 * carriage, and the film running to the pallet while it wraps). The status strip shows what it is doing (cyan idle,
 * green working, amber on its way home or low), the beacon flashes while it moves, the pallet drone's rear screen shows
 * its name, job and battery.
 */
public final class DroneRenderer {
	private static final double RANGE = 128;

	private DroneRenderer() {
	}

	static void render(Vec3 camera, PoseStack pose, SubmitNodeCollector collector, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		Level level = minecraft.level;
		if (level == null) {
			return;
		}
		double time = level.getGameTime() + partialTick;
		for (TerminalBlockEntity terminal : WarehouseClient.terminals()) {
			if (terminal.isRemoved() || terminal.getBlockPos().distToCenterSqr(camera) > (RANGE + 96) * (RANGE + 96)) {
				continue;
			}
			TerminalPayload info = WarehouseClient.snapshot(terminal.getBlockPos());
			for (Drone.View view : terminal.clientFleet()) {
				Leg leg = view.leg();
				Leg.Key k = leg.at(time);
				double dx = k.x() - camera.x, dz = k.z() - camera.z, dy = leg.y() - camera.y;
				if (dx * dx + dz * dz + dy * dy > RANGE * RANGE) {
					continue;
				}
				int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(k.x(), leg.y() + 0.5, k.z()));
				boolean moving = time < leg.end();
				TerminalPayload.DroneLine line = line(info, view);
				int strip = 0xFF5ADCF0;
				if (line != null) {
					strip = line.battery() < 0.2F || line.job().equals("TO DOCK") ? 0xFFFFB83C : moving ? 0xFF64E68C : 0xFF5ADCF0;
				}
				// Part W5: LOCATE: its strip blue and its beacon flashing fast for ten seconds.
				boolean locating = time < view.locateUntil();
				if (locating) {
					strip = ((long) time / 4) % 2 == 0 ? 0xFF3C8CFF : 0xFF1A3A80;
				}
				WhQuads.Tint tint = new WhQuads.Tint(strip, locating ? ((long) time / 4) % 2 == 0 : moving && ((long) time / 10) % 2 == 0);
				pose.pushPose();
				pose.translate(dx, dy, dz);
				pose.rotateDegrees(Axis.YP, k.yaw());
				pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
				pose.translate(-8, 0, -8);
				if (view.droneKind() == DroneKind.PALLET) {
					palletDrone(pose, collector, k, light, tint, line, minecraft.font);
				} else {
					stacker(pose, collector, k, light, tint);
				}
				pose.popPose();
			}
		}
	}

	private static TerminalPayload.DroneLine line(TerminalPayload info, Drone.View view) {
		if (info == null) {
			return null;
		}
		String name = String.format(Locale.ROOT, "%s-%02d", view.droneKind().prefix, view.id());
		for (TerminalPayload.DroneLine l : info.fleet()) {
			if (l.name().equals(name)) {
				return l;
			}
		}
		return null;
	}

	private static void palletDrone(PoseStack pose, SubmitNodeCollector collector, Leg.Key k, int light, WhQuads.Tint tint,
			TerminalPayload.DroneLine line, Font font) {
		palletDrone(pose, collector, k.part(0), k.part(1), k.part(2), k.look(), k.part(3) > 0.5F, light, tint,
				line == null ? null : line.name(), line == null ? "" : line.job() + (line.where().isEmpty() ? "" : " " + line.where()),
				line == null ? 0 : line.battery(), font);
	}

	/**
	 * A pallet drone in its own frame (pixels, its middle at (8, 0, 8), facing +z): its forks {@code lift} up, its head
	 * across by {@code traverse} and turned by {@code head}, carrying load {@code look} (-1 none), the scanner's lines while
	 * {@code scanning}; its rear screen shows {@code name} (none: blank), the job and the battery. Also the cargo hall's
	 * look-only drone (CargoHallRenderer).
	 */
	static void palletDrone(PoseStack pose, SubmitNodeCollector collector, float lift, float traverse, float head, int look, boolean scanning,
			int light, WhQuads.Tint tint, @Nullable String name, String job, float battery, Font font) {
		float stage = (float) Kinematics.stage(lift);
		WhQuads.submitPart(collector, pose, "pd_chassis", light, tint);
		WhQuads.submitPart(collector, pose, "pd_mast_outer", light, tint);
		pose.pushPose();
		pose.translate(0, stage, 0);
		WhQuads.submitPart(collector, pose, "pd_mast_mid", light, tint);
		pose.translate(0, stage, 0);
		WhQuads.submitPart(collector, pose, "pd_mast_inner", light, tint);
		pose.popPose();
		pose.pushPose();
		pose.translate(0, lift, 0);
		WhQuads.submitPart(collector, pose, "pd_carriage", light, tint);
		pose.popPose();
		// The head: across by traverse, up by lift, turned about its pivot.
		pose.pushPose();
		pose.translate(Kinematics.HEAD_X + traverse, lift, Kinematics.HEAD_Z);
		pose.rotateDegrees(Axis.YP, head);
		pose.translate(-Kinematics.HEAD_X, 0, -Kinematics.HEAD_Z);
		if (look >= 0) {
			pose.pushPose();
			pose.translate(Kinematics.CARRIED[0], Kinematics.CARRIED[1], Kinematics.CARRIED[2]);
			WhQuads.submit(collector, pose, WhQuads.loadQuads(look), light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		// Part W5: a cycle count: the scanner's two red lines from the carriage's face to the label it reads (6 px under
		// it, 15.7 px out to the rack's face: Planner.countJob), the red line across the label.
		if (scanning) {
			float[] from = {8, 7.5F, 19.6F};
			for (float dx : new float[] {-2.2F, 2.2F}) {
				WhQuads.line(collector, pose, from, new float[] {8 + dx, 1.5F, 19.6F + 15.6F}, 0.18F, 0xFFFF3020);
			}
			WhQuads.line(collector, pose, new float[] {8 - 2.4F, 1.5F, 19.6F + 15.55F}, new float[] {8 + 2.4F, 1.5F, 19.6F + 15.55F}, 0.35F, 0xFFFF5040);
		}
		pose.translate(0, 0, Kinematics.HEAD_SHIFT);
		WhQuads.submitPart(collector, pose, "pd_head", light, tint);
		pose.popPose();
		// The rear screen (its face at z 0.7, x 4.4..11.6, y 4.4..6.8, facing back): name, job, battery.
		if (name != null) {
			pose.pushPose();
			pose.translate(11.5, 6.7, 0.69);
			pose.rotateDegrees(Axis.YP, 180);
			pose.scale(0.06F, -0.06F, 0.06F);
			int bat = Math.round(battery * 100);
			submit(collector, pose, font, name + "  " + bat + "%", 0, 0, 0xFFE8EEF4);
			submit(collector, pose, font, job, 0, 11, 0xFF64E68C);
			bars(collector, pose, font, battery, 0, 22);
			pose.popPose();
		}
	}

	private static void stacker(PoseStack pose, SubmitNodeCollector collector, Leg.Key k, int light, WhQuads.Tint tint) {
		stacker(pose, collector, k.part(0), k.part(1), k.part(2), k.part(3), k.part(4), k.look(), light, tint);
	}

	/**
	 * A stacker drone in its own frame: its arm's base turn, shoulder, elbow and wrist (degrees), its film carriage up by
	 * {@code film}, the case on its cups ({@code look} 0..5, -1 none; 100 wrapping). Also the cargo hall's (CargoHallRenderer).
	 */
	static void stacker(PoseStack pose, SubmitNodeCollector collector, float q0, float q1, float q2, float q3, float film, int look, int light,
			WhQuads.Tint tint) {
		double bx = Kinematics.SD_BASE_X, bz = Kinematics.SD_BASE_Z;
		WhQuads.submitPart(collector, pose, "sd_base", light, tint);
		pose.pushPose();
		pose.translate(0, Kinematics.FILM_BASE + film, 0);
		WhQuads.submitPart(collector, pose, "sd_film", light, tint);
		if (look == 100) {
			// The film from the roll (x -1.3, z 3.7 on the carriage) to the turning pallet's near corner, a block ahead.
			double rx = -1.3, rz = 3.7, tx = 1.0, tz = 17.0;
			double ang = Math.toDegrees(Math.atan2(tx - rx, tz - rz)), len = Math.hypot(tx - rx, tz - rz);
			pose.pushPose();
			pose.translate(rx, 0.4, rz);
			pose.rotateDegrees(Axis.YP, (float) ang);
			pose.scale(1, 1, (float) (len / 16.0));
			WhQuads.submitPart(collector, pose, "film_web", light, WhQuads.Tint.NONE);
			pose.popPose();
		}
		pose.popPose();
		pose.pushPose();
		turnY(pose, bx, bz, q0);
		WhQuads.submitPart(collector, pose, "sd_j1", light, tint);
		turnX(pose, Kinematics.SD_SHOULDER, bz, q1);
		WhQuads.submitPart(collector, pose, "sd_upper", light, tint);
		turnX(pose, Kinematics.SD_SHOULDER + Kinematics.SD_UPPER, bz, q2);
		WhQuads.submitPart(collector, pose, "sd_fore", light, tint);
		double wy = Kinematics.SD_SHOULDER + Kinematics.SD_UPPER + Kinematics.SD_FORE;
		turnX(pose, wy, bz, q3);
		WhQuads.submitPart(collector, pose, "sd_wrist", light, tint);
		if (look >= 0 && look < 6) {
			// The case on the cups, its top against them; the wrist turned back so it stays square to the drone.
			double cx = bx + Kinematics.CUPS[0], cy = wy + Kinematics.CUPS[1];
			pose.translate(cx, cy, bz);
			pose.rotateDegrees(Axis.YP, -q0);
			pose.translate(-Kinematics.CASE / 2, Kinematics.CASE_H, Kinematics.CASE / 2);
			pose.rotateDegrees(Axis.XP, 180);
			String[] kinds = {"case_carton", "case_carton_w", "case_sack", "case_crate", "case_drum", "case_ingot"};
			WhQuads.submitPart(collector, pose, kinds[look], light, WhQuads.Tint.NONE);
		}
		pose.popPose();
	}

	private static void turnY(PoseStack pose, double x, double z, float deg) {
		pose.translate(x, 0, z);
		pose.rotateDegrees(Axis.YP, deg);
		pose.translate(-x, 0, -z);
	}

	private static void turnX(PoseStack pose, double y, double z, float deg) {
		pose.translate(0, y, z);
		pose.rotateDegrees(Axis.XP, deg);
		pose.translate(0, -y, -z);
	}

	static void submit(SubmitNodeCollector collector, PoseStack pose, Font font, String text, float x, float y, int colour) {
		collector.submitText(pose, x, y, io.github.jimbozoomer.jugcraft.client.Texts.of(text), false, Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
	}

	/** A battery bar of ten cells. */
	static void bars(SubmitNodeCollector collector, PoseStack pose, Font font, float level, float x, float y) {
		int n = Math.round(level * 10);
		StringBuilder on = new StringBuilder(), off = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			(i < n ? on : off).append('■');
		}
		int colour = level < 0.2F ? 0xFFFF5046 : level < 0.5F ? 0xFFFFB83C : 0xFF64E68C;
		submit(collector, pose, font, on.toString(), x, y, colour);
		submit(collector, pose, font, off.toString(), x + font.width(on.toString()), y, 0xFF2A3A30);
	}
}
