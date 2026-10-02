package io.github.jimbozoomer.jugcraft.client.blueprint;

import io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The Survey Stake screen: how much of the blueprint is in place, what is still needed (with icons), blocks in
 * the way, and PERSONAL/PARTY, ROTATE and REMOVE for the player who placed it (or the party leader). It asks
 * the server for fresh numbers every second while open.
 */
public class StakeScreen extends Screen {
	private static final int W = 252;
	private static final int H = 184;
	static StakeScreen open;

	private BlueprintNetwork.StakeInfoPayload info;
	private int left;
	private int top;
	private int ticks;
	private Button mode;
	private Button rotate;
	private Button remove;

	public StakeScreen(BlueprintNetwork.StakeInfoPayload info) {
		super(Component.translatable("block.jugcraft.survey_stake"));
		this.info = info;
	}

	void update(BlueprintNetwork.StakeInfoPayload info) {
		this.info = info;
		refreshButtons();
	}

	@Override
	protected void init() {
		open = this;
		left = (width - W) / 2;
		top = (height - H) / 2;
		mode = addRenderableWidget(Button.builder(Component.literal(""), b -> action(BlueprintNetwork.StakeActionPayload.MODE))
				.bounds(left + 10, top + H - 28, 76, 20).build());
		rotate = addRenderableWidget(Button.builder(Component.literal("ROTATE"), b -> action(BlueprintNetwork.StakeActionPayload.ROTATE))
				.bounds(left + 90, top + H - 28, 64, 20).build());
		remove = addRenderableWidget(Button.builder(Component.literal("REMOVE"), b -> {
			action(BlueprintNetwork.StakeActionPayload.REMOVE);
			onClose();
		}).bounds(left + 158, top + H - 28, 64, 20).build());
		refreshButtons();
	}

	private void refreshButtons() {
		if (mode == null) {
			return;
		}
		mode.setMessage(Component.literal(info.mode().toUpperCase(java.util.Locale.ROOT)));
		mode.active = info.canChange();
		rotate.active = info.canChange();
		remove.active = info.canChange();
	}

	private void action(int action) {
		ClientPlayNetworking.send(new BlueprintNetwork.StakeActionPayload(info.pos(), action));
	}

	@Override
	public void tick() {
		if (++ticks % 20 == 0) {
			action(BlueprintNetwork.StakeActionPayload.INFO);
		}
	}

	@Override
	public void removed() {
		open = null;
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		SciFi.frame(g, left, top, left + W, top + H);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.text(font, SciFi.fit(font, "| SURVEY STAKE - " + info.name().toUpperCase(java.util.Locale.ROOT), W - 16), left + 8, top + 7, SciFi.ACCENT, false);
		g.text(font, info.done() + " / " + info.total() + " blocks in place", left + 10, top + 22, SciFi.WHITE, false);
		int barW = W - 20;
		g.fill(left + 10, top + 34, left + 10 + barW, top + 40, 0xFF0C0A0B);
		g.fill(left + 10, top + 34, left + 10 + (info.total() == 0 ? 0 : barW * info.done() / info.total()), top + 40, SciFi.ACCENT);
		g.text(font, info.items().isEmpty() ? "NOTHING ELSE NEEDED" : "STILL NEEDED", left + 10, top + 46, SciFi.DIM, false);
		for (int i = 0; i < Math.min(10, info.items().size()); i++) {
			ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(info.items().get(i))));
			int x = left + 10 + (i % 2) * 118, y = top + 57 + (i / 2) * 17;
			g.item(stack, x, y);
			g.text(font, SciFi.fit(font, info.counts().get(i) + " x " + stack.getHoverName().getString(), 96), x + 19, y + 4, SciFi.WHITE, false);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
		}
		int y = top + 144;
		if (!info.handOnly().isEmpty()) {
			String names = String.join(", ", info.handOnly().stream()
					.map(id -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))).getHoverName().getString()).toList());
			g.text(font, SciFi.fit(font, "Place by hand: " + names, W - 20), left + 10, y - 10, SciFi.AMBER, false);
		}
		if (info.wrong() > 0) {
			g.text(font, info.wrong() + " blocks in the way (orange outline)", left + 10, y, SciFi.AMBER, false);
		}
	}
}
