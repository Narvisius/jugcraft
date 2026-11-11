package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlock;
import io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlockEntity;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * What the Advanced Crafting Table shows live (crafting/AdvancedCraftingTableBlockEntity; take 2, approved 26 October
 * 2026): the items in its 36 grid slots lying flat on their cells of the lit glass, cell for cell as on the screen; the
 * result turning slowly over the grid under the holo emitter, a drone as a see-through cyan hologram of its own model
 * (other results as themselves) in a faint beam; the status lamp lit green while the grid makes something; and the
 * touchscreen: the result's name and READY, or NO MATCH, with a map of the filled cells. Client only: the table never
 * ticks, it sends what it holds when that changes. Places are pixels in the table's structure as it faces north
 * (tools/adv_crafting_table.py LIVE), turned with it.
 */
public final class AdvancedCraftingTableRenderer implements BlockEntityRenderer<AdvancedCraftingTableBlockEntity, AdvancedCraftingTableRenderer.State> {
	// tools/adv_crafting_table.py: GX, GZ, GC, GRID_Y; EMITTER, LENS_Y; LAMP; SCREEN; TOP
	private static final float GX = 10.0F, GZ = 1.2F, GC = 2.0F, GRID_Y = 14.5F, TOP = 14.8F;
	private static final float EMITTER_X = 16.0F, EMITTER_Z = 7.2F, LENS_Y = 28.2F;
	private static final float LAMP_X = 24.0F, LAMP_Y = 13.25F, LAMP_Z = 0.0F - 0.12F;
	private static final float SCREEN_X0 = 33.7F, SCREEN_Y0 = 23.8F, SCREEN_X1 = 46.3F, SCREEN_Y1 = 30.2F, SCREEN_Z = 13.9F;
	/** The screen's page: 116 units across its glass (a unit in pixels). */
	private static final float UNIT = (SCREEN_X1 - SCREEN_X0) / 116F;
	/** An item lying on a cell: 1.7 of the cell's 2 pixels (the fixed view draws an item a block across). */
	private static final float ITEM_SCALE = 1.7F / 16F;
	/** The hologram's reach over the grid (blocks) and how high its underside floats over the top (pixels). */
	private static final float HOLO_REACH = 0.36F, HOLO_LIFT = 3.0F;
	private static final double TURN_SECONDS = 12.0;
	private static final RenderType HOLO = RenderTypes.entityTranslucentEmissive(DroneMesh.TEXTURE);
	private static final RenderType PAINT = RenderTypes.entityCutout(DroneMesh.TEXTURE);
	private static final float[] WHITE = {DroneMesh.WHITE_U, DroneMesh.WHITE_V, DroneMesh.WHITE_U, DroneMesh.WHITE_V};
	private static final int CYAN = 0xFF46D2F0, WHITE_TEXT = 0xFFE2F6FF, GREY = 0xFF78909A, GREEN = 0xFF6EF08C;

	private final ItemModelResolver itemModels;
	private final Font font;

