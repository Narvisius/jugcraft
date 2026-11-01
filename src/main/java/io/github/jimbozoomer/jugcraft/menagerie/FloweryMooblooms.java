package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.*;

/** Supplied variant data and skins; ordinary cow milk, wheat breeding and navigation are retained. */
public final class FloweryMooblooms {
    private FloweryMooblooms(){}
    public record Variant(String name,Block flower,TagKey<Biome> biomes){}
    public static final List<Variant> VARIANTS=new ArrayList<>();
    public static EntityType<Bloom> TYPE;
    public static void register(){
        for(var entry:OwnerBackpacks.source("newest_mooblooms.json").getAsJsonArray("variants")){
            var d=entry.getAsJsonObject();VARIANTS.add(new Variant(d.get("name").getAsString(),BuiltInRegistries.BLOCK.getValue(Identifier.parse(d.get("flower_block").getAsString())),TagKey.create(Registries.BIOME,Identifier.parse(d.get("biomes").getAsString().substring(1)))));
        }
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("flowery_moobloom"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.of(Bloom::new,MobCategory.CREATURE).sized(.9f,1.4f).eyeHeight(1.3f).clientTrackingRange(10).build(key));
        FabricDefaultAttributeRegistry.register(TYPE,Cow.createAttributes());
        SpawnPlacements.register(TYPE,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules);
        BiomeModifications.addSpawn(c->VARIANTS.stream().anyMatch(v->c.hasTag(v.biomes)),MobCategory.CREATURE,TYPE,4,2,3);
        var itemKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("flowery_moobloom_spawn_egg"));
        var egg=Registry.register(BuiltInRegistries.ITEM,itemKey,new SpawnEggItem(new Item.Properties().setId(itemKey).spawnEgg(TYPE)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(e->e.accept(egg));
    }
    public static final class Bloom extends Cow {
        private static final EntityDataAccessor<Integer> FLOWER=SynchedEntityData.defineId(Bloom.class,EntityDataSerializers.INT);
        private static final EntityDataAccessor<Boolean> SHEARED=SynchedEntityData.defineId(Bloom.class,EntityDataSerializers.BOOLEAN);
        private int regrow;
        public Bloom(EntityType<? extends Cow> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(FLOWER,0);builder.define(SHEARED,false);}
        public Variant flowers(){return VARIANTS.get(Math.clamp(entityData.get(FLOWER),0,VARIANTS.size()-1));}
        public boolean sheared(){return entityData.get(SHEARED);}
        @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group){
            var result=super.finalizeSpawn(level,difficulty,reason,group);var biome=level.getBiome(blockPosition());var choices=new ArrayList<Integer>();
            for(int i=0;i<VARIANTS.size();i++)if(biome.is(VARIANTS.get(i).biomes))choices.add(i);
            entityData.set(FLOWER,choices.isEmpty()?random.nextInt(VARIANTS.size()):choices.get(random.nextInt(choices.size())));return result;
        }
        @Override public Cow getBreedOffspring(ServerLevel level,AgeableMob other){
            var calf=TYPE.create(level,EntitySpawnReason.BREEDING);if(calf!=null)calf.entityData.set(FLOWER,other instanceof Bloom bloom&&random.nextBoolean()?bloom.entityData.get(FLOWER):entityData.get(FLOWER));return calf;
        }
        @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
            var held=player.getItemInHand(hand);
            if(held.is(Items.SHEARS)&&!isBaby()&&!sheared()){
                if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
                if(!Authority.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
                spawnAtLocation(p.level(),new ItemStack(flowers().flower,3));entityData.set(SHEARED,true);regrow=6000;
                held.hurtAndBreak(1,p,hand.asEquipmentSlot());playSound(net.minecraft.sounds.SoundEvents.SHEEP_SHEAR,1,1);return InteractionResult.SUCCESS;
            }
            // Guard milk and breeding through the same claims boundary as harvesting.
            if(!level().isClientSide()&&(held.is(Items.BUCKET)||isFood(held))&&!Authority.mayChange(level(),player,blockPosition()))return InteractionResult.FAIL;
            return super.mobInteract(player,hand);
        }
        @Override public void aiStep(){super.aiStep();if(!level().isClientSide()&&sheared()&&onGround()&&level().getBlockState(blockPosition().below()).is(Blocks.GRASS_BLOCK)&&--regrow<=0){entityData.set(SHEARED,false);regrow=0;}}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putString("Flower",flowers().name);out.putInt("FlowerRegrow",regrow);out.putBoolean("FlowersSheared",sheared());}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);String name=in.getStringOr("Flower","dandelion");for(int i=0;i<VARIANTS.size();i++)if(VARIANTS.get(i).name.equals(name))entityData.set(FLOWER,i);regrow=Math.clamp(in.getIntOr("FlowerRegrow",0),0,6000);entityData.set(SHEARED,in.getBooleanOr("FlowersSheared",false));}
    }
}
