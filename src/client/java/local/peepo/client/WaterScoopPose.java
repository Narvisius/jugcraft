package local.peepo.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A one-handed scoop: the container follows a fixed-length arm, never the source center. */
final class WaterScoopPose {
    static void prepare(PeepoState s,float t){
        var p=s.toolPose;
        float bend=t<.45F?smooth(t/.45F):t<.6F?1:1-smooth((t-.6F)/.4F);
        p.bodyPitch=.95F*bend;p.bodyRoll=p.bodyX=p.bodyY=p.zRot=0;
        p.xRot=-.8F*bend;
        arm(s,false,null);arm(s,true,null);
        // Grip near the rim/neck while the bottom dips into the water, then lifts upright.
        p.x=p.rightX;p.y=p.rightY+1.45F*Mth.cos(p.xRot);p.z=p.rightZ+1.45F*Mth.sin(p.xRot);
    }
    static void apply(PeepoState s,ModelPart left,ModelPart right,ModelPart head,
                      ModelPart leftLeg,ModelPart rightLeg,ModelPart[] torso){
        var p=s.toolPose;
        head.yRot=0;head.xRot=.08F;
        for(var part:torso)CompanionWorkPose.lean(part,p.bodyPitch,0,0,0);
        arm(s,false,right);arm(s,true,left);
        leftLeg.xRot=.12F*p.bodyPitch;rightLeg.xRot=-.12F*p.bodyPitch;
        leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;
    }
    private static void arm(PeepoState s,boolean left,ModelPart arm){
        var p=s.toolPose;float bend=p.bodyPitch/.95F;
        float x=(left?1:-1)*(s.pumpkin?3.69F:3.21F),z=s.pumpkin?-1.6F:-.8F;
        float cos=Mth.cos(p.bodyPitch),sin=Mth.sin(p.bodyPitch);
        float y=22.5F-3.3F*cos-z*sin;z=-3.3F*sin+z*cos;
        float pitch=left?.1F+.55F*bend:-1.30F+.30F*bend;
        float roll=left?-.18F-.22F*bend:.08F;
        float handX=x-2.7F*Mth.sin(roll)*Mth.cos(pitch);
        float handY=y+2.7F*Mth.cos(roll)*Mth.cos(pitch);
        float handZ=z+2.7F*Mth.sin(pitch);
        if(left){p.leftX=handX;p.leftY=handY;p.leftZ=handZ;}
        else {p.rightX=handX;p.rightY=handY;p.rightZ=handZ;}
        if(arm!=null){
            arm.x=x;arm.y=y;arm.z=z;arm.xRot=pitch;arm.yRot=0;arm.zRot=roll;
            arm.xScale=arm.yScale=arm.zScale=1;
        }
    }
    private static float smooth(float t){return t*t*(3-2*t);}
}
