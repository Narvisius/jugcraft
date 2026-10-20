package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Physical equipment delivers its inscription at the actual strike, hook or arrow impact. */
public final class NouveauEquipment {
    private static final Set<UUID> CASTING=new HashSet<>();
    private record ArrowCast(Plan plan,Cause cause,Ledger ledger,long until){}
    private static final Map<UUID,ArrowCast> ARROWS=new HashMap<>();
    private NouveauEquipment(){}
    public static void register(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{ARROWS.clear();CASTING.clear();});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20==0)ARROWS.entrySet().removeIf(e->e.getValue().until()<server.getTickCount());});
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,damage,blocked)->{
            if(!(target instanceof ServerPlayer p)||!JugNouveau.enabled())return;
            if(blocked&&(p.getUseItem().is(NouveauCatalog.item("enchanters_shield"))||p.getOffhandItem().is(NouveauCatalog.item("enchanters_shield")))){
                p.setAttached(JugcraftConcordance.FOCUS,ConcordanceProgress.focus(p).gain(ConcordanceProgress.now(p.level()),2));
                NouveauStatuses.give(p,"mana_regen",100,0,p);NouveauStatuses.give(p,"spell_damage",100,0,p);
            }
            if(damage<=0||CASTING.contains(p.getUUID()))return;
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                var armor=p.getItemBySlot(slot);if(armor.getOrDefault(NouveauGear.REACTIVE,false)&&armor.has(JugcraftConcordance.INSCRIPTION)){
                    castAt(p,armor,impact(p,p),1);break;
                }
            }
        });
    }
    public static void melee(ServerPlayer p,Entity target){if(!CASTING.contains(p.getUUID())&&target instanceof LivingEntity living&&p.getMainHandItem().is(NouveauCatalog.item("enchanters_sword"))&&Authority.mayStrike(p,living)&&ConcordanceEffects.mayHarm(p,living))castAt(p,p.getMainHandItem(),impact(p,living),1);}
    public static int enchantment(net.minecraft.world.entity.player.Player p,ItemStack stack,String name){var holder=p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,io.github.jimbozoomer.jugcraft.Jugcraft.id("nouveau_"+name))).orElse(null);return holder==null?0:net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder,stack);}
    public static int enchantments(net.minecraft.world.entity.player.Player p,String name){int levels=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))levels+=enchantment(p,p.getItemBySlot(slot),name);return levels;}
    public static void swing(ServerPlayer p){
        if(!JugNouveau.enabled()||NouveauScrying.active(p)||!p.isAlive()||p.isSpectator()||CASTING.contains(p.getUUID()))return;var held=p.getMainHandItem();int level=enchantment(p,held,"reactive");if(level==0&&held.getOrDefault(NouveauGear.REACTIVE,false))level=1;
        if(level<=0||!held.has(JugcraftConcordance.INSCRIPTION)||p.getRandom().nextInt(10)>=level||!RateGate.allow(p,"nouveau_reactive",10))return;
        var instrument=new Instrument("jugcraft:nouveau_reactive","jugcraft:nouveau_reactive",32,12,160,2,2400);var compiled=io.github.jimbozoomer.jugcraft.concordance.compose.Compiler.compileAuthored(held.get(JugcraftConcordance.INSCRIPTION).text(),ConcordanceData.rules().catalog(),instrument);if(compiled.plan()!=null)castAt(p,held,ComposedSpells.deliver(p.level(),p,compiled.plan().root(),null),1);
    }
    public static ComposedSpells.Impact impact(ServerPlayer p,LivingEntity target){return new ComposedSpells.Impact(target.position(),target.getUUID(),target.blockPosition(),target.blockPosition(),p.getEyePosition());}
    public static boolean castAt(ServerPlayer p,ItemStack tool,ComposedSpells.Impact impact,double durationMultiplier){
        if(!JugNouveau.enabled()||NouveauScrying.active(p)||!p.isAlive()||p.isSpectator()||CASTING.contains(p.getUUID()))return false;
        var inscription=tool.get(JugcraftConcordance.INSCRIPTION);if(inscription==null)return false;
        var instrument=ConcordanceData.rules().catalog().instrumentFor(BuiltInRegistries.ITEM.getKey(tool.getItem()).toString());
        if(instrument==null&&(tool.getItem() instanceof NouveauGear.Armor||tool.getOrDefault(NouveauGear.REACTIVE,false)||enchantment(p,tool,"reactive")>0))instrument=new Instrument("jugcraft:nouveau_reactive","jugcraft:nouveau_reactive",32,12,160,2,2400);
        if(instrument==null)return false;
        ConcordanceProgress.grant(p,"jugcraft:nouveau_arcana",io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState.MASTERED);
        var compiled=io.github.jimbozoomer.jugcraft.concordance.compose.Compiler.compile(inscription.text(),ConcordanceData.rules().catalog(),r->ConcordanceProgress.knowledge(p).state(r),instrument);
        if(compiled.plan()==null)return false;var plan=compiled.plan();
        if(durationMultiplier!=1)plan=duration(plan,durationMultiplier);
        int cost=NouveauGear.spellCost(p,plan);if(tool.is(NouveauCatalog.item("enchanters_mirror"))||tool.is(NouveauCatalog.item("enchanters_gauntlet")))cost=Math.max(1,cost-1);if(ConcordanceProgress.currentFocus(p)<cost||!RateGate.allow(p,"nouveau_equipment_cast",Math.max(10,plan.cooldown())))return false;
        CASTING.add(p.getUUID());try{
            var outcome=ComposedSpells.performAt(p.level(),p,plan,Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:nouveau_equipment",ConcordanceEffects.nextSerial()),new Ledger(plan.limits()),impact);
            if(outcome.applied())ConcordanceProgress.spendFocus(p,cost);return outcome.applied();
        }finally{CASTING.remove(p.getUUID());}
    }
    public static Plan duration(Plan plan,double multiplier){return new Plan(plan.text(),duration(plan.root(),multiplier),plan.focus(),plan.cooldown(),plan.capacity(),plan.limits(),plan.duration());}
    private static Plan.Node duration(Plan.Node node,double multiplier){
        var steps=node.steps().stream().map(s->new Plan.Step(s.index(),s.component(),s.effect().duration()>0?s.effect().withDuration((int)Math.min(2400,s.effect().duration()*multiplier)):s.effect(),s.principle(),s.scaling())).toList();
        return new Plan.Node(node.depth(),node.delivery(),node.form(),node.range(),node.selection(),node.pick(),node.radius(),node.targets(),steps,node.termination(),node.pulses(),node.interval(),node.then()==null?null:duration(node.then(),multiplier));
    }
    public static void arrowTick(AbstractArrow arrow){
        if(!(arrow.level() instanceof ServerLevel l)||!(arrow.getOwner() instanceof ServerPlayer p)||NouveauGear.CONJURED_ARROW==null)return;var ammo=arrow.getPickupItemStackOrigin();
        if(ammo.getOrDefault(NouveauGear.CONJURED_ARROW,false))arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        if(arrow.tickCount!=1||ammo.has(NouveauGear.ARROW_SHOT))return;
        if(ammo.getOrDefault(NouveauGear.CONJURED_ARROW,false)){if(ConcordanceProgress.currentFocus(p)<1){arrow.discard();return;}ConcordanceProgress.spendFocus(p,1);}
        ammo.set(NouveauGear.ARROW_SHOT,arrow.getUUID());
        if(ammo.is(NouveauCatalog.item("amplify_arrow")))arrow.setBaseDamage(((NouveauArrowAccess)arrow).jugcraft$baseDamage()+1);
        if(ammo.is(NouveauCatalog.item("split_arrow")))for(int side:new int[]{-1,1}){
            var copy=ammo.copyWithCount(1);copy.set(NouveauGear.CONJURED_ARROW,true);var child=new net.minecraft.world.entity.projectile.arrow.Arrow(l,p,copy,arrow.getWeaponItem());child.snapTo(arrow.position());child.setDeltaMovement(arrow.getDeltaMovement().yRot(side*.15F));child.pickup=AbstractArrow.Pickup.DISALLOWED;l.addFreshEntity(child);
        }
    }
    public static void arrowHit(AbstractArrow arrow,HitResult hit){
        if(!(arrow.getOwner() instanceof ServerPlayer p)||arrow.getWeaponItem()==null||!JugNouveau.enabled()||!p.isAlive()||p.isSpectator())return;
        var weapon=arrow.getWeaponItem();if(!weapon.is(NouveauCatalog.item("spell_bow"))&&!weapon.is(NouveauCatalog.item("spell_crossbow")))return;
        var inscription=weapon.get(JugcraftConcordance.INSCRIPTION);if(inscription==null)return;var ammo=arrow.getPickupItemStackOrigin();UUID key=ammo.getOrDefault(NouveauGear.ARROW_SHOT,arrow.getUUID());var cast=ARROWS.get(key);
        if(cast==null){
            if(ARROWS.size()>=256)return;
            var instrument=ConcordanceData.rules().catalog().instrumentFor(BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString());if(instrument==null)return;var compiled=io.github.jimbozoomer.jugcraft.concordance.compose.Compiler.compileAuthored(inscription.text(),ConcordanceData.rules().catalog(),instrument);if(compiled.plan()==null)return;
            Plan plan=compiled.plan();if(ammo.is(NouveauCatalog.item("amplify_arrow"))){var n=plan.root();var steps=n.steps().stream().map(s->new Plan.Step(s.index(),s.component(),s.effect().withMagnitude(Math.min(40,s.effect().magnitude()+Math.max(1,s.effect().magnitude()/2))),s.principle(),s.scaling())).toList();plan=new Plan(plan.text(),new Plan.Node(n.depth(),n.delivery(),n.form(),n.range(),n.selection(),n.pick(),n.radius(),n.targets(),steps,n.termination(),n.pulses(),n.interval(),n.then()),plan.focus(),plan.cooldown(),plan.capacity(),plan.limits(),plan.duration());}
            int cost=NouveauGear.spellCost(p,plan);if(ConcordanceProgress.currentFocus(p)<cost)return;ConcordanceProgress.spendFocus(p,cost);
            int multiple=ammo.is(NouveauCatalog.item("split_arrow"))||ammo.is(NouveauCatalog.item("pierce_arrow"))?3:1;
            cast=new ArrowCast(plan,Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:nouveau_arrow",ConcordanceEffects.nextSerial()),new Ledger(new Ledger.Limits(Math.min(64,plan.limits().targets()*multiple),Math.min(384,plan.limits().work()*multiple),plan.limits().branches())),p.level().getServer().getTickCount()+2400);ARROWS.put(key,cast);
        }
        BlockPos at=BlockPos.containing(hit.getLocation()),open=at;UUID entity=null;
        if(hit instanceof BlockHitResult block){at=block.getBlockPos();open=at.relative(block.getDirection());}
        if(hit instanceof EntityHitResult struck){if(!(struck.getEntity() instanceof LivingEntity))return;entity=struck.getEntity().getUUID();}
        ComposedSpells.performAt(p.level(),p,cast.plan(),cast.cause(),cast.ledger(),new ComposedSpells.Impact(hit.getLocation(),entity,open,at,arrow.position()));
    }
}
