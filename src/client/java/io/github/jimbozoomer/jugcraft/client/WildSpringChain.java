package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
import io.github.jimbozoomer.jugcraft.wilds.*;
import java.util.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Supplied Verlet chains, retargeted to the original Gecko bone hierarchy. */
final class WildSpringChain {
    record Frame(WildSpringChain chain,Vec3 origin,float yaw,float time,boolean frozen) {}
    private static final Map<net.minecraft.world.entity.LivingEntity,WildSpringChain> CHAINS=new WeakHashMap<>();
    private final String[] names;
    private final Vec3[] points,previous;
    private final double damping,attraction;
    private final String blend;
    private WildSpringChain(String[] names,double damping,double attraction,String blend){this.names=names;this.damping=damping;this.attraction=attraction;this.blend=blend;points=new Vec3[names.length];previous=new Vec3[names.length];}
    private Vec3 anchor;
    private float lastTime=Float.NaN;
    static Frame capture(WildElokosa mob,float partial){return new Frame(CHAINS.computeIfAbsent(mob,e->new WildSpringChain(new String[]{"tail1","tail2","tail3","tail4","tail5","tail6"},.04,2,"tailPhysicsBlendController")),mob.getPosition(partial),Mth.rotLerp(partial,mob.yBodyRotO,mob.yBodyRot),mob.tickCount+partial,WildCold.frozen(mob));}
    static Frame capture(WildSculptor mob,float partial){return new Frame(CHAINS.computeIfAbsent(mob,e->new WildSpringChain(new String[]{"beard3","beard4","beard5Rot","beard6"},.02,0,null)),mob.getPosition(partial),Mth.rotLerp(partial,mob.yBodyRotO,mob.yBodyRot),mob.tickCount+partial,WildCold.frozen(mob)||Math.abs(mob.getY()-mob.yo)>.001);}
    static Matrix4f matrix(GeoBone bone,BoneSnapshots bones,Matrix4f root,boolean pivot){
        if(bone==null)return new Matrix4f(root);
        var result=matrix(bone.parent(),bones,root,false);var state=bones.get(bone);
        result.translate(state.getTranslateX()/16,state.getTranslateY()/16,state.getTranslateZ()/16)
                .translate(bone.pivotX()/16,bone.pivotY()/16,bone.pivotZ()/16)
                .rotateZYX(bone.baseRotZ()+state.getRotZ(),bone.baseRotY()+state.getRotY(),bone.baseRotX()+state.getRotX())
                .scale(state.getScaleX(),state.getScaleY(),state.getScaleZ());
        if(!pivot)result.translate(-bone.pivotX()/16,-bone.pivotY()/16,-bone.pivotZ()/16);
        return result;
    }
    void apply(Frame frame,BoneSnapshots bones){
        GeoBone[] chain=new GeoBone[names.length];Vec3[] targets=new Vec3[names.length];Matrix4f[] rest=new Matrix4f[names.length];
        Matrix4f root=new Matrix4f().rotateY((180-frame.yaw())*Mth.DEG_TO_RAD).translate(0,.01F,0);
        for(int i=0;i<names.length;i++){
            var state=bones.get(names[i]);if(state.isEmpty())return;chain[i]=state.get().getBone();
            rest[i]=matrix(chain[i],bones,root,true);var point=rest[i].transformPosition(new Vector3f());targets[i]=frame.origin().add(point.x,point.y,point.z);
        }
        float dt=frame.time()-lastTime;
        if(anchor==null||anchor.distanceToSqr(frame.origin())>64||!Float.isFinite(dt)||dt<0||dt>1){
            for(int i=0;i<names.length;i++)points[i]=previous[i]=targets[i];dt=0;
        }
        if(!frame.frozen()&&dt>0){
            Vec3 start=points[0];double step=dt/10;
            // Original gravity .1, quadratic falloff .25, ten substeps and unit mass.
            // Elokosa attracts to its keyed tail; Sculptor has a free-hanging beard.
            for(int pass=0;pass<10;pass++){
                points[0]=start.lerp(targets[0],(pass+1)/10D);
                for(int i=1;i<names.length;i++){
                    Vec3 old=points[i],force=new Vec3(0,-.1,0).add(targets[i].subtract(old).scale(attraction/(1+i*i*.25)));
                    Vec3 next=old.add(old.subtract(previous[i]).scale(1-damping)).add(force.scale(step*step*(1-damping)));
                    double length=targets[i].distanceTo(targets[i-1]);Vec3 delta=next.subtract(points[i-1]);
                    points[i]=points[i-1].add(delta.lengthSqr()<1e-10?targets[i].subtract(targets[i-1]):delta.normalize().scale(length));previous[i]=old;
                }
            }
            previous[0]=targets[0];
        }
        anchor=frame.origin();lastTime=frame.time();
        float alpha=blend==null?1:Math.clamp(1+bones.get(blend).map(s->s.getTranslateX()).orElse(0F),0,1);
        Vec3[] drawn=new Vec3[names.length];Vec3 correction=targets[0].subtract(points[0]);
        for(int i=0;i<names.length;i++)drawn[i]=targets[i].lerp(points[i].add(correction),alpha);
        for(int i=0;i<names.length-1;i++){
            var state=bones.get(chain[i]);Matrix4f parent=matrix(chain[i].parent(),bones,root,false);
            Vec3 delta=drawn[i+1].subtract(drawn[i]);if(delta.lengthSqr()<1e-10)continue;
            var desired=new Matrix4f(parent).invert().transformDirection(new Vector3f((float)delta.x,(float)delta.y,(float)delta.z)).normalize();
            Vec3 natural=targets[i+1].subtract(targets[i]);
            var local=new Matrix4f(rest[i]).invert().transformDirection(new Vector3f((float)natural.x,(float)natural.y,(float)natural.z)).normalize();
            var rotation=new Quaternionf().rotationZYX(chain[i].baseRotZ()+state.getRotZ(),chain[i].baseRotY()+state.getRotY(),chain[i].baseRotX()+state.getRotX());
            var from=rotation.transform(new Vector3f(local)).normalize();
            var euler=new Quaternionf().rotationTo(from,desired).mul(rotation).getEulerAnglesZYX(new Vector3f());
            if(Float.isFinite(euler.x)&&Float.isFinite(euler.y)&&Float.isFinite(euler.z))state.setRotation(euler.x-chain[i].baseRotX(),euler.y-chain[i].baseRotY(),euler.z-chain[i].baseRotZ());
        }
    }
}

