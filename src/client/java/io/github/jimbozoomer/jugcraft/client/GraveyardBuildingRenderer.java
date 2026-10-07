package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * The inscriptions of a graveyard building (pack 3): drawn as the headstones' are, but kept by the block entity of the
 * building's part 0 and cut up to a few blocks from it (the family name over a mausoleum's door, its crypt fronts), so
 * they are drawn even when that one block is out of view.
 */
public class GraveyardBuildingRenderer extends HeadstoneRenderer {
	public GraveyardBuildingRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
