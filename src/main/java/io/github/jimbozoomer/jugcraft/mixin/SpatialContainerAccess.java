package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface SpatialContainerAccess {
    @Accessor("container1") Container jugcraft$first();
    @Accessor("container2") Container jugcraft$second();
}