	public AdvancedCraftingTableRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
		this.font = context.font();
	}

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] items = new ItemStackRenderState[AdvancedCraftingTableBlockEntity.SLOTS];
		final boolean[] filled = new boolean[AdvancedCraftingTableBlockEntity.SLOTS];
		final ItemStackRenderState result = new ItemStackRenderState();
		int turns;
		int count;
		boolean ready;
		int droneTier;
		int made;
		String name = "";
		double time;

		State() {
			for (int i = 0; i < items.length; i++) {
				items[i] = new ItemStackRenderState();
			}
		}
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** The table is three blocks wide; its anchor may be off screen while the rest is not. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 48;
	}

	@Override
	public void extractRenderState(AdvancedCraftingTableBlockEntity table, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(table, state, crumbling);
		BlockState block = table.getBlockState();
		Direction facing = block.getBlock() instanceof AdvancedCraftingTableBlock piece ? piece.facingOf(block) : Direction.NORTH;
		state.turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		state.count = 0;
		for (int i = 0; i < state.items.length; i++) {
			ItemStack stack = table.getItem(i);
			state.filled[i] = !stack.isEmpty();
			if (state.filled[i]) {
				state.count++;
				itemModels.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED, table.getLevel(), null,
						(int) table.getBlockPos().asLong() + i);
			} else {
				state.items[i].clear();
			}
		}
		ItemStack made = table.result();
		state.ready = !made.isEmpty();
		state.made = made.getCount();
		state.droneTier = 0;
		state.name = "";
		state.result.clear();
		if (state.ready) {
			state.name = made.getHoverName().getString().toUpperCase(Locale.ROOT);
			state.droneTier = droneTier(made);
			if (state.droneTier == 0) {
				itemModels.updateForTopItem(state.result, made, ItemDisplayContext.FIXED, table.getLevel(), null, (int) table.getBlockPos().asLong());
			}
		}
		state.time = table.getLevel() == null ? 0 : (table.getLevel().getGameTime() + partialTick) / 20.0;
	}

	/** A drone's tier (jugcraft:drone_t1 to drone_t9), or 0. */
	private static int droneTier(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String path = id.getPath();
		if (id.getNamespace().equals(Jugcraft.MOD_ID) && path.length() == 8 && path.startsWith("drone_t")) {
			int tier = path.charAt(7) - '0';
			return tier >= 1 && tier <= 9 ? tier : 0;
		}
		return 0;
	}

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		poses.pushPose();
		poses.translate(0.5, 0, 0.5);
		// a quarter clockwise seen from above takes (x, z) to (-z, x), as the blockstate turns the models
		poses.mulPose(new Matrix4f().rotationY((float) (-Math.PI / 2 * state.turns)));
		poses.translate(-0.5, 0, -0.5);
		grid(state, poses, collector);
		if (state.ready) {
			hologram(state, poses, collector);
			lamp(poses, collector);
		}
		screen(state, poses, collector);
		poses.popPose();
	}

	/** The grid's items lying on their cells: column 0 the user's left (+x as the table faces north), row 0 the far row. */
	private static void grid(State state, PoseStack poses, SubmitNodeCollector collector) {
		int side = 6;
		for (int i = 0; i < state.items.length; i++) {
			if (!state.filled[i]) {
				continue;
			}
			int col = i % side, row = i / side;
			float x = GX + side * GC - (col + 0.5F) * GC;
			float z = GZ + side * GC - (row + 0.5F) * GC;
			// laid flat face up, its top away from the user (+z), as a plate's food lies
			poses.pushPose();
			poses.translate(x / 16F, (GRID_Y + 0.12F) / 16F, z / 16F);
			poses.rotateDegrees(Axis.XP, 90.0F);
			poses.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
			state.items[i].submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poses.popPose();
		}
	}

	/** The result over the grid, turning: a drone's own model in see-through cyan, anything else as itself; the beam. */
	private static void hologram(State state, PoseStack poses, SubmitNodeCollector collector) {
		double angle = state.time / TURN_SECONDS * Math.PI * 2;
		float flicker = (float) (0.5 + 0.06 * Math.sin(state.time * 7.3) + 0.03 * Math.sin(state.time * 23.0));
		float bottom = (TOP + HOLO_LIFT) / 16F;
		float top = bottom;
		if (state.droneTier > 0) {
			DroneMesh.Part[] parts = DroneMesh.get(state.droneTier, DroneMesh.PARKED);
			float[] reach = DroneMesh.reach(state.droneTier);
			float[] middle = DroneMesh.middle(state.droneTier);
			if (parts != null && reach != null) {
				float scale = Math.min(0.75F, HOLO_REACH / Math.max(0.05F, reach[0]));
				top = bottom + reach[1] * scale;
				int alpha = Math.round(flicker * 255);
				collector.submitCustomGeometry(poses, HOLO, (pose, consumer) -> {
					DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
					g.translate(EMITTER_X / 16F, bottom, EMITTER_Z / 16F).yaw(angle).scale(scale).translate(-middle[0], 0, -middle[1]);
					for (DroneMesh.Part part : parts) {
						float[] pos = part.pos();
						byte[] n = part.normal();
						int[] color = part.color();
						for (int t = 0; t + 2 < color.length; t += 3) {
							for (int k = 0; k < 4; k++) {
								int v = t + Math.min(k, 2);
								g.meshVertex(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2], DroneMesh.WHITE_U, DroneMesh.WHITE_V, n[v * 3] / 127f,
										n[v * 3 + 1] / 127f, n[v * 3 + 2] / 127f, holo(color[v], alpha), LightCoordsUtil.FULL_BRIGHT);
							}
						}
					}
				});
			}
		} else if (!state.result.isEmpty()) {
			top = bottom + 0.3F;
			poses.pushPose();
			poses.translate(EMITTER_X / 16F, bottom + 0.15F, EMITTER_Z / 16F);
			poses.rotate(Axis.YP, (float) angle);
			poses.scale(0.32F, 0.32F, 0.32F);
			state.result.submit(poses, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
			poses.popPose();
		}
		float beamTop = LENS_Y / 16F, beamBottom = top + 0.02F;
		collector.submitCustomGeometry(poses, HOLO, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = LightCoordsUtil.FULL_BRIGHT;
			g.color = 0x1446D2F0;
			float cx = EMITTER_X / 16F, cz = EMITTER_Z / 16F, r0 = 0.8F / 16F, r1 = 3.2F / 16F;
			for (int k = 0; k < 8; k++) {
				double a0 = Math.PI * 2 * k / 8, a1 = Math.PI * 2 * (k + 1) / 8;
				float ax = cx + r0 * (float) Math.cos(a0), az = cz + r0 * (float) Math.sin(a0);
				float bx = cx + r0 * (float) Math.cos(a1), bz = cz + r0 * (float) Math.sin(a1);
				float qx = cx + r1 * (float) Math.cos(a1), qz = cz + r1 * (float) Math.sin(a1);
				float dx = cx + r1 * (float) Math.cos(a0), dz = cz + r1 * (float) Math.sin(a0);
				float mx = (float) Math.cos((a0 + a1) / 2), mz = (float) Math.sin((a0 + a1) / 2);
				g.face(ax, beamTop, az, bx, beamTop, bz, qx, beamBottom, qz, dx, beamBottom, dz, mx, 0, mz,
						WHITE[0], WHITE[1], WHITE[0], WHITE[1], WHITE[0], WHITE[1], WHITE[0], WHITE[1]);
			}
		});
	}

	/** A mesh colour as hologram: its lightness in cyan, see-through. */
	private static int holo(int argb, int alpha) {
		float l = (0.3F * (argb >> 16 & 0xFF) + 0.59F * (argb >> 8 & 0xFF) + 0.11F * (argb & 0xFF)) / 255F;
		float f = Math.min(1.25F, 0.55F + 0.7F * l);
		int r = Math.min(255, Math.round(90 * f)), g = Math.min(255, Math.round(225 * f)), b = Math.min(255, Math.round(255 * f));
		return alpha << 24 | r << 16 | g << 8 | b;
	}

	/** The status lamp lit green on its lens (dark while the grid makes nothing). */
	private static void lamp(PoseStack poses, SubmitNodeCollector collector) {
		collector.submitCustomGeometry(poses, PAINT, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = LightCoordsUtil.FULL_BRIGHT;
			g.color = GREEN;
			float h = 0.28F / 16F;
			// 0.1 in front of the lens (the two never share a plane)
			g.box(LAMP_X / 16F - h, LAMP_Y / 16F - h, (LAMP_Z - 0.18F) / 16F, LAMP_X / 16F + h, LAMP_Y / 16F + h, (LAMP_Z - 0.10F) / 16F, WHITE);
		});
	}

	/**
	 * The touchscreen's page (116 units across its glass, y down from its top, as the user sees it): the header, the
	 * result's name and READY, or NO MATCH (or GRID EMPTY), the number of items, and a map of the grid's filled cells.
	 */
	private void screen(State state, PoseStack poses, SubmitNodeCollector collector) {
		poses.pushPose();
		// the page's top-left as seen from the front (its +x the user's right: -x as the table faces north), toward the user
		poses.translate(SCREEN_X1 / 16F, SCREEN_Y1 / 16F, (SCREEN_Z - 0.1F) / 16F);       // 0.1 in front of the glass
		poses.rotateDegrees(Axis.YP, 180);
		float k = UNIT / 16F;
		poses.scale(k, -k, k);
		final boolean ready = state.ready;
		final int count = state.count;
		final boolean[] filled = state.filled.clone();
		collector.submitCustomGeometry(poses, PAINT, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			g.light = LightCoordsUtil.FULL_BRIGHT;
			rect(g, 0, 0, 116, 9, 0xFF0E2834);
			if (ready) {
				rect(g, 3, 40, 52, 54, 0xFF6EF08C);
				rect(g, 4, 41, 51, 53, 0xFF1A6E3C);
			} else {
				rect(g, 3, 40, 52, 54, 0xFF46545C);
				rect(g, 4, 41, 51, 53, 0xFF162026);
			}
			for (int j = 0; j < 6; j++) {
				for (int i = 0; i < 6; i++) {
					boolean on = filled[j * 6 + i];
					int c = on ? (ready ? 0xFF3CC8EC : 0xFF466E7E) : 0xFF142630;
					rect(g, 67 + i * 7.5F, 12 + j * 7.5F, 67 + i * 7.5F + 6.5F, 12 + j * 7.5F + 6.5F, c);
				}
			}
		});
		poses.translate(0, 0, 0.9F);         // the words over the boxes (toward the user)
		text(poses, collector, "RESULT", 3, 1.5F, 0.8F, CYAN, false);
		text(poses, collector, "AF-6", 113, 1.5F, 0.8F, CYAN, true);
		if (ready) {
			List<FormattedCharSequence> lines = font.split(Component.literal(state.name), 82);
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				sequence(poses, collector, lines.get(i), 3, 12 + i * 8.5F, 0.75F, WHITE_TEXT);
			}
			text(poses, collector, "x" + state.made + "   " + count + (count == 1 ? " ITEM" : " ITEMS"), 3, 31, 0.65F, WHITE_TEXT, false);
			text(poses, collector, "READY", 27.5F, 43.5F, 0.85F, 0xFFE6FFEC, false, true);
		} else {
			text(poses, collector, count == 0 ? "GRID EMPTY" : "NO MATCH", 3, 13, 0.85F, GREY, false);
			text(poses, collector, count + (count == 1 ? " ITEM" : " ITEMS"), 3, 31, 0.65F, GREY, false);
			text(poses, collector, "IDLE", 27.5F, 43.5F, 0.85F, GREY, false, true);
		}
		poses.popPose();
	}

	private static void rect(DroneDepotRenderer.Geometry g, float u0, float v0, float u1, float v1, int argb) {
		g.color = argb;
		g.box(u0, v0, -0.05, u1, v1, 0.05, WHITE);
	}

	private void text(PoseStack poses, SubmitNodeCollector collector, String s, float u, float v, float scale, int colour, boolean right) {
		text(poses, collector, s, u, v, scale, colour, right, false);
	}

	private void text(PoseStack poses, SubmitNodeCollector collector, String s, float u, float v, float scale, int colour, boolean right,
			boolean centre) {
		float w = font.width(s) * scale;
		float at = right ? u - w : centre ? u - w / 2 : u;
		sequence(poses, collector, Texts.of(s), at, v, scale, colour);
	}

	private static void sequence(PoseStack poses, SubmitNodeCollector collector, FormattedCharSequence s, float u, float v, float scale, int colour) {
		poses.pushPose();
		poses.translate(u, v, 0);
		poses.scale(scale, scale, scale);
		collector.submitText(poses, 0, 0, s, false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
		poses.popPose();
	}
}
