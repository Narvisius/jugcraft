package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.machine.form.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * A form machine's recipe page, in its machine screen's look (docs/features/machine-screens.md): the status lamp and
 * the machine's name along the top (green while a recipe is locked, amber when the catalogue is stale), the job it is
 * on under it, the searchable recipe list in one pane (the chosen recipe's key lit) and its inputs, outputs, cost and
 * ports in the other, with the keys along the bottom. Read-only preview until Lock is clicked. The retained open menu
 * owns synchronization and authority.
 */
public final class MachineRecipeScreen extends Screen implements ScreenKit.Fitted {
    private final FormMachineScreen parent;
    private final FormMachineMenu menu;
    private final MachineScreenThemes.Theme theme;
    private final List<ScreenKit.TextButton> rows = new ArrayList<>();
    private List<Integer> filtered = List.of();
    private String query = "";
    private int selected, offset, detailScroll, left, top, panelWidth, panelHeight, listWidth, rowCount;
    private ScreenKit.TextButton lock, automatic, priority, stock, clearRules, previous, next;
    private EditBox search;
    private record Line(ItemStack icon, Component text, int color) { }
    /** Where the panes start under the title strip and the job line, and how much room the keys take under them. */
    private static final int PANES_TOP = 30, KEYS_HEIGHT = 50, ROW = 20;
    // Minecraft key input codes, shared with HandbookScreen's navigation.
    private static final int KEY_PAGE_UP = 266, KEY_PAGE_DOWN = 267;

    public MachineRecipeScreen(FormMachineScreen parent, FormMachineMenu menu) {
        super(tr("recipes")); this.parent = parent; this.menu = menu; this.theme = parent.theme();
        selected = menu.selectedRecipe() > 0 && menu.selectedRecipe() <= menu.catalog().size() ? menu.selectedRecipe() - 1 : 0;
    }
    private static Component tr(String key, Object... args) { return Component.translatable("container.jugcraft.form." + key, args); }
    @Override public ScreenRectangle panelArea() { return new ScreenRectangle(left, top, panelWidth, panelHeight); }
    @Override protected void init() {
        panelWidth = Math.min(640, width - 12); panelHeight = Math.min(400, height - 12);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        listWidth = Math.min(190, Math.max(110, panelWidth / 3));
        int paneBottom = top + panelHeight - KEYS_HEIGHT;
        search = addRenderableWidget(new EditBox(font, left + 8, top + PANES_TOP + 4, listWidth - 14, 16, tr("search_recipes")));
        search.setMaxLength(100); search.setHint(tr("search_recipes")); search.setValue(query);
        search.setResponder(value -> { query = value; offset = 0; detailScroll = 0; refresh(); });
        rows.clear(); rowCount = Math.max(1, (paneBottom - 22 - (top + PANES_TOP + 24)) / ROW);
        for (int i = 0; i < rowCount; i++) {
            final int row = i;
            rows.add(addRenderableWidget(new ScreenKit.TextButton(left + 8, top + PANES_TOP + 24 + i * ROW, listWidth - 14, ROW - 2, theme,
                    Component.empty(), b -> {
                if (offset + row < filtered.size()) { selected = filtered.get(offset + row); detailScroll = 0; refresh(); }
            })));
        }
        previous = addRenderableWidget(new ScreenKit.TextButton(left + 7, paneBottom - 20, 14, 16, theme, Component.literal("<"), b -> scrollList(-rowCount)));
        next = addRenderableWidget(new ScreenKit.TextButton(left + listWidth - 21, paneBottom - 20, 14, 16, theme, Component.literal(">"), b -> scrollList(rowCount)));
        int keysTop = paneBottom + 4;
        automatic = addRenderableWidget(new ScreenKit.TextButton(left + 6, keysTop, listWidth - 10, 18, theme, tr("automatic"),
                b -> request(FormMachineMenu.BUTTON_AUTO)));
        addRenderableWidget(new ScreenKit.TextButton(left + 6, keysTop + 22, listWidth - 10, 18, theme, tr("back_machine"), b -> onClose()));
        int detailLeft = left + listWidth + 2, detailWidth = panelWidth - listWidth - 8;
        lock = addRenderableWidget(new ScreenKit.TextButton(detailLeft, keysTop, detailWidth, 18, theme, tr("lock_recipe"),
                b -> request(FormMachineMenu.BUTTON_RECIPE + selected)));
        int controlWidth = (detailWidth - 4) / 2;
        priority = addRenderableWidget(new ScreenKit.TextButton(detailLeft, keysTop + 22, controlWidth, 18, theme, tr("automation_priority", 0),
                b -> request(FormMachineMenu.BUTTON_PRIORITY + selected)));
        stock = addRenderableWidget(new ScreenKit.TextButton(detailLeft + controlWidth + 4, keysTop + 22, detailWidth - controlWidth - 4, 18, theme,
                tr("automation_stock", tr("automation_off")), b -> request(FormMachineMenu.BUTTON_TARGET + selected)));
        clearRules = addRenderableWidget(new ScreenKit.TextButton(left + panelWidth - 112, top + 2, 106, 12, theme, tr("automation_reset"),
                b -> request(FormMachineMenu.BUTTON_CLEAR_RULES)));
        priority.setTooltip(Tooltip.create(tr("automation_help")));
        stock.setTooltip(Tooltip.create(tr("automation_help")));
        clearRules.setTooltip(Tooltip.create(tr("automation_reset_help")));
        refresh();
    }
    private Component name(int index) {
        var e = menu.catalog().get(index);
        String path = e.id().getPath(); path = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
        return Component.translatableWithFallback("recipe." + e.id().getNamespace() + "." + e.id().getPath().replace('/', '.'),
                Character.toUpperCase(path.charAt(0)) + path.substring(1));
    }
    private String searchable(int i) {
        var e = menu.catalog().get(i); var r = e.recipe();
        StringBuilder text = new StringBuilder(name(i).getString()).append(' ').append(e.id());
        r.items().forEach(p -> p.ingredient().items().forEach(item -> text.append(' ').append(new ItemStack(item).getHoverName().getString())));
        r.results().forEach(item -> text.append(' ').append(item.create().getHoverName().getString()));
        r.fluids().forEach(f -> text.append(' ').append(fluidName(f.fluid()).getString()));
        r.fluidResults().forEach(f -> text.append(' ').append(fluidName(f.fluid()).getString()));
        return text.toString().toLowerCase(Locale.ROOT);
    }
    private void refresh() {
        var matches = new ArrayList<Integer>();
        for (int i = 0; i < menu.catalog().size(); i++) if (searchable(i).contains(query.toLowerCase(Locale.ROOT))) matches.add(i);
        filtered = List.copyOf(matches);
        if (!filtered.isEmpty() && !filtered.contains(selected)) { selected = filtered.getFirst(); detailScroll = 0; }
        offset = Math.clamp(offset, 0, Math.max(0, filtered.size() - rowCount));
        for (int i = 0; i < rows.size(); i++) {
            var button = rows.get(i); int pos = offset + i;
            button.visible = pos < filtered.size();
            if (button.visible) {
                int index = filtered.get(pos);
                // The chosen recipe's key is lit, rather than marked with an arrow.
                button.setSelected(index == selected);
                button.setMessage(Component.literal(ScreenKit.fit(font, name(index).getString(), listWidth - 22)));
                button.setTooltip(Tooltip.create(name(index).copy().append("\n" + menu.catalog().get(index).id())));
            }
        }
        previous.active = offset > 0;
        next.active = offset + rowCount < filtered.size();
    }
    private void scrollList(int amount) { offset += amount; refresh(); }
    private void request(int id) {
        if (minecraft.gameMode != null && minecraft.player != null && minecraft.player.containerMenu == menu && !menu.catalogStale())
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.player.containerMenu != menu) minecraft.gui.setScreen(null);
    }
    @Override public void onClose() {
        parent.returnFromRecipes();
        minecraft.gui.setScreen(minecraft.player != null && minecraft.player.containerMenu == menu ? parent : null);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        switch (event.input()) {
            case KEY_PAGE_UP -> detailScroll = Math.max(0, detailScroll - 100);
            case KEY_PAGE_DOWN -> detailScroll += 100;
            default -> { return super.keyPressed(event); }
        }
        return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (x < left + listWidth) scrollList((int) -Math.signum(dy) * 3);
        else detailScroll = Math.max(0, detailScroll - (int) (dy * 24));
        return true;
    }
    private static Component fluidName(Fluid fluid) { return FluidVariantAttributes.getName(FluidVariant.of(fluid)); }
    private void text(List<Line> lines, Component text, int color) { lines.add(new Line(ItemStack.EMPTY, text, color)); }
    private String port(FormPort.Kind kind, int index) {
        return menu.form().ports().stream().filter(p -> p.kind() == kind && (kind.item() ? p.reaches(index) : p.target() == index))
                .map(p -> p.side().name().toLowerCase(Locale.ROOT) + " [" + (p.column()+1) + "," + (p.row()+1) + "," + (p.layer()+1) + "]")
                .reduce((a,b) -> a + "; " + b).orElse("internal");
    }
    private Component targetLabel() {
        if (selected < 0 || selected >= menu.catalog().size() || menu.stockTarget(selected) == 0) return tr("automation_off");
        return tr(menu.catalog().get(selected).recipe().results().isEmpty() ? "automation_mb" : "automation_units", menu.stockTarget(selected));
    }
    private List<Line> preview() {
        var lines = new ArrayList<Line>();
        // Headings in the look's bright accent: its label colour is for the casing, and some looks' is dark.
        int text = theme.text(), dim = theme.dim(), heading = theme.progress();
        if (filtered.isEmpty()) { text(lines, tr(menu.catalog().isEmpty() ? "no_recipes" : "no_recipe_matches"), dim); return lines; }
        var r = menu.catalog().get(selected).recipe();
        text(lines, name(selected), heading);
        var portDetails = new ArrayList<Line>();
        text(lines, tr("recipe_inputs"), heading);
        for (int i = 0; i < r.items().size(); i++) {
            var part = r.items().get(i);
            var names = part.ingredient().items().map(item -> new ItemStack(item).getHoverName().getString()).toList();
            var icon = new ItemStack(part.ingredient().items().findFirst().orElseThrow());
            lines.add(new Line(icon, tr("recipe_item_in", i+1, part.count(), String.join(" / ", names)), text));
            text(portDetails, Component.literal(port(FormPort.Kind.ITEM_IN, i)), dim);
        }
        for (int i = 0; i < r.fluids().size(); i++) {
            var f = r.fluids().get(i);
            lines.add(new Line(new ItemStack(f.fluid().getBucket()), tr("recipe_fluid_in", i+1, f.amount(), fluidName(f.fluid())), text));
            text(portDetails, Component.literal(port(FormPort.Kind.FLUID_IN, i)), dim);
        }
        r.tool().ifPresentOrElse(tool -> {
            String names = String.join(" / ", tool.items().map(item -> new ItemStack(item).getHoverName().getString()).toList());
            boolean present = false;
            for (int i = 0; i < menu.form().sockets().size(); i++) if (tool.test(menu.slots.get(menu.form().firstSocketSlot()+i).getItem())) present = true;
            text(lines, tr(present ? "recipe_tool_present" : "recipe_tool_missing", names), present ? theme.ok() : theme.warn());
        }, () -> text(lines, tr("recipe_no_tool"), dim));
        text(lines, tr("recipe_outputs"), heading);
        for (int i = 0; i < r.results().size(); i++) {
            var item = r.results().get(i).create();
            lines.add(new Line(item, tr("recipe_item_out", i+1, item.getCount(), item.getHoverName()), text));
            text(portDetails, Component.literal(port(FormPort.Kind.ITEM_OUT, i)), dim);
        }
        for (int i = 0; i < r.fluidResults().size(); i++) {
            var f = r.fluidResults().get(i); int tank = f.tank() < 0 ? i : f.tank();
            lines.add(new Line(new ItemStack(f.fluid().getBucket()), tr("recipe_fluid_out", tank+1, f.amount(), fluidName(f.fluid())), text));
            text(portDetails, MachineStatus.tankLabel(menu.form(), menu.form().tanks().inputTanks().size()+tank).copy().append(" / " + port(FormPort.Kind.FLUID_OUT, tank)), dim);
            if (f.retained()) text(lines, tr("recipe_retained"), dim);
            if (menu.form().waterDrainOutput() == tank) text(lines, tr("recipe_overflow"), dim);
        }
        text(lines, menu.hasAutomation() ? tr("automation_summary", menu.recipePriority(selected), targetLabel())
            : tr("automation_missing"), dim);
        var effect = menu.upgrades();
        int ticks = effect.ticks(r.time());
        long rate = menu.form().profile().lanePerTick(effect.use(r.energyPerTick() > 0 ? r.energyPerTick() : menu.form().family().usePerTick));
        text(lines, tr("recipe_cost", ticks, String.format(Locale.ROOT,"%.1f",ticks/20.0), rate, rate*ticks), text);
        text(lines, tr("recipe_cost_hint", menu.form().profile().lanes()), dim);
        text(lines, tr("next_batch_hint"), dim);
        text(lines, tr("recipe_ports_hint"), dim);
        lines.addAll(portDetails);
        text(lines, tr("recipe_status").copy().append(menu.status().title(menu.form())).append(". ").append(menu.status().detail(menu.form())), text);
        return lines;
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractBackground(g,x,y,delta);
        int paneBottom = top + panelHeight - KEYS_HEIGHT;
        ScreenKit.body(g, theme, left, top, panelWidth, panelHeight);
        // The recipe list, with its search field, keys and page keys; then the chosen recipe's details.
        ScreenKit.pane(g, theme, left + 4, top + PANES_TOP, listWidth - 6, paneBottom - top - PANES_TOP);
        ScreenKit.pane(g, theme, left + listWidth + 2, top + PANES_TOP, panelWidth - listWidth - 8, paneBottom - top - PANES_TOP);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int choice = menu.selectedRecipe();
        lock.active = !menu.catalogStale() && !filtered.isEmpty() && choice != selected+1;
        automatic.active = !menu.catalogStale() && choice != 0;
        automatic.setSelected(choice == 0);
        lock.setSelected(choice == selected+1);
        priority.active = stock.active = !filtered.isEmpty() && menu.canEditAutomation(selected);
        clearRules.active = !menu.catalogStale() && menu.hasAutomation() && menu.automationRuleCount() > 0;
        priority.setMessage(tr("automation_priority", menu.catalog().isEmpty() ? 0 : menu.recipePriority(selected)));
        stock.setMessage(tr("automation_stock", targetLabel()));
        super.extractRenderState(g,mx,my,delta);
        // The lamp: green while a recipe is locked, amber when the catalogue is stale or the lock is unavailable.
        boolean locked = choice > 0 && choice <= menu.catalog().size();
        int lamp = menu.catalogStale() || (choice > 0 && !locked) ? theme.warn() : locked ? theme.ok() : theme.dim();
        ScreenKit.title(g, font, theme, left, top, tr("recipes_of", parent.getTitle()), lamp, panelWidth - 118);
        Component state = menu.catalogStale() ? tr("catalog_stale") : choice == 0 ? tr("automatic_hint")
                : locked ? tr("locked_recipe",name(choice-1)) : tr("lock_unavailable");
        // On the casing, so in its title colour (or the warning colour).
        g.text(font,ScreenKit.fit(font,state.getString(),panelWidth-16),left+8,top+17,menu.catalogStale()||(choice>0&&!locked)?theme.warn():theme.label(),false);
        if (mx>=left && mx<left+panelWidth && my>=top+15 && my<top+27) g.setTooltipForNextFrame(font,state,mx,my);
        int paneBottom = top + panelHeight - KEYS_HEIGHT;
        String count = tr("recipe_count", filtered.size()).getString();
        g.text(font,count,left+(listWidth-font.width(count))/2,paneBottom-16,theme.dim(),false);
        int px=left+listWidth+8, py=top+PANES_TOP+5, pw=panelWidth-listWidth-20, bottom=paneBottom-4;
        var lines=preview();
        int total=0;
        for(var line:lines) total+=Math.max(line.icon().isEmpty()?0:18,font.split(line.text(),pw-20).size()*10)+5;
        detailScroll=Math.clamp(detailScroll,0,Math.max(0,total-(bottom-py)));
        g.enableScissor(px,py,left+panelWidth-8,bottom);
        int yy=py-detailScroll;
        for(var line:lines) {
            if(!line.icon().isEmpty()) g.item(line.icon(),px,yy);
            int ty=yy;
            for(var wrapped:font.split(line.text(),pw-20)) {g.text(font,wrapped,px+20,ty,line.color(),false);ty+=10;}
            yy+=Math.max(line.icon().isEmpty()?0:18,ty-yy)+5;
        }
        g.disableScissor();
        if(total>bottom-py) {
            int track=bottom-py, thumb=Math.max(12,track*track/total);
            int sy=py+(track-thumb)*detailScroll/Math.max(1,total-track);
            g.fill(left+panelWidth-11,sy,left+panelWidth-9,sy+thumb,theme.progress());
        }
    }
}
