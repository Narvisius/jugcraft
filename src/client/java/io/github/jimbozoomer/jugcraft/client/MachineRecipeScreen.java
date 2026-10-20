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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Read-only preview until Lock is clicked. The retained open menu owns synchronization and authority. */
public final class MachineRecipeScreen extends Screen {
    private final FormMachineScreen parent;
    private final FormMachineMenu menu;
    private final List<Button> rows = new ArrayList<>();
    private List<Integer> filtered = List.of();
    private String query = "";
    private int selected, offset, detailScroll, left, top, panelWidth, panelHeight, listWidth, rowCount;
    private Button lock, automatic, priority, stock, clearRules;
    private EditBox search;
    private record Line(ItemStack icon, Component text, int color) { }
    private static final int TEXT = 0xFFD4E1DF, DIM = 0xFF9CADAA, GREEN = 0xFF89C9A4;
    // Minecraft key input codes, shared with HandbookScreen's navigation.
    private static final int KEY_PAGE_UP = 266, KEY_PAGE_DOWN = 267;

    public MachineRecipeScreen(FormMachineScreen parent, FormMachineMenu menu) {
        super(tr("recipes")); this.parent = parent; this.menu = menu;
        selected = menu.selectedRecipe() > 0 && menu.selectedRecipe() <= menu.catalog().size() ? menu.selectedRecipe() - 1 : 0;
    }
    private static Component tr(String key, Object... args) { return Component.translatable("container.jugcraft.form." + key, args); }
    @Override protected void init() {
        panelWidth = Math.min(640, width - 12); panelHeight = Math.min(400, height - 12);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        listWidth = Math.min(190, Math.max(110, panelWidth / 3));
        search = addRenderableWidget(new EditBox(font, left + 8, top + 40, listWidth - 16, 18, tr("search_recipes")));
        search.setMaxLength(100); search.setValue(query);
        search.setResponder(value -> { query = value; offset = 0; detailScroll = 0; refresh(); });
        rows.clear(); rowCount = Math.max(1, (panelHeight - 136) / 22);
        for (int i = 0; i < rowCount; i++) {
            final int row = i;
            rows.add(addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (offset + row < filtered.size()) { selected = filtered.get(offset + row); detailScroll = 0; refresh(); }
            }).bounds(left + 8, top + 64 + i * 22, listWidth - 16, 20).build()));
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> scrollList(-rowCount)).bounds(left + 8, top + panelHeight - 69, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> scrollList(rowCount)).bounds(left + listWidth - 32, top + panelHeight - 69, 24, 18).build());
        lock = addRenderableWidget(Button.builder(tr("lock_recipe"), b -> request(FormMachineMenu.BUTTON_RECIPE + selected))
                .bounds(left + listWidth + 8, top + panelHeight - 47, panelWidth - listWidth - 16, 18).build());
        automatic = addRenderableWidget(Button.builder(tr("automatic"), b -> request(FormMachineMenu.BUTTON_AUTO))
                .bounds(left + 8, top + panelHeight - 47, listWidth - 16, 18).build());
        addRenderableWidget(Button.builder(tr("back_machine"), b -> onClose()).bounds(left + 8, top + panelHeight - 24, listWidth - 16, 18).build());
        int controlWidth = (panelWidth - listWidth - 20) / 2;
        priority = addRenderableWidget(Button.builder(tr("automation_priority", 0), b -> request(FormMachineMenu.BUTTON_PRIORITY + selected))
            .bounds(left + listWidth + 8, top + panelHeight - 24, controlWidth, 18).build());
        stock = addRenderableWidget(Button.builder(tr("automation_stock", tr("automation_off")), b -> request(FormMachineMenu.BUTTON_TARGET + selected))
            .bounds(left + listWidth + 12 + controlWidth, top + panelHeight - 24, controlWidth, 18).build());
        clearRules = addRenderableWidget(Button.builder(tr("automation_reset"), b -> request(FormMachineMenu.BUTTON_CLEAR_RULES))
            .bounds(left + panelWidth - 116, top + 3, 108, 18).build());
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
                button.setMessage(Component.literal((index == selected ? "> " : "") + font.plainSubstrByWidth(name(index).getString(), listWidth - 36)));
                button.setTooltip(Tooltip.create(name(index).copy().append("\n" + menu.catalog().get(index).id())));
            }
        }
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
        if (filtered.isEmpty()) { text(lines, tr(menu.catalog().isEmpty() ? "no_recipes" : "no_recipe_matches"), DIM); return lines; }
        var r = menu.catalog().get(selected).recipe();
        text(lines, name(selected), GREEN);
        var portDetails = new ArrayList<Line>();
        text(lines, tr("recipe_inputs"), GREEN);
        for (int i = 0; i < r.items().size(); i++) {
            var part = r.items().get(i);
            var names = part.ingredient().items().map(item -> new ItemStack(item).getHoverName().getString()).toList();
            var icon = new ItemStack(part.ingredient().items().findFirst().orElseThrow());
            lines.add(new Line(icon, tr("recipe_item_in", i+1, part.count(), String.join(" / ", names)), TEXT));
            text(portDetails, Component.literal(port(FormPort.Kind.ITEM_IN, i)), DIM);
        }
        for (int i = 0; i < r.fluids().size(); i++) {
            var f = r.fluids().get(i);
            lines.add(new Line(new ItemStack(f.fluid().getBucket()), tr("recipe_fluid_in", i+1, f.amount(), fluidName(f.fluid())), TEXT));
            text(portDetails, Component.literal(port(FormPort.Kind.FLUID_IN, i)), DIM);
        }
        r.tool().ifPresentOrElse(tool -> {
            String names = String.join(" / ", tool.items().map(item -> new ItemStack(item).getHoverName().getString()).toList());
            boolean present = false;
            for (int i = 0; i < menu.form().sockets().size(); i++) if (tool.test(menu.slots.get(menu.form().firstSocketSlot()+i).getItem())) present = true;
            text(lines, tr(present ? "recipe_tool_present" : "recipe_tool_missing", names), present ? GREEN : 0xFFFFB678);
        }, () -> text(lines, tr("recipe_no_tool"), DIM));
        text(lines, tr("recipe_outputs"), GREEN);
        for (int i = 0; i < r.results().size(); i++) {
            var item = r.results().get(i).create();
            lines.add(new Line(item, tr("recipe_item_out", i+1, item.getCount(), item.getHoverName()), TEXT));
            text(portDetails, Component.literal(port(FormPort.Kind.ITEM_OUT, i)), DIM);
        }
        for (int i = 0; i < r.fluidResults().size(); i++) {
            var f = r.fluidResults().get(i); int tank = f.tank() < 0 ? i : f.tank();
            lines.add(new Line(new ItemStack(f.fluid().getBucket()), tr("recipe_fluid_out", tank+1, f.amount(), fluidName(f.fluid())), TEXT));
            text(portDetails, MachineStatus.tankLabel(menu.form(), menu.form().tanks().inputTanks().size()+tank).copy().append(" / " + port(FormPort.Kind.FLUID_OUT, tank)), DIM);
            if (f.retained()) text(lines, tr("recipe_retained"), DIM);
            if (menu.form().waterDrainOutput() == tank) text(lines, tr("recipe_overflow"), DIM);
        }
        text(lines, menu.hasAutomation() ? tr("automation_summary", menu.recipePriority(selected), targetLabel())
            : tr("automation_missing"), DIM);
        var effect = menu.upgrades();
        int ticks = effect.ticks(r.time());
        long rate = menu.form().profile().lanePerTick(effect.use(r.energyPerTick() > 0 ? r.energyPerTick() : menu.form().family().usePerTick));
        text(lines, tr("recipe_cost", ticks, String.format(Locale.ROOT,"%.1f",ticks/20.0), rate, rate*ticks), TEXT);
        text(lines, tr("recipe_cost_hint", menu.form().profile().lanes()), DIM);
        text(lines, tr("next_batch_hint"), DIM);
        text(lines, tr("recipe_ports_hint"), DIM);
        lines.addAll(portDetails);
        text(lines, tr("recipe_status").copy().append(menu.status().title(menu.form())).append(". ").append(menu.status().detail(menu.form())), TEXT);
        return lines;
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractBackground(g,x,y,delta);
        g.fill(left-1,top-1,left+panelWidth+1,top+panelHeight+1,0xFF6D8C87);
        g.fill(left,top,left+panelWidth,top+panelHeight,0xFF15252B);
        g.fill(left+listWidth,top+38,left+listWidth+1,top+panelHeight-52,0xFF48645F);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        lock.active = !menu.catalogStale() && !filtered.isEmpty() && menu.selectedRecipe() != selected+1;
        automatic.active = !menu.catalogStale() && menu.selectedRecipe() != 0;
        priority.active = stock.active = !filtered.isEmpty() && menu.canEditAutomation(selected);
        clearRules.active = !menu.catalogStale() && menu.hasAutomation() && menu.automationRuleCount() > 0;
        priority.setMessage(tr("automation_priority", menu.catalog().isEmpty() ? 0 : menu.recipePriority(selected)));
        stock.setMessage(tr("automation_stock", targetLabel()));
        super.extractRenderState(g,mx,my,delta);
        if (query.isEmpty()) g.text(font,tr("search_recipes"),left+12,top+45,DIM,false);
        g.text(font,title,left+8,top+8,GREEN,false);
        int choice = menu.selectedRecipe();
        Component state = menu.catalogStale() ? tr("catalog_stale") : choice == 0 ? tr("automatic_hint")
                : choice <= menu.catalog().size() ? tr("locked_recipe",name(choice-1)) : tr("lock_unavailable");
        g.text(font,font.plainSubstrByWidth(state.getString(),panelWidth-16),left+8,top+23,menu.catalogStale()?0xFFFFB678:TEXT,false);
        if (mx>=left && mx<left+panelWidth && my>=top+21 && my<top+35) g.setTooltipForNextFrame(font,state,mx,my);
        g.text(font,filtered.size()+" recipes",left+38,top+panelHeight-64,DIM,false);
        int px=left+listWidth+8, py=top+41, pw=panelWidth-listWidth-20, bottom=top+panelHeight-55;
        var lines=preview();
        int total=0;
        for(var line:lines) total+=Math.max(line.icon().isEmpty()?0:18,font.split(line.text(),pw-20).size()*10)+5;
        detailScroll=Math.clamp(detailScroll,0,Math.max(0,total-(bottom-py)));
        g.enableScissor(px,py,left+panelWidth-6,bottom);
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
            g.fill(left+panelWidth-5,sy,left+panelWidth-3,sy+thumb,GREEN);
        }

    }
}
