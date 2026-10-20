package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

/** Real trees, timber comparisons, winter berries, clipped sprigs and wall/door wreaths in a saved normal world. */
public class HollyClientGameTests extends TreeBatchTwoClientGameTests {
	private final Map<BlockPos,BlockState> saved = new LinkedHashMap<>();
	@Override protected List<String> shapes() { return List.of("holly"); }
	@Override protected List<String> biomes() { return List.of("woodland", "dense_forest"); }
	@Override protected String worldName() { return "Holly Inspection"; }
	@Override protected String exportName() { return "tree-batch-6b-door-face-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_holly_"; }
	@Override protected BlockPos surveySpot(ServerLevel level,BlockPos first,ResourceKey<Biome> biome) {
		for(int[] offset:new int[][]{{0,0},{256,0},{-256,0},{0,256},{0,-256},{256,256},{-256,-256}}) {
			var at=offset[0]==0&&offset[1]==0?first:BiomeClientGameTests.surfaceSpot(level,first.offset(offset[0],0,offset[1]),biome);
			if(at==null) continue;
			var counts=survey(level,at,biome); Jugcraft.LOGGER.info("[holly natural sample] {} {} {}",biome.identifier(),at,counts);
			if(counts.getOrDefault("leaf:jugcraft:holly_leaves",0)>0) return at;
		}
		throw new AssertionError("No natural holly in surveyed "+biome.identifier());
	}
	@Override protected void validateSurvey(String biome,Map<String,Integer> counts) {
		if(counts.getOrDefault("leaf:jugcraft:holly_leaves",0)==0) throw new AssertionError("Missing holly in "+biome);
	}
	@Override protected void beforeReopenValidation(ClientGameTestContext context, TestSingleplayerContext world) {
		// The tree spawn is outside the wreath display's entity-loading distance. Visit the saved decorations first.
		world.getServer().runCommand("tp @a 1050.5 180 -865.5 135 0");
		world.getConnection().waitForChunksRender();
		context.waitFor(client -> client.level.getEntitiesOfClass(DoorWreath.class,
				new AABB(GARDEN.offset(50,1,131)).inflate(12)).size() == 3, 200);
	}
	@Override protected void validateReopened(ServerLevel level) {
		saved.forEach((pos,state)-> {
			var now=level.getBlockState(pos);
			if(state.hasProperty(LeavesBlock.DISTANCE) && now.is(state.getBlock())) now=now.setValue(LeavesBlock.DISTANCE,state.getValue(LeavesBlock.DISTANCE));
			if(!now.equals(state)) throw new AssertionError("Holly/wreath state changed across save: "+pos+" expected "+state+" got "+now);
		});
		for (int offset : new int[]{46,54}) {
			var support=GARDEN.offset(offset,1,131);
			var rings=level.getEntitiesOfClass(DoorWreath.class,new AABB(support).inflate(1),e->e.getPos().equals(support));
			if(rings.size()!=(offset==46?1:2)) throw new AssertionError("Door wreaths lost across save/reopen: "+offset+" "+rings.size());
			for(var ring:rings) {
				if(!ring.wreathState().is(JugcraftAgriculture.block(ring.isFront()?"holly_wreath":"autumn_wreath"))) throw new AssertionError("Saved wreath changed physical face");
				if(!ring.isFront() && ring.wreathState().getValue(AutumnWreathBlock.FLOWERS)!=AutumnWreathBlock.Mums.RED) throw new AssertionError("Saved wreath lost mums");
				if(ring.isFront() && new AABB(support).contains(ring.getBoundingBox().getCenter())!=(offset==54)) throw new AssertionError("Saved wreath switched panel faces");
			}
		}
	}
	@Override protected void extraDisplays(ClientGameTestContext context,TestSingleplayerContext world,List<String> tour) {
		var wood=GARDEN.offset(0,0,80); var wall=GARDEN.offset(0,0,130);
		world.getServer().runOnServer(server->{
			var level=server.overworld();
			for(var p:BlockPos.betweenClosed(wood.offset(-25,-1,-12),wall.offset(70,-1,25))) level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level,wood,0,new String[]{"jugcraft:holly","minecraft:birch","minecraft:pale_oak","jugcraft:pine","jugcraft:eucalyptus","jugcraft:palm"});
			for(int season=0;season<3;season++) for(boolean clipped:List.of(false,true)) {
				var foliage=SeasonalLeavesBlock.Foliage.values()[season]; var at=wall.offset(season*9,clipped?1:4,0);
				for(int x=0;x<4;x++) for(int y=0;y<2;y++) {
					var pos=at.offset(x,y,0); var state=JugcraftAgriculture.block("holly_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(SeasonalLeavesBlock.SEASON,foliage).setValue(HollyLeavesBlock.CLIPPED,clipped);
					level.setBlock(pos,state,Block.UPDATE_CLIENTS); level.setBlock(pos,state,Block.UPDATE_CLIENTS); saved.put(pos,state);
				}
				PineWoodClientGameTests.label(level,at.offset(1,-1,2),foliage.name(),clipped?"Clipped appearance":"Evergreen / berries","Display only");
			}
			// One supported natural clipping demonstration, separate from placed decorative panels.
			var clip=wall.offset(32,1,0); level.setBlock(clip.below(),JugcraftAgriculture.block("holly_log").defaultBlockState(),Block.UPDATE_CLIENTS);
			var natural=JugcraftAgriculture.block("holly_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(SeasonalLeavesBlock.SEASON,SeasonalLeavesBlock.Foliage.BARE);
			level.setBlock(clip,natural,Block.UPDATE_CLIENTS); level.setBlock(clip,natural,Block.UPDATE_CLIENTS); saved.put(clip,level.getBlockState(clip));
			PineWoodClientGameTests.label(level,clip.offset(0,-1,2),"Clip with shears","Natural leaf only","Random ticks paused");
			for(int x=0;x<9;x++) for(int y=0;y<5;y++) level.setBlock(wall.offset(40+x,y,0),JugcraftAgriculture.block("holly_planks").defaultBlockState(),Block.UPDATE_CLIENTS);
			var wreath=JugcraftAgriculture.block("holly_wreath").defaultBlockState().setValue(AutumnWreathBlock.FACING,Direction.SOUTH);
			var hung=wall.offset(42,3,1); level.setBlock(hung,wreath,Block.UPDATE_ALL); saved.put(hung,level.getBlockState(hung));
			var door=wall.offset(46,0,1); level.setBlock(door,Blocks.OAK_DOOR.defaultBlockState(),Block.UPDATE_ALL); level.setBlock(door.above(),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),Block.UPDATE_ALL);
			var onDoor=door.above().south(); level.setBlock(onDoor,wreath,Block.UPDATE_ALL);
			// Keep one migrated block and a second door with distinct wreaths on its two physical faces.
			var right=wall.offset(54,0,1); var rightState=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HINGE,DoorHingeSide.RIGHT).setValue(DoorBlock.OPEN,true);
			level.setBlock(right,rightState,Block.UPDATE_ALL); level.setBlock(right.above(),rightState.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),Block.UPDATE_ALL);
			DoorWreath.attach(level,right.above(),false,JugcraftAgriculture.block("autumn_wreath").defaultBlockState().setValue(AutumnWreathBlock.FLOWERS,AutumnWreathBlock.Mums.RED));
			PineWoodClientGameTests.label(level,right.offset(0,0,3),"Both faces","Holly front","Autumn back");
			PineWoodClientGameTests.label(level,wall.offset(43,0,3),"Holly Wreath","8 sprigs + string","Walls and doors");
			var player=server.getPlayerList().getPlayers().getFirst();
			for(var stack:List.of(new ItemStack(Items.SHEARS),new ItemStack(JugcraftAgriculture.item("holly_sprig"),32),new ItemStack(JugcraftAgriculture.block("holly_wreath"),8),new ItemStack(JugcraftAgriculture.block("holly_sapling"),16))) player.getInventory().add(stack);
		});
		checkClippingInput(context, world, wall.offset(32,1,0), wall.offset(0,4,0));
		checkDoorInput(context, world, wall.offset(54,0,1));
		shoot(context,world,GARDEN.offset(8,5,11),GARDEN.above(3),screenshotPrefix()+"tree_close");
		shoot(context,world,wood.offset(2,7,32),wood.offset(2,2,0),screenshotPrefix()+"wood_comparison");
		shoot(context,world,wood.offset(-18,4,11),wood.offset(-18,2,0),screenshotPrefix()+"wood_close");
		for(boolean cutout:List.of(true,false)) {
			context.runOnClient(client->client.options.cutoutLeaves().set(cutout));
			shoot(context,world,wall.offset(10,5,19),wall.offset(10,3,0),screenshotPrefix()+(cutout?"foliage_cutout":"foliage_solid"));
		}
		context.runOnClient(client->client.options.cutoutLeaves().set(true));
		shoot(context,world,wall.offset(44,4,11),wall.offset(44,2,0),screenshotPrefix()+"wreaths");
		var doorBase = wall.offset(46,0,1);
		world.getServer().runOnServer(server -> {
			var level = server.overworld(); var state = level.getBlockState(doorBase);
			((DoorBlock)state.getBlock()).setOpen(null, level, state, doorBase, true);
		});
		shoot(context,world,wall.offset(50,1,4),doorBase.above(),screenshotPrefix()+"wreath_open_door");
		world.getServer().runOnServer(server -> {
			var level = server.overworld(); var state = level.getBlockState(doorBase);
			((DoorBlock)state.getBlock()).setOpen(null, level, state, doorBase, false);
		});
		context.waitTicks(5);
		shoot(context,world,wall.offset(50,1,2),wall.offset(54,1,1),screenshotPrefix()+"wreath_right_front");
		shoot(context,world,wall.offset(58,1,2),wall.offset(54,1,1),screenshotPrefix()+"wreath_right_back");
		// A winter garden view keeps the same geometry, now with scarlet berry foliage.
		world.getServer().runOnServer(server->{
			var level=server.overworld();
			for(var p:BlockPos.betweenClosed(GARDEN.offset(-10,0,-90),GARDEN.offset(10,12,10))) {
				var state=level.getBlockState(p); if(state.is(JugcraftAgriculture.block("holly_leaves"))) {
					var winter=state.setValue(SeasonalLeavesBlock.SEASON,SeasonalLeavesBlock.Foliage.BARE); level.setBlock(p,winter,Block.UPDATE_CLIENTS); saved.put(p.immutable(),winter);
				}
			}
		});
		shoot(context,world,GARDEN.offset(8,5,11),GARDEN.above(3),screenshotPrefix()+"winter_tree");
		tour.add("Timber and native comparisons: /tp @s 1002 181 -889 180 0");
		tour.add("Foliage states and clipping: /tp @s 1010 181 -851 180 0");
		tour.add("Holly Wreaths on wall and door: /tp @s 1044 181 -859 180 0");
		tour.add("Right-click a holly tree's leaves or the marked natural leaf with shears. An empty hand gives no sprigs. Flat panels are decorative and cannot be clipped.");
		tour.add("Crouch and right-click either broad face of a door to hang a wreath, open or closed. It stays on that same face as the door swings. Click the wreath to use its wooden door; punch to remove it. The right-hinged sample has Holly on the front and Autumn on the back. Obstructed wreaths drop once.");
		tour.add("Random ticks are frozen. /gamerule minecraft:random_tick_speed 3 restores sprig regrowth, sapling growth and leaf decay.");
		tour.add("Garden trees display winter berries; all foliage stays green. Natural biome stops were not planted. Batch 7 is unstarted.");
	}

	private void checkDoorInput(ClientGameTestContext context, TestSingleplayerContext world, BlockPos door) {
		var server=world.getServer(); var support=door.above();
		server.runOnServer(minecraft->{
			var player=minecraft.getPlayerList().getPlayers().getFirst(); player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugcraftAgriculture.block("holly_wreath"),2));
			player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
			player.getAbilities().flying=false; player.onUpdateAbilities();
		});
		server.runCommand("tp @a "+(door.getX()-1.5)+" "+door.getY()+" "+(door.getZ()+0.5));
		context.waitTicks(10); context.getInput().holdKey(options->options.keyShift); context.waitTicks(3);
		context.getInput().lookAt(support); context.waitTicks(3); context.getInput().pressKey(options->options.keyUse); context.waitTicks(6);
		context.getInput().releaseKey(options->options.keyShift);
		server.runOnServer(minecraft->{
			var player=minecraft.getPlayerList().getPlayers().getFirst();
			if(player.getMainHandItem().getCount()!=1 || minecraft.overworld().getEntitiesOfClass(DoorWreath.class,new AABB(support).inflate(1),e->e.getPos().equals(support)&&e.isFront()).size()!=1)
				throw new AssertionError("Real input did not place wreath on the open door's inner face");
			player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
		});
		context.waitTicks(6); context.getInput().lookAt(support); context.waitTicks(3);
		context.runOnClient(client->{if(!(client.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof DoorWreath ring) || !ring.isFront()) throw new AssertionError("Inner door wreath is not pickable");});
		context.getInput().pressKey(options->options.keyUse); context.waitTicks(6);
		server.runOnServer(minecraft->{
			var level=minecraft.overworld(); if(level.getBlockState(door).getValue(DoorBlock.OPEN)) throw new AssertionError("Clicking wreath did not close its door");
			((DoorBlock)level.getBlockState(door).getBlock()).setOpen(null,level,level.getBlockState(door),door,true);
		});
		context.waitTicks(6); context.getInput().lookAt(support); context.waitTicks(3); context.getInput().pressKey(options->options.keyAttack); context.waitTicks(6);
		server.runOnServer(minecraft->{
			var level=minecraft.overworld();
			if(!level.getEntitiesOfClass(DoorWreath.class,new AABB(support).inflate(1),e->e.getPos().equals(support)&&e.isFront()).isEmpty()) throw new AssertionError("Actual attack did not remove the inner-face wreath");
			DoorWreath.attach(level,support,true,JugcraftAgriculture.block("holly_wreath").defaultBlockState());
			var player=minecraft.getPlayerList().getPlayers().getFirst(); player.setGameMode(GameType.CREATIVE); player.getInventory().add(new ItemStack(JugcraftAgriculture.block("holly_wreath"),8));
		});
		Jugcraft.LOGGER.info("[wreath interaction] Actual client input placed on open door, picked inner face, closed through wreath and removed it");
	}

	private void checkClippingInput(ClientGameTestContext context, TestSingleplayerContext world, BlockPos natural, BlockPos display) {
		var server = world.getServer();
		server.runOnServer(minecraft -> {
			var player = minecraft.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		});
		server.runCommand("tp @a " + (natural.getX()+0.5) + " " + (natural.getY()-1) + " " + (natural.getZ()+2.5));
		context.waitTicks(10); context.getInput().lookAt(natural); context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse); context.waitTicks(5);
		server.runOnServer(minecraft -> {
			if(minecraft.overworld().getBlockState(natural).getValue(HollyLeavesBlock.CLIPPED)) throw new AssertionError("Empty hand clipped holly");
			minecraft.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
		});
		context.waitTicks(5); context.getInput().pressKey(options -> options.keyUse); context.waitTicks(10);
		server.runOnServer(minecraft -> {
			var player = minecraft.getPlayerList().getPlayers().getFirst();
			if(!minecraft.overworld().getBlockState(natural).getValue(HollyLeavesBlock.CLIPPED) || player.getMainHandItem().getDamageValue()!=1)
				throw new AssertionError("Real shears input failed to clip the marked natural leaf");
		});
		context.getInput().pressKey(options -> options.keyUse); context.waitTicks(5);
		server.runOnServer(minecraft -> {
			if(minecraft.getPlayerList().getPlayers().getFirst().getMainHandItem().getDamageValue()!=1) throw new AssertionError("Repeat clip consumed shears");
		});
		server.runCommand("tp @a " + (display.getX()+0.5) + " " + (display.getY()-1) + " " + (display.getZ()+2.5));
		server.runOnServer(minecraft -> { var player=minecraft.getPlayerList().getPlayers().getFirst(); player.getAbilities().mayfly=true; player.getAbilities().flying=true; player.onUpdateAbilities(); });
		context.waitTicks(10); context.getInput().lookAt(display); context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse); context.waitTicks(5);
		server.runOnServer(minecraft -> {
			var level=minecraft.overworld(); var player=minecraft.getPlayerList().getPlayers().getFirst();
			if(level.getBlockState(display).getValue(HollyLeavesBlock.CLIPPED) || player.getMainHandItem().getDamageValue()!=1)
				throw new AssertionError("Placed display leaf yielded sprigs");
			level.setBlock(natural, level.getBlockState(natural).setValue(HollyLeavesBlock.CLIPPED,false),Block.UPDATE_CLIENTS);
			player.setGameMode(GameType.CREATIVE);
		});
		Jugcraft.LOGGER.info("[holly interaction] Actual client input: empty hand refused, shears clipped natural leaf once, display leaf refused");
	}
}
