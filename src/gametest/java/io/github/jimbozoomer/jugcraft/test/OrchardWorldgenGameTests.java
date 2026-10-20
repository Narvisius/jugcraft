package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Batch 14: actual shape envelopes, timber, fruit access, obstruction and processing integration. */
public class OrchardWorldgenGameTests {
    static final List<String> SHAPES = List.of("apple_tree_bees", "pear", "peach", "lemon", "orange");
    private static final BlockPos AT = new BlockPos(23000, 110, -23000);
    private static final long[] SEEDS = {1,2,3,4096,8192,65537,-7,987654321,314159265,-271828182,Long.MAX_VALUE,Long.MIN_VALUE};
    static Block log(String shape) {
        return shape.equals("pear") ? JugcraftAgriculture.block("fruitwood_log") : shape.equals("peach") ? Blocks.CHERRY_LOG : Blocks.OAK_LOG;
    }
    static String tree(String shape) { return shape.startsWith("apple") ? "apple" : shape; }

    @GameTest(maxTicks = 400)
    public void shapesKeepTheirSizesTimberAndHarvestableCrowns(GameTestHelper h) {
        var level=h.getLevel();
        for(String shape:SHAPES) {
            var hashes=new HashSet<Long>();var sizes=new ArrayList<String>();
            for(long seed:SEEDS) {
                TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);
                h.assertTrue(TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),AT),shape+" placement failed "+seed);
                var size=TreeBatchTwoGameTests.measure(level,AT);
                int low=shape.equals("pear")?7:shape.startsWith("apple")?5:4, high=low+1;
                h.assertTrue(size.height()>=low&&size.height()<=high,shape+" height "+size);
                h.assertTrue(size.width()>=4&&size.width()<=(shape.equals("peach")?9:7),shape+" crown width "+size);
                h.assertTrue(size.leaves()>=12,shape+" missing crown");
                int harvestable=0; boolean fork=false;
                for(var p:BlockPos.betweenClosed(AT.offset(-9,0,-9),AT.offset(9,12,9))) {
                    var s=level.getBlockState(p);
                    if(s.is(BlockTags.LOGS)) {
                        h.assertTrue(s.is(log(shape)),shape+" unexpected timber "+s);
                        fork|=p.getX()!=AT.getX()||p.getZ()!=AT.getZ();
                    }
                    if(s.is(BlockTags.LEAVES)) {
                        h.assertTrue(s.is(JugcraftAgriculture.block(tree(shape)+"_leaves")),shape+" wrong leaves");
                        h.assertTrue(!s.getValue(LeavesBlock.PERSISTENT)&&s.getValue(LeavesBlock.DISTANCE)<7,shape+" seed "+seed+" unsupported leaves at "+p+": "+s);
                        if(FruitingLeavesBlock.canFruit(s,level,p))harvestable++;
                    }
                }
                h.assertTrue(harvestable>=4,shape+" has no accessible fruiting underside");
                if(shape.equals("peach")) {
                    h.assertTrue(fork,"Peach must fork away from its trunk");
                    var connected=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(AT);connected.add(AT);
                    while(!queue.isEmpty()) {
                        var current=queue.removeFirst();
                        for(var direction:Direction.values()) {
                            var next=current.relative(direction);
                            if(level.getBlockState(next).is(log(shape))&&connected.add(next))queue.addLast(next);
                        }
                    }
                    h.assertTrue(connected.size()==size.logs(),"Peach branches must connect face-to-face to the rooted trunk");
                }
                hashes.add(size.hash());sizes.add(size.height()+"h/"+size.width()+"w");
                TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);
                TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),AT);
                h.assertTrue(size.equals(TreeBatchTwoGameTests.measure(level,AT)),shape+" seed not reproducible");
            }
            h.assertTrue(hashes.size()>=3,shape+" lacks variation");
            Jugcraft.LOGGER.info("[batch 14 measured] {} {}",shape,sizes);
        }
        h.succeed();
    }

    @GameTest
    public void badSoilAndBlockedTrunksDoNotPlaceTrees(GameTestHelper h) {
        var level=h.getLevel();
        for(String shape:SHAPES) {
            TreeBatchTwoGameTests.reset(level,AT,Blocks.STONE);
            h.assertTrue(!TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(1),AT),shape+" grew on stone");
            TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);
            level.setBlock(AT,Blocks.BEDROCK.defaultBlockState(),Block.UPDATE_CLIENTS);
            h.assertTrue(!TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(1),AT),shape+" replaced obstacle");
            h.assertTrue(level.getBlockState(AT).is(Blocks.BEDROCK)&&TreeBatchTwoGameTests.measure(level,AT).logs()==0,"Partial blocked tree");
        }
        h.succeed();
    }

    @GameTest
    public void pearsAreConsumedAndBottledByTheExistingPress(GameTestHelper h) {
        var pos=new BlockPos(3,2,3);h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,JugcraftAgriculture.block("cider_press"));
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
        var absolute=h.absolutePos(pos);player.setPos(absolute.getX()+.5,absolute.getY(),absolute.getZ()+2);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugcraftAgriculture.item("pear"),2));
        var hit=new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);
        for(int i=0;i<2;i++)player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        var press=h.getBlockEntity(pos,CiderPressBlockEntity.class);
        h.assertTrue(player.getMainHandItem().isEmpty()&&press.apples()==2,"Pear input must be consumed");
        long now=h.getLevel().getGameTime();press.grind(now);press.grind(now+8);
        for(int i=0;i<4;i++)press.turn(now+16+i*8);
        h.assertTrue(press.juice()==2&&press.pulp()==0,"Two pears should produce two servings");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().is(JugcraftAgriculture.item("sweet_cider"))&&press.juice()==1,"Bottling must consume one serving and bottle");h.succeed();
    }

    @GameTest
    public void orchardFruitUsesExistingFermentationAndCandySystems(GameTestHelper h) {
        var ferment=TagKey.create(Registries.ITEM,Jugcraft.id("fermentable"));
        for(String tree:List.of("pear","peach","lemon","orange"))h.assertTrue(new ItemStack(JugcraftAgriculture.item(tree)).is(ferment),tree+" not fermentable");
        var flavour=CandyFlavour.of(new ItemStack(JugcraftAgriculture.item("orange")));
        h.assertTrue(flavour==CandyFlavour.ORANGE&&flavour.effect.equals(MobEffects.JUMP_BOOST)&&flavour.seconds==15,"Orange candy effect");
        h.assertTrue(CandyFlavour.of(new ItemStack(JugcraftAgriculture.item("lemon")))==null,"Do not change lemon candy routing");h.succeed();
    }
    @GameTest
    public void orangesAreConsumedAndRemainInThePouredCandy(GameTestHelper h) {
        var pos=new BlockPos(3,2,3);h.setBlock(pos.below(),Blocks.STONE);h.setBlock(pos,JugcraftAgriculture.block("candy_kettle"));
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
        var absolute=h.absolutePos(pos);player.setPos(absolute.getX()+.5,absolute.getY(),absolute.getZ()+2);
        var hit=new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false);
        for(var item:List.of(Items.MILK_BUCKET,Items.SUGAR,JugcraftAgriculture.item("orange"))) {
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        }
        var kettle=h.getBlockEntity(pos,CandyKettleBlockEntity.class);
        h.assertTrue(player.getMainHandItem().isEmpty()&&kettle.flavours().equals(List.of(CandyFlavour.ORANGE)),"Orange must be consumed into flavour");
        kettle.setTemperature(116);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugcraftAgriculture.item("candy_tray")));
        player.gameMode.useItemOn(player,h.getLevel(),player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        var batch=CandyTrayItem.batch(player.getMainHandItem());
        h.assertTrue(batch!=null&&batch.kind()==CandyKind.FUDGE&&batch.pieces()==2&&batch.flavours().equals(List.of(CandyFlavour.ORANGE))&&kettle.empty(),"Poured orange fudge must preserve flavour and empty the kettle");
        h.succeed();
    }

}
