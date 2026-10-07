package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Draws a String Light Hook's strand, sagging from this hook to the one it is strung to. String lights are a dark
 * wire with a tiny pumpkin bulb about every {@value #SPACING} blocks; lit, the bulbs glow at full brightness, unlit
 * they are dim orange. Bat Bunting is a twine cord hung with orange and black pennants and paper bats about every
 * {@value #PENNANT_SPACING} blocks. Drawn from the hook that holds the strand, and off screen too, since a strand can
 * be long.
 */
public class StringLightsRenderer implements BlockEntityRenderer<StringLightHookBlockEntity, StringLightsRenderer.State> {
	private static final RenderType STRAND = RenderTypes.entityCutout(Jugcraft.id("textures/entity/string_lights.png"));
	/** Twine (u 0-0.25), an orange pennant (0.25-0.5), a black pennant (0.5-0.75) and a paper bat (0.75-1). */
	private static final RenderType BUNTING = RenderTypes.entityCutout(Jugcraft.id("textures/entity/bat_bunting.png"));
	/** A garland's cord (u 0-0.25), its leaves (0.25-0.5 and 0.5-0.75) and a warm bulb (0.75-1). */
	private static final RenderType VINE_GARLAND = RenderTypes.entityCutout(Jugcraft.id("textures/entity/pumpkin_vine_garland.png"));
	private static final RenderType LEAF_GARLAND = RenderTypes.entityCutout(Jugcraft.id("textures/entity/autumn_leaf_garland.png"));
	private static final float LEAF_SPACING = 0.26F;
	private static final float LEAF = 0.24F;
	private static final float GOURD_SPACING = 1.4F;
	private static final float GOURD = 0.17F;
	private static final float TWIST = 0.022F;
	/** The autumn leaves' colours, in turn: red maple, orange, gold aspen, rust and amber. */
	private static final int[] AUTUMN = {0xFFC8361A, 0xFFE07A1E, 0xFFE8B42A, 0xFFA8401E, 0xFFD89A28};
	private static final float PENNANT_SPACING = 0.5F;
	private static final float PENNANT_WIDTH = 0.4F;
	private static final float PENNANT_DROP = 0.42F;
	private static final float WIRE = 0.6F / 16;
	private static final float BULB = 2.5F / 16;
	private static final float SPACING = 0.75F;
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends BlockEntityRenderState {
		@Nullable Vec3 offset;
		boolean lit;
		StringLightHookBlockEntity.Strand strand = StringLightHookBlockEntity.Strand.LIGHTS;
	}

	public StringLightsRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StringLightHookBlockEntity hook, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(hook, state, crumbling);
		BlockPos link = hook.link();
		BlockPos pos = hook.getBlockPos();
		state.offset = null;
		if (link != null && hook.getLevel() != null && hook.getLevel().getBlockEntity(link) instanceof StringLightHookBlockEntity) {
			state.offset = Vec3.atLowerCornerOf(link.subtract(pos));
			state.lit = StringLightHookBlockEntity.glows(hook.getLevel(), pos, link);
			state.strand = hook.strand();
		}
	}

	/** The point a fraction {@code t} along the strand, sagging most in the middle. */
	private static Vector3f along(Vector3f start, Vec3 offset, float sag, float t) {
		return new Vector3f(start).add((float) offset.x * t, (float) offset.y * t - sag * 4.0F * t * (1.0F - t), (float) offset.z * t);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		Vec3 offset = state.offset;
		if (offset == null) {
			return;
		}
		float length = (float) offset.length();
		float sag = 0.15F + 0.05F * length;
		int segments = Math.max(4, (int) Math.ceil(length * 3.0F));
		int bulbs = Math.max(1, (int) Math.floor(length / SPACING));
		Vector3f start = new Vector3f(0.5F, 0.5F, 0.5F);
		int light = state.lightCoords;
		if (state.strand == StringLightHookBlockEntity.Strand.BUNTING) {
			submitBunting(pose, collector, offset, length, sag, segments, start, light);
			return;
		}
		if (state.strand == StringLightHookBlockEntity.Strand.PUMPKIN_VINE || state.strand == StringLightHookBlockEntity.Strand.AUTUMN_LEAVES) {
			submitGarland(pose, collector, offset, length, sag, segments, start, light, state.lit,
					state.strand == StringLightHookBlockEntity.Strand.PUMPKIN_VINE);
			return;
		}
		int bulbLight = state.lit ? FULL_BRIGHT : light;
		int bulbColor = state.lit ? 0xFFFFFFFF : 0xFF9A7A5A;
		collector.submitCustomGeometry(pose, STRAND, (matrix, buffer) -> {
			for (int i = 0; i < segments; i++) {
				Vector3f a = along(start, offset, sag, i / (float) segments);
				Vector3f b = along(start, offset, sag, (i + 1) / (float) segments);
				wire(buffer, matrix, light, a, b, new Vector3f(0, WIRE, 0), 0.0F, 0.5F);
				Vector3f side = new Vector3f(b).sub(a).cross(0, 1, 0);
				wire(buffer, matrix, light, a, b, side.lengthSquared() < 1.0E-8F ? new Vector3f(WIRE, 0, 0) : side.normalize().mul(WIRE), 0.0F, 0.5F);
			}
			for (int i = 1; i <= bulbs; i++) {
				Vector3f at = along(start, offset, sag, i / (float) (bulbs + 1)).sub(0, BULB * 0.7F, 0);
				bulb(buffer, matrix, bulbLight, bulbColor, at);
			}
		});
	}

	/**
	 * Bat Bunting: the twine cord, then a pennant or bat hanging below it about every {@value #PENNANT_SPACING} blocks,
	 * orange, black and a bat in turn, each a flat shape along the cord seen from both sides.
	 */
	private static void submitBunting(PoseStack pose, SubmitNodeCollector collector, Vec3 offset, float length, float sag, int segments,
			Vector3f start, int light) {
		int pennants = Math.max(1, (int) Math.floor(length / PENNANT_SPACING));
		collector.submitCustomGeometry(pose, BUNTING, (matrix, buffer) -> {
			for (int i = 0; i < segments; i++) {
				Vector3f a = along(start, offset, sag, i / (float) segments);
				Vector3f b = along(start, offset, sag, (i + 1) / (float) segments);
				wire(buffer, matrix, light, a, b, new Vector3f(0, WIRE, 0), 0.0F, 0.25F);
				Vector3f side = new Vector3f(b).sub(a).cross(0, 1, 0);
				wire(buffer, matrix, light, a, b, side.lengthSquared() < 1.0E-8F ? new Vector3f(WIRE, 0, 0) : side.normalize().mul(WIRE), 0.0F, 0.25F);
			}
			float half = Math.min(PENNANT_WIDTH, length / pennants * 0.9F) / 2 / length;
			for (int i = 0; i < pennants; i++) {
				float middle = (i + 0.5F) / pennants;
				Vector3f a = along(start, offset, sag, middle - half);
				Vector3f b = along(start, offset, sag, middle + half);
				int kind = i % 3;
				float u0 = 0.25F + kind * 0.25F;
				// A bat hangs a little lower on its thread than the pennants.
				float drop = kind == 2 ? PENNANT_DROP * 0.8F : PENNANT_DROP;
				Vector3f lower = new Vector3f(0, kind == 2 ? -0.06F : 0.0F, 0);
				Vector3f top0 = new Vector3f(a).add(lower);
				Vector3f top1 = new Vector3f(b).add(lower);
				Vector3f bottom0 = new Vector3f(top0).sub(0, drop, 0);
				Vector3f bottom1 = new Vector3f(top1).sub(0, drop, 0);
				Vector3f normal = new Vector3f(b).sub(a).cross(0, 1, 0);
				normal = normal.lengthSquared() < 1.0E-8F ? new Vector3f(1, 0, 0) : normal.normalize();
				// Seen from both sides: two sides lifted off the pennant's middle, never one plane drawn twice.
				DecorDraw.twoSided(buffer, matrix, corners(bottom0, bottom1, top1, top0, u0, u0 + 0.25F), normal.x(), normal.y(), normal.z(),
						0xFFFFFFFF, light, DecorDraw.TWO_SIDED_LIFT);
			}
		});
	}

	/**
	 * A garland: for the vine two green strands twisting round each other, broad pumpkin leaves hanging either side,
	 * and a little pumpkin or gourd about every {@value #GOURD_SPACING} blocks; for the leaves a twine cord hung thick
	 * with red, orange and gold leaves. Both carry warm bulbs about every {@value #SPACING} blocks that glow when lit.
	 */
	private static void submitGarland(PoseStack pose, SubmitNodeCollector collector, Vec3 offset, float length, float sag, int segments,
			Vector3f start, int light, boolean lit, boolean vine) {
		int leaves = Math.max(2, (int) Math.floor(length / LEAF_SPACING));
		int bulbs = Math.max(1, (int) Math.floor(length / SPACING));
		int gourds = vine ? Math.max(1, (int) Math.floor(length / GOURD_SPACING)) : 0;
		int bulbLight = lit ? FULL_BRIGHT : light;
		int bulbColor = lit ? 0xFFFFFFFF : 0xFF8A7050;
		collector.submitCustomGeometry(pose, vine ? VINE_GARLAND : LEAF_GARLAND, (matrix, buffer) -> {
			for (int i = 0; i < segments; i++) {
				float ta = i / (float) segments;
				float tb = (i + 1) / (float) segments;
				Vector3f a = along(start, offset, sag, ta);
				Vector3f b = along(start, offset, sag, tb);
				Vector3f side = across(a, b);
				if (vine) {
					for (int strand = 0; strand < 2; strand++) {
						float twist = strand * (float) Math.PI;
						Vector3f twistA = twisted(side, ta * length * 9.0F + twist);
						Vector3f twistB = twisted(side, tb * length * 9.0F + twist);
						Vector3f a1 = new Vector3f(a).add(twistA);
						Vector3f b1 = new Vector3f(b).add(twistB);
						wire(buffer, matrix, light, a1, b1, new Vector3f(0, WIRE * 1.3F, 0), 0.0F, 0.25F);
						wire(buffer, matrix, light, a1, b1, new Vector3f(side).mul(WIRE * 1.3F), 0.0F, 0.25F);
					}
				} else {
					wire(buffer, matrix, light, a, b, new Vector3f(0, WIRE, 0), 0.0F, 0.25F);
					wire(buffer, matrix, light, a, b, new Vector3f(side).mul(WIRE), 0.0F, 0.25F);
				}
			}
			for (int i = 0; i < leaves; i++) {
				float t = (i + 0.5F) / leaves;
				Vector3f at = along(start, offset, sag, t);
				Vector3f dir = new Vector3f(along(start, offset, sag, Math.min(1.0F, t + 0.01F))).sub(along(start, offset, sag, Math.max(0.0F, t - 0.01F)));
				dir = dir.lengthSquared() < 1.0E-8F ? new Vector3f(1, 0, 0) : dir.normalize();
				Vector3f side = across(new Vector3f(at).sub(dir), new Vector3f(at).add(dir));
				// Each leaf hangs from the cord, turned out to one side and the other in turn.
				float angle = (i % 2 == 0 ? 1 : -1) * (0.45F + 0.3F * ((i * 37 % 11) / 11.0F));
				float size = LEAF * (0.8F + 0.35F * ((i * 53 % 7) / 7.0F));
				Vector3f down = new Vector3f(0, -(float) Math.cos(angle), 0).add(new Vector3f(side).mul((float) Math.sin(angle)));
				Vector3f half = new Vector3f(dir).mul(size / 2);
				Vector3f top0 = new Vector3f(at).sub(half);
				Vector3f top1 = new Vector3f(at).add(half);
				Vector3f bottom0 = new Vector3f(top0).add(new Vector3f(down).mul(size));
				Vector3f bottom1 = new Vector3f(top1).add(new Vector3f(down).mul(size));
				Vector3f normal = new Vector3f(dir).cross(down);
				normal = normal.lengthSquared() < 1.0E-8F ? new Vector3f(0, 0, 1) : normal.normalize();
				int color = vine ? 0xFFFFFFFF : AUTUMN[i % AUTUMN.length];
				float u0 = vine || i % 3 != 0 ? 0.25F : 0.5F;
				// Seen from both sides: two sides lifted off the leaf's middle, never one plane drawn twice.
				DecorDraw.twoSided(buffer, matrix, corners(bottom0, bottom1, top1, top0, u0, u0 + 0.25F), normal.x(), normal.y(), normal.z(),
						color, light, DecorDraw.TWO_SIDED_LIFT);
			}
			for (int i = 0; i < gourds; i++) {
				// Pumpkins and gourds in turn, hanging just under the vine.
				Vector3f at = along(start, offset, sag, (i + 0.5F) / gourds).sub(0, GOURD * 0.9F, 0);
				cube(buffer, matrix, light, i % 2 == 0 ? 0xFFFFFFFF : 0xFFB8D060, at, i % 2 == 0 ? GOURD : GOURD * 0.75F, 0.5F, 0.75F);
			}
			for (int i = 1; i <= bulbs; i++) {
				Vector3f at = along(start, offset, sag, i / (float) (bulbs + 1)).sub(0, BULB * 0.6F, 0);
				cube(buffer, matrix, bulbLight, bulbColor, at, BULB * 0.8F, 0.75F, 1.0F);
			}
		});
	}

	/** Level and across the strand from {@code a} to {@code b}, a unit vector (east when the strand hangs straight down). */
	private static Vector3f across(Vector3f a, Vector3f b) {
		Vector3f side = new Vector3f(b).sub(a).cross(0, 1, 0);
		return side.lengthSquared() < 1.0E-8F ? new Vector3f(1, 0, 0) : side.normalize();
	}

	/** An offset {@value #TWIST} from the strand, turned {@code phase} radians round it from {@code side}. */
	private static Vector3f twisted(Vector3f side, float phase) {
		return new Vector3f(side).mul((float) Math.cos(phase) * TWIST).add(0, (float) Math.sin(phase) * TWIST, 0);
	}

	/**
	 * A flat strip of wire from {@code a} to {@code b}, {@code across} wide each side, seen from both sides (two sides lifted
	 * off the strip's middle, never one plane drawn twice); textured u0 to u1.
	 */
	private static void wire(VertexConsumer buffer, PoseStack.Pose matrix, int light, Vector3f a, Vector3f b, Vector3f across, float u0, float u1) {
		Vector3f normal = new Vector3f(across).cross(new Vector3f(b).sub(a));
		normal = normal.lengthSquared() < 1.0E-12F ? new Vector3f(0, 1, 0) : normal.normalize();
		DecorDraw.twoSided(buffer, matrix, corners(new Vector3f(a).sub(across), new Vector3f(a).add(across), new Vector3f(b).add(across),
				new Vector3f(b).sub(across), u0, u1), normal.x(), normal.y(), normal.z(), 0xFFFFFFFF, light, DecorDraw.TWO_SIDED_LIFT);
	}

	/** The corners {x, y, z, u, v} of a quad laid out as {@link #quad} lays them (u0 to u1 across, v 1 at the bottom to 0 at the top). */
	private static float[][] corners(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float u1) {
		return new float[][] {{a.x(), a.y(), a.z(), u0, 1.0F}, {b.x(), b.y(), b.z(), u1, 1.0F}, {c.x(), c.y(), c.z(), u1, 0.0F},
				{d.x(), d.y(), d.z(), u0, 0.0F}};
	}

	/** A little cube of pumpkin, {@link #BULB} across, centred on {@code at}. */
	private static void bulb(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, Vector3f at) {
		cube(buffer, matrix, light, color, at, BULB, 0.5F, 1.0F);
	}

	/** A little cube {@code size} across centred on {@code at}, textured u0 to u1. */
	private static void cube(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, Vector3f at, float size, float u0, float u1) {
		float h = size / 2;
		float x0 = at.x - h;
		float x1 = at.x + h;
		float y0 = at.y - h;
		float y1 = at.y + h;
		float z0 = at.z - h;
		float z1 = at.z + h;
		quad(buffer, matrix, light, color, new Vector3f(0, 0, -1), new Vector3f(x1, y0, z0), new Vector3f(x0, y0, z0), new Vector3f(x0, y1, z0),
				new Vector3f(x1, y1, z0), u0, u1);
		quad(buffer, matrix, light, color, new Vector3f(0, 0, 1), new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1), new Vector3f(x1, y1, z1),
				new Vector3f(x0, y1, z1), u0, u1);
		quad(buffer, matrix, light, color, new Vector3f(-1, 0, 0), new Vector3f(x0, y0, z0), new Vector3f(x0, y0, z1), new Vector3f(x0, y1, z1),
				new Vector3f(x0, y1, z0), u0, u1);
		quad(buffer, matrix, light, color, new Vector3f(1, 0, 0), new Vector3f(x1, y0, z1), new Vector3f(x1, y0, z0), new Vector3f(x1, y1, z0),
				new Vector3f(x1, y1, z1), u0, u1);
		quad(buffer, matrix, light, color, new Vector3f(0, 1, 0), new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1), new Vector3f(x1, y1, z0),
				new Vector3f(x0, y1, z0), u0, u1);
		quad(buffer, matrix, light, color, new Vector3f(0, -1, 0), new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0), new Vector3f(x1, y0, z1),
				new Vector3f(x0, y0, z1), u0, u1);
	}

	/** One quad, its texture from u0 to u1 across the whole height of the strand texture. */
	private static void quad(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, Vector3f normal, Vector3f a, Vector3f b,
			Vector3f c, Vector3f d, float u0, float u1) {
		vertex(buffer, matrix, light, color, normal, a, u0, 1.0F);
		vertex(buffer, matrix, light, color, normal, b, u1, 1.0F);
		vertex(buffer, matrix, light, color, normal, c, u1, 0.0F);
		vertex(buffer, matrix, light, color, normal, d, u0, 0.0F);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, int light, int color, Vector3f normal, Vector3f at, float u, float v) {
		buffer.addVertex(matrix, at.x(), at.y(), at.z()).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(matrix, normal.x(), normal.y(), normal.z());
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
