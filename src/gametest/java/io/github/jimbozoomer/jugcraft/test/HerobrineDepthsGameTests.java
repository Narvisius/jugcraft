package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.herobrine.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.NbtOps;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.finance.Ledger;
import java.util.UUID;

/** Real world placement, persistence, fluid and player-edit regression tests for permanent discoveries. */
public class HerobrineDepthsGameTests {
    private static final String ARENA = "jugcraft-test:tatterlace_arena";
    private static BlockPos fixture(GameTestHelper h, boolean water) {
        for (int x = 1; x < 47; x++) for (int z = 1; z < 47; z++) {
            h.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            h.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            if (water) for (int y = 5; y <= 11; y++) h.setBlock(new BlockPos(x, y, z), Blocks.WATER);
        }
        return h.absolutePos(new BlockPos(24, 5, 18));
    }
    private static void build(GameTestHelper h, String variant, boolean water) {
        var level = h.getLevel();
        var at = fixture(h, water);
        if (variant.equals("broken_bridge") || variant.equals("balcony")) {
            for (int x=-5;x<=5;x++) for(int z=2;z<=8;z++) for(int y=-2;y<0;y++)
                level.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        var plan = HerobrineDepths.plan(level, at, variant);
        h.assertTrue(plan != null, variant + " must have a valid layout");
        String id = HerobrineMarks.buildNow(level, plan);
        var mark = HerobrineMarks.get(level.getServer()).marks().get(id);
        h.assertTrue(mark != null && mark.finished(), variant + " must finish all placements");
        h.assertTrue(!mark.changes().isEmpty(), "Must construct actual world blocks");
        if(variant.equals("broken_bridge")||variant.equals("balcony")) {
            h.assertTrue(level.getBlockState(at.offset(0,-1,3)).is(Blocks.CRACKED_STONE_BRICKS),"Deck spans the ravine");
            h.assertTrue(level.getBlockState(at.offset(0,-2,3)).isAir(),"Span remains above open air");
        }
        h.assertTrue(!HerobrineDepths.spaced(level, at, water), "Saved site prevents duplicate placement");
        h.assertTrue(HerobrineDepths.spaced(level, at.below(40), water), "A separate cave depth remains eligible");
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = HerobrineMarks.CODEC.encodeStart(ops, HerobrineMarks.get(level.getServer())).getOrThrow();
        var restored = HerobrineMarks.CODEC.parse(ops, encoded).getOrThrow();
        h.assertTrue(restored.marks().get(id).finished(), "The permanent site survives SavedData round trip");
        var changed = mark.changes().getFirst().pos();
        level.setBlock(changed, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        HerobrineMarks.undo(level, id);
        h.assertTrue(level.getBlockState(changed).is(Blocks.DIAMOND_BLOCK), "Explicit operator undo preserves player edits");
        h.succeed();
    }
    @GameTest(structure=ARENA, padding=48, skyAccess=true)
    public void naturalSoilsRemainWildWithoutAcceptingFarmland(GameTestHelper h) {
        for(var block:java.util.List.of(Blocks.GRASS_BLOCK,Blocks.PODZOL,Blocks.MYCELIUM,Blocks.MOSS_BLOCK,Blocks.PALE_MOSS_BLOCK))
            h.assertTrue(HerobrineMarkPlans.natural(block.defaultBlockState()),"Natural surface soil: "+block);
        h.assertTrue(!HerobrineMarkPlans.natural(Blocks.FARMLAND.defaultBlockState()),"Player farmland is still protected");
        h.assertTrue(!HerobrineMarkPlans.natural(Blocks.OAK_PLANKS.defaultBlockState()),"Player building materials stay protected");
        h.succeed();
    }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void torchTrail(GameTestHelper h) { build(h,"torch_trail",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void shelter(GameTestHelper h) { build(h,"shelter",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void watchingRoom(GameTestHelper h) { build(h,"watching_room",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void sealedStairs(GameTestHelper h) { build(h,"sealed_stairs",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void pillarGrid(GameTestHelper h) { build(h,"pillar_grid",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void brokenBridge(GameTestHelper h) { build(h,"broken_bridge",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void balcony(GameTestHelper h) { build(h,"balcony",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void buriedShrine(GameTestHelper h) { build(h,"buried_shrine",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void emptyHall(GameTestHelper h) { build(h,"empty_hall",false); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void underwaterRing(GameTestHelper h) { build(h,"glowstone_ring",true); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void underwaterCross(GameTestHelper h) { build(h,"glowstone_cross",true); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void underwaterPyramid(GameTestHelper h) { build(h,"glowstone_pyramid",true); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true) public void underwaterTrail(GameTestHelper h) { build(h,"glowstone_trail",true); }
    @GameTest(structure=ARENA, padding=48, skyAccess=true)
    public void sourceWaterAndPlayerObstructions(GameTestHelper h) {
        var at = fixture(h, true); var level = h.getLevel();
        h.assertTrue(HerobrineDepths.plan(level,at,"glowstone_cross") != null,"Source water accepts pattern");
        level.setBlock(at,Blocks.LAVA.defaultBlockState(),3);
        h.assertTrue(HerobrineDepths.plan(level,at,"glowstone_cross") == null,"No lava replacement");
        level.setBlock(at,Blocks.WATER.defaultBlockState(),3);
        level.setBlock(at.above(),Blocks.CHEST.defaultBlockState(),3);
        h.assertTrue(HerobrineDepths.plan(level,at,"glowstone_cross") == null,"A player's container blocks the whole site");
        h.assertTrue(HerobrineDepths.plan(level,new BlockPos(20000000,0,20000000),"empty_hall") == null,"Never load distant chunks");
        h.succeed();
    }
    @GameTest(structure=ARENA, padding=48, skyAccess=true)
    public void changedWaterStopsTheQueuedPattern(GameTestHelper h) {
        var at=fixture(h,true); var level=h.getLevel();
        var plan=HerobrineDepths.plan(level,at,"glowstone_ring");
        h.assertTrue(plan!=null,"Ring fixture");
        BlockPos first=plan.steps().getFirst().pos();
        var flowing=Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,1);
        level.setBlock(first,flowing,3);
        String id=HerobrineMarks.buildNow(level,plan);
        h.assertTrue(level.getBlockState(first).equals(flowing),"Apply must reject changed source-water state");
        h.assertTrue(!HerobrineMarks.get(level.getServer()).marks().containsKey(id),"No blocks changed on refusal");
        h.succeed();
    }
    @GameTest(structure=ARENA, padding=48, skyAccess=true)
    public void claimsBlockPlanningAndQueuedPlacement(GameTestHelper h) {
        var at=fixture(h,false); var level=h.getLevel();
        var plan=HerobrineDepths.plan(level,at,"torch_trail");
        h.assertTrue(plan!=null,"Unclaimed trail fixture");
        var registry=LandRegistry.get(level.getServer()); var book=Ledger.get(level.getServer()).book();
        long key=LandRegistry.keyOf(plan.steps().getFirst().pos());
        var admin=h.makeMockServerPlayerInLevel();
        h.assertTrue(registry.assign(book,key,book.account(UUID.randomUUID(),"DepthClaimTest"),0).ok(),"Claim fixture");
        try {
            h.assertTrue(HerobrineDepths.plan(level,at,"torch_trail")==null,"Claim prevents planning");
            String id=HerobrineMarks.buildNow(level,plan);
            var mark=HerobrineMarks.get(level.getServer()).marks().get(id);
            h.assertTrue(mark==null||!mark.finished(),"Claim after planning aborts construction");
            h.assertTrue(level.getBlockState(at.offset(-1,0,0)).isAir(),"No torch written into newly claimed land");
        } finally { registry.abandon(admin,book,key,true,0); }
        h.succeed();
    }
    @GameTest(structure=ARENA, padding=48, skyAccess=true)
    public void excavationNeedsApprovalAndNeverEatsOre(GameTestHelper h) {
        var at=fixture(h,false); var level=h.getLevel();
        level.setBlock(at.offset(0,2,4),Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(HerobrineDepths.plan(level,at,"watching_room")==null,"Cannot excavate even plain stone without wilderness approval");
        var approval=HerobrineMarks.approve(level,at,64,"depth-test");
        try {
            h.assertTrue(HerobrineDepths.plan(level,at,"watching_room")!=null,"Approved natural stone can be excavated");
            level.setBlock(at.offset(0,2,4),Blocks.DIAMOND_ORE.defaultBlockState(),3);
            h.assertTrue(HerobrineDepths.plan(level,at,"watching_room")==null,"Approval never permits ore removal");
            level.setBlock(at.offset(0,2,4),Blocks.STONE.defaultBlockState(),3);
            var plan=HerobrineDepths.plan(level,at,"watching_room");
            HerobrineMarks.revoke(level,approval);
            String id=HerobrineMarks.buildNow(level,plan);
            h.assertTrue(level.getBlockState(at.offset(0,2,4)).is(Blocks.STONE),"Revocation is rechecked while applying");
            var mark=HerobrineMarks.get(level.getServer()).marks().get(id);
            h.assertTrue(mark==null||!mark.finished(),"Aborted construction cannot be reported finished");
        } finally { HerobrineMarks.revoke(level,approval); }
        h.succeed();
    }
    @GameTest(structure=ARENA, padding=48, skyAccess=true, maxTicks=100)
    public void noAutomaticExpiryOrInventoryRemoval(GameTestHelper h) {
        var at=fixture(h,false); var level=h.getLevel();
        var plan=HerobrineDepths.plan(level,at,"shelter");
        h.assertTrue(plan!=null,"Shelter fixture");
        String id=HerobrineMarks.buildNow(level,plan);
        var stove=at.offset(-2,0,6);
        ((FurnaceBlockEntity)level.getBlockEntity(stove)).setItem(0,new ItemStack(Items.DIAMOND));
        var player=h.makeMockServerPlayerInLevel(); player.snapTo(at.getX(),at.getY(),at.getZ(),0,0);
        h.runAfterDelay(40,()->{
            h.assertTrue(HerobrineMarks.get(level.getServer()).marks().containsKey(id),"Approaching never removes the structure");
            HerobrineMarks.undo(level,id);
            h.assertTrue(level.getBlockEntity(stove) instanceof FurnaceBlockEntity f && f.getItem(0).is(Items.DIAMOND),"Operator undo cannot remove an adopted furnace inventory");
            h.succeed();
        });
    }
}
