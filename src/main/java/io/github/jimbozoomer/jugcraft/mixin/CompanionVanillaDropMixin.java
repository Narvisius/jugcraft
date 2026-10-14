package io.github.jimbozoomer.jugcraft.mixin;
import local.peepo.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockEntity.class)
public abstract class CompanionVanillaDropMixin {
 @Inject(method="preRemoveSideEffects",at=@At("HEAD")) private void peepo$drop(BlockPos pos,BlockState state,CallbackInfo ci){if(this instanceof VanillaCompanionAccess access){var port=access.peepo$existingPort();if(port!=null)port.drop();}}
}
