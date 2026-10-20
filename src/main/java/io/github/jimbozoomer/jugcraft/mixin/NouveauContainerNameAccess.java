package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseContainerBlockEntity.class)
public interface NouveauContainerNameAccess {
    @Accessor("name") void jugcraft$setName(Component name);
}
