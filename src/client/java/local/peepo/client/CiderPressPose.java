package local.peepo.client;

import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity;
import local.peepo.PeepoEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Sit on one fixed end of the real capstan, following the block renderer's rotation. */
final class CiderPressPose {
    static float angle(float handle,Direction facing){
        return handle-switch(facing){case EAST->90;case SOUTH->180;case WEST->270;default->0;};
    }
    static Vec3 seat(BlockPos pos,float angle){
        double radians=Math.toRadians(angle);
        return Vec3.atBottomCenterOf(pos).add(Math.cos(radians)*4.3/16,17.0/16,-Math.sin(radians)*4.3/16);
    }
    static void prepare(PeepoEntity npc,PeepoState s,CiderPressBlockEntity press,float partial){
        float angle=angle(press.visualHandleAngle(partial),press.getBlockState().getValue(CiderPressBlock.FACING));
        // Feet are below the seat: the rig's hips/leg pivots sit 1.5 pixels above its origin.
        s.pressOffset=seat(npc.workTarget(),angle).subtract(npc.getPosition(partial)).add(0,-1.5/16,0);
        s.pressSeated=true;s.bodyRot=180-angle;s.yRot=s.xRot=0;s.shadowRadius=0;
        s.workPhase=angle*Mth.DEG_TO_RAD;
    }
    static void apply(PeepoState s,ModelPart left,ModelPart right,ModelPart head,ModelPart leftLeg,ModelPart rightLeg){
        float kick=Mth.sin(s.workPhase*2)*.08F;
        leftLeg.xRot=-1.25F+kick;rightLeg.xRot=-1.25F-kick;
        leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;
        left.xRot=right.xRot=-.18F;left.yRot=right.yRot=0;
        left.zRot=-.10F;right.zRot=.10F;
        left.xScale=left.yScale=left.zScale=right.xScale=right.yScale=right.zScale=1;
        head.xRot=.06F;head.yRot=0;
    }
}
