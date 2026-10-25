package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.Level;

/** Three supplied weapon meshes, distinct durability/speed/damage and original swing clips. */
public final class JimtaSummonedWeapon extends PathfinderMob implements GeoEntity {
    public static final Map<String,EntityType<JimtaSummonedWeapon>> TYPES=new LinkedHashMap<>();
    private static final EntityDataAccessor<Integer> STRIKE=SynchedEntityData.defineId(JimtaSummonedWeapon.class,EntityDataSerializers.INT);
    private final String kind;private int shownStrike=-1;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public JimtaSummonedWeapon(EntityType<? extends PathfinderMob> type,Level level,String kind){super(type,level);this.kind=kind;setNoGravity(true);moveControl=new FlyingMoveControl(this,30,true);}
    public String kind(){return kind;}
    public int attackInterval(){return kind.equals("rapier")?10:kind.equals("claymore")?30:20;}
    public void animateStrike(){entityData.set(STRIKE,entityData.get(STRIKE)+1);}
    @Override protected PathNavigation createNavigation(Level level){var navigation=new FlyingPathNavigation(this,level);navigation.setCanFloat(true);return navigation;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(STRIKE,0);}
    public static void register(){
        for(String kind:List.of("sword","rapier","claymore")){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_summoned_"+kind));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaSummonedWeapon>of((t,l)->new JimtaSummonedWeapon(t,l,kind),MobCategory.MISC).sized(.6F,1.3F).clientTrackingRange(12).updateInterval(1).build(key));TYPES.put(kind,type);
            FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,kind.equals("claymore")?40:kind.equals("rapier")?15:25).add(Attributes.ATTACK_DAMAGE,kind.equals("claymore")?8:kind.equals("rapier")?3:5).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FLYING_SPEED,kind.equals("rapier")?2.2:kind.equals("sword")?1.5:1));
        }
    }
    public static boolean summon(ServerPlayer owner){boolean spawned=false;int i=0;for(var type:TYPES.values()){var weapon=type.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);if(weapon==null)continue;weapon.snapTo(owner.getX()+Math.cos(i*2.1),owner.getY()+1.2,owner.getZ()+Math.sin(i++*2.1),owner.getYRot(),0);spawned|=NouveauSummons.adoptSpellbook(owner,weapon,"summon_swords",12000);}return spawned;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<>("weapon",2,state->{int strike=entityData.get(STRIKE);if(strike!=shownStrike){shownStrike=strike;if(strike>0){state.controller().reset();return state.setAndContinue(RawAnimation.begin().thenPlay(kind.equals("rapier")?"summoned_sword_multistab":kind.equals("claymore")?"summoned_sword_basic_downswing":"summoned_sword_basic_swing"));}}return PlayState.CONTINUE;}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
