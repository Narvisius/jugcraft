package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
/** The native 26.3 sign mesh is submitted by its animated block-entity renderer. */
@Mixin(SignBlock.class)
public abstract class OwnerHangingSignModelMixin extends BaseEntityBlock {
    protected OwnerHangingSignModelMixin(Properties properties){super(properties);}
    @Override protected RenderShape getRenderShape(BlockState state){return state.getBlock() instanceof HangingSignBlock?RenderShape.INVISIBLE:super.getRenderShape(state);}
}
