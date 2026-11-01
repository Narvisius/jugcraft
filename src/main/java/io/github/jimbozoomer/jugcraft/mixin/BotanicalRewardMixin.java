package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.BotanicalLoonium;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BotanicalRewardMixin implements BotanicalLoonium.Carrier {
    @Unique private ItemStack jugcraft$reward=ItemStack.EMPTY;
    @Override public void jugcraft$looniumReward(ItemStack stack){jugcraft$reward=stack.copy();}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$saveReward(ValueOutput out,CallbackInfo ci){if(!jugcraft$reward.isEmpty())out.store("jugcraft_loonium_reward",ItemStack.CODEC,jugcraft$reward);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$loadReward(ValueInput in,CallbackInfo ci){jugcraft$reward=in.read("jugcraft_loonium_reward",ItemStack.CODEC).orElse(ItemStack.EMPTY);}
    @Inject(method="dropAllDeathLoot",at=@At("HEAD"),cancellable=true)
    private void jugcraft$singleReward(ServerLevel level,DamageSource source,CallbackInfo ci){
        var mob=(LivingEntity)(Object)this;
        if(mob.entityTags().contains("jugcraft_fel_blaze")){
            if(level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS)){
                int count=6;
                if(source.getEntity() instanceof net.minecraft.world.entity.player.Player player){
                    int looting=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(net.minecraft.world.item.enchantment.Enchantments.LOOTING).map(e->net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e,player.getMainHandItem())).orElse(0);
                    count=10+Math.round(level.getRandom().nextFloat()*6*Math.clamp(looting,0,20));
                }
                mob.spawnAtLocation(level,new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER,count));
            }
            ci.cancel();return;
        }
        if(!mob.entityTags().contains(BotanicalLoonium.MARKER))return;
        var reward=jugcraft$reward;jugcraft$reward=ItemStack.EMPTY;if(!reward.isEmpty()&&level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS))mob.spawnAtLocation(level,reward);ci.cancel();
    }
}
