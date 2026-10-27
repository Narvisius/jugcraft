package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Explicit resident migration: real UUID and saved state, no loot, respawn rolls or copied spawn eggs. */
final class SpatialResidents {
    private static final Set<String> TYPES=Set.of("minecraft:cow","minecraft:pig","minecraft:sheep","minecraft:chicken","minecraft:rabbit","minecraft:goat","minecraft:mooshroom","minecraft:cat","minecraft:wolf","minecraft:parrot","minecraft:fox");
    record Entry(CompoundTag data,double x,double y,double z){
        static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(CompoundTag.CODEC.fieldOf("data").forGetter(Entry::data),Codec.DOUBLE.fieldOf("x").forGetter(Entry::x),Codec.DOUBLE.fieldOf("y").forGetter(Entry::y),Codec.DOUBLE.fieldOf("z").forGetter(Entry::z)).apply(i,Entry::new));
    }
    static final Codec<List<Entry>> CODEC=Entry.CODEC.listOf(0,24);
    private SpatialResidents(){}
    private static boolean supported(Entity entity){return entity instanceof Mob&&entity.isAlive()&&!entity.isPassenger()&&!entity.isVehicle()&&!(entity instanceof Leashable leash&&leash.isLeashed())&&TYPES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());}
    static List<Entry> capture(ServerLevel level,ServerPlayer player,AABB bounds,BlockPos origin){
        var result=new ArrayList<Entry>();
        for(var entity:level.getEntities((Entity)null,bounds.inflate(.01),e->true)){
            if(result.size()>=24||!supported(entity)||!bounds.contains(entity.getBoundingBox().minX,entity.getBoundingBox().minY,entity.getBoundingBox().minZ)||!bounds.contains(entity.getBoundingBox().maxX,entity.getBoundingBox().maxY,entity.getBoundingBox().maxZ))throw new IllegalArgumentException("Spatial residents: at most 24 fully enclosed, unmounted, unleashed farm animals or pets; move players and other entities outside");
            if(entity instanceof TamableAnimal pet&&pet.isTame()&&pet.getOwner()!=player)throw new IllegalArgumentException("Only the pet's owner may capture it");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());if(!entity.save(out))throw new IllegalArgumentException("Resident cannot be saved");
            var data=out.buildResult();if(data.toString().length()>65536)throw new IllegalArgumentException("Resident data exceeds spatial budget");
            result.add(new Entry(data,entity.getX()-origin.getX(),entity.getY()-origin.getY(),entity.getZ()-origin.getZ()));
        }return List.copyOf(result);
    }
    static List<Entity> prepare(ServerLevel level,List<Entry> entries,BlockPos origin,AABB bounds){
        var result=new ArrayList<Entity>();var ids=new HashSet<UUID>();
        for(var entry:entries){
            if(!Double.isFinite(entry.x)||!Double.isFinite(entry.y)||!Double.isFinite(entry.z)||entry.data.toString().length()>65536)throw new IllegalArgumentException("Invalid spatial resident payload");
            var input=TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),entry.data.copy());
            var entity=EntityType.loadEntityRecursive(input,level,EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
            if(entity==null||!supported(entity)||!ids.add(entity.getUUID()))throw new IllegalArgumentException("Unsupported or duplicate resident identity");
            entity.snapTo(origin.getX()+entry.x,origin.getY()+entry.y,origin.getZ()+entry.z,entity.getYRot(),entity.getXRot());entity.setDeltaMovement(Vec3.ZERO);
            var box=entity.getBoundingBox();if(!bounds.contains(box.minX,box.minY,box.minZ)||!bounds.contains(box.maxX,box.maxY,box.maxZ))throw new IllegalArgumentException("Resident does not fit its destination region");
            result.add(entity);
        }return result;
    }
    static void checkAbsent(ServerLevel level,List<Entity> incoming){
        for(var entity:incoming)for(var world:level.getServer().getAllLevels())if(world.getEntity(entity.getUUID())!=null)throw new IllegalArgumentException("Resident UUID already exists in a loaded world; transfer refused");
    }
    static void removeRecorded(ServerLevel level,List<Entity> recorded,AABB bounds){
        for(var saved:recorded)for(var world:level.getServer().getAllLevels()){
            var current=world.getEntity(saved.getUUID());if(current==null)continue;
            if(world!=level||!bounds.inflate(.01).contains(current.position())||current.getType()!=saved.getType())throw new IllegalArgumentException("Resident identity moved outside the recovery region; preserve journal");
            current.discard();
        }
    }
    static void install(ServerLevel level,List<Entity> residents){
        checkAbsent(level,residents);
        for(var entity:residents)if(!level.noCollision(entity)||!level.addFreshEntity(entity))throw new IllegalArgumentException("Resident destination is obstructed or identity could not be restored");
    }
}
