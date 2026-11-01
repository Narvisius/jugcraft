package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.jimbozoomer.jugcraft.aquatic.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(FishingHook.class)
public abstract class OwnerAquaticHookMixin {
    @Shadow @Final @Mutable private int luck;
    @Shadow @Final @Mutable private int lureSpeed;
    @Shadow private int nibble;
    @Unique private ItemStack jugcraft$rod=ItemStack.EMPTY;
    @Unique private String jugcraft$tackle="";
    @Inject(method="<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",at=@At("TAIL"))
    private void jugcraft$tackle(Player player,Level level,int luck,int lure,CallbackInfo ci){
        var rod=OwnerAquaticGear.held(player);if(!(rod.getItem() instanceof OwnerAquaticGear.Rod r))return;jugcraft$rod=rod;jugcraft$tackle=OwnerAquaticGear.hook(rod);
        if(jugcraft$tackle.equals("gold")||jugcraft$tackle.equals("nether_star"))this.luck++;
        if(r.material.equals("neptunium"))lureSpeed+=100;if(!OwnerAquaticGear.part(rod,1).isEmpty())lureSpeed+=1;lureSpeed=Math.min(500,lureSpeed);
        var self=(FishingHook)(Object)this;if(jugcraft$tackle.equals("light"))self.setDeltaMovement(self.getDeltaMovement().multiply(1.5,1,1.5));else if(jugcraft$tackle.equals("heavy"))self.setDeltaMovement(self.getDeltaMovement().multiply(.6,.15,.6));
    }
    @Inject(method="shouldStopFishing",at=@At("HEAD"),cancellable=true)
    private void jugcraft$nativeRod(Player p,CallbackInfoReturnable<Boolean> cir){
        var self=(FishingHook)(Object)this;
        if(!jugcraft$rod.isEmpty()&&p.canInteractWithLevel()&&(p.getMainHandItem()==jugcraft$rod||p.getOffhandItem()==jugcraft$rod)&&self.distanceToSqr(p)<=1024)cir.setReturnValue(false);
    }
    @Inject(method="retrieve",at=@At("HEAD"),cancellable=true)
    private void jugcraft$protect(ItemStack rod,CallbackInfoReturnable<Integer> cir){
        var self=(FishingHook)(Object)this;if(jugcraft$rod.isEmpty())return;
        if(rod!=jugcraft$rod){self.discard();cir.setReturnValue(0);return;}
        if(self.getPlayerOwner() instanceof ServerPlayer p){var target=self.getHookedIn();if(target!=null&&(!Authority.mayChange(p.level(),p,target.blockPosition())||target instanceof LivingEntity living&&!Authority.mayStrike(p,living))){self.discard();cir.setReturnValue(0);}}
    }
    @ModifyExpressionValue(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> jugcraft$fish(ObjectArrayList<ItemStack> original){return OwnerAquaticFishing.caught(original,(FishingHook)(Object)this,jugcraft$rod,jugcraft$tackle,luck);}
    @Inject(method="retrieve",at=@At("RETURN"),cancellable=true)
    private void jugcraft$wear(ItemStack rod,CallbackInfoReturnable<Integer> cir){
        if(jugcraft$rod.isEmpty()||cir.getReturnValueI()<=0)return;double chance=switch(jugcraft$tackle){case "iron"->.2;case "diamond","nether_star"->.5;default->0;};
        if(((FishingHook)(Object)this).level().getRandom().nextDouble()<chance)cir.setReturnValue(0);
    }
    @Inject(method="catchingFish",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/projectile/FishingHook;nibble:I",opcode=org.objectweb.asm.Opcodes.PUTFIELD,ordinal=1,shift=At.Shift.AFTER))
    private void jugcraft$note(net.minecraft.core.BlockPos pos,CallbackInfo ci){if(jugcraft$tackle.equals("note")&&nibble>0)((FishingHook)(Object)this).playSound(OwnerAquaticSounds.sound("bobber_note_catch"),.7f,1);}
    @ModifyExpressionValue(method="catchingFish",at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I",ordinal=0))
    private int jugcraft$window(int original){return jugcraft$tackle.equals("redstone")?35+((FishingHook)(Object)this).level().getRandom().nextInt(36):original;}
}
