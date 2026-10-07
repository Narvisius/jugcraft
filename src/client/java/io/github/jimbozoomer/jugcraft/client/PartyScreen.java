package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.client.blueprint.SciFi;
import io.github.jimbozoomer.jugcraft.party.PartyRequestPayload;
import io.github.jimbozoomer.jugcraft.party.PartyStatePayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Party screen (the Party key, P by default), in the red-and-graphite look of the Drone Tower and
 * Blueprint screens. It shows the player's party and pending invites from the server's
 * {@link PartyStatePayload}, and every button runs an ordinary {@code /party} command, so the server
 * checks everything exactly as it does for typed commands.
 */
public class PartyScreen extends Screen {
	private static final int WIDTH = 248;
	private static final int HEIGHT = 196;
	/** Member rows shown; a bigger party (a server can allow up to 64) lists the rest as "+N". */
	private static final int ROWS = 8;
	private static final int ROW = 12;

	/** The last state the server sent; null until the first one arrives. */
	private static PartyStatePayload state;

	private int left;
	private int top;
	private EditBox name;

	public PartyScreen() {
		super(Component.translatable("screen.jugcraft.party.title"));
	}

	/** Called on the client thread when the server sends the state; refreshes the screen if it is open. */
	static void receive(PartyStatePayload payload) {
		state = payload;
		if (Minecraft.getInstance().gui.screen() instanceof PartyScreen screen) {
			String typed = screen.name == null ? "" : screen.name.getValue();
			screen.rebuildWidgets();
			screen.name.setValue(typed);
		}
	}

	/** Forgets the state when leaving a world, so another server's party never shows. */
	static void clear() {
		state = null;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		if (state == null) {
			ClientPlayNetworking.send(PartyRequestPayload.INSTANCE);
		}
		name = addRenderableWidget(new EditBox(font, left + 8, top + HEIGHT - 22, 120, 14, Component.translatable("screen.jugcraft.party.name")));
		name.setMaxLength(16);
		name.setHint(Component.translatable("screen.jugcraft.party.name"));
		if (state == null || !state.enabled()) {
			name.visible = false;
			return;
		}
		List<PartyStatePayload.Member> members = state.members();
		boolean inParty = !members.isEmpty();
		boolean leads = state.youLead();
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.party.invite"), button -> {
			String target = name.getValue().trim();
			if (!target.isEmpty()) {
				run("party invite " + target);
				name.setValue("");
			}
		}).bounds(left + 132, top + HEIGHT - 23, 52, 16).build()).active = !inParty || leads;
		name.visible = !inParty || leads;
		if (inParty) {
			addRenderableWidget(Button.builder(Component.translatable(leads ? "screen.jugcraft.party.disband" : "screen.jugcraft.party.leave"),
					button -> run(leads ? "party disband" : "party leave")).bounds(left + WIDTH - 60, top + HEIGHT - 23, 52, 16).build());
		}
		if (leads) {
			for (int i = 0; i < Math.min(members.size(), ROWS); i++) {
				PartyStatePayload.Member member = members.get(i);
				if (member.you()) {
					continue;
				}
				int y = top + 36 + i * ROW;
				addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.party.make_leader"),
						button -> run("party leader " + member.name())).bounds(left + WIDTH - 76, y - 1, 34, 11).build());
				addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.party.kick"),
						button -> run("party kick " + member.name())).bounds(left + WIDTH - 40, y - 1, 32, 11).build());
			}
		}
		if (!inParty && !state.invitesFrom().isEmpty()) {
			int y = top + 153;
			addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.party.accept"), button -> run("party accept"))
					.bounds(left + WIDTH - 118, y - 3, 54, 14).build());
			addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.party.decline"), button -> run("party decline"))
					.bounds(left + WIDTH - 62, y - 3, 54, 14).build());
		}
	}

	/** Runs a {@code /party} command as the player, then asks for the new state. */
	private static void run(String command) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.connection.sendCommand(command);
			ClientPlayNetworking.send(PartyRequestPayload.INSTANCE);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		SciFi.frame(graphics, left, top, left + WIDTH, top + HEIGHT);
		graphics.fill(left + 6, top + 20, left + WIDTH - 6, top + 22 + ROWS * ROW + 16, SciFi.PANEL);
		graphics.fill(left + 6, top + 138, left + WIDTH - 6, top + 170, SciFi.PANEL);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, "▌" + title.getString(), left + 8, top + 7, SciFi.ACCENT, false);
		if (state == null) {
			graphics.text(font, "LINKING…", left + 12, top + 26, SciFi.AMBER, false);
			return;
		}
		if (!state.enabled()) {
			say(graphics, Component.translatable("screen.jugcraft.party.disabled").getString(), left + 12, top + 26, SciFi.AMBER);
			return;
		}
		List<PartyStatePayload.Member> members = state.members();
		if (members.isEmpty()) {
			say(graphics, Component.translatable("screen.jugcraft.party.solo").getString(), left + 12, top + 26, SciFi.WHITE);
		} else {
			graphics.text(font, Component.translatable("screen.jugcraft.party.members", members.size(), state.maxSize()).getString(),
					left + 12, top + 25, SciFi.DIM, false);
			for (int i = 0; i < Math.min(members.size(), ROWS); i++) {
				PartyStatePayload.Member member = members.get(i);
				int y = top + 37 + i * ROW;
				graphics.fill(left + 10, y + 1, left + 14, y + 5, member.online() ? SciFi.GOOD : SciFi.DIM);
				StringBuilder line = new StringBuilder(member.name());
				if (member.you()) {
					line.append(' ').append(Component.translatable("screen.jugcraft.party.you").getString());
				}
				if (!member.online()) {
					line.append(" · ").append(Component.translatable("screen.jugcraft.party.offline").getString());
				}
				graphics.text(font, SciFi.fit(font, line.toString(), 120), left + 18, y, member.online() ? SciFi.WHITE : SciFi.DIM, false);
				if (member.leader()) {
					graphics.text(font, Component.translatable("screen.jugcraft.party.leader").getString(), left + 142, y, SciFi.AMBER, false);
				}
			}
			if (members.size() > ROWS) {
				graphics.text(font, "+" + (members.size() - ROWS), left + 18, top + 37 + ROWS * ROW, SciFi.DIM, false);
			}
		}
		graphics.text(font, Component.translatable("screen.jugcraft.party.invites").getString(), left + 12, top + 141, SciFi.DIM, false);
		List<String> invites = state.invitesFrom();
		if (invites.isEmpty()) {
			graphics.text(font, Component.translatable("screen.jugcraft.party.no_invites").getString(), left + 12, top + 153, SciFi.DIM, false);
		} else {
			String latest = Component.translatable("screen.jugcraft.party.invite_from", invites.get(invites.size() - 1)).getString();
			graphics.text(font, SciFi.fit(font, latest, 104), left + 12, top + 153, SciFi.WHITE, false);
			if (invites.size() > 1) {
				graphics.text(font, Component.translatable("screen.jugcraft.party.more_invites", invites.size() - 1).getString(),
						left + 12, top + 161, SciFi.DIM, false);
			}
		}
	}

	/** Text wrapped to the panel width. */
	private void say(GuiGraphicsExtractor graphics, String text, int x, int y, int colour) {
		int lineY = y;
		for (var line : font.split(Component.literal(text), WIDTH - 24)) {
			graphics.text(font, line, x, lineY, colour, false);
			lineY += 10;
		}
	}
}
