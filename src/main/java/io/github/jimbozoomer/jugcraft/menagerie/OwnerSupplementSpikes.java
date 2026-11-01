package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Supplied six-face spikes and lingering-potion coatings, with real owner authority. */
final class OwnerSupplementSpikes {
    private OwnerSupplementSpikes() {}

    static InteractionResult use(OwnerSupplementData data,ItemStack held,Player player,InteractionHand hand){
        if(held.getItem() instanceof OwnerSupplementSoap&&data.spikeCharges>0){
            data.spikeCharges=0;data.spikePotion=PotionContents.EMPTY;held.consume(1,player);data.changed();
            return InteractionResult.SUCCESS;
        }
        var potion=held.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(!held.is(Items.LINGERING_POTION)||!potion.hasEffects()||data.spikeCharges>=16||data.spikeCharges>0&&!data.spikePotion.equals(potion))return InteractionResult.TRY_WITH_EMPTY_HAND;
        data.spikePotion=potion;data.spikeCharges=16;data.changed();
        if(!player.isCreative()){held.shrink(1);player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE),net.minecraft.util.Prediction.SERVER_ONLY);}
        data.getLevel().playSound(null,data.getBlockPos(),SoundEvents.HONEY_BLOCK_FALL,SoundSource.BLOCKS,.5F,1.5F);
        return InteractionResult.SUCCESS;
    }

    static void contact(ServerLevel level,OwnerSupplementData data,Entity entity){
        if(!(entity instanceof LivingEntity target)||!target.isAlive()||target.isSpectator()||target instanceof Player p&&p.isCreative())return;
        var owner=Authority.present(level,data.owner);
        if(owner==null||!Authority.mayChange(level,owner,data.getBlockPos())||!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(owner,level,target))return;
        if(HouseholdBlock.facing(data.getBlockState())==Direction.UP&&target instanceof Player&&target.isShiftKeyDown())return;
        if(level.getGameTime()-data.spikeLastHit<20)return;
        data.spikeLastHit=level.getGameTime();data.setChanged();
        target.hurtServer(level,level.damageSources().playerAttack(owner),target.getY()>data.getBlockPos().getY()+.0625?3:1.5F);
        if(data.spikeCharges==0)return;
        boolean used=false;
        for(var effect:data.spikePotion.getAllEffects()){
            if(!target.canBeAffected(effect)||target.hasEffect(effect.getEffect()))continue;
            if(effect.getEffect().value().isInstantaneous())effect.getEffect().value().applyInstantaneousEffect(level,owner,owner,target,effect.getAmplifier(),.5);
            else target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(),Math.max(1,effect.getDuration()/10),effect.getAmplifier()),owner);
            used=true;
        }
        if(used){if(--data.spikeCharges==0)data.spikePotion=PotionContents.EMPTY;data.changed();}
    }

    static VoxelShape shape(Direction facing){return switch(facing){
        case UP->Block.box(0,0,0,16,1,16);case DOWN->Block.box(0,15,0,16,16,16);
        case NORTH->Block.box(0,0,15,16,16,16);case SOUTH->Block.box(0,0,0,16,16,1);
        case EAST->Block.box(0,0,0,1,16,16);case WEST->Block.box(15,0,0,16,16,16);
    };}
}
