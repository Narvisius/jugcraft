package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** The complete archwood, masonry and cultivated-plant families use vanilla lifecycles. */
public final class NouveauNature {
    public static final List<String> COLORS=List.of("blue","green","purple","red");
    private static final Set<String> SIMPLE=Set.of("building","log","leaves","sapling","slab","stairs","fence","fence_gate",
        "door","trapdoor","button","pressure_plate","crop","berry","fruit","grate","lamp","bed");
    private NouveauNature(){}
    public static void register(){
        Registry.register(net.minecraft.core.registries.BuiltInRegistries.FEATURE_TYPE,Jugcraft.id("nouveau_archwood"),NouveauArchwoodFeature.CODEC);
        for(var e:NouveauCatalog.BLOCK_ENTRIES.values()){
            if(!SIMPLE.contains(e.kind())||NouveauCatalog.BLOCKS.containsKey(e.source()))continue;
            boolean wood=e.source().contains("archwood");
            var p=BlockBehaviour.Properties.of().strength(wood?2F:1.5F).sound(wood?SoundType.WOOD:SoundType.STONE).mapColor(wood?MapColor.WOOD:MapColor.COLOR_PURPLE);
            NouveauCatalog.registerBlock(e,p,props->switch(e.kind()){
                case "log" -> e.source().startsWith("stripped_")?new RotatedPillarBlock(props):new io.github.jimbozoomer.jugcraft.agriculture.StrippableLogBlock(props,"stripped_"+e.source()){@Override public Block stripped(){return NouveauCatalog.block("stripped_"+e.source());}};
                case "leaves" -> new TintedParticleLeavesBlock(.01F,props.noOcclusion().randomTicks().strength(.2F).sound(SoundType.GRASS));
                case "sapling" -> new SaplingBlock(grower(e.source().replace("_sapling","")),props.noCollision().randomTicks().instabreak().sound(SoundType.GRASS));
                case "slab" -> new SlabBlock(props);
                case "stairs" -> new StairBlock(Blocks.STONE.defaultBlockState(),props);
                case "fence" -> new FenceBlock(props);
                case "fence_gate" -> new FenceGateBlock(WoodType.OAK,props);
                case "door" -> new DoorBlock(BlockSetType.OAK,props.noOcclusion());
                case "trapdoor" -> new TrapDoorBlock(BlockSetType.OAK,props.noOcclusion());
                case "button" -> new ButtonBlock(BlockSetType.OAK,30,props.noCollision()){};
                case "pressure_plate" -> new PressurePlateBlock(BlockSetType.OAK,props.noCollision()){};
                case "crop" -> new Magebloom(props.noCollision().randomTicks().instabreak().sound(SoundType.CROP));
                case "berry" -> new Sourceberries(props.noCollision().randomTicks().strength(.2F).sound(SoundType.SWEET_BERRY_BUSH));
                case "fruit" -> new Archfruit(e.source(),props.noOcclusion().randomTicks().strength(.2F).sound(SoundType.WOOD));
                case "grate" -> new NouveauGrate(props.noOcclusion());
                case "lamp" -> e.source().equals("source_lamp")?new NouveauDecor.SourceLamp(props):e.source().equals("magelight_torch")?new NouveauFixtures.Torch(props.noOcclusion().lightLevel(s->14)):new NouveauFixtures.Sconce(props.noOcclusion().lightLevel(s->14));
                case "bed" -> new NouveauDecor.PetBed(props.noOcclusion());
                default -> e.source().equals("starbuncle_plush")?new NouveauFixtures.Plush(props.noOcclusion()):new Block(props);
            });
        }
        for(var e:NouveauCatalog.BLOCK_ENTRIES.values())if(e.kind().equals("potted"))NouveauCatalog.registerBlock(e,BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT),props->new FlowerPotBlock(NouveauCatalog.block(e.source().substring(7)),props));
        for(var e:NouveauCatalog.BLOCK_ENTRIES.values())if(SIMPLE.contains(e.kind())&&NouveauCatalog.BLOCKS.containsKey(e.source())){
            if(Set.of("berry","fruit").contains(e.kind()))NouveauCatalog.registerItem(e,p->new FruitItem(e.source(),p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(.3F).build()).useBlockDescriptionPrefix()));
            else if(e.kind().equals("sapling"))NouveauCatalog.registerItem(e,p->new BlockItem(NouveauCatalog.block(e.source()),p.compostable(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_LOW).useBlockDescriptionPrefix()));
            else NouveauCatalog.registerBlockItem(e);
        }
        for(String color:COLORS){
            FlammableBlockRegistry.getDefaultInstance().add(NouveauCatalog.block(color+"_archwood_log"),5,5);
            FlammableBlockRegistry.getDefaultInstance().add(NouveauCatalog.block(color+"_archwood_leaves"),30,60);
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(c->c.getBiome().getBaseTemperature()>.2F&&c.getBiome().getBaseTemperature()<1.2F),
                GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("nouveau/"+color+"_archwood")));
        }
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.VEGETAL_DECORATION,
            ResourceKey.create(Registries.PLACED_FEATURE,Jugcraft.id("nouveau/sourceberries")));
    }
    private static final class FruitItem extends BlockItem {
        private final String name;FruitItem(String name,Properties p){super(NouveauCatalog.block(name),p);this.name=name;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,net.minecraft.world.entity.LivingEntity entity){var result=super.finishUsingItem(stack,level,entity);if(!level.isClientSide()){String status=switch(name){case "bastion_pod"->"shielding";case "mendosteen_pod"->"recovery";case "frostaya_pod"->"freezing";case "bombegranate_pod"->"blasting";default->"mana_regen";};NouveauStatuses.give(entity,status,name.equals("bombegranate_pod")?100:600,0,entity);}return result;}
    }
    private static TreeGrower grower(String name){
        var key=ResourceKey.create(Registries.FEATURE,Jugcraft.id("nouveau/"+name));
        return new TreeGrower("jugcraft_nouveau_"+name,WeightedList.of(key),WeightedList.of(),WeightedList.of(),key);
    }
    public static final class Magebloom extends CropBlock {
        public Magebloom(Properties p){super(p);}
        @Override protected ItemLike getBaseSeedId(){return NouveauCatalog.item("magebloom_crop");}
    }
    public static final class Sourceberries extends SweetBerryBushBlock {
        public Sourceberries(Properties p){super(p);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){
            if(s.getValue(AGE)<2)return InteractionResult.PASS;
            if(l instanceof ServerLevel server){
                if(!JugNouveau.enabled()||!Authority.mayChange(server,player,pos))return InteractionResult.FAIL;
                popResource(l,pos,new ItemStack(NouveauCatalog.item("sourceberry_bush"),1+l.getRandom().nextInt(2)+(s.getValue(AGE)==3?1:0)));
                l.setBlock(pos,s.setValue(AGE,1),Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Archfruit extends Block implements BonemealableBlock {
        public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
        public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
        private final String name;
        public Archfruit(String name,Properties p){super(p);this.name=name;registerDefaultState(stateDefinition.any().setValue(AGE,0).setValue(FACING,Direction.NORTH));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AGE,FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return c.getClickedFace().getAxis().isHorizontal()?defaultBlockState().setValue(FACING,c.getClickedFace()):null;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,2,3,13,14,13);}
        @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
            return l.getBlockState(p.relative(s.getValue(FACING).getOpposite())).getBlock() instanceof RotatedPillarBlock;
        }
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos neighbor,BlockState next,RandomSource random){return canSurvive(s,l,p)?s:Blocks.AIR.defaultBlockState();}
        @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(s.getValue(AGE)<2&&r.nextInt(5)==0){l.setBlock(p,s.setValue(AGE,s.getValue(AGE)+1),Block.UPDATE_ALL);NouveauSource.naturalGrowth(l,p,s);}}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
            if(s.getValue(AGE)<2)return InteractionResult.PASS;
            if(l instanceof ServerLevel server){
                if(!JugNouveau.enabled()||!Authority.mayChange(server,player,p))return InteractionResult.FAIL;
                popResource(l,p,new ItemStack(NouveauCatalog.item(name),2));l.setBlock(p,s.setValue(AGE,0),Block.UPDATE_ALL);
            }return InteractionResult.SUCCESS;
        }
        @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s,BonemealSource source){return s.getValue(AGE)<2;}
        @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){return true;}
        @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s,BonemealSource source){l.setBlock(p,s.setValue(AGE,Math.min(2,s.getValue(AGE)+1)),Block.UPDATE_ALL);}
    }
}
