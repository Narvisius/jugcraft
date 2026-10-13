package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;

/**
 * Drawing food lying on the Farmhouse Kitchen's blocks (the stove's hob, the skillet's pan, the cutting board), laid out
 * for a block facing north and turned with the block the way its blockstate turns its model.
 */
final class KitchenDraw {
	private KitchenDraw() {
	}

	/** Quarter turns clockwise (seen from above) from north to {@code facing}, as a blockstate turns a model. */
	static int turns(Direction facing) {
		return switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	/**
	 * Draws {@code item} lying flat with its middle at ({@code x}, {@code y}, {@code z}) pixels in a block facing north,
	 * turned to {@code facing}, {@code scale} of a whole item across and turned {@code spin} degrees where it lies.
	 */
	static void flat(PoseStack pose, SubmitNodeCollector collector, ItemStackRenderState item, Direction facing, float x, float y, float z,
			float scale, float spin, int light) {
		int turns = turns(facing);
		for (int i = 0; i < turns; i++) {
			float nx = 16 - z;
			z = x;
			x = nx;
		}
		pose.pushPose();
		pose.translate(x / 16, y / 16, z / 16);
		pose.rotateDegrees(Axis.YP, spin - 90.0F * turns);
		pose.rotateDegrees(Axis.XP, 90.0F);
		pose.scale(scale, scale, scale);
		item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}
}
