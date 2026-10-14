package local.peepo.client;

import local.peepo.WorkAnimation;
import net.minecraft.util.Mth;

/** One reusable pose per render state. Props and hands share these exact transforms. */
final class MachineWorkClip {
    static final float VALVE_RADIUS=2.6F, VALVE_Y=19.6F, LEVER_GRIP=-3.4F, WRENCH_GRIP=2.25F;
    float x,y,z,xRot,zRot,leftX,leftY,leftZ,rightX,rightY,rightZ;
    float bodyPitch,bodyRoll,bodyX,bodyY;
    static final float HOE_SCALE=.65F, HOE_REACH=11.313708F*HOE_SCALE;

    void stir(float phase){
        // Ease through the near side, then lean into the far sweep. The loop stays continuous.
        float sweep=phase+.18F*Mth.sin(phase),side=Mth.cos(sweep),depth=Mth.sin(sweep);
        x=side*.90F;y=20+Mth.sin(sweep*2)*.12F;z=-3.4F+depth*.60F;
        xRot=-.2618F+depth*.065F;zRot=-side*.065F;
        bodyPitch=.10F+depth*.045F;bodyRoll=-side*.035F;
        bodyX=side*.10F;bodyY=(1-Mth.cos(sweep*2))*.045F;
        // One hand guides the top; the other pushes just below it on the same wooden shaft.
        grips(.175F,.33F,.325F);
    }

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
        float lift=smooth(Mth.clamp((t-.14F)/.46F,0,1));
        float gather=smooth(Math.min(1,t/.14F))*(1-lift);
        float strike=smooth(Mth.clamp((t-.60F)/.31F,0,1));
        float settle=smooth(Mth.clamp((t-.91F)/.09F,0,1));
        float follow=smooth(Mth.clamp((t-.65F)/.33F,0,1));
        float windup=lift*(1-strike);
        x=windup*.12F;zRot=0;
        // Gather low, sweep above the brow, then strike without a frozen pose at the top.
        xRot=Mth.lerp(strike,Mth.lerp(lift,.65F+gather*.20F,-.65F),2.30F-settle*.10F);
        // After impact, ease the handle back around the planted blade rather than through the soil.
        float contactY=groundY+HOE_REACH*Mth.cos(xRot);
        float contactZ=cropZ+HOE_REACH*Mth.sin(xRot);
        y=Mth.lerp(strike,19.4F+gather*.40F-lift*4.0F,contactY);
        z=Mth.lerp(strike,-3.4F-lift*.8F,contactZ);
        // Weight and shoulders follow the hands a little later, giving the downstroke some heft.
        bodyPitch=Mth.lerp(follow,.08F+gather*.10F-lift*.30F,.56F);
        bodyRoll=windup*.045F;bodyX=-windup*.18F;
        bodyY=Mth.lerp(follow,gather*.25F-lift*.22F,.56F);
        grips(0,.22F,-(.65F+windup*.12F)*HOE_SCALE);
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
                float t=phase/Mth.TWO_PI;
                // Rebound immediately after contact, gather into a long lift, then swing down.
                // Eased segments meet with zero velocity, including across the repeating seam.
                float lift=t<.16F?.16F*smooth(t/.16F):t<.70F?
                    Mth.lerp(smooth((t-.16F)/.54F),.16F,1):1-smooth((t-.70F)/.30F);
                y=20.0F-lift*4.1F;
                z-=(1-lift)*.9F+1.0F; // Keep the long head clear of the brow on the backswing.
                xRot=.82F-lift*1.20F;
                // Offset the two grips along the handle so the arms have different sweep angles.
                gripY=-.45F;gripSeparation=.45F;spread=.25F;
                float sway=Mth.sin(phase)*.16F;
                x=sway;zRot=sway*.25F;
                // The torso settles just after impact, rather than moving as one rigid piece with the arms.
                float follow=phase-.18F;
                float settle=(1+Mth.cos(follow))*.5F;
                bodyPitch=.05F+.23F*Mth.cos(follow);
                bodyY=settle*.20F;bodyRoll=-sway*.22F;bodyX=sway*.35F;
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
        grips(gripY,spread,gripSeparation);
    }
    void grips(float gripY,float spread,float gripSeparation){
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
