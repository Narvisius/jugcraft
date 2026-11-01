package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.world.level.block.BonemealSource;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class BotanicalFlowers {
    static void register(){
        Registry.register(BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("botanical_flowers"),Natural.CODEC);
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("botanical_flowers")));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(e->Jugtania.BLOCKS.forEach((name,b)->{if((name.contains("flower")||name.contains("mushroom"))&&b.asItem()!=Items.AIR)e.accept(b);}));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->Jugtania.BLOCKS.values().stream().filter(b->b instanceof BotanicalBlock block&&!block.kind.equals("fake_air")&&b.asItem()!=Items.AIR).forEach(e::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(e->Jugtania.ITEMS.forEach((name,i)->{if(!Jugtania.BLOCKS.containsKey(name)&&!name.startsWith("brew_"))e.accept(i);}));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(e->{for(String name:BotanicalBrewing.BREWS.keySet()){e.accept(BotanicalBrewing.filled(name,false));e.accept(BotanicalBrewing.filled(name,true));if(BotanicalIncense.allowed(name)){var stick=new ItemStack(Jugtania.item("incense_stick"));stick.set(BotanicalBrewing.BREW,name);stick.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(BotanicalBrewing.BREWS.get(name).color())));e.accept(stick);}}});
    }
    public static final class Flower extends FlowerBlock implements BonemealableBlock {
        private final String name;
        public Flower(String name,Properties properties){super(MobEffects.REGENERATION,5,properties);this.name=name;}
        @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,BonemealSource source){return name.endsWith("_mystical_flower")&&level.isEmptyBlock(pos.above());}
        @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source){return true;}
        @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source){
            if(!isValidBonemealTarget(level,pos,state,source))return;
            var tall=Jugtania.block(name.replace("_mystical_flower","_double_flower"));
            if(tall instanceof DoublePlantBlock)DoublePlantBlock.placeAt(level,tall.defaultBlockState(),pos,3);
        }
    }
    public static final class Fertilizer extends Item {
        public Fertilizer(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;var player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
            var origin=context.getClickedPos().above();boolean changed=false;
            for(int i=0;i<32;i++){
                var pos=origin.offset(level.getRandom().nextInt(7)-3,level.getRandom().nextInt(3)-1,level.getRandom().nextInt(7)-3);
                if(!level.hasChunkAt(pos)||!level.isEmptyBlock(pos)||!Authority.mayChange(level,player,pos))continue;
                var state=randomFlower(level.getRandom());if(state.canSurvive(level,pos))changed|=level.setBlock(pos,state,3);
            }
            if(changed){if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);level.levelEvent(1505,origin,0);return InteractionResult.SUCCESS;}return InteractionResult.FAIL;
        }
    }
    private static BlockState randomFlower(RandomSource random){return Jugtania.block(Jugtania.COLORS.get(random.nextInt(16))+"_mystical_flower").defaultBlockState();}
    record Natural() implements Feature {
        static final MapCodec<Natural> CODEC=MapCodec.unit(Natural::new);
        @Override public MapCodec<Natural> codec(){return CODEC;}
        @Override public boolean place(WorldGenLevel level,ChunkGenerator generator,RandomSource random,BlockPos origin){
            boolean changed=false;for(int i=0;i<16;i++){
                var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
                if(!level.ensureCanWrite(pos)||!level.isEmptyBlock(pos))continue;var state=randomFlower(random);if(state.canSurvive(level,pos))changed|=level.setBlock(pos,state,2);
            }return changed;
        }
    }
}
