package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.*;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;

/** Bounded delayed spell deliveries. The same ledger follows every missile, orbit, pulse and continuation. */
public final class NouveauSpellDelivery {
    private static final List<Flight> ACTIVE=new ArrayList<>();
    private static final int TOTAL=128,PER_PLAYER=12;
    private static final Set<String> SHAPING=Set.of("place_block","phantom_block","conjure_water","freeze","break","exchange","crush","smelt","launch","gravity","pull","gust","animate_block");
    private static final class Flight {
        final UUID player;final ResourceKey<Level> dimension;final Plan plan;final Cause cause;final Ledger ledger;final String mode;final int color;final String particle;Vec3 axis,fixedOrbit;
        UUID anchor;ComposedSpells.Impact original,impact;Vec3 position,velocity;long due,until;int strikes,pulse,redirects;double distance;
        Flight(ServerPlayer p,Plan plan,Cause cause,Ledger ledger,String mode,ComposedSpells.Impact impact,long due,long until,Vec3 velocity){this.player=p.getUUID();dimension=p.level().dimension();this.plan=plan;this.cause=cause;this.ledger=ledger;this.mode=mode;this.impact=impact;original=impact;position=impact.origin();this.due=due;this.until=until;this.velocity=velocity;color=NouveauControls.color(p.getMainHandItem());particle=NouveauControls.particle(p.getMainHandItem());axis=p.getLookAngle().multiply(1,0,1).normalize();}
    }
    private NouveauSpellDelivery(){}
    public static void register(){ServerTickEvents.END_SERVER_TICK.register(server->{
        for(var f:List.copyOf(ACTIVE)){
            var l=server.getLevel(f.dimension);var p=server.getPlayerList().getPlayer(f.player);
            if(l==null||p==null||p.level()!=l||!p.isAlive()||p.isSpectator()||!JugNouveau.enabled()||l.getGameTime()>f.until||!l.hasChunkAt(BlockPos.containing(f.position))){ACTIVE.remove(f);continue;}
            if(l.getGameTime()<f.due)continue;
            switch(f.mode){
                case "projectile" -> missile(l,p,f);
                case "orbit" -> orbit(l,p,f);
                case "rune" -> field(l,p,f,true);
                case "linger","wall" -> field(l,p,f,false);
                default -> {ACTIVE.remove(f);sequential(l,p,f.plan,f.cause,f.ledger,f.impact,f.original);}
            }
        }
    });ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.clear());}
    private static boolean room(UUID owner,int count){return ACTIVE.size()+count<=TOTAL&&ACTIVE.stream().filter(f->f.player.equals(owner)).count()+count<=PER_PLAYER;}
    private static int flag(Plan plan,String key){return plan.root().steps().stream().mapToInt(s->s.effect().augment(key)).max().orElse(0);}
    public static ComposedSpells.@Nullable Outcome start(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger){
        String mode=plan.root().delivery().replace("jugcraft:nouveau_","");
        if(mode.equals("underfoot")){var at=p.blockPosition().below();return ComposedSpells.performAt(l,p,plan,cause,ledger,new ComposedSpells.Impact(Vec3.atCenterOf(at),null,at.above(),at,p.position()));}
        if(!Set.of("projectile","orbit","rune","linger","wall").contains(mode))return null;
        int count=mode.equals("projectile")?Math.min(5,1+flag(plan,"delivery_split")*2):1;if(!room(p.getUUID(),count))return new ComposedSpells.Outcome(false,false);
        NouveauResources.sound(l,p.blockPosition(),NouveauControls.sound(p.getMainHandItem()),.5F,1);
        var target=ComposedSpells.deliver(l,p,plan.root(),null);int duration=Math.clamp(200*(1+flag(plan,"delivery_duration"))/(1+flag(plan,"delivery_duration_down")),20,1200);
        for(int i=0;i<count;i++){
            double angle=(i-(count-1)/2.0)*.13;Vec3 look=p.getLookAngle();Vec3 velocity=new Vec3(look.x*Math.cos(angle)-look.z*Math.sin(angle),look.y,look.x*Math.sin(angle)+look.z*Math.cos(angle)).normalize().scale((.6+.3*flag(plan,"delivery_accelerate"))/(1+.5*flag(plan,"delivery_decelerate")));
            var f=new Flight(p,plan,cause,ledger,mode,target,l.getGameTime(),l.getGameTime()+duration,velocity);f.position=mode.equals("projectile")?p.getEyePosition():target.point();ACTIVE.add(f);
        }return new ComposedSpells.Outcome(true,false);
    }
    /** Echoes share the paid cast and its work ledger; no recursive cast or extra summon population. */
    public static void echoSummons(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger){
        if(!NouveauGear.worn(p,"summon_focus"))return;
        String delivery=plan.root().delivery();boolean orbit=delivery.equals("jugcraft:nouveau_orbit");
        if(!orbit&&!delivery.equals("jugcraft:nouveau_self"))return;
        for(var mob:l.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(16),e->e.isAlive()&&NouveauSummons.ownedBy(e,p.getUUID())).stream().sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(8).toList()){
            if(orbit){if(!room(p.getUUID(),1))break;int duration=Math.clamp(200*(1+flag(plan,"delivery_duration"))/(1+flag(plan,"delivery_duration_down")),20,1200);var f=new Flight(p,plan,cause,ledger,"orbit",NouveauEquipment.impact(p,mob),l.getGameTime(),l.getGameTime()+duration,Vec3.ZERO);f.anchor=mob.getUUID();f.position=mob.position();ACTIVE.add(f);}
            else ComposedSpells.performAt(l,p,plan,cause,ledger,NouveauEquipment.impact(p,mob));
        }
    }
    public static boolean launch(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger,ComposedSpells.Impact target,Vec3 direction){
        int count=Math.min(5,1+flag(plan,"delivery_split")*2);if(!room(p.getUUID(),count))return false;
        for(int i=0;i<count;i++){double angle=(i-(count-1)/2.0)*.13;Vec3 v=new Vec3(direction.x*Math.cos(angle)-direction.z*Math.sin(angle),direction.y,direction.x*Math.sin(angle)+direction.z*Math.cos(angle)).normalize().scale((.6+.3*flag(plan,"delivery_accelerate"))/(1+.5*flag(plan,"delivery_decelerate")));var f=new Flight(p,plan,cause,ledger,"projectile",target,l.getGameTime(),l.getGameTime()+200,v);f.position=target.origin();ACTIVE.add(f);}return true;
    }
    /** Device deliveries stay anchored to their muzzle/target, never to the remote owner's body. */
    public static boolean device(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger,ComposedSpells.Impact impact,Vec3 direction){
        String mode=plan.root().delivery().replace("jugcraft:nouveau_","");
        if(mode.equals("projectile"))return launch(l,p,plan,cause,ledger,impact,direction);
        if(mode.equals("self")||mode.equals("underfoot")){
            var at=BlockPos.containing(impact.origin());if(mode.equals("underfoot"))at=at.below();
            return ComposedSpells.performAt(l,p,plan,cause,ledger,new ComposedSpells.Impact(Vec3.atCenterOf(at),null,at.above(),at,impact.origin())).applied();
        }
        if(Set.of("orbit","rune","linger","wall").contains(mode)){
            if(!room(p.getUUID(),1))return false;int duration=Math.clamp(200*(1+flag(plan,"delivery_duration"))/(1+flag(plan,"delivery_duration_down")),20,1200);
            var f=new Flight(p,plan,cause,ledger,mode,impact,l.getGameTime(),l.getGameTime()+duration,direction);
            f.axis=direction.normalize();f.position=mode.equals("orbit")?impact.origin():impact.point();if(mode.equals("orbit"))f.fixedOrbit=impact.origin();ACTIVE.add(f);return true;
        }
        return ComposedSpells.performAt(l,p,plan,cause,ledger,impact).applied();
    }
    public static boolean sequential(Plan.Node node){return node.then()!=null&&node.steps().stream().anyMatch(s->s.component().equals("jugcraft:nouveau_reset"))||node.steps().size()>1&&node.steps().stream().anyMatch(s->Set.of("jugcraft:nouveau_delay","jugcraft:nouveau_reset","jugcraft:nouveau_rewind").contains(s.component())||SHAPING.contains(s.component().replace("jugcraft:nouveau_","")));}
    public static ComposedSpells.Outcome sequential(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger,ComposedSpells.Impact start){return sequential(l,p,plan,cause,ledger,start,start);}
    private static ComposedSpells.Outcome sequential(ServerLevel l,ServerPlayer p,Plan plan,Cause cause,Ledger ledger,ComposedSpells.Impact start,ComposedSpells.Impact original){
        var node=plan.root();boolean applied=false,denied=false;var impact=start;
        for(int i=0;i<node.steps().size();i++){
            var step=node.steps().get(i);
            if(step.component().equals("jugcraft:nouveau_reset")){impact=original;continue;}
            if(step.component().equals("jugcraft:nouveau_delay")){
                if(i+1>=node.steps().size())continue;if(!room(p.getUUID(),1))break;
                var remaining=copy(plan,node.steps().subList(i+1,node.steps().size()),node.then(),0);
                long due=l.getGameTime()+Math.max(1,step.effect().duration());var wait=new Flight(p,remaining,cause,ledger,"delay",impact,due,due+40,Vec3.ZERO);wait.original=original;wait.position=impact.point();ACTIVE.add(wait);return new ComposedSpells.Outcome(true,denied);
            }
            boolean shape=NouveauGear.worn(p,"shapers_focus")&&SHAPING.contains(step.component().replace("jugcraft:nouveau_",""))&&impact.struck()==null;
            Map<BlockPos,net.minecraft.world.level.block.state.BlockState> before=new LinkedHashMap<>();
            if(shape){int radius=Math.min(6,step.effect().augment("radius"));var origin=impact.block();
                // Keep the struck block and its nearest neighbors even at the largest supported radius.
                BlockPos.betweenClosedStream(origin.offset(-radius,-radius,-radius),origin.offset(radius,radius+1,radius)).map(BlockPos::immutable)
                    .sorted(Comparator.comparingDouble(origin::distSqr)).filter(l::hasChunkAt).limit(512).forEach(at->before.put(at,l.getBlockState(at)));
            }
            var once=copy(plan,List.of(step),null,0);
            if(impact.struck()!=null&&l.getEntity(impact.struck()) instanceof NouveauAnimatedBlock moving&&moving.belongs(p)&&step.effect().kind()==EffectKind.NOUVEAU_BLOCK){var e=step.effect();var directed=new EffectSpec(EffectKind.NOUVEAU,Intent.HELPFUL,e.magnitude(),e.duration(),e.status(),e.stacking(),e.school(),e.augments());once=copy(plan,List.of(new Plan.Step(step.index(),step.component(),directed,step.principle(),step.scaling())),null,0);}
            var result=ComposedSpells.performAt(l,p,once,cause,ledger,impact);applied|=result.applied();denied|=result.notAllowed();
            if(step.component().equals("jugcraft:nouveau_rewind")&&impact.struck()!=null&&l.getEntity(impact.struck()) instanceof LivingEntity entity)impact=NouveauEquipment.impact(p,entity);
            if(result.applied()&&shape&&i+1<node.steps().size()){
                var remaining=copy(plan,node.steps().subList(i+1,node.steps().size()),node.then(),0);int copies=0;
                for(var entry:before.entrySet()){var at=entry.getKey();var moving=NouveauAnimatedBlock.moved(l,p,at);if(moving==null&&l.getBlockState(at).equals(entry.getValue()))continue;
                    var next=moving!=null?NouveauEquipment.impact(p,moving):new ComposedSpells.Impact(Vec3.atCenterOf(at),null,at.above(),at,impact.origin());
                    var repeated=sequential(l,p,remaining,cause,ledger,next,original);denied|=repeated.notAllowed();if(++copies>=16)break;
                }if(copies>0)return new ComposedSpells.Outcome(true,denied);
            }
        }
        if(NouveauTomes.preset(cause)){if(applied||node.steps().stream().allMatch(s->s.component().equals("jugcraft:nouveau_reset")))applied|=NouveauTomes.next(l,p,node,cause,ledger,impact);return new ComposedSpells.Outcome(applied,denied);}
        if(applied&&node.then()!=null&&ledger.branch()&&cause.triggered()!=null){var next=new Plan(plan.text(),node.then(),plan.focus(),plan.cooldown(),plan.capacity(),plan.limits(),plan.duration());var result=ComposedSpells.performAt(l,p,next,cause.triggered(),ledger,impact);denied|=result.notAllowed();}
        return new ComposedSpells.Outcome(applied,denied);
    }
    private static Plan copy(Plan p,List<Plan.Step> steps,Plan.@Nullable Node then,int pulse){var n=p.root();var numbered=steps.stream().map(s->new Plan.Step(s.index()+pulse*64,s.component(),s.effect(),s.principle(),s.scaling())).toList();return new Plan(p.text(),new Plan.Node(n.depth(),n.delivery(),n.form(),n.range(),n.selection(),n.pick(),n.radius(),n.targets(),numbered,null,1,0,then),p.focus(),p.cooldown(),p.capacity(),p.limits(),p.duration());}
    private static void resolve(ServerLevel l,ServerPlayer p,Flight f,ComposedSpells.Impact impact){ComposedSpells.performAt(l,p,copy(f.plan,f.plan.root().steps(),f.plan.root().then(),f.pulse++),f.cause,f.ledger,impact);NouveauSignals.spell(l,p,impact.point(),f.plan.text(),f.plan.capacity(),true);}
    private static void missile(ServerLevel l,ServerPlayer p,Flight f){
        Vec3 end=f.position.add(f.velocity);var block=l.clip(new ClipContext(f.position,end,flag(f.plan,"delivery_sensitive")>0?ClipContext.Block.OUTLINE:ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        Vec3 stop=block.getType()==HitResult.Type.BLOCK?block.getLocation():end;
        if(block.getType()==HitResult.Type.BLOCK&&l.getBlockEntity(block.getBlockPos()) instanceof NouveauWorkshop prism&&prism.permitted(p)){
            if(prism.kind().equals("void_prism")){ACTIVE.remove(f);return;}
            if(prism.kind().equals("spell_prism")){if(++f.redirects==4)NouveauAdvancements.award(p,"prismatic");if(f.redirects>16){ACTIVE.remove(f);return;}var d=prism.getBlockState().getValue(NouveauWorkshopBlock.FACING);f.velocity=new Vec3(d.getStepX(),d.getStepY(),d.getStepZ()).scale(f.velocity.length());f.position=Vec3.atCenterOf(prism.getBlockPos()).add(f.velocity.normalize().scale(.7));prism.signal(15,l.getGameTime()+20);prism.setActive(!prism.getBlockState().getValue(NouveauWorkshopBlock.ACTIVE));return;}
        }
        var entity=ProjectileUtil.getEntityHitResult(p,f.position,stop,new AABB(f.position,stop).inflate(.4),e->e instanceof LivingEntity&&e!=p&&e.isAlive()&&!e.isSpectator(),f.position.distanceToSqr(stop));
        trail(l,f,f.position);
        if(entity!=null){var e=entity.getEntity();resolve(l,p,f,new ComposedSpells.Impact(entity.getLocation(),e.getUUID(),e.blockPosition(),e.blockPosition(),f.position));f.strikes++;f.position=entity.getLocation().add(f.velocity.normalize().scale(.8));}
        else if(block.getType()==HitResult.Type.BLOCK){resolve(l,p,f,new ComposedSpells.Impact(block.getLocation(),null,block.getBlockPos().relative(block.getDirection()),block.getBlockPos(),f.position));f.strikes++;f.position=block.getLocation().add(f.velocity.normalize().scale(1.1));}
        else f.position=end;
        f.distance+=f.velocity.length();if(f.strikes>flag(f.plan,"delivery_pierce")||f.distance>f.plan.root().range())ACTIVE.remove(f);
    }
    private static void trail(ServerLevel l,Flight f,Vec3 at){var sprite=NouveauResources.PARTICLES.get(f.particle);if(sprite!=null)l.sendParticles(sprite,at.x,at.y,at.z,1,0,0,0,0);l.sendParticles(new net.minecraft.core.particles.DustParticleOptions(f.color,.6F),at.x,at.y,at.z,1,0,0,0,0);}
    private static void orbit(ServerLevel l,ServerPlayer p,Flight f){
        int count=Math.min(8,3+flag(f.plan,"delivery_magnitude"));double radius=2+flag(f.plan,"delivery_radius"),angle=l.getGameTime()*.12*(1+flag(f.plan,"delivery_accelerate"));
        for(int i=0;i<count;i++){
            var anchor=f.anchor!=null&&l.getEntity(f.anchor) instanceof LivingEntity summon?summon:flag(f.plan,"delivery_sensitive")>0&&f.impact.struck()!=null&&l.getEntity(f.impact.struck()) instanceof LivingEntity target?target:p;
            var center=f.fixedOrbit!=null?f.fixedOrbit:anchor.position();var at=center.add(Math.cos(angle+i*Math.PI*2/count)*radius,1,Math.sin(angle+i*Math.PI*2/count)*radius);trail(l,f,at);
            if(l.getGameTime()%5!=0)continue;
            if(flag(f.plan,"delivery_sensitive")>0&&!l.getBlockState(BlockPos.containing(at)).isAir())resolve(l,p,f,new ComposedSpells.Impact(at,null,BlockPos.containing(at),BlockPos.containing(at),p.position()));
            else for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(.5),e->e!=p&&e.isAlive()).stream().limit(1).toList())resolve(l,p,f,NouveauEquipment.impact(p,target));
        }
    }
    private static void field(ServerLevel l,ServerPlayer p,Flight f,boolean rune){
        if(l.getGameTime()%5==0){double radius=rune?.6:2+flag(f.plan,"delivery_radius");for(int i=0;i<8;i++){double a=i*Math.PI/4;l.sendParticles(ParticleTypes.ENCHANT,f.position.x+Math.cos(a)*radius,f.position.y+.1+(f.mode.equals("wall")?i%3:0),f.position.z+Math.sin(a)*radius,1,0,0,0,0);}}
        if(!rune&&l.getGameTime()%Math.max(5,20/(1+flag(f.plan,"delivery_accelerate")))!=0)return;
        double radius=rune?.8:2+flag(f.plan,"delivery_radius");
        if(flag(f.plan,"delivery_sensitive")>0){resolve(l,p,f,f.impact);if(rune)ACTIVE.remove(f);return;}
        for(var target:l.getEntitiesOfClass(LivingEntity.class,new AABB(f.position,f.position).inflate(radius),e->e!=p&&e.isAlive()).stream().limit(8).toList()){
            if(f.mode.equals("wall")&&Math.abs(target.position().subtract(f.position).dot(f.axis))>.6)continue;
            resolve(l,p,f,new ComposedSpells.Impact(target.position(),target.getUUID(),target.blockPosition(),target.blockPosition(),f.position));if(rune){ACTIVE.remove(f);break;}
        }
    }
}
