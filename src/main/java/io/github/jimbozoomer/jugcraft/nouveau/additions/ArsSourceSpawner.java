package io.github.jimbozoomer.jugcraft.nouveau.additions;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

/** Loaded specimen jars supply species, never inventories, identities or ownership. */
public final class ArsSourceSpawner {
    private static final Set<NouveauWorkshop> JARS=Collections.newSetFromMap(new WeakHashMap<>());
    private static final TagKey<EntityType<?>> DENIED=TagKey.create(Registries.ENTITY_TYPE,io.github.jimbozoomer.jugcraft.Jugcraft.id("ars_additions_source_spawner_denylist"));
    private static final TagKey<EntityType<?>> BOSSES=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("c:bosses"));
    private ArsSourceSpawner(){}
    public static void observe(NouveauWorkshop jar){if(jar.kind().equals("mob_jar"))JARS.add(jar);}
    public static void tick(ServerLevel level,ServerPlayer actor,NouveauWorkshop spawner){
        spawner.setActive(false);
        if(level.hasNeighborSignal(spawner.getBlockPos())||!level.hasNearbyAlivePlayer(spawner.getBlockPos().getX()+.5,spawner.getBlockPos().getY()+.5,spawner.getBlockPos().getZ()+.5,16))return;
        if(spawner.advanceSourceSpawnDelay(20)>0)return;
        spawner.sourceSpawnDelay(200+level.getRandom().nextInt(601));
        var candidates=JARS.stream().filter(j->j.getLevel()==level&&!j.isRemoved()&&j.captured()!=null&&level.hasChunkAt(j.getBlockPos())&&j.permitted(actor)
            &&Math.abs(j.getBlockPos().getX()-spawner.getBlockPos().getX())<=10&&Math.abs(j.getBlockPos().getY()-spawner.getBlockPos().getY())<=10&&Math.abs(j.getBlockPos().getZ()-spawner.getBlockPos().getZ())<=10)
            .sorted(Comparator.comparingLong(j->j.getBlockPos().asLong())).limit(64).toList();
        if(candidates.isEmpty()){spawner.status("Place a captured creature jar within ten blocks");return;}
        int made=0;
        // Bounded attempts also terminate when every specimen is denied, obstructed or too crowded.
        for(int attempt=0;attempt<16&&made<4;attempt++){
            var jar=candidates.get(level.getRandom().nextInt(candidates.size()));var source=jar.captured().copyTag();
            var id=Identifier.tryParse(source.getStringOr("id",""));if(id==null)continue;var type=BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if(type==null||type.builtInRegistryHolder().is(DENIED)||type.builtInRegistryHolder().is(BOSSES)||Set.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden","minecraft:elder_guardian","minecraft:villager","minecraft:wandering_trader").contains(id.toString()))continue;
            if(level.getDifficulty()==Difficulty.PEACEFUL&&!type.getCategory().isFriendly())continue;
            if(level.getEntitiesOfClass(Mob.class,new AABB(spawner.getBlockPos()).inflate(4),m->m.getType()==type).size()>=6)continue;
            var entity=type.create(level,EntitySpawnReason.SPAWNER);
            if(!(entity instanceof Mob mob)||mob instanceof io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity<?>||mob instanceof NouveauBeast beast&&beast.boss())continue;
            // Retain harmless appearance variants, not cargo, equipment, effects, attributes or marked identities.
            var appearance=new net.minecraft.nbt.CompoundTag();
            for(String key:List.of("Variant","variant","Color","color","Sheared","Type","type","Size","size"))if(source.contains(key))appearance.put(key,source.get(key).copy());
            mob.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),appearance));
            var pos=spawner.getBlockPos().offset(level.getRandom().nextInt(9)-4,level.getRandom().nextInt(3)-1,level.getRandom().nextInt(9)-4);
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!Authority.mayChange(level,actor,pos))continue;
            mob.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,level.getRandom().nextFloat()*360,0);
            if(!level.noCollision(mob)||!mob.checkSpawnObstruction(level))continue;
            long cost=(long)Math.ceil((200+(type.getCategory().isPersistent()?1000:0)+mob.getMaxHealth()*50)/100.0);
            if(!NouveauSource.canPay(level,actor,spawner,cost)){spawner.status("Needs Ley Charge for spawning");break;}
            mob.addTag("jugcraft:source_spawner");
            if(!level.addFreshEntity(mob))continue;
            if(!NouveauSource.pay(level,actor,spawner,cost)){mob.discard();continue;}
            made++;level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,mob.getX(),mob.getY()+.5,mob.getZ(),8,.3,.4,.3,.01);
        }
        if(made>0){spawner.setActive(true);spawner.status("Spawned "+made+" creatures");}
    }
}
