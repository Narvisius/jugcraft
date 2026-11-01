package io.github.jimbozoomer.jugcraft.crafting;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.mixin.CraftingResultAccess;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.gamerules.GameRules;

/** A menu-local choice among real server recipes. Never an item-grant or client-authored output. */
public final class CraftingChoices {
    public static final int MAX_OPTIONS = 1024;
    private static final AtomicLong REVISIONS = new AtomicLong();
    private static final Map<ResultContainer, State> STATES = new WeakHashMap<>();
    private static final Map<RecipeManager, List<RecipeHolder<CraftingRecipe>>> CATALOGS = new WeakHashMap<>();
    public static final class State {
        private RecipeManager manager;
        private CraftingInput input;
        private List<RecipeHolder<CraftingRecipe>> matches = List.of();
        private RecipeHolder<CraftingRecipe> selected;
        private List<Option> options = List.of();
        private long revision;
        public long revision() { return revision; }
        public List<Option> options() { return options; }
        public RecipeHolder<CraftingRecipe> selected() { return selected; }
    }
    public record Option(Identifier recipe, ItemStack output) { }
    public record Grid(CraftingContainer inputs, ResultContainer result, int slot) { }
    private CraftingChoices() { }
    public static State state(ResultContainer result) { return STATES.computeIfAbsent(result, ignored -> new State()); }
    public static boolean supported(AbstractContainerMenu menu) {
        return menu instanceof AbstractCraftingMenu
            || menu instanceof io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalTerminal.Menu
            || menu instanceof io.github.jimbozoomer.jugcraft.nouveau.NouveauStorageTerminal.Menu;
    }
    public static Grid grid(AbstractContainerMenu menu) {
        if (!supported(menu)) return null;
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            if (slot instanceof ResultSlot && slot.container instanceof ResultContainer result)
                return new Grid(((CraftingResultAccess)slot).jugcraft$inputs(), result, i);
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public static List<RecipeHolder<CraftingRecipe>> recipes(RecipeManager manager) {
        return CATALOGS.computeIfAbsent(manager, key -> key.getRecipes().stream()
            .filter(r -> r.value() instanceof CraftingRecipe)
            .map(r -> (RecipeHolder<CraftingRecipe>)(RecipeHolder<?>)r).toList());
    }
    private static boolean allowed(ServerPlayer player, RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().isSpecial() || !player.level().getGameRules().get(GameRules.LIMITED_CRAFTING)
            || player.getRecipeBook().contains(recipe.id());
    }
    private static boolean same(ItemStack a, ItemStack b) {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
    }
    private static boolean sameOptions(List<Option> a, List<Option> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).recipe.equals(b.get(i).recipe) || !same(a.get(i).output, b.get(i).output)) return false;
        return true;
    }
    public static void update(AbstractContainerMenu menu, ServerPlayer player, CraftingContainer inputs,
                              ResultContainer result, RecipeHolder<CraftingRecipe> hint) {
        var state = state(result);
        var manager = player.level().getServer().getRecipeManager();
        var input = inputs.asCraftInput();
        boolean changed = state.manager != manager || !input.equals(state.input);
        if (changed) {
            state.manager = manager;
            state.input = CraftingInput.of(input.width(), input.height(), input.items().stream().map(ItemStack::copy).toList());
            state.matches = input.isEmpty() ? List.of() : recipes(manager).stream()
                .filter(r -> r.value().matches(input, player.level())).toList();
        }
        var valid = new ArrayList<RecipeHolder<CraftingRecipe>>();
        var options = new ArrayList<Option>();
        for (var recipe : state.matches) {
            if (!allowed(player, recipe)) continue;
            var output = recipe.value().assemble(input);
            if (output.isEmpty() || !output.isItemEnabled(player.level().enabledFeatures())) continue;
            valid.add(recipe);
            if (options.stream().noneMatch(o -> same(o.output, output)))
                options.add(new Option(recipe.id().identifier(), output.copy()));
        }
        var selected = hint != null && valid.contains(hint) ? hint : state.selected;
        if (selected == null || !valid.contains(selected)) selected = valid.isEmpty() ? null : valid.getFirst();
        // Keep the chosen recipe's identity even where another recipe has the identical output.
        if (selected != null) {
            ItemStack selectedOutput = selected.value().assemble(input);
            for (int i = 0; i < options.size(); i++) if (same(options.get(i).output, selectedOutput))
                options.set(i, new Option(selected.id().identifier(), selectedOutput.copy()));
        }
        changed |= !Objects.equals(state.selected, selected) || !sameOptions(state.options, options);
        state.selected = selected; state.options = List.copyOf(options);
        if (changed || state.revision == 0) state.revision = REVISIONS.incrementAndGet();
        ItemStack output = ItemStack.EMPTY;
        result.setRecipeUsed(null);
        if (selected != null && result.setRecipeUsed(player, selected)) output = selected.value().assemble(input);
        result.setItem(0, output);
        int slot = -1;
        for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i).container == result) { slot = i; break; }
        if (slot >= 0) {
            menu.setRemoteSlot(slot, output);
            player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), slot, output));
        }
        if (changed) send(player, menu, state);
    }
    public static void refresh(AbstractContainerMenu menu, ServerPlayer player) {
        var grid = grid(menu);
        if (grid != null) update(menu, player, grid.inputs, grid.result, null);
    }
    /** A reload or changed limited-crafting rule must not leave a takeable stale result. */
    public static void beforeClick(AbstractContainerMenu menu, ServerPlayer player) {
        var grid = grid(menu);
        if (grid == null) return;
        var state = STATES.get(grid.result);
        if (state != null && (state.manager != player.level().getServer().getRecipeManager()
                || state.selected != null && !allowed(player, state.selected))) refresh(menu, player);
    }
    public static boolean select(ServerPlayer player, int menuId, long revision, Identifier recipe) {
        var menu = player.containerMenu;
        var grid = grid(menu);
        if (menu.containerId != menuId || grid == null || !menu.stillValid(player) || !player.isAlive()
                || player.isSpectator() || !menu.getCarried().isEmpty()
                || !menu.slots.get(grid.slot).isActive()) return false;
        update(menu, player, grid.inputs, grid.result, null);
        var state = state(grid.result);
        if (state.revision != revision || state.options.stream().noneMatch(o -> o.recipe.equals(recipe))) {
            send(player, menu, state); return false;
        }
        var chosen = state.matches.stream().filter(r -> r.id().identifier().equals(recipe)).findFirst().orElse(null);
        if (chosen == null || !allowed(player, chosen)) return false;
        update(menu, player, grid.inputs, grid.result, chosen);
        menu.broadcastChanges();
        return true;
    }
    public static RecipeHolder<CraftingRecipe> remainingRecipe(ResultContainer result, CraftingInput input, ServerPlayer player) {
        var state = STATES.get(result);
        return state != null && state.manager == player.level().getServer().getRecipeManager()
            && state.selected != null && state.selected.value().matches(input, player.level()) ? state.selected : null;
    }
    private static void send(ServerPlayer player, AbstractContainerMenu menu, State state) {
        if (ServerPlayNetworking.canSend(player, Options.TYPE)) {
            // Recipe packs exceeding this generous bound must be fixed rather than emitting an unbounded packet.
            if (state.options.size() > MAX_OPTIONS) { Jugcraft.LOGGER.warn("Too many crafting choices: {}", state.options.size()); return; }
            ServerPlayNetworking.send(player, new Options(menu.containerId, state.revision, state.options));
        }
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Options.TYPE, Options.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (packet, context) -> {
            var player = context.player(); var menu = player.containerMenu;
            if (menu.containerId != packet.menu || !supported(menu) || !menu.stillValid(player)
                    || !player.isAlive() || player.isSpectator()) return;
            // Opening/reopening a screen queries its options. That must not consume the
            // selection budget and discard the player's first click in the same tick.
            String rateKey = packet.recipe.isEmpty() ? "crafting_choices_query" : "crafting_choices_select";
            if (!RateGate.allow(player, rateKey, 2)) return;
            if (packet.recipe.isEmpty()) {
                refresh(menu, player); var grid = grid(menu);
                if (grid != null) send(player, menu, state(grid.result));
            } else {
                var id = Identifier.tryParse(packet.recipe);
                if (id != null) select(player, packet.menu, packet.revision, id);
            }
        });
    }
    public record Request(int menu, long revision, String recipe) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Jugcraft.id("crafting_choice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            public Request decode(RegistryFriendlyByteBuf b) { return new Request(b.readVarInt(), b.readLong(), b.readUtf(256)); }
            public void encode(RegistryFriendlyByteBuf b, Request p) { b.writeVarInt(p.menu); b.writeLong(p.revision); b.writeUtf(p.recipe, 256); }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Options(int menu, long revision, List<Option> entries) implements CustomPacketPayload {
        public static final Type<Options> TYPE = new Type<>(Jugcraft.id("crafting_choices"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Options> CODEC = new StreamCodec<>() {
            public Options decode(RegistryFriendlyByteBuf b) {
                int menu = b.readVarInt(); long revision = b.readLong(); int size = b.readVarInt();
                if (size < 0 || size > MAX_OPTIONS) throw new IllegalArgumentException("Invalid crafting choice count");
                var entries = new ArrayList<Option>();
                for (int i = 0; i < size; i++) entries.add(new Option(b.readIdentifier(), ItemStack.STREAM_CODEC.decode(b)));
                return new Options(menu, revision, List.copyOf(entries));
            }
            public void encode(RegistryFriendlyByteBuf b, Options p) {
                b.writeVarInt(p.menu); b.writeLong(p.revision); b.writeVarInt(p.entries.size());
                for (var entry : p.entries) { b.writeIdentifier(entry.recipe); ItemStack.STREAM_CODEC.encode(b, entry.output); }
            }
        };
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
