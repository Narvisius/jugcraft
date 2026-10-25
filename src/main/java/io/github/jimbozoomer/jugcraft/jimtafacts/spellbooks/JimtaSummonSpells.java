package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons;
import java.util.Set;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Source summon counts and ten-minute leases, using Jugcraft's shared summon authority. */
public final class JimtaSummonSpells {
    public static final Set<String> NAMES=Set.of("summon_vex","summon_polar_bear","summon_horse","raise_dead","summon_swords");
    public static EntityType<SpectralSteed> STEED;
    public static EntityType<SummonedZombie> ZOMBIE;
    public static EntityType<SummonedSkeleton> SKELETON;
    private JimtaSummonSpells(){}
    public static void register(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_spectral_steed"));
        STEED=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.of(SpectralSteed::new,MobCategory.CREATURE).sized(1.396F,1.6F).clientTrackingRange(10).build(key));
        FabricDefaultAttributeRegistry.register(STEED,Horse.createBaseHorseAttributes().add(Attributes.MAX_HEALTH,15).add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.JUMP_STRENGTH,.4).add(Attributes.SAFE_FALL_DISTANCE,6));
        var zombieKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_summoned_zombie"));
        ZOMBIE=Registry.register(BuiltInRegistries.ENTITY_TYPE,zombieKey,EntityType.Builder.of(SummonedZombie::new,MobCategory.MISC).sized(.6F,1.95F).clientTrackingRange(10).build(zombieKey));
        FabricDefaultAttributeRegistry.register(ZOMBIE,net.minecraft.world.entity.monster.zombie.Zombie.createAttributes());
        var skeletonKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_summoned_skeleton"));
        SKELETON=Registry.register(BuiltInRegistries.ENTITY_TYPE,skeletonKey,EntityType.Builder.of(SummonedSkeleton::new,MobCategory.MISC).sized(.6F,1.99F).clientTrackingRange(10).build(skeletonKey));
        FabricDefaultAttributeRegistry.register(SKELETON,net.minecraft.world.entity.monster.skeleton.AbstractSkeleton.createAttributes());
        JimtaSummonedWeapon.register();
    }
    public static boolean cast(ServerPlayer owner,String name){
        if(owner.isShiftKeyDown())return NouveauSummons.dismissSpellbook(owner,name);
        if(name.equals("summon_swords"))return JimtaSummonedWeapon.summon(owner);
        int count=name.equals("raise_dead")||name.equals("summon_vex")?3:1;boolean spawned=false;
        for(int i=0;i<count;i++){
            Mob mob=switch(name){case "summon_vex"->EntityTypes.VEX.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);
                case "summon_polar_bear"->EntityTypes.POLAR_BEAR.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);
                case "summon_horse"->STEED.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);
                default->owner.getRandom().nextFloat()<.3?SKELETON.create(owner.level(),EntitySpawnReason.MOB_SUMMONED):ZOMBIE.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);};
            if(mob==null)continue;
            double angle=owner.getYRot()*Math.PI/180+Math.PI*2*i/count;
            var at=owner.blockPosition().offset((int)Math.round(Math.cos(angle)*2),0,(int)Math.round(Math.sin(angle)*2));
            for(int d=0;d<4&&owner.level().hasChunkAt(at)&&owner.level().getBlockState(at.below()).isAir();d++)at=at.below();
            if(!owner.level().hasChunkAt(at)||!Authority.mayChange(owner.level(),owner,at))continue;
            mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,owner.getYRot(),0);
            if(name.equals("summon_polar_bear")){mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24);mob.setHealth(24);mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);}
            if(name.equals("summon_vex")){mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);}
            if(mob instanceof SpectralSteed horse){horse.tameWithName(owner);horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));}
            if(name.equals("raise_dead")){
                mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(mob instanceof SummonedSkeleton?Items.BOW:Items.STONE_SWORD));
                // Level-one source raises mostly unarmoured undead; a small leather chance is retained.
                if(owner.getRandom().nextInt(16)==0)mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
            }
            spawned|=NouveauSummons.adoptSpellbook(owner,mob,name,12000);
        }
        return spawned;
    }
    public static final class SummonedZombie extends net.minecraft.world.entity.monster.zombie.Zombie{
        public SummonedZombie(EntityType<? extends net.minecraft.world.entity.monster.zombie.Zombie> type,Level level){super(type,level);}
        @Override protected boolean isSunSensitive(){return false;}
    }
    public static final class SummonedSkeleton extends net.minecraft.world.entity.monster.skeleton.Skeleton{
        public SummonedSkeleton(EntityType<? extends net.minecraft.world.entity.monster.skeleton.Skeleton> type,Level level){super(type,level);}
    }
    public static final class SpectralSteed extends Horse {
        public SpectralSteed(EntityType<? extends Horse> type,Level level){super(type,level);}
        @Override public boolean isFood(ItemStack stack){return false;}
        @Override public boolean canMate(net.minecraft.world.entity.animal.Animal other){return false;}
        @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
            if(!NouveauSummons.ownedBy(this,player.getUUID())||player.isShiftKeyDown())return InteractionResult.FAIL;
            if(!level().isClientSide())player.startRiding(this,true,false);
            return InteractionResult.SUCCESS;
        }
    }
}
