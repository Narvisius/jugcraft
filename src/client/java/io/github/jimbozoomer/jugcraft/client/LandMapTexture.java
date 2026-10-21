package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * The Land tab's map, drawn like a vanilla map: one pixel per block, map colours, hills shaded by the block to the north
 * and water darkening with depth. It reads only chunks this client already has (no loading), and paints a few rows each
 * frame so opening the tab never stutters. Unknown ground is left as blank map paper.
 */
final class LandMapTexture {
	static final int PAPER = 0xFFD6C49B;
	private static final Identifier ID = Jugcraft.id("dynamic/exchange_land_map");
	private static final int ROWS_PER_FRAME = 24;
	private static final int REFRESH_MS = 5_000;
	private static LandMapTexture instance;

	private final int chunks;
	private final int size;
	private final DynamicTexture texture;
	private final int[] heights;
	private long centre = Long.MIN_VALUE;
	private int nextRow = Integer.MAX_VALUE;
	private long finishedAt;

	private LandMapTexture(int chunks) {
		this.chunks = chunks;
		this.size = chunks * 16;
		this.texture = new DynamicTexture(() -> "Jugcraft Exchange land map", size, size, true);
		this.heights = new int[size];
		Minecraft.getInstance().getTextureManager().register(ID, texture);
	}

	static LandMapTexture get(int chunks) {
		if (instance == null || instance.chunks != chunks) {
			instance = new LandMapTexture(chunks);
		}
		return instance;
	}

	/** Draws the map with its top left at x, y, squeezed into {@code drawSize} GUI pixels. */
	void draw(GuiGraphicsExtractor g, int centreX, int centreZ, int x, int y, int drawSize) {
		long key = (centreX & 0xFFFFFFFFL) | ((long) centreZ << 32);
		long now = System.currentTimeMillis();
		if (key != centre) {
			centre = key;
			restart();
		} else if (nextRow >= size && now - finishedAt > REFRESH_MS) {
			restart(); // repaint now and then so new builds show up
		}
		if (nextRow < size) {
			paintRows(centreX, centreZ);
			if (nextRow >= size) {
				finishedAt = now;
			}
		}
		g.blit(ID, x, y, x + drawSize, y + drawSize, 0f, 1f, 0f, 1f);
	}

	private void restart() {
		nextRow = 0;
		java.util.Arrays.fill(heights, Integer.MIN_VALUE);
	}

	private void paintRows(int centreX, int centreZ) {
		ClientLevel level = Minecraft.getInstance().level;
		NativeImage image = texture.getPixels();
		if (image == null) {
			return;
		}
		int half = chunks / 2;
		int x0 = (centreX - half) * 16;
		int z0 = (centreZ - half) * 16;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int end = Math.min(size, nextRow + ROWS_PER_FRAME);
		for (int row = nextRow; row < end; row++) {
			int z = z0 + row;
			for (int col = 0; col < size; col++) {
				int x = x0 + col;
				int colour = PAPER;
				int height = Integer.MIN_VALUE;
				if (level != null && level.hasChunk(x >> 4, z >> 4)) {
					BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos.set(x, 0, z));
					pos.set(x, top.getY() - 1, z);
					BlockState state = level.getBlockState(pos);
					MapColor mapColour = state.getMapColor(level, pos);
					for (int i = 0; i < 16 && mapColour == MapColor.NONE && pos.getY() > level.getMinY(); i++) {
						pos.move(0, -1, 0);
						state = level.getBlockState(pos);
						mapColour = state.getMapColor(level, pos);
					}
					height = pos.getY();
					if (mapColour == MapColor.WATER) {
						int depth = 0;
						while (depth < 12 && !state.getFluidState().isEmpty() && pos.getY() > level.getMinY()) {
							pos.move(0, -1, 0);
							state = level.getBlockState(pos);
							depth++;
						}
						double shade = depth * 0.1 + ((x + z) & 1) * 0.2;
						colour = shade(mapColour.col, shade < 0.5 ? 255 : shade > 0.9 ? 180 : 220);
					} else if (mapColour != MapColor.NONE) {
						int north = heights[col];
						int brightness = 220;
						if (row > 0 && north != Integer.MIN_VALUE) {
							double slope = (height - north) * 0.8 + (((x + z) & 1) - 0.5) * 0.4;
							brightness = slope > 0.6 ? 255 : slope < -0.6 ? 180 : 220;
						}
						colour = shade(mapColour.col, brightness);
					}
				}
				heights[col] = height;
				image.setPixel(col, row, colour);
			}
		}
		nextRow = end;
		texture.upload();
	}

	private static int shade(int rgb, int brightness) {
		int r = ((rgb >> 16) & 0xFF) * brightness / 255;
		int gr = ((rgb >> 8) & 0xFF) * brightness / 255;
		int b = (rgb & 0xFF) * brightness / 255;
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}
}
