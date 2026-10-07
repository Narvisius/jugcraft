package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Farm Stand's goods and its chalk, from its first block: in each of its six crates up to three of what it
 * holds, heaped (one if it holds one, two for a few, three for more); each crate's price chalked on the slate tag on its
 * front; and the owner's name on the slate header board over its awning. Positions are in pixels for a stand facing north
 * whose second block lies to the west, x measured from the seam between its blocks (tools/decor20.py FARM_STAND).
 */
public class FarmStandRenderer implements BlockEntityRenderer<FarmStandBlockEntity, FarmStandRenderer.State> {
	/** Each crate's middle (x, floor y, z), front row first: the front row on the table, the back row on the riser. */
	static final float[][] CRATES = {{10.0F, 8.5F, 4.5F}, {0.0F, 8.5F, 4.5F}, {-10.0F, 8.5F, 4.5F}, {10.0F, 13.5F, 11.5F}, {0.0F, 13.5F, 11.5F},
			{-10.0F, 13.5F, 11.5F}};
	/**
	 * Where each row's price tags hang (the front row's on the table's apron, the back row's on their crates' fronts): the
	 * tags' middle height and their faces' distance from the north side.
	 */
	static final float[][] TAGS = {{6.4F, 0.4F}, {14.8F, 7.4F}};
	/** The slate header board's middle (x, y) and its face's distance from the north side, and its width, in pixels. */
	static final float[] BOARD = {0.0F, 26.8F, 0.2F, 12.0F};
	/** Where the copies of the goods lie in a crate (pixels from its middle), and their size. */
	private static final float[][] HEAP = {{-1.8F, 0.6F, -0.8F}, {1.9F, 0.6F, 0.9F}, {0.0F, 1.6F, 0.2F}};
	private static final float GOODS = 0.42F;
	private static final int CHALK = 0xFFEDEBE0;
	/** The chalk's tallest letters, in pixels, and how wide a tag's chalk may run. */
	private static final float TAG_TEXT = 2.4F;
	private static final float TAG_WIDTH = 7.0F;
	private static final float BOARD_TEXT = 2.6F;

	private final ItemModelResolver itemModels;
	private final Font font;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		final ItemStackRenderState[] goods = new ItemStackRenderState[FarmStandBlockEntity.CRATES * HEAP.length];
		final String[] prices = new String[FarmStandBlockEntity.CRATES];
		String owner = "";

		State() {
			for (int i = 0; i < goods.length; i++) {
				goods[i] = new ItemStackRenderState();
			}
		}
	}

	public FarmStandRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
		this.font = Minecraft.getInstance().font;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** How many copies of a crate's goods to heap in it. */
	static int shown(int count) {
		return count <= 0 ? 0 : count == 1 ? 1 : count <= 8 ? 2 : 3;
	}

	@Override
	public void extractRenderState(FarmStandBlockEntity stand, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(stand, state, crumbling);
		BlockState block = stand.getBlockState();
		if (!(block.getBlock() instanceof FarmStandBlock)) {
			return;
		}
		state.facing = block.getValue(FarmStandBlock.FACING);
		state.owner = stand.ownerName();
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			ItemStack goods = stand.crate(crate);
			int shown = shown(goods.getCount());
			for (int copy = 0; copy < HEAP.length; copy++) {
				itemModels.updateForTopItem(state.goods[crate * HEAP.length + copy], copy < shown ? goods : ItemStack.EMPTY, ItemDisplayContext.FIXED,
						stand.getLevel(), null, (int) stand.getBlockPos().asLong() + crate * HEAP.length + copy);
			}
			state.prices[crate] = goods.isEmpty() ? "" : FarmStandScreen.shortPrice(stand.price(crate));
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		// Into the stand's frame: facing north, x from the seam (west of the first block's east edge by a block).
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(0.0F, 0.0F, -0.5F);
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			float[] at = CRATES[crate];
			for (int copy = 0; copy < HEAP.length; copy++) {
				ItemStackRenderState goods = state.goods[crate * HEAP.length + copy];
				if (goods.isEmpty()) {
					continue;
				}
				float[] lie = HEAP[copy];
				pose.pushPose();
				pose.translate((at[0] + lie[0] - 8.0F) / 16.0F, (at[1] + lie[1]) / 16.0F, (at[2] + lie[2]) / 16.0F);
				pose.rotateDegrees(Axis.YP, 180.0F + (copy - 1) * 18.0F);
				pose.rotateDegrees(Axis.XP, -62.0F);
				pose.scale(GOODS, GOODS, GOODS);
				goods.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
				pose.popPose();
			}
			float[] tag = TAGS[crate / 3];
			chalk(pose, collector, state.prices[crate], at[0] - 8.0F, tag[0], tag[1], TAG_WIDTH, TAG_TEXT, state.lightCoords);
		}
		chalk(pose, collector, state.owner, BOARD[0] - 8.0F, BOARD[1], BOARD[2], BOARD[3] - 1.0F, BOARD_TEXT, state.lightCoords);
		pose.popPose();
	}

	/**
	 * Chalks {@code text} centred at ({@code x}, {@code y}) pixels on a face {@code z} pixels from the north side, facing
	 * north, its letters at most {@code height} pixels tall and the line at most {@code width} pixels wide.
	 */
	private void chalk(PoseStack pose, SubmitNodeCollector collector, String text, float x, float y, float z, float width, float height,
			int light) {
		if (text.isEmpty()) {
			return;
		}
		FormattedCharSequence line = Component.literal(text).getVisualOrderText();
		int wide = Math.max(1, font.width(line));
		float scale = Math.min(height / 8.0F, width / wide) / 16.0F;
		pose.pushPose();
		pose.translate(x / 16.0F, y / 16.0F, z / 16.0F - 0.002F);
		pose.rotateDegrees(Axis.YP, 180.0F);
		pose.scale(scale, -scale, scale);
		collector.submitText(pose, -wide / 2.0F, -4.0F, line, false, Font.DisplayMode.POLYGON_OFFSET, light, CHALK, 0, 0);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The stand is two blocks wide.
	}
}
