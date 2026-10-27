package local.peepo.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A deterministic, paid sixty-tick craft: gather, press together, then inspect the preview. */
public final class CraftingBenchPose {
    public float ticks,leftGather,rightGather,press,inspect;
    public float bodyPitch,bodyRoll,bodyY;
    public float leftX,leftY,leftZ,rightX,rightY,rightZ;
    public float ingredient0X,ingredient0Y,ingredient0Z,ingredient1X,ingredient1Y,ingredient1Z;
    public float ingredientScale,resultX,resultY,resultZ,resultScale,resultTilt;
    private static final float MAX_REACH=3.1F;

    public void evaluate(float elapsedTicks,boolean pumpkin){
        ticks=Mth.clamp(elapsedTicks,0,59.99F);
        leftGather=smooth(ticks/12);
        rightGather=smooth((ticks-12)/12);
        float pressing=ticks>24 && ticks<48?Mth.sin((ticks-24)/24*Mth.PI):0;
        press=pressing*pressing;
        inspect=smooth((ticks-48)/5);
        float workingZ=(float)(-local.peepo.CraftingTableJob.STANDING_OFFSET*16)-(pumpkin?.4F:0);
        bodyPitch=.25F+.20F*press-.15F*inspect;
        bodyRoll=(Mth.sin(leftGather*Mth.PI)-Mth.sin(rightGather*Mth.PI))*.025F;
        bodyY=0;

        // Place the ingredients near the table center, ahead of the inward-facing standing worker.
        ingredient0X=Mth.lerp(leftGather,2.5F,.85F);
        ingredient1X=Mth.lerp(rightGather,-2.5F,-.85F);
        ingredient0Y=23.72F-Mth.sin(leftGather*Mth.PI)*.65F;
        ingredient1Y=23.72F-Mth.sin(rightGather*Mth.PI)*.65F;
        ingredient0Z=Mth.lerp(leftGather,workingZ+.35F,workingZ);
        ingredient1Z=Mth.lerp(rightGather,workingZ+.35F,workingZ);
        ingredientScale=.17F*(1-smooth((ticks-45)/3));
        resultX=0;resultY=Mth.lerp(inspect,22.8F,20.65F);
        resultZ=Mth.lerp(inspect,workingZ,workingZ-.60F);
        resultScale=.20F*smooth((ticks-48)/2);
        resultTilt=-.12F+Mth.sin(Mth.clamp((ticks-53)/7,0,1)*Mth.PI)*.16F;

        // Grip the outside edges, leaving the small ingredient/result model visible between the hands.
        leftX=Mth.lerp(inspect,ingredient0X+.65F,1.35F);
        rightX=Mth.lerp(inspect,ingredient1X-.65F,-1.35F);
        float handY=22.80F+.50F*press;
        leftY=Mth.lerp(inspect,handY-Mth.sin(leftGather*Mth.PI)*.65F,resultY+.05F);
        rightY=Mth.lerp(inspect,handY-Mth.sin(rightGather*Mth.PI)*.65F,resultY+.05F);
        leftZ=Mth.lerp(inspect,ingredient0Z,resultZ);
        rightZ=Mth.lerp(inspect,ingredient1Z,resultZ);
    }

    public void clear(){
        ticks=leftGather=rightGather=press=inspect=0;
        bodyPitch=bodyRoll=bodyY=0;
        leftX=leftY=leftZ=rightX=rightY=rightZ=0;
        ingredient0X=ingredient0Y=ingredient0Z=ingredient1X=ingredient1Y=ingredient1Z=0;
        ingredientScale=resultX=resultY=resultZ=resultScale=resultTilt=0;
    }

    static void apply(PeepoState s,ModelPart left,ModelPart right,ModelPart head,
                      ModelPart leftLeg,ModelPart rightLeg,ModelPart[] torso){
        var p=s.craftPose;
        for(var part:torso)CompanionWorkPose.lean(part,p.bodyPitch,p.bodyRoll,0,0);
        head.yRot=0;head.xRot=.28F+.13F*p.press-.17F*p.inspect;
        // Keep the original hip joints and planted feet. Only the upper body bends about the hips.
        leftLeg.xRot=rightLeg.xRot=0;
        leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;

        // Keep the shoulders outside the shirt/costume so the gathering motion does not disappear in it.
        left.x=s.pumpkin?3.69F:3.05F;right.x=-left.x;
        left.y=right.y=19.2F;left.z=right.z=s.pumpkin?-2.8F:-1.8F;
        CompanionWorkPose.lean(left,p.bodyPitch,p.bodyRoll,0,p.bodyY);
        CompanionWorkPose.lean(right,p.bodyPitch,p.bodyRoll,0,p.bodyY);
        reach(left,p.leftX,p.leftY,p.leftZ);
        reach(right,p.rightX,p.rightY,p.rightZ);
    }

    private static void reach(ModelPart arm,float x,float y,float z){
        float dx=x-arm.x,dy=y-arm.y,dz=z-arm.z;
        float distance=Mth.sqrt(dx*dx+dy*dy+dz*dz);
        float scale=distance>MAX_REACH?MAX_REACH/distance:1;
        CompanionWorkPose.reach(arm,arm.x+dx*scale,arm.y+dy*scale,arm.z+dz*scale);
    }
    private static float smooth(float t){t=Mth.clamp(t,0,1);return t*t*(3-2*t);}
}
