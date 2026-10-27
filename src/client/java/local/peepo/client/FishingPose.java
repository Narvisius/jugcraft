package local.peepo.client;

import local.peepo.WorkAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Both hands and the smaller rod share one grip; feet dangle beyond the chosen dock edge. */
final class FishingPose {
    static final float ROD_SCALE=.85F;
    static float smooth(float t){t=Mth.clamp(t,0,1);return t*t*(3-2*t);}
    static void evaluate(PeepoState s){
        float t=s.fishTime;var p=s.toolPose;
        float angle=.85F+Mth.sin(s.ageInTicks*.08F)*.025F,lean=.06F;
        if(s.work==WorkAnimation.FISH_CAST){float back=smooth(t/8),swing=smooth((t-8)/12);angle=Mth.lerp(swing,Mth.lerp(back,.85F,-.55F),.85F);lean=Mth.lerp(swing,-.09F*back,.12F);}
        if(s.work==WorkAnimation.FISH_BITE){angle=.85F-.25F*smooth(t/10);lean=-.06F*smooth(t/10);}
        if(s.work==WorkAnimation.FISH_REEL){float pull=smooth(t/15);angle=.6F-.45F*pull;lean=-.18F*pull;}
        p.x=0;p.y=21.0F;p.z=s.pumpkin?-4.3F:-3.3F;p.xRot=angle;p.zRot=0;
        p.bodyPitch=lean;p.bodyY=1.35F;p.bodyX=p.bodyRoll=0;
    }
    static void apply(PeepoState s,ModelPart left,ModelPart right,ModelPart head,ModelPart ll,ModelPart rl,ModelPart[] torso){
        var p=s.toolPose;
        for(var part:torso)CompanionWorkPose.lean(part,p.bodyPitch,0,0,p.bodyY);
        head.yRot=0;head.xRot=s.work==WorkAnimation.FISH_REEL?-.10F:.06F;
        ll.y+=1.35F;rl.y+=1.35F;ll.z-=.65F;rl.z-=.65F;
        float kick=Mth.sin(s.ageInTicks*.15F)*.22F;
        ll.xRot=-1.05F+kick;rl.xRot=-1.05F-kick;ll.yRot=rl.yRot=ll.zRot=rl.zRot=0;
        left.x=s.pumpkin?2.9F:2.3F;right.x=-left.x;left.y=right.y=20.55F;left.z=right.z=s.pumpkin?-2.6F:-1.4F;
        CompanionWorkPose.reach(right,p.x-.35F,p.y+.3F*ROD_SCALE,p.z);
        CompanionWorkPose.reach(left,p.x+.35F,p.y-.7F*ROD_SCALE*Mth.cos(p.xRot),p.z-.7F*ROD_SCALE*Mth.sin(p.xRot));
    }
}
