package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ResultSlot.class)
public interface CraftingResultAccess {
    @Accessor("craftSlots") CraftingContainer jugcraft$inputs();
}
