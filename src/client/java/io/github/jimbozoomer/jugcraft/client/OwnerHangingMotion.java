package io.github.jimbozoomer.jugcraft.client;

import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

/** Supplied Amendments pendulum constants, adapted to bounded client extraction. No world changes. */
final class OwnerHangingMotion {
    private static final WeakHashMap<BlockEntity,Motion> MOTIONS=new WeakHashMap<>();
    private static final double K=Math.pow(Math.PI*2*.6,2),MIN=K*(1-Math.cos(Math.toRadians(.8))),MAX=K*(1-Math.cos(Math.toRadians(60)));
    private static final class Motion {long tick=Long.MIN_VALUE;double angle,previous,velocity;int immunity;}
    static float angle(BlockEntity be,float partial,Vec3 normal,boolean lantern){
        if(be.getLevel()==null)return 0;
        var m=MOTIONS.computeIfAbsent(be,key->{var n=new Motion();n.angle=Math.sin(key.getBlockPos().asLong())*Math.toRadians(.8);n.velocity=Math.sqrt(Math.max(0,2*(MIN-K*(1-Math.cos(n.angle)))));return n;});
        long now=be.getLevel().getGameTime();
        if(m.tick!=now){
            // Do not simulate a backlog after unload, pause or a distant frame.
            if(m.tick!=Long.MIN_VALUE&&(now-m.tick>5||now<m.tick)){m.angle=m.previous=0;m.velocity=Math.sqrt(2*MIN);m.immunity=0;}
            m.tick=now;m.previous=m.angle;if(m.immunity>0)m.immunity--;
            if(m.immunity==0){
                int checked=0;
                for(var entity:be.getLevel().getEntities((net.minecraft.world.entity.Entity)null,new AABB(be.getBlockPos()).inflate(.1),e->!e.isSpectator())){
                    if(++checked>16)break;
                    double velocity=entity.getDeltaMovement().dot(normal);if(Math.abs(velocity)<.01)continue;
                    var bounds=entity.getBoundingBox();double mass=Math.clamp(bounds.getXsize()*bounds.getYsize()*bounds.getZsize(),.05,8);
                    m.velocity+=velocity*15*mass/(1+mass);m.immunity=10;break;
                }
            }
            double cap=lantern?K*(1-Math.cos(Math.toRadians(45))):MAX;
            double potential=K*(1-Math.cos(m.angle));
            if(potential+.5*m.velocity*m.velocity>cap)m.velocity=Math.copySign(Math.sqrt(Math.max(0,2*(cap-potential))),m.velocity);
            double acc=-K*Math.sin(m.angle);
            if(potential+.5*m.velocity*m.velocity>MIN)acc-=.525*m.velocity;
            double dt=be.getBlockState().getFluidState().isEmpty()?.05:.025;
            m.velocity+=dt*acc;m.angle=Math.clamp(m.angle+dt*m.velocity,-Math.toRadians(60),Math.toRadians(60));
        }
        return (float)Math.toDegrees(m.previous+(m.angle-m.previous)*Math.clamp(partial,0,1));
    }
}
