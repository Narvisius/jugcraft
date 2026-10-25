package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauStatuses;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.event.*;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.cost.Ammo;
import net.spell_power.api.SpellPower;
import org.jspecify.annotations.Nullable;

/** The held item's finite spell list is authoritative. No client-provided effect, target or price is accepted. */
public final class JimtaSpellcasting {
    public record Working(String name, String school, int focus, int cooldown, String description) {}
    public static final Map<String, Working> SPELLS = new LinkedHashMap<>();
    private static final Map<ServerPlayer, Map<String, Long>> READY = new WeakHashMap<>();
    private static final String SOURCE = "jugcraft:jimtafacts_spellbooks", PREFIX = "jugcraft:jimtafacts/";
    private static final TagKey<MobEffect> CLEANSE_IMMUNE = TagKey.create(Registries.MOB_EFFECT, Jugcraft.id("jimtafacts/cleanse_immune"));
    private static final Ledger.Limits LIMITS = new Ledger.Limits(16, 96, 0);
    private JimtaSpellcasting() {}

    public static void register() {
        JimtaSpellbooks.data("spells").entrySet().forEach(entry -> {
            var row=entry.getValue().getAsJsonObject(); SPELLS.put(entry.getKey(), new Working(entry.getKey(), row.get("school").getAsString(),
                row.get("focus").getAsInt(), row.get("cooldown").getAsInt(), row.get("description").getAsString()));
        });
        JimtaSpellStatuses.register();
        JimtaCombatSpells.register();
        JimtaFieldSpells.register();
        JimtaSummonSpells.register();
        JimtaChannelSpells.register();
        JimtaStrikeSpells.register();
        JimtaConstruct.register();
        JimtaAttackSpells.register();
        JimtaWardSpells.register();
        JimtaPersonalSpells.register();
        JimtaAreaSpells.register();
        SpellContainerSource.addSource(new SpellContainerSource.Entry(SOURCE, JimtaSpellcasting::containers,
            p -> sources(p).stream().map(s->List.of(s.getItem(),JimtaSpellbooks.spells(s))).toList()));
        SpellHandlers.registerCustomImpact(Jugcraft.id("jimtafacts_spellbook"), JimtaSpellcasting::impact);
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> {
            try {
                String name=name(args.spell()); if(name==null) return null;
                var player=args.caster(); var held=source(player,name); var working=SPELLS.get(name);
                if(working==null||!JimtaSpellbooks.enabled()||!JimtaSpellbooks.spells(held).contains(name)) return SpellCast.Attempt.none();
                if(player instanceof ServerPlayer server&&!ready(server,name))return SpellCast.Attempt.none();
                if(!(player instanceof ServerPlayer recaster&&JimtaRecasts.isPending(recaster,name))&&ConcordanceProgress.currentFocus(player)<JimtaSpellbooks.focusCost(player,held,working))
                    return SpellCast.Attempt.failMissingItem(new SpellCast.Attempt.MissingItemInfo(new Ammo.Searched(ConcordanceSpells.MISSING_FOCUS,null)));
                return null;
            } catch(RuntimeException error) { Jugcraft.LOGGER.error("Jimtafacts spellbook cast check",error); return SpellCast.Attempt.none(); }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->READY.clear());
    }
    static List<ItemStack> sources(Player p){var list=new ArrayList<ItemStack>();list.add(p.getMainHandItem());list.addAll(JimtaSpecialArmor.equipped(p));return list;}
    public static ItemStack source(Player player,String name){return sources(player).stream().filter(s->JimtaSpellbooks.spells(s).contains(name)).findFirst().orElse(ItemStack.EMPTY);}
    private static List<SpellContainerSource.SourcedContainer> containers(Player player,String source) {
        try {
            var spells=sources(player).stream().flatMap(stack->JimtaSpellbooks.spells(stack).stream()).distinct().map(s->PREFIX+s).toList();
            return spells.isEmpty()?List.of():List.of(new SpellContainerSource.SourcedContainer(source,null,
                new SpellContainer(SpellContainer.ContentType.NONE,"","","",0,spells,0)));
        } catch(RuntimeException error) { Jugcraft.LOGGER.error("Jimtafacts spellbook list",error); return List.of(); }
    }
    static @Nullable String name(Holder<Spell> spell) {
        String id=ConcordanceSpells.spellId(spell); return id!=null&&id.startsWith(PREFIX)?id.substring(PREFIX.length()):null;
    }
    private static SpellHandlers.ImpactResult impact(Holder<Spell> spell,SpellPower.Result power,LivingEntity caster,@Nullable Entity ignored,SpellExecution.ImpactContext context) {
        try {
            String name=name(spell);
            if(caster instanceof ServerPlayer player&&JimtaChannelSpells.PROFILES.containsKey(name))return new SpellHandlers.ImpactResult(JimtaChannelSpells.pulse(player,name,source(player,name)),false);
            return new SpellHandlers.ImpactResult(caster instanceof ServerPlayer player&&name!=null&&cast(player,InteractionHand.MAIN_HAND,name),false);
        } catch(RuntimeException error) { Jugcraft.LOGGER.error("Jimtafacts spellbook impact",error); return new SpellHandlers.ImpactResult(false,false); }
    }
    public static boolean castScroll(ServerPlayer player,InteractionHand hand,JimtaSpellbooks.Scroll scroll) {
        if(JimtaChannelSpells.PROFILES.containsKey(scroll.spell)){
            var stack=player.getItemInHand(hand);if(!ready(player,scroll.spell)||!JimtaChannelSpells.pulse(player,scroll.spell,stack))return false;
            JimtaChannelSpells.end(player,scroll.spell);stack.consume(1,player);cooldown(player,scroll.spell,SPELLS.get(scroll.spell).cooldown());return true;
        }
        return JimtaCombatSpells.PROFILES.containsKey(scroll.spell)?JimtaCombatSpells.scroll(player,hand,scroll.spell):cast(player,hand,scroll.spell);
    }
    static boolean ready(ServerPlayer player,String name) {if(JimtaSummonSpells.NAMES.contains(name)&&io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons.hasSpellbookSummon(player,name))return false;return READY.getOrDefault(player,Map.of()).getOrDefault(name,Long.MIN_VALUE)<=player.level().getGameTime();}
    public static void cooldown(ServerPlayer player,String name,int ticks) {READY.computeIfAbsent(player,p->new HashMap<>()).put(name,player.level().getGameTime()+Math.max(0,(int)Math.ceil(JimtaSpellbookCurios.cooldownTicks(player,ticks)*JimtaUpgrades.cooldownFactor(player))));}
    public static boolean cast(ServerPlayer player,InteractionHand hand,String name) {
        var working=SPELLS.get(name); var stack=player.getItemInHand(hand);if(!JimtaSpellbooks.spells(stack).contains(name))stack=source(player,name);
        if(working==null||!JimtaSpellbooks.enabled()||!player.isAlive()||player.isSpectator()||!JimtaSpellbooks.spells(stack).contains(name)) return false;
        long now=player.level().getGameTime(); var ready=READY.computeIfAbsent(player,p->new HashMap<>());
        if(ready.getOrDefault(name,Long.MIN_VALUE)>now) return false;
        boolean recast=JimtaRecasts.isPending(player,name);
        if(!ready(player,name))return false;
        int cost=recast?0:JimtaSpellbooks.focusCost(player,stack,working);
        if(ConcordanceProgress.currentFocus(player)<cost) return false;
        if(!perform(player,name)) return false;
        // Effect, payment and item consumption are one server-thread operation. No listeners defer payment.
        ConcordanceProgress.spendFocus(player,cost); if(!JimtaRecasts.isPending(player,name)&&!JimtaSummonSpells.NAMES.contains(name))cooldown(player,name,working.cooldown());
        if(!recast&&stack.getItem() instanceof JimtaSpellbooks.Scroll) stack.consume(1,player);
        return true;
    }
    public static boolean ally(ServerPlayer player,LivingEntity target) {
        return player==target||io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons.ownedBy(target,player.getUUID())||target instanceof Player other&&JugcraftParties.sameParty(player.getUUID(),other.getUUID())
            ||target instanceof TamableAnimal pet&&pet.getOwner()==player;
    }
    private static ConcordanceEffects.Context context(ServerPlayer player,String name,String step,Ledger ledger,long serial) {
        return new ConcordanceEffects.Context(player.level(),Cause.of(player.getUUID(),Cause.Origin.SPELL,PREFIX+name,serial),player,ledger,step,player.position());
    }
    private static EffectSpec status(String name,int duration,boolean harmful) {
        return new EffectSpec(EffectKind.STATUS,harmful?Intent.HARMFUL:Intent.HELPFUL,0,duration,"jugcraft:jimtafacts_"+name,Stacking.STRONGEST,null);
    }
    static @Nullable LivingEntity target(ServerPlayer player,double range) {
        Vec3 eye=player.getEyePosition(),end=eye.add(player.getLookAngle().scale(range));
        var block=player.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        Vec3 stop=block.getType()==HitResult.Type.BLOCK?block.getLocation():end;
        var hit=ProjectileUtil.getEntityHitResult(player,eye,stop,player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
            e->e instanceof LivingEntity&&e.isAlive()&&!e.isSpectator()&&e.isPickable(),eye.distanceToSqr(stop));
        return hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
    }
    /** Public for focused tests; normal use always comes through the held-item/cost gate above. */
    public static boolean perform(ServerPlayer player,String name) {
        if(!SPELLS.containsKey(name)||!JimtaSpellbooks.enabled()||!player.isAlive()||player.isSpectator())return false;
        var level=player.level();var ledger=new Ledger(LIMITS);long serial=ConcordanceEffects.nextSerial();
        var context=context(player,name,"effect",ledger,serial);
        if(JimtaFieldSpells.PROFILES.containsKey(name))return JimtaFieldSpells.start(player,name);
        if(JimtaSummonSpells.NAMES.contains(name))return JimtaSummonSpells.cast(player,name);
        if(JimtaAttackSpells.NAMES.contains(name))return JimtaAttackSpells.cast(player,name);
        if(JimtaWardSpells.NAMES.contains(name))return JimtaWardSpells.cast(player,name);
        if(JimtaAreaSpells.NAMES.contains(name))return JimtaAreaSpells.cast(player,name);
        if(JimtaPersonalSpells.NAMES.contains(name))return JimtaPersonalSpells.cast(player,name);
        if(JimtaConstruct.SPELLS.contains(name))return JimtaConstruct.cast(player,name);
        if(JimtaStrikeSpells.NAMES.contains(name))return JimtaStrikeSpells.cast(player,name);
        switch(name) {
            case "heal" -> { return ConcordanceEffects.apply(context,EffectSpec.of(EffectKind.RESTORATION,Intent.HELPFUL,5,0),player).applied(); }
            case "greater_heal" -> {
                if(player.getHealth()>=player.getMaxHealth())return false;
                // Shared restoration uses a deliberately bounded forty-point effect; repeated steps
                // permit large-health players while retaining the ledger's sixteen-target cap.
                int missing=(int)Math.ceil(player.getMaxHealth()-player.getHealth());boolean changed=false;
                for(int i=0;i<16&&missing>0;i++,missing-=40)changed|=ConcordanceEffects.apply(context(player,name,"restore/"+i,ledger,serial),EffectSpec.of(EffectKind.RESTORATION,Intent.HELPFUL,Math.min(40,missing),0),player).applied();
                return changed;
            }
            case "cleanse","fortify" -> {
                double radius=name.equals("cleanse")?3:8;boolean changed=false;
                for(var living:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(radius),e->e.isAlive()&&ally(player,e)&&player.distanceToSqr(e)<=radius*radius).stream().limit(16).toList()) {
                    if(name.equals("fortify")) changed|=ConcordanceEffects.apply(context,EffectSpec.of(EffectKind.PROTECTION,Intent.HELPFUL,8,2400),living).applied();
                    else if(ledger.admit("cleanse","entity:"+living.getUUID(),1).admitted()) {
                        for(var effect:List.copyOf(living.getActiveEffects()))if(effect.getEffect().value().getCategory()==MobEffectCategory.HARMFUL&&!effect.getEffect().is(CLEANSE_IMMUNE))changed|=living.removeEffect(effect.getEffect());
                    }
                }
                return changed;
            }
            case "haste" -> { var living=target(player,24);if(living==null||!ally(player,living))living=player;return ConcordanceEffects.apply(context,status("hastened",600,false),living).applied(); }
            case "blessing_of_life" -> {var living=target(player,64);return living!=null&&ally(player,living)&&ConcordanceEffects.apply(context,EffectSpec.of(EffectKind.RESTORATION,Intent.HELPFUL,6,0),living).applied();}
            case "spider_aspect" -> {return ConcordanceEffects.apply(context,status("spider_aspect",400,false),player).applied();}
            case "counterspell" -> {
                var living=target(player,80);if(living==null||!TwoHanded.allowed(player,level,living)||!ConcordanceEffects.mayHarm(player,living)||ally(player,living))return false;
                if(living instanceof JimtaSpellCreature creature){creature.release();return true;}
                boolean changed=false;
                if(living instanceof ServerPlayer other&&other instanceof net.spell_engine.internals.casting.SpellCaster.Player caster&&caster.getSpellCastProcess()!=null){caster.getInteractor().requestClear();changed=true;}
                for(var effect:JimtaSpellStatuses.EFFECTS.values())changed|=living.removeEffect(effect);
                return changed;
            }
            case "slow","blight" -> {
                var living=target(player,24);if(living==null||!TwoHanded.allowed(player,level,living))return false;
                return ConcordanceEffects.apply(context,status(name.equals("slow")?"slowed":"blight",400,true),living).applied();
            }
            case "charge","oakskin","gluttony" -> {return ConcordanceEffects.apply(context,status(name,name.equals("gluttony")?1200:600,false),player).applied();}
            case "angel_wings" -> {return NouveauStatuses.give(player,"flight",600,0,player);}
            case "root","wisp" -> {var aimed=target(player,name.equals("root")?32:48);return aimed!=null&&JimtaSpellCreature.summon(player,aimed,name);}
            case "summon_ender_chest" -> {
                player.openMenu(new SimpleMenuProvider((id,inventory,who)->ChestMenu.threeRows(id,inventory,player.getEnderChestInventory()),Component.translatable("container.enderchest")));
                return true;
            }
            case "teleport" -> {return teleport(player,16);}
            case "touch_dig" -> {
                var hit=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(8)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
                if(hit.getType()!=HitResult.Type.BLOCK)return false;
                return ConcordanceEffects.apply(context,new EffectSpec(EffectKind.NOUVEAU_BLOCK,Intent.HARMFUL,3,0,"jugcraft:nouveau/break",Stacking.STRONGEST,null),hit.getBlockPos()).applied();
            }
            case "wololo" -> {
                if(!(target(player,16) instanceof Sheep sheep)||!TwoHanded.allowed(player,level,sheep)||!Authority.mayChange(level,player,sheep.blockPosition()))return false;
                int old=sheep.getColor().getId();sheep.setColor(DyeColor.values()[(old+1+player.getRandom().nextInt(15))%16]);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,sheep.getX(),sheep.getY()+.5,sheep.getZ(),12,.4,.3,.4,0);return true;
            }
            default -> {return false;}
        }
    }
    static boolean teleport(ServerPlayer player,int range) {
        var level=player.level();Vec3 eye=player.getEyePosition(),forward=player.getLookAngle();
        var hit=level.clip(new ClipContext(eye,eye.add(forward.scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,player));
        double reach=Math.min(range,eye.distanceTo(hit.getLocation())-.6);
        for(double distance=reach;distance>=2;distance-=.5) {
            Vec3 at=eye.add(forward.scale(distance)).subtract(0,player.getEyeHeight(),0);BlockPos pos=BlockPos.containing(at);
            if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!Authority.mayChange(level,player,pos)
                    ||!Authority.mayChange(level,player,player.blockPosition())||!level.getFluidState(pos).isEmpty()
                    ||!level.noCollision(player,player.getBoundingBox().move(at.subtract(player.position()))))continue;
            player.teleportTo(level,at.x,at.y,at.z,Set.of(),player.getYRot(),player.getXRot(),true);player.resetFallDistance();JimtaSpellbookCurios.afterTeleport(player);
            level.sendParticles(ParticleTypes.PORTAL,at.x,at.y+.5,at.z,20,.3,.7,.3,.1);return true;
        }
        return false;
    }
}
