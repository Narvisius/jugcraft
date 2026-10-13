package local.peepo.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Shared procedural clips on the existing rig. Work targets/timing stay server-authoritative. */
final class CompanionWorkPose {
    static final float GRIP_Y=20F, GRIP_Z=-3.4F;
    static float gripX(PeepoState s){return Mth.cos(s.workPhase)*.65F;}
    static float gripZ(PeepoState s){return GRIP_Z+Mth.sin(s.workPhase)*.65F;}
    static void interact(PeepoState s,ModelPart left,ModelPart right,ModelPart head){
        float lift=Mth.sin(s.workPhase*3)*.24F;
        left.xRot=right.xRot=-1.45F+lift;
        left.yRot=.28F;right.yRot=-.28F;left.zRot=right.zRot=0;
        head.yRot=0;head.xRot=.12F+lift*.08F;
    }
    static void pie(PeepoState s,ModelPart left,ModelPart right,ModelPart head,ModelPart leftLeg,ModelPart rightLeg,ModelPart[] torso){
        head.yRot=0;head.xRot=.16F+Mth.sin(s.ageInTicks*.06F)*.025F;
        if(s.work==local.peepo.WorkAnimation.PIE_WAIT){
            // Rest both hands low while watching the opening, rather than repeatedly working an empty surface.
            left.xRot=right.xRot=-.45F;left.yRot=.3F;right.yRot=-.3F;
            left.zRot=right.zRot=0;leftLeg.xRot=rightLeg.xRot=0;return;
        }
        float lean=s.pieReach*2.4F;
        for(var part:torso)part.z-=lean;
        left.x=s.pumpkin?2.9F:2.3F;right.x=-left.x;left.y=right.y=19.2F;
        left.z=right.z=(s.pumpkin?-2.6F:-1.4F)-lean;
        // Hands push/catch at the mouth; the pie slides the last short distance along the shelf.
        float gripZ=Math.max(s.pieZ,left.z-3.8F);
        reach(left,s.pieX+2.5F,s.pieY+.7F,gripZ);reach(right,s.pieX-2.5F,s.pieY+.7F,gripZ);
        if(s.work!=local.peepo.WorkAnimation.PIE_CARRY)leftLeg.xRot=rightLeg.xRot=0;
    }
    static void stir(PeepoState s,ModelPart left,ModelPart right,ModelPart head,ModelPart leftLeg,ModelPart rightLeg){
        grip(s,left,1);grip(s,right,-1);
        head.yRot=0;head.xRot=.18F;
        // Keep both feet planted on the rim while the hands circle the spoon.
        leftLeg.xRot=rightLeg.xRot=0;
        leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;
    }
    private static void grip(PeepoState s,ModelPart arm,int side){
        // Move the shoulders towards the front of the broad torso; both hands share the spoon grip.
        arm.x=side*2.3F;arm.y=19.2F;arm.z=-1.4F;
        reach(arm,gripX(s)+side*.35F,GRIP_Y,gripZ(s));
    }
    static void crank(PeepoState s,ModelPart left,ModelPart right,ModelPart head,ModelPart leftLeg,ModelPart rightLeg,ModelPart[] torso){
        float lift=Math.max(0,17.0F-s.crankY),hang=Mth.clamp(lift/2,0,1);
        float push=Mth.clamp((s.crankY-16)/4,0,1),pitch=.22F*push-.12F*hang;
        float sway=s.crankX*.7F,bodyY=-lift+.25F*push,bodyZ=s.crankZ+5.5F;
        // Track the handle from beside the rotor: rise on the upper arc, then lean into
        // the downward push. The server entity remains at its checked standing position.
        head.yRot=0;head.xRot=-.12F*hang+.1F*push;
        for(var part:torso){lean(part,pitch,0,sway,bodyY);part.z+=bodyZ;}
        leftLeg.y+=bodyY;rightLeg.y+=bodyY;leftLeg.x+=sway;rightLeg.x+=sway;
        leftLeg.z+=bodyZ;rightLeg.z+=bodyZ;
        float kick=Mth.sin(s.workPhase)*.35F;
        leftLeg.xRot=hang*(.4F+kick)+push*.18F;rightLeg.xRot=hang*(.4F-kick)-push*.18F;
        leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;
        left.x=s.pumpkin?2.9F:2.3F;right.x=-left.x;
        left.y=right.y=19.2F;left.z=right.z=s.pumpkin?-2.6F:-1.4F;
        lean(left,pitch,0,sway,bodyY);lean(right,pitch,0,sway,bodyY);
        left.z+=bodyZ;right.z+=bodyZ;
        reach(left,s.crankX+.32F,s.crankY,s.crankZ);reach(right,s.crankX-.32F,s.crankY,s.crankZ);
    }
    static void tool(PeepoState s,ModelPart left,ModelPart right,ModelPart head,ModelPart leftLeg,ModelPart rightLeg,ModelPart[] torso){
        var p=s.toolPose;
        left.x=s.pumpkin?2.9F:2.3F;right.x=-left.x;
        left.y=right.y=19.2F;left.z=right.z=s.pumpkin?-2.6F:-1.4F;
        head.yRot=0;head.xRot=.14F;
        if(p.bodyPitch!=0 || p.bodyRoll!=0 || p.bodyX!=0 || p.bodyY!=0){
            for(var part:torso)lean(part,p);
            lean(left,p);lean(right,p);
        }
        // Solve the arms after the shoulders move, keeping both hands on the fixed tool path.
        reach(left,p.leftX,p.leftY,p.leftZ);reach(right,p.rightX,p.rightY,p.rightZ);
        leftLeg.xRot=rightLeg.xRot=0;
        leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;
        if(s.work==local.peepo.WorkAnimation.HARVEST){
            float brace=Math.max(0,p.bodyPitch);
            leftLeg.xRot=brace*.35F;rightLeg.xRot=-brace*.20F;
        }
    }
    private static void lean(ModelPart part,MachineWorkClip pose){
        lean(part,pose.bodyPitch,pose.bodyRoll,pose.bodyX,pose.bodyY);
    }
    private static void lean(ModelPart part,float pitch,float roll,float bodyX,float bodyY){
        // The rig's parts have different pivots (some at y=9.6). Rotate all around the hips,
        // so head, shirt, bare torso, shorts and costume move together with the feet braced.
        float dy=part.y-22.5F,cosX=Mth.cos(pitch),sinX=Mth.sin(pitch);
        float y=dy*cosX-part.z*sinX,z=dy*sinX+part.z*cosX;
        float cosZ=Mth.cos(roll),sinZ=Mth.sin(roll);
        float x=part.x*cosZ-y*sinZ;
        part.y=22.5F+part.x*sinZ+y*cosZ+bodyY;part.x=x+bodyX;part.z=z;
        part.xRot+=pitch;part.zRot+=roll;
    }
    private static void reach(ModelPart arm,float x,float y,float z){
        float dx=x-arm.x,dy=y-arm.y,dz=z-arm.z;
        float length=Mth.sqrt(dx*dx+dy*dy+dz*dz);
        arm.xRot=-(float)Math.acos(Mth.clamp(dy/length,-1,1));arm.yRot=(float)Math.atan2(-dx,-dz);arm.zRot=0;
        arm.yScale=length/2.7F;
    }
}
