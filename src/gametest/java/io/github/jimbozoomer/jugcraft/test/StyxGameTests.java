package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Calendar;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.garden.Garden;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.styx.*;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.phys.Vec3;

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

	// ---------------------------------------------------------------- Concordance Chunk D, D-03: Stolas's Regard
	private static final String FULL_MOON="jugcraft:full_moon";
	/** Midnight of day 0: the full moon is up (as in ConcordanceSkyGameTests). */
	private static final long NIGHT_ZERO=18000L;
	/** A sky in which only the full moon can be seen, so no test waits on the weather or the other patterns. */
	private static final Function<Pattern,String> ONLY_MOON=pattern -> pattern.id().equals(FULL_MOON) ? null : "no_sky";

	/** Styxhexenhammer in the test's corner, placed as cuttingsAreAtomicPerPlayerAndRequireProximity places him. */
	private static Styxhexenhammer wizard(GameTestHelper h) {
		var level=h.getLevel();
		var wizard=JugcraftStyx.WIZARD.create(level,EntitySpawnReason.COMMAND);
		h.assertTrue(wizard!=null,"Wizard creates on a server");
		BlockPos p=h.absolutePos(new BlockPos(1,2,1));wizard.snapTo(p.getX(),p.getY(),p.getZ(),0,0);level.addFreshEntity(wizard);
		return wizard;
	}

	/** A listener a block from him who has had their cuttings, so he talks; they leave when the test ends. */
	private static ServerPlayer listener(GameTestHelper h, Styxhexenhammer wizard) {
		ServerPlayer player=TestPlayers.mock(h);
		player.snapTo(wizard.getX()+1,wizard.getY(),wizard.getZ(),0,0);
		StyxState.get(h.getLevel()).claim(player.getUUID());
		return player;
	}

	/** What he says to {@code listener} after his own line, as translation keys; his line still comes first. */
	private static List<String> hint(GameTestHelper h, Styxhexenhammer wizard, ServerPlayer listener) {
		List<String> said=wizard.says(listener).stream().map(line -> line.getContents() instanceof TranslatableContents t ? t.getKey() : "").toList();
		h.assertTrue(!said.isEmpty() && said.getFirst().startsWith("message.jugcraft.styx.line."),"His own line comes first: "+said);
		return said.subList(1,said.size());
	}

	/**
	 * Concordance Chunk D, D-03: after his line, Styx tells each listener their own next step toward Stolas's Regard, read
	 * from their own research, and the sky step until they have recorded an observation. A stranger to binding hears his
	 * line alone. Two listeners hear their own steps; the second, taking the assay first, is still asked to examine, and
	 * the kept assay counts once the earlier steps are done.
	 */
	@GameTest(maxTicks = 20) public void styxHintsByTheListenersOwnProgress(GameTestHelper h) {
		var wizard=wizard(h);
		ServerPlayer first=listener(h,wizard), second=listener(h,wizard);
		h.assertValueEqual(hint(h,wizard,first),List.of(),"A stranger to binding hears only his line");
		ConcordanceProgress.grant(first,Workers.RESEARCH,ResearchState.ENCOUNTERED);
		h.assertValueEqual(hint(h,wizard,first),List.of("message.jugcraft.styx.stolas.binding","message.jugcraft.styx.stolas.sky"),
				"Begun the Binding Arts: understand them, and the sky");
		ConcordanceProgress.grant(first,Workers.RESEARCH,ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(second,Workers.RESEARCH,ResearchState.UNDERSTOOD);
		h.assertValueEqual(hint(h,wizard,first),List.of("message.jugcraft.styx.stolas.examine","message.jugcraft.styx.stolas.sky"),"Understood: examine");
		ConcordanceProgress.record(first,new Evidence.Examined("minecraft:feather",15));
		h.assertValueEqual(hint(h,wizard,first).getFirst(),"message.jugcraft.styx.stolas.cultivate","A feather examined: harvest");
		ConcordanceProgress.record(first,new Evidence.Practiced(Garden.ACTIVITY,"jugcraft:sunpetal"));
		h.assertValueEqual(hint(h,wizard,first).getFirst(),"message.jugcraft.styx.stolas.assay","A crop harvested: dissolve");
		h.assertValueEqual(hint(h,wizard,second),List.of("message.jugcraft.styx.stolas.examine","message.jugcraft.styx.stolas.sky"),
				"The other listener hears their own step");
		ConcordanceProgress.record(second,new Evidence.Practiced(Assaying.ACTIVITY,"minecraft:raw_iron"));
		h.assertValueEqual(hint(h,wizard,second).getFirst(),"message.jugcraft.styx.stolas.examine","An early assay is kept, but the examination comes first");
		ConcordanceProgress.record(second,new Evidence.Examined("jugcraft:stolas_starflower",15));
		ConcordanceProgress.record(second,new Evidence.Practiced(Garden.ACTIVITY,"jugcraft:dewmoss"));
		h.assertValueEqual(ConcordanceProgress.knowledge(second).state(StolasRegard.RESEARCH),ResearchState.UNDERSTOOD,
				"The kept assay counts once the harvest is done");
		h.assertValueEqual(hint(h,wizard,second),List.of("message.jugcraft.styx.stolas.serve","message.jugcraft.styx.stolas.sky"),"Understood: invite him");
		AstralClaims.of(h.getLevel().getServer()).observe(second.getUUID(),FULL_MOON,h.getLevel().getGameTime());
		h.assertValueEqual(hint(h,wizard,second),List.of("message.jugcraft.styx.stolas.serve"),"An observation recorded: the sky goes unsaid");
		ConcordanceProgress.grant(second,StolasRegard.RESEARCH,ResearchState.MASTERED);
		h.assertValueEqual(hint(h,wizard,second),List.of("message.jugcraft.styx.stolas.kept"),"Mastered: he has heard of it");
		h.assertValueEqual(hint(h,wizard,first),List.of("message.jugcraft.styx.stolas.assay","message.jugcraft.styx.stolas.sky"),
				"The first listener's step is still their own");
		wizard.discard();h.succeed();
	}

	/**
	 * Concordance Chunk D, D-03: talking teaches nothing. A listener with the Binding Arts understood and an assay already
	 * kept talks to him through the game's own interaction: their research and evidence, the codex's advancement, their sky
	 * record and their inventory are as before. With the Concordance off he says only his line.
	 */
	@GameTest(maxTicks = 20) public void talkingGrantsNoResearch(GameTestHelper h) {
		var level=h.getLevel();var wizard=wizard(h);
		ServerPlayer player=listener(h,wizard);
		ConcordanceProgress.grant(player,Workers.RESEARCH,ResearchState.UNDERSTOOD);
		ConcordanceProgress.record(player,new Evidence.Practiced(Assaying.ACTIVITY,"minecraft:raw_iron"));
		Knowledge before=ConcordanceProgress.knowledge(player);
		var inventory=player.getInventory();var carried=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
		for(int i=0;i<inventory.getContainerSize();i++)carried.add(inventory.getItem(i).copy());
		h.assertTrue(wizard.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO).consumesAction(),"He answers");
		h.assertTrue(ConcordanceProgress.knowledge(player).equals(before),"Talking records nothing");
		h.assertValueEqual(ConcordanceProgress.knowledge(player).state(StolasRegard.RESEARCH),ResearchState.NONE,"Stolas's Regard is not begun by talk");
		AdvancementHolder begun=level.getServer().getAdvancements().get(ConcordanceProgress.advancementFor(StolasRegard.RESEARCH,ResearchState.ENCOUNTERED));
		h.assertTrue(begun!=null && !player.getAdvancements().getOrStartProgress(begun).isDone(),"No codex page opens by talk");
		h.assertTrue(AstralClaims.of(level.getServer()).observed(player.getUUID()).isEmpty(),"He records no sky observation");
		boolean unchanged=true;for(int i=0;i<carried.size();i++)unchanged&=net.minecraft.world.item.ItemStack.matches(carried.get(i),inventory.getItem(i));
		h.assertTrue(unchanged,"He gives nothing: the cuttings were given before (what they carry is as it was)");
		boolean on=JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		try {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,false);
			h.assertValueEqual(hint(h,wizard,player),List.of(),"With the Concordance off he says only his line");
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,on);
		}
		wizard.discard();h.succeed();
	}

	/**
	 * Concordance Chunk D, D-03: the sky step is no research, so Styx names it apart, whatever the listener's research,
	 * until they have recorded an observation at an Orrery Observatory. Looking when nothing is up records nothing; the full
	 * moon observed ends it.
	 */
	@GameTest(maxTicks = 20) public void styxNamesTheSkyStepUntilOneIsRecorded(GameTestHelper h) {
		var level=h.getLevel();var wizard=wizard(h);
		ServerPlayer player=listener(h,wizard);
		ConcordanceProgress.grant(player,Workers.RESEARCH,ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player,StolasRegard.RESEARCH,ResearchState.UNDERSTOOD);
		h.assertValueEqual(hint(h,wizard,player),List.of("message.jugcraft.styx.stolas.serve","message.jugcraft.styx.stolas.sky"),"Understood, no star seen");
		BlockPos at=new BlockPos(3,1,3);
		h.setBlock(at,Sky.OBSERVATORY);
		var observatory=(ObservatoryBlockEntity)level.getBlockEntity(h.absolutePos(at));
		long noon=Calendar.DAY+6000;
		h.assertTrue(Sky.up(noon).isEmpty() && observatory.observe(player,level,noon,level.getGameTime(),pattern -> null)==0,"Nothing up at noon: nothing recorded");
		h.assertTrue(hint(h,wizard,player).contains("message.jugcraft.styx.stolas.sky"),"He still names the sky");
		h.assertTrue(observatory.observe(player,level,NIGHT_ZERO,level.getGameTime(),ONLY_MOON)==1,"The full moon observed");
		h.assertValueEqual(hint(h,wizard,player),List.of("message.jugcraft.styx.stolas.serve"),"Then he names it no more");
		wizard.discard();h.succeed();
	}
}
