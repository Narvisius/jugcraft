package local.peepo.client;

import local.peepo.WorkAnimation;
import net.minecraft.util.Mth;

/** One reusable pose per render state. Props and hands share these exact transforms. */
final class MachineWorkClip {
    static final float VALVE_RADIUS=2.6F, VALVE_Y=19.6F, LEVER_GRIP=-3.4F, WRENCH_GRIP=2.25F;
    float x,y,z,xRot,zRot,leftX,leftY,leftZ,rightX,rightY,rightZ;
    float bodyPitch,bodyRoll,bodyX,bodyY;
    static final float HOE_SCALE=.65F, HOE_REACH=11.313708F*HOE_SCALE;

    void shear(float phase,float flankY,float flankZ){
        // Move the real shears over a small patch of fleece; two hands share its handles.
        float snip=Mth.sin(phase*4),sweep=Mth.sin(phase);
        x=sweep*1.15F;y=flankY+Mth.cos(phase)*.7F;z=flankZ+2.3F;
        xRot=1.2F+snip*.14F;zRot=0;
        bodyPitch=.10F+Math.max(0,snip)*.10F;bodyRoll=sweep*.06F;bodyX=sweep*.25F;bodyY=0;
        leftX=x+.55F;rightX=x-.55F;leftY=rightY=y;leftZ=rightZ=z;
    }

    void harvest(float phase,float groundY,float cropZ){
        float t=Mth.clamp(phase/Mth.TWO_PI,0,1);
        float lift=smooth(Math.min(1,t/.55F));
        float strike=smooth(Mth.clamp((t-.65F)/.30F,0,1));
        x=0;zRot=0;
        // Native diagonal hoe: lift its head behind the brow, then drive it down into the crop.
        xRot=Mth.lerp(strike,Mth.lerp(lift,.65F,-.55F),2.30F);
        float contactY=groundY+HOE_REACH*Mth.cos(2.30F);
        float contactZ=cropZ+HOE_REACH*Mth.sin(2.30F);
        y=Mth.lerp(strike,Mth.lerp(lift,19.4F,15.8F),contactY);
        z=Mth.lerp(strike,-3.4F,contactZ);
        bodyPitch=Mth.lerp(strike,Mth.lerp(lift,.08F,-.22F),.56F);
        bodyRoll=0;bodyX=0;bodyY=Mth.lerp(strike,-lift*.25F,.65F);
        float c=Mth.cos(xRot),s=Mth.sin(xRot);
        leftX=.22F;rightX=-.22F;
        leftY=y+.65F*HOE_SCALE*c;rightY=y-.65F*HOE_SCALE*c;
        leftZ=z+.65F*HOE_SCALE*s;rightZ=z-.65F*HOE_SCALE*s;
    }

    void chop(float phase,float boardY,float boardZ){
        // Contact at the loop seam, a slow two-handed lift, then a short forceful downstroke.
        float t=phase/Mth.TWO_PI;
        float down=t<.12F?1:t<.70F?1-smooth((t-.12F)/.58F):t<.975F?smooth((t-.70F)/.275F):1;
        x=-1;z=Math.min(-3.8F,Math.max(-5.0F,boardZ+8.5F));
        y=Mth.lerp(down,16.8F,boardY-1.25F);xRot=down*Mth.HALF_PI;zRot=0;
        bodyPitch=-.12F+down*.46F;bodyRoll=0;bodyX=0;bodyY=-.20F+down*.45F;
        float c=Mth.cos(xRot),s=Mth.sin(xRot);
        // Stacked hands clasp the actual knife handle; both move with its rotation.
        leftX=x+.30F;rightX=x-.30F;
        leftY=y+.45F*c;rightY=y-.45F*c;
        leftZ=z+.45F*s;rightZ=z-.45F*s;
    }

    void evaluate(WorkAnimation clip,float phase,boolean pumpkin){
        x=0;y=20;z=-4.2F-(pumpkin?.65F:0);xRot=zRot=0;
        bodyPitch=bodyRoll=bodyX=bodyY=0;
        float gripY=0, spread=.34F;
        float gripSeparation=0;
        switch(clip){
            case VALVE -> {
                float effort=Mth.sin(phase), brace=1-Mth.cos(phase*2);
                y=VALVE_Y;zRot=effort*.65F;spread=VALVE_RADIUS;
                bodyRoll=-effort*.18F;bodyX=-effort*.30F;
                bodyPitch=.04F+brace*.04F;bodyY=brace*.08F;
            }
            case LEVER -> {
                float effort=Mth.sin(phase);
                y=22.3F;z=pumpkin?-4.65F:-4.4F;
                // Keep the enlarged grip in front of the machine face, including the costume.
                xRot=pumpkin?-.015F+effort*.165F:-.08F+effort*.24F;
                gripY=LEVER_GRIP;spread=.65F;
                bodyPitch=.10F+effort*.16F;
            }
            case MALLET -> {
                // Rest on contact, slow wind-up, quick strike, then hold contact through the loop seam.
                float t=phase/Mth.TWO_PI;
                float strike=t<.15F?1:t<.65F?1-smooth((t-.15F)/.5F):t<.8F?smooth((t-.65F)/.15F):1;
                // Lift the larger head above the brow, then bring the torso into the downstroke.
                y=16.8F+strike*2.8F;xRot=-.05F+strike*.65F;
                bodyPitch=-.12F+strike*.40F;bodyY=strike*.16F;
            }
            case WRENCH -> {
                y=18.5F;gripY=WRENCH_GRIP;
                // A deliberate tightening stroke followed by a shorter, faster ratchet return.
                float t=phase/Mth.TWO_PI;
                float turn=t<.7F?smooth(t/.7F):1-smooth((t-.7F)/.3F);
                zRot=-.90F+turn*1.8F;
                float effort=Mth.sin(zRot);
                bodyRoll=-zRot*.24F;bodyX=-effort*.45F;
                // Slide the far hand towards the jaws while the near hand supplies leverage.
                gripSeparation=effort*.55F;spread=.15F;
            }
            default -> {return;}
        }
        float cosX=Mth.cos(xRot), sinX=Mth.sin(xRot), cosZ=Mth.cos(zRot), sinZ=Mth.sin(zRot);
        // ModelPart applies Z * Y * X. These clips have no Y rotation.
        float leftGrip=gripY-gripSeparation,rightGrip=gripY+gripSeparation;
        leftX=x+spread*cosZ-leftGrip*cosX*sinZ;
        rightX=x-spread*cosZ-rightGrip*cosX*sinZ;
        leftY=y+spread*sinZ+leftGrip*cosX*cosZ;
        rightY=y-spread*sinZ+rightGrip*cosX*cosZ;
        leftZ=z+leftGrip*sinX;rightZ=z+rightGrip*sinX;
    }
    private static float smooth(float t){return t*t*(3-2*t);}
}
