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
 * A storage tank's screen (right-click its shell), in the industrial look (docs/features/machine-screens.md): the
 * status lamp and the tank's name, with what it is rated for in a chip; a picture of the tank cut away to show the
 * liquid inside at its level (the fluid's own moving texture, the level marked in tenths); then three panes: its
 * contents (what, how much of how much, a fill bar), its flow over the last second (in, out, net and how long until it is
 * full or empty at that rate) and the outlet valve; and a help strip. Everything is read live from the outlet's synced
 * block entity.
 */
public class StorageTankScreen extends Screen {
	static final int W = 352, H = 196;
	static final int AMBER = 0xFFF0B030, WHITE = 0xFFFFFFFF;
	// The tank's paint, as on the real thing: white shell, the yellow and black checker band, dark rim and roof edge.
	static final int SHELL = 0xFFE8EAE6, SHELL_SHADE = 0xFFB8BCB8, SHELL_LIGHT = 0xFFFAFAF8, BAND_Y = 0xFFE2B822, BAND_K = 0xFF1A1A1C,
			RIM = 0xFF363A3E, LINING = 0xFF3C2E28, LINING_DARK = 0xFF2A201C, FOUNDATION = 0xFF9E9E9C;
	/** The readout panes' left edge and width. */
	private static final int READ_X = 136, READ_W = 210;

	private final BlockPos outlet;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("storage_tank");
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
		ScreenKit.body(g, theme, x0, y0, W, H);
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
		int nameRoom = W - 8;
		if (tank != null) {
			// What the tank is rated for, in its rating's colour, in a chip at the right of the title strip.
			io.github.jimbozoomer.jugcraft.nuclear.TankRating rating = tank.rating();
			int colour = switch (rating) {
				case FLAMMABLE -> 0xFFE0605A;
				case CORROSIVE -> 0xFFE8C040;
				case CRYOGENIC -> 0xFF78B8F0;
				default -> 0xFFB8C0C8;
			};
			String rated = t("rating", io.github.jimbozoomer.jugcraft.nuclear.TankUpgrades.name(rating));
			ScreenKit.chip(g, font, theme, x0 + W - 4, y0 + 1, rated, colour);
			nameRoom = W - 24 - font.width(rated);
		}
		boolean formed = tank != null && tank.formed();
		int in = formed ? tank.inRate() : 0, out = formed ? tank.outRate() : 0;
		ScreenKit.title(g, font, theme, x0, y0, Component.literal(name), !formed ? theme.warn() : in > 0 || out > 0 ? theme.ok() : theme.dim(),
				nameRoom);
		if (!formed) {
			ScreenKit.pane(g, theme, x0 + 6, y0 + 15, W - 12, 60);
			MachineSkin.wrap(g, font, Component.translatable("screen.jugcraft.tank.not_formed"), x0 + 12, y0 + 22, W - 24, y0 + 72, theme.warn());
			return;
		}
		long capacity = tank.fluid.capacity() / FluidConstants.BUCKET * 1000;
		FluidVariant variant = tank.fluid.variant;
		long amount = variant.isBlank() ? 0 : tank.fluid.amount * 1000 / FluidConstants.BUCKET;
		float fill = capacity <= 0 ? 0 : Math.min(1, amount / (float) capacity);
		ScreenKit.pane(g, theme, x0 + 6, y0 + 15, 124, 155);
		picture(g, tank, variant, fill, x0 + 10, y0 + 17);
		readouts(g, tank, state, variant, amount, capacity, fill, in, out, x0 + READ_X, y0 + 15);
		ScreenKit.help(g, font, theme, x0 + 6, y0 + 174, W - 12, 16, Component.translatable("screen.jugcraft.tank.hint"));
	}

	/**
	 * The tank cut away: its foundation, the shell (shaded so it reads round) with the checker band and the dark rim
	 * under a low roof, the front opened up to show the inside (the dark lining) and the liquid from the floor to its
	 * level, its surface lighter; the level scale in tenths beside it.
	 */
	private void picture(GuiGraphicsExtractor g, StorageTankPortBlockEntity tank, FluidVariant variant, float fill, int x, int y) {
		int w = 104, h = 150;
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
			g.fill(scale, yy, scale + (k % 5 == 0 ? 6 : 3), yy + 1, theme.dim());
		}
		g.fill(scale - 2, level - 1, scale + 8, level + 1, theme.warn());
		String pct = String.format("%.0f%%", fill * 100);
		g.text(font, pct, (cx0 + cx1) / 2 - font.width(pct) / 2, Math.max(cy0 + 2, level - 10), WHITE, true);
	}

	/** Three panes: what it holds and how much, how fast it is filling and emptying and how long that leaves, the outlet. */
	private void readouts(GuiGraphicsExtractor g, StorageTankPortBlockEntity tank, BlockState state, FluidVariant variant, long amount, long capacity,
			float fill, int in, int out, int x, int y) {
		int w = READ_W - 12;
		// Contents.
		int line = ScreenKit.pane(g, font, theme, x, y, READ_W, 56, t("heading.contents"));
		String fluid = variant.isBlank() || amount <= 0 ? t("empty") : FluidVariantAttributes.getName(variant).getString();
		int fx = x + 6;
		if (!variant.isBlank() && amount > 0) {
			int colour = variant.getFluid().isSame(Fluids.WATER) ? 0xFF3F76E4
					: 0xFF000000 | (Minecraft.getInstance().level != null
					? FluidVariantRendering.getColor(variant, Minecraft.getInstance().level, tank.getBlockPos()) : FluidVariantRendering.getColor(variant));
			g.fill(fx, line, fx + 8, line + 8, colour == 0xFFFFFFFF ? 0xFF9A7A40 : colour);
			fx += 12;
		}
		g.text(font, ScreenKit.fit(font, fluid, x + 6 + w - fx), fx, line, theme.text(), false);
		line += 11;
		String percent = t("fill", String.format("%.1f", fill * 100));
		g.text(font, ScreenKit.fit(font, mb(amount) + " / " + mb(capacity) + " mB", w - font.width(percent) - 4), x + 6, line, theme.text(), false);
		g.text(font, percent, x + 6 + w - font.width(percent), line, theme.dim(), false);
		line += 11;
		ScreenKit.bar(g, theme, x + 6, line, w, 6, fill, theme.energy());
		// Flow.
		int fy = y + 59;
		line = ScreenKit.pane(g, font, theme, x, fy, READ_W, 62, t("heading.flow"));
		ScreenKit.row(g, font, theme, x + 6, line, w, t("in"), "+" + mb(in) + " mB/s", in > 0 ? theme.ok() : theme.dim());
		line += 10;
		ScreenKit.row(g, font, theme, x + 6, line, w, t("out"), "-" + mb(out) + " mB/s", out > 0 ? AMBER : theme.dim());
		line += 10;
		int net = in - out;
		String eta;
		int etaColour = theme.text();
		if (net > 0 && amount < capacity) {
			eta = t("full_in", time((capacity - amount) / net));
			etaColour = theme.ok();
		} else if (net < 0 && amount > 0) {
			eta = t("empty_in", time(amount / -net));
			etaColour = AMBER;
		} else {
			eta = t("steady");
		}
		ScreenKit.row(g, font, theme, x + 6, line, w, t("net"), (net >= 0 ? "+" : "") + mb(net) + " mB/s", theme.text());
		line += 10;
		g.text(font, ScreenKit.fit(font, eta, w), x + 6 + w - font.width(ScreenKit.fit(font, eta, w)), line, etaColour, false);
		// The outlet valve, with a lamp.
		int oy = y + 124;
		line = ScreenKit.pane(g, font, theme, x, oy, READ_W, 31, t("heading.outlet"));
		String valve;
		int valveColour;
		if (state.getValue(StorageTankPortBlock.POWERED)) {
			valve = t("held_shut");
			valveColour = AMBER;
		} else if (state.getValue(StorageTankPortBlock.OPEN)) {
			valve = t("open");
			valveColour = theme.ok();
		} else {
			valve = t("shut");
			valveColour = AMBER;
		}
		MachineSkin.lamp(g, x + 6, line + 1, valveColour);
		g.text(font, ScreenKit.fit(font, valve, w - 10), x + 16, line, valveColour, false);
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
