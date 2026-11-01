package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Mob.class)
public interface OwnerPetGoalsAccess {
    @Accessor("goalSelector") GoalSelector jugcraft$petGoals();
}
