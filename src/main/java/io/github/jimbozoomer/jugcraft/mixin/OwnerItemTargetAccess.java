package io.github.jimbozoomer.jugcraft.mixin;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.item.ItemEntity.class)
public interface OwnerItemTargetAccess {
    @org.spongepowered.asm.mixin.gen.Accessor("target") java.util.UUID jugcraft$pickupTarget();
}
