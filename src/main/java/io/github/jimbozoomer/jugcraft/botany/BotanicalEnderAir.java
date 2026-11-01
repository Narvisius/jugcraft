package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.ClipContext;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** End air is captured in glass, then thrown to convert at most 64 supplied-tag stones. */
public final class BotanicalEnderAir extends ThrowableItemProjectile {
    public static EntityType<BotanicalEnderAir> TYPE;
    private static final TagKey<Block> CONVERTIBLE=TagKey.create(Registries.BLOCK,Jugcraft.id("jugtania_ender_air_convertable"));
    public BotanicalEnderAir(EntityType<? extends BotanicalEnderAir> type,Level level){super(type,level);}
    static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("botanical_ender_air"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<BotanicalEnderAir>of(BotanicalEnderAir::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build(key));
        UseItemCallback.EVENT.register((player,world,hand)->{
            var held=player.getItemInHand(hand);if(!held.is(Items.GLASS_BOTTLE)||world.dimension()!=Level.END)return InteractionResult.PASS;
            var eye=player.getEyePosition();var hit=world.clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(player.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,player));
            if(hit.getType()==HitResult.Type.BLOCK&&!world.getFluidState(hit.getBlockPos()).isEmpty())return InteractionResult.PASS;
            if(!world.getEntitiesOfClass(AreaEffectCloud.class,player.getBoundingBox().inflate(3.5),cloud->cloud.isAlive()&&cloud.getParticle().getType()==ParticleTypes.DRAGON_BREATH).isEmpty())return InteractionResult.PASS;
            if(player instanceof ServerPlayer p){if(p.isSpectator()||!Authority.mayChange(p.level(),p,p.blockPosition()))return InteractionResult.FAIL;p.setItemInHand(hand,ItemUtils.createFilledResult(held,p,new ItemStack(Jugtania.item("ender_air_bottle"))));p.level().playSound(null,p.blockPosition(),SoundEvents.BOTTLE_FILL,SoundSource.PLAYERS,.5F,1);}
            return InteractionResult.SUCCESS;
        });
    }
    @Override protected Item getDefaultItem(){return Jugtania.item("ender_air_bottle");}
    @Override public void tick(){
        if(!level().isClientSide()&&(tickCount>200||!level().hasChunkAt(BlockPos.containing(position().add(getDeltaMovement()))))){discard();return;}super.tick();
    }
    @Override protected void onHit(HitResult hit){
        if(!(level() instanceof ServerLevel level))return;
        if(!(getOwner() instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator()){discard();return;}
        var center=BlockPos.containing(hit.getLocation());
        if(hit instanceof EntityHitResult entityHit&&entityHit.getEntity().getType()==net.minecraft.world.entity.EntityTypes.GHAST&&level.dimension()==Level.OVERWORLD){
            if(entityHit.getEntity() instanceof LivingEntity ghast&&Authority.mayStrike(p,ghast)&&Authority.mayChange(level,p,ghast.blockPosition())){
                // Original table is one tear with weight one against seven empty entries.
                if(random.nextInt(8)==0)Block.popResource(level,ghast.blockPosition(),new ItemStack(Items.GHAST_TEAR));level.sendParticles(ParticleTypes.SPLASH,ghast.getX(),ghast.getY()+2,ghast.getZ(),40,1,.2,1,.1);
            }
        }else{
            var positions=new ArrayList<BlockPos>();for(var pos:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))if(level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&level.getBlockEntity(pos)==null&&level.getBlockState(pos).is(CONVERTIBLE)&&Authority.mayChange(level,p,pos)&&ClaimTransfers.allowed(level,p.blockPosition(),pos))positions.add(pos.immutable());
            Collections.shuffle(positions,new Random(random.nextLong()));for(int i=0;i<Math.min(64,positions.size());i++)level.setBlockAndUpdate(positions.get(i),Blocks.END_STONE.defaultBlockState());
        }
        level.levelEvent(2002,center,0x1B1140);discard();
    }
    static final class Bottle extends Item {
        Bottle(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var stack=p.getItemInHand(hand);if(p.isSpectator()||p.getCooldowns().isOnCooldown(stack)||!Authority.mayChange(p.level(),p,p.blockPosition()))return InteractionResult.FAIL;
            var projectile=new BotanicalEnderAir(TYPE,level);projectile.setOwner(p);projectile.setItem(stack.copyWithCount(1));projectile.setPos(p.getX(),p.getEyeY()-.1,p.getZ());projectile.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5F,1);
            if(!p.level().addFreshEntity(projectile))return InteractionResult.FAIL;if(!p.hasInfiniteMaterials())stack.shrink(1);p.getCooldowns().addCooldown(Jugtania.item("ender_air_bottle").getDefaultInstance(),6);level.playSound(null,p.blockPosition(),BotanicalSounds.get("ender_air_throw"),SoundSource.PLAYERS,1,1);return InteractionResult.SUCCESS;
        }
    }
}
