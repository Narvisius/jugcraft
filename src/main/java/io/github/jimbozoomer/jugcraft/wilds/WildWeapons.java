package io.github.jimbozoomer.jugcraft.wilds;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;

/** Native original blowgun charge/ammunition, poison darts and extended-reach melee tools. */
public final class WildWeapons {
    public static EntityType<Dart> DART;
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("wilds_dart"));DART=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Dart>of(Dart::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build(key));}
    private static int enchant(Player p,ItemStack stack,ResourceKey<Enchantment> key){return p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,stack)).orElse(0);}
    public static Item dart(Item.Properties p){return new ArrowItem(p){
        @Override public AbstractArrow createArrow(Level l,ItemStack ammo,LivingEntity owner,ItemStack weapon){return new Dart(l,owner,ammo,weapon);}
        @Override public net.minecraft.world.entity.projectile.Projectile asProjectile(Level l,Position at,ItemStack ammo,Direction direction){return new Dart(l,at,ammo);}
    };}
    public static final class Dart extends AbstractArrow {
        private BlockPos dispenserOrigin;
        public Dart(EntityType<? extends Dart> type,Level l){super(type,l);setBaseDamage(1);}
        public Dart(Level l,LivingEntity owner,ItemStack ammo,ItemStack weapon){super(DART,owner,l,ammo,weapon);setBaseDamage(1);}
        public Dart(Level l,Position at,ItemStack ammo){super(DART,at.x(),at.y(),at.z(),l,ammo.copyWithCount(1),null);setBaseDamage(1);pickup=Pickup.ALLOWED;dispenserOrigin=BlockPos.containing(at.x(),at.y(),at.z());}
        @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(OwnerWilds.item("dart"));}
        @Override protected boolean canHitEntity(Entity e){
            if(!super.canHitEntity(e))return false;
            if(e instanceof LivingEntity living&&getOwner() instanceof ServerPlayer p)return TwoHanded.allowed(p,p.level(),living);
            // Unattended traps cannot infer player consent or cross a land boundary.
            if(e instanceof LivingEntity living&&getOwner() instanceof WildUmvuthana follower)return WildSolar.canHarm(follower,living);
            if(dispenserOrigin!=null)return !(e instanceof Player)&&!(e instanceof TamableAnimal pet&&pet.isTame())&&io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level(),dispenserOrigin,e.blockPosition());
            return true;
        }
        @Override public void tick(){super.tick();if(!level().isClientSide()&&dispenserOrigin!=null&&!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level(),dispenserOrigin,blockPosition()))discard();}
        @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out){super.addAdditionalSaveData(out);if(dispenserOrigin!=null)out.store("dispenser_origin",BlockPos.CODEC,dispenserOrigin);}
        @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in){super.readAdditionalSaveData(in);dispenserOrigin=in.read("dispenser_origin",BlockPos.CODEC).orElse(null);}
        @Override protected void doPostHurtEffects(LivingEntity target){super.doPostHurtEffects(target);if(getOwner() instanceof ServerPlayer p&&!TwoHanded.allowed(p,p.level(),target))return;target.addEffect(new MobEffectInstance(MobEffects.POISON,getOwner() instanceof Player?40:30,getOwner() instanceof Player?3:1),getOwner());target.setArrowCount(Math.max(0,target.getArrowCount()-1));}
    }
    public static final class Blowgun extends BowItem {
        public Blowgun(Properties p){super(p.durability(300));}
        @Override public Predicate<ItemStack> getAllSupportedProjectiles(){return s->s.is(OwnerWilds.item("dart"));}
        @Override public Predicate<ItemStack> getSupportedHeldProjectiles(){return getAllSupportedProjectiles();}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(!p.hasInfiniteMaterials()&&enchant(p,stack,Enchantments.INFINITY)==0&&p.getProjectile(stack).isEmpty())return InteractionResult.FAIL;p.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public boolean releaseUsing(ItemStack stack,Level raw,LivingEntity user,int remaining){if(!(user instanceof ServerPlayer p))return false;float charge=(getUseDuration(stack,p)-remaining)/5F;float power=Math.min(1,(charge*charge+charge*2)/3);if(power<.1)return false;boolean infinite=p.hasInfiniteMaterials()||enchant(p,stack,Enchantments.INFINITY)>0;var ammo=p.getProjectile(stack);if(ammo.isEmpty()&&!infinite)return false;var l=p.level();var shot=new Dart(l,p,new ItemStack(OwnerWilds.item("dart")),stack);shot.shootFromRotation(p,p.getXRot(),p.getYRot(),0,power*1.1F,1);shot.setCritArrow(power==1);int damage=enchant(p,stack,Enchantments.POWER);if(damage>0)shot.setBaseDamage(1+damage*.5+.5);if(enchant(p,stack,Enchantments.FLAME)>0)shot.igniteForSeconds(5);if(infinite)shot.pickup=AbstractArrow.Pickup.CREATIVE_ONLY;if(!l.addFreshEntity(shot))return false;if(!infinite)ammo.shrink(1);stack.hurtAndBreak(1,p,p.getUsedItemHand().asEquipmentSlot());l.playSound(null,p.blockPosition(),OwnerWilds.sound("umvuthana.blowdart"),net.minecraft.sounds.SoundSource.PLAYERS,1,1/(p.getRandom().nextFloat()*.4F+1.2F)+power*.5F);p.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));return true;}
    }
    public static Item melee(boolean dagger,Item.Properties p){
        p=p.sword(ToolMaterial.STONE,dagger?1:3,dagger?-2:-2.4F);
        var attributes=ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,dagger?2:4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,dagger?-2:-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        if(!dagger)attributes.add(Attributes.ENTITY_INTERACTION_RANGE,new AttributeModifier(Jugcraft.id("wilds_spear_reach"),1.5,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        if(dagger)p=p.repairable(OwnerWilds.item("naga_fang"));
        return new Item(p.attributes(attributes.build())){
            @Override public float getAttackDamageBonus(Entity target,float damage,DamageSource source){if(dagger&&source.getEntity() instanceof ServerPlayer player&&target instanceof LivingEntity living&&TwoHanded.allowed(player,player.level(),living)){var offset=player.position().subtract(target.position());var forward=target.getLookAngle();if(offset.horizontalDistanceSqr()>.001&&forward.x*offset.x+forward.z*offset.z<-.5*Math.sqrt(offset.horizontalDistanceSqr()))return damage;}return super.getAttackDamageBonus(target,damage,source);}
            @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){super.hurtEnemy(stack,target,attacker);if(dagger&&attacker instanceof ServerPlayer p&&TwoHanded.allowed(p,p.level(),target))target.addEffect(new MobEffectInstance(MobEffects.POISON,40,3),p);}
        };
    }
}
