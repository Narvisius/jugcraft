package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The supplied six-swig unstable bottle and mana cookie, not a portable-mana refill exploit. */
public final class BotanicalConsumables extends Item {
    public static DataComponentType<Integer> SWIGS;
    private final boolean bottle;
    BotanicalConsumables(boolean bottle,Properties p){super(bottle?p.stacksTo(1):p);this.bottle=bottle;}
    static void register(){SWIGS=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_bottle_swigs"),DataComponentType.<Integer>builder().persistent(Codec.intRange(1,6)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(player.isSpectator()||level instanceof net.minecraft.server.level.ServerLevel s&&!Authority.mayChange(s,player,player.blockPosition()))return InteractionResult.FAIL;player.startUsingItem(hand);return InteractionResult.CONSUME;}
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return bottle?20:32;}
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return bottle?ItemUseAnimation.DRINK:ItemUseAnimation.EAT;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level world,LivingEntity user){
        if(!(user instanceof ServerPlayer p)||!p.isAlive()||!Authority.mayChange(p.level(),p,p.blockPosition()))return stack;
        if(!bottle){p.addEffect(new MobEffectInstance(MobEffects.SATURATION,20));if(!p.hasInfiniteMaterials())stack.shrink(1);return stack;}
        int left=Math.clamp(stack.getOrDefault(SWIGS,6),1,6);
        // Consume/update before effects that can scatter the held item. There is never a second copy of the bottle.
        ItemStack result=stack;
        if(!p.hasInfiniteMaterials()){
            if(left==1){result=new ItemStack(Items.GLASS_BOTTLE);if(p.getMainHandItem()==stack)p.setItemInHand(InteractionHand.MAIN_HAND,result);else if(p.getOffhandItem()==stack)p.setItemInHand(InteractionHand.OFF_HAND,result);stack.setCount(0);}
            else{stack.set(SWIGS,left-1);stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of((float)(7-left)),List.of(),List.of(),List.of()));}
        }
        int effect=p.getRandom().nextInt(15);
        switch(effect){
            case 0->{var v=p.getDeltaMovement();p.setDeltaMovement((p.getRandom().nextDouble()-.5)*3,v.y,(p.getRandom().nextDouble()-.5)*3);p.syncVelocity=true;}
            case 1->{if(!p.level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,p.position())){var at=p.blockPosition();if(p.level().getBlockState(at).isAir()&&Authority.mayChange(p.level(),p,at))p.level().setBlockAndUpdate(at,Blocks.WATER.defaultBlockState());}}
            case 2->p.igniteForSeconds(4);
            case 3->{p.level().sendParticles(ParticleTypes.EXPLOSION,p.getX(),p.getY(),p.getZ(),1,0,0,0,0);p.hurtServer(p.level(),p.damageSources().magic(),1);}
            case 4->{if(!p.level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,p.position())){p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,300));var v=p.getDeltaMovement();p.setDeltaMovement(v.x,6,v.z);p.syncVelocity=true;}}
            case 5->p.setHealth(Math.max(.5F,p.getRandom().nextFloat()*p.getMaxHealth()));
            case 6->p.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,2400,9));
            case 7->{
                // Scatter the rest of the inventory; retain the in-use bottle so finishUsingItem cannot duplicate its dropped copy.
                for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.isEmpty()||item==stack||item==result)continue;var dropped=p.drop(item.copy(),true,net.minecraft.util.Prediction.SERVER_ONLY);if(dropped!=null)p.getInventory().setItem(i,ItemStack.EMPTY);}
            }
            case 8->{p.setYRot(p.getRandom().nextFloat()*360);p.setXRot(p.getRandom().nextFloat()*180-90);p.teleportTo(p.getX(),p.getY(),p.getZ());}
            case 9->{
                var column=p.blockPosition();for(int y=p.level().getMaxY()-2;y>p.level().getMinY();y--){var at=new BlockPos(column.getX(),y,column.getZ());if(p.level().getBlockState(at.below()).getCollisionShape(p.level(),at.below()).isEmpty())continue;var point=Vec3.atBottomCenterOf(at);var box=p.getBoundingBox().move(point.subtract(p.position()));if(p.level().noCollision(p,box)&&!p.level().containsAnyLiquid(box)&&Authority.mayChange(p.level(),p,at)){p.teleportTo(point.x,point.y,point.z);p.fallDistance=0;break;}}
            }
            case 10->p.addEffect(new MobEffectInstance(MobEffects.SPEED,60,200));
            case 11->p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,6000));
            case 12->BotanicalPixie.friend(p);
            case 13->{p.addEffect(new MobEffectInstance(MobEffects.NAUSEA,160,3));p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,160));}
            case 14->{p.setHealth(Math.min(p.getHealth(),1));var head=new ItemStack(Items.PLAYER_HEAD);head.set(DataComponents.PROFILE,ResolvableProfile.createResolved(p.getGameProfile()));p.getInventory().placeItemBackInInventory(head,net.minecraft.util.Prediction.SERVER_ONLY);}
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> text,TooltipFlag flag){if(bottle){text.accept(Component.literal(Math.clamp(stack.getOrDefault(SWIGS,6),1,6)+" swigs remaining"));text.accept(Component.literal("Unstable: may injure, teleport or scatter your inventory. Does not refill mana."));}else text.accept(Component.literal("Restores hunger through a short saturation effect."));}
}
