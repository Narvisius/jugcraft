package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * The spinning parts of kinetic blocks, from assets/jugcraft/kinetic_rotors.json (written by
 * tools/kinetic_rotors.py from the same boxes as the block models). While a block turns, its block
 * model leaves the rotor out and this draws it, turned like the block model and spun about its axis.
 */
public final class KineticRotors {
	private static final Identifier FILE = Jugcraft.id("kinetic_rotors.json");
	private static @Nullable Map<String, Rotor> rotors;

	private KineticRotors() {
	}

	record Quad(float[] normal, float[][] vertices) {
	}

	record Rotor(char axis, float[] center, String property, float speed, @Nullable String variantProperty,
			Map<String, float[]> variants, Map<RenderType, List<Quad>> quads) {
	}

	/** A rotor to draw this frame: its block-state rotation (degrees about x, then y) and its spin angle. */
	public record Spin(Rotor rotor, float xRot, float yRot, float angle) {
	}

	/** The rotor of this block entity's block while it spins, or null (the block model then shows it still). */
	public static @Nullable Spin extract(BlockEntity entity, float partialTick) {
		BlockState state = entity.getBlockState();
		Rotor rotor = rotors().get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
		if (rotor == null || !"true".equals(value(state, rotor.property())) || entity.getLevel() == null) {
			return null;
		}
		float[] turn = rotor.variantProperty() == null ? null : rotor.variants().get(value(state, rotor.variantProperty()));
		long time = entity.getLevel().getGameTime();
		float angle = ((time % 7200) + partialTick) * rotor.speed() % 360;
		return new Spin(rotor, turn == null ? 0 : turn[0], turn == null ? 0 : turn[1], angle);
	}

	public static void submit(@Nullable Spin spin, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (spin == null) {
			return;
		}
		Rotor rotor = spin.rotor();
		pose.pushPose();
		// The block state's rotation, as vanilla applies it to the block model (x first, then y, about the center).
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.rotateDegrees(Axis.YP, -spin.yRot());
		pose.rotateDegrees(Axis.XP, -spin.xRot());
		pose.translate(-0.5F, -0.5F, -0.5F);
		// The spin about the rotor axis.
		float[] c = rotor.center();
		pose.translate(c[0], c[1], c[2]);
		pose.rotateDegrees(switch (rotor.axis()) {
			case 'x' -> Axis.XP;
			case 'y' -> Axis.YP;
			default -> Axis.ZP;
		}, spin.angle());
		pose.translate(-c[0], -c[1], -c[2]);
		for (Map.Entry<RenderType, List<Quad>> entry : rotor.quads().entrySet()) {
			List<Quad> quads = entry.getValue();
			collector.submitCustomGeometry(pose, entry.getKey(), (matrix, buffer) -> {
				for (Quad quad : quads) {
					for (float[] v : quad.vertices()) {
						buffer.addVertex(matrix, v[0], v[1], v[2]).setColor(0xFFFFFFFF).setUv(v[3], v[4])
								.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
								.setNormal(matrix, quad.normal()[0], quad.normal()[1], quad.normal()[2]);
					}
				}
			});
		}
		pose.popPose();
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

	private static Map<String, Rotor> rotors() {
		if (rotors == null) {
			rotors = load();
		}
		return rotors;
	}

	private static Map<String, Rotor> load() {
		Map<String, Rotor> out = new HashMap<>();
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
		if (resource.isEmpty()) {
			Jugcraft.LOGGER.warn("Missing {}: kinetic blocks will not spin", FILE);
			return out;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				JsonObject data = entry.getValue().getAsJsonObject();
				char axis = data.get("axis").getAsString().charAt(0);
				JsonArray centerUv = data.getAsJsonArray("center");
				// The center is given on the two axes other than the rotor axis, in pixels.
				float[] center = {0.5F, 0.5F, 0.5F};
				int other = 0;
				for (int i = 0; i < 3; i++) {
					if (i != axis - 'x') {
						center[i] = centerUv.get(other++).getAsFloat() / 16;
					}
				}
				Map<String, float[]> variants = new HashMap<>();
				for (Map.Entry<String, JsonElement> variant : data.getAsJsonObject("variants").entrySet()) {
					JsonArray turn = variant.getValue().getAsJsonArray();
					variants.put(variant.getKey(), new float[] {turn.get(0).getAsFloat(), turn.get(1).getAsFloat()});
				}
				Map<RenderType, List<Quad>> quads = new LinkedHashMap<>();
				for (JsonElement element : data.getAsJsonArray("quads")) {
					JsonObject quad = element.getAsJsonObject();
					RenderType type = RenderTypes.entitySolid(Jugcraft.id("textures/block/" + quad.get("texture").getAsString() + ".png"));
					JsonArray n = quad.getAsJsonArray("normal");
					float[][] vertices = new float[4][];
					JsonArray list = quad.getAsJsonArray("vertices");
					for (int i = 0; i < 4; i++) {
						JsonArray v = list.get(i).getAsJsonArray();
						vertices[i] = new float[] {v.get(0).getAsFloat() / 16, v.get(1).getAsFloat() / 16, v.get(2).getAsFloat() / 16,
								v.get(3).getAsFloat(), v.get(4).getAsFloat()};
					}
					quads.computeIfAbsent(type, t -> new ArrayList<>())
							.add(new Quad(new float[] {n.get(0).getAsFloat(), n.get(1).getAsFloat(), n.get(2).getAsFloat()}, vertices));
				}
				JsonElement variantProperty = data.get("variant_property");
				out.put(entry.getKey(), new Rotor(axis, center, data.get("property").getAsString(), data.get("speed").getAsFloat(),
						variantProperty == null || variantProperty.isJsonNull() ? null : variantProperty.getAsString(), variants, quads));
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read {}", FILE, e);
		}
		return out;
	}
}
