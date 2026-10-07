package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.EngravePayload;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.OpenEpitaphPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The epitaph screen, opened by the Stonemason's Chisel on a headstone ({@link OpenEpitaphPayload}): a slab of slate
 * with one line to write per line of the epitaph, filled with what is cut there now. "Done" sends the lines to the
 * server ({@link EngravePayload}), which checks them; "Cancel" changes nothing.
 */
public class EpitaphScreen extends Screen {
	private static final int WIDTH = 220;
	private static final int HEIGHT = 46 + Epitaph.LINES * 22 + 30;
	private static final int SLATE = 0xFF3E444D;
	private static final int SLATE_EDGE = 0xFF2A2F36;
	private static final int CUT = 0xFFC9CED5;

	private final BlockPos pos;
	private final int slot;
	private final List<String> start;
	private final List<EditBox> boxes = new ArrayList<>();
	private int left;
	private int top;

	public EpitaphScreen(OpenEpitaphPayload payload) {
		super(Component.translatable("screen.jugcraft.epitaph.title"));
		pos = payload.pos();
		slot = payload.slot();
		start = payload.lines();
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		boxes.clear();
		for (int i = 0; i < Epitaph.LINES; i++) {
			EditBox box = addRenderableWidget(new EditBox(font, left + 20, top + 36 + i * 22, WIDTH - 40, 18,
					Component.translatable("screen.jugcraft.epitaph.line", i + 1)));
			box.setMaxLength(Epitaph.LINE_LENGTH);
			box.setValue(i < start.size() ? start.get(i) : "");
			boxes.add(box);
		}
		int buttons = top + 40 + Epitaph.LINES * 22 + 8;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> done()).bounds(left + 20, buttons, 84, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
				.bounds(left + WIDTH - 104, buttons, 84, 20).build());
		setFocused(boxes.get(0));
	}

	private void done() {
		List<String> lines = new ArrayList<>();
		for (EditBox box : boxes) {
			lines.add(box.getValue());
		}
		ClientPlayNetworking.send(new EngravePayload(pos, slot, lines));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, SLATE_EDGE);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, SLATE);
		// An incised border, as on a New England slate.
		graphics.fill(left + 8, top + 8, left + WIDTH - 8, top + 9, CUT);
		graphics.fill(left + 8, top + HEIGHT - 9, left + WIDTH - 8, top + HEIGHT - 8, CUT);
		graphics.fill(left + 8, top + 8, left + 9, top + HEIGHT - 8, CUT);
		graphics.fill(left + WIDTH - 9, top + 8, left + WIDTH - 8, top + HEIGHT - 8, CUT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, title.getString(), left + (WIDTH - font.width(title)) / 2, top + 14, CUT, false);
		String hint = Component.translatable("screen.jugcraft.epitaph.hint").getString();
		graphics.text(font, hint, left + (WIDTH - font.width(hint)) / 2, top + 24, 0xFF9AA2AC, false);
	}
}
