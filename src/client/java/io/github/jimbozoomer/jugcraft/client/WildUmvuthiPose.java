package io.github.jimbozoomer.jugcraft.client;
import com.geckolib.renderer.base.BoneSnapshots;
import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Triple;
/** Original Umvuthi idle, belly bounce and per-feather controller equations. */
final class WildUmvuthiPose {
 private final BoneSnapshots snapshots;WildUmvuthiPose(BoneSnapshots snapshots){this.snapshots=snapshots;}
 private Bone getMowzieBone(String n){return new Bone(n);}
 private float getControllerValue(String n){return -snapshots.get(n).map(b->b.getTranslateX()).orElse(0F);}
 void apply(float frame,float yaw,float pitch,boolean alive){float headYaw=yaw/Mth.DEG_TO_RAD,headPitch=pitch/Mth.DEG_TO_RAD;

        
        Bone rightThigh = this.getMowzieBone("rightThigh");
        Bone leftThigh = this.getMowzieBone("leftThigh");
        float liftLegs = 0;
        leftThigh.addRotX(1.0f * liftLegs);
        rightThigh.addRotX(1.0f * liftLegs);
        leftThigh.addRotZ(1.5f * liftLegs);
        rightThigh.addRotZ(-1.5f * liftLegs);
        leftThigh.addRotY(-0.5f * liftLegs);
        rightThigh.addRotY(0.5f * liftLegs);
        if (alive) {
            Bone neck1 = this.getMowzieBone("neck");
            Bone neck2 = this.getMowzieBone("neck2");
            Bone head = this.getMowzieBone("head");
            Bone[] lookPieces = new Bone[]{neck1, neck2, head};

            float maxYaw = 140.0f;
            headYaw = Mth.clamp(headYaw,-maxYaw,maxYaw);
            for (Bone bone : lookPieces) {
                bone.addRotX(headPitch * ((float)Math.PI / 180) / (float)lookPieces.length);
                bone.addRotY(headYaw * ((float)Math.PI / 180) / (float)lookPieces.length);
            }
            Bone featherRaiseController = this.getMowzieBone("featherRaiseController");
            Bone rightFoot = this.getMowzieBone("rightFoot");
            Bone rightAnkle = this.getMowzieBone("rightAnkle");
            Bone rightCalf = this.getMowzieBone("rightCalf");
            Bone leftFoot = this.getMowzieBone("leftFoot");
            Bone leftAnkle = this.getMowzieBone("leftAnkle");
            Bone leftCalf = this.getMowzieBone("leftCalf");
            Bone rightHand = this.getMowzieBone("rightHand");
            Bone rightLowerArm = this.getMowzieBone("rightLowerArm");
            Bone rightArmJoint = this.getMowzieBone("rightArmJoint");
            Bone leftHand = this.getMowzieBone("leftHand");
            Bone leftLowerArm = this.getMowzieBone("leftLowerArm");
            Bone leftArmJoint = this.getMowzieBone("leftArmJoint");
            Bone chest = this.getMowzieBone("chest");
            Bone body = this.getMowzieBone("body");
            Bone headJoint = this.getMowzieBone("headJoint");
            float idleSpeed = 0.08f;
            featherRaiseController.addPosX((float)(Math.sin(((double)frame - 1.2) * (double)idleSpeed) * 0.1));
            body.addRotX((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.035));
            chest.addPosY((float)(-0.7 + Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.014));
            chest.addRotX((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * -0.017));
            neck1.addRotX((float)(Math.cos(((double)frame - 0.3) * (double)idleSpeed) * -0.052));
            neck2.addRotX((float)(Math.cos(((double)frame - 0.5) * (double)idleSpeed) * -0.052));
            headJoint.addRotX((float)(Math.cos(((double)frame - 0.6) * (double)idleSpeed) * 0.1));
            leftArmJoint.addRotX((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.052));
            leftLowerArm.addRotY((float)(-Math.cos(((double)frame - 0.0) * (double)idleSpeed) * 0.035));
            leftHand.addRotY((float)(-Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.087));
            rightArmJoint.addRotX((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.052));
            rightLowerArm.addRotY((float)(Math.cos(((double)frame - 0.0) * (double)idleSpeed) * 0.035));
            rightHand.addRotY((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.087));
            leftThigh.addRotY((float)(Math.cos(((double)frame - 0.0) * (double)idleSpeed) * -0.052) * (1.0f - liftLegs));
            leftThigh.addRotZ((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * -0.052) * (1.0f - liftLegs));
            leftCalf.addRotX((float)(Math.sin(((double)frame - 0.2) * (double)idleSpeed) * 0.087) * (1.0f - liftLegs));
            leftAnkle.addRotX((float)(Math.cos(((double)frame - 0.2) * (double)idleSpeed) * -0.087) * (1.0f - liftLegs));
            leftFoot.addRotX((float)(Math.cos(((double)frame - 0.4) * (double)idleSpeed) * -0.14));
            rightThigh.addRotY((float)(Math.cos(((double)frame - 0.0) * (double)idleSpeed) * 0.052) * (1.0f - liftLegs));
            rightThigh.addRotZ((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.052) * (1.0f - liftLegs));
            rightCalf.addRotX((float)(Math.sin(((double)frame - 0.2) * (double)idleSpeed) * 0.087) * (1.0f - liftLegs));
            rightAnkle.addRotX((float)(Math.cos(((double)frame - 0.2) * (double)idleSpeed) * -0.087) * (1.0f - liftLegs));
            rightFoot.addRotX((float)(Math.cos(((double)frame - 0.4) * (double)idleSpeed) * -0.14));
            leftThigh.addRotY((float)(Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.035) * liftLegs);
            rightThigh.addRotY((float)(-Math.sin(((double)frame - 0.0) * (double)idleSpeed) * 0.035) * liftLegs);
            float armAimControl = this.getControllerValue("armAimController");
            leftArmJoint.addRotX(headPitch * ((float)Math.PI / 180) * armAimControl);
            leftArmJoint.addRotY(headYaw * ((float)Math.PI / 180) * armAimControl);
        }
        float bellyBounceControl = this.getControllerValue("bellyBounceController");
        float jiggleSpeed = 2.5f;
        float jiggleScale = (float)((double)bellyBounceControl * 0.1 * Math.cos(jiggleSpeed * frame));
        Bone stomach = this.getMowzieBone("stomach");
        Bone chest = this.getMowzieBone("chest");
        Bone tail = this.getMowzieBone("tail");
        stomach.setScale(stomach.getScaleX() + jiggleScale, stomach.getScaleY() + jiggleScale, stomach.getScaleZ() + jiggleScale);
        chest.addPosY(jiggleScale * 3.0f);
        leftThigh.addPosX(-jiggleScale * 5.0f);
        rightThigh.addPosX(jiggleScale * 5.0f);
        tail.addPosZ(jiggleScale * 4.0f);
        float featherShakeControl = this.getControllerValue("featherShakeController");
        float featherRaiseControl = this.getControllerValue("featherRaiseController");
        ArrayList<Triple<Bone,Direction.Axis,Boolean>> feathers = new ArrayList<Triple<Bone,Direction.Axis,Boolean>>();
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersFront1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersFront2"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersFront3"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersFront4"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersBack1"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersBack2"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersBack3"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersBack4"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersBack5"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersLeft1"), Direction.Axis.Z, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersLeft2"), Direction.Axis.Z, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersLeft3"), Direction.Axis.Z, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersLeft4"), Direction.Axis.Z, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersLeft5"), Direction.Axis.Z, true));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersRight1"), Direction.Axis.Z, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersRight2"), Direction.Axis.Z, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersRight3"), Direction.Axis.Z, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersRight4"), Direction.Axis.Z, false));
        feathers.add(Triple.of(this.getMowzieBone("neckFeathersRight5"), Direction.Axis.Z, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersFront1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersFront2"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersFront3"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersLeft1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersLeft2"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersLeft3"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersRight1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersRight2"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestFeathersRight3"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersFront1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersFront2"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersFront3"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersRight1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersRight2"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersRight3"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersRight4"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersRight5"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersLeft1"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersLeft2"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersLeft3"), Direction.Axis.X, true));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersLeft4"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("bellyFeathersLeft5"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestDraperyFront"), Direction.Axis.X, false));
        feathers.add(Triple.of(this.getMowzieBone("chestDraperyRight"), Direction.Axis.Z, false));
        feathers.add(Triple.of(this.getMowzieBone("chestDraperyLeft"), Direction.Axis.Z, true));
        feathers.add(Triple.of(this.getMowzieBone("chestDraperyBack"), Direction.Axis.X, true));
        for (var feather : feathers) {
            Direction.Axis axis;
            Bone bone;
            bone = feather.getLeft();
            float oscillation = (float)((double)featherShakeControl * 0.13 * Math.cos(1.4 * (double)frame + (double)bone.getPivotY() * -0.15 + (double)bone.getPivotZ() * -0.1)) + featherRaiseControl;
            if (((Boolean)feather.getRight()).booleanValue()) {
                oscillation *= -1.0f;
            }
            if ((axis = (Direction.Axis)feather.getMiddle()) == Direction.Axis.X) {
                bone.addRotX(oscillation);
                continue;
            }
            if (axis == Direction.Axis.Y) {
                bone.addRotY(oscillation);
                continue;
            }
            bone.addRotZ(oscillation);
        }
        Bone mask = this.getMowzieBone("mask");
        Bone body = this.getMowzieBone("body");
        mask.setScale(1.0f / (float)body.getScale().x, 1.0f / (float)body.getScale().y, 1.0f / (float)body.getScale().z);
    
 }
 private final class Bone {
  final String name;Bone(String name){this.name=name;}
  void addRotX(float v){snapshots.ifPresent(name,b->b.setRotX(b.getRotX()+v));}
  void addRotY(float v){snapshots.ifPresent(name,b->b.setRotY(b.getRotY()+v));}
  void addRotZ(float v){snapshots.ifPresent(name,b->b.setRotZ(b.getRotZ()+v));}
  void addPosX(float v){snapshots.ifPresent(name,b->b.setTranslateX(b.getTranslateX()+v));}
  void addPosY(float v){snapshots.ifPresent(name,b->b.setTranslateY(b.getTranslateY()+v));}
  void addPosZ(float v){snapshots.ifPresent(name,b->b.setTranslateZ(b.getTranslateZ()+v));}
  float getScaleX(){return snapshots.get(name).map(b->b.getScaleX()).orElse(1F);}
  float getScaleY(){return snapshots.get(name).map(b->b.getScaleY()).orElse(1F);}
  float getScaleZ(){return snapshots.get(name).map(b->b.getScaleZ()).orElse(1F);}
  org.joml.Vector3f getScale(){return new org.joml.Vector3f(getScaleX(),getScaleY(),getScaleZ());}
  void setScale(float x,float y,float z){snapshots.ifPresent(name,b->b.setScale(x,y,z));}
  float getPivotY(){return switch(name){case "body"->0.0F;case "chest"->16.0F;case "neckJoint"->21.67063F;case "neck"->21.67063F;case "neckFeathersFront4"->33.17063F;case "neckFeathersRight4"->33.17063F;case "neckFeathersLeft4"->33.17063F;case "neckFeathersBack4"->33.17063F;case "neck2Joint"->32.67158F;case "neck2"->32.67158F;case "headJoint"->42.09546F;case "headRotator"->48.59546F;case "head"->48.59546F;case "haloJoint"->48.77483F;case "halo"->48.77483F;case "pinnacles"->48.77483F;case "pinnaclesRight"->48.77483F;case "pinnacleTopRight"->48.77483F;case "pinnacleMidRight"->40.77483F;case "pinnacleBottomRight"->32.77483F;case "pinnaclesLeft"->48.77483F;case "pinnacleTopLeft"->48.77483F;case "pinnacleMidLeft"->40.77483F;case "pinnacleBottomLeft"->32.77483F;case "sun"->48.59546F;case "sun_render"->48.59546F;case "maskTwitcher"->47.59546F;case "mask"->48.59546F;case "maskJaw"->47.67601F;case "beak2"->46.76835F;case "maskHead"->49.67601F;case "leftSide"->50.0861F;case "leftCheek"->50.0861F;case "leftEyelidJoint"->50.02574F;case "leftEyelid"->51.02574F;case "rightSide"->50.0861F;case "rightCheek"->50.0861F;case "rightEyelidJoint"->50.02574F;case "rightEyelid"->51.02574F;case "maskCenter"->52.09022F;case "beak"->46.56089F;case "neckFeathersFront1"->42.09546F;case "neckFeathersFront2"->41.09546F;case "neckFeathersFront3"->39.09546F;case "neckFeathersBack1"->42.09546F;case "neckFeathersBack2"->41.09546F;case "neckFeathersBack5"->36.09546F;case "neckFeathersBack3"->39.09546F;case "neckFeathersRight1"->42.09546F;case "neckFeathersRight2"->41.09546F;case "neckFeathersRight5"->36.09546F;case "neckFeathersRight3"->39.09546F;case "neckFeathersLeft1"->42.09546F;case "neckFeathersLeft2"->41.09546F;case "neckFeathersLeft5"->36.09546F;case "neckFeathersLeft3"->39.09546F;case "leftArmJoint"->21.67063F;case "leftUpperArm"->21.67063F;case "leftLowerArm"->10.79822F;case "leftHand"->1.21217F;case "leftFinger"->-1.64685F;case "leftRing"->-0.17286F;case "rightArmJoint"->21.67063F;case "rightUpperArm"->21.67063F;case "rightLowerArm"->10.79822F;case "rightHand"->1.21217F;case "rightFinger"->-1.64685F;case "rightRing"->-0.17286F;case "chestCollar"->-2.32937F;case "chestDraperyFront"->23.92063F;case "chestDraperyRight"->23.92063F;case "chestDraperyLeft"->23.92063F;case "chestDraperyBack"->23.92063F;case "chestFeathersFront1"->20.67063F;case "chestFeathersLeft2"->23.67063F;case "chestFeathersRight2"->23.67063F;case "chestFeathersFront2"->23.67063F;case "chestFeathersLeft3"->20.67063F;case "chestFeathersRight3"->20.67063F;case "chestFeathersFront3"->18.17063F;case "chestFeathersLeft1"->16.92063F;case "chestFeathersRight1"->16.92063F;case "rightThighJoint"->6.0F;case "rightThigh"->6.0F;case "rightCalf"->-5.23259F;case "rightAnkle"->-11.23259F;case "rightFoot"->-16.25464F;case "leftThighJoint"->6.0F;case "leftThigh"->6.0F;case "leftCalf"->-5.23259F;case "leftAnkle"->-11.23259F;case "leftFoot"->-16.25464F;case "tail"->2.0F;case "tail1"->2.0F;case "tail2"->2.0F;case "tail5"->2.0F;case "tail3"->2.0F;case "tail4"->2.0F;case "stomach"->8.0F;case "bellyFeathersFront1"->9.0F;case "bellyFeathersRight1"->10.0F;case "bellyFeathersRight4"->14.0F;case "bellyFeathersLeft4"->14.0F;case "bellyFeathersRight2"->12.0F;case "bellyFeathersRight5"->14.25F;case "bellyFeathersLeft5"->14.25F;case "bellyFeathersRight3"->11.0F;case "bellyFeathersLeft3"->11.0F;case "bellyFeathersLeft1"->10.0F;case "bellyFeathersLeft2"->12.0F;case "bellyFeathersFront2"->13.5F;case "bellyFeathersFront3"->16.0F;case "featherShakeController"->0.0F;case "featherRaiseController"->0.0F;case "bellyBounceController"->0.0F;case "armAimController"->0.0F;case "superNovaCenterRotator"->48.59546F;case "superNovaCenter"->48.59546F;default->0F;};}
  float getPivotZ(){return switch(name){case "body"->-3.0F;case "chest"->-1.0F;case "neckJoint"->0.30667F;case "neck"->2.30667F;case "neckFeathersFront4"->-4.69333F;case "neckFeathersRight4"->0.80667F;case "neckFeathersLeft4"->0.80667F;case "neckFeathersBack4"->5.30667F;case "neck2Joint"->0.85029F;case "neck2"->0.85029F;case "headJoint"->0.73297F;case "headRotator"->-3.76703F;case "head"->-3.76703F;case "haloJoint"->-10.5737F;case "halo"->-10.5737F;case "pinnacles"->-10.60495F;case "pinnaclesRight"->-10.60495F;case "pinnacleTopRight"->-10.60495F;case "pinnacleMidRight"->-10.60495F;case "pinnacleBottomRight"->-10.60495F;case "pinnaclesLeft"->-10.60495F;case "pinnacleTopLeft"->-10.60495F;case "pinnacleMidLeft"->-10.60495F;case "pinnacleBottomLeft"->-10.60495F;case "sun"->-3.76703F;case "sun_render"->-3.76703F;case "maskTwitcher"->-12.26703F;case "mask"->-11.51703F;case "maskJaw"->-8.56021F;case "beak2"->-15.89821F;case "maskHead"->-6.56021F;case "leftSide"->-13.07589F;case "leftCheek"->-13.07589F;case "leftEyelidJoint"->-13.00808F;case "leftEyelid"->-12.50808F;case "rightSide"->-13.07589F;case "rightCheek"->-13.07589F;case "rightEyelidJoint"->-13.00808F;case "rightEyelid"->-12.50808F;case "maskCenter"->-12.53021F;case "beak"->-15.18369F;case "neckFeathersFront1"->-2.76703F;case "neckFeathersFront2"->-2.76703F;case "neckFeathersFront3"->-2.76703F;case "neckFeathersBack1"->4.23297F;case "neckFeathersBack2"->4.23297F;case "neckFeathersBack5"->4.23297F;case "neckFeathersBack3"->4.23297F;case "neckFeathersRight1"->0.73297F;case "neckFeathersRight2"->0.73297F;case "neckFeathersRight5"->0.73297F;case "neckFeathersRight3"->0.73297F;case "neckFeathersLeft1"->0.73297F;case "neckFeathersLeft2"->0.73297F;case "neckFeathersLeft5"->0.73297F;case "neckFeathersLeft3"->0.73297F;case "leftArmJoint"->-0.69333F;case "leftUpperArm"->-0.69333F;case "leftLowerArm"->-0.78165F;case "leftHand"->-1.34271F;case "leftFinger"->-6.98978F;case "leftRing"->-1.34271F;case "rightArmJoint"->-0.69333F;case "rightUpperArm"->-0.69333F;case "rightLowerArm"->-0.78165F;case "rightHand"->-1.34271F;case "rightFinger"->-6.98978F;case "rightRing"->-1.34271F;case "chestCollar"->0.30667F;case "chestDraperyFront"->-9.69333F;case "chestDraperyRight"->-0.19333F;case "chestDraperyLeft"->-0.19333F;case "chestDraperyBack"->6.30667F;case "chestFeathersFront1"->-9.69333F;case "chestFeathersLeft2"->-1.19333F;case "chestFeathersRight2"->-1.19333F;case "chestFeathersFront2"->-9.69333F;case "chestFeathersLeft3"->-0.19333F;case "chestFeathersRight3"->-0.19333F;case "chestFeathersFront3"->-9.69333F;case "chestFeathersLeft1"->-8.19333F;case "chestFeathersRight1"->-8.19333F;case "rightThighJoint"->-2.0F;case "rightThigh"->-2.0F;case "rightCalf"->-1.80485F;case "rightAnkle"->-1.80485F;case "rightFoot"->-1.79397F;case "leftThighJoint"->-2.0F;case "leftThigh"->-2.0F;case "leftCalf"->-1.80485F;case "leftAnkle"->-1.80485F;case "leftFoot"->-1.79397F;case "tail"->7.0F;case "tail1"->7.0F;case "tail2"->7.0F;case "tail5"->7.0F;case "tail3"->7.0F;case "tail4"->7.0F;case "stomach"->-3.0F;case "bellyFeathersFront1"->-14.0F;case "bellyFeathersRight1"->-14.0F;case "bellyFeathersRight4"->-13.0F;case "bellyFeathersLeft4"->-13.0F;case "bellyFeathersRight2"->-7.0F;case "bellyFeathersRight5"->5.5F;case "bellyFeathersLeft5"->5.5F;case "bellyFeathersRight3"->5.0F;case "bellyFeathersLeft3"->5.0F;case "bellyFeathersLeft1"->-14.0F;case "bellyFeathersLeft2"->-7.0F;case "bellyFeathersFront2"->-12.0F;case "bellyFeathersFront3"->-14.0F;case "featherShakeController"->0.0F;case "featherRaiseController"->0.0F;case "bellyBounceController"->0.0F;case "armAimController"->0.0F;case "superNovaCenterRotator"->-3.76703F;case "superNovaCenter"->-3.76703F;default->0F;};}
 }
}
