package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Supplied six-point bend-constraint tail, applied to the recovered native bone hierarchy. */
final class WildNagaTail {
    private static final String[] NAMES={"tail1","tail2","tail3","tail4","tail5","tailEnd"};
    private final Vec3[] points=new Vec3[6],previous=new Vec3[6],angularVelocity=new Vec3[5];
    private int lastTick=Integer.MIN_VALUE;
    private Vec3 anchor;
    private static double wrap(double angle){return Math.atan2(Math.sin(angle),Math.cos(angle));}
    private static Vec3 wrap(Vec3 v){return new Vec3(wrap(v.x),wrap(v.y),wrap(v.z));}
    private static Vec3 angles(Vec3 a,Vec3 b){var d=b.subtract(a);return new Vec3(Math.atan2(d.z,d.x),Math.atan2(d.horizontalDistance(),d.y),0);}
    private static Vec3 direction(Vec3 angle){double sine=Math.sin(angle.y);return new Vec3(Math.cos(angle.x)*sine,Math.cos(angle.y),Math.sin(angle.x)*sine);}
    private static void chain(PoseStack pose,String name,Map<String,String> parents,Function<String,ModelPart> bones){String parent=parents.get(name);if(parent!=null&&!parent.equals("$mesh"))chain(pose,parent,parents,bones);bones.apply(name).translateAndRotate(pose);}
    private static Matrix4f matrix(String name,float yaw,Map<String,String> parents,Function<String,ModelPart> bones){var pose=new PoseStack();pose.translate(0,1.5,0);pose.rotateDegrees(Axis.YP,180-yaw);pose.scale(-1,-1,1);if(name!=null&&!name.equals("$mesh"))chain(pose,name,parents,bones);return new Matrix4f(pose.last().pose());}
    private static Vec3 point(Matrix4f transform,Vec3 origin){var v=transform.transformPosition(new Vector3f());return origin.add(v.x,v.y,v.z);}
    void apply(int tick,float partial,Vec3 position,float yaw,boolean grounded,boolean frozen,Map<String,String> parents,Function<String,ModelPart> bones){
        Vec3[] targets=new Vec3[6];Matrix4f[] rest=new Matrix4f[6];
        for(int i=0;i<6;i++){rest[i]=matrix(NAMES[i],yaw,parents,bones);targets[i]=point(rest[i],position);}
        if(anchor==null||anchor.distanceToSqr(position)>64||tick<lastTick||tick-lastTick>40){
            for(int i=0;i<6;i++)points[i]=previous[i]=targets[i];Arrays.fill(angularVelocity,Vec3.ZERO);lastTick=tick;
        }
        if(tick!=lastTick&&!frozen){
            for(int i=0;i<6;i++)previous[i]=points[i];
            // Source gravity=.5, stiffness=.5, falloff=.5, damping=.97, 30 substeps.
            for(int step=0;step<30;step++)for(int i=0;i<5;i++){
                if(i==0)points[0]=targets[0];
                double length=Math.max(.01,targets[i].distanceTo(targets[i+1])),mass=.5+.5/(i+2D);
                Vec3 current=angles(points[i],points[i+1]),goal=angles(targets[i],targets[i+1]);
                Vec3 torque=wrap(current.subtract(goal)).scale(-.5/Math.sqrt(i+1));double force=length*mass*Math.sin(Math.PI-current.y);
                torque=torque.add(0,.5*force-(grounded&&points[i+1].y<position.y?force:0),0);
                Vec3 acceleration=wrap(torque).scale(1/(mass*length*length));
                angularVelocity[i]=wrap(angularVelocity[i].add(acceleration.scale(1/30D)).scale(.97));
                Vec3 next=wrap(current.add(angularVelocity[i].scale(1/30D)));
                points[i+1]=points[i].add(direction(next).scale(length));
            }
            lastTick=tick;
        }
        anchor=position;Vec3[] drawn=new Vec3[6];for(int i=0;i<6;i++)drawn[i]=previous[i].lerp(points[i],partial);
        // Translation follows the root immediately; only the tail bends behind it.
        Vec3 offset=targets[0].subtract(drawn[0]);for(int i=0;i<6;i++)drawn[i]=drawn[i].add(offset);
        for(int i=0;i<5;i++){
            ModelPart bone=bones.apply(NAMES[i]);Matrix4f parent=matrix(parents.get(NAMES[i]),yaw,parents,bones);
            Vec3 wanted=drawn[i+1].subtract(drawn[i]);var desired=new Matrix4f(parent).invert().transformDirection(new Vector3f((float)wanted.x,(float)wanted.y,(float)wanted.z)).normalize();
            Vec3 restDelta=targets[i+1].subtract(targets[i]);var restLocal=new Matrix4f(rest[i]).invert().transformDirection(new Vector3f((float)restDelta.x,(float)restDelta.y,(float)restDelta.z)).normalize();
            var original=new Quaternionf().rotationZYX(bone.zRot,bone.yRot,bone.xRot);var base=original.transform(new Vector3f(restLocal)).normalize();
            var rotation=new Quaternionf().rotationTo(base,desired).mul(original);var euler=rotation.getEulerAnglesZYX(new Vector3f());
            if(Float.isFinite(euler.x)&&Float.isFinite(euler.y)&&Float.isFinite(euler.z)){bone.xRot=euler.x;bone.yRot=euler.y;bone.zRot=euler.z;}
        }
    }
}
