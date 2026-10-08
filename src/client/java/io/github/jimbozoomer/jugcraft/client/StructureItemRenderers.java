package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Items that show the very thing they place, in 3D and in miniature (Justin, October 8 2026: "make it look like it
 * actually does in game ... so people know what they're actually spawning in"). The nuclear plant's equipment, the
 * power line structures and the drones are drawn in the world by Java renderers, not block models, so their items
 * use special item models (item JSON {@code "type": "minecraft:special"}) that call the same drawing code:
 *
 * <ul>
 * <li>{@code jugcraft:equipment} with {@code "kind"}: a piece of plant equipment (NuclearRenderer.drawItem);
 * <li>{@code jugcraft:power_line} with {@code "kind"}: a pole, tower or box (LineRenderer.drawItem);
 * <li>{@code jugcraft:drone} with {@code "tier"} (10: the Creative Drone): a drone at rest (DroneModel.draw).
 * </ul>
 *
 * <p>Each is centred in the block and scaled so that in the slot its outline fills {@link #FIT} of a block's, and the
 * item model's display transforms (its base model's parent is minecraft:block/block) show it like a block item: in the
 * slot, in the hand, on the ground and in an item frame. Its size is measured once by drawing it into {@link Bounds}.
 * The item JSONs are written by tools/structure_items.py.
 */
public final class StructureItemRenderers {
	/** How much of a whole block's outline in the slot the miniature fills (as the tank kits', tools/tank_kit_models.py). */
	static final float FIT = 15 / 16F;
	private static final float SIN45 = (float) Math.sqrt(0.5), SIN30 = 0.5F, COS30 = (float) Math.sqrt(0.75);

	private StructureItemRenderers() {
	}

	public static void register() {
		SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("equipment"), EquipmentUnbaked.MAP_CODEC);
		SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("power_line"), LineUnbaked.MAP_CODEC);
		SpecialModelRenderers.ID_MAPPER.put(Jugcraft.id("drone"), DroneUnbaked.MAP_CODEC);
	}

	/** Draws a structure into a consumer through a pose, lit with {@code light}. */
	@FunctionalInterface
	interface Drawing {
		void draw(PoseStack.Pose pose, VertexConsumer consumer, int light);
	}

	/** A special item renderer for one structure: its drawing, fitted into the block. */
	static final class Miniature implements NoDataSpecialModelRenderer {
		private final RenderType renderType;
		private final Drawing drawing;
		/** min x, y, z, max x, y, z of the drawing, measured on first use. */
		private float[] box;

		Miniature(RenderType renderType, Drawing drawing) {
			this.renderType = renderType;
			this.drawing = drawing;
		}

		private float[] box() {
			if (box == null) {
				Bounds bounds = new Bounds();
				drawing.draw(new PoseStack().last(), bounds, 0);
				box = bounds.box();
			}
			return box;
		}

		/**
		 * The scale that fits the drawing into the slot: as seen in the slot (turned 225 degrees and tipped 30, the block
		 * item's view), its outline is at most {@link #FIT} of a whole block's, so a tall vessel or a long turbine fills
		 * the slot as a block does instead of shrinking to its longest side.
		 */
		private float scale(float[] b) {
			float w = b[3] - b[0], h = b[4] - b[1], d = b[5] - b[2];
			if (w <= 0 && h <= 0 && d <= 0) {
				return 1;
			}
			float across = SIN45 * (w + d) / 2, high = COS30 * h / 2 + SIN30 * SIN45 * (w + d) / 2;
			float cubeAcross = SIN45, cubeHigh = COS30 / 2 + SIN30 * SIN45;
			return FIT * Math.min(across > 0 ? cubeAcross / across : Float.MAX_VALUE, high > 0 ? cubeHigh / high : Float.MAX_VALUE);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
			float[] b = box();
			float s = scale(b);
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			poseStack.scale(s, s, s);
			poseStack.translate(-(b[0] + b[3]) / 2, -(b[1] + b[4]) / 2, -(b[2] + b[5]) / 2);
			collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> drawing.draw(pose, consumer, light));
			poseStack.popPose();
		}

		@Override
		public void getExtents(Consumer<Vector3fc> output) {
			float[] b = box();
			float s = scale(b);
			for (int corner = 0; corner < 8; corner++) {
				float x = (corner & 1) == 0 ? b[0] : b[3], y = (corner & 2) == 0 ? b[1] : b[4], z = (corner & 4) == 0 ? b[2] : b[5];
				output.accept(new Vector3f(0.5F + (x - (b[0] + b[3]) / 2) * s, 0.5F + (y - (b[1] + b[4]) / 2) * s,
						0.5F + (z - (b[2] + b[5]) / 2) * s));
			}
		}
	}

	/** Measures what is drawn into it: the box round every vertex. */
	static final class Bounds implements VertexConsumer {
		private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		private float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

		float[] box() {
			return minX > maxX ? new float[] {0, 0, 0, 1, 1, 1} : new float[] {minX, minY, minZ, maxX, maxY, maxZ};
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int argb) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv3(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			return this;
		}
	}

	/** {@code {"type": "jugcraft:equipment", "kind": "reactor_pressure_vessel"}}: an EquipmentKind by its id. */
	record EquipmentUnbaked(String kind) implements NoDataSpecialModelRenderer.Unbaked {
		static final MapCodec<EquipmentUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.fieldOf("kind").forGetter(EquipmentUnbaked::kind)).apply(i, EquipmentUnbaked::new));

		@Override
		public MapCodec<EquipmentUnbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
			EquipmentKind found = EquipmentKind.byId(kind);
			if (found == null) {
				Jugcraft.LOGGER.warn("No plant equipment {} for an item model; it shows nothing", kind);
				return new Miniature(RenderTypes.entityCutout(NuclearRenderer.ITEM_TEXTURE), (pose, consumer, light) -> { });
			}
			return new Miniature(RenderTypes.entityCutout(NuclearRenderer.ITEM_TEXTURE),
					(pose, consumer, light) -> NuclearRenderer.drawItem(found, pose, consumer, light));
		}
	}

	/** {@code {"type": "jugcraft:power_line", "kind": "lattice_tower"}}: a LineKind by its id. */
	record LineUnbaked(String kind) implements NoDataSpecialModelRenderer.Unbaked {
		static final MapCodec<LineUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.fieldOf("kind").forGetter(LineUnbaked::kind)).apply(i, LineUnbaked::new));

		@Override
		public MapCodec<LineUnbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
			LineKind found = LineKind.byId(kind);
			if (found == null) {
				Jugcraft.LOGGER.warn("No power line structure {} for an item model; it shows nothing", kind);
				return new Miniature(RenderTypes.entityCutout(LineRenderer.ITEM_TEXTURE), (pose, consumer, light) -> { });
			}
			return new Miniature(RenderTypes.entityCutout(LineRenderer.ITEM_TEXTURE),
					(pose, consumer, light) -> LineRenderer.drawItem(found, pose, consumer, light));
		}
	}

	/**
	 * {@code {"type": "jugcraft:drone", "tier": 1}}: a drone of that tier (10: the Creative Drone) at rest, its nose to
	 * the north (the slot's right), in the drones' own texture (a drone is a few blocks across, so its texture holds up).
	 */
	record DroneUnbaked(int tier) implements NoDataSpecialModelRenderer.Unbaked {
		static final MapCodec<DroneUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 10).fieldOf("tier").forGetter(DroneUnbaked::tier)).apply(i, DroneUnbaked::new));

		@Override
		public MapCodec<DroneUnbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
			return new Miniature(RenderTypes.entityCutout(DroneDepotRenderer.TEXTURE), (pose, consumer, light) ->
					DroneModel.draw(new DroneDepotRenderer.Geometry(pose, consumer), 0, 0, 0, (float) Math.PI, tier, -1, 0, light, 0));
		}
	}
}
