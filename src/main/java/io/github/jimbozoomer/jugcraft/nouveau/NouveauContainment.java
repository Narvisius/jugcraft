package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.level.storage.*;

/** A jar saves the actual entity once; no loot roll, respawn copy or inventory snapshot is invented. */
public final class NouveauContainment {
    public static DataComponentType<CustomData> CAPTURE;
    private static final Set<Entity> MOVING=Collections.newSetFromMap(new IdentityHashMap<>());
    public static boolean capturing(Entity entity){return MOVING.contains(entity);}
    private NouveauContainment(){}
    public static void register(){CAPTURE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_captured_entity"),DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CustomData.CODEC)).build());}
    public static boolean capture(ServerLevel l,ServerPlayer p,NouveauWorkshop jar,Mob mob){
        if(!jar.kind().equals("mob_jar")||jar.captured()!=null||!jar.permitted(p)||!mob.isAlive()||mob.isPassenger()||mob.isVehicle()
            ||mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(jar.getBlockPos()))>16||!Authority.mayChange(l,p,mob.blockPosition()))return false;
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if(!id.startsWith("minecraft:")&&!id.startsWith("jugcraft:nouveau_")&&!id.equals("jugcraft:starbuncle")||mob instanceof NouveauBeast beast&&beast.boss()||Set.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden","minecraft:elder_guardian","minecraft:villager","minecraft:wandering_trader").contains(id))return false;
        if(NouveauSummons.owned(mob)||mob instanceof NouveauAnimatedBlock)return false;
        if(mob instanceof Starbuncle star&&!star.mayServe(p)||mob instanceof NouveauHelper helper&&!p.getUUID().equals(helper.owner()))return false;
        if(mob instanceof TamableAnimal pet&&pet.isTame()&&pet.getOwner()!=p)return false;
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());if(!mob.save(output))return false;
        var tag=output.buildResult();if(tag.toString().length()>131072)return false;
        jar.captured(CustomData.of(tag));if(mob instanceof Starbuncle)NouveauAdvancements.award(p,"shrunk_starbuncle");if(mob instanceof io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity<?> worker&&worker.owner()!=null)io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster.of(l.getServer()).remove(worker.owner(),worker.getUUID());MOVING.add(mob);try{mob.discard();}finally{MOVING.remove(mob);}return true;
    }
    public static boolean lightning(ServerLevel l,ServerPlayer p,NouveauWorkshop jar){
        if(jar.captured()!=null||!jar.permitted(p))return false;
        for(var bolt:l.getEntitiesOfClass(LightningBolt.class,new net.minecraft.world.phys.AABB(jar.getBlockPos()).inflate(3),Entity::isAlive).stream().limit(1).toList()){
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());if(!bolt.save(out))return false;jar.captured(CustomData.of(out.buildResult()));bolt.discard();NouveauAdvancements.award(p,"catch_lightning");return true;
        }return false;
    }
    public static boolean release(ServerLevel l,ServerPlayer p,NouveauWorkshop jar){
        if(jar.captured()==null||!jar.permitted(p))return false;
        var input=TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),jar.captured().copyTag());
        var entity=EntityType.loadEntityRecursive(input,l,EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
        if(!(entity instanceof Mob)&&!(entity instanceof LightningBolt)||l.getEntity(entity.getUUID())!=null)return false;if(entity instanceof LightningBolt bolt)bolt.setVisualOnly(true);
        if(entity instanceof io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity<?> worker&&worker.owner()!=null){
            var roster=io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster.of(l.getServer()).workers(worker.owner());
            if(roster.size()>=io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster.MAX_PER_PLAYER)return false;
            String kind=entity instanceof Starbuncle?"starbuncle":null;
            if(kind!=null&&roster.values().stream().filter(e->e.kind().equals(kind)).count()>=8)return false;
        }
        var at=jar.getBlockPos().above();entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,entity.getYRot(),entity.getXRot());
        if(!Authority.mayChange(l,p,at)||!l.noCollision(entity)||!l.addFreshEntity(entity))return false;
        jar.captured(null);return true;
    }
    public static Mob specimen(ServerLevel l,NouveauWorkshop jar){
        if(jar.captured()==null)return null;var input=TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),jar.captured().copyTag());var e=EntityType.loadEntityRecursive(input,l,EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
        if(!(e instanceof Mob mob))return null;mob.snapTo(jar.getBlockPos().getX()+.5,jar.getBlockPos().getY(),jar.getBlockPos().getZ()+.5,0,0);return mob;
    }
    private static void save(ServerLevel l,NouveauWorkshop jar,Mob mob){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());if(mob.save(out))jar.captured(CustomData.of(out.buildResult()));}
    public static InteractionResult interact(ServerPlayer p,NouveauWorkshop jar,InteractionHand hand){
        if(!jar.kind().equals("mob_jar"))return InteractionResult.PASS;
        if(!jar.permitted(p))return InteractionResult.FAIL;if(jar.captured()==null&&jar.getItem(0).isEmpty()&&p.getItemInHand(hand).is(Items.CLOCK)){jar.setItem(0,p.getItemInHand(hand).split(1));NouveauAdvancements.award(p,"time_in_a_bottle");return InteractionResult.SUCCESS;}var l=p.level();var mob=specimen(l,jar);if(mob==null)return InteractionResult.PASS;var held=p.getItemInHand(hand);ItemStack output=ItemStack.EMPTY;
        if(held.is(Items.BUCKET)&&mob.getType()==net.minecraft.world.entity.EntityTypes.COW)output=new ItemStack(Items.MILK_BUCKET);
        if(held.is(Items.BOWL)&&mob.getType()==net.minecraft.world.entity.EntityTypes.MOOSHROOM)output=new ItemStack(Items.MUSHROOM_STEW);
        if(held.is(Items.SHEARS)&&mob instanceof Shearable sheep&&sheep.readyForShearing()){sheep.shear(l,net.minecraft.sounds.SoundSource.PLAYERS,held);held.hurtAndBreak(1,p,hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);save(l,jar,mob);return InteractionResult.SUCCESS;}
        if(!output.isEmpty()){held.consume(1,p);p.getInventory().placeItemBackInInventory(output,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;}
        return InteractionResult.PASS;
    }
    public static void tick(ServerLevel l,ServerPlayer p,NouveauWorkshop jar){
        if(!jar.every(300)||jar.captured()==null)return;var mob=specimen(l,jar);if(mob==null)return;
        if(mob.getType()==net.minecraft.world.entity.EntityTypes.CHICKEN&&!mob.isBaby())jar.output(new ItemStack(Items.EGG));
        if(mob instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep&&sheep.isSheared()&&l.getBlockState(jar.getBlockPos().below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)){sheep.setSheared(false);save(l,jar,sheep);}
    }
}
