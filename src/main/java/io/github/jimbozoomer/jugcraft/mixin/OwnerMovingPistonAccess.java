package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(PistonMovingBlockEntity.class)
public interface OwnerMovingPistonAccess {
    @Accessor("progress") float jugcraft$progress();
    @Accessor("progress") void jugcraft$progress(float value);
    @Accessor("progressO") void jugcraft$previous(float value);
    @Accessor("lastTicked") void jugcraft$lastTick(long value);
    @Invoker("moveCollidedEntities") static void jugcraft$push(Level level,BlockPos pos,float progress,PistonMovingBlockEntity node){throw new AssertionError();}
    @Invoker("moveStuckEntities") static void jugcraft$carry(Level level,BlockPos pos,float progress,PistonMovingBlockEntity node){throw new AssertionError();}
}
