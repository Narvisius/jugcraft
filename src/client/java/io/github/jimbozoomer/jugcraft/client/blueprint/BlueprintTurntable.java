package io.github.jimbozoomer.jugcraft.client.blueprint;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * The Blueprint Table's turning preview of the selected blueprint: {@link TurntableRaster} draws it on a worker
 * thread about twelve times a second (one turn every 14 seconds) and the newest picture is put in a texture the
 * screen draws. Two texture pixels per GUI pixel, so it stays sharp at the usual GUI scales.
 */
final class BlueprintTurntable implements AutoCloseable {
	private static final Identifier ID = Jugcraft.id("dynamic/blueprint_turntable");
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "Jugcraft blueprint preview");
		thread.setDaemon(true);
		return thread;
	});
	private static final long TURN_MS = 14_000;
	private static final long FRAME_MS = 80;

	private final int guiWidth;
	private final int guiHeight;
	private final TurntableRaster raster;
	private final DynamicTexture texture;
	private String shownId = "";
	private Blueprint pending;
	private CompletableFuture<int[]> job;
	private long lastStart;
	private boolean hasPicture;

	BlueprintTurntable(int guiWidth, int guiHeight) {
		this.guiWidth = guiWidth;
		this.guiHeight = guiHeight;
		this.raster = new TurntableRaster(guiWidth * 2, guiHeight * 2);
		this.texture = new DynamicTexture(() -> "jugcraft blueprint preview", guiWidth * 2, guiHeight * 2, true);
		Minecraft.getInstance().getTextureManager().register(ID, texture);
	}

	/** Draws the blueprint's preview with its top left at x, y (the picture lags a frame when it changes). */
	void draw(GuiGraphicsExtractor g, Blueprint blueprint, int x, int y) {
		if (!blueprint.id.equals(shownId)) {
			shownId = blueprint.id;
			pending = blueprint;
			hasPicture = false;
		}
		long now = System.currentTimeMillis();
		if (job != null && job.isDone()) {
			int[] argb = job.getNow(null);
			job = null;
			if (argb != null) {
				NativeImage image = texture.getPixels();
				int w = raster.width();
				for (int i = 0; i < argb.length; i++) {
					image.setPixel(i % w, i / w, argb[i]);
				}
				texture.upload();
				hasPicture = true;
			}
		}
		if (job == null && (pending != null || now - lastStart >= FRAME_MS)) {
			lastStart = now;
			Blueprint changed = pending;
			pending = null;
			int[][] blocks = changed == null ? null : blocks(changed);
			double yaw = 0.6 + (now % TURN_MS) * (Math.PI * 2) / TURN_MS;
			job = CompletableFuture.supplyAsync(() -> {
				if (blocks != null) {
					raster.setBlocks(blocks[0], blocks[1]);
				}
				raster.draw(yaw, Math.toRadians(28));
				return raster.pixels().clone();
			}, WORKER);
		}
		if (hasPicture) {
			g.blit(ID, x, y, x + guiWidth, y + guiHeight, 0f, 1f, 0f, 1f);
		}
	}

	/** The blueprint's blocks as positions and map colours (glass and other colourless blocks go pale blue). */
	private static int[][] blocks(Blueprint blueprint) {
		java.util.Collection<Blueprint.Cell> cells = blueprint.rawCells();
		int[] xyz = new int[cells.size() * 3];
		int[] colours = new int[cells.size()];
		var level = Minecraft.getInstance().level;
		int n = 0;
		for (Blueprint.Cell cell : cells) {
			if (cell.state().isAir()) {
				continue;
			}
			xyz[n * 3] = cell.offset().getX();
			xyz[n * 3 + 1] = cell.offset().getY();
			xyz[n * 3 + 2] = cell.offset().getZ();
			int colour = level == null ? 0 : cell.state().getMapColor(level, BlockPos.ZERO).col;
			colours[n] = colour == 0 ? 0xA0C8F0 : colour;
			n++;
		}
		return new int[][] {java.util.Arrays.copyOf(xyz, n * 3), java.util.Arrays.copyOf(colours, n)};
	}

	@Override
	public void close() {
		if (job != null) {
			job.cancel(false);
		}
		Minecraft.getInstance().getTextureManager().release(ID);
	}
}
