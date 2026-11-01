package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Directional item conveyor. Paid wind pauses under redstone and survives reloads. */
final class BotanicalWind {
    private static AABB bounds(BlockPos a,BlockPos b){return new AABB(a.getX(),a.getY(),a.getZ(),b.getX(),b.getY(),b.getZ());}

    private final BotanicalWorkshop flower;
    private Direction direction=Direction.NORTH;
    private int remaining;
    BotanicalWind(BotanicalWorkshop flower){this.flower=flower;}
    void face(Direction value){direction=value.getAxis().isHorizontal()?value:Direction.NORTH;flower.setChanged();}
    void rotate(ServerPlayer player){face(direction.getClockWise());player.sendOverlayMessage(Component.literal("Wind direction: "+direction.getName()));}
    void tick(ServerLevel level,ServerPlayer actor){
        if(remaining==0){if(!flower.draw(1))return;remaining=20;}
        remaining--;flower.setChanged();var origin=flower.effectivePos();var unit=Vec3.atLowerCornerOf(direction.getUnitVec3i());
        var bounds=bounds(origin.relative(direction),origin.relative(direction,16)).inflate(direction.getAxis()==Direction.Axis.X?0:2,3,direction.getAxis()==Direction.Axis.Z?0:2);
        int examined=0;
        for(var drop:level.getEntitiesOfClass(ItemEntity.class,bounds)){
            if(++examined>32)break;if(!drop.isAlive()||drop.getAge()<20)continue;
            var at=drop.blockPosition();var movement=drop.getDeltaMovement().add(unit.scale(.05));
            if(movement.lengthSqr()>1)movement=movement.normalize();var next=BlockPos.containing(drop.position().add(movement.scale(2)));
            if(!level.hasChunkAt(next)||!ClaimTransfers.allowed(level,origin,at)||!ClaimTransfers.allowed(level,at,next)||!Authority.mayChange(level,actor,at)||!Authority.mayChange(level,actor,next))continue;
            var from=Vec3.atCenterOf(origin);var hit=level.clip(new net.minecraft.world.level.ClipContext(from.add(unit.scale(.6)),drop.position(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,drop));
            if(hit.getType()!=HitResult.Type.MISS)continue;
            drop.setDeltaMovement(movement);drop.needsSync=true;
        }
        if(level.getGameTime()%10==0)level.sendParticles(ParticleTypes.CLOUD,origin.getX()+.5+unit.x,origin.getY()+.5,origin.getZ()+.5+unit.z,1,.02,.02,.02,.01);
    }
    void save(ValueOutput out){out.putString("flower_wind_direction",direction.getName());out.putInt("flower_wind_remaining",remaining);}
    void load(ValueInput in){var saved=Direction.byName(in.getStringOr("flower_wind_direction","north"));direction=saved!=null&&saved.getAxis().isHorizontal()?saved:Direction.NORTH;remaining=Math.clamp(in.getIntOr("flower_wind_remaining",0),0,20);}
}
