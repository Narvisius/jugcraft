package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.concordance.compose.Composition;
import io.github.jimbozoomer.jugcraft.concordance.compose.Draft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/** A constellation editor over the existing bounded Draft and server compiler. No text entry or client unlocks. */
public class ComposerScreen extends Screen {
    private static final Slot[] STAGES = {Slot.DELIVERY, Slot.SELECTION, Slot.OPERATION, Slot.MODIFIER};
    private static final int GOLD = 0xFFE6C586, TEXT = 0xFFE2E7F1, DIM = 0xFF9CAAC3;
    private final @Nullable Screen parent;
    private final Runnable listener = () -> dirty = true;
    private final Map<String, AbstractWidget> controls = new LinkedHashMap<>();
    private final List<Star> stars = new ArrayList<>();
    private @Nullable ComposerScroll report;
    private @Nullable Button inscribe;
    private boolean dirty, details, paths;
    private int stage, branch, palettePage, x, y, w, h, mapW, sideX, sideW, mapTop, mapBottom;
    private String focus = "";
    private long changedAt = Util.getMillis();

    public ComposerScreen(@Nullable Screen parent) {
        super(Component.translatable("screen.jugcraft.composer.title"));
        this.parent = parent;
    }

    static Component ui(String key, Object... args) { return Component.translatable("ui.jugcraft." + key, args); }
    public static Component wordName(String id) {
        Identifier parsed = Identifier.tryParse(id);
        String name = Composition.shortName(id);
        if (parsed != null && parsed.getNamespace().equals("jugcraft"))
            return Component.translatableWithFallback("ui.jugcraft.word." + name, name);
        return Component.translatableWithFallback("component." + (parsed == null ? "jugcraft." + name
            : parsed.getNamespace() + "." + parsed.getPath().replace('/', '.')), name);
    }
    private static Component description(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return Component.translatableWithFallback("component." + (parsed == null ? "jugcraft." + id
            : parsed.getNamespace() + "." + parsed.getPath().replace('/', '.')) + ".description", "");
    }
    private List<Draft.Part> parts() {
        List<List<Draft.Part>> levels = ComposerClient.draft().levels();
        branch = Math.min(branch, levels.size() - 1);
        return levels.get(branch);
    }
    private int indexOf(String id) {
        List<Draft.Part> parts = parts();
        for (int i = 0; i < parts.size(); i++) if (parts.get(i).id().equals(id)) return i;
        return -1;
    }
    private boolean has(Slot slot) { return parts().stream().anyMatch(p -> ComposerClient.slotOf(p.id()) == slot); }
    private boolean complete() {
        return ComposerClient.draft().levels().stream().allMatch(level -> java.util.Arrays.stream(new Slot[]{Slot.DELIVERY, Slot.SELECTION, Slot.OPERATION})
            .allMatch(slot -> level.stream().anyMatch(part -> ComposerClient.slotOf(part.id()) == slot)));
    }
    private boolean canStage(int at) { return at == 0 || has(Slot.DELIVERY) && (at == 1 || has(Slot.SELECTION)); }
    private boolean selected(ComposerStatePayload.Word word) {
        if (ComposerClient.slot(word) != Slot.MODIFIER) return indexOf(word.id()) >= 0;
        Draft.Part chosen = ComposerClient.draft().chosen();
        return chosen != null && chosen.modifiers().contains(word.id());
    }

