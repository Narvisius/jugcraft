package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jspecify.annotations.Nullable;
@Mixin(MushroomCow.class)
public interface BotanicalMooshroomAccess {
    @Accessor("stewEffects") @Nullable SuspiciousStewEffects jugcraft$stew();
    @Accessor("stewEffects") void jugcraft$stew(@Nullable SuspiciousStewEffects value);
}
