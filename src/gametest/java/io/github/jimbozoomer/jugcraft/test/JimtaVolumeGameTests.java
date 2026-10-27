package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryVolumes;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public final class JimtaVolumeGameTests {
    @GameTest public void volumesAreOwnedBoundedAndSurviveToolReferenceDeletion(GameTestHelper h){
        var store=FactoryVolumes.get(h.getLevel());var owner=UUID.randomUUID();var other=UUID.randomUUID();
        var at=h.absolutePos(new BlockPos(2,2,2));var v=store.create(owner,at);
        h.assertTrue(v!=null,"New owner has a free survey slot");
        h.assertTrue(!store.resize(v.id(),other,at.above(),false)&&!store.remove(v.id(),other),"Copied references cannot resize or remove someone else's volume");
        h.assertTrue(!store.resize(v.id(),owner,at.offset(32,0,0),false),"Oversized survey is rejected");
        h.assertTrue(store.resize(v.id(),owner,at.offset(31,3,4),false),"32-block inclusive width is supported");
        var ids=new java.util.ArrayList<UUID>();ids.add(v.id());
        for(int i=1;i<16;i++){var next=store.create(owner,at);h.assertTrue(next!=null,"Owner can create up to 16 volumes");ids.add(next.id());}
        h.assertTrue(store.create(owner,at)==null,"Per-owner cap bounds saved world data");
        // The item is not needed to reclaim a persisted volume by its owner's reference.
        for(var id:ids)h.assertTrue(store.remove(id,owner),"Owner can reclaim each volume slot");
        h.assertTrue(!store.remove(v.id(),owner),"Stale reference cannot delete a different volume");h.succeed();
    }
    @GameTest public void linkedVolumeLocksEditingUntilWorkshopReleasesIt(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);
        try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops.BLOCKS.get("filler"));
            var machine=h.getBlockEntity(pos,io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops.Node.class);machine.owner=player.getUUID();
            var store=FactoryVolumes.get(h.getLevel());var at=h.absolutePos(new BlockPos(3,2,2));var volume=store.create(player.getUUID(),at);
            h.assertTrue(store.bind(volume.id(),player,machine),"Owner can bind a paused loaded workshop");
            h.assertTrue(!store.resize(volume.id(),player.getUUID(),at.above(),false)&&!store.plan(volume.id(),player.getUUID(),1,false)&&!store.remove(volume.id(),player.getUUID()),"Live consumer locks resize, plan and deletion");
            h.assertTrue(machine.setArea(at,at.above()),"Authorised replacement survey releases the previous binding");
            h.assertTrue(store.resize(volume.id(),player.getUUID(),at.above(),false)&&store.remove(volume.id(),player.getUUID()),"Released record is editable and reclaimable");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }

}
