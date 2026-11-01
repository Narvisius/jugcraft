package io.github.jimbozoomer.jugcraft.ribbits;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import java.util.*;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Resident registration and native household blocks; originals live under newest/ribbits. */
public final class OwnerRibbits {
    public static final Map<String,Block> BLOCKS=new LinkedHashMap<>();
    public static final Map<String,EntityType<Ribbit>> TYPES=new LinkedHashMap<>();
    public static final Map<String,SoundEvent> SOUNDS=new HashMap<>();
    public static Item MARACA;
    private OwnerRibbits(){}
    public static SoundEvent sound(String name){return SOUNDS.getOrDefault(name,SoundEvents.FROG_AMBIENT);}
    private static Block block(String name,Block base,Function<BlockBehaviour.Properties,Block> factory){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("ribbits_"+name));var result=Registry.register(BuiltInRegistries.BLOCK,key,factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key)));BLOCKS.put(name,result);
        item(name,p->name.equals("giant_lilypad")?new PlaceOnWaterBlockItem(result,p.useBlockDescriptionPrefix()):new BlockItem(result,p.useBlockDescriptionPrefix()));return result;
    }
    private static Item item(String name,Function<Item.Properties,Item> factory){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ribbits_"+name));var result=Registry.register(BuiltInRegistries.ITEM,key,factory.apply(new Item.Properties().setId(key)));CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(e->e.accept(result));return result;}
    public static void register(){
        RibbitWorld.register();
        for(var entry:OwnerBackpacks.source("newest_ribbits.json").getAsJsonArray("sounds")){String name=entry.getAsString();var id=Jugcraft.id("ribbits_"+name);SOUNDS.put(name,Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)));}
        for(String name:List.of("brown_toadstool","red_toadstool","toadstool_stem"))block(name,Blocks.MUSHROOM_STEM,HugeMushroomBlock::new);
        var planks=block("mossy_oak_planks",Blocks.OAK_PLANKS,Block::new);
        block("mossy_oak_planks_stairs",Blocks.OAK_STAIRS,p->new StairBlock(planks.defaultBlockState(),p));block("mossy_oak_planks_slab",Blocks.OAK_SLAB,SlabBlock::new);
        block("mossy_oak_planks_fence",Blocks.OAK_FENCE,FenceBlock::new);block("mossy_oak_planks_fence_gate",Blocks.OAK_FENCE_GATE,p->new FenceGateBlock(WoodType.OAK,p));
        block("mossy_oak_door",Blocks.OAK_DOOR,p->new DoorBlock(BlockSetType.OAK,p));
        block("swamp_lantern",Blocks.LANTERN,p->new LanternBlock(p.lightLevel(s->15)));
        block("giant_lilypad",Blocks.LILY_PAD,LilyPadBlock::new);
        block("swamp_daisy",Blocks.OXEYE_DAISY,p->new FlowerBlock(net.minecraft.world.effect.MobEffects.REGENERATION,8,p));
        block("umbrella_leaf",Blocks.FERN,p->new Plant(p));block("toadstool",Blocks.BROWN_MUSHROOM,p->new Plant(p));
        MARACA=item("maraca",p->new Item(p.stacksTo(1)){
            @Override public InteractionResult use(Level level,Player user,InteractionHand hand){user.startUsingItem(hand);return InteractionResult.SUCCESS;}
            @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
            @Override public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack){return net.minecraft.world.item.ItemUseAnimation.BRUSH;}
        });
        for(int i=-1;i<5;i++){
            int profession=i;String name=i<0?"ribbit":"ribbit_"+Ribbit.PROFESSIONS[i];var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(name));
            var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Ribbit>of((t,l)->new Ribbit(t,l,profession),MobCategory.CREATURE).sized(.6F,.95F).clientTrackingRange(8).build(key));TYPES.put(name,type);FabricDefaultAttributeRegistry.register(type,Ribbit.attributes());
            if(i>=0)item(name+"_spawn_egg",p->new SpawnEggItem(p.spawnEgg(type)));
        }
    }
    private static final class Plant extends VegetationBlock {
        Plant(Properties p){super(p.noCollision().instabreak());}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,14,14);}
    }
}
