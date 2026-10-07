package io.github.jimbozoomer.jugcraft.styx;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.town.Town;
import io.github.jimbozoomer.jugcraft.town.TownBuilder;
import io.github.jimbozoomer.jugcraft.town.TownData;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** New-town resident plus explicit, previewed placement for worlds without an assigned home. */
public final class StyxConservatory {
	public static final int WIDTH=67, HEIGHT=63, DEPTH=49, BLOCKS_PER_TICK=128, CURRENT_LAYOUT=3;
	public record Placement(BlockPos offset, BlockState state) { }
	private record Preview(BlockPos origin, long expires) { }
	private static final Map<UUID,Preview> PREVIEWS=new HashMap<>();
	private record Blueprint(int width, int height, int depth, List<Placement> blocks) { }
	private static final Map<Integer,Blueprint> BLUEPRINTS=new HashMap<>();
	private StyxConservatory() { }
	public static List<Placement> placements() { return blueprint(CURRENT_LAYOUT).blocks(); }
	public static List<Placement> placements(int layout) { return blueprint(layout).blocks(); }
	private static Blueprint blueprint(int layout) {
		if(layout<1 || layout>CURRENT_LAYOUT) throw new IllegalArgumentException("Unknown Styx layout: "+layout);
		return BLUEPRINTS.computeIfAbsent(layout,version -> {
			String path="/data/jugcraft/styx/conservatory"+(version==CURRENT_LAYOUT ? "" : "_v"+version)+".json";
			try(var stream=StyxConservatory.class.getResourceAsStream(path)) {
				if(stream==null) throw new IllegalStateException("Missing conservatory blueprint");
				var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
				var size=json.getAsJsonArray("size");int w=size.get(0).getAsInt(),h=size.get(1).getAsInt(),d=size.get(2).getAsInt();
				int[] dimensions=switch(version) {case 1 -> new int[]{27,28,21};case 2 -> new int[]{49,35,39};default -> new int[]{WIDTH,HEIGHT,DEPTH};};
				if(w!=dimensions[0] || h!=dimensions[1] || d!=dimensions[2]) throw new IllegalStateException("Unexpected conservatory size");
				var palette=new ArrayList<BlockState>();
				for(var entry:json.getAsJsonArray("palette")) {
					String text=entry.getAsString();BlockState state=TownData.parse(text);
					if(state.isAir() && !text.equals("minecraft:air")) throw new IllegalStateException("Invalid blueprint state: "+text);
					palette.add(state);
				}
				var list=new ArrayList<Placement>();
				for(var entry:json.getAsJsonArray("blocks")) {
					var b=entry.getAsJsonArray();
					BlockPos pos=new BlockPos(b.get(0).getAsInt(),b.get(1).getAsInt(),b.get(2).getAsInt());
					if(pos.getX()<0 || pos.getX()>=w || pos.getY()<0 || pos.getY()>=h || pos.getZ()<0 || pos.getZ()>=d) throw new IllegalStateException("Conservatory block out of bounds");
					list.add(new Placement(pos,palette.get(b.get(3).getAsInt())));
				}
				return new Blueprint(w,h,d,List.copyOf(list));
			} catch(Exception e) { throw new IllegalStateException("Cannot load conservatory",e); }
		});
	}
	public static BlockPos routineTarget(BlockPos home,int layout,int phase) {
		if(layout==1) return home.offset(phase==0 ? 18 : 8,phase==1 ? 8 : phase==2 ? 15 : 1,phase==0 ? 11 : phase==2 ? 9 : 10);
		if(layout==2) return home.offset(phase==0 ? 37 : 12,phase==1 ? 9 : phase==2 ? 17 : 1,phase==0 ? 21 : 24);
		return home.offset(phase==0 ? 52 : 13,phase==1 ? 9 : phase==2 ? 41 : 1,phase==0 ? 34 : phase==2 ? 25 : 24);
	}
	public static boolean loaded(ServerLevel level,BlockPos origin) { return loaded(level,origin,CURRENT_LAYOUT); }
	public static boolean loaded(ServerLevel level,BlockPos origin,int layout) {
		Blueprint plan=blueprint(layout);
		for(int x=origin.getX()>>4;x<=(origin.getX()+plan.width()-1)>>4;x++)
			for(int z=origin.getZ()>>4;z<=(origin.getZ()+plan.depth()-1)>>4;z++)
				if(level.getChunkSource().getChunkNow(x,z)==null) return false;
		return true;
	}
	/** Reads loaded chunks only. Rejects homes, block entities, solid obstacles and the protected town. */
	public static String canPlace(ServerLevel level,BlockPos origin) {
		if(level.dimension()!=Level.OVERWORLD) return "The conservatory belongs in the Overworld.";
		if(StyxState.get(level).origin.isPresent()) return "This world already has a conservatory.";
		if(origin.getY()<level.getMinY() || origin.getY()+HEIGHT>=level.getMaxY()) return "Not enough vertical room.";
		if(!loaded(level,origin)) return "Load the whole preview area first; no chunks will be forced.";
		for(int x=0;x<WIDTH;x++) for(int z=0;z<DEPTH;z++) for(int y=0;y<HEIGHT;y++) {
			BlockPos p=origin.offset(x,y,z); BlockState state=level.getBlockState(p);
			if(Town.isInside(level,p) || level.getBlockEntity(p)!=null) return "The footprint overlaps a protected town or a block entity.";
			if(!state.isAir() && !state.canBeReplaced() && !(y==0 && (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)))) return "Clear a level "+WIDTH+" x "+DEPTH+" area with "+HEIGHT+" blocks of headroom first. Obstruction: "+BuiltInRegistries.BLOCK.getKey(state.getBlock())+" at "+p.toShortString()+" (floor "+origin.getY()+").";
		}
		return "";
	}
	public static boolean begin(ServerLevel level,BlockPos origin) {
		if(!canPlace(level,origin).isEmpty()) return false;
		StyxState state=StyxState.get(level);state.origin=Optional.of(origin.immutable());state.placed=0;state.layout=CURRENT_LAYOUT;state.setDirty();return true;
	}
	public static void tick(ServerLevel level) {
		StyxTownDistrict.reserve(level);
		StyxState state=StyxState.get(level);
		if(state.origin.isEmpty() || state.resident.isPresent()) return;
		BlockPos origin=state.origin.get();
		if(!loaded(level,origin,state.layout)) return;
		List<Placement> blocks=placements(state.layout);
		if(state.townHome) {
			if(!StyxTownDistrict.ready(level,origin)) return;
			state.placed=blocks.size();
		}
		int end=Math.min(blocks.size(),state.placed+BLOCKS_PER_TICK);
		state.setDirty(); // Retain progress even if a later entry in this batch is obstructed.
		for(;state.placed<end;state.placed++) {
			Placement p=blocks.get(state.placed);BlockPos at=origin.offset(p.offset());
			BlockState current=level.getBlockState(at);
			// Pause rather than overwrite a new obstruction introduced after preview or while unloaded.
			if(current!=p.state() && !current.isAir() && !current.canBeReplaced() && !(p.offset().getY()==0 && (current.is(BlockTags.DIRT) || current.is(Blocks.GRASS_BLOCK)))) return;
			TownBuilder.set(level,at,p.state());
		}
		state.setDirty();
		if(state.placed==blocks.size()) {
			Styxhexenhammer wizard=JugcraftStyx.WIZARD.create(level,EntitySpawnReason.STRUCTURE);
			if(wizard!=null) {
				wizard.setHome(origin,state.layout);BlockPos spawn=routineTarget(origin,state.layout,3);
				wizard.snapTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0);
				if(level.addFreshEntity(wizard)) {state.resident=Optional.of(wizard.getUUID());state.setDirty();}
			}
		}
	}
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick(server.overworld()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PREVIEWS.clear());
		CommandRegistrationCallback.EVENT.register((dispatcher,access,env) -> dispatcher.register(Commands.literal("jugcraft")
				.then(Commands.literal("styx")
					.executes(c -> {var s=StyxState.get(c.getSource().getServer().overworld());c.getSource().sendSuccess(() -> Component.literal(s.origin.map(p -> "Nightglass Conservatory: "+p.toShortString()+"; placed "+s.placed+" blocks").orElse("No conservatory. Operators: /jugcraft styx preview, then /jugcraft styx place.")),false);return 1;})
					.then(Commands.literal("preview").requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions())).executes(c -> {
						var player=c.getSource().getPlayerOrException();BlockPos origin=player.blockPosition().offset(-WIDTH/2,-1,-DEPTH/2);
						String error=canPlace(player.level(),origin);
						if(!error.isEmpty()) {c.getSource().sendFailure(Component.literal(error));return 0;}
						PREVIEWS.put(player.getUUID(),new Preview(origin,player.level().getGameTime()+1200));
						c.getSource().sendSuccess(() -> Component.literal("Conservatory footprint "+origin.toShortString()+" to "+origin.offset(WIDTH-1,HEIGHT-1,DEPTH-1).toShortString()+". /jugcraft styx place confirms this area within 60 seconds."),false);return 1;
					}))
					.then(Commands.literal("place").requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions())).executes(c -> {
						var player=c.getSource().getPlayerOrException();Preview preview=PREVIEWS.remove(player.getUUID());
						if(preview==null || preview.expires()<player.level().getGameTime() || player.blockPosition().distSqr(preview.origin())>4096 || !begin(player.level(),preview.origin())) {
							c.getSource().sendFailure(Component.literal("Preview expired, moved out of reach, or footprint changed. Run /jugcraft styx preview again."));return 0;
						}
						c.getSource().sendSuccess(() -> Component.literal("Building the conservatory in bounded steps; Styxhexenhammer will arrive when it is complete."),true);return 1;
					})))));
	}
}
