package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.spell_power.api.*;

/** Source status mechanics adapted to the existing Spell Power attributes and Focus pool. */
public final class JimtaSpellStatuses {
    public static final Map<String,Holder<MobEffect>> EFFECTS=new LinkedHashMap<>();
    private JimtaSpellStatuses() {}
    public static void register() {
        for(String name:List.of("immolate","hastened","slowed","charge","oakskin","blight","gluttony","rend","guided","chilled","spider_aspect","vigor","echoing_strikes","frostbitten_strikes")) {
            var status=new Status(name);
            if(name.equals("hastened")||name.equals("slowed"))
                status.addAttributeModifier(SpellPowerMechanics.HASTE.attributeEntry,Jugcraft.id("jimtafacts/"+name),name.equals("hastened")?.1:-.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            if(name.equals("oakskin"))status.addAttributeModifier(Attributes.MOVEMENT_SPEED,Jugcraft.id("jimtafacts/oakskin"),-.25,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            if(name.equals("rend"))status.addAttributeModifier(Attributes.ARMOR,Jugcraft.id("jimtafacts/rend"),-3,AttributeModifier.Operation.ADD_VALUE);
            if(name.equals("vigor"))status.addAttributeModifier(Attributes.MAX_HEALTH,Jugcraft.id("jimtafacts/vigor"),2,AttributeModifier.Operation.ADD_VALUE);
            if(name.equals("chilled"))status.addAttributeModifier(Attributes.MOVEMENT_SPEED,Jugcraft.id("jimtafacts/chilled"),-.25,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            if(name.equals("charge")) {
                status.addAttributeModifier(Attributes.MOVEMENT_SPEED,Jugcraft.id("jimtafacts/charge_speed"),.05,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                status.addAttributeModifier(Attributes.ATTACK_DAMAGE,Jugcraft.id("jimtafacts/charge_melee"),.05,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                status.addAttributeModifier(SpellSchools.getSchool("spell_power:lightning").attributeEntry,Jugcraft.id("jimtafacts/charge_magic"),.05,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
            EFFECTS.put(name,Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("jimtafacts_"+name),(MobEffect)status));
        }
    }
    public static boolean has(LivingEntity entity,String name) {
        var effect=EFFECTS.get(name);return JimtaSpellbooks.enabled()&&effect!=null&&entity.hasEffect(effect);
    }
    public static float incoming(LivingEntity entity,DamageSource source,float damage) {
        if(has(entity,"oakskin"))damage*=1-Math.min(.75F,.1F+.05F*entity.getEffect(EFFECTS.get("oakskin")).getAmplifier());
        if(JimtaSpellbooks.enabled()&&(source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.Identifier.fromNamespaceAndPath("spell_power","all")))||source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)||source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)))damage*=JimtaUpgrades.resistance(entity);
        if(JimtaSpellbooks.enabled()&&entity.getMainHandItem().getItem()==JimtaSpellbooks.ITEMS.get("magehunter")&&(source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.Identifier.fromNamespaceAndPath("spell_power","all")))||source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)||source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)))damage*=.85F;
        if(source.getEntity() instanceof LivingEntity attacker&&has(attacker,"blight"))damage*=.9F;
        if(source.getEntity() instanceof LivingEntity attacker&&has(attacker,"spider_aspect")&&(entity.hasEffect(MobEffects.POISON)||entity.hasEffect(MobEffects.WITHER)||has(entity,"blight")))damage*=1.05F;
        if(damage>0&&source.getEntity() instanceof LivingEntity&&has(entity,"guided")) {damage*=1.5F;entity.removeEffect(EFFECTS.get("guided"));}
        return damage;
    }
    public static float healing(LivingEntity entity,float amount) { return has(entity,"blight")?amount*.8F:amount; }
    public static void consumed(ServerPlayer player,ItemStack before,boolean consumed) {
        if(!consumed||!has(player,"gluttony"))return;
        var food=before.get(DataComponents.FOOD);if(food==null||food.nutrition()<=0)return;
        // Source food-to-mana ratio translated to shared Focus, capped to prevent oversized modded foods from flooding it.
        int gained=Math.clamp((food.nutrition()+1)/2,1,10);
        player.setAttached(JugcraftConcordance.FOCUS,ConcordanceProgress.focus(player).gain(ConcordanceProgress.now(player.level()),gained));
    }
    private static final class Status extends MobEffect {
        private final String name;
        Status(String name) {super(Set.of("immolate","slowed","blight","rend","guided","chilled").contains(name)?MobEffectCategory.HARMFUL:MobEffectCategory.BENEFICIAL,
            switch(name){case "oakskin"->0x785134;case "charge"->0x66c8e8;case "blight"->0x687d32;default->0xa8bdd9;});this.name=name;}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return name.equals("chilled")||name.equals("immolate");}
        @Override public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level,LivingEntity target,int amplifier){if(name.equals("immolate"))return JimtaImmolate.tick(level,target,amplifier);if(JimtaSpellbooks.enabled()&&name.equals("chilled"))target.setTicksFrozen(Math.min(300,target.getTicksFrozen()+2));return true;}
    }
}
