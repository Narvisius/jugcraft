package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Supplied vine balls, slingshot and returning chakrams, with saved custody of the thrown item. */
public final class BotanicalThrownTools extends ThrowableItemProjectile {
    public static EntityType<BotanicalThrownTools> TYPE;
    private int age,bounces;private boolean returning,paid;private final Map<UUID,Integer> hits=new HashMap<>();
    public BotanicalThrownTools(EntityType<? extends BotanicalThrownTools> type,Level level){super(type,level);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_thrown_tool"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalThrownTools>of(BotanicalThrownTools::new,MobCategory.MISC).sized(.35F,.35F).clientTrackingRange(8).updateInterval(1).build(key));}
    @Override protected Item getDefaultItem(){return Jugtania.item("vine_ball");}
    private boolean chakram(){return getItem().is(Jugtania.item("thorn_chakram"))||getItem().is(Jugtania.item("flare_chakram"));}
    private boolean flame(){return getItem().is(Jugtania.item("flare_chakram"));}
    private boolean allowed(ServerPlayer p,BlockPos at){return level().hasChunkAt(at)&&level().getWorldBorder().isWithinBounds(at)&&Authority.mayChange(p.level(),p,at)&&ClaimTransfers.allowed(p.level(),p.blockPosition(),at);}
    @Override public void tick(){
        if(!level().isClientSide()){
            age++;if(age>=300||!level().hasChunkAt(BlockPos.containing(position().add(getDeltaMovement())))){recover();return;}
            if(chakram()){
                setNoGravity(true);if(age>=60||bounces>=16)returning=true;
                if(returning){if(!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.level()!=level()){recover();return;}
                    if(distanceToSqr(p)<2){recover();return;}setDeltaMovement(p.getEyePosition().subtract(position()).normalize().scale(1.2));noPhysics=true;
                }
            }
        }
        var velocity=getDeltaMovement();super.tick();if(chakram()&&!returning&&getDeltaMovement().distanceToSqr(velocity)<.1)setDeltaMovement(velocity);
        if(level().isClientSide()&&flame())level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,getX(),getY(),getZ(),0,0,0);
    }
    @Override protected void onHit(HitResult hit){if(!returning)super.onHit(hit);}
    @Override protected void onHitBlock(BlockHitResult hit){
        if(!(level() instanceof ServerLevel level))return;
        if(chakram()){
            if(!(getOwner() instanceof ServerPlayer p)||!allowed(p,hit.getBlockPos())){returning=true;return;}
            var d=hit.getDirection();var normal=new Vec3(d.getStepX(),d.getStepY(),d.getStepZ());var v=getDeltaMovement();setDeltaMovement(v.subtract(normal.scale(2*v.dot(normal))));setPos(hit.getLocation().add(normal.scale(.05)));bounces++;return;
        }
        if(!(getOwner() instanceof ServerPlayer p)||!p.isAlive()){recover();return;}
        var face=hit.getDirection();int placed=0;
        if(face.getAxis()!=Direction.Axis.Y){var at=hit.getBlockPos().relative(face);var property=VineBlock.getPropertyForFace(face.getOpposite());
            for(int i=0;i<9&&!level.isOutsideBuildHeight(at);i++,at=at.below()){
                if(!allowed(p,at))break;var state=level.getBlockState(at);if(state.is(Jugtania.block("solid_vine")))continue;if(!state.isAir())break;
                var vines=Jugtania.block("solid_vine").defaultBlockState().setValue(property,true);if(!vines.canSurvive(level,at))break;
                if(level.setBlockAndUpdate(at,vines))placed++;
            }
        }
        if(placed==0)recover();else{paid=false;discard();}
    }
    @Override protected void onHitEntity(EntityHitResult hit){
        if(!(level() instanceof ServerLevel level))return;if(!chakram()){recover();return;}
        if(!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||!(hit.getEntity() instanceof LivingEntity target)||target==p||!Authority.mayStrike(p,target)||!allowed(p,target.blockPosition()))return;
        if(age-hits.getOrDefault(target.getUUID(),-20)<10)return;if(hits.size()>=64&&!hits.containsKey(target.getUUID())){returning=true;return;}hits.put(target.getUUID(),age);
        if(target.hurtServer(level,damageSources().thrown(this,p),12)){if(flame())target.igniteForSeconds(5);else if(random.nextInt(3)==0)target.addEffect(new MobEffectInstance(MobEffects.POISON,60));}
    }
    private void recover(){
        if(!(level() instanceof ServerLevel level)||isRemoved())return;
        if(paid){var item=getItem().copyWithCount(1);paid=false;
            if(getOwner() instanceof ServerPlayer p&&p.isAlive()&&p.level()==level&&distanceToSqr(p)<4)p.getInventory().add(item);
            if(!item.isEmpty()){var drop=new ItemEntity(level,getX(),getY(),getZ(),item);drop.setDefaultPickUpDelay();if(getOwner()!=null)drop.setTarget(getOwner().getUUID());if(!level.addFreshEntity(drop)){paid=true;return;}}
        }discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("age",age);out.putInt("bounces",bounces);out.putBoolean("returning",returning);out.putBoolean("paid",paid);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);age=Math.clamp(in.getIntOr("age",0),0,300);bounces=Math.clamp(in.getIntOr("bounces",0),0,16);returning=in.getBooleanOr("returning",false);paid=in.getBooleanOr("paid",false);}
    static boolean launch(ServerPlayer p,ItemStack ammunition,boolean straight){
        if(p.isSpectator()||!Authority.mayChange(p.level(),p,p.blockPosition())||ammunition.isEmpty())return false;
        var projectile=new BotanicalThrownTools(TYPE,p.level());projectile.setOwner(p);projectile.setItem(ammunition.copyWithCount(1));projectile.setPos(p.getEyePosition().add(0,-.1,0));projectile.setNoGravity(straight);projectile.shootFromRotation(p,p.getXRot(),p.getYRot(),0,straight?2.4F:1.5F,1);
        // A recoverable projectile always takes custody of one real item, including creative throws.
        if(!p.level().addFreshEntity(projectile))return false;ammunition.shrink(1);projectile.paid=true;p.getInventory().setChanged();
        p.level().playSound(null,p.blockPosition(),BotanicalSounds.get(projectile.chakram()?projectile.flame()?"flare_chakram_throw":"thorn_chakram_throw":"vine_ball_throw"),SoundSource.PLAYERS,1,1);return true;
    }
    static final class Thrower extends Item {
        Thrower(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player user,InteractionHand hand){if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var stack=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;var id=stack.copyWithCount(1);if(!launch(p,stack,false))return InteractionResult.FAIL;p.getCooldowns().addCooldown(id,6);return InteractionResult.SUCCESS;}
    }
    static final class Slingshot extends Item {
        Slingshot(Properties p){super(p.stacksTo(1));}
        private ItemStack ammo(Player p){for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(Jugtania.item("vine_ball")))return s;}return ItemStack.EMPTY;}
        @Override public InteractionResult use(Level level,Player p,InteractionHand hand){if(ammo(p).isEmpty())return InteractionResult.FAIL;p.startUsingItem(hand);return InteractionResult.CONSUME;}
        @Override public int getUseDuration(ItemStack s,LivingEntity e){return 72000;}
        @Override public ItemUseAnimation getUseAnimation(ItemStack s){return ItemUseAnimation.BOW;}
        @Override public boolean releaseUsing(ItemStack s,Level level,LivingEntity user,int remaining){return user instanceof ServerPlayer p&&72000-remaining>=20&&launch(p,ammo(p),true);}
    }
    public static final class SolidVines extends VineBlock {
        public SolidVines(Properties p){super(p);}
        @Override protected boolean isRandomlyTicking(BlockState s){return false;}
    }
}
