package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * A pumpkin worn as a head, as the Scarecrow and the Harvest Effigy wear one: any pumpkin item drawn as its model, and a
 * hand-carved one as the plain pumpkin it was carved from with its own carving drawn over it (as
 * {@link CarvedPumpkinRenderer} draws the block, in its own glow, at full brightness when lit). Part of a render state.
 */
final class CarvedHead {
	/** A block item drawn FIXED is half a block. */
	private static final float FIXED = 0.5F;
	/** How far outside the pumpkin the carving sits, in pumpkin widths. */
	private static final float OUT = 0.004F;

	final ItemStackRenderState pumpkin = new ItemStackRenderState();
	PumpkinCarving carving = PumpkinCarving.BLANK;
	@Nullable RenderType carvingType;
	int carvingLight;

	/** Reads {@code head} for drawing; {@code light} is the light round it. */
	void extract(ItemModelResolver itemModels, ItemStack head, @Nullable Level level, int seed, int light) {
		carvingType = null;
		ItemStack drawn = head;
		if (head.getItem() instanceof BlockItem item && item.getBlock() instanceof CarvedPumpkinBlock) {
			// The block is modelled as the plain pumpkin with the carving drawn over it: do the same.
			Block plain = JugcraftAgriculture.plainPumpkin(item.getBlock());
			drawn = plain == null ? ItemStack.EMPTY : new ItemStack(plain);
			carving = head.getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK);
			boolean lit = ScarecrowBlockEntity.lit(head);
			if (!carving.isBlank()) {
				carvingType = CarvingTextures.get(carving, lit, ScarecrowBlockEntity.soul(head), CarvingTextures.Glow.of(head));
				carvingLight = lit ? LightCoordsUtil.FULL_BRIGHT : light;
			}
		}
		itemModels.updateForTopItem(pumpkin, drawn, ItemDisplayContext.FIXED, level, null, seed);
	}

	/** Draws the head {@code size} blocks a side centred on the pose's origin, its face turned {@code facing}. */
	void submit(PoseStack pose, SubmitNodeCollector collector, Direction facing, float size, int light) {
		pose.pushPose();
		if (!pumpkin.isEmpty()) {
			pose.pushPose();
			pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(facing));
			pose.scale(size / FIXED, size / FIXED, size / FIXED);
			pumpkin.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		RenderType type = carvingType;
		if (type != null) {
			// The carving's sides are named in the world, so it is drawn unturned over a cube the head's size.
			PumpkinCarving drawn = carving;
			int glow = carvingLight;
			float scale = size * (1.0F + 2.0F * OUT);
			pose.scale(scale, scale, scale);
			pose.translate(-0.5F, -0.5F, -0.5F);
			collector.submitCustomGeometry(pose, type, (matrix, buffer) -> {
				for (int face = 0; face < PumpkinCarving.FACES; face++) {
					if (!drawn.isBlank(face)) {
						CarvedPumpkinRenderer.side(buffer, matrix, PumpkinCarving.side(facing, face), face, glow);
					}
				}
			});
		}
		pose.popPose();
	}
}
