package io.github.jimbozoomer.jugcraft.arcane;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Supplied thrown Aureal bottle: replenishment, lost-soul conversion and persisted Aureal creatures. */
public final class ArcaneSplash extends ThrowableItemProjectile {
    public static EntityType<ArcaneSplash> TYPE;
    public static AttachmentType<Boolean> AUREAL_CREATURE;
    public ArcaneSplash(EntityType<? extends ArcaneSplash> t,Level l){super(t,l);}
    static void register(){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("forbidden_aureal_bottle"));TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ArcaneSplash>of(ArcaneSplash::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(10).build(key));AUREAL_CREATURE=AttachmentRegistry.create(Jugcraft.id("arcane_aureal_creature"),b->b.initializer(()->false).persistent(com.mojang.serialization.Codec.BOOL));}
    @Override protected Item getDefaultItem(){return ForbiddenArcane.item("splash_aureal_bottle");}
    @Override protected double getDefaultGravity(){return .055;}
    @Override public void tick(){super.tick();if(!level().isClientSide()&&tickCount>200)discard();}
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);if(!(level() instanceof ServerLevel l))return;
        if(getOwner() instanceof ServerPlayer p&&Authority.mayChange(l,p,blockPosition())){
            var targets=new ArrayList<LivingEntity>();l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),getBoundingBox().inflate(4,2,4),LivingEntity::isAlive,targets,64);
            for(var target:targets){if(distanceToSqr(target)>=16||!Authority.mayChange(l,p,target.blockPosition()))continue;
                if(target instanceof ServerPlayer recipient)recipient.setAttached(ArcaneEssences.AUREAL,Math.min(10000,recipient.getAttachedOrCreate(ArcaneEssences.AUREAL)+30));
                else if(target instanceof ArcaneLostSoul soul&&Authority.mayStrike(p,soul)){String next=soul.kind().equals("lost_soul")?"enchanted_lost_soul":soul.kind().equals("corrupt_lost_soul")?"lost_soul":"";if(!next.isEmpty())soul.convertTo(ArcaneLostSoul.TYPES.get(next),ConversionParams.single(soul,true,true),EntitySpawnReason.CONVERSION,e->e.setHealth(Math.min(e.getMaxHealth(),soul.getHealth())));}
                else if(target.getType().getCategory()==MobCategory.CREATURE||target.getType().getCategory()==MobCategory.AMBIENT)target.setAttached(AUREAL_CREATURE,true);
            }
        }
        l.sendParticles(ParticleTypes.ENCHANT,getX(),getY(),getZ(),32,.7,.4,.7,.1);l.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,1,1);discard();
    }
    public static final class Bottle extends Item {
        public Bottle(Properties p){super(p.stacksTo(16));}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){if(l instanceof ServerLevel s){var shot=new ArcaneSplash(TYPE,s);shot.setOwner(player);shot.setItem(player.getItemInHand(hand).copyWithCount(1));shot.setPos(player.getX(),player.getEyeY()-.1,player.getZ());shot.shootFromRotation(player,player.getXRot(),player.getYRot(),-20,.7F,1);if(!s.addFreshEntity(shot))return InteractionResult.FAIL;player.getItemInHand(hand).consume(1,player);s.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW,net.minecraft.sounds.SoundSource.PLAYERS,.5F,.8F);}return InteractionResult.SUCCESS;}
    }
}
