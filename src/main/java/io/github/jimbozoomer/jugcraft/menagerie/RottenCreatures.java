package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.*;

/** Native adaptation of the supplied undead encounters; original constants remain in the source archive. */
public final class RottenCreatures {
    public static Holder<MobEffect> FREEZE, CHANNELLED;
    private static final JsonObject ATTRIBUTES=read("rotten_attributes");
    private RottenCreatures(){}
    private static JsonObject read(String name){
        try(var input=RottenCreatures.class.getResourceAsStream("/jugcraft/menagerie/"+name+".json")){
            return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    static AttributeSupplier.Builder attributes(Menagerie.Species species){
        var result=MenagerieAnimal.attributes(species);var values=ATTRIBUTES.getAsJsonObject(species.id());
        Map<String,Holder<Attribute>> names=Map.of("field_23716",Attributes.MAX_HEALTH,"field_23719",Attributes.MOVEMENT_SPEED,
                "field_23721",Attributes.ATTACK_DAMAGE,"field_23724",Attributes.ARMOR,"field_23717",Attributes.FOLLOW_RANGE,
                "field_23718",Attributes.KNOCKBACK_RESISTANCE,"field_23720",Attributes.FLYING_SPEED);
        values.entrySet().forEach(e->{if(names.containsKey(e.getKey()))result.add(names.get(e.getKey()),e.getValue().getAsDouble());});
        return result;
    }
    static void register(){
        FREEZE=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("rotten_freeze"),new MobEffect(MobEffectCategory.HARMFUL,5883888){}
                .addAttributeModifier(Attributes.MOVEMENT_SPEED,Jugcraft.id("rotten_freeze"),-.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        CHANNELLED=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("rotten_channelled"),new MobEffect(MobEffectCategory.HARMFUL,2064988){});
        Registry.registerForHolder(BuiltInRegistries.POTION,Jugcraft.id("rotten_corrupted"),new Potion("rotten_corrupted"));
        for(String name:List.of("freeze","long_freeze","strong_freeze"))Registry.registerForHolder(BuiltInRegistries.POTION,Jugcraft.id("rotten_"+name),new Potion("rotten_"+name,new MobEffectInstance(FREEZE,name.equals("long_freeze")?800:name.equals("strong_freeze")?200:400,name.equals("strong_freeze")?1:0)));
        for(var s:Menagerie.SPECIES.values())if(s.id().startsWith("rotten_"))Menagerie.item(s.id()+"_spawn_egg",p->new MenagerieItems.Spawn(s.id(),p));
        Menagerie.item("rotten_corrupted_wart",Item::new);
        for(String name:List.of("frozen_rotten_flesh","magma_rotten_flesh"))Menagerie.item("rotten_"+name,p->new Flesh(name,p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(1).build())));
        Menagerie.item("rotten_spear",p->new Spear(p.sword(ToolMaterial.IRON,3,-2.8F).stacksTo(1)));
        RottenStorage.register();
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,amount)->{
            if(!(entity instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||mob.isRemoved()||mob.isPassenger()||mob.isVehicle())return true;
            String id=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            if(!Set.of("minecraft:zombie","minecraft:zombie_villager","minecraft:drowned","minecraft:husk","jugcraft:rotten_swampy","jugcraft:rotten_undead_miner","jugcraft:rotten_mummy").contains(id))return true;
            String form=mob.isInLava()?"burned":mob.isFullyFrozen()?"frostbitten":"";
            int chance=level.getDifficulty()==Difficulty.HARD?2:level.getDifficulty()==Difficulty.NORMAL?4:0;
            if(form.isEmpty()||chance==0||mob.getRandom().nextInt(chance)!=0)return true;
            var replacement=Menagerie.TYPES.get("rotten_"+form).create(level,EntitySpawnReason.CONVERSION);
            if(replacement==null)return true;
            replacement.snapTo(mob.getX(),mob.getY(),mob.getZ(),mob.getYRot(),mob.getXRot());
            replacement.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),EntitySpawnReason.CONVERSION,null);
            replacement.setCustomName(mob.getCustomName());
            if(mob.isPersistenceRequired())replacement.setPersistenceRequired();
            if(!level.addFreshEntity(replacement))return true;
            for(var slot:EquipmentSlot.values()){replacement.equipSaved(slot,mob.getItemBySlot(slot).copy());mob.setItemSlot(slot,ItemStack.EMPTY);}
            mob.discard();return false;
        });
    }
    private static final class Flesh extends Item {
        private final String kind;
        Flesh(String kind,Properties p){super(p);this.kind=kind;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
            var result=super.finishUsingItem(stack,level,user);
            if(!level.isClientSide()&&user.getRandom().nextFloat()<.8F)user.addEffect(new MobEffectInstance(kind.startsWith("frozen")?FREEZE:MobEffects.HUNGER,300));
            return result;
        }
    }
    private static final class Spear extends Item {
        Spear(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(who instanceof ServerPlayer p){var held=p.getItemInHand(hand);if(!CavernProjectile.launch(p,held,"rotten_spear",true))return InteractionResult.FAIL;held.consume(1,p);p.getCooldowns().addCooldown(new ItemStack(this),15);}
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Undead extends MenagerieAnimal implements Enemy {
        private int cooldown=100,casting,limitedLife=-1,dashCooldown=100;
        private UUID summoner;
        private boolean wolfCalled;
        public Undead(EntityType<? extends MenagerieAnimal> type,Level level,Menagerie.Species species){super(type,level,species);}
        @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group){
            var data=super.finalizeSpawn(level,difficulty,reason,group);
            switch(species().source()){
                case "burned" -> variant(random.nextFloat()<.05F?1:0);
                case "undead_miner" -> setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND,new ItemStack(switch(variant()){case 1->Items.IRON_PICKAXE;case 2->Items.GOLDEN_PICKAXE;case 3->Items.DIAMOND_PICKAXE;default->Items.STONE_PICKAXE;}));
                case "glacial_hunter" -> setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND,new ItemStack(Menagerie.item("rotten_spear")));
                case "zombie_lackey","skeleton_lackey" -> {
                    setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));
                    if(species().source().equals("skeleton_lackey"))setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.STONE_SWORD));
                }
                default -> {}
            }
            return data;
        }
        @Override public void refreshArmor(){
            super.refreshArmor();var values=ATTRIBUTES.getAsJsonObject(species().id());
            if(armor().isEmpty()&&values.has("field_23724"))getAttribute(Attributes.ARMOR).setBaseValue(values.get("field_23724").getAsDouble());
        }
        @Override protected void customServerAiStep(ServerLevel level){
            super.customServerAiStep(level);
            if(limitedLife>0&&--limitedLife==0){discard();return;}
            if(summoner!=null&&tickCount%20==0&&level.getEntity(summoner) instanceof Undead parent&&parent.isAlive())setTarget(parent.getTarget());
            String kind=species().source();var target=getTarget();
            if(kind.equals("burned")){
                if(isInWater())variant(2);else if(isInLava()&&variant()==2)variant(getHealth()<=getMaxHealth()/2?1:0);
                if(variant()==0&&getHealth()<=getMaxHealth()/2)variant(1);
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant()==2?.1:variant()==1?.3:.2);
            }
            if(kind.equals("zap")&&tickCount%20==0){
                var leader=level.getEntitiesOfClass(Undead.class,getBoundingBox().inflate(16,4,16),e->e.species().source().equals("immortal")&&e.isAlive()).stream().min(Comparator.comparingDouble(this::distanceToSqr));
                leader.ifPresent(e->{if(getTarget()==null)getNavigation().moveTo(e,1.25);if(e.getTarget()!=null)setTarget(e.getTarget());});
            }
            if(kind.equals("scarab")&&target!=null&&target.getY()>getY()+1&&onGround())setDeltaMovement(getDeltaMovement().add(0,.4,0));
            if(cooldown>0)cooldown--;if(dashCooldown>0)dashCooldown--;
            if(target==null||!target.isAlive()||!hasLineOfSight(target)||distanceToSqr(target)>256){casting=0;return;}
            if(kind.equals("glacial_hunter")&&!wolfCalled){wolfCalled=RottenStorage.summonWolf(this,level,target);}
            if(kind.equals("dead_beard")||kind.equals("mummy")){
                if(casting>0){getNavigation().stop();level.sendParticles(ParticleTypes.WITCH,getX(),getEyeY(),getZ(),1,.25,.1,.25,0);if(--casting==0)summon(level,kind,target);}
                else if(cooldown==0){casting=kind.equals("mummy")?15:20;cooldown=kind.equals("mummy")&&variant()>0?260:340;}
            }
            if(kind.equals("immortal")&&level.isThundering()){
                if(dashCooldown==0&&distanceToSqr(target)>64){
                    var toward=target.position().subtract(position()).normalize().scale(1.3);setDeltaMovement(toward.add(0,.2,0));dashCooldown=640;heal(4);
                }
                if(cooldown==0&&getHealth()<=getMaxHealth()*.8F&&level.canSeeSky(blockPosition())){
                    lightning(level,target);cooldown=getHealth()<=getMaxHealth()*.4F?800:1600;
                }
                if(tickCount%1200==0){int count=level.getEntitiesOfClass(Undead.class,getBoundingBox().inflate(16,4,16),e->e.species().source().equals("zap")).size();if(count>0)addEffect(new MobEffectInstance(MobEffects.REGENERATION,1200,Math.min(count-1,3)));}
            }
        }
        private void summon(ServerLevel level,String kind,LivingEntity target){
            int cap=kind.equals("mummy")?5:4;
            long current=level.getEntitiesOfClass(Undead.class,getBoundingBox().inflate(20),e->getUUID().equals(e.summoner)).size();
            for(int n=(int)current;n<cap;n++){
                String id=kind.equals("mummy")?"scarab":random.nextBoolean()?"zombie_lackey":"skeleton_lackey";
                var entity=Menagerie.TYPES.get("rotten_"+id).create(level,EntitySpawnReason.MOB_SUMMONED);if(!(entity instanceof Undead child))continue;
                var at=blockPosition().offset(random.nextInt(5)-2,0,random.nextInt(5)-2);
                if(!level.hasChunkAt(at))continue;
                child.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
                if(!level.noCollision(child))continue;
                child.finalizeSpawn(level,level.getCurrentDifficultyAt(at),EntitySpawnReason.MOB_SUMMONED,null);
                child.summoner=getUUID();child.limitedLife=kind.equals("mummy")?600:500;child.setTarget(target);
                if(level.addFreshEntity(child))level.sendParticles(ParticleTypes.POOF,child.getX(),child.getY()+.4,child.getZ(),8,.3,.3,.3,.02);
            }
        }
        private void lightning(ServerLevel level,LivingEntity target){
            // Visual bolt plus a targeted strike: no fire or claim-bypassing area explosion.
            var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
            if(bolt!=null){bolt.setVisualOnly(true);bolt.setPos(target.position());level.addFreshEntity(bolt);}
            target.hurtServer(level,damageSources().mobAttack(this),5);target.addEffect(new MobEffectInstance(CHANNELLED,140),this);
        }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target){
            boolean hit=super.doHurtTarget(level,target);if(!hit||!(target instanceof LivingEntity living))return hit;
            switch(species().source()){
                case "burned" -> {if(variant()!=2)living.igniteForSeconds(3);}
                case "frostbitten" -> living.addEffect(new MobEffectInstance(FREEZE,140),this);
                case "glacial_hunter" -> living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,140),this);
                case "swampy" -> {living.addEffect(new MobEffectInstance(MobEffects.POISON,140),this);living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,140),this);}
                case "mummy" -> living.addEffect(new MobEffectInstance(MobEffects.HUNGER,140,2),this);
                case "zap","immortal" -> living.addEffect(new MobEffectInstance(CHANNELLED,140),this);
                default -> {}
            }
            return hit;
        }
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            if(species().source().equals("burned")&&variant()==2&&source.getDirectEntity() instanceof AbstractArrow)return false;
            if(species().source().equals("immortal")){
                if(source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)){heal(getMaxHealth());return false;}
                if(source.getDirectEntity() instanceof AbstractArrow)return false;
            }
            return super.hurtServer(level,source,amount);
        }
        @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){
            if(limitedLife>=0)return;super.dropCustomDeathLoot(level,source,hit);
            if(species().source().equals("undead_miner"))spawnAtLocation(level,new ItemStack(switch(variant()){case 1->Items.IRON_INGOT;case 2->Items.GOLD_INGOT;case 3->Items.DIAMOND;default->Items.COAL;}));
        }
        @Override protected boolean shouldDropLoot(ServerLevel level){return limitedLife<0&&super.shouldDropLoot(level);}
        @Override public boolean shouldDropExperience(){return limitedLife<0&&super.shouldDropExperience();}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("RottenCooldown",cooldown);out.putInt("RottenLife",limitedLife);out.putInt("RottenDashCooldown",dashCooldown);out.putBoolean("RottenWolfCalled",wolfCalled);if(summoner!=null)out.putString("RottenSummoner",summoner.toString());}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);cooldown=Math.clamp(in.getIntOr("RottenCooldown",100),0,1600);limitedLife=Math.clamp(in.getIntOr("RottenLife",-1),-1,600);dashCooldown=Math.clamp(in.getIntOr("RottenDashCooldown",100),0,640);wolfCalled=in.getBooleanOr("RottenWolfCalled",false);try{summoner=UUID.fromString(in.getStringOr("RottenSummoner",""));}catch(IllegalArgumentException e){summoner=null;}}
    }
}
