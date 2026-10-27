package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.fluid.*;
import io.github.jimbozoomer.jugcraft.tools.Jetpack;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Original wearable art; fuel and foam are saved resources, never a disguised rechargeable battery. */
public final class IndustrialBackpacks {
    public static final long FUEL_CAPACITY = 30_000 * FluidNetworks.DROPLETS_PER_MB;
    public static final long FUEL_PER_TICK = 2 * FluidNetworks.DROPLETS_PER_MB;
    public static final int FOAM_CAPACITY = 800;
    /** One sprayed block consumes 100 mB. Transfers never round up a partial dose. */
    public static final long FOAM_PER_BLOCK = 100 * FluidNetworks.DROPLETS_PER_MB;
    private IndustrialBackpacks() {}
    static void registerStorage() {
        FluidStorage.ITEM.registerForItems((stack, context) -> fuelStorage(context), IndustrialGear.ITEMS.get("jimta_biogas_jetpack"));
        FluidStorage.ITEM.registerForItems((stack, context) -> foamStorage(context), IndustrialGear.ITEMS.get("jimta_construction_foam_pack"));
    }
    public static boolean fuel(FluidVariant variant) {
        return !variant.isBlank() && BuiltInRegistries.FLUID.getKey(variant.getFluid()).equals(Jugcraft.id("biogas"));
    }
    public static long fuelAmount(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelJetpack)) return 0;
        var value = stack.get(JugcraftFluids.STORED_FLUID);
        return value == null || !fuel(value.variant()) ? 0 : Math.clamp(value.amount(), 0, FUEL_CAPACITY);
    }
    private static ItemStack filled(ItemStack stack, FluidVariant variant, long amount) {
        var next = stack.copyWithCount(1);
        if (amount <= 0) next.remove(JugcraftFluids.STORED_FLUID);
        else next.set(JugcraftFluids.STORED_FLUID, new StoredFluid(variant, Math.min(amount, FUEL_CAPACITY)));
        return next;
    }
    public static Storage<FluidVariant> fuelStorage(ContainerItemContext context) {
        return new SingleSlotStorage<>() {
            private ItemStack held() { return context.getItemVariant().toStack(); }
            private boolean change(FluidVariant variant, long amount, TransactionContext tx) {
                var old = held();
                return old.getItem() instanceof FuelJetpack && context.exchange(ItemVariant.of(filled(old, variant, amount)), 1, tx) == 1;
            }
            @Override public long insert(FluidVariant resource, long maximum, TransactionContext tx) {
                if (!fuel(resource) || maximum <= 0 || !(held().getItem() instanceof FuelJetpack)) return 0;
                long old = getAmount(), accepted = Math.min(maximum, FUEL_CAPACITY - old);
                return accepted > 0 && change(resource, old + accepted, tx) ? accepted : 0;
            }
            @Override public long extract(FluidVariant resource, long maximum, TransactionContext tx) {
                if (!getResource().equals(resource) || maximum <= 0) return 0;
                long old = getAmount(), moved = Math.min(maximum, old);
                return moved > 0 && change(resource, old - moved, tx) ? moved : 0;
            }
            @Override public boolean isResourceBlank() { return getAmount() == 0; }
            @Override public FluidVariant getResource() { return getAmount() == 0 ? FluidVariant.blank() : held().get(JugcraftFluids.STORED_FLUID).variant(); }
            @Override public long getAmount() { return fuelAmount(held()); }
            @Override public long getCapacity() { return FUEL_CAPACITY; }
        };
    }
    public static final class FuelJetpack extends Item implements Jetpack {
        FuelJetpack(Properties p) { super(p); }
        @Override public boolean isJetpack(ItemStack stack) { return IndustrialGear.active(stack); }
        @Override public boolean hasThrustFuel(ItemStack stack) { return fuelAmount(stack) >= FUEL_PER_TICK; }
        @Override public boolean payThrust(ItemStack stack) {
            long amount = fuelAmount(stack);
            if (amount < FUEL_PER_TICK) return false;
            var fluid = stack.get(JugcraftFluids.STORED_FLUID).variant();
            if (amount == FUEL_PER_TICK) stack.remove(JugcraftFluids.STORED_FLUID);
            else stack.set(JugcraftFluids.STORED_FLUID, new StoredFluid(fluid, amount - FUEL_PER_TICK));
            return true;
        }
        @Override public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer();
            if (player == null || player.isSpectator() || !player.isAlive()) return InteractionResult.FAIL;
            if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
            if (!Authority.mayChange(level, player, context.getClickedPos())) return InteractionResult.FAIL;
            var tank = FluidStorage.SIDED.find(level, context.getClickedPos(), context.getClickedFace());
            if (tank == null) return InteractionResult.PASS;
            var pack = fuelStorage(ContainerItemContext.ofPlayerHand(player, context.getHand()));
            try (var tx = Transaction.openOuter()) {
                long moved = player.isSecondaryUseActive()
                        ? StorageUtil.move(pack, tank, IndustrialBackpacks::fuel, FUEL_CAPACITY, tx)
                        : StorageUtil.move(tank, pack, IndustrialBackpacks::fuel, FUEL_CAPACITY, tx);
                if (moved > 0) tx.commit();
            }
            player.sendOverlayMessage(Component.literal("Biogas: " + fuelAmount(context.getItemInHand()) / FluidNetworks.DROPLETS_PER_MB + " / 30,000 mB"));
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) return super.use(level, player, hand);
            if (!level.isClientSide() && !player.isSpectator()) {
                var stack = player.getItemInHand(hand); boolean off = IndustrialGear.active(stack);
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("JimtaGearOff", off));
                player.sendOverlayMessage(Component.literal(off ? "Jetpack disabled." : "Jetpack enabled."));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.literal("Biogas: " + fuelAmount(stack) / FluidNetworks.DROPLETS_PER_MB + " / 30,000 mB"));
            out.accept(Component.literal("Fill from a tank; sneak-use a tank to empty. Wear on chest and hold jump in the air."));
            out.accept(Component.literal("2 mB per thrust tick. Shift-use in air toggles flight. Uses existing jetpack controls."));
        }
    }
    public static int foam(ItemStack stack) {
        return stack.getItem() instanceof FoamPack ? Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("JimtaFoam", 0), 0, FOAM_CAPACITY) : 0;
    }
    public static int wornFoam(Player player) { return foam(player.getItemBySlot(EquipmentSlot.CHEST)); }
    public static boolean foamFluid(FluidVariant fluid) {
        return !fluid.isBlank() && fluid.equals(FluidVariant.of(IndustrialHeat.CONSTRUCTION_FOAM.source()));
    }
    /** Bridges legacy saved block charges to transactional fluid containers without a second balance. */
    public static Storage<FluidVariant> foamStorage(ContainerItemContext context) {
        return new SingleSlotStorage<>() {
            private ItemStack held() { return context.getItemVariant().toStack(); }
            private boolean change(long amount, TransactionContext tx) {
                var next = held().copyWithCount(1);
                if (!(next.getItem() instanceof FoamPack)) return false;
                CustomData.update(DataComponents.CUSTOM_DATA, next, tag -> tag.putInt("JimtaFoam", (int)(amount / FOAM_PER_BLOCK)));
                return context.exchange(ItemVariant.of(next), 1, tx) == 1;
            }
            @Override public long insert(FluidVariant fluid, long maximum, TransactionContext tx) {
                if (!foamFluid(fluid) || maximum <= 0) return 0;
                long old = getAmount(), moved = Math.min(maximum, getCapacity() - old) / FOAM_PER_BLOCK * FOAM_PER_BLOCK;
                return moved > 0 && change(old + moved, tx) ? moved : 0;
            }
            @Override public long extract(FluidVariant fluid, long maximum, TransactionContext tx) {
                if (!foamFluid(fluid) || maximum <= 0) return 0;
                long old = getAmount(), moved = Math.min(maximum, old) / FOAM_PER_BLOCK * FOAM_PER_BLOCK;
                return moved > 0 && change(old - moved, tx) ? moved : 0;
            }
            @Override public boolean isResourceBlank() { return getAmount() == 0; }
            @Override public FluidVariant getResource() { return isResourceBlank() ? FluidVariant.blank() : FluidVariant.of(IndustrialHeat.CONSTRUCTION_FOAM.source()); }
            @Override public long getAmount() { return foam(held()) * FOAM_PER_BLOCK; }
            @Override public long getCapacity() { return FOAM_CAPACITY * FOAM_PER_BLOCK; }
        };
    }
    public static int consumeWornFoam(Player player, int wanted) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST); int amount = foam(stack), used = Math.min(Math.max(0, wanted), amount);
        if (used > 0) CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("JimtaFoam", amount - used));
        return used;
    }
    public static int refillFoam(ItemStack pack, ItemStack supply) {
        if (!(pack.getItem() instanceof FoamPack) || !supply.is(ConstructionChemistry.FOAM_CANISTER)) return 0;
        int old = foam(pack), available = Math.max(0, supply.getMaxDamage() - supply.getDamageValue());
        int moved = Math.min(FOAM_CAPACITY - old, available);
        if (moved <= 0) return 0;
        CustomData.update(DataComponents.CUSTOM_DATA, pack, tag -> tag.putInt("JimtaFoam", old + moved));
        if (moved == available) supply.shrink(1); else supply.setDamageValue(supply.getDamageValue() + moved);
        return moved;
    }
    public static final class FoamPack extends Item {
        FoamPack(Properties p) { super(p); }
        @Override public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer();
            if (player == null || player.isSpectator() || !player.isAlive()) return InteractionResult.FAIL;
            if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
            if (!Authority.mayChange(level, player, context.getClickedPos())) return InteractionResult.FAIL;
            var tank = FluidStorage.SIDED.find(level, context.getClickedPos(), context.getClickedFace());
            if (tank == null) return InteractionResult.PASS;
            var pack = foamStorage(ContainerItemContext.ofPlayerHand(player, context.getHand()));
            try (var tx = Transaction.openOuter()) {
                long moved = player.isSecondaryUseActive()
                        ? StorageUtil.move(pack, tank, IndustrialBackpacks::foamFluid, FOAM_CAPACITY * FOAM_PER_BLOCK, tx)
                        : StorageUtil.move(tank, pack, IndustrialBackpacks::foamFluid, FOAM_CAPACITY * FOAM_PER_BLOCK, tx);
                if (moved > 0) tx.commit();
            }
            player.sendOverlayMessage(Component.literal("Construction foam: " + foam(player.getItemInHand(context.getHand())) + " / 800 blocks"));
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            var supply = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!supply.is(ConstructionChemistry.FOAM_CANISTER)) return super.use(level, player, hand);
            if (player.isSpectator() || !player.isAlive()) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                int moved = refillFoam(player.getItemInHand(hand), supply);
                player.sendOverlayMessage(Component.literal(moved > 0 ? "Loaded " + moved + " blocks of foam." : "Foam backpack is full."));
                player.getInventory().setChanged();
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.literal("Construction foam: " + foam(stack) + " / 800 blocks"));
            out.accept(Component.literal("Use with a foam canister in the other hand to refill. Wear on chest to feed either foam sprayer."));
            out.accept(Component.literal("Or use a tank to fill; sneak-use to empty. 100 mB construction foam per block."));
        }
    }
}
