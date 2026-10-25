package io.github.jimbozoomer.jugcraft.jimtafacts.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaAbilities;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class JimtaPlayerMixin {
    @Inject(method="hasCorrectToolForDrops",at=@At("RETURN"),cancellable=true)
    private void jimta$stoneClaws(BlockState state,CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValue()&&JimtaAbilities.wearing((Player)(Object)this,"digging_claws")
                &&state.is(BlockTags.MINEABLE_WITH_PICKAXE)&&!state.is(BlockTags.NEEDS_IRON_TOOL)&&!state.is(BlockTags.NEEDS_DIAMOND_TOOL))cir.setReturnValue(true);
    }
}
