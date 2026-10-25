package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.*;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellPower;
import org.jspecify.annotations.Nullable;

/** Native Spell Engine collision/targeting, shared authority, one cost per cast (never per target). */
public final class JimtaCombatSpells {
    public record Profile(int damage,String secondary,int duration) {}
    public static final Map<String,Profile> PROFILES=new LinkedHashMap<>();
    private static final Map<ServerPlayer,Float> LEECH=new WeakHashMap<>();
    private JimtaCombatSpells() {}
    public static void register() {
        JimtaBloodProjectile.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->LEECH.clear());
        JimtaSpellbooks.data("combat").entrySet().forEach(e->{var r=e.getValue().getAsJsonObject();PROFILES.put(e.getKey(),new Profile(r.get("damage").getAsInt(),r.get("secondary").getAsString(),r.get("duration").getAsInt()));});
        SpellHandlers.registerCustomImpact(Jugcraft.id("jimtafacts_combat"),JimtaCombatSpells::impact);
        SpellEvents.COST_CONSUME.register(args->{
            try {
                String name=JimtaSpellcasting.name(args.spell());
                if(!(args.caster() instanceof ServerPlayer player)||!PROFILES.containsKey(name))return;
                var held=JimtaSpellcasting.source(player,name);var working=JimtaSpellcasting.SPELLS.get(name);
                if(working==null||!JimtaSpellbooks.spells(held).contains(name))return;
                ConcordanceProgress.spendFocus(player,JimtaSpellbooks.focusCost(player,held,working));
                JimtaSpellcasting.cooldown(player,name,working.cooldown());
                if(held.getItem() instanceof JimtaSpellbooks.Scroll)held.consume(1,player);
            }catch(RuntimeException error){Jugcraft.LOGGER.error("Jimtafacts combat settlement",error);}
        });
    }
    public static boolean scroll(ServerPlayer player,InteractionHand hand,String name) {
        if(hand!=InteractionHand.MAIN_HAND||!JimtaSpellbooks.enabled()||!JimtaSpellcasting.ready(player,name))return false;
        var entry=SpellRegistry.from(player.level()).get(Jugcraft.id("jimtafacts/"+name)).orElse(null);
        if(entry==null)return false;
        var targets=SpellTarget.findTargets(player,entry,SpellTarget.SearchResult.empty(),true);
        int before=player.getMainHandItem().getCount();
        SpellExecution.performSpell(player.level(),player,entry,targets,SpellCast.Action.RELEASE,1);
        return player.isCreative()||player.getMainHandItem().getCount()<before;
    }
    public static SpellHandlers.ImpactResult impact(Holder<Spell> spell,SpellPower.Result power,LivingEntity caster,@Nullable Entity target,SpellExecution.ImpactContext delivery) {
        try {
            String name=JimtaSpellcasting.name(spell);var profile=PROFILES.get(name);
            if(profile==null||!(caster instanceof ServerPlayer player)||!(target instanceof LivingEntity living)||!JimtaSpellbooks.enabled()
                    ||!player.isAlive()||player.isSpectator()||JimtaSpellcasting.ally(player,living)||!TwoHanded.allowed(player,player.level(),living))return new SpellHandlers.ImpactResult(false,false);
            var ledger=new Ledger(new Ledger.Limits(1,8,0));var cause=Cause.of(player.getUUID(),Cause.Origin.SPELL,"jugcraft:jimtafacts/"+name,ConcordanceEffects.nextSerial());
            var at=delivery.position()==null?player.position():delivery.position();
            var context=new ConcordanceEffects.Context(player.level(),cause,player,ledger,"damage",at);
            String school=switch(JimtaSpellcasting.SPELLS.get(name).school()) {case "ice"->"frost";case "blood"->"soul";case "holy"->"healing";case "fire","lightning"->JimtaSpellcasting.SPELLS.get(name).school();default->"arcane";};
            int damage=Math.clamp(profile.damage()+(int)Math.floor(Invocations.powerAboveBase(player,"spell_power:"+school)*.5),0,40);
            if(profile.damage()==0)damage=0;
            if(name.equals("fire_arrow")&&delivery.focusMode()==SpellTarget.FocusMode.AREA)damage=(damage+1)/2;
            float before=living.getHealth();int invulnerability=living.damageCooldownTime;
            boolean needles=name.equals("blood_needles")||name.equals("acupuncture");
            if(needles&&ConcordanceEffects.mayHarm(player,living))living.damageCooldownTime=0;
            boolean changed;
            try{changed=damage>0&&ConcordanceEffects.apply(context,new EffectSpec(EffectKind.DAMAGE,Intent.HARMFUL,damage,0,null,Stacking.STRONGEST,"spell_power:"+school),living).applied();}
            finally{if(needles)living.damageCooldownTime=Math.max(invulnerability,living.damageCooldownTime);}
            if(changed&&profile.secondary().startsWith("leech")){
                float ratio=profile.secondary().equals("leech25")?.25F:.15F;
                float stored=LEECH.getOrDefault(player,0F)+Math.max(0,before-living.getHealth())*ratio;int restore=Math.min(40,(int)stored);LEECH.put(player,stored-restore);
                if(restore>0)ConcordanceEffects.apply(new ConcordanceEffects.Context(player.level(),cause,player,ledger,"leech",at),EffectSpec.of(EffectKind.RESTORATION,Intent.HELPFUL,restore,0),player);
            }
            var secondary=new ConcordanceEffects.Context(player.level(),cause,player,ledger,"secondary",at);
            EffectSpec effect=switch(profile.secondary()) {
                case "ignite","rend_fire" -> new EffectSpec(EffectKind.NOUVEAU_CREATURE,Intent.HARMFUL,1,Math.min(profile.duration(),100),"jugcraft:nouveau/ignite",Stacking.STRONGEST,null);
                case "freeze" -> new EffectSpec(EffectKind.NOUVEAU_CREATURE,Intent.HARMFUL,1,profile.duration(),"jugcraft:nouveau/freeze",Stacking.STRONGEST,null);
                case "poison","wither" -> new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,profile.duration(),"minecraft:"+profile.secondary(),Stacking.STRONGEST,null);
                case "rend","guided","chilled" -> new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,profile.duration(),"jugcraft:jimtafacts_"+profile.secondary(),Stacking.STRONGEST,null);
                case "knockback" -> EffectSpec.of(EffectKind.MOVEMENT,Intent.HARMFUL,10,0);
                default -> null;
            };
            if(effect!=null)changed|=ConcordanceEffects.apply(secondary,effect,living).applied();
            if(profile.secondary().equals("rend_fire"))changed|=ConcordanceEffects.apply(new ConcordanceEffects.Context(player.level(),cause,player,ledger,"rend",at),new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,profile.duration(),"jugcraft:jimtafacts_rend",Stacking.STRONGEST,null),living).applied();
            return new SpellHandlers.ImpactResult(changed,false);
        }catch(RuntimeException error){Jugcraft.LOGGER.error("Jimtafacts combat impact",error);return new SpellHandlers.ImpactResult(false,false);}
    }
}
