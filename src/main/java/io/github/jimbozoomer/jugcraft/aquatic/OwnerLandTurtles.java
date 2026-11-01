package io.github.jimbozoomer.jugcraft.aquatic;

import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;

/** Original three small land turtles: flowers/vines, native breeding, buoyancy and shore finding. */
public final class OwnerLandTurtles {
    private OwnerLandTurtles(){}
    public static final Map<String,EntityType<Turtle>> TYPES=new LinkedHashMap<>();
    public static final TagKey<Item> FOOD=TagKey.create(Registries.ITEM,Jugcraft.id("aquatic_turtle_edible"));
    public static void register(){
        for(String name:List.of("box_turtle","arrau_turtle","starshell_turtle")){
            var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("aquatic_"+name));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Turtle>of(Turtle::new,MobCategory.CREATURE).sized(.5f,.25f).eyeHeight(.1875f).clientTrackingRange(8).build(key));TYPES.put(name,type);
            FabricDefaultAttributeRegistry.register(type,Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH,10).add(Attributes.MOVEMENT_SPEED,.1).add(Attributes.ARMOR,1.5));
            SpawnPlacements.register(type,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules);
            OwnerAquaculture.item(name,Item::new);
            OwnerAquaculture.item(name+"_spawn_egg",p->new SpawnEggItem(p.spawnEgg(type)));
        }
        for(var entry:OwnerBackpacks.source("newest_aquatic_turtles.json").getAsJsonArray("spawns")){
            var d=entry.getAsJsonObject();var spawn=d.getAsJsonObject("spawn");var type=TYPES.get(spawn.get("type").getAsString().replace("aquaculture:",""));if(type==null)continue;
            var includes=d.getAsJsonArray("includeBiomes");var excludes=d.getAsJsonArray("excludeBiomes");boolean all=d.has("and")&&d.get("and").getAsBoolean();
            BiomeModifications.addSpawn(c->{boolean match=all;for(var e:includes){String tag=e.getAsString();boolean has=tag.startsWith("#")?c.hasTag(TagKey.create(Registries.BIOME,Identifier.parse(tag.substring(1)))):c.getBiomeKey().identifier().equals(Identifier.parse(tag));match=all?match&&has:match||has;}if(excludes!=null)for(var e:excludes)if(c.hasTag(TagKey.create(Registries.BIOME,Identifier.parse(e.getAsString().replace("#","")))))return false;return match;},MobCategory.CREATURE,type,spawn.get("weight").getAsInt(),spawn.get("minCount").getAsInt(),spawn.get("maxCount").getAsInt());
        }
        OwnerAquaculture.item("turtle_soup",p->new Item(p.stacksTo(1).food(net.minecraft.world.food.Foods.MUSHROOM_STEW).usingConvertsTo(Items.BOWL)));
    }
    public static final class Turtle extends Animal {
        Turtle(EntityType<? extends Animal> type,Level level){super(type,level);setPathfindingMalus(PathType.WATER,.2f);}
        @Override protected void registerGoals(){
            goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.2));goalSelector.addGoal(2,new BreedGoal(this,1.05));goalSelector.addGoal(3,new TemptGoal(this,1.15,this::isFood,false));goalSelector.addGoal(4,new FollowParentGoal(this,1.1));goalSelector.addGoal(5,new Shore(this));goalSelector.addGoal(6,new RandomStrollGoal(this,1));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        }
        @Override public boolean isFood(ItemStack stack){return stack.is(FOOD);}
        @Override public boolean canMate(Animal other){return other.getType()==getType()&&super.canMate(other);}
        @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob mate){return (AgeableMob)getType().create(level,EntitySpawnReason.BREEDING);}
        @Override public void aiStep(){super.aiStep();if(isInWater())setAirSupply(getMaxAirSupply());}
        @Override public InteractionResult mobInteract(Player player,InteractionHand hand){if(player instanceof ServerPlayer p&&isFood(player.getItemInHand(hand))&&!Authority.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;return super.mobInteract(player,hand);}
    }
    private static final class Shore extends MoveToBlockGoal {
        private final Turtle turtle;
        Shore(Turtle t){super(t,1.2,24);turtle=t;verticalSearchStart=-1;}
        @Override public boolean canUse(){return turtle.isInWater()&&super.canUse();}
        @Override public boolean canContinueToUse(){return turtle.isInWater()&&tryTicks<=1200&&isValidTarget(turtle.level(),blockPos);}
        @Override protected boolean isValidTarget(LevelReader level,BlockPos pos){return level.hasChunkAt(pos)&&level.getBlockState(pos).isSolid()&&level.getBlockState(pos.above()).isAir();}
    }
}
