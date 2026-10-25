package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.furniture.*;
import io.github.jimbozoomer.jugcraft.grave.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import java.util.*;

public class ArtisanFurnitureGameTests {
    static FurnitureDesign cabinet(){return new FurnitureDesign("Saved workshop cabinet",FurnitureDesign.stool().parts(),0,true,7,2,1);}
    @GameTest public void hostileDesignsAreRejectedAndCostsComputed(GameTestHelper h){
        var d=cabinet();h.assertTrue(FurnitureDesign.parse(d.json()).equals(d),"Every design field round trips");
        for(String invalid:List.of("", "[]", "[".repeat(1000), d.json().replace("\"schema\":1","\"schema\":2"),d.json().replace("\"seat\":0","\"seat\":99"),d.json().replace("[1,8,1,14,2,14,0,0]","[1,8,1,160,2,14,0,0]")," ".repeat(12001))){
            boolean rejected=false;try{FurnitureDesign.parse(invalid);}catch(RuntimeException e){rejected=true;}h.assertTrue(rejected,"Malformed/oversized design rejected");
        }
        h.assertTrue(d.cost()>FurnitureDesign.stool().cost(),"Storage/light features require additional materials");
        h.assertTrue(!d.shape(Direction.EAST).isEmpty(),"Rotated design has collision");h.succeed();
    }
    @GameTest public void workshopRequiresNearbyBenchAndPaysExactlyOnce(GameTestHelper h){
        var p=DeathGraveGameTests.player(h);var pos=p.blockPosition();h.getLevel().setBlockAndUpdate(pos,ArtisanFurniture.WORKBENCH.defaultBlockState());var d=cabinet();
        h.assertTrue(!ArtisanFurniture.manufacture(p,new ArtisanFurniture.Request(pos,d.json(),false)),"Survival cannot build without materials");
        var paid=DeathGraveGameTests.player(h);paid.getInventory().setItem(0,new ItemStack(ArtisanFurniture.MATERIAL,64));
        h.assertTrue(!ArtisanFurniture.manufacture(paid,new ArtisanFurniture.Request(pos.offset(30,0,0),d.json(),false)),"Remote/unloaded workstation rejected");
        h.assertTrue(ArtisanFurniture.manufacture(paid,new ArtisanFurniture.Request(pos,d.json(),false)),"Nearby valid paid build succeeds");
        h.assertTrue(paid.getInventory().countItem(ArtisanFurniture.MATERIAL)==64-d.cost(),"Cost calculated and consumed on server");
        h.assertTrue(paid.getInventory().countItem(ArtisanFurniture.FURNITURE.asItem())==1,"Exactly one furniture item produced");
        h.assertTrue(!ArtisanFurniture.manufacture(paid,new ArtisanFurniture.Request(pos,d.json(),false)),"Same-tick duplicate request rejected");h.succeed();
    }
    @GameTest public void fullInventoryAndInvalidRecyclingDoNotConsumeItems(GameTestHelper h){
        var p=DeathGraveGameTests.player(h);var pos=p.blockPosition();h.getLevel().setBlockAndUpdate(pos,ArtisanFurniture.WORKBENCH.defaultBlockState());
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(net.minecraft.world.item.Items.STONE,64));p.getInventory().setItem(0,new ItemStack(ArtisanFurniture.MATERIAL,64));
        h.assertTrue(!ArtisanFurniture.manufacture(p,new ArtisanFurniture.Request(pos,cabinet().json(),false)),"Full inventory blocks manufacture");h.assertTrue(p.getInventory().countItem(ArtisanFurniture.MATERIAL)==64,"Failed build keeps payment");
        var other=DeathGraveGameTests.player(h);var broken=new ItemStack(ArtisanFurniture.FURNITURE);broken.set(ArtisanFurniture.DESIGN,"bad");other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,broken);
        h.assertTrue(!ArtisanFurniture.manufacture(other,new ArtisanFurniture.Request(pos,"",true))&&broken.getCount()==1,"Invalid design cannot be converted to paid materials");h.succeed();
    }
    @GameTest public void customStorageDesignAndItemsSurviveSaveAndBreak(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(3,3,3));l.setBlockAndUpdate(pos,ArtisanFurniture.FURNITURE.defaultBlockState());
        var be=(DesignedFurnitureEntity)l.getBlockEntity(pos);be.design(cabinet());be.setItem(0,new ItemStack(Items.DIAMOND,7));
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());be.saveWithFullMetadata(out);
        var restored=new DesignedFurnitureEntity(pos,l.getBlockState(pos));restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()));
        h.assertTrue(restored.design().equals(cabinet())&&restored.getItem(0).getCount()==7,"Save/reload retains design and all inventory");
        var drops=Block.getDrops(l.getBlockState(pos),l,pos,be);h.assertTrue(drops.size()==1&&FurnitureDesign.parse(drops.getFirst().get(ArtisanFurniture.DESIGN)).equals(cabinet()),"Broken furniture keeps its design");
        l.destroyBlock(pos,true);int diamonds=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(diamonds==7,"Breaking custom storage drops contents exactly once");h.succeed();
    }
    @GameTest public void graveRecoversCustomFurnitureAndAircraftWithoutLosingData(GameTestHelper h){
        var p=DeathGraveGameTests.player(h);var item=ArtisanFurniture.stack(cabinet());p.getInventory().setItem(0,item.copy());var aircraft=io.github.jimbozoomer.jugcraft.aircraft.JugcraftAircraft.ITEMS.get("airship");p.getInventory().setItem(1,new ItemStack(aircraft));
        var grave=DeathGraveGameTests.kill(h,p);p.setHealth(20);h.assertTrue(DeathGraves.recover(p,grave.id())==2,"Both custom furniture and aircraft recovered");
        boolean matched=false;for(int i=0;i<p.getInventory().getContainerSize();i++)if(ItemStack.matches(item,p.getInventory().getItem(i)))matched=true;
        h.assertTrue(matched&&p.getInventory().countItem(aircraft)==1,"Design components and aircraft preserved exactly");h.assertTrue(DeathGraves.recover(p,grave.id())==0,"No duplicate recovery");h.succeed();
    }
}
