package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.*;

/** Consumes real supplies only when their effects help; never drinks harmful or redundant potions. */
final class OwnerBackpackAlchemy {
    private OwnerBackpackAlchemy(){}
    static void tick(ServerPlayer player,ItemStack pack,OwnerBackpackInventory inventory){
        if(player.level().getServer().getTickCount()%40!=0||!OwnerBackpacks.has(pack,"alchemy_upgrade")&&!OwnerBackpacks.has(pack,"advanced_alchemy_upgrade"))return;
        for(int i=0;i<inventory.getContainerSize();i++){
            var item=inventory.getItem(i);var consumable=item.get(DataComponents.CONSUMABLE);
            if(consumable==null||!OwnerBackpackFilters.matches(pack,item,"alchemy"))continue;
            var effects=new java.util.ArrayList<MobEffectInstance>();
            var potion=item.get(DataComponents.POTION_CONTENTS);if(potion!=null)potion.getAllEffects().forEach(effects::add);
            boolean cures=false;
            for(var effect:consumable.onConsumeEffects()){
                if(effect instanceof ApplyStatusEffectsConsumeEffect apply)effects.addAll(apply.effects());
                if(effect instanceof ClearAllStatusEffectsConsumeEffect)cures=player.getActiveEffects().stream().anyMatch(e->e.getEffect().value().getCategory()==MobEffectCategory.HARMFUL);
                if(effect instanceof RemoveStatusEffectsConsumeEffect remove)cures|=player.getActiveEffects().stream().anyMatch(e->remove.effects().contains(e.getEffect())&&e.getEffect().value().getCategory()==MobEffectCategory.HARMFUL);
            }
            if(effects.stream().anyMatch(e->e.getEffect().value().getCategory()==MobEffectCategory.HARMFUL))continue;
            boolean useful=cures;
            for(var effect:effects){
                if(effect.getEffect().value().isInstantaneous()){
                    useful|=effect.getEffect().is(MobEffects.INSTANT_HEALTH)&&player.getHealth()<player.getMaxHealth()-2;
                }else{var current=player.getEffect(effect.getEffect());useful|=current==null||current.getAmplifier()<effect.getAmplifier()||!current.isInfiniteDuration()&&current.getDuration()<40;}
            }
            if(!useful)continue;
            var serving=item.copyWithCount(1);var remainder=serving.finishUsingItem(player.level(),player);
            inventory.removeItem(i,1);if(!remainder.isEmpty()){var left=inventory.insert(remainder);if(!left.isEmpty())player.getInventory().placeItemBackInInventory(left,net.minecraft.util.Prediction.SERVER_ONLY);}
            return;
        }
    }
}
