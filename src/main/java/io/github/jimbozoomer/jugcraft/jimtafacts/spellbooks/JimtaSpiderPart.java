package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Three source-sized hit regions, recreated with their parent and never saved independently. */
public final class JimtaSpiderPart extends Entity {
    public static EntityType<JimtaSpiderPart> TYPE;
    private static final EntityDataAccessor<Integer> PARENT=SynchedEntityData.defineId(JimtaSpiderPart.class,EntityDataSerializers.INT),INDEX=SynchedEntityData.defineId(JimtaSpiderPart.class,EntityDataSerializers.INT);
    private static final float[] WIDTH={1.4F,.95F,1.95F},HEIGHT={1F,.95F,1.7F};
    public JimtaSpiderPart(EntityType<?> type,Level level){super(type,level);setNoGravity(true);noPhysics=true;}
    public static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_spider_part"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaSpiderPart>of(JimtaSpiderPart::new,MobCategory.MISC).sized(1,1).noSave().noLootTable().clientTrackingRange(16).updateInterval(1).build(key));}
    public static JimtaSpiderPart create(JimtaEncounterCreatures.IceSpider spider,int index){
        var part=new JimtaSpiderPart(TYPE,spider.level());part.entityData.set(PARENT,spider.getId());part.entityData.set(INDEX,index);part.refreshDimensions();part.position(spider);((ServerLevel)spider.level()).addFreshEntity(part);return part;
    }
    public JimtaEncounterCreatures.IceSpider parent(){var e=level().getEntity(entityData.get(PARENT));return e instanceof JimtaEncounterCreatures.IceSpider spider?spider:null;}
    private int index(){return Math.clamp(entityData.get(INDEX),0,2);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(PARENT,-1);b.define(INDEX,0);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key==INDEX)refreshDimensions();}
    @Override public EntityDimensions getDimensions(Pose pose){int i=entityData==null?0:index();return EntityDimensions.scalable(WIDTH[i],HEIGHT[i]);}
    public void position(JimtaEncounterCreatures.IceSpider spider){
        double z=new double[]{1,0,-1.25}[index()];Vec3 offset=new Vec3(0,1.025*spider.crouchHeight(1),z).yRot(-spider.yBodyRot*(float)Math.PI/180);
        var normal=spider.normal;var rotated=new org.joml.Quaternionf().rotationTo(0,1,0,(float)normal.x,(float)normal.y,(float)normal.z).transform(new org.joml.Vector3f((float)offset.x,(float)offset.y,(float)offset.z));
        setPos(spider.position().add(rotated.x,rotated.y,rotated.z));setDeltaMovement(Vec3.ZERO);
    }
    @Override public void tick(){super.tick();var spider=parent();if(spider==null||!spider.isAlive()){if(!level().isClientSide())discard();return;}position(spider);}
    @Override public boolean isPickable(){var spider=parent();return spider!=null&&spider.isAlive();}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        var spider=parent();if(spider==null||!spider.isAlive()||source.getEntity()==spider)return false;
        if(source.getEntity() instanceof ServerPlayer p&&!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,level,spider))return false;
        return spider.hurtServer(level,source,amount);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){}
    @Override protected void addAdditionalSaveData(ValueOutput out){}
}
