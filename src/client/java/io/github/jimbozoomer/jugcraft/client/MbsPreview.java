package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentItem;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry;
import io.github.jimbozoomer.jugcraft.nuclear.TankKitItem;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import io.github.jimbozoomer.jugcraft.placement.StructureReach;
import io.github.jimbozoomer.jugcraft.powerline.LineItem;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import io.github.jimbozoomer.jugcraft.powerline.LinePlacement;
import io.github.jimbozoomer.jugcraft.powerline.LineRegistry;
import io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The placement preview for every MBS placed from one item (Justin, October 8 2026: "I want the MBS to also show what
 * it would look like when you put it down"; asked, he chose a see-through copy of the real thing). While such an item
 * is in the main hand and aimed at a block, a faint, see-through copy of exactly what it would place stands where it
 * would go, facing the way it would face, worked out the way the item itself places:
 *
 * <ul>
 * <li>a storage tank kit: the whole tank as it stands once formed (painted in its courses), foundation and all;
 * <li>a piece of plant equipment: the equipment as its renderer draws it;
 * <li>a power line support or box: the structure turned along its line, its legs down to the ground, and the wire it
 * would hang to the structure it would be wired to (the same one the server picks);
 * <li>multi-block furniture (wall screens, tables, panels, racks), the round table and multi-block machines: their
 * blocks' own models.
 * </ul>
 *
 * <p>It is tinted pale blue where it fits; where something is in the way it turns red and each cell in the way is
 * marked with a red box. Blueprints keep their own preview (client/blueprint/BlueprintPlacementPreview.java).
 * Replaces the power lines' old box preview.
 */
public final class MbsPreview {
	/** The see-through copy: the real textures, faded and tinted (ARGB, multiplied into each vertex's colour). */
	static final int CLEAR = 0x9CC4DCFF;
	static final int BLOCKED = 0x9CFF6A5A;
	/** A cell in the way. */
	private static final int IN_THE_WAY = 0x90FF4030;
	private static final RenderType BOXES = RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/misc/blueprint_ghost.png"));
	private static final RenderType BLOCKS = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	private static final RenderType EQUIPMENT = RenderTypes.entityTranslucentCull(NuclearRenderer.TEXTURE);
	private static final RenderType LINES = RenderTypes.entityTranslucentCull(LineRenderer.TEXTURE);
	/**
	 * Each block of a block-model copy is drawn this much larger than its cell, about its middle, so none of its faces
	 * lies in the plane of a face of the world (the foundation's top in the ground's top): no z-fighting.
	 */
	private static final float GROW = 1.004F;
	/** How often (ticks) what is in the way is looked at again while aiming at the same spot. */
	private static final int REFRESH = 10;
	private static final int MAX_BOXES = 400;

	private static @Nullable Cached cached;

	private MbsPreview() {
	}

	/** What was worked out for one aim: the copy (block models) and the cells in the way. */
	private record Cached(Object key, long tick, @Nullable Mesh mesh, List<BlockPos> blocked, @Nullable Object extra) {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			Level level = minecraft.level;
			if (player == null || level == null || player.isSpectator()) {
				return;
			}
			ItemStack held = player.getMainHandItem();
			Item item = held.getItem();
			StructureGhost.Source source = item instanceof BlockItem blockItem ? blockSource(blockItem) : null;
			if (!(item instanceof TankKitItem || item instanceof EquipmentItem || item instanceof LineItem
					|| source != null)) {
				return;
			}
			// The block under the crosshair; further out than the crosshair reaches (64 blocks), the block looked at
			// within the item's full placing reach, where using it places it (StructureReach).
			BlockHitResult hit = minecraft.hitResult instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK ? b
					: StructureReach.farTarget(player, held, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			if (hit == null) {
				return;
			}
			Vec3 camera = context.levelState().cameraRenderState.pos;
			PoseStack pose = context.poseStack();
			SubmitNodeCollector collector = context.submitNodeCollector();
			if (item instanceof TankKitItem kit) {
				tank(level, player, hit, kit, camera, pose, collector);
			} else if (item instanceof EquipmentItem equipment) {
				equipment(level, player, hit, equipment.kind, camera, pose, collector);
			} else if (item instanceof LineItem line) {
				line(level, hit, line.kind, camera, pose, collector);
			} else if (source != null) {
				blocks(level, player, hit, held, source, camera, pose, collector);
			}
		});
	}

	/** Existing MBS sources retain their own preview; the tower console adapts its exact six-part layout. */
	static StructureGhost.@Nullable Source blockSource(BlockItem item) {
		if (item.getBlock() instanceof StructureGhost.Source source) return source;
		if (item.getBlock() instanceof MasterConsoleBlock console) return place -> masterConsoleGhost(place, console);
		return null;
	}
	private static StructureGhost masterConsoleGhost(BlockPlaceContext place, MasterConsoleBlock console) {
		Direction facing = place.getHorizontalDirection().getOpposite();
		BlockPos origin = place.getClickedPos();
		List<StructureGhost.Cell> cells = new ArrayList<>(MasterConsoleBlock.FOOTPRINT.size());
		for (int part = 0; part < MasterConsoleBlock.FOOTPRINT.size(); part++) {
			BlockPos at = MasterConsoleBlock.FOOTPRINT.partPos(origin, facing, part);
			cells.add(new StructureGhost.Cell(at, console.defaultBlockState().setValue(MasterConsoleBlock.FACING, facing).setValue(MasterConsoleBlock.PART, part)));
		}
		return new StructureGhost(List.copyOf(cells), console.blockedPlacement(place));
	}

	// ------------------------------------------------------------------ the kinds of MBS

	/** A tank kit: its stake on the ground clicked (TankKitItem.useOn), turned to face the player. */
	private static void tank(Level level, Player player, BlockHitResult hit, TankKitItem kit, Vec3 camera, PoseStack pose, SubmitNodeCollector collector) {
		BlockPos clicked = hit.getBlockPos();
		BlockPos stake = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.above();
		Rotation rotation = BlueprintItem.rotationFor(player.getDirection());
		Blueprint blueprint = TankKitItem.blueprint(kit.kit);
		TankKitItem.Formed formed = TankKitItem.formed(kit.kit, rotation);
		if (blueprint == null || formed == null) {
			return;
		}
		Object key = List.of(kit.kit, rotation, stake);
		Cached c = refresh(level, key, () -> {
			List<StructureGhost.Cell> cells = new ArrayList<>(formed.cells().size());
			for (Blueprint.Cell cell : formed.cells()) {
				cells.add(new StructureGhost.Cell(cell.offset(), cell.state()));
			}
			return new Cached(key, level.getGameTime(), Mesh.ofBlocks(level, stake, cells, formed.inside()),
					TankKitItem.blocked(level, kit.kit, stake, rotation), null);
		});
		draw(collector, pose, camera, stake, BLOCKS, (p, consumer) -> c.mesh.replay(p, consumer, tint(c.blocked)));
		boxes(collector, pose, camera, c.blocked);
	}

	/** Plant equipment: its anchor on the face clicked, facing the way the player looks (EquipmentItem.useOn). */
	private static void equipment(Level level, Player player, BlockHitResult hit, EquipmentKind kind, Vec3 camera, PoseStack pose,
			SubmitNodeCollector collector) {
		BlockPos clicked = hit.getBlockPos();
		if (EquipmentRegistry.of(level).anchorOf(clicked) != null && !player.isSecondaryUseActive()) {
			return;   // aiming at equipment: the click is for it
		}
		BlockPos anchor = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		Direction facing = player.getDirection();
		Object key = List.of(kind, anchor, facing);
		Cached c = refresh(level, key, () -> {
			List<BlockPos> blocked = new ArrayList<>();
			if (!EquipmentPlacement.fits(level, anchor, kind, facing)) {
				if (level.isOutsideBuildHeight(anchor) || !level.getBlockState(anchor).canBeReplaced()) {
					blocked.add(anchor);
				}
				for (BlockPos offset : kind.frames(facing)) {
					BlockPos p = anchor.offset(offset);
					if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
						blocked.add(p);
					}
				}
			}
			return new Cached(key, level.getGameTime(), null, blocked, null);
		});
		int tint = tint(c.blocked);
		draw(collector, pose, camera, anchor, EQUIPMENT, (p, consumer) -> NuclearRenderer.drawGhost(kind, facing, p, new Tinted(consumer, tint)));
		boxes(collector, pose, camera, c.blocked);
	}

	/** A power line support or box: on its footing (LinePlacement), wired to the structure the server would pick. */
	private static void line(Level level, BlockHitResult hit, LineKind kind, Vec3 camera, PoseStack pose, SubmitNodeCollector collector) {
		BlockPos clicked = hit.getBlockPos();
		if (LineRegistry.of(level).anchorOf(clicked) != null) {
			return;   // aiming at a structure: using the item there upgrades it instead
		}
		BlockPos at = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
		Object key = List.of(kind, at);
		Cached c = refresh(level, key, () -> {
			BlockPos footing = LinePlacement.footing(level, at, kind);
			boolean fits = footing != null && LinePlacement.fits(level, footing, kind, null);
			BlockPos anchor = footing == null ? at : footing;
			BlockPos partner = fits ? LinePlacement.autoPartner(level, anchor, kind) : null;
			return new Cached(key, level.getGameTime(), null, fits ? List.of() : List.of(anchor), new BlockPos[] {anchor, partner});
		});
		BlockPos[] found = (BlockPos[]) c.extra;
		BlockPos anchor = found[0], partner = found[1];
		int tint = tint(c.blocked);
		draw(collector, pose, camera, anchor, LINES,
				(p, consumer) -> LineRenderer.drawGhost(level, anchor, kind, partner, p, new Tinted(consumer, tint)));
	}

	/** Multi-block furniture, the round table, multi-block machines: placed as their block places them. */
	private static void blocks(Level level, Player player, BlockHitResult hit, ItemStack held, StructureGhost.Source source, Vec3 camera,
			PoseStack pose, SubmitNodeCollector collector) {
		BlockPlaceContext place = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held, hit);
		BlockPos origin = place.getClickedPos();
		Object key = List.of(held.getItem(), origin, player.getDirection(), hit.getDirection());
		Cached c = refresh(level, key, () -> {
			StructureGhost ghost = source.ghost(place);
			if (ghost == null) {
				return new Cached(key, level.getGameTime(), null, List.of(), null);
			}
			List<StructureGhost.Cell> cells = new ArrayList<>(ghost.cells().size());
			for (StructureGhost.Cell cell : ghost.cells()) {
				cells.add(new StructureGhost.Cell(cell.pos().subtract(origin), cell.state()));
			}
			return new Cached(key, level.getGameTime(), Mesh.ofBlocks(level, origin, cells, Set.of()), ghost.blocked(), null);
		});
		if (c.mesh == null) {
			return;
		}
		draw(collector, pose, camera, origin, BLOCKS, (p, consumer) -> c.mesh.replay(p, consumer, tint(c.blocked)));
		boxes(collector, pose, camera, c.blocked);
	}

	// ------------------------------------------------------------------ drawing

	private static Cached refresh(Level level, Object key, java.util.function.Supplier<Cached> work) {
		Cached c = cached;
		if (c == null || !Objects.equals(c.key, key) || level.getGameTime() - c.tick >= REFRESH || level.getGameTime() < c.tick) {
			c = work.get();
			cached = c;
		}
		return c;
	}

	private static int tint(List<BlockPos> blocked) {
		return blocked.isEmpty() ? CLEAR : BLOCKED;
	}

	private static void draw(SubmitNodeCollector collector, PoseStack pose, Vec3 camera, BlockPos at, RenderType type,
			SubmitNodeCollector.CustomGeometryRenderer drawing) {
		pose.pushPose();
		pose.translate(at.getX() - camera.x, at.getY() - camera.y, at.getZ() - camera.z);
		collector.submitCustomGeometry(pose, type, drawing);
		pose.popPose();
	}

	/** A red box in each cell that is in the way. */
	private static void boxes(SubmitNodeCollector collector, PoseStack pose, Vec3 camera, List<BlockPos> blocked) {
		if (blocked.isEmpty()) {
			return;
		}
		BlockPos origin = blocked.get(0);
		List<BlockPos> shown = blocked.size() > MAX_BOXES ? blocked.subList(0, MAX_BOXES) : blocked;
		draw(collector, pose, camera, origin, BOXES, (p, consumer) -> {
			for (BlockPos b : shown) {
				float x = b.getX() - origin.getX(), y = b.getY() - origin.getY(), z = b.getZ() - origin.getZ();
				cube(p, consumer, x + 0.08F, y + 0.08F, z + 0.08F, x + 0.92F, y + 0.92F, z + 0.92F, IN_THE_WAY);
			}
		});
	}

	private static void cube(PoseStack.Pose pose, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz, int color) {
		quad(pose, c, color, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0);
		quad(pose, c, color, ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz, 0, -1, 0);
		quad(pose, c, color, ax, ay, az, ax, by, az, bx, by, az, bx, ay, az, 0, 0, -1);
		quad(pose, c, color, bx, ay, bz, bx, by, bz, ax, by, bz, ax, ay, bz, 0, 0, 1);
		quad(pose, c, color, ax, ay, bz, ax, by, bz, ax, by, az, ax, ay, az, -1, 0, 0);
		quad(pose, c, color, bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz, 1, 0, 0);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer c, int color, float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz) {
		vertex(pose, c, color, x0, y0, z0, 0, 0, nx, ny, nz);
		vertex(pose, c, color, x1, y1, z1, 0, 1, nx, ny, nz);
		vertex(pose, c, color, x2, y2, z2, 1, 1, nx, ny, nz);
		vertex(pose, c, color, x3, y3, z3, 1, 0, nx, ny, nz);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer c, int color, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		c.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}

	/** {@code a} times {@code b}, channel by channel (ARGB). */
	static int multiply(int a, int b) {
		int alpha = (a >>> 24) * (b >>> 24) / 255, r = (a >> 16 & 255) * (b >> 16 & 255) / 255;
		int g = (a >> 8 & 255) * (b >> 8 & 255) / 255, bl = (a & 255) * (b & 255) / 255;
		return alpha << 24 | r << 16 | g << 8 | bl;
	}

	// ------------------------------------------------------------------ geometry

	/**
	 * Block models baked into one see-through mesh, from an origin block: each block's quads (its own model, as the
	 * world draws it), less the faces the world would not draw (against a whole block of the copy, the tank's space
	 * inside, or a solid block of the world), each block grown by {@link #GROW}.
	 */
	static final class Mesh {
		private float[] data = new float[9 * 1024];
		private int size;

		static Mesh ofBlocks(Level level, BlockPos origin, List<StructureGhost.Cell> cells, Set<BlockPos> inside) {
			Minecraft minecraft = Minecraft.getInstance();
			Map<BlockPos, BlockState> at = new HashMap<>();
			for (StructureGhost.Cell cell : cells) {
				at.put(cell.pos(), cell.state());
			}
			Mesh mesh = new Mesh();
			List<BlockStateModelPart> parts = new ArrayList<>();
			for (StructureGhost.Cell cell : cells) {
				BlockState state = cell.state();
				if (state.isAir()) {
					continue;
				}
				BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
				parts.clear();
				BlockPos world = origin.offset(cell.pos());
				model.collectParts(RandomSource.create(Mth.getSeed(world)), parts);
				for (BlockStateModelPart part : parts) {
					for (Direction side : Direction.values()) {
						if (hidden(level, origin, at, inside, cell.pos(), side)) {
							continue;
						}
						for (BakedQuad quad : part.getQuads(side)) {
							mesh.quad(quad, cell.pos(), colour(minecraft, state, quad));
						}
					}
					for (BakedQuad quad : part.getQuads(null)) {
						mesh.quad(quad, cell.pos(), colour(minecraft, state, quad));
					}
				}
			}
			return mesh;
		}

		/** Whether the face of block {@code pos} toward {@code side} is out of sight. */
		private static boolean hidden(Level level, BlockPos origin, Map<BlockPos, BlockState> at, Set<BlockPos> inside, BlockPos pos, Direction side) {
			BlockPos next = pos.relative(side);
			if (inside.contains(next)) {
				return true;
			}
			BlockState there = at.get(next);
			if (there != null) {
				return there.isSolidRender();
			}
			return level.getBlockState(origin.offset(next)).isSolidRender();
		}

		/** A tinted face's colour (grass, leaves), as away from any biome; white otherwise. */
		private static int colour(Minecraft minecraft, BlockState state, BakedQuad quad) {
			if (!quad.materialInfo().isTinted()) {
				return 0xFFFFFFFF;
			}
			BlockTintSource source = minecraft.getBlockColors().getTintSource(state, quad.materialInfo().tintIndex());
			return source == null ? 0xFFFFFFFF : 0xFF000000 | source.color(state);
		}

		private void quad(BakedQuad quad, BlockPos cell, int colour) {
			Direction d = quad.direction();
			for (int i = 0; i < 4; i++) {
				Vector3fc p = quad.position(i);
				long uv = quad.packedUV(i);
				put(cell.getX() + 0.5F + (p.x() - 0.5F) * GROW, cell.getY() + 0.5F + (p.y() - 0.5F) * GROW, cell.getZ() + 0.5F + (p.z() - 0.5F) * GROW,
						UVPair.unpackU(uv), UVPair.unpackV(uv), d.getStepX(), d.getStepY(), d.getStepZ(), colour);
			}
		}

		private void put(float x, float y, float z, float u, float v, float nx, float ny, float nz, int colour) {
			if (size + 9 > data.length) {
				data = java.util.Arrays.copyOf(data, data.length * 2);
			}
			data[size++] = x;
			data[size++] = y;
			data[size++] = z;
			data[size++] = u;
			data[size++] = v;
			data[size++] = nx;
			data[size++] = ny;
			data[size++] = nz;
			data[size++] = Float.intBitsToFloat(colour);
		}

		void replay(PoseStack.Pose pose, VertexConsumer consumer, int tint) {
			for (int i = 0; i < size; i += 9) {
				int colour = multiply(Float.floatToRawIntBits(data[i + 8]), tint);
				consumer.addVertex(pose, data[i], data[i + 1], data[i + 2]).setColor(colour).setUv(data[i + 3], data[i + 4])
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
			}
		}
	}

	/** Passes geometry on with every vertex's colour multiplied by a tint (the renderers' drawing, faded). */
	static final class Tinted implements VertexConsumer {
		private final VertexConsumer to;
		private final int tint;

		Tinted(VertexConsumer to, int tint) {
			this.to = to;
			this.tint = tint;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			to.addVertex(x, y, z);
			return this;
		}

		@Override
		public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
			to.addVertex(x, y, z, multiply(color, tint), u, v, overlay, light, nx, ny, nz);
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			to.setColor(multiply(a << 24 | r << 16 | g << 8 | b, tint));
			return this;
		}

		@Override
		public VertexConsumer setColor(int argb) {
			to.setColor(multiply(argb, tint));
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			to.setUv(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			to.setUv1(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			to.setUv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv3(float u, float v) {
			to.setUv3(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			to.setNormal(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			to.setLineWidth(width);
			return this;
		}
	}
}
