package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Source-shaped Gaia ritual and phased fight; no arena terrain is destroyed or regenerated. */
public final class BotanicalGaia extends Monster {
    public static EntityType<BotanicalGaia> TYPE;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(BotanicalGaia.class,EntityDataSerializers.INT);
    private final Set<UUID> participants=new LinkedHashSet<>(),minions=new LinkedHashSet<>();
    private final Map<BlockPos,Integer> mines=new LinkedHashMap<>();
    private BlockPos center;private UUID summoner;private boolean hard,rewarded;private int age,phaseTime,attackTimer,emptyTicks;
    private final ServerBossEvent bar=new ServerBossEvent(java.util.UUID.randomUUID(),Component.literal("Guardian of Gaia"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    public BotanicalGaia(EntityType<? extends BotanicalGaia> type,Level level){super(type,level);setPersistenceRequired();setNoGravity(true);}
    static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_gaia_guardian"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalGaia>of(BotanicalGaia::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(12).build(key));
        FabricDefaultAttributeRegistry.register(TYPE,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,320).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,32));
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim,source,amount)->{
            if(source.getEntity() instanceof LivingEntity attacker&&minion(attacker)){
                var boss=encounter(attacker);return boss!=null&&victim instanceof ServerPlayer p&&boss.accepts(p);
            }return true;
        });
        UseBlockCallback.EVENT.register((user,world,hand,hit)->{
            var offer=user.getItemInHand(hand);if(!world.getBlockState(hit.getBlockPos()).is(Blocks.BEACON)||!offer.is(Jugtania.item("terrasteel_ingot"))&&!offer.is(Jugtania.item("gaia_ingot")))return InteractionResult.PASS;
            if(user instanceof ServerPlayer p){String failure=summon(p,hit.getBlockPos(),offer);if(failure!=null){p.sendOverlayMessage(Component.literal(failure));return InteractionResult.FAIL;}}return InteractionResult.SUCCESS;
        });
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PHASE,0);}
    @Override protected void registerGoals(){}
    public int phase(){return entityData.get(PHASE);}
    private void phase(int value){entityData.set(PHASE,value);phaseTime=0;}
    private static String arena(ServerPlayer p,BlockPos beacon){
        var level=p.level();
        for(int x:new int[]{-4,4})for(int z:new int[]{-4,4}){var at=beacon.offset(x,1,z);if(!level.hasChunkAt(at)||!level.getBlockState(at).is(Jugtania.block("gaia_pylon")))return "Place four Gaia Pylons at offsets ±4 X, ±4 Z, one block above the beacon.";}
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            if(x*x+z*z>144)continue;var floor=beacon.offset(x,0,z);
            if(!level.hasChunkAt(floor)||!level.getWorldBorder().isWithinBounds(floor)||!Authority.mayChange(level,p,floor))return "The complete 12-block-radius arena must be loaded and yours to use.";
            if(!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)&&!level.getBlockState(floor.below()).isFaceSturdy(level,floor.below(),Direction.UP))return "The arena needs a continuous solid floor within one block of beacon height.";
            for(int y=1;y<=5;y++){var at=floor.above(y);if(!Authority.mayChange(level,p,at))return "The arena crosses protected land.";if(Math.abs(x)==4&&Math.abs(z)==4&&y<=2)continue;if(!level.getBlockState(at).getCollisionShape(level,at).isEmpty())return "Clear five blocks of headroom across the arena.";}
        }return null;
    }
    private static String summon(ServerPlayer p,BlockPos beacon,ItemStack offer){
        if(!p.isAlive()||p.isSpectator()||p.level().getDifficulty()==Difficulty.PEACEFUL)return "The Gaia ritual needs a non-Peaceful world.";
        if(!p.level().getEntitiesOfClass(BotanicalGaia.class,new AABB(beacon).inflate(48),Entity::isAlive).isEmpty())return "Another Gaia ritual is already nearby.";
        var error=arena(p,beacon);if(error!=null)return error;
        var boss=new BotanicalGaia(TYPE,p.level());boss.center=beacon.above();boss.summoner=p.getUUID();boss.hard=offer.is(Jugtania.item("gaia_ingot"));boss.participants.add(p.getUUID());
        for(var candidate:p.level().players())if(candidate!=p&&candidate.isAlive()&&!candidate.isSpectator()&&!candidate.isCreative()&&candidate.distanceToSqr(Vec3.atCenterOf(boss.center))<=144&&boss.participants.size()<16)boss.participants.add(candidate.getUUID());
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(320*(boss.participants.size()>1?1+boss.participants.size()*.25:1));if(boss.hard)boss.getAttribute(Attributes.ARMOR).setBaseValue(15);
        boss.setHealth(1);boss.setPos(Vec3.atCenterOf(boss.center));if(!p.level().addFreshEntity(boss))return "The ritual could not begin.";
        if(!p.hasInfiniteMaterials())offer.shrink(1);for(var id:boss.participants){var member=p.level().getServer().getPlayerList().getPlayer(id);if(member!=null)member.sendSystemMessage(Component.literal("Gaia ritual begun. Stay within the beacon arena; endure the summoned waves to earn rewards."));}return null;
    }
    private List<ServerPlayer> active(ServerLevel level){var list=new ArrayList<ServerPlayer>();for(var id:participants){var p=level.getServer().getPlayerList().getPlayer(id);if(p!=null&&p.level()==level&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(Vec3.atCenterOf(center))<=18*18)list.add(p);}return list;}
    private boolean allowed(ServerLevel level,BlockPos at){var owner=level.getServer().getPlayerList().getPlayer(summoner);return owner!=null&&level.hasChunkAt(at)&&level.getWorldBorder().isWithinBounds(at)&&Authority.mayChange(level,owner,at);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level)||!isAlive())return;if(center==null||summoner==null){discard();return;}age++;phaseTime++;setDeltaMovement(Vec3.ZERO);
        var players=active(level);bar.setProgress(Math.clamp(getHealth()/getMaxHealth(),0,1));for(var p:List.copyOf(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);for(var p:players)bar.addPlayer(p);
        if(players.isEmpty()){if(++emptyTicks>=200)cancel();return;}emptyTicks=0;
        if(age>=36000||!allowed(level,center)||!level.getBlockState(center.below()).is(Blocks.BEACON)){cancel();return;}
        for(int x:new int[]{-4,4})for(int z:new int[]{-4,4})if(!level.hasChunkAt(center.offset(x,0,z))||!level.getBlockState(center.offset(x,0,z)).is(Jugtania.block("gaia_pylon"))){cancel();return;}
        if(phase()==0){setHealth(Math.min(getMaxHealth(),getMaxHealth()*phaseTime/160F));if(phaseTime>=160)phase(1);return;}
        tickMines(level,players);
        // Hard-mode volleys use the supplied four-of-fifteen-tick cadence. Each
        // missile locks its destination on launch, so a player can dodge it.
        if(hard&&age%15<4){
            var target=players.get(random.nextInt(players.size()));
            if(distanceToSqr(target)<=144&&BotanicalRodEffect.gaiaMissile(this,target))
                level.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Jugcraft.id("jugtania_missile")),net.minecraft.sounds.SoundSource.HOSTILE,1,.8F+random.nextFloat()*.2F);
        }
        if(phase()==2){
            if(phaseTime%80==0&&phaseTime<=800)wave(level,players);
            if(phaseTime>=900){cleanupMinions(level);phase(3);teleport(level,players.getFirst());}return;
        }
        if(phase()==1&&getHealth()<=getMaxHealth()*.2F){phase(2);setPos(Vec3.atCenterOf(center).add(0,2,0));return;}
        if(++attackTimer>=(hard?phase()==3?35:45:phase()==3?40:60)){
            attackTimer=0;var target=players.get(random.nextInt(players.size()));teleport(level,target);
            for(int i=0;i<(hard&&phase()==3?7:6)&&mines.size()<48;i++){
                var at=center.offset(random.nextInt(21)-10,0,random.nextInt(21)-10);
                if(allowed(level,at)&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))mines.putIfAbsent(at,age+40);
            }
            int count=Math.min(8,players.size()*(hard?2:1));for(int i=0;i<count;i++)BotanicalPixie.gaia(this,players.get(i%players.size()));
        }
        if(attackTimer==12){for(var p:players)if(distanceToSqr(p)<16&&allowed(level,p.blockPosition())&&hasLineOfSight(p))p.hurtServer(level,damageSources().mobAttack(this),hard?10:8);}
    }
    boolean accepts(ServerPlayer p){return isAlive()&&center!=null&&phase()!=0&&p.level()==level()&&p.isAlive()&&!p.isSpectator()&&participants.contains(p.getUUID())&&p.distanceToSqr(Vec3.atCenterOf(center))<=324&&allowed(p.level(),p.blockPosition());}
    private void tickMines(ServerLevel level,List<ServerPlayer> players){
        for(var entry:new ArrayList<>(mines.entrySet())){
            var at=entry.getKey();if(!allowed(level,at)){mines.remove(at);continue;}
            if(age<entry.getValue()){
                if(age%4==0)for(int i=0;i<12;i++){double angle=Math.PI*2*i/12;level.sendParticles(ParticleTypes.WITCH,at.getX()+.5+Math.cos(angle)*1.25,at.getY()+.12,at.getZ()+.5+Math.sin(angle)*1.25,1,0,.02,0,0);}
            }else{
                level.sendParticles(ParticleTypes.EXPLOSION,at.getX()+.5,at.getY()+.5,at.getZ()+.5,1,0,0,0,0);
                for(var p:players)if(accepts(p)&&p.distanceToSqr(Vec3.atCenterOf(at))<6.25&&hasLineOfSight(p)){
                    p.hurtServer(level,damageSources().mobAttack(this),hard?10:8);
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,80),this);
                }mines.remove(at);
            }
        }
    }
    public static boolean minion(LivingEntity entity){return entity.entityTags().contains("jugcraft_gaia_minion");}
    private static BotanicalGaia encounter(LivingEntity entity){
        if(!(entity.level() instanceof ServerLevel level))return null;
        for(var tag:entity.entityTags())if(tag.startsWith("jugcraft_gaia:"))try{var boss=level.getEntity(UUID.fromString(tag.substring("jugcraft_gaia:".length())));return boss instanceof BotanicalGaia gaia&&gaia.isAlive()?gaia:null;}catch(IllegalArgumentException ignored){return null;}return null;
    }
    public static boolean allowsEffect(LivingEntity victim,Entity source){
        if(source instanceof net.minecraft.world.entity.projectile.Projectile projectile)source=projectile.getOwner();
        if(!(source instanceof LivingEntity attacker)||!minion(attacker))return true;
        if(victim==attacker)return true; // A witch may drink its own defensive potion.
        var boss=encounter(attacker);return boss!=null&&victim instanceof ServerPlayer p&&boss.accepts(p);
    }
    public static void guardMinion(LivingEntity entity){
        if(!minion(entity)||!(entity instanceof Mob mob)||!(entity.level() instanceof ServerLevel level)||entity.tickCount%20!=0)return;
        var boss=encounter(entity);if(boss==null){mob.setTarget(null);mob.setNoAi(true);if(entity.tickCount>100)entity.discard();return;}
        mob.setNoAi(false);if(boss.phase()!=2||!boss.allowed(level,mob.blockPosition())||mob.distanceToSqr(Vec3.atCenterOf(boss.center))>324){mob.discard();return;}
        var players=boss.active(level);mob.setTarget(players.stream().filter(boss::accepts).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null));
    }
    private void teleport(ServerLevel level,ServerPlayer target){
        for(int i=0;i<24;i++){double angle=random.nextDouble()*Math.PI*2;double radius=3+random.nextDouble()*6;var at=center.offset((int)Math.round(Math.cos(angle)*radius),0,(int)Math.round(Math.sin(angle)*radius));
            if(!allowed(level,at))continue;for(int y=0;y>=-1;y--){var feet=at.offset(0,y,0);var to=new Vec3(feet.getX()+.5,feet.getY(),feet.getZ()+.5);var box=getBoundingBox().move(to.subtract(position()));if(!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)||!level.noCollision(this,box)||level.containsAnyLiquid(box))continue;
                level.sendParticles(ParticleTypes.PORTAL,getX(),getY()+1,getZ(),30,.3,.7,.3,.1);teleportTo(to.x,to.y,to.z);getLookControl().setLookAt(target);return;}
        }
    }
    private void wave(ServerLevel level,List<ServerPlayer> players){
        minions.removeIf(id->level.getEntity(id)==null||!level.getEntity(id).isAlive());int count=Math.min(32-minions.size(),Math.min(12,players.size()*3));
        for(int i=0;i<count;i++){
            EntityType<? extends Mob> type=switch(random.nextInt(4)){case 0->net.minecraft.world.entity.EntityTypes.ZOMBIE;case 1->net.minecraft.world.entity.EntityTypes.SKELETON;case 2->net.minecraft.world.entity.EntityTypes.WITCH;default->net.minecraft.world.entity.EntityTypes.WITHER_SKELETON;};var mob=type.create(level,EntitySpawnReason.MOB_SUMMONED);if(mob==null)continue;
            for(int attempt=0;attempt<12;attempt++){var at=center.offset(random.nextInt(19)-9,0,random.nextInt(19)-9);if(!allowed(level,at)||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))continue;mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,random.nextFloat()*360,0);if(!level.noCollision(mob)||level.containsAnyLiquid(mob.getBoundingBox()))continue;
                mob.finalizeSpawn(level,level.getCurrentDifficultyAt(at),EntitySpawnReason.MOB_SUMMONED,null);mob.setCanPickUpLoot(false);mob.setTarget(players.get(i%players.size()));mob.addTag("jugcraft_gaia_minion");mob.addTag("jugcraft_gaia:"+getUUID());if(level.addFreshEntity(mob))minions.add(mob.getUUID());break;
            }
        }
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        if(phase()!=1&&phase()!=3||!(source.getEntity() instanceof ServerPlayer p)||!participants.contains(p.getUUID())||!active(level).contains(p)||!allowed(level,p.blockPosition()))return false;
        boolean hit=super.hurtServer(level,source,Math.min(32,amount));if(hit&&isAlive())attackTimer=Math.max(attackTimer,hard?31:46);return hit;
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKill){
        if(rewarded||phase()!=3||!(source.getEntity() instanceof ServerPlayer killer)||!participants.contains(killer.getUUID()))return;rewarded=true;
        for(var recipient:active(level)){
            String variant=(hard?"hard":"normal")+(recipient==killer?"_killer":"_helper");var table=level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("botanical_gaia/"+variant)));
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,position()).create(LootContextParamSets.CHEST);
            for(var item:table.getRandomItems(params)){if(item.is(Jugtania.item("dice")))item.set(BotanicalRelics.OWNER,recipient.getUUID());recipient.getInventory().placeItemBackInInventory(item,net.minecraft.util.Prediction.SERVER_ONLY);}
        }cleanupMinions(level);bar.removeAllPlayers();
    }
    private void cleanupMinions(ServerLevel level){for(var id:minions){var entity=level.getEntity(id);if(entity!=null&&entity.entityTags().contains("jugcraft_gaia_minion"))entity.discard();}minions.clear();}
    private void cancel(){if(level() instanceof ServerLevel level)cleanupMinions(level);bar.removeAllPlayers();discard();}
    @Override public void checkDespawn(){}
    @Override public void startSeenByPlayer(ServerPlayer p){if(participants.contains(p.getUUID()))bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){bar.removePlayer(p);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(center!=null)out.store("arena",BlockPos.CODEC,center);if(summoner!=null)out.store("summoner",UUIDUtil.CODEC,summoner);out.store("participants",UUIDUtil.CODEC.listOf(),new ArrayList<>(participants));out.store("minions",UUIDUtil.CODEC.listOf(),new ArrayList<>(minions));out.store("mines",BlockPos.CODEC.listOf(),new ArrayList<>(mines.keySet()));out.putBoolean("hard",hard);out.putBoolean("rewarded",rewarded);out.putInt("phase",phase());out.putInt("phaseTime",phaseTime);out.putInt("age",age);out.putInt("attackTimer",attackTimer);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);center=in.read("arena",BlockPos.CODEC).orElse(null);summoner=in.read("summoner",UUIDUtil.CODEC).orElse(null);participants.clear();in.read("participants",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(16).forEach(participants::add);minions.clear();in.read("minions",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(32).forEach(minions::add);hard=in.getBooleanOr("hard",false);rewarded=in.getBooleanOr("rewarded",false);entityData.set(PHASE,Math.clamp(in.getIntOr("phase",0),0,3));phaseTime=Math.clamp(in.getIntOr("phaseTime",0),0,36000);age=Math.clamp(in.getIntOr("age",0),0,36000);attackTimer=Math.clamp(in.getIntOr("attackTimer",0),0,60);mines.clear();in.read("mines",BlockPos.CODEC.listOf()).orElse(List.of()).stream().filter(at->center!=null&&at.distSqr(center)<=225).limit(48).forEach(at->mines.put(at,age+40));}
}
