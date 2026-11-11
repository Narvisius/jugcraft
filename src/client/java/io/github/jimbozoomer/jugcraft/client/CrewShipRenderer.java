package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the crew drop ship (the drop ship sheet, take 4) from {@link CrewShipMesh}, where its timeline
 * ({@link CrewShipFlight}) puts it this frame: the thrusters swinging, the gear, the side doors, the stern door, the
 * platform and the leaf, each gun turned and elevated as the server aims it. Each corner is lit by the world's light
 * where it is (sampled round the ship every few ticks), so the floodlights' light and the sky's fall on it as on the
 * ground; its lights glow full bright. The thrusters' exhaust, the floodlights' beams and the fire of entry are drawn
 * additively over it.
 * <p>
 * Coming in, with the side doors open (the hover and the touchdown), the crew it brings are seen in their seats (C4,
 * Justin, 9 November 2026: "crew already seated in the ship before it lands"): stand-ins dressed as each of them (the
 * ship tells the client who: {@link CrewShip#passengerLooks}) drawn as the tower's crew are, sitting where the server will
 * seat them for real as the ramp opens ({@link CrewShip#place}).
 */
public class CrewShipRenderer extends EntityRenderer<CrewShip, CrewShipRenderer.State> {
	/** The light grid: its spacing and size (blocks, cells), resampled every this many ticks. */
	static final int CELL = 4, NX = 13, NY = 6, NZ = 13, RESAMPLE = 8;
	/** Past this far from the camera (blocks) the ship is drawn from its coarser far model, and its interior's fittings
	 * only nearer than {@link #INTERIOR} (Justin, 8 November 2026: "Build, with distance cutoff"). */
	static final double FAR = 48, INTERIOR = 40;
	private static final float SWING = CrewShip.GUN_SWING;

	public static final class State extends EntityRenderState {
		int phase;
		float t, yaw, reach, ceiling;
		boolean roof, night;
		final float[] aimYaw = new float[CrewShip.GUNS.length], aimPitch = new float[CrewShip.GUNS.length];
		/** The ship's id; the door guns' barrel clusters' turn (radians); the heavy guns' barrels' kick (0 at rest, 1 just
		 * fired: guns 2 to 5, left and right); the flight deck's door's opening (0 shut, 1 open). */
		int id;
		final float[] spin = new float[2], kick = new float[8];
		float fdoor;
		@Nullable Light light;
		/** The crew seen in their seats coming in: each stand-in as drawn, and where (from the entity, blocks). */
		final java.util.List<Rider> riders = new java.util.ArrayList<>();
		@Nullable TowerCrewRenderer crewRenderer;
	}

	record Rider(TowerCrewRenderer.State state, float x, float y, float z) {
	}

	/** The stand-in the riders are drawn from (one, dressed as each in turn; never in the world). */
	private static io.github.jimbozoomer.jugcraft.tower.crew.@Nullable TowerCrew standIn;

	/** The world's light round the ship: block and sky (0-15) on a grid from {@code ox, oy, oz} (world). */
	record Light(int ox, int oy, int oz, float[] block, float[] sky, long at) {
		/** The packed light at a world point, blended between the grid's points. */
		int at(double x, double y, double z) {
			double fx = Mth.clamp((x - ox) / CELL, 0, NX - 1.001), fy = Mth.clamp((y - oy) / CELL, 0, NY - 1.001), fz = Mth.clamp((z - oz) / CELL, 0, NZ - 1.001);
			int ix = (int) fx, iy = (int) fy, iz = (int) fz;
			double ax = fx - ix, ay = fy - iy, az = fz - iz;
			float b = 0, s = 0;
			for (int c = 0; c < 8; c++) {
				int dx = c & 1, dy = (c >> 1) & 1, dz = c >> 2;
				double w = (dx == 1 ? ax : 1 - ax) * (dy == 1 ? ay : 1 - ay) * (dz == 1 ? az : 1 - az);
				int k = ((ix + dx) * NY + (iy + dy)) * NZ + (iz + dz);
				b += (float) (w * block[k]);
				s += (float) (w * sky[k]);
			}
			return ((int) (s * 16) << 16) | (int) (b * 16);
		}
	}

	public CrewShipRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0;
	}

	/** How far round its middle the ship (with its doors, guns and ramp) reaches, in blocks. */
	static final double SIGHT_REACH = 20;

	/**
	 * It flies far from where the entity stands, so the game's own test (round the entity) won't do: drawn while a box
	 * round where the ship is now could be seen ({@link Sight}; Justin's JFR, 8 November 2026: drawn every frame while he
	 * was inside the tower). The box takes in the floodlight beams' reach below it and the landing zone round it when
	 * they are showing.
	 */
	@Override
	public boolean shouldRender(CrewShip ship, Frustum frustum, double x, double y, double z, float partialTick) {
		ClientLevel level = Minecraft.getInstance().level;
		long now = level == null ? ship.phaseStart() : level.getGameTime();
		CrewShipFlight.Pose p = ship.pose(Math.max(0, now - ship.phaseStart() + partialTick), new CrewShipFlight.Pose());
		Vec3 mid = ship.world(p, 0, 5, -2);
		long day = Math.floorMod(ship.level().getOverworldClockTime(), 24000L);
		boolean beams = p.lights || (day >= 12542 && day < 23460);
		double reach = p.zone ? Math.max(CrewShipFlight.ZONE_A, CrewShipFlight.ZONE_B) + 2
				: beams ? SIGHT_REACH + CrewShip.BEAM_REACH * CrewShip.BEAM_SPREAD : SIGHT_REACH;
		double low = mid.y - SIGHT_REACH;
		if (beams) {
			low = Math.min(low, mid.y - CrewShip.BEAM_REACH - 2);
		}
		if (p.zone || p.entry > 0.01F) {
			low = Math.min(low, ship.getY() - 2);
		}
		return Sight.visible(mid.x - reach, low, mid.z - reach, mid.x + reach, mid.y + SIGHT_REACH, mid.z + reach);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CrewShip ship, State state, float partialTick) {
		super.extractRenderState(ship, state, partialTick);
		ClientLevel level = Minecraft.getInstance().level;
		long now = level == null ? ship.phaseStart() : level.getGameTime();
		state.phase = ship.phase();
		long day = Math.floorMod(ship.level().getOverworldClockTime(), 24000L);
		state.night = day >= 12542 && day < 23460;
		state.t = Math.max(0, now - ship.phaseStart() + partialTick);
		state.yaw = ship.shipYaw();
		state.roof = ship.roof();
		state.reach = ship.reach();
		state.ceiling = ship.ceiling();
		for (int i = 0; i < CrewShip.GUNS.length; i++) {
			state.aimYaw[i] = ship.aim(i).x();
			state.aimPitch[i] = ship.aim(i).y();
		}
		state.id = ship.getId();
		for (int i = 0; i < 2; i++) {
			state.spin[i] = Mth.lerp(partialTick, ship.spinAngleO[i], ship.spinAngle[i]);
		}
		for (int k = 0; k < state.kick.length; k++) {
			float age = ship.kickAge[k] + partialTick;
			state.kick[k] = age < KICK_TICKS ? Mth.square(1 - age / KICK_TICKS) : 0;
		}
		state.fdoor = Mth.lerp(partialTick, ship.fdoorO, ship.fdoor);
		state.light = light(ship, level, now);
		state.riders.clear();
		state.crewRenderer = null;
		// the crew it brings while it comes in; a pickup's squad from when they're aboard till it's away
		if (level != null && (state.phase == CrewShipFlight.HOVER || state.phase == CrewShipFlight.TOUCHDOWN
				|| ship.pickup() && state.phase >= CrewShipFlight.UNLOAD && state.phase <= CrewShipFlight.LIFT)) {
			riders(ship, state, level, partialTick);
		}
	}

	/** The crew it brings, in their seats, while they can be seen through the open side doors and the ship is near. */
	private static void riders(CrewShip ship, State state, ClientLevel level, float partialTick) {
		CrewShipFlight.Pose p = CrewShipFlight.pose(state.phase, state.t, state.yaw, state.roof, state.reach, state.ceiling, new CrewShipFlight.Pose());
		if (p.sides < 0.02F) {
			return;
		}
		Matrix4f m = new Matrix4f().translate((float) p.x, (float) p.y, (float) p.z).rotateY(p.yaw).rotateX(p.pitch);
		Vector3f mid = m.transformPosition(0, 5, -2, new Vector3f());
		Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
		if (cam.distanceToSqr(state.x + mid.x, state.y + mid.y, state.z + mid.z) > INTERIOR * INTERIOR) {
			return;
		}
		java.util.List<io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.@Nullable Look> looks = ship.passengerLooks();
		if (looks.isEmpty()) {
			return;
		}
		if (standIn == null || standIn.level() != level) {
			// (with an id of its own: it is never added to the world, and drawing it asks for one. Without, the game closed
			// as a ship came in with its crew aboard: StandIns)
			standIn = StandIns.id(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD));
			if (standIn == null) {
				return;
			}
			standIn.snapTo(0, level.getMinY() - 1000, 0, 0, 0);   // (far below everyone: nobody it would greet)
		}
		if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(standIn) instanceof TowerCrewRenderer renderer)) {
			return;
		}
		state.crewRenderer = renderer;
		Vector3f at = new Vector3f(), face = new Vector3f();
		for (int i = 0; i < looks.size(); i++) {
			if (looks.get(i) == null) {
				continue;                                  // (a seat still to be filled)
			}
			float[] s = CrewShip.place(i);
			boolean seated = i < CrewShip.SEATS.length;
			// a sitter's origin is under the seat's point by the crew's vehicle attachment (0.6); standing, on the floor
			m.transformPosition(s[0], seated ? s[1] - 0.6F : s[1], s[2], at);
			m.transformDirection(s[3], 0, s[4], face);
			float yaw = (float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90.0F;
			standIn.dressAs(looks.get(i));
			standIn.setYRot(yaw);
			standIn.yRotO = yaw;
			standIn.yBodyRot = standIn.yBodyRotO = yaw;
			standIn.yHeadRot = standIn.yHeadRotO = yaw;
			standIn.setXRot(0);
			standIn.xRotO = 0;
			TowerCrewRenderer.State st = renderer.createRenderState(standIn, partialTick);
			st.isPassenger = seated;
			if (seated) {                                  // (seated, security's rifle isn't in their hands: as the real ones)
				st.act = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_IDLE;
				st.prop = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_NONE;
				st.rifle.clear();
			}
			st.greet = io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.NONE;
			st.nameTag = null;
			if (state.light != null) {
				st.lightCoords = state.light.at(state.x + at.x, state.y + at.y + 1.0, state.z + at.z);
			}
			state.riders.add(new Rider(st, at.x, at.y, at.z));
		}
	}

	/** The light grid round where the ship is now (kept on the ship, resampled every few ticks; high up, the open sky). */
	private static @Nullable Light light(CrewShip ship, @Nullable ClientLevel level, long now) {
		if (level == null) {
			return null;
		}
		Light old = ship.clientLight instanceof Light l ? l : null;
		if (old != null && now - ship.clientLightAt < RESAMPLE && ship.clientLightAt <= now) {
			return old;
		}
		ship.clientLightAt = now;
		CrewShipFlight.Pose p = ship.pose(now - ship.phaseStart(), new CrewShipFlight.Pose());
		double cx = ship.getX() + p.x, cy = ship.getY() + p.y, cz = ship.getZ() + p.z;
		int ox = Mth.floor(cx) - CELL * (NX / 2), oy = Mth.floor(cy) - CELL, oz = Mth.floor(cz) - CELL * (NZ / 2);
		float[] block = new float[NX * NY * NZ], sky = new float[NX * NY * NZ];
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int i = 0; i < NX; i++) {
			for (int j = 0; j < NY; j++) {
				for (int k = 0; k < NZ; k++) {
					m.set(ox + i * CELL, oy + j * CELL, oz + k * CELL);
					int n = (i * NY + j) * NZ + k;
					block[n] = level.getBrightness(LightLayer.BLOCK, m);
					sky[n] = level.getBrightness(LightLayer.SKY, m);
				}
			}
		}
		// (the same light as before kept as it was, so what was worked out from it, and kept on the card, still holds)
		Light l = old != null && old.ox() == ox && old.oy() == oy && old.oz() == oz && java.util.Arrays.equals(old.block(), block)
				&& java.util.Arrays.equals(old.sky(), sky) ? old : new Light(ox, oy, oz, block, sky, now);
		ship.clientLight = l;
		return l;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		CrewShipMesh mesh = CrewShipMesh.get();
		if (mesh == null) {
			return;
		}
		CrewShipFlight.Pose p = CrewShipFlight.pose(state.phase, state.t, state.yaw, state.roof, state.reach, state.ceiling, new CrewShipFlight.Pose());
		Matrix4f ship = new Matrix4f().translate((float) p.x, (float) p.y, (float) p.z).rotateY(p.yaw).rotateX(p.pitch);
		Light light = state.light;
		double ex = state.x, ey = state.y, ez = state.z;
		// how far the ship's middle is from the camera: the far model past FAR, the interior's fittings only near
		Vector3f mid = ship.transformPosition(0, 5, -2, new Vector3f());
		double dist = Math.sqrt(Mth.square(ex + mid.x - camera.pos.x) + Mth.square(ey + mid.y - camera.pos.y) + Mth.square(ez + mid.z - camera.pos.z));
		String far = dist > FAR ? "~far" : "";
		Draw d = new Draw(mesh, pose, collector, light, ex, ey, ez, far, still(state.id));
		Matrix4f m;
		d.part("hull" + far, new Matrix4f(ship));
		if (dist < INTERIOR) {
			d.part("interior", new Matrix4f(ship));
		}
		// the gear: its doors open over the first 0.3 of its way (up into the hull, then aside), then the legs run out
		// (tools/crew_ship.py gear_phase; the parts are drawn down, the doors open)
		float o = Mth.clamp(p.gear / 0.3F, 0, 1), e = Mth.clamp((p.gear - 0.3F) / 0.7F, 0, 1);
		float rise = Math.min(1, o / 0.3F) * GEAR_DOOR_RISE, slide = Mth.clamp((o - 0.3F) / 0.7F, 0, 1) * GEAR_DOOR_SLIDE;
		if (e > 0.001F) {
			for (int k = 1; k <= 5; k++) {
				d.part("gear_s" + k + far, new Matrix4f(ship).translate(0, k * GEAR_RUN * (1 - e), 0));
			}
		}
		d.part("gear_door_a" + far, new Matrix4f(ship).translate(0, rise - GEAR_DOOR_RISE, GEAR_DOOR_SLIDE - slide));
		d.part("gear_door_b" + far, new Matrix4f(ship).translate(0, rise - GEAR_DOOR_RISE, slide - GEAR_DOOR_SLIDE));
		CrewShipMesh.Part leaf = mesh.parts.get("leaf");
		if (leaf != null) {                              // the ramp, turned about its hinge (shut: the slanted back)
			d.part("leaf" + far, new Matrix4f(ship).translate(leaf.px(), leaf.py(), leaf.pz()).rotateX(p.leaf));
		}
		// the flight deck's door slides aside into the bulkhead as a player comes near (CrewShip#fdoor)
		d.part("fdoor" + far, new Matrix4f(ship).translate(CrewShip.DOOR_SLIDE * state.fdoor, 0, 0));
		// the side doors open first, then the door guns' booms run out (CrewShip#door, #boom)
		float door = CrewShip.door(p.sides), boom = CrewShip.boom(p.sides);
		d.part("door_l" + far, new Matrix4f(ship).translate(0, 0, -4.4F * door));
		d.part("door_r" + far, new Matrix4f(ship).translate(0, 0, -4.4F * door));
		d.part("gun_l" + far, new Matrix4f(ship).translate(-SWING * boom, 0, 0));
		d.part("gun_r" + far, new Matrix4f(ship).translate(SWING * boom, 0, 0));
		for (String side : new String[] {"thruster_l", "thruster_r"}) {
			CrewShipMesh.Part t = mesh.parts.get(side);
			if (t != null) {
				m = new Matrix4f(ship).translate(t.px(), t.py(), t.pz()).rotateX(Mth.HALF_PI * p.tilt);
				d.part(side + far, m);
				if (p.thrust > 0.01F) {
					exhaust(new Matrix4f(m), p.thrust, state.ageInTicks + (side.endsWith("r") ? 37 : 0), pose, collector);
				}
			}
		}
		// the guns: each turned and elevated as the server aims it (the door guns stowed as their booms come in), the door
		// guns' barrel clusters turning, the heavy guns' barrels kicking back as they fire
		for (int i = 0; i < CrewShip.GUNS.length; i++) {
			CrewShip.Gun g = CrewShip.GUNS[i];
			String kind = i < 2 ? "door" : g.name().startsWith("wing") ? "wing" : g.name();
			float gx = i < 2 ? CrewShip.gunX(i, p.sides) : g.x();
			Matrix4f at = new Matrix4f(ship).translate(gx, g.y(), g.z());
			String gun = "#" + i;
			d.part(d.turret(kind, "base"), new Matrix4f(at), d.turret(kind, "base") + gun);
			Matrix4f turned = new Matrix4f(at).rotateY(CrewShip.drawnYaw(i, state.aimYaw[i], p.sides));
			d.part(d.turret(kind, "yaw"), turned, d.turret(kind, "yaw") + gun);
			CrewShipMesh.Part elev = mesh.parts.get("turret_" + kind + "_pitch");
			if (elev != null) {
				Matrix4f raised = new Matrix4f(turned).translate(elev.px(), elev.py(), elev.pz())
						.rotateX(CrewShip.drawnPitch(i, state.aimPitch[i], p.sides));
				d.part(d.turret(kind, "pitch"), raised, d.turret(kind, "pitch") + gun);
				if (i < 2) {
					d.part(d.turret(kind, "spin"), new Matrix4f(raised).rotateZ(state.spin[i]), d.turret(kind, "spin") + gun);
				} else {
					for (int r = 0; r < 2; r++) {
						String n = d.turret(kind, r == 0 ? "recoil_l" : "recoil_r");
						d.part(n, new Matrix4f(raised).translate(0, 0, state.kick[(i - 2) * 2 + r] * KICK * g.scale()), n + gun);
					}
				}
			}
		}
		// the floodlights: from the hover on, and all the way in and out at night (Justin, 6 November 2026: "at nighttime,
		// it needs to have the lights on already")
		if (p.lights || state.night) {
			beams(mesh, ship, p, pose, collector);
		}
		if (p.zone) {
			zone(ship, p, state.ageInTicks, pose, collector);
		}
		if (p.entry > 0.01F) {
			entry(mesh, ship, p.entry, pose, collector);
		}
		TowerCrewRenderer crew = state.crewRenderer;
		if (crew != null) {
			for (Rider r : state.riders) {
				pose.pushPose();
				pose.translate(r.x(), r.y(), r.z());
				crew.submit(r.state(), pose, collector, camera);
				pose.popPose();
			}
		}
		super.submit(state, pose, collector, camera);
	}

	/** Frames a part must have stood where it is before it's kept on the graphics card ({@link Retained}). */
	static final int STILL_FRAMES = 3;
	/** The gear's door's rise into the hull and slide (open), a stage's run (tools/crew_ship.py GEAR_*); a heavy gun's
	 * barrel's kick back (blocks, at the gun's size 1) and how many ticks it takes to run back out. */
	static final float GEAR_DOOR_RISE = 0.06F, GEAR_DOOR_SLIDE = 0.95F, GEAR_RUN = (CrewShipFlight.G + 0.035F) / 5, KICK = 0.22F;
	static final int KICK_TICKS = 6;

	/** Where each part of each ship was drawn last, and for how many frames it has stood there (by the ship's id). */
	private static final java.util.Map<Integer, java.util.Map<String, Still>> STILL = new java.util.HashMap<>();
	private static long draws;

	private static final class Still {
		final Matrix4f m = new Matrix4f();
		double x, y, z;
		int frames = -1;
		long seen;
	}

	private static java.util.Map<String, Still> still(int id) {
		draws++;
		if (draws % 600 == 0) {
			STILL.values().removeIf(map -> map.values().stream().allMatch(st -> draws - st.seen > 600));
		}
		return STILL.computeIfAbsent(id, k -> new java.util.HashMap<>());
	}

	/** A shape kept on the card: a part's batch as lit by {@code light} where it stood (to a 256th of a block). */
	private record Kept(CrewShipMesh.Batch batch, @Nullable Light light, long x, long y, long z) {
	}

	/** One ship's drawing this frame. */
	private record Draw(CrewShipMesh mesh, PoseStack pose, SubmitNodeCollector collector, @Nullable Light light, double ex, double ey, double ez,
			String far, java.util.Map<String, Still> still) {
		/** A gun's part's name, its far model when far and it has one. */
		String turret(String kind, String move) {
			String n = "turret_" + kind + "_" + move;
			return !far.isEmpty() && mesh.parts.containsKey(n + far) ? n + far : n;
		}

		/**
		 * Draws a part with its transform {@code m} (from the entity), each corner lit by the world's light where it is
		 * ("optimize what you can", Justin, 9 November 2026): a part that has stood where it is a few frames (the ship
		 * down) is kept on the graphics card, built once and only placed each frame ({@link Retained}; not its see-through
		 * glass, drawn after); otherwise its corners are written, those of faces turned away from the camera skipped (the
		 * game would cull them anyway), each corner's light kept from the last time while nothing has moved.
		 */
		void part(String name, Matrix4f m) {
			part(name, m, name);
		}

		/** As {@link #part(String, Matrix4f)}, for one of several drawn from the same part ({@code key}: which, for
		 * whether it has stood still: each gun's). */
		void part(String name, Matrix4f m, String key) {
			CrewShipMesh.Part part = mesh.parts.get(name);
			if (part == null) {
				return;
			}
			Still st = still.computeIfAbsent(key, k -> new Still());
			if (st.m.equals(m) && st.x == ex && st.y == ey && st.z == ez) {
				st.frames++;
			} else {
				st.m.set(m);
				st.x = ex;
				st.y = ey;
				st.z = ez;
				st.frames = 0;
			}
			st.seen = draws;
			boolean keep = st.frames >= STILL_FRAMES && Retained.usable();
			if (keep) {
				Retained.mark(collector);
			}
			long kx = Math.round((ex + m.m30()) * 256), ky = Math.round((ey + m.m31()) * 256), kz = Math.round((ez + m.m32()) * 256);
			@Nullable Light lt = light;
			double x = ex, y = ey, z = ez;
			pose.pushPose();
			pose.mulPose(m);
			for (CrewShipMesh.Batch b : part.batches()) {
				RenderType type = mesh.types[b.material()];
				boolean kept = keep && !b.translucent;
				collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
					if (kept && Retained.queue(type, new Kept(b, lt, kx, ky, kz), 0, matrix.pose(),
							(p, out) -> corners(b, p.pose(), p.normal(), m, lt, x, y, z, out, false))) {
						return;
					}
					corners(b, matrix.pose(), matrix.normal(), m, lt, x, y, z, buffer, true);
				});
			}
			pose.popPose();
		}
	}

	/** A batch's corners into {@code buffer}, placed by {@code place} (turning normals by {@code turn}); those of faces
	 * turned from the camera left out when {@code cull} (the camera at the place's origin). */
	private static void corners(CrewShipMesh.Batch b, Matrix4f place, org.joml.Matrix3f turn, Matrix4f m, @Nullable Light light,
			double ex, double ey, double ez, VertexConsumer buffer, boolean cull) {
		float[] pos = b.pos(), uv = b.uv(), n = b.normal(), face = b.face;
		int[] lit = lights(b, m, light, ex, ey, ez);
		float cx = 0, cy = 0, cz = 0;
		if (cull) {
			Vector3f cam = new Matrix4f(place).invertAffine().transformPosition(0, 0, 0, new Vector3f());
			cx = cam.x;
			cy = cam.y;
			cz = cam.z;
		}
		Vector3f at = new Vector3f(), nn = new Vector3f();
		for (int q = 0, nq = b.quads(); q < nq; q++) {
			int f = q * 6;
			// (a quad whose facing isn't known is NaN here: never skipped)
			if (cull && face[f] * (cx - face[f + 3]) + face[f + 1] * (cy - face[f + 4]) + face[f + 2] * (cz - face[f + 5]) < -1e-4F) {
				continue;
			}
			for (int c = 0; c < 4; c++) {
				int v = q * 4 + c;
				turn.transform(n[v * 3], n[v * 3 + 1], n[v * 3 + 2], nn).normalize();
				place.transformPosition(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2], at);
				buffer.addVertex(at.x, at.y, at.z, 0xFFFFFFFF, uv[v * 2], uv[v * 2 + 1], OverlayTexture.NO_OVERLAY, lit[v], nn.x, nn.y, nn.z);
			}
		}
	}

	/** Light worked out for a batch's corners (glowing: full bright; with no light grid: full sky), kept on the batch
	 * while the part's place and the light grid stay the same. */
	private static int[] lights(CrewShipMesh.Batch b, Matrix4f m, @Nullable Light light, double ex, double ey, double ez) {
		Object key = light == null ? NO_LIGHT : light;
		if (b.lit != null && b.litFor == key && b.litX == ex && b.litY == ey && b.litZ == ez && b.litM.equals(m)) {
			return b.lit;
		}
		float[] pos = b.pos(), n = b.normal();
		int nv = pos.length / 3;
		int[] lit = b.lit != null && b.lit.length == nv ? b.lit : new int[nv];
		Vector3f w = new Vector3f(), wn = new Vector3f();
		for (int v = 0; v < nv; v++) {
			if (b.glow()) {
				lit[v] = LightCoordsUtil.FULL_BRIGHT;
			} else if (light == null) {
				lit[v] = LightCoordsUtil.FULL_SKY;
			} else {
				m.transformDirection(n[v * 3], n[v * 3 + 1], n[v * 3 + 2], wn);
				m.transformPosition(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2], w);
				lit[v] = light.at(ex + w.x + wn.x * 0.5, ey + w.y + wn.y * 0.5, ez + w.z + wn.z * 0.5);
			}
		}
		b.lit = lit;
		b.litFor = key;
		b.litM.set(m);
		b.litX = ex;
		b.litY = ey;
		b.litZ = ez;
		return lit;
	}

	private static final Object NO_LIGHT = new Object();

	// ---------------------------------------------------------------- the keep-out ring

	static final net.minecraft.resources.Identifier RING = io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/entity/crew_ship/zone_ring.png");
	/** The ring's band: from 1 to 2 blocks over the ground (the second block, at eye level); how far along it one length of
	 * its texture goes; how fast it turns (blocks a tick). */
	static final float RING_LOW = 1.0F, RING_HIGH = 2.0F, RING_TEXTURE = 16.0F, RING_SPIN = 0.06F;

	/**
	 * The keep-out ring (Justin: "that circle thing that goes around spinning with the letters on it ... hologrammy ... stay
	 * back ... black letters, red circle around the ship"; then "float at like a second block level ... so people can see it
	 * ... instead of them just not paying attention on the ground"): a band standing round the ship 10 blocks out
	 * ({@link CrewShipFlight#ZONE_A}), floating from 1 to 2 blocks over the ground, translucent glowing red with STAY BACK
	 * along it, readable from outside and from inside, turning round the ship and flickering a little.
	 */
	private static void zone(Matrix4f ship, CrewShipFlight.Pose p, float age, PoseStack pose, SubmitNodeCollector collector) {
		pose.pushPose();
		pose.mulPose(new Matrix4f(ship).translate(0, (float) -p.y, 0));
		int n = 120;
		float[][] at = new float[n + 1][];
		float[] s = new float[n + 1];
		for (int i = 0; i <= n; i++) {
			double a = 2 * Math.PI * i / n;
			at[i] = new float[] {CrewShipFlight.ZONE_A * (float) Math.cos(a), CrewShipFlight.ZONE_Z + CrewShipFlight.ZONE_B * (float) Math.sin(a)};
			if (i > 0) {
				s[i] = s[i - 1] + (float) Math.hypot(at[i][0] - at[i - 1][0], at[i][1] - at[i - 1][1]);
			}
		}
		// a whole number of texture lengths round it, so the words meet where it closes
		float per = s[n] / Math.max(1, Math.round(s[n] / RING_TEXTURE));
		float spin = age * RING_SPIN / per;
		float flicker = 0.9F + 0.1F * Mth.sin(age * 0.9F) * Mth.sin(age * 0.37F);
		int colour = ((int) (255 * flicker) << 24) | 0xFFFFFF;
		collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentCull(RING), (matrix, buffer) -> {
			for (int i = 0; i < n; i++) {
				float[] a = at[i], b = at[i + 1];
				float nx = b[1] - a[1], nz = -(b[0] - a[0]);
				float len = (float) Math.hypot(nx, nz);
				nx /= len;
				nz /= len;
				// outside: the words run against the ring's angle (left to right seen from outside), facing out
				float u0 = -s[i] / per + spin, u1 = -s[i + 1] / per + spin;
				band(buffer, matrix, new float[][] {{a[0], RING_LOW, a[1]}, {a[0], RING_HIGH, a[1]}, {b[0], RING_HIGH, b[1]}, {b[0], RING_LOW, b[1]}},
						new float[][] {{u0, 1}, {u0, 0}, {u1, 0}, {u1, 1}}, colour, nx, nz);
				// inside: the other way round, facing in
				float w0 = s[i] / per - spin, w1 = s[i + 1] / per - spin;
				band(buffer, matrix, new float[][] {{a[0], RING_LOW, a[1]}, {b[0], RING_LOW, b[1]}, {b[0], RING_HIGH, b[1]}, {a[0], RING_HIGH, a[1]}},
						new float[][] {{w0, 1}, {w1, 1}, {w1, 0}, {w0, 0}}, colour, -nx, -nz);
			}
		});
		pose.popPose();
	}

	private static void band(VertexConsumer buffer, PoseStack.Pose matrix, float[][] q, float[][] uv, int colour, float nx, float nz) {
		for (int k = 0; k < 4; k++) {
			buffer.addVertex(matrix, q[k][0], q[k][1], q[k][2]).setColor(colour).setUv(uv[k][0], uv[k][1]).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(matrix, nx, 0, nz);
		}
	}

	// ---------------------------------------------------------------- the glows

	/** A ring of a chamfered square (the thrusters' section) of size r at axial z. */
	static float[][] ring(float r, float z) {
		float[][] c = {{1, 0.72F}, {0.72F, 1}, {-0.72F, 1}, {-1, 0.72F}, {-1, -0.72F}, {-0.72F, -1}, {0.72F, -1}, {1, -0.72F}};
		float[][] out = new float[8][];
		for (int i = 0; i < 8; i++) {
			out[i] = new float[] {c[i][0] * r, c[i][1] * r, z};
		}
		return out;
	}

	static final net.minecraft.resources.Identifier FLAME = io.github.jimbozoomer.jugcraft.Jugcraft.id("textures/entity/crew_ship/flame.png");
	/**
	 * The flame's layers, outside in (the rocket plume Justin picked on 6 November 2026, option B of the thruster sheet):
	 * radius at the nozzle and at its end, length (of the whole flame), colour at the nozzle and at the end, opacity at
	 * the nozzle and at the end, how fast the fire runs down it (texture lengths a tick) and how long one is (blocks).
	 */
	static final float[][] FLAME_LAYERS = {
			{1.15F, 2.00F, 1.00F, 1.0F, 0.55F, 0.15F, 0.70F, 0.18F, 0.05F, 0.45F, 0.0F, 0.09F, 4.5F},   // the outer plume, orange to deep red
			{1.00F, 1.25F, 0.80F, 1.0F, 0.85F, 0.45F, 1.00F, 0.35F, 0.08F, 0.85F, 0.0F, 0.14F, 3.2F},   // the flame, yellow to orange
			{0.95F, 0.50F, 0.30F, 1.0F, 1.00F, 0.95F, 1.00F, 0.92F, 0.70F, 1.00F, 0.5F, 0.22F, 1.6F}};  // the white-hot core

	/**
	 * A thruster's exhaust (Justin, 6 November 2026: "It needs to be, like actual thrusters"; then the rocket plume he
	 * picked from the thruster sheet): white-hot at the nozzle, yellow, then orange and deep red down a ragged flame that
	 * widens as it goes, the fire streaking down it and the whole flame flickering in length; longer the harder it burns
	 * (the smoke rolling off its end and along the ground: CrewShipEffects). Drawn translucent and full bright, both
	 * sides of each face.
	 */
	private static void exhaust(Matrix4f m, float thrust, float age, PoseStack pose, SubmitNodeCollector collector) {
		float nozzle = 4.0F;
		float flicker = 1 + 0.07F * Mth.sin(age * 1.9F) + 0.05F * Mth.sin(age * 3.7F + 1.3F) + 0.03F * Mth.sin(age * 7.1F);
		// idling (on the ground, at the roof) only a short glow and the heat round the lip; the fire grows as it burns harder
		float burn = Mth.clamp((thrust - 0.3F) / 0.45F, 0, 1);
		float len = (1.2F + 8.1F * thrust * thrust) * flicker;
		pose.pushPose();
		pose.mulPose(m);
		collector.submitCustomGeometry(pose, RenderTypes.eyes(FLAME), (matrix, buffer) -> {
			int segs = 5;
			for (float[] L : FLAME_LAYERS) {
				float l = len * L[2];
				float scroll = age * L[11];
				for (int s2 = 0; s2 < segs; s2++) {
					float t0 = (float) s2 / segs, t1 = (float) (s2 + 1) / segs;
					float r0 = Mth.lerp((float) Math.pow(t0, 0.7), L[0], L[1]), r1 = Mth.lerp((float) Math.pow(t1, 0.7), L[0], L[1]);
					float[][] a = ring(r0, nozzle + l * t0), b = ring(r1, nozzle + l * t1);
					float k = 0.15F + 0.85F * burn;
					int c0 = colour(Mth.lerp(t0, L[3], L[6]), Mth.lerp(t0, L[4], L[7]), Mth.lerp(t0, L[5], L[8]), Mth.lerp(t0, L[9], L[10]) * k);
					int c1 = colour(Mth.lerp(t1, L[3], L[6]), Mth.lerp(t1, L[4], L[7]), Mth.lerp(t1, L[5], L[8]), Mth.lerp(t1, L[9], L[10]) * k);
					float v0 = l * t0 / L[12] - scroll, v1 = l * t1 / L[12] - scroll;
					for (int i = 0; i < 8; i++) {
						int j = (i + 1) % 8;
						flame(buffer, matrix, a[i], a[j], b[j], b[i], i / 8F, (i + 1) / 8F, v0, v1, c0, c1);
					}
				}
			}
		});
		pose.popPose();
	}

	/** A textured, translucent quad seen from both sides: corners a, b at the near end (colour c0, texture v0), c, d at the far. */
	static void flame(VertexConsumer buffer, PoseStack.Pose matrix, float[] a, float[] b, float[] c, float[] d, float u0, float u1,
			float v0, float v1, int c0, int c1) {
		float[][] corners = {a, b, c, d};
		float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
		int[] cols = {c0, c0, c1, c1};
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < 4; i++) {
				int k = pass == 0 ? i : 3 - i;
				float[] p = corners[k];
				buffer.addVertex(matrix, p[0], p[1], p[2]).setColor(cols[k]).setUv(uv[k][0], uv[k][1]).setOverlay(OverlayTexture.NO_OVERLAY)
						.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(matrix, 0, 1, 0);
			}
		}
	}

	/**
	 * The floodlights' beams: a soft cone of light from each lamp that shines down or out, to the ground or as far as a
	 * beam reaches (CrewShip.BEAM_REACH), a brighter core inside a wider glow, fading along it (pale on dark: they were
	 * drawn black until 6 November 2026, Justin: "the spotlights, you just have black lights here").
	 */
	private static void beams(CrewShipMesh mesh, Matrix4f ship, CrewShipFlight.Pose p, PoseStack pose, SubmitNodeCollector collector) {
		pose.pushPose();
		pose.mulPose(ship);
		float drop = (float) p.y;
		collector.submitCustomGeometry(pose, RenderTypes.eyes(mesh.white), (matrix, buffer) -> {
			for (float[] l : CrewShip.LAMPS) {
				Vector3f d = new Vector3f(l[3], l[4], l[5]).normalize();
				if (d.y > -0.15F) {
					continue;
				}
				float t = Math.min(CrewShip.BEAM_REACH, (l[1] + drop) / -d.y);
				Vector3f a = Math.abs(d.y) > 0.9F ? new Vector3f(d).cross(0, 0, 1).normalize() : new Vector3f(d).cross(0, 1, 0).normalize();
				Vector3f b = new Vector3f(d).cross(a).normalize();
				for (int layer = 0; layer < 2; layer++) {
					float r0 = layer == 0 ? 0.55F : 0.32F, r1 = r0 + t * (layer == 0 ? CrewShip.BEAM_SPREAD : CrewShip.BEAM_SPREAD * 0.45F);
					int c0 = colour(1.0F, 0.96F, 0.86F, layer == 0 ? 0.08F : 0.11F), c1 = colour(1.0F, 0.96F, 0.86F, 0.0F);
					for (int k = 0; k < 12; k++) {
						double t0 = 2 * Math.PI * k / 12, t1 = 2 * Math.PI * (k + 1) / 12;
						float[] p0 = point(l, a, b, d, t0, r0, 0), p1 = point(l, a, b, d, t1, r0, 0), p2 = point(l, a, b, d, t1, r1, t),
								p3 = point(l, a, b, d, t0, r1, t);
						cone(buffer, matrix, p0, p1, p2, p3, c0, c1);
					}
				}
			}
		});
		pose.popPose();
	}

	private static float[] point(float[] l, Vector3f a, Vector3f b, Vector3f d, double th, float r, float t) {
		float ca = (float) Math.cos(th), sa = (float) Math.sin(th);
		return new float[] {l[0] + (a.x * ca + b.x * sa) * r + d.x * t, l[1] + (a.y * ca + b.y * sa) * r + d.y * t, l[2] + (a.z * ca + b.z * sa) * r + d.z * t};
	}

	/** Coming into the air: a white-hot shock ahead of the nose, a sheath of fire round the hull, a trail behind. */
	private static void entry(CrewShipMesh mesh, Matrix4f ship, float k, PoseStack pose, SubmitNodeCollector collector) {
		float[][] layers = {{-27, 0.5F, -18, 9, 1.0F, 0.95F, 0.82F, 1.1F}, {-26, 1, -4, 13, 1.0F, 0.62F, 0.26F, 0.62F},
				{-25, 1.5F, 20, 16.5F, 1.0F, 0.44F, 0.14F, 0.42F}, {16, 13, 150, 24, 1.0F, 0.48F, 0.16F, 0.34F}, {140, 24, 360, 34, 0.75F, 0.5F, 0.42F, 0.14F}};
		pose.pushPose();
		pose.mulPose(ship);
		collector.submitCustomGeometry(pose, RenderTypes.eyes(mesh.white), (matrix, buffer) -> {
			for (float[] L : layers) {
				int c0 = colour(L[4], L[5], L[6], L[7] * k), c1 = colour(L[4], L[5], L[6], L[7] * k * 0.35F);
				for (int j = 0; j < 24; j++) {
					double a0 = 2 * Math.PI * j / 24, a1 = 2 * Math.PI * (j + 1) / 24;
					float[] p0 = sheath(a0, L[1], L[0]), p1 = sheath(a1, L[1], L[0]), p2 = sheath(a1, L[3], L[2]), p3 = sheath(a0, L[3], L[2]);
					cone(buffer, matrix, p0, p1, p2, p3, c0, c1);
				}
			}
		});
		pose.popPose();
	}

	private static float[] sheath(double a, float r, float z) {
		return new float[] {(float) Math.cos(a) * r, CrewShipFlight.G + 4 + ((float) Math.sin(a) * 0.62F + 0.15F) * r, z};
	}

	/** A colour at an opacity: the glows are drawn translucent over what's behind (eyes), not added to it. */
	static int colour(float r, float g, float b, float k) {
		k = Mth.clamp(k, 0, 1);
		return ((int) (k * 255) << 24) | ((int) (Mth.clamp(r, 0, 1) * 255) << 16) | ((int) (Mth.clamp(g, 0, 1) * 255) << 8) | (int) (Mth.clamp(b, 0, 1) * 255);
	}

	/** A translucent glowing quad seen from both sides: the first two corners in colour c0, the far two in c1. */
	static void cone(VertexConsumer buffer, PoseStack.Pose matrix, float[] a, float[] b, float[] c, float[] d, int c0, int c1) {
		float[][] corners = {a, b, c, d};
		int[] cols = {c0, c0, c1, c1};
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < 4; i++) {
				int k = pass == 0 ? i : 3 - i;
				float[] p = corners[k];
				buffer.addVertex(matrix, p[0], p[1], p[2]).setColor(cols[k]).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
						.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(matrix, 0, 1, 0);
			}
		}
	}
}
