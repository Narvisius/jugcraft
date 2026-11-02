package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.pets.Pets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;

/** Compact companion journal: server-owned rows, no entity NBT or client-selected ownership. */
public final class PetInventoryScreen extends Screen {
    private Pets.Snapshot data;
    private int page, selected, left, top, panelWidth, panelHeight;
    public PetInventoryScreen(Pets.Snapshot data) { super(Component.literal("Companions")); this.data = data; }
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Pets.View.TYPE, (packet, context) -> {
            var mc = Minecraft.getInstance(); var data = Pets.JSON.fromJson(packet.json(), Pets.Snapshot.class);
            if (mc.gui.screen() instanceof PetInventoryScreen screen) { if(screen.data.offset()!=data.offset())screen.selected=0; screen.data = data; screen.rebuildWidgets(); }
            else mc.gui.setScreen(new PetInventoryScreen(data));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)
                Screens.getWidgets(screen).add(Button.builder(Component.literal("Pets"), b -> ClientPlayNetworking.send(new Pets.Action("", "open"))).bounds(4, 4, 42, 18).build());
        });
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        panelWidth = Math.min(410, width - 12); panelHeight = Math.min(260, height - 12);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        selected = Math.max(0, Math.min(selected, data.pets().size() - 1));
        page = selected / 5;
        int listWidth = Math.min(165, panelWidth / 2 - 8);
        for (int i = page * 5; i < Math.min(data.pets().size(), page * 5 + 5); i++) {
            final int index = i; var pet = data.pets().get(i);
            button((i == selected ? "> " : "") + font.plainSubstrByWidth(pet.name(), listWidth - 25), left + 10, top + 40 + (i % 5) * 25, listWidth,
                () -> { selected = index; rebuildWidgets(); });
        }
        button("<", left + 10, top + panelHeight - 30, 24, () -> { if(selected<5&&data.offset()>0)ClientPlayNetworking.send(new Pets.Action(Integer.toString(data.offset()-200),"page"));else {selected = Math.max(0, selected - 5); rebuildWidgets();} });
        button(">", left + 38, top + panelHeight - 30, 24, () -> { if(selected+5>=data.pets().size()&&data.offset()+data.pets().size()<data.total())ClientPlayNetworking.send(new Pets.Action(Integer.toString(data.offset()+200),"page"));else {selected = Math.min(data.pets().size() - 1, selected + 5); rebuildWidgets();} });
        button("Refresh", left + 67, top + panelHeight - 30, 65, () -> send("open"));
        button("Done", left + panelWidth - 62, top + panelHeight - 30, 52, this::onClose);
        if (!data.pets().isEmpty()) {
            var pet = data.pets().get(selected); int x = left + listWidth + 22, w = panelWidth - listWidth - 32;
            button(pet.state().equals("ACTIVE") ? "Recall pet" : "Summon pet", x, top + 87, w, () -> send(pet.state().equals("ACTIVE") ? "recall" : "summon"));
            button("Mode: " + pet.mode(), x, top + 113, w, () -> send(Pets.MODES.get((Pets.MODES.indexOf(pet.mode()) + 1) % Pets.MODES.size())));
        }
    }
    private void send(String action) { ClientPlayNetworking.send(new Pets.Action(data.pets().isEmpty() ? "" : data.pets().get(selected).id(), action)); }
    private void button(String text, int x, int y, int w, Runnable action) { addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build()); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFFB69A62);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xFF202D2A);
        g.fill(left + 4, top + 4, left + panelWidth - 4, top + 29, 0xFF344A40);
        g.text(font, "COMPANIONS  (" + data.total() + ")", left + 12, top + 12, 0xFFF4E1AD, false);
        int x = left + Math.min(165, panelWidth / 2 - 8) + 22;
        if (data.pets().isEmpty()) g.text(font, "Tame a pet to begin your collection.", left + 12, top + 50, 0xFFE2D3B2, false);
        else {
            var pet = data.pets().get(selected);
            g.text(font, "Level " + pet.level(), x, top + 38, 0xFFF4E1AD, false);
            g.text(font, "Bond " + pet.xp(), x, top + 51, 0xFFE2D3B2, false);
            g.text(font, pet.state().replace('_', ' '), x, top + 69, 0xFFBBD6B7, false);
            var entry=new net.minecraft.nbt.CompoundTag();entry.putString("Claim",pet.id()+pet.preview());entry.putString("Type",pet.type());
            var preview=net.minecraft.nbt.CompoundTag.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(pet.preview())).result().orElse(new net.minecraft.nbt.CompoundTag());entry.put("Preview",preview);
            var visual=local.peepo.client.TransportCratePreview.mob(entry);
            int px=left+panelWidth-61,py=top+34;
            g.fill(px-1,py-1,px+51,py+49,0xFFAB9466);g.fill(px,py,px+50,py+48,0xFF293B34);
            if(visual!=null){int size=(int)Math.min(35,Math.min(35/Math.max(.1F,visual.getBbHeight()),38/Math.max(.1F,visual.getBbWidth())));InventoryScreen.extractEntityInInventoryFollowsMouse(g,px+1,py+1,px+49,py+47,size,.1F,mx,my,visual);}
            int y = top + 141;
            String help = "Travel together to improve health, strength and pace. Level 5 defenders reveal threats. Knockouts recover in one minute.";
            for (var line : font.split(Component.literal(help), panelWidth - (x - left) - 12)) { if (y > top + panelHeight - 65) break; g.text(font, line, x, y, 0xFFC2C8B5, false); y += 11; }
        }
        int y = top + panelHeight - 58;
        for (var line : font.split(Component.literal(data.message()), panelWidth - 20)) { if (y > top + panelHeight - 36) break; g.text(font, line, left + 10, y, 0xFFEBCB83, false); y += 10; }
        super.extractRenderState(g, mx, my, delta);
    }
}
