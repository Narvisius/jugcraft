package io.github.jimbozoomer.jugcraft.mixin;
import java.util.UUID;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ZombieVillager.class)
public interface NouveauZombieAccess {
    @Invoker("startConverting") void jugcraft$beginRestoration(UUID player,int ticks);
}
