package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve the owner of a slidy block through the native piston carrier. Other block entities remain immovable. */
@Mixin(PistonBaseBlock.class)
public abstract class OwnerSlidingPistonMixin {
 @Unique private final ThreadLocal<Deque<Map<BlockPos,UUID>>> jugcraft$slides=ThreadLocal.withInitial(ArrayDeque::new);
 @Inject(method="isPushable",at=@At("RETURN"),cancellable=true)
 private static void jugcraft$slidyPushable(BlockState state,Level level,BlockPos pos,Direction direction,boolean destroy,Direction facing,CallbackInfoReturnable<Boolean> cir){
  if(state.getBlock() instanceof OwnerSupplementBlock block&&block.function.equals("slidy")){
   boolean allowed=!io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(level,pos)&&!io.github.jimbozoomer.jugcraft.finance.LandRegistry.crossesBoundary(level,pos,pos.relative(direction))&&OwnerSlidingBlocks.pistonAllowed(level,pos,direction);
   cir.setReturnValue(allowed);
  }
 }
 @Inject(method="moveBlocks",at=@At("HEAD"))
 private void jugcraft$remember(Level level,BlockPos piston,Direction direction,boolean extending,CallbackInfoReturnable<Boolean> cir){
  var owners=new HashMap<BlockPos,UUID>();var resolver=new PistonStructureResolver(level,piston,direction,extending);
  if(resolver.resolve())for(var source:resolver.getToPush())if(level.getBlockEntity(source) instanceof OwnerSupplementData data&&data.block().function.equals("slidy")&&data.owner!=null)owners.put(source.relative(resolver.getPushDirection()),data.owner);
  jugcraft$slides.get().push(owners);
 }
 @Inject(method="moveBlocks",at=@At("RETURN"))
 private void jugcraft$attach(Level level,BlockPos piston,Direction direction,boolean extending,CallbackInfoReturnable<Boolean> cir){
  var stack=jugcraft$slides.get();if(stack.isEmpty())return;var owners=stack.pop();if(stack.isEmpty())jugcraft$slides.remove();
  if(!cir.getReturnValueZ())return;owners.forEach((pos,owner)->{if(level.getBlockEntity(pos) instanceof OwnerSlidingBlocks.NativeCarrier carrier)carrier.jugcraft$slidingOwner(owner);});
 }
}
