package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyArtifacts;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** The owner's status vocabulary, shared by glyphs, foods, equipment and potions. */
public final class NouveauStatuses {
    public static final Map<String,Holder<MobEffect>> EFFECTS=new LinkedHashMap<>();
    private static AttachmentType<UUID> CASTER;
    private static AttachmentType<Integer> BUBBLE;private static AttachmentType<Long> BUBBLE_UNTIL;
    private static final Set<String> HARMFUL=Set.of("blasting","freezing","gravity","hex","shocked","snared","summoning_sickness");
    private NouveauStatuses(){}
    public static void register(){
        NouveauBubble.register();
        BUBBLE=AttachmentRegistry.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).buildAndRegister(Jugcraft.id("nouveau_bubble_power"));BUBBLE_UNTIL=AttachmentRegistry.<Long>builder().persistent(com.mojang.serialization.Codec.LONG).buildAndRegister(Jugcraft.id("nouveau_bubble_until"));
        CASTER=AttachmentRegistry.<UUID>builder().persistent(UUIDUtil.CODEC).buildAndRegister(Jugcraft.id("nouveau_status_caster"));
        for(String name:List.of("blasting","bounce","flight","freezing","glide","gravity","hex","immolate","looting","magic_find","mana_regen","recovery","scrying","shielding","shocked","snared","soaked","spell_damage","summoning_sickness")){
            var effect=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("nouveau_"+name),(MobEffect)new Status(name));EFFECTS.put(name,effect);
        }
    }
    public static void bubble(LivingEntity target,int power,int duration,LivingEntity actor){target.setAttached(BUBBLE,power);target.setAttached(BUBBLE_UNTIL,target.level().getGameTime()+duration);target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,duration,0),actor);NouveauBubble.show(target,duration,false);}
    public static boolean has(LivingEntity e,String name){return EFFECTS.containsKey(name)&&e.hasEffect(EFFECTS.get(name));}
    public static int strength(LivingEntity e,String name){var effect=EFFECTS.get(name);var value=effect==null?null:e.getEffect(effect);return value==null?0:value.getAmplifier()+1;}
    public static boolean give(LivingEntity e,String name,int duration,int amplifier,LivingEntity caster){
        var effect=EFFECTS.get(name);if(effect==null)return false;if(caster!=null)e.setAttached(CASTER,caster.getUUID());
        return e.addEffect(new MobEffectInstance(effect,Math.clamp(duration,1,24000),Math.clamp(amplifier,0,4)),caster);
    }
    private static final class Status extends MobEffect {
        private final String name;
        Status(String name){super(HARMFUL.contains(name)?MobEffectCategory.HARMFUL:MobEffectCategory.BENEFICIAL,HARMFUL.contains(name)?0x8f62b5:0x74c8b1);this.name=name;}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
        @Override public boolean applyEffectTick(ServerLevel l,LivingEntity e,int amplifier){
            if(!JugNouveau.enabled())return true;
            switch(name){
                case "freezing" -> {e.setTicksFrozen(Math.min(300,e.getTicksFrozen()+2+amplifier));e.setDeltaMovement(e.getDeltaMovement().multiply(.8,1,.8));}
                case "soaked" -> e.clearFire();
                case "snared" -> {e.setDeltaMovement(0,Math.min(0,e.getDeltaMovement().y),0);if(e instanceof Mob m)m.getNavigation().stop();}
                case "gravity" -> {if(!e.onGround())e.setDeltaMovement(e.getDeltaMovement().multiply(.9,1,.9).add(0,-.15,0));if(e instanceof ServerPlayer p&&!p.isCreative()&&!p.isSpectator()&&p.getAbilities().flying){p.getAbilities().flying=false;p.onUpdateAbilities();}}
                case "flight" -> {if(e instanceof ServerPlayer p&&!has(e,"gravity"))AlchemyArtifacts.grantRitualFlight(p,40);}
                case "glide" -> {if(e instanceof Player p&&!p.onGround()&&!p.isInWater()&&!has(e,"gravity"))p.startFallFlying();}
                case "mana_regen" -> {if(e instanceof ServerPlayer p&&e.tickCount%Math.max(10,40/(amplifier+1))==0)p.setAttached(JugcraftConcordance.FOCUS,ConcordanceProgress.focus(p).gain(ConcordanceProgress.now(l),1));}
                case "hex" -> {if(e instanceof ServerPlayer p&&p.tickCount%80==0)ConcordanceProgress.spendFocus(p,1);}
                case "magic_find" -> {if(e.tickCount%40==0)for(var target:l.getEntitiesOfClass(LivingEntity.class,e.getBoundingBox().inflate(75),t->t instanceof NouveauBeast||t instanceof NouveauHelper||t instanceof Starbuncle||t instanceof BookWyrm).stream().limit(32).toList())target.addEffect(new MobEffectInstance(MobEffects.GLOWING,60));}
                case "scrying" -> {} // NouveauScrySight scans once per player, with bounded work and private markers.
                case "blasting" -> {var effect=e.getEffect(EFFECTS.get(name));if(effect!=null&&effect.getDuration()==1){var p=Authority.answering(l,e.getAttached(CASTER));if(p!=null)burst(l,p,e,4+amplifier*2);}}
                default -> {}
            }return true;
        }
    }
    public static void burst(ServerLevel l,ServerPlayer p,LivingEntity center,int damage){
        var ledger=new Ledger(new Ledger.Limits(16,64,0));var cause=Cause.of(p.getUUID(),Cause.Origin.SPELL,"jugcraft:nouveau_blasting",ConcordanceEffects.nextSerial());
        for(var target:l.getEntitiesOfClass(LivingEntity.class,center.getBoundingBox().inflate(3),LivingEntity::isAlive).stream().limit(16).toList())ConcordanceEffects.apply(new ConcordanceEffects.Context(l,cause,p,ledger,"blast",center.position()),EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,damage,0),target);
        l.sendParticles(ParticleTypes.FLAME,center.getX(),center.getY()+.7,center.getZ(),32,1,1,1,.06);
    }
    private static void scry(ServerLevel l,ServerPlayer p){
        Block chosen=p.getOffhandItem().getItem() instanceof net.minecraft.world.item.BlockItem b?b.getBlock():null;
        int start=Math.floorMod(p.tickCount/20,81)*9;
        for(int i=0;i<9;i++){int n=(start+i)%729;var at=p.blockPosition().offset(n%9-4,(n/81)%9-4,(n/9)%9-4);if(!l.hasChunkAt(at))continue;var s=l.getBlockState(at);
            if(chosen!=null?s.is(chosen):s.is(Blocks.BUDDING_AMETHYST))l.sendParticles(p,ParticleTypes.END_ROD,true,false,at.getX()+.5,at.getY()+.5,at.getZ()+.5,1,0,0,0,0);
        }
    }
    public static float incoming(LivingEntity e,net.minecraft.world.damagesource.DamageSource source,float amount){
        if(!JugNouveau.enabled())return amount;
        if(amount>0&&BUBBLE!=null&&e.hasAttached(BUBBLE)){if(e.level().getGameTime()<e.getAttachedOrElse(BUBBLE_UNTIL,0L)){amount+=e.getAttachedOrElse(BUBBLE,0);NouveauBubble.show(e,5,true);e.removeEffect(MobEffects.LEVITATION);}e.removeAttached(BUBBLE);e.removeAttached(BUBBLE_UNTIL);}
        if(has(e,"hex")&&(e.isOnFire()||e.getTicksFrozen()>0||e.hasEffect(MobEffects.POISON)||e.hasEffect(MobEffects.WITHER)))amount+=strength(e,"hex");
        if(source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)){
            if(has(e,"gravity"))amount*=2;
            if(has(e,"bounce")){e.setDeltaMovement(e.getDeltaMovement().multiply(1+.2*strength(e,"bounce"),0,1+.2*strength(e,"bounce")).add(0,.7,0));if(e instanceof ServerPlayer p)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(e));return 0;}
        }
        return Math.max(0,amount-strength(e,"shielding"));
    }
    public static float healing(LivingEntity e,float amount){if(has(e,"hex"))amount*=.5F;return amount*(1+.2F*strength(e,"recovery"));}
}
