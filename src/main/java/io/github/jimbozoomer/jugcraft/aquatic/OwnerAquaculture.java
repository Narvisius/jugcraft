package io.github.jimbozoomer.jugcraft.aquatic;

import com.google.gson.*;
import java.util.*;
import java.util.function.Function;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.fish.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;

/** Supplied fish habitats, dimensions, skins and food values, using native swimming/bucket persistence. */
public final class OwnerAquaculture {
    private OwnerAquaculture(){}
    public enum Shape {
        SMALL(.3f,.3f),MEDIUM(.5f,.3f),LARGE(.6f,.4f),LONGNOSE(.8f,.4f),CATFISH(.55f,.3f),JELLYFISH(.7f,.3f),HALIBUT(.7f,.2f);
        public final float width,height;Shape(float w,float h){width=w;height=h;}
    }
    public record Species(String name,Shape shape,int nutrition,int fillets){}
    public static final Map<String,Species> SPECIES=new LinkedHashMap<>();
    public static final Map<String,EntityType<Fish>> TYPES=new LinkedHashMap<>();
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static Item item(String name,Function<Item.Properties,Item> factory){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("aquatic_"+name));var item=Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key)));ITEMS.put(name,item);return item;}
    public static void register(){
        OwnerAquaticSounds.register();
        var data=OwnerBackpacks.source("newest_aquatic.json");
        for(var element:data.getAsJsonArray("fish")){
            var d=element.getAsJsonObject();var s=new Species(d.get("name").getAsString(),Shape.valueOf(d.get("shape").getAsString()),d.get("nutrition").getAsInt(),d.get("fillets").getAsInt());SPECIES.put(s.name,s);
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("aquatic_"+s.name));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Fish>of((t,l)->new Fish(t,l,s),MobCategory.WATER_AMBIENT).sized(s.shape.width,s.shape.height).clientTrackingRange(8).build(key));TYPES.put(s.name,type);
            FabricDefaultAttributeRegistry.register(type,AbstractFish.createAttributes());
            SpawnPlacements.register(type,SpawnPlacementTypes.IN_WATER,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WaterAnimal::checkSurfaceWaterAnimalSpawnRules);
            item(s.name,p->s.name.equals("minnow")?new OwnerAquaticGear.Bait(p,50):new Item(s.nutrition>0?p.food(new FoodProperties.Builder().nutrition(s.nutrition).saturationModifier(s.nutrition==3?.2f:.1f).build()):p));
            item(s.name+"_bucket",p->new FishBucket(type,p.stacksTo(1)));
            item(s.name+"_spawn_egg",p->new SpawnEggItem(p.spawnEgg(type)));
        }
        item("fish_fillet_raw",p->new Item(p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(.2f).build())));
        item("fish_fillet_cooked",p->new Item(p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(.6f).build())));
        item("sushi",p->new Item(p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(.8f).build())));
        item("algae",p->new Item(p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0).alwaysEdible().build())));
        item("fish_bones",Item::new);
        OwnerAquaticGear.register();
        OwnerFishWeights.register();
        OwnerNeptunium.register();
        OwnerAquaticRewards.register();
        OwnerLandTurtles.register();
        OwnerAquaticStations.register();
        OwnerFishMount.register();
        OwnerAquaticFishing.register();
        for(var value:data.getAsJsonArray("spawns")){
            var d=value.getAsJsonObject();var spawn=d.getAsJsonObject("spawn");var type=TYPES.get(spawn.get("type").getAsString().replace("jugcraft:aquatic_",""));
            if(type==null)continue;
            var include=d.getAsJsonArray("includeBiomes");var exclude=d.getAsJsonArray("excludeBiomes");boolean all=d.has("and")&&d.get("and").getAsBoolean();
            BiomeModifications.addSpawn(context->(all?matchesAll(context,include):matchesAny(context,include))&&!matchesAny(context,exclude),MobCategory.WATER_AMBIENT,type,spawn.get("weight").getAsInt(),spawn.get("minCount").getAsInt(),spawn.get("maxCount").getAsInt());
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e->ITEMS.entrySet().stream().filter(x->SPECIES.containsKey(x.getKey())||x.getKey().startsWith("fish_fillet")||x.getKey().equals("sushi")||x.getKey().equals("algae")).forEach(x->e.accept(x.getValue())));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.entrySet().stream().filter(x->!SPECIES.containsKey(x.getKey())&&!x.getKey().startsWith("fish_fillet")&&!x.getKey().equals("sushi")&&!x.getKey().equals("algae")).forEach(x->e.accept(x.getValue())));
    }
    public static int fillets(ItemStack stack){
        if(stack.is(Items.COD))return 4;if(stack.is(Items.SALMON))return 2;if(stack.is(Items.PUFFERFISH))return 1;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());var species=SPECIES.get(id.getPath().replace("aquatic_",""));
        return id.getNamespace().equals("jugcraft")&&species!=null?species.fillets:0;
    }
    private static boolean match(BiomeSelectionContext context,String id){return id.startsWith("#")?context.hasTag(TagKey.create(Registries.BIOME,Identifier.parse(id.substring(1)))):context.getBiomeKey().identifier().equals(Identifier.parse(id));}
    private static boolean matchesAny(BiomeSelectionContext c,JsonArray list){if(list==null)return false;for(var e:list)if(match(c,e.getAsString()))return true;return false;}
    private static boolean matchesAll(BiomeSelectionContext c,JsonArray list){if(list==null)return true;for(var e:list)if(!match(c,e.getAsString()))return false;return true;}
    public static final class FishBucket extends MobBucketItem {
        FishBucket(EntityType<? extends Mob> type,Item.Properties props){super(type,Fluids.WATER,SoundEvents.BUCKET_EMPTY_FISH,props);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p){var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
                if(hit.getType()==HitResult.Type.BLOCK&&(!Authority.mayChange(p.level(),p,hit.getBlockPos())||!Authority.mayChange(p.level(),p,hit.getBlockPos().relative(hit.getDirection()))))return InteractionResult.FAIL;
            }
            return super.use(level,player,hand);
        }
    }
    public static final class Fish extends AbstractSchoolingFish {
        public final Species species;
        public Fish(EntityType<? extends AbstractSchoolingFish> type,Level level,Species species){super(type,level);this.species=species;}
        @Override protected void registerGoals(){super.registerGoals();goalSelector.removeAllGoals(g->g instanceof FollowFlockLeaderGoal);goalSelector.addGoal(3,new SameSpeciesSchool(this));}
        @Override public ItemStack getBucketItemStack(){return new ItemStack(ITEMS.get(species.name+"_bucket"));}
        @Override protected SoundEvent getFlopSound(){return OwnerAquaticSounds.sound(species.shape==Shape.JELLYFISH?"jellyfish_flop":"fish_flop");}
        @Override protected SoundEvent getAmbientSound(){return OwnerAquaticSounds.sound("fish_ambient");}
        @Override protected SoundEvent getDeathSound(){return OwnerAquaticSounds.sound("fish_death");}
        @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return OwnerAquaticSounds.sound("fish_hurt");}
        @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p&&player.getItemInHand(hand).is(Items.WATER_BUCKET)&&!Authority.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
            return super.mobInteract(player,hand);
        }
        @Override public void playerTouch(Player player){super.playerTouch(player);if(species.shape==Shape.JELLYFISH&&isAlive()&&player instanceof ServerPlayer p&&distanceToSqr(p)<1&&tickCount%10==0&&Authority.mayStrike(p.level(),this,null,p)){if(p.hurtServer(p.level(),damageSources().mobAttack(this),.5f))playSound(OwnerAquaticSounds.sound("jellyfish_collide"),1,1);}}
    }
    /** The native school goal compares Java classes; these species share a class, so compare registry types too. */
    private static final class SameSpeciesSchool extends Goal {
        private final Fish fish;private Fish leader;private int nextSearch;
        SameSpeciesSchool(Fish fish){this.fish=fish;setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){
            if(!fish.isInWater()||fish.tickCount<nextSearch)return false;nextSearch=fish.tickCount+40;
            leader=fish.level().getEntitiesOfClass(Fish.class,fish.getBoundingBox().inflate(8),f->f.isAlive()&&f.isInWater()&&f.getType()==fish.getType()&&f.getId()<fish.getId()).stream().min(Comparator.comparingInt(Entity::getId)).orElse(null);
            return leader!=null&&fish.distanceToSqr(leader)>4;
        }
        @Override public boolean canContinueToUse(){return leader!=null&&leader.isAlive()&&leader.isInWater()&&fish.isInWater()&&fish.distanceToSqr(leader)>4&&fish.distanceToSqr(leader)<121;}
        @Override public void tick(){if(fish.tickCount%10==0&&leader!=null)fish.getNavigation().moveTo(leader,1);}
        @Override public void stop(){leader=null;fish.getNavigation().stop();}
    }
}
