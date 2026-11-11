package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.TowerData;
import io.github.jimbozoomer.jugcraft.tower.TowerPlacement;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The Drone Tower Core's placement warning (tower/TowerPlacement; the approved mockup tower_core_warning_mockup): in the
 * tower screen's colours, a hazard band with WARNING, "Be sure this is the location you want to select for your Drone
 * Tower. It is an extremely large structure.", a plan of the finished tower (every column of it, shaded by height, the
 * core in red) beside its width, length and height (worked out from the tower's own data, so they follow the design),
 * how far it reaches each way from the core and where the core would stand. CANCEL (or Esc) places nothing; PLACE TOWER
 * HERE, live after a second so a double click can't place it, has the server place the core.
 */
public class TowerPlaceScreen extends Screen {
	private static final int WIDTH = 300, HEIGHT = 226;
	private static final int FRAME = 0xFF3A2422, GLASS = 0xF0141012, PANEL = 0xFF1E1A1C;
	private static final int ACCENT = 0xFFE05040, DIM = 0xFFC28E84, WHITE = 0xFFECE6E6, AMBER = 0xFFFFC04A;
	private static final int YELLOW = 0xFFF2C21B, BLACK = 0xFF141012, RED = 0xFFFF5A3C;
	/** The plan's shades: a column's top up to each height (blocks above the core), darkest lowest. */
	private static final int[] SHADE_TOP = {30, 80, 165, Integer.MAX_VALUE};
	private static final int[] SHADE = {0xFF5A4020, 0xFF8A6028, 0xFFC08A34, 0xFFFFB84A};
	/** PLACE TOWER HERE stays greyed this long after the warning opens (ticks). */
	private static final int ARM_TICKS = 20;
	/** The plan as runs of one shade along x: {z, x0, x1 (exclusive), colour}, relative to the extent's corner. */
	private static List<int[]> runs;
	private final int id;
	private final BlockPos core;
	private final TowerData.Extent size;
	private int left, top, age;
	private boolean answered;
	private Button place;

	public TowerPlaceScreen(int id, BlockPos core) {
		super(Component.translatable("screen.jugcraft.tower_place.title"));
		this.id = id;
		this.core = core;
		this.size = TowerData.get(TowerData.CURRENT_LAYOUT).extent();
	}

	/** Opens the warning when the server asks (client entry point). */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(TowerPlacement.Ask.TYPE,
				(payload, context) -> Minecraft.getInstance().gui.setScreen(new TowerPlaceScreen(payload.id(), payload.core())));
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int bw = (WIDTH - 16) / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower_place.cancel"), button -> onClose())
				.bounds(left + 6, top + HEIGHT - 28, bw, 20).build());
		place = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.tower_place.place"), button -> answer(true))
				.bounds(left + WIDTH - 6 - bw, top + HEIGHT - 28, bw, 20).build());
		place.active = age >= ARM_TICKS;
	}

	@Override
	public void tick() {
		super.tick();
		age++;
		if (place != null) {
			place.active = age >= ARM_TICKS;
		}
	}

	/** Whether PLACE TOWER HERE is live yet (client tests). */
	public boolean armed() {
		return age >= ARM_TICKS;
	}

	/** PLACE TOWER HERE, as the button does once it is live (client tests). */
	public void placeTower() {
		if (armed()) {
			answer(true);
		}
	}

	private void answer(boolean yes) {
		if (!answered) {
			answered = true;
			ClientPlayNetworking.send(new TowerPlacement.Answer(id, yes));
		}
		if (yes) {
			// the server places it and opens the core's loading screen in this one's place
			Minecraft.getInstance().gui.setScreen(null);
		}
	}

	@Override
	public void onClose() {
		answer(false);
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, FRAME);
		g.fill(left, top, left + WIDTH, top + HEIGHT, GLASS);
		for (int[] c : new int[][] {{left, top}, {left + WIDTH - 10, top}, {left, top + HEIGHT - 2}, {left + WIDTH - 10, top + HEIGHT - 2}}) {
			g.fill(c[0], c[1], c[0] + 10, c[1] + 2, ACCENT);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		// the title
		g.fill(left + 8, top + 6, left + 11, top + 15, ACCENT);
		g.text(font, Component.translatable("screen.jugcraft.tower_place.title").getString(), left + 14, top + 7, ACCENT, false);
		String sub = Component.translatable("screen.jugcraft.tower_place.sub").getString();
		g.text(font, sub, left + WIDTH - 8 - font.width(sub), top + 7, DIM, false);
		// the hazard band, WARNING on a plate in the middle
		int bx0 = left + 6, bx1 = left + WIDTH - 6, by0 = top + 21, by1 = top + 41;
		g.fill(bx0, by0, bx1, by1, BLACK);
		for (int y = by0; y < by1; y++) {
			for (int x = bx0 - 12 + (y - by0) % 12; x < bx1; x += 12) {
				g.fill(Math.max(bx0, x), y, Math.min(bx1, x + 6), y + 1, YELLOW);
			}
		}
		String warning = Component.translatable("screen.jugcraft.tower_place.warning").getString();
		int pw = font.width(warning) * 2 + 16;
		int px0 = (bx0 + bx1 - pw) / 2;
		g.fill(px0 - 1, by0, px0 + pw + 1, by1, ACCENT);
		g.fill(px0, by0 + 1, px0 + pw, by1 - 1, BLACK);
		g.pose().pushMatrix();
		g.pose().translate(px0 + 8, by0 + 3);
		g.pose().scale(2F, 2F);
		g.text(font, warning, 0, 0, RED, false);
		g.pose().popMatrix();
		// the message
		int y = top + 47;
		for (FormattedCharSequence line : font.split(Component.translatable("screen.jugcraft.tower_place.message"), WIDTH - 16)) {
			g.text(font, line, left + 8, y, WHITE, false);
			y += 10;
		}
		// the plan
		int mx = left + 6, my = top + 79, mw = size.width() + 8, mh = size.length() + 8;
		g.fill(mx, my, mx + mw, my + mh, PANEL);
		for (int[] r : runs(size)) {
			g.fill(mx + 4 + r[1], my + 4 + r[0], mx + 4 + r[2], my + 5 + r[0], r[3]);
		}
		int ox = mx + 4 - size.minX(), oz = my + 4 - size.minZ();
		g.fill(ox - 2, oz - 2, ox + 3, oz + 3, 0xFF000000);
		g.fill(ox - 1, oz - 1, ox + 2, oz + 2, ACCENT);
		g.text(font, "N", mx + mw - 9, my + 3, WHITE, false);
		g.fill(mx + mw - 7, my + 12, mx + mw - 6, my + 17, WHITE);
		// the numbers
		int x = mx + mw + 7;
		y = my + 1;
		g.text(font, Component.translatable("screen.jugcraft.tower_place.finished").getString(), x, y, AMBER, false);
		y += 13;
		y = dimension(g, x, y, "width", size.width(), "east_west");
		y = dimension(g, x, y, "length", size.length(), "north_south");
		y = dimension(g, x, y, "height", size.height(), null);
		g.text(font, Component.translatable("screen.jugcraft.tower_place.up_down", size.maxY(), -size.minY()).getString(), x, y, DIM, false);
		y += 14;
		g.text(font, Component.translatable("screen.jugcraft.tower_place.reaches", size.maxX(), -size.minX(), -size.minZ(), size.maxZ()).getString(),
				x, y, WHITE, false);
		y += 11;
		g.text(font, Component.translatable("screen.jugcraft.tower_place.core_at", core.getX(), core.getY(), core.getZ()).getString(), x, y, DIM, false);
		y += 14;
		for (FormattedCharSequence line : font.split(Component.translatable("screen.jugcraft.tower_place.tip"), left + WIDTH - 8 - x)) {
			g.text(font, line, x, y, DIM, false);
			y += 10;
		}
	}

	/** One of WIDTH, LENGTH, HEIGHT: its name, "n blocks" right-aligned, which way. Returns the next line's y. */
	private int dimension(GuiGraphicsExtractor g, int x, int y, String name, int blocks, String way) {
		g.text(font, Component.translatable("screen.jugcraft.tower_place." + name).getString(), x, y, DIM, false);
		String value = Component.translatable("screen.jugcraft.tower_place.blocks", blocks).getString();
		g.text(font, value, x + 94 - font.width(value), y, WHITE, false);
		if (way != null) {
			g.text(font, Component.translatable("screen.jugcraft.tower_place." + way).getString(), x + 98, y, DIM, false);
		}
		return y + 11;
	}

	/** The plan's runs, worked out once from the finished tower's columns. */
	private static synchronized List<int[]> runs(TowerData.Extent size) {
		if (runs == null) {
			List<int[]> out = new ArrayList<>();
			for (int z = size.minZ(); z <= size.maxZ(); z++) {
				int x = size.minX();
				while (x <= size.maxX()) {
					int t = size.top(x, z);
					if (t == Integer.MIN_VALUE) {
						x++;
						continue;
					}
					int colour = shade(t);
					int x0 = x;
					while (x <= size.maxX() && size.top(x, z) != Integer.MIN_VALUE && shade(size.top(x, z)) == colour) {
						x++;
					}
					out.add(new int[] {z - size.minZ(), x0 - size.minX(), x - size.minX(), colour});
				}
			}
			runs = out;
		}
		return runs;
	}

	private static int shade(int top) {
		for (int i = 0; i < SHADE_TOP.length; i++) {
			if (top <= SHADE_TOP[i]) {
				return SHADE[i];
			}
		}
		return SHADE[SHADE.length - 1];
	}
}
