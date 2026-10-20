package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Ephemeral server-owned multipart hitboxes. Parent alone owns health, loot and persistence. */
public final class RecoveredCreaturePart extends Entity {
    private static final Set<String> MULTIPART=Set.of("beast_anaconda","beast_centipede_head","beast_bone_serpent","beast_void_worm","cavern_gum_worm","beast_murmur");
    public static EntityType<RecoveredCreaturePart> TYPE;
    private static final EntityDataAccessor<Integer> PARENT=SynchedEntityData.defineId(RecoveredCreaturePart.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> MODEL=SynchedEntityData.defineId(RecoveredCreaturePart.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN=SynchedEntityData.defineId(RecoveredCreaturePart.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> INDEX=SynchedEntityData.defineId(RecoveredCreaturePart.class,EntityDataSerializers.INT);
    private MenagerieAnimal parent;
    public RecoveredCreaturePart(EntityType<? extends RecoveredCreaturePart> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("recovered_creature_part"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<RecoveredCreaturePart>of(RecoveredCreaturePart::new,MobCategory.MISC).sized(.9F,.8F).clientTrackingRange(12).updateInterval(1).noSave().build(key));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(PARENT,-1);b.define(MODEL,"");b.define(SKIN,"");b.define(INDEX,0);}
    public int segmentIndex(){return entityData.get(INDEX);}
    public MenagerieAnimal parent(){return level().getEntity(entityData.get(PARENT)) instanceof MenagerieAnimal a?a:null;}
    public String model(){return entityData.get(MODEL);}
    public String skin(){return entityData.get(SKIN);}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(parent==null||!parent.isAlive()||!Float.isFinite(amount)||amount<=0)return false;
        if(source.getEntity()==parent||source.getEntity() instanceof RecoveredCreaturePart)return false;
        if(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(player,level,parent))return false;
        return parent.hurtServer(level,source,amount);
    }
    @Override public void tick(){super.tick();if(!level().isClientSide()&&(parent==null||!parent.isAlive()||parent.isRemoved()||parent.level()!=level()))discard();}
    @Override protected void addAdditionalSaveData(ValueOutput out){}
    @Override protected void readAdditionalSaveData(ValueInput in){}

    record Segment(String model,String texture,double spacing,double elevation){}
    private static List<Segment> segments(MenagerieAnimal animal){
        var parts=new ArrayList<Segment>();String n=animal.species().id();
        switch(n){
            case "beast_anaconda" -> {for(int i=0;i<7;i++)parts.add(new Segment("beasts_modelanaconda_"+(i==0?"neck":i==6?"tail":"body"),animal.texture(),.85,0));}
            case "beast_centipede_head" -> {for(int i=0;i<5;i++)parts.add(new Segment("beasts_modelcavecentipede_"+(i==4?"2":"1"),animal.texture(),.8,0));}
            case "beast_bone_serpent" -> {for(int i=0;i<7;i++)parts.add(new Segment("beasts_modelboneserpent"+(i==6?"tail":"body"),"menagerie/beast/entity/bone_serpent_"+(i==6?"tail":"mid"),.9,0));}
            case "beast_void_worm" -> {for(int i=0;i<10;i++)parts.add(new Segment("beasts_modelvoidworm"+(i>6?"tail":"body"),"menagerie/beast/entity/void_worm/void_worm_"+(i>6?"tail":"body"),1.3,0));}
            case "cavern_gum_worm" -> {for(int i=0;i<8;i++)parts.add(new Segment("caves_gumwormsegmentmodel","menagerie/cavern/entity/gum_worm_segment_"+(i%3),1.4,0));}
            case "beast_murmur" -> {parts.add(new Segment("beasts_modelmurmurneck",animal.texture(),0,1.75));parts.add(new Segment("beasts_modelmurmurhead",animal.texture(),0,2));}
        }
        return parts;
    }
    /** Called once per parent tick; no world scan, chunk forcing, separate loot or recursive children. */
    public static void update(MenagerieAnimal animal,List<RecoveredCreaturePart> parts){
        if(!(animal.level() instanceof ServerLevel level))return;
        if(!MULTIPART.contains(animal.species().id()))return;
        if(!animal.isAlive()){parts.forEach(Entity::discard);parts.clear();return;}
        var profile=segments(animal);if(profile.isEmpty())return;double ageScale=animal.isBaby()?.5:1;
        if(parts.size()!=profile.size()||parts.stream().anyMatch(Entity::isRemoved)){
            parts.forEach(Entity::discard);parts.clear();
            Vec3 anchor=animal.position();
            for(var segment:profile){
                var part=TYPE.create(level,EntitySpawnReason.TRIGGERED);if(part==null)continue;
                part.parent=animal;part.entityData.set(PARENT,animal.getId());part.entityData.set(MODEL,segment.model());part.entityData.set(SKIN,segment.texture());
                part.entityData.set(INDEX,parts.size());
                anchor=anchor.add(Vec3.directionFromRotation(0,animal.getYRot()).scale(-segment.spacing()*ageScale));
                part.snapTo(anchor.x,anchor.y+segment.elevation()*ageScale,anchor.z,animal.getYRot(),0);
                if(level.addFreshEntity(part))parts.add(part);
            }
        }
        Vec3 anchor=animal.position();
        for(int i=0;i<parts.size();i++){
            var part=parts.get(i);var spec=profile.get(i);part.entityData.set(SKIN,spec.texture());
            var offset=part.position().subtract(anchor);double horizontal=offset.horizontalDistance();
            Vec3 direction=horizontal<.001||horizontal>16?Vec3.directionFromRotation(0,animal.getYRot()).scale(-1):new Vec3(offset.x/horizontal,0,offset.z/horizontal);
            var target=anchor.add(direction.scale(spec.spacing()*ageScale)).add(0,spec.elevation()*ageScale,0);
            if(!level.hasChunkAt(BlockPos.containing(target)))target=anchor;
            part.setPos(target);part.setYRot((float)Math.toDegrees(Math.atan2(direction.x,-direction.z)));
            if(!animal.species().id().equals("beast_murmur"))anchor=target;
        }
    }
}
