package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;


public final class BotanicalEquestrian extends Item {
    private final boolean zombie;
    BotanicalEquestrian(boolean zombie,Properties p){super(p);this.zombie=zombie;}
    static void register(){ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->{
        if(!source.is(DamageTypes.DROWN))return true;var subject=entity.getVehicle() instanceof AbstractHorse horse?horse:entity;
        return !(subject instanceof AbstractHorse horse&&horse.isTamed()&&(horse instanceof ZombieHorse||horse instanceof SkeletonHorse));
    });}
    private static net.minecraft.world.entity.animal.equine.AbstractHorse convert(Horse horse,boolean zombie,ConversionParams.AfterConversion<net.minecraft.world.entity.animal.equine.AbstractHorse> finish){return zombie?horse.convertTo(net.minecraft.world.entity.EntityTypes.ZOMBIE_HORSE,ConversionParams.single(horse,true,true),EntitySpawnReason.CONVERSION,e->finish.finalizeConversion(e)):horse.convertTo(net.minecraft.world.entity.EntityTypes.SKELETON_HORSE,ConversionParams.single(horse,true,true),EntitySpawnReason.CONVERSION,e->finish.finalizeConversion(e));}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player user,LivingEntity target,InteractionHand hand){
        if(!(target instanceof Horse horse)||!horse.isTamed())return InteractionResult.PASS;
        if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!horse.isAlive()||horse.isVehicle()||horse.isPassenger()||!Authority.mayChange(p.level(),p,horse.blockPosition())||horse.getOwnerReference()!=null&&!horse.getOwnerReference().matches(p))return InteractionResult.FAIL;
        double speed=horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED),health=horse.getAttributeBaseValue(Attributes.MAX_HEALTH),jump=horse.getAttributeBaseValue(Attributes.JUMP_STRENGTH);int age=horse.getAge(),temper=horse.getTemper();
        net.minecraft.world.entity.ConversionParams.AfterConversion<net.minecraft.world.entity.animal.equine.AbstractHorse> finish=converted->{
            converted.setTamed(true);converted.setOwner(p);converted.setAge(age);converted.setTemper(temper);
            converted.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed*2);converted.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health*2);converted.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jump*1.5);converted.setHealth(converted.getMaxHealth());converted.setPersistenceRequired();
        };
        var replacement=convert(horse,zombie,finish);if(replacement==null)return InteractionResult.FAIL;
        // Undead horses cannot wear the original horse armor; return it exactly once, preserving its data.
        var armor=replacement.getItemBySlot(EquipmentSlot.BODY);if(!armor.isEmpty()){replacement.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);p.getInventory().placeItemBackInInventory(armor,net.minecraft.util.Prediction.SERVER_ONLY);}
        replacement.playSound(BotanicalSounds.get("virus_infect"),1+p.getRandom().nextFloat(),1.3F+p.getRandom().nextFloat()*.7F);
        if(!p.hasInfiniteMaterials())stack.shrink(1);return InteractionResult.SUCCESS;
    }
}
