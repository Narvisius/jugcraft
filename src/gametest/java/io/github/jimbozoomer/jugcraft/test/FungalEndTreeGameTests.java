package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Real seeded placement, obstruction rollback, End growth, renewable drops and shared wood processing. */
public class FungalEndTreeGameTests {
    static final List<String> SHAPES=List.of("huge_glowcap_dome","huge_glowcap_parasol","huge_blight","huge_marrow","huge_bramble","dragonblood","dragonblood_shrub");
    static final BlockPos AT=new BlockPos(51000,100,-51000);
    static Block block(String id){return JugcraftAgriculture.block(id);}
    static Block soil(String shape){return shape.contains("glowcap")?Blocks.MYCELIUM:shape.contains("blight")?Blocks.SOUL_SOIL:shape.contains("marrow")?Blocks.NETHER_WART_BLOCK:shape.contains("bramble")?Blocks.CRIMSON_NYLIUM:Blocks.END_STONE;}
    static boolean grow(ServerLevel level,String shape,long seed,BlockPos at){
        return shape.startsWith("huge_")?new FungalGrowthFeature(shape.substring(5)).place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),at)
            :TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),at);
    }
    static Map<BlockPos,BlockState> snapshot(ServerLevel level,BlockPos at){
        var map=new LinkedHashMap<BlockPos,BlockState>();
        for(var p:BlockPos.betweenClosed(at.offset(-8,0,-8),at.offset(8,15,8)))if(!level.getBlockState(p).isAir())map.put(p.subtract(at).immutable(),level.getBlockState(p));
        return map;
    }
    @GameTest(maxTicks=400)
    public void sevenFormsStayBoundedConnectedAndVaryWithSeed(GameTestHelper h){
        var level=h.getLevel();
        for(String shape:SHAPES){var hashes=new HashSet<Integer>();
            for(long seed:TreeBatchFourBCGameTests.SEEDS){
                TreeBatchTwoGameTests.reset(level,AT,soil(shape));h.assertTrue(grow(level,shape,seed,AT),"Placement failed "+shape);
                var snap=snapshot(level,AT);hashes.add(snap.hashCode());
                if(shape.contains("glowcap"))h.assertTrue(snap.values().stream().filter(v->v.is(block("glowcap_block"))).count()<=109
                    &&snap.values().stream().filter(v->v.is(Blocks.MUSHROOM_STEM)).count()<=9,"Compost block bound exceeded");
                int height=snap.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow()+1;
                int width=Math.max(snap.keySet().stream().mapToInt(BlockPos::getX).max().orElseThrow()-snap.keySet().stream().mapToInt(BlockPos::getX).min().orElseThrow()+1,
                        snap.keySet().stream().mapToInt(BlockPos::getZ).max().orElseThrow()-snap.keySet().stream().mapToInt(BlockPos::getZ).min().orElseThrow()+1);
                int min=shape.contains("glowcap")?6:shape.equals("dragonblood")?6:shape.contains("shrub")||shape.contains("bramble")?2:4;
                int max=shape.contains("glowcap")?10:shape.equals("dragonblood")?8:shape.contains("shrub")||shape.contains("bramble")?3:13;
                h.assertTrue(height>=min&&height<=max&&width<=(shape.equals("dragonblood")?9:7),"Outside bounds "+shape+" "+height+"x"+width);
                var reached=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(BlockPos.ZERO);reached.add(BlockPos.ZERO);
                while(!queue.isEmpty()){var p=queue.remove();for(var d:Direction.values()){var n=p.relative(d);if(snap.containsKey(n)&&reached.add(n))queue.add(n);}}
                h.assertTrue(reached.size()==snap.size(),"Floating blocks in "+shape);
                for(var state:snap.values())if(state.is(BlockTags.LEAVES))h.assertTrue(state.getValue(LeavesBlock.DISTANCE)<7&&!state.getValue(LeavesBlock.PERSISTENT),"Unsupported foliage "+shape);
                h.assertTrue(level.getBlockState(AT.below()).is(soil(shape)),"Changed the floor "+shape);
                TreeBatchTwoGameTests.reset(level,AT,soil(shape));grow(level,shape,seed,AT);h.assertTrue(snapshot(level,AT).equals(snap),"Not deterministic "+shape);
                Jugcraft.LOGGER.info("[fungal End shape] {} seed {}: {}h {}w {} blocks",shape,seed,height,width,snap.size());
            }
            h.assertTrue(hashes.size()>1,"No seed variation "+shape);
        }
        TreeBatchTwoGameTests.reset(level,AT,Blocks.END_STONE);h.succeed();
    }
    @GameTest(maxTicks=200)
    public void invalidFloorsAndBlockedGrowthPreserveEveryBlock(GameTestHelper h){
        var level=h.getLevel();var at=AT.east(60);
        for(String shape:SHAPES){
            TreeBatchTwoGameTests.reset(level,at,Blocks.STONE);h.assertTrue(!grow(level,shape,1,at),"Accepted stone "+shape);
            TreeBatchTwoGameTests.reset(level,at,soil(shape));level.setBlock(at.above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
            var before=snapshot(level,at);h.assertTrue(!grow(level,shape,1,at),"Accepted obstruction "+shape);h.assertTrue(snapshot(level,at).equals(before),"Partial placement "+shape);
        }
        TreeBatchTwoGameTests.reset(level,at,Blocks.END_STONE);h.succeed();
    }
    @GameTest(maxTicks=200)
    public void bramblesGrowThroughGroundCoverButNeverReplaceWater(GameTestHelper h){
        var level=h.getLevel();var at=AT.east(360);
        TreeBatchTwoGameTests.reset(level,at,Blocks.CRIMSON_NYLIUM);
        for(var p:BlockPos.betweenClosed(at.offset(-3,0,-3),at.offset(3,0,3)))
            level.setBlock(p,((p.getX()+p.getZ())%2==0?block("bramble"):Blocks.WARPED_ROOTS).defaultBlockState(),Block.UPDATE_CLIENTS);
        h.assertTrue(grow(level,"huge_bramble",1,at),"Existing small brambles/roots vetoed growth");
        h.assertTrue(level.getBlockState(at).is(Blocks.CRIMSON_STEM),"Bramble did not replace its seed");
        for(Block obstruction:List.of(Blocks.WATER,Blocks.OAK_LEAVES,Blocks.OAK_SAPLING)){
            TreeBatchTwoGameTests.reset(level,at,Blocks.CRIMSON_NYLIUM);
            level.setBlock(at.above(),obstruction.defaultBlockState(),Block.UPDATE_CLIENTS);
            var before=snapshot(level,at);
            h.assertTrue(!grow(level,"huge_bramble",1,at),"Replaced protected obstruction "+obstruction);
            h.assertTrue(snapshot(level,at).equals(before),"Partial placement around protected obstruction");
        }
        TreeBatchTwoGameTests.reset(level,at,Blocks.END_STONE);h.succeed();
    }
    @GameTest(maxTicks=200)
    public void dragonbloodSaplingsGrowWithoutSkylightAndPreserveEndStone(GameTestHelper h){
        var level=h.getLevel();var at=AT.east(120);var sapling=(SaplingBlock)block("dragonblood_sapling");
        for(Block floor:List.of(Blocks.END_STONE,Blocks.MOSS_BLOCK)){
            TreeBatchTwoGameTests.reset(level,at,floor);
            for(var p:BlockPos.betweenClosed(at.offset(-16,14,-16),at.offset(16,14,16)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
            var state=sapling.defaultBlockState();h.assertTrue(state.canSurvive(level,at),"Valid floor rejected");level.setBlock(at,state,Block.UPDATE_CLIENTS);
            var random=RandomSource.create(109);
            for(int i=0;i<256&&level.getBlockState(at).is(sapling);i++)level.getBlockState(at).randomTick(level,at,random);
            h.assertTrue(level.getBlockState(at).is(block("dragonblood_log"))&&level.getBlockState(at.below()).is(floor),"Dark sapling failed or changed floor");
        }
        for(Block floor:List.of(Blocks.SAND,Blocks.GRAVEL,Blocks.STONE,Blocks.DIRT)){
            level.setBlock(at.below(),floor.defaultBlockState(),Block.UPDATE_CLIENTS);h.assertTrue(!sapling.defaultBlockState().canSurvive(level,at),"Invalid sapling soil");}
        TreeBatchTwoGameTests.reset(level,at,Blocks.END_STONE);h.succeed();
    }
    @GameTest(maxTicks=200, dimension="minecraft:the_nether")
    public void fungiRegrowFromSeedsWithoutCreatingBoneMealLoops(GameTestHelper h){
        var level=h.getLevel();var at=AT.east(180);
        for(String plant:List.of("glowcap","blight_fungus","bramble")){
            String shape=plant.equals("glowcap")?"huge_glowcap_dome":plant.equals("blight_fungus")?"huge_blight":"huge_bramble";
            TreeBatchTwoGameTests.reset(level,at,soil(shape));
            for(var p:BlockPos.betweenClosed(at.offset(-16,14,-16),at.offset(16,14,16)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
            var seed=(GrowingFloorPlantBlock)block(plant);var state=seed.defaultBlockState();level.setBlock(at,state,Block.UPDATE_CLIENTS);
            h.assertTrue(seed.isValidBonemealTarget(level,at,state,BonemealSource.INTERACTION),"Valid growth target rejected "+plant);
            seed.performBonemeal(level,RandomSource.create(1),at,state,BonemealSource.INTERACTION);h.assertTrue(!level.getBlockState(at).is(seed),"Seed did not grow "+plant);
            int successes=0;var random=RandomSource.create(9);for(int i=0;i<4096;i++)if(seed.isBonemealSuccess(level,random,at,state,BonemealSource.INTERACTION))successes++;
            h.assertTrue(successes>850&&successes<1200,"Growth cost drift "+plant);
            var leaves=block(plant.equals("glowcap")?"glowcap_block":plant.equals("blight_fungus")?"blight_wart_block":"bramble_bush_leaves");
            int seeds=0;for(int i=0;i<4096;i++)for(var drop:Block.getDrops(leaves.defaultBlockState(),level,at,null))if(drop.is(seed.asItem()))seeds+=drop.getCount();
            h.assertTrue(seeds>0&&seeds<400,"Renewable drop rate drift "+plant);
            var silk=new ItemStack(Items.IRON_AXE);
            silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
            h.assertTrue(!leaves.asItem().components().has(net.minecraft.core.component.DataComponents.COMPOSTABLE),"Harvested cap/leaves gained compost value");
            var kept=Block.getDrops(leaves.defaultBlockState(),level,at,null,null,silk);
            h.assertTrue(kept.size()==1&&kept.getFirst().is(leaves.asItem()),"Silk Touch cap/leaf collection failed");
        }
        // Worst case 109 glow cap blocks + nine Silk-Touch stems: even treating each stem as one whole compost layer
        // and giving the composter its free first layer, expected output remains below four bone meal per growth.
        h.assertTrue((109*.03*.65+9+1)/7.0<4,"Glowcap positive compost loop");
        h.assertTrue(!(Blocks.BONE_BLOCK instanceof BonemealableBlock),"Marrow gained a growth path");
        var fire=FlammableBlockRegistry.getDefaultInstance().get(block("bramble_bush_leaves"));
        h.assertTrue(fire.getIgniteOdds()==0&&fire.getBurnOdds()==0,"Nether brambles burn");
        TreeBatchTwoGameTests.reset(level,at,Blocks.END_STONE);h.succeed();
    }
    @GameTest(maxTicks=100)
    public void cavePlacementFindsRealFloorsAndRejectsSurfaceAndLowCeilings(GameTestHelper h){
        var level=h.getLevel();var at=AT.east(240);var placement=new CaveFungalFloorPlacement();
        var context=new net.minecraft.world.level.levelgen.placement.PlacementContext(level,level.getChunkSource().getGenerator(),Optional.empty());
        var found=new ArrayList<BlockPos>();
        TreeBatchTwoGameTests.reset(level,at,Blocks.MUD);
        placement.modify(context,RandomSource.create(9),at,found::add);h.assertTrue(found.isEmpty(),"Cave placement selected the surface");
        for(var p:BlockPos.betweenClosed(at.offset(-16,14,-16),at.offset(16,14,16)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        placement.modify(context,RandomSource.create(9),at,found::add);h.assertTrue(found.size()==1&&found.getFirst().getY()==at.getY(),"Missed actual cave floor");
        for(var p:BlockPos.betweenClosed(at.offset(-16,3,-16),at.offset(16,3,16)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
        found.clear();placement.modify(context,RandomSource.create(9),at,found::add);h.assertTrue(found.isEmpty(),"Accepted a low cave ceiling");
        TreeBatchTwoGameTests.reset(level,at,Blocks.END_STONE);h.succeed();
    }
    @GameTest
    public void dragonbloodCraftsStripsSawsAndHasRenewableSaplingsAndDye(GameTestHelper h){
        var level=h.getLevel();var relative=new BlockPos(3,3,3);var player=h.makeMockPlayer(GameType.SURVIVAL);var plank=block("dragonblood_planks").asItem();
        for(String form:List.of("log","wood"))for(var axis:Direction.Axis.values()){
            h.setBlock(relative,block("dragonblood_"+form).defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis));var axe=new ItemStack(Items.IRON_AXE);player.setItemInHand(InteractionHand.MAIN_HAND,axe);h.useBlock(relative,player);
            h.assertTrue(h.getBlockState(relative).is(block("stripped_dragonblood_"+form))&&h.getBlockState(relative).getValue(RotatedPillarBlock.AXIS)==axis,"Stripping failed");
        }
        for(String form:List.of("dragonblood_log","dragonblood_wood","stripped_dragonblood_log","stripped_dragonblood_wood")){
            PineWoodGameTests.craft(h,plank,4,1,1,block(form).asItem());var saw=MachineRecipes.find(level,MachineKind.SAWMILL,new ItemStack(block(form))).orElseThrow();h.assertTrue(saw.output().create().is(plank)&&saw.output().create().getCount()==6,"Sawmill failed");}
        var farm=MachineRecipes.find(level,MachineKind.TREE_FARM,new ItemStack(block("dragonblood_sapling"))).orElseThrow();h.assertTrue(farm.output().create().is(block("dragonblood_log").asItem()),"Missing farm route");
        PineWoodGameTests.craft(h,Items.DYE.red(),1,1,1,JugcraftAgriculture.item("dragonblood_resin"));
        int seeds=0,resin=0;for(int i=0;i<4096;i++)for(var drop:Block.getDrops(block("dragonblood_fronds").defaultBlockState(),level,h.absolutePos(relative),null)){
            if(drop.is(block("dragonblood_sapling").asItem()))seeds++;if(drop.is(JugcraftAgriculture.item("dragonblood_resin")))resin++;}
        h.assertTrue(seeds>0&&resin>0,"No renewal or resin");h.succeed();
    }
}
