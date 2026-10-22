package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class CandyCavityGameTests {
    @GameTest(maxTicks=20)
    public void candyTerrainAndTreePillarsAreSolidUprightBlocks(GameTestHelper h) {
        for(String id:List.of("cake_layer","block_of_chocolate","licoroot","candy_cane_block","stripped_candy_cane_block")) {
            var state=Menagerie.BLOCKS.get("cavern_"+id).defaultBlockState();var at=h.absolutePos(new BlockPos(1,1,1));
            h.assertTrue(state.isCollisionShapeFullBlock(h.getLevel(),at),"Full-block collision, not furniture slab: "+id);
            h.assertTrue(state.isSolidRender(),"Opaque cubes cull internal terrain faces: "+id);
            h.assertTrue(state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)==net.minecraft.core.Direction.Axis.Y,"Upright default texture orientation: "+id);
        }
        h.succeed();
    }
    @GameTest(maxTicks=20)
    public void cakeTerracesAndSodaRiversHaveContinuousSupportedFloors(GameTestHelper h) {
        for(long seed:new long[]{1,42,-571019}) {
            Set<Integer> terraces=new HashSet<>();int candy=0;
            for(int x=-320;x<=320;x+=4)for(int z=-160;z<=160;z+=4) {
                var c=CandyCavity.column(seed,x,z);terraces.add(c.floor());
                h.assertTrue(c.roof()-c.floor()>=30,"Tall cavern clearance");
                h.assertTrue(c.floor()>-53&&c.roof()<35,"No bedrock/surface excavation");
                h.assertTrue(!CandyCavity.terrain(seed,x,c.floor(),z,c).isAir(),"Solid terrace top");
                for(int y=-51;y<c.floor();y++)if(CandyCavity.terrain(seed,x,y,z,c).is(Menagerie.BLOCKS.get("cavern_rock_candy_pink")))candy++;
            }
            h.assertTrue(terraces.size()>10&&candy>0,"Multiple terraces, banks and embedded candy veins");
            for(int band=-1;band<=1;band++)for(int x=-257;x<=257;x++) {
                int z=(int)Math.round(CandyCavity.riverZ(seed,x,band));
                var c=CandyCavity.column(seed,x,z);
                h.assertTrue(c.river()&&c.floor()<CandyCavity.SODA_Y-1,"Connected deep river channel");
                var state=CandyCavity.terrain(seed,x,CandyCavity.SODA_Y,z,c);
                h.assertTrue(!state.getFluidState().isEmpty()&&state.getFluidState().isSource(),"Real bucketable soda, not a purple placeholder");
                h.assertTrue(!CandyCavity.terrain(seed,x,c.floor(),z,c).getFluidState().isSource(),"Sealed river bottom");
                h.assertTrue(Math.abs(CandyCavity.riverZ(seed,x+1,band)-z)<2,"River remains joined across positive/negative chunk boundaries");
            }
        }
        h.succeed();
    }
    @GameTest(maxTicks=20)
    public void suppliedCandyTemplatesLoadAndLandmarkPiecesSurviveSerialization(GameTestHelper h) {
        var manager=h.getLevel().getStructureTemplateManager();
        for(String size:List.of("big","small"))for(int v=0;v<(size.equals("big")?5:4);v++)for(int c=0;c<6;c++) {
            var t=manager.get(Jugcraft.id("cavern/candy/"+size+"_lollipop_"+v+"_"+c)).orElseThrow();
            h.assertTrue(t.getSize().getY()>=5,"Supplied lollipop geometry loads");
        }
        h.assertTrue(manager.get(Jugcraft.id("cavern/candy/soda_bottle")).orElseThrow().getSize().getZ()==26,"Supplied sideways bottle loads");
        var context=StructurePieceSerializationContext.fromLevel(h.getLevel());
        for(String name:List.of("cavern_licowitch_tower","cavern/landmarks/gingerbread_town")) {
            var piece=new CandyLandmark.Piece(manager,Jugcraft.id(name),new BlockPos(-43,-31,57));
            var saved=piece.createTag(context);var loaded=CandyLandmark.PIECE.load(context,saved);
            h.assertTrue(piece.getBoundingBox().equals(loaded.getBoundingBox()),"Landmark chunk bounds survive save/load");
            h.assertTrue(loaded instanceof CandyLandmark.Piece,"Native landmark type restored");
        }
        var structures=h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for(String name:List.of("cavern_licowitch_tower","cavern_gingerbread_town"))
            h.assertTrue(structures.getValue(Jugcraft.id(name)) instanceof CandyLandmark,"Natural structures use floor-aware placement");
        h.succeed();
    }
}
