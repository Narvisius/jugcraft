package io.github.jimbozoomer.jugcraft.nouveau;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Persisted summon leases, native creature rendering and one authority-checked combat controller. */
public final class NouveauSummons {
    private static AttachmentType<UUID> OWNER;
    private static AttachmentType<Long> UNTIL;
    private static AttachmentType<BlockState> BLOCK;
    private static final Set<LivingEntity> LIVE=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LivingEntity,Deque<Memory>> HISTORY=new WeakHashMap<>();
    private record Memory(Vec3 position,float health){}
    private NouveauSummons(){}
    public static boolean ownedBy(LivingEntity entity,UUID owner){return OWNER!=null&&owner.equals(entity.getAttached(OWNER));}
    public static boolean owned(LivingEntity entity){return OWNER!=null&&entity.hasAttached(OWNER);}
    public static void register(){
        OWNER=AttachmentRegistry.<UUID>builder().persistent(UUIDUtil.CODEC).buildAndRegister(Jugcraft.id("nouveau_summoner"));
        UNTIL=AttachmentRegistry.<Long>builder().persistent(Codec.LONG).buildAndRegister(Jugcraft.id("nouveau_summon_expiry"));
        BLOCK=AttachmentRegistry.<BlockState>builder().persistent(BlockState.CODEC).buildAndRegister(Jugcraft.id("nouveau_animated_block"));
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(entity instanceof LivingEntity living&&living.hasAttached(UNTIL)){if(living instanceof Mob mob&&living.hasAttached(OWNER))install(mob);else LIVE.add(living);}});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{LIVE.clear();HISTORY.clear();});
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->ownedEntity(entity)&&(entity instanceof net.minecraft.world.entity.decoration.Mannequin||player.isShiftKeyDown())?net.minecraft.world.InteractionResult.FAIL:net.minecraft.world.InteractionResult.PASS);
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var mob:List.copyOf(LIVE)){
                if(mob.isRemoved()){LIVE.remove(mob);continue;}if(!(mob.level() instanceof ServerLevel level)||!level.hasChunkAt(mob.blockPosition()))continue;
                if(level.getGameTime()>=mob.getAttachedOrElse(UNTIL,0L)||!JugNouveau.enabled()){mob.discard();LIVE.remove(mob);continue;}
                if(mob instanceof net.minecraft.world.entity.decoration.Mannequin&&server.getTickCount()%20==0){
                    var p=Authority.answering(level,mob.getAttached(OWNER));if(p!=null)for(var foe:level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(12),e->e instanceof Enemy&&ConcordanceEffects.mayHarm(p,e)&&Authority.mayStrike(p,e)).stream().limit(16).toList())foe.setTarget(mob);
                }
            }
            if(server.getTickCount()%20==0)for(var player:server.getPlayerList().getPlayers()){
                remember(player);for(var entity:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(16),e->!(e instanceof net.minecraft.world.entity.player.Player)).stream().limit(32).toList())remember(entity);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
            if(source.getEntity() instanceof Mob mob&&mob.hasAttached(OWNER)){
                var p=Authority.answering((ServerLevel)mob.level(),mob.getAttached(OWNER));
                return p!=null&&ConcordanceEffects.mayHarm(p,target)&&Authority.mayStrike(p,target);
            }return true;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,amount)->{
            if(entity.hasAttached(OWNER)){if(entity.level() instanceof ServerLevel level){var player=Authority.answering(level,entity.getAttached(OWNER));if(player!=null&&NouveauGear.worn(player,"summon_focus")&&source.getEntity() instanceof LivingEntity killer&&ConcordanceEffects.mayHarm(player,killer)&&Authority.mayStrike(player,killer))killer.hurtServer(level,level.damageSources().indirectMagic(player,player),6);}entity.discard();return false;}return true;
        });
    }
    public static void enemyWolf(ServerLevel l,Mob hunter,LivingEntity target){
        var wolf=net.minecraft.world.entity.EntityTypes.WOLF.create(l,EntitySpawnReason.MOB_SUMMONED);if(wolf==null)return;wolf.snapTo(hunter.getX()+1,hunter.getY(),hunter.getZ(),hunter.getYRot(),0);if(!l.noCollision(wolf))return;wolf.setAttached(UNTIL,l.getGameTime()+600);wolf.setTarget(target);if(l.addFreshEntity(wolf))LIVE.add(wolf);
    }
    private static boolean ownedEntity(Entity e){return e instanceof LivingEntity living&&owned(living);}
    private static void remember(LivingEntity entity){var memory=HISTORY.computeIfAbsent(entity,e->new ArrayDeque<>());memory.addLast(new Memory(entity.position(),entity.getHealth()));while(memory.size()>6)memory.removeFirst();}
    public static boolean rewind(ServerLevel level,ServerPlayer actor,LivingEntity target){
        var history=HISTORY.get(target);if(history==null||history.size()<2)return false;var before=history.getFirst();var at=BlockPos.containing(before.position());
        if(!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at)||!level.noCollision(target,target.getBoundingBox().move(before.position().subtract(target.position()))))return false;
        if(target!=actor&&!ConcordanceEffects.mayHarm(actor,target))return false;
        if(target instanceof ServerPlayer p)p.teleportTo(level,before.position.x,before.position.y,before.position.z,Set.of(),p.getYRot(),p.getXRot(),true);
        else target.teleportTo(before.position.x,before.position.y,before.position.z);
        target.setHealth(Math.min(target.getMaxHealth(),Math.max(1,before.health)));history.clear();return true;
    }
    public static boolean dye(ServerPlayer player,LivingEntity target){
        var color=io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock.dyeColor(player.getOffhandItem());if(color==null)return false;
        if(target instanceof Sheep sheep){sheep.setColor(color);return true;}
        if(target instanceof Starbuncle star&&star.mayServe(player)){star.color(color.getId());return true;}return false;
    }
    public static boolean summon(ServerLevel l,ServerPlayer p,BlockPos at,String kind,int power,int duration,int split,int pierce){
        if(!Authority.mayChange(l,p,at)||!l.hasChunkAt(at)||!RateGate.allow(p,"nouveau_summon",200))return false;
        if(p.hasEffect(NouveauStatuses.EFFECTS.get("summoning_sickness")))return false;
        int count=Math.min(8-(int)LIVE.stream().filter(m->p.getUUID().equals(m.getAttached(OWNER))).count(),(kind.equals("summon_wolves")?2:kind.equals("summon_vex")?3:1)+Math.min(4,split));
        boolean spawned=false;
        for(int n=0;n<count;n++){
            var pos=at.offset(n%3,0,n/3);if(!l.hasChunkAt(pos)||!Authority.mayChange(l,p,pos))continue;
            EntityType<? extends LivingEntity> type=switch(kind){case "summon_wolves"->net.minecraft.world.entity.EntityTypes.WOLF;case "summon_vex"->net.minecraft.world.entity.EntityTypes.VEX;case "summon_undead"->pierce>0?net.minecraft.world.entity.EntityTypes.WITHER_SKELETON:net.minecraft.world.entity.EntityTypes.SKELETON;case "summon_steed"->net.minecraft.world.entity.EntityTypes.HORSE;default->net.minecraft.world.entity.EntityTypes.MANNEQUIN;};
            LivingEntity mob=type.create(l,EntitySpawnReason.MOB_SUMMONED);if(mob==null)continue;
            mob.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,p.getYRot(),0);if(!l.noCollision(mob))continue;
            mob.setAttached(OWNER,p.getUUID());boolean focus=NouveauGear.worn(p,"summon_focus");mob.setAttached(UNTIL,l.getGameTime()+Math.clamp(focus?duration*2:duration,100,4800));if(focus){mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED,duration*2,1));mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH,duration*2,1));}
            if(mob instanceof Mob m){m.setPersistenceRequired();for(var slot:EquipmentSlot.VALUES)m.setDropChance(slot,0);}
            if(mob instanceof TamableAnimal tame)tame.tame(p);
            if(mob instanceof AbstractHorse horse){horse.tameWithName(p);horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));}
            if(kind.equals("summon_undead"))mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(pierce>0?Items.STONE_SWORD:Items.BOW));
            if(kind.equals("summon_decoy")){mob.setCustomName(p.getDisplayName());mob.setComponent(net.minecraft.core.component.DataComponents.PROFILE,net.minecraft.world.item.component.ResolvableProfile.createResolved(p.getGameProfile()));for(var slot:EquipmentSlot.VALUES)mob.setItemSlot(slot,p.getItemBySlot(slot).copy());}
            if(!l.addFreshEntity(mob))continue;if(mob instanceof Mob m)install(m);else LIVE.add(mob);spawned=true;
        }
        if(spawned&&!kind.equals("summon_decoy")&&!kind.equals("summon_steed"))NouveauStatuses.give(p,"summoning_sickness",100,0,p);
        return spawned;
    }
    private static void install(Mob mob){
        if(!LIVE.add(mob))return;mob.removeAllGoals(g->true);
        mob.getGoalSelector().addGoal(0,new Goal(){
            int age;{setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return mob.isAlive();}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public void tick(){
                if(!(mob.level() instanceof ServerLevel l)||++age%10!=0)return;
                var p=Authority.answering(l,mob.getAttached(OWNER));if(p==null||p.level()!=l||!p.isAlive()){mob.getNavigation().stop();return;}
                if(mob instanceof AbstractHorse||mob.isNoAi())return;
                var enemy=l.getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(12),e->e instanceof Enemy&&e.isAlive()&&!e.hasAttached(OWNER)&&ConcordanceEffects.mayHarm(p,e)&&Authority.mayStrike(p,e)).stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                var destination=enemy==null?p:enemy;mob.getLookControl().setLookAt(destination,30,30);
                if(enemy!=null&&mob.getMainHandItem().is(Items.BOW)&&mob.distanceToSqr(enemy)<=144&&mob.hasLineOfSight(enemy)){
                    mob.getNavigation().stop();if(age%30==0){var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(l,mob,new ItemStack(Items.ARROW),mob.getMainHandItem());var delta=enemy.getEyePosition().subtract(mob.getEyePosition());arrow.shoot(delta.x,delta.y+Math.sqrt(delta.horizontalDistanceSqr())*.15,delta.z,1.5F,2);arrow.pickup=net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;l.addFreshEntity(arrow);}return;
                }
                if(mob.distanceToSqr(destination)>4){mob.getNavigation().moveTo(destination,1.1);mob.getMoveControl().setWantedPosition(destination.getX(),destination.getY(),destination.getZ(),1.1);}
                else if(enemy!=null&&age%20==0){
                    var cause=Cause.of(p.getUUID(),Cause.Origin.CREATURE,"jugcraft:nouveau_summon",ConcordanceEffects.nextSerial());
                    ConcordanceEffects.apply(new ConcordanceEffects.Context(l,cause,p,new Ledger(new Ledger.Limits(1,4,0)),"strike",mob.position()),EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,4,0),enemy);
                }
            }
        });
    }
    public static boolean moveBlock(ServerLevel l,ServerPlayer p,BlockPos at,String operation,int power,int duration){
        var block=l.getBlockState(at);if(block.isAir()||block.getDestroySpeed(l,at)<0||l.getBlockEntity(at)!=null||!Authority.mayChange(l,p,at))return false;
        if(operation.equals("animate_block"))return NouveauAnimatedBlock.animate(l,p,at,duration);
        var falling=FallingBlockEntity.fall(l,at,block);Vec3 motion=switch(operation){case "launch"->new Vec3(0,.3*Math.min(4,power),0);case "pull"->p.position().subtract(Vec3.atCenterOf(at)).normalize().scale(.3);case "gust"->Vec3.atCenterOf(at).subtract(p.position()).normalize().scale(.3);default->Vec3.ZERO;};
        falling.setDeltaMovement(motion);return true;
    }
    private static void recoverBlock(Mob mob,ServerLevel l){var block=mob.getAttached(BLOCK);if(block!=null){mob.removeAttached(BLOCK);net.minecraft.world.level.block.Block.popResource(l,mob.blockPosition(),new ItemStack(block.getBlock()));}}
}
