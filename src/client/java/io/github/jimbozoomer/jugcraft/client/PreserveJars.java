package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.PreserveJarItem;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.item.ItemStack;

/**
 * Jars of preserves standing in a Canning Kettle or on a Pantry Shelf: a glass body in the colour of what is in it (pale
 * when empty), a tin lid, and over a sealed jar's lid a red gingham cloth.
 */
final class PreserveJars {
	private static final RenderType GLASS = RenderTypes.entityCutout(Jugcraft.id("textures/entity/jar_glass.png"));
	private static final RenderType LID = RenderTypes.entityCutout(Jugcraft.id("textures/entity/jar_lid.png"));
	private static final RenderType CLOTH = RenderTypes.entityCutout(Jugcraft.id("textures/entity/jar_cloth.png"));
	private static final int EMPTY = 0xFFE8F0EC;
	private static final int WHITE = 0xFFFFFFFF;

	private PreserveJars() {
	}

	/** Draws {@code jars} standing at the spots {@code spots} ({x, y, z} of each jar's foot centre, in pixels), each {@code width} wide and {@code height} tall. */
	static void submit(SubmitNodeCollector collector, PoseStack pose, List<ItemStack> jars, float[][] spots, float width, float height, int light) {
		int count = Math.min(jars.size(), spots.length);
		if (count == 0) {
			return;
		}
		float h = width / 2;
		collector.submitCustomGeometry(pose, GLASS, (matrix, buffer) -> {
			for (int i = 0; i < count; i++) {
				float[] at = spots[i];
				int color = jars.get(i).getItem() instanceof PreserveJarItem jar ? 0xFF000000 | jar.color : EMPTY;
				TintedBoxes.box(buffer, matrix, at[0] - h, at[1], at[2] - h, at[0] + h, at[1] + height, at[2] + h, color, light);
			}
		});
		collector.submitCustomGeometry(pose, LID, (matrix, buffer) -> {
			for (int i = 0; i < count; i++) {
				float[] at = spots[i];
				TintedBoxes.box(buffer, matrix, at[0] - h + 0.25F, at[1] + height, at[2] - h + 0.25F, at[0] + h - 0.25F, at[1] + height + 0.6F,
						at[2] + h - 0.25F, WHITE, light);
			}
		});
		collector.submitCustomGeometry(pose, CLOTH, (matrix, buffer) -> {
			for (int i = 0; i < count; i++) {
				if (PreserveJarItem.sealed(jars.get(i))) {
					float[] at = spots[i];
					TintedBoxes.box(buffer, matrix, at[0] - h - 0.2F, at[1] + height - 0.6F, at[2] - h - 0.2F, at[0] + h + 0.2F, at[1] + height + 0.9F,
							at[2] + h + 0.2F, WHITE, light);
				}
			}
		});
	}
}
