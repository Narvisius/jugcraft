package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.MeshQuadCollection;
import net.fabricmc.fabric.api.client.renderer.v1.model.ModelStateHelper;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

/**
 * Item models made of free quads instead of boxes: the Runebound arms' smooth meshes (tools/arms_mesh.py,
 * docs/features/arms-vii.md "Runebound meshes"). A model JSON with {@code "fabric:type": {"id": "jugcraft:mesh",
 * "optional": true}} lists {@code "quads"}, each {@code {"t": texture slot, "e": 1 if it glows, "v": four corners of
 * [x, y, z (model pixels), u, v (0 to 1 of the sprite), nx, ny, nz]}}, beside the usual textures and display
 * transforms. They are baked into a Fabric renderer API mesh (per-corner normals, so curves shade smoothly; glowing
 * quads at full light), which Fabric's item model wrapper draws as any item model is drawn: the hand poses, enchantment
 * glint and motion are unchanged.
 *
 * <p>Built on Fabric API's model loading and renderer API (part of fabric-api, no new dependency), written as its own
 * test mod's mesh models are. Each fallback keeps the arm visible: if this loader is not registered, the JSON loads
 * as a vanilla model from its {@code "elements"} (the arm's old box model); if its quads cannot be read or baked,
 * those boxes are used instead. {@link #BAKED} and {@link #FALLBACKS} count what happened, so the client game test
 * can tell meshes from boxes.
 */
public final class MeshItemModels {
	public static final Identifier TYPE = Jugcraft.id("mesh");
	/** Mesh models baked as meshes since the game started (every resource reload bakes them again). */
	public static final AtomicInteger BAKED = new AtomicInteger();
	/** Mesh models drawn as their box models instead: unreadable quads or a failed bake. */
	public static final AtomicInteger FALLBACKS = new AtomicInteger();

	private MeshItemModels() {
	}

	public static void register() {
		try {
			UnbakedModelDeserializer.register(TYPE, new Deserializer());
		} catch (RuntimeException | LinkageError e) {
			Jugcraft.LOGGER.warn("Could not register the mesh item models; the Runebound arms keep their box models", e);
		}
	}

	/** One quad: its texture slot, whether it glows, and its four corners' 8 numbers each. */
	record MeshQuad(String slot, boolean emissive, float[] vertices) {
	}

	static final class Deserializer implements UnbakedModelDeserializer {
		@Override
		public UnbakedModel deserialize(JsonObject json, JsonDeserializationContext context) {
			UnbakedGeometry boxes = null;
			if (json.has("elements")) {
				List<CuboidModelElement> elements = new ArrayList<>();
				for (JsonElement element : GsonHelper.getAsJsonArray(json, "elements")) {
					elements.add(context.deserialize(element, CuboidModelElement.class));
				}
				boxes = new UnbakedCuboidGeometry(elements);
			}
			List<MeshQuad> quads;
			try {
				quads = quads(GsonHelper.getAsJsonArray(json, "quads"));
			} catch (RuntimeException e) {
				Jugcraft.LOGGER.warn("Unreadable mesh model quads; drawing its box model instead", e);
				FALLBACKS.incrementAndGet();
				return context.deserialize(json, CuboidModel.class);
			}
			UnbakedModel.GuiLight guiLight = null;
			if (json.has("gui_light")) {
				guiLight = UnbakedModel.GuiLight.getByName(GsonHelper.getAsString(json, "gui_light"));
			}
			ItemTransforms transforms = null;
			if (json.has("display")) {
				transforms = context.deserialize(GsonHelper.getAsJsonObject(json, "display"), ItemTransforms.class);
			}
			TextureSlots.Data textures = json.has("textures")
					? TextureSlots.parseTextureMap(GsonHelper.getAsJsonObject(json, "textures"))
					: TextureSlots.Data.EMPTY;
			String parent = GsonHelper.getAsString(json, "parent", "");
			return new CuboidModel(new MeshGeometry(quads, boxes), guiLight, null, transforms, textures,
					parent.isEmpty() ? null : Identifier.parse(parent));
		}

		private static List<MeshQuad> quads(JsonArray array) {
			List<MeshQuad> out = new ArrayList<>(array.size());
			for (JsonElement element : array) {
				JsonObject quad = element.getAsJsonObject();
				JsonArray corners = quad.getAsJsonArray("v");
				if (corners.size() != 4) {
					throw new JsonParseException("A mesh quad needs 4 corners, not " + corners.size());
				}
				float[] vertices = new float[32];
				for (int i = 0; i < 4; i++) {
					JsonArray corner = corners.get(i).getAsJsonArray();
					if (corner.size() != 8) {
						throw new JsonParseException("A mesh quad's corner needs 8 numbers, not " + corner.size());
					}
					for (int k = 0; k < 8; k++) {
						vertices[i * 8 + k] = corner.get(k).getAsFloat();
					}
				}
				boolean emissive = quad.has("e") && quad.get("e").getAsInt() != 0;
				out.add(new MeshQuad(quad.get("t").getAsString(), emissive, vertices));
			}
			return out;
		}
	}

	/** The quads, baked into a mesh; the box model, if the JSON has one, if they cannot be. */
	record MeshGeometry(List<MeshQuad> quads, @Nullable UnbakedGeometry boxes) implements UnbakedGeometry {
		@Override
		public QuadCollection bake(TextureSlots textures, ModelBaker baker, ModelState state, ModelDebugName name) {
			try {
				QuadCollection baked = mesh(textures, baker, state, name);
				BAKED.incrementAndGet();
				return baked;
			} catch (RuntimeException e) {
				FALLBACKS.incrementAndGet();
				if (boxes == null) {
					throw e;
				}
				Jugcraft.LOGGER.warn("Could not bake a mesh item model; drawing its box model instead", e);
				return boxes.bake(textures, baker, state, name);
			}
		}

		private QuadCollection mesh(TextureSlots textures, ModelBaker baker, ModelState state, ModelDebugName name) {
			MutableMesh builder = Renderer.get().mutableMesh();
			QuadEmitter emitter = builder.emitter();
			emitter.pushTransform(ModelStateHelper.asQuadTransform(state, baker.materials()));
			try {
				Map<String, Material.Baked> materials = new HashMap<>();
				for (MeshQuad quad : quads) {
					Material.Baked material = materials.get(quad.slot());
					if (material == null) {
						Material unbaked = Objects.requireNonNull(textures.getMaterial(quad.slot()), quad.slot());
						material = baker.materials().get(unbaked, name);
						materials.put(quad.slot(), material);
					}
					float[] v = quad.vertices();
					for (int i = 0; i < 4; i++) {
						int o = i * 8;
						emitter.pos(i, v[o] / 16.0F, v[o + 1] / 16.0F, v[o + 2] / 16.0F);
						emitter.uv(i, v[o + 3], v[o + 4]);
						emitter.normal(i, v[o + 5], v[o + 6], v[o + 7]);
					}
					// The UVs are 0 to 1 of the sprite; foil (glint) is left to follow the stack.
					emitter.materialBake(material, MutableQuadView.BAKE_NORMALIZED);
					emitter.emissive(quad.emissive());
					emitter.emit();
				}
			} finally {
				emitter.popTransform();
			}
			return new MeshQuadCollection(builder.immutableCopy());
		}
	}
}
