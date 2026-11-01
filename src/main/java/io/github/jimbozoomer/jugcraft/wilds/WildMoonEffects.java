package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Paw debuffs and persistent eclipse suspension, including unload/reload and milk removal. */
public final class WildMoonEffects {
    public static Holder<MobEffect> FRAGILITY,ECLIPSED,MOONS_CURSE;
    private static AttachmentType<List<MobEffectInstance>> SUSPENDED;
    static void register(){
        FRAGILITY=effect("fragility",MobEffectCategory.HARMFUL,10874950);ECLIPSED=effect("eclipsed",MobEffectCategory.NEUTRAL,0xF0F0F0);MOONS_CURSE=effect("moons_curse",MobEffectCategory.NEUTRAL,0xA78BDA);
        SUSPENDED=AttachmentRegistry.<List<MobEffectInstance>>builder().persistent(MobEffectInstance.CODEC.listOf(0,256)).buildAndRegister(Jugcraft.id("wilds_eclipsed_effects"));
    }
    private static Holder<MobEffect> effect(String name,MobEffectCategory kind,int color){return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("wilds_"+name),new MobEffect(kind,color){});}
    public static boolean blocks(LivingEntity e,MobEffectInstance effect){return ECLIPSED!=null&&e.hasEffect(ECLIPSED)&&!effect.is(ECLIPSED);}
    public static float damage(LivingEntity e,DamageSource source,float amount){if(FRAGILITY==null||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;var effect=e.getEffect(FRAGILITY);return effect==null?amount:amount*(1+.2F*(effect.getAmplifier()+1));}
    public static void tick(LivingEntity e){
        if(SUSPENDED==null||e.level().isClientSide())return;
        if(!e.isAlive()){e.removeAttached(SUSPENDED);return;}
        if(e.hasEffect(ECLIPSED)){
            var current=e.getActiveEffects().stream().filter(effect->!effect.is(ECLIPSED)).toList();if(current.isEmpty())return;
            var saved=new ArrayList<>(e.getAttachedOrElse(SUSPENDED,List.of()));
            for(var effect:current){if(saved.size()>=256)break;saved.add(new MobEffectInstance(effect));e.removeEffect(effect.getEffect());}
            e.setAttached(SUSPENDED,List.copyOf(saved));
        }else if(e.hasAttached(SUSPENDED)){
            var effects=e.getAttachedOrElse(SUSPENDED,List.of());e.removeAttached(SUSPENDED);for(var effect:effects)e.addEffect(new MobEffectInstance(effect));
        }
    }
    public static final String[] PAWS={"full","gibbous","half","crescent","new"};
    public static Holder<MobEffect> pawEffect(int phase){return switch(phase){case 0->MobEffects.WEAKNESS;case 1->MobEffects.SLOWNESS;case 2->MobEffects.MINING_FATIGUE;case 3->FRAGILITY;default->ECLIPSED;};}
    public static final class Paw extends Item {
        final int phase;
        public Paw(int phase,Properties p){super(p.durability(3));this.phase=phase;}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(p.isSpectator()||p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;if(p instanceof ServerPlayer sp){
            var targets=new ArrayList<LivingEntity>();sp.level().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),p.getBoundingBox().inflate(10),e->e!=p&&e.isAlive()&&p.distanceToSqr(e)<=100&&!p.isAlliedTo(e)&&!(e instanceof TamableAnimal pet&&p.getUUID().equals((pet.getOwnerReference()==null?null:pet.getOwnerReference().getUUID()))),targets,64);
            for(var e:targets)if(p.hasLineOfSight(e)&&TwoHanded.allowed(sp,sp.level(),e)){e.addEffect(new MobEffectInstance(pawEffect(phase),600),p);tick(e);sp.level().sendParticles(ParticleTypes.WITCH,e.getX(),e.getY()+e.getBbHeight()/2,e.getZ(),12,.3,.3,.3,.03);}
            WildParticles.moon(sp.level(),p,PAWS[phase]);
            if(!p.isCreative()){for(String name:PAWS)p.getCooldowns().addCooldown(new ItemStack(OwnerWilds.item("elokosa_paw_"+name)),1800);stack.hurtAndBreak(1,p,hand.asEquipmentSlot());}
            sp.level().playSound(null,p.blockPosition(),OwnerWilds.sound("elokosa.paw"),net.minecraft.sounds.SoundSource.PLAYERS,1,1.15F-.06F*phase);p.swing(hand,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        }return InteractionResult.SUCCESS;}
    }
}
