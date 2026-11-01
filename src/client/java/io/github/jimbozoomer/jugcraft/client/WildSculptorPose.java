package io.github.jimbozoomer.jugcraft.client;
import com.geckolib.renderer.base.BoneSnapshots;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Original Sculptor idle equations, translated to render snapshots. */
final class WildSculptorPose {
 private final BoneSnapshots bones;private WildSculptorPose(BoneSnapshots b){bones=b;}
 static void apply(BoneSnapshots bones,float frame){new WildSculptorPose(bones).idle(frame);}
 private Bone getBone(String n){return new Bone(n);}
 private final class Bone{final String n;Bone(String n){this.n=n;}
 void addRotX(double v){bones.ifPresent(n,b->b.setRotX(b.getRotX()+(float)v));}
 void addRotY(double v){bones.ifPresent(n,b->b.setRotY(b.getRotY()+(float)v));}
 void addRotZ(double v){bones.ifPresent(n,b->b.setRotZ(b.getRotZ()+(float)v));}
 void addPosX(double v){bones.ifPresent(n,b->b.setTranslateX(b.getTranslateX()+(float)v));}
 void addPosY(double v){bones.ifPresent(n,b->b.setTranslateY(b.getTranslateY()+(float)v));}
 void addPosZ(double v){bones.ifPresent(n,b->b.setTranslateZ(b.getTranslateZ()+(float)v));}
 void setScaleY(float v){bones.ifPresent(n,b->b.setScaleY(v));} float getScaleZ(){return bones.get(n).map(b->b.getScaleZ()).orElse(1F);}
 }
 /** The supplied rig has actual knee/skirt locators. Solve against those, rather
  * than painting corrections onto the texture or introducing replacement cubes. */
 static void correct(BoneSnapshots bones){
  var pose=new WildSculptorPose(bones);pose.beads();pose.skirt();
 }
 private Vec3 point(String name){
  var b=bones.get(name);if(b.isEmpty())return null;
  var v=WildSpringChain.matrix(b.get().getBone(),bones,new Matrix4f(),true).transformPosition(new Vector3f());
  return new Vec3(v.x*16,v.y*16,v.z*16);
 }
 private void beads(){
  var head=bones.get("head");var joint=bones.get("headJoint");if(head.isEmpty()||joint.isEmpty())return;
  var h=head.get();var j=joint.get().getBone();
  var center=new Vec3(j.pivotX(),j.pivotY(),j.pivotZ());
  var direction=new Vec3(0,0,-1).yRot(h.getBone().baseRotY()+h.getRotY()).xRot(h.getBone().baseRotX()+h.getRotX());
  for(int i=1;i<=9;i++){
   String name="bead"+i;var snapshot=bones.get(name);if(snapshot.isEmpty())continue;
   var b=snapshot.get().getBone();var radial=new Vec3(b.pivotX(),b.pivotY(),b.pivotZ()).subtract(center).normalize().multiply(1,-1,1);
   double amount=Math.pow(Math.max(0,radial.dot(direction)),3.5)*3;
   var push=(i<=3?new Vec3(0,-1,0):i<=5?new Vec3(0,-.5,.25):i<=7?new Vec3(0,-.5,1):new Vec3(0,-.25,.75)).normalize().scale(amount);
   var target=getBone(name);target.addPosX(push.x);target.addPosY(push.y);target.addPosZ(push.z);
  }
 }
 private void skirt(){
  Vec3[] knee=new Vec3[2],end=new Vec3[2];float[] dots=new float[2];
  for(int i=0;i<2;i++){
   String side=i==0?"Right":"Left";
   var thigh=point("thigh"+side);var calf=point("calf"+side);var hem=point("skirtEnd"+side);
   if(thigh==null||calf==null||hem==null)return;
   knee[i]=calf.subtract(thigh).normalize();end[i]=hem.subtract(thigh).normalize();
   dots[i]=(float)Math.pow(Math.max(0,knee[i].dot(new Vec3(0,-1,0))),3);
  }
  // Capture every locator before modifying any part of this frame's hierarchy.
  var frontR=point("skirtFrontLocRight");var frontL=point("skirtFrontLocLeft");var backR=point("skirtBackLocRight");var backL=point("skirtBackLocLeft");
  if(frontR==null||frontL==null||backR==null||backL==null)return;
  for(int i=0;i<2;i++){
   String side=i==0?"Right":"Left";var hem=getBone("skirtEnd"+side);hem.addPosX(i==0?.2:-.2);hem.addPosY(-1.5);
   var joint=getBone("skirtJoint"+side);joint.addPosX(Math.max(-.9*dots[i],-.7)*(i==0?1:-1));joint.addPosZ(Math.max(-.7*dots[i],-.5));
   align("skirtJoint2"+side,end[i],knee[i]);
  }
  var average=knee[0].add(knee[1]).multiply(0,1,1).normalize();float angle=(float)Math.atan2(average.y,average.z);
  getBone("skirtBack").addRotX(-angle+3.48);getBone("skirtFront").addRotX(-Math.min(angle,-2)+3.48);
  var fd=frontL.subtract(frontR);var bd=backL.subtract(backR);
  float frontAngle=(float)Math.atan2(fd.z,fd.x);if(frontAngle<.001||frontAngle>3.141)frontAngle=0;
  final float fa=frontAngle;float ba=(float)Math.atan2(bd.z,bd.x);
  bones.ifPresent("skirtFront",b->{b.setScaleX(Math.max(.3F+.15F*(float)fd.length(),.4F));b.setRotY(fa*.6F-b.getBone().baseRotY());});
  bones.ifPresent("skirtBack",b->{b.setScaleX(.15F+.1F*(float)bd.length());b.setRotY(ba*.5F-b.getBone().baseRotY());});
  getBone("skirtFront").addPosX(.5*(frontR.x+fd.x*.5));getBone("skirtBack").addPosX(.5*(backR.x+bd.x*.5));
  float both=dots[0]*dots[1],fade=Math.min(1,both*2),turn=Math.clamp(fa,-.5F,.5F)*(1-fade);
  getBone("skirtRight").addRotY(turn-both*.4);getBone("skirtLeft").addRotY(turn+both*.4);
  var front=bones.get("skirtFront");if(front.isPresent())bones.ifPresent("clothFront",b->{var f=front.get();b.setRotation(f.getRotX()+f.getBone().baseRotX()-b.getBone().baseRotX(),f.getRotY()+f.getBone().baseRotY()-b.getBone().baseRotY(),f.getRotZ()+f.getBone().baseRotZ()-b.getBone().baseRotZ());});
  bones.ifPresent("clothFront2",b->{var parent=WildSpringChain.matrix(b.getBone().parent(),bones,new Matrix4f(),false);var q=parent.getUnnormalizedRotation(new Quaternionf()).invert();var v=q.getEulerAnglesZYX(new Vector3f());b.setRotation(v.x-b.getBone().baseRotX(),v.y-b.getBone().baseRotY(),v.z-b.getBone().baseRotZ());});
 }
 private void align(String name,Vec3 from,Vec3 to){
  if(from.lengthSqr()<1e-8||to.lengthSqr()<1e-8)return;
  bones.ifPresent(name,b->{
   var parent=WildSpringChain.matrix(b.getBone().parent(),bones,new Matrix4f(),false).invert();
   var a=parent.transformDirection(new Vector3f((float)from.x,(float)from.y,(float)from.z)).normalize();
   var z=parent.transformDirection(new Vector3f((float)to.x,(float)to.y,(float)to.z)).normalize();
   var current=new Quaternionf().rotationZYX(b.getBone().baseRotZ()+b.getRotZ(),b.getBone().baseRotY()+b.getRotY(),b.getBone().baseRotX()+b.getRotX());
   var e=new Quaternionf().rotationTo(a,z).mul(current).getEulerAnglesZYX(new Vector3f());b.setRotation(e.x-b.getBone().baseRotX(),e.y-b.getBone().baseRotY(),e.z-b.getBone().baseRotZ());
  });
 }
 private void idle(float frame){
        
        Bone eyebrowRight = getBone("eyebrowRight");
        Bone eyebrowLeft = getBone("eyebrowLeft");
        Bone clothFront2 = getBone("clothFront2");
        Bone clothBack = getBone("clothBack");
        Bone footLeft = getBone("footLeft");
        Bone calfLeft = getBone("calfLeft");
        Bone thighLeft = getBone("thighLeft");
        Bone headJoint = getBone("headJoint");
        Bone chest = getBone("chest");
        Bone footRight = getBone("footRight");
        Bone calfRight = getBone("calfRight");
        Bone thighRight = getBone("thighRight");
        Bone mouth = getBone("mouth");
        Bone handLeft = getBone("handLeft");
        Bone lowerArmLeft = getBone("lowerArmLeft");
        Bone shoulderLeft = getBone("shoulderLeft");
        Bone handRight = getBone("handRight");
        Bone lowerArmRight = getBone("lowerArmRight");
        Bone shoulderRight = getBone("shoulderRight");
        Bone body = getBone("body");
        float idleAnim = WildEarthRenderer.controller(bones,"idleAnimController");
        float idleAnimDisableArms = 1-WildEarthRenderer.controller(bones,"idleAnimDisableArmsController");
        float idleSpeed = 0.08f;
        eyebrowRight.addPosY(idleAnim * (float)(Math.sin((double)(frame * idleSpeed) + 0.4) * 0.1));
        eyebrowLeft.addPosY(idleAnim * (float)(Math.sin((double)(frame * idleSpeed) + 0.4) * 0.1));
        clothFront2.addRotX(idleAnim * (float)(Math.sin(frame * idleSpeed - 1.0f) * 0.05));
        clothBack.addRotX(idleAnim * (float)(-Math.sin(frame * idleSpeed - 1.0f) * 0.05));
        footLeft.addRotZ(idleAnim * (float)(-0.175 - Math.sin((double)(frame * idleSpeed) + 0.5) * 0.035));
        calfLeft.addRotZ(idleAnim * (float)(0.175 + Math.sin((double)(frame * idleSpeed) + 0.5) * 0.035));
        thighLeft.addRotZ(idleAnim * (float)(0.035 - Math.sin((double)(frame * idleSpeed) + 1.5) * 0.035));
        headJoint.addRotX(idleAnim * (float)(Math.sin((double)(frame * idleSpeed) + 1.5) * 0.017));
        chest.addRotX(idleAnim * (float)(-Math.sin((frame * idleSpeed + 1.0f) * idleSpeed) * 0.017));
        footRight.addRotY(-0.1309f);
        footRight.addRotZ(idleAnim * (float)(-0.35 + Math.sin((double)(frame * idleSpeed) + 0.5) * 0.035));
        calfRight.addRotZ(idleAnim * (float)(0.567 - Math.sin((double)(frame * idleSpeed) + 0.5) * 0.035));
        thighRight.addRotZ(idleAnim * (float)(-0.297 + Math.sin((double)(frame * idleSpeed) + 1.5) * 0.035));
        mouth.addPosY(idleAnim * (float)(-Math.sin((double)(frame * idleSpeed) + 0.5) * (double)0.1f));
        mouth.setScaleY((float)((double)mouth.getScaleZ() + (double)idleAnim * Math.sin((double)(frame * idleSpeed) + 0.5) * (double)0.05f));
        handLeft.addRotY(idleAnim * idleAnimDisableArms * (float)(-Math.sin(frame * idleSpeed + 1.0f) * 0.05));
        lowerArmLeft.addRotX(idleAnim * idleAnimDisableArms * (float)(-Math.sin((double)(frame * idleSpeed) + 0.5) * 0.05));
        lowerArmLeft.addRotY(idleAnim * idleAnimDisableArms * (float)(-Math.sin((double)(frame * idleSpeed) + 0.5) * 0.05));
        shoulderLeft.addRotZ(idleAnim * idleAnimDisableArms * (float)(Math.sin((double)(frame * idleSpeed) - 0.5) * 0.05));
        handRight.addRotY(idleAnim * idleAnimDisableArms * (float)(Math.sin(frame * idleSpeed + 1.0f) * 0.05));
        lowerArmRight.addRotX(idleAnim * idleAnimDisableArms * (float)(-Math.sin((double)(frame * idleSpeed) + 0.5) * 0.05));
        lowerArmRight.addRotY(idleAnim * idleAnimDisableArms * (float)(Math.sin((double)(frame * idleSpeed) + 0.5) * 0.05));
        shoulderRight.addRotZ(idleAnim * idleAnimDisableArms * (float)(-Math.sin((double)(frame * idleSpeed) - 0.5) * 0.05));
        body.addRotX(idleAnim * (float)(-Math.cos((double)(frame * idleSpeed) + 0.5) * 0.017));
        body.addPosY(idleAnim * (float)(Math.sin(frame * idleSpeed) * 1.0));
    
}
}
