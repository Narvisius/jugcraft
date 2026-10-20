package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Thread levels come from their armor sockets; bonuses use the existing Focus and combat boundary. */
public final class NouveauThreads {
    private static AttachmentType<Boolean> UNDYING_SPENT;
    private static final Map<Player,Integer> IMMOLATION=new WeakHashMap<>();
    private NouveauThreads(){}
    public static void register(){
        UNDYING_SPENT=AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).copyOnDeath().buildAndRegister(Jugcraft.id("nouveau_undying_spent"));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,amount)->{
            if(!(entity instanceof ServerPlayer p)||!JugNouveau.enabled()||level(p,"undying")<3||p.getAttachedOrElse(UNDYING_SPENT,false)||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return true;
            p.setAttached(UNDYING_SPENT,true);p.setHealth(1);p.clearFire();p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));p.level().broadcastEntityEvent(p,(byte)35);return false;
        });
    }
    public static int socket(ItemStack armor,int position){
        if(!(armor.getItem() instanceof NouveauGear.Armor item))return 0;
        int tier=armor.getOrDefault(NouveauGear.ARMOR_TIER,0),base=item.entry.source().startsWith("sorcerer")?position+1:item.entry.source().startsWith("battlemage")?1:Math.min(2,position+1);
        return Math.clamp(base+(tier==2?1:0),1,3);
    }
    public static int level(Player p,String thread){
        if(NouveauGear.THREADS==null||!JugNouveau.enabled())return 0;
        int value=0;
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var armor=p.getItemBySlot(slot);var list=armor.getOrDefault(NouveauGear.THREADS,List.of());
            for(int i=0;i<list.size();i++)if(list.get(i).equals(thread))value=Math.max(value,socket(armor,i));
        }return value;
    }
    public static int focusCapacity(Player p){
        if(NouveauGear.THREADS==null||!JugNouveau.enabled())return FocusPool.MAX;
        int bonus=level(p,"magic_capacity")*2+(NouveauGear.worn(p,"amulet_of_mana_boost")?10:0)+(NouveauGear.worn(p,"ring_of_potential")?4:0)+(NouveauGear.worn(p,"ring_of_lesser_discount")||NouveauGear.worn(p,"ring_of_greater_discount")?2:0)+NouveauEquipment.enchantments(p,"mana_boost")*5;
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))if(p.getItemBySlot(slot).getItem() instanceof NouveauGear.Armor armor)bonus+=armor.entry.source().startsWith("sorcerer")?5:armor.entry.source().startsWith("arcanist")?3:1;
        return Math.min(FocusPool.HARD_MAX,FocusPool.MAX+bonus);
    }
    public static void pulse(ServerPlayer p){
        attribute(p,"movement_speed",.2*level(p,"starbuncle"),true);
        attribute(p,"knockback_resistance",Math.min(1,.15*level(p,"amethyst_golem")+(NouveauFamiliars.active(p).equals("amethyst_golem")?.5:0)),false);
        attribute(p,"step_height",level(p,"high_step"),false);
        attribute(p,"safe_fall_distance",2*level(p,"heights"),false);
        attribute(p,"oxygen_bonus",2*level(p,"depths"),false);
        attribute(p,"water_movement_efficiency",level(p,"depths")>=3?1:0,false);
        if(level(p,"depths")>=3&&p.isUnderWater())p.setAirSupply(p.getMaxAirSupply());
        if(level(p,"heights")>0)p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,40,level(p,"heights")-1,true,false));
        if(p.isSleeping()&&p.getSleepTimer()>=100)p.setAttached(UNDYING_SPENT,false);
        if(IMMOLATION.containsKey(p)){int left=IMMOLATION.get(p)-20;if(left<=0)IMMOLATION.remove(p);else IMMOLATION.put(p,left);}
        int repair=level(p,"repairing");if(repair>0&&p.tickCount%Math.max(20,100-repair*20)==0){
            var equipment=new ArrayList<ItemStack>();for(var slot:EquipmentSlot.VALUES)equipment.add(p.getItemBySlot(slot));
            for(var stack:equipment)if(stack.isDamaged()&&(stack.getItem() instanceof NouveauGear.Armor||stack.getItem() instanceof NouveauInstruments.Visual)&&ConcordanceProgress.spendFocus(p,1)){stack.setDamageValue(Math.max(0,stack.getDamageValue()-repair));break;}
        }
    }
    private static void attribute(Player p,String name,double value,boolean multiply){
        var holder=BuiltInRegistries.ATTRIBUTE.get(Identifier.withDefaultNamespace(name)).orElse(null);if(holder==null)return;
        var instance=p.getAttribute(holder);if(instance==null)return;var id=Jugcraft.id("nouveau_threads/"+name);var old=instance.getModifier(id);
        if(old!=null&&old.amount()==value)return;if(old!=null)instance.removeModifier(id);
        if(value>0)instance.addTransientModifier(new AttributeModifier(id,value,multiply?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_VALUE));
    }
    public static float absorb(Player p,DamageSource source,float amount){
        if(!JugNouveau.enabled())return amount;
        if(source.is(DamageTypeTags.IS_FALL))amount*=Math.max(.1F,1-.2F*level(p,"feather"));
        if(source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC))amount=Math.max(0,amount-level(p,"warding"));
        return amount;
    }
    public static boolean damaging(EffectSpec e){return e.kind()==EffectKind.DAMAGE||e.kind()==EffectKind.NOUVEAU&&e.status()!=null&&Set.of("harm","cold_snap","flare","crush","cut","fangs","explosion","lightning","wind_shear","wind_burst").stream().anyMatch(n->e.status().endsWith("/"+n));}
    public static boolean earth(EffectSpec e){return e.school()!=null&&e.school().contains("earth")||e.status()!=null&&Set.of("break","crush","cut","fangs","grow","harvest","gravity","fell","animate_block","conjure_terrain").stream().anyMatch(n->e.status().endsWith("/"+n));}
    public static EffectSpec beforeSpell(ConcordanceEffects.Context context,EffectSpec effect,LivingEntity target){
        var p=ConcordanceEffects.person(context);if(p==null||!damaging(effect)||context.cause().origin()!=Cause.Origin.SPELL&&context.cause().origin()!=Cause.Origin.INVOCATION)return effect;
        int fire=level(p,"kindling"),cold=level(p,"chilling");
        if(fire>0)target.igniteForSeconds(5*fire);if(cold>0){target.setTicksFrozen(Math.max(target.getTicksFrozen(),200*cold));target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200*cold,cold==3?1:0),p);}
        if(p.isOnFire()&&level(p,"immolation")>0){p.clearFire();IMMOLATION.put(p,200*level(p,"immolation"));}
        int added=level(p,"spellpower")+(IMMOLATION.containsKey(p)&&(effect.school()!=null&&effect.school().contains("fire")||effect.status()!=null&&(effect.status().endsWith("/flare")||effect.status().endsWith("/ignite")))?level(p,"immolation")*2:0);
        if(NouveauFamiliars.active(p).equals("drygmy")&&earth(effect))added+=2;
        var spellDamage=p.getEffect(NouveauStatuses.EFFECTS.get("spell_damage"));if(spellDamage!=null)added+=spellDamage.getAmplifier()+1;
        return added==0?effect:effect.withMagnitude(Math.min(EffectSpec.MAX_MAGNITUDE,effect.magnitude()+added));
    }
    public static void afterSpell(ConcordanceEffects.Context context,EffectSpec effect,LivingEntity target,float before){
        var p=ConcordanceEffects.person(context);if(p==null||!damaging(effect))return;
        float dealt=Math.max(0,before-target.getHealth());if(dealt>0&&level(p,"life_drain")>0)p.heal(dealt*.2F*level(p,"life_drain"));
    }
    public static void consumed(ServerPlayer p,ItemStack original){
        var food=original.get(DataComponents.FOOD);int level=level(p,"whirlisprig")+(NouveauFamiliars.active(p).equals("whirlisprig")?1:0);
        if(food!=null&&level>0)p.getFoodData().setSaturation(Math.min(p.getFoodData().getFoodLevel(),p.getFoodData().getSaturationLevel()+food.saturation()*.3F*level));
        var potion=original.get(DataComponents.POTION_CONTENTS);int wixie=level(p,"wixie")+(NouveauFamiliars.active(p).equals("wixie")?1:0);
        if(potion!=null&&wixie>0)for(var effect:potion.getAllEffects())if(!effect.getEffect().value().isInstantaneous())p.addEffect(new MobEffectInstance(effect.getEffect(),Math.min(24000,(int)(effect.getDuration()*(1+.15F*wixie))),effect.getAmplifier()));
    }
}
