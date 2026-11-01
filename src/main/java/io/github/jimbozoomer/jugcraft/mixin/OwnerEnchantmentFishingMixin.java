package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FishingHook.class)
public abstract class OwnerEnchantmentFishingMixin {
    @Shadow @Final @Mutable private int luck;
    @Inject(method="<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",at=@At("TAIL"))
    private void jugcraft$deepwater(Player player,Level level,int luck,int lure,CallbackInfo ci){
        if(level.isClientSide()||!level.getBiome(player.blockPosition()).is(BiomeTags.IS_DEEP_OCEAN))return;
        var rod=player.getMainHandItem().getItem() instanceof FishingRodItem?player.getMainHandItem():player.getOffhandItem();
        this.luck+=Math.min(3,OwnerEnchantments.level(player,rod,"deepwater_bait"));
    }
}
