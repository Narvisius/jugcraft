package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BrewingStandBlockEntity.class)
public interface CompanionBrewingAccessor {
 @Accessor("brewTime") int peepo$brewTime();
 @Accessor("fuel") int peepo$fuel();
}
