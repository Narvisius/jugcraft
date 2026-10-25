package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Recasts are server-owned; wall anchors are bounded and never change the terrain. */
public final class JimtaAreaSpells {
    public static final Set<String> NAMES=Set.of("black_hole","gravity_fissure","ball_lightning","firefly_swarm","fang_swirl","sunbeam","wall_of_fire","ice_tomb");
    private record Wall(ServerLevel level,long expiry,List<Vec3> anchors){}
    private static final Map<ServerPlayer,Wall> WALLS=new WeakHashMap<>();
    private JimtaAreaSpells(){}
    public static void register(){JimtaAreaEntity.register();
        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:new ArrayList<>(WALLS.keySet())){var wall=WALLS.get(p);if(p.level()!=wall.level()||!p.isAlive()||p.isRemoved()){WALLS.remove(p);continue;}if(p.level().getGameTime()>=wall.expiry()){finishWall(p);JimtaRecasts.finish(p,"wall_of_fire");}}});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->WALLS.clear());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim,source,amount)->{if(victim.getVehicle() instanceof JimtaAreaEntity tomb&&tomb.kind().equals("ice_tomb")&&victim.level() instanceof ServerLevel level){tomb.hurtServer(level,source,amount);return false;}return true;});
    }
    static Vec3 aimed(ServerPlayer p,int range){var target=JimtaSpellcasting.target(p,range);return target!=null?target.position():p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();}
    static Vec3 ground(ServerPlayer p,Vec3 at){var pos=BlockPos.containing(at).above(2);for(int i=0;i<18;i++,pos=pos.below()){if(!p.level().hasChunkAt(pos))return at;if(!p.level().getBlockState(pos.below()).getCollisionShape(p.level(),pos.below()).isEmpty()&&p.level().getBlockState(pos).getCollisionShape(p.level(),pos).isEmpty())return new Vec3(at.x,pos.getY(),at.z);}return at;}
    public static boolean cast(ServerPlayer p,String name){
        if(name.equals("ball_lightning"))return JimtaShot.launch(p,name,p.getEyePosition(),p.getLookAngle().scale(.6),5)!=null;
        if(name.equals("wall_of_fire")){Vec3 anchor=ground(p,aimed(p,20));if(!Authority.mayChange(p.level(),p,BlockPos.containing(anchor)))return false;var wall=WALLS.get(p);if(wall==null||!JimtaRecasts.isPending(p,name)){wall=new Wall(p.level(),p.level().getGameTime()+40,new ArrayList<>());WALLS.put(p,wall);wall.anchors().add(anchor);JimtaRecasts.begin(p,name,4,40);return true;}double used=0;var points=wall.anchors();for(int i=1;i<points.size();i++)used+=points.get(i).distanceTo(points.get(i-1));Vec3 prev=points.getLast(),delta=anchor.subtract(prev);boolean finalAnchor=used+delta.length()>=13;anchor=prev.add(delta.normalize().scale(Math.min(delta.length(),Math.max(0,13-used))));if(anchor.distanceToSqr(prev)>.01)points.add(anchor);if(finalAnchor||points.size()>=4){finishWall(p);JimtaRecasts.finish(p,name);}else JimtaRecasts.consume(p,name);return true;}
        if(name.equals("ice_tomb")){if(p.isPassenger()||!Authority.mayChange(p.level(),p,p.blockPosition()))return false;var shell=JimtaAreaEntity.create(p,name,p.position(),null);if(shell==null)return false;if(!p.startRiding(shell,true,false)){shell.discard();return false;}return true;}
        var target=JimtaSpellcasting.target(p,name.equals("sunbeam")?48:32);Vec3 at=name.equals("gravity_fissure")?p.getEyePosition().add(p.getLookAngle().scale(2)):name.equals("black_hole")?aimed(p,25):ground(p,target==null?aimed(p,name.equals("sunbeam")?48:32):target.position());
        var field=JimtaAreaEntity.create(p,name,at,target);if(field==null)return false;if(name.equals("gravity_fissure"))field.setDeltaMovement(p.getLookAngle().scale(.2));if(name.equals("fang_swirl"))field.start=p.position();return true;
    }
    private static void finishWall(ServerPlayer p){var wall=WALLS.remove(p);if(wall==null||wall.level()!=p.level()||!p.isAlive())return;var anchors=wall.anchors();if(anchors.size()==1){Vec3 second=ground(p,aimed(p,20));Vec3 delta=second.subtract(anchors.getFirst());anchors.add(anchors.getFirst().add(delta.normalize().scale(Math.min(13,delta.length()))));}for(int i=1;i<anchors.size();i++){Vec3 a=anchors.get(i-1),b=anchors.get(i);double length=a.distanceTo(b);int count=Math.clamp((int)Math.ceil(length/.8),1,18);for(int j=0;j<count;j++)JimtaAreaEntity.create(p,"wall_of_fire",ground(p,a.lerp(b,(double)j/count)),null);}}
}
