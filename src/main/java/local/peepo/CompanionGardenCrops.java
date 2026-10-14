package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Explicit special-crop rules. No generic block breaking, world scans or growth acceleration. */
final class CompanionGardenCrops {
    static final int COLUMN_LIMIT=16, BAMBOO_MIN=4;
    private CompanionGardenCrops(){}
    static boolean column(BlockState state){return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO) || state.is(Blocks.CACTUS);}
    static boolean vine(BlockState state){return state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT);}
    static boolean fruit(BlockState state){return state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON);}
    static boolean stem(BlockState state){return state.getBlock() instanceof StemBlock || state.getBlock() instanceof AttachedStemBlock;}
    static boolean supported(BlockState state){return fruit(state) || stem(state) || column(state) || state.is(Blocks.BAMBOO_SAPLING) || vine(state)
        || state.is(Blocks.COCOA) || state.is(Blocks.SWEET_BERRY_BUSH) || state.getBlock() instanceof CranberryBushBlock;}
    static boolean seed(ItemStack stack){return stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.MELON_SEEDS) || stack.is(Items.SUGAR_CANE) || stack.is(Items.BAMBOO) || stack.is(Items.CACTUS)
        || stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(Items.COCOA_BEANS)
        || stack.is(JugcraftAgriculture.item("cranberries"));}
    static Item seedFor(BlockState state){
        if(state.is(Blocks.PUMPKIN_STEM) || state.is(Blocks.ATTACHED_PUMPKIN_STEM))return Items.PUMPKIN_SEEDS;
        if(state.is(Blocks.MELON_STEM) || state.is(Blocks.ATTACHED_MELON_STEM))return Items.MELON_SEEDS;
        if(state.is(Blocks.SUGAR_CANE))return Items.SUGAR_CANE;
        if(state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING))return Items.BAMBOO;
        if(state.is(Blocks.CACTUS))return Items.CACTUS;
        if(state.is(Blocks.SWEET_BERRY_BUSH))return Items.SWEET_BERRIES;
        if(state.is(Blocks.COCOA))return Items.COCOA_BEANS;
        if(vine(state))return Items.GLOW_BERRIES;
        return state.getBlock() instanceof CranberryBushBlock?JugcraftAgriculture.item("cranberries"):Items.AIR;
    }
    static BlockState start(BlockState state){
        if(state.is(Blocks.ATTACHED_PUMPKIN_STEM))return Blocks.PUMPKIN_STEM.defaultBlockState();
        if(state.is(Blocks.ATTACHED_MELON_STEM))return Blocks.MELON_STEM.defaultBlockState();
        if(state.is(Blocks.COCOA))return state.setValue(CocoaBlock.AGE,0);
        if(vine(state))return Blocks.CAVE_VINES.defaultBlockState();
        if(state.is(Blocks.BAMBOO))return Blocks.BAMBOO_SAPLING.defaultBlockState();
        return state.getBlock().defaultBlockState();
    }
    static BlockState seedState(ItemStack stack){
        if(stack.is(Items.PUMPKIN_SEEDS))return Blocks.PUMPKIN_STEM.defaultBlockState();
        if(stack.is(Items.MELON_SEEDS))return Blocks.MELON_STEM.defaultBlockState();
        if(stack.is(Items.SUGAR_CANE))return Blocks.SUGAR_CANE.defaultBlockState();
        if(stack.is(Items.BAMBOO))return Blocks.BAMBOO_SAPLING.defaultBlockState();
        if(stack.is(Items.CACTUS))return Blocks.CACTUS.defaultBlockState();
        if(stack.is(Items.SWEET_BERRIES))return Blocks.SWEET_BERRY_BUSH.defaultBlockState();
        if(stack.is(JugcraftAgriculture.item("cranberries")))return JugcraftAgriculture.block("cranberry_bush").defaultBlockState();
        return null; // Cocoa faces and vine supports are learned from real planted positions.
    }
    static boolean neighborsLoaded(Level level,BlockPos pos){
        if(!level.hasChunkAt(pos))return false;
        for(var side:Direction.Plane.HORIZONTAL)if(!level.hasChunkAt(pos.relative(side)))return false;
        return true;
    }
    static boolean base(Level level,BlockPos pos,BlockState state){
        if(!column(state))return true;
        var below=level.getBlockState(pos.below());return !below.is(state.getBlock()) && !below.is(Blocks.BAMBOO_SAPLING);
    }
    static BlockPos fruitStem(Level level,BlockPos pos,BlockState fruit){
        if(!fruit(fruit) || !neighborsLoaded(level,pos))return null;
        for(var side:Direction.Plane.HORIZONTAL){var at=pos.relative(side);var state=level.getBlockState(at);
            if(state.is(fruit.is(Blocks.PUMPKIN)?Blocks.ATTACHED_PUMPKIN_STEM:Blocks.ATTACHED_MELON_STEM)
                && state.getValue(AttachedStemBlock.FACING)==side.getOpposite())return at;
        }
        return null;
    }
    static boolean ripe(Level level,BlockPos pos,BlockState state){
        if(fruit(state))return fruitStem(level,pos,state)!=null;
        if(column(state)){
            int height=state.is(Blocks.BAMBOO)?BAMBOO_MIN:3;
            for(int i=1;i<height;i++){
                if(!level.hasChunkAt(pos.above(i)))return false;
                var next=level.getBlockState(pos.above(i));
                if(!next.is(state.getBlock()))return state.is(Blocks.CACTUS) && next.is(Blocks.CACTUS_FLOWER) && base(level,pos,state);
            }
            return base(level,pos,state);
        }
        if(state.is(Blocks.SWEET_BERRY_BUSH))return state.getValue(SweetBerryBushBlock.AGE)==SweetBerryBushBlock.MAX_AGE;
        if(state.is(Blocks.COCOA))return state.getValue(CocoaBlock.AGE)==CocoaBlock.MAX_AGE;
        if(vine(state))return CaveVines.hasGlowBerries(state);
        return state.getBlock() instanceof CranberryBushBlock && CranberryBushBlock.isRipe(state);
    }
    record Change(BlockPos pos,BlockState before,BlockState after){}
    record Harvest(List<Change> changes,List<ItemStack> drops,boolean cocoa){}
    /** Called only at completion; drops are stored before any block is changed. */
    static Harvest harvest(PeepoEntity npc,BlockPos pos,BlockState state){
        var level=(ServerLevel)npc.level();if(!neighborsLoaded(level,pos) || !state.canSurvive(level,pos) || !ripe(level,pos,state))return null;
        var changes=new ArrayList<Change>();var drops=new ArrayList<ItemStack>();
        if(fruit(state)){
            drops.addAll(Block.getDrops(state,level,pos,null,npc,ItemStack.EMPTY));
            changes.add(new Change(pos,state,Blocks.AIR.defaultBlockState()));
        }else if(column(state)){
            int height=1;
            while(height<=COLUMN_LIMIT){
                var at=pos.above(height);if(!level.hasChunkAt(at))return null;
                if(!level.getBlockState(at).is(state.getBlock()))break;height++;
            }
            if(height>COLUMN_LIMIT)return null; // Never trigger an unbounded falling-column cascade.
            var above=pos.above(height);var top=level.getBlockState(above);
            if(state.is(Blocks.CACTUS) && top.is(Blocks.CACTUS_FLOWER)){
                drops.addAll(Block.getDrops(top,level,above,null,npc,ItemStack.EMPTY));changes.add(new Change(above,top,Blocks.AIR.defaultBlockState()));
            }
            for(int i=height-1;i>=1;i--){var at=pos.above(i);var part=level.getBlockState(at);
                drops.addAll(Block.getDrops(part,level,at,null,npc,ItemStack.EMPTY));changes.add(new Change(at,part,Blocks.AIR.defaultBlockState()));}
            // The retained bamboo base must be able to grow after a fully mature stalk was cut.
            if(state.is(Blocks.BAMBOO))changes.add(new Change(pos,state,state.setValue(BambooStalkBlock.STAGE,0).setValue(BambooStalkBlock.LEAVES,net.minecraft.world.level.block.state.properties.BambooLeaves.NONE)));
        }else if(state.is(Blocks.SWEET_BERRY_BUSH) || vine(state)){
            Block.dropFromBlockInteractLootTable(level,state.is(Blocks.SWEET_BERRY_BUSH)?BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH:BuiltInLootTables.HARVEST_CAVE_VINE,
                pos,state,null,null,npc,(server,drop)->drops.add(drop));
            changes.add(new Change(pos,state,vine(state)?state.setValue(CaveVines.BERRIES,false):state.setValue(SweetBerryBushBlock.AGE,1)));
        }else if(state.getBlock() instanceof CranberryBushBlock){
            drops.add(new ItemStack(JugcraftAgriculture.item("cranberries"),CranberryBushBlock.PICK_MIN+level.getRandom().nextInt(CranberryBushBlock.PICK_MAX-CranberryBushBlock.PICK_MIN+1)));
            changes.add(new Change(pos,state,state.setValue(CranberryBushBlock.AGE,CranberryBushBlock.PICK_RESET)));
        }else if(state.is(Blocks.COCOA)){
            drops.addAll(Block.getDrops(state,level,pos,null,npc,ItemStack.EMPTY));
            changes.add(new Change(pos,state,state.setValue(CocoaBlock.AGE,0)));
        }
        return new Harvest(changes,drops,state.is(Blocks.COCOA));
    }
}
