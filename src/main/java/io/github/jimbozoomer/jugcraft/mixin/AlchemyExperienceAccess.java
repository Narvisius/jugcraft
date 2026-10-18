package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla merges equally valued orbs. Preserve that multiplicity during partial Mind Stone intake. */
@Mixin(ExperienceOrb.class)
public interface AlchemyExperienceAccess {
    @Accessor("count") int jugcraft$count();
    @Accessor("count") void jugcraft$count(int count);
}
