package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.ExosuitItem;
import io.github.jimbozoomer.jugcraft.gear.JugcraftExosuit;
import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;
import org.jspecify.annotations.Nullable;

/**
 * Worn 3D models: armor that is not limited to vanilla's flat armor shape, made of boxes of any size, sticking out at any
 * angle, fixed to the wearer's body parts so they follow walking, swinging, sneaking and armor stand poses. Drawn on
 * every humanoid (players, zombies, skeletons, armor stands) for each worn piece with entries in
 * assets/jugcraft/worn_models.json, each keyed "{item}_{body part}" (the body part one of head, body, right_arm,
 * left_arm, right_leg, left_leg) and holding quads in that part's space ({@link QuadModel}).
 *
 * <p>Two kinds of entry:
 * <ul>
 * <li>The 3D armor sets of tools/armor_models.py (the knight armor of bronze and steel), keyed by the item's id
 * ("steel_chestplate_right_arm"), each set on one atlas texture. They are drawn as vanilla draws armor
 * ({@code armorCutoutNoCull}), with the enchantment glint over an enchanted piece, and not on babies and small armor
 * stands, whose body parts are a different size. A piece reaches the render state's equipment through its item's
 * equippable asset id, as any armor does. When a whole set is 3D its equipment asset has no flat layers left, so
 * tools/gear.py writes no asset file at all (26.3 cannot read an empty layer map): the vanilla armor layer finds no
 * asset and draws nothing under the 3D pieces.</li>
 * <li>The exosuit's parts (tools/exosuit.py: the shoulder plates, skirt plates and the Ronin's hat), keyed by livery and
 * piece ("vanguard_chestplate_right_arm"). They are drawn exactly as before this layer replaced ExosuitLayer: with their
 * own block textures, over the exosuit's flat layers, without a glint of their own.</li>
 * </ul>
 * The file is read once per resource reload. A key that names neither an item nor an exosuit piece is skipped with a
 * warning, as is the whole file if it is missing.
 */
