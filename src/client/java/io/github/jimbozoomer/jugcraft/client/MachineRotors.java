package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * The turning parts of multi-block machines: the giant sawmill's blade and belt drive, the giant sieve's eccentric
 * weights and the industrial forms' couplings and fans, from assets/jugcraft/machine_rotor_quads.json (written by
 * tools/machine_rotors.py). The machines' block entity renderers ({@link WindTurbineRenderer}, {@link FormMachineRenderer})
 * draw them and their block models leave them out, so they stand still while the machine is idle and turn while its
 * master block is lit.
 *
 * <p>Each machine's angles are kept here on the client, per block entity, and every speed change is eased over a few
 * ticks: the parts spin up when the machine starts, run down when it stops and never jump, even when weak power makes
 * the machine stop and start every few ticks.
 */
public final class MachineRotors {
	private static final Identifier FILE = Jugcraft.id("machine_rotor_quads.json");
	/** Longer than this (ticks) since a machine was last drawn, its rotors start again at their current target speed. */
	private static final double MAX_GAP = 40;
	private static @Nullable Map<String, List<Rotor>> rotors;
	/** Angles and speeds of the machines drawn lately; entries go when their block entities are unloaded. */
	private static final Map<BlockEntity, Phase> PHASES = new WeakHashMap<>();

	private MachineRotors() {
	}

	/**
	 * One turning part: its axis, the point it turns about (blocks, north-facing structure space), the block state property
	 * that sets it running, its full speed (degrees a tick), the ticks it takes to reach it or stop, whether it is drawn
	 * standing still while idle, the block state values it needs (a compact copy shows no giant's rotor), and its quads.
	 */
	record Rotor(char axis, float[] center, String property, float speed, float ease, boolean always,
			Map<String, String> when, QuadModel quads) {
	}

	/** A rotor to draw this frame: the machine's facing (degrees about y, as its block models turn) and the angle. */
	public record Spin(Rotor rotor, float yRot, float angle) {
	}

	private static final class Phase {
		double time = Double.NaN;
		final float[] angle;
		final float[] speed;

		Phase(int rotors) {
			angle = new float[rotors];
			speed = new float[rotors];
		}
	}

	/** The rotors of this machine to draw now, turned to their eased angles; empty for machines without any. */
	public static List<Spin> extract(BlockEntity machine, float partialTick) {
		Level level = machine.getLevel();
		if (level == null) {
			return List.of();
		}
		BlockState state = machine.getBlockState();
		List<Rotor> list = rotors().get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
		if (list == null || list.isEmpty() || !state.hasProperty(MachineBlock.FACING)) {
			return List.of();
		}
		double now = level.getGameTime() + (double) partialTick;
		Phase phase = PHASES.computeIfAbsent(machine, m -> new Phase(list.size()));
		double dt = now - phase.time;
		boolean fresh = Double.isNaN(dt) || dt < 0 || dt > MAX_GAP;
		phase.time = now;
		float yRot = yRotation(state.getValue(MachineBlock.FACING));
		List<Spin> out = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			Rotor rotor = list.get(i);
			String running = value(state, rotor.property());
			if (running == null || !matches(state, rotor.when())) {
				continue;
			}
			float target = "true".equals(running) ? rotor.speed() : 0;
			float speed = phase.speed[i];
			if (fresh) {
				speed = target;
			} else {
				// Ease towards the target speed, then turn by the mean speed over the frame.
				float step = (float) (Math.abs(rotor.speed()) * dt / Math.max(1, rotor.ease()));
				float next = speed + Math.max(-step, Math.min(step, target - speed));
				phase.angle[i] = (float) ((phase.angle[i] + (speed + next) / 2 * dt) % 360);
				speed = next;
			}
			phase.speed[i] = speed;
			if (rotor.always() || speed != 0) {
				out.add(new Spin(rotor, yRot, phase.angle[i]));
			}
		}
		return out;
	}

	/** Draws the rotors in the master block's pose (the block entity renderer's). */
	public static void submit(List<Spin> spins, PoseStack pose, SubmitNodeCollector collector, int light) {
		for (Spin spin : spins) {
			Rotor rotor = spin.rotor();
			pose.pushPose();
			// The machine's facing, as its block models are turned: about the master block's centre.
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -spin.yRot());
			pose.translate(-0.5F, 0.0F, -0.5F);
			// The turn about the rotor's own axis.
			float[] c = rotor.center();
			pose.translate(c[0], c[1], c[2]);
			pose.rotateDegrees(switch (rotor.axis()) {
				case 'x' -> Axis.XP;
				case 'y' -> Axis.YP;
				default -> Axis.ZP;
			}, spin.angle());
			pose.translate(-c[0], -c[1], -c[2]);
			rotor.quads().submit(pose, collector, light);
			pose.popPose();
		}
	}

	/** Degrees the blockstates turn a north-facing model about y (clockwise seen from above). */
	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	private static boolean matches(BlockState state, Map<String, String> when) {
		for (Map.Entry<String, String> entry : when.entrySet()) {
			if (!entry.getValue().equals(value(state, entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	/** The serialized value of the block state property with this name, or null if the block has none. */
	private static @Nullable String value(BlockState state, String name) {
		for (Property<?> property : state.getProperties()) {
			if (property.getName().equals(name)) {
				return valueName(state, property);
			}
		}
		return null;
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}

	private static Map<String, List<Rotor>> rotors() {
		if (rotors == null) {
			rotors = load();
		}
		return rotors;
	}

	private static Map<String, List<Rotor>> load() {
		Map<String, List<Rotor>> out = new HashMap<>();
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
		if (resource.isEmpty()) {
			Jugcraft.LOGGER.warn("Missing {}: machine rotors will not be drawn", FILE);
			return out;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				JsonObject data = entry.getValue().getAsJsonObject();
				JsonArray c = data.getAsJsonArray("center");
				float[] center = {c.get(0).getAsFloat() / 16, c.get(1).getAsFloat() / 16, c.get(2).getAsFloat() / 16};
				Map<String, String> when = new HashMap<>();
				if (data.has("when")) {
					for (Map.Entry<String, JsonElement> condition : data.getAsJsonObject("when").entrySet()) {
						when.put(condition.getKey(), condition.getValue().getAsString());
					}
				}
				Rotor rotor = new Rotor(data.get("axis").getAsString().charAt(0), center, data.get("property").getAsString(),
						data.get("speed").getAsFloat(), data.has("ease") ? data.get("ease").getAsFloat() : 10,
						data.has("always") && data.get("always").getAsBoolean(), when, QuadModel.parse(data.getAsJsonArray("quads")));
				out.computeIfAbsent(data.get("block").getAsString(), block -> new ArrayList<>()).add(rotor);
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read {}", FILE, e);
		}
		return out;
	}
}
