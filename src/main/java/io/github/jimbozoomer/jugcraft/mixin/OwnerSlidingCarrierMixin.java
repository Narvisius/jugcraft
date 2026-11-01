package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSlidingBlocks;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PistonMovingBlockEntity.class)
public abstract class OwnerSlidingCarrierMixin implements OwnerSlidingBlocks.NativeCarrier {
 @Unique private UUID jugcraft$owner;
 public void jugcraft$slidingOwner(UUID owner){jugcraft$owner=owner;((PistonMovingBlockEntity)(Object)this).setChanged();}
 @Inject(method="saveAdditional",at=@At("TAIL")) private void jugcraft$save(ValueOutput out,CallbackInfo ci){if(jugcraft$owner!=null)out.store("JugcraftSlideOwner",UUIDUtil.CODEC,jugcraft$owner);}
 @Inject(method="loadAdditional",at=@At("TAIL")) private void jugcraft$load(ValueInput in,CallbackInfo ci){jugcraft$owner=in.read("JugcraftSlideOwner",UUIDUtil.CODEC).orElse(null);}
 @Inject(method="tick",at=@At("RETURN")) private static void jugcraft$finish(Level level,BlockPos pos,BlockState state,PistonMovingBlockEntity node,CallbackInfo ci){OwnerSlidingBlocks.pistonFinished(node,((OwnerSlidingCarrierMixin)(Object)node).jugcraft$owner);}
 @Inject(method="finalTick",at=@At("RETURN")) private void jugcraft$finishNow(CallbackInfo ci){OwnerSlidingBlocks.pistonFinished((PistonMovingBlockEntity)(Object)this,jugcraft$owner);}
}
