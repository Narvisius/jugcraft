package io.github.jimbozoomer.jugcraft.jimtafacts;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.storage.*;

/** Treasure ambusher retaining the supplied 60-health chest, hop/close cycle and projectile-resistant closed shell. */
public final class JimtaMimic extends Monster {
    public static final EntityType<JimtaMimic> TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,Jugcraft.id("jimta_mimic"),
            EntityType.Builder.of(JimtaMimic::new,MobCategory.MONSTER).sized(.875F,.875F).eyeHeight(.6F).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimta_mimic"))));
    public static final SoundEvent HURT=sound("mimic_hurt"),DEATH=sound("mimic_death"),FART=sound("fart");
    private static SoundEvent sound(String name){return Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id("jimtafacts/"+name),SoundEvent.createVariableRangeEvent(Jugcraft.id("jimtafacts/"+name)));}
    private boolean dormant=true;
    private int hopDelay=15,attackDelay;
    public int ticksInAir;
    public JimtaMimic(EntityType<? extends JimtaMimic> type,Level level){super(type,level);xpReward=10;}
    public static void register(){
        FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,60)
                .add(Attributes.FOLLOW_RANGE,16).add(Attributes.KNOCKBACK_RESISTANCE,.8).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,5));
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("jimta_campsite"),JimtaCampsite.CODEC);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.UNDERGROUND_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("jimtafacts/campsite")));
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1,false){@Override public boolean canUse(){return !dormant&&super.canUse();}});
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,8));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,(p,l)->!dormant||distanceToSqr(p)<9));
    }
    @Override public void setTarget(LivingEntity target){if(target!=null)dormant=false;super.setTarget(target);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void tick(){
        super.tick();
        if(onGround()){if(ticksInAir>0)playSound(SoundEvents.CHEST_CLOSE,.6F,.8F);ticksInAir=0;}else ticksInAir++;
        if(!level().isClientSide()){
            if(attackDelay>0)attackDelay--;
            if(dormant){getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);}
            else if(onGround()&&getTarget()!=null){
                if(--hopDelay<=0){var direction=getTarget().position().subtract(position()).multiply(1,0,1).normalize();
                    setDeltaMovement(direction.x*.34,.42,direction.z*.34);syncVelocity=true;hopDelay=12+random.nextInt(9);playSound(SoundEvents.CHEST_OPEN,.6F,.8F);}
                else{var motion=getDeltaMovement();setDeltaMovement(0,motion.y,0);}
            }
        }
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!player.isCreative()&&!player.isSpectator()&&!level().isClientSide()){setTarget(player);hopDelay=0;}
        return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(source.getEntity() instanceof Player p)setTarget(p);
        if(onGround()&&source.is(DamageTypeTags.IS_PROJECTILE)&&!source.is(DamageTypeTags.BYPASSES_ARMOR)){playSound(HURT,1,1);hopDelay=0;return false;}
        return super.hurtServer(level,source,amount);
    }
    @Override protected SoundEvent getHurtSound(DamageSource source){return HURT;}
    @Override protected SoundEvent getDeathSound(){return DEATH;}
    @Override public void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("JimtaDormant",dormant);}
    @Override public void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);dormant=in.getBooleanOr("JimtaDormant",true);}
}