public class WornModelLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private static final Identifier FILE = Jugcraft.id("worn_models.json");
	/** Submission orders: the plates, then the glint over them (it is drawn only where the plates already are). */
	private static final int PLATES = 0;
	private static final int GLINT = 1;
	private static @Nullable Map<Item, Worn> models;

	/** A body part a key in worn_models.json can end with. */
	private enum Bone {
		HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;

		/** "_head", "_right_arm"... */
		final String suffix = "_" + name().toLowerCase(Locale.ROOT);

		ModelPart of(HumanoidModel<?> model) {
			return switch (this) {
				case HEAD -> model.head;
				case BODY -> model.body;
				case RIGHT_ARM -> model.rightArm;
				case LEFT_ARM -> model.leftArm;
				case RIGHT_LEG -> model.rightLeg;
				case LEFT_LEG -> model.leftLeg;
			};
		}

		/** The body part {@code key} ends with, or null (the rocket pack's entry, which RocketPackLayer draws). */
		static @Nullable Bone ending(String key) {
			for (Bone bone : values()) {
				if (key.length() > bone.suffix.length() && key.endsWith(bone.suffix)) {
					return bone;
				}
			}
			return null;
		}
	}

	/** One worn item's models by body part; {@code exosuit} is the exosuit piece's armor type, null for an armor set's. */
	private record Worn(Map<Bone, QuadModel> bones, @Nullable ArmorType exosuit) {
	}

	public WornModelLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	/** Adds the layer to every humanoid renderer, and rereads worn_models.json after every resource reload. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register(new WornModelLayer((RenderLayerParent) renderer));
			}
		});
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Jugcraft.id("worn_models"),
				(ResourceManagerReloadListener) manager -> models = load(manager));
	}

	/** How many body parts carry a worn model of {@code item} (0 for none): for the client tests. */
	public static int bones(Item item) {
		Worn worn = models().get(item);
		return worn == null ? 0 : worn.bones().size();
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		Map<Item, Worn> all = models();
		if (all.isEmpty()) {
			return;
		}
		draw(all, pose, collector, light, state, state.headEquipment, ArmorType.HELMET);
		draw(all, pose, collector, light, state, state.chestEquipment, ArmorType.CHESTPLATE);
		draw(all, pose, collector, light, state, state.legsEquipment, ArmorType.LEGGINGS);
		draw(all, pose, collector, light, state, state.feetEquipment, ArmorType.BOOTS);
	}

	/** Draws the worn models of {@code stack}, worn in the {@code slot} slot (the render state holds only stacks equipped there). */
	private void draw(Map<Item, Worn> all, PoseStack pose, SubmitNodeCollector collector, int light, S state, ItemStack stack,
			ArmorType slot) {
		Worn worn = stack.isEmpty() ? null : all.get(stack.getItem());
		if (worn == null) {
			return;
		}
		boolean exosuit = worn.exosuit() != null;
		// The exosuit's parts as they always were: only from their own slot, and on every wearer. Armor sets' boxes are sized
		// for adult body parts, so babies (and small armor stands, which count as babies) wear none, as they wear no flat
		// Jugcraft armor either.
		if (exosuit ? worn.exosuit() != slot : state.isBaby) {
			return;
		}
		boolean foil = !exosuit && stack.hasFoil();
		// An armless armor stand hides its arms but still poses them; vanilla draws a chestplate's sleeves on them, so armor
		// sets' pauldrons and vambraces stay too. Elsewhere a hidden body part hides what is worn on it.
		boolean stand = !exosuit && state instanceof ArmorStandRenderState;
		for (Map.Entry<Bone, QuadModel> entry : worn.bones().entrySet()) {
			ModelPart part = entry.getKey().of(getParentModel());
			if (!part.visible && !stand) {
				continue;
			}
			pose.pushPose();
			part.translateAndRotate(pose);
			// Every entry is stored turned half a turn about z (tools/exosuit.py, tools/armor_models.py), as the rocket pack is:
			// turn it back, so faces keep their winding.
			pose.rotateDegrees(Axis.ZP, 180);
			QuadModel model = entry.getValue();
			if (exosuit) {
				model.submit(pose, collector, light);
			} else {
				model.submitAs(pose, collector, PLATES, light, RenderTypes::armorCutoutNoCull);
				if (foil) {
					model.submitAs(pose, collector, GLINT, light, RenderTypes::armorCutoutNoCullGlint);
				}
			}
			pose.popPose();
		}
	}

	private static Map<Item, Worn> models() {
		Map<Item, Worn> all = models;
		if (all == null) {
			// Only before the first reload has run.
			all = load(Minecraft.getInstance().getResourceManager());
			models = all;
		}
		return all;
	}

	private static Map<Item, Worn> load(ResourceManager manager) {
		Map<Item, Worn> out = new HashMap<>();
		Optional<Resource> resource = manager.getResource(FILE);
		if (resource.isEmpty()) {
			Jugcraft.LOGGER.warn("Missing {}: worn armor shows only its flat layers", FILE);
			return out;
		}
		Map<String, ExosuitItem> exosuit = exosuitPieces();
		try (Reader reader = resource.get().openAsReader()) {
			for (Map.Entry<String, JsonElement> entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
				String key = entry.getKey();
				Bone bone = Bone.ending(key);
				if (bone == null) {
					continue;
				}
				try {
					String name = key.substring(0, key.length() - bone.suffix.length());
					ExosuitItem piece = exosuit.get(name);
					Item item = piece != null ? piece : BuiltInRegistries.ITEM.getValue(Jugcraft.id(name));
					// ITEM gives air for an unknown id, and an empty slot holds air: never draw on nothing.
					if (item == Items.AIR) {
						Jugcraft.LOGGER.warn("{}: {} names no item", FILE, key);
						continue;
					}
					ArmorType type = piece != null ? piece.type() : null;
					Worn worn = out.computeIfAbsent(item, i -> new Worn(new EnumMap<>(Bone.class), type));
					if (worn.exosuit() != type) {
						Jugcraft.LOGGER.warn("{}: {} mixes exosuit parts and armor set parts", FILE, key);
						continue;
					}
					worn.bones().put(bone, QuadModel.parse(entry.getValue().getAsJsonArray()));
				} catch (Exception e) {
					Jugcraft.LOGGER.warn("{}: could not read {}", FILE, key, e);
				}
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read {}", FILE, e);
		}
		return out;
	}

	/** The exosuit pieces by the names their parts have in worn_models.json, "{livery}_{piece}" ("vanguard_helmet"). */
	private static Map<String, ExosuitItem> exosuitPieces() {
		Map<String, ExosuitItem> out = new HashMap<>();
		for (ExosuitItem.Style style : ExosuitItem.Style.values()) {
			for (ArmorType type : JugcraftExosuit.PIECES) {
				if (JugcraftExosuit.piece(style, type) instanceof ExosuitItem piece) {
					out.put(style.id + "_" + piece.piece(), piece);
				}
			}
		}
		return out;
	}
}
