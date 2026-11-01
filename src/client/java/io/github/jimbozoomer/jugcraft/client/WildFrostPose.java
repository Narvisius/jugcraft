package io.github.jimbozoomer.jugcraft.client;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
/** Original Frostmaw sleeping pose, quadruped/biped gait and hand/crystal controls, statically adapted from supplied data. */
final class WildFrostPose {
 private final java.util.function.Function<String,ModelPart> lookup;
 WildFrostPose(ModelPart root){lookup=RecoveredWildlifeModel.recoveredLookup(root);}
 private ModelPart b(String name){return lookup.apply(name);}
 private void scale(ModelPart p,float n){p.xScale=p.yScale=p.zScale=n;}
 private float wave(float speed,float degree,boolean invert,float offset,float weight,float time,float amount){return (float)(Math.cos(time*speed+offset)*degree*amount+weight*amount)*(invert?-1:1);}
 private void walk(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.xRot+=wave(s,d,i,o,w,t,a);}
 private void swing(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.yRot+=wave(s,d,i,o,w,t,a);}
 private void flap(ModelPart p,float s,float d,boolean i,float o,float w,float t,float a){p.zRot+=wave(s,d,i,o,w,t,a);}
 private void bob(ModelPart p,float s,float d,boolean bounce,float t,float a){float n=(float)Math.sin(t*s)*a*d;p.y+=bounce?-Math.abs(n):n-a*d;}
 void scale(ModelPart p,float x,float y,float z){p.xScale=x;p.yScale=y;p.zScale=z;}
 void faceTarget(float yaw,float pitch,float divisor,ModelPart p){p.yRot+=yaw/(57.295776F*divisor);p.xRot+=pitch/(57.295776F*divisor);}
 void apply(boolean active,boolean hasCrystal,int action,float actionTicks,float frame,float limbSwing,float limbSwingAmount,float headYaw,float headPitch){float globalSpeed = 0.5f;
        float globalHeightQuad = 1.2f * (1.0f - b("standUpController").x);
        float globalDegreeQuad = 0.8f * (1.0f - b("standUpController").x);
        float frontDegree = 1.1f;
        float frontOffset = 1.5707964f;
        float globalHeightBi = 1.0f * b("standUpController").x;
        float globalDegreeBi = 1.0f * b("standUpController").x;
        float lookLimit = 50.0f;
        if (headYaw > lookLimit) {
            headYaw = lookLimit;
        }
        if (headYaw < -lookLimit) {
            headYaw = -lookLimit;
        }
        if (action == 3) {
            headYaw /= frame;
            headPitch /= frame;
        }
        b("waist").z += 10.0f;
        if (active) {
            if (action != 6 && action != 7 && action != 8) {
                boolean frozen;
                faceTarget(headYaw * (1.0f - b("standUpController").x), headPitch, 1.0f, b("headJoint"));
                float yawAmount = headYaw / 57.295776f;
                b("headJoint").zRot += yawAmount * b("standUpController").x;
                bob(b("waist"), globalSpeed, globalHeightQuad * 3.0f, false, limbSwing, limbSwingAmount);
                walk(b("waist"), globalSpeed, globalHeightQuad * 0.12f, false, frontOffset, 0.08f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("headJoint"), globalSpeed, globalHeightQuad * 0.12f, true, frontOffset + 0.4f, 0.08f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("legLeftJoint"), globalSpeed, globalHeightQuad * 0.12f, true, frontOffset, 0.08f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("legRightJoint"), globalSpeed, globalHeightQuad * 0.12f, true, frontOffset, 0.08f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("armLeftJoint"), globalSpeed, globalHeightQuad * 0.12f, true, frontOffset, 0.08f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("armRightJoint"), globalSpeed, globalHeightQuad * 0.12f, true, frontOffset, 0.08f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                swing(b("waist"), 0.5f * globalSpeed, globalDegreeQuad * 0.2f, true, 0.0f, 0.0f, limbSwing, limbSwingAmount);
                swing(b("headJoint"), 0.5f * globalSpeed, globalDegreeQuad * 0.2f, false, 0.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("waist"), 0.5f * globalSpeed, 0.15f * globalHeightQuad, false, 1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("legLeft1"), 0.5f * globalSpeed, 0.15f * globalHeightQuad, true, 1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("legRight1"), 0.5f * globalSpeed, 0.15f * globalHeightQuad, true, 1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("chest"), 0.5f * globalSpeed, 0.15f * globalHeightQuad, true, 1.0f, 0.0f, limbSwing, limbSwingAmount);
                swing(b("legLeft1"), 0.5f * globalSpeed, 0.2f * globalDegreeQuad, false, 0.0f, 0.6f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                swing(b("legRight1"), 0.5f * globalSpeed, 0.2f * globalDegreeQuad, false, 0.0f, -0.6f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("legLeft1"), 0.5f * globalSpeed, 0.7f * globalDegreeQuad, false, 0.0f, 0.4f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("legRight1"), 0.5f * globalSpeed, 0.7f * globalDegreeQuad, true, 0.0f, -0.4f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("legLeft2"), 0.5f * globalSpeed, 0.6f * globalDegreeQuad, false, -1.8f, 0.2f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("legRight2"), 0.5f * globalSpeed, 0.6f * globalDegreeQuad, true, -1.8f, -0.2f * (1.0f - b("standUpController").x), limbSwing, limbSwingAmount);
                walk(b("leftFoot"), 0.5f * globalSpeed, 0.4f * globalDegreeQuad, false, -1.5f, 0.4f * globalDegreeQuad, limbSwing, limbSwingAmount);
                walk(b("rightFoot"), 0.5f * globalSpeed, 0.4f * globalDegreeQuad, true, -1.5f, -0.4f * globalDegreeQuad, limbSwing, limbSwingAmount);
                swing(b("chest"), 0.5f * globalSpeed, 0.3f * globalDegreeQuad * frontDegree, false, frontOffset, 0.0f, limbSwing, limbSwingAmount);
                swing(b("headJoint"), 0.5f * globalSpeed, 0.3f * globalDegreeQuad * frontDegree, true, frontOffset, 0.0f, limbSwing, limbSwingAmount);
                swing(b("armLeft2"), 0.5f * globalSpeed, 0.3f * globalDegreeQuad * frontDegree, true, frontOffset, 0.4f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                swing(b("armRight2"), 0.5f * globalSpeed, 0.3f * globalDegreeQuad * frontDegree, true, frontOffset, -0.4f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                flap(b("chest"), 0.5f * globalSpeed, 0.1f * globalDegreeQuad * frontDegree, true, frontOffset + 1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("armLeftJoint"), 0.5f * globalSpeed, 0.1f * globalDegreeQuad * frontDegree, false, frontOffset + 1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("armRightJoint"), 0.5f * globalSpeed, 0.1f * globalDegreeQuad * frontDegree, false, frontOffset + 1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("headJoint"), 0.5f * globalSpeed, 0.1f * globalDegreeQuad * frontDegree, false, frontOffset + 1.0f, 0.0f, limbSwing, limbSwingAmount);
                walk(b("armLeftJoint"), 0.5f * globalSpeed, 0.4f * globalDegreeQuad * frontDegree, true, frontOffset, -0.7f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                walk(b("armRightJoint"), 0.5f * globalSpeed, 0.4f * globalDegreeQuad * frontDegree, false, frontOffset, 0.7f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                walk(b("armLeftJoint2"), 0.5f * globalSpeed, 0.4f * globalDegreeQuad * frontDegree, true, frontOffset + 2.0f, 0.6f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                walk(b("armRightJoint2"), 0.5f * globalSpeed, 0.4f * globalDegreeQuad * frontDegree, false, frontOffset + 2.0f, -0.6f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                walk(b("leftHand"), 0.5f * globalSpeed, 0.5f * globalDegreeQuad * frontDegree, false, frontOffset + 1.5f, 0.05f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                walk(b("rightHand"), 0.5f * globalSpeed, 0.5f * globalDegreeQuad * frontDegree, true, frontOffset + 1.5f, 0.05f * globalDegreeQuad * frontDegree, limbSwing, limbSwingAmount);
                flap(b("root"), 0.5f * globalSpeed, 0.1f * globalHeightBi, true, -0.7f, 0.0f, limbSwing, limbSwingAmount);
                bob(b("waist"), globalSpeed, globalHeightBi * 3.0f, false, limbSwing, limbSwingAmount);
                walk(b("waist"), globalSpeed, 0.05f * globalHeightBi, false, -1.0f, 0.1f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("legLeftJoint"), globalSpeed, 0.05f * globalHeightBi, true, -1.0f, 0.1f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("legRightJoint"), globalSpeed, 0.05f * globalHeightBi, true, -1.0f, 0.1f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("chest"), globalSpeed, 0.05f * globalHeightBi, false, -2.0f, 0.05f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("headJoint"), globalSpeed, 0.1f * globalHeightBi, true, -2.0f, -0.1f * b("standUpController").x, limbSwing, limbSwingAmount);
                flap(b("chest"), 0.5f * globalSpeed, 0.2f * globalHeightBi, false, -1.0f, 0.0f, limbSwing, limbSwingAmount);
                flap(b("headJoint"), 0.5f * globalSpeed, -0.2f * globalHeightBi, false, -1.0f, 0.0f, limbSwing, limbSwingAmount);
                walk(b("armLeftJoint"), 0.5f * globalSpeed, 0.2f * globalDegreeBi * frontDegree, false, frontOffset, 0.4f * globalDegreeBi, limbSwing, limbSwingAmount);
                walk(b("armLeftJoint2"), 0.5f * globalSpeed, 0.2f * globalDegreeBi * frontDegree, true, frontOffset + 2.0f, 0.6f * globalDegreeBi, limbSwing, limbSwingAmount);
                walk(b("leftHand"), 0.5f * globalSpeed, 0.2f * globalDegreeBi * frontDegree, true, frontOffset + 1.5f, 0.05f * globalDegreeBi, limbSwing, limbSwingAmount);
                walk(b("armRightJoint"), 0.5f * globalSpeed, 0.2f * globalDegreeBi * frontDegree, true, frontOffset, -0.4f * globalDegreeBi, limbSwing, limbSwingAmount);
                walk(b("armRightJoint2"), 0.5f * globalSpeed, 0.2f * globalDegreeBi * frontDegree, false, frontOffset + 2.0f, -0.6f * globalDegreeBi, limbSwing, limbSwingAmount);
                walk(b("rightHand"), 0.5f * globalSpeed, 0.2f * globalDegreeBi * frontDegree, false, frontOffset + 1.5f, -0.05f * globalDegreeBi, limbSwing, limbSwingAmount);
                swing(b("legLeft1"), 0.5f * globalSpeed, 0.2f * globalDegreeBi, false, 0.0f, 0.6f * b("standUpController").x, limbSwing, limbSwingAmount);
                swing(b("legRight1"), 0.5f * globalSpeed, 0.2f * globalDegreeBi, false, 0.0f, -0.6f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("legLeft1"), 0.5f * globalSpeed, 0.7f * globalDegreeBi, false, 0.0f, 0.4f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("legRight1"), 0.5f * globalSpeed, 0.7f * globalDegreeBi, true, 0.0f, -0.4f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("legLeft2"), 0.5f * globalSpeed, 0.6f * globalDegreeBi, false, -1.8f, 0.2f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("legRight2"), 0.5f * globalSpeed, 0.6f * globalDegreeBi, true, -1.8f, -0.2f * b("standUpController").x, limbSwing, limbSwingAmount);
                walk(b("leftFoot"), 0.5f * globalSpeed, 0.4f * globalDegreeBi, false, -1.5f, 0.4f * globalDegreeBi, limbSwing, limbSwingAmount);
                walk(b("rightFoot"), 0.5f * globalSpeed, 0.4f * globalDegreeBi, true, -1.5f, -0.4f * globalDegreeBi, limbSwing, limbSwingAmount);
                
                frozen = false;
                if (!(frozen || action == 10 && actionTicks >= 118 || action == 12)) {
                    walk(b("waist"), 0.08f, 0.05f, false, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("headJoint"), 0.08f, 0.05f, true, 0.8f, 0.0f, frame, 1.0f);
                    walk(b("legRightJoint"), 0.08f, 0.05f, true, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("legLeftJoint"), 0.08f, 0.05f, true, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("armLeftJoint"), 0.08f, 0.05f, true, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("armRightJoint"), 0.08f, 0.05f, true, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("armLeftJoint2"), 0.08f, 0.07f, true, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("armRightJoint2"), 0.08f, 0.07f, true, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("leftHand"), 0.08f, 0.07f, false, 0.0f, 0.0f, frame, 1.0f);
                    walk(b("rightHand"), 0.08f, 0.07f, false, 0.0f, 0.0f, frame, 1.0f);
                    b("armLeftJoint").z = (float)((double)b("armLeftJoint").z + (double)1.8f * Math.cos(frame * 0.08f));
                    b("armRightJoint").z = (float)((double)b("armRightJoint").z + (double)1.8f * Math.cos(frame * 0.08f));
                    b("armLeftJoint").y = (float)((double)b("armLeftJoint").y - (double)0.4f * Math.cos(frame * 0.08f));
                    b("armRightJoint").y = (float)((double)b("armRightJoint").y - (double)0.4f * Math.cos(frame * 0.08f));
                }
            }
            if (action != 12) {
                b("eyeLidRight").visible = false;
                b("eyeLidLeft").visible = false;
            }
        } else if (action != 6 && action != 7) {
            b("eyeLidLeft").visible = true;
            b("eyeLidRight").visible = true;
            b("root").zRot += 0.9f;
            b("root").x -= 20.0f;
            b("chest").zRot -= 0.1f;
            b("chest").xRot = (float)((double)b("chest").xRot + 0.2);
            b("chest").yRot = (float)((double)b("chest").yRot + 0.2);
            b("headJoint").zRot -= 0.3f;
            b("armRightJoint").zRot -= 0.8f;
            b("armRightJoint").xRot += 0.2f;
            b("armRightJoint").yRot -= 0.5f;
            b("armRightJoint").y += 8.0f;
            b("armRight2").yRot += 0.2f;
            b("armLeftJoint").xRot -= 1.3f;
            b("armLeft1").xRot -= 0.8f;
            b("armLeftJoint2").xRot += 1.3f;
            b("armLeftJoint2").zRot += 0.3f;
            b("armRightJoint2").yRot += 0.5f;
            b("leftHand").xRot -= 0.65f;
            b("handController").x += 0.8f;
            b("rightHand").xRot -= 1.7f;
            b("rightHand").yRot += 0.8f;
            b("rightHand").zRot -= 3.3f;
            b("handController").y += 0.8f;
            b("legLeftJoint").xRot += 0.7f;
            b("legLeft1").xRot -= 0.6f;
            b("legLeft2").xRot -= 0.6f;
            b("legLeft2").zRot += 0.2f;
            b("legLeftJoint").yRot += 0.9f;
            b("legLeftJoint").zRot += 0.2f;
            b("legLeft2").yRot += 0.3f;
            b("legRightJoint").yRot -= 0.4f;
            b("legRight1").xRot += 1.1f;
            b("legRight1").zRot += 0.45f;
            b("legRight2").xRot -= 0.3f;
            
            float chestScale = (float)(1.05 + (double)0.05f * Math.cos(frame * 0.05f));
            scale(b("chest"),chestScale, chestScale, chestScale);
            scale(b("backHair"),chestScale, chestScale, chestScale);
            b("backHair").z = (float)((double)b("backHair").z - ((double)chestScale - 1.05) * 20.0);
            b("headJoint").xRot += (float)((double)0.04f * Math.cos(frame * 0.05f - 1.0f));
        }
        b("jawJoint").xRot = (float)((double)b("jawJoint").xRot + 0.08 * (double)b("roarController").x * Math.cos(2.0f * frame));
        scale(b("headBack"),1.0f, 1.0f - b("roarController").y, 1.0f);
        b("rightFingersJoint").xRot = (float)((double)b("rightFingersJoint").xRot - (double)b("handController").y * Math.PI / 2.0);
        b("rightFingers").xRot = (float)((double)b("rightFingers").xRot - (double)b("handController").y * Math.PI / 2.0);
        b("rightThumb").yRot += (float)((double)b("handController").y * Math.PI);
        b("rightThumb").zRot += b("handController").y * 0.7f;
        b("rightThumb").z -= b("handController").y * 6.0f;
        b("leftFingersJoint").xRot = (float)((double)b("leftFingersJoint").xRot - (double)b("handController").x * Math.PI / 2.0);
        b("leftFingers").xRot = (float)((double)b("leftFingers").xRot - (double)b("handController").x * Math.PI / 2.0);
        b("leftThumb").yRot -= (float)((double)b("handController").x * Math.PI);
        b("leftThumb").zRot -= b("handController").x * 0.7f;
        b("leftThumb").z -= b("handController").x * 6.0f;
        b("iceCrystalJoint").xRot = -b("headJoint").xRot - b("chest").xRot - b("waist").xRot - b("root").xRot + 0.9f;
        b("iceCrystal").y = (float)((double)b("iceCrystal").y + (2.0 * Math.cos(0.15f * frame) + 5.0));
        scale(b("iceCrystal"),2.0f);
        b("iceCrystal").zRot += (float)Math.PI;
        b("iceCrystal").y -= 6.0f * (1.0f - Math.min(b("jawJoint").xRot - b("jawJoint").getInitialPose().xRot(), 1.0f));
        b("iceCrystalHand").y = (float)((double)b("iceCrystalHand").y - ((double)1.7f * Math.cos(0.1f * frame) - 1.0));
        b("iceCrystalHand").yRot -= frame * 0.05f;
        b("iceCrystalHand").zRot = (float)((double)b("iceCrystalHand").zRot + Math.PI);
        if (hasCrystal) {
            b("iceCrystal").visible = action != 6 || actionTicks > 28;
            b("iceCrystalHand").visible = !active;
        } else {
            b("iceCrystal").visible = false;
            b("iceCrystalHand").visible = false;
        }
    b("armRightJoint").xScale/=b("chest").xScale;b("armRightJoint").yScale/=b("chest").yScale;b("armRightJoint").zScale/=b("chest").zScale;b("armLeftJoint").xScale/=b("chest").xScale;b("armLeftJoint").yScale/=b("chest").yScale;b("armLeftJoint").zScale/=b("chest").zScale;b("headJoint").xScale/=b("chest").xScale;b("headJoint").yScale/=b("chest").yScale;b("headJoint").zScale/=b("chest").zScale;b("backHair").xScale/=b("chest").xScale;b("backHair").yScale/=b("chest").yScale;b("backHair").zScale/=b("chest").zScale;
 }
}
