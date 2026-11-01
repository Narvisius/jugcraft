package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;

/** Real, saved mana projectiles. Sampling bounds and permission checks precede every flight segment. */
public final class BotanicalBurst extends Projectile {
    public static EntityType<BotanicalBurst> TYPE;
    private static final EntityDataAccessor<Integer> COLOR=SynchedEntityData.defineId(BotanicalBurst.class,EntityDataSerializers.INT);
    private boolean handheld;private UUID operator;private BlockPos source,magnet;private ItemStack lens=ItemStack.EMPTY;private int mana,age,freeTicks,orbit;private float loss,gravity,debt;private BlockPos lastPhase;
    private final Set<UUID> contacted=new HashSet<>();private final Set<BlockPos> prisms=new HashSet<>();
    public record Properties(int capacity,int free,float loss,float speed,float gravity){}
    public BotanicalBurst(EntityType<? extends Projectile> type,Level level){super(type,level);setNoGravity(true);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_mana_burst"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalBurst>of(BotanicalBurst::new,MobCategory.MISC).sized(.15F,.15F).clientTrackingRange(12).updateInterval(1).build(key));
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,damage,amount)->!(damage.getDirectEntity() instanceof FireworkRocketEntity rocket&&cosmeticFirework(rocket)));
    }
    private static boolean cosmeticFirework(FireworkRocketEntity rocket){return rocket.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("jugcraft_botanical_cosmetic",false);}
    static Properties properties(String spreader,List<String> lenses){
        int mana=spreader.equals("handheld")?120:spreader.equals("gaia_spreader")?640:spreader.equals("elven_spreader")?240:160,free=spreader.equals("gaia_spreader")?120:spreader.equals("elven_spreader")?80:60;
        float loss=spreader.equals("gaia_spreader")?20:4,speed=spreader.equals("handheld")?1F:spreader.equals("gaia_spreader")?.4F:spreader.equals("elven_spreader")?.25F:.2F,gravity=0;
        for(String lens:lenses)switch(lens){case "power"->{mana*=2;speed*=.85F;loss*=2;}case "speed"->{speed*=2;mana=(int)(mana*.75);free/=3;loss*=2;}case "time"->{free=(int)(free*2.25);speed*=.8F;}case "efficiency"->{loss/=5;free=(int)(free*1.1);}case "gravity"->{gravity=.0015F;free=(int)(free*1.2);}case "messenger"->{mana/=5;free*=3;speed*=3;}default->{}}
        return new Properties(mana,free,loss,speed,gravity);
    }
    static BotanicalBurst launch(BotanicalWorkshop host,ServerPlayer actor,ItemStack lens,int mana,Properties props){
        var burst=new BotanicalBurst(TYPE,actor.level());burst.operator=actor.getUUID();burst.source=host.getBlockPos().immutable();burst.lens=lens.copyWithCount(1);burst.mana=mana;burst.freeTicks=props.free;burst.loss=props.loss;burst.gravity=props.gravity;burst.setOwner(actor);
        burst.setPos(Vec3.atCenterOf(host.getBlockPos()));burst.setDeltaMovement(host.aim().scale(props.speed));burst.entityData.set(COLOR,BotanicalLenses.color(lens,actor.level().getGameTime()));return actor.level().addFreshEntity(burst)?burst:null;
    }
    static BotanicalBurst launchHandheld(ServerPlayer actor,ItemStack lens,Properties props){
        var burst=new BotanicalBurst(TYPE,actor.level());burst.handheld=true;burst.operator=actor.getUUID();burst.source=actor.blockPosition().immutable();burst.lens=lens.copyWithCount(1);burst.mana=props.capacity;burst.freeTicks=props.free;burst.loss=props.loss;burst.gravity=props.gravity;burst.setOwner(actor);
        burst.setPos(actor.getEyePosition().add(0,-.1,0));burst.setDeltaMovement(actor.getLookAngle().scale(props.speed));burst.entityData.set(COLOR,BotanicalLenses.color(lens,actor.level().getGameTime()));return actor.level().addFreshEntity(burst)?burst:null;
    }
    static void storm(ServerPlayer actor,Vec3 origin,int color){
        var burst=new BotanicalBurst(TYPE,actor.level());burst.handheld=true;burst.operator=actor.getUUID();burst.source=BlockPos.containing(origin);burst.lens=new ItemStack(Jugtania.item("lens_storm"));burst.lens.set(BotanicalLenses.COLOR,color);burst.mana=300;burst.freeTicks=50;burst.loss=2.5F;burst.setOwner(actor);burst.setPos(origin);
        var r=actor.getRandom();burst.setDeltaMovement(new Vec3(r.nextDouble()-.5,r.nextDouble()-.5,r.nextDouble()-.5).normalize().scale(.5));actor.level().addFreshEntity(burst);
    }
    private boolean sourceAvailable(ServerLevel level,ServerPlayer actor){return handheld?Authority.mayChange(level,actor,source):level.getBlockEntity(source) instanceof BotanicalWorkshop origin&&origin.kind().contains("spreader")&&origin.permitted(actor);}
    BlockPos source(){return source;}
    boolean handheld(){return handheld;}
    int mana(){return mana;}
    void spend(int amount){mana=Math.max(0,mana-amount);}
    boolean colored(){return lens.getOrDefault(BotanicalLenses.COLOR,-1)>=0||lens.getOrDefault(BotanicalLenses.SECOND_COLOR,-1)>=0;}
    int dye(ServerLevel level){int color=lens.getOrDefault(BotanicalLenses.SECOND_COLOR,-1);if(color<0)color=lens.getOrDefault(BotanicalLenses.COLOR,5);return color==16?level.getRandom().nextInt(16):Math.clamp(color,0,15);}
    boolean has(String name){return BotanicalLenses.kinds(lens).contains(name);}
    private boolean permitted(ServerLevel level,ServerPlayer actor,BlockPos at){return level.hasChunkAt(at)&&level.getWorldBorder().isWithinBounds(at)&&Authority.mayChange(level,actor,at)&&source!=null&&ClaimTransfers.allowed(level,source,at);}
    private static boolean ghost(BlockState state){return state.getBlock() instanceof BotanicalBlock b&&b.kind.equals("prism")||state.getBlock() instanceof BotanicalRouting.Detector||state.getBlock() instanceof BotanicalPlatforms.Platform;}
    private BlockHitResult trace(ServerLevel level,Vec3 from,Vec3 to,boolean phantom){return level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this){
        @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){return pos.equals(source)||ghost(state)||phantom?Shapes.empty():super.getBlockShape(state,world,pos);}
    });}
    static boolean tripped(BotanicalWorkshop host,ServerPlayer actor,ItemStack lens){
        var level=actor.level();var properties=properties(host.kind(),BotanicalLenses.kinds(lens));Vec3 from=Vec3.atCenterOf(host.getBlockPos()),motion=host.aim().scale(properties.speed);boolean phantom=BotanicalLenses.kinds(lens).contains("phantom");
        // Simulation never creates entities, applies lens effects or loads chunks.
        for(int age=0;age<Math.min(600,properties.free+properties.capacity/Math.max(.1F,properties.loss));age++){
            var next=from.add(motion);for(int n=0;n<=8;n++){var at=BlockPos.containing(from.lerp(next,n/8.0));if(!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at)||!ClaimTransfers.allowed(level,host.getBlockPos(),at))return false;}
            var trace=new ClipContext(from,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor){@Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState s,BlockGetter world,BlockPos pos){return pos.equals(host.getBlockPos())||ghost(s)||phantom?Shapes.empty():super.getBlockShape(s,world,pos);}};
            var hit=level.clip(trace);var end=hit.getType()==HitResult.Type.BLOCK?hit.getLocation():next;
            for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.25),e->e.isAlive()&&!e.isSpectator()))if(target.getBoundingBox().inflate(.25).clip(from,end).isPresent())return true;
            if(hit.getType()==HitResult.Type.BLOCK)return false;from=next;motion=motion.add(0,-properties.gravity,0);
        }return false;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(COLOR,0x20FF20);}
    @Override public void tick(){
        super.tick();if(level().isClientSide()){for(int i=0;i<3;i++){var at=position().subtract(getDeltaMovement().scale(i/3.0));level().addParticle(new DustParticleOptions(entityData.get(COLOR),.7F),at.x,at.y,at.z,0,0,0);}return;}
        if(!(level() instanceof ServerLevel level))return;var actor=Authority.answering(level,operator);
        if(actor==null||source==null||!level.hasChunkAt(source)||!sourceAvailable(level,actor)||++age>1200||mana<=0||position().distanceToSqr(Vec3.atCenterOf(source))>256*256){finish();return;}
        if(age>freeTicks){debt+=loss;int cost=(int)debt;mana-=cost;debt-=cost;if(mana<=0){finish();return;}}
        entityData.set(COLOR,lens.isEmpty()&&!prisms.isEmpty()?0xFFFFFF:BotanicalLenses.color(lens,level.getGameTime()));if(has("magnet"))magnetize(level,actor);
        if(has("influence"))influence(level,actor);
        BotanicalPlanets.pull(this,actor);
        Vec3 from=position(),motion=getDeltaMovement(),next=from.add(motion);int samples=Math.max(1,(int)Math.ceil(motion.length()*8));if(samples>128){finish();return;}
        for(int i=0;i<=samples;i++)if(!permitted(level,actor,BlockPos.containing(from.lerp(next,i/(double)samples)))){finish();return;}
        var hit=trace(level,from,next,has("phantom"));var end=hit.getType()==HitResult.Type.BLOCK?hit.getLocation():next;
        // Visit crossed cells so pools receive through their open interior and detector plates can pulse.
        for(int i=0;i<=samples;i++){
            var at=BlockPos.containing(from.lerp(end,i/(double)samples));if(at.equals(source))continue;
            BotanicalRouting.trigger(level,actor,at);
            if(level.getBlockEntity(at) instanceof BotanicalWorkshop receiver){
                if(receiver.kind().equals("prism")&&receiver.permitted(actor)&&!level.hasNeighborSignal(at)&&prisms.size()<32&&prisms.add(at.immutable()))applyPrism(receiver.getItem(0));
                if(has("redirect")&&receiver.kind().contains("spreader")&&receiver.permitted(actor)){receiver.pointAt(Vec3.atCenterOf(source));finish();return;}
                if(BotanicalRouting.receiver(receiver.kind())&&receiver.permitted(actor)){BotanicalRouting.receive(receiver,actor,mana);mana=0;finish();return;}
            }
            if(has("phantom")&&!at.equals(lastPhase)&&!level.getBlockState(at).getCollisionShape(level,at).isEmpty()&&!ghost(level.getBlockState(at))){freeTicks-=4;lastPhase=at.immutable();}
        }
        if(has("damage")||has("redirect")||has("fire")||has("paint")){
            int seen=0;for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.25))){
                if(++seen>32||contacted.size()>=128)break;if(!target.isAlive()||target==actor||contacted.contains(target.getUUID())||target.getBoundingBox().inflate(.25).clip(from,end).isEmpty()||!permitted(level,actor,target.blockPosition())||!Authority.mayStrike(actor,target))continue;
                contacted.add(target.getUUID());if(has("paint")&&target instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep){BotanicalLensTerrain.sheep(this,level,actor,sheep);finish();return;}if(has("fire"))target.igniteForSeconds(5);if(has("redirect"))target.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(source));
                if(has("damage")&&!(target instanceof net.minecraft.world.entity.player.Player)&&mana>=16&&target.hurtServer(level,level.damageSources().playerAttack(actor),8))mana-=16;
            }
        }
        if(hit.getType()==HitResult.Type.BLOCK){
            var at=hit.getBlockPos();
            if(level.getBlockState(at).is(Jugtania.block("mana_bomb"))&&BotanicalManaStorm.ignite(level,actor,at,dye(level))){finish();return;}
            if(has("warp")&&level.getBlockState(at).is(Jugtania.block("piston_relay"))){var destination=BotanicalForceRelay.destination(level,actor,at);if(destination!=null&&permitted(level,actor,destination)){var point=Vec3.atCenterOf(destination).add(motion.normalize().scale(.7));if(permitted(level,actor,BlockPos.containing(point))){setPos(point);return;}}finish();return;}
            if(has("piston"))BotanicalForceRelay.push(actor,at,hit.getDirection().getOpposite(),source);
            if(has("mine")&&BotanicalLensTerrain.bore(this,level,actor,at)){setPos(hit.getLocation().add(motion.normalize().scale(.02)));return;}
            if(has("weight"))BotanicalLensTerrain.weight(this,level,actor,at);
            if(has("paint"))BotanicalLensTerrain.paint(this,level,actor,at);
            if(has("fire"))BotanicalLensTerrain.fire(this,level,actor,hit);
            if(has("light"))BotanicalLensTerrain.light(this,level,actor,hit);
            if(has("explosive")||has("storm"))BotanicalLensTerrain.explode(this,level,actor,hit.getLocation());
            if(has("bounce")){var normal=Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());var reflected=motion.subtract(normal.scale(2*motion.dot(normal)));setPos(hit.getLocation().add(normal.scale(.01)));setDeltaMovement(reflected);return;}
            if(has("firework"))celebrate(level,actor,hit.getLocation());finish();return;
        }
        setPos(next);setDeltaMovement(motion.add(0,-gravity,0));
    }
    private void applyPrism(ItemStack installed){
        lens=BotanicalLenses.name(installed).isEmpty()?ItemStack.EMPTY:installed.copyWithCount(1);gravity=0;float multiplier=1;
        for(String kind:BotanicalLenses.kinds(lens))switch(kind){
            case "speed"->{multiplier*=2;freeTicks/=3;loss*=2;}
            case "power"->{multiplier*=.85F;loss*=2;}
            case "time"->{freeTicks=(int)(freeTicks*2.25);multiplier*=.8F;}
            case "efficiency"->{loss/=5;freeTicks=(int)(freeTicks*1.1);}
            case "gravity"->{gravity=.0015F;freeTicks=(int)(freeTicks*1.2);}
            case "messenger"->{freeTicks*=3;multiplier*=3;}
            default->{}
        }
        freeTicks=Math.clamp(freeTicks,0,3600);loss=Math.clamp(loss,.001F,10000);var motion=getDeltaMovement().scale(multiplier);setDeltaMovement(motion.length()>8?motion.normalize().scale(8):motion);entityData.set(COLOR,lens.isEmpty()?0xFFFFFF:BotanicalLenses.color(lens,level().getGameTime()));
    }
    private void magnetize(ServerLevel level,ServerPlayer actor){
        if(magnet!=null&&(!permitted(level,actor,magnet)||!(level.getBlockEntity(magnet) instanceof BotanicalWorkshop w)||!BotanicalRouting.receiver(w.kind())||BotanicalRouting.room(w,actor)<=0))magnet=null;
        boolean fresh=magnet==null;if(fresh)for(var at:BlockPos.betweenClosed(blockPosition().offset(-3,-3,-3),blockPosition().offset(3,3,3))){if(at.distSqr(source)<=9||!permitted(level,actor,at))continue;if(level.getBlockEntity(at) instanceof BotanicalWorkshop w&&BotanicalRouting.receiver(w.kind())&&BotanicalRouting.room(w,actor)>0){magnet=at.immutable();break;}}
        if(magnet!=null){var motion=getDeltaMovement();var toward=Vec3.atCenterOf(magnet).subtract(position()).normalize();var adjusted=motion.subtract(motion.normalize().subtract(toward).scale(motion.length()*.1));setDeltaMovement(fresh?adjusted.scale(.75):adjusted);}
    }
    private void influence(ServerLevel level,ServerPlayer actor){int checked=0;for(var entity:level.getEntities(this,getBoundingBox().inflate(3.5))){
        if(++checked>128)break;if(!(entity instanceof ItemEntity||entity instanceof ExperienceOrb||entity instanceof AbstractArrow||entity instanceof FallingBlockEntity||entity instanceof PrimedTnt||entity instanceof BotanicalBurst other&&!other.has("influence")))continue;
        var destination=BlockPos.containing(entity.position().add(getDeltaMovement()));if(!permitted(level,actor,entity.blockPosition())||!permitted(level,actor,destination)||!ClaimTransfers.allowed(level,entity.blockPosition(),destination))continue;entity.setDeltaMovement(getDeltaMovement());entity.needsSync=true;
    }}
    private void celebrate(ServerLevel level,ServerPlayer actor,Vec3 at){
        // Native rockets provide the original launch/trail/burst presentation.
        // A scoped marker denies their entity damage without weakening ordinary fireworks.
        var active=new ArrayList<FireworkRocketEntity>();level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(FireworkRocketEntity.class),new AABB(at,at).inflate(32),BotanicalBurst::cosmeticFirework,active,8);if(active.size()>=8)return;
        var stack=new ItemStack(Items.FIREWORK_ROCKET);var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("jugcraft_botanical_cosmetic",true);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var explosion=new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL,it.unimi.dsi.fastutil.ints.IntList.of(entityData.get(COLOR)&0xFFFFFF),it.unimi.dsi.fastutil.ints.IntList.of(),true,true);
        stack.set(DataComponents.FIREWORKS,new Fireworks(0,List.of(explosion)));
        var rocket=new FireworkRocketEntity(level,actor,at.x,at.y+.1,at.z,stack);level.addFreshEntity(rocket);
    }
    public void orbit(Vec3 center){if(orbit==0)freeTicks=Math.min(3600,freeTicks*3);float radius=Math.min(7.5F,(Math.max(40,orbit)-40)/40F+1.5F);double angle=(orbit++%360)*Math.PI/18;var target=center.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);var movement=target.subtract(position());setDeltaMovement(movement.length()>8?movement.normalize().scale(8):movement);}
    private void finish(){if(!handheld&&level() instanceof ServerLevel s&&source!=null&&s.hasChunkAt(source)&&s.getBlockEntity(source) instanceof BotanicalWorkshop w)w.lenses.finished(getUUID());discard();}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.store("prisms",BlockPos.CODEC.listOf(),new ArrayList<>(prisms));out.putBoolean("handheld",handheld);if(operator!=null)out.store("operator",UUIDUtil.CODEC,operator);if(source!=null)out.store("source",BlockPos.CODEC,source);if(magnet!=null)out.store("magnet",BlockPos.CODEC,magnet);out.store("lens",ItemStack.OPTIONAL_CODEC,lens);out.putInt("mana",mana);out.putInt("age",age);out.putInt("free",freeTicks);out.putFloat("loss",loss);out.putFloat("gravity",gravity);out.putFloat("debt",debt);out.putInt("orbit",orbit);out.store("contacted",UUIDUtil.CODEC.listOf(),new ArrayList<>(contacted));if(lastPhase!=null)out.store("phase",BlockPos.CODEC,lastPhase);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);prisms.clear();in.read("prisms",BlockPos.CODEC.listOf()).orElse(List.of()).stream().limit(32).forEach(prisms::add);handheld=in.getBooleanOr("handheld",false);operator=in.read("operator",UUIDUtil.CODEC).orElse(null);source=in.read("source",BlockPos.CODEC).orElse(null);magnet=in.read("magnet",BlockPos.CODEC).orElse(null);lens=in.read("lens",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);mana=Math.clamp(in.getIntOr("mana",0),0,1280);age=Math.clamp(in.getIntOr("age",1200),0,1200);freeTicks=Math.clamp(in.getIntOr("free",0),-512,3600);loss=in.getFloatOr("loss",4);if(!Float.isFinite(loss)||loss<.01F||loss>80)loss=4;gravity=in.getFloatOr("gravity",0);if(!Float.isFinite(gravity)||gravity<0||gravity>.01F)gravity=0;debt=in.getFloatOr("debt",0);if(!Float.isFinite(debt)||debt<0||debt>=1)debt=0;orbit=Math.clamp(in.getIntOr("orbit",0),0,1200);contacted.clear();in.read("contacted",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(128).forEach(contacted::add);lastPhase=in.read("phase",BlockPos.CODEC).orElse(null);entityData.set(COLOR,BotanicalLenses.color(lens,0));}
}
