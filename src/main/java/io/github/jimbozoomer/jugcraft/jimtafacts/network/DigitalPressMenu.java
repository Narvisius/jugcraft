package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaContainer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Name-press engraving: a real ingot and one knife durability per output, tied to the held knife. */
public final class DigitalPressMenu {
    public static MenuType<Menu> TYPE;
    private DigitalPressMenu() {}
    public static void register() {
        TYPE = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("jimta_name_press"), new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.serverboundPlay().register(Name.TYPE, Name.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Name.TYPE, (packet, context) -> context.server().execute(() -> {
            var p = context.player(); if (p.containerMenu instanceof Menu menu && menu.containerId == packet.menu && menu.stillValid(p) && RateGate.allow(p, "jimta_press_name", 2)) menu.name(packet.value);
        }));
    }
    public record Name(int menu, String value) implements CustomPacketPayload {
        public static final Type<Name> TYPE = new Type<>(Jugcraft.id("jimta_press_name"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Name> CODEC = StreamCodec.of((b, p) -> { b.writeVarInt(p.menu); b.writeUtf(p.value, 256); }, b -> new Name(b.readVarInt(), b.readUtf(256)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static boolean ingot(ItemStack s) { return s.is(Items.IRON_INGOT) || s.is(Items.COPPER_INGOT) || s.is(Items.GOLD_INGOT) || s.is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots"))); }
    public static final class Menu extends AbstractContainerMenu {
        private final JimtaContainer input = new JimtaContainer(1), result = new JimtaContainer(1);
        private final Inventory inventory;
        private final InteractionHand hand;
        private final ItemStack knife;
        private String name = "";
        public Menu(int id, Inventory inventory) { this(id, inventory, null, ItemStack.EMPTY); }
        public Menu(int id, Inventory inventory, InteractionHand hand, ItemStack knife) {
            super(TYPE, id); this.inventory = inventory; this.hand = hand; this.knife = knife;
            input.addListener(ignored -> refresh());
            addSlot(new Slot(input, 0, 40, 51) { @Override public boolean mayPlace(ItemStack s) { return ingot(s); } });
            addSlot(new Slot(result, 0, 120, 51) {
                @Override public boolean mayPlace(ItemStack s) { return false; }
                @Override public boolean mayPickup(Player p) { return p.level().isClientSide() || stillValid(p) && ingot(input.getItem(0)) && !name.isBlank(); }
                @Override public void onTake(Player p, ItemStack s) {
                    if (p instanceof ServerPlayer && stillValid(p)) { input.removeItem(0, 1); knife.hurtAndBreak(1, p, hand); refresh(); }
                    super.onTake(p, s);
                }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 94 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 152));
        }
        public void name(String value) { name = value.codePoints().filter(c -> !Character.isISOControl(c)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString().strip(); refresh(); broadcastFullState(); }
        private void refresh() {
            if (hand == null) return;
            ItemStack output = ItemStack.EMPTY;
            if (hand != null && stillValid(inventory.player) && !name.isBlank() && ingot(input.getItem(0))) { output = new ItemStack(DigitalStorage.ITEMS.get("name_press")); output.set(DigitalEquipment.PRESS_NAME, name); output.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Name Press: " + name)); }
            result.setItem(0, output);
        }
        @Override public boolean stillValid(Player p) { return p.level().isClientSide() || hand != null && p.isAlive() && !p.isSpectator() && p.getItemInHand(hand) == knife && !knife.isEmpty() && knife.getItem() instanceof DigitalEquipment.Knife; }
        @Override public void clicked(int slot, int button, ContainerInput type, Player p) { if (stillValid(p)) super.clicked(slot, button, type, p); }
        @Override public ItemStack quickMoveStack(Player p, int index) {
            if (!stillValid(p) || index == 1 || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
            var slot = slots.get(index); var stack = slot.getItem(); var original = stack.copy();
            if (index == 0 ? !moveItemStackTo(stack, 2, slots.size(), true) : !ingot(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack); slot.setChanged(); return original;
        }
        @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container != result && super.canTakeItemForPickAll(stack, slot); }
        @Override public void removed(Player p) { super.removed(p); if (!p.level().isClientSide()) clearContainer(p, input); result.clearContent(); }
    }
}
