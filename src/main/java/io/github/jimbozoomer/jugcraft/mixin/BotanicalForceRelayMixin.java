package io.github.jimbozoomer.jugcraft.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.botany.BotanicalForceRelay;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(PistonBaseBlock.class)
public abstract class BotanicalForceRelayMixin {
    @WrapMethod(method="moveBlocks")
    private boolean jugcraft$moveLinks(Level level,BlockPos pos,Direction direction,boolean extending,Operation<Boolean> original){var links=BotanicalForceRelay.capture(level,pos,direction,extending);boolean moved=original.call(level,pos,direction,extending);if(moved&&level instanceof ServerLevel server)BotanicalForceRelay.moved(server,links,extending?direction:direction.getOpposite());return moved;}
}
