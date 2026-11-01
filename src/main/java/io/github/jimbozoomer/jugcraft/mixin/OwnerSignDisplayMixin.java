package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerSignDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public abstract class OwnerSignDisplayMixin extends BlockEntity implements OwnerSignDisplay {
    @Unique private ItemStack jugcraft$front=ItemStack.EMPTY;
    @Unique private ItemStack jugcraft$back=ItemStack.EMPTY;
    protected OwnerSignDisplayMixin(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    public ItemStack jugcraft$display(SignTextSlot side){return side==SignTextSlot.FRONT?jugcraft$front:jugcraft$back;}
    public void jugcraft$display(SignTextSlot side,ItemStack stack){if(side==SignTextSlot.FRONT)jugcraft$front=stack.copyWithCount(stack.isEmpty()?0:1);else jugcraft$back=stack.copyWithCount(stack.isEmpty()?0:1);}
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void jugcraft$save(ValueOutput out,CallbackInfo ci){out.store("JugcraftDisplayFront",ItemStack.OPTIONAL_CODEC,jugcraft$front);out.store("JugcraftDisplayBack",ItemStack.OPTIONAL_CODEC,jugcraft$back);}
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void jugcraft$load(ValueInput in,CallbackInfo ci){jugcraft$display(SignTextSlot.FRONT,in.read("JugcraftDisplayFront",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));jugcraft$display(SignTextSlot.BACK,in.read("JugcraftDisplayBack",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level!=null&&!level.isClientSide()){
            Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,jugcraft$front);Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,jugcraft$back);
            jugcraft$front=ItemStack.EMPTY;jugcraft$back=ItemStack.EMPTY;
        }
        super.preRemoveSideEffects(pos,state);
    }
}
