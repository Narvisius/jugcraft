package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.machine.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Supplied upgrade modules occupy existing processor slots; all transfers use native transactions and claims. */
public final class IndustrialUpgrades {
    public enum Kind { OVERCLOCKER, ENERGY_STORAGE, TRANSFORMER, EJECTOR, PULLING, FLUID_EJECTOR, REDSTONE_INVERTER }
    public static final Map<Kind, Upgrade> ITEMS = new LinkedHashMap<>();
    private IndustrialUpgrades() {}
    public static void register() {
        for (var kind : Kind.values()) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + kind.name().toLowerCase(Locale.ROOT) + "_upgrade"));
            ITEMS.put(kind, Registry.register(BuiltInRegistries.ITEM, key, new Upgrade(new Item.Properties().setId(key).stacksTo(4), kind)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> ITEMS.values().forEach(out::accept));
    }
    public static int count(MachineBlockEntity machine, Kind kind) {
        int count = 0;
        for (int slot = machine.kind().slots; slot < machine.kind().containerSize(); slot++) {
            var stack = machine.getItem(slot);
            if (stack.getItem() instanceof Upgrade upgrade && upgrade.kind == kind) count += stack.getCount();
        }
        return Math.min(4, count);
    }
    public static boolean invertRedstone(MachineBlockEntity machine, boolean powered) {
        return count(machine, Kind.REDSTONE_INVERTER) > 0 ? !powered : powered;
    }
    public static SimpleEnergyStorage buffer(MachineBlockEntity machine, long capacity, long input, long output) {
        return new SimpleEnergyStorage(capacity + 40_000, input * 256, output, machine::setChanged) {
            private boolean restoringAmount;
            @Override public long getCapacity() {
                // Removing storage strands already stored charge until spent; never silently destroys it or creates more.
                return restoringAmount ? capacity + 40_000 : Math.max(getAmount(), capacity + 10_000L * count(machine, Kind.ENERGY_STORAGE));
            }
            @Override public void setAmount(long amount) {
                // Loading a save with removed modules must preserve stranded charge as well.
                restoringAmount = true;
                try { super.setAmount(amount); } finally { restoringAmount = false; }
            }
            @Override public long insert(long amount, TransactionContext transaction) {
                long limit = input * (1L << (2 * count(machine, Kind.TRANSFORMER)));
                return super.insert(Math.min(amount, limit), transaction);
            }
        };
    }
    private static int face(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("jimtafacts_upgrade_face", -1), -1, 5);
    }
    public static void tick(MachineBlockEntity machine, ServerLevel level) {
        if (machine.kind().upgradeSlots() == 0 || level.getGameTime() % 8 != 0) return;
        transfer(machine, level);
    }
    public static void transfer(MachineBlockEntity machine, ServerLevel level) {
        if (machine.kind().upgradeSlots() == 0) return;
        boolean itemMoved = false, fluidMoved = false;
        // At most two upgrade stacks and six loaded neighbors. No remote lookups or chunk loading.
        for (int slot = machine.kind().slots; slot < machine.kind().containerSize(); slot++) {
            var stack = machine.getItem(slot);
            if (!(stack.getItem() instanceof Upgrade upgrade)) continue;
            boolean fluid = upgrade.kind == Kind.FLUID_EJECTOR;
            if ((!fluid && itemMoved) || fluid && fluidMoved || !(fluid || upgrade.kind == Kind.EJECTOR || upgrade.kind == Kind.PULLING)) continue;
            int selected = face(stack); long budget = fluid ? 250L * FluidNetworks.DROPLETS_PER_MB : Math.min(16, 4 * stack.getCount());
            for (var side : Direction.values()) {
                if (selected >= 0 && selected != side.ordinal()) continue;
                var from = machine.getBlockPos(); var to = from.relative(side);
                if (!ClaimTransfers.allowed(level, from, to)) continue;
                var state = level.getBlockState(to);
                if (state.getBlock() instanceof MachineBlock b && b.masterPos(to, state).equals(from)) continue;
                try (var transaction = Transaction.openOuter()) {
                    long moved = 0;
                    if (fluid) {
                        var source = machine.fluidFor(side); var target = FluidStorage.SIDED.find(level, to, side.getOpposite());
                        if (source != null && target != null) moved = StorageUtil.move(source, target, variant -> true, budget, transaction);
                    } else {
                        var own = ItemStorage.SIDED.find(level, from, side);
                        var neighbor = ItemStorage.SIDED.find(level, to, side.getOpposite());
                        if (own != null && neighbor != null) moved = upgrade.kind == Kind.PULLING
                                ? StorageUtil.move(neighbor, own, variant -> true, budget, transaction)
                                : StorageUtil.move(own, neighbor, variant -> true, budget, transaction);
                    }
                    if (moved > 0) {
                        transaction.commit(); machine.setChanged();
                        if (fluid) fluidMoved = true; else itemMoved = true;
                        break;
                    }
                }
            }
        }
    }
    public static final class Upgrade extends Item {
        public final Kind kind;
        Upgrade(Properties properties, Kind kind) { super(properties); this.kind = kind; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown() || player.isSpectator() || !(kind == Kind.EJECTOR || kind == Kind.PULLING || kind == Kind.FLUID_EJECTOR)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                var stack = player.getItemInHand(hand); int next = face(stack) == 5 ? -1 : face(stack) + 1;
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("jimtafacts_upgrade_face", next));
                stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) next + 1), List.of(), List.of(), List.of()));
                player.sendOverlayMessage(Component.literal("Transfer face: " + (next < 0 ? "any" : Direction.values()[next].getName())));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable("tooltip.jugcraft.jimta_" + kind.name().toLowerCase(Locale.ROOT) + "_upgrade").withStyle(ChatFormatting.GRAY));
            if (kind == Kind.EJECTOR || kind == Kind.PULLING || kind == Kind.FLUID_EJECTOR)
                lines.accept(Component.literal("Sneak-use to choose face: " + (face(stack) < 0 ? "any" : Direction.values()[face(stack)].getName())).withStyle(ChatFormatting.AQUA));
        }
    }
}
