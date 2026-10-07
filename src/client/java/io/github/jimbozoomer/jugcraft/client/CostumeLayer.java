package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Draws a worn outfit of decorations batch 14 (a Vampire Cape, Mummy Wraps, a Skeleton Suit, a Werewolf Mask, Cat Ears
 * and Tail, Bat Wings) over the whole wearer, from the boxes in assets/jugcraft/costumes.json (written by
 * tools/decor14_data.py, which also paints each outfit's texture on the same boxes). Outfits are worn on the head, like
 * the Ghost Sheet ({@link GhostSheetLayer}), and have equipment assets without layers, so nothing else draws them.
 *
 * <p>Each piece hangs from one of the wearer's body parts, so it turns, bends and swings as that part does, and may move
 * through joints that turn by a motion ({@link #angle}): the cape flares out behind as its wearer walks and wraps round
 * them while they sneak; tails sway and curl; bat wings fold behind while their wearer stands on something and spread
 * and flap while they are off the ground. A glowing outfit (the Skeleton Suit) is drawn again with its glow texture at
 * full brightness, like a spider's eyes. Everything is worked out from the render state and the block under the wearer,
 * each frame on each client; nothing is sent or saved.
 */
public class CostumeLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private static final Identifier FILE = Jugcraft.id("costumes.json");
	private static final int FULL_BRIGHT = 0xF000F0;
	private static @Nullable Map<Item, Outfit> outfits;

	/** A box in a body part's (or joint's) space, in pixels, with where its faces sit on the texture and their sizes there. */
	private record Box(float x0, float y0, float z0, float x1, float y1, float z1, int u, int v, int w, int h, int d) {
	}

	/** Moves to ({@code x}, {@code y}, {@code z}) pixels and turns about {@code axis} by {@code motion}. */
	private record Joint(float x, float y, float z, String axis, String motion) {
	}

	private record Piece(String part, List<Joint> joints, List<Box> boxes) {
	}

	private record Outfit(RenderType type, @Nullable RenderType glow, float width, float height, List<Piece> pieces) {
	}

	public CostumeLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	/** Adds the layer to every humanoid renderer (players, zombies and skeletons in costume, armor stands). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new CostumeLayer((RenderLayerParent) renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		Outfit outfit = outfits().get(state.headEquipment.getItem());
		if (outfit == null) {
			return;
		}
		boolean airborne = airborne(state);
		for (Piece piece : outfit.pieces()) {
			ModelPart part = part(getParentModel(), piece.part());
			if (part == null || !part.visible) {
				continue;
			}
			pose.pushPose();
			part.translateAndRotate(pose);
			for (Joint joint : piece.joints()) {
				pose.translate(joint.x() / 16.0F, joint.y() / 16.0F, joint.z() / 16.0F);
				float degrees = (float) Math.toDegrees(angle(joint.motion(), state, airborne));
				pose.rotateDegrees(switch (joint.axis()) {
					case "x" -> Axis.XP;
					case "z" -> Axis.ZP;
					default -> Axis.YP;
				}, degrees);
			}
			collector.submitCustomGeometry(pose, outfit.type(), (matrix, buffer) -> {
				for (Box box : piece.boxes()) {
					box(buffer, matrix, box, light, outfit.width(), outfit.height());
				}
			});
			if (outfit.glow() != null) {
				collector.submitCustomGeometry(pose, outfit.glow(), (matrix, buffer) -> {
					for (Box box : piece.boxes()) {
						box(buffer, matrix, box, FULL_BRIGHT, outfit.width(), outfit.height());
					}
				});
			}
			pose.popPose();
		}
	}

	/**
	 * How far a joint turns, in radians, for its motion. The cape flares further the faster its wearer walks and folds
	 * round them while they sneak (its side panels a quarter turn, its front panels nearly another); tails sway with
	 * time and a walk; wings fold back on the ground and beat in the air.
	 */
	static float angle(String motion, HumanoidRenderState state, boolean airborne) {
		float wrap = state.isCrouching ? 1.0F : 0.0F;
		float t = state.ageInTicks;
		float walk = Math.min(1.0F, state.walkAnimationSpeed);
		float wing = airborne ? -0.15F + 0.55F * Mth.sin(t * 1.1F) : -1.2F;
		return switch (motion) {
			case "cape_flare" -> (0.12F + 0.9F * walk + 0.05F * Mth.sin(t * 0.25F)) * (1.0F - 0.8F * wrap);
			case "cape_side_left" -> Mth.lerp(wrap, -0.25F, 1.57F);
			case "cape_side_right" -> -Mth.lerp(wrap, -0.25F, 1.57F);
			case "cape_front_left" -> Mth.lerp(wrap, -0.9F, 1.45F);
			case "cape_front_right" -> -Mth.lerp(wrap, -0.9F, 1.45F);
			case "tail_sway" -> 0.45F * Mth.sin(t * 0.1F) + 0.4F * walk * Mth.sin(state.walkAnimationPos * 0.6F);
			case "tail_lift" -> 0.55F + 0.1F * Mth.sin(t * 0.07F);
			case "tail_curl" -> 0.45F + 0.1F * Mth.sin(t * 0.09F + 1.0F);
			case "wolf_tail" -> -0.75F + 0.3F * walk;
			case "wing_left" -> wing;
			case "wing_right" -> -wing;
			default -> 0.0F;
		};
	}

	/** Whether nothing solid is just under the wearer's feet (one block looked at). */
	private static boolean airborne(HumanoidRenderState state) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return false;
		}
		BlockPos below = BlockPos.containing(state.x, state.y - 0.05, state.z);
		return level.getBlockState(below).getCollisionShape(level, below).isEmpty();
	}

	private static @Nullable ModelPart part(HumanoidModel<?> model, String name) {
		return switch (name) {
			case "head" -> model.head;
			case "body" -> model.body;
			case "right_arm" -> model.rightArm;
			case "left_arm" -> model.leftArm;
			case "right_leg" -> model.rightLeg;
			case "left_leg" -> model.leftLeg;
			default -> null;
		};
	}

	private static Map<Item, Outfit> outfits() {
		if (outfits == null) {
			outfits = new HashMap<>();
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
			if (resource.isEmpty()) {
				Jugcraft.LOGGER.warn("Missing {}: outfits are worn but not drawn", FILE);
				return outfits;
			}
			try (Reader reader = resource.get().openAsReader()) {
				for (Map.Entry<String, JsonElement> entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
					Item item = BuiltInRegistries.ITEM.getValue(Jugcraft.id(entry.getKey()));
					outfits.put(item, parse(entry.getValue().getAsJsonObject()));
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.warn("Could not read {}", FILE, e);
			}
		}
		return outfits;
	}

	private static Outfit parse(JsonObject json) {
		JsonArray size = json.getAsJsonArray("size");
		RenderType glow = json.has("glow") ? RenderTypes.eyes(Identifier.parse(json.get("glow").getAsString())) : null;
		List<Piece> pieces = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("pieces")) {
			JsonObject piece = element.getAsJsonObject();
			List<Joint> joints = new ArrayList<>();
			for (JsonElement j : piece.getAsJsonArray("joints")) {
				JsonArray a = j.getAsJsonArray();
				joints.add(new Joint(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsString(), a.get(4).getAsString()));
			}
			List<Box> boxes = new ArrayList<>();
			for (JsonElement b : piece.getAsJsonArray("boxes")) {
				JsonArray a = b.getAsJsonArray();
				boxes.add(new Box(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat(), a.get(4).getAsFloat(),
						a.get(5).getAsFloat(), a.get(6).getAsInt(), a.get(7).getAsInt(), a.get(8).getAsInt(), a.get(9).getAsInt(), a.get(10).getAsInt()));
			}
			pieces.add(new Piece(piece.get("part").getAsString(), List.copyOf(joints), List.copyOf(boxes)));
		}
		return new Outfit(RenderTypes.entityCutout(Identifier.parse(json.get("texture").getAsString())), glow, size.get(0).getAsFloat(),
				size.get(1).getAsFloat(), List.copyOf(pieces));
	}

	/**
	 * The six faces of a box, textured as a vanilla model's box is (as {@link GhostSheetLayer} draws them): the top and
	 * bottom in a row above the four sides, which run west, north, east, south. Model space has y downwards.
	 */
	private static void box(VertexConsumer buffer, PoseStack.Pose matrix, Box b, int light, float width, float height) {
		int u = b.u();
		int v = b.v();
		int w = b.w();
		int h = b.h();
		int d = b.d();
		face(buffer, matrix, light, width, height, 0, -1, 0, u + d, v, w, d,
				b.x0(), b.y0(), b.z0(), b.x1(), b.y0(), b.z0(), b.x1(), b.y0(), b.z1(), b.x0(), b.y0(), b.z1());
		face(buffer, matrix, light, width, height, 0, 1, 0, u + d + w, v, w, d,
				b.x0(), b.y1(), b.z1(), b.x1(), b.y1(), b.z1(), b.x1(), b.y1(), b.z0(), b.x0(), b.y1(), b.z0());
		face(buffer, matrix, light, width, height, -1, 0, 0, u, v + d, d, h,
				b.x0(), b.y0(), b.z1(), b.x0(), b.y0(), b.z0(), b.x0(), b.y1(), b.z0(), b.x0(), b.y1(), b.z1());
		face(buffer, matrix, light, width, height, 0, 0, -1, u + d, v + d, w, h,
				b.x0(), b.y0(), b.z0(), b.x1(), b.y0(), b.z0(), b.x1(), b.y1(), b.z0(), b.x0(), b.y1(), b.z0());
		face(buffer, matrix, light, width, height, 1, 0, 0, u + d + w, v + d, d, h,
				b.x1(), b.y0(), b.z0(), b.x1(), b.y0(), b.z1(), b.x1(), b.y1(), b.z1(), b.x1(), b.y1(), b.z0());
		face(buffer, matrix, light, width, height, 0, 0, 1, u + 2 * d + w, v + d, w, h,
				b.x1(), b.y0(), b.z1(), b.x0(), b.y0(), b.z1(), b.x0(), b.y1(), b.z1(), b.x1(), b.y1(), b.z1());
	}

	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, int light, float width, float height, float nx, float ny, float nz,
			int tu, int tv, int tw, int th, float... corners) {
		float[][] uv = {{tu, tv}, {tu + tw, tv}, {tu + tw, tv + th}, {tu, tv + th}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i * 3] / 16.0F, corners[i * 3 + 1] / 16.0F, corners[i * 3 + 2] / 16.0F).setColor(0xFFFFFFFF)
					.setUv(uv[i][0] / width, uv[i][1] / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(matrix, nx, ny, nz);
		}
	}
}
