package local.peepo.client;

import net.minecraft.util.Mth;

/** Keep the shared prop and both grips together inside the shoulders' comfortable reach. */
final class BoundedActionReach {
    static final float MAX_REACH=3.0F;
    static void limit(PeepoState s){
        var p=s.toolPose;
        float width=s.pumpkin?2.9F:2.3F,z=s.pumpkin?-2.6F:-1.4F;
        float y=-3.3F*Mth.cos(p.bodyPitch)-z*Mth.sin(p.bodyPitch);
        float shoulderZ=-3.3F*Mth.sin(p.bodyPitch)+z*Mth.cos(p.bodyPitch);
        float cos=Mth.cos(p.bodyRoll),sin=Mth.sin(p.bodyRoll);
        float centerX=-y*sin+p.bodyX,centerY=22.5F+y*cos+p.bodyY;
        float midX=(p.leftX+p.rightX)*.5F,midY=(p.leftY+p.rightY)*.5F,midZ=(p.leftZ+p.rightZ)*.5F;
        float dx=midX-centerX,dy=midY-centerY,dz=midZ-shoulderZ;
        float lengthSquared=dx*dx+dy*dy+dz*dz;
        if(lengthSquared<.00001F)return;
        float factor=1;
        for(int side=-1;side<=1;side+=2){
            float x=(side>0?p.leftX:p.rightX)-midX-side*width*cos;
            float h=(side>0?p.leftY:p.rightY)-midY-side*width*sin;
            float d=(side>0?p.leftZ:p.rightZ)-midZ;
            float dot=x*dx+h*dy+d*dz,c=x*x+h*h+d*d-MAX_REACH*MAX_REACH;
            factor=Math.min(factor,Mth.clamp((-dot+Mth.sqrt(Math.max(0,dot*dot-lengthSquared*c)))/lengthSquared,0,1));
        }
        dx*=factor-1;dy*=factor-1;dz*=factor-1;
        p.x+=dx;p.y+=dy;p.z+=dz;
        p.leftX+=dx;p.leftY+=dy;p.leftZ+=dz;p.rightX+=dx;p.rightY+=dy;p.rightZ+=dz;
    }
}
