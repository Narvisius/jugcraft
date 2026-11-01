package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
/** Original Wroughtnaut resting/active pose and authored gait, statically adapted from supplied data. */
final class WildWroughtPose {
 private final java.util.function.Function<String,ModelPart> lookup;
 WildWroughtPose(ModelPart root){lookup=root.createPartLookup();}
 private ModelPart b(String name){return lookup.apply(name);}
 private void scale(ModelPart p,float n){p.xScale=p.yScale=p.zScale=n;}
 private float wave(float speed,float degree,boolean invert,float offset,float weight,float time,float amount){return (float)(Math.cos(time*speed+offset)*degree*amount+weight*amount)*(invert?-1:1);}
 private void walk(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.xRot+=wave(s,d,i,o,w,t,a);}
 private void swing(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.yRot+=wave(s,d,i,o,w,t,a);}
 private void flap(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.zRot+=wave(s,d,i,o,w,t,a);}
 private void bob(ModelPart p,float s,float d,boolean bounce,float t,float a){float n=(float)Math.sin(t*s)*a*d;p.y+=bounce?-Math.abs(n):n-a*d;}
 void apply(boolean active,int action,float frame,float walkAmount,float headYaw,float headPitch){float limbSwing=0,limbSwingAmount=0;
        
        if (active) {
            b("eyeLeft").visible = true;
            b("eyeRight").visible = true;
        } else {
            b("eyeLeft").visible = false;
            b("eyeRight").visible = false;
        }
        if (action != 6 && action != 7) {
            if (active || action == 9) {
                b("shoulderLeft").zRot = (float)((double)b("shoulderLeft").zRot - 0.4);
                b("shoulderRight").zRot = (float)((double)b("shoulderRight").zRot + 0.35);
                b("shoulderLeft").yRot = (float)((double)b("shoulderLeft").yRot - 0.3);
                b("shoulderRight").yRot = (float)((double)b("shoulderRight").yRot + 0.2);
                b("upperArmLeft").zRot = (float)((double)b("upperArmLeft").zRot + 0.5);
                b("upperArmRight").zRot = (float)((double)b("upperArmRight").zRot + 0.5);
                b("upperArmLeft").yRot = (float)((double)b("upperArmLeft").yRot - 0.5);
                b("upperArmRight").yRot = (float)((double)b("upperArmRight").yRot + 0.3);
                b("lowerArmLeft").zRot = (float)((double)b("lowerArmLeft").zRot + 1.5);
                b("lowerArmRight").zRot = (float)((double)b("lowerArmRight").zRot - 1.3);
                b("lowerArmLeft").yRot = (float)((double)b("lowerArmLeft").yRot + 0.3);
                b("lowerArmRight").yRot = (float)((double)b("lowerArmRight").yRot + 0.7);
                b("handRight").zRot = (float)((double)b("handRight").zRot + 0.3);
                b("handRight").xRot = (float)((double)b("handRight").xRot - 0.2);
                b("handLeftJoint").xRot = (float)((double)b("handLeftJoint").xRot + 1.6);
                b("handLeftJoint").zRot = (float)((double)b("handLeftJoint").zRot - 0.5);
                b("axeHandle").yRot = (float)((double)b("axeHandle").yRot + 0.8);
                float y = Mth.wrapDegrees((float)headYaw) / 57.295776f;
                b("head").yRot += Mth.clamp((float)y, (float)-0.7853982f, (float)0.7853982f);
                b("neck").xRot = b("neck").xRot + (headPitch > 0.0f ? headPitch * 1.4f : headPitch) / 57.295776f;
            } else {
                b("shoulderLeft").zRot = (float)((double)b("shoulderLeft").zRot - 0.4);
                b("shoulderRight").zRot = (float)((double)b("shoulderRight").zRot + 0.4);
                b("shoulderLeft").yRot = (float)((double)b("shoulderLeft").yRot - 0.4);
                b("shoulderRight").yRot = (float)((double)b("shoulderRight").yRot + 0.4);
                b("upperArmLeft").zRot = (float)((double)b("upperArmLeft").zRot + 0.5);
                b("upperArmRight").zRot = (float)((double)b("upperArmRight").zRot + 0.5);
                b("upperArmLeft").yRot = (float)((double)b("upperArmLeft").yRot - 0.5);
                b("upperArmRight").yRot = (float)((double)b("upperArmRight").yRot + 0.5);
                b("lowerArmLeft").zRot = (float)((double)b("lowerArmLeft").zRot + 1.5);
                b("lowerArmRight").zRot = (float)((double)b("lowerArmRight").zRot - 1.5);
                b("lowerArmLeft").yRot += 1.0f;
                b("lowerArmRight").yRot += 1.0f;
                b("axeBase").y += 30.0f;
                b("axeBase").z = (float)((double)b("axeBase").z + 3.5);
                b("axeBase").yRot = (float)((double)b("axeBase").yRot + 0.35);
                b("handRight").xRot = (float)((double)b("handRight").xRot - 0.9);
                b("handRightJoint").zRot = (float)((double)b("handRightJoint").zRot - 0.88);
                b("handRightJoint").xRot = (float)((double)b("handRightJoint").xRot - 0.2);
                b("handLeftJoint").yRot = (float)((double)b("handLeftJoint").yRot - 0.3);
                b("handLeftJoint").xRot = (float)((double)b("handLeftJoint").xRot + 0.8);
                b("neck").xRot = (float)((double)b("neck").xRot + 0.5);
                b("rootBox").y -= 5.0f;
                b("thighRightJoint").xRot = (float)((double)b("thighRightJoint").xRot + 0.35);
                b("thighRightJoint").yRot = (float)((double)b("thighRightJoint").yRot + 0.5);
                b("thighLeftJoint").xRot = (float)((double)b("thighLeftJoint").xRot + 0.35);
                b("thighLeftJoint").yRot = (float)((double)b("thighLeftJoint").yRot - 0.5);
                b("calfRightJoint").xRot = (float)((double)b("calfRightJoint").xRot - 0.6);
                b("calfLeftJoint").xRot = (float)((double)b("calfLeftJoint").xRot - 0.6);
                b("footLeft").xRot = (float)((double)b("footLeft").xRot + 0.25);
                b("footRight").xRot = (float)((double)b("footRight").xRot + 0.25);
            }
        }
        
        limbSwing = (float)((Math.PI * 4 * (double)frame - (double)(30.0f * Mth.sin((float)((float)(0.3141592653589793 * (double)(frame - 9.0f))))) - 84.82300164692441) / (Math.PI * 4)) + 5.0f;
        float walkTimerInterp=walkAmount*10;
        limbSwingAmount = (float)Math.pow(Mth.sin((float)((float)((double)walkTimerInterp * Math.PI * 0.05))), 2.0);
        float globalSpeed = 0.15707964f;
        float globalDegree = 0.8f;
        float height = 2.0f;
        b("waist").z = (float)((double)b("waist").z - (double)(limbSwingAmount * 3.0f) * Math.pow(Math.sin(globalSpeed * (frame - 13.0f)), 2.0));
        bob(b("waist"), 2.0f * globalSpeed, 1.0f * height, false, limbSwing, limbSwingAmount);
        swing(b("waist"), 1.0f * globalSpeed, 0.3f * globalDegree, false, 0.0f, 0.0f, limbSwing, limbSwingAmount);
        swing(b("stomachJoint"), 1.0f * globalSpeed, 0.6f * globalDegree, true, 0.0f, 0.0f, limbSwing, limbSwingAmount);
        swing(b("head"), 1.0f * globalSpeed, 0.3f * globalDegree, false, 0.0f, 0.0f, limbSwing, limbSwingAmount);
        swing(b("thighLeftJoint"), 1.0f * globalSpeed, 0.4f * globalDegree, true, 0.0f, 0.5f, limbSwing, limbSwingAmount);
        walk(b("thighLeftJoint"), 1.0f * globalSpeed, 0.4f * globalDegree, false, 0.0f, 0.3f * globalDegree, limbSwing, limbSwingAmount);
        walk(b("calfLeftJoint"), 1.0f * globalSpeed, 0.5f * globalDegree, false, -2.2f, 0.1f * globalDegree, limbSwing, limbSwingAmount);
        walk(b("footLeftJoint"), 1.0f * globalSpeed, 0.4f * globalDegree, false, -2.1f, 0.26f * globalDegree, limbSwing, limbSwingAmount);
        swing(b("thighRightJoint"), 1.0f * globalSpeed, 0.4f * globalDegree, true, 0.0f, -0.5f, limbSwing, limbSwingAmount);
        walk(b("thighRightJoint"), 1.0f * globalSpeed, 0.4f * globalDegree, true, 0.0f, -0.3f * globalDegree, limbSwing, limbSwingAmount);
        walk(b("calfRightJoint"), 1.0f * globalSpeed, 0.5f * globalDegree, true, -2.2f, -0.1f * globalDegree, limbSwing, limbSwingAmount);
        walk(b("footRightJoint"), 1.0f * globalSpeed, 0.4f * globalDegree, true, -2.1f, -0.26f * globalDegree, limbSwing, limbSwingAmount);
        walk(b("groinFront"), 2.0f * globalSpeed, 0.16000001f, true, -0.5f, 0.1f, limbSwing, limbSwingAmount);
        walk(b("groinBack"), 2.0f * globalSpeed, 0.16000001f, false, -0.5f, 0.1f, limbSwing, limbSwingAmount);
        walk(b("neck"), 2.0f * globalSpeed, 0.080000006f, true, -0.5f, 0.1f, limbSwing, limbSwingAmount);
        flap(b("shoulderLeft"), 2.0f * globalSpeed, 0.040000003f, false, -0.5f, 0.0f, limbSwing, limbSwingAmount);
        flap(b("shoulderRight"), 2.0f * globalSpeed, 0.040000003f, true, -0.5f, 0.0f, limbSwing, limbSwingAmount);
        swing(b("shoulderLeft"), 1.0f * globalSpeed, 0.080000006f, true, 0.0f, -0.3f, limbSwing, limbSwingAmount);
        swing(b("shoulderRight"), 1.0f * globalSpeed, 0.080000006f, true, 0.0f, -0.3f, limbSwing, limbSwingAmount);
        flap(b("upperArmLeftJoint"), 2.0f * globalSpeed, 0.040000003f, true, -1.0f, 0.0f, limbSwing, limbSwingAmount);
        flap(b("upperArmRightJoint"), 2.0f * globalSpeed, 0.040000003f, true, -1.0f, 0.0f, limbSwing, limbSwingAmount);
        flap(b("handLeft"), 2.0f * globalSpeed, 0.2f, false, -0.5f, 0.5f, limbSwing, limbSwingAmount);
        walk(b("lowerArmLeftJoint"), 2.0f * globalSpeed, 0.1f, true, -0.5f, 0.0f, limbSwing, limbSwingAmount);
        walk(b("handLeft"), 2.0f * globalSpeed, 0.2f, false, -0.5f, 0.6f, limbSwing, limbSwingAmount);
        b("lowerArmLeftJoint").yRot -= 0.65f * limbSwingAmount;
        b("swordJoint").x += 2.0f;
        b("swordJoint").z -= 3.0f;
        b("swordJoint").yRot = (float)((double)b("swordJoint").yRot + 1.3);
        b("swordJoint").xRot = (float)((double)b("swordJoint").xRot - 0.5);
        b("swordJoint").zRot = (float)((double)b("swordJoint").zRot - 0.5);
        b("sword").x -= 8.0f;
        b("sword").z = (float)((double)b("sword").z - 5.5);
        b("sword").y -= 5.0f;
        scale(b("sword"),1.7f);
    
 }
}
