package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import io.github.jimbozoomer.jugcraft.mixin.*;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;

/** Bounded effects centered on a pedestal, spending only its buffer. */
public final class AlchemyPedestalEffects {
    public static final Set<String> EXTRA=Set.of("black_hole_band","archangel_smite","ignition_ring","zero_ring","watch_of_flowing_time","mind_stone");
    private AlchemyPedestalEffects(){}
    public static boolean run(AlchemyMachinesBlockEntity be,ServerPlayer p,String id){
        return switch(id){case "black_hole_band"->pickup(be,p);case "mind_stone"->experience(be,p);
            case "watch_of_flowing_time"->accelerate(be,p);default->defend(be,p,id);};
    }
    private static boolean pay(AlchemyMachinesBlockEntity be,int amount){if(be.essence()<amount)return false;be.setEssence(be.essence()-amount);return true;}
    private static boolean visible(ServerPlayer p,BlockPos from,Vec3 to){return p.level().clip(new ClipContext(Vec3.atCenterOf(from).add(0,.5,0),to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    private static boolean pickup(AlchemyMachinesBlockEntity be,ServerPlayer p){
        var level=p.level();var center=Vec3.atCenterOf(be.getBlockPos());var drops=new ArrayList<ItemEntity>();
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class),new AABB(be.getBlockPos()).inflate(be.range()),e->e.isAlive(),drops,24);
        int moved=0;
        for(var drop:drops){
            if(moved==4||be.essence()<1)break;UUID reserved=((AlchemicalItemAccess)drop).jugcraft$pickupTarget();
            if(drop.hasPickUpDelay()||reserved!=null&&!reserved.equals(p.getUUID())||drop.getOwner()!=null&&!drop.getOwner().getUUID().equals(p.getUUID())
                    ||drop.position().distanceToSqr(center)>be.range()*be.range()||!Authority.mayChange(level,p,drop.blockPosition())||!visible(p,be.getBlockPos(),drop.position()))continue;
            var item=drop.getItem();int inserted=0;
            for(var side:Direction.values()){
                var pos=be.getBlockPos().relative(side);if(!level.hasChunkAt(pos)||!Authority.mayChange(level,p,pos))continue;
                if(level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity chest){
                    if(!chest.mayUse(p)||chest.viewers>0)continue;
                    int slot=AlchemyAutomation.room(chest,item,0,chest.getContainerSize());if(slot<0)continue;
                    var out=chest.getItem(slot);inserted=Math.min(item.getCount(),out.isEmpty()?item.getMaxStackSize():out.getMaxStackSize()-out.getCount());
                    if(out.isEmpty())chest.setItem(slot,item.copyWithCount(inserted));else out.grow(inserted);chest.setChanged();
                }else{
                    var storage=ItemStorage.SIDED.find(level,pos,side.getOpposite());if(storage==null)continue;
                    try(var tx=Transaction.openOuter()){inserted=(int)storage.insert(ItemVariant.of(item),item.getCount(),tx);if(inserted>0)tx.commit();}
                }
                if(inserted>0)break;
            }
            if(inserted>0){item.shrink(inserted);if(item.isEmpty())drop.discard();else drop.setItem(item);pay(be,1);moved++;}
        }be.recordTargets(moved);return moved>0;
    }
    private static boolean experience(AlchemyMachinesBlockEntity be,ServerPlayer p){
        var stone=be.getItem(1);int room=AlchemyStorage.XP_CAPACITY-AlchemyStorage.storedXp(stone),taken=0;
        if(room==0)return false;var orbs=new ArrayList<ExperienceOrb>();
        p.level().getEntities(EntityTypeTest.forClass(ExperienceOrb.class),new AABB(be.getBlockPos()).inflate(be.range()),e->e.isAlive(),orbs,16);
        for(var orb:orbs){
            if(be.essence()<1||taken>=100||orb.position().distanceToSqr(Vec3.atCenterOf(be.getBlockPos()))>be.range()*be.range()||!Authority.mayChange(p.level(),p,orb.blockPosition())||!visible(p,be.getBlockPos(),orb.position()))continue;
            int value=orb.getValue();if(value<=0)continue;var access=(AlchemyExperienceAccess)orb;
            int available=Math.min(room-taken,100-taken);
            int count=Math.min(access.jugcraft$count(),available/value);
            int partial=count<access.jugcraft$count()?Math.min(value-1,available-count*value):0;
            if(count==0&&partial==0)continue;
            // Large vanilla orbs can exceed the whole pulse budget. Split only the one partially consumed unit.
            if(partial>0){
                var remainder=new ExperienceOrb(p.level(),orb.getX(),orb.getY(),orb.getZ(),value-partial);
                if(!p.level().addFreshEntity(remainder))continue;
            }
            taken+=count*value+partial;int remaining=access.jugcraft$count()-count-(partial>0?1:0);
            if(remaining==0)orb.discard();else access.jugcraft$count(remaining);pay(be,1);
        }
        if(taken>0){stone.set(AlchemyStorage.EXPERIENCE,AlchemyStorage.storedXp(stone)+taken);stone.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);be.setChanged();}
        be.recordTargets(taken);return taken>0;
    }
    private static boolean accelerate(AlchemyMachinesBlockEntity be,ServerPlayer p){
        boolean worked=false;
        for(var side:Direction.values()){
            if(be.essence()<128)break;var pos=be.getBlockPos().relative(side);
            if(!p.level().hasChunkAt(pos)||!Authority.mayChange(p.level(),p,pos))continue;
            boolean advanced=p.level().getBlockEntity(pos) instanceof AlchemicalWorkshopBlockEntity w?w.bonusPulse(p):AlchemyMachines.bonusWork(p,pos);
            if(advanced){pay(be,128);worked=true;be.recordTargets(1);}
        }return worked;
    }
    private static boolean defend(AlchemyMachinesBlockEntity be,ServerPlayer p,String id){
        var mobs=new ArrayList<LivingEntity>();var center=Vec3.atCenterOf(be.getBlockPos());
        p.level().getEntities(EntityTypeTest.forClass(LivingEntity.class),new AABB(be.getBlockPos()).inflate(be.range()),e->e instanceof Enemy&&e.isAlive(),mobs,16);
        int done=0,cost=id.equals("archangel_smite")?64:16;
        var cause=Cause.of(p.getUUID(),Cause.Origin.SHRINE,"jugcraft:"+id,ConcordanceEffects.nextSerial());
        var ledger=new Ledger(new Ledger.Limits(4,32,0));
        for(var target:mobs){
            if(done==4||be.essence()<cost)break;
            if(target.position().distanceToSqr(center)>be.range()*be.range()||!Authority.mayChange(p.level(),p,target.blockPosition())||!Authority.mayStrike(p,target)
                    ||!ConcordanceEffects.mayHarm(p,target)||!visible(p,be.getBlockPos(),target.getEyePosition()))continue;
            var effect=id.equals("zero_ring")?new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,1,60,"minecraft:slowness",Stacking.STRONGEST,null)
                :id.equals("ignition_ring")?new EffectSpec(EffectKind.STATUS,Intent.HARMFUL,0,60,"jugcraft:smoulder",Stacking.STRONGEST,null)
                :EffectSpec.of(EffectKind.DAMAGE,Intent.HARMFUL,8,0);
            if(ConcordanceEffects.apply(new ConcordanceEffects.Context(p.level(),cause,p,ledger,"pedestal",center),effect,target).applied()){pay(be,cost);done++;}
        }be.recordTargets(done);return done>0;
    }
    public static boolean harvest(AlchemyMachinesBlockEntity be,ServerPlayer p,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        if(be.essence()<16||!Authority.mayChange(p.level(),p,pos)||!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state))return false;
        if(crop instanceof OrganismCropBlock organism){
            if(!io.github.jimbozoomer.jugcraft.concordance.garden.Garden.enabled()||!Authority.mayChange(p.level(),p,pos.below()))return false;
            var drops=organism.harvest(p.level(),pos,state);if(drops.isEmpty()||p.level().getBlockState(pos).equals(state))return false;
            for(var drop:drops)Block.popResource(p.level(),pos,drop);
        }else {ArmItem.reap(p.level(),pos,p,ItemStack.EMPTY);if(p.level().getBlockState(pos).equals(state))return false;}
        pay(be,16);return true;
    }
}
