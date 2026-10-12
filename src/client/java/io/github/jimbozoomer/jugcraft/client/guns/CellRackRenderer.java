package io.github.jimbozoomer.jugcraft.client.guns;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.guns.CellRackBlock;
import io.github.jimbozoomer.jugcraft.guns.CellRackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Energy Cells standing in a Cell Rack's six cradles (slice 10E), each upright in its cradle's cup and facing
 * out of the rack's front, on its two shelves. The cradles are where tools/guns.py builds them.
 */
public class CellRackRenderer implements BlockEntityRenderer<CellRackBlockEntity, CellRackRenderer.State> {
	/** The cradles' columns, in pixels of the north-facing model, as CellRackBlock places them (left to right from the front). */
	private static final float[] COLUMNS = {12.5F, 8.0F, 3.5F};
	/** The middle of each shelf's cells' icons, in pixels up: a cell's foot stands half a pixel down in its cup. */
	private static final float[] ROWS = {5.25F, 12.25F};
	/** How far in from the rack's front the cells stand, in pixels: just before their contacts. */
	private static final float DEPTH = 8.5F;
	/** A cell's icon, 16 pixels across, drawn this size: the cell stands about 5 pixels tall. */
	private static final float SCALE = 0.35F;
	/** The icon draws the cell leaning at 45 degrees; turned this much, it stands upright, its terminal at the top. */
	private static final float UPRIGHT = -45.0F;
	private final ItemModelResolver itemModels;

	public static final class State extends BlockEntityRenderState {
		final ItemStackRenderState[] cells = new ItemStackRenderState[CellRackBlockEntity.SLOTS];
		Direction facing = Direction.NORTH;

		State() {
			for (int i = 0; i < cells.length; i++) {
				cells[i] = new ItemStackRenderState();
			}
		}
	}

	public CellRackRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CellRackBlockEntity rack, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(rack, state, crumbling);
		state.facing = rack.getBlockState().getValue(CellRackBlock.FACING);
		for (int slot = 0; slot < CellRackBlockEntity.SLOTS; slot++) {
			itemModels.updateForTopItem(state.cells[slot], rack.getItem(slot), ItemDisplayContext.FIXED, rack.getLevel(), null,
					(int) rack.getBlockPos().asLong() + slot);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int slot = 0; slot < CellRackBlockEntity.SLOTS; slot++) {
			ItemStackRenderState cell = state.cells[slot];
			if (cell.isEmpty()) {
				continue;
			}
			pose.pushPose();
			// Turn the north-facing layout to the rack's facing, as its block model is turned.
			pose.translate(0.5F, 0.0F, 0.5F);
			pose.rotateDegrees(Axis.YP, -yRotation(state.facing));
			// Its icon, shown as an item frame shows one, faces out of the rack's front.
			pose.translate(COLUMNS[slot % 3] / 16.0F - 0.5F, ROWS[slot / 3] / 16.0F, DEPTH / 16.0F - 0.5F);
			pose.rotateDegrees(Axis.ZP, UPRIGHT);
			pose.scale(SCALE, SCALE, SCALE);
			cell.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}

	private static float yRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}
}
