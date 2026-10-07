package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank's screen (right-click its shell): a picture of the tank cut away to show the liquid inside at its
 * level (the fluid's own moving texture, the level marked in tenths), what it holds and how much (mB, of how much), how
 * much came in and went out over the last second, how long until it is full or empty at that rate, and the outlet
 * valve. Everything is read live from the outlet's synced block entity.
 */
public class StorageTankScreen extends Screen {
	static final int W = 352, H = 196;
	static final int BG = 0xF00E1820, PANEL = 0xF0132029, EDGE = 0xFF2B5566, ACCENT = 0xFF4FC8E8, TEXT = 0xFFBCD4DC, DIM = 0xFF6F8A94,
			GREEN = 0xFF5ADC78, AMBER = 0xFFF0B030, RED = 0xFFF04034, WHITE = 0xFFFFFFFF;
	// The tank's paint, as on the real thing: white shell, the yellow and black checker band, dark rim and roof edge.
	static final int SHELL = 0xFFE8EAE6, SHELL_SHADE = 0xFFB8BCB8, SHELL_LIGHT = 0xFFFAFAF8, BAND_Y = 0xFFE2B822, BAND_K = 0xFF1A1A1C,
			RIM = 0xFF363A3E, LINING = 0xFF3C2E28, LINING_DARK = 0xFF2A201C, FOUNDATION = 0xFF9E9E9C;

	private final BlockPos outlet;
	private int left, top;

	public StorageTankScreen(BlockPos outlet) {
		super(Component.translatable("screen.jugcraft.tank.title"));
		this.outlet = outlet;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private @Nullable StorageTankPortBlockEntity tank() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.getBlockEntity(outlet) instanceof StorageTankPortBlockEntity t ? t : null;
	}

	private static String t(String key, Object... args) {
		return Component.translatable("screen.jugcraft.tank." + key, args).getString();
	}

	private static String mb(long mb) {
		return String.format("%,d", mb);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int x0 = left, y0 = top;
		g.fill(x0, y0, x0 + W, y0 + H, BG);
		g.outline(x0, y0, W, H, EDGE);
		g.fill(x0, y0 + 20, x0 + W, y0 + 21, ACCENT);
		StorageTankPortBlockEntity tank = tank();
		BlockState state = tank == null ? null : tank.getBlockState();
		String name = t("title");
		if (state != null && state.getBlock() instanceof StorageTankOutletBlock && state.getValue(StorageTankOutletBlock.SERVICE)
				!= StorageTankOutletBlock.Service.NONE) {
			String key = "tank.jugcraft." + state.getValue(StorageTankOutletBlock.SERVICE).getSerializedName();
			name = Component.translatable(key + ".name").getString() + "  " + Component.translatable(key + ".tag").getString();
		}
		if (tank != null && !tank.title().isEmpty()) {
			name = tank.title();
		}
		g.text(font, name, x0 + 8, y0 + 6, ACCENT, false);
		if (tank != null) {
			// What the tank is rated for, in its rating's colour.
			io.github.jimbozoomer.jugcraft.nuclear.TankRating rating = tank.rating();
			int colour = switch (rating) {
				case FLAMMABLE -> 0xFFE0605A;
				case CORROSIVE -> 0xFFE8C040;
				case CRYOGENIC -> 0xFF78B8F0;
				default -> 0xFFB8C0C8;
			};
			g.text(font, Component.translatable("screen.jugcraft.tank.rating", io.github.jimbozoomer.jugcraft.nuclear.TankUpgrades.name(rating)),
					x0 + 8, y0 + 17, colour, false);
		}
		if (tank == null || !tank.formed()) {
			g.text(font, t("not_formed"), x0 + 8, y0 + 40, AMBER, false);
			return;
		}
		long capacity = tank.fluid.capacity() / FluidConstants.BUCKET * 1000;
		FluidVariant variant = tank.fluid.variant;
		long amount = variant.isBlank() ? 0 : tank.fluid.amount * 1000 / FluidConstants.BUCKET;
		float fill = capacity <= 0 ? 0 : Math.min(1, amount / (float) capacity);
		picture(g, tank, variant, fill, x0 + 10, y0 + 28);
		readouts(g, tank, state, variant, amount, capacity, fill, x0 + 136, y0 + 30);
	}

	/**
	 * The tank cut away: its foundation, the shell (shaded so it reads round) with the checker band and the dark rim
	 * under a low roof, the front opened up to show the inside (the dark lining) and the liquid from the floor to its
	 * level, its surface lighter; the level scale in tenths beside it.
	 */
	private void picture(GuiGraphicsExtractor g, StorageTankPortBlockEntity tank, FluidVariant variant, float fill, int x, int y) {
		int w = 104, h = 150;
		g.fill(x - 2, y, x + w + 16, y + h + 6, PANEL);
		int sx0 = x + 6, sx1 = x + w - 6, roofTop = y + 10, wallTop = y + 18, base = y + h - 6;
		// The roof, low and wide, its rim dark; the handrail on it.
		g.fill(sx0 + 6, roofTop, sx1 - 6, roofTop + 3, SHELL_SHADE);
		g.fill(sx0 + 2, roofTop + 3, sx1 - 2, wallTop - 3, SHELL);
		g.fill(sx0, wallTop - 3, sx1, wallTop, RIM);
		g.fill(sx0 + 4, roofTop - 6, sx0 + 5, roofTop, BAND_Y);
		g.fill(sx1 - 5, roofTop - 6, sx1 - 4, roofTop, BAND_Y);
		g.fill(sx0 + 4, roofTop - 6, sx1 - 4, roofTop - 5, BAND_Y);
		// The shell: shaded at both sides, lit down the middle.
		int sw = sx1 - sx0;
		for (int i = 0; i < sw; i++) {
			double u = (i + 0.5) / sw * 2 - 1;
			double light = Math.sqrt(Math.max(0, 1 - u * u));
			int c = lerp(SHELL_SHADE, i < sw / 2 ? SHELL_LIGHT : SHELL, light);
			g.fill(sx0 + i, wallTop, sx0 + i + 1, base, c);
		}
		// The checker band round the top course.
		for (int k = 0; k * 6 < sw; k++) {
			for (int r = 0; r < 2; r++) {
				g.fill(sx0 + k * 6, wallTop + r * 4, Math.min(sx1, sx0 + k * 6 + 6), wallTop + r * 4 + 4, (k + r) % 2 == 0 ? BAND_Y : BAND_K);
			}
		}
		// The foundation.
		g.fill(sx0 - 4, base, sx1 + 4, base + 5, FOUNDATION);
		// The cut-away: the lining, then the liquid from the floor to its level.
		int cx0 = sx0 + 12, cx1 = sx1 - 12, cy0 = wallTop + 12, cy1 = base - 4;
		g.fill(cx0 - 2, cy0 - 2, cx1 + 2, cy1 + 2, SHELL_SHADE);
		g.fillGradient(cx0, cy0, cx1, cy1, LINING_DARK, LINING);
		int level = cy1 - Math.round(fill * (cy1 - cy0));
		if (fill > 0 && !variant.isBlank()) {
			TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
					.get(variant.getFluid().defaultFluidState()).stillMaterial().sprite();
			int colour = 0xFF000000 | (Minecraft.getInstance().level != null
					? FluidVariantRendering.getColor(variant, Minecraft.getInstance().level, tank.getBlockPos()) : FluidVariantRendering.getColor(variant));
			g.enableScissor(cx0, level, cx1, cy1);
			for (int ty = cy1 - 16; ty > level - 16; ty -= 16) {
				for (int tx = cx0; tx < cx1; tx += 16) {
					g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, colour);
				}
			}
			g.disableScissor();
			// Its surface catching the light.
			g.fill(cx0, level, cx1, level + 1, 0x90FFFFFF);
		}
		// The level scale in tenths, and the level itself.
		int scale = sx1 + 4;
		for (int k = 0; k <= 10; k++) {
			int yy = cy1 - Math.round(k / 10F * (cy1 - cy0));
			g.fill(scale, yy, scale + (k % 5 == 0 ? 6 : 3), yy + 1, DIM);
		}
		g.fill(scale - 2, level - 1, scale + 8, level + 1, RED);
		String pct = String.format("%.0f%%", fill * 100);
		g.text(font, pct, (cx0 + cx1) / 2 - font.width(pct) / 2, Math.max(cy0 + 2, level - 10), WHITE, true);
	}

	/** What it holds, how much, how fast it is filling and emptying, how long that leaves, and the outlet valve. */
	private void readouts(GuiGraphicsExtractor g, StorageTankPortBlockEntity tank, BlockState state, FluidVariant variant, long amount, long capacity,
			float fill, int x, int y) {
		g.fill(x - 4, y - 2, x + 208, y + 156, PANEL);
		int line = y + 4;
		g.text(font, t("fluid"), x + 2, line, DIM, false);
		String fluid = variant.isBlank() || amount <= 0 ? t("empty") : FluidVariantAttributes.getName(variant).getString();
		if (!variant.isBlank() && amount > 0) {
			int colour = variant.getFluid().isSame(Fluids.WATER) ? 0xFF3F76E4
					: 0xFF000000 | (Minecraft.getInstance().level != null
					? FluidVariantRendering.getColor(variant, Minecraft.getInstance().level, tank.getBlockPos()) : FluidVariantRendering.getColor(variant));
			g.fill(x + 54, line, x + 62, line + 8, colour == 0xFFFFFFFF ? 0xFF9A7A40 : colour);
		}
		g.text(font, fluid, x + (variant.isBlank() || amount <= 0 ? 54 : 66), line, WHITE, false);
		line += 18;
		g.text(font, t("contents"), x + 2, line, DIM, false);
		g.text(font, mb(amount) + " mB", x + 54, line, WHITE, false);
		line += 11;
		g.text(font, t("of", mb(capacity), mb(capacity / 1000)), x + 54, line, DIM, false);
		line += 11;
		g.text(font, t("fill", String.format("%.1f", fill * 100)), x + 54, line, TEXT, false);
		line += 18;
		int in = tank.inRate(), out = tank.outRate();
		g.text(font, t("in"), x + 2, line, DIM, false);
		g.text(font, "+" + mb(in) + " mB/s", x + 54, line, in > 0 ? GREEN : DIM, false);
		line += 11;
		g.text(font, t("out"), x + 2, line, DIM, false);
		g.text(font, "-" + mb(out) + " mB/s", x + 54, line, out > 0 ? RED : DIM, false);
		line += 11;
		int net = in - out;
		String eta;
		int etaColour = TEXT;
		if (net > 0 && amount < capacity) {
			eta = t("full_in", time((capacity - amount) / net));
			etaColour = GREEN;
		} else if (net < 0 && amount > 0) {
			eta = t("empty_in", time(amount / -net));
			etaColour = AMBER;
		} else {
			eta = t("steady");
		}
		g.text(font, t("net"), x + 2, line, DIM, false);
		g.text(font, (net >= 0 ? "+" : "") + mb(net) + " mB/s", x + 54, line, TEXT, false);
		line += 11;
		g.text(font, eta, x + 54, line, etaColour, false);
		line += 18;
		g.text(font, t("outlet"), x + 2, line, DIM, false);
		String valve;
		int valveColour;
		if (state.getValue(StorageTankPortBlock.POWERED)) {
			valve = t("held_shut");
			valveColour = AMBER;
		} else if (state.getValue(StorageTankPortBlock.OPEN)) {
			valve = t("open");
			valveColour = GREEN;
		} else {
			valve = t("shut");
			valveColour = AMBER;
		}
		g.text(font, valve, x + 54, line, valveColour, false);
		line += 14;
		g.text(font, t("hint"), x + 2, line, DIM, false);
	}

	/** Seconds as "1h 4m", "3m 20s" or "45s". */
	private static String time(long seconds) {
		if (seconds >= 3600) {
			return seconds / 3600 + "h " + seconds % 3600 / 60 + "m";
		}
		if (seconds >= 60) {
			return seconds / 60 + "m " + seconds % 60 + "s";
		}
		return seconds + "s";
	}

	private static int lerp(int a, int b, double t) {
		int r = (int) Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
		int gr = (int) Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
		int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | r << 16 | gr << 8 | bl;
	}
}
