package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CampfireBlockEntity.class)
public interface CompanionCampfireAccessor {
 @Accessor("cookingProgress") int[] peepo$progress();
 @Accessor("cookingTime") int[] peepo$times();
}
