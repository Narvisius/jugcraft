package io.github.jimbozoomer.jugcraft.wilds;
import net.minecraft.world.level.ClipContext;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.rime.Frostward;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Shared bounded ice breath, accumulated freeze and regenerating original ice crystal. */
public final class WildCold {
    public static Holder<MobEffect> FROZEN;
    private record Exposure(float amount,long tick){}
    private static final Map<LivingEntity,Exposure> EXPOSURE=new WeakHashMap<>();
    static void register(){
        FROZEN=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("wilds_frozen"),new Frozen());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim,source,damage)->!(source.getEntity() instanceof LivingEntity actor&&frozen(actor)));
        UseItemCallback.EVENT.register((p,l,h)->frozen(p)?InteractionResult.FAIL:InteractionResult.PASS);
        UseBlockCallback.EVENT.register((p,l,h,hit)->frozen(p)?InteractionResult.FAIL:InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((p,l,h,pos,dir)->frozen(p)?InteractionResult.FAIL:InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((p,l,h,target,hit)->frozen(p)?InteractionResult.FAIL:InteractionResult.PASS);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->EXPOSURE.clear());
    }
    public static boolean frozen(LivingEntity e){return FROZEN!=null&&e.hasEffect(FROZEN);}
    private static boolean immune(LivingEntity e){return e instanceof WildFrostmaw||!e.canFreeze()||Frostward.warded(e)||e.isOnFire();}
    static void chill(LivingEntity target,float amount,LivingEntity caster){if(immune(target))return;long now=target.level().getGameTime();var old=EXPOSURE.get(target);float next=amount+(old==null||now-old.tick>30?0:old.amount);if(next>=1){EXPOSURE.remove(target);target.addEffect(new MobEffectInstance(FROZEN,50),caster);target.stopUsingItem();}else EXPOSURE.put(target,new Exposure(next,now));}
    private static final class Frozen extends MobEffect {
        Frozen(){super(MobEffectCategory.HARMFUL,0xA4DBFF);addAttributeModifier(Attributes.MOVEMENT_SPEED,Jugcraft.id("wilds_frozen_movement"),-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
        @Override public boolean applyEffectTick(ServerLevel level,LivingEntity e,int amplifier){if(immune(e))return false;e.setTicksFrozen(e.getTicksRequiredToFreeze());e.setDeltaMovement(0,Math.min(0,e.getDeltaMovement().y),0);e.stopUsingItem();if(e instanceof Mob mob){mob.getNavigation().stop();mob.setJumping(false);}if(e.tickCount%5==0)level.sendParticles(WildParticles.TYPES.get("snowflake"),e.getX(),e.getY()+e.getBbHeight()/2,e.getZ(),4,e.getBbWidth()/2,e.getBbHeight()/2,e.getBbWidth()/2,0);return true;}
    }
    static boolean mayHit(LivingEntity caster,LivingEntity target){return target!=caster&&target.isAlive()&&!target.isSpectator()&&!immune(target)&&!target.isAlliedTo(caster)&&(caster instanceof ServerPlayer p?(!(target instanceof TamableAnimal pet)||!p.getUUID().equals((pet.getOwnerReference()==null?null:pet.getOwnerReference().getUUID())))&&TwoHanded.allowed(p,p.level(),target):target instanceof Player victim&&!victim.isCreative());}
    static void breath(LivingEntity caster,Vec3 origin,Vec3 direction,int age){
        if(!(caster.level() instanceof ServerLevel l))return;var look=direction.normalize();double reach=10;var end=origin.add(look.scale(reach));var hit=l.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster));if(hit.getType()!=HitResult.Type.MISS)reach=origin.distanceTo(hit.getLocation());
        for(int i=0;i<4;i++){var at=origin.add(look.scale(reach*(i+1)/4));l.sendParticles(WildParticles.TYPES.get("snowflake"),at.x,at.y,at.z,3,.12+i*.12,.12+i*.12,.12+i*.12,.02);}
        if(age%10==0){var victims=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),new AABB(origin,origin.add(look.scale(reach))).inflate(4),e->mayHit(caster,e),victims,32);for(var e:victims){var delta=e.getBoundingBox().getCenter().subtract(origin);if(delta.length()>reach+e.getBbWidth()/2||delta.normalize().dot(look)<Math.cos(Math.PI/8)||l.clip(new ClipContext(origin,e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster)).getType()!=HitResult.Type.MISS)continue;if(e.hurtServer(l,caster.damageSources().indirectMagic(caster,caster),1)){chill(e,.23F,caster);e.setDeltaMovement(e.getDeltaMovement().multiply(.25,1,.25));}}}
        // Bounded source-water freezing uses native melting ice and shared claims, never rewrites saved terrain.
        if(age%10==0&&caster instanceof ServerPlayer p){var center=BlockPos.containing(origin.add(look.scale(reach/2)));int changed=0;for(var at:BlockPos.betweenClosed(center.offset(-3,-2,-3),center.offset(3,2,3))){if(changed==16)break;if(!l.hasChunkAt(at)||!l.getBlockState(at).is(Blocks.WATER)||!l.getFluidState(at).isSource()||!l.isEmptyBlock(at.above())||!Authority.mayChange(l,p,at))continue;var delta=Vec3.atCenterOf(at).subtract(origin);if(delta.length()>reach||delta.normalize().dot(look)<Math.cos(Math.PI/8)||l.clip(new ClipContext(origin,Vec3.atCenterOf(at),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster)).getType()!=HitResult.Type.MISS)continue;l.setBlockAndUpdate(at,Blocks.FROSTED_ICE.defaultBlockState());l.scheduleTick(at,Blocks.FROSTED_ICE,140);changed++;}}
    }
    public static final class Crystal extends Item {
        public Crystal(Properties p){super(p.durability(600).rarity(Rarity.RARE));}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(stack.getDamageValue()+5>=stack.getMaxDamage())return InteractionResult.FAIL;if(!l.isClientSide())stack.setDamageValue(stack.getDamageValue()+5);p.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 72000;}
        @Override public void onUseTick(Level l,LivingEntity user,ItemStack stack,int remaining){if(l.isClientSide())return;int age=getUseDuration(stack,user)-remaining;if(stack.getDamageValue()>=stack.getMaxDamage()-1){user.stopUsingItem();return;}stack.setDamageValue(stack.getDamageValue()+1);if(age==1)user.playSound(OwnerWilds.sound("frostmaw.icebreathstart"),1,1);if(age%20==0)user.playSound(OwnerWilds.sound("frostmaw.icebreath"),.8F,1);if(age>=10)breath(user,user.getEyePosition(),user.getLookAngle(),age);}
        @Override public void inventoryTick(ItemStack stack,ServerLevel l,Entity owner,EquipmentSlot slot){if(!(owner instanceof LivingEntity e)||e.getUseItem()!=stack)if(stack.getDamageValue()>0)stack.setDamageValue(stack.getDamageValue()-1);}

    }
}