    @Override protected void init() {
        ComposerClient.unlisten(listener); ComposerClient.listen(listener);
        double scroll = report == null ? 0 : report.scrollAmount();
        controls.clear(); stars.clear();
        w = Math.min(720, width - 16); h = Math.min(404, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        sideW = Math.max(116, Math.min(210, w / 3)); sideX = x + w - sideW - 12;
        mapW = sideX - x - 18; mapTop = y + 76; mapBottom = y + h - (paths ? 64 : 40);
        parts();
        if (!canStage(stage)) stage = has(Slot.DELIVERY) ? 1 : 0;
        button("back", x + w - 62, y + 11, 50, 18, ui("close"), true, () -> onClose());
        for (int i = 0; i < STAGES.length; i++) {
            int at = i, bw = (mapW - 8) / 4;
            Button b = button("stage:" + i, x + 12 + i * bw, y + 46, bw - 3, 21,
                ui("stage." + i), canStage(i), () -> { stage = at; palettePage = 0; changedAt = Util.getMillis(); dirty = true; });
            b.setTooltip(Tooltip.create(ui("stage.hint." + i)));
        }
        if (stage == 3 && !parts().isEmpty()) {
            Draft draft = ComposerClient.draft();
            if (draft.chosen() == null || draft.level() != branch) draft.choose(branch, parts().size() - 1);
            int selected = draft.index();
            button("part_previous", x + 12, mapTop, 19, 19, Component.literal("<"), true,
                () -> { draft.choose(branch, Math.floorMod(selected - 1, parts().size())); dirty = true; });
            button("part_next", x + mapW - 12, mapTop, 19, 19, Component.literal(">"), true,
                () -> { draft.choose(branch, (selected + 1) % parts().size()); dirty = true; });
            button("part_selected", x + 34, mapTop, mapW - 50, 19, ui("compose.enhance", wordName(draft.chosen().id())), true,
                () -> { draft.choose(branch, (selected + 1) % parts().size()); dirty = true; });
            mapTop += 26;
        }
        ComposerStatePayload state = ComposerClient.state();
        List<ComposerStatePayload.Word> words = state == null ? List.of() : state.words().stream()
            .filter(word -> ComposerClient.slot(word) == STAGES[stage] || stage == 3 && ComposerClient.slot(word) == Slot.TERMINATION).toList();
        int pageSize = mapBottom - mapTop < 96 ? 3 : 6;
        palettePage = Math.min(palettePage, Math.max(0, (words.size() - 1) / pageSize));
        int start = palettePage * pageSize, count = Math.min(pageSize, words.size() - start);
        int cols = count <= 3 ? Math.max(1, count) : 3, rows = (count + cols - 1) / cols;
        int nodeW = Math.max(40, (mapW - 20) / cols);
        int usable = Math.max(50, mapBottom - mapTop - 16);
        for (int i = 0; i < count; i++) {
            var word = words.get(start + i);
            int nx = x + 12 + (i % cols) * nodeW;
            int ny = mapTop + 8 + (i / cols) * Math.max(42, usable / Math.max(1, rows));
            Star star = new Star(nx, ny, nodeW - 2, Math.min(54, Math.max(40, usable / Math.max(1, rows) - 3)), word);
            boolean enabled = ComposerClient.ready();
            if (ComposerClient.slot(word) == Slot.MODIFIER) {
                Draft.Part chosen = ComposerClient.draft().chosen();
                var target = chosen == null ? null : ComposerClient.word(chosen.id());
                enabled &= target != null && (target.aspects() & word.aspects()) != 0
                    && (selected(word) || chosen.modifiers().size() < Grammar.MAX_MODIFIERS);
            } else if (ComposerClient.slot(word) == Slot.OPERATION && !selected(word)) {
                enabled &= parts().stream().filter(p -> ComposerClient.slotOf(p.id()) == Slot.OPERATION).count() < Grammar.MAX_OPERATIONS;
            }
            star.active = enabled;
            add("word:" + word.id(), star); stars.add(star);
        }
        int by = y + h - 31;
        int bw = (mapW - 8) / 4;
        button("undo", x + 12, by, bw - 3, 18, ui("undo"), ComposerClient.draft().canUndo(), () -> edited(ComposerClient.draft().undo()));
        button("restart", x + 12 + bw, by, bw - 3, 18, ui("reset"), !ComposerClient.draft().isEmpty(), () -> {
            if (ComposerClient.draft().clear()) { branch = 0; stage = 0; edited(true); }
        });
        button("load", x + 12 + bw * 2, by, bw - 3, 18, ui("load"), state != null && !state.inscribed().isEmpty(), ComposerClient::load)
            .setTooltip(Tooltip.create(Component.translatable("screen.jugcraft.composer.load")));
        button("paths", x + 12 + bw * 3, by, bw - 3, 18, ui("paths", branch + 1), true, () -> { paths = !paths; dirty = true; });
        int fy = y + h - 54;
        int branches = state == null || state.instrument().isEmpty() ? 0 : state.instrument().get().branches();
        int levels = ComposerClient.draft().levels().size();
        if (paths) {
            int branchW = (mapW - 10) / 3;
            button("branch", x + 12, fy, branchW - 3, 18, ui("branch", branch + 1, levels), levels > 1,
                () -> { branch = (branch + 1) % levels; stage = 0; dirty = true; });
            button("add_branch", x + 12 + branchW, fy, branchW - 3, 18, ui("branch.add"),
                ComposerClient.ready() && complete() && levels - 1 < branches && ComposerClient.draft().canBranch(), () -> {
                    if (ComposerClient.draft().branch()) { branch = levels; stage = 0; paths = false; edited(true); }
                });
            button("remove_branch", x + 12 + branchW * 2, fy, branchW - 3, 18, ui("branch.remove"), levels > 1,
                () -> { edited(ComposerClient.draft().unbranch()); branch = Math.min(branch, ComposerClient.draft().levels().size() - 1); });
        }
        if (words.size() > pageSize) {
            button("more", x + mapW - 54, mapBottom - 18, 56, 16, ui("more"), true,
                () -> { palettePage = (palettePage + 1) % ((words.size() + pageSize - 1) / pageSize); dirty = true; });
        }
        inscribe = button("inscribe", sideX, y + h - 31, sideW, 20,
            Component.translatable("screen.jugcraft.composer.inscribe"), ComposerClient.inscribable(), ComposerClient::inscribe);
        report = addRenderableWidget(new ComposerScroll(minecraft, sideX, y + 79, sideW, Math.max(25, h - 154), this::focused));
        int ry = 0;
        if (!ComposerClient.ready()) {
            ry = report(ry, Component.translatable(state == null ? "screen.jugcraft.composer.opening"
                : state.status() == ComposerStatePayload.DISABLED ? "message.jugcraft.concordance.disabled" : "compose.jugcraft.problem.no_instrument"), GOLD);
        } else if (ComposerClient.draft().isEmpty()) {
            ry = report(ry, ui("compose.start"), TEXT);
        } else {
            List<List<Draft.Part>> all = ComposerClient.draft().levels();
            for (int i = 0; i < all.size(); i++) {
                if (all.size() > 1) ry = report(ry, ui("branch", i + 1, all.size()), GOLD);
                for (Draft.Part part : all.get(i)) {
                    Component label = wordName(part.id()).copy();
                    for (String mod : part.modifiers()) label = label.copy().append(" · ").append(wordName(mod));
                    ry = report(ry, label, TEXT);
                }
            }
        }
        ry += 8;
        if (ComposerClient.writing()) ry = report(ry, Component.translatable("screen.jugcraft.composer.writing"), GOLD);
        else if (ComposerClient.checking()) ry = report(ry, Component.translatable("screen.jugcraft.composer.checking"), DIM);
        else if (!ComposerClient.draft().isEmpty() && !complete()) ry = report(ry, ui("stage.hint." + stage), GOLD);
        else if (ComposerClient.result() != null && (ComposerClient.result().text().equals(ComposerClient.draft().text())
            || ComposerClient.result().text().isEmpty() && ComposerClient.result().what() != io.github.jimbozoomer.jugcraft.concordance.Composing.Outcome.CLEARED)) {
            boolean valid = ComposerClient.result().report().summary().isPresent();
            ry = report(ry, valid ? ui(ComposerClient.wipeable() && ComposerClient.state().inscribed().equals(ComposerClient.draft().text())
                ? "compose.inscribed" : "compose.ready") : ComposerClient.result().heading(), valid ? 0xFFA6D6C3 : GOLD);
            // Errors are always shown. Valid spell mechanics live under the optional details control.
            if (details || !valid)
                for (Component line : ComposerClient.result().report().lines()) ry = report(ry + 3, line, DIM);
        }
        if (state != null && state.locked() > 0) report(ry + 8, ui("compose.locked", state.locked()), DIM);
        report.arrange(scroll);
        button("details", sideX, y + h - 54, sideW / 2 - 2, 18, ui(details ? "less" : "details"), true, () -> { details = !details; dirty = true; });
        button("wipe", sideX + sideW / 2 + 2, y + h - 54, sideW / 2 - 2, 18, ui("wipe"), ComposerClient.wipeable(), ComposerClient::wipe);
        AbstractWidget restore = controls.get(focus);
        if (restore != null && restore.active) setFocused(restore);
    }

    private int report(int at, Component text, int color) {
        BookArt.Paragraph widget = new BookArt.Paragraph(text.copy().withColor(color & 0xFFFFFF), font, sideW - 12, color);
        widget.setX(sideX + 3); report.add(widget, at);
        return at + widget.getHeight() + 3;
    }
    private void edited(boolean changed) {
        if (changed) { changedAt = Util.getMillis(); ComposerClient.edited(); }
    }
    private void select(ComposerStatePayload.Word word) {
        Draft draft = ComposerClient.draft(); Slot slot = ComposerClient.slot(word);
        if (slot == null) return;
        int found = indexOf(word.id());
        if (slot == Slot.MODIFIER) {
            if (selected(word)) {
                // Rebuild only this part's modifier list, retaining branches and one undo step.
                List<String> levels = new ArrayList<>();
                for (int l = 0; l < draft.levels().size(); l++) {
                    List<String> written = new ArrayList<>();
                    for (int i = 0; i < draft.levels().get(l).size(); i++) {
                        Draft.Part part = draft.levels().get(l).get(i);
                        List<String> mods = l == draft.level() && i == draft.index() ? part.modifiers().stream().filter(m -> !m.equals(word.id())).toList() : part.modifiers();
                        written.add(new Draft.Part(part.id(), mods).written());
                    }
                    levels.add(String.join(" ", written));
                }
                int index = draft.index(); draft.read(String.join(" then ", levels), true); draft.choose(branch, index); edited(true);
            } else edited(draft.add(word.id(), slot, ComposerClient::slotOf));
        } else if (found >= 0 && (slot == Slot.OPERATION || slot == Slot.TERMINATION)) {
            draft.choose(branch, found); edited(draft.remove());
        } else {
            if (!parts().isEmpty()) draft.choose(branch, parts().size() - 1); else draft.choose(-1, -1);
            edited(draft.add(word.id(), slot, ComposerClient::slotOf));
            if (stage < 2) { stage++; palettePage = 0; dirty = true; }
        }
    }
    private <T extends AbstractWidget> T add(String key, T widget) { controls.put(key, widget); return addRenderableWidget(widget); }
    private Button button(String key, int bx, int by, int bw, int bh, Component label, boolean active, Runnable action) {
        Button button = new BookArt.Tab(bx, by, bw, bh, label, Component.empty(), "", (key.equals("stage:" + stage) || key.equals("part:" + ComposerClient.draft().index()) && branch == ComposerClient.draft().level()), true, unused -> action.run());
        button.active = active; return add(key, button);
    }
    private void focused(@Nullable GuiEventListener target) {
        controls.forEach((key, widget) -> { if (widget == target) focus = key; });
    }
    @Override public void setFocused(@Nullable GuiEventListener target) { super.setFocused(target); focused(target); }
    @Override protected void setInitialFocus() { if (getFocused() == null) super.setInitialFocus(); }
    @Override public void tick() {
        super.tick();
        if (dirty) { dirty = false; rebuildWidgets(); }
        if (inscribe != null) inscribe.active = ComposerClient.inscribable();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.shortcutKey() == InputConstants.KEYCODE_Z && event.hasControlDownWithQuirk()) {
            edited(ComposerClient.draft().undo()); return true;
        }
        if (event.input() == InputConstants.KEY_LEFT || event.input() == InputConstants.KEY_RIGHT) {
            int at = Math.max(0, Math.min(3, stage + (event.input() == InputConstants.KEY_LEFT ? -1 : 1)));
            if (canStage(at)) { stage = at; dirty = true; }
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void removed() { ComposerClient.unlisten(listener); super.removed(); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Deliberately no animated world blur; reduced motion freezes every decorative motion.
        g.fill(0, 0, width, height, 0xCF050913);
        g.fill(x, y, x + w, y + h, 0xFF0C1424);
        g.outline(x, y, w, h, 0xFF74613F);
        g.outline(x + 3, y + 3, w - 6, h - 6, 0xFF243148);
        double time = ConcordanceClientOptions.reducedMotion() ? 0 : Util.getMillis() / 1200.0;
        for (int i = 0; i < 72; i++) {
            int sx = x + 8 + Math.floorMod(i * 97 + i * i * 13, Math.max(1, mapW - 8));
            int sy = y + 35 + Math.floorMod(i * 59 + 17, Math.max(1, h - 50));
            int alpha = (int) (45 + 30 * (1 + Math.sin(time + i * 2.3)));
            g.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0x9ABBD6);
        }
        g.text(font, title, x + 16, y + 13, GOLD, false);
        g.text(font, ui("compose.subtitle"), x + 16, y + 27, DIM, false);
        g.fill(sideX - 7, y + 44, sideX - 6, y + h - 12, 0xFF344158);
        g.text(font, ui("compose.preview"), sideX + 3, y + 48, GOLD, false);
        int capacity = ComposerClient.state() == null || ComposerClient.state().instrument().isEmpty() ? 0 : ComposerClient.state().instrument().get().capacity();
        int used = ComposerClient.capacity();
        g.text(font, ui("compose.capacity", used, capacity), sideX + 3, y + 62, DIM, false);
        g.fill(sideX + 3, y + 74, sideX + sideW - 6, y + 76, 0xFF29354B);
        if (capacity > 0) g.fill(sideX + 3, y + 74, sideX + 3 + (sideW - 9) * Math.min(used, capacity) / capacity, y + 76,
            used > capacity ? 0xFFD58073 : 0xFF92BFC2);
        var preview = ComposerClient.result();
        if (!ComposerClient.checking() && preview != null && preview.text().equals(ComposerClient.draft().text()) && preview.report().summary().isPresent()) {
            var numbers = preview.report().summary().get();
            String seconds = io.github.jimbozoomer.jugcraft.concordance.compose.Text.seconds(numbers.cooldown());
            g.text(font, ui("compose.cost_compact", numbers.focus(), seconds), sideX + 3, y + h - 70, GOLD, false);
            if (mouseX >= sideX && mouseX < sideX + sideW && mouseY >= y + h - 72 && mouseY < y + h - 57)
                g.setTooltipForNextFrame(font, ui("compose.cost", numbers.focus(), seconds), mouseX, mouseY);
        }
        int rootX = x + 12 + mapW / 2, rootY = Math.max(mapTop + 10, mapBottom - 8);
        BookArt.circle(g, rootX, rootY, 7, 0xFF38475B);
        BookArt.star(g, rootX, rootY, 3, GOLD);
        double reveal = ConcordanceClientOptions.reducedMotion() ? 1 : Math.min(1, (Util.getMillis() - changedAt) / 320.0);
        for (Star star : stars) {
            int endX = star.getX() + star.getWidth() / 2, endY = star.getY() + 13;
            boolean chosen = selected(star.word);
            BookArt.line(g, rootX, rootY, rootX + (int)((endX - rootX) * reveal), rootY + (int)((endY - rootY) * reveal), chosen ? 0xFFBBA376 : 0xFF304057);
            if (chosen && !ConcordanceClientOptions.reducedMotion()) {
                double pos = (time / 3 + endX / 99.0) % 1;
                BookArt.star(g, rootX + (int)((endX - rootX) * pos), rootY + (int)((endY - rootY) * pos), 1, 0xFFE9D9AA);
            }
        }
        if (stars.isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(ui(ComposerClient.ready() ? "compose.no_words" : "compose.need_wand"), mapW - 25);
            int ty = mapTop + 20;
            for (var line : lines) { g.text(font, line, x + 18, ty, DIM, false); ty += 11; }
        }
    }

    private final class Star extends Button {
        private final ComposerStatePayload.Word word;
        Star(int x, int y, int w, int h, ComposerStatePayload.Word word) {
            super(x, y, w, h, wordName(word.id()), unused -> select(word), DEFAULT_NARRATION);
            this.word = word;
            setTooltip(Tooltip.create(wordName(word.id()).copy().append("\n").append(description(word.id())).append("\n")
                .append(Component.translatable("screen.jugcraft.composer.word_cost", word.capacity(), word.focus()))));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            boolean chosen = selected(word), lit = isHovered() || isFocused();
            int cx = getX() + width / 2, cy = getY() + 13;
            int color = !active ? 0xFF566177 : chosen ? GOLD : lit ? 0xFFCBEAF1 : 0xFF92B5D1;
            BookArt.circle(g, cx, cy, lit ? 12 : 10, color);
            if (chosen) BookArt.circle(g, cx, cy, 13, 0xFF665B47);
            BookArt.star(g, cx, cy, chosen ? 5 : 3, color);
            if (chosen) g.text(font, "+", cx + 13, cy - 10, GOLD, false);
            if (isFocused()) g.outline(getX(), getY(), width, height, color);
            String label = BookArt.fit(font, getMessage().getString(), width - 4);
            g.fill(cx - font.width(label) / 2 - 2, getY() + 28, cx + font.width(label) / 2 + 2, getY() + 39, 0xFF0C1424);
            g.text(font, label, cx - font.width(label) / 2, getY() + 29, color, false);
        }
    }
}
