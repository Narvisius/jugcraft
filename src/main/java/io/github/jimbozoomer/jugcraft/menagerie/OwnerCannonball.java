package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Original gravity and glancing ricochets, with bounded, individually permission-checked impact work. */
public final class OwnerCannonball extends ThrowableItemProjectile {
    private int bounces,life;
    public OwnerCannonball(EntityType<? extends OwnerCannonball> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return OwnerSupplementaries.ITEMS.get("cannonball");}
    @Override protected double getDefaultGravity(){return .035;}
    @Override protected float getAirDrag(){return .99F;}
    @Override public void tick(){
        if(!level().isClientSide()){
            if(++life>300||getDeltaMovement().lengthSqr()>100){discard();return;}
            var end=position().add(getDeltaMovement());
            for(int i=0;i<=8;i++){var p=BlockPos.containing(position().lerp(end,i/8D));if(!level().hasChunkAt(p)||!level().getWorldBorder().isWithinBounds(p)){discard();return;}}
        }super.tick();
    }
    @Override protected void onHitEntity(EntityHitResult hit){
        if(!(level() instanceof ServerLevel level))return;
        if(getOwner() instanceof ServerPlayer actor&&hit.getEntity() instanceof LivingEntity target&&ConcordanceEffects.mayHarm(actor,target)&&TwoHanded.allowed(actor,level,target)){
            target.hurtServer(level,damageSources().thrown(this,actor),(float)Math.min(40,6*getDeltaMovement().lengthSqr()));
        }else if(getOwner() instanceof OwnerPlunderer crew&&hit.getEntity() instanceof LivingEntity target&&crew.canAttack(target)){
            target.hurtServer(level,damageSources().thrown(this,crew),(float)Math.min(24,6*getDeltaMovement().lengthSqr()));
        }discard();
    }
    @Override protected void onHitBlock(BlockHitResult hit){
        if(!(level() instanceof ServerLevel level))return;
        if(!(getOwner() instanceof ServerPlayer actor)||!Authority.mayChange(level,actor,hit.getBlockPos())){discard();return;}
        var velocity=getDeltaMovement();var normal=new Vec3(hit.getDirection().getStepX(),hit.getDirection().getStepY(),hit.getDirection().getStepZ());
        if(bounces<3&&(level.getBlockState(hit.getBlockPos()).is(Blocks.SLIME_BLOCK)||Math.abs(velocity.normalize().dot(normal))<Math.cos(Math.toRadians(75)))){
            bounces++;var bounced=velocity.subtract(normal.scale(2*velocity.dot(normal)));setDeltaMovement(bounced);setPos(hit.getLocation().add(normal.scale(.26)));needsSync=true;
            level.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.ANVIL_LAND,net.minecraft.sounds.SoundSource.BLOCKS,.35F,1.8F);return;
        }
        double budget=velocity.lengthSqr()*5;int broken=0;
        // No explosive flood-fill: a 3x3x3 impact has a fixed bound and respects containers/claims per cell.
        var cells=new java.util.ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(hit.getBlockPos().offset(-1,-1,-1),hit.getBlockPos().offset(1,1,1)))cells.add(p.immutable());
        cells.sort(java.util.Comparator.comparingDouble(p->p.distToCenterSqr(hit.getLocation())));
        for(var p:cells){if(budget<=0)break;if(!level.hasChunkAt(p)||!Authority.mayChange(level,actor,p))continue;var state=level.getBlockState(p);float cost=state.getDestroySpeed(level,p);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity()||cost<0||state.getBlock().getExplosionResistance()>30)continue;
            cost=Math.max(.5F,cost);if(cost>budget)continue;if(level.destroyBlock(p,true,actor)){budget-=cost;broken++;}
        }
        if(broken==0||budget<1){discard();return;}setDeltaMovement(velocity.scale(Math.min(.85,budget/(velocity.lengthSqr()*5))));setPos(hit.getLocation().add(velocity.normalize().scale(.3)));needsSync=true;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("Bounces",bounces);out.putInt("FlightAge",life);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);bounces=Math.clamp(in.getIntOr("Bounces",0),0,3);life=Math.clamp(in.getIntOr("FlightAge",0),0,301);}
}
