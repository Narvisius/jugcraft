package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Fox.class)
public interface OwnerFluteFoxAccess {
    @Invoker("trusts") boolean jugcraft$trusts(LivingEntity player);
    @Invoker("setSleeping") void jugcraft$sleeping(boolean sleeping);
}
