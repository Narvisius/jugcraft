package io.github.jimbozoomer.jugcraft.vehiclebay;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Small, durable chassis settings. No client packet can directly change an installed fitout. */
public record VehicleFitout(int armor, int drive) {
    public static final VehicleFitout STANDARD = new VehicleFitout(0, 0);
    public VehicleFitout { armor = Math.clamp(armor, 0, 1); drive = Math.clamp(drive, 0, 2); }
    public double speed() { return (armor == 1 ? .9 : 1) * (drive == 1 ? 1.2 : drive == 2 ? .85 : 1); }
    public double toughness() { return armor == 1 ? 1.5 : 1; }
    public int fuelCost(int tick) { return drive == 1 ? 2 : drive == 2 && tick % 2 == 0 ? 0 : 1; }
    public static VehicleFitout read(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return new VehicleFitout(data.getIntOr("BayArmor", 0), data.getIntOr("BayDrive", 0));
    }
    public static int fuel(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("BayFuel", 0));
    }
    public ItemStack write(ItemStack stack, int fuel) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt("BayArmor", armor); data.putInt("BayDrive", drive); data.putInt("BayFuel", Math.max(0, fuel));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }
}
