package local.peepo;

import net.minecraft.world.phys.Vec3;

/** Stationary mounts need a correction only when displaced, not a teleport every tick. */
final class CompanionMotion {
    static void position(PeepoEntity npc,Vec3 at,float yaw){
        if(npc.position().distanceToSqr(at)>1.0E-6)npc.snapTo(at.x,at.y,at.z,yaw,0);
        npc.setYRot(yaw);npc.setXRot(0);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
    }
}
