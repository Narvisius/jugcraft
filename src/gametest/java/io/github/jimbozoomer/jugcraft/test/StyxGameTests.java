package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.styx.*;
import java.util.HashSet;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;

public final class StyxGameTests {
	@GameTest public void cuttingsAreAtomicPerPlayerAndRequireProximity(GameTestHelper h) {
		var level=h.getLevel();var player=h.makeMockServerPlayerInLevel();
		var wizard=JugcraftStyx.WIZARD.create(level,EntitySpawnReason.COMMAND);
		h.assertTrue(wizard!=null,"Wizard creates on a server");
		BlockPos p=h.absolutePos(new BlockPos(1,2,1));wizard.snapTo(p.getX(),p.getY(),p.getZ(),0,0);level.addFreshEntity(wizard);
		player.snapTo(p.getX()+20,p.getY(),p.getZ(),0,0);
		h.assertTrue(!wizard.giveCuttings(player),"Distant players cannot claim");
		player.snapTo(p.getX()+1,p.getY(),p.getZ(),0,0);
		for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
		for(int i=0;i<7;i++) player.getInventory().setItem(i,ItemStack.EMPTY);
		h.assertTrue(!wizard.giveCuttings(player) && !StyxState.get(level).claimed(player.getUUID()),"Full inventory does not consume claim");
		h.assertTrue(player.getInventory().getItem(0).isEmpty(),"No partial gift");
		player.getInventory().setItem(7,ItemStack.EMPTY);
		h.assertTrue(wizard.giveCuttings(player),"Eight cuttings granted");
		for(int i=0;i<8;i++) h.assertTrue(player.getInventory().getItem(i).is(JugcraftStyx.FLOWERS.get(JugcraftStyx.NAMES[i]).asItem()),"Correct cultivar "+i);
		h.assertTrue(!wizard.giveCuttings(player),"No duplicate claim");
		var other=h.makeMockServerPlayerInLevel();other.snapTo(p.getX(),p.getY(),p.getZ()+1,0,0);
		h.assertTrue(wizard.giveCuttings(other),"Second player gets an independent claim");
		wizard.discard();h.succeed();
	}
	@GameTest public void plantsGrowWithoutChangingTheirIdentity(GameTestHelper h) {
		BlockPos pos=h.absolutePos(new BlockPos(1,1,1));var level=h.getLevel();
		for(var block:JugcraftStyx.FLOWERS.values()) {
			if(!(block instanceof StyxFlowerBlock flower)) continue;
			level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,block.defaultBlockState(),3);
			for(int i=0;i<2;i++) flower.performBonemeal(level,level.getRandom(),pos,level.getBlockState(pos),BonemealSource.INTERACTION);
			h.assertTrue(level.getBlockState(pos).is(block) && level.getBlockState(pos).getValue(StyxFlowerBlock.AGE)==2,"Cutting matures with stable ID");
			flower.performBonemeal(level,level.getRandom(),pos,level.getBlockState(pos),BonemealSource.INTERACTION);
			h.assertTrue(level.getBlockState(pos).getValue(StyxFlowerBlock.AGE)==2,"Propagation preserves the mature plant");
			var drops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos),level,pos,null);
			h.assertTrue(drops.size()==1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount()==1,"Harvest returns one replantable flower");
		}
		h.succeed();
	}
	@GameTest public void tallFlowerDropsOnceAndPotsReturnTheirContents(GameTestHelper h) {
		var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(1,1,1));
		var tall=JugcraftStyx.FLOWERS.get("ravenquill_lupine");
		var lower=tall.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
		var upper=lower.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
		var drops=net.minecraft.world.level.block.Block.getDrops(lower,level,pos,null);
		h.assertTrue(drops.size()==1 && drops.getFirst().is(tall.asItem()),"Only lower half grants a flower");
		h.assertTrue(net.minecraft.world.level.block.Block.getDrops(upper,level,pos.above(),null).isEmpty(),"Upper half cannot duplicate the flower");
		for(var entry:JugcraftStyx.FLOWERS.entrySet()) {
			if(entry.getValue()==tall)continue;
			var pot=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("potted_"+entry.getKey()));
			h.assertTrue(pot instanceof net.minecraft.world.level.block.FlowerPotBlock p && p.getPotted()==entry.getValue(),"Pot preserves cultivar identity");
			var loot=net.minecraft.world.level.block.Block.getDrops(pot.defaultBlockState(),level,pos,null);
			h.assertTrue(loot.size()==2 && loot.stream().anyMatch(i -> i.is(Items.FLOWER_POT)) && loot.stream().anyMatch(i -> i.is(entry.getValue().asItem())),"Pot and flower returned on harvest");
		}
		h.succeed();
	}
	@GameTest public void homeAndClaimsRoundTripThroughSaveCodec(GameTestHelper h) {
		StyxState state=new StyxState();UUID id=UUID.randomUUID();state.origin=java.util.Optional.of(new BlockPos(10,80,20));state.resident=java.util.Optional.of(UUID.randomUUID());state.placed=120;state.layout=StyxConservatory.CURRENT_LAYOUT;state.claim(id);
		var json=StyxState.CODEC.encodeStart(JsonOps.INSTANCE,state).getOrThrow();
		var loaded=StyxState.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
		h.assertTrue(loaded.claimed(id) && loaded.origin.equals(state.origin) && loaded.resident.equals(state.resident) && loaded.placed==120 && loaded.layout==StyxConservatory.CURRENT_LAYOUT,"Home identity, layout, partial build and eligibility survive restart");
		json.getAsJsonObject().addProperty("layout",2);
		h.assertTrue(StyxState.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().layout==2,"Earlier observatory save retains layout 2");
		json.getAsJsonObject().remove("layout");
		var legacy=StyxState.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
		h.assertTrue(legacy.layout==1 && legacy.placed==120 && legacy.claimed(id),"Old saves retain their original layout and progress");
		h.assertTrue(StyxConservatory.routineTarget(BlockPos.ZERO,1,2).equals(new BlockPos(8,15,9)),"Old resident retains its original observatory destination");
		h.assertTrue(StyxConservatory.routineTarget(BlockPos.ZERO,2,2).equals(new BlockPos(12,17,24)),"Layout 2 resident retains its original destination");
		h.succeed();
	}
	@GameTest public void conservatoryDataHasEveryFlowerAndNoDuplicatePosition(GameTestHelper h) {
		var seen=new HashSet<BlockPos>();var flowers=new HashSet<String>();
		for(var b:StyxConservatory.placements()) {
			h.assertTrue(seen.add(b.offset()),"Blueprint positions unique");
			JugcraftStyx.FLOWERS.forEach((name,block) -> {if(b.state().is(block))flowers.add(name);});
		}
		h.assertTrue(flowers.size()==8 && seen.size()>6000,"Complete observatory and greenhouse with eight flower types");
		h.assertTrue(StyxConservatory.placements(1).size()==2270,"Frozen original construction cursor remains compatible");
		h.assertTrue(StyxConservatory.placements(2).size()==6751,"Frozen layout 2 construction cursor remains compatible");
		h.succeed();
	}
	@GameTest public void previewRejectsUnloadedChunksWithoutLoadingThem(GameTestHelper h) {
		BlockPos pos=new BlockPos(260000,80,260000);
		h.assertTrue(!StyxConservatory.loaded(h.getLevel(),pos),"Unloaded setup");
		h.assertTrue(!StyxConservatory.canPlace(h.getLevel(),pos).isEmpty(),"Placement rejects unavailable chunks");
		h.assertTrue(!StyxConservatory.loaded(h.getLevel(),pos),"Validation does not load chunks");h.succeed();
	}
	@GameTest public void townDistrictIsOptInForOldSavesAndDoesNotOverlapCity(GameTestHelper h) {
		var old = new com.google.gson.JsonObject();old.add("origin",BlockPos.CODEC.encodeStart(JsonOps.INSTANCE,new BlockPos(64,70,32)).getOrThrow());
		var legacy = io.github.jimbozoomer.jugcraft.town.TownState.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();
		h.assertTrue(!legacy.styxDistrict(),"Existing towns never gain an automatic land overwrite");
		legacy.place(legacy.origin(),true);
		legacy.markBuilt(new net.minecraft.world.level.ChunkPos(16,6).pack());
		var saved = io.github.jimbozoomer.jugcraft.town.TownState.CODEC.encodeStart(JsonOps.INSTANCE,legacy).getOrThrow();
		var restored = io.github.jimbozoomer.jugcraft.town.TownState.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();
		h.assertTrue(restored.styxDistrict() && restored.built(new net.minecraft.world.level.ChunkPos(16,6).pack()),"District reservation and chunk progress survive restart");
		var town = io.github.jimbozoomer.jugcraft.town.TownData.get();
		for(var block:StyxConservatory.placements(StyxTownDistrict.LAYOUT)) {
			int x=block.offset().getX()+StyxTownDistrict.X,z=block.offset().getZ()+StyxTownDistrict.Z;
			h.assertTrue(x>=town.size && StyxTownDistrict.contains(x,z),"Buildings fit their reserved plot outside all existing city blocks");
		}
		for(int x=StyxTownDistrict.ROAD_X;x<StyxTownDistrict.X;x++) for(int z=94;z<=98;z++) for(int y=1;y<=3;y++)
			h.assertTrue(town.index(x,y,z)<=1,"The road connector does not intersect gate walls or buildings");
		StyxState resident = new StyxState();resident.townHome=true;resident.layout=3;resident.origin=java.util.Optional.of(StyxTownDistrict.home(restored.origin()));
		var home = StyxState.CODEC.encodeStart(JsonOps.INSTANCE,resident).getOrThrow();
		h.assertTrue(StyxState.CODEC.parse(JsonOps.INSTANCE,home).getOrThrow().townHome,"Town-built resident retains its construction mode");
		home.getAsJsonObject().remove("town_home");
		h.assertTrue(!StyxState.CODEC.parse(JsonOps.INSTANCE,home).getOrThrow().townHome,"Earlier manual homes keep manual construction");
		h.succeed();
	}
}
